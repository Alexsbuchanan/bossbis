package org.bossbis.calc;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Focused unit test for the ported magic max hit ({@code getPlayerMaxMagicHit} + {@code getSpellMaxHit}
 * + the magic branch of {@code getMinAndMax} + {@code getMaxHit}). Locks the two seeded gate cases:
 * <ul>
 *   <li>Tumeken's shadow in max mage (parity corpus row {@code 005-tumekens-shadow-max-mage}) -> 65;</li>
 *   <li>Fire Bolt with chaos gauntlets (parity corpus row {@code 006-fire-bolt-chaos-gauntlets}) -> 20.</li>
 * </ul>
 */
class MagicMaxHitTest
{
	@Test
	void tumekensShadowMaxMage()
	{
		// Same loadout as parity corpus row 005-tumekens-shadow-max-mage (expected maxHit 65).
		PlayerVsNpcCalc calc = MagicMaxHitFixtures.tumekensShadowMaxMage();
		assertThat(calc.getMaxHit()).isEqualTo(65);
	}

	@Test
	void fireBoltChaosGauntlets()
	{
		// Same loadout as parity corpus row 006-fire-bolt-chaos-gauntlets (expected maxHit 20).
		PlayerVsNpcCalc calc = MagicMaxHitFixtures.fireBoltChaosGauntlets();
		assertThat(calc.getMaxHit()).isEqualTo(20);
	}
}
