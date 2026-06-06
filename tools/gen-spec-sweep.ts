// Spec-sweep generator: runs the REAL weirdgloop calc with usingSpecialAttack=true
// (via getSpecCalc()) for weapons with supported special attacks, asserting the
// SPEC maxHit/accuracy/dps against the Java spec port (getSpecCalc()/getSpecDps()).
//
//     npm run gen-spec-sweep
//
// After running, `npm run gen-corpus` folds scenarios/sweep-spec/*.json into the
// Java corpus. Rows carry a top-level `spec: true` discriminator.

import { mkdirSync, writeFileSync, rmSync } from 'node:fs';
import { resolve, join } from 'node:path';
import {
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
const outDir = resolve(here, 'scenarios', 'sweep-spec');

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

const MELEE_GEAR: Record<string, [string, string?]> = {
  head: ['Torva full helm'], cape: ['Infernal max cape'], neck: ['Amulet of torture'],
  body: ['Torva platebody'], shield: ['Avernic defender'], legs: ['Torva platelegs'],
  hands: ['Ferocious gloves'], feet: ['Primordial boots'], ring: ['Ultor ring'],
};

interface SpecScenario {
  key: string;
  weapon: [string, string?];
  category: EquipmentCategory;
  styleName: string;
  monId: number;
  prayers: Prayer[];
  ring?: [string, string?]; // e.g. Lightbearer to vary spec regen
}

function s2(key: string, weapon: [string, string?], category: EquipmentCategory, styleName: string, monId: number, ring?: [string, string?]): SpecScenario {
  return { key, weapon, category, styleName, monId, prayers: [Prayer.PIETY], ring };
}

// Common target: Abyssal demon (415) and a high-HP target (General Graardor 2215)
// so spec dps regen + dist scaling are exercised against different defences.
const ADEMON = 415;
const GRAARDOR = 2215;

const scenarios: SpecScenario[] = [
  s2('dwh-ademon', ['Dragon warhammer'], EquipmentCategory.BLUNT, 'Pound', ADEMON),
  s2('dwh-graardor', ['Dragon warhammer'], EquipmentCategory.BLUNT, 'Pound', GRAARDOR),
  s2('bgs-graardor', ['Bandos godsword'], EquipmentCategory.TWO_HANDED_SWORD, 'Slash', GRAARDOR),
  s2('ags-ademon', ['Armadyl godsword'], EquipmentCategory.TWO_HANDED_SWORD, 'Slash', ADEMON),
  s2('zgs-ademon', ['Zamorak godsword'], EquipmentCategory.TWO_HANDED_SWORD, 'Slash', ADEMON),
  s2('sgs-graardor', ['Saradomin godsword'], EquipmentCategory.TWO_HANDED_SWORD, 'Slash', GRAARDOR),
  s2('dclaws-ademon', ['Dragon claws'], EquipmentCategory.CLAW, 'Slash', ADEMON),
  s2('dclaws-graardor', ['Dragon claws'], EquipmentCategory.CLAW, 'Slash', GRAARDOR),
  s2('ddagger-ademon', ['Dragon dagger', 'Unpoisoned'], EquipmentCategory.STAB_SWORD, 'Lunge', ADEMON),
  s2('abyssaldagger-ademon', ['Abyssal dagger', 'Unpoisoned'], EquipmentCategory.STAB_SWORD, 'Lunge', ADEMON),
  s2('voidwaker-graardor', ['Voidwaker'], EquipmentCategory.SLASH_SWORD, 'Slash', GRAARDOR),
  s2('voidwaker-ademon', ['Voidwaker'], EquipmentCategory.SLASH_SWORD, 'Slash', ADEMON),
  s2('dlong-ademon', ['Dragon longsword'], EquipmentCategory.SLASH_SWORD, 'Slash', ADEMON),
  s2('dmace-ademon', ['Dragon mace'], EquipmentCategory.SPIKED, 'Pummel', ADEMON),
  s2('granitehammer-ademon', ['Granite hammer'], EquipmentCategory.BLUNT, 'Pound', ADEMON),
  s2('saradominsword-graardor', ['Saradomin sword'], EquipmentCategory.TWO_HANDED_SWORD, 'Slash', GRAARDOR),
  s2('eldermaul-graardor', ['Elder maul'], EquipmentCategory.BLUNT, 'Pound', GRAARDOR),
  // Lightbearer halves spec regen (different specDps from the same loadout).
  s2('dwh-ademon-lightbearer', ['Dragon warhammer'], EquipmentCategory.BLUNT, 'Pound', ADEMON, ['Lightbearer']),
  s2('bgs-graardor-lightbearer', ['Bandos godsword'], EquipmentCategory.TWO_HANDED_SWORD, 'Slash', GRAARDOR, ['Lightbearer']),
];

const PRAYER_NAME: Record<number, string> = { [Prayer.PIETY]: 'PIETY' };

function buildRow(sc: SpecScenario) {
  const gear: Record<string, [string, string?]> = { ...MELEE_GEAR, weapon: sc.weapon };
  if (sc.ring) gear.ring = sc.ring;
  // 2H weapons occupy the shield slot.
  if (sc.category === EquipmentCategory.TWO_HANDED_SWORD || sc.weapon[0] === 'Dragon claws') {
    delete gear.shield;
  }

  const pieces: Record<string, EquipmentPiece> = {};
  const ids: Record<string, number> = {};
  for (const [slot, val] of Object.entries(gear)) {
    if (!val) continue;
    const piece = eqp(val[0], val[1] ?? '');
    pieces[slot] = piece;
    ids[slot] = piece.id;
  }

  const s = skills();
  const style = styleFor(sc.category, sc.styleName);
  const monster: Monster = getTestMonsterById(sc.monId);
  const resolvedVersion = monster.version ?? '';

  const player: Player = getTestPlayer(monster, {
    skills: s, prayers: sc.prayers, style, equipment: pieces,
  } as any);

  const baseCalc = new PlayerVsNPCCalc(player, monster, { loadoutName: 'test', detailedOutput: true });
  const specCalc = baseCalc.getSpecCalc();
  if (!specCalc) {
    throw new Error(`getSpecCalc() returned null (spec unsupported) for ${sc.weapon[0]}`);
  }

  const expected: Record<string, number> = {
    specMaxHit: specCalc.getDistribution().getMax(),
    specAccuracy: specCalc.getHitChance(),
    specDps: baseCalc.getSpecDps(),
  };

  const prayerNames = sc.prayers.map((p) => PRAYER_NAME[p]).filter(Boolean);

  return {
    name: `spec-${sc.key}`,
    spec: true,
    source: `gen-spec-sweep.ts: ${sc.weapon[0]} spec vs ${monster.name} (weirdgloop@${WG_COMMIT.slice(0, 7)})`,
    exercises: ['PlayerVsNpcCalc.spec'],
    inputs: {
      player: {
        skills: s,
        prayers: prayerNames,
        style: { name: style.name, type: style.type ?? null, stance: style.stance ?? null },
        equipment: ids,
      },
      monster: { id: sc.monId, version: resolvedVersion },
    },
    expected,
    weirdgloopCommit: WG_COMMIT,
  };
}

// Self-check: DWH spec vs Abyssal demon should produce a known spec max hit.
// DWH spec deals +50% damage and reduces def by 30% on hit; assert the spec calc
// returns a finite, sane spec max hit and a non-zero spec dps.
function selfCheck() {
  const m = getTestMonsterById(ADEMON);
  const player = getTestPlayer(m, {
    skills: skills(), prayers: [Prayer.PIETY],
    style: styleFor(EquipmentCategory.BLUNT, 'Pound'),
    equipment: { weapon: eqp('Dragon warhammer') },
  } as any);
  const base = new PlayerVsNPCCalc(player, m, { loadoutName: 'test', detailedOutput: true });
  const spec = base.getSpecCalc();
  if (!spec) throw new Error('spec self-check FAILED: DWH getSpecCalc() null');
  const specMax = spec.getDistribution().getMax();
  const specDps = base.getSpecDps();
  if (!Number.isFinite(specMax) || specMax <= 0) throw new Error(`spec self-check FAILED: DWH specMax=${specMax}`);
  if (!Number.isFinite(specDps) || specDps <= 0) throw new Error(`spec self-check FAILED: DWH specDps=${specDps}`);

  // Voidwaker spec is a guaranteed-accuracy magic-style hit; specAccuracy should be 1.
  const vw = getTestMonsterById(GRAARDOR);
  const vwPlayer = getTestPlayer(vw, {
    skills: skills(), prayers: [Prayer.PIETY],
    style: styleFor(EquipmentCategory.SLASH_SWORD, 'Slash'),
    equipment: { weapon: eqp('Voidwaker') },
  } as any);
  const vwSpec = new PlayerVsNPCCalc(vwPlayer, vw, { loadoutName: 'test', detailedOutput: true }).getSpecCalc();
  if (!vwSpec) throw new Error('spec self-check FAILED: Voidwaker getSpecCalc() null');
  const vwAcc = vwSpec.getHitChance();
  if (Math.abs(vwAcc - 1.0) > 1e-12) throw new Error(`spec self-check FAILED: Voidwaker specAccuracy=${vwAcc} (expected 1.0)`);

  console.log(`spec self-check OK: DWH specMax=${specMax}, specDps=${specDps}; Voidwaker specAccuracy=${vwAcc}`);
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
    if (!Number.isFinite(row.expected.specDps) || !Number.isFinite(row.expected.specMaxHit) || !Number.isFinite(row.expected.specAccuracy)) {
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
  console.log(`Generated ${rows.length} spec row(s) into ${outDir}`);
  if (errors) console.log(`  (${errors} scenario(s) errored — see above)`);
}

main();
