import React, { useEffect, useMemo, useRef, useState } from 'react';
import { sha256 } from './lib.js';
import { makeTopology, shortestPath, diameterPath, metrics } from './simulation/topology.js';

/* ---------- CORE: transactions -> block -> hash (animated) ---------- */
export function TxToBlock() {
  const fields = ['index', 'previousHash', 'timestamp', 'txRoot', 'nonce', 'difficulty', 'minerId'];
  const [hl, setHl] = useState(-1);
  return (
    <figure className="fig">
      <div className="tag live">Live explanation · how a block is assembled</div>
      <svg className="flow" viewBox="0 0 640 220" role="img" aria-label="transactions flow into a block, then SHA-256 produces the block hash">
        {['tx1', 'tx2', 'tx3'].map((t, i) => (
          <g key={t} className="tx" style={{ '--fx': '-90px', '--fy': `${(i - 1) * 30}px`, animationDelay: i * 0.25 + 's' }}>
            <rect x="10" y={50 + i * 45} width="90" height="28" fill="none" stroke="var(--net)" /><text x="55" y={69 + i * 45} textAnchor="middle">{t}.id</text></g>))}
        <path d="M100 110 H150" stroke="var(--net)" markerEnd="" /><text x="125" y="100" textAnchor="middle">concat</text>
        <rect x="150" y="90" width="90" height="40" fill="none" stroke="var(--block)" /><text x="195" y="115" textAnchor="middle">SHA-256 → root</text>
        <rect x="270" y="15" width="170" height="190" fill="none" stroke="var(--block)" />
        {fields.map((f, i) => <g key={f} onMouseEnter={() => setHl(i)} onMouseLeave={() => setHl(-1)}>
          <rect x="274" y={20 + i * 25} width="162" height="22" fill={hl === i ? 'rgba(246,196,83,.2)' : 'none'} />
          <text x="284" y={35 + i * 25} style={{ fill: hl === i ? 'var(--block)' : 'var(--text-2)' }}>{f}</text></g>)}
        <path d="M195 130 C195 170 250 150 274 115" fill="none" stroke="var(--block)" />
        <path d="M440 110 H500" stroke="var(--ok)" /><text x="470" y="100" textAnchor="middle">SHA-256</text>
        <rect x="500" y="90" width="130" height="40" fill="none" stroke="var(--ok)" /><text x="565" y="115" textAnchor="middle">block hash</text>
      </svg>
      <figcaption>Hover a field: all seven header fields feed the single SHA-256 call in Block.calculateHash(). The block's own hash is not an input.</figcaption>
    </figure>
  );
}

/* ---------- CORE: avalanche ---------- */
export function Avalanche() {
  const [t, setT] = useState('Bob→Alice:5'); const [a, setA] = useState(['', '']);
  const base = 'Bob→Alice:5';
  useEffect(() => { Promise.all([sha256(base), sha256(t)]).then(setA); }, [t]);
  const bits = (h) => [...h].map((c) => parseInt(c, 16).toString(2).padStart(4, '0')).join('');
  const [x, y] = [bits(a[0] || '0'), bits(a[1] || '0')]; const flipped = [...x].filter((c, i) => c !== y[i]).length;
  return (
    <figure className="fig">
      <div className="tag live">Live explanation · hash avalanche</div>
      <div className="controls">Change one character:<input type="text" value={t} aria-label="message" onChange={(e) => setT(e.target.value)} /></div>
      <div className="diffhash">original {a[0]}</div>
      <div className="diffhash">edited &nbsp; &nbsp;{[...(a[1] || '')].map((c, i) => c === a[0][i] ? c : <i key={i}>{c}</i>)}</div>
      <div className="big"><div><b>{flipped}/256</b>bits flipped</div><div><b>≈128</b>expected if unrelated</div></div>
      <figcaption>HASHING ≠ ENCRYPTION. A hash reveals tampering, it does not hide content. CN CONNECTION · UNIT V — basic cryptography.</figcaption>
    </figure>
  );
}

/* ---------- Node state ---------- */
export function NodeState() {
  const items = [['localChain', 'the ledger this node currently believes'], ['knownBlocks', 'every block seen, including side branches (hash → block)'], ['mempool', 'pending transactions not yet in a block'], ['peers', 'neighbour node IDs from the topology']];
  return <figure className="fig"><div className="tag java">Java · Node.java</div><div className="map">{items.map(([k, v]) => <div key={k}><b>{k}</b><br />{v}</div>)}</div>
    <figcaption>No node holds the authoritative copy. Each decides for itself, which is why two nodes can briefly disagree.</figcaption></figure>;
}

