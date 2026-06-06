// Node ESM loader hook that lets us import weirdgloop's pure-calc chain
// (PlayerVsNPCCalc + lib/types/enums) outside of Next.js/jsdom, the same way
// their jest.config.ts moduleNameMapper stubs assets and resolves @/*.
//
// Responsibilities:
//   1. resolve(): redirect non-calc specifiers (images, css, React, mobx,
//      localforage, react-toastify, axios, next/image) to tools/upstream-stub.mjs.
//   2. resolve(): map @/* -> tools/upstream/src/* as file:// URLs, trying .ts/.tsx
//      /.json/index variants. We do this here (rather than via tsconfig paths)
//      because tools/upstream/package.json has no "type":"module", so Node/tsx
//      would otherwise treat those files as CommonJS and drop tsconfig path
//      resolution mid-chain.
//   3. load(): force resolved upstream .ts/.tsx files to format:'module' so the
//      tsx loader (registered after us) transpiles them as ESM, then defer to it.
//
// Registered alongside tsx in run-with-upstream.mjs via the tsx programmatic API.

import { fileURLToPath, pathToFileURL } from 'node:url';
import { dirname, resolve as pathResolve, join } from 'node:path';
import { existsSync, statSync } from 'node:fs';

const here = dirname(fileURLToPath(import.meta.url));
const STUB_URL = pathToFileURL(pathResolve(here, 'upstream-stub.mjs')).href;
const STATE_STUB = pathResolve(here, 'upstream-state-stub.ts');
const UPSTREAM_SRC = pathResolve(here, 'upstream', 'src');

// Asset extensions weirdgloop imports for side-effect-free URL values.
const ASSET_RE = /\.(jpg|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga|css|less)(\?.*)?$/i;

// Bare modules that are React/runtime-only and never touched by the calc path.
const STUB_BARE = new Set([
  'react',
  'react-dom',
  'react/jsx-runtime',
  'next/image',
  'next/link',
  'mobx',
  'mobx-react-lite',
  'localforage',
  'react-toastify',
  'axios',
]);

const TS_EXTS = ['.ts', '.tsx', '.mts', '.cts'];

// Track which file URLs we resolved from upstream so load() can force ESM format.
const upstreamFileUrls = new Set();

function resolveCandidate(base) {
  // Exact file.
  if (existsSync(base) && statSync(base).isFile()) return base;
  // base + ext
  for (const ext of [...TS_EXTS, '.js', '.jsx', '.mjs', '.cjs', '.json']) {
    if (existsSync(base + ext)) return base + ext;
  }
  // base/index.*
  if (existsSync(base) && statSync(base).isDirectory()) {
    for (const ext of [...TS_EXTS, '.js', '.jsx', '.mjs', '.cjs', '.json']) {
      const idx = join(base, 'index' + ext);
      if (existsSync(idx)) return idx;
    }
  }
  return null;
}

export async function resolve(specifier, context, nextResolve) {
  if (ASSET_RE.test(specifier) || STUB_BARE.has(specifier)) {
    return { url: STUB_URL, shortCircuit: true };
  }

  // @/state -> our lightweight stub (avoids mobx/localforage/worker chain).
  if (specifier === '@/state') {
    const url = pathToFileURL(STATE_STUB).href;
    upstreamFileUrls.add(url);
    return { url, shortCircuit: true };
  }

  // @/foo -> tools/upstream/src/foo
  if (specifier.startsWith('@/')) {
    const target = pathResolve(UPSTREAM_SRC, specifier.slice(2));
    const file = resolveCandidate(target);
    if (file) {
      const url = pathToFileURL(file).href;
      upstreamFileUrls.add(url);
      return { url, shortCircuit: true };
    }
  }

  // Relative imports from inside an upstream file -> resolve against the parent,
  // trying TS extensions (so e.g. '../../cdn/json/equipment.json' and
  // '../../../cdn/json/monsters.json' work, and bare-relative .ts files too).
  if (
    (specifier.startsWith('./') || specifier.startsWith('../')) &&
    context.parentURL &&
    context.parentURL.startsWith('file:') &&
    context.parentURL.includes('/upstream/')
  ) {
    const parentDir = dirname(fileURLToPath(context.parentURL));
    const target = pathResolve(parentDir, specifier);
    const file = resolveCandidate(target);
    if (file) {
      const url = pathToFileURL(file).href;
      if (TS_EXTS.some((e) => file.endsWith(e))) upstreamFileUrls.add(url);
      return { url, shortCircuit: true };
    }
  }

  return nextResolve(specifier, context);
}

export async function load(url, context, nextLoad) {
  // Force upstream .ts/.tsx files to be treated as ESM so the tsx loader
  // transpiles them with ESM semantics (otherwise Node sees no "type":"module"
  // in tools/upstream/package.json and falls back to CommonJS).
  if (upstreamFileUrls.has(url) && !url.endsWith('.json')) {
    return nextLoad(url, { ...context, format: 'module' });
  }
  return nextLoad(url, context);
}
