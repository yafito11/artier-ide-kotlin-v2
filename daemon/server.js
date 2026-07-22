/**
 * Legacy entry — prefer TypeScript daemon:
 *   npm run build && npm start
 *   or: npm run dev
 *
 * This file re-exports a thin note for older scripts that still point at server.js.
 */
console.log('[Daemon] Legacy server.js — use: npm run dev (TypeScript Fastify)');
console.log('[Daemon] Starting TypeScript source via ts-node if available...');

try {
  require('ts-node/register');
  require('./src/server.ts');
} catch (e) {
  console.error('[Daemon] Failed to start TypeScript server:', e.message);
  console.error('[Daemon] Run: npm install && npm run build && npm start');
  process.exit(1);
}
