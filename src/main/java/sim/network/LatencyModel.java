package sim.network;

import java.util.Random;

/**
 * Computes per-link simulated latency between peer nodes.
 * Supports distance-based propagation modeling and statistical internet delay distributions.
 */
public class LatencyModel {

    public enum Mode {
        DISTANCE_BASED,
        GAUSSIAN_RANDOM,
        UNIFORM_RANDOM,
        CONSTANT
    }

    private final Mode mode;
    private final double baseDelayMs;
    private final double jitterStdDevMs;
    private final double bandwidthBytesPerSec;
    private final Random random;

    public LatencyModel() {
        this(Mode.DISTANCE_BASED, 20.0, 15.0, 10_000_000.0, 42L); // 10 MB/s bandwidth
    }

    public LatencyModel(Mode mode, double baseDelayMs, double jitterStdDevMs, double bandwidthBytesPerSec, long seed) {
        this.mode = mode;
        this.baseDelayMs = baseDelayMs;
        this.jitterStdDevMs = jitterStdDevMs;
        this.bandwidthBytesPerSec = bandwidthBytesPerSec > 0 ? bandwidthBytesPerSec : 10_000_000.0;
        this.random = new Random(seed);
    }

    /**
     * Calculates the link latency in milliseconds for a transmission between node u and node v.
     *
     * @param u source node ID
     * @param v target node ID
     * @param topology network topology containing spatial coordinates
     * @param messageSizeBytes message payload size in bytes
     * @return latency in milliseconds (always >= 1 ms)
     */
    public long calculateLatencyMs(int u, int v, NetworkTopology topology, int messageSizeBytes) {
        double latency = baseDelayMs;

        // Serialization transmission delay: (bytes / bytesPerSec) * 1000 ms
        double serializationDelay = (messageSizeBytes / bandwidthBytesPerSec) * 1000.0;
        latency += serializationDelay;

        switch (mode) {
            case DISTANCE_BASED:
                if (topology != null) {
                    double[] c1 = topology.getCoordinates(u);
                    double[] c2 = topology.getCoordinates(v);
                    double dx = c1[0] - c2[0];
                    double dy = c1[1] - c2[1];
                    double dist = Math.sqrt(dx * dx + dy * dy); // 0 to ~141
                    // Scale: 1 unit approx 1.0 ms propagation delay
                    latency += dist * 1.0;
                }
                // Add Gaussian jitter
                latency += random.nextGaussian() * jitterStdDevMs;
                break;

            case GAUSSIAN_RANDOM:
                latency += Math.abs(random.nextGaussian() * jitterStdDevMs);
                break;

            case UNIFORM_RANDOM:
                latency += (random.nextDouble() * 2.0 - 1.0) * jitterStdDevMs;
                break;

            case CONSTANT:
            default:
                break;
        }

        return Math.max(2L, Math.round(latency));
    }

    public Mode getMode() {
        return mode;
    }

    public double getBaseDelayMs() {
        return baseDelayMs;
    }

    public double getJitterStdDevMs() {
        return jitterStdDevMs;
    }
}
