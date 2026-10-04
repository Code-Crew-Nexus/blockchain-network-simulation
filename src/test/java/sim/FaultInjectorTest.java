package sim;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sim.engine.EventType;
import sim.engine.SimulationEngine;
import sim.faults.FaultInjector;
import sim.faults.PartitionInfo;
import sim.network.BarabasiAlbertTopology;
import sim.network.LatencyModel;
import sim.network.NetworkTopology;
import sim.node.Node;
import sim.node.NodeState;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Fault Injector & Consensus Partition Recovery Tests")
class FaultInjectorTest {

    @Test
    @DisplayName("Crash injector should transition designated fraction of nodes to offline state")
    void testNodeCrashFault() {
        int numNodes = 30;
        double crashFraction = 0.30;
        NetworkTopology topo = NetworkTopology.create(new BarabasiAlbertTopology(4, 3, 42L), numNodes, Collections.emptyMap());
        SimulationEngine engine = new SimulationEngine(topo, new LatencyModel(), 1, 4, 42L);

        FaultInjector injector = new FaultInjector(42L);
        Set<Integer> crashed = injector.injectNodeCrashes(engine, crashFraction, 10L);

        assertEquals((int) Math.round(numNodes * crashFraction), crashed.size());

        // Before T=10ms, all nodes are online
        for (int id : crashed) {
            assertEquals(NodeState.ONLINE, engine.getNode(id).getState());
        }

        // Run engine past T=10ms
        engine.runUntil(20L);

        // Crashed nodes should now be OFFLINE_CRASHED
        for (int id : crashed) {
            assertEquals(NodeState.OFFLINE_CRASHED, engine.getNode(id).getState());
            assertFalse(engine.getNode(id).isOnline());
        }
    }

    @Test
    @DisplayName("Network partition should isolate clusters and reconcile upon healing")
    void testNetworkPartitionAndHealing() {
        int numNodes = 20;
        NetworkTopology topo = NetworkTopology.create(new BarabasiAlbertTopology(4, 3, 42L), numNodes, Collections.emptyMap());
        SimulationEngine engine = new SimulationEngine(topo, new LatencyModel(), 1, 4, 42L);

        FaultInjector injector = new FaultInjector(42L);
        PartitionInfo pInfo = injector.injectNetworkPartition(engine, 50L, 500L);

        assertNotNull(pInfo);
        assertEquals(numNodes, pInfo.getNodePartitionMap().size());

        // Step engine to start of partition
        engine.runUntil(60L);

        for (int i = 0; i < numNodes; i++) {
            Node n = engine.getNode(i);
            assertEquals(NodeState.PARTITIONED, n.getState());
            assertTrue(n.getPartitionId() == 1 || n.getPartitionId() == 2);
        }

        // Run engine past heal time
        engine.runUntil(600L);

        for (int i = 0; i < numNodes; i++) {
            Node n = engine.getNode(i);
            assertEquals(NodeState.ONLINE, n.getState());
            assertEquals(0, n.getPartitionId());
        }
    }
}
