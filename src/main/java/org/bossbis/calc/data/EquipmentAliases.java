package org.bossbis.calc.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.Map;

/**
 * Loads the bundled {@code equipment_aliases.json} ({@code {variantId: baseId}}) and folds a variant
 * item id to its canonical base id.
 *
 * <p>This is the flat, O(1) equivalent of upstream {@code getCanonicalItemId} (Equipment.ts:202-208),
 * which scans the inverse {@code {baseId: [variantIds]}} shape in {@code EquipmentAliases.ts}. The
 * bundled JSON is pre-inverted to {@code {variant: base}}, so canonicalization is a single map lookup.
 */
public final class EquipmentAliases
{
	private final Map<Integer, Integer> variantToBase;

	private EquipmentAliases(Map<Integer, Integer> m) { this.variantToBase = m; }

	public static EquipmentAliases fromBundled(Gson gson)
	{
		Map<Integer, Integer> m = EquipmentRepository.readResource(
			gson, "/org/bossbis/data/equipment_aliases.json",
			new TypeToken<Map<Integer, Integer>>(){}.getType());
		return new EquipmentAliases(m);
	}

	public int canonical(int id) { return variantToBase.getOrDefault(id, id); }
}
