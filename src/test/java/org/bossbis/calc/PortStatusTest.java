package org.bossbis.calc;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Meta-gate over the parity corpus: proves no row silently runs un-gated and that the expected set
 * of rows is live.
 *
 * <p>It computes how many rows WOULD skip in {@link ParityCorpusTest} (i.e. exercise at least one
 * un-ported calc path). At v0.1.4 (Milestone 2) the {@code PlayerVsNpcCalc.distribution} path
 * (getAttackerDist + applyNpcTransforms + getDistribution + getMax) joins the already-ported
 * {@code accuracy}, {@code defenceRoll}, {@code maxMeleeHit}, {@code maxRangedHit}, and
 * {@code maxMagicHit} paths, so EVERY corpus row now RUNs (asserts): the 3 {@code maxAttackRoll} rows,
 * the 3 {@code npcDefRoll} rows, the 2 {@code accuracy} (hit-chance) rows, the 6 plain melee
 * {@code maxHit} rows, the 3 ranged {@code maxHit} rows (bofa L99 = 29, bofa L1 = 3, comp-ogre-bow +
 * brutal vs Zogre = 19), the 4 magic {@code maxHit} rows (tumeken's shadow = 65, fire-bolt + chaos
 * gauntlets = 20, tumeken's-shadow + salve = 67, fire-bolt + chaos gauntlets + tome of fire = 22), AND
 * the {@code blisterwood-flail-max-melee} row, which now asserts {@code maxHit} = 55 via {@code getMax()}
 * (the vampyrebane {@code scaleDamage(5,4)} bonus is applied in {@code getDistribution}; the base
 * {@code getMinAndMax().max()} is 44, and 44*5/4 = 55). Skip-count is exactly {@code total - PORTED_ROWS}
 * (zero). This locks both that every row now has a live assertion and that no row runs un-gated.
 */
class PortStatusTest
{
	/**
	 * Number of corpus rows whose calc path is ported at v0.1.4 (Milestone 2): every row. The 3
	 * {@code maxAttackRoll} + 3 {@code npcDefRoll} + 2 {@code accuracy} + 6 melee {@code maxHit} + 3
	 * ranged {@code maxHit} + 4 magic {@code maxHit} rows (21) plus the blisterwood-flail row, whose
	 * distribution-pipeline vampyrebane bonus is now ported (22 total). No row skips.
	 */
	private static final long PORTED_ROWS = 22;

	@Test
	void onlyThePortedRowsRunTheRestSkip()
	{
		List<CorpusRow> rows = CorpusRow.loadAll();
		assertThat(rows).as("parity corpus must be seeded with real rows").isNotEmpty();

		long wouldSkip = rows.stream()
			.filter(r -> !PortState.allPorted(r.exercises()))
			.count();

		assertThat(wouldSkip)
			.as("at v0.1.4 the distribution path is ported, so every corpus row runs (zero skip)")
			.isEqualTo(rows.size() - PORTED_ROWS);

		long wouldRun = rows.size() - wouldSkip;
		assertThat(wouldRun)
			.as("every corpus row asserts at v0.1.4 (maxHit via getMax incl. blisterwood = 55)")
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
