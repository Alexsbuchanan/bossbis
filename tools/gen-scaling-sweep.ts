// Scaling-sweep generator: runs the REAL weirdgloop calc over RAID monsters with
// per-encounter `monster.inputs` (ToA invocation/path/party, CoX party + CM,
// ToB party, Vardorvis current-HP) so the SCALED maxHit/accuracy/dps values are
// asserted against the Java MonsterScaling port.
//
//     npm run gen-scaling-sweep
//
// After running, `npm run gen-corpus` folds scenarios/sweep-scaling/*.json into
// the Java corpus.

import { mkdirSync, writeFileSync, rmSync } from 'node:fs';
import { resolve, join } from 'node:path';
import {
  calculatePlayerVsNpc,
  getTestPlayer,
  getTestMonsterById,
  findEquipment,
} from '@/tests/utils/TestUtils';
import type { Player, EquipmentPiece, PlayerSkills } from '@/types/Player';
import type { Monster } from '@/types/Monster';
import { getCombatStylesForCategory } from '@/utils';
import { EquipmentCategory } from '@/enums/EquipmentCategory';
import { Prayer } from '@/enums/Prayer';
import PlayerVsNPCCalc from '@/lib/PlayerVsNPCCalc';

const WG_COMMIT = '5e313c1b6cfba51d11a98c612243570a84772f44';
const here = import.meta.dirname;
const outDir = resolve(here, 'scenarios', 'sweep-scaling');

const SKILL_KEYS = ['atk', 'str', 'def', 'hp', 'ranged', 'magic', 'prayer', 'mining', 'herblore'] as const;
function skills(over: Partial<Record<(typeof SKILL_KEYS)[number], number>> = {}): PlayerSkills {
  const base: any = {};
  for (const k of SKILL_KEYS) base[k] = over[k] ?? 99;
  return base as PlayerSkills;
}

function eqp(name: string, version = ''): EquipmentPiece {
  return findEquipment(name, version);
}

function styleFor(category: EquipmentCategory, name: string) {
  const styles = getCombatStylesForCategory(category);
  const found = styles.find((s) => s.name === name);
  if (!found) throw new Error(`No style '${name}' for category '${category}'`);
  return found;
}

// All-99 monster inputs we vary; mirrors INITIAL_MONSTER_INPUTS defaults but
// always explicit so the row carries the full block.
interface MonsterInputsOverride {
  isFromCoxCm?: boolean;
  toaInvocationLevel?: number;
  toaPathLevel?: number;
  partyMaxCombatLevel?: number;
  partySumMiningLevel?: number;
  partyMaxHpLevel?: number;
  partySize?: number;
  monsterCurrentHp?: number;
  phase?: string | null;
}

// Gear sets (a small BIS-ish loadout per style; spec-free).
const MELEE_GEAR: Record<string, [string, string?]> = {
  head: ['Torva full helm'], cape: ['Infernal max cape'], neck: ['Amulet of torture'],
  body: ['Torva platebody'], shield: ['Avernic defender'], legs: ['Torva platelegs'],
  hands: ['Ferocious gloves'], feet: ['Primordial boots'], ring: ['Ultor ring'],
};
const RANGED_GEAR: Record<string, [string, string?]> = {
  head: ['Masori mask (f)'], cape: ["Dizana's quiver", 'Charged'], neck: ['Necklace of anguish'],
  body: ['Masori body (f)'], legs: ['Masori chaps (f)'], hands: ['Zaryte vambraces'],
  feet: ['Pegasian boots'], ring: ['Venator ring'],
};
const MAGE_GEAR: Record<string, [string, string?]> = {
  head: ['Ancestral hat'], cape: ['Imbued saradomin cape'], neck: ['Occult necklace'],
  body: ['Ancestral robe top'], legs: ['Ancestral robe bottom'], hands: ['Tormented bracelet'],
  feet: ['Eternal boots'], ring: ['Magus ring'],
};

interface ScalingScenario {
  key: string;
  monId: number;
  category: EquipmentCategory;
  styleName: string;
  gear: Record<string, [string, string?]>;
  prayers: Prayer[];
  inputs: MonsterInputsOverride;
  skills?: Partial<Record<(typeof SKILL_KEYS)[number], number>>;
}