/* ---------- Discrete-event queue ---------- */
const EV = [[120, 'BLOCK_RECEIVED', 'Node 8'], [142, 'BLOCK_RECEIVED', 'Node 19'], [165, 'NODE_CRASH', 'Node 4'], [184, 'BLOCK_RECEIVED', 'Node 11'], [201, 'BLOCK_RECEIVED', 'Node 23'], [250, 'PARTITION_START', 'network'], [310, 'BLOCK_RECEIVED', 'Node 5']];
export function EventQueue() {
  const [i, setI] = useState(0);
  const msg = i === 0 ? 'Virtual clock is at 0 ms. Nothing has been dispatched.' : `Dispatched ${EV[i - 1][1]} (${EV[i - 1][2]}). The clock jumped straight to ${EV[i - 1][0]} ms. No waiting happened.`;
  return (
    <figure className="fig">
      <div className="tag live">Live explanation · illustrative events, same mechanics as SimulationEngine.step()</div>
      <div className="big"><div><b>{i ? EV[i - 1][0] : 0} ms</b>Virtual clock</div><div><b>{EV.length - i}</b>Events queued</div></div>
      <div className="queue">{EV.slice(i).map((e) => <div key={e[0]}><span>{e[0]} ms</span><span>{e[1]} · {e[2]}</span></div>)}{i === EV.length && <div><span>—</span><span>queue empty</span></div>}</div>
      <div className="controls"><button className="primary" disabled={i === EV.length} onClick={() => setI(i + 1)}>Next event</button><button onClick={() => setI(0)}>Reset</button></div>
      <div className="narr">{msg}</div>
      <figcaption>PriorityQueue&lt;Event&gt; → poll earliest timestamp → set currentTimeMs → processEvent() → schedule future events. Virtual time makes runs reproducible; real threads would add timing noise.</figcaption>
    </figure>
  );
}

/* ---------- Latency ---------- */
export function Latency() {
  const [dist, setDist] = useState(60), [jit, setJit] = useState(8);
  const base = 20, ser = (500000 / 10000000) * 1000, tot = Math.max(1, Math.round(base + ser + dist + jit));
  return (
    <figure className="fig">
      <div className="tag live">Live explanation · LatencyModel.calculateLatencyMs() with default parameters (20 ms base, 10 MB/s, 500,000 B block)</div>
      <svg viewBox="0 0 440 60" width="100%"><line x1="30" y1="30" x2="410" y2="30" stroke="var(--net)" /><circle cx="30" cy="30" r="8" fill="none" stroke="var(--block)" /><circle cx="410" cy="30" r="8" fill="none" stroke="var(--block)" />
        <rect className="packet" style={{ '--dur': tot / 100 + 's' }} x="26" y="26" width="8" height="8" fill="var(--mine)" /></svg>
      <div className="dl"><span>Base delay</span><b>{base} ms</b><span>Serialization (500000 B ÷ 10 MB/s)</span><b>{ser} ms</b><span>Distance (1 ms per unit)</span><b>{dist} ms</b><span>Jitter (Gaussian, sample)</span><b>{jit} ms</b><span className="tot">Arrival</span><b className="tot">{tot} ms</b></div>
      <div className="controls">Distance<input type="range" min="0" max="141" value={dist} onChange={(e) => setDist(+e.target.value)} aria-label="distance" />Jitter<input type="range" min="-20" max="30" value={jit} onChange={(e) => setJit(+e.target.value)} aria-label="jitter" /></div>
      <figcaption>Modes in code: DISTANCE_BASED, GAUSSIAN_RANDOM, UNIFORM_RANDOM, CONSTANT. Latency is never below 1 ms.</figcaption>
    </figure>
  );
}

