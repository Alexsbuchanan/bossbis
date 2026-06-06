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
| calc/support/D3.java | (d3-array sum/max/min/some/range/cross) | 5e313c1 | ported | match left-to-right summation order |
| calc/support/MergeWith.java | (lodash.mergewith default semantics) | 5e313c1 | ported | no custom customizer |
| calc/support/Utils.java | src/utils.ts | 5e313c1 | partial | isDefined + FeatureStatus; getCombatStylesForCategory deferred to M3 |
| calc/state/BoostsCalculator.java | src/utils.ts PotionMap + src/state.tsx recomputeBoosts | — | not-ported | per-skill boost deltas; max-combine |
| calc/types/Prayer.java | src/enums/Prayer.ts | 5e313c1 | ported | numeric enum 0..30; ordinal == upstream value; PrayerData + PrayerMap factors |
| calc/types/Potion.java | src/enums/Potion.ts | 5e313c1 | ported | plain ordinal enum (22 members); no JSON |
| calc/types/EquipmentCategory.java | src/enums/EquipmentCategory.ts | 5e313c1 | ported | string-valued (32 members) + fromValue |
| calc/types/MonsterAttribute.java | src/enums/MonsterAttribute.ts | 5e313c1 | ported | string-valued (16 members incl. vampyre1/2/3) + fromValue + isVampyre |
| calc/types/BurnImmunity.java | src/types/Monster.ts:9-13 | 5e313c1 | ported | string-valued (Weak/Normal/Strong) + fromValue |
| calc/types/UserIssueType.java | src/enums/UserIssueType.ts | 5e313c1 | ported | string-valued (12 members) + fromValue |
| calc/types/CombatStyle.java | src/types/PlayerCombatStyle.ts | 5e313c1 | ported | PlayerCombatStyle + CombatStyleType/Stance (nullable strings) + getRangedDamageType |
| calc/types/Spell.java | src/types/Spell.ts | 5e313c1 | ported | Spell @Value (max_hit) + Spellbook + Spellement |
| calc/types/Player model | src/types/Player.ts | 5e313c1 | ported | PlayerSkills(+mining/herblore)/PlayerBonuses(ranged_str,magic_str)/PlayerOffensive/PlayerDefensive/EquipmentStats/ItemVars/EquipmentPiece(inlined stats)/PlayerEquipment/Buffs(no potions)/Player |

## Status values
- `not-ported` — no Java equivalent yet.
- `partial` — Java exists but does not fully cover the TS source (see Notes).
- `ported` — Java mirror complete and parity-tested against `Synced @`.

## Bump workflow
1. `cd tools/upstream && git checkout <new-commit>`
2. `cd ../.. && git add tools/upstream && git commit -m "sync: weirdgloop -> <new-commit>"`
3. `tools/update-data.sh` to refresh the committed JSON snapshot; commit the diff.
4. Regenerate the parity corpus; inspect the diff; update Java + this table per changed row.