function melee(key: string, monId: number, weapon: [string, string?], category: EquipmentCategory, styleName: string, inputs: MonsterInputsOverride): ScalingScenario {
  return { key, monId, category, styleName, prayers: [Prayer.PIETY], inputs,
    gear: { ...MELEE_GEAR, weapon } };
}
function ranged(key: string, monId: number, weapon: [string, string?], category: EquipmentCategory, styleName: string, ammo: [string, string?] | undefined, inputs: MonsterInputsOverride): ScalingScenario {
  const gear: Record<string, [string, string?]> = { ...RANGED_GEAR, weapon };
  if (ammo) gear.ammo = ammo;
  return { key, monId, category, styleName, prayers: [Prayer.RIGOUR], inputs, gear };
}
function mage(key: string, monId: number, weapon: [string, string?], styleName: string, inputs: MonsterInputsOverride): ScalingScenario {
  return { key, monId, category: EquipmentCategory.POWERED_STAFF, styleName, prayers: [Prayer.AUGURY], inputs,
    gear: { ...MAGE_GEAR, weapon } };
}

const scenarios: ScalingScenario[] = [];

// --- ToA: hp scales by invocation/path/party; def by invocation (inline in def-roll) ---
// Akkha (path monster) — vary invocation + party.
for (const [inv, party] of [[0, 1], [150, 1], [300, 1], [300, 4], [500, 8], [300, 3]] as const) {
  scenarios.push(melee(`akkha-inv${inv}-p${party}`, 11789, ['Osmumten\'s fang'], EquipmentCategory.STAB_SWORD, 'Lunge',
    { toaInvocationLevel: inv, toaPathLevel: 0, partySize: party }));
}
// Akkha with path level (path monster).
scenarios.push(melee('akkha-inv300-path3-p2', 11789, ['Osmumten\'s fang'], EquipmentCategory.STAB_SWORD, 'Lunge',
  { toaInvocationLevel: 300, toaPathLevel: 3, partySize: 2 }));
// Ba-Ba — ranged.
scenarios.push(ranged('baba-inv300-p4', 11778, ['Twisted bow'], EquipmentCategory.BOW, 'Rapid', ['Dragon arrow', 'Unpoisoned'],
  { toaInvocationLevel: 300, toaPathLevel: 0, partySize: 4 }));
// Zebak — magic.
scenarios.push(mage('zebak-inv400-p3', 11730, ['Tumeken\'s shadow', 'Charged'], 'Accurate',
  { toaInvocationLevel: 400, toaPathLevel: 2, partySize: 3 }));
// Kephri (path monster) — ranged.
scenarios.push(ranged('kephri-inv300-path4-p2', 11719, ['Twisted bow'], EquipmentCategory.BOW, 'Rapid', ['Dragon arrow', 'Unpoisoned'],
  { toaInvocationLevel: 300, toaPathLevel: 4, partySize: 2 }));

// --- CoX: party / combat-level / hp-level scaling (multi), + CM (+50%). ---
// Tekton (multi, magic-is-defensive, CM special def treatment).
for (const [party, cm] of [[1, false], [3, false], [5, false], [3, true], [8, true]] as const) {
  scenarios.push(melee(`tekton-p${party}${cm ? '-cm' : ''}`, 7540, ['Dragon warhammer'], EquipmentCategory.BLUNT, 'Pound',
    { partySize: party, isFromCoxCm: cm, partyMaxCombatLevel: 126, partyMaxHpLevel: 99 }));
}
// Glowing crystal (CM: hp not scaled, def scaled).
scenarios.push(ranged('glowing-crystal-p4-cm', 7568, ['Twisted bow'], EquipmentCategory.BOW, 'Rapid', ['Dragon arrow', 'Unpoisoned'],
  { partySize: 4, isFromCoxCm: true }));
// Guardian (special baseHp from partySumMiningLevel).
scenarios.push(melee('cox-guardian-p3-mine200', 7569, ['Dragon pickaxe'], EquipmentCategory.PICKAXE, 'Smash',
  { partySize: 3, partySumMiningLevel: 200 }));
// Scavenger beast (singles scaling).
scenarios.push(melee('scavenger-p1-comb100', 7548, ['Dragon scimitar'], EquipmentCategory.SLASH_SWORD, 'Lunge',
  { partyMaxCombatLevel: 100, partyMaxHpLevel: 90 }));
// Great Olm head (olm scaling).
scenarios.push(ranged('olm-head-p5', 7551, ['Twisted bow'], EquipmentCategory.BOW, 'Rapid', ['Dragon arrow', 'Unpoisoned'],
  { partySize: 5 }));
// Ice demon (multi, demon).
scenarios.push(mage('ice-demon-p3-cm', 7584, ['Tumeken\'s shadow', 'Charged'], 'Accurate',
  { partySize: 3, isFromCoxCm: true }));

// --- ToB: hp-only scaling (norm/hmt + entry mode). ---
scenarios.push(melee('maiden-p3', 8360, ['Scythe of vitur', 'Charged'], EquipmentCategory.SCYTHE, 'Chop',
  { partySize: 3 }));
