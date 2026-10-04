import React, { useEffect, useState } from 'react';
import { blockHash, txRoot, prefixOk } from './lib.js';
import { makeTopology } from './simulation/topology.js';
import { propagation, crashSweep, giniSizes, giniByN, partition } from './data/experimentResults.js';

export const Ch = ({ n, id, title, q, band, children, bridge }) => (
  <section className={'chapter' + (band ? ' band' : '')} id={id}>
    <div className="chead"><div className="num">{n}</div><h2>{title}</h2></div><div className="q">{q}</div>
    {children}
    {bridge && <div className="bridge">{bridge}</div>}
  </section>
);
const Hash = ({ h, d = 0, bad }) => (
  <span className={'hash' + (bad ? ' bad' : '')}><span className="z">{h.slice(0, d)}</span>{h.slice(d)}</span>
);

export function Overlay() {
  const pts = [[30,50],[90,20],[90,80],[150,50],[210,25],[210,75]];
  const links = [[0,1],[0,2],[1,2],[1,3],[2,3],[3,4],[3,5],[4,5]];
  return (<>
    <div className="p2p fig">
      <div>
        <svg viewBox="0 0 240 100" width="100%"><g stroke="var(--text-3)">
          {[20,60,100,140,180,220].map((x,i)=><line key={i} x1={x} y1="88" x2="120" y2="42"/>)}</g>
          <circle cx="120" cy="42" r="16" fill="none" stroke="var(--bad)"/><text x="120" y="45" textAnchor="middle">SERVER</text>
          {[20,60,100,140,180,220].map((x,i)=><circle key={i} cx={x} cy="88" r="5" fill="var(--text-2)"/>)}</svg>
        <div className="tag">Traditional application · one authoritative server</div>
      </div>
      <div>
        <svg viewBox="0 0 240 100" width="100%"><g stroke="var(--net)">{links.map(([a,b],i)=><line key={i} x1={pts[a][0]} y1={pts[a][1]} x2={pts[b][0]} y2={pts[b][1]}/>)}</g>
          {pts.map((p,i)=><circle key={i} cx={p[0]} cy={p[1]} r="7" fill="var(--space-0)" stroke="var(--block)"/>)}</svg>
        <div className="tag live">Blockchain overlay · every node keeps its own chain</div>
      </div>
    </div>
    <div className="cols">
      <p>In a normal web service, packets cross a distributed Internet, but the <strong>data</strong> lives on a central application server. Ask the server and you get the one true answer.</p>
      <p>A blockchain has <strong>no such server</strong>. Each node stores its own copy of the ledger, checks every block itself, forwards what is valid to its peers, and may briefly disagree with its neighbours.</p>
    </div>
    <p><strong>A blockchain network does not replace the Internet or TCP/IP. It is an application-level peer-to-peer overlay running on top of ordinary networking.</strong></p>
    <div className="cn">CN CONNECTION · UNIT I — connection topology, layering, client/server vs peer-to-peer</div>
  </>);
}

export function BlockLab() {
  const [txs, setTxs] = useState(['alice→bob:5', 'bob→carol:2']);
  const [hl, setHl] = useState(null);
  const [out, setOut] = useState({});
  const b = { index: 2, prev: 'a41f…(previous block hash)', ts: 1760000000, txs, difficulty: 2, miner: 7, nonce: 0 };
  useEffect(() => { (async () => setOut({ root: await txRoot(txs), hash: await blockHash(b) }))(); }, [txs]);
  const rows = [['index', b.index], ['previousHash', b.prev], ['timestamp', b.ts], ['transactions root', out.root || '…'], ['nonce', b.nonce], ['difficulty', b.difficulty], ['minerId', b.miner]];
  const f = ['index','prev','timestamp','root','nonce','difficulty','miner'];
  return (
    <figure className="fig">
      <div className="tag live">Live explanation · SHA-256 runs in your browser</div>
      {rows.map(([k, v], i) => <div key={k} className={'field' + (hl === i ? ' hl' : '')} onMouseEnter={() => setHl(i)} onMouseLeave={() => setHl(null)}><span>{k}</span><span>{String(v)}</span></div>)}
      <p style={{ marginTop: '1rem' }}>Edit a transaction. The root changes, so the whole header hash changes.</p>
      {txs.map((t, i) => <input key={i} type="text" value={t} aria-label={'transaction ' + (i + 1)} onChange={(e) => setTxs(txs.map((x, j) => (j === i ? e.target.value : x)))} style={{ marginBottom: '.5rem' }} />)}
      <div style={{ marginTop: '1rem' }}>SHA-256( index + previousHash + timestamp + root + nonce + difficulty + minerId )</div>
      <Hash h={out.hash || '…'} />
      <figcaption>Hashing detects change (integrity). It does not hide data: hashing ≠ encryption. CN CONNECTION · UNIT V — basic cryptography</figcaption>
    </figure>
  );
}

