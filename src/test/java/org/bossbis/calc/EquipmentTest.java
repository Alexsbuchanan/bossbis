package org.bossbis.calc;

import com.google.gson.Gson;
import org.bossbis.calc.data.EquipmentRepository;
import org.bossbis.calc.data.MonsterRepository;
import org.bossbis.calc.types.EquipmentPiece;
import org.bossbis.calc.types.EquipmentStats;
import org.bossbis.calc.types.Monster;
import org.bossbis.calc.types.Player;
import org.bossbis.calc.types.PlayerEquipment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gate for {@link Equipment#calculateEquipmentBonusesFromGear}.
 *
 * <p>The asserted totals are transcribed verbatim from upstream
 * {@code tools/upstream/src/tests/lib/Equipment.test.ts} (the {@code calculateEquipmentBonusesFromGear}
 * suite). Two cases are ported, both exercising {@code ammoApplicability} <em>and</em> the
 * Dizana's-quiver special case in tandem:
 *
 * <ul>
 *   <li><b>Twisted bow + Dragon arrow + charged Dizana's quiver</b> — the bow accepts Dragon arrows
 *       ({@code ammoApplicability == INCLUDED}), so the arrow's {@code ranged_str} is summed and the
 *       charged quiver adds its +10 ranged / +1 ranged_str. Upstream asserts {@code offensive.ranged==98},
 *       {@code bonuses.ranged_str==84}. (Cross-checked against equipment.json: tbow 70/20 + arrow 0/60 +
 *       quiver 18/3 = 88/83, then +10/+1 from the charged-quiver special case = 98/84.)</li>
 *   <li><b>Dragon dart + charged Dizana's quiver (no ammo)</b> — a thrown weapon uses no ammo slot, so
 *       {@code ammoApplicability == ALLOWED} (not INCLUDED) and the quiver bonus is NOT applied. Upstream
 *       asserts {@code offensive.ranged==18}, {@code bonuses.ranged_str==38}. (Cross-checked: dart 0/35 +
 *       quiver 18/3 = 18/38, no quiver bonus.)</li>
 * </ul>
 */
class EquipmentTest
{
	private static final int TWISTED_BOW = 20997;
	private static final int DRAGON_ARROW = 11212;        // Unpoisoned
	private static final int DIZANA_QUIVER_CHARGED = 28951;
	private static final int DRAGON_DART = 11230;         // Unpoisoned

	static EquipmentRepository equipment;
	static MonsterRepository monsters;
	static Equipment calc;

	@BeforeAll
	static void load()
	{
		Gson gson = new Gson();   // tests may use new Gson()
		equipment = EquipmentRepository.fromBundled(gson);
		monsters = MonsterRepository.fromBundled(gson);
		calc = new Equipment(equipment);
	}

	@Test
	void dizanaQuiverChargedAppliesBonusWhenWeaponUsesAmmo()
	{
		Monster monster = EquipmentFixtures.abyssalDemon(monsters);
		EquipmentPiece cape = EquipmentFixtures.resolve(equipment, DIZANA_QUIVER_CHARGED);
		EquipmentPiece weapon = EquipmentFixtures.resolve(equipment, TWISTED_BOW);
		EquipmentPiece ammo = EquipmentFixtures.resolve(equipment, DRAGON_ARROW);
		PlayerEquipment eq = EquipmentFixtures.equipment(cape, weapon, ammo);
		Player p = EquipmentFixtures.player(eq, EquipmentFixtures.rapidRanged());

		EquipmentStats agg = calc.calculateEquipmentBonusesFromGear(p, monster);

		assertThat(agg.getOffensive().getRanged()).isEqualTo(98);
		assertThat(agg.getBonuses().getRangedStr()).isEqualTo(84);
	}

	@Test
	void dizanaQuiverChargedDoesNotApplyBonusWhenWeaponDoesNotUseAmmo()
	{
		Monster monster = EquipmentFixtures.abyssalDemon(monsters);
		EquipmentPiece cape = EquipmentFixtures.resolve(equipment, DIZANA_QUIVER_CHARGED);
		EquipmentPiece weapon = EquipmentFixtures.resolve(equipment, DRAGON_DART);
		PlayerEquipment eq = EquipmentFixtures.equipment(cape, weapon, null);
		Player p = EquipmentFixtures.player(eq, EquipmentFixtures.rapidRanged());

		EquipmentStats agg = calc.calculateEquipmentBonusesFromGear(p, monster);

		assertThat(agg.getOffensive().getRanged()).isEqualTo(18);
		assertThat(agg.getBonuses().getRangedStr()).isEqualTo(38);
	}
}
