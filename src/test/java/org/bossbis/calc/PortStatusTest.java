package org.bossbis.calc;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Meta-gate over the parity corpus: proves no row silently runs un-gated and that the expected set
 * of rows is live.
 *
 * <p>It computes how many rows WOULD skip in {@link ParityCorpusTest} (i.e. exercise at least one
 * un-ported calc path). At v0.1.4 (Milestone 3) the {@code PlayerVsNpcCalc.dps} (getExpectedAttackSpeed
 * + getDpt + getDps) and {@code PlayerVsNpcCalc.ttk} (getHtk + getTtk) paths join the already-ported
 * {@code distribution}, {@code accuracy}, {@code defenceRoll}, {@code maxMeleeHit}, {@code maxRangedHit},
 * and {@code maxMagicHit} paths, so EVERY corpus row now RUNs (asserts): the 3 {@code maxAttackRoll}
 * rows, the 3 {@code npcDefRoll} rows, the 2 {@code accuracy} (hit-chance) rows, the 6 plain melee
 * {@code maxHit} rows, the 3 ranged {@code maxHit} rows (bofa L99 = 29, bofa L1 = 3, comp-ogre-bow +
 * brutal vs Zogre = 19), the 4 magic {@code maxHit} rows (tumeken's shadow = 65, fire-bolt + chaos
 * gauntlets = 20, tumeken's-shadow + salve = 67, fire-bolt + chaos gauntlets + tome of fire = 22), the
 * {@code blisterwood-flail-max-melee} row, which asserts {@code maxHit} = 55 via {@code getMax()} (the
 * vampyrebane {@code scaleDamage(5,4)} bonus is applied in {@code getDistribution}; the base
 * {@code getMinAndMax().max()} is 44, and 44*5/4 = 55), AND the 3 new {@code dps} rows (whip melee /
 * bofa ranged / trident magic vs Abyssal demon) plus 1 {@code ttk} row (empty player vs Abyssal demon,
 * the CombatCalc dps=0.677 case). Skip-count is exactly {@code total - PORTED_ROWS} (zero). This locks
 * both that every row now has a live assertion and that no row runs un-gated.
 */
class PortStatusTest
{
	/**
	 * Number of corpus rows whose calc path is ported at v0.1.4 (Milestone 3): every row. The 3
	 * {@code maxAttackRoll} + 3 {@code npcDefRoll} + 2 {@code accuracy} + 6 melee {@code maxHit} + 3
	 * ranged {@code maxHit} + 4 magic {@code maxHit} rows (21), the blisterwood-flail row (22), and the
	 * 3 {@code dps} + 1 {@code ttk} rows now that the dps/ttk paths are ported (26 total). No row skips.
	 */
	private static final long PORTED_ROWS = 26;

	@Test
	void onlyThePortedRowsRunTheRestSkip()
	{
		List<CorpusRow> rows = CorpusRow.loadAll();
		assertThat(rows).as("parity corpus must be seeded with real rows").isNotEmpty();

		long wouldSkip = rows.stream()
			.filter(r -> !PortState.allPorted(r.exercises()))
			.count();

		assertThat(wouldSkip)
			.as("at v0.1.4 the dps/ttk paths are ported, so every corpus row runs (zero skip)")
			.isEqualTo(rows.size() - PORTED_ROWS);

		long wouldRun = rows.size() - wouldSkip;
		assertThat(wouldRun)
			.as("every corpus row asserts at v0.1.4 (maxHit via getMax incl. blisterwood = 55; dps; ttk)")
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
