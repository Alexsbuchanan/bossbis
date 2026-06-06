package org.bossbis.calc.types;

import lombok.Value;

/**
 * Port of the {@code PlayerOffensive} interface (src/types/Player.ts:69-75).
 *
 * <p>All keys ({@code stab}/{@code slash}/{@code crush}/{@code magic}/{@code ranged}) match the
 * camelCase Java names in equipment.json, so no {@link com.google.gson.annotations.SerializedName}
 * annotations are needed.
 */
@Value
public class PlayerOffensive
{
	int stab;
	int slash;
	int crush;
	int magic;
	int ranged;
}
