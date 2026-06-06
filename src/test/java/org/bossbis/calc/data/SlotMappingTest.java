package org.bossbis.calc.data;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SlotMappingTest
{
	@Test
	void mapsWeirdgloopSlotStrings()
	{
		assertThat(SlotMapping.fromWeirdgloop("neck")).isEqualTo(SlotMapping.Slot.NECK);
		assertThat(SlotMapping.fromWeirdgloop("ammo")).isEqualTo(SlotMapping.Slot.AMMO);
		assertThat(SlotMapping.fromWeirdgloop("weapon")).isEqualTo(SlotMapping.Slot.WEAPON);
	}

	@Test
	void twoHandedExcludesShield()
	{
		assertThat(SlotMapping.occupies(SlotMapping.Slot.WEAPON, true))
			.contains(SlotMapping.Slot.WEAPON, SlotMapping.Slot.SHIELD);
	}
}
