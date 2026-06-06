package org.bossbis.calc;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Meta-gate over the parity corpus: proves no row silently runs un-gated.
 *
 * <p>It computes how many rows WOULD skip in {@link ParityCorpusTest} (i.e.
 * exercise at least one un-ported calc path) and asserts that equals the total
 * row count. At v0.1.0 every row exercises an un-ported {@code PlayerVsNpcCalc.*}
 * path, so skip-count == total. The moment a calc path is ported in v0.1.1+ and
 * its rows start asserting, this count drops below the total — flagging that
 * {@link ParityCorpusTest} now has live assertions to maintain.
 */
class PortStatusTest
{
	@Test
	void everyRowExercisesAnUnportedPathSoAllSkip()
	{
		List<CorpusRow> rows = CorpusRow.loadAll();
		assertThat(rows).as("parity corpus must be seeded with real rows").isNotEmpty();

		long wouldSkip = rows.stream()
			.filter(r -> !PortState.allPorted(r.exercises()))
			.count();

		assertThat(wouldSkip)
			.as("every corpus row must be gated on an un-ported calc path at v0.1.0 "
				+ "(no row may run an assertion before its calc path is ported)")
			.isEqualTo(rows.size());
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
