package org.bossbis.calc.types;

import lombok.Value;

/**
 * Port of the {@code Player.buffs} object (src/types/Player.ts:121-168).
 *
 * <p>All keys are already camelCase, so no {@link com.google.gson.annotations.SerializedName} is
 * needed. The upstream {@code buffs.potions} array is UI-only ("should NOT be used in calculator
 * code") and is intentionally omitted from this calc model (spec §4.2); potion deltas are carried
 * by {@code Player.boosts} instead.
 */
@Value
public class Buffs
{
	boolean onSlayerTask;
	boolean inWilderness;
	boolean forinthrySurge;
	int soulreaperStacks;
	int baAttackerLevel;
	int chinchompaDistance;
	boolean kandarinDiary;
	boolean chargeSpell;
	boolean markOfDarknessSpell;
	boolean usingSunfireRunes;
}
