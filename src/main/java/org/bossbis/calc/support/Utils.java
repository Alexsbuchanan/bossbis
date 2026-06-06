package org.bossbis.calc.support;

/** Port of src/utils.ts helpers used by src/lib (isDefined, FeatureStatus). */
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

	// TODO(M3): port getCombatStylesForCategory once EquipmentCategory + PlayerCombatStyle exist
}