/* ---------- Barabási–Albert growth ---------- */
function baSteps(N, m0 = 4, m = 3, seed = 42) {
  let s = seed; const r = () => ((s = (s * 16807) % 2147483647) - 1) / 2147483646;
  const edges = [], deg = Array(N).fill(0), list = [], hist = [];
  for (let i = 0; i < m0; i++) for (let j = i + 1; j < m0; j++) { edges.push([i, j]); deg[i]++; deg[j]++; list.push(i, j); }
  hist.push({ n: m0, edges: edges.slice(), deg: deg.slice(), t: [] });
  for (let i = m0; i < N; i++) { const T = new Set(); let g = 0; while (T.size < Math.min(m, i) && g++ < 1000) T.add(list[Math.floor(r() * list.length)]);
    const tot = list.length, prob = deg.slice(0, i).map((d) => d / tot);
    T.forEach((t) => { edges.push([i, t]); deg[i]++; deg[t]++; list.push(i, t); });
    hist.push({ n: i + 1, edges: edges.slice(), deg: deg.slice(), t: [...T], prob }); }
  return hist;
}
export function BAGrowth() {
  const N = 40, h = useMemo(() => baSteps(N), []); const [k, setK] = useState(0), [play, setPlay] = useState(false);
  useEffect(() => { if (!play) return; const id = setInterval(() => setK((x) => (x >= h.length - 1 ? (setPlay(false), x) : x + 1)), 500); return () => clearInterval(id); }, [play]);
  const st = h[k], pos = (i) => [50 + 40 * Math.cos(2 * Math.PI * i / N), 50 + 40 * Math.sin(2 * Math.PI * i / N)];
  const mx = Math.max(...st.prob || [0]);
  return (
    <figure className="fig">
      <div className="tag live">Live explanation · mirrors BarabasiAlbertTopology.createGraph() (m0=4, m=3)</div>
      <svg viewBox="0 0 100 100" width="100%" style={{ maxHeight: 440 }} role="img" aria-label="preferential attachment growth">
        {st.edges.map(([a, b], i) => <line key={i} x1={pos(a)[0]} y1={pos(a)[1]} x2={pos(b)[0]} y2={pos(b)[1]} stroke={st.t.includes(b) && a === st.n - 1 ? 'var(--mine)' : 'var(--line-2)'} strokeWidth=".25" />)}
        {Array.from({ length: st.n }, (_, i) => <circle key={i} cx={pos(i)[0]} cy={pos(i)[1]} r={1 + Math.sqrt(st.deg[i]) * .45} fill={i === st.n - 1 ? 'var(--mine)' : 'var(--space-1)'} stroke="var(--block)" strokeWidth=".3" style={{ transition: 'all .5s' }}>
          <title>{`node ${i}: degree ${st.deg[i]}${st.prob && i < st.prob.length ? `, attach prob ${(st.prob[i] * 100).toFixed(1)}%` : ''}`}</title></circle>)}
      </svg>
      <div className="controls"><button className="primary" onClick={() => setPlay(!play)}>{play ? 'Pause' : 'Play growth'}</button><button onClick={() => setK(Math.min(k + 1, h.length - 1))}>Add node</button><button onClick={() => { setK(0); setPlay(false); }}>Reset</button></div>
      <div className="narr">{st.t.length ? `Node ${st.n - 1} joined and picked ${st.t.map((t) => 'node ' + t).join(', ')}. Chance of picking a node = its degree ÷ total degree, so bigger circles (hubs) were likelier.${mx ? ` Largest single chance this round: ${(mx * 100).toFixed(1)}%.` : ''}` : 'Start: a fully connected core of 4 nodes.'}</div>
      <figcaption>The Java code keeps a repeated-degree list: a node appears once per link, so a uniform draw from the list is degree-proportional. Hover a node for its degree and chance.</figcaption>
    </figure>
  );
}

