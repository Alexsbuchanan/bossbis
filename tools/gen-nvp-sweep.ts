// NvP parity-sweep generator: enumerates a matrix of (monster ATTACKER x player
// DEFENDER) scenarios, runs the REAL weirdgloop NPCVsPlayerCalc (via TestUtils'
// calculateNpcVsPlayer + a direct NPCVsPlayerCalc for getAverageDamageTaken) on
// each, and emits one parity-corpus row per scenario under
// tools/scenarios/sweep-nvp/*.json with the upstream-asserted expected fields
// {playerDefRoll, npcMaxHit, npcMaxAttackRoll, npcAccuracy, npcDps, avgDmgTaken}
// and a kind:"nvp" discriminator.
//
// Run via the npm script (wires the same loader chain as gen-sweep):
//
//     npm run gen-nvp-sweep
//
// Each row carries the schema ParityCorpusTest consumes (kind:"nvp" branch).
// After running this, run `npm run gen-corpus` to fold sweep-nvp/*.json into the
// Java corpus.

import { mkdirSync, writeFileSync, rmSync } from 'node:fs';
import { resolve, join } from 'node:path';
import {
  getTestPlayer,
  getTestMonsterById,
  findEquipment,
  calculateNpcVsPlayer,
} from '@/tests/utils/TestUtils';
import NPCVsPlayerCalc from '@/lib/NPCVsPlayerCalc';
import type { Player, EquipmentPiece, PlayerSkills } from '@/types/Player';
import type { Monster } from '@/types/Monster';
import { getCombatStylesForCategory } from '@/utils';
import { EquipmentCategory } from '@/enums/EquipmentCategory';
import type { PlayerCombatStyle } from '@/types/PlayerCombatStyle';

const WG_COMMIT = '5e313c1b6cfba51d11a98c612243570a84772f44';
const here = import.meta.dirname;
const outDir = resolve(here, 'scenarios', 'sweep-nvp');

function eqp(name: string, version = ''): EquipmentPiece {
  return findEquipment(name, version);
}

const SKILL_KEYS = ['atk', 'str', 'def', 'hp', 'ranged', 'magic', 'prayer', 'mining', 'herblore'] as const;

function skills(over: Partial<Record<(typeof SKILL_KEYS)[number], number>> = {}): PlayerSkills {
  const base: any = {};
  for (const k of SKILL_KEYS) base[k] = over[k] ?? 99;
  return base as PlayerSkills;
}

function styleFor(category: EquipmentCategory, name: string): PlayerCombatStyle {
  const styles = getCombatStylesForCategory(category);
  const found = styles.find((s) => s.name === name);
  if (!found) {
    throw new Error(`No style '${name}' for category '${category}'. Have: ${styles.map((s) => s.name).join(',')}`);
  }
  return found;
}

// ---------------------------------------------------------------------------
// Monster attacker catalogue (clean single-style attackers across stab/slash/
// crush/ranged/magic, varied max hit + speed + special max-hit paths).
// ---------------------------------------------------------------------------

interface Attacker { id: number; tag: string; }

const attackers: Attacker[] = [
  { id: 415, tag: 'abyssal-demon-stab' },        // stab, mh 8
  { id: 7241, tag: 'abyssal-demon-cok-stab' },   // stab, mh 8 (different version)
  { id: 2267, tag: 'dagannoth-rex-slash' },      // slash, mh 26
  { id: 2586, tag: 'abyssal-walker-crush' },     // crush, mh 13
  { id: 6330, tag: 'nmz-dad-crush' },            // crush, big mh 57
  { id: 2265, tag: 'dagannoth-supreme-ranged' }, // ranged, mh 26
  { id: 2042, tag: 'zulrah-serpentine-ranged' }, // ranged, mh 41, speed 3
  { id: 2266, tag: 'dagannoth-prime-magic' },    // magic, wiki-override 26 -> 50
  { id: 492, tag: 'cave-kraken-magic' },         // magic, mh 13, speed 6
  { id: 443, tag: 'infernal-mage-magic' },       // magic, mh 8
  { id: 2, tag: 'aberrant-spectre-magic' },      // magic, wiki-override + slayer-helm reduced max hit
];

