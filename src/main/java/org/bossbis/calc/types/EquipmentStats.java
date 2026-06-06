package org.bossbis.calc.types;

import lombok.Value;

/**
 * Port of the {@code EquipmentStats} interface (src/types/Player.ts:77-81).
 *
 * <p>The aggregated stat triple carried at the {@code Player} level (the sum of all worn gear).
 * {@link EquipmentPiece} does NOT extend this — it inlines the three nested objects as direct
 * fields so the per-item JSON maps directly — but {@code Player} carries this aggregate.
 */
@Value
public class EquipmentStats
{
	PlayerBonuses bonuses;
	PlayerOffensive offensive;
	PlayerDefensive defensive;
}