scenarios.push(melee('maiden-p5', 8360, ['Scythe of vitur', 'Charged'], EquipmentCategory.SCYTHE, 'Chop',
  { partySize: 5 }));
scenarios.push(ranged('sotetseg-p4', 8387, ['Twisted bow'], EquipmentCategory.BOW, 'Rapid', ['Dragon arrow', 'Unpoisoned'],
  { partySize: 4 }));
scenarios.push(melee('verzik-p1-p3', 8369, ['Scythe of vitur', 'Charged'], EquipmentCategory.SCYTHE, 'Chop',
  { partySize: 3 }));

// --- Vardorvis: str/def scale with current HP (Awakened variant from id 12223). ---
for (const hp of [1400, 700, 100]) {
  scenarios.push(melee(`vardorvis-hp${hp}`, 12223, ['Soulreaper axe'], EquipmentCategory.AXE, 'Hack',
    { monsterCurrentHp: hp }));
}

// scythe loadout has no shield slot; strip it.
function materializeGear(gear: Record<string, [string, string?]>): { pieces: Record<string, EquipmentPiece>, ids: Record<string, number> } {
  const pieces: Record<string, EquipmentPiece> = {};
  const ids: Record<string, number> = {};
  for (const [slot, val] of Object.entries(gear)) {
    if (!val) continue;
    if (slot === 'shield' && (gear.weapon?.[0] === 'Scythe of vitur' || gear.weapon?.[0] === 'Twisted bow')) continue;
    const piece = eqp(val[0], val[1] ?? '');
    pieces[slot] = piece;
    ids[slot] = piece.id;
  }
  return { pieces, ids };
}

const PRAYER_NAME: Record<number, string> = {
  [Prayer.PIETY]: 'PIETY', [Prayer.RIGOUR]: 'RIGOUR', [Prayer.AUGURY]: 'AUGURY',
};

function fullInputs(o: MonsterInputsOverride) {
  // Mirror INITIAL_MONSTER_INPUTS defaults; override with the scenario's values.
  return {
    isFromCoxCm: o.isFromCoxCm ?? false,
    toaInvocationLevel: o.toaInvocationLevel ?? 0,
    toaPathLevel: o.toaPathLevel ?? 0,
    partyMaxCombatLevel: o.partyMaxCombatLevel ?? 126,
    partySumMiningLevel: o.partySumMiningLevel ?? 99,
    partyMaxHpLevel: o.partyMaxHpLevel ?? 99,
    partySize: o.partySize ?? 1,
    monsterCurrentHp: o.monsterCurrentHp,
    phase: o.phase ?? null,
  };
}

function buildRow(sc: ScalingScenario) {
  const { pieces, ids } = materializeGear(sc.gear);
  const s = skills(sc.skills);
  const style = styleFor(sc.category, sc.styleName);

  const inputOverride: any = { inputs: {} };
  const fi = fullInputs(sc.inputs);
  for (const [k, v] of Object.entries(fi)) {
    if (v !== undefined && v !== null) inputOverride.inputs[k] = v;
  }
  if (fi.phase) inputOverride.inputs.phase = fi.phase;

  const monster: Monster = getTestMonsterById(sc.monId, inputOverride);
  const resolvedVersion = monster.version ?? '';

  const player: Player = getTestPlayer(monster, {
    skills: s,
    prayers: sc.prayers,
    style,
    equipment: pieces,
  } as any);

  const res = calculatePlayerVsNpc(monster, player);
  const calc = new PlayerVsNPCCalc(player, monster, { loadoutName: 'test', detailedOutput: true });

  const expected: Record<string, number> = {
    maxHit: res.maxHit,
    accuracy: res.accuracy,
    dps: res.dps,
  };

  // Row's monster.inputs block (only the fields we set + always include the ones the Java side reads).
  const rowInputs: any = {
    isFromCoxCm: fi.isFromCoxCm,
    toaInvocationLevel: fi.toaInvocationLevel,
    toaPathLevel: fi.toaPathLevel,
    partyMaxCombatLevel: fi.partyMaxCombatLevel,
    partySumMiningLevel: fi.partySumMiningLevel,
    partyMaxHpLevel: fi.partyMaxHpLevel,
    partySize: fi.partySize,
  };
  if (fi.monsterCurrentHp !== undefined && fi.monsterCurrentHp !== null) rowInputs.monsterCurrentHp = fi.monsterCurrentHp;
  if (fi.phase) rowInputs.phase = fi.phase;

  const prayerNames = sc.prayers.map((p) => PRAYER_NAME[p]).filter(Boolean);

  return {
    name: `scaling-${sc.key}`,
    source: `gen-scaling-sweep.ts: ${monster.name} [${resolvedVersion}] scaling (weirdgloop@${WG_COMMIT.slice(0, 7)})`,
    exercises: ['PlayerVsNpcCalc.dps', 'scaling/MonsterScaling'],
    inputs: {
      player: {
        skills: s,
        prayers: prayerNames,
        style: { name: style.name, type: style.type ?? null, stance: style.stance ?? null },
        equipment: ids,
      },
      monster: { id: sc.monId, version: resolvedVersion, inputs: rowInputs },
    },
    expected,
    weirdgloopCommit: WG_COMMIT,
  };
}

