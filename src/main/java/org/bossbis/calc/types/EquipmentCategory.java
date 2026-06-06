package org.bossbis.calc.types;

/**
 * Port of src/enums/EquipmentCategory.ts — the category of the weapon being used.
 * String-valued: each member carries the weirdgloop {@code value} that Gson reads
 * from equipment.json (see OSRS wiki Property:Combat_style).
 */
public enum EquipmentCategory
{
	NONE(""),
	TWO_HANDED_SWORD("2h Sword"),
	AXE("Axe"),
	BANNER("Banner"),
	BLADED_STAFF("Bladed Staff"),
	BLASTER("Blaster"),
	BLUDGEON("Bludgeon"),
	BLUNT("Blunt"),
	BOW("Bow"),
	BULWARK("Bulwark"),
	CHINCHOMPA("Chinchompas"),
	CLAW("Claw"),
	CROSSBOW("Crossbow"),
	DAGGER("Dagger"),
	GUN("Gun"),
	MULTI_MELEE("Multi-Melee"),
	PARTISAN("Partisan"),
	PICKAXE("Pickaxe"),
	POLEARM("Polearm"),
	POLESTAFF("Polestaff"),
	POWERED_STAFF("Powered Staff"),
	POWERED_WAND("Powered Wand"),
	SALAMANDER("Salamander"),
	SCYTHE("Scythe"),
	SLASH_SWORD("Slash Sword"),
	SPEAR("Spear"),
	SPIKED("Spiked"),
	STAB_SWORD("Stab Sword"),
	STAFF("Staff"),
	THROWN("Thrown"),
	UNARMED("Unarmed"),
	WHIP("Whip"),
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
