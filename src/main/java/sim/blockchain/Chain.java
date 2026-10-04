package sim.blockchain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Encapsulates the local blockchain ledger of a node.
 * Implements block validation, fork detection, and longest-chain consensus rules.
 */
public class Chain implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Block> blocks;

    public Chain() {
        this.blocks = new ArrayList<>();
    }

    public Chain(Block genesis) {
        this.blocks = new ArrayList<>();
        if (genesis != null) {
            this.blocks.add(genesis);
        }
    }

    /**
     * Creates a chain initialized with a standard genesis block.
     *
     * @param difficulty initial difficulty
     * @return initialized chain
     */
    public static Chain createWithGenesis(int difficulty) {
        Block genesis = Block.createGenesis(difficulty);
        return new Chain(genesis);
    }

    /**
     * Returns the tip (latest block) of the chain.
     */
    public Block getLatestBlock() {
        if (blocks.isEmpty()) return null;
        return blocks.get(blocks.size() - 1);
    }

    /**
     * Returns the total number of blocks in the chain.
     */
    public int getLength() {
        return blocks.size();
    }

    /**
     * Returns the total cumulative Proof-of-Work difficulty.
     * In Nakamoto consensus, the heaviest chain with highest cumulative work wins.
     */
    public long getCumulativeDifficulty() {
        long totalWork = 0;
        for (Block b : blocks) {
            // Work equivalent to 2^difficulty (scaled or directly represented)
            totalWork += (1L << Math.min(b.getDifficulty(), 30));
        }
        return totalWork;
    }

    /**
     * Validates whether a candidate block can be legally appended to the tip of this chain.
     */
    public boolean canAccept(Block block, int expectedDifficulty) {
        if (block == null) return false;
        Block tip = getLatestBlock();
        if (tip == null) {
            return block.getIndex() == 0 && block.isValid(expectedDifficulty);
        }
        if (block.getIndex() != tip.getIndex() + 1) {
            return false;
        }
        if (!Objects.equals(block.getPreviousHash(), tip.getHash())) {
            return false;
        }
        return block.isValid(expectedDifficulty);
    }

    /**
     * Attempts to append a block to the tip of this chain.
     *
     * @param block candidate block
     * @param expectedDifficulty target difficulty
     * @return true if added, false if invalid or does not fit tip
     */
    public boolean addBlock(Block block, int expectedDifficulty) {
        if (canAccept(block, expectedDifficulty)) {
            blocks.add(block);
            return true;
        }
        return false;
    }

    /**
     * Fully validates the entire chain from genesis to tip.
     */
    public boolean isValid() {
        if (blocks.isEmpty()) return true;

        // Genesis check
        Block genesis = blocks.get(0);
        if (genesis.getIndex() != 0 || !genesis.isValid(genesis.getDifficulty())) {
            return false;
        }

        // Subsequent blocks check
        for (int i = 1; i < blocks.size(); i++) {
            Block prev = blocks.get(i - 1);
            Block curr = blocks.get(i);

            if (curr.getIndex() != prev.getIndex() + 1) return false;
            if (!Objects.equals(curr.getPreviousHash(), prev.getHash())) return false;
            if (!curr.isValid(curr.getDifficulty())) return false;
        }
        return true;
    }

    /**
     * Determines whether this chain should reorganize and adopt an alternative chain.
     * Rule: Alternative chain must be valid and have higher cumulative work (or longer length if equal).
     */
    public boolean shouldAdopt(Chain alternativeChain) {
        if (alternativeChain == null || !alternativeChain.isValid()) {
            return false;
        }
        long myWork = this.getCumulativeDifficulty();
        long altWork = alternativeChain.getCumulativeDifficulty();
        if (altWork > myWork) {
            return true;
        }
        return altWork == myWork && alternativeChain.getLength() > this.getLength();
    }

    /**
     * Replaces the internal blocks with the blocks of the target chain.
     */
    public void replaceWith(Chain newChain) {
        this.blocks.clear();
        this.blocks.addAll(newChain.getBlocks());
    }

    /**
     * Finds the index of the last common ancestor block between this chain and another chain.
     * Returns -1 if there is no common block.
     */
    public int findCommonAncestorIndex(Chain other) {
        if (other == null) return -1;
        int minLen = Math.min(this.getLength(), other.getLength());
        int commonIdx = -1;
        for (int i = 0; i < minLen; i++) {
            if (this.blocks.get(i).getHash().equals(other.blocks.get(i).getHash())) {
                commonIdx = i;
            } else {
                break;
            }
        }
        return commonIdx;
    }

    /**
     * Deep-copies the chain.
     */
    public Chain copy() {
        Chain copy = new Chain();
        copy.blocks.addAll(this.blocks);
        return copy;
    }

    public List<Block> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    public Block getBlockByIndex(int index) {
        if (index >= 0 && index < blocks.size()) {
            return blocks.get(index);
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("Chain[Length: %d, Tip: %s, TotalWork: %d]",
                blocks.size(),
                getLatestBlock() != null ? getLatestBlock().getHash().substring(0, 8) : "none",
                getCumulativeDifficulty());
    }
}
