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
import org.bossbis.calc.types.MonsterInputs;
import org.bossbis.calc.types.Player;
import org.bossbis.calc.types.PlayerEquipment;
import org.bossbis.calc.types.PlayerSkills;
import org.bossbis.calc.types.Prayer;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Focused unit test for {@link NpcVsPlayerCalc} (damage taken). The broader gate is the
 * {@code kind:"nvp"} rows in {@link ParityCorpusTest}. Here we lock the contract directly for an
 * Abyssal demon (id 415, Standard — a stab/melee attacker) against (1) an empty all-99 player and
 * (2) a Justiciar-tank player, plus a Dagannoth Prime (id 2266) magic attacker that exercises the
 * magic-wiki-maxHit override (computed 26 → wiki 50).
 *
 * <p>All expected values are the full-precision results of upstream {@code NPCVsPlayerCalc}
 * (weirdgloop@5e313c1), captured via {@code tools/probe-nvp.ts}; floats asserted at relative ≤ 1e-9.
 */
class NvpTest
{
	private static final EquipmentRepository EQUIP = EquipmentRepository.fromBundled(new com.google.gson.Gson());
	private static final MonsterRepository MONSTERS = MonsterRepository.fromBundled(new com.google.gson.Gson());
	private static final Equipment AGG = new Equipment(EQUIP);

	// --- Abyssal demon (415, Standard) vs empty all-99 player ---
	@Test
	void abyssalDemonVsEmptyPlayer()
	{
		NpcVsPlayerCalc calc = abyssalDemon(emptyPlayerEquipment());

		assertThat(calc.getPlayerDefenceRoll()).isEqualTo(6848);
		assertThat(calc.getNPCMaxHit()).isEqualTo(8);
		assertThat(calc.getNPCMaxAttackRoll()).isEqualTo(6784);
		assertClose(calc.getHitChance(), 0.4952547817199591);
		assertClose(calc.getDps(), 0.8254246361999319);
		assertClose(calc.getAverageDamageTaken(), 186.9070875073532);
	}

	// --- Abyssal demon vs Justiciar tank (exercises the Justiciar reduction transform) ---
	@Test
	void abyssalDemonVsJusticiarTank()
	{
		NpcVsPlayerCalc calc = abyssalDemon(justiciarEquipment());

		assertThat(calc.getPlayerDefenceRoll()).isEqualTo(37557);
		assertThat(calc.getNPCMaxHit()).isEqualTo(8);
		assertThat(calc.getNPCMaxAttackRoll()).isEqualTo(6784);
		assertClose(calc.getHitChance(), 0.09031364822408008);
		assertClose(calc.getDps(), 0.1170732476978816);
		assertClose(calc.getAverageDamageTaken(), 26.509773021770872);
	}

	// --- Dagannoth Prime (2266) magic attacker: magic-wiki-maxHit override (26 -> 50) ---
	@Test
	void dagannothPrimeMagicMaxHitOverride()
	{
		NpcVsPlayerCalc calc = build(2266, "", emptyPlayerEquipment());
		assertThat(calc.getNPCMaxHit()).isEqualTo(50);
	}

	// ---------------------------------------------------------------------------------------------

	private static void assertClose(double actual, double expected)
	{
		assertThat(actual).isCloseTo(expected, within(Math.abs(expected) * 1e-9));
	}

	private static NpcVsPlayerCalc abyssalDemon(PlayerEquipment eq)
	{
		return build(415, "Standard", eq);
	}

	private static NpcVsPlayerCalc build(int monsterId, String version, PlayerEquipment eq)
	{
		Monster monster = monster(monsterId, version);
		// generateEmptyPlayer default style: Punch (crush, Accurate) for the empty/no-weapon loadout.
		PlayerCombatStyle style = new PlayerCombatStyle("Punch", CombatStyle.CRUSH, CombatStyle.ACCURATE);
		Player p = playerWithAggregatedStats(eq, style, monster);
		return new NpcVsPlayerCalc(p, monster, EQUIP);
	}

	private static PlayerEquipment emptyPlayerEquipment()
	{
		return new PlayerEquipment(null, null, null, null, null, null, null, null, null, null, null);
	}

	private static PlayerEquipment justiciarEquipment()
	{
		return new PlayerEquipment(
			resolve(22326), // Justiciar faceguard
			null, null, null, null,
			resolve(22327), // Justiciar chestguard
			null,
			resolve(22328), // Justiciar legguards
			null, null, null);
	}

	private static Monster monster(int id, String version)
	{
		Monster base = MONSTERS.resolve(id, version);
		return new Monster(
			base.getId(), base.getName(), base.getImage(), base.getVersion(), base.getSize(),
			base.getSpeed(), base.getStyle(), base.getMaxHit(), base.getSkills(), base.getOffensive(),
			base.getDefensive(), base.getAttributes(), base.getWeakness(), base.getImmunities(),
			base.isSlayerMonster(), MonsterInputs.initial());
	}

	private static Player playerWithAggregatedStats(PlayerEquipment eq, PlayerCombatStyle style, Monster monster)
	{
		Player raw = new Player(
			"test", style,
			new PlayerSkills(99, 99, 99, 99, 99, 99, 99, 99, 99),
			new PlayerSkills(0, 0, 0, 0, 0, 0, 0, 0, 0),
			eq, 4,
			Collections.<Prayer>emptyList(),
			new Buffs(true, false, false, 0, 0, 4, true, false, false, false),
			null, null, null, null);

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
