package org.bossbis.calc;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Meta-gate over the parity corpus: proves no row silently runs un-gated and that the expected set
 * of rows is live.
 *
 * <p>It computes how many rows WOULD skip in {@link ParityCorpusTest} (i.e. exercise at least one
 * un-ported calc path). At v0.1.5 the {@code NpcVsPlayerCalc.dps} path (the full NPC-vs-player /
 * damage-taken calc) joins the already-ported {@code PlayerVsNpcCalc.*} paths
 * ({@code distribution}, {@code accuracy}, {@code defenceRoll}, {@code maxMeleeHit},
 * {@code maxRangedHit}, {@code maxMagicHit}, {@code dps}, {@code ttk}).
 *
 * <p>The corpus carries the player-vs-npc parity-sweep rows (tools/gen-parity-sweep.ts, tagged
 * {@code PlayerVsNpcCalc.dps}) AND the npc-vs-player sweep rows (tools/gen-nvp-sweep.ts, tagged
 * {@code NpcVsPlayerCalc.dps}, {@code kind:"nvp"}). Every corpus row — the hand-transcribed rows plus
 * both generated sweeps — exercises only ported paths, so NONE skip and every row asserts. This locks
 * both that every row has a live assertion and that no row runs un-gated.
 */
class PortStatusTest
{
	@Test
	void everyCorpusRowRunsNoneSkip()
	{
		List<CorpusRow> rows = CorpusRow.loadAll();
		assertThat(rows).as("parity corpus must be seeded with real rows").isNotEmpty();

		long wouldSkip = rows.stream()
			.filter(r -> !PortState.allPorted(r.exercises()))
			.count();

		assertThat(wouldSkip)
			.as("every exercised calc path is ported at v0.1.5, so no corpus row skips")
			.isZero();

		long wouldRun = rows.size() - wouldSkip;
		assertThat(wouldRun)
			.as("every corpus row asserts (hand-transcribed + parity-sweep)")
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
