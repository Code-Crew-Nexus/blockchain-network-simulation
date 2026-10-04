package sim;

import sim.blockchain.Transaction;
import sim.engine.EventType;
import sim.engine.SimulationEngine;
import sim.experiments.DecentralizationExperiment;
import sim.experiments.FaultToleranceExperiment;
import sim.experiments.PropagationExperiment;
import sim.faults.FaultInjector;
import sim.metrics.MetricsCollector;
import sim.metrics.SimulationReport;
import sim.network.*;
import sim.visualize.ChartGenerator;

import java.io.File;
import java.util.*;

/**
 * Main Command-Line Interface (CLI) entry point.
 * Supports batch academic experiment execution, automated artifact generation,
 * and parameterized custom blockchain network simulations.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=========================================================================");
        System.out.println(" BLOCKCHAIN NETWORK SIMULATION: Decentralization, Propagation, & Faults ");
        System.out.println(" A Discrete-Event Study for Computer Networks Evaluation");
        System.out.println("=========================================================================\n");

        String outputDir = "output";
        new File(outputDir).mkdirs();

        if (args == null || args.length == 0 || containsArg(args, "--all")) {
            System.out.println("[*] Executing ALL benchmark experiments sequentially...\n");
            PropagationExperiment.runExperiment(outputDir);
            FaultToleranceExperiment.runExperiment(outputDir);
            DecentralizationExperiment.runExperiment(outputDir);
            System.out.println("\n[✓] ALL EXPERIMENTS COMPLETED SUCCESSFULLY!");
            System.out.println("[*] High-resolution charts and JSON reports generated in: /" + outputDir);
            return;
        }

        if (containsArg(args, "--help") || containsArg(args, "-h")) {
            printHelp();
            return;
        }

        String experiment = getArgValue(args, "--experiment");
        if (experiment != null) {
            switch (experiment.toLowerCase()) {
                case "propagation":
                    PropagationExperiment.runExperiment(outputDir);
                    break;
                case "fault":
                case "faulttolerance":
                    FaultToleranceExperiment.runExperiment(outputDir);
                    break;
                case "decentralization":
                case "gini":
                    DecentralizationExperiment.runExperiment(outputDir);
                    break;
                default:
                    System.err.println("Unknown experiment: " + experiment);
                    printHelp();
            }
            return;
        }

        // Custom simulation execution via CLI flags
        runCustomSimulation(args, outputDir);
    }

    private static void runCustomSimulation(String[] args, String outputDir) {
        int nodes = parseInt(getArgValue(args, "--nodes"), 100);
        String topoType = getArgValue(args, "--topology", "scale_free");
        int difficulty = parseInt(getArgValue(args, "--difficulty"), 2);
        int fanout = parseInt(getArgValue(args, "--fanout"), 6);
        String fault = getArgValue(args, "--fault", "none");
        double faultRate = parseDouble(getArgValue(args, "--fault-rate"), 0.2);

        System.out.printf("[*] Running Custom Simulation: Nodes=%d, Topology=%s, Difficulty=%d, Fanout=%d, Fault=%s\n",
                nodes, topoType, difficulty, fanout, fault);

        long seed = 42L;
        TopologyStrategy strategy;
        switch (topoType.toLowerCase()) {
            case "ring":
                strategy = new RingTopology(4);
                break;
            case "erdos":
            case "random":
                strategy = new ErdosRenyiTopology(0.08, seed);
                break;
            case "scale_free":
            case "barabasi":
            default:
                strategy = new BarabasiAlbertTopology(4, 3, seed);
                break;
        }

        NetworkTopology topology = NetworkTopology.create(strategy, nodes, Collections.emptyMap());
        LatencyModel latencyModel = new LatencyModel(LatencyModel.Mode.DISTANCE_BASED, 20.0, 10.0, 10_000_000.0, seed);
        SimulationEngine engine = new SimulationEngine(topology, latencyModel, difficulty, fanout, seed);

        MetricsCollector collector = new MetricsCollector();
        collector.attachToEngine(engine);

        FaultInjector injector = new FaultInjector(seed);
        if ("crash".equalsIgnoreCase(fault)) {
            injector.injectNodeCrashes(engine, faultRate, 50L);
            System.out.printf("[!] Injected node crashes on %.0f%% of nodes.\n", faultRate * 100);
        } else if ("partition".equalsIgnoreCase(fault)) {
            injector.injectNetworkPartition(engine, 100L, 2000L);
            System.out.println("[!] Injected network partition between T=100ms and T=2000ms.");
        } else if ("byzantine".equalsIgnoreCase(fault)) {
            injector.injectByzantineNodes(engine, faultRate);
            System.out.printf("[!] Injected Byzantine behavior on %.0f%% of nodes.\n", faultRate * 100);
        }

        // Transactions & Mining
        for (int i = 0; i < 5; i++) {
            Transaction tx = new Transaction("Node-0", "Node-" + (i + 1), 5.0, 0.01, 10L);
            engine.scheduleAfter(10, EventType.TX_CREATED, 0, -1, tx);
        }
        engine.scheduleAfter(20, EventType.MINE_TICK, 0, -1, null);

        // Run until completion
        engine.runUntil(15_000);

        SimulationReport report = collector.generateReport("Custom_Sim_" + topoType, engine);
        try {
            collector.saveReportToJson(report, outputDir + "/custom_simulation_report.json");
            var topoChart = ChartGenerator.createTopologyChart(topology, "Custom Topology: " + topoType);
            ChartGenerator.saveChartAsPng(topoChart, outputDir + "/custom_topology.png");
            System.out.println("[✓] Custom simulation complete. Report saved to: " + outputDir + "/custom_simulation_report.json");
        } catch (Exception e) {
            System.err.println("Error saving artifacts: " + e.getMessage());
        }
    }

    private static boolean containsArg(String[] args, String target) {
        for (String a : args) {
            if (a.equalsIgnoreCase(target)) return true;
        }
        return false;
    }

    private static String getArgValue(String[] args, String key) {
        return getArgValue(args, key, null);
    }

    private static String getArgValue(String[] args, String key, String defaultVal) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equalsIgnoreCase(key)) {
                return args[i + 1];
            }
        }
        return defaultVal;
    }

    private static int parseInt(String val, int defaultVal) {
        try {
            return val != null ? Integer.parseInt(val) : defaultVal;
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private static double parseDouble(String val, double defaultVal) {
        try {
            return val != null ? Double.parseDouble(val) : defaultVal;
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private static void printHelp() {
        System.out.println("USAGE:");
        System.out.println("  mvn exec:java -Dexec.mainClass=\"sim.Main\" [-Dexec.args=\"<OPTIONS>\"]");
        System.out.println("\nOPTIONS:");
        System.out.println("  --all                       Run all three core academic experiments (Default)");
        System.out.println("  --experiment <NAME>         Run specific experiment (propagation | fault | decentralization)");
        System.out.println("  --nodes <INT>               Number of nodes (e.g. 50, 100, 300) [Default: 100]");
        System.out.println("  --topology <TYPE>           Topology type: ring | erdos | scale_free [Default: scale_free]");
        System.out.println("  --difficulty <INT>          PoW mining difficulty leading zeroes [Default: 2]");
        System.out.println("  --fanout <INT>              Gossip relay fanout [Default: 6]");
        System.out.println("  --fault <TYPE>              Fault type: none | crash | partition | byzantine");
        System.out.println("  --fault-rate <FLOAT>        Fault rate fraction (e.g. 0.2 for 20%) [Default: 0.2]");
        System.out.println("  --help                      Display this help manual\n");
    }
}
