package org.bossbis.calc.types;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class EnumsTest
{
	@Test
	void counts()
	{
		assertThat(Potion.values().length).isEqualTo(22);
		assertThat(MonsterAttribute.values().length).isEqualTo(16);
		assertThat(UserIssueType.values().length).isEqualTo(12);
	}

	@Test
	void stringValuedLookups()
	{
		assertThat(EquipmentCategory.fromValue("Bow")).isEqualTo(EquipmentCategory.BOW);
		assertThat(MonsterAttribute.fromValue("vampyre2")).isEqualTo(MonsterAttribute.VAMPYRE2);
		assertThat(BurnImmunity.fromValue("Strong")).isEqualTo(BurnImmunity.STRONG);
	}
}
