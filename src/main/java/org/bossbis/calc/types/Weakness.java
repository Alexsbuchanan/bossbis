package org.bossbis.calc.types;

import lombok.Value;
import org.bossbis.calc.types.Spell.Spellement;

/**
 * Port of the nullable {@code Monster.weakness} object (src/types/Monster.ts:51-54).
 *
 * <p>An elemental weakness: which {@link Spellement} element and the percentage severity. The whole
 * object is null when the monster has no weakness. {@code element}/{@code severity} are camelCase,
 * so no {@link com.google.gson.annotations.SerializedName} is needed.
 */
@Value
public class Weakness
{
	Spellement element;
	int severity;
}
