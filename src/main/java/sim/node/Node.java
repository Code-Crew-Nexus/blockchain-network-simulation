package sim.node;

import sim.blockchain.Block;
import sim.blockchain.Chain;
import sim.blockchain.Transaction;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an individual peer node in the blockchain network.
 * Manages peer connectivity, local ledger state, mempool, mining, and fork resolution.
 */
public class Node {
    private final int nodeId;
    private final Set<Integer> peers;
    private final Chain localChain;
    private final List<Transaction> mempool;
    private final Map<String, Block> knownBlocks;

    private NodeState state;
    private int partitionId; // 0 = unpartitioned, 1, 2... = partition cluster
    private boolean isByzantine;
    private double miningPower; // relative hash rate fraction

    public Node(int nodeId, Block genesisBlock) {
        this.nodeId = nodeId;
        this.peers = new HashSet<>();
        this.localChain = genesisBlock != null ? new Chain(genesisBlock) : new Chain();
        this.mempool = new ArrayList<>();
        this.knownBlocks = new HashMap<>();
        this.state = NodeState.ONLINE;
        this.partitionId = 0;
        this.isByzantine = false;
        this.miningPower = 1.0;

        if (genesisBlock != null) {
            knownBlocks.put(genesisBlock.getHash(), genesisBlock);
        }
    }

    public int getNodeId() {
        return nodeId;
    }

    public Set<Integer> getPeers() {
        return Collections.unmodifiableSet(peers);
    }

    public void addPeer(int peerId) {
        if (peerId != nodeId) {
            peers.add(peerId);
        }
    }

    public void removePeer(int peerId) {
        peers.remove(peerId);
    }

    public void clearPeers() {
        peers.clear();
    }

    public Chain getLocalChain() {
        return localChain;
    }

    public List<Transaction> getMempool() {
        return Collections.unmodifiableList(mempool);
    }

    public NodeState getState() {
        return state;
    }

    public void setState(NodeState state) {
        this.state = state;
    }

    public boolean isOnline() {
        return state != NodeState.OFFLINE_CRASHED;
    }

    public boolean isCrashed() {
        return state == NodeState.OFFLINE_CRASHED;
    }

    public int getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(int partitionId) {
        this.partitionId = partitionId;
        if (partitionId > 0 && this.state == NodeState.ONLINE) {
            this.state = NodeState.PARTITIONED;
        } else if (partitionId == 0 && this.state == NodeState.PARTITIONED) {
            this.state = NodeState.ONLINE;
        }
    }

    public boolean isByzantine() {
        return isByzantine;
    }

    public void setByzantine(boolean byzantine) {
        isByzantine = byzantine;
    }

    public double getMiningPower() {
        return miningPower;
    }

    public void setMiningPower(double miningPower) {
        this.miningPower = miningPower;
    }

    /**
     * Checks whether communication with a peer is currently possible
     * given crash states and network partitions.
     */
    public boolean canCommunicateWith(Node other) {
        if (this.isCrashed() || other.isCrashed()) {
            return false;
        }
        // If either is partitioned, they can only communicate if in the same non-zero partition
        if (this.partitionId != other.partitionId) {
            return false;
        }
        return true;
    }

    /**
     * Adds a transaction to the mempool if not already present.
     */
    public boolean receiveTransaction(Transaction tx) {
        if (!isOnline() || tx == null) return false;
        if (!mempool.contains(tx)) {
            mempool.add(tx);
            return true;
        }
        return false;
    }

    /**
     * Processes an incoming block received from gossip.
     *
     * @param block block received
     * @param difficulty expected difficulty
     * @return true if block was accepted and extended the chain or caused reorganization
     */
    public boolean receiveBlock(Block block, int difficulty) {
        if (!isOnline() || block == null) return false;

        // Byzantine nodes can reject valid blocks or behave erratically
        if (isByzantine) {
            // Byzantine node ignores external valid blocks to mine private selfish chain
            return false;
        }

        // Check if block is already known
        if (knownBlocks.containsKey(block.getHash())) {
            return false;
        }

        // Validate block's internal cryptographic hash
        if (!block.isValid(difficulty)) {
            return false;
        }

        knownBlocks.put(block.getHash(), block);

        // Case 1: Fits directly on top of local chain
        if (localChain.canAccept(block, difficulty)) {
            localChain.addBlock(block, difficulty);
            // Remove confirmed transactions from mempool
            for (Transaction tx : block.getTransactions()) {
                mempool.remove(tx);
            }
            return true;
        }

        // Case 2: Block is at a lower or equal height that does not beat our chain
        Block tip = localChain.getLatestBlock();
        if (tip != null && block.getIndex() <= tip.getIndex()) {
            // Stored in knownBlocks, but does not trigger reorg
            return false;
        }

        // Case 3: Block might be part of a longer alternative fork
        return attemptReorganization(block, difficulty);
    }

