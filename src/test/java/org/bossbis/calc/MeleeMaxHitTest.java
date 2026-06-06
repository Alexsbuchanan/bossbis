package org.bossbis.calc;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Focused unit test for the ported melee max hit ({@code getPlayerMaxMeleeHit} + {@code getMinAndMax}
 * + {@code getMaxHit}). Locks the Osmumten's fang max-melee case (same loadout as parity corpus row
 * {@code 001-osmumtens-fang-max-melee}, expected {@code maxHit} 50).
 */
class MeleeMaxHitTest
{
	@Test
	void osmumtensFangMaxMelee()
	{
		// Same loadout as parity corpus row 001-osmumtens-fang-max-melee (expected maxHit 50).
		PlayerVsNpcCalc calc = MeleeMaxHitFixtures.osmumtensFangMaxMelee();
		assertThat(calc.getMaxHit()).isEqualTo(50);
	}
}
