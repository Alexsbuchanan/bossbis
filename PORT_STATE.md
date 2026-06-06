# Port State

Tracks which TypeScript sources from `weirdgloop/osrs-dps-calc` are ported to Java, at which upstream commit each Java class is synchronized, and caveats.

**Upstream pinned commit (dev submodule `tools/upstream`):** `5e313c1b6cfba51d11a98c612243570a84772f44`

| Java class | TypeScript source | Synced @ | Status | Notes |
|---|---|---|---|---|
| calc/BaseCalc.java | src/lib/BaseCalc.ts | — | not-ported | |
| calc/PlayerVsNpcCalc.java | src/lib/PlayerVsNPCCalc.ts | — | not-ported | incl. getSpecCalc (ported, not optimizer-used) |
| calc/NpcVsPlayerCalc.java | src/lib/NPCVsPlayerCalc.ts | — | not-ported | |
| calc/HitDist.java | src/lib/HitDist.ts | — | not-ported | getHash via BigInteger; verbatim 0x8F000000 mask |
| calc/Equipment.java | src/lib/Equipment.ts | — | not-ported | bonus aggregation, attack speed, canonicalize |
| calc/Comparator.java | src/lib/Comparator.ts | — | not-ported | uses lodash.mergewith |
| calc/MonsterScaling.java | src/lib/MonsterScaling.ts | — | not-ported | |
| calc/CalcDetails.java | src/lib/CalcDetails.ts | — | not-ported | |
| calc/CalcMath.java | src/lib/Math.ts | 5e313c1 | ported | iSqrt, iLerp, MinMax, Factor |
| calc/Constants.java | src/lib/constants.ts | 5e313c1 | partial | scalars only; ID arrays deferred to v0.1.1 |
| calc/EquipmentAliases.java | src/lib/EquipmentAliases.ts | — | not-ported | |
| calc/dists/ClawsDist.java | src/lib/dists/claws.ts | — | not-ported | |
| calc/dists/BoltsDist.java | src/lib/dists/bolts.ts | — | not-ported | |
| calc/scaling/ChambersOfXericScaling.java | src/lib/scaling/ChambersOfXeric.ts | — | not-ported | |
| calc/scaling/TheatreOfBloodScaling.java | src/lib/scaling/TheatreOfBlood.ts | — | not-ported | |
| calc/scaling/TombsOfAmascutScaling.java | src/lib/scaling/TombsOfAmascut.ts | — | not-ported | |
| calc/scaling/DefenceReduction.java | src/lib/scaling/DefenceReduction.ts | — | not-ported | |
| calc/scaling/Phases.java | src/lib/scaling/Phases.ts | — | not-ported | |
| calc/scaling/VardorvisScaling.java | src/lib/scaling/Vardorvis.ts | — | not-ported | |
| calc/support/D3.java | (d3-array sum/max/min/some/range/cross) | — | not-ported | match left-to-right summation order |
| calc/support/MergeWith.java | (lodash.mergewith default semantics) | — | not-ported | no custom customizer |
| calc/support/Utils.java | src/utils.ts | — | not-ported | keys, isDefined, typedMerge, FeatureStatus |
| calc/state/BoostsCalculator.java | src/utils.ts PotionMap + src/state.tsx recomputeBoosts | — | not-ported | per-skill boost deltas; max-combine |

## Status values
- `not-ported` — no Java equivalent yet.
- `partial` — Java exists but does not fully cover the TS source (see Notes).
- `ported` — Java mirror complete and parity-tested against `Synced @`.

## Bump workflow
1. `cd tools/upstream && git checkout <new-commit>`
2. `cd ../.. && git add tools/upstream && git commit -m "sync: weirdgloop -> <new-commit>"`
3. `tools/update-data.sh` to refresh the committed JSON snapshot; commit the diff.
4. Regenerate the parity corpus; inspect the diff; update Java + this table per changed row.
