package sim.network;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.*;

/**
 * Barabási–Albert Scale-Free Topology Strategy.
 * Generates scale-free networks using preferential attachment (rich-get-richer).
 * Accurately models real-world P2P blockchain topologies (Bitcoin, Ethereum) where super-nodes/hubs emerge.
 * Exhibits power-law degree distribution P(k) ~ k^-3, ultra-low diameter, and high degree inequality (high Gini).
 */
public class BarabasiAlbertTopology implements TopologyStrategy {

    private final int m0; // initial connected core size
    private final int m;  // edges to attach from each new node
    private final Random random;

    public BarabasiAlbertTopology() {
        this(4, 3, 42L);
    }

    public BarabasiAlbertTopology(int m0, int m, long seed) {
        this.m0 = Math.max(2, m0);
        this.m = Math.max(1, Math.min(m, m0));
        this.random = new Random(seed);
    }

    @Override
    public Graph<Integer, DefaultWeightedEdge> createGraph(int numNodes, Map<String, Object> params) {
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        for (int i = 0; i < numNodes; i++) {
            graph.addVertex(i);
        }

        if (numNodes < 2) return graph;

        int initialNodes = Math.min(numNodes, Math.max(m0, m + 1));

        // Connect initial nodes in a complete or ring topology
        for (int i = 0; i < initialNodes; i++) {
            for (int j = i + 1; j < initialNodes; j++) {
                DefaultWeightedEdge edge = graph.addEdge(i, j);
                if (edge != null) {
                    graph.setEdgeWeight(edge, 1.0);
                }
            }
        }

        // Maintain repeated degree array for O(1) preferential sampling
        List<Integer> degreeList = new ArrayList<>();
        for (int i = 0; i < initialNodes; i++) {
            int deg = graph.degreeOf(i);
            for (int d = 0; d < deg; d++) {
                degreeList.add(i);
            }
        }

        // Add remaining nodes one by one with preferential attachment
        for (int i = initialNodes; i < numNodes; i++) {
            Set<Integer> targets = new HashSet<>();
            int edgesToAttach = Math.min(m, i);

            // Sample targets with probability proportional to degree
            int attempts = 0;
            while (targets.size() < edgesToAttach && attempts < 1000) {
                attempts++;
                if (!degreeList.isEmpty()) {
                    int chosen = degreeList.get(random.nextInt(degreeList.size()));
                    targets.add(chosen);
                } else {
                    targets.add(random.nextInt(i));
                }
            }

            // Fallback if needed
            int fallback = 0;
            while (targets.size() < edgesToAttach && fallback < i) {
                targets.add(fallback++);
            }

            // Add edges
            for (int target : targets) {
                DefaultWeightedEdge edge = graph.addEdge(i, target);
                if (edge != null) {
                    graph.setEdgeWeight(edge, 1.0);
                    degreeList.add(i);
                    degreeList.add(target);
                }
            }
        }

        return graph;
    }

    @Override
    public Map<Integer, double[]> generateCoordinates(Graph<Integer, DefaultWeightedEdge> graph, int numNodes) {
        Map<Integer, double[]> coords = new HashMap<>();

        // Place high-degree hubs closer to the center, low-degree nodes on outer shells
        int maxDegree = 1;
        for (int v : graph.vertexSet()) {
            maxDegree = Math.max(maxDegree, graph.degreeOf(v));
        }

        Random layoutRand = new Random(numNodes);
        for (int i = 0; i < numNodes; i++) {
            int deg = graph.degreeOf(i);
            // Higher degree -> smaller radius (closer to center)
            double degreeRatio = (double) deg / maxDegree;
            double radius = 10.0 + (1.0 - Math.pow(degreeRatio, 0.5)) * 34.0;
            double angle = 2.0 * Math.PI * i / numNodes + (layoutRand.nextDouble() - 0.5) * 0.2;

            double x = 50.0 + radius * Math.cos(angle);
            double y = 50.0 + radius * Math.sin(angle);
            coords.put(i, new double[]{x, y});
        }

        return coords;
    }

    @Override
    public String getName() {
        return "Barabási–Albert Scale-Free";
    }
}
