package org.bossbis.calc;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Meta-gate over the parity corpus: proves no row silently runs un-gated and that the expected set
 * of rows is live.
 *
 * <p>It computes how many rows WOULD skip in {@link ParityCorpusTest} (i.e. exercise at least one
 * un-ported calc path). At v0.1.1 the {@code PlayerVsNpcCalc.accuracy} path is ported, so the 3
 * {@code maxAttackRoll} rows RUN (assert) and the 6 {@code maxHit} rows still SKIP — skip-count is
 * exactly {@code total - 3}. This locks both that the attack-roll rows now have live assertions and
 * that no other row leaked into the asserting set prematurely.
 */
class PortStatusTest
{
	/** Number of corpus rows whose calc path is ported at v0.1.1 (the 3 maxAttackRoll rows). */
	private static final long PORTED_ROWS = 3;

	@Test
	void onlyTheAttackRollRowsRunTheRestSkip()
	{
		List<CorpusRow> rows = CorpusRow.loadAll();
		assertThat(rows).as("parity corpus must be seeded with real rows").isNotEmpty();

		long wouldSkip = rows.stream()
			.filter(r -> !PortState.allPorted(r.exercises()))
			.count();

		assertThat(wouldSkip)
			.as("at v0.1.1 exactly the 3 maxAttackRoll (accuracy) rows run; the 6 maxHit rows still skip")
			.isEqualTo(rows.size() - PORTED_ROWS);

		long wouldRun = rows.size() - wouldSkip;
		assertThat(wouldRun)
			.as("exactly the 3 ported attack-roll rows assert at v0.1.1")
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
