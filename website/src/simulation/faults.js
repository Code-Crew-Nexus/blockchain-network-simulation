// Browser versions of FaultInjector: random crashes and coordinate-based partition (x < 50 => group 1).
export function pickCrashed(n, rate, exclude, rand) {
  const ids = [...Array(n).keys()].filter((i) => i !== exclude);
  for (let i = ids.length - 1; i > 0; i--) { const j = Math.floor(rand() * (i + 1)); [ids[i], ids[j]] = [ids[j], ids[i]]; }
  return ids.slice(0, Math.round(n * rate));
}
export const bisect = (coords) => coords.map((c) => (c[0] < 50 ? 1 : 2));
