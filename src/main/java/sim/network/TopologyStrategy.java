package sim.network;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.Map;

/**
 * Strategy interface for generating peer-to-peer network topologies.
 * Follows the Strategy design pattern to allow dynamic selection of network structures.
 */
public interface TopologyStrategy {

    /**
     * Constructs a JGraphT graph according to the specific topology algorithm.
     *
     * @param numNodes number of nodes in the network
     * @param params   algorithm-specific parameters (e.g., edge probability, initial nodes)
     * @return constructed weighted graph
     */
    Graph<Integer, DefaultWeightedEdge> createGraph(int numNodes, Map<String, Object> params);

    /**
     * Generates 2D (x, y) coordinates for each node, normalized between 0.0 and 100.0.
     * Used for spatial distance calculation and visual layout plotting.
     *
     * @param graph    the constructed graph
     * @param numNodes number of nodes
     * @return map of Node ID to [x, y] coordinates
     */
    Map<Integer, double[]> generateCoordinates(Graph<Integer, DefaultWeightedEdge> graph, int numNodes);

    /**
     * Returns the human-readable identifier of the topology strategy.
     */
    String getName();
}
