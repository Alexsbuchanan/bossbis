package org.bossbis.calc.types;

import lombok.Value;

/**
 * Port of the {@code inputs.defenceReductions} object (src/types/Monster.ts:107-118).
 *
 * <p>{@code vulnerability}/{@code accursed} are boolean spell flags; the rest are integer counts of
 * stacked defence-reduction special attacks. All keys are camelCase, so no
 * {@link com.google.gson.annotations.SerializedName} is needed.
 */
@Value
public class DefenceReductions
{
	boolean vulnerability;
	boolean accursed;
	int elderMaul;
	int dwh;
	int arclight;
	int emberlight;
	int bgs;
	int tonalztic;
	int seercull;
	int ayak;
}
