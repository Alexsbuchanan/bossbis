# bossbis dev tools

Node/TypeScript tooling that generates the Java parity reference corpus from
weirdgloop's OSRS DPS calc (`tools/upstream`, pinned at
`5e313c1b6cfba51d11a98c612243570a84772f44`).

## Parity corpus

`npm run gen-corpus` reads every `tools/scenarios/*.json`, validates each is a
well-formed parity row, and writes them (sorted by `name`) into
`src/test/resources/parity/parity-corpus.json`. The Java `ParityCorpusTest`
loads that file; at v0.1.0 every row is SKIPPED because no `PlayerVsNpcCalc.*`
calc path is ported yet (`src/test/resources/parity/port-state.json` lists none
as ported).

### Where the rows come from (v0.1.0)

Rows are **transcribed from upstream's Jest tests** under
`tools/upstream/src/tests/calc/` — each carries a real, upstream-asserted
expected value:

- `GeneratedTests.test.ts` → `maxHit` cases (Osmumten's fang, Dragon hunter
  lance, Blisterwood flail, Obsidian sword, Tumeken's shadow, Fire bolt).
- `BasicRolls.test.ts` → `maxAttackRoll` (accuracy) cases for melee/ranged/magic
  (Abyssal whip, Bow of faerdhinen, Trident of the seas).

Skills not set in a Jest case default to `99`, mirroring `getTestPlayer` /
`generateEmptyPlayer`. Gear ids and the asserted field+value are copied exactly
from the Jest test. Each row's `exercises` lists the (not-yet-ported) calc paths
it touches so the Java runner can skip un-ported rows.

## Parity sweep (live upstream calc)

`npm run gen-sweep` runs the FULL upstream calc (`calculatePlayerVsNpc` from
`upstream/src/tests/utils/TestUtils.ts`) over a broad scenario matrix and writes
one corpus row per scenario into `tools/scenarios/sweep/*.json`, each carrying the
real upstream `{maxAttackRoll, npcDefRoll, accuracy, maxHit, dps, ttk}` as
`expected`. Then re-run `npm run gen-corpus` to fold them into the Java corpus.

### How the live import works

Importing weirdgloop's calc under plain Node fails because its enum/util files
import PNG/SVG assets and pull in React/mobx/localforage/axios. We replicate
their `jest.config.ts` `moduleNameMapper` with a committed, reproducible Node
ESM loader chain (`tools/run-with-upstream.mjs`):

- `tools/upstream-stub-loader.mjs` — a `resolve`/`load` hook that:
  - maps image/css imports and the bare modules `react`/`react-dom`/`next/image`/
    `mobx`/`localforage`/`react-toastify`/`axios` to `tools/upstream-stub.mjs`
    (an inert Proxy), and
  - maps `@/*` → `upstream/src/*` (and forces those `.ts`/`.tsx` files to ESM,
    since `upstream/package.json` has no `"type":"module"`), and
  - redirects `@/state` to `tools/upstream-state-stub.ts`, a lightweight
    `generateEmptyPlayer` (verbatim from `state.tsx`) that avoids the
    mobx/localforage/worker chain `TestUtils` would otherwise drag in.
- `tsx` (registered via its programmatic API) transpiles the TypeScript.

`gen-parity-sweep.ts` opens with a **self-check** that reproduces two known
values (Osmumten's fang max melee = 50; empty-player-vs-Abyssal-demon accuracy =
0.29098123501694634) and throws if either drifts, so the harness is verified
before any `expected` value is trusted.

### Adding sweep scenarios

Edit the `loadouts` / `monsters` / `variationsFor` catalogue in
`tools/gen-parity-sweep.ts`, then `npm run gen-sweep && npm run gen-corpus`.
Each loadout is tagged with `banes` so `monstersFor` targets the relevant
monster attributes (keeping the cross-product focused). Rows record the monster
`version` that upstream's `getTestMonsterById` actually resolved (it matches by
id only and returns the first row), so shared ids resolve identically on the
Java side via `MonsterRepository.resolve(id, version)`.

## Adding a scenario

1. Create `tools/scenarios/NNN-<name>.json` (see existing files for the shape).
2. Run `npm run gen-corpus` — it validates and regenerates the corpus.
3. Tag `exercises` with the calc path(s) the row touches.
