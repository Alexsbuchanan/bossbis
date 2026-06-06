# Port State

Tracks which TypeScript sources from `weirdgloop/osrs-dps-calc` are ported to Java, at which upstream commit each Java class is synchronized, and caveats.

**Upstream pinned commit (dev submodule `tools/upstream`):** `5e313c1b6cfba51d11a98c612243570a84772f44`

| Java class | TypeScript source | Synced @ | Status | Notes |
|---|---|---|---|---|
| calc/BaseCalc.java | src/lib/BaseCalc.ts | 5e313c1 | partial | accuracy machinery: constructor/init (canonicalize + allEquippedItems + sanitizeInputs), all 48 isWearingX/isUsingX predicates + wearing/wearingAll, pure track* helpers (CalcDetails dropped), and the 4 strictfp accuracy-roll statics (getNormalAccuracyRoll/getFangAccuracyRoll/getConflictionGauntletsAccuracyRoll/getMaxAccuracyHitChance, verbatim); max-hit/distribution pending (v0.1.2+). Reads pre-aggregated Player offensive/bonuses/defensive (no re-aggregation — matches upstream). scaleMonster deferred to M5 (identity placeholder, TODO(M5)). leagues disjuncts dropped (spec §4.2); spec-attack stance reset throws TODO(M3) (needs getCombatStylesForCategory; usingSpecialAttack defaults false) |
| calc/PlayerVsNpcCalc.java | src/lib/PlayerVsNPCCalc.ts | 5e313c1 | partial: attack rolls + accuracy | getMaxAttackRoll dispatch + getPlayerMaxMeleeAttackRoll/getPlayerMaxRangedAttackRoll/getPlayerMaxMagicAttackRoll (with ordered accuracy-only special cases) + helpers (getCombatPrayers, demonbaneFactor via applyDemonbane, tbowScaling, isBindSpell, getSpellement, getMonsterWeakness). All max-hit/distribution/DPS/TTK/spec methods stubbed -> UnsupportedOperationException("not ported until v0.1.2+"). getNPCDefenceRoll/getHitChance also stubbed (M5/M6). leagues disjuncts dropped (spec §4.2). The 3 maxAttackRoll corpus rows (whip 16060 / bofa 21120 / trident 8690) assert exactly. |
| calc/NpcVsPlayerCalc.java | src/lib/NPCVsPlayerCalc.ts | — | not-ported | |
| calc/HitDist.java | src/lib/HitDist.ts | — | not-ported | getHash via BigInteger; verbatim 0x8F000000 mask |
| calc/Equipment.java | src/lib/Equipment.ts | 5e313c1 | ported | calculateEquipmentBonusesFromGear + calculateAttackSpeed + ammoApplicability/ammoForRangedWeapons + getCanonicalItem/Equipment (via injected EquipmentRepository); ordered special cases (blowpipe dart, Crystal blessing, Tumeken's ×3/×4 cap 1000, Keris penalty, Dinh's bulwark, Virtus, Void mage, Dizana's quiver); all leagues-talent branches dropped (model has no leagues, spec §4.2) |
| calc/Comparator.java | src/lib/Comparator.ts | — | not-ported | uses lodash.mergewith |
| calc/MonsterScaling.java | src/lib/MonsterScaling.ts | — | not-ported | |
| calc/CalcDetails.java | src/lib/CalcDetails.ts | — | not-ported | |
| calc/CalcMath.java | src/lib/Math.ts | 5e313c1 | ported | iSqrt, iLerp, MinMax, Factor |
| calc/Constants.java | src/lib/constants.ts | 5e313c1 | partial | scalars + BLOWPIPE_IDS, AUTOCAST/CAST_STANCES, TOMBS_OF_AMASCUT_(PATH_)MONSTER_IDS (ported with the Equipment consumer), YAMA_IDS/YAMA_VOID_FLARE_IDS + IMMUNE_TO_BURN_DAMAGE_NPC_IDS (ported with the BaseCalc consumer), P2_WARDEN_IDS_SET + KEPHRI_OVERLORD_IDS_SET + TITAN_BOSS_IDS (ported with the PlayerVsNpcCalc attack-roll consumer); remaining ID arrays deferred to later v0.1.1 milestones |
| calc/data/EquipmentAliases.java | src/lib/EquipmentAliases.ts | 5e313c1 | ported | flat {variant:base} map; canonical(id)=getOrDefault(id,id) (O(1) equiv of getCanonicalItemId) |
| calc/data/EquipmentRepository.java | src/lib/Equipment.ts:202-226 | 5e313c1 | ported | fromBundled(Gson) indexes equipment.json by id (putIfAbsent guards dup id 32640); canonicalId + resolve (canonical→literal→empty); loads all 5306 rows |
| calc/data/SlotMapping.java | §6.4 (slot strings + 2H rule) | 5e313c1 | ported | Slot enum {HEAD,CAPE,NECK,AMMO,WEAPON,BODY,SHIELD,LEGS,HANDS,FEET,RING}; fromWeirdgloop(String); occupies(slot,2H)→{WEAPON,SHIELD} for 2H weapon else slot |
| calc/data/MonsterRepository.java | src/lib/Monsters.ts | 5e313c1 | ported | fromBundled(Gson) indexes monsters.json by (id,version) and by id (first wins); resolve(id,version) prefers exact then id-fallback; loads all 2830 rows |
| calc/data/SpellRepository.java | src/lib/spells.json | 5e313c1 | ported | fromBundled(Gson) indexes spells.json by name; byName(String) |
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
| calc/state/BoostsCalculator.java | src/utils.ts PotionMap + src/state.tsx recomputeBoosts | BoostsCalculatorTest | ported | per-skill boost deltas; max-combine; all 22 potions |
| calc/types/Prayer.java | src/enums/Prayer.ts | 5e313c1 | ported | numeric enum 0..30; ordinal == upstream value; PrayerData + PrayerMap factors |
| calc/types/Potion.java | src/enums/Potion.ts | 5e313c1 | ported | plain ordinal enum (22 members); no JSON |
| calc/types/EquipmentCategory.java | src/enums/EquipmentCategory.ts | 5e313c1 | ported | string-valued (32 members) + fromValue |
| calc/types/MonsterAttribute.java | src/enums/MonsterAttribute.ts | 5e313c1 | ported | string-valued (16 members incl. vampyre1/2/3) + fromValue + isVampyre (instance + static isVampyre(Iterable) overload, ported with the PlayerVsNpcCalc consumer) |
| calc/types/BurnImmunity.java | src/types/Monster.ts:9-13 | 5e313c1 | ported | string-valued (Weak/Normal/Strong) + fromValue |
| calc/types/UserIssueType.java | src/enums/UserIssueType.ts | 5e313c1 | ported | string-valued (12 members) + fromValue |
| calc/types/CombatStyle.java | src/types/PlayerCombatStyle.ts | 5e313c1 | ported | PlayerCombatStyle + CombatStyleType/Stance (nullable strings) + getRangedDamageType |
| calc/types/Spell.java | src/types/Spell.ts | 5e313c1 | ported | Spell @Value (max_hit) + Spellbook + Spellement |
| calc/types/Player model | src/types/Player.ts | 5e313c1 | ported | PlayerSkills(+mining/herblore)/PlayerBonuses(ranged_str,magic_str)/PlayerOffensive/PlayerDefensive/EquipmentStats/ItemVars/EquipmentPiece(inlined stats)/PlayerEquipment/Buffs(no potions)/Player |
| calc/types/Monster model | src/types/Monster.ts + src/lib/Monsters.ts:32-58 | 5e313c1 | ported | Monster(is_slayer_monster, maxHit UI-only)/MonsterSkills/MonsterOffensive(magic_str,ranged_str)/MonsterDefensive(flat_armour,light/standard/heavy)/MonsterInputs.initial()=INITIAL_MONSTER_INPUTS/DefenceReductions/MonsterPrayers/Weakness/Immunities |
| (test) parity harness | src/tests/calc/* (transcribed) | 5e313c1 | ported | CorpusRow/PortState + ScenarioPlayer (corpus row -> calc Player+Monster, mirrors getTestPlayer) + ParityCorpusTest (computes+asserts ported rows at §5.3 tolerance, skips un-ported) + PortStatusTest gate; corpus seeded with 9 rows; at v0.1.1 the PlayerVsNpcCalc.accuracy path is ported so the 3 maxAttackRoll rows assert exactly, the 6 maxHit rows skip |

## Parity corpus harness (v0.1.0)

The parity-corpus harness is in place and seeded with **9 rows transcribed from
upstream's Jest tests** (`tools/upstream/src/tests/calc/`):

- 6 `maxHit` rows from `GeneratedTests.test.ts` (Osmumten's fang =50, Dragon
  hunter lance =58, Blisterwood flail =55, Obsidian sword =46, Tumeken's shadow
  =65, Fire bolt =20).
- 3 `maxAttackRoll` (accuracy) rows from `BasicRolls.test.ts` (Abyssal whip
  =16060, Bow of faerdhinen =21120, Trident of the seas =8690).

`tools/gen-reference-corpus.ts` validates `tools/scenarios/*.json` and writes
them (sorted by name) to `src/test/resources/parity/parity-corpus.json`.
`src/test/resources/parity/port-state.json` is the machine-readable companion to
this file. At **v0.1.1** the `PlayerVsNpcCalc.accuracy` path (player attack rolls)
is ported, so `ScenarioPlayer` builds each `accuracy` row into a calc-ready
Player+Monster and `ParityCorpusTest` asserts its `maxAttackRoll` exactly: the 3
attack-roll rows RUN and PASS (whip 16060, BOFA 21120, trident 8690); the 6
`maxHit` rows still SKIP (their `maxMeleeHit`/`maxMagicHit` paths are
`not-ported`). `PortStatusTest` locks that exactly 3 rows run and 6 skip. The
remaining `*Calc` / `HitDist` / `scaling/*` / `dists/*` paths are still
`not-ported`. Automated generation from the live upstream calc remains deferred
(requires stubbing weirdgloop's PNG/asset imports, per `tools/README.md`).

## Status values
- `not-ported` — no Java equivalent yet.
- `partial` — Java exists but does not fully cover the TS source (see Notes).
- `ported` — Java mirror complete and parity-tested against `Synced @`.

## Bump workflow
1. `cd tools/upstream && git checkout <new-commit>`
2. `cd ../.. && git add tools/upstream && git commit -m "sync: weirdgloop -> <new-commit>"`
3. `tools/update-data.sh` to refresh the committed JSON snapshot; commit the diff.
4. Regenerate the parity corpus; inspect the diff; update Java + this table per changed row.