export function Mining() {
  const [d, setD] = useState(2), [r, setR] = useState(null), [busy, setBusy] = useState(false);
  const mine = async () => {
    setBusy(true); const t0 = performance.now(); const tries = [];
    const b = { index: 3, prev: '00c9…', ts: 1760000600, txs: ['alice→bob:5'], difficulty: d, miner: 7 };
    let n = 0, h = await blockHash(b, 0);
    while (!prefixOk(h, d)) { n++; h = await blockHash(b, n); if (tries.length < 6 || n % 1 === 0) { tries.push([n, h]); if (tries.length > 7) tries.shift(); } }
    setR({ n, h, ms: Math.round(performance.now() - t0), tries: tries.slice(-6) }); setBusy(false);
  };
  return (
    <figure className="fig">
      <div className="tag live">Live explanation · mirrors Block.mine()</div>
      <div className="controls">Difficulty (leading zero hex chars)
        {[1, 2, 3].map((x) => <button key={x} className={d === x ? 'on' : ''} onClick={() => setD(x)}>{x}</button>)}
        <button className="primary" disabled={busy} onClick={mine}>{busy ? 'Mining…' : 'Mine again'}</button></div>
      {r && <>
        <div className="attempts">{r.tries.map(([n, h]) => <div key={n} className={n === r.n ? 'win' : ''}>nonce {n} → {h.slice(0, 40)}… {n === r.n ? '✓ VALID' : ''}</div>)}</div>
        <div className="big"><div><b>{r.n + 1}</b>Attempts</div><div><b>{r.n}</b>Final nonce</div><div><b>{r.ms} ms</b>Browser time</div></div>
        <Hash h={r.h} d={d} /></>}
      <figcaption>targetPrefix = "0".repeat(difficulty); while hash does not start with it: nonce++. Each extra zero multiplies expected work by 16.</figcaption>
    </figure>
  );
}

export function ChainLab() {
  const [txs, setTxs] = useState(['alice→bob:5', 'bob→carol:2', 'carol→dan:1']);
  const [base, setBase] = useState(null), [cur, setCur] = useState(null);
  const mk = (i, prev, t) => ({ index: i, prev, ts: 1760000000 + i * 600, txs: [t], difficulty: 2, miner: i, nonce: 0 });
  const run = async (list, fixed) => {
    const out = []; let prev = '0'.repeat(64);
    for (let i = 0; i < 3; i++) {
      const b = mk(i + 1, prev, list[i]); let n = fixed ? fixed[i].nonce : 0, h = await blockHash(b, n);
      if (!fixed) while (!prefixOk(h, 2)) { n++; h = await blockHash(b, n); }
      out.push({ ...b, nonce: n, hash: h }); prev = fixed ? fixed[i].hash : h;
    }
    return out;
  };
  useEffect(() => { run(['alice→bob:5', 'bob→carol:2', 'carol→dan:1']).then((c) => { setBase(c); setCur(c); }); }, []);
  const edit = async (v) => { const l = txs.slice(); l[1] = v; setTxs(l); if (base) { const c = await run(l, base.map((b, i) => ({ nonce: b.nonce, hash: b.hash }))); /* stored nonces, stored hashes as links */ const re = []; let prev = '0'.repeat(64);
    for (let i = 0; i < 3; i++) { const b = mk(i + 1, i === 0 ? prev : re[i - 1].hash, l[i]); const h = await blockHash({ ...b }, base[i].nonce); re.push({ ...b, nonce: base[i].nonce, hash: h }); } setCur(re); } };
  if (!cur) return <figure className="fig">mining three blocks…</figure>;
  const status = cur.map((b, i) => (!prefixOk(b.hash, 2) ? 'bad' : i && b.prev !== base[i - 1].hash ? 'bad' : 'ok'));
  return (
    <figure className="fig">
      <div className="tag live">Live explanation · tamper with block 2</div>
      <div className="chain">{cur.map((b, i) => <div key={i} className={'blk ' + status[i]}><h3>Block {b.index}</h3>
        <div>prev</div><div className="h">{b.prev.slice(0, 16)}…</div><div>hash</div><div className="h"><Hash h={b.hash} d={prefixOk(b.hash, 2) ? 2 : 0} bad={status[i] === 'bad'} /></div>
        <div>{status[i] === 'ok' ? '✓ valid' : '✗ invalid'}</div></div>)}</div>
      <p style={{ marginTop: '1rem' }}>Change block 2's transaction (we keep its old nonce, so nobody has re-mined it):</p>
      <input type="text" value={txs[1]} aria-label="block 2 transaction" onChange={(e) => edit(e.target.value)} />
      <figcaption>Block 2 hash changes → block 3's previousHash no longer matches → Chain.isValid() fails. To forge it, an attacker must redo the proof of work for every later block.</figcaption>
    </figure>
  );
}

