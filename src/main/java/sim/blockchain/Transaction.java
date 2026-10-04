package sim.blockchain;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Represents a discrete transaction within a block or mempool.
 */
public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String txId;
    private final String sender;
    private final String recipient;
    private final double amount;
    private final double fee;
    private final long timestamp;

    public Transaction(String sender, String recipient, double amount, double fee, long timestamp) {
        this.sender = sender;
        this.recipient = recipient;
        this.amount = amount;
        this.fee = fee;
        this.timestamp = timestamp;
        this.txId = calculateTxId();
    }

    // Jackson deserialization constructor
    public Transaction() {
        this("0", "0", 0.0, 0.0, 0L);
    }

    private String calculateTxId() {
        String data = sender + recipient + amount + fee + timestamp;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public String getTxId() {
        return txId;
    }

    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public double getAmount() {
        return amount;
    }

    public double getFee() {
        return fee;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction that)) return false;
        return Objects.equals(txId, that.txId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(txId);
    }

    @Override
    public String toString() {
        return String.format("Tx[%s: %s->%s (%.2f)]", txId.substring(0, 8), sender, recipient, amount);
    }
}
