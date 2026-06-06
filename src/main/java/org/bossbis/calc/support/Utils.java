package org.bossbis.calc.support;

import java.util.ArrayList;
import java.util.List;
import org.bossbis.calc.types.CombatStyle.PlayerCombatStyle;
import org.bossbis.calc.types.EquipmentCategory;

/** Port of src/utils.ts helpers used by src/lib (isDefined, FeatureStatus, getCombatStylesForCategory). */
public final class Utils
{
	private Utils() {}

	/**
	 * Port of utils.ts:36 FeatureStatus — declaration order == upstream numeric value.
	 * Used to distinguish whether a partially-supported feature is implemented.
	 */
	public enum FeatureStatus
	{
		/** The feature is fully implemented and is expected to give accurate and comprehensive results. */
		IMPLEMENTED,

		/** The feature is partially but not wholly implemented, and may or may not be accurate. */
		PARTIALLY_IMPLEMENTED,

		/** The feature is known to exist, but has not been implemented. */
		UNIMPLEMENTED,

		/** The feature does not apply to the given conditions. */
		NOT_APPLICABLE,
	}

	/** Port of utils.ts:136 isDefined — for type narrowing; rejects null (and JS undefined). */
	public static boolean isDefined(Object id)
	{
		return id != null;
	}

	private static PlayerCombatStyle cs(String name, String type, String stance)
	{
		return new PlayerCombatStyle(name, type, stance);
	}

