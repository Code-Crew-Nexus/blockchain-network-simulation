package sim.experiments;

import org.knowm.xchart.XYChart;
import sim.blockchain.Transaction;
import sim.engine.EventType;
import sim.engine.SimulationEngine;
import sim.metrics.MetricsCollector;
import sim.metrics.SimulationReport;
import sim.network.*;
import sim.visualize.ChartGenerator;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Experiment 1: Propagation Velocity Comparison.
 * Compares block propagation dynamics across Ring, Erdős–Rényi, and Barabási–Albert topologies.
 * Evaluates how network diameter and degree heterogeneity influence dissemination speed.
 */
public class PropagationExperiment {

    public static void main(String[] args) {
        runExperiment("output");
    }

    public static Map<String, SimulationReport> runExperiment(String outputDir) {
        System.out.println("=================================================================");
        System.out.println(" EXPERIMENT 1: Blockchain Block Propagation Velocity Across Topologies");
        System.out.println("=================================================================");

        new File(outputDir).mkdirs();

        int numNodes = 100;
        int difficulty = 2;
        int fanout = 6;
        long seed = 42L;

        TopologyStrategy[] strategies = new TopologyStrategy[]{
                new RingTopology(4),
                new ErdosRenyiTopology(0.08, seed),
                new BarabasiAlbertTopology(4, 3, seed)
        };

        Map<String, List<double[]>> comparativeCurves = new LinkedHashMap<>();
        Map<String, SimulationReport> reports = new LinkedHashMap<>();

        for (TopologyStrategy strategy : strategies) {
            String name = strategy.getName();
            System.out.printf("[+] Running simulation on topology: %s (N=%d)...\n", name, numNodes);

            NetworkTopology topology = NetworkTopology.create(strategy, numNodes, Collections.emptyMap());
            LatencyModel latencyModel = new LatencyModel(LatencyModel.Mode.DISTANCE_BASED, 20.0, 10.0, 10_000_000.0, seed);
            SimulationEngine engine = new SimulationEngine(topology, latencyModel, difficulty, fanout, seed);

            MetricsCollector collector = new MetricsCollector();
            collector.attachToEngine(engine);

            // Generate a topology visualization chart for this network
            try {
                String safeName = strategy.getClass().getSimpleName().toLowerCase();
                XYChart topoChart = ChartGenerator.createTopologyChart(topology, "Topology: " + name + " (N=" + numNodes + ")");
                ChartGenerator.saveChartAsPng(topoChart, outputDir + "/topology_" + safeName + ".png");
            } catch (IOException e) {
                System.err.println("Failed to save topology chart: " + e.getMessage());
            }

            // Populate some mempool transactions
            for (int i = 0; i < 5; i++) {
                Transaction tx = new Transaction("Node-0", "Node-" + (i + 1), 10.0 + i, 0.05, 5L);
                engine.scheduleAfter(5, EventType.TX_CREATED, 0, -1, tx);
            }

            // Schedule block mining at Node 0 at T = 10 ms
            engine.scheduleAfter(10, EventType.MINE_TICK, 0, -1, null);

            // Run simulation until block reaches all nodes or timeout
            long timeoutMs = 25_000;
            engine.runUntil(timeoutMs);

            SimulationReport report = collector.generateReport("Propagation - " + name, engine);
            reports.put(name, report);

            List<double[]> curve = report.getPropagationCurve();
            comparativeCurves.put(name, curve);

            System.out.printf("    - Avg Degree: %.2f | Gini: %.4f | Diameter: %d\n",
                    report.getAverageDegree(), report.getGiniCoefficient(), report.getDiameter());
            System.out.printf("    - T_50: %.1f ms | T_90: %.1f ms | T_100: %.1f ms\n",
                    report.getTimeTo50PercentMs(), report.getTimeTo90PercentMs(), report.getTimeTo100PercentMs());
        }

        // Generate comparative propagation curve plot
        try {
            XYChart comparisonChart = ChartGenerator.createPropagationCurveChart(
                    comparativeCurves, "Block Propagation Velocity (N=100, Fanout=" + fanout + ")");
            String chartPath = outputDir + "/propagation_comparison.png";
            ChartGenerator.saveChartAsPng(comparisonChart, chartPath);
            System.out.println("[✓] Saved propagation comparison chart to: " + chartPath);

            // Save JSON report
            MetricsCollector mc = new MetricsCollector();
            for (Map.Entry<String, SimulationReport> entry : reports.entrySet()) {
                String safeName = entry.getKey().replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
                mc.saveReportToJson(entry.getValue(), outputDir + "/propagation_report_" + safeName + ".json");
            }
            System.out.println("[✓] Saved propagation JSON reports to: " + outputDir);
        } catch (IOException e) {
            System.err.println("Error saving propagation artifacts: " + e.getMessage());
        }

        printSummaryTable(reports);
        return reports;
    }

    private static void printSummaryTable(Map<String, SimulationReport> reports) {
        System.out.println("\n-----------------------------------------------------------------------------------------");
        System.out.printf("%-26s | %-6s | %-6s | %-8s | %-8s | %-8s | %-8s\n",
                "Topology", "Gini", "Diam.", "AvgPath", "T_50 (ms)", "T_90 (ms)", "T_100 (ms)");
        System.out.println("-----------------------------------------------------------------------------------------");
        for (Map.Entry<String, SimulationReport> entry : reports.entrySet()) {
            SimulationReport r = entry.getValue();
            System.out.printf("%-26s | %-6.3f | %-6d | %-8.2f | %-8.1f | %-8.1f | %-8.1f\n",
                    entry.getKey(), r.getGiniCoefficient(), r.getDiameter(),
                    r.getAveragePathLength(), r.getTimeTo50PercentMs(),
                    r.getTimeTo90PercentMs(), r.getTimeTo100PercentMs());
        }
        System.out.println("-----------------------------------------------------------------------------------------\n");
    }
}
