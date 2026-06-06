package org.bossbis.calc.types;

/**
 * Port of src/enums/UserIssueType.ts — string-valued issue identifiers.
 * Each member carries the weirdgloop {@code value} string used in the UI/JSON.
 */
public enum UserIssueType
{
	EQUIPMENT_MISSING_AMMO("equipment_slot_ammo_missing"),
	EQUIPMENT_WRONG_AMMO("equipment_slot_ammo_wrong"),
	EQUIPMENT_SET_EFFECT_UNSUPPORTED("equipment_slot_body_unsupported_set_effect"),
	WEAPON_WRONG_MONSTER("equipment_slot_weapon_wrong_monster"),
	EQUIPMENT_SPEC_UNSUPPORTED("pvm_results_weapon_unsupported_spec"),
	SPELL_WRONG_WEAPON("spell_wrong_weapon"),
	SPELL_WRONG_MONSTER("spell_wrong_monster"),
	MONSTER_UNIQUE_EFFECTS("monster_overall_unique_effects"),
	RING_RECOIL_UNSUPPORTED("equipment_slot_ring_recoil"),
	FEET_RECOIL_UNSUPPORTED("equipment_slot_feet_recoil"),
	HANDS_EFFECT_UNSUPPORTED("equipment_slot_hands_effect"),
	LEAGUES_SIX_TALENT_UNSUPPORTED("leagues_six_talent_unsupported"),
	;

	private final String value;

	UserIssueType(String value)
	{
		this.value = value;
	}

	public String value()
	{
		return value;
	}

	public static UserIssueType fromValue(String v)
	{
		for (UserIssueType t : values())
		{
			if (t.value.equals(v))
			{
				return t;
			}
		}
		return null;
	}
}
