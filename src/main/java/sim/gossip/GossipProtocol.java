package sim.gossip;

import sim.blockchain.Block;
import sim.blockchain.Transaction;
import sim.engine.EventType;
import sim.engine.SimulationEngine;
import sim.node.Node;

import java.util.*;

/**
 * Epidemic Gossip Protocol for P2P Block and Transaction Dissemination.
 * Provides deterministic, reproducible propagation with configurable fanout,
 * duplicate filtering, and latency-aware discrete event scheduling.
 */
public class GossipProtocol {

    private final int fanout; // -1 means flood to all peers; >0 means random sample of k peers
    private final Random random;

    // Tracks which nodes have already processed a specific block hash: blockHash -> Set<NodeId>
    private final Map<String, Set<Integer>> blockDeliveredTo;

    // Tracks which nodes have seen a specific transaction ID: txId -> Set<NodeId>
    private final Map<String, Set<Integer>> txDeliveredTo;

    public GossipProtocol(int fanout, long seed) {
        this.fanout = fanout;
        this.random = new Random(seed);
        this.blockDeliveredTo = new HashMap<>();
        this.txDeliveredTo = new HashMap<>();
    }

    public int getFanout() {
        return fanout;
    }

    /**
     * Initiates block dissemination from the miner node across the network.
     */
    public void broadcastBlock(int minerId, Block block, SimulationEngine engine) {
        if (block == null) return;

        blockDeliveredTo.computeIfAbsent(block.getHash(), k -> new HashSet<>()).add(minerId);
        relayBlockToPeers(minerId, -1, block, engine);
    }

    /**
     * Handles arrival of a block at a receiving node.
     */
    public void onBlockReceived(int receiverId, int fromPeerId, Block block, SimulationEngine engine) {
        if (block == null) return;

        Node receiver = engine.getNode(receiverId);
        if (receiver == null || !receiver.isOnline()) {
            return;
        }

        Set<Integer> deliveredNodes = blockDeliveredTo.computeIfAbsent(block.getHash(), k -> new HashSet<>());
        if (deliveredNodes.contains(receiverId)) {
            // Already processed this block; suppress gossip loop
            return;
        }

        // Deliver block to node's consensus/ledger processor
        boolean accepted = receiver.receiveBlock(block, engine.getDifficulty());
        deliveredNodes.add(receiverId);

        // Continue gossiping to neighbors if block was valid/accepted
        if (accepted || receiver.hasBlock(block.getHash())) {
            relayBlockToPeers(receiverId, fromPeerId, block, engine);
        }
    }

    /**
     * Relays a block to downstream peers according to the configured fanout factor.
     */
    private void relayBlockToPeers(int senderId, int excludePeerId, Block block, SimulationEngine engine) {
        Node sender = engine.getNode(senderId);
        if (sender == null || !sender.isOnline()) return;

        List<Integer> candidatePeers = new ArrayList<>();
        Set<Integer> deliveredNodes = blockDeliveredTo.getOrDefault(block.getHash(), Collections.emptySet());

        for (int peerId : sender.getPeers()) {
            if (peerId == excludePeerId) continue;
            if (deliveredNodes.contains(peerId)) continue; // Peer already got it

            Node peer = engine.getNode(peerId);
            if (peer != null && sender.canCommunicateWith(peer)) {
                candidatePeers.add(peerId);
            }
        }

        List<Integer> targets = selectTargets(candidatePeers, fanout);
        int blockSizeBytes = 500_000; // Simulated ~500KB block size

        for (int targetId : targets) {
            long latency = engine.getLatencyModel().calculateLatencyMs(
                    senderId, targetId, engine.getTopology(), blockSizeBytes);
            engine.scheduleAfter(latency, EventType.BLOCK_RECEIVED, senderId, targetId, block);
        }
    }

    /**
     * Broadcasts a new transaction into the network mempools.
     */
    public void broadcastTransaction(int senderId, Transaction tx, SimulationEngine engine) {
        if (tx == null) return;

        txDeliveredTo.computeIfAbsent(tx.getTxId(), k -> new HashSet<>()).add(senderId);
        relayTransactionToPeers(senderId, -1, tx, engine);
    }

