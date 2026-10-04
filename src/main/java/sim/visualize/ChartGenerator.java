package sim.visualize;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;
import sim.network.NetworkTopology;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Generates scientific, publication-ready charts and diagrams using XChart.
 * Renders topology scatter layouts, propagation velocity curves, degree histograms,
 * and fault tolerance / decentralization graphs.
 */
public class ChartGenerator {

    /**
     * Renders a 2D network topology diagram showing node coordinates and connecting edges.
     */
    public static XYChart createTopologyChart(NetworkTopology topology, String title) {
        XYChart chart = new XYChartBuilder()
                .width(800)
                .height(600)
                .title(title != null ? title : "Network Topology Visualization")
                .xAxisTitle("X Coordinate (Normalized)")
                .yAxisTitle("Y Coordinate (Normalized)")
                .theme(Styler.ChartTheme.Matlab)
                .build();

        chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideE);
        chart.getStyler().setMarkerSize(10);
        chart.getStyler().setXAxisMin(0.0);
        chart.getStyler().setXAxisMax(100.0);
        chart.getStyler().setYAxisMin(0.0);
        chart.getStyler().setYAxisMax(100.0);

        // Separate nodes into Hubs (degree >= 2x average) and Regular nodes
        double avgDegree = topology.getAverageDegree();
        List<Double> regularX = new ArrayList<>();
        List<Double> regularY = new ArrayList<>();
        List<Double> hubX = new ArrayList<>();
        List<Double> hubY = new ArrayList<>();

        for (int v = 0; v < topology.getNumNodes(); v++) {
            double[] coord = topology.getCoordinates(v);
            if (topology.getDegree(v) > avgDegree * 1.8 && avgDegree > 1.5) {
                hubX.add(coord[0]);
                hubY.add(coord[1]);
            } else {
                regularX.add(coord[0]);
                regularY.add(coord[1]);
            }
        }

        // Draw sample of edges (draw up to 400 edges to keep visual uncluttered)
        int edgeCount = 0;
        for (DefaultWeightedEdge e : topology.getGraph().edgeSet()) {
            if (edgeCount++ > 400) break;
            Integer u = topology.getGraph().getEdgeSource(e);
            Integer v = topology.getGraph().getEdgeTarget(e);
            double[] c1 = topology.getCoordinates(u);
            double[] c2 = topology.getCoordinates(v);

            var edgeSeries = chart.addSeries("e" + edgeCount,
                    new double[]{c1[0], c2[0]},
                    new double[]{c1[1], c2[1]});
            edgeSeries.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
            edgeSeries.setMarker(SeriesMarkers.NONE);
            edgeSeries.setLineColor(new Color(180, 195, 210, 80));
            edgeSeries.setShowInLegend(false);
        }

        if (!regularX.isEmpty()) {
            var nodeSeries = chart.addSeries("Regular Nodes (" + regularX.size() + ")", regularX, regularY);
            nodeSeries.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
            nodeSeries.setMarker(SeriesMarkers.CIRCLE);
            nodeSeries.setMarkerColor(new Color(41, 128, 185));
        }

