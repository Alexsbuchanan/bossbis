package org.bossbis.calc.types;

import com.google.gson.annotations.SerializedName;
import lombok.Value;

/**
 * Port of src/types/Spell.ts — a single combat spell.
 *
 * <p>Loaded from spells.json via Gson. {@code max_hit} maps to {@code maxHit};
 * {@code spellbook}/{@code element} are lowercase string-valued enums (Spellbook/Spellement)
 * whose constants carry {@link SerializedName} so Gson reads/writes the upstream strings.
 */
@Value
public class Spell
{
	String name;
	String image;
	@SerializedName("max_hit")
	int maxHit;
	Spellbook spellbook;
	Spellement element;
	boolean unselectable;

	/** Port of the Spellbook union: 'standard' | 'ancient' | 'lunar' | 'arceuus'. */
	public enum Spellbook
	{
		@SerializedName("standard")
		STANDARD,
		@SerializedName("ancient")
		ANCIENT,
		@SerializedName("lunar")
		LUNAR,
		@SerializedName("arceuus")
		ARCEUUS,
	}

	/** Port of the Spellement union: 'air' | 'water' | 'earth' | 'fire'. */
	public enum Spellement
	{
		@SerializedName("air")
		AIR,
		@SerializedName("water")
		WATER,
		@SerializedName("earth")
		EARTH,
		@SerializedName("fire")
		FIRE,
	}
}