/* ---------- Diameter, average path, degree histogram ---------- */
export function PathsAndDegree() {
  const [type, setType] = useState('scale'), [mode, setMode] = useState('diam'), [hov, setHov] = useState(null);
  const topo = useMemo(() => makeTopology(type, 60, 42), [type]); const met = useMemo(() => metrics(topo), [topo]);
  const dp = useMemo(() => diameterPath(topo), [topo]);
  const pairs = useMemo(() => [[0, 30], [5, 41], [12, 55], [20, 47]].map(([a, b]) => shortestPath(topo, a, b)), [topo]);
  const inP = new Set((mode === 'diam' ? dp : pairs.flat())); const dg = topo.adj.map((a) => a.length);
  const counts = {}; dg.forEach((d) => (counts[d] = (counts[d] || 0) + 1)); const keys = Object.keys(counts).map(Number).sort((a, b) => a - b); const mc = Math.max(...Object.values(counts));
  const onPath = (a, b) => { const P = mode === 'diam' ? [dp] : pairs; return P.some((p) => p.some((x, i) => i < p.length - 1 && ((x === a && p[i + 1] === b) || (x === b && p[i + 1] === a)))); };
  return (
    <figure className="fig">
      <div className="tag live">Live explanation · 60 nodes (Java samples ≤100 source nodes with Dijkstra for average path)</div>
      <div className="tabs">{[['ring', 'Ring'], ['random', 'Random'], ['scale', 'Scale-free']].map(([k, l]) => <button key={k} className={type === k ? 'on' : ''} onClick={() => setType(k)}>{l}</button>)}
        <button className={mode === 'diam' ? 'on' : ''} onClick={() => setMode('diam')}>Show diameter</button><button className={mode === 'apl' ? 'on' : ''} onClick={() => setMode('apl')}>Sample paths</button></div>
      <svg viewBox="0 0 100 100" width="100%" style={{ maxHeight: 440 }} role="img" aria-label="network with highlighted shortest paths">
        {topo.edges.map(([a, b], i) => <line key={i} x1={topo.coords[a][0]} y1={topo.coords[a][1]} x2={topo.coords[b][0]} y2={topo.coords[b][1]} stroke={onPath(a, b) ? 'var(--mine)' : 'var(--line)'} strokeWidth={onPath(a, b) ? .7 : .15} />)}
        {topo.coords.map((c, i) => <circle key={i} cx={c[0]} cy={c[1]} r={hov !== null && dg[i] === hov ? 1.9 : 1.1 + dg[i] * .06} fill={hov !== null && dg[i] === hov ? 'var(--mine)' : inP.has(i) ? 'var(--block)' : 'var(--space-1)'} stroke="var(--text-3)" strokeWidth=".2" />)}
      </svg>
      <div className="big"><div><b>{mode === 'diam' ? dp.length - 1 : met.apl.toFixed(2)}</b>{mode === 'diam' ? 'hops on the diameter path' : 'average path length'}</div><div><b>{met.gini.toFixed(3)}</b>degree Gini</div></div>
      <p>Diameter is the largest of all shortest-path distances. Average path length is their mean. Hover a bar to light the nodes with that degree.</p>
      <div className="hist" role="img" aria-label="degree histogram">{keys.map((k) => <span key={k} className={hov === k ? 'hl' : ''} style={{ height: (counts[k] / mc) * 100 + '%' }} onMouseEnter={() => setHov(k)} onMouseLeave={() => setHov(null)} title={`degree ${k}: ${counts[k]} nodes`} />)}</div>
      <div className="tag">degree {keys[0]} → {keys[keys.length - 1]} · bar height = number of nodes</div>
    </figure>
  );
}

/* ---------- Partition, fork, heaviest work, reorg ---------- */
export function ForkReorg() {
  const [stage, setStage] = useState(0); const A = [4, 4, 4], B = [4, 4, 4, 4];
  const sum = (a) => a.reduce((x, y) => x + y, 0);
  const lab = ['Healthy: one chain', 'Partition: both halves mine', 'Heal: nodes compare work', 'Reorganization'];
  const wA = stage >= 1 ? sum(A) : 0, wB = stage >= 1 ? sum(B) : 0;
  const blk = (x, y, t, c, o = 1) => <g key={t} opacity={o}><rect x={x} y={y} width="46" height="28" fill="none" stroke={c} /><text x={x + 23} y={y + 18} textAnchor="middle">{t}</text></g>;
  return (
    <figure className="fig">
      <div className="tag live">Live explanation · fork and heaviest-work rule (Chain.shouldAdopt)</div>
      <svg viewBox="0 0 640 190" width="100%" role="img" aria-label="chain fork and reorganization">
        {blk(10, 80, 'G', 'var(--block)')}{blk(80, 80, '1', 'var(--block)')}{blk(150, 80, '2', 'var(--block)')}
        {stage >= 1 && <>{blk(220, 30, '3A', stage === 3 ? 'var(--bad)' : 'var(--net)', stage === 3 ? .35 : 1)}{blk(220, 130, '3B', stage === 3 ? 'var(--ok)' : 'var(--mine)')}{blk(290, 130, '4B', stage === 3 ? 'var(--ok)' : 'var(--mine)')}
          <text x="350" y="48" style={{ fill: 'var(--net)' }}>A: 4+4+4 = {wA} work</text><text x="350" y="148" style={{ fill: 'var(--mine)' }}>B: 4+4+4+4 = {wB} work</text></>}
        {stage === 0 && blk(220, 80, '3', 'var(--block)')}
        {stage === 3 && <text x="350" y="100" style={{ fill: 'var(--ok)' }}>B adopted · 3A txs → mempool · B txs leave mempool</text>}
      </svg>
      <div className="controls"><button className="primary" disabled={stage === 3} onClick={() => setStage(stage + 1)}>Next stage</button><button onClick={() => setStage(0)}>Reset</button></div>
      <div className="narr">{lab[stage]}{stage === 2 ? `. ${wB} > ${wA}, so branch B is heavier and Node.attemptReorganization() adopts it.` : ''}{stage === 3 ? '. Common ancestor is block 2; the local chain is replaced.' : ''}</div>
      <figcaption>Each block adds 2^difficulty of work (difficulty 2 → 4 here for simplicity of display). Equal work falls back to the longer chain. Cross-partition messages stop while split: canCommunicateWith() is false.</figcaption>
    </figure>
  );
}
