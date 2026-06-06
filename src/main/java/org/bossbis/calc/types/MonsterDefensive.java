package org.bossbis.calc.types;

import com.google.gson.annotations.SerializedName;
import lombok.Value;

/**
 * Port of the {@code Monster.defensive} object (src/types/Monster.ts:40-44).
 *
 * <p>Carries {@code flat_armour} plus the melee/magic defences {@code stab}/{@code slash}/
 * {@code crush}/{@code magic}, and the three ranged subtypes {@code light}/{@code standard}/
 * {@code heavy} (there is no single {@code ranged} key — it is split). Only {@code flat_armour}
 * differs from camelCase (verified against a real monsters.json row), so it carries
 * {@link SerializedName}.
 */
@Value
public class MonsterDefensive
{
	@SerializedName("flat_armour")
	int flatArmour;
	int stab;
	int slash;
	int crush;
	int magic;
	int light;
	int standard;
	int heavy;
}
