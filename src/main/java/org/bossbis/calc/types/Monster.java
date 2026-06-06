package org.bossbis.calc.types;

import com.google.gson.annotations.SerializedName;
import java.util.Set;
import lombok.Value;

/**
 * Port of the {@code Monster} interface (src/types/Monster.ts:15-58).
 *
 * <p>{@code style} is the {@code MonsterCombatStyle} (= nullable CombatStyleType string). The JSON
 * uses snake_case for {@code is_slayer_monster} (verified against a real monsters.json row), so it
 * carries {@link SerializedName}; the snake_case keys inside {@code offensive}/{@code defensive} are
 * handled by {@link MonsterOffensive}/{@link MonsterDefensive}.
 *
 * <p>{@code maxHit} is UI-only — kept as a field for round-trip fidelity but never used by the calc
 * (the engine computes its own max hit). {@code weakness} and {@code image} are nullable. The
 * per-encounter {@code inputs} block ({@link MonsterInputs}) is not part of the bundled monsters.json
 * row — it is assembled separately (see {@link MonsterInputs#initial()}).
 */
@Value
public class Monster
{
	int id;
	String name;
	String image;
	String version;
	int size;
	int speed;
	String style;        // MonsterCombatStyle = CombatStyleType, nullable
	Integer maxHit;      // UI-only; not used by the calc
	MonsterSkills skills;
	MonsterOffensive offensive;
	MonsterDefensive defensive;
	Set<MonsterAttribute> attributes;
	Weakness weakness;   // nullable
	Immunities immunities;
	@SerializedName("is_slayer_monster")
	boolean isSlayerMonster;
	MonsterInputs inputs;
}
