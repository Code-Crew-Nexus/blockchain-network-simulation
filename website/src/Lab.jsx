import React, { useMemo, useState } from 'react';
import { makeTopology } from './simulation/topology.js';
import { propagation, crashSweep, giniByN, giniSizes, partition } from './data/experimentResults.js';

const rngOf = (s) => () => ((s = (s * 16807) % 2147483647) - 1) / 2147483646;
function spread(topo, fan, off, cut, seed = 7) {
  const N = topo.n, r = rngOf(seed), at = Array(N).fill(Infinity); let msgs = 0;
  if (off.has(0)) return { at, msgs, t50: 0, t90: 0, got: 0, online: N - off.size };
  at[0] = 0; const q = [[0, 0, -1]], seen = new Set([0]);
  while (q.length) { q.sort((a, b) => a[0] - b[0]); const [tm, u, from] = q.shift();
    const c = topo.adj[u].filter((v) => v !== from && !seen.has(v) && !off.has(v) && !(cut && cut(u, v)));
    c.sort(() => r() - .5).slice(0, fan).forEach((v) => { seen.add(v); msgs++; at[v] = tm + 70 + Math.round(r() * 30); q.push([at[v], v, u]); }); }
  const online = N - off.size, s = at.filter(isFinite).sort((a, b) => a - b), pct = (p) => (s.length >= Math.ceil(online * p) ? s[Math.ceil(online * p) - 1] : null);
  return { at, msgs, t50: pct(.5), t90: pct(.9), got: s.length, online };
}
const cls = { ring: 'Ring', random: 'Erdős–Rényi', scale: 'Barabási–Albert' };
const fmt = (v) => (v === null ? 'not reached' : v + ' ms');

