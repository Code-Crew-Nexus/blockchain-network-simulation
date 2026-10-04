package sim.faults;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates network partition state mapping nodes to partition cluster IDs.
 */
public class PartitionInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Integer, Integer> nodePartitionMap;
    private final long startTimeMs;
    private final long healTimeMs;

    public PartitionInfo(Map<Integer, Integer> nodePartitionMap, long startTimeMs, long healTimeMs) {
        this.nodePartitionMap = nodePartitionMap != null ? new HashMap<>(nodePartitionMap) : new HashMap<>();
        this.startTimeMs = startTimeMs;
        this.healTimeMs = healTimeMs;
    }

    public Map<Integer, Integer> getNodePartitionMap() {
        return Collections.unmodifiableMap(nodePartitionMap);
    }

    public long getStartTimeMs() {
        return startTimeMs;
    }

    public long getHealTimeMs() {
        return healTimeMs;
    }

    @Override
    public String toString() {
        return String.format("PartitionInfo[Nodes: %d, Start: %d ms, Heal: %d ms]",
                nodePartitionMap.size(), startTimeMs, healTimeMs);
    }
}
