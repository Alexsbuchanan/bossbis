package org.bossbis.calc.types;

import lombok.Value;

/**
 * Port of the {@code Monster.skills} object (src/types/Monster.ts:24-31).
 *
 * <p>All keys ({@code atk}/{@code def}/{@code hp}/{@code magic}/{@code ranged}/{@code str}) match
 * the camelCase Java names in monsters.json, so no
 * {@link com.google.gson.annotations.SerializedName} is needed.
 */
@Value
public class MonsterSkills
{
	int atk;
	int def;
	int hp;
	int magic;
	int ranged;
	int str;
}
