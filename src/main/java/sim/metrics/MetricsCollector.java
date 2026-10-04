package sim.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import sim.blockchain.Block;
import sim.blockchain.Chain;
import sim.engine.Event;
import sim.engine.EventType;
import sim.engine.SimulationEngine;
import sim.network.NetworkTopology;
import sim.node.Node;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Collects, analyzes, and exports simulation metrics including
 * decentralization (Gini coefficient), gossip propagation velocity, and fault tolerance.
 */
public class MetricsCollector {

    private final ObjectMapper objectMapper;

    // Tracking for block propagation
    private String trackedBlockHash;
    private long blockMinedTimestamp;
    private final Map<Integer, Long> nodeReceiveTimes;
    private final List<double[]> propagationTimeSeries; // [relativeTimeMs, percentReached]

    // Tracking for partition reconvergence
    private long partitionHealTimeMs;
    private long reconvergenceCompletedTimeMs;

    public MetricsCollector() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.nodeReceiveTimes = new HashMap<>();
        this.propagationTimeSeries = new ArrayList<>();
        this.trackedBlockHash = null;
        this.blockMinedTimestamp = 0L;
        this.partitionHealTimeMs = -1L;
        this.reconvergenceCompletedTimeMs = -1L;
    }

    /**
     * Attaches this collector to the simulation engine to listen for discrete events.
     */
    public void attachToEngine(SimulationEngine engine) {
        engine.addEventListener(this::onEvent);
    }

    private void onEvent(Event event) {
        if (event.getType() == EventType.BLOCK_MINED) {
            Block block = (Block) event.getPayload();
            // If we are not currently tracking a block or this is the target block
            if (trackedBlockHash == null) {
                trackedBlockHash = block.getHash();
                blockMinedTimestamp = event.getTimestampMs();
                nodeReceiveTimes.clear();
                nodeReceiveTimes.put(event.getSourceNodeId(), event.getTimestampMs());
                propagationTimeSeries.clear();
                propagationTimeSeries.add(new double[]{0.0, 0.0});
            }
        } else if (event.getType() == EventType.BLOCK_RECEIVED) {
            Block block = (Block) event.getPayload();
            if (trackedBlockHash != null && trackedBlockHash.equals(block.getHash())) {
                int receiverId = event.getTargetNodeId();
                if (!nodeReceiveTimes.containsKey(receiverId)) {
                    nodeReceiveTimes.put(receiverId, event.getTimestampMs());
                }
            }
        } else if (event.getType() == EventType.PARTITION_HEAL) {
            partitionHealTimeMs = event.getTimestampMs();
        }
    }

    public void setTrackedBlockHash(String hash, long minedTime) {
        this.trackedBlockHash = hash;
        this.blockMinedTimestamp = minedTime;
        this.nodeReceiveTimes.clear();
        this.propagationTimeSeries.clear();
    }

    /**
     * Records snapshot of current propagation curve based on received timestamps.
     */
    public List<double[]> computePropagationCurve(int totalOnlineNodes) {
        if (totalOnlineNodes <= 0 || nodeReceiveTimes.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map.Entry<Integer, Long>> sortedDeliveries = new ArrayList<>(nodeReceiveTimes.entrySet());
        sortedDeliveries.sort(Comparator.comparingLong(Map.Entry::getValue));

        List<double[]> curve = new ArrayList<>();
        curve.add(new double[]{0.0, (1.0 / totalOnlineNodes) * 100.0}); // Miner initially has it

        int count = 1;
        for (Map.Entry<Integer, Long> entry : sortedDeliveries) {
            long relativeTime = Math.max(0, entry.getValue() - blockMinedTimestamp);
            count++;
            double pct = Math.min(100.0, ((double) count / totalOnlineNodes) * 100.0);
            curve.add(new double[]{relativeTime, pct});
        }

        this.propagationTimeSeries.clear();
        this.propagationTimeSeries.addAll(curve);
        return curve;
    }

    /**
     * Calculates the time in milliseconds to reach specified target percentages (e.g. 50%, 90%, 100%).
     */
    public double calculateTimeToReachPercent(double targetPercent, int totalOnlineNodes) {
        List<double[]> curve = computePropagationCurve(totalOnlineNodes);
        for (double[] pt : curve) {
            if (pt[1] >= targetPercent) {
                return pt[0];
            }
        }
        return -1.0; // Not reached
    }

    /**
     * Generates a complete SimulationReport from the current engine state.
     */
    public SimulationReport generateReport(String experimentName, SimulationEngine engine) {
        SimulationReport report = new SimulationReport();
        report.setExperimentName(experimentName);
        report.setTimestamp(System.currentTimeMillis());

        NetworkTopology topo = engine.getTopology();
        if (topo != null) {
            report.setTopologyType(topo.getTopologyName());
            report.setNumNodes(topo.getNumNodes());
            report.setNumEdges(topo.getNumEdges());
            report.setAverageDegree(topo.getAverageDegree());
            report.setGiniCoefficient(topo.calculateGiniCoefficient());
            report.setDiameter(topo.calculateDiameter());
            report.setAveragePathLength(topo.calculateAveragePathLength());
            report.setDegreeDistribution(topo.getDegreeDistribution());
        }

        // Online node counting
        int onlineNodes = 0;
        int crashedNodes = 0;
        for (Node node : engine.getNodes().values()) {
            if (node.isOnline()) {
                onlineNodes++;
            } else {
                crashedNodes++;
            }
        }
        int totalNodes = engine.getNodes().size();
        report.setCrashFraction(totalNodes > 0 ? (double) crashedNodes / totalNodes : 0.0);

        // Propagation metrics
        List<double[]> curve = computePropagationCurve(onlineNodes);
        report.setPropagationCurve(curve);
        report.setTimeTo50PercentMs(calculateTimeToReachPercent(50.0, onlineNodes));
        report.setTimeTo90PercentMs(calculateTimeToReachPercent(90.0, onlineNodes));
        report.setTimeTo100PercentMs(calculateTimeToReachPercent(100.0, onlineNodes));

        // Consensus & canonical chain metrics
        Map<String, Integer> tipFrequency = new HashMap<>();
        Map<String, Block> tipMap = new HashMap<>();
        for (Node node : engine.getNodes().values()) {
            if (node.isOnline()) {
                Block tip = node.getLocalChain().getLatestBlock();
                if (tip != null) {
                    tipFrequency.put(tip.getHash(), tipFrequency.getOrDefault(tip.getHash(), 0) + 1);
                    tipMap.put(tip.getHash(), tip);
                }
            }
        }

        String canonicalTipHash = null;
        int maxTipAgree = 0;
        for (Map.Entry<String, Integer> entry : tipFrequency.entrySet()) {
            if (entry.getValue() > maxTipAgree) {
                maxTipAgree = entry.getValue();
                canonicalTipHash = entry.getKey();
            }
        }

        if (onlineNodes > 0) {
            report.setPercentNodesOnCanonicalChain(((double) maxTipAgree / onlineNodes) * 100.0);
        } else {
            report.setPercentNodesOnCanonicalChain(0.0);
        }

        // Total blocks vs Orphaned blocks
        Set<String> allKnownBlockHashes = new HashSet<>();
        Set<String> canonicalChainHashes = new HashSet<>();

        if (canonicalTipHash != null && tipMap.containsKey(canonicalTipHash)) {
            // Find a node that has the canonical tip
            for (Node n : engine.getNodes().values()) {
                if (n.getLocalChain().getLatestBlock() != null &&
                        n.getLocalChain().getLatestBlock().getHash().equals(canonicalTipHash)) {
                    for (Block b : n.getLocalChain().getBlocks()) {
                        canonicalChainHashes.add(b.getHash());
                    }
                    break;
                }
            }
        }

        for (Node n : engine.getNodes().values()) {
            allKnownBlockHashes.addAll(n.getKnownBlocks().keySet());
        }

        int totalMined = allKnownBlockHashes.size();
        int orphans = 0;
        for (String hash : allKnownBlockHashes) {
            if (!canonicalChainHashes.contains(hash)) {
                orphans++;
            }
        }

        report.setTotalBlocksMined(totalMined);
        report.setOrphanBlocksCount(orphans);
        report.setOrphanBlockRate(totalMined > 0 ? ((double) orphans / totalMined) * 100.0 : 0.0);

        // Partition reconvergence duration
        if (partitionHealTimeMs > 0) {
            if (reconvergenceCompletedTimeMs > 0) {
                report.setPartitionReconvergenceTimeMs(reconvergenceCompletedTimeMs - partitionHealTimeMs);
            } else {
                report.setPartitionReconvergenceTimeMs(Math.max(0, engine.getCurrentTimeMs() - partitionHealTimeMs));
            }
        }

        return report;
    }

    /**
     * Exports the simulation report to a JSON file.
     */
    public void saveReportToJson(SimulationReport report, String filePath) throws IOException {
        File file = new File(filePath);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        objectMapper.writeValue(file, report);
    }

    public String reportToJsonString(SimulationReport report) throws IOException {
        return objectMapper.writeValueAsString(report);
    }
}
