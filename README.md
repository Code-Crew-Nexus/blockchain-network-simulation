# Blockchain Network Simulation

**A Simulation-Based Study of Blockchain Network Decentralization, Propagation and Fault Tolerance**

A Computer Networks PBL implemented in **Java 17** with an interactive **React + Vite** companion website. The Java program is the reference simulation and experiment engine; the web application explains the same concepts visually and presents Java-generated results.

> **Suggested GitHub repository name:** `blockchain-network-simulation`
>
> **Live website:** add the final Vercel URL here after deployment.

---

## Project goals

This project studies three questions about a blockchain peer-to-peer overlay network:

1. **Propagation:** how network structure affects the time required for a newly mined block to spread through the network.
2. **Fault tolerance:** how node crashes and network partitions affect block delivery and local chain agreement.
3. **Structural decentralization:** how evenly peer connections are distributed across Ring, Erdős–Rényi, and Barabási–Albert network models.

The project connects these questions to Computer Networks concepts such as topology, forwarding, latency, application-level peer-to-peer communication, and fault/partition behaviour. It does **not** claim to implement a complete TCP/IP stack or a production cryptocurrency network.

---

## Technology stack

### Java simulation

- Java 17+
- Apache Maven
- JGraphT 1.5.2
- XChart 3.8.8
- JavaFX 21.0.2
- Jackson 2.17.1
- JUnit 5.10.2

### Interactive website

- React 18
- Vite 5
- JavaScript / JSX
- Fraunces Variable, Source Sans 3 Variable, IBM Plex Mono
- Static deployment; no backend or database required

---

## What the Java project implements

### Blockchain model

`Block.java` models block metadata including the index, previous hash, timestamp, transactions, difficulty, miner ID, nonce, and SHA-256 hash. Proof of Work increments the nonce until the block hash begins with the configured number of hexadecimal zero characters.

The project uses a **simplified transaction root**: transaction IDs are concatenated and hashed once. It is intentionally not presented as a full Bitcoin-style binary Merkle tree.

### Chain selection

`Chain.java` and `Node.java` maintain local blockchain state and support reorganization toward a complete alternative branch with greater cumulative simulated work. Each block contributes work based on its configured difficulty; equal-work cases may be resolved using chain length.

### Gossip propagation

`GossipProtocol.java` relays blocks and transactions to a configurable number of peers (`fanout`). It avoids immediate sender echo, respects node/partition reachability, and uses duplicate tracking so the same block is not repeatedly processed by the same node.

### Discrete-event simulation

`SimulationEngine.java` uses a timestamp-ordered priority queue rather than wall-clock threads. Events include block mining/receipt, transaction creation/receipt, node crash/recovery, partition start/heal, and mining ticks. This makes the experiments reproducible under a virtual clock.

### Network models

The project includes:

- **Ring Lattice** — regular local-neighbour connectivity.
- **Erdős–Rényi Random Graph** — probabilistic edges with a connectivity safeguard.
- **Barabási–Albert Scale-Free Graph** — preferential attachment that produces hubs.

`NetworkTopology.java` calculates structural metrics such as degree Gini, diameter, average path length, and degree distribution.

### Faults

`FaultInjector.java` supports crash faults, network partitions, healing, and a simplified Byzantine state for custom simulation runs.

---

## Computer Networks connection

The website keeps the CN syllabus relationship focused on concepts that genuinely help explain the project:

- **Network topology:** the overlay graph changes hop counts and propagation behaviour.
- **Forwarding vs gossip:** IP routing selects paths toward destinations; blockchain gossip decides which peers receive application-level information next.
- **Layering:** the blockchain overlay runs above ordinary networking infrastructure; the Java simulator abstracts lower-layer protocol details into peer links, latency, and delivery events.
- **Transport/latency abstraction:** TCP/UDP are not implemented; the simulation models delivery timing directly.
- **Basic cryptography:** SHA-256 hashing supports integrity and Proof of Work; hashing is not encryption.

---

## Repository structure

```text
blockchain-network-simulation/
├── .gitignore
├── README.md
├── NEXUS_GIT_SETUP.md
├── pom.xml
├── run-cli.bat
├── run-experiments.bat
├── run-gui.bat
├── src/
│   ├── main/java/sim/
│   │   ├── Main.java
│   │   ├── blockchain/
│   │   ├── dashboard/
│   │   ├── engine/
│   │   ├── experiments/
│   │   ├── faults/
│   │   ├── gossip/
│   │   ├── metrics/
│   │   ├── network/
│   │   ├── node/
│   │   └── visualize/
│   └── test/java/sim/
├── output/
│   ├── *.json
│   └── *.png
└── website/
    ├── index.html
    ├── package.json
    ├── package-lock.json
    ├── vite.config.js
    ├── scripts/build-data.py
    └── src/
        ├── App.jsx
        ├── Figures.jsx
        ├── Lab.jsx
        ├── Parts.jsx
        ├── data/
        ├── simulation/
        └── styles/
```

