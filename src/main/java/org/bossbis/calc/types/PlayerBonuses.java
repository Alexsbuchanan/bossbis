package org.bossbis.calc.types;

import com.google.gson.annotations.SerializedName;
import lombok.Value;

/**
 * Port of the {@code PlayerBonuses} interface (src/types/Player.ts:54-59).
 *
 * <p>The JSON uses snake_case for {@code ranged_str}/{@code magic_str} (verified against a real
 * equipment.json row), so those carry {@link SerializedName}.
 */
@Value
public class PlayerBonuses
{
	int str;
	@SerializedName("ranged_str")
	int rangedStr;
	@SerializedName("magic_str")
	int magicStr;
	int prayer;
}