    /**
     * Attempts to trace backwards from a newly received block to see if it connects
     * to a fork of our chain with greater cumulative work (longest chain rule).
     */
    private boolean attemptReorganization(Block newTipCandidate, int difficulty) {
        List<Block> alternativeBranch = new ArrayList<>();
        Block curr = newTipCandidate;

        while (curr != null && curr.getIndex() > 0) {
            alternativeBranch.add(0, curr);
            curr = knownBlocks.get(curr.getPreviousHash());
        }

        // Check if branch connects back to genesis
        if (curr == null || curr.getIndex() != 0) {
            return false; // Incomplete branch in known blocks
        }
        alternativeBranch.add(0, curr);

        // Build candidate chain
        Chain candidateChain = new Chain();
        for (Block b : alternativeBranch) {
            candidateChain.addBlock(b, b.getDifficulty());
        }

        if (localChain.shouldAdopt(candidateChain)) {
            // Reorganization accepted:
            // 1. Gather all txs from old chain blocks being abandoned
            int commonIdx = localChain.findCommonAncestorIndex(candidateChain);
            for (int i = commonIdx + 1; i < localChain.getLength(); i++) {
                Block abandoned = localChain.getBlockByIndex(i);
                if (abandoned != null) {
                    mempool.addAll(abandoned.getTransactions());
                }
            }
            // 2. Remove confirmed txs from newly adopted blocks
            for (int i = commonIdx + 1; i < candidateChain.getLength(); i++) {
                Block adopted = candidateChain.getBlockByIndex(i);
                if (adopted != null) {
                    mempool.removeAll(adopted.getTransactions());
                }
            }
            // 3. Adopt candidate chain
            localChain.replaceWith(candidateChain);
            return true;
        }

        return false;
    }

    /**
     * Mines a new block on top of the local chain tip.
     *
     * @param difficulty mining difficulty
     * @param timestamp simulation timestamp
     * @return mined block or null if offline
     */
    public Block mineBlock(int difficulty, long timestamp) {
        if (!isOnline()) return null;

        Block tip = localChain.getLatestBlock();
        String prevHash = tip != null ? tip.getHash() : Block.GENESIS_PREV_HASH;
        int nextIndex = tip != null ? tip.getIndex() + 1 : 0;

        List<Transaction> txsToInclude = new ArrayList<>();
        int txLimit = Math.min(mempool.size(), 10);
        for (int i = 0; i < txLimit; i++) {
            txsToInclude.add(mempool.get(i));
        }

        Block candidate;
        if (isByzantine) {
            // Byzantine node creates an invalid block (e.g. invalid prevHash or corrupt difficulty)
            candidate = new Block(nextIndex, "BYZANTINE_CORRUPTED_HASH", timestamp, txsToInclude, difficulty, nodeId);
        } else {
            candidate = new Block(nextIndex, prevHash, timestamp, txsToInclude, difficulty, nodeId);
            candidate.mine(difficulty);
        }

        if (!isByzantine) {
            localChain.addBlock(candidate, difficulty);
            knownBlocks.put(candidate.getHash(), candidate);
            mempool.removeAll(txsToInclude);
        }

        return candidate;
    }

    public boolean hasBlock(String hash) {
        return knownBlocks.containsKey(hash);
    }

    public Block getBlock(String hash) {
        return knownBlocks.get(hash);
    }

    public Map<String, Block> getKnownBlocks() {
        return Collections.unmodifiableMap(knownBlocks);
    }

    @Override
    public String toString() {
        return String.format("Node-%d [State: %s, Peers: %d, ChainHeight: %d, Partition: %d]",
                nodeId, state, peers.size(), localChain.getLength(), partitionId);
    }
}
