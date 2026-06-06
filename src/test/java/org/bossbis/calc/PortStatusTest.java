package org.bossbis.calc;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Meta-gate over the parity corpus: proves no row silently runs un-gated and that the expected set
 * of rows is live.
 *
 * <p>It computes how many rows WOULD skip in {@link ParityCorpusTest} (i.e. exercise at least one
 * un-ported calc path). At v0.1.2 the {@code PlayerVsNpcCalc.accuracy},
 * {@code PlayerVsNpcCalc.defenceRoll}, and {@code PlayerVsNpcCalc.maxMeleeHit} paths are ported, so the
 * 3 {@code maxAttackRoll} rows, the 3 {@code npcDefRoll} rows, the 2 {@code accuracy} (hit-chance) rows,
 * and 3 of the 4 melee {@code maxHit} rows RUN (assert). The 2 magic {@code maxHit} rows still SKIP
 * ({@code maxMagicHit} un-ported), and the {@code blisterwood-flail-max-melee} row also SKIPs: its
 * asserted {@code maxHit} (55) is a distribution output (the vampyre {@code scaleDamage(5,4)} bonus is
 * applied in {@code getDistribution}, NOT {@code getPlayerMaxMeleeHit}; {@code getMinAndMax().max()} is
 * 44), so it additionally exercises the un-ported {@code PlayerVsNpcCalc.distribution} path (v0.1.4).
 * Skip-count is exactly {@code total - 11}. This locks both that the attack-roll + defence-roll +
 * accuracy + melee-max-hit rows now have live assertions and that no other row leaked into the
 * asserting set prematurely.
 */
class PortStatusTest
{
	/**
	 * Number of corpus rows whose calc path is ported at v0.1.2: the 3 {@code maxAttackRoll} (accuracy)
	 * rows + the 3 {@code npcDefRoll} (defenceRoll) rows + the 2 {@code accuracy} (hit-chance) rows +
	 * 3 of the 4 melee {@code maxHit} (maxMeleeHit) rows (the blisterwood row needs the distribution
	 * pipeline for its vampyre bonus and so skips until v0.1.4).
	 */
	private static final long PORTED_ROWS = 11;

	@Test
	void onlyThePortedRowsRunTheRestSkip()
	{
		List<CorpusRow> rows = CorpusRow.loadAll();
		assertThat(rows).as("parity corpus must be seeded with real rows").isNotEmpty();

		long wouldSkip = rows.stream()
			.filter(r -> !PortState.allPorted(r.exercises()))
			.count();

		assertThat(wouldSkip)
			.as("at v0.1.2 the 3 maxAttackRoll + 3 npcDefRoll + 2 accuracy + 3 melee maxHit rows run; the 2 magic maxHit + blisterwood (distribution) rows skip")
			.isEqualTo(rows.size() - PORTED_ROWS);

		long wouldRun = rows.size() - wouldSkip;
		assertThat(wouldRun)
			.as("exactly the 3 attack-roll + 3 defence-roll + 2 accuracy + 3 melee max-hit rows assert at v0.1.2")
			.isEqualTo(PORTED_ROWS);
	}

	@Test
	void everyRowTagsAtLeastOneCalcPath()
	{
		// A row with no exercises would be vacuously "all ported" -> run un-gated.
		for (CorpusRow row : CorpusRow.loadAll())
		{
			assertThat(row.exercises())
				.as("row '%s' must tag at least one calc path in 'exercises'", row.name())
				.isNotEmpty();
		}
	}
}
