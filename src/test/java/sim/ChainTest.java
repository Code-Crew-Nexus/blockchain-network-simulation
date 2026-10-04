package sim;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sim.blockchain.Block;
import sim.blockchain.Chain;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Blockchain Ledger & Fork Resolution Tests")
class ChainTest {

    @Test
    @DisplayName("Chain should append consecutive valid blocks")
    void testSequentialBlockAddition() {
        int diff = 1;
        Chain chain = Chain.createWithGenesis(diff);
        assertEquals(1, chain.getLength());

        Block b1 = new Block(1, chain.getLatestBlock().getHash(), 100L, List.of(), diff, 0);
        b1.mine(diff);
        assertTrue(chain.addBlock(b1, diff));
        assertEquals(2, chain.getLength());
        assertEquals(b1.getHash(), chain.getLatestBlock().getHash());
        assertTrue(chain.isValid());
    }

    @Test
    @DisplayName("Chain should reject block with mismatched previousHash")
    void testRejectMismatchedPreviousHash() {
        int diff = 1;
        Chain chain = Chain.createWithGenesis(diff);

        Block badBlock = new Block(1, "invalid_previous_hash", 100L, List.of(), diff, 0);
        badBlock.mine(diff);

        assertFalse(chain.addBlock(badBlock, diff));
        assertEquals(1, chain.getLength());
    }

    @Test
    @DisplayName("Longest chain fork resolution: heavier chain should supersede shorter chain")
    void testForkResolutionLongestChain() {
        int diff = 1;
        Chain chainA = Chain.createWithGenesis(diff);
        Chain chainB = chainA.copy();

        // Chain A mines 1 block
        Block a1 = new Block(1, chainA.getLatestBlock().getHash(), 100L, List.of(), diff, 1);
        a1.mine(diff);
        chainA.addBlock(a1, diff);

        // Chain B mines 2 blocks (competing fork)
        Block b1 = new Block(1, chainB.getLatestBlock().getHash(), 100L, List.of(), diff, 2);
        b1.mine(diff);
        chainB.addBlock(b1, diff);

        Block b2 = new Block(2, b1.getHash(), 200L, List.of(), diff, 2);
        b2.mine(diff);
        chainB.addBlock(b2, diff);

        assertEquals(2, chainA.getLength());
        assertEquals(3, chainB.getLength());

        // Chain A should adopt Chain B as it is longer with higher cumulative difficulty
        assertTrue(chainA.shouldAdopt(chainB));
        chainA.replaceWith(chainB);

        assertEquals(3, chainA.getLength());
        assertEquals(b2.getHash(), chainA.getLatestBlock().getHash());
        assertTrue(chainA.isValid());
    }
}
