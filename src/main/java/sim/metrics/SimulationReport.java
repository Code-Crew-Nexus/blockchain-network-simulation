package sim.metrics;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Structured report holding simulation results and metrics.
 * Designed for clean JSON serialization and academic data presentation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SimulationReport implements Serializable {
    private static final long serialVersionUID = 1L;

    private String experimentName;
    private long timestamp;
    private String topologyType;
    private int numNodes;
    private int numEdges;
    private double averageDegree;
    private double giniCoefficient;
    private int diameter;
    private double averagePathLength;

    // Propagation Metrics
    private double timeTo50PercentMs;
    private double timeTo90PercentMs;
    private double timeTo100PercentMs;
    private List<double[]> propagationCurve; // [timestampMs, percentReached]

    // Fault-Tolerance & Consensus Metrics
    private double crashFraction;
    private double partitionReconvergenceTimeMs;
    private double percentNodesOnCanonicalChain;
    private double percentNodesOnMinorityForkPreHeal;
    private int totalBlocksMined;
    private int orphanBlocksCount;
    private double orphanBlockRate;

    // Degree distribution
    private Map<Integer, Integer> degreeDistribution;

    public SimulationReport() {}

    // Getters and Setters
    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTopologyType() {
        return topologyType;
    }

    public void setTopologyType(String topologyType) {
        this.topologyType = topologyType;
    }

    public int getNumNodes() {
        return numNodes;
    }

    public void setNumNodes(int numNodes) {
        this.numNodes = numNodes;
    }

    public int getNumEdges() {
        return numEdges;
    }

    public void setNumEdges(int numEdges) {
        this.numEdges = numEdges;
    }

    public double getAverageDegree() {
        return averageDegree;
    }

    public void setAverageDegree(double averageDegree) {
        this.averageDegree = averageDegree;
    }

    public double getGiniCoefficient() {
        return giniCoefficient;
    }

    public void setGiniCoefficient(double giniCoefficient) {
        this.giniCoefficient = giniCoefficient;
    }

    public int getDiameter() {
        return diameter;
    }

    public void setDiameter(int diameter) {
        this.diameter = diameter;
    }

    public double getAveragePathLength() {
        return averagePathLength;
    }

    public void setAveragePathLength(double averagePathLength) {
        this.averagePathLength = averagePathLength;
    }

    public double getTimeTo50PercentMs() {
        return timeTo50PercentMs;
    }

    public void setTimeTo50PercentMs(double timeTo50PercentMs) {
        this.timeTo50PercentMs = timeTo50PercentMs;
    }

    public double getTimeTo90PercentMs() {
        return timeTo90PercentMs;
    }

    public void setTimeTo90PercentMs(double timeTo90PercentMs) {
        this.timeTo90PercentMs = timeTo90PercentMs;
    }

    public double getTimeTo100PercentMs() {
        return timeTo100PercentMs;
    }

    public void setTimeTo100PercentMs(double timeTo100PercentMs) {
        this.timeTo100PercentMs = timeTo100PercentMs;
    }

    public List<double[]> getPropagationCurve() {
        return propagationCurve;
    }

    public void setPropagationCurve(List<double[]> propagationCurve) {
        this.propagationCurve = propagationCurve;
    }

    public double getCrashFraction() {
        return crashFraction;
    }

    public void setCrashFraction(double crashFraction) {
        this.crashFraction = crashFraction;
    }

    public double getPartitionReconvergenceTimeMs() {
        return partitionReconvergenceTimeMs;
    }

    public void setPartitionReconvergenceTimeMs(double partitionReconvergenceTimeMs) {
        this.partitionReconvergenceTimeMs = partitionReconvergenceTimeMs;
    }

    public double getPercentNodesOnCanonicalChain() {
        return percentNodesOnCanonicalChain;
    }

    public void setPercentNodesOnCanonicalChain(double percentNodesOnCanonicalChain) {
        this.percentNodesOnCanonicalChain = percentNodesOnCanonicalChain;
    }

    public double getPercentNodesOnMinorityForkPreHeal() {
        return percentNodesOnMinorityForkPreHeal;
    }

    public void setPercentNodesOnMinorityForkPreHeal(double percentNodesOnMinorityForkPreHeal) {
        this.percentNodesOnMinorityForkPreHeal = percentNodesOnMinorityForkPreHeal;
    }

    public int getTotalBlocksMined() {
        return totalBlocksMined;
    }

    public void setTotalBlocksMined(int totalBlocksMined) {
        this.totalBlocksMined = totalBlocksMined;
    }

    public int getOrphanBlocksCount() {
        return orphanBlocksCount;
    }

    public void setOrphanBlocksCount(int orphanBlocksCount) {
        this.orphanBlocksCount = orphanBlocksCount;
    }

    public double getOrphanBlockRate() {
        return orphanBlockRate;
    }

    public void setOrphanBlockRate(double orphanBlockRate) {
        this.orphanBlockRate = orphanBlockRate;
    }

    public Map<Integer, Integer> getDegreeDistribution() {
        return degreeDistribution;
    }

    public void setDegreeDistribution(Map<Integer, Integer> degreeDistribution) {
        this.degreeDistribution = degreeDistribution;
    }
}
