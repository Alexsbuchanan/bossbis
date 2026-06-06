package org.bossbis.calc.types;

import java.util.List;
import lombok.Value;
import org.bossbis.calc.types.CombatStyle.PlayerCombatStyle;

/**
 * Port of the {@code Player} interface (src/types/Player.ts:105-174).
 *
 * <p>Upstream {@code Player extends EquipmentStats}, so it carries BOTH the aggregated stat triple
 * (the inlined {@code bonuses}/{@code offensive}/{@code defensive}, summed across worn gear) AND
 * the raw {@code equipment} map. {@code boosts} is a {@link PlayerSkills} of additive deltas (may be
 * negative). The UI-only {@code buffs.potions} and {@code leagues} state are intentionally omitted
 * from this calc model (spec §4.2). All keys are camelCase, so no
 * {@link com.google.gson.annotations.SerializedName} is needed.
 */
@Value
public class Player
{
	String name;
	PlayerCombatStyle style;
	PlayerSkills skills;
	PlayerSkills boosts;
	PlayerEquipment equipment;
	int attackSpeed;
	List<Prayer> prayers;
	Buffs buffs;
	Spell spell;
	// Aggregated EquipmentStats (Player extends EquipmentStats upstream).
	PlayerBonuses bonuses;
	PlayerOffensive offensive;
	PlayerDefensive defensive;
}
