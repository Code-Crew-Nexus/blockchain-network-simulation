package sim;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sim.blockchain.Block;
import sim.engine.EventType;
import sim.engine.SimulationEngine;
import sim.network.LatencyModel;
import sim.network.NetworkTopology;
import sim.network.RingTopology;
import sim.node.Node;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Gossip Protocol & Discrete-Event Delivery Tests")
class GossipProtocolTest {

    @Test
    @DisplayName("Gossip protocol should deliver block to all nodes in a ring network")
    void testGossipDeliveryAcrossRing() {
        int numNodes = 10;
        int diff = 1;
        int fanout = 2;
        long seed = 123L;

        NetworkTopology ringTopo = NetworkTopology.create(new RingTopology(2), numNodes, Collections.emptyMap());
        LatencyModel latency = new LatencyModel(LatencyModel.Mode.CONSTANT, 10.0, 0.0, 10_000_000.0, seed);
        SimulationEngine engine = new SimulationEngine(ringTopo, latency, diff, fanout, seed);

        // Schedule block mining at Node 0
        engine.scheduleAfter(0, EventType.MINE_TICK, 0, -1, null);

        // Run until queue empty
        engine.runUntilEmpty();

        // Every node should have received the block and updated local chain tip
        Block tipNode0 = engine.getNode(0).getLocalChain().getLatestBlock();
        assertNotNull(tipNode0);
        assertEquals(1, tipNode0.getIndex());

        for (int i = 0; i < numNodes; i++) {
            Node n = engine.getNode(i);
            assertEquals(2, n.getLocalChain().getLength(), "Node " + i + " should have 2 blocks (genesis + mined)");
            assertEquals(tipNode0.getHash(), n.getLocalChain().getLatestBlock().getHash(),
                    "Node " + i + " should have identical canonical tip");
        }
    }

    @Test
    @DisplayName("Gossip protocol should suppress duplicate blocks")
    void testDuplicateSuppression() {
        int numNodes = 5;
        int diff = 1;
        NetworkTopology topo = NetworkTopology.create(new RingTopology(2), numNodes, Collections.emptyMap());
        LatencyModel latency = new LatencyModel(LatencyModel.Mode.CONSTANT, 5.0, 0.0, 10_000_000.0, 42L);
        SimulationEngine engine = new SimulationEngine(topo, latency, diff, 2, 42L);

        // Broadcast a block
        engine.scheduleAfter(0, EventType.MINE_TICK, 0, -1, null);
        engine.runUntilEmpty();

        long eventsProcessedFirstRun = engine.getTotalEventsProcessed();

        // Broadcast the exact same block again
        Block alreadyMined = engine.getNode(0).getLocalChain().getLatestBlock();
        engine.getGossipProtocol().broadcastBlock(0, alreadyMined, engine);
        engine.runUntilEmpty();

        // The second broadcast should be dropped by all nodes since they already have it
        assertEquals(eventsProcessedFirstRun, engine.getTotalEventsProcessed());
    }
}