/* ---------- Final combined lab ---------- */
export function SimLab() {
  const [S, setS] = useState({ n: 60, type: 'scale', fan: 6, fault: 'none', rate: 30, diff: 2 });
  const set = (o) => setS({ ...S, ...o });
  const topo = useMemo(() => makeTopology(S.type, S.n, 42), [S.type, S.n]);
  const off = useMemo(() => { const o = new Set(); if (S.fault === 'crash') { const r = rngOf(11); const ids = [...Array(S.n).keys()].slice(1).sort(() => r() - .5); ids.slice(0, Math.round(S.n * S.rate / 100)).forEach((i) => o.add(i)); } return o; }, [S.fault, S.rate, S.n, S.type]);
  const side = (i) => topo.coords[i][0] < 50;
  const cut = S.fault === 'partition' ? (a, b) => side(a) !== side(b) : null;
  const R = spread(topo, S.fan, off, cut);
  const presets = { 'Slow Ring': { type: 'ring', fan: 2, fault: 'none' }, 'Fast Gossip': { type: 'random', fan: 8, fault: 'none' }, 'Create Hubs': { type: 'scale', fan: 6, fault: 'none' }, 'Crash 30%': { fault: 'crash', rate: 30 }, 'Split Network': { fault: 'partition' } };
  const pctRe = Math.round(R.got / Math.max(1, R.online) * 100);
  return (
    <figure className="fig">
      <div className="tag live">Live explanation · browser model, simplified. Java results are in the Experiments chapter.</div>
      <div className="controls">Presets {Object.entries(presets).map(([k, v]) => <button key={k} onClick={() => set(v)}>{k}</button>)}</div>
      <div className="lab">
        <svg viewBox="0 0 100 100" role="img" aria-label="simulation lab network">
          {topo.edges.map(([a, b], i) => { const x = cut && cut(a, b), d = off.has(a) || off.has(b); return <line key={i} x1={topo.coords[a][0]} y1={topo.coords[a][1]} x2={topo.coords[b][0]} y2={topo.coords[b][1]} stroke={x ? 'var(--bad)' : 'var(--line-2)'} strokeDasharray={x ? '1 1.4' : 0} strokeWidth=".15" opacity={d ? .15 : 1} />; })}
          {topo.coords.map((c, i) => <circle key={i} cx={c[0]} cy={c[1]} r={i === 0 ? 1.8 : 1.1} fill={off.has(i) ? 'none' : isFinite(R.at[i]) ? 'var(--ok)' : 'var(--space-1)'} stroke={off.has(i) ? 'var(--bad)' : i === 0 ? 'var(--block)' : 'var(--text-3)'} strokeWidth=".25" />)}
        </svg>
        <div>
          <div className="narr">{S.fault === 'partition' ? `The network is split into two halves. The miner (node 0) is on one side, so only ${pctRe}% of online nodes can hear about the block.` : S.fault === 'crash' ? `${off.size} nodes are crashed (hollow red). The block routes around them and reaches ${pctRe}% of online nodes.` : `All ${S.n} nodes online. The block reaches ${pctRe}% of them with fanout ${S.fan}.`} {S.type === 'ring' && S.fan < 4 ? 'On a ring the wave has to walk around the circle.' : ''}</div>
          <div className="big"><div><b>{R.msgs}</b>messages</div><div><b>{fmt(R.t50)}</b>T50</div><div><b>{fmt(R.t90)}</b>T90</div><div><b>{pctRe}%</b>coverage</div></div>
          <div className="tag">Mining at difficulty {S.diff} needs ≈{(16 ** S.diff).toLocaleString()} hashes on average</div>
        </div>
      </div>
      <div className="controls">
        <label>Nodes {S.n}<input type="range" min="20" max="120" step="10" value={S.n} onChange={(e) => set({ n: +e.target.value })} /></label>
        <label>Fanout {S.fan}<input type="range" min="1" max="8" value={S.fan} onChange={(e) => set({ fan: +e.target.value })} /></label>
        <label>Difficulty {S.diff}<input type="range" min="1" max="4" value={S.diff} onChange={(e) => set({ diff: +e.target.value })} /></label>
        <label>Topology <select value={S.type} onChange={(e) => set({ type: e.target.value })}>{Object.entries(cls).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select></label>
        <label>Fault <select value={S.fault} onChange={(e) => set({ fault: e.target.value })}><option value="none">none</option><option value="crash">node crash</option><option value="partition">partition</option></select></label>
        {S.fault === 'crash' && <label>Fault rate {S.rate}%<input type="range" min="0" max="60" step="10" value={S.rate} onChange={(e) => set({ rate: +e.target.value })} /></label>}
      </div>
      <figcaption>Hollow red = crashed. Dashed red = cut links. Green = node holds the block.</figcaption>
    </figure>
  );
}

/* ---------- Network split / heal ---------- */
export function SplitHeal() {
  const [st, setSt] = useState('healthy');
  const topo = useMemo(() => makeTopology('random', 40, 42), []);
  const side = (i) => topo.coords[i][0] < 50, split = st === 'split';
  const dx = (i) => (split ? (side(i) ? -7 : 7) : 0);
  const msg = { healthy: 'One connected network. Every node can reach every other.', split: 'Both groups are alive, but cross-links are gone. Group A mines block 3A; Group B mines 3B and 4B. Neither side knows about the other.', healed: 'Links restored. Nodes now exchange blocks and compare cumulative work; Group A switches to the heavier branch B.' }[st];
  return (
    <figure className="fig">
      <div className="tag live">Live explanation · partition then heal</div>
      <svg viewBox="0 0 100 100" width="100%" style={{ maxHeight: 400 }} role="img" aria-label="network splitting into two groups and healing">
        {topo.edges.map(([a, b], i) => { const x = side(a) !== side(b); return <line key={i} className="mv" x1={topo.coords[a][0] + dx(a)} y1={topo.coords[a][1]} x2={topo.coords[b][0] + dx(b)} y2={topo.coords[b][1]} stroke={x ? 'var(--bad)' : 'var(--line-2)'} strokeWidth=".18" opacity={x && split ? 0 : 1} />; })}
        {topo.coords.map((c, i) => <circle key={i} className="mv" cx={c[0] + dx(i)} cy={c[1]} r="1.3" fill="var(--space-1)" stroke={split ? (side(i) ? 'var(--net)' : 'var(--mine)') : 'var(--text-3)'} strokeWidth=".3" />)}
        {split && <><text x="22" y="96" textAnchor="middle" style={{ fill: 'var(--net)' }}>GROUP A</text><text x="78" y="96" textAnchor="middle" style={{ fill: 'var(--mine)' }}>GROUP B</text></>}
      </svg>
      <div className="controls"><button className="primary" disabled={split} onClick={() => setSt('split')}>Split network</button><button disabled={!split} onClick={() => setSt('healed')}>Heal network</button><button onClick={() => setSt('healthy')}>Reset</button></div>
      <div className="narr">{msg}</div>
      <figcaption>Mirrors FaultInjector.injectNetworkPartition(): nodes are split by coordinates and canCommunicateWith() fails across the cut.</figcaption>
    </figure>
  );
}

/* ---------- Experiment recap strips ---------- */
export function ExpStrips() {
  const last = giniByN.ring.length - 1, c0 = crashSweep[0], cL = crashSweep[crashSweep.length - 1];
  const rows = [
    ['Experiment 1 · Propagation', 'Does topology change how fast a block spreads?', 'N=100 · difficulty 2 · fanout 6 · seed 42 · ring / Erdős–Rényi / Barabási–Albert',
      [[propagation.ring.t90 + ' ms', 'Ring T90'], [propagation.random.t90 + ' ms', 'Random T90'], [propagation.scale.t90 + ' ms', 'Scale-free T90']],
      `Ring diameter is ${propagation.ring.diameter} hops against ${propagation.random.diameter} and ${propagation.scale.diameter}. The same gossip rule is slower on the longer graph.`],
    ['Experiment 2 · Fault tolerance', 'How much failure can the gossip network absorb?', 'Scale-free N=100 · crash sweep 0–50% · plus one timed partition and heal',
      [[c0.t90 + ' → ' + cL.t90 + ' ms', 'T90 from 0% to 50% crashed'], [partition.percentNodesOnCanonicalChain + '%', 'nodes on canonical chain after heal'], [partition.partitionReconvergenceTimeMs + ' ms', 'partition reconvergence time']],
      'Crashes slow propagation only mildly. The partition result is a measured limitation of the current reorganisation code, since full reconvergence was not reached.'],
    ['Experiment 3 · Structural inequality', 'Do the topologies spread links equally as the network grows?', `N = ${giniSizes.join(', ')} · degree Gini`,
      [[giniByN.ring[last].toFixed(3), 'Ring Gini at N=' + giniSizes[last]], [giniByN.random[last].toFixed(3), 'Random Gini'], [giniByN.scale[last].toFixed(3), 'Scale-free Gini']],
      'The ring is perfectly even, random graphs even out as they grow, and preferential attachment keeps hubs. Gini is a proxy for connectivity inequality, not full decentralization.'],
  ];
  return <div>{rows.map(([t, q, setup, nums, take]) => (
    <div className="strip" key={t}><div className="tag java">Java experiment</div><h3>{t}</h3><div className="sq">{q}</div><div className="setup">{setup}</div>
      <div className="cmp">{nums.map(([v, l], i) => <div key={i}><b>{v}</b>{l}</div>)}</div><p><strong>Takeaway.</strong> {take}</p></div>))}</div>;
}
