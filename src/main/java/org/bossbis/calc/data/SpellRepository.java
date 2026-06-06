package org.bossbis.calc.data;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import org.bossbis.calc.types.Spell;

/**
 * Repository over the bundled {@code spells.json}, indexed by spell name.
 */
public final class SpellRepository
{
	private final Map<String, Spell> byName;

	private SpellRepository(Map<String, Spell> byName)
	{
		this.byName = byName;
	}

	public static SpellRepository fromBundled(Gson gson)
	{
		Spell[] all = EquipmentRepository.readResource(
			gson, "/org/bossbis/data/spells.json", Spell[].class);
		Map<String, Spell> byName = new HashMap<>(all.length * 2);
		for (Spell s : all)
		{
			byName.putIfAbsent(s.getName(), s);
		}
		return new SpellRepository(byName);
	}

	/**
	 * Looks up a spell by its exact name.
	 *
	 * @return the matched {@link Spell}, or {@code null} if no spell has that name
	 */
	public Spell byName(String name)
	{
		return byName.get(name);
	}

	/** Number of distinct spells indexed. */
	public int size()
	{
		return byName.size();
	}
}
