package org.bossbis.calc.types;

import com.google.gson.annotations.SerializedName;

/**
 * Port of src/enums/MonsterAttribute.ts — string-valued monster attributes.
 * Each member carries the weirdgloop {@code value} that Gson reads from monsters.json.
 * Gson matches enum constants by NAME or {@link SerializedName} (not by {@link #value()}),
 * so every constant is annotated with its lowercase upstream string.
 */
public enum MonsterAttribute
{
	@SerializedName("demon") DEMON("demon"),
	@SerializedName("dragon") DRAGON("dragon"),
	@SerializedName("fiery") FIERY("fiery"),
	@SerializedName("flying") FLYING("flying"),
	@SerializedName("golem") GOLEM("golem"),
	@SerializedName("kalphite") KALPHITE("kalphite"),
	@SerializedName("leafy") LEAFY("leafy"),
	@SerializedName("penance") PENANCE("penance"),
	@SerializedName("rat") RAT("rat"),
	@SerializedName("shade") SHADE("shade"),
	@SerializedName("spectral") SPECTRAL("spectral"),
	@SerializedName("undead") UNDEAD("undead"),
	@SerializedName("vampyre1") VAMPYRE1("vampyre1"),
	@SerializedName("vampyre2") VAMPYRE2("vampyre2"),
	@SerializedName("vampyre3") VAMPYRE3("vampyre3"),
	@SerializedName("xerician") XERICIAN("xerician"),
	;

	private final String value;

	MonsterAttribute(String value)
	{
		this.value = value;
	}

	public String value()
	{
		return value;
	}

	public static MonsterAttribute fromValue(String v)
	{
		for (MonsterAttribute a : values())
		{
			if (a.value.equals(v))
			{
				return a;
			}
		}
		return null;
	}

	/** Port of isVampyre — true if the attribute is any of the three vampyre tiers. */
	public boolean isVampyre()
	{
		return this == VAMPYRE1 || this == VAMPYRE2 || this == VAMPYRE3;
	}

	/**
	 * Port of the array overload of {@code isVampyre} (src/enums/MonsterAttribute.ts:20-24) — true if
	 * any attribute in the collection is a vampyre tier.
	 */
	public static boolean isVampyre(Iterable<MonsterAttribute> attrs)
	{
		if (attrs == null)
		{
			return false;
		}
		for (MonsterAttribute a : attrs)
		{
			if (a != null && a.isVampyre())
			{
				return true;
			}
		}
		return false;
	}
}
