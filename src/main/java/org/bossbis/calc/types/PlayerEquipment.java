package org.bossbis.calc.types;

import lombok.Value;

/**
 * Port of the {@code PlayerEquipment} interface (src/types/Player.ts:40-52).
 *
 * <p>The eleven fixed gear slots; each is a nullable {@link EquipmentPiece}. Slot names are already
 * camelCase, so no {@link com.google.gson.annotations.SerializedName} is needed.
 */
@Value
public class PlayerEquipment
{
	EquipmentPiece head;
	EquipmentPiece cape;
	EquipmentPiece neck;
	EquipmentPiece ammo;
	EquipmentPiece weapon;
	EquipmentPiece body;
	EquipmentPiece shield;
	EquipmentPiece legs;
	EquipmentPiece hands;
	EquipmentPiece feet;
	EquipmentPiece ring;
}