const seeded = (s) => () => ((s = (s * 16807) % 2147483647) - 1) / 2147483646;
export function Gossip() {
  const [type, setType] = useState('ring'), [fan, setFan] = useState(6), [t, setT] = useState(9999);
  const presets = { 'Slow Ring': ['ring', 2], 'Fast Gossip': ['random', 8], 'Create Hubs': ['scale', 6] };
  const N = 60, topo = makeTopology(type, N, 42);
  const rnd = seeded(7);
  const arrive = Array(N).fill(Infinity); arrive[0] = 0; let msgs = 0;
  const q = [[0, 0, -1]]; const seen = new Set([0]);
  while (q.length) { q.sort((a, b) => a[0] - b[0]); const [tm, u, from] = q.shift();
    const cand = topo.adj[u].filter((v) => v !== from && !seen.has(v));
    const pick = cand.sort(() => rnd() - 0.5).slice(0, fan);
    pick.forEach((v) => { seen.add(v); msgs++; const lat = 20 + 50 + Math.round(rnd() * 30); arrive[v] = tm + lat; q.push([arrive[v], v, u]); }); }
  const s = [...arrive].sort((a, b) => a - b), t50 = s[Math.floor(N * .5) - 1], t90 = s[Math.floor(N * .9) - 1], max = s[N - 1];
  const got = arrive.filter((x) => x <= Math.min(t, max)).length;
  return (
    <figure className="fig">
      <div className="tag live">Live explanation · 60 nodes, simplified browser model (not the Java run)</div>
      <div className="controls">Presets {Object.entries(presets).map(([k, [ty, f]]) => <button key={k} onClick={() => { setType(ty); setFan(f); setT(9999); }}>{k}</button>)}</div>
      <div className="tabs">{[['ring', 'Ring'], ['random', 'Random'], ['scale', 'Scale-free']].map(([k, l]) => <button key={k} className={type === k ? 'on' : ''} onClick={() => setType(k)}>{l}</button>)}</div>
      <svg viewBox="0 0 100 100" width="100%" style={{ maxHeight: 460 }} role="img" aria-label="gossip spreading across the network">
        {topo.edges.map(([a, b], i) => <line key={i} x1={topo.coords[a][0]} y1={topo.coords[a][1]} x2={topo.coords[b][0]} y2={topo.coords[b][1]} stroke="var(--line-2)" strokeWidth=".15" />)}
        {topo.coords.map((c, i) => <circle key={i} cx={c[0]} cy={c[1]} r={i === 0 ? 1.8 : 1.1} fill={arrive[i] <= t ? 'var(--ok)' : 'var(--space-1)'} stroke={i === 0 ? 'var(--block)' : 'var(--text-3)'} strokeWidth=".25" />)}
      </svg>
      <div className="controls">Fanout {fan}<input type="range" min="1" max="8" value={fan} onChange={(e) => setFan(+e.target.value)} aria-label="fanout" />
        Virtual time {Math.min(t, max)} ms<input type="range" min="0" max={max} value={Math.min(t, max)} onChange={(e) => setT(+e.target.value)} aria-label="virtual time" /></div>
      <div className="big"><div><b>{got}/{N}</b>Nodes with block</div><div><b>{t50}</b>T50 ms</div><div><b>{t90}</b>T90 ms</div><div><b>{msgs}</b>Messages</div></div>
      <div className="narr">{`${Math.min(t, max)} ms: ${got} of ${N} nodes hold the block.${got >= N * .5 ? " T50 reached." : ""}${got >= N * .9 ? " T90 reached." : ""} Each informed node forwards to up to ${fan} peers that lack it.`}</div>
      <figcaption>Each node picks up to <em>fanout</em> peers that have not got the block yet, schedules BLOCK_RECEIVED after a latency, and a node that already holds the block discards duplicates.</figcaption>
    </figure>
  );
}

