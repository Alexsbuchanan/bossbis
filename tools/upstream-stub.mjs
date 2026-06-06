// Stub module substituted for weirdgloop's non-calc dependencies (image/asset
// imports, React, mobx, localforage, react-toastify, axios, next/image). The
// pure-calc chain (PlayerVsNPCCalc and its lib/types/enums deps) never *calls*
// these at module-eval or calc time — they are only imported for side-effect-free
// values (image URLs) or types — so an inert object is sufficient.
//
// This mirrors what weirdgloop's jest.config.ts does via moduleNameMapper
// (fileMock.js / styleMock.js) plus jsdom providing React's runtime.

const stub = new Proxy(function stub() {}, {
  get: (_t, prop) => {
    // Common named exports that upstream destructures; return inert callables/objects.
    if (prop === '__esModule') return true;
    if (prop === 'default') return stub;
    return stub;
  },
  apply: () => stub,
  construct: () => ({}),
});

export default stub;
