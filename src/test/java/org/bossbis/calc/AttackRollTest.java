package org.bossbis.calc;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Focused unit test for {@link PlayerVsNpcCalc#getMaxAttackRoll()} — the melee attack roll.
 *
 * <p>The value is transcribed from upstream {@code BasicRolls.test.ts} (melee "L99 accuracy",
 * Abyssal whip, {@code maxAttackRoll === 16060}), the same case seeded as corpus row 007. The
 * broader gate is {@link ParityCorpusTest} over all three attack-roll rows.
 */
class AttackRollTest
{
	@Test
	void basicMeleeWhipMaxAttackRoll()
	{
		PlayerVsNpcCalc calc = AttackRollFixtures.basicMeleeWhipL99();
		assertThat(calc.getMaxAttackRoll()).isEqualTo(16060);
	}
}
