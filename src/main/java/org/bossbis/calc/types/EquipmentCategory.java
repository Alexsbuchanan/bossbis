package org.bossbis.calc.types;

import com.google.gson.annotations.SerializedName;

/**
 * Port of src/enums/EquipmentCategory.ts — the category of the weapon being used.
 * String-valued: each member carries the weirdgloop {@code value} that Gson reads
 * from equipment.json (see OSRS wiki Property:Combat_style). Gson matches enum
 * constants by NAME or {@link SerializedName} (not by {@link #value()}), so every
 * constant is annotated with its upstream string for correct deserialization.
 */
public enum EquipmentCategory
{
	@SerializedName("") NONE(""),
	@SerializedName("2h Sword") TWO_HANDED_SWORD("2h Sword"),
	@SerializedName("Axe") AXE("Axe"),
	@SerializedName("Banner") BANNER("Banner"),
	@SerializedName("Bladed Staff") BLADED_STAFF("Bladed Staff"),
	@SerializedName("Blaster") BLASTER("Blaster"),
	@SerializedName("Bludgeon") BLUDGEON("Bludgeon"),
	@SerializedName("Blunt") BLUNT("Blunt"),
	@SerializedName("Bow") BOW("Bow"),
	@SerializedName("Bulwark") BULWARK("Bulwark"),
	@SerializedName("Chinchompas") CHINCHOMPA("Chinchompas"),
	@SerializedName("Claw") CLAW("Claw"),
	@SerializedName("Crossbow") CROSSBOW("Crossbow"),
	@SerializedName("Dagger") DAGGER("Dagger"),
	@SerializedName("Gun") GUN("Gun"),
	@SerializedName("Multi-Melee") MULTI_MELEE("Multi-Melee"),
	@SerializedName("Partisan") PARTISAN("Partisan"),
	@SerializedName("Pickaxe") PICKAXE("Pickaxe"),
	@SerializedName("Polearm") POLEARM("Polearm"),
	@SerializedName("Polestaff") POLESTAFF("Polestaff"),
	@SerializedName("Powered Staff") POWERED_STAFF("Powered Staff"),
	@SerializedName("Powered Wand") POWERED_WAND("Powered Wand"),
	@SerializedName("Salamander") SALAMANDER("Salamander"),
	@SerializedName("Scythe") SCYTHE("Scythe"),
	@SerializedName("Slash Sword") SLASH_SWORD("Slash Sword"),
	@SerializedName("Spear") SPEAR("Spear"),
	@SerializedName("Spiked") SPIKED("Spiked"),
	@SerializedName("Stab Sword") STAB_SWORD("Stab Sword"),
	@SerializedName("Staff") STAFF("Staff"),
	@SerializedName("Thrown") THROWN("Thrown"),
	@SerializedName("Unarmed") UNARMED("Unarmed"),
	@SerializedName("Whip") WHIP("Whip"),
	;

	private final String value;

	EquipmentCategory(String value)
	{
		this.value = value;
	}

	public String value()
	{
		return value;
	}

	public static EquipmentCategory fromValue(String v)
	{
		for (EquipmentCategory c : values())
		{
			if (c.value.equals(v))
			{
				return c;
			}
		}
		return NONE;
	}
}
