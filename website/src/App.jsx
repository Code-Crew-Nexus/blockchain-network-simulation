import React, { useEffect, useState } from 'react';
import { TxToBlock, Avalanche, NodeState, EventQueue, Latency, BAGrowth, PathsAndDegree, ForkReorg } from './Figures.jsx';
import { SimLab, SplitHeal, ExpStrips } from './Lab.jsx';
import { Ch, Overlay, BlockLab, Mining, ChainLab, Gossip, PropResults, GiniPlot, CrashPlot, PartitionNumbers } from './Parts.jsx';

const steps = [['Network', 'overlay'], ['Block', 'block'], ['Mine', 'mine'], ['Chain', 'chain'], ['Propagate', 'gossip'], ['Topology', 'topology'], ['Fail', 'faults'], ['Lab', 'lab'], ['Experiment', 'experiments'], ['Audit', 'audit']];
const map = [['Proof-of-work nonce search', 'Block.mine()'], ['Header hash', 'Block.calculateHash()'], ['Transaction root (simplified)', 'Block.getTransactionsMerkleRoot()'], ['Chain tamper check', 'Chain.isValid()'], ['Heaviest-work rule', 'Chain.shouldAdopt()'], ['Reorganization', 'Node.attemptReorganization()'],
  ['Gossip wave', 'GossipProtocol.relayBlockToPeers()'], ['Event queue', 'SimulationEngine.step()'], ['Preferential attachment', 'BarabasiAlbertTopology.createGraph()'], ['Gini', 'NetworkTopology.calculateGiniCoefficient()'], ['Network split', 'FaultInjector.injectNetworkPartition()'], ['Latency', 'LatencyModel.calculateLatencyMs()']];

