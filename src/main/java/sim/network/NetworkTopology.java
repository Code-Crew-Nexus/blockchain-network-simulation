package sim.network;

import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.*;

/**
 * Encapsulates the underlying JGraphT peer-to-peer network graph, 2D coordinates,
 * and comprehensive network metrics (Gini coefficient, path length, degree distribution).
 */
public class NetworkTopology {

    private final Graph<Integer, DefaultWeightedEdge> graph;
    private final TopologyStrategy strategy;
    private final Map<Integer, double[]> coordinates;
    private final int numNodes;

    public NetworkTopology(Graph<Integer, DefaultWeightedEdge> graph,
                           TopologyStrategy strategy,
                           Map<Integer, double[]> coordinates) {
        this.graph = graph;
        this.strategy = strategy;
        this.numNodes = graph.vertexSet().size();
        this.coordinates = coordinates != null ? coordinates : new HashMap<>();
    }

    /**
     * Factory constructor using a given topology strategy.
     */
    public static NetworkTopology create(TopologyStrategy strategy, int numNodes, Map<String, Object> params) {
        Graph<Integer, DefaultWeightedEdge> g = strategy.createGraph(numNodes, params);
        Map<Integer, double[]> coords = strategy.generateCoordinates(g, numNodes);
        return new NetworkTopology(g, strategy, coords);
    }

    public Graph<Integer, DefaultWeightedEdge> getGraph() {
        return graph;
    }

    public TopologyStrategy getStrategy() {
        return strategy;
    }

    public String getTopologyName() {
        return strategy != null ? strategy.getName() : "Custom Graph";
    }

    public int getNumNodes() {
        return numNodes;
    }

    public int getNumEdges() {
        return graph.edgeSet().size();
    }

    public Set<Integer> getNeighbors(int nodeId) {
        Set<Integer> neighbors = new HashSet<>();
        if (!graph.containsVertex(nodeId)) return neighbors;

        for (DefaultWeightedEdge edge : graph.edgesOf(nodeId)) {
            Integer source = graph.getEdgeSource(edge);
            Integer target = graph.getEdgeTarget(edge);
            neighbors.add(source.equals(nodeId) ? target : source);
        }
        return neighbors;
    }

    public int getDegree(int nodeId) {
        if (!graph.containsVertex(nodeId)) return 0;
        return graph.degreeOf(nodeId);
    }

    public double getAverageDegree() {
        if (numNodes == 0) return 0.0;
        return (2.0 * getNumEdges()) / numNodes;
    }

    public double[] getCoordinates(int nodeId) {
        return coordinates.getOrDefault(nodeId, new double[]{50.0, 50.0});
    }

    public Map<Integer, double[]> getAllCoordinates() {
        return Collections.unmodifiableMap(coordinates);
    }

    /**
     * Calculates the degree distribution (frequency count of each degree value).
     */
    public Map<Integer, Integer> getDegreeDistribution() {
        Map<Integer, Integer> distribution = new TreeMap<>();
        for (int v : graph.vertexSet()) {
            int deg = graph.degreeOf(v);
            distribution.put(deg, distribution.getOrDefault(deg, 0) + 1);
        }
        return distribution;
    }

    /**
     * Computes the Gini Coefficient of Node Degree:
     * G = [ 2 * sum_{i=1}^n (i * d_(i)) ] / [ n * sum_{i=1}^n d_i ] - (n + 1) / n
     * Quantifies structural decentralization:
     * 0.0 = perfect equality (e.g. Ring / k-regular graph)
     * 1.0 = extreme inequality / centralization (e.g. Star graph)
     */
    public double calculateGiniCoefficient() {
        if (numNodes <= 1) return 0.0;

        List<Integer> degrees = new ArrayList<>();
        long totalDegree = 0;
        for (int v : graph.vertexSet()) {
            int deg = graph.degreeOf(v);
            degrees.add(deg);
            totalDegree += deg;
        }

        if (totalDegree == 0) return 0.0;

        Collections.sort(degrees);

        double weightedSum = 0;
        for (int i = 0; i < degrees.size(); i++) {
            // i is 0-indexed, rank is (i + 1)
            weightedSum += (i + 1.0) * degrees.get(i);
        }

        double n = degrees.size();
        double gini = (2.0 * weightedSum) / (n * totalDegree) - (n + 1.0) / n;
        return Math.max(0.0, Math.min(1.0, gini));
    }

    /**
     * Calculates the average shortest path length across all node pairs.
     * Samples for larger networks to guarantee high performance.
     */
    public double calculateAveragePathLength() {
        if (numNodes <= 1) return 0.0;
        DijkstraShortestPath<Integer, DefaultWeightedEdge> dijkstra = new DijkstraShortestPath<>(graph);

        double totalDistance = 0.0;
        long pathCount = 0;

        // Sample up to 100 source nodes for speed in large networks
        List<Integer> vertices = new ArrayList<>(graph.vertexSet());
        int sampleSize = Math.min(vertices.size(), 100);
        Collections.shuffle(vertices, new Random(42));

        for (int i = 0; i < sampleSize; i++) {
            int src = vertices.get(i);
            var paths = dijkstra.getPaths(src);
            for (int dst : graph.vertexSet()) {
                if (src != dst) {
                    var path = paths.getPath(dst);
                    if (path != null) {
                        totalDistance += path.getLength();
                        pathCount++;
                    }
                }
            }
        }

        return pathCount > 0 ? (totalDistance / pathCount) : 0.0;
    }

    /**
     * Calculates network diameter (longest shortest path).
     */
    public int calculateDiameter() {
        if (numNodes <= 1) return 0;
        DijkstraShortestPath<Integer, DefaultWeightedEdge> dijkstra = new DijkstraShortestPath<>(graph);

        double maxDist = 0;
        List<Integer> vertices = new ArrayList<>(graph.vertexSet());
        int sampleSize = Math.min(vertices.size(), 100);

        for (int i = 0; i < sampleSize; i++) {
            int src = vertices.get(i);
            var paths = dijkstra.getPaths(src);
            for (int dst : graph.vertexSet()) {
                if (src != dst) {
                    var path = paths.getPath(dst);
                    if (path != null) {
                        maxDist = Math.max(maxDist, path.getLength());
                    }
                }
            }
        }
        return (int) maxDist;
    }

    /**
     * Creates a deep copy of this topology.
     */
    public NetworkTopology copy() {
        Graph<Integer, DefaultWeightedEdge> gCopy = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        for (int v : graph.vertexSet()) {
            gCopy.addVertex(v);
        }
        for (DefaultWeightedEdge e : graph.edgeSet()) {
            Integer src = graph.getEdgeSource(e);
            Integer dst = graph.getEdgeTarget(e);
            DefaultWeightedEdge newEdge = gCopy.addEdge(src, dst);
            if (newEdge != null) {
                gCopy.setEdgeWeight(newEdge, graph.getEdgeWeight(e));
            }
        }
        Map<Integer, double[]> coordsCopy = new HashMap<>(this.coordinates);
        return new NetworkTopology(gCopy, this.strategy, coordsCopy);
    }
}
