// Lightweight stand-in for weirdgloop's @/state, redirected by
// upstream-stub-loader.mjs. The real state.tsx pulls in mobx/localforage/
// react-toastify/the worker chain, none of which the pure calc needs.
// TestUtils.ts only consumes generateEmptyPlayer, so we replicate exactly that
// function (verbatim from upstream state.tsx) using only the pure deps.

import { getCombatStylesForCategory } from '@/utils';
import { EquipmentCategory } from '@/enums/EquipmentCategory';
import { DEFAULT_ATTACK_SPEED } from '@/lib/constants';
import type { Player, PlayerEquipment } from '@/types/Player';

const generateInitialEquipment = (): PlayerEquipment => ({
  ammo: null,
  body: null,
  cape: null,
  feet: null,
  hands: null,
  head: null,
  legs: null,
  neck: null,
  ring: null,
  shield: null,
  weapon: null,
});

export const generateEmptyPlayer = (name?: string): Player => ({
  name: name ?? 'Loadout 1',
  style: getCombatStylesForCategory(EquipmentCategory.NONE)[0],
  skills: {
    atk: 99,
    def: 99,
    hp: 99,
    magic: 99,
    prayer: 99,
    ranged: 99,
    str: 99,
    mining: 99,
    herblore: 99,
  },
  boosts: {
    atk: 0,
    def: 0,
    hp: 0,
    magic: 0,
    prayer: 0,
    ranged: 0,
    str: 0,
    mining: 0,
    herblore: 0,
  },
  equipment: generateInitialEquipment(),
  attackSpeed: DEFAULT_ATTACK_SPEED,
  prayers: [],
  bonuses: {
    str: 0,
    ranged_str: 0,
    magic_str: 0,
    prayer: 0,
  },
  defensive: {
    stab: 0,
    slash: 0,
    crush: 0,
    magic: 0,
    ranged: 0,
  },
  offensive: {
    stab: 0,
    slash: 0,
    crush: 0,
    magic: 0,
    ranged: 0,
  },
  buffs: {
    potions: [],
    onSlayerTask: true,
    inWilderness: false,
    kandarinDiary: true,
    chargeSpell: false,
    markOfDarknessSpell: false,
    forinthrySurge: false,
    soulreaperStacks: 0,
    baAttackerLevel: 0,
    chinchompaDistance: 4,
    usingSunfireRunes: false,
  },
  spell: null,
  leagues: {
    six: {
      selectedNodeIds: new Set<string>(['node1']),
      effects: {},
      distanceToEnemy: 1,
      enemyPrayers: {
        melee: false,
        ranged: false,
        magic: false,
      },
      blindbagWeapons: [],
      regenerateMagicBonus: 0,
      cullingSpree: false,
      bowHitsWithoutDamage: 0,
    },
  },
} as unknown as Player);
