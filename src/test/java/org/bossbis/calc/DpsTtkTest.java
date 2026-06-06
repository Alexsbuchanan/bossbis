package org.bossbis.calc;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Focused unit test for {@link PlayerVsNpcCalc#getDps()} and {@link PlayerVsNpcCalc#getTtk()}.
 *
 * <p>The broader gate is {@link ParityCorpusTest} (rows {@code basic-*-dps} and
 * {@code empty-player-abyssal-demon-ttk}). Here we lock the contract directly for the BasicRolls melee
 * loadout (Abyssal whip, L99, vs Abyssal demon / Catacombs of Kourend, id 7241 — the same fixture as
 * {@link HitChanceTest} and corpus row 007/023). Both expected values are the full-precision results of
 * the upstream {@code PlayerVsNPCCalc.getDps()}/{@code getTtk()} pipeline (weirdgloop@5e313c1), captured
 * via {@code tools/probe-dps.ts}; we assert at relative {@code <= 1e-9}.
 *
 * <p>Also locks the algebraic identities the ports must satisfy:
 * {@code getDps() == getDpt() / SECONDS_PER_TICK}, {@code getDpt() == getExpectedDamage() /
 * getExpectedAttackSpeed()}, and {@code getTtk() == getHtk() * getExpectedAttackSpeed() *
 * SECONDS_PER_TICK}.
 */
class DpsTtkTest
{
	/** Upstream getDps() for the whip-L99-vs-Abyssal-demon(7241) loadout (tools/probe-dps.ts). */
	private static final double EXPECTED_DPS = 3.1272565012556295;

	/** Upstream getTtk() for the same loadout (tools/probe-dps.ts). */
	private static final double EXPECTED_TTK = 50.40878639755701;

	@Test
	void basicMeleeWhipDpsMatchesUpstream()
	{
		PlayerVsNpcCalc calc = AttackRollFixtures.basicMeleeWhipL99();

		assertThat(calc.getDps())
			.isCloseTo(EXPECTED_DPS, within(Math.abs(EXPECTED_DPS) * 1e-9));
	}

	@Test
	void basicMeleeWhipTtkMatchesUpstream()
	{
		PlayerVsNpcCalc calc = AttackRollFixtures.basicMeleeWhipL99();

		assertThat(calc.getTtk())
			.isCloseTo(EXPECTED_TTK, within(Math.abs(EXPECTED_TTK) * 1e-9));
	}

	@Test
	void dpsTtkAlgebraicIdentitiesHold()
	{
		PlayerVsNpcCalc calc = AttackRollFixtures.basicMeleeWhipL99();

		double dpt = calc.getExpectedDamage() / calc.getExpectedAttackSpeed();
		assertThat(calc.getDpt()).isEqualTo(dpt);
		assertThat(calc.getDps()).isEqualTo(dpt / Constants.SECONDS_PER_TICK);
		assertThat(calc.getTtk())
			.isEqualTo(calc.getHtk() * calc.getExpectedAttackSpeed() * Constants.SECONDS_PER_TICK);
	}
}
