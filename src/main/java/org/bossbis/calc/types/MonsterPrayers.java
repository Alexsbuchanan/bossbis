package org.bossbis.calc.types;

import lombok.Value;

/**
 * Port of the {@code inputs.prayers} object (src/types/Monster.ts:127-131).
 *
 * <p>Whether the monster is praying against each combat style. All keys are camelCase, so no
 * {@link com.google.gson.annotations.SerializedName} is needed.
 */
@Value
public class MonsterPrayers
{
	boolean melee;
	boolean ranged;
	boolean magic;
}
