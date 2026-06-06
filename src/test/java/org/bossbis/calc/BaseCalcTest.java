package org.bossbis.calc;

import com.google.gson.Gson;
import java.util.Collections;
import org.bossbis.calc.data.EquipmentRepository;
import org.bossbis.calc.data.MonsterRepository;
import org.bossbis.calc.types.Buffs;
import org.bossbis.calc.types.CombatStyle;
import org.bossbis.calc.types.CombatStyle.PlayerCombatStyle;
import org.bossbis.calc.types.EquipmentPiece;
import org.bossbis.calc.types.Monster;
import org.bossbis.calc.types.Player;
import org.bossbis.calc.types.PlayerEquipment;
import org.bossbis.calc.types.PlayerSkills;
import org.bossbis.calc.types.Prayer;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Tests for {@link BaseCalc} — the accuracy formula, the long-safe {@code trackFactor} helper, and a
 * few of the {@code isWearingX} predicates. {@link TestCalc} is a minimal concrete subclass (BaseCalc
 * is abstract until {@code PlayerVsNpcCalc} extends it in Milestone 3) that exposes the protected
 * helpers for testing.
 */
class BaseCalcTest
{
	private static final Gson GSON = new Gson();
	private static final EquipmentRepository EQUIP = EquipmentRepository.fromBundled(GSON);
	private static final MonsterRepository MONSTERS = MonsterRepository.fromBundled(GSON);

	// item ids (verified against the bundled equipment.json)
	private static final int ABYSSAL_WHIP = 4151;
	private static final int SLAYER_HELMET_I = 11865;
	private static final int BANDOS_CHESTPLATE = 11832;

	/** A test-only concrete subclass exposing the protected helpers for assertions. */
	static final class TestCalc extends BaseCalc
	{
		TestCalc(Player player, Monster monster)
		{
			super(player, monster, new CalcOpts(), EQUIP);
		}

		// expose protected helpers
		int track(int base, int n, int d)
		{
			return trackFactor("test", base, n, d);
		}

		boolean wearingSlayerHelmet()
		{
			return isWearingSlayerHelmet();
		}

		boolean wearingImbuedBlackMask()
		{
			return isWearingImbuedBlackMask();
		}

		boolean usingAbyssal()
		{
			return isUsingAbyssal();
		}

		boolean usingMeleeStyle()
		{
			return isUsingMeleeStyle();
		}
	}

	@Test
	void trackFactorUsesLongIntermediate()
	{
		// 2_000_000 * 1500 / 1000 = 3_000_000 — would overflow int if multiplied as int.
		assertThat(BaseCalcTestHook.trackFactor(2_000_000, 1500, 1000)).isEqualTo(3_000_000);
	}

	@Test
	void normalAccuracyRollMatchesFormula()
	{
		// atk>def branch: 1 - (def+2)/(2*(atk+1)); pick atk=20000, def=10000.
		double expected = 1.0 - (10000.0 + 2) / (2 * (20000.0 + 1));
		assertThat(BaseCalc.getNormalAccuracyRoll(20000, 10000)).isCloseTo(expected, within(1e-12));
	}

	@Test
	void isWearingPredicates()
	{
		// Abyssal whip + Slayer helmet (i) + Bandos chestplate, melee slash (Accurate) style.
		PlayerCombatStyle style = new PlayerCombatStyle("Lash", CombatStyle.SLASH, CombatStyle.ACCURATE);
		PlayerEquipment eq = new PlayerEquipment(
			resolve(SLAYER_HELMET_I), // head
			null,                     // cape
			null,                     // neck
			null,                     // ammo
			resolve(ABYSSAL_WHIP),    // weapon
			resolve(BANDOS_CHESTPLATE), // body
			null, null, null, null, null);
		TestCalc calc = new TestCalc(player(eq, style), abyssalDemon());

		assertThat(calc.usingMeleeStyle()).isTrue();
		assertThat(calc.usingAbyssal()).isTrue();          // abyssal whip + melee
		assertThat(calc.wearingImbuedBlackMask()).isTrue(); // slayer helmet (i)
		assertThat(calc.wearingSlayerHelmet()).isTrue();    // slayer helmet (i) is in the slayer-helmet list
	}

	private static EquipmentPiece resolve(int id)
	{
		return EQUIP.resolve(id).orElseThrow(
			() -> new IllegalArgumentException("No equipment row for id " + id));
	}

	private static Monster abyssalDemon()
	{
		return MONSTERS.resolve(415, "Standard");
	}

	private static Player player(PlayerEquipment eq, PlayerCombatStyle style)
	{
		return new Player(
			"test",
			style,
			new PlayerSkills(99, 99, 99, 99, 99, 99, 99, 99, 99),
			new PlayerSkills(0, 0, 0, 0, 0, 0, 0, 0, 0),
			eq,
			4,
			Collections.<Prayer>emptyList(),
			new Buffs(false, false, false, 0, 0, 0, false, false, false, false),
			null,
			null, null, null);
	}
}
