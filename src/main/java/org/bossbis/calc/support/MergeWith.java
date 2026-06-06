package org.bossbis.calc.support;

import java.util.LinkedHashMap;
import java.util.Map;

/** Minimal port of lodash.mergewith default semantics for the nested holders src/lib merges. */
public final class MergeWith
{
	private MergeWith() {}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> deepMerge(Map<String, Object> base, Map<String, Object> updates)
	{
		Map<String, Object> out = new LinkedHashMap<>(base);
		for (Map.Entry<String, Object> e : updates.entrySet())
		{
			Object u = e.getValue();
			if (u == null)
			{
				continue;   // lodash skips undefined sources
			}
			Object b = out.get(e.getKey());
			if (b instanceof Map && u instanceof Map)
			{
				out.put(e.getKey(), deepMerge((Map<String, Object>) b, (Map<String, Object>) u));
			}
			else
			{
				out.put(e.getKey(), u);
			}
		}
		return out;
	}
}
