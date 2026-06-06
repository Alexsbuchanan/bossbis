package org.bossbis.calc.data;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.bossbis.calc.types.EquipmentPiece;

/**
 * Repository over the bundled {@code equipment.json}, implementing the weirdgloop half of the §6
 * item-reconciliation pipeline (alias-fold then match-by-id).
 *
 * <p>Ports {@code getCanonicalItemId} (Equipment.ts:202-208) and {@code getCanonicalItem}
 * (Equipment.ts:209-226). {@link #canonicalId(int)} folds a variant id to its base via
 * {@link EquipmentAliases}; {@link #resolve(int)} returns the canonical row, falling back to a
 * literal-id row, else {@link Optional#empty()} (no stats — caller skips the item).
 */
public final class EquipmentRepository
{
	private final Map<Integer, EquipmentPiece> byId;
	private final EquipmentAliases aliases;

	private EquipmentRepository(Map<Integer, EquipmentPiece> byId, EquipmentAliases aliases)
	{
		this.byId = byId;
		this.aliases = aliases;
	}

	public static EquipmentRepository fromBundled(Gson gson)
	{
		EquipmentPiece[] all = readResource(gson, "/org/bossbis/data/equipment.json", EquipmentPiece[].class);
		Map<Integer, EquipmentPiece> byId = new HashMap<>(all.length * 2);
		for (EquipmentPiece p : all)
		{
			// Guard the single known duplicate id (32640, non-equipment leagues artifact): first wins.
			byId.putIfAbsent(p.getId(), p);
		}
		return new EquipmentRepository(byId, EquipmentAliases.fromBundled(gson));
	}

	public int canonicalId(int rlId)
	{
		return aliases.canonical(rlId);
	}

	public Optional<EquipmentPiece> resolve(int rlId)
	{
		EquipmentPiece byCanonical = byId.get(canonicalId(rlId));
		if (byCanonical != null)
		{
			return Optional.of(byCanonical);
		}
		return Optional.ofNullable(byId.get(rlId));   // non-aliased variant with its own row
	}

	/** Number of distinct equipment ids indexed (post dedup). */
	public int size()
	{
		return byId.size();
	}

	static <T> T readResource(Gson gson, String path, Type type)
	{
		try (InputStreamReader r = new InputStreamReader(
			EquipmentRepository.class.getResourceAsStream(path), StandardCharsets.UTF_8))
		{
			return gson.fromJson(r, type);   // Gson 2.8.5 reader API
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Failed to load bundled resource " + path, e);
		}
	}
}
