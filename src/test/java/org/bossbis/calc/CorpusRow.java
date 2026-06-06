package org.bossbis.calc;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Gson DTO for one row of the parity reference corpus
 * ({@code /parity/parity-corpus.json} on the test classpath).
 *
 * <p>Rows are transcribed from weirdgloop's Jest tests by
 * {@code tools/gen-reference-corpus.ts}. At v0.1.0 no calc path is ported, so
 * {@link ParityCorpusTest} skips every row.
 */
final class CorpusRow
{
	private String name;
	private String source;
	private String kind;
	private List<String> exercises;
	private Inputs inputs;
	private Map<String, Double> expected;
	private String weirdgloopCommit;

	String name()
	{
		return name;
	}

	/** Discriminator: {@code "pvn"} (player-vs-npc, default) or {@code "nvp"} (npc-vs-player / damage taken). */
	String kind()
	{
		return kind == null ? "pvn" : kind;
	}

	String source()
	{
		return source;
	}

	List<String> exercises()
	{
		return exercises == null ? Collections.emptyList() : exercises;
	}

	Inputs inputs()
	{
		return inputs;
	}

	Map<String, Double> expected()
	{
		return expected == null ? Collections.emptyMap() : expected;
	}

	String weirdgloopCommit()
	{
		return weirdgloopCommit;
	}

	/** Used as the {@code @ParameterizedTest} display name. */
	@Override
	public String toString()
	{
		return name;
	}

	/** Loads and parses every row from {@code /parity/parity-corpus.json}. */
	static List<CorpusRow> loadAll()
	{
		try (InputStream in = CorpusRow.class.getResourceAsStream("/parity/parity-corpus.json"))
		{
			if (in == null)
			{
				throw new IllegalStateException("parity-corpus.json not on the test classpath");
			}
			List<CorpusRow> rows = new Gson().fromJson(
				new InputStreamReader(in, StandardCharsets.UTF_8),
				new TypeToken<List<CorpusRow>>(){}.getType());
			return rows == null ? Collections.emptyList() : rows;
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Failed to load parity-corpus.json", e);
		}
	}

	static final class Inputs
	{
		private PlayerInput player;
		private MonsterInput monster;

		PlayerInput player()
		{
			return player;
		}

		MonsterInput monster()
		{
			return monster;
		}
	}

	static final class PlayerInput
	{
		private Map<String, Integer> skills;
		private List<String> prayers;
		private String spell;
		private List<String> potions;
		private BuffsInput buffs;
		private Map<String, Object> style;
		private Map<String, Integer> equipment;

		Map<String, Integer> skills()
		{
			return skills == null ? Collections.emptyMap() : skills;
		}

		List<String> prayers()
		{
			return prayers == null ? Collections.emptyList() : prayers;
		}

		String spell()
		{
			return spell;
		}

		/** Potion enum names (e.g. {@code SUPER_COMBAT}); empty when the row omits potions. */
		List<String> potions()
		{
			return potions == null ? Collections.emptyList() : potions;
		}

		/** Optional buff overrides; {@code null} when the row keeps generateEmptyPlayer defaults. */
		BuffsInput buffs()
		{
			return buffs;
		}

		Map<String, Object> style()
		{
			return style == null ? Collections.emptyMap() : style;
		}

		Map<String, Integer> equipment()
		{
			return equipment == null ? Collections.emptyMap() : equipment;
		}
	}

	/**
	 * Optional {@code player.buffs} block. Only {@code onSlayerTask} is varied by the sweep; the rest of
	 * the buffs keep their {@code generateEmptyPlayer} defaults in {@link ScenarioPlayer}.
	 */
	static final class BuffsInput
	{
		private Boolean onSlayerTask;

		/** {@code null} => keep the generateEmptyPlayer default (true). */
		Boolean onSlayerTask()
		{
			return onSlayerTask;
		}
	}

	static final class MonsterInput
	{
		private int id;
		private String version;
		private MonsterInputsInput inputs;

		int id()
		{
			return id;
		}

		String version()
		{
			return version;
		}

		/** Optional per-encounter inputs; {@code null} when the row omits the block. */
		MonsterInputsInput inputs()
		{
			return inputs;
		}
	}

	/**
	 * Optional {@code monster.inputs} block (ToA invocation level + defence reductions). Absent fields
	 * fall back to {@link org.bossbis.calc.types.MonsterInputs#initial()} in {@link ScenarioPlayer}.
	 */
	static final class MonsterInputsInput
	{
		private Integer toaInvocationLevel;
		private DefenceReductionsInput defenceReductions;

		Integer toaInvocationLevel()
		{
			return toaInvocationLevel;
		}

		DefenceReductionsInput defenceReductions()
		{
			return defenceReductions;
		}
	}

	/** Optional {@code monster.inputs.defenceReductions} block (defaults: all false/0). */
	static final class DefenceReductionsInput
	{
		private boolean vulnerability;
		private boolean accursed;
		private int elderMaul;
		private int dwh;
		private int arclight;
		private int emberlight;
		private int bgs;
		private int tonalztic;
		private int seercull;
		private int ayak;

		boolean vulnerability()
		{
			return vulnerability;
		}

		boolean accursed()
		{
			return accursed;
		}

		int elderMaul()
		{
			return elderMaul;
		}

		int dwh()
		{
			return dwh;
		}

		int arclight()
		{
			return arclight;
		}

		int emberlight()
		{
			return emberlight;
		}

		int bgs()
		{
			return bgs;
		}

		int tonalztic()
		{
			return tonalztic;
		}

		int seercull()
		{
			return seercull;
		}

		int ayak()
		{
			return ayak;
		}
	}
}
