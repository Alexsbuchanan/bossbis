package org.bossbis.calc;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BundledDataSmokeTest
{
	private InputStream open(String name)
	{
		// Mirrors how the calc will load data in production: classpath, never the filesystem.
		return getClass().getResourceAsStream("/org/bossbis/data/" + name);
	}

	private JsonArray parseArray(String name) throws Exception
	{
		try (InputStream in = open(name))
		{
			assertThat(in).as("%s must be on the classpath", name).isNotNull();
			// NOTE: net.runelite:client pins Gson 2.8.5 transitively, which has only the
			// INSTANCE method new JsonParser().parse(Reader) — the static JsonParser.parseReader
			// was added in Gson 2.8.6. The calc's production data loader (v0.1.0) must likewise
			// target the client's Gson 2.8.5 API.
			return new JsonParser().parse(
				new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
		}
	}

	@Test
	void equipmentJsonIsBundledAndLarge() throws Exception
	{
		JsonArray arr = parseArray("equipment.json");
		assertThat(arr.size()).as("equipment rows").isGreaterThan(5000);
	}

	@Test
	void monstersJsonIsBundledAndLarge() throws Exception
	{
		JsonArray arr = parseArray("monsters.json");
		assertThat(arr.size()).as("monster rows").isGreaterThan(2000);
	}

	@Test
	void aliasesAndSpellsAreBundled() throws Exception
	{
		try (InputStream a = open("equipment_aliases.json"); InputStream s = open("spells.json"))
		{
			assertThat(a).as("equipment_aliases.json").isNotNull();
			assertThat(s).as("spells.json").isNotNull();
		}
	}

	@Test
	void dataVersionIsRecorded() throws Exception
	{
		Properties p = new Properties();
		try (InputStream in = open("data-version.properties"))
		{
			assertThat(in).as("data-version.properties").isNotNull();
			p.load(in);
		}
		assertThat(p.getProperty("weirdgloop.commit"))
			.as("recorded weirdgloop commit")
			.hasSize(40);
	}
}