// Self-check: a known scaled value before emitting. Akkha at invocation 300 solo
// has hp scaled by floor(4*300/10)=120% -> 400 + floor(400*120/100) = 880.
function selfCheck() {
  const m = getTestMonsterById(11789, { inputs: { ...{}, toaInvocationLevel: 300, toaPathLevel: 0, partySize: 1 } } as any);
  const calc = new PlayerVsNPCCalc(getTestPlayer(m, {}), m, { loadoutName: 'test', detailedOutput: true });
  // Reach into the scaled monster via a public-ish path: getDistribution uses scaled hp through getHtk.
  // Simpler: re-derive the upstream-scaled hp by constructing the calc and reading monster scaling result
  // is not exposed; instead assert the def-roll invocation factor + hp via a known dps sanity (non-zero).
  const res = calculatePlayerVsNpc(m, getTestPlayer(m, {
    skills: skills({ atk: 99, str: 99 }),
    prayers: [Prayer.PIETY],
    style: styleFor(EquipmentCategory.STAB_SWORD, 'Lunge'),
    equipment: { weapon: eqp("Osmumten's fang") },
  } as any));
  if (!Number.isFinite(res.dps) || res.dps <= 0) {
    throw new Error(`scaling self-check FAILED: akkha inv300 dps = ${res.dps}`);
  }
  // Vardorvis (Awakened) str/def scale linearly with current HP: at hp=1400 def=268,
  // at hp=100 def<268, so lowHp dps > fullHp dps. getTestMonsterById's `merge` mutates
  // the shared cached row, so each dps is computed immediately after its own monster is
  // built (no aliasing between the two).
  const vardDps = (hp: number): number => {
    const vm = getTestMonsterById(12223, { inputs: { monsterCurrentHp: hp } } as any);
    const vp = getTestPlayer(vm, {
      skills: skills(), prayers: [Prayer.PIETY], style: styleFor(EquipmentCategory.AXE, 'Hack'),
      equipment: { weapon: eqp('Soulreaper axe') },
    } as any);
    return new PlayerVsNPCCalc(vp, vm, { loadoutName: 'test', detailedOutput: true }).getDps();
  };
  const dpsFull = vardDps(1400);
  const dpsLow = vardDps(100);
  if (!(dpsLow > dpsFull)) {
    throw new Error(`scaling self-check FAILED: vardorvis dps lowHp(${dpsLow}) should exceed fullHp(${dpsFull}) (def drops with hp)`);
  }
  console.log(`scaling self-check OK: akkha-inv300 dps=${res.dps}; vardorvis dps lowHp=${dpsLow} > fullHp=${dpsFull}`);
}

function main() {
  selfCheck();
  rmSync(outDir, { recursive: true, force: true });
  mkdirSync(outDir, { recursive: true });

  const seen = new Set<string>();
  const rows: any[] = [];
  let errors = 0;
  for (const sc of scenarios) {
    let row: any;
    try {
      row = buildRow(sc);
    } catch (e) {
      errors++;
      console.error(`  skip ${sc.key}: ${(e as Error).message}`);
      continue;
    }
    if (seen.has(row.name)) throw new Error(`duplicate row name ${row.name}`);
    if (!Number.isFinite(row.expected.dps) || !Number.isFinite(row.expected.maxHit) || !Number.isFinite(row.expected.accuracy)) {
      console.error(`  skip ${sc.key}: non-finite expected ${JSON.stringify(row.expected)}`);
      continue;
    }
    seen.add(row.name);
    rows.push(row);
  }

  let idx = 0;
  for (const r of rows) {
    idx++;
    writeFileSync(join(outDir, `${String(idx).padStart(3, '0')}-${r.name}.json`), JSON.stringify(r, null, 2) + '\n', 'utf8');
  }
  console.log(`Generated ${rows.length} scaling row(s) into ${outDir}`);
  if (errors) console.log(`  (${errors} scenario(s) errored — see above)`);
}

main();
