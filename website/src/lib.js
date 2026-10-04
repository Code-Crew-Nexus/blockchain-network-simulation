export async function sha256(s) {
  const b = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(s));
  return [...new Uint8Array(b)].map((x) => x.toString(16).padStart(2, '0')).join('');
}
// Mirrors Block.calculateHash(): index + previousHash + timestamp + txRoot + nonce + difficulty + minerId
export async function txRoot(txs) { return txs.length ? sha256(txs.join('')) : 'EMPTY_TX_ROOT'; } // simplified: concat ids -> SHA-256 (not a Merkle tree)
export async function blockHash(b, nonce = b.nonce) {
  return sha256(`${b.index}${b.prev}${b.ts}${await txRoot(b.txs)}${nonce}${b.difficulty}${b.miner}`);
}
export const prefixOk = (h, d) => h.startsWith('0'.repeat(d));
