package org.bossbis.calc;

import com.google.gson.Gson;

/**
 * Test-only helper that builds magic max-hit loadouts by reusing the {@link ScenarioPlayer} corpus-row
 * builder, wiring in the {@link org.bossbis.calc.data.SpellRepository} the magic path needs for the
 * elemental spell-tier lookups. Keeps the focused {@link MagicMaxHitTest} in lockstep with the seeded
 * corpus rows ({@code 005-tumekens-shadow-max-mage} = 65, {@code 006-fire-bolt-chaos-gauntlets} = 20).
 */
final class MagicMaxHitFixtures
{
	private MagicMaxHitFixtures() {}

	private static final ScenarioPlayer BUILDER = new ScenarioPlayer(new Gson());

	/** Builds the corpus row 005 (Tumeken's shadow max mage) calc; expected {@code getMaxHit() == 65}. */
	static PlayerVsNpcCalc tumekensShadowMaxMage()
	{
		return forRow("tumekens-shadow-max-mage");
	}

	/** Builds the corpus row 006 (Fire Bolt + chaos gauntlets) calc; expected {@code getMaxHit() == 20}. */
	static PlayerVsNpcCalc fireBoltChaosGauntlets()
	{
		return forRow("fire-bolt-chaos-gauntlets");
	}

	/** Builds an arbitrary magic corpus row by name. */
	static PlayerVsNpcCalc forRow(String name)
	{
		CorpusRow row = corpusRow(name);
		var monster = BUILDER.monster(row);
		var player = BUILDER.player(row, monster);
		return new PlayerVsNpcCalc(player, monster, BUILDER.equipmentRepository(), BUILDER.spellRepository());
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
