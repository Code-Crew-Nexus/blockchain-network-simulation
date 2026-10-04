# Blockchain Network Simulation — Interactive Website

React + Vite companion website for the Java Computer Networks PBL **A Simulation-Based Study of Blockchain Network Decentralization, Propagation and Fault Tolerance**.

The Java project is the reference implementation and experiment engine. This web app explains the same concepts visually, provides lightweight browser demonstrations, and presents Java-generated results.

## Local development

```bash
cd website
npm ci
npm run dev
```

## Production build

```bash
npm run build
npm run preview
```

Vite writes the production bundle to:

```text
dist/
```

## Refresh Java experiment data

After rerunning the Java experiments from the repository root:

```bash
cd website
npm run data
```

The Python utility reads the committed Java output files and regenerates:

```text
src/data/experimentResults.js
```

## Learning journey

1. Network overlay and traditional-network comparison
2. Block structure and SHA-256
3. Proof of Work and nonce
4. Chain integrity / tampering
5. Gossip, fanout, latency, and event simulation
6. Ring / Erdős–Rényi / Barabási–Albert topology, path metrics, and Gini
7. Crash faults, partitions, forks, and reorganization
8. Combined Simulation Lab
9. Java experiment recap
10. Audit / limitations
11. Java implementation map

## Source layout

```text
src/
├── App.jsx
├── Figures.jsx
├── Lab.jsx
├── Parts.jsx
├── lib.js
├── main.jsx
├── data/
│   └── experimentResults.js
├── simulation/
│   ├── faults.js
│   ├── gossip.js
│   └── topology.js
└── styles/
    ├── tokens.css
    ├── base.css
    └── chapters.css
```

Typography uses Fraunces Variable for editorial headings, Source Sans 3 Variable for body copy, and IBM Plex Mono for technical values/code.

## Data honesty

- **Live / browser demonstrations** are simplified educational models.
- **Java experiment results** are the reference numerical outputs.
- Do not manually invent experiment values in JSX when a Java-generated output exists.
- The crash-sweep and Gini-versus-N reference tables are retained in the root README/data generator because the current Java experiments do not emit those two tables as dedicated JSON.
- The current partition report records 73% canonical-tip agreement; see the root README for the related metric note.

## Vercel

Import the repository into Vercel and configure:

```text
Root Directory:    website
Framework Preset:  Vite
Build Command:     npm run build
Output Directory:  dist
```

Production URL:

```text
Add the final *.vercel.app URL here after deployment.
```
