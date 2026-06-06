package org.bossbis.calc.types;

import lombok.Value;

/**
 * Port of the {@code EquipmentPiece} interface (src/types/Player.ts:20-34).
 *
 * <p>Upstream {@code EquipmentPiece extends EquipmentStats}, so each item's JSON carries the three
 * nested {@code bonuses}/{@code offensive}/{@code defensive} objects directly. We INLINE them as
 * direct fields here (rather than routing through a composed {@link EquipmentStats}) so Gson maps
 * the nested objects straight onto the piece and {@code getBonuses()} exists on the piece.
 *
 * <p>JSON keys verified against a real equipment.json row: {@code isTwoHanded} is camelCase (no
 * annotation needed); {@code bonuses.ranged_str}/{@code bonuses.magic_str} are snake_case (handled
 * inside {@link PlayerBonuses}). {@code itemVars} does not appear in equipment.json (runtime-only
 * field) but is retained for round-trip completeness.
 */
@Value
public class EquipmentPiece
{
	String name;
	int id;
	double weight;
	String version;
	String slot;
	String image;
	int speed;
	EquipmentCategory category;
	boolean isTwoHanded;
	ItemVars itemVars;
	PlayerBonuses bonuses;
	PlayerOffensive offensive;
	PlayerDefensive defensive;
}
