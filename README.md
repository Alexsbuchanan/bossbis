# BossBiS

A RuneLite plugin (work in progress) that scans your bank, lets you pick a boss, and suggests the best loadout you can assemble from your owned items — including required consumables and minion/phase switches.

## Status

**v0.0 (Foundation):** a buildable, hub-shaped plugin skeleton. No calc or optimizer yet. See `docs/superpowers/specs/2026-06-05-bossbis-design.md` for the full design and `docs/superpowers/plans/` for phase plans.

## Layout

- `src/main/java/org/bossbis/plugin/` — the RuneLite plugin.
- `src/main/java/org/bossbis/calc/` — the Java port of weirdgloop's DPS calc (vendored as internal source; populated from v0.1.0).
- `src/main/resources/org/bossbis/data/` — committed item/monster JSON snapshot + `data-version.properties`.
- `tools/upstream/` — dev-only git submodule: weirdgloop's calc, for porting reference + corpus generation (not a build input).
- `tools/` — `update-data.sh` (refresh the snapshot), `gen-reference-corpus.ts` (parity corpus), `check-determinism.sh`.

## Attribution

DPS formulas are ported from the OSRS Wiki's open-source DPS calculator: https://github.com/weirdgloop/osrs-dps-calc, authored by Jayden Bailey / WeirdGloop, used under GPLv3. Item and monster data are derived from the OSRS Wiki under CC-BY-SA. Bundled data snapshot version is recorded in `src/main/resources/org/bossbis/data/data-version.properties`.

## Building

Prerequisites: JDK 11+, Node 20.11+, Git 2.28+.

```bash
git clone --recurse-submodules https://github.com/<you>/bossbis.git
cd bossbis
./gradlew build           # compile + run tests (does NOT require the submodule)
./gradlew run             # launch a dev RuneLite client with BossBiS sideloaded
```

If you cloned without `--recurse-submodules`, the build still works (data is committed); to port code or regenerate the corpus, run `git submodule update --init`.

## Tracking upstream weirdgloop

```bash
git fetch weirdgloop
git log --oneline weirdgloop/main | head
tools/update-data.sh <new-commit>   # bump the data snapshot
```

See `PORT_STATE.md` for which Java classes are synced to which upstream commit.

## Distribution

BossBiS targets the RuneLite Plugin Hub. The plugin is one self-contained repo; we submit a two-line manifest (`repository=` + `commit=`) to a fork of `runelite/plugin-hub`. The hub builds this repo into a single jar. Metadata lives in `runelite-plugin.properties` (`build=gradle`).

## License

GPLv3 (see `LICENSE`) — required because the calc is a derived work of weirdgloop's GPLv3 calc. (GPLv3 hub acceptance is being confirmed with maintainers; see the spec, §2.2.)