// ---------------------------------------------------------------------------
// Player defender catalogue. The player's WEAPON/style matters because
// getPlayerDefenceRoll reads the stance and the monster's attack style picks the
// defensive bonus; we vary defensive gear (tank/Justiciar/Elysian/Dinh's) and
// the slayer helmet (reduced max-hit branch).
// ---------------------------------------------------------------------------

interface Defender {
  key: string;
  desc: string;
  category: EquipmentCategory;
  styleName: string;
  gear: Record<string, [string, string?]>;
  skills?: Partial<Record<(typeof SKILL_KEYS)[number], number>>;
}

const TORVA_TANK: Record<string, [string, string?]> = {
  head: ['Torva full helm', 'Restored'],
  body: ['Torva platebody', 'Restored'],
  legs: ['Torva platelegs', 'Restored'],
  shield: ['Avernic defender', 'Normal'],
};

const JUSTICIAR: Record<string, [string, string?]> = {
  head: ['Justiciar faceguard'],
  body: ['Justiciar chestguard'],
  legs: ['Justiciar legguards'],
};

const defenders: Defender[] = [
  // Bare empty player (Punch / crush / Accurate). Lowest defence baseline.
  { key: 'empty', desc: 'Empty player (no gear)', category: EquipmentCategory.NONE, styleName: 'Punch', gear: {} },
  // Empty player in a Defensive stance (Block) — +3 stance bonus.
  { key: 'empty-block', desc: 'Empty player (Block / Defensive)', category: EquipmentCategory.NONE, styleName: 'Block', gear: {} },
  // Torva tank armour.
  { key: 'torva-tank', desc: 'Torva tank (Avernic defender)', category: EquipmentCategory.NONE, styleName: 'Punch', gear: TORVA_TANK },
  // Justiciar set — exercises the Justiciar damage-reduction transform.
  { key: 'justiciar', desc: 'Justiciar set', category: EquipmentCategory.NONE, styleName: 'Punch', gear: JUSTICIAR },
  // Elysian spirit shield — exercises the 70% reduced-by-max(1,trunc(d/4)) transform.
  { key: 'elysian', desc: 'Torva + Elysian spirit shield', category: EquipmentCategory.NONE, styleName: 'Punch',
    gear: { ...TORVA_TANK, shield: ['Elysian spirit shield'] } },
  // Dinh's bulwark in Block stance — exercises the x0.8 reduction.
  { key: 'dinhs-block', desc: "Dinh's bulwark (Block)", category: EquipmentCategory.BULWARK, styleName: 'Block',
    gear: { ...TORVA_TANK, weapon: ["Dinh's bulwark"], shield: undefined as any } },
  // Slayer helmet (i) — reduced max-hit branch for Aberrant spectre / Banshee / etc.
  { key: 'slayer-helm', desc: 'Slayer helmet (i) + tank', category: EquipmentCategory.NONE, styleName: 'Punch',
    gear: { ...TORVA_TANK, head: ['Slayer helmet (i)'] } },
  // Lower-defence player (L40 def) for accuracy spread.
  { key: 'lowdef', desc: 'Low defence (L40)', category: EquipmentCategory.NONE, styleName: 'Punch', gear: {}, skills: { def: 40 } },
];

// ---------------------------------------------------------------------------
// Build one corpus row from (attacker, defender).
// ---------------------------------------------------------------------------

interface CorpusRow {
  name: string;
  source: string;
  kind: 'nvp';
  exercises: string[];
  inputs: {
    player: {
      skills: PlayerSkills;
      prayers: string[];
      style: { name: string; type: string | null; stance: string | null };
      equipment: Record<string, number>;
    };
    monster: { id: number; version: string };
  };
  expected: Record<string, number>;
  weirdgloopCommit: string;
}

