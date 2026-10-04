// Browser versions of RingTopology / ErdosRenyiTopology / BarabasiAlbertTopology (same rules, same layouts).
export const rng = (s) => () => { s |= 0; s = (s + 0x6d2b79f5) | 0; let t = Math.imul(s ^ (s >>> 15), 1 | s);
  t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t; return ((t ^ (t >>> 14)) >>> 0) / 4294967296; };
export const TYPES = { ring: 'Ring', random: 'Random', scale: 'Scale-Free' };

export function makeTopology(type, n, seed = 42) {
  const r = rng(seed), set = new Set(), add = (a, b) => { if (a !== b) set.add(a < b ? a + '-' + b : b + '-' + a); };
  if (type === 'ring') for (let i = 0; i < n; i++) for (let o = 1; o <= 2; o++) add(i, (i + o) % n); // k = 4
  if (type === 'random') { // G(n,p) with p = max(0.05, 2.5 ln n / n), then bridge components (as the Java default)
    const p = Math.max(0.05, 2.5 * Math.log(n) / n);
    for (let i = 0; i < n; i++) for (let j = i + 1; j < n; j++) if (r() < p) add(i, j);
    const comp = components(n, set);
    for (let c = 1; c < comp.length; c++) add(comp[0][Math.floor(r() * comp[0].length)], comp[c][Math.floor(r() * comp[c].length)]);
  }
  if (type === 'scale') { // preferential attachment, m0 = 4, m = 3
    const m0 = Math.min(n, 4), bag = [];
    for (let i = 0; i < m0; i++) for (let j = i + 1; j < m0; j++) { add(i, j); bag.push(i, j); }
    for (let i = m0; i < n; i++) {
      const t = new Set(); let guard = 0;
      while (t.size < Math.min(3, i) && guard++ < 1000) t.add(bag[Math.floor(r() * bag.length)]);
      t.forEach((x) => { add(i, x); bag.push(i, x); });
    }
  }
  const edges = [...set].map((e) => e.split('-').map(Number));
  const adj = Array.from({ length: n }, () => []);
  edges.forEach(([a, b]) => { adj[a].push(b); adj[b].push(a); });
  return { type, n, edges, adj, coords: layout(type, n, adj) };
}

function components(n, set) {
  const adj = Array.from({ length: n }, () => []);
  set.forEach((e) => { const [a, b] = e.split('-').map(Number); adj[a].push(b); adj[b].push(a); });
  const seen = Array(n).fill(false), out = [];
  for (let s = 0; s < n; s++) if (!seen[s]) { const c = [s], q = [s]; seen[s] = true;
    while (q.length) { const u = q.pop(); adj[u].forEach((v) => { if (!seen[v]) { seen[v] = true; c.push(v); q.push(v); } }); } out.push(c); }
  return out;
}

// Coordinates 0..100, same formulas as generateCoordinates() in the Java strategies.
function layout(type, n, adj) {
  const lr = rng(n), maxDeg = Math.max(1, ...adj.map((a) => a.length));
  return Array.from({ length: n }, (_, i) => {
    const a = 2 * Math.PI * i / n;
    if (type === 'ring') return [50 + 40 * Math.cos(a), 50 + 40 * Math.sin(a)];
    if (type === 'random') { const rad = 38 + (lr() - 0.5) * 12; return [Math.min(95, Math.max(5, 50 + rad * Math.cos(a))), Math.min(95, Math.max(5, 50 + rad * Math.sin(a)))]; }
    const rad = 10 + (1 - Math.sqrt(adj[i].length / maxDeg)) * 34, aa = a + (lr() - 0.5) * 0.2;
    return [50 + rad * Math.cos(aa), 50 + rad * Math.sin(aa)];
  });
}

// Degree Gini (same formula as NetworkTopology.calculateGiniCoefficient), diameter and average path length (BFS).
export function metrics(topo) {
  const { n, adj } = topo, d = adj.map((a) => a.length).sort((x, y) => x - y), tot = d.reduce((s, x) => s + x, 0);
  const gini = tot ? Math.max(0, (2 * d.reduce((s, x, i) => s + (i + 1) * x, 0)) / (n * tot) - (n + 1) / n) : 0;
  let diameter = 0, sum = 0, pairs = 0;
  for (let s = 0; s < n; s++) { const dist = Array(n).fill(-1); dist[s] = 0; const q = [s];
    for (let h = 0; h < q.length; h++) adj[q[h]].forEach((v) => { if (dist[v] < 0) { dist[v] = dist[q[h]] + 1; q.push(v); } });
    dist.forEach((x, v) => { if (v !== s && x > 0) { diameter = Math.max(diameter, x); sum += x; pairs++; } }); }
  return { gini, diameter, apl: pairs ? sum / pairs : 0, edges: topo.edges.length };
}

// BFS shortest path a -> b (array of node ids) and the longest shortest path (the diameter path).
export function shortestPath(topo, a, b) {
  const prev = Array(topo.n).fill(-2); prev[a] = -1; const q = [a];
  for (let h = 0; h < q.length && prev[b] === -2; h++) topo.adj[q[h]].forEach((v) => { if (prev[v] === -2) { prev[v] = q[h]; q.push(v); } });
  if (prev[b] === -2) return [];
  const out = []; for (let x = b; x !== -1; x = prev[x]) out.push(x); return out.reverse();
}
export function diameterPath(topo) {
  let best = [];
  for (let s = 0; s < topo.n; s++) for (let t = s + 1; t < topo.n; t++) { if (best.length && topo.adj[s].length > 6) continue; const p = shortestPath(topo, s, t); if (p.length > best.length) best = p; }
  return best;
}
export const degrees = (topo) => topo.adj.map((a) => a.length);
