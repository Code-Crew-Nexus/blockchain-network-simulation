package sim.dashboard;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.XYChart;
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

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

/**
 * Interactive JavaFX Desktop Dashboard for the Blockchain Network Simulation.
 * Allows configuring simulation parameters, executing live experiments in background threads,
 * and visualizing network topologies, propagation curves, degree distributions, and JSON logs.
 */
public class DashboardApp extends Application {

    // Controls
    private Slider nodesSlider;
    private Label nodesLabel;
    private ComboBox<String> topologyCombo;
    private Slider difficultySlider;
    private Label difficultyLabel;
    private Slider fanoutSlider;
    private Label fanoutLabel;
    private ComboBox<String> faultCombo;
    private Slider faultRateSlider;
    private Label faultRateLabel;

    private Button runSimButton;
    private Button runExperimentsButton;
    private ProgressBar progressBar;
    private Label statusLabel;

    // View Tabs
    private ImageView topologyImageView;
    private ImageView propagationImageView;
    private ImageView faultImageView;
    private ImageView degreeImageView;
    private TextArea logTextArea;
    private Label giniMetricLabel;
    private Label diameterMetricLabel;
    private Label t90MetricLabel;
    private Label canonicalMetricLabel;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Blockchain Network Simulation: Decentralization, Propagation & Fault Tolerance");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f4f6f9;");

        // Top Header
        VBox headerBox = createHeader();
        root.setTop(headerBox);

        // Left Control Panel
        VBox controlPanel = createControlPanel();
        root.setLeft(controlPanel);

        // Center Content Tabs
        TabPane tabPane = createTabPane();
        root.setCenter(tabPane);

        // Bottom Status Bar
        HBox statusBar = createStatusBar();
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 1280, 820);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Initial default run on launch
        Platform.runLater(this::executeSingleSimulation);
    }

    private VBox createHeader() {
        VBox box = new VBox(4);
        box.setPadding(new Insets(14, 20, 14, 20));
        box.setStyle("-fx-background-color: #1a252f;");

        Label title = new Label("Blockchain Network Decentralization & Fault Tolerance Simulator");
        title.setStyle("-fx-text-fill: #ecf0f1; -fx-font-size: 19px; -fx-font-weight: bold;");

        Label subtitle = new Label("Discrete-Event P2P Consensus Simulator | JGraphT & XChart Analytics Engine");
        subtitle.setStyle("-fx-text-fill: #bdc3c7; -fx-font-size: 12px;");

        box.getChildren().addAll(title, subtitle);
        return box;
    }

    private VBox createControlPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(16));
        panel.setPrefWidth(320);
        panel.setStyle("-fx-background-color: #ffffff; -fx-border-color: #dcdde1; -fx-border-width: 0 1 0 0;");

        Label settingsHeader = new Label("Simulation Parameters");
        settingsHeader.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Nodes Slider
        nodesSlider = new Slider(20, 500, 100);
        nodesSlider.setBlockIncrement(20);
        nodesLabel = new Label("Nodes (N): 100");
        nodesSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                nodesLabel.setText(String.format("Nodes (N): %d", newVal.intValue())));

        // Topology Selection
        Label topoTitle = new Label("Network Topology:");
        topologyCombo = new ComboBox<>();
        topologyCombo.getItems().addAll("Barabási–Albert (Scale-Free)", "Erdős–Rényi (Random)", "Ring Lattice (k=4)");
        topologyCombo.setValue("Barabási–Albert (Scale-Free)");
        topologyCombo.setMaxWidth(Double.MAX_VALUE);

        // PoW Difficulty
        difficultySlider = new Slider(1, 4, 2);
        difficultySlider.setMajorTickUnit(1);
        difficultySlider.setSnapToTicks(true);
        difficultyLabel = new Label("PoW Difficulty: 2 leading 0s");
        difficultySlider.valueProperty().addListener((obs, oldVal, newVal) ->
                difficultyLabel.setText(String.format("PoW Difficulty: %d leading 0s", newVal.intValue())));

        // Gossip Fanout
        fanoutSlider = new Slider(2, 16, 6);
        fanoutLabel = new Label("Gossip Fanout: 6 peers");
        fanoutSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                fanoutLabel.setText(String.format("Gossip Fanout: %d peers", newVal.intValue())));

        // Fault Injection
        Label faultTitle = new Label("Fault Injection:");
        faultCombo = new ComboBox<>();
        faultCombo.getItems().addAll("None (Normal Operation)", "Node Crash Faults", "Network Partition & Heal", "Byzantine Adversaries");
        faultCombo.setValue("None (Normal Operation)");
        faultCombo.setMaxWidth(Double.MAX_VALUE);

        faultRateSlider = new Slider(0.05, 0.50, 0.20);
        faultRateLabel = new Label("Fault Intensity: 20%");
        faultRateSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                faultRateLabel.setText(String.format("Fault Intensity: %.0f%%", newVal.doubleValue() * 100)));

        // Action Buttons
        runSimButton = new Button("▶  Run Simulation");
        runSimButton.setMaxWidth(Double.MAX_VALUE);
        runSimButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px;");
        runSimButton.setOnAction(e -> executeSingleSimulation());

        runExperimentsButton = new Button("⚡  Run All 3 Experiments");
        runExperimentsButton.setMaxWidth(Double.MAX_VALUE);
        runExperimentsButton.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px;");
        runExperimentsButton.setOnAction(e -> executeAllExperiments());

        // Quick Metric Highlights Card
        VBox metricCard = createMetricCard();

        panel.getChildren().addAll(
                settingsHeader,
                new Separator(),
                nodesLabel, nodesSlider,
                topoTitle, topologyCombo,
                difficultyLabel, difficultySlider,
                fanoutLabel, fanoutSlider,
                faultTitle, faultCombo,
                faultRateLabel, faultRateSlider,
                new Separator(),
                runSimButton,
                runExperimentsButton,
                new Separator(),
                metricCard
        );

        ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return new VBox(scroll);
    }

    private VBox createMetricCard() {
        VBox card = new VBox(6);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e9ecef; -fx-border-radius: 4; -fx-background-radius: 4;");

        Label cardTitle = new Label("Live Metrics Snapshot");
        cardTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #34495e;");

        giniMetricLabel = new Label("Degree Gini: -");
        diameterMetricLabel = new Label("Network Diameter: -");
        t90MetricLabel = new Label("T_90 Propagation: -");
        canonicalMetricLabel = new Label("Canonical Consensus: -");

        card.getChildren().addAll(cardTitle, giniMetricLabel, diameterMetricLabel, t90MetricLabel, canonicalMetricLabel);
        return card;
    }

    private TabPane createTabPane() {
        TabPane tabPane = new TabPane();

        // Tab 1: Topology
        Tab topoTab = new Tab("Network Topology");
        topoTab.setClosable(false);
        topologyImageView = createImageView();
        topoTab.setContent(wrapInScrollPane(topologyImageView));

        // Tab 2: Propagation
        Tab propTab = new Tab("Propagation Dynamics");
        propTab.setClosable(false);
        propagationImageView = createImageView();
        propTab.setContent(wrapInScrollPane(propagationImageView));

        // Tab 3: Faults & Convergence
        Tab faultTab = new Tab("Fault Tolerance & Consensus");
        faultTab.setClosable(false);
        faultImageView = createImageView();
        faultTab.setContent(wrapInScrollPane(faultImageView));

        // Tab 4: Degree Distribution
        Tab degreeTab = new Tab("Decentralization & Degree P(k)");
        degreeTab.setClosable(false);
        degreeImageView = createImageView();
        degreeTab.setContent(wrapInScrollPane(degreeImageView));

        // Tab 5: JSON Logs
        Tab logTab = new Tab("Simulation Event Log & JSON");
        logTab.setClosable(false);
        logTextArea = new TextArea();
        logTextArea.setEditable(false);
        logTextArea.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px;");
        logTab.setContent(logTextArea);

        tabPane.getTabs().addAll(topoTab, propTab, faultTab, degreeTab, logTab);
        return tabPane;
    }

    private ImageView createImageView() {
        ImageView iv = new ImageView();
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        return iv;
    }

    private StackPane wrapInScrollPane(ImageView imageView) {
        StackPane pane = new StackPane(imageView);
        pane.setPadding(new Insets(16));
        pane.setAlignment(Pos.CENTER);
        return pane;
    }

    private HBox createStatusBar() {
        HBox bar = new HBox(12);
        bar.setPadding(new Insets(8, 16, 8, 16));
        bar.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-width: 1 0 0 0;");
        bar.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #2c3e50;");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(220);
        progressBar.setVisible(false);

        bar.getChildren().addAll(statusLabel, progressBar);
        return bar;
    }

    private void executeSingleSimulation() {
        setBusy(true, "Running simulation...");

        int nodes = (int) nodesSlider.getValue();
        String topoSelection = topologyCombo.getValue();
        int difficulty = (int) difficultySlider.getValue();
        int fanout = (int) fanoutSlider.getValue();
        String faultSelection = faultCombo.getValue();
        double faultRate = faultRateSlider.getValue();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                long seed = 42L;
                TopologyStrategy strategy;
                if (topoSelection.contains("Ring")) {
                    strategy = new RingTopology(4);
                } else if (topoSelection.contains("Erdős")) {
                    strategy = new ErdosRenyiTopology(0.08, seed);
                } else {
                    strategy = new BarabasiAlbertTopology(4, 3, seed);
                }

                NetworkTopology topology = NetworkTopology.create(strategy, nodes, Collections.emptyMap());
                LatencyModel latencyModel = new LatencyModel(LatencyModel.Mode.DISTANCE_BASED, 20.0, 10.0, 10_000_000.0, seed);
                SimulationEngine engine = new SimulationEngine(topology, latencyModel, difficulty, fanout, seed);

                MetricsCollector collector = new MetricsCollector();
                collector.attachToEngine(engine);

                FaultInjector injector = new FaultInjector(seed);
                if (faultSelection.contains("Crash")) {
                    injector.injectNodeCrashes(engine, faultRate, 50L);
                } else if (faultSelection.contains("Partition")) {
                    injector.injectNetworkPartition(engine, 100L, 2500L);
                } else if (faultSelection.contains("Byzantine")) {
                    injector.injectByzantineNodes(engine, faultRate);
                }

                // Inject Transactions
                for (int i = 0; i < 5; i++) {
                    Transaction tx = new Transaction("Node-0", "Node-" + (i + 1), 10.0, 0.05, 10L);
                    engine.scheduleAfter(10, EventType.TX_CREATED, 0, -1, tx);
                }

                // Mine block
                engine.scheduleAfter(20, EventType.MINE_TICK, 0, -1, null);

                // Run engine
                engine.runUntil(18_000);

                SimulationReport report = collector.generateReport("Interactive_Run", engine);

                // Build Charts
                XYChart topoChart = ChartGenerator.createTopologyChart(topology, "Topology: " + strategy.getName() + " (N=" + nodes + ")");
                BufferedImage topoImg = ChartGenerator.getBufferedImage(topoChart);

                Map<String, List<double[]>> curveMap = new HashMap<>();
                curveMap.put(strategy.getName(), report.getPropagationCurve());
                XYChart propChart = ChartGenerator.createPropagationCurveChart(curveMap, "Propagation Dynamics (N=" + nodes + ", Fanout=" + fanout + ")");
                BufferedImage propImg = ChartGenerator.getBufferedImage(propChart);

                CategoryChart degChart = ChartGenerator.createDegreeDistributionChart(topology.getDegreeDistribution(), "Degree Distribution P(k)");
                BufferedImage degImg = ChartGenerator.getBufferedImage(degChart);

                List<double[]> crashPoints = List.of(new double[]{faultRate * 100, Math.max(100.0, report.getTimeTo90PercentMs())});
                XYChart faultChart = ChartGenerator.createConvergenceVsCrashChart(crashPoints, "Consensus State");
                BufferedImage faultImg = ChartGenerator.getBufferedImage(faultChart);

                String jsonStr = collector.reportToJsonString(report);

                Platform.runLater(() -> {
                    topologyImageView.setImage(SwingFXUtils.toFXImage(topoImg, null));
                    propagationImageView.setImage(SwingFXUtils.toFXImage(propImg, null));
                    degreeImageView.setImage(SwingFXUtils.toFXImage(degImg, null));
                    faultImageView.setImage(SwingFXUtils.toFXImage(faultImg, null));

                    giniMetricLabel.setText(String.format("Degree Gini: %.4f", report.getGiniCoefficient()));
                    diameterMetricLabel.setText(String.format("Network Diameter: %d", report.getDiameter()));
                    t90MetricLabel.setText(String.format("T_90 Propagation: %.1f ms", report.getTimeTo90PercentMs()));
                    canonicalMetricLabel.setText(String.format("Canonical Consensus: %.1f%%", report.getPercentNodesOnCanonicalChain()));

                    logTextArea.setText(jsonStr);
                    setBusy(false, "Simulation completed successfully.");
                });

                return null;
            }
        };

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            Platform.runLater(() -> {
                setBusy(false, "Error: " + (ex != null ? ex.getMessage() : "Simulation failed"));
                if (ex != null) ex.printStackTrace();
            });
        });

        new Thread(task).start();
    }

    private void executeAllExperiments() {
        setBusy(true, "Executing all 3 benchmark experiments in background...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String outDir = "output";
                PropagationExperiment.runExperiment(outDir);
                FaultToleranceExperiment.runExperiment(outDir);
                DecentralizationExperiment.runExperiment(outDir);

                // Load generated comparison images
                File propFile = new File(outDir + "/propagation_comparison.png");
                File faultFile = new File(outDir + "/fault_tolerance_study.png");
                File giniFile = new File(outDir + "/decentralization_gini_vs_n.png");

                Platform.runLater(() -> {
                    if (propFile.exists()) {
                        propagationImageView.setImage(new Image(propFile.toURI().toString()));
                    }
                    if (faultFile.exists()) {
                        faultImageView.setImage(new Image(faultFile.toURI().toString()));
                    }
                    if (giniFile.exists()) {
                        degreeImageView.setImage(new Image(giniFile.toURI().toString()));
                    }
                    logTextArea.setText("ALL 3 BENCHMARK EXPERIMENTS COMPLETED SUCCESSFULLY!\nArtifacts generated in /output:\n- propagation_comparison.png\n- fault_tolerance_study.png\n- decentralization_gini_vs_n.png\n- JSON reports");
                    setBusy(false, "All 3 benchmark experiments completed!");
                });
                return null;
            }
        };

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            Platform.runLater(() -> {
                setBusy(false, "Experiment error: " + (ex != null ? ex.getMessage() : "Unknown"));
                if (ex != null) ex.printStackTrace();
            });
        });

        new Thread(task).start();
    }

    private void setBusy(boolean busy, String message) {
        runSimButton.setDisable(busy);
        runExperimentsButton.setDisable(busy);
        progressBar.setVisible(busy);
        statusLabel.setText(message);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