`output/` is intentionally committed because it contains the Java-generated experiment evidence used by the academic project and website.

---

## Run the Java project

### Requirements

- JDK 17 or newer
- Maven 3.8+

Verify:

```bash
java -version
mvn -version
```

### Run tests

```bash
mvn clean test
```

### Run all reference experiments

```bash
mvn compile exec:java -Dexec.mainClass="sim.Main" -Dexec.args="--all"
```

On Windows you can also run:

```text
run-experiments.bat
```

### Run a custom simulation

```bash
mvn compile exec:java -Dexec.mainClass="sim.Main" -Dexec.args="--nodes 150 --topology scale_free --difficulty 3 --fanout 8 --fault partition"
```

Supported custom options include node count, topology, Proof-of-Work difficulty, gossip fanout, fault type, and fault rate.

### Launch the JavaFX dashboard

```bash
mvn javafx:run
```

or on Windows:

```text
run-gui.bat
```

---

## Run the website

```bash
cd website
npm ci
npm run dev
```

Production validation:

```bash
npm run build
npm run preview
```

The production bundle is written to:

```text
website/dist/
```

### Refresh website data after Java experiments

After regenerating the Java JSON files in `output/`:

```bash
cd website
npm run data
```

This rebuilds `src/data/experimentResults.js` from the project outputs.

---

## Website learning journey

The current website is organized as an interactive research journey:

1. Network overlay and traditional-network comparison
2. Block construction
3. Proof of Work and nonce
4. Chain integrity and tampering
5. Gossip propagation, latency, and discrete-event simulation
6. Topology, path metrics, preferential attachment, and Gini
7. Crashes, partitions, forks, and reorganization
8. Combined simulation lab
9. Java experiment recap
10. Audit and limitations
11. Java implementation map

Browser interactions are labelled as educational/live demonstrations. Java-generated JSON/plots remain the reference experiment evidence.

---

## Current reference outputs

The committed `output/` directory includes:

- propagation comparison chart and JSON reports for Ring, Random, and Scale-Free topologies;
- fault-tolerance study and partition JSON report;
- Gini-versus-network-size chart;
- Barabási–Albert degree-distribution chart;
- topology images.

Two reference tables used by the website are not currently emitted as dedicated JSON by the Java experiments, so they are retained here and in the website data generator.

### Crash-sweep reference table

| Crash rate | Online nodes | T50 (ms) | T90 (ms) | Canonical-tip agreement |
| ---: | ---: | ---: | ---: | ---: |
| 0% | 100 | 268 | 374 | 100.0% |
| 10% | 90 | 268 | 383 | 98.9% |
| 20% | 80 | 259 | 394 | 97.5% |
| 30% | 70 | 251 | 354 | 98.6% |
| 40% | 60 | 232 | 375 | 96.7% |
| 50% | 50 | 216 | 445 | 90.0% |

### Degree-Gini reference table

| Topology | N=20 | N=50 | N=100 | N=200 | N=300 | N=400 | N=500 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Ring | 0.0000 | 0.0000 | 0.0000 | 0.0000 | 0.0000 | 0.0000 | 0.0000 |
| Erdős–Rényi | 0.3079 | 0.2712 | 0.1718 | 0.1286 | 0.1092 | 0.0986 | 0.0855 |
| Barabási–Albert | 0.2944 | 0.3350 | 0.3557 | 0.3609 | 0.3641 | 0.3721 | 0.3648 |

### Important metric note

The current generated outputs should be treated exactly as produced by the present Java implementation. In particular:

- the current partition JSON reports **73% canonical-tip agreement** after the simulated healing run;
- the field named `partitionReconvergenceTimeMs` currently reports `7500 ms`, but the present implementation does not separately record the exact instant of full reconvergence, so this value should not be described as verified 100% reconvergence time;
- propagation and orphan-rate metrics should be regenerated if their Java calculations are changed later.

Do not replace generated values with older README claims.

---

## Vercel deployment

The website is designed for a static Vite deployment.

When importing this repository into Vercel, use:

```text
Root Directory:    website
Framework Preset:  Vite
Build Command:     npm run build
Output Directory:  dist
```

After deployment, add the production `*.vercel.app` URL to this README and to `website/README.md`.

---

## Git / Nexus organization setup

A complete copy-paste PowerShell workflow for creating the new Nexus GitHub repository is provided in:

**[`NEXUS_GIT_SETUP.md`](NEXUS_GIT_SETUP.md)**

Recommended repository name:

```text
blockchain-network-simulation
```

Recommended description:

> Simulation-based study of blockchain network decentralization, block propagation and fault tolerance using Java, graph algorithms and interactive web visualizations.

Suggested GitHub topics:

```text
blockchain
computer-networks
java
network-simulation
gossip-protocol
graph-algorithms
fault-tolerance
react
vite
pbl
```

---

## Academic scope

This is an undergraduate Computer Networks PBL and educational simulation. It is not a full Bitcoin/Ethereum implementation, production P2P stack, security proof, or complete model of real Internet transport/routing behaviour.
