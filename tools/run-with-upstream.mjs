// Bootstraps the Node ESM loader chain needed to import weirdgloop's pure-calc
// modules, then runs the TS entry passed as argv[2].
//
//   node tools/run-with-upstream.mjs <entry.ts>
//
// Loader order (Node runs resolve hooks last-registered-first):
//   1. tsx (via its programmatic register API) — transpiles .ts/.tsx and
//      resolves @/* via tsconfig paths.
//   2. upstream-stub — registered AFTER tsx so its resolve hook runs FIRST,
//      short-circuiting asset/React/mobx/etc. specifiers to an inert stub
//      before tsx ever tries to read a .png off disk. This replicates
//      weirdgloop's jest.config.ts moduleNameMapper (fileMock/styleMock).
import { register as registerHook } from 'node:module';
import { register as registerTsx } from 'tsx/esm/api';
import { pathToFileURL } from 'node:url';
import { resolve as pathResolve } from 'node:path';

const here = new URL('.', import.meta.url);
registerTsx({ tsconfig: pathResolve(pathFromUrl(here), 'tsconfig.json') });
registerHook('./upstream-stub-loader.mjs', here);

function pathFromUrl(u) {
  return new URL('.', u).pathname;
}

const entry = process.argv[2];
if (!entry) {
  console.error('usage: node run-with-upstream.mjs <entry.ts>');
  process.exit(1);
}
await import(pathToFileURL(pathResolve(process.cwd(), entry)).href);