function buildRow(att: Attacker, def: Defender): CorpusRow {
  const gearPieces: Record<string, EquipmentPiece> = {};
  const gearIds: Record<string, number> = {};
  for (const [slot, val] of Object.entries(def.gear)) {
    if (!val) continue;
    const piece = eqp(val[0], val[1] ?? '');
    gearPieces[slot] = piece;
    gearIds[slot] = piece.id;
  }

  const s = skills(def.skills);
  const style = styleFor(def.category, def.styleName);

  const monster: Monster = getTestMonsterById(att.id);
  const resolvedVersion = monster.version ?? '';

  const overrides: any = {
    skills: s,
    style,
    equipment: gearPieces,
  };
  const player: Player = getTestPlayer(monster, overrides);

  const res = calculateNpcVsPlayer(monster, player);
  const calc = new NPCVsPlayerCalc(player, monster, { loadoutName: 'test' });
  const avg = calc.getAverageDamageTaken();

  const expected: Record<string, number> = {
    playerDefRoll: res.playerDefRoll,
    npcMaxHit: res.npcMaxHit,
    npcMaxAttackRoll: res.npcMaxAttackRoll,
    npcAccuracy: res.npcAccuracy,
    npcDps: res.npcDps,
  };
  if (avg !== undefined && Number.isFinite(avg)) {
    expected.avgDmgTaken = avg;
  }

  const name = `nvp-${att.tag}-${def.key}`;
  return {
    name,
    source: `gen-nvp-sweep.ts: ${def.desc} defending vs ${att.tag} (weirdgloop@${WG_COMMIT.slice(0, 7)})`,
    kind: 'nvp',
    exercises: ['NpcVsPlayerCalc.dps'],
    inputs: {
      player: {
        skills: s,
        prayers: [],
        style: { name: style.name, type: style.type ?? null, stance: style.stance ?? null },
        equipment: gearIds,
      },
      monster: { id: att.id, version: resolvedVersion },
    },
    expected,
    weirdgloopCommit: WG_COMMIT,
  };
}

// ---------------------------------------------------------------------------
// Self-check: reproduce a KNOWN nvp value (Abyssal demon 415 vs empty all-99
// player) before trusting the harness.
// ---------------------------------------------------------------------------

function selfCheck(): void {
  const monster = getTestMonsterById(415);
  const player = getTestPlayer(monster, {});
  const res = calculateNpcVsPlayer(monster, player);
  const want = {
    playerDefRoll: 6848,
    npcMaxHit: 8,
    npcMaxAttackRoll: 6784,
    npcAccuracy: 0.4952547817199591,
    npcDps: 0.8254246361999319,
  };
  for (const [k, v] of Object.entries(want)) {
    const got = (res as any)[k];
    if (typeof v === 'number' && Number.isInteger(v)) {
      if (got !== v) throw new Error(`self-check FAILED: ${k} = ${got}, expected ${v}`);
    } else if (Math.abs(got - v) > 1e-12) {
      throw new Error(`self-check FAILED: ${k} = ${got}, expected ${v}`);
    }
  }
  const calc = new NPCVsPlayerCalc(player, monster, { loadoutName: 'test' });
  const avg = calc.getAverageDamageTaken();
  if (avg === undefined || Math.abs(avg - 186.9070875073532) > 1e-9 * 186.9070875073532) {
    throw new Error(`self-check FAILED: avgDmgTaken = ${avg}, expected 186.9070875073532`);
  }
  console.log(`self-check OK: abyssal-demon-vs-empty playerDefRoll=${res.playerDefRoll} npcDps=${res.npcDps} avgDmgTaken=${avg}`);
}

// ---------------------------------------------------------------------------
// Main.
// ---------------------------------------------------------------------------

function main(): void {
  selfCheck();
  rmSync(outDir, { recursive: true, force: true });
  mkdirSync(outDir, { recursive: true });

  const rows: CorpusRow[] = [];
  const seen = new Set<string>();
  let errors = 0;

  for (const att of attackers) {
    for (const def of defenders) {
      let row: CorpusRow;
      try {
        row = buildRow(att, def);
      } catch (e) {
        errors++;
        console.error(`  skip ${att.tag}/${def.key}: ${(e as Error).message}`);
        continue;
      }
      if (seen.has(row.name)) throw new Error(`duplicate row name ${row.name}`);
      seen.add(row.name);
      rows.push(row);
    }
  }

  let idx = 0;
  for (const r of rows) {
    idx++;
    const file = join(outDir, `${String(idx).padStart(3, '0')}-${r.name}.json`);
    writeFileSync(file, JSON.stringify(r, null, 2) + '\n', 'utf8');
  }

  console.log(`Generated ${rows.length} nvp sweep row(s) into ${outDir}`);
  if (errors) console.log(`  (${errors} attacker/defender combos errored — see above)`);
}

main();
