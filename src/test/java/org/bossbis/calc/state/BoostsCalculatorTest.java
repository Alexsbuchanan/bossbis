package org.bossbis.calc.state;

import java.util.Set;
import org.bossbis.calc.types.PlayerSkills;
import org.bossbis.calc.types.Potion;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BoostsCalculatorTest
{
	private static final PlayerSkills L99 = new PlayerSkills(99, 99, 99, 99, 99, 99, 99, 99, 99);

	@Test
	void superCombatOn99s()
	{
		// Super combat: atk/str/def = floor(5 + level*0.15) = floor(5 + 14.85) = 19.
		PlayerSkills b = new BoostsCalculator().computeBoosts(L99, Set.of(Potion.SUPER_COMBAT));
		assertThat(b.getAtk()).isEqualTo(19);
		assertThat(b.getStr()).isEqualTo(19);
		assertThat(b.getDef()).isEqualTo(19);
		assertThat(b.getRanged()).isEqualTo(0);   // super combat doesn't boost ranged
		assertThat(b.getMagic()).isEqualTo(0);
	}

	@Test
	void maxCombinesAcrossPotions()
	{
		// Overload(+) atk = floor(6 + 99*0.16) = 21 > super combat 19 -> max wins per skill.
		PlayerSkills b = new BoostsCalculator()
			.computeBoosts(L99, Set.of(Potion.SUPER_COMBAT, Potion.OVERLOAD_PLUS));
		assertThat(b.getAtk()).isEqualTo(21);
		assertThat(b.getRanged()).isEqualTo(21);   // overload boosts ranged too
	}

	@Test
	void noPotionsYieldsZeroDelta()
	{
		PlayerSkills b = new BoostsCalculator().computeBoosts(L99, Set.of());
		assertThat(b.getStr()).isEqualTo(0);
	}
}
