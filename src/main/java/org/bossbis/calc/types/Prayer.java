package org.bossbis.calc.types;

import org.bossbis.calc.CalcMath.Factor;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Port of src/enums/Prayer.ts (numeric enum 0..30 + PrayerData + PrayerMap).
 * Declaration order == upstream numeric value, so {@code ordinal()} matches the
 * TS numeric value (the calc indexes prayers by their numeric value).
 *
 * <p>Factors are denominator-of-100 [numerator, divisor] pairs so additive
 * prayers combine correctly (see upstream PrayerMap comment).
 */
public enum Prayer
{
	BURST_OF_STRENGTH(new PrayerData(1, "Burst of Strength", 1, "melee", null,
		null, new Factor(105, 100), null, null, false)),
	CLARITY_OF_THOUGHT(new PrayerData(2, "Clarity of Thought", 1, "melee", null,
		new Factor(105, 100), null, null, null, false)),
	SHARP_EYE(new PrayerData(3, "Sharp Eye", 1, "ranged", null,
		new Factor(105, 100), new Factor(105, 100), null, null, false)),
	MYSTIC_WILL(new PrayerData(4, "Mystic Will", 1, "magic", null,
		new Factor(105, 100), null, null, new Factor(105, 100), false)),
	SUPERHUMAN_STRENGTH(new PrayerData(5, "Superhuman Strength", 6, "melee", null,
		null, new Factor(110, 100), null, null, false)),
	IMPROVED_REFLEXES(new PrayerData(6, "Improved Reflexes", 6, "melee", null,
		new Factor(110, 100), null, null, null, false)),
	HAWK_EYE(new PrayerData(7, "Hawk Eye", 6, "ranged", null,
		new Factor(110, 100), new Factor(110, 100), null, null, false)),
	MYSTIC_LORE(new PrayerData(8, "Mystic Lore", 6, "magic", 10d,
		new Factor(110, 100), null, null, new Factor(110, 100), false)),
	ULTIMATE_STRENGTH(new PrayerData(9, "Ultimate Strength", 12, "melee", null,
		null, new Factor(115, 100), null, null, false)),
	INCREDIBLE_REFLEXES(new PrayerData(10, "Incredible Reflexes", 12, "melee", null,
		new Factor(115, 100), null, null, null, false)),
	EAGLE_EYE(new PrayerData(11, "Eagle Eye", 12, "ranged", null,
		new Factor(115, 100), new Factor(115, 100), null, null, false)),
	MYSTIC_MIGHT(new PrayerData(12, "Mystic Might", 12, "magic", 20d,
		new Factor(115, 100), null, null, new Factor(115, 100), false)),
	CHIVALRY(new PrayerData(14, "Chivalry", 24, "melee", null,
		new Factor(115, 100), new Factor(118, 100), new Factor(120, 100), null, false)),
	PIETY(new PrayerData(18, "Piety", 24, "melee", null,
		new Factor(120, 100), new Factor(123, 100), new Factor(125, 100), null, false)),
	RIGOUR(new PrayerData(19, "Rigour", 24, "ranged", null,
		new Factor(120, 100), new Factor(123, 100), new Factor(125, 100), null, false)),
	AUGURY(new PrayerData(20, "Augury", 24, "magic", 40d,
		new Factor(125, 100), null, new Factor(125, 100), new Factor(125, 100), false)),
	THICK_SKIN(new PrayerData(13, "Thick Skin", 1, null, null,
		null, null, new Factor(105, 100), null, false)),
	ROCK_SKIN(new PrayerData(17, "Rock Skin", 6, null, null,
		null, null, new Factor(110, 100), null, false)),
	STEEL_SKIN(new PrayerData(21, "Steel Skin", 12, null, null,
		null, null, new Factor(115, 100), null, false)),
	DEADEYE(new PrayerData(15, "Deadeye", 12, "ranged", null,
		new Factor(118, 100), new Factor(118, 100), new Factor(105, 100), null, false)),
	MYSTIC_VIGOUR(new PrayerData(16, "Mystic Vigour", 12, "magic", 30d,
		new Factor(118, 100), null, new Factor(105, 100), new Factor(118, 100), false)),
	PROTECT_MAGIC(new PrayerData(4, "Protect from Magic", 12, null, null,
		null, null, null, null, true)),
	PROTECT_RANGED(new PrayerData(6, "Protect from Missiles", 12, null, null,
		null, null, null, null, true)),
	PROTECT_MELEE(new PrayerData(5, "Protect from Melee", 12, null, null,
		null, null, null, null, true)),
	RETRIBUTION(new PrayerData(7, "Retribution", 3, null, null,
		null, null, null, null, true)),
	REDEMPTION(new PrayerData(8, "Redemption", 6, null, null,
		null, null, null, null, true)),
	SMITE(new PrayerData(9, "Smite", 18, null, null,
		null, null, null, null, true)),
	RAPID_RESTORE(new PrayerData(1, "Rapid Restore", 1, null, null,
		null, null, null, null, true)),
	RAPID_HEAL(new PrayerData(2, "Rapid Heal", 2, null, null,
		null, null, null, null, true)),
	PROTECT_ITEM(new PrayerData(3, "Protect Item", 2, null, null,
		null, null, null, null, true)),
	PRESERVE(new PrayerData(10, "Preserve", 2, null, null,
		null, null, null, null, true)),
	;

	private final PrayerData data;

	Prayer(PrayerData data)
	{
		this.data = data;
	}

	public PrayerData data()
	{
		return data;
	}

	@Value
	@Accessors(fluent = true)
	public static class PrayerData
	{
		int renderOrder;
		String name;
		double drainRate;
		String combatStyle;          // "melee" | "ranged" | "magic" | null
		Double magicDamageBonus;     // nullable
		Factor factorAccuracy;       // nullable
		Factor factorStrength;       // nullable
		Factor factorDefence;        // nullable
		Factor factorDefenceMagic;   // nullable
		boolean renderInOther;       // overhead/utility prayers rendered in a separate group
	}
}