        if (!hubX.isEmpty()) {
            var hubSeries = chart.addSeries("Super-Nodes / Hubs (" + hubX.size() + ")", hubX, hubY);
            hubSeries.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);
            hubSeries.setMarker(SeriesMarkers.DIAMOND);
            hubSeries.setMarkerColor(new Color(231, 76, 60));
        }

        return chart;
    }

    /**
     * Renders a block propagation curve: virtual time (ms) on X-axis vs % nodes reached on Y-axis.
     * Can plot multiple topology curves on the same chart for comparative analysis.
     *
     * @param curvesMap Map of Series Label -> List of [timeMs, percentReached]
     */
    public static XYChart createPropagationCurveChart(Map<String, List<double[]>> curvesMap, String title) {
        XYChart chart = new XYChartBuilder()
                .width(850)
                .height(550)
                .title(title != null ? title : "Blockchain Block Propagation Velocity")
                .xAxisTitle("Simulated Time (ms)")
                .yAxisTitle("% Online Nodes Reached")
                .theme(Styler.ChartTheme.Matlab)
                .build();

        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideSE);
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        chart.getStyler().setYAxisMin(0.0);
        chart.getStyler().setYAxisMax(105.0);
        chart.getStyler().setMarkerSize(5);

        for (Map.Entry<String, List<double[]>> entry : curvesMap.entrySet()) {
            List<double[]> points = entry.getValue();
            if (points == null || points.isEmpty()) continue;

            List<Double> xData = new ArrayList<>();
            List<Double> yData = new ArrayList<>();
            for (double[] pt : points) {
                xData.add(pt[0]);
                yData.add(pt[1]);
            }

            var series = chart.addSeries(entry.getKey(), xData, yData);
            series.setMarker(SeriesMarkers.CIRCLE);
        }

        return chart;
    }

    /**
     * Renders a degree distribution histogram / bar chart.
     */
    public static CategoryChart createDegreeDistributionChart(Map<Integer, Integer> distribution, String title) {
        CategoryChart chart = new CategoryChartBuilder()
                .width(800)
                .height(500)
                .title(title != null ? title : "Node Degree Distribution P(k)")
                .xAxisTitle("Node Degree (k)")
                .yAxisTitle("Number of Nodes")
                .theme(Styler.ChartTheme.Matlab)
                .build();

        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
        chart.getStyler().setAvailableSpaceFill(0.85);

        List<Integer> degrees = new ArrayList<>(distribution.keySet());
        Collections.sort(degrees);
        List<String> xLabels = new ArrayList<>();
        List<Number> yValues = new ArrayList<>();

        for (int deg : degrees) {
            xLabels.add(String.valueOf(deg));
            yValues.add(distribution.get(deg));
        }

        if (xLabels.isEmpty()) {
            xLabels.add("0");
            yValues.add(0);
        }

        chart.addSeries("Degree Frequency", xLabels, yValues);
        return chart;
    }

    /**
     * Renders a chart of Gini coefficient vs Network size N for multiple topologies.
     */
    public static XYChart createGiniVsNChart(Map<String, List<double[]>> giniDataMap, String title) {
        XYChart chart = new XYChartBuilder()
                .width(850)
                .height(550)
                .title(title != null ? title : "Decentralization: Degree Gini Coefficient vs Network Size (N)")
                .xAxisTitle("Network Size N (Number of Nodes)")
                .yAxisTitle("Gini Coefficient (0 = Perfect Equality, 1 = Extreme Inequality)")
                .theme(Styler.ChartTheme.Matlab)
                .build();

        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        chart.getStyler().setYAxisMin(0.0);
        chart.getStyler().setYAxisMax(1.0);
        chart.getStyler().setMarkerSize(8);

        for (Map.Entry<String, List<double[]>> entry : giniDataMap.entrySet()) {
            List<double[]> points = entry.getValue();
            if (points == null || points.isEmpty()) continue;

            List<Double> xData = new ArrayList<>();
            List<Double> yData = new ArrayList<>();
            for (double[] pt : points) {
                xData.add(pt[0]);
                yData.add(pt[1]);
            }

            var series = chart.addSeries(entry.getKey(), xData, yData);
            series.setMarker(SeriesMarkers.DIAMOND);
        }

        return chart;
    }

    /**
     * Renders a convergence time vs crash rate chart.
     */
    public static XYChart createConvergenceVsCrashChart(List<double[]> crashVsConvergence, String title) {
        XYChart chart = new XYChartBuilder()
                .width(850)
                .height(550)
                .title(title != null ? title : "Consensus Resilience: Convergence Time vs Node Crash Rate")
                .xAxisTitle("Node Crash Rate (% of Total Nodes Offline)")
                .yAxisTitle("Network Convergence Time (ms)")
                .theme(Styler.ChartTheme.Matlab)
                .build();

        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        chart.getStyler().setMarkerSize(8);

        List<Double> xData = new ArrayList<>();
        List<Double> yData = new ArrayList<>();
        for (double[] pt : crashVsConvergence) {
            xData.add(pt[0]);
            yData.add(pt[1]);
        }

        var series = chart.addSeries("Convergence Delay", xData, yData);
        series.setMarker(SeriesMarkers.SQUARE);
        series.setLineColor(new Color(192, 57, 43));
        series.setMarkerColor(new Color(192, 57, 43));

        return chart;
    }

    /**
     * Saves any XChart to a high-resolution PNG file.
     */
    public static void saveChartAsPng(org.knowm.xchart.internal.chartpart.Chart<?, ?> chart, String filePath) throws IOException {
        File file = new File(filePath);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        BitmapEncoder.saveBitmap(chart, filePath.replace(".png", ""), BitmapEncoder.BitmapFormat.PNG);
    }

    /**
     * Converts an XChart to an in-memory BufferedImage for GUI embedding.
     */
    public static BufferedImage getBufferedImage(org.knowm.xchart.internal.chartpart.Chart<?, ?> chart) {
        return BitmapEncoder.getBufferedImage(chart);
    }
}
