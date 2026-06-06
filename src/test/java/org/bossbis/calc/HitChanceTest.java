package org.bossbis.calc;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Focused unit test for {@link PlayerVsNpcCalc#getHitChance()} / {@link PlayerVsNpcCalc#getDisplayHitChance()}.
 *
 * <p>The broader gate is {@link ParityCorpusTest} (rows 013 — normal accuracy from CombatCalc.test.ts,
 * and 014 — Fang accuracy). Here we lock the contract directly: for a plain melee (no Fang/Confliction)
 * loadout, {@code getHitChance()} equals {@link BaseCalc#getNormalAccuracyRoll(double, double)} of the
 * computed attack/defence rolls, and {@code getDisplayHitChance()} equals {@code getHitChance()} when no
 * Brimstone-magic blend applies.
 */
class HitChanceTest
{
	@Test
	void plainMeleeHitChanceIsNormalAccuracyRoll()
	{
		PlayerVsNpcCalc calc = AttackRollFixtures.basicMeleeWhipL99();

		int atk = calc.getMaxAttackRoll();
		long def = calc.getNPCDefenceRoll();
		double expected = BaseCalc.getNormalAccuracyRoll(atk, def);

		assertThat(calc.getHitChance())
			.isCloseTo(expected, within(Math.abs(expected) * 1e-9));
	}

	@Test
	void displayHitChanceEqualsHitChanceWithoutBrimstoneMagic()
	{
		PlayerVsNpcCalc calc = AttackRollFixtures.basicMeleeWhipL99();

		assertThat(calc.getDisplayHitChance())
			.isEqualTo(calc.getHitChance());
	}
}