const col = { ring: 'var(--net)', random: 'var(--ok)', scale: 'var(--block)' };
const lab = { ring: 'Ring', random: 'Erdős–Rényi', scale: 'Barabási–Albert' };
export function PropResults() {
  const ks = ['ring', 'random', 'scale'];
  const row = (k, f) => ks.map((x) => <div key={x}><b style={{ color: col[x] }}>{f(propagation[x])}</b>{lab[x]}</div>);
  return (<figure className="fig"><div className="tag java">Java experiment · N=100, difficulty 2, fanout 6, seed 42</div>
    <div className="cmp">{row('t90', (p) => p.t90 + ' ms')}</div><div className="tag">T90 · time for 90% of nodes to have the block</div>
    <div className="cmp">{row('d', (p) => p.diameter)}</div><div className="tag">Diameter · longest shortest path (hops)</div>
    <div className="cmp">{row('g', (p) => p.gini.toFixed(3))}</div><div className="tag">Degree Gini · connectivity inequality</div>
    <figcaption>Same gossip rule, different graph. The ring's diameter of 25 against 4 explains the slowdown.</figcaption></figure>);
}

function Plot({ series, xs, yMax, xl, yl }) {
  const W = 640, H = 260, m = 44, X = (i) => m + (i / (xs.length - 1)) * (W - m - 10), Y = (v) => H - 30 - (v / yMax) * (H - 50);
  return (<svg viewBox={`0 0 ${W} ${H}`} width="100%" role="img" aria-label={yl + ' versus ' + xl}>
    <line x1={m} y1={H - 30} x2={W - 10} y2={H - 30} stroke="var(--line-2)" /><line x1={m} y1="10" x2={m} y2={H - 30} stroke="var(--line-2)" />
    {xs.map((x, i) => <text key={i} x={X(i)} y={H - 12} textAnchor="middle">{x}</text>)}
    {[0, yMax / 2, yMax].map((v, i) => <text key={i} x={m - 6} y={Y(v) + 4} textAnchor="end">{+v.toFixed(2)}</text>)}
    <text x={W / 2} y={H} textAnchor="middle" style={{ fill: 'var(--text-3)' }}>{xl}</text><text x="4" y="12" style={{ fill: 'var(--text-3)' }}>{yl}</text>
    {series.map((s) => <g key={s.name}><polyline fill="none" stroke={s.c} strokeWidth="1.8" points={s.v.map((v, i) => X(i) + ',' + Y(v)).join(' ')} />
      <text x={X(xs.length - 1) - 4} y={Y(s.v[s.v.length - 1]) - 6} textAnchor="end" style={{ fill: s.c }}>{s.name}</text></g>)}
  </svg>);
}
export function GiniPlot() {
  return (<figure className="fig"><div className="tag java">Java experiment 3 · degree Gini vs N (values from README table / generated chart)</div>
    <Plot xs={giniSizes} xl="N nodes" yl="degree Gini" yMax={0.4} series={[{ name: 'Ring', v: giniByN.ring, c: col.ring }, { name: 'Random', v: giniByN.random, c: col.random }, { name: 'Scale-free', v: giniByN.scale, c: col.scale }]} />
    <figcaption>In this project degree Gini is a structural proxy for connectivity inequality, not a full measure of decentralization.</figcaption></figure>);
}
export function CrashPlot() {
  return (<figure className="fig"><div className="tag java">Java experiment 2 · crash sweep, scale-free N=100</div>
    <Plot xs={crashSweep.map((r) => r.rate + '%')} xl="nodes crashed" yl="T90 ms" yMax={500} series={[{ name: 'T90', v: crashSweep.map((r) => r.t90), c: col.scale }, { name: 'T50', v: crashSweep.map((r) => r.t50), c: col.ring }]} />
    <figcaption>Gossip keeps reaching the surviving nodes even at 50% crashes; agreement among online nodes falls to {crashSweep[5].agree}%.</figcaption></figure>);
}
export const PartitionNumbers = () => (<div className="big"><div><b>{partition.percentNodesOnCanonicalChain}%</b>nodes on canonical chain after heal</div><div><b>{partition.partitionReconvergenceTimeMs} ms</b>reconvergence time</div><div><b>{partition.orphanBlocksCount}/{partition.totalBlocksMined}</b>blocks orphaned</div></div>);
