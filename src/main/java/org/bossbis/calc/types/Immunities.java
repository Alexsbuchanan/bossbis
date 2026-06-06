package org.bossbis.calc.types;

import lombok.Value;

/**
 * Port of the {@code Monster.immunities} object (src/types/Monster.ts:55-57).
 *
 * <p>Currently only a nullable burn immunity. {@code burn} is camelCase, so no
 * {@link com.google.gson.annotations.SerializedName} is needed.
 */
@Value
public class Immunities
{
	BurnImmunity burn;
}
