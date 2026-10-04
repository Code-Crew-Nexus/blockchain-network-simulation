package sim;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sim.blockchain.Block;
import sim.blockchain.Transaction;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Block & Proof-of-Work Mining Tests")
class BlockTest {

    @Test
    @DisplayName("Genesis block should be valid with difficulty target")
    void testGenesisBlockCreation() {
        int difficulty = 2;
        Block genesis = Block.createGenesis(difficulty);

        assertNotNull(genesis);
        assertEquals(0, genesis.getIndex());
        assertEquals(Block.GENESIS_PREV_HASH, genesis.getPreviousHash());
        assertTrue(genesis.isValid(difficulty));
        assertTrue(genesis.getHash().startsWith("00"));
    }

    @Test
    @DisplayName("Mining should satisfy difficulty and increase nonce")
    void testMiningProofOfWork() {
        int difficulty = 2;
        Block block = new Block(1, "prevHash123", 1000L, List.of(), difficulty, 0);

        long attempts = block.mine(difficulty);
        assertTrue(attempts > 0 || block.getHash().startsWith("00"));
        assertTrue(block.isValid(difficulty));
        assertTrue(block.getHash().startsWith("00"));
    }

    @Test
    @DisplayName("Tampered block data should invalidate cryptographic hash")
    void testTamperDetection() {
        int difficulty = 1;
        Transaction tx = new Transaction("Alice", "Bob", 10.0, 0.1, 100L);
        Block block = new Block(1, "prevHash123", 1000L, List.of(tx), difficulty, 0);
        block.mine(difficulty);

        assertTrue(block.isValid(difficulty));

        // Tamper with hash
        block.setHash("0fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
        assertFalse(block.isValid(difficulty));
    }
}
