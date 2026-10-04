package sim.engine;

/**
 * Categorization of discrete simulation events.
 */
public enum EventType {
    /** A node successfully mines a new block */
    BLOCK_MINED,

    /** A node receives a propagated block from a peer */
    BLOCK_RECEIVED,

    /** A new transaction is created and injected into a node's mempool */
    TX_CREATED,

    /** A node receives a transaction from a peer */
    TX_RECEIVED,

    /** A periodic mining trigger event */
    MINE_TICK,

    /** Fault Injection: A node abruptly crashes */
    NODE_CRASH,

    /** Fault Injection: A crashed node recovers and rejoins the network */
    NODE_RECOVER,

    /** Fault Injection: Network splits into isolated partitions */
    PARTITION_START,

    /** Fault Injection: Network partition heals and cross-links are restored */
    PARTITION_HEAL
}
