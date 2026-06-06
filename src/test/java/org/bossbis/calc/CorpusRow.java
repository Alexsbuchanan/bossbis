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
	private List<String> exercises;
	private Inputs inputs;
	private Map<String, Double> expected;
	private String weirdgloopCommit;

	String name()
	{
		return name;
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

		Map<String, Object> style()
		{
			return style == null ? Collections.emptyMap() : style;
		}

		Map<String, Integer> equipment()
		{
			return equipment == null ? Collections.emptyMap() : equipment;
		}
	}

	static final class MonsterInput
	{
		private int id;
		private String version;

		int id()
		{
			return id;
		}

		String version()
		{
			return version;
		}
	}
}
