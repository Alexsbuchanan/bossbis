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

### Why not generate from the live calc yet?

**Automated generation from the live upstream calc is deferred to v0.1.1 — it
requires stubbing weirdgloop's PNG/asset imports the way their `jest.config.ts`
`moduleNameMapper` does.** Under plain `tsx`, importing the calc fails because
weirdgloop's enum files import PNG assets. For now, rows are transcribed from
`upstream/src/tests/` (which already run under Jest with those stubs in place).

## Adding a scenario

1. Create `tools/scenarios/NNN-<name>.json` (see existing files for the shape).
2. Run `npm run gen-corpus` — it validates and regenerates the corpus.
3. Tag `exercises` with the calc path(s) the row touches.
