package org.bossbis.calc;

/** Port of src/lib/constants.ts — scalar constants only (ID membership arrays ported in v0.1.1+ with their consumers). */
public final class Constants
{
	private Constants() {}

	public static final double SECONDS_PER_TICK = 0.6;
	public static final int DEFAULT_ATTACK_SPEED = 4;
	public static final double TTK_DIST_EPSILON = 0.0001;
	public static final int TTK_DIST_MAX_ITER_ROUNDS = 1000;
}
