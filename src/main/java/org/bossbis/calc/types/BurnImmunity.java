package org.bossbis.calc.types;

import com.google.gson.annotations.SerializedName;

/**
 * Port of src/types/Monster.ts:9-13 BurnImmunity — string-valued.
 * Each member carries the weirdgloop {@code value} that Gson reads from monsters.json.
 * Gson matches enum constants by NAME or {@link SerializedName} (not by {@link #value()}),
 * so every constant is annotated with its capitalized upstream string.
 */
public enum BurnImmunity
{
	@SerializedName("Weak") WEAK("Weak"),
	@SerializedName("Normal") NORMAL("Normal"),
	@SerializedName("Strong") STRONG("Strong"),
	;

	private final String value;

	BurnImmunity(String value)
	{
		this.value = value;
	}

	public String value()
	{
		return value;
	}

	public static BurnImmunity fromValue(String v)
	{
		for (BurnImmunity b : values())
		{
			if (b.value.equals(v))
			{
				return b;
			}
		}
		return null;
	}
}
