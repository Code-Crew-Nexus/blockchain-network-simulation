package sim.experiments;

import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.XYChart;
import sim.metrics.MetricsCollector;
import sim.metrics.SimulationReport;
import sim.network.*;
import sim.visualize.ChartGenerator;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Experiment 3: Network Decentralization and Degree Inequality.
 * Evaluates structural decentralization via the Gini coefficient of node degree
 * across increasing network sizes (N = 20 to 500).
 */
public class DecentralizationExperiment {

    public static void main(String[] args) {
        runExperiment("output");
    }

    public static void runExperiment(String outputDir) {
        System.out.println("=================================================================");
        System.out.println(" EXPERIMENT 3: Decentralization & Degree Gini Coefficient vs N");
        System.out.println("=================================================================");

        new File(outputDir).mkdirs();

        int[] nodeSizes = new int[]{20, 50, 100, 200, 300, 400, 500};
        long seed = 42L;

        TopologyStrategy[] strategies = new TopologyStrategy[]{
                new RingTopology(4),
                new ErdosRenyiTopology(0.08, seed),
                new BarabasiAlbertTopology(4, 3, seed)
        };

        Map<String, List<double[]>> giniVsNMap = new LinkedHashMap<>();
        Map<String, Map<Integer, Double>> rawResults = new LinkedHashMap<>();

        for (TopologyStrategy strategy : strategies) {
            String name = strategy.getName();
            List<double[]> points = new ArrayList<>();
            Map<Integer, Double> nToGini = new LinkedHashMap<>();

            System.out.printf("[+] Evaluating topology: %s...\n", name);

            for (int n : nodeSizes) {
                NetworkTopology topo = NetworkTopology.create(strategy, n, Collections.emptyMap());
                double gini = topo.calculateGiniCoefficient();
                points.add(new double[]{(double) n, gini});
                nToGini.put(n, gini);
                System.out.printf("    - N = %3d | Gini: %.4f | Avg Degree: %.2f | Diameter: %d\n",
                        n, gini, topo.getAverageDegree(), topo.calculateDiameter());
            }

            giniVsNMap.put(name, points);
            rawResults.put(name, nToGini);
        }

        // Generate and save Gini vs N chart
        try {
            XYChart giniChart = ChartGenerator.createGiniVsNChart(
                    giniVsNMap, "Decentralization Analysis: Degree Gini Coefficient vs Network Size N");
            String chartPath = outputDir + "/decentralization_gini_vs_n.png";
            ChartGenerator.saveChartAsPng(giniChart, chartPath);
            System.out.println("[✓] Saved decentralization Gini vs N chart to: " + chartPath);

            // Also create a sample degree distribution histogram for N = 100 Barabasi-Albert
            NetworkTopology ba100 = NetworkTopology.create(new BarabasiAlbertTopology(4, 3, seed), 100, Collections.emptyMap());
            CategoryChart histChart = ChartGenerator.createDegreeDistributionChart(
                    ba100.getDegreeDistribution(), "Degree Distribution P(k) - Scale-Free Hub Network (N=100)");
            String histPath = outputDir + "/degree_distribution_barabasi_100.png";
            ChartGenerator.saveChartAsPng(histChart, histPath);
            System.out.println("[✓] Saved sample degree distribution histogram to: " + histPath);

            // Save JSON summary
            MetricsCollector mc = new MetricsCollector();
            SimulationReport report = new SimulationReport();
            report.setExperimentName("Decentralization_Gini_Study");
            report.setTimestamp(System.currentTimeMillis());
            report.setDegreeDistribution(ba100.getDegreeDistribution());
            mc.saveReportToJson(report, outputDir + "/decentralization_metrics.json");
            System.out.println("[✓] Saved decentralization metrics JSON to: " + outputDir + "/decentralization_metrics.json");
        } catch (IOException e) {
            System.err.println("Error saving decentralization charts: " + e.getMessage());
        }

        printGiniTable(nodeSizes, rawResults);
    }

    private static void printGiniTable(int[] nodeSizes, Map<String, Map<Integer, Double>> rawResults) {
        System.out.println("\n-----------------------------------------------------------------------------------------");
        System.out.printf("%-30s", "Topology");
        for (int n : nodeSizes) {
            System.out.printf(" | N=%-4d", n);
        }
        System.out.println("\n-----------------------------------------------------------------------------------------");
        for (Map.Entry<String, Map<Integer, Double>> entry : rawResults.entrySet()) {
            System.out.printf("%-30s", entry.getKey());
            for (int n : nodeSizes) {
                Double gini = entry.getValue().get(n);
                System.out.printf(" | %-6.4f", gini != null ? gini : 0.0);
            }
            System.out.println();
        }
        System.out.println("-----------------------------------------------------------------------------------------\n");
    }
}
