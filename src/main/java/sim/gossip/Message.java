package sim.gossip;

import java.io.Serializable;

/**
 * Message envelope for P2P gossip dissemination.
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        BLOCK,
        TRANSACTION
    }

    private final String messageId;
    private final Type type;
    private final int senderId;
    private final Object payload;
    private final int sizeBytes;

    public Message(String messageId, Type type, int senderId, Object payload, int sizeBytes) {
        this.messageId = messageId;
        this.type = type;
        this.senderId = senderId;
        this.payload = payload;
        this.sizeBytes = sizeBytes;
    }

    public String getMessageId() {
        return messageId;
    }

    public Type getType() {
        return type;
    }

    public int getSenderId() {
        return senderId;
    }

    public Object getPayload() {
        return payload;
    }

    public int getSizeBytes() {
        return sizeBytes;
    }

    @Override
    public String toString() {
        return String.format("Msg[%s, Type: %s, From: %d, Size: %d bytes]", messageId, type, senderId, sizeBytes);
    }
}
