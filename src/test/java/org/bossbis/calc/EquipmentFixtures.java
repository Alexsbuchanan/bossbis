package org.bossbis.calc;

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

/**
 * Tiny test-only helpers to build a {@link Player} from slot&rarr;id (resolved via
 * {@link EquipmentRepository}), mirroring the {@code generateEmptyPlayer}/{@code getTestPlayer}
 * defaults of upstream {@code TestUtils.ts} (all skills default to 99, no boosts, no prayers).
 *
 * <p>These exist only to drive {@link EquipmentTest}; they live in test sources, not in {@code main}.
 */
final class EquipmentFixtures
{
	private EquipmentFixtures() {}

	/** All-99 base skills (mirrors {@code generateEmptyPlayer}). */
	static PlayerSkills skills99()
	{
		// (atk, def, hp, magic, prayer, ranged, str, mining, herblore)
		return new PlayerSkills(99, 99, 99, 99, 99, 99, 99, 99, 99);
	}

	/** Zero boosts (no potions active). */
	static PlayerSkills zeroBoosts()
	{
		return new PlayerSkills(0, 0, 0, 0, 0, 0, 0, 0, 0);
	}

	/** All-false/zero buffs (no slayer task, no wilderness, etc.). */
	static Buffs noBuffs()
	{
		// (onSlayerTask, inWilderness, forinthrySurge, soulreaperStacks, baAttackerLevel,
		//  chinchompaDistance, kandarinDiary, chargeSpell, markOfDarknessSpell, usingSunfireRunes)
		return new Buffs(false, false, false, 0, 0, 0, false, false, false, false);
	}

	/** Resolves a piece by cache id through the repo (canonical row), failing loudly if absent. */
	static EquipmentPiece resolve(EquipmentRepository repo, int id)
	{
		return repo.resolve(id).orElseThrow(
			() -> new IllegalArgumentException("No equipment row for id " + id));
	}

	/**
	 * Resolves Abyssal demon (Standard, id 415) — the monster the upstream Equipment.test.ts uses.
	 * It is not a Tombs-of-Amascut monster, so it does not perturb the Tumeken's/Keris special cases.
	 */
	static Monster abyssalDemon(MonsterRepository monsters)
	{
		return monsters.resolve(415, "Standard");
	}

	/**
	 * Builds a {@link PlayerEquipment} with only the given slots populated. Slot order matches the
	 * record: head, cape, neck, ammo, weapon, body, shield, legs, hands, feet, ring.
	 */
	static PlayerEquipment equipment(EquipmentPiece cape, EquipmentPiece weapon, EquipmentPiece ammo)
	{
		return new PlayerEquipment(
			null,    // head
			cape,    // cape
			null,    // neck
			ammo,    // ammo
			weapon,  // weapon
			null,    // body
			null,    // shield
			null,    // legs
			null,    // hands
			null,    // feet
			null);   // ring
	}

	/**
	 * Builds a {@link Player} with all-99 base skills, no boosts/prayers/spell, the given raw
	 * equipment map and style. The aggregated {@code bonuses}/{@code offensive}/{@code defensive}
	 * fields are left null (the calc-under-test computes them).
	 */
	static Player player(PlayerEquipment eq, PlayerCombatStyle style)
	{
		return new Player(
			"test",
			style,
			skills99(),
			zeroBoosts(),
			eq,
			4,                          // attackSpeed (overwritten by the aggregation result)
			Collections.<Prayer>emptyList(),
			noBuffs(),
			null,                       // no spell
			null, null, null);          // aggregated bonuses/offensive/defensive (computed by the calc)
	}

	/** A ranged stance for the Dizana cases (type=ranged, Rapid) — matches the bow combat style. */
	static PlayerCombatStyle rapidRanged()
	{
		return new PlayerCombatStyle("Rapid", CombatStyle.RANGED, CombatStyle.RAPID);
	}
}
