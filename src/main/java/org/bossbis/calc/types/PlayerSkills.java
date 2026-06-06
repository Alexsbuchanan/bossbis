package org.bossbis.calc.types;

import lombok.Value;

/**
 * Port of the {@code PlayerSkills} interface (src/types/Player.ts:8-18).
 *
 * <p>Used both for the player's base skill levels AND for {@code Player.boosts}, which holds
 * additive deltas (current = base + boost) that may be negative. Field/constructor order is
 * {@code (atk, def, hp, magic, prayer, ranged, str, mining, herblore)} — note {@code str} is
 * seventh, matching upstream. {@code mining} and {@code herblore} are required.
 */
@Value
public class PlayerSkills
{
	int atk;
	int def;
	int hp;
	int magic;
	int prayer;
	int ranged;
	int str;
	int mining;
	int herblore;
}
