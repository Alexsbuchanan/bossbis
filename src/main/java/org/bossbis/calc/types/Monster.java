package org.bossbis.calc.types;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Set;
import lombok.Value;

/**
 * Port of the {@code Monster} interface (src/types/Monster.ts:15-58).
 *
 * <p>{@code style} is the monster's combat style(s). The upstream TS declares this as a single
 * {@code MonsterCombatStyle}, but the bundled monsters.json carries it as an ARRAY of capitalized
 * strings (e.g. {@code ["Crush"]}, {@code ["Slash","Ranged"]}, {@code ["None"]}) and includes values
 * outside {@code CombatStyleType} (e.g. {@code "All"}, {@code "Various"}, {@code "Typeless"}). It is
 * therefore modeled as {@code List<String>} — the faithful, safe Java type that Gson populates
 * directly from the JSON array. The JSON uses snake_case for {@code is_slayer_monster} (verified
 * against a real monsters.json row), so it carries {@link SerializedName}; the snake_case keys inside
 * {@code offensive}/{@code defensive} are handled by {@link MonsterOffensive}/{@link MonsterDefensive}.
 *
 * <p>{@code maxHit} is UI-only — kept as a field for round-trip fidelity but never used by the calc
 * (the engine computes its own max hit). The JSON value is a STRING ({@code "max_hit"}) that can be
 * non-integer/multi-phase, so it is modeled as {@code String} to avoid breaking parsing.
 * {@code weakness} and {@code image} are nullable. The per-encounter {@code inputs} block
 * ({@link MonsterInputs}) is not part of the bundled monsters.json row — it is assembled separately
 * (see {@link MonsterInputs#initial()}).
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
	List<String> style;  // capitalized style names, e.g. ["Crush"], ["Slash","Ranged"], ["None"]
	@SerializedName("max_hit")
	String maxHit;       // UI-only String; can be non-integer/multi-phase; not used by the calc
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
