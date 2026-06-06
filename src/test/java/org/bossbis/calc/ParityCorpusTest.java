package org.bossbis.calc;

import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Parity runner over the reference corpus
 * ({@code src/test/resources/parity/parity-corpus.json}).
 *
 * <p>A row whose {@code exercises} includes any un-ported calc path is SKIPPED
 * via {@link Assumptions#assumeTrue}. At v0.1.0 no {@code PlayerVsNpcCalc.*}
 * path is ported, so every row skips and the assertion below is never reached.
 * When v0.1.1+ ports a calc path and flips it to {@code "ported"} in
 * {@code port-state.json}, the matching rows "light up" — the assume passes and
 * this test must then assert the expected value (replacing the placeholder
 * throw).
 */
class ParityCorpusTest
{
	static List<CorpusRow> rows()
	{
		return CorpusRow.loadAll();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("rows")
	void parity(CorpusRow row)
	{
		Assumptions.assumeTrue(PortState.allPorted(row.exercises()),
			() -> "skipped: exercises un-ported paths " + row.exercises());

		// Unreached at v0.1.0 (every row skips above). v0.1.1+ replaces this with
		// the real assertion once the exercised calc path is ported.
		throw new AssertionError(
			"No calc path is ported yet; this row should have been skipped: " + row.name());
	}
}
