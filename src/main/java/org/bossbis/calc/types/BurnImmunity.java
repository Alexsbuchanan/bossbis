package org.bossbis.calc.types;

/**
 * Port of src/types/Monster.ts:9-13 BurnImmunity — string-valued.
 * Each member carries the weirdgloop {@code value} that Gson reads from monsters.json.
 */
public enum BurnImmunity
{
	WEAK("Weak"),
	NORMAL("Normal"),
	STRONG("Strong"),
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
