package sim.faults;

import sim.engine.EventType;
import sim.engine.SimulationEngine;
import sim.node.Node;

import java.util.*;

/**
 * Injects operational network faults, Byzantine adversaries, and churn into the simulation.
 * Includes random node crashes, network partitions with healing, and Byzantine equivocation.
 */
public class FaultInjector {

    private final Random random;

    public FaultInjector() {
        this(42L);
    }

    public FaultInjector(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Injects abrupt node crashes into a random fraction of the network.
     *
     * @param engine simulation engine
     * @param crashFraction fraction of nodes to crash (0.0 to 1.0)
     * @param crashTimeMs virtual timestamp when crashes take effect
     * @return set of crashed node IDs
     */
    public Set<Integer> injectNodeCrashes(SimulationEngine engine, double crashFraction, long crashTimeMs) {
        Set<Integer> crashedNodes = new HashSet<>();
        List<Integer> allNodeIds = new ArrayList<>(engine.getNodes().keySet());
        Collections.shuffle(allNodeIds, random);

        int numToCrash = (int) Math.round(allNodeIds.size() * Math.max(0.0, Math.min(1.0, crashFraction)));
        for (int i = 0; i < numToCrash; i++) {
            int nodeId = allNodeIds.get(i);
            crashedNodes.add(nodeId);
            engine.schedule(new sim.engine.Event(crashTimeMs, EventType.NODE_CRASH, -1, nodeId, null));
        }

        return crashedNodes;
    }

    /**
     * Schedules node recoveries after a specified downtime duration.
     */
    public void scheduleNodeRecoveries(SimulationEngine engine, Set<Integer> crashedNodes, long recoveryTimeMs) {
        for (int nodeId : crashedNodes) {
            engine.schedule(new sim.engine.Event(recoveryTimeMs, EventType.NODE_RECOVER, -1, nodeId, null));
        }
    }

    /**
     * Injects a network partition splitting the network into two isolated clusters (Partitions 1 and 2),
     * and schedules partition healing at a later timestamp.
     *
     * @param engine simulation engine
     * @param startTimeMs partition onset time
     * @param healTimeMs partition healing time
     * @return PartitionInfo mapping each node to its partition ID
     */
    public PartitionInfo injectNetworkPartition(SimulationEngine engine, long startTimeMs, long healTimeMs) {
        Map<Integer, Integer> nodePartitionMap = new HashMap<>();
        List<Integer> allNodes = new ArrayList<>(engine.getNodes().keySet());

        // Geographic / coordinate-based bisection (split by X coordinate)
        double avgX = 50.0;
        for (int nodeId : allNodes) {
            double[] coords = engine.getTopology().getCoordinates(nodeId);
            int partition = coords[0] < avgX ? 1 : 2;
            nodePartitionMap.put(nodeId, partition);
        }

        // Guarantee both partitions have at least 1 node
        long count1 = nodePartitionMap.values().stream().filter(p -> p == 1).count();
        if (count1 == 0 || count1 == allNodes.size()) {
            // Fallback to balanced index split
            for (int i = 0; i < allNodes.size(); i++) {
                nodePartitionMap.put(allNodes.get(i), (i % 2) + 1);
            }
        }

        PartitionInfo info = new PartitionInfo(nodePartitionMap, startTimeMs, healTimeMs);

        // Schedule partition onset and healing
        engine.schedule(new sim.engine.Event(startTimeMs, EventType.PARTITION_START, -1, -1, info));
        engine.schedule(new sim.engine.Event(healTimeMs, EventType.PARTITION_HEAL, -1, -1, info));

        return info;
    }

    /**
     * Designates a fraction of nodes as Byzantine adversaries.
     */
    public Set<Integer> injectByzantineNodes(SimulationEngine engine, double byzantineFraction) {
        Set<Integer> byzantineSet = new HashSet<>();
        List<Integer> allNodeIds = new ArrayList<>(engine.getNodes().keySet());
        Collections.shuffle(allNodeIds, random);

        int numByzantine = (int) Math.round(allNodeIds.size() * Math.max(0.0, Math.min(0.49, byzantineFraction)));
        for (int i = 0; i < numByzantine; i++) {
            int nodeId = allNodeIds.get(i);
            Node node = engine.getNode(nodeId);
            if (node != null) {
                node.setByzantine(true);
                byzantineSet.add(nodeId);
            }
        }
        return byzantineSet;
    }

    /**
     * Configures network churn where nodes periodically crash and recover.
     */
    public void scheduleNetworkChurn(SimulationEngine engine, int totalChurnEvents, long simDurationMs) {
        List<Integer> nodeIds = new ArrayList<>(engine.getNodes().keySet());
        if (nodeIds.isEmpty()) return;

        for (int i = 0; i < totalChurnEvents; i++) {
            long crashTime = (long) (random.nextDouble() * (simDurationMs * 0.7));
            long recoverTime = crashTime + (long) (random.nextDouble() * 2000 + 500);
            int nodeId = nodeIds.get(random.nextInt(nodeIds.size()));

            engine.schedule(new sim.engine.Event(crashTime, EventType.NODE_CRASH, -1, nodeId, null));
            engine.schedule(new sim.engine.Event(recoverTime, EventType.NODE_RECOVER, -1, nodeId, null));
        }
    }
}
