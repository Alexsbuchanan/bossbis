package org.bossbis.calc.types;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CombatStyleSpellTest
{
	@Test
	void rangedDamageTypeByCategory()
	{
		assertThat(CombatStyle.getRangedDamageType(EquipmentCategory.BOW)).isEqualTo("standard");
		assertThat(CombatStyle.getRangedDamageType(EquipmentCategory.CROSSBOW)).isEqualTo("heavy");
	}

	@Test
	void spellHoldsFields()
	{
		Spell s = new Spell("Fire Bolt", "img", 12, Spell.Spellbook.STANDARD, Spell.Spellement.FIRE, false);
		assertThat(s.getMaxHit()).isEqualTo(12);
		assertThat(s.getSpellbook()).isEqualTo(Spell.Spellbook.STANDARD);
	}
}
