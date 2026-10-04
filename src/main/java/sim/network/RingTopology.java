package sim.network;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.HashMap;
import java.util.Map;

/**
 * Ring Topology Strategy.
 * Generates a regular ring lattice where each node is connected to its k nearest circular neighbors.
 * Serves as a baseline benchmark with high diameter O(N) and perfect structural degree equality (Gini = 0).
 */
public class RingTopology implements TopologyStrategy {

    private final int kNeighbors; // e.g., 2 for simple ring, 4 for 2 neighbors on each side

    public RingTopology() {
        this(2);
    }

    public RingTopology(int kNeighbors) {
        this.kNeighbors = Math.max(2, (kNeighbors / 2) * 2); // ensure even
    }

    @Override
    public Graph<Integer, DefaultWeightedEdge> createGraph(int numNodes, Map<String, Object> params) {
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        for (int i = 0; i < numNodes; i++) {
            graph.addVertex(i);
        }

        if (numNodes < 2) return graph;

        int halfK = Math.min(kNeighbors / 2, (numNodes - 1) / 2);
        for (int i = 0; i < numNodes; i++) {
            for (int offset = 1; offset <= halfK; offset++) {
                int neighbor = (i + offset) % numNodes;
                if (!graph.containsEdge(i, neighbor)) {
                    DefaultWeightedEdge edge = graph.addEdge(i, neighbor);
                    if (edge != null) {
                        graph.setEdgeWeight(edge, 1.0);
                    }
                }
            }
        }

        return graph;
    }

    @Override
    public Map<Integer, double[]> generateCoordinates(Graph<Integer, DefaultWeightedEdge> graph, int numNodes) {
        Map<Integer, double[]> coords = new HashMap<>();
        double centerX = 50.0;
        double centerY = 50.0;
        double radius = 40.0;

        for (int i = 0; i < numNodes; i++) {
            double angle = 2.0 * Math.PI * i / numNodes;
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);
            coords.put(i, new double[]{x, y});
        }
        return coords;
    }

    @Override
    public String getName() {
        return "Ring Lattice (k=" + kNeighbors + ")";
    }
}
