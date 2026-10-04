package sim.engine;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Encapsulates a scheduled discrete simulation event in virtual time.
 * Naturally ordered by virtual timestamp, with atomic sequence number tie-breaking.
 */
public class Event implements Comparable<Event> {
    private static final AtomicLong SEQUENCE_GENERATOR = new AtomicLong(0);

    private final long timestampMs;
    private final long sequenceId;
    private final EventType type;
    private final int sourceNodeId;
    private final int targetNodeId;
    private final Object payload;

    public Event(long timestampMs, EventType type, int sourceNodeId, int targetNodeId, Object payload) {
        this.timestampMs = timestampMs;
        this.sequenceId = SEQUENCE_GENERATOR.getAndIncrement();
        this.type = type;
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.payload = payload;
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    public long getSequenceId() {
        return sequenceId;
    }

    public EventType getType() {
        return type;
    }

    public int getSourceNodeId() {
        return sourceNodeId;
    }

    public int getTargetNodeId() {
        return targetNodeId;
    }

    public Object getPayload() {
        return payload;
    }

    @Override
    public int compareTo(Event o) {
        int cmp = Long.compare(this.timestampMs, o.timestampMs);
        if (cmp != 0) return cmp;
        return Long.compare(this.sequenceId, o.sequenceId);
    }

    @Override
    public String toString() {
        return String.format("[%d ms] %s (src:%d -> dst:%d)", timestampMs, type, sourceNodeId, targetNodeId);
    }
}
