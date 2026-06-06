package org.bossbis.calc;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.bossbis.calc.types.Monster;
import org.bossbis.calc.types.Player;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.within;

/**
 * Parity runner over the reference corpus
 * ({@code src/test/resources/parity/parity-corpus.json}).
 *
 * <p>A row whose {@code exercises} includes any un-ported calc path is SKIPPED via
 * {@link Assumptions#assumeTrue}. When every exercised path is {@code ported} (per
 * {@code port-state.json}), the row is built into a calc-ready {@link Player}+{@link Monster} via
 * {@link ScenarioPlayer}, a {@link PlayerVsNpcCalc} is constructed, and each present {@code expected}
 * field is asserted at the §5.3 tolerance: integer outputs ({@code maxAttackRoll}, {@code npcDefRoll},
 * {@code maxHit}) EXACT; floating outputs ({@code accuracy}, {@code hitChance}, {@code dps},
 * {@code ttk}) relative {@code <= 1e-9}.
 *
 * <p>At v0.1.1 the {@code PlayerVsNpcCalc.accuracy} path is ported, so the 3 {@code maxAttackRoll}
 * rows run; the 6 {@code maxHit} rows still skip (their {@code maxMeleeHit}/{@code maxRangedHit}/
 * {@code maxMagicHit} paths are not ported).
 */
class ParityCorpusTest
{
	/** Relative tolerance for floating outputs (§5.3). */
	private static final double FLOAT_REL_TOL = 1e-9;

	/** Integer-valued expected fields (asserted EXACT). */
	private static final List<String> INTEGER_FIELDS = Arrays.asList(
		"maxAttackRoll", "npcDefRoll", "maxHit");

	private static final ScenarioPlayer BUILDER = new ScenarioPlayer(new Gson());

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

		Monster monster = BUILDER.monster(row);
		Player player = BUILDER.player(row, monster);
		PlayerVsNpcCalc calc = new PlayerVsNpcCalc(player, monster,
			BUILDER.equipmentRepository(), BUILDER.spellRepository());

		assertThat(row.expected())
			.as("row '%s' must carry at least one expected field", row.name())
			.isNotEmpty();

		for (Map.Entry<String, Double> e : row.expected().entrySet())
		{
			String field = e.getKey();
			double expected = e.getValue();
			double actual = compute(calc, field, row);

			if (INTEGER_FIELDS.contains(field))
			{
				assertThat((long) actual)
					.as("row '%s' field '%s' (integer, exact)", row.name(), field)
					.isEqualTo((long) expected);
			}
			else
			{
				assertThat(actual)
					.as("row '%s' field '%s' (float, relative <= %s)", row.name(), field, FLOAT_REL_TOL)
					.isCloseTo(expected, within(Math.abs(expected) * FLOAT_REL_TOL));
			}
		}
	}

	/** Maps an expected-field name to the matching calc getter. */
	private static double compute(PlayerVsNpcCalc calc, String field, CorpusRow row)
	{
		switch (field)
		{
			case "maxAttackRoll":
				return calc.getMaxAttackRoll();
			case "npcDefRoll":
				return calc.getNPCDefenceRoll();
			case "maxHit":
				return calc.getMaxHit();
			case "accuracy":
			case "hitChance":
				return calc.getHitChance();
			case "dps":
				return calc.getDps();
			case "ttk":
				return calc.getTtk();
			default:
				fail("row '%s': no calc getter mapped for expected field '%s'", row.name(), field);
				return Double.NaN; // unreachable
		}
	}
}