export default function App() {
  const [p, setP] = useState(0), [act, setAct] = useState('overlay');
  useEffect(() => { const f = () => setP(scrollY / Math.max(1, document.body.scrollHeight - innerHeight)); addEventListener('scroll', f); return () => removeEventListener('scroll', f); }, []);
  useEffect(() => { const io = new IntersectionObserver((es) => es.forEach((e) => e.isIntersecting && setAct(e.target.id)), { rootMargin: '-40% 0px -55% 0px' }); steps.forEach(([, id]) => { const el = document.getElementById(id); el && io.observe(el); }); return () => io.disconnect(); }, []);
  return (<>
    <header className="nav"><div className="nav-row"><b>Blockchain network simulation</b>
      <a className="keep" href="#overlay">Research Journey</a><a href="#lab">Simulation Lab</a><a href="#experiments">Experiments</a><a href="#implementation">Implementation</a></div>
      <div className="tracker">{steps.map(([l, id]) => <a key={id} className={act === id ? 'on' : ''} href={'#' + id}>{l}</a>)}</div><div className="progress" style={{ width: p * 100 + '%' }} /></header>

    <section className="hero">
      <svg className="bg" viewBox="0 0 100 60" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
        {[[10,20,35,10],[35,10,60,25],[60,25,85,12],[35,10,40,45],[40,45,60,25],[60,25,70,50],[40,45,70,50],[85,12,70,50]].map((l, i) => <line key={i} x1={l[0]} y1={l[1]} x2={l[2]} y2={l[3]} stroke="var(--net)" strokeWidth=".15" />)}
        {[[10,20],[35,10],[60,25],[85,12],[40,45],[70,50]].map((c, i) => <circle key={i} cx={c[0]} cy={c[1]} r=".9" fill="var(--block)" />)}
      </svg>
      <div className="kicker">A simulation-based study</div>
      <h1>From one block to an entire network</h1>
      <p className="ask">What happens between one miner creating a block and an entire decentralized network agreeing that it exists?</p>
      <div className="facts"><div><b>3</b>topology models</div><div><b>100</b>node reference experiments</div><div><b>T50/T90/T100</b>propagation metrics</div><div><b>Java</b>discrete-event simulation</div></div>
      <div><button className="primary" onClick={() => (location.hash = 'overlay')}>Explore the network journey</button></div>
      <p className="tag" style={{ marginTop: '2rem' }}>A Simulation-Based Study of Blockchain Network — Decentralization, Propagation and Fault Tolerance</p>
    </section>

    <Ch n="01" id="overlay" title="Why blockchain is a networks problem" q="Who holds the truth when there is no server?" bridge="Once the nodes exist, what exactly are they sharing? Blocks.">
      <Overlay /><NodeState /></Ch>

    <Ch n="02" id="block" band title="What is a block?" q="What does one block actually contain in this Java project?" bridge="Anyone can build such a block. So what makes one hard to make?">
      <TxToBlock />
      <BlockLab />
      <Avalanche />
      <div className="cols"><p>A block is a small record: an <strong>index</strong>, the <strong>previousHash</strong> that points back at its parent, a <strong>timestamp</strong>, the <strong>transactions</strong>, a <strong>difficulty</strong>, the <strong>minerId</strong>, a <strong>nonce</strong>, and its own <strong>hash</strong>.</p>
        <p><strong>SHA-256</strong> squeezes the header into 64 hex characters. Change one character anywhere and the output looks completely different. That is the core trick: the hash is a tamper-evident fingerprint.</p></div>
      <details><summary>Is the transaction root a real Merkle tree?</summary><p>No. <code>getTransactionsMerkleRoot()</code> concatenates the transaction IDs and hashes them once with SHA-256. A production Merkle tree hashes pairs level by level so single transactions can be proven without the whole list.</p></details>
    </Ch>

    <Ch n="03" id="mine" title="Proof of work and the nonce" q="If anyone can construct a block, what makes mining difficult?" bridge="A valid block is expensive to make and cheap to check. Now how do blocks link into a chain?">
      <Mining />
      <div className="cols"><p>The nonce has no meaning by itself. Its only job is to change the header so the hash changes. The miner keeps trying nonces until the hash begins with the required number of zero hex characters.</p>
        <p>This project defines difficulty as that count of leading zeros, which suits teaching. Real systems express it as a numeric target. Checking a block costs one hash, but finding it costs thousands, and that asymmetry is what makes rewriting history costly.</p></div>
    </Ch>

    <Ch n="04" id="chain" band title="The chain" q="Why can't someone quietly edit an old block?" bridge="The block is valid and linked. But how does anyone else learn about it?">
      <ChainLab />
      <div className="cols"><p>Each block stores its parent's hash, so editing an old block changes its hash and breaks the link in the next block. Honest nodes reject the broken chain.</p>
        <p>When two valid chains compete, <code>Chain.shouldAdopt()</code> compares cumulative work, where each block contributes 2<sup>difficulty</sup>. Equal work falls back to the longer chain. So the rule is the <strong>heaviest valid chain</strong>, not simply the longest.</p></div>
    </Ch>

    <Ch n="05" id="gossip" title="Propagation" q="The miner has the block. How does everyone else learn about it?" bridge="Every peer follows the same rule. So why does topology change how fast it spreads?">
      <Gossip />
      <div className="cols"><p>This is gossip, not routing. IP routing decides how packets reach <em>one</em> peer. Gossip decides <em>which peers</em> hear next. The Java <code>GossipProtocol</code> excludes the sender and peers that already have the block, picks up to <em>fanout</em> candidates at random, and schedules a delayed BLOCK_RECEIVED event for each.</p>
        <p>More fanout can mean faster spread but more messages. Duplicates are dropped through a <code>blockHash → Set&lt;NodeId&gt;</code> record. That is application-level suppression, not CRC or ARQ.</p></div>
      <div className="cn">CN CONNECTION · UNIT III — forwarding vs dissemination. Gossip is not an IP routing protocol.</div>
      <Latency />
      <EventQueue />
      <details><summary>How is latency computed?</summary><p>Base delay + serialization (bytes ÷ bandwidth) + distance-based propagation + Gaussian jitter. Blocks are modelled at 500,000 bytes and transactions at 500 bytes. The default model uses 20 ms base, 10 MB/s bandwidth.</p></details>
      <details><summary>Why a discrete-event simulation?</summary><p>Events sit in a <code>PriorityQueue</code>. <code>SimulationEngine.step()</code> pops the earliest virtual timestamp, dispatches it, and schedules future events. Real thread scheduling would add timing noise; virtual time keeps experiments reproducible.</p></details>
    </Ch>

    <Ch n="06" id="topology" band title="Topology" q="The gossip rule stayed the same. Why was one network faster?" bridge="Fast networks may concentrate links in a few hubs. What happens when nodes fail?">
      <BAGrowth />
      <PathsAndDegree />
      <PropResults />
      <div className="cols"><p><strong>Ring lattice (k=4):</strong> every node links to its two nearest neighbours on each side, so all degrees are equal and Gini is 0, but routes are long.</p>
        <p><strong>Erdős–Rényi (p=0.08):</strong> every pair gets one random trial, then disconnected components are bridged so the chain stays connected.</p>
        <p><strong>Barabási–Albert (m0=4, m=3):</strong> each new node attaches to existing nodes with probability proportional to their degree. Hubs emerge.</p>
        <p><strong>Gini</strong> sorts node degrees and measures how unequally links are spread. Higher means more unequal connectivity.</p></div>
      <div className="cn">CN CONNECTION · UNIT I — network topology and the overlay it creates</div>
      <details><summary>Gini formula as implemented</summary><p>G = (2 × Σ rank·degree) / (n × Σ degree) − (n+1)/n, with degrees sorted ascending.</p></details>
      <GiniPlot />
    </Ch>

    <Ch n="07" id="faults" title="When the network fails" q="What happens when nodes disappear, or the network is cut in two?" bridge="So what did the Java experiments actually measure?">
      <SplitHeal />
      <CrashPlot />
      <div className="cols"><p>A crash removes a computer. A <strong>partition</strong> keeps the computers alive but stops communication between groups. <code>FaultInjector.injectNetworkPartition()</code> splits nodes by x-coordinate (balanced fallback), then heals later.</p>
        <p>While split, each side can mine its own block, creating a <strong>fork</strong>. After healing, a node adopts the heaviest valid chain, returns abandoned transactions to its mempool, and drops transactions confirmed on the winning branch.</p></div>
      <ForkReorg />
      <PartitionNumbers />
      <details><summary>Implementation limitation</summary><p>Reorganization rebuilds the competing branch from <code>knownBlocks</code>. If a parent is missing, it fails. In the partition run only 73% of nodes ended on the canonical chain by the end, so full reconvergence was not reached. This is a measured result of the current Java code.</p></details>
    </Ch>

    <Ch n="08" id="lab" band title="Simulation lab" q="Now that you know every part, what happens if you change them all?" bridge="That was the browser model. What did the real Java experiments measure?">
      <SimLab /></Ch>

    <Ch n="09" id="experiments" title="What the Java experiments found" q="What do the numbers say?" bridge="What does this simulation prove, and what does it not?">
      <ExpStrips />
    </Ch>

    <Ch n="10" id="audit" band title="Audit" q="What does this simulation prove, and what does it not prove?" bridge="How was all of this implemented?">
      <div className="cols"><p>It demonstrates relationships <strong>inside this simulation model</strong>. It does not reproduce Bitcoin or Ethereum.</p><p>Topologies are synthetic and latency is simulated. Degree Gini measures connectivity inequality, not every dimension of decentralization.</p>
        <p>The Byzantine model is simplified. Browser demos are educational; the Java-generated experiment data is the project reference.</p></div>
    </Ch>

    <Ch n="11" id="implementation" title="Java implementation map" q="Which class produced what you saw?">
      <div className="map">{map.map(([a, b]) => <div key={b}><b>YOU SAW </b>{a}<br /><b>JAVA </b><code>{b}</code></div>)}</div>
    </Ch>
    <footer className="foot">A Simulation-Based Study of Blockchain Network — Decentralization, Propagation and Fault Tolerance.</footer>
  </>);
}
