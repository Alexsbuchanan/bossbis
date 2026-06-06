package org.bossbis.calc;

import java.util.Collections;
import org.bossbis.calc.data.EquipmentRepository;
import org.bossbis.calc.data.MonsterRepository;
import org.bossbis.calc.types.Buffs;
import org.bossbis.calc.types.CombatStyle;
import org.bossbis.calc.types.CombatStyle.PlayerCombatStyle;
import org.bossbis.calc.types.EquipmentPiece;
import org.bossbis.calc.types.EquipmentStats;
import org.bossbis.calc.types.Monster;
import org.bossbis.calc.types.Player;
import org.bossbis.calc.types.PlayerEquipment;
import org.bossbis.calc.types.PlayerSkills;
import org.bossbis.calc.types.Prayer;

/**
 * Tiny test-only helpers that build a calc-ready {@link Player} (with the aggregated
 * {@code offensive}/{@code bonuses}/{@code defensive} populated via the Milestone-1 {@link Equipment}
 * aggregation) for the focused {@link AttackRollTest}. Mirrors upstream {@code getTestPlayer}
 * (all skills default to 99; aggregation drives the stats), seeded for the BasicRolls melee case.
 */
final class AttackRollFixtures
{
	private AttackRollFixtures() {}

	private static final EquipmentRepository EQUIP = EquipmentRepository.fromBundled(new com.google.gson.Gson());
	private static final MonsterRepository MONSTERS = MonsterRepository.fromBundled(new com.google.gson.Gson());
	private static final Equipment AGG = new Equipment(EQUIP);

	private static final int ABYSSAL_WHIP = 4151;

	static EquipmentRepository equipment()
	{
		return EQUIP;
	}

	/**
	 * Builds the BasicRolls "melee L99 accuracy" case: Abyssal whip, 99 attack, no prayer/boost,
	 * Flick (slash, Accurate). Monster is Abyssal demon (Catacombs of Kourend, id 7241) — same as
	 * corpus row 007.
	 */
	static PlayerVsNpcCalc basicMeleeWhipL99()
	{
		Monster monster = monster(7241, "Catacombs of Kourend");
		PlayerCombatStyle style = new PlayerCombatStyle("Flick", CombatStyle.SLASH, CombatStyle.ACCURATE);
		PlayerEquipment eq = new PlayerEquipment(
			null, null, null, null,
			resolve(ABYSSAL_WHIP), // weapon
			null, null, null, null, null, null);
		Player p = playerWithAggregatedStats(eq, style, monster);
		return new PlayerVsNpcCalc(p, monster, EQUIP);
	}

	private static Monster monster(int id, String version)
	{
		Monster base = MONSTERS.resolve(id, version);
		// The bundled monsters.json rows carry no inputs; mirror getTestPlayer (INITIAL_MONSTER_INPUTS).
		return new Monster(
			base.getId(), base.getName(), base.getImage(), base.getVersion(), base.getSize(),
			base.getSpeed(), base.getStyle(), base.getMaxHit(), base.getSkills(), base.getOffensive(),
			base.getDefensive(), base.getAttributes(), base.getWeakness(), base.getImmunities(),
			base.isSlayerMonster(), org.bossbis.calc.types.MonsterInputs.initial());
	}

	private static Player playerWithAggregatedStats(PlayerEquipment eq, PlayerCombatStyle style, Monster monster)
	{
		Player raw = new Player(
			"test",
			style,
			new PlayerSkills(99, 99, 99, 99, 99, 99, 99, 99, 99),
			new PlayerSkills(0, 0, 0, 0, 0, 0, 0, 0, 0),
			eq,
			4,
			Collections.<Prayer>emptyList(),
			new Buffs(true, false, false, 0, 0, 4, true, false, false, false),
			null,
			null, null, null);

		EquipmentStats agg = AGG.calculateEquipmentBonusesFromGear(raw, monster);
		int attackSpeed = AGG.calculateAttackSpeed(raw, monster);

		return new Player(
			raw.getName(), raw.getStyle(), raw.getSkills(), raw.getBoosts(), raw.getEquipment(),
			attackSpeed, raw.getPrayers(), raw.getBuffs(), raw.getSpell(),
			agg.getBonuses(), agg.getOffensive(), agg.getDefensive());
	}

	private static EquipmentPiece resolve(int id)
	{
		return EQUIP.resolve(id).orElseThrow(
			() -> new IllegalArgumentException("No equipment row for id " + id));
	}
}