    /**
     * Handles arrival of a transaction at a receiving node.
     */
    public void onTransactionReceived(int receiverId, int fromPeerId, Transaction tx, SimulationEngine engine) {
        if (tx == null) return;

        Node receiver = engine.getNode(receiverId);
        if (receiver == null || !receiver.isOnline()) return;

        Set<Integer> delivered = txDeliveredTo.computeIfAbsent(tx.getTxId(), k -> new HashSet<>());
        if (delivered.contains(receiverId)) return;

        boolean added = receiver.receiveTransaction(tx);
        delivered.add(receiverId);

        if (added) {
            relayTransactionToPeers(receiverId, fromPeerId, tx, engine);
        }
    }

    private void relayTransactionToPeers(int senderId, int excludePeerId, Transaction tx, SimulationEngine engine) {
        Node sender = engine.getNode(senderId);
        if (sender == null || !sender.isOnline()) return;

        List<Integer> candidates = new ArrayList<>();
        Set<Integer> delivered = txDeliveredTo.getOrDefault(tx.getTxId(), Collections.emptySet());

        for (int peerId : sender.getPeers()) {
            if (peerId == excludePeerId) continue;
            if (delivered.contains(peerId)) continue;

            Node peer = engine.getNode(peerId);
            if (peer != null && sender.canCommunicateWith(peer)) {
                candidates.add(peerId);
            }
        }

        List<Integer> targets = selectTargets(candidates, fanout);
        int txSizeBytes = 500; // 500 bytes per transaction

        for (int targetId : targets) {
            long latency = engine.getLatencyModel().calculateLatencyMs(
                    senderId, targetId, engine.getTopology(), txSizeBytes);
            engine.scheduleAfter(latency, EventType.TX_RECEIVED, senderId, targetId, tx);
        }
    }

    /**
     * Synchronizes a rejoining or recovered node with its neighbors.
     */
    public void synchronizeNodeWithPeers(int nodeId, SimulationEngine engine) {
        Node node = engine.getNode(nodeId);
        if (node == null || !node.isOnline()) return;

        for (int peerId : node.getPeers()) {
            Node peer = engine.getNode(peerId);
            if (peer != null && node.canCommunicateWith(peer)) {
                // Peer sends its latest block to recovering node
                Block tip = peer.getLocalChain().getLatestBlock();
                if (tip != null && tip.getIndex() > node.getLocalChain().getLength() - 1) {
                    long latency = engine.getLatencyModel().calculateLatencyMs(
                            peerId, nodeId, engine.getTopology(), 500_000);
                    engine.scheduleAfter(latency, EventType.BLOCK_RECEIVED, peerId, nodeId, tip);
                }
            }
        }
    }

    /**
     * Reconciles chains between all components after a network partition heals.
     */
    public void reconcilePartitions(SimulationEngine engine) {
        // Collect all distinct tip blocks from online nodes
        Map<String, Block> candidateTips = new HashMap<>();
        for (Node node : engine.getNodes().values()) {
            if (node.isOnline()) {
                Block tip = node.getLocalChain().getLatestBlock();
                if (tip != null) {
                    candidateTips.put(tip.getHash(), tip);
                }
            }
        }

        // Cross-propagate these tips to all nodes so longest chain wins
        for (Block tip : candidateTips.values()) {
            for (Node node : engine.getNodes().values()) {
                if (node.isOnline() && !node.hasBlock(tip.getHash())) {
                    long latency = engine.getLatencyModel().calculateLatencyMs(
                            tip.getMinerId(), node.getNodeId(), engine.getTopology(), 500_000);
                    engine.scheduleAfter(latency, EventType.BLOCK_RECEIVED, tip.getMinerId(), node.getNodeId(), tip);
                }
            }
        }
    }

    /**
     * Picks up to fanout targets uniformly at random from candidates.
     */
    private List<Integer> selectTargets(List<Integer> candidates, int k) {
        if (candidates.isEmpty()) return Collections.emptyList();
        if (k <= 0 || k >= candidates.size()) {
            return candidates; // Flood to all
        }

        List<Integer> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled, random);
        return shuffled.subList(0, k);
    }

    public Set<Integer> getNodesWithBlock(String blockHash) {
        return blockDeliveredTo.getOrDefault(blockHash, Collections.emptySet());
    }

    public void clearDeliveryTracking() {
        blockDeliveredTo.clear();
        txDeliveredTo.clear();
    }
}
