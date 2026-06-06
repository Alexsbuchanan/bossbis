package org.bossbis.calc.types;

import com.google.gson.annotations.SerializedName;
import lombok.Value;

/**
 * Port of the {@code Monster.offensive} object (src/types/Monster.ts:32-39).
 *
 * <p>The JSON uses snake_case for {@code magic_str}/{@code ranged_str} (verified against a real
 * monsters.json row), so those carry {@link SerializedName}.
 */
@Value
public class MonsterOffensive
{
	int atk;
	int magic;
	@SerializedName("magic_str")
	int magicStr;
	int ranged;
	@SerializedName("ranged_str")
	int rangedStr;
	int str;
}