	/**
	 * Port of {@code getCombatStylesForCategory} (utils.ts:602-817). Returns the ordered combat styles
	 * available for a weapon category, with the pseudo Manual-Cast style appended (utils.ts:813-815).
	 * The {@code BLASTER} case returns an empty list verbatim (no Manual-Cast appended), matching the
	 * early {@code return []} in upstream.
	 */
	public static List<PlayerCombatStyle> getCombatStylesForCategory(EquipmentCategory style)
	{
		List<PlayerCombatStyle> ret = new ArrayList<>();
		switch (style)
		{
			case TWO_HANDED_SWORD:
				ret.add(cs("Chop", "slash", "Accurate"));
				ret.add(cs("Slash", "slash", "Aggressive"));
				ret.add(cs("Smash", "crush", "Aggressive"));
				ret.add(cs("Block", "slash", "Defensive"));
				break;
			case BANNER:
				ret.add(cs("Lunge", "stab", "Accurate"));
				ret.add(cs("Swipe", "slash", "Aggressive"));
				ret.add(cs("Pound", "crush", "Controlled"));
				ret.add(cs("Block", "stab", "Defensive"));
				break;
			case BLADED_STAFF:
				ret.add(cs("Jab", "stab", "Accurate"));
				ret.add(cs("Swipe", "slash", "Aggressive"));
				ret.add(cs("Fend", "crush", "Defensive"));
				ret.add(cs("Spell", "magic", "Defensive Autocast"));
				ret.add(cs("Spell", "magic", "Autocast"));
				break;
			case BLASTER:
				// TODO? (upstream returns [] without appending Manual Cast)
				return new ArrayList<>();
			case BOW:
			case CROSSBOW:
			case THROWN:
				ret.add(cs("Accurate", "ranged", "Accurate"));
				ret.add(cs("Rapid", "ranged", "Rapid"));
				ret.add(cs("Longrange", "ranged", "Longrange"));
				break;
			case GUN:
				ret.add(cs("Kick", "crush", "Aggressive"));
				break;
			case BULWARK:
				ret.add(cs("Pummel", "crush", "Accurate"));
				ret.add(cs("Block", null, null));
				break;
			case MULTI_MELEE:
				ret.add(cs("Poke", "stab", "Accurate"));
				ret.add(cs("Slash", "slash", "Aggressive"));
				ret.add(cs("Pound", "crush", "Aggressive"));
				ret.add(cs("Block", "slash", "Defensive"));
				break;
			case PARTISAN:
				ret.add(cs("Stab", "stab", "Accurate"));
				ret.add(cs("Lunge", "stab", "Aggressive"));
				ret.add(cs("Pound", "crush", "Aggressive"));
				ret.add(cs("Block", "stab", "Defensive"));
				break;
			case PICKAXE:
				ret.add(cs("Spike", "stab", "Accurate"));
				ret.add(cs("Impale", "stab", "Aggressive"));
				ret.add(cs("Smash", "crush", "Aggressive"));
				ret.add(cs("Block", "stab", "Defensive"));
				break;
			case POLEARM:
				ret.add(cs("Jab", "stab", "Controlled"));
				ret.add(cs("Swipe", "slash", "Aggressive"));
				ret.add(cs("Fend", "stab", "Defensive"));
				break;
			case POWERED_STAFF:
			case POWERED_WAND:
				ret.add(cs("Accurate", "magic", "Accurate"));
				ret.add(cs("Accurate", "magic", "Accurate"));
				ret.add(cs("Longrange", "magic", "Longrange"));
				break;
			case SALAMANDER:
				ret.add(cs("Scorch", "slash", "Aggressive"));
				ret.add(cs("Flare", "ranged", "Rapid"));
				ret.add(cs("Blaze", "magic", "Defensive"));
				break;
			case CHINCHOMPA:
				ret.add(cs("Short fuse", "ranged", "Accurate"));
				ret.add(cs("Medium fuse", "ranged", "Rapid"));
				ret.add(cs("Long fuse", "ranged", "Longrange"));
				break;
			case CLAW:
				ret.add(cs("Chop", "slash", "Accurate"));
				ret.add(cs("Slash", "slash", "Aggressive"));
				ret.add(cs("Lunge", "stab", "Controlled"));
				ret.add(cs("Block", "slash", "Defensive"));
				break;
			case BLUDGEON:
				ret.add(cs("Pound", "crush", "Aggressive"));
				ret.add(cs("Pummel", "crush", "Aggressive"));
				ret.add(cs("Smash", "crush", "Aggressive"));
				break;
			case BLUNT:
				ret.add(cs("Pound", "crush", "Accurate"));
				ret.add(cs("Pummel", "crush", "Aggressive"));
				ret.add(cs("Block", "crush", "Defensive"));
				break;
			case POLESTAFF:
				ret.add(cs("Bash", "crush", "Accurate"));
				ret.add(cs("Pound", "crush", "Aggressive"));
				ret.add(cs("Block", "crush", "Defensive"));
				break;
			case SPIKED:
				ret.add(cs("Pound", "crush", "Accurate"));
				ret.add(cs("Pummel", "crush", "Aggressive"));
				ret.add(cs("Spike", "stab", "Controlled"));
				ret.add(cs("Block", "crush", "Defensive"));
				break;
			case STAFF:
				ret.add(cs("Bash", "crush", "Accurate"));
				ret.add(cs("Pound", "crush", "Aggressive"));
				ret.add(cs("Focus", "crush", "Defensive"));
				ret.add(cs("Spell", "magic", "Defensive Autocast"));
				ret.add(cs("Spell", "magic", "Autocast"));
				break;
			case AXE:
				ret.add(cs("Chop", "slash", "Accurate"));
				ret.add(cs("Hack", "slash", "Aggressive"));
				ret.add(cs("Smash", "crush", "Aggressive"));
				ret.add(cs("Block", "slash", "Defensive"));
				break;
			case NONE:
			case UNARMED:
				ret.add(cs("Punch", "crush", "Accurate"));
				ret.add(cs("Kick", "crush", "Aggressive"));
				ret.add(cs("Block", "crush", "Defensive"));
				break;
			case SCYTHE:
				ret.add(cs("Reap", "slash", "Accurate"));
				ret.add(cs("Chop", "slash", "Aggressive"));
				ret.add(cs("Jab", "crush", "Aggressive"));
				ret.add(cs("Block", "slash", "Defensive"));
				break;
			case SLASH_SWORD:
				ret.add(cs("Chop", "slash", "Accurate"));
				ret.add(cs("Slash", "slash", "Aggressive"));
				ret.add(cs("Lunge", "stab", "Controlled"));
				ret.add(cs("Block", "slash", "Defensive"));
				break;
			case SPEAR:
				ret.add(cs("Lunge", "stab", "Controlled"));
				ret.add(cs("Swipe", "slash", "Controlled"));
				ret.add(cs("Pound", "crush", "Controlled"));
				ret.add(cs("Block", "stab", "Defensive"));
				break;
			case STAB_SWORD:
				ret.add(cs("Stab", "stab", "Accurate"));
				ret.add(cs("Lunge", "stab", "Aggressive"));
				ret.add(cs("Slash", "slash", "Aggressive"));
				ret.add(cs("Block", "stab", "Defensive"));
				break;
			case WHIP:
				ret.add(cs("Flick", "slash", "Accurate"));
				ret.add(cs("Lash", "slash", "Controlled"));
				ret.add(cs("Deflect", "slash", "Defensive"));
				break;
			default:
				break;
		}

		// Add a pseudo combat style here for manual casting
		ret.add(cs("Spell", "magic", "Manual Cast"));
		return ret;
	}
}
