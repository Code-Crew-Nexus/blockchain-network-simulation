package sim.blockchain;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a block in the blockchain ledger.
 * Encapsulates block headers, transactions, SHA-256 calculation, and Proof-of-Work mining.
 */
public class Block implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String GENESIS_PREV_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final int index;
    private final String previousHash;
    private final long timestamp;
    private final List<Transaction> transactions;
    private final int difficulty;
    private final int minerId;
    private long nonce;
    private String hash;

    public Block(int index, String previousHash, long timestamp, List<Transaction> transactions, int difficulty, int minerId) {
        this.index = index;
        this.previousHash = previousHash;
        this.timestamp = timestamp;
        this.transactions = transactions != null ? new ArrayList<>(transactions) : new ArrayList<>();
        this.difficulty = difficulty;
        this.minerId = minerId;
        this.nonce = 0L;
        this.hash = calculateHash();
    }

    // Default constructor for Jackson
    public Block() {
        this(0, GENESIS_PREV_HASH, 0L, Collections.emptyList(), 0, -1);
    }

    /**
     * Computes the SHA-256 cryptographic hash of the block contents.
     */
    public String calculateHash() {
        String data = index + previousHash + timestamp + getTransactionsMerkleRoot() + nonce + difficulty + minerId;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm missing", e);
        }
    }

    /**
     * Computes a combined hash representation of the transactions (simplified Merkle root).
     */
    public String getTransactionsMerkleRoot() {
        if (transactions.isEmpty()) {
            return "EMPTY_TX_ROOT";
        }
        StringBuilder sb = new StringBuilder();
        for (Transaction tx : transactions) {
            sb.append(tx.getTxId());
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : hashBytes) {
                out.append(String.format("%02x", b));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            return sb.toString();
        }
    }

    /**
     * Performs Proof-of-Work mining by finding a nonce that yields a hash
     * with the specified number of leading zero hexadecimal characters.
     *
     * @param targetDifficulty number of leading zeros required
     * @return number of hash attempts performed
     */
    public long mine(int targetDifficulty) {
        String targetPrefix = getDifficultyPrefix(targetDifficulty);
        long attempts = 0;
        while (!hash.startsWith(targetPrefix)) {
            nonce++;
            attempts++;
            hash = calculateHash();
        }
        return attempts;
    }

    /**
     * Validates whether this block is cryptographically sound and meets the difficulty target.
     *
     * @param expectedDifficulty expected PoW difficulty
     * @return true if valid, false otherwise
     */
    public boolean isValid(int expectedDifficulty) {
        if (hash == null) return false;
        if (!hash.equals(calculateHash())) return false;
        String prefix = getDifficultyPrefix(expectedDifficulty);
        return hash.startsWith(prefix);
    }

    /**
     * Generates a string of leading zeros corresponding to the given difficulty.
     */
    public static String getDifficultyPrefix(int difficulty) {
        if (difficulty <= 0) return "";
        return "0".repeat(difficulty);
    }

    /**
     * Creates a standardized genesis block.
     *
     * @param difficulty initial difficulty
     * @return genesis block
     */
    public static Block createGenesis(int difficulty) {
        Block genesis = new Block(0, GENESIS_PREV_HASH, 0L, Collections.emptyList(), difficulty, 0);
        genesis.mine(difficulty);
        return genesis;
    }

    // Setters for mutable state during mining or deserialization
    public void setNonce(long nonce) {
        this.nonce = nonce;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    // Getters
    public int getIndex() {
        return index;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public int getDifficulty() {
        return difficulty;
    }

    public int getMinerId() {
        return minerId;
    }

    public long getNonce() {
        return nonce;
    }

    public String getHash() {
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Block block)) return false;
        return index == block.index && Objects.equals(hash, block.hash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, hash);
    }

    @Override
    public String toString() {
        return String.format("Block #%d [Hash: %s..., Prev: %s..., Miner: %d, Nonce: %d]",
                index,
                hash != null && hash.length() >= 8 ? hash.substring(0, 8) : "null",
                previousHash != null && previousHash.length() >= 8 ? previousHash.substring(0, 8) : "null",
                minerId, nonce);
    }
}
