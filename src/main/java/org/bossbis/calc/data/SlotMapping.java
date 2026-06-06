package org.bossbis.calc.data;

import java.util.EnumSet;
import java.util.Set;

/**
 * Maps weirdgloop {@code slot} strings (read from a matched {@code equipment.json} row) to a typed
 * {@link Slot} enum, and applies the §6.4 two-handed rule.
 *
 * <p>The slot is always read from the canonical equipment row — it is never inferred from a bank
 * item. A two-handed weapon occupies both {@link Slot#WEAPON} and {@link Slot#SHIELD} (i.e. it
 * displaces the shield); every other piece occupies only its own slot.
 */
public final class SlotMapping
{
	private SlotMapping() {}

	/** The eleven equipment slots, in the canonical worn-equipment order. */
	public enum Slot
	{
		HEAD, CAPE, NECK, AMMO, WEAPON, BODY, SHIELD, LEGS, HANDS, FEET, RING
	}

	/**
	 * Maps a lowercase weirdgloop slot string to its {@link Slot}.
	 *
	 * @throws IllegalArgumentException if the string is not one of the eleven known slots
	 */
	public static Slot fromWeirdgloop(String slot)
	{
		switch (slot)
		{
			case "head":   return Slot.HEAD;
			case "cape":   return Slot.CAPE;
			case "neck":   return Slot.NECK;
			case "ammo":   return Slot.AMMO;
			case "weapon": return Slot.WEAPON;
			case "body":   return Slot.BODY;
			case "shield": return Slot.SHIELD;
			case "legs":   return Slot.LEGS;
			case "hands":  return Slot.HANDS;
			case "feet":   return Slot.FEET;
			case "ring":   return Slot.RING;
			default:
				throw new IllegalArgumentException("Unknown weirdgloop slot: " + slot);
		}
	}

	/**
	 * Returns the slots a piece occupies. A two-handed weapon occupies {@code {WEAPON, SHIELD}};
	 * anything else occupies only its own slot.
	 */
	public static Set<Slot> occupies(Slot slot, boolean isTwoHanded)
	{
		if (slot == Slot.WEAPON && isTwoHanded)
		{
			return EnumSet.of(Slot.WEAPON, Slot.SHIELD);
		}
		return EnumSet.of(slot);
	}
}
