package org.bossbis.calc.types;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PrayerTest
{
	@Test
	void ordinalsAndCountMatchUpstream()
	{
		assertThat(Prayer.BURST_OF_STRENGTH.ordinal()).isEqualTo(0);
		assertThat(Prayer.values().length).isEqualTo(31);   // 0..30 inclusive
	}

	@Test
	void pietyCarriesItsMultipliers()
	{
		// Piety: +23% strength, +20% attack, +25% defence (denominator-100 factors).
		Prayer.PrayerData piety = Prayer.PIETY.data();
		assertThat(piety.factorStrength().numerator()).isEqualTo(123);
		assertThat(piety.factorStrength().divisor()).isEqualTo(100);
		assertThat(piety.combatStyle()).isEqualTo("melee");
	}
}
