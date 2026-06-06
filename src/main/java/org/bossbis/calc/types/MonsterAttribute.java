package org.bossbis.calc.types;

/**
 * Port of src/enums/MonsterAttribute.ts — string-valued monster attributes.
 * Each member carries the weirdgloop {@code value} that Gson reads from monsters.json.
 */
public enum MonsterAttribute
{
	DEMON("demon"),
	DRAGON("dragon"),
	FIERY("fiery"),
	FLYING("flying"),
	GOLEM("golem"),
	KALPHITE("kalphite"),
	LEAFY("leafy"),
	PENANCE("penance"),
	RAT("rat"),
	SHADE("shade"),
	SPECTRAL("spectral"),
	UNDEAD("undead"),
	VAMPYRE1("vampyre1"),
	VAMPYRE2("vampyre2"),
	VAMPYRE3("vampyre3"),
	XERICIAN("xerician"),
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
}
