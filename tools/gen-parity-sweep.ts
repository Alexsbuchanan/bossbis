// Parity-sweep generator: enumerates a BROAD matrix of (weapon loadout × monster
// × buff) scenarios, runs the REAL weirdgloop calc (calculatePlayerVsNpc from
// upstream's TestUtils) on each, and emits one parity-corpus row per scenario
// under tools/scenarios/sweep/*.json with the upstream-asserted expected fields
// {maxAttackRoll, npcDefRoll, accuracy, maxHit, dps, ttk}.
//
// Run via the npm script (which wires the loader chain that stubs weirdgloop's
// PNG/React/mobx deps the same way their jest.config.ts moduleNameMapper does):
//
//     npm run gen-sweep
//
// Each row carries the same schema ParityCorpusTest consumes. After running this,
// run `npm run gen-corpus` to fold sweep/*.json into the Java corpus.

import { mkdirSync, writeFileSync, rmSync } from 'node:fs';
import { resolve, join } from 'node:path';
import {
  calculatePlayerVsNpc,
  getTestPlayer,
  getTestMonsterById,
  findEquipment,
  findSpell,
} from '@/tests/utils/TestUtils';
import type { Player, EquipmentPiece } from '@/types/Player';
import type { Monster } from '@/types/Monster';
import { getCombatStylesForCategory } from '@/utils';
import { EquipmentCategory } from '@/enums/EquipmentCategory';
import { Prayer } from '@/enums/Prayer';
import Potion from '@/enums/Potion';
import { PotionMap } from '@/utils';
import type { PlayerSkills } from '@/types/Player';
import type { PlayerCombatStyle } from '@/types/PlayerCombatStyle';

const WG_COMMIT = '5e313c1b6cfba51d11a98c612243570a84772f44';
const here = import.meta.dirname;
const outDir = resolve(here, 'scenarios', 'sweep');

// ---------------------------------------------------------------------------
// Equipment lookup helpers (by name+version, mirroring upstream findEquipment).
// ---------------------------------------------------------------------------

function eqp(name: string, version = ''): EquipmentPiece {
  return findEquipment(name, version);
}

// Skill name list ParityCorpusTest's validator requires (all 9 present).
const SKILL_KEYS = ['atk', 'str', 'def', 'hp', 'ranged', 'magic', 'prayer', 'mining', 'herblore'] as const;

function skills(over: Partial<Record<(typeof SKILL_KEYS)[number], number>> = {}): PlayerSkills {
  const base: any = {};
  for (const k of SKILL_KEYS) base[k] = over[k] ?? 99;
  return base as PlayerSkills;
}

