package org.bossbis.calc.types;

import lombok.Value;

/**
 * Port of the optional {@code itemVars} object on {@code EquipmentPiece} (src/types/Player.ts:30-33).
 *
 * <p>Both keys ({@code blowpipeDartName}/{@code blowpipeDartId}) are already camelCase in the data,
 * so no {@link com.google.gson.annotations.SerializedName} is needed. Fields are nullable
 * (boxed types) since the object is optional and sparsely populated.
 */
@Value
public class ItemVars
{
	String blowpipeDartName;
	Integer blowpipeDartId;
}
