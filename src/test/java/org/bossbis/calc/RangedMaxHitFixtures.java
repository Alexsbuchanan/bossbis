package org.bossbis.calc;

import com.google.gson.Gson;

/**
 * Test-only helper that builds the same loadout as parity corpus row
 * {@code 018-bofa-max-ranged-l99} (expected {@code maxHit} 29) by reusing the {@link ScenarioPlayer}
 * corpus-row builder. Keeps the focused {@link RangedMaxHitTest} in lockstep with the corpus row so a
 * divergence is impossible.
 */
final class RangedMaxHitFixtures
{
	private RangedMaxHitFixtures() {}

	private static final ScenarioPlayer BUILDER = new ScenarioPlayer(new Gson());

	/** Builds the corpus row 018 (Bow of faerdhinen max ranged, L99) calc; expected {@code getMaxHit() == 29}. */
	static PlayerVsNpcCalc bofaMaxRangedL99()
	{
		return forRow("bofa-max-ranged-l99");
	}

	/** Builds an arbitrary ranged corpus row by name. */
	static PlayerVsNpcCalc forRow(String name)
	{
		CorpusRow row = corpusRow(name);
		var monster = BUILDER.monster(row);
		var player = BUILDER.player(row, monster);
		return new PlayerVsNpcCalc(player, monster, BUILDER.equipmentRepository());
	}

	private static CorpusRow corpusRow(String name)
	{
		for (CorpusRow row : CorpusRow.loadAll())
		{
			if (name.equals(row.name()))
			{
				return row;
			}
		}
		throw new IllegalStateException("No corpus row named " + name);
	}
}
