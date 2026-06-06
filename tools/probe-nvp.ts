// One-off probe: prints upstream NPCVsPlayerCalc outputs for a few fixed
// (monster attacker x player defender) scenarios, used to seed NvpTest + the
// gen-nvp-sweep self-check. Run via:
//
//   node run-with-upstream.mjs probe-nvp.ts

import {
  getTestPlayer,
  getTestMonsterById,
  findEquipment,
  calculateNpcVsPlayer,
} from '@/tests/utils/TestUtils';
import NPCVsPlayerCalc from '@/lib/NPCVsPlayerCalc';
import type { EquipmentPiece, Player } from '@/types/Player';
import type { Monster } from '@/types/Monster';

function eqp(name: string, version = ''): EquipmentPiece {
  return findEquipment(name, version);
}

function probe(label: string, monsterId: number, gear: Record<string, [string, string?]>) {
  const monster: Monster = getTestMonsterById(monsterId);
  const equipment: any = {};
  for (const [slot, val] of Object.entries(gear)) {
    equipment[slot] = eqp(val[0], val[1] ?? '');
  }
  const player: Player = getTestPlayer(monster, { equipment } as any);
  const res = calculateNpcVsPlayer(monster, player);
  const calc = new NPCVsPlayerCalc(player, monster, { loadoutName: 'test' });
  const avg = calc.getAverageDamageTaken();
  console.log(`=== ${label} (id ${monsterId}, resolved '${monster.version}', style '${monster.style}') ===`);
  console.log(JSON.stringify({
    playerDefRoll: res.playerDefRoll,
    npcMaxHit: res.npcMaxHit,
    npcMaxAttackRoll: res.npcMaxAttackRoll,
    npcAccuracy: res.npcAccuracy,
    npcDps: res.npcDps,
    avgDmgTaken: avg,
  }, null, 2));
}

// 1) Abyssal demon (415) melee attacker vs an empty all-99 player (no gear).
probe('Abyssal demon vs empty player', 415, {});

// 2) Abyssal demon vs a tank-armour player (Justiciar set).
probe('Abyssal demon vs Justiciar tank', 415, {
  head: ['Justiciar faceguard'],
  body: ['Justiciar chestguard'],
  legs: ['Justiciar legguards'],
});

// 3) A ranged attacker (Vorkath, 8059) vs empty player.
probe('Vorkath vs empty player', 8059, {});
