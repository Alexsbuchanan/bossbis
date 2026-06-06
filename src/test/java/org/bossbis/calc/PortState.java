package org.bossbis.calc;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads {@code /parity/parity-corpus} companion {@code port-state.json} — the
 * machine-readable view of {@code PORT_STATE.md} — listing which calc paths are
 * ported.
 *
 * <p>A path is considered ported iff it appears in the {@code "ported"} array
 * OR its entry in the {@code "paths"} map equals {@code "ported"}. At v0.1.0
 * none of the {@code PlayerVsNpcCalc.*} paths are ported, so {@link #allPorted}
 * returns {@code false} for every parity-corpus row.
 */
final class PortState
{
	private static final Set<String> PORTED_PATHS = load();

	private PortState()
	{
	}

	private static Set<String> load()
	{
		try (InputStream in = PortState.class.getResourceAsStream("/parity/port-state.json"))
		{
			if (in == null)
			{
				throw new IllegalStateException("port-state.json not on the test classpath");
			}
			Map<String, Object> doc = new Gson().fromJson(
				new InputStreamReader(in, StandardCharsets.UTF_8),
				new TypeToken<Map<String, Object>>(){}.getType());

			Set<String> ported = new HashSet<>();

			Object portedArr = doc.get("ported");
			if (portedArr instanceof Collection)
			{
				for (Object p : (Collection<?>) portedArr)
				{
					ported.add(String.valueOf(p));
				}
			}

			Object paths = doc.get("paths");
			if (paths instanceof Map)
			{
				for (Map.Entry<?, ?> e : ((Map<?, ?>) paths).entrySet())
				{
					if ("ported".equals(String.valueOf(e.getValue())))
					{
						ported.add(String.valueOf(e.getKey()));
					}
				}
			}

			return ported;
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Failed to load port-state.json", e);
		}
	}

	/** True iff the given calc path is marked ported. */
	static boolean isPorted(String path)
	{
		return PORTED_PATHS.contains(path);
	}

	/**
	 * True iff EVERY exercised calc path is ported (so the row may be asserted).
	 * An empty list is vacuously "all ported" — but corpus rows always tag at
	 * least one path, and {@link PortStatusTest} guards against silent skips.
	 */
	static boolean allPorted(List<String> exercises)
	{
		for (String path : exercises)
		{
			if (!isPorted(path))
			{
				return false;
			}
		}
		return true;
	}
}
