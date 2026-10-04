package sim;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sim.network.*;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Network Topology & Decentralization Gini Tests")
class NetworkTopologyTest {

    @Test
    @DisplayName("Ring topology should have uniform degree and Gini coefficient of 0.0")
    void testRingTopologyGiniZero() {
        int numNodes = 20;
        int k = 4;
        NetworkTopology ring = NetworkTopology.create(new RingTopology(k), numNodes, Collections.emptyMap());

        assertEquals(numNodes, ring.getNumNodes());
        assertEquals(k, (int) ring.getAverageDegree());

        for (int v = 0; v < numNodes; v++) {
            assertEquals(k, ring.getDegree(v));
        }

        double gini = ring.calculateGiniCoefficient();
        assertEquals(0.0, gini, 0.0001, "A regular ring lattice must have Gini = 0.0 (perfect decentralization)");
    }

    @Test
    @DisplayName("Barabási–Albert topology should exhibit degree inequality (Gini > 0.25)")
    void testBarabasiAlbertGiniInequality() {
        int numNodes = 100;
        NetworkTopology ba = NetworkTopology.create(new BarabasiAlbertTopology(4, 3, 42L), numNodes, Collections.emptyMap());

        assertEquals(numNodes, ba.getNumNodes());
        assertTrue(ba.getNumEdges() > numNodes);

        double gini = ba.calculateGiniCoefficient();
        assertTrue(gini > 0.25, "Scale-free networks must exhibit high Gini coefficient due to hubs");

        // Small-world property: diameter should be significantly smaller than Ring diameter
        NetworkTopology ring = NetworkTopology.create(new RingTopology(2), numNodes, Collections.emptyMap());
        assertTrue(ba.calculateDiameter() < ring.calculateDiameter());
    }

    @Test
    @DisplayName("Erdős–Rényi random graph should be fully connected")
    void testErdosRenyiConnectivity() {
        int numNodes = 50;
        NetworkTopology er = NetworkTopology.create(new ErdosRenyiTopology(0.1, 42L), numNodes, Collections.emptyMap());

        assertEquals(numNodes, er.getNumNodes());
        double avgPath = er.calculateAveragePathLength();
        assertTrue(avgPath > 0.0, "Connected graph must have positive average path length");
        assertTrue(er.calculateDiameter() >= 1);
    }
}
