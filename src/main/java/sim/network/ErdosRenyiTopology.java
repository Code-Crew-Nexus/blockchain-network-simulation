package sim.network;

import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.*;

/**
 * Erdős–Rényi Random Graph Strategy G(n, p).
 * Edge between any pair of nodes is generated with independent probability p.
 * Guarantees global network connectivity by bridging disconnected components.
 */
public class ErdosRenyiTopology implements TopologyStrategy {

    private final double edgeProbability;
    private final Random random;

    public ErdosRenyiTopology() {
        this(-1.0, 42L); // Auto-calculate threshold if negative
    }

    public ErdosRenyiTopology(double edgeProbability, long seed) {
        this.edgeProbability = edgeProbability;
        this.random = new Random(seed);
    }

    @Override
    public Graph<Integer, DefaultWeightedEdge> createGraph(int numNodes, Map<String, Object> params) {
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        for (int i = 0; i < numNodes; i++) {
            graph.addVertex(i);
        }

        if (numNodes < 2) return graph;

        // Calculate connection probability above the connectivity threshold ln(n)/n
        double p = edgeProbability;
        if (p <= 0.0) {
            p = Math.max(0.05, (2.5 * Math.log(numNodes)) / numNodes);
        }

        for (int i = 0; i < numNodes; i++) {
            for (int j = i + 1; j < numNodes; j++) {
                if (random.nextDouble() < p) {
                    DefaultWeightedEdge edge = graph.addEdge(i, j);
                    if (edge != null) {
                        graph.setEdgeWeight(edge, 1.0);
                    }
                }
            }
        }

        // Ensure graph is fully connected (crucial for P2P blockchain simulations)
        ConnectivityInspector<Integer, DefaultWeightedEdge> inspector = new ConnectivityInspector<>(graph);
        List<Set<Integer>> components = inspector.connectedSets();
        if (components.size() > 1) {
            Set<Integer> mainComponent = components.get(0);
            for (int c = 1; c < components.size(); c++) {
                Set<Integer> otherComponent = components.get(c);
                int u = new ArrayList<>(mainComponent).get(random.nextInt(mainComponent.size()));
                int v = new ArrayList<>(otherComponent).get(random.nextInt(otherComponent.size()));
                if (!graph.containsEdge(u, v)) {
                    DefaultWeightedEdge edge = graph.addEdge(u, v);
                    if (edge != null) {
                        graph.setEdgeWeight(edge, 1.0);
                    }
                }
                mainComponent.addAll(otherComponent);
            }
        }

        return graph;
    }

    @Override
    public Map<Integer, double[]> generateCoordinates(Graph<Integer, DefaultWeightedEdge> graph, int numNodes) {
        Map<Integer, double[]> coords = new HashMap<>();
        double centerX = 50.0;
        double centerY = 50.0;
        double radius = 38.0;

        // Circular layout with controlled radial jitter for organic appearance
        Random layoutRand = new Random(numNodes);
        for (int i = 0; i < numNodes; i++) {
            double angle = 2.0 * Math.PI * i / numNodes;
            double r = radius + (layoutRand.nextDouble() - 0.5) * 12.0;
            double x = Math.max(5.0, Math.min(95.0, centerX + r * Math.cos(angle)));
            double y = Math.max(5.0, Math.min(95.0, centerY + r * Math.sin(angle)));
            coords.put(i, new double[]{x, y});
        }
        return coords;
    }

    @Override
    public String getName() {
        return "Erdős–Rényi Random";
    }
}
