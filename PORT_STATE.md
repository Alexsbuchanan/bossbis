# Port State

Tracks which TypeScript sources from `weirdgloop/osrs-dps-calc` are ported to Java, at which upstream commit each Java class is synchronized, and caveats.

**Upstream pinned commit (dev submodule `tools/upstream`):** `5e313c1b6cfba51d11a98c612243570a84772f44`

| Java class | TypeScript source | Synced @ | Status | Notes |
|---|---|---|---|---|
| calc/BaseCalc.java | src/lib/BaseCalc.ts | 5e313c1 | partial | accuracy machinery: constructor/init (canonicalize + allEquippedItems + sanitizeInputs), all 48 isWearingX/isUsingX predicates + wearing/wearingAll, pure track* helpers (CalcDetails dropped), and the 4 strictfp accuracy-roll statics (getNormalAccuracyRoll/getFangAccuracyRoll/getConflictionGauntletsAccuracyRoll/getMaxAccuracyHitChance, verbatim); max-hit/distribution pending (v0.1.2+). Reads pre-aggregated Player offensive/bonuses/defensive (no re-aggregation — matches upstream). scaleMonster deferred to M5 (identity placeholder, TODO(M5)). leagues disjuncts dropped (spec §4.2); spec-attack stance reset throws TODO(M3) (needs getCombatStylesForCategory; usingSpecialAttack defaults false) |
| calc/PlayerVsNpcCalc.java | src/lib/PlayerVsNPCCalc.ts | 5e313c1 | partial: accuracy pipeline (no max-hit/distribution) | getMaxAttackRoll dispatch + getPlayerMaxMeleeAttackRoll/getPlayerMaxRangedAttackRoll/getPlayerMaxMagicAttackRoll (with ordered accuracy-only special cases) + getNPCDefenceRoll (verbatim PlayerVsNPCCalc.ts:147-205: spec defenceStyle overrides kept, magic-vs-def level via USES_DEFENCE_LEVEL_FOR_MAGIC_DEFENCE_NPC_IDS, effectiveLevel=level+9, ranged light/standard/heavy split + mixed=avg, statBonus=bonus+64 (0 if styleless), defenceRoll=trackFactor(effLevel,[statBonus,1]) long, then ToA invocation factor [250+inv,250]; returns long) + getHitChance/getDisplayHitChance (verbatim PlayerVsNPCCalc.ts:1239-1373: GUARANTEED_ACCURACY_MONSTERS/DOOM_OF_MOKHAIOTL/VERZIK_P1+Dawnbringer/P2_WARDEN/Scurrius rat/Tormented Demon/Royal-Titan elementals/Eclipse-Moon clone/ALWAYS_MAX_HIT_MONSTERS/spec Voidwaker+Dawnbringer/Seercull+Mlb guaranteed-accuracy guards in source order, then getNormalAccuracyRoll(atk,def) with the Fang (getFangAccuracyRoll; ToA squared variant 1-(1-h)^2) and Confliction-gauntlets (getConflictionGauntletsAccuracyRoll, non-2H magic) paths; getDisplayHitChance adds the Brimstone-ring magic 0.75/0.25 blend) + helpers (getCombatPrayers, demonbaneFactor via applyDemonbane, tbowScaling, isBindSpell, getSpellement, getMonsterWeakness). max-hit/distribution/DPS/TTK/spec methods stubbed -> UnsupportedOperationException (v0.1.2+). leagues disjuncts dropped (spec §4.2; incl. the crossbow double-accuracy + max-accuracy-from-range hit-chance talents). overrides.accuracy short-circuit dropped (no overrides model). The 3 maxAttackRoll corpus rows (whip 16060 / bofa 21120 / trident 8690), the 3 npcDefRoll rows (abyssal demon plain 12096 / +1 DWH 8736 / +50 BGS 7896), and the 2 accuracy rows (empty-player vs abyssal demon 0.29098123501694634 — CombatCalc.test.ts; Osmumten's fang vs abyssal demon 0.9605482099995817 — Fang accuracy roll) assert at §5.3 tolerance. |
| calc/NpcVsPlayerCalc.java | src/lib/NPCVsPlayerCalc.ts | — | not-ported | |
| calc/HitDist.java | src/lib/HitDist.ts | — | not-ported | getHash via BigInteger; verbatim 0x8F000000 mask |
| calc/Equipment.java | src/lib/Equipment.ts | 5e313c1 | ported | calculateEquipmentBonusesFromGear + calculateAttackSpeed + ammoApplicability/ammoForRangedWeapons + getCanonicalItem/Equipment (via injected EquipmentRepository); ordered special cases (blowpipe dart, Crystal blessing, Tumeken's ×3/×4 cap 1000, Keris penalty, Dinh's bulwark, Virtus, Void mage, Dizana's quiver); all leagues-talent branches dropped (model has no leagues, spec §4.2) |
| calc/Comparator.java | src/lib/Comparator.ts | — | not-ported | uses lodash.mergewith |
| calc/MonsterScaling.java | src/lib/MonsterScaling.ts | 5e313c1 | partial: defence reductions; raid stat-scaling deferred | scaleMonster order-of-operations shell; only the applyDefenceReductions transform is ported. The raid party/HP/level stat-scaling transforms (applyCoxScaling/applyTobScaling/applyToaScaling/applyVardScaling/applyMonsterPhases) are stubbed with TODO(later) markers (deferred to the raid-encounters phase). NB the ToA *defence* invocation factor is applied inline in PlayerVsNpcCalc.getNPCDefenceRoll, not here. scaleMonsterHpOnly stubbed (Vardorvis HP-scaling deferred). Wired into the BaseCalc constructor (replacing the M2 identity stub), guarded by !disableMonsterScaling; baseMonster stays unscaled. |
| calc/CalcDetails.java | src/lib/CalcDetails.ts | — | not-ported | |
| calc/CalcMath.java | src/lib/Math.ts | 5e313c1 | ported | iSqrt, iLerp, MinMax, Factor |
| calc/Constants.java | src/lib/constants.ts | 5e313c1 | partial | scalars + BLOWPIPE_IDS, AUTOCAST/CAST_STANCES, TOMBS_OF_AMASCUT_(PATH_)MONSTER_IDS (ported with the Equipment consumer), YAMA_IDS/YAMA_VOID_FLARE_IDS + IMMUNE_TO_BURN_DAMAGE_NPC_IDS (ported with the BaseCalc consumer), P2_WARDEN_IDS_SET + KEPHRI_OVERLORD_IDS_SET + TITAN_BOSS_IDS (ported with the PlayerVsNpcCalc attack-roll consumer); USES_DEFENCE_LEVEL_FOR_MAGIC_DEFENCE_NPC_IDS (ICE_DEMON + VERZIK + FRAGMENT_OF_SEREN + baboon brawler + prifddinas rabbit) ported with getNPCDefenceRoll; GUARANTEED_ACCURACY_MONSTERS + DOOM_OF_MOKHAIOTL_IDS + VERZIK_P1_IDS_SET + TITAN_ELEMENTAL_IDS + ECLIPSE_MOON_IDS + ALWAYS_MAX_HIT_MONSTERS_(MELEE/RANGED/MAGIC) ported with getHitChance; VERZIK_IDS/VARDORVIS_IDS/SOTETSEG_IDS/NIGHTMARE_IDS/NEX_IDS/ARAXXOR_IDS/HUEYCOATL_IDS + AKKHA/BABA/KEPHRI/ZEBAK/TOA_OBELISK/P3_WARDEN _SET ported with DefenceReduction.getDefenceFloor; remaining ID arrays deferred to later v0.1.1 milestones |
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
| calc/scaling/DefenceReduction.java | src/lib/scaling/DefenceReduction.ts | 5e313c1 | ported | applyDefenceReductions (verbatim order: accursed/vulnerability, elderMaul, dwh, arclight, emberlight, tonalztic, seercull, bgs cascading def->str->atk->magic->ranged, ayak) + getDefenceFloor; integer-truncating; the per-skill def floor honoured. Guards a null inputs/defenceReductions block (unassembled bundled rows) as identity. |
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
| (test) parity harness | src/tests/calc/* (transcribed) | 5e313c1 | ported | CorpusRow/PortState + ScenarioPlayer (corpus row -> calc Player+Monster, mirrors getTestPlayer) + ParityCorpusTest (computes+asserts ported rows at §5.3 tolerance, skips un-ported) + PortStatusTest gate; corpus seeded with 14 rows; at v0.1.1 the PlayerVsNpcCalc.accuracy + PlayerVsNpcCalc.defenceRoll paths are ported so the 3 maxAttackRoll rows + 3 npcDefRoll rows + 2 accuracy (hit-chance) rows assert (the 2 accuracy rows map accuracy -> getHitChance; float relative tolerance ≤1e-9), the 6 maxHit rows skip. HitChanceTest unit-locks getHitChance==getNormalAccuracyRoll(atk,def) for plain melee and getDisplayHitChance==getHitChance without Brimstone-magic. ScenarioPlayer + CorpusRow extended so a row may carry an optional monster.inputs block (ToA invocation level + defence reductions; defaults to MonsterInputs.initial() when absent) |

## Parity corpus harness (v0.1.0)

The parity-corpus harness is in place and seeded with **14 rows transcribed from
(or generated against) upstream's Jest tests** (`tools/upstream/src/tests/calc/`):

- 6 `maxHit` rows from `GeneratedTests.test.ts` (Osmumten's fang =50, Dragon
  hunter lance =58, Blisterwood flail =55, Obsidian sword =46, Tumeken's shadow
  =65, Fire bolt =20).
- 3 `maxAttackRoll` (accuracy) rows from `BasicRolls.test.ts` (Abyssal whip
  =16060, Bow of faerdhinen =21120, Trident of the seas =8690).
- 3 `npcDefRoll` (defenceRoll) rows from `DefenceRolls.test.ts` (abyssal demon
  plain 12096, +1 DWH 8736, +50 BGS 7896).
- 2 `accuracy` (hit-chance) rows: empty player vs abyssal demon (rolls 7040/12096
  upstream-asserted in `CombatCalc.test.ts`; expected hit chance 0.29098123501694634
  via upstream `getNormalAccuracyRoll`, which reproduces that test's 29.10%), and
  Osmumten's fang vs abyssal demon (the `GeneratedTests.test.ts` fang loadout;
  expected 0.9605482099995817 via upstream `getFangAccuracyRoll` over the rolls
  35164/12096). The two hit-chance expected values were tsx-generated from the
  pure, dependency-free accuracy-roll statics copied verbatim from `BaseCalc.ts`
  (the full calc graph pulls in `axios`/React via `utils.ts`/`state.tsx`, so only
  the pure lib pieces are run — confirmed against the known 29.10% anchor).

`tools/gen-reference-corpus.ts` validates `tools/scenarios/*.json` and writes
them (sorted by name) to `src/test/resources/parity/parity-corpus.json`.
`src/test/resources/parity/port-state.json` is the machine-readable companion to
this file. At **v0.1.1** the `PlayerVsNpcCalc.accuracy` (player attack rolls +
hit chance) and `PlayerVsNpcCalc.defenceRoll` (getNPCDefenceRoll + defence
reductions) paths are ported, so `ScenarioPlayer` builds each such row into a
calc-ready Player+Monster and `ParityCorpusTest` asserts it at the §5.3 tolerance:
the 3 attack-roll rows RUN and PASS (whip 16060, BOFA 21120, trident 8690), the 3
defence-roll rows RUN and PASS (12096 / 8736 / 7896), and the 2 accuracy rows RUN
and PASS (0.29098123501694634 / 0.9605482099995817, float relative ≤1e-9); the 6
`maxHit` rows still SKIP (their `maxMeleeHit`/`maxMagicHit` paths are
`not-ported`). `PortStatusTest` locks that exactly 8 rows run and 6 skip. The
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
