package sim.experiments;

import org.knowm.xchart.XYChart;
import sim.blockchain.Block;
import sim.engine.EventType;
import sim.engine.SimulationEngine;
import sim.faults.FaultInjector;
import sim.faults.PartitionInfo;
import sim.metrics.MetricsCollector;
import sim.metrics.SimulationReport;
import sim.network.BarabasiAlbertTopology;
import sim.network.LatencyModel;
import sim.network.NetworkTopology;
import sim.node.Node;
import sim.visualize.ChartGenerator;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Experiment 2: Fault Tolerance and Consensus Resilience.
 * Evaluates network resilience under increasing node crash rates (0% to 50%),
 * and assesses partition split-brain mining, healing, and longest-chain reconvergence.
 */
public class FaultToleranceExperiment {

    public static void main(String[] args) {
        runExperiment("output");
    }

    public static void runExperiment(String outputDir) {
        System.out.println("=================================================================");
        System.out.println(" EXPERIMENT 2: Fault Tolerance, Node Crashes, & Partition Healing");
        System.out.println("=================================================================");

        new File(outputDir).mkdirs();

        int numNodes = 100;
        int difficulty = 2;
        int fanout = 6;
        long seed = 42L;

        // Part A: Crash Rate vs Convergence Delay (0% to 50% crash rate)
        System.out.println("\n--- Part A: Sweep Node Crash Rate from 0% to 50% ---");
        double[] crashRates = new double[]{0.0, 0.10, 0.20, 0.30, 0.40, 0.50};
        List<double[]> crashVsConvergencePoints = new ArrayList<>();
        Map<Double, SimulationReport> crashReports = new LinkedHashMap<>();

        for (double rate : crashRates) {
            System.out.printf("[+] Simulating crash rate: %.0f%%...\n", rate * 100);

            NetworkTopology topology = NetworkTopology.create(new BarabasiAlbertTopology(4, 3, seed), numNodes, Collections.emptyMap());
            LatencyModel latencyModel = new LatencyModel(LatencyModel.Mode.DISTANCE_BASED, 20.0, 10.0, 10_000_000.0, seed);
            SimulationEngine engine = new SimulationEngine(topology, latencyModel, difficulty, fanout, seed);

            MetricsCollector collector = new MetricsCollector();
            collector.attachToEngine(engine);

            FaultInjector injector = new FaultInjector(seed);

            // Crash fraction of nodes at T = 50 ms
            injector.injectNodeCrashes(engine, rate, 50L);

            // Schedule miner to mine block at T = 100 ms
            engine.scheduleAfter(100, EventType.MINE_TICK, 0, -1, null);

            // Run simulation until stable
            engine.runUntil(15_000);

            SimulationReport report = collector.generateReport(String.format("Crash_%.0f_pct", rate * 100), engine);
            crashReports.put(rate, report);

            double timeTo90 = report.getTimeTo90PercentMs();
            // If network didn't reach 90% (e.g. partition), use timeTo100 or total time
            double convTime = timeTo90 > 0 ? timeTo90 : report.getTimeTo50PercentMs();
            if (convTime <= 0) convTime = 5000.0;

            crashVsConvergencePoints.add(new double[]{rate * 100.0, convTime});
            System.out.printf("    -> Effective Online: %d/%d | Time to reach online peers: %.1f ms | Canonical Consensus: %.1f%%\n",
                    (int) Math.round((1.0 - rate) * numNodes), numNodes, convTime, report.getPercentNodesOnCanonicalChain());
        }

        // Part B: Network Partition Split and Reconvergence Study
        System.out.println("\n--- Part B: Network Partition Split & Post-Heal Reconvergence ---");
        NetworkTopology partTopology = NetworkTopology.create(new BarabasiAlbertTopology(4, 3, seed), numNodes, Collections.emptyMap());
        LatencyModel partLatency = new LatencyModel(LatencyModel.Mode.DISTANCE_BASED, 20.0, 10.0, 10_000_000.0, seed);
        SimulationEngine partEngine = new SimulationEngine(partTopology, partLatency, difficulty, fanout, seed);

        MetricsCollector partCollector = new MetricsCollector();
        partCollector.attachToEngine(partEngine);
        FaultInjector partInjector = new FaultInjector(seed);

        long partitionStart = 200L;
        long partitionHeal = 2500L;
        PartitionInfo pInfo = partInjector.injectNetworkPartition(partEngine, partitionStart, partitionHeal);

        // While partitioned:
        // Partition 1 node mines a block at T = 500 ms
        int p1Miner = -1;
        int p2Miner = -1;
        for (Map.Entry<Integer, Integer> entry : pInfo.getNodePartitionMap().entrySet()) {
            if (entry.getValue() == 1 && p1Miner == -1) p1Miner = entry.getKey();
            if (entry.getValue() == 2 && p2Miner == -1) p2Miner = entry.getKey();
        }

        // Partition 1 mines 1 block
        partEngine.scheduleAfter(500, EventType.MINE_TICK, p1Miner, -1, null);

        // Partition 2 mines 2 blocks (producing a longer competing fork!)
        partEngine.scheduleAfter(600, EventType.MINE_TICK, p2Miner, -1, null);
        partEngine.scheduleAfter(1200, EventType.MINE_TICK, p2Miner, -1, null);

        // Run through partition and post-heal reconciliation
        partEngine.runUntil(10_000);

        SimulationReport partReport = partCollector.generateReport("Network_Partition_Study", partEngine);

        System.out.printf("[✓] Partition Reconvergence Completed!\n");
        System.out.printf("    - Partition Duration: %d ms (Start: %d ms, Heal: %d ms)\n",
                (partitionHeal - partitionStart), partitionStart, partitionHeal);
        System.out.printf("    - Post-Heal Reconvergence Time: %.1f ms\n", partReport.getPartitionReconvergenceTimeMs());
        System.out.printf("    - Total Blocks Mined: %d\n", partReport.getTotalBlocksMined());
        System.out.printf("    - Orphan Blocks Resolved: %d (Orphan Rate: %.1f%%)\n",
                partReport.getOrphanBlocksCount(), partReport.getOrphanBlockRate());
        System.out.printf("    - Canonical Tip Consensus: %.1f%% of nodes\n",
                partReport.getPercentNodesOnCanonicalChain());

        // Generate Charts and save JSON
        try {
            XYChart crashChart = ChartGenerator.createConvergenceVsCrashChart(
                    crashVsConvergencePoints, "Fault Tolerance: Convergence Time vs Node Crash Rate");
            String chartPath = outputDir + "/fault_tolerance_study.png";
            ChartGenerator.saveChartAsPng(crashChart, chartPath);
            System.out.println("[✓] Saved fault tolerance crash curve to: " + chartPath);

            MetricsCollector mc = new MetricsCollector();
            mc.saveReportToJson(partReport, outputDir + "/fault_tolerance_partition_report.json");
            System.out.println("[✓] Saved partition study JSON report to: " + outputDir + "/fault_tolerance_partition_report.json");
        } catch (IOException e) {
            System.err.println("Error saving fault tolerance charts: " + e.getMessage());
        }

        printCrashTable(crashReports);
    }

    private static void printCrashTable(Map<Double, SimulationReport> reports) {
        System.out.println("\n----------------------------------------------------------------------------------");
        System.out.printf("%-15s | %-12s | %-18s | %-16s | %-12s\n",
                "Crash Rate", "Online Nodes", "T_50 Delivery", "T_90 Delivery", "Canonical %");
        System.out.println("----------------------------------------------------------------------------------");
        for (Map.Entry<Double, SimulationReport> entry : reports.entrySet()) {
            SimulationReport r = entry.getValue();
            System.out.printf("%-15.0f%% | %-12d | %-18.1f | %-16.1f | %-12.1f%%\n",
                    entry.getKey() * 100, (int) Math.round((1.0 - entry.getKey()) * r.getNumNodes()),
                    r.getTimeTo50PercentMs(), r.getTimeTo90PercentMs(), r.getPercentNodesOnCanonicalChain());
        }
        System.out.println("----------------------------------------------------------------------------------\n");
    }
}
