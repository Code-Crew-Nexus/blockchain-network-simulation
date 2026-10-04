package sim.engine;

import sim.blockchain.Block;
import sim.blockchain.Transaction;
import sim.faults.PartitionInfo;
import sim.gossip.GossipProtocol;
import sim.network.LatencyModel;
import sim.network.NetworkTopology;
import sim.node.Node;
import sim.node.NodeState;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Discrete-Event Simulation Core Engine.
 * Coordinates virtual time advancement via a global PriorityQueue,
 * processing blockchain generation, gossip propagation, and fault injections deterministically.
 */
public class SimulationEngine {

    private final PriorityQueue<Event> eventQueue;
    private final Map<Integer, Node> nodes;
    private NetworkTopology topology;
    private LatencyModel latencyModel;
    private GossipProtocol gossipProtocol;
    private int difficulty;

    private long currentTimeMs;
    private long totalEventsProcessed;

    private final List<Consumer<Event>> eventListeners;
    private final List<BiConsumer<Long, SimulationEngine>> stepListeners;

    public SimulationEngine(NetworkTopology topology,
                            LatencyModel latencyModel,
                            int difficulty,
                            int gossipFanout,
                            long seed) {
        this.eventQueue = new PriorityQueue<>();
        this.nodes = new HashMap<>();
        this.topology = topology;
        this.latencyModel = latencyModel != null ? latencyModel : new LatencyModel();
        this.difficulty = difficulty;
        this.currentTimeMs = 0L;
        this.totalEventsProcessed = 0L;
        this.eventListeners = new ArrayList<>();
        this.stepListeners = new ArrayList<>();

        // Initialize genesis block
        Block genesis = Block.createGenesis(difficulty);

        // Initialize nodes
        for (int i = 0; i < topology.getNumNodes(); i++) {
            Node node = new Node(i, genesis);
            // Populate peer graph from topology
            for (int neighbor : topology.getNeighbors(i)) {
                node.addPeer(neighbor);
            }
            nodes.put(i, node);
        }

        // Initialize gossip protocol
        this.gossipProtocol = new GossipProtocol(gossipFanout, seed);
    }

    /**
     * Schedules a future discrete event.
     */
    public void schedule(Event event) {
        if (event != null) {
            eventQueue.add(event);
        }
    }

    /**
     * Helper to schedule an event at a delay relative to currentTimeMs.
     */
    public void scheduleAfter(long delayMs, EventType type, int sourceId, int targetId, Object payload) {
        schedule(new Event(currentTimeMs + Math.max(0, delayMs), type, sourceId, targetId, payload));
    }

    /**
     * Advances simulation by processing the single next event.
     *
     * @return true if an event was processed, false if the queue is empty
     */
    public boolean step() {
        if (eventQueue.isEmpty()) {
            return false;
        }

        Event event = eventQueue.poll();
        this.currentTimeMs = event.getTimestampMs();
        this.totalEventsProcessed++;

        processEvent(event);

        // Notify registered event listeners
        for (Consumer<Event> listener : eventListeners) {
            listener.accept(event);
        }

        // Notify step listeners
        for (BiConsumer<Long, SimulationEngine> stepListener : stepListeners) {
            stepListener.accept(currentTimeMs, this);
        }

        return true;
    }

    /**
     * Dispatches the event to the appropriate handler.
     */
    private void processEvent(Event event) {
        switch (event.getType()) {
            case BLOCK_MINED: {
                Block block = (Block) event.getPayload();
                gossipProtocol.broadcastBlock(event.getSourceNodeId(), block, this);
                break;
            }

            case BLOCK_RECEIVED: {
                Block block = (Block) event.getPayload();
                gossipProtocol.onBlockReceived(event.getTargetNodeId(), event.getSourceNodeId(), block, this);
                break;
            }

            case TX_CREATED: {
                Transaction tx = (Transaction) event.getPayload();
                Node node = nodes.get(event.getSourceNodeId());
                if (node != null && node.isOnline()) {
                    node.receiveTransaction(tx);
                    gossipProtocol.broadcastTransaction(event.getSourceNodeId(), tx, this);
                }
                break;
            }

            case TX_RECEIVED: {
                Transaction tx = (Transaction) event.getPayload();
                gossipProtocol.onTransactionReceived(event.getTargetNodeId(), event.getSourceNodeId(), tx, this);
                break;
            }

            case NODE_CRASH: {
                Node node = nodes.get(event.getTargetNodeId());
                if (node != null) {
                    node.setState(NodeState.OFFLINE_CRASHED);
                }
                break;
            }

            case NODE_RECOVER: {
                Node node = nodes.get(event.getTargetNodeId());
                if (node != null) {
                    node.setState(NodeState.ONLINE);
                    // On recovery, query peers to catch up on chain
                    gossipProtocol.synchronizeNodeWithPeers(node.getNodeId(), this);
                }
                break;
            }

            case PARTITION_START: {
                PartitionInfo partitionInfo = (PartitionInfo) event.getPayload();
                if (partitionInfo != null) {
                    for (Map.Entry<Integer, Integer> entry : partitionInfo.getNodePartitionMap().entrySet()) {
                        Node node = nodes.get(entry.getKey());
                        if (node != null) {
                            node.setPartitionId(entry.getValue());
                        }
                    }
                }
                break;
            }

            case PARTITION_HEAL: {
                // Heal partition: clear partition IDs and trigger cross-component reconciliation
                for (Node node : nodes.values()) {
                    node.setPartitionId(0);
                }
                // Trigger mutual gossip synchronization between all nodes
                gossipProtocol.reconcilePartitions(this);
                break;
            }

            case MINE_TICK: {
                Node miner = nodes.get(event.getSourceNodeId());
                if (miner != null && miner.isOnline()) {
                    Block newBlock = miner.mineBlock(difficulty, currentTimeMs);
                    if (newBlock != null) {
                        scheduleAfter(0, EventType.BLOCK_MINED, miner.getNodeId(), -1, newBlock);
                    }
                }
                break;
            }
        }
    }

    /**
     * Executes the simulation until the queue is exhausted or maxTimeMs is reached.
     */
    public void runUntil(long maxTimeMs) {
        while (!eventQueue.isEmpty() && eventQueue.peek().getTimestampMs() <= maxTimeMs) {
            step();
        }
        if (currentTimeMs < maxTimeMs) {
            currentTimeMs = maxTimeMs;
        }
    }

    /**
     * Executes until all scheduled events have finished processing.
     */
    public void runUntilEmpty() {
        while (!eventQueue.isEmpty()) {
            step();
        }
    }

    public void addEventListener(Consumer<Event> listener) {
        eventListeners.add(listener);
    }

    public void addStepListener(BiConsumer<Long, SimulationEngine> listener) {
        stepListeners.add(listener);
    }

    // Getters and Setters
    public long getCurrentTimeMs() {
        return currentTimeMs;
    }

    public long getTotalEventsProcessed() {
        return totalEventsProcessed;
    }

    public int getQueueSize() {
        return eventQueue.size();
    }

    public Map<Integer, Node> getNodes() {
        return Collections.unmodifiableMap(nodes);
    }

    public Node getNode(int nodeId) {
        return nodes.get(nodeId);
    }

    public NetworkTopology getTopology() {
        return topology;
    }

    public void setTopology(NetworkTopology topology) {
        this.topology = topology;
    }

    public LatencyModel getLatencyModel() {
        return latencyModel;
    }

    public void setLatencyModel(LatencyModel latencyModel) {
        this.latencyModel = latencyModel;
    }

    public GossipProtocol getGossipProtocol() {
        return gossipProtocol;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }
}