// Replicate state.tsx recomputeBoosts (max-combine of PotionMap.calculateFn) so
// the calc (which reads player.boosts) sees the same boosts the Java
// BoostsCalculator derives from the same potion set.
function computeBoosts(s: PlayerSkills, potions: Potion[]): Partial<PlayerSkills> {
  const boosts: any = { atk: 0, def: 0, magic: 0, prayer: 0, ranged: 0, str: 0, mining: 0, herblore: 0 };
  for (const p of potions) {
    const result: any = PotionMap[p].calculateFn(s);
    for (const k of Object.keys(result)) {
      if (result[k] > boosts[k]) boosts[k] = result[k];
    }
  }
  return boosts;
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
// Loadout model. A loadout describes a player (gear/style/prayer/skills/potions/
// spell + buffs) abstractly; we materialize it both into a Player override (for
// the upstream calc) and into a corpus-row player block (for the Java side).
// ---------------------------------------------------------------------------

interface Loadout {
  key: string;            // short slug used in the row name
  desc: string;           // human-readable
  category: EquipmentCategory;
  styleName: string;
  gear: Record<string, [string, string?]>; // slot -> [name, version]
  spell?: string;         // spell name (std spellbook), for magic
  skills?: Partial<Record<(typeof SKILL_KEYS)[number], number>>;
  banes?: string[];       // extra monster-attribute tags this loadout targets
}

// Buff/prayer/potion variation applied on top of a loadout.
interface Variation {
  suffix: string;
  prayers: Prayer[];
  potions: Potion[];
  onSlayerTask: boolean;
}

const PRAYER_NAME: Record<number, string> = {
  [Prayer.PIETY]: 'PIETY',
  [Prayer.RIGOUR]: 'RIGOUR',
  [Prayer.AUGURY]: 'AUGURY',
};

const POTION_NAME: Record<number, string> = {
  [Potion.SUPER_COMBAT]: 'SUPER_COMBAT',
  [Potion.RANGING]: 'RANGING',
  [Potion.SUPER_MAGIC]: 'SUPER_MAGIC',
  [Potion.SUPER_ATTACK]: 'SUPER_ATTACK',
  [Potion.SUPER_STRENGTH]: 'SUPER_STRENGTH',
  [Potion.SATURATED_HEART]: 'SATURATED_HEART',
  [Potion.OVERLOAD_PLUS]: 'OVERLOAD_PLUS',
  [Potion.SMELLING_SALTS]: 'SMELLING_SALTS',
};

// ---------------------------------------------------------------------------
// Common support gear sets per style (BIS-ish, no leagues, no spec).
// ---------------------------------------------------------------------------

const MELEE_GEAR: Record<string, [string, string?]> = {
  head: ['Torva full helm'],
  cape: ['Infernal max cape'],
  neck: ['Amulet of torture'],
  body: ['Torva platebody'],
  shield: ['Avernic defender'],
  legs: ['Torva platelegs'],
  hands: ['Ferocious gloves'],
  feet: ['Primordial boots'],
  ring: ['Ultor ring'],
};

const RANGED_GEAR: Record<string, [string, string?]> = {
  head: ['Masori mask (f)'],
  cape: ["Dizana's quiver", 'Charged'],
  neck: ['Necklace of anguish'],
  body: ['Masori body (f)'],
  legs: ['Masori chaps (f)'],
  hands: ['Zaryte vambraces'],
  feet: ['Pegasian boots'],
  ring: ['Venator ring'],
};

const MAGE_GEAR: Record<string, [string, string?]> = {
  head: ['Ancestral hat'],
  cape: ['Imbued saradomin cape'],
  neck: ['Occult necklace'],
  body: ['Ancestral robe top'],
  shield: ['Elidinis\' ward (f)'],
  legs: ['Ancestral robe bottom'],
  hands: ['Tormented bracelet'],
  feet: ['Eternal boots'],
  ring: ['Magus ring'],
};

function withWeapon(base: Record<string, [string, string?]>, slot: string, w: [string, string?]): Record<string, [string, string?]> {
  return { ...base, [slot]: w };
}

// ---------------------------------------------------------------------------
// Loadout catalogue.
// ---------------------------------------------------------------------------

const loadouts: Loadout[] = [];

// --- Melee weapons (style = first non-default offensive stance) ---
function melee(key: string, desc: string, weapon: [string, string?], category: EquipmentCategory, styleName: string, banes?: string[], gearOverride?: Record<string, [string, string?]>): Loadout {
  return {
    key, desc, category, styleName, banes,
    gear: gearOverride ?? withWeapon(MELEE_GEAR, 'weapon', weapon),
  };
}

loadouts.push(
  melee('whip', 'Abyssal whip (slash)', ['Abyssal whip'], EquipmentCategory.WHIP, 'Lash', ['demon']),
  melee('rapier', 'Ghrazi rapier (stab)', ['Ghrazi rapier'], EquipmentCategory.STAB_SWORD, 'Lunge', ['plain']),
  melee('fang', "Osmumten's fang (stab)", ["Osmumten's fang"], EquipmentCategory.STAB_SWORD, 'Lunge', ['kalphite']),
  melee('scythe', 'Scythe of vitur (slash)', ['Scythe of vitur', 'Charged'], EquipmentCategory.SCYTHE, 'Chop', ['plain'],
    withWeapon({ ...MELEE_GEAR, shield: undefined as any }, 'weapon', ['Scythe of vitur', 'Charged'])),
  melee('dhl', 'Dragon hunter lance (stab)', ['Dragon hunter lance'], EquipmentCategory.SPEAR, 'Lunge', ['dragon']),
  melee('inq-mace', "Inquisitor's mace (crush)", ["Inquisitor's mace"], EquipmentCategory.SPIKED, 'Pummel', ['plain']),
  melee('keris', 'Keris partisan (stab)', ['Keris partisan'], EquipmentCategory.PARTISAN, 'Lunge', ['kalphite']),
  melee('dscim', 'Dragon scimitar (slash)', ['Dragon scimitar'], EquipmentCategory.SLASH_SWORD, 'Lunge'),
  melee('dwh', 'Dragon warhammer (crush)', ['Dragon warhammer'], EquipmentCategory.BLUNT, 'Pound'),
  melee('obsi-sword', 'Toktz-xil-ak obsidian sword (stab)', ['Toktz-xil-ak'], EquipmentCategory.STAB_SWORD, 'Lunge'),
  melee('tzhaar-em', 'Tzhaar-ket-em (crush, blunt)', ['Tzhaar-ket-em'], EquipmentCategory.BLUNT, 'Pound'),
  melee('blisterwood', 'Blisterwood flail (crush)', ['Blisterwood flail'], EquipmentCategory.STAFF, 'Pound', ['vampyre']),
  melee('dharok', "Dharok's greataxe (slash)", ["Dharok's greataxe", 'Undamaged'], EquipmentCategory.AXE, 'Hack', ['plain'],
    {
      head: ["Dharok's helm", 'Undamaged'],
      cape: ['Infernal max cape'],
      neck: ['Amulet of torture'],
      weapon: ["Dharok's greataxe", 'Undamaged'],
      body: ["Dharok's platebody", 'Undamaged'],
      legs: ["Dharok's platelegs", 'Undamaged'],
      hands: ['Ferocious gloves'],
      feet: ['Primordial boots'],
      ring: ['Ultor ring'],
    }),
);

// Obsidian set bonus (obsidian armour + tzhaar weapon).
loadouts.push({
  key: 'obsi-set',
  desc: 'Obsidian armour + Tzhaar-ket-em (10% obsidian bonus)',
  category: EquipmentCategory.BLUNT,
  styleName: 'Pound',
  gear: {
    head: ['Obsidian helmet'],
    body: ['Obsidian platebody'],
    legs: ['Obsidian platelegs'],
    neck: ['Berserker necklace'],
    weapon: ['Tzhaar-ket-em'],
    cape: ['Infernal max cape'],
    hands: ['Ferocious gloves'],
    feet: ['Primordial boots'],
    ring: ['Ultor ring'],
  },
});

// Salve amulet (ei) + undead target (salve multiplier path).
loadouts.push(melee('whip-salve', 'Abyssal whip + Salve amulet(ei) (undead)', ['Abyssal whip'], EquipmentCategory.WHIP, 'Lash', ['undead'],
  withWeapon({ ...MELEE_GEAR, neck: ['Salve amulet(ei)', ''] }, 'weapon', ['Abyssal whip'])));

// Slayer helmet (i) on-task (black-mask multiplier path).
loadouts.push(melee('whip-slayerhelm', 'Abyssal whip + Slayer helmet (i)', ['Abyssal whip'], EquipmentCategory.WHIP, 'Lash', ['demon', 'undead'],
  withWeapon({ ...MELEE_GEAR, head: ['Slayer helmet (i)', ''] }, 'weapon', ['Abyssal whip'])));

// Void melee set (void multiplier path).
loadouts.push(melee('whip-void', 'Abyssal whip + Void melee set', ['Abyssal whip'], EquipmentCategory.WHIP, 'Lash', undefined, {
  head: ['Void melee helm', 'Normal'],
  body: ['Void knight top', 'Normal'],
  legs: ['Void knight robe', 'Normal'],
  hands: ['Void knight gloves'],
  cape: ['Infernal max cape'],
  neck: ['Amulet of torture'],
  shield: ['Avernic defender'],
  feet: ['Primordial boots'],
  ring: ['Ultor ring'],
  weapon: ['Abyssal whip'],
}));

// --- Ranged weapons ---
function ranged(key: string, desc: string, weapon: [string, string?], category: EquipmentCategory, styleName: string, ammo?: [string, string?], banes?: string[], gearOverride?: Record<string, [string, string?]>): Loadout {
  const gear = gearOverride ?? { ...RANGED_GEAR, weapon };
  if (ammo) gear.ammo = ammo;
  return { key, desc, category, styleName, banes, gear };
}

loadouts.push(
  ranged('tbow', 'Twisted bow (rapid) + dragon arrows', ['Twisted bow'], EquipmentCategory.BOW, 'Rapid', ['Dragon arrow', 'Unpoisoned'], ['plain']),
  ranged('bofa', 'Bow of faerdhinen (c) + crystal armour (rapid)', ['Bow of faerdhinen (c)'], EquipmentCategory.BOW, 'Rapid', undefined, ['demon'], {
    head: ['Crystal helm', 'Active'],
    cape: ["Dizana's quiver", 'Charged'],
    neck: ['Necklace of anguish'],
    body: ['Crystal body', 'Active'],
    legs: ['Crystal legs', 'Active'],
    hands: ['Zaryte vambraces'],
    feet: ['Pegasian boots'],
    ring: ['Venator ring'],
    weapon: ['Bow of faerdhinen (c)'],
  }),
  ranged('blowpipe', 'Toxic blowpipe + dragon darts (rapid)', ['Toxic blowpipe', 'Charged'], EquipmentCategory.THROWN, 'Rapid', ['Dragon dart', 'Unpoisoned'], ['demon']),
  ranged('dhcb', 'Dragon hunter crossbow + dragon bolts (rapid)', ['Dragon hunter crossbow'], EquipmentCategory.CROSSBOW, 'Rapid', ['Dragon bolts', 'Unpoisoned'], ['dragon']),
  ranged('zcb', 'Zaryte crossbow + dragon bolts (rapid)', ['Zaryte crossbow'], EquipmentCategory.CROSSBOW, 'Rapid', ['Dragon bolts', 'Unpoisoned']),
  ranged('acb', 'Armadyl crossbow + dragon bolts (rapid)', ['Armadyl crossbow'], EquipmentCategory.CROSSBOW, 'Rapid', ['Dragon bolts', 'Unpoisoned']),
  ranged('msb', 'Magic shortbow + rune arrows (rapid)', ['Magic shortbow'], EquipmentCategory.BOW, 'Rapid', ['Rune arrow', 'Unpoisoned'], ['lowlevel']),
  ranged('chin', 'Black chinchompa (medium fuse)', ['Black chinchompa'], EquipmentCategory.CHINCHOMPA, 'Medium fuse'),
);

// Ranged void set.
loadouts.push(ranged('bp-void', 'Toxic blowpipe + Void ranger set', ['Toxic blowpipe', 'Charged'], EquipmentCategory.THROWN, 'Rapid', ['Dragon dart', 'Unpoisoned'], undefined, {
  head: ['Void ranger helm', 'Normal'],
  body: ['Void knight top', 'Normal'],
  legs: ['Void knight robe', 'Normal'],
  hands: ['Void knight gloves'],
  cape: ["Ava's assembler", 'Normal'],
  neck: ['Necklace of anguish'],
  feet: ['Pegasian boots'],
  ring: ['Venator ring'],
  weapon: ['Toxic blowpipe', 'Charged'],
  ammo: ['Dragon dart', 'Unpoisoned'],
}));

// --- Magic weapons / spells ---
function powered(key: string, desc: string, weapon: [string, string?], styleName: string, banes?: string[]): Loadout {
  return {
    key, desc, category: EquipmentCategory.POWERED_STAFF, styleName, banes,
    gear: { ...MAGE_GEAR, weapon, shield: undefined as any },
  };
}

loadouts.push(
  powered('shadow', "Tumeken's shadow (accurate)", ["Tumeken's shadow", 'Charged'], 'Accurate', ['magic-weak']),
  powered('sang', 'Sanguinesti staff (accurate)', ['Sanguinesti staff', 'Charged'], 'Accurate', ['magic-weak']),
  powered('trident-seas', 'Trident of the seas (accurate)', ['Trident of the seas', 'Charged'], 'Accurate', ['magic-weak']),
  powered('trident-swamp', 'Trident of the swamp (accurate)', ['Trident of the swamp', 'Charged'], 'Accurate', ['magic-weak']),
);

// Standard-spellbook spells cast on a staff (Kodai wand + ward).
function spellLoadout(key: string, desc: string, spell: string, banes?: string[], gearOverride?: Record<string, [string, string?]>): Loadout {
  return {
    key, desc, category: EquipmentCategory.STAFF, styleName: 'Spell',
    spell, banes,
    gear: gearOverride ?? { ...MAGE_GEAR, weapon: ['Kodai wand'], shield: ['Elidinis\' ward (f)'] },
  };
}

loadouts.push(
  spellLoadout('firebolt', 'Fire bolt (Kodai + ancestral)', 'Fire Bolt', ['magic-weak']),
  spellLoadout('firewave', 'Fire wave (Kodai + ancestral)', 'Fire Wave', ['magic-weak']),
  spellLoadout('firesurge', 'Fire surge (Kodai + ancestral)', 'Fire Surge', ['magic-weak']),
  spellLoadout('icebarrage', 'Ice barrage (Kodai + ancestral)', 'Ice Barrage', ['magic-weak']),
  spellLoadout('firebolt-chaos', 'Fire bolt + chaos gauntlets', 'Fire Bolt', ['magic-weak'], {
    head: ['Ancestral hat'], cape: ['Imbued saradomin cape'], neck: ['Occult necklace'],
    body: ['Ancestral robe top'], legs: ['Ancestral robe bottom'], hands: ['Chaos gauntlets'],
    feet: ['Eternal boots'], ring: ['Magus ring'], weapon: ['Kodai wand'], shield: ['Elidinis\' ward (f)'],
  }),
);

// ---------------------------------------------------------------------------
// Monster catalogue (real ids triggering different bane/attribute paths).
// ---------------------------------------------------------------------------

interface MonScenario { id: number; version: string; tag: string; attrs: string[]; }

const monsters: MonScenario[] = [
  { id: 415, version: 'Standard', tag: 'abyssal-demon', attrs: ['demon', 'common'] },
  { id: 8059, version: 'Post-quest', tag: 'vorkath', attrs: ['dragon', 'undead', 'fiery', 'common'] },
  { id: 7275, version: '', tag: 'brutal-black-dragon', attrs: ['dragon', 'fiery'] },
  { id: 12191, version: 'Post-quest, Awake', tag: 'duke', attrs: ['demon'] },
  { id: 5862, version: '', tag: 'cerberus', attrs: ['demon'] },
  { id: 484, version: 'Normal', tag: 'bloodveld', attrs: ['demon'] },
  { id: 963, version: 'Crawling', tag: 'kalphite-queen', attrs: ['kalphite'] },
  { id: 2215, version: '', tag: 'general-graardor', attrs: ['plain', 'common'] },
  { id: 2205, version: '', tag: 'commander-zilyana', attrs: ['plain'] },
  { id: 9756, version: '1', tag: 'vyrewatch-sentinel', attrs: ['vampyre'] },
  { id: 2, version: '', tag: 'aberrant-spectre', attrs: ['undead', 'spectral'] },
  { id: 319, version: '', tag: 'corporeal-beast', attrs: ['plain'] },
  { id: 2042, version: 'Serpentine', tag: 'zulrah-serpentine', attrs: ['magic-weak'] },
  { id: 8615, version: 'Serpentine', tag: 'alchemical-hydra', attrs: ['dragon'] },
  { id: 7221, version: 'Group', tag: 'scurrius', attrs: ['lowlevel'] },
];

// Select the monsters relevant to a loadout: a shared 'common' baseline plus any
// monsters whose attributes intersect the loadout's bane tags (keeps the cross-
// product focused at ~250-350 rows rather than a full 33×15 blowup).
function monstersFor(lo: Loadout): MonScenario[] {
  const wanted = new Set<string>(['common', ...(lo.banes ?? [])]);
  const out = monsters.filter((m) => m.attrs.some((a) => wanted.has(a)));
  // Always include at least the common baseline.
  return out.length ? out : monsters.filter((m) => m.attrs.includes('common'));
}

// ---------------------------------------------------------------------------
// Variation matrix per style.
// ---------------------------------------------------------------------------

function variationsFor(category: EquipmentCategory, spell: boolean): Variation[] {
  // Determine style family.
  const meleeCats = new Set<EquipmentCategory>([
    EquipmentCategory.WHIP, EquipmentCategory.STAB_SWORD, EquipmentCategory.SLASH_SWORD,
    EquipmentCategory.SCYTHE, EquipmentCategory.SPEAR, EquipmentCategory.SPIKED,
    EquipmentCategory.PARTISAN, EquipmentCategory.BLUNT, EquipmentCategory.AXE, EquipmentCategory.STAFF,
  ]);
  const rangedCats = new Set<EquipmentCategory>([
    EquipmentCategory.BOW, EquipmentCategory.CROSSBOW, EquipmentCategory.THROWN, EquipmentCategory.CHINCHOMPA,
  ]);

  if (spell || category === EquipmentCategory.POWERED_STAFF || category === EquipmentCategory.STAFF) {
    // STAFF here means a blisterwood flail (melee) OR a spell-cast staff. Distinguish by `spell`.
    if (spell) {
      return [
        { suffix: 'augury-task', prayers: [Prayer.AUGURY], potions: [Potion.SUPER_MAGIC], onSlayerTask: true },
        { suffix: 'noprayer-offtask', prayers: [], potions: [], onSlayerTask: false },
      ];
    }
  }
  if (category === EquipmentCategory.POWERED_STAFF) {
    return [
      { suffix: 'augury-task', prayers: [Prayer.AUGURY], potions: [Potion.SATURATED_HEART], onSlayerTask: true },
      { suffix: 'augury-offtask', prayers: [Prayer.AUGURY], potions: [Potion.SATURATED_HEART], onSlayerTask: false },
    ];
  }
  if (rangedCats.has(category)) {
    return [
      { suffix: 'rigour-task', prayers: [Prayer.RIGOUR], potions: [Potion.RANGING], onSlayerTask: true },
      { suffix: 'rigour-offtask', prayers: [Prayer.RIGOUR], potions: [Potion.RANGING], onSlayerTask: false },
    ];
  }
  if (meleeCats.has(category)) {
    return [
      { suffix: 'piety-task', prayers: [Prayer.PIETY], potions: [Potion.SUPER_COMBAT], onSlayerTask: true },
      { suffix: 'piety-offtask', prayers: [Prayer.PIETY], potions: [Potion.SUPER_COMBAT], onSlayerTask: false },
    ];
  }
  return [{ suffix: 'noprayer-offtask', prayers: [], potions: [], onSlayerTask: false }];
}

// ---------------------------------------------------------------------------
// Build one corpus row from a (loadout, monster, variation).
// ---------------------------------------------------------------------------

interface CorpusRow {
  name: string;
  source: string;
  exercises: string[];
  inputs: {
    player: {
      skills: PlayerSkills;
      prayers: string[];
      style: { name: string; type: string | null; stance: string | null };
      spell?: string;
      potions?: string[];
      buffs?: { onSlayerTask: boolean };
      equipment: Record<string, number>;
    };
    monster: { id: number; version: string };
  };
  expected: Record<string, number>;
  weirdgloopCommit: string;
}

function buildRow(lo: Loadout, mon: MonScenario, v: Variation): CorpusRow {
  // Resolve gear name+version -> EquipmentPiece, drop undefined slots.
  const gearPieces: Record<string, EquipmentPiece> = {};
  const gearIds: Record<string, number> = {};
  for (const [slot, val] of Object.entries(lo.gear)) {
    if (!val) continue;
    const piece = eqp(val[0], val[1] ?? '');
    gearPieces[slot] = piece;
    gearIds[slot] = piece.id;
  }

  const s = skills(lo.skills);
  const boosts = computeBoosts(s, v.potions);
  const style = styleFor(lo.category, lo.styleName);
  const spell = lo.spell ? findSpell(lo.spell) : null;

  // IMPORTANT: getTestMonsterById matches by id only and returns the FIRST row
  // with that id. Some ids are shared across versions (e.g. Duke 12191 =
  // "Awakened, Awake" AND "Post-quest, Awake"), so the version upstream actually
  // used may differ from the tag's intended version. We record the RESOLVED
  // monster.version so the Java MonsterRepository.resolve(id, version) picks the
  // identical row the upstream calc used.
  const monster: Monster = getTestMonsterById(mon.id);
  const resolvedVersion = monster.version ?? '';
  if (mon.version && resolvedVersion !== mon.version) {
    console.error(`  note ${lo.key}/${mon.tag}: id ${mon.id} resolved to version '${resolvedVersion}' (tag wanted '${mon.version}')`);
  }

  const overrides: any = {
    skills: s,
    boosts,
    prayers: v.prayers,
    style,
    equipment: gearPieces,
    spell,
    buffs: {
      potions: v.potions,
      onSlayerTask: v.onSlayerTask,
    },
  };

  const player: Player = getTestPlayer(monster, overrides);
  const res = calculatePlayerVsNpc(monster, player);

  const expected: Record<string, number> = {
    maxAttackRoll: res.maxAttackRoll,
    npcDefRoll: res.npcDefRoll,
    accuracy: res.accuracy,
    maxHit: res.maxHit,
    dps: res.dps,
    // calculatePlayerVsNpc doesn't return getTtk(); read it off an identically
    // constructed calc.
    ttk: ttkOf(monster, player),
  };

  const name = `sweep-${lo.key}-${mon.tag}-${v.suffix}`;
  const prayerNames = v.prayers.map((p) => PRAYER_NAME[p]).filter(Boolean);
  const potionNames = v.potions.map((p) => POTION_NAME[p]).filter(Boolean);

  const row: CorpusRow = {
    name,
    source: `gen-parity-sweep.ts: ${lo.desc} vs ${mon.tag} [${v.suffix}] (weirdgloop@${WG_COMMIT.slice(0, 7)})`,
    exercises: ['PlayerVsNpcCalc.dps'],
    inputs: {
      player: {
        skills: s,
        prayers: prayerNames,
        style: { name: style.name, type: style.type ?? null, stance: style.stance ?? null },
        equipment: gearIds,
      },
      monster: { id: mon.id, version: resolvedVersion },
    },
    expected,
    weirdgloopCommit: WG_COMMIT,
  };
  if (spell) row.inputs.player.spell = lo.spell;
  if (potionNames.length) row.inputs.player.potions = potionNames;
  if (!v.onSlayerTask) row.inputs.player.buffs = { onSlayerTask: false };

  return row;
}

// ttk via a fresh calc instance (TestUtils.calculatePlayerVsNpc doesn't expose
// getTtk, so we read it off a calc built the same way).
import PlayerVsNPCCalc from '@/lib/PlayerVsNPCCalc';
function ttkOf(monster: Monster, player: Player): number {
  const calc = new PlayerVsNPCCalc(player, monster, { loadoutName: 'test', detailedOutput: true });
  return calc.getTtk();
}

// ---------------------------------------------------------------------------
// Self-check: before trusting the harness, confirm it reproduces two known
// values from the hand-transcribed corpus (Osmumten's fang max melee = 50;
// empty player vs Abyssal demon accuracy = 0.29098123501694634). If the loader
// chain or upstream pin drifts, this fails loudly rather than emitting wrong
// "expected" values.
// ---------------------------------------------------------------------------

function selfCheck(): void {
  // (1) Osmumten's fang in max melee (corpus row 001) == 50.
  const ademon = getTestMonsterById(415);
  const fangGear: Record<string, [string, string?]> = {
    head: ['Torva full helm'], cape: ['Infernal max cape'], neck: ['Amulet of torture'],
    weapon: ["Osmumten's fang"], body: ['Torva platebody'], shield: ['Avernic defender'],
    legs: ['Torva platelegs'], hands: ['Ferocious gloves'], feet: ['Primordial boots'],
    ring: ['Berserker ring (i)'],
  };
  const gear: any = {};
  for (const [slot, val] of Object.entries(fangGear)) gear[slot] = eqp(val[0], val[1] ?? '');
  const fangPlayer = getTestPlayer(ademon, {
    skills: skills({ atk: 118, str: 118 }),
    prayers: [Prayer.PIETY],
    style: styleFor(EquipmentCategory.STAB_SWORD, 'Lunge'),
    equipment: gear,
  } as any);
  const fangMax = calculatePlayerVsNpc(ademon, fangPlayer).maxHit;
  if (fangMax !== 50) {
    throw new Error(`self-check FAILED: Osmumten's fang max melee = ${fangMax}, expected 50`);
  }

  // (2) Empty player vs Abyssal demon accuracy.
  const empty = getTestPlayer(ademon, {});
  const acc = calculatePlayerVsNpc(ademon, empty).accuracy;
  const wantAcc = 0.29098123501694634;
  if (Math.abs(acc - wantAcc) > 1e-12) {
    throw new Error(`self-check FAILED: empty-vs-abyssal-demon accuracy = ${acc}, expected ${wantAcc}`);
  }

  console.log(`self-check OK: fang max melee = ${fangMax}; empty-vs-abyssal-demon accuracy = ${acc}`);
}

// ---------------------------------------------------------------------------
// Main: enumerate, run, write.
// ---------------------------------------------------------------------------

function main(): void {
  selfCheck();
  rmSync(outDir, { recursive: true, force: true });
  mkdirSync(outDir, { recursive: true });

  const rows: CorpusRow[] = [];
  const seen = new Set<string>();
  let errors = 0;

  for (const lo of loadouts) {
    const spell = !!lo.spell;
    const variations = variationsFor(lo.category, spell);
    const mons = monstersFor(lo);
    for (const mon of mons) {
      for (const v of variations) {
        let row: CorpusRow;
        try {
          row = buildRow(lo, mon, v);
        } catch (e) {
          errors++;
          console.error(`  skip ${lo.key}/${mon.tag}/${v.suffix}: ${(e as Error).message}`);
          continue;
        }
        if (seen.has(row.name)) {
          throw new Error(`duplicate row name ${row.name}`);
        }
        seen.add(row.name);
        rows.push(row);
      }
    }
  }

  // Trim/sanity-check: skip rows with non-finite expected fields (e.g. ttk=Infinity
  // when the player can never damage the monster). Those would fail the Java
  // validator (finite-number requirement) and aren't meaningful parity rows.
  const finiteRows = rows.filter((r) => {
    for (const [, val] of Object.entries(r.expected)) {
      if (!Number.isFinite(val)) return false;
    }
    return true;
  });
  const dropped = rows.length - finiteRows.length;

  let idx = 0;
  for (const r of finiteRows) {
    idx++;
    const file = join(outDir, `${String(idx).padStart(3, '0')}-${r.name}.json`);
    writeFileSync(file, JSON.stringify(r, null, 2) + '\n', 'utf8');
  }

  console.log(`Generated ${finiteRows.length} sweep row(s) into ${outDir}`);
  if (dropped) console.log(`  (dropped ${dropped} row(s) with non-finite expected fields, e.g. ttk=Infinity)`);
  if (errors) console.log(`  (${errors} loadout/monster combos errored during lookup — see above)`);
}

main();
