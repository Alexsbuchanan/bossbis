package org.bossbis.calc.types;

import java.util.Arrays;
import java.util.List;
import lombok.Value;

/**
 * Port of src/types/PlayerCombatStyle.ts.
 *
 * <p>Holds the {@link PlayerCombatStyle} record plus the combat-style/stance/ranged-damage
 * vocabularies and {@code getRangedDamageType}. The upstream {@code CombatStyleType} is a
 * union of five string literals OR {@code null}; we model it as a nullable {@code String}
 * field (the canonical values are exposed as constants), NOT as a {@code NULL} enum member.
 */
public final class CombatStyle
{
	private CombatStyle() {}

	/** Port of CombatStyleTypes — the five attack types (a CombatStyleType may also be null). */
	public static final String STAB = "stab";
	public static final String SLASH = "slash";
	public static final String CRUSH = "crush";
	public static final String MAGIC = "magic";
	public static final String RANGED = "ranged";

	public static final List<String> COMBAT_STYLE_TYPES = Arrays.asList(STAB, SLASH, CRUSH, MAGIC, RANGED);

	/** Port of RangedDamageTypes — the player stat is still "ranged", so these are kept separate. */
	public static final String LIGHT = "light";
	public static final String STANDARD = "standard";
	public static final String HEAVY = "heavy";
	public static final String MIXED = "mixed";

	public static final List<String> RANGED_DAMAGE_TYPES = Arrays.asList(LIGHT, STANDARD, HEAVY, MIXED);

	/** Port of CombatStyleStance — the valid stance strings (a stance may also be null). */
	public static final String ACCURATE = "Accurate";
	public static final String AGGRESSIVE = "Aggressive";
	public static final String AUTOCAST = "Autocast";
	public static final String CONTROLLED = "Controlled";
	public static final String DEFENSIVE = "Defensive";
	public static final String DEFENSIVE_AUTOCAST = "Defensive Autocast";
	public static final String LONGRANGE = "Longrange";
	public static final String RAPID = "Rapid";
	public static final String MANUAL_CAST = "Manual Cast"; // pseudo stance

	/** Port of isCombatStyleType — null counts as a valid CombatStyleType, undefined does not. */
	public static boolean isCombatStyleType(String s)
	{
		return s == null || COMBAT_STYLE_TYPES.contains(s);
	}

	/**
	 * Port of getRangedDamageType — maps a ranged weapon category to its damage type.
	 * Throws for non-ranged categories (mirrors the upstream {@code throw new Error}).
	 */
	public static String getRangedDamageType(EquipmentCategory category)
	{
		switch (category)
		{
			case THROWN:
				return LIGHT;
			case BOW:
				return STANDARD;
			case CROSSBOW:
			case CHINCHOMPA:
				return HEAVY;
			case SALAMANDER:
				return MIXED;
			default:
				throw new IllegalArgumentException("Not a ranged weapon category: " + category);
		}
	}

	/** Port of the PlayerCombatStyle interface ({@code type}/{@code stance} are nullable). */
	@Value
	public static class PlayerCombatStyle
	{
		String name;
		String type;    // CombatStyleType: one of COMBAT_STYLE_TYPES, or null
		String stance;  // CombatStyleStance: one of the stance constants, or null
	}
}
