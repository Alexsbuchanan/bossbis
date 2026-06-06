package org.bossbis.calc;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Focused unit test for the ported ranged max hit ({@code getPlayerMaxRangedHit} + the ranged branch
 * of {@code getMinAndMax} + {@code getMaxHit}). Locks the Bow-of-faerdhinen L99 max-ranged case (same
 * loadout as parity corpus row {@code 018-bofa-max-ranged-l99}, expected {@code maxHit} 29).
 */
class RangedMaxHitTest
{
	@Test
	void bofaMaxRangedL99()
	{
		// Same loadout as parity corpus row 018-bofa-max-ranged-l99 (expected maxHit 29).
		PlayerVsNpcCalc calc = RangedMaxHitFixtures.bofaMaxRangedL99();
		assertThat(calc.getMaxHit()).isEqualTo(29);
	}
}
