package sim.node;

/**
 * Operational state of a network node.
 */
public enum NodeState {
    /** Node is fully operational and participating in consensus and gossip */
    ONLINE,

    /** Node is crashed and unresponsive (drops all incoming/outgoing events) */
    OFFLINE_CRASHED,

    /** Node is isolated in a network partition and only communicates within its component */
    PARTITIONED
}
