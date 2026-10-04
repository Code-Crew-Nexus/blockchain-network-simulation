// Browser version of GossipProtocol + a tiny discrete-event clock (SimulationEngine).
// Rules kept from Java: relay to up to `fanout` random peers that have not processed the block yet,
// duplicates are ignored, offline nodes drop messages, partition blocks sends across groups.
import { rng } from './topology.js';
import { pickCrashed, bisect } from './faults.js';

export class GossipSim {
  constructor(topo, { fanout = 3, fault = 'none', rate = 0.2, seed = 42 } = {}) {
    Object.assign(this, { topo, fanout, fault, rate, t: 0, q: [], msgs: [], src: 0, group: null, started: false, healed: false, sent: 0 });
    this.rand = rng(seed);
    this.got = Array(topo.n).fill(null); // virtual ms at which each node received the block
    this.off = Array(topo.n).fill(false);
  }
  canTalk(a, b) { return !this.off[a] && !this.off[b] && (!this.group || this.group[a] === this.group[b]); }
  lat(a, b) { // Java: 20 base + 50 serialisation (500 kB at 10 MB/s) + distance + jitter
    const [p, q] = [this.topo.coords[a], this.topo.coords[b]], g = Math.sqrt(-2 * Math.log(this.rand() || 1e-9)) * Math.cos(6.2832 * this.rand());
    return Math.max(2, Math.round(70 + Math.hypot(p[0] - q[0], p[1] - q[1]) + g * 10));
  }
  relay(s, ex) {
    const c = this.topo.adj[s].filter((p) => p !== ex && this.got[p] === null && this.canTalk(s, p));
    for (let i = c.length - 1; i > 0; i--) { const j = Math.floor(this.rand() * (i + 1)); [c[i], c[j]] = [c[j], c[i]]; }
    c.slice(0, this.fanout).forEach((to) => { const m = { from: s, to, sendT: this.t, arriveT: this.t + this.lat(s, to), done: false };
      this.msgs.push(m); this.q.push(m); this.sent++; });
  }
  start() {
    this.started = true; this.got[this.src] = 0;
    if (this.fault === 'partition') this.group = bisect(this.topo.coords);
    if (this.fault === 'crash') this.crashAt = 40, this.crashSet = pickCrashed(this.topo.n, this.rate, this.src, this.rand);
    this.relay(this.src, -1);
  }
  advance(dt) {
    if (!this.started) return;
    const end = this.t + dt;
    for (;;) {
      this.q.sort((a, b) => a.arriveT - b.arriveT);
      const next = this.q[0];
      if (this.crashAt != null && this.crashAt <= end && (!next || this.crashAt <= next.arriveT)) { this.t = this.crashAt; this.crashSet.forEach((i) => { this.off[i] = true; }); this.crashAt = null; continue; }
      if (!next || next.arriveT > end) break;
      this.q.shift(); this.t = next.arriveT; next.done = true;
      if (!this.off[next.to] && this.got[next.to] === null) { this.got[next.to] = this.t; this.relay(next.to, next.from); }
    }
    this.t = end; this.msgs = this.msgs.filter((m) => !m.done || end - m.arriveT < 300);
  }
  heal() { // links return; holders of the block gossip again over the restored links
    if (!this.group) return; this.group = null; this.healed = true;
    this.got.forEach((g, i) => { if (g !== null && !this.off[i]) this.relay(i, -1); });
  }
  split() { this.group = bisect(this.topo.coords); this.healed = false; }
  get finished() { return this.started && this.q.length === 0; }
  stats() {
    const on = this.off.map((o, i) => (o ? -1 : i)).filter((i) => i >= 0), times = on.map((i) => this.got[i]).filter((x) => x !== null).sort((a, b) => a - b);
    const at = (p) => { const k = Math.ceil(p * on.length) - 1; return times.length > k ? Math.round(times[k]) : null; };
    return { online: on.length, reached: times.length, t50: at(0.5), t90: at(0.9), t100: at(1), agree: on.length ? (100 * times.length) / on.length : 0 };
  }
}
