package org.bossbis.calc;

import com.google.gson.Gson;

/**
 * Test-only helper that builds the same loadout as parity corpus row
 * {@code 001-osmumtens-fang-max-melee} (expected {@code maxHit} 50) by reusing the
 * {@link ScenarioPlayer} corpus-row builder. Keeps the focused {@link MeleeMaxHitTest} in lockstep
 * with the corpus row so a divergence is impossible.
 */
final class MeleeMaxHitFixtures
{
	private MeleeMaxHitFixtures() {}

	private static final ScenarioPlayer BUILDER = new ScenarioPlayer(new Gson());

	/** Builds the corpus row 001 (Osmumten's fang max melee) calc; expected {@code getMaxHit() == 50}. */
	static PlayerVsNpcCalc osmumtensFangMaxMelee()
	{
		CorpusRow row = corpusRow("osmumtens-fang-max-melee");
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
