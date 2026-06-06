package org.bossbis.calc;

import com.google.gson.Gson;
import java.util.List;
import org.bossbis.calc.types.CombatStyle;
import org.bossbis.calc.types.Monster;
import org.bossbis.calc.types.Player;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link ScenarioPlayer} builds a calc-ready Player faithfully for corpus row
 * {@code 007-basic-melee-whip-l99-accuracy}: whip in the weapon slot, 99 attack, Flick (slash,
 * Accurate) style, and aggregated {@code offensive.slash} equal to the whip's offensive value (82).
 */
class ScenarioPlayerTest
{
	private static final ScenarioPlayer BUILDER = new ScenarioPlayer(new Gson());

	@Test
	void buildsWhipMeleeRow()
	{
		CorpusRow row = row("basic-melee-whip-l99-accuracy");
		Monster monster = BUILDER.monster(row);
		Player p = BUILDER.player(row, monster);

		assertThat(p.getEquipment().getWeapon()).isNotNull();
		assertThat(p.getEquipment().getWeapon().getName()).isEqualTo("Abyssal whip");
		assertThat(p.getSkills().getAtk()).isEqualTo(99);
		assertThat(p.getStyle().getType()).isEqualTo(CombatStyle.SLASH);
		assertThat(p.getStyle().getStance()).isEqualTo(CombatStyle.ACCURATE);
		assertThat(p.getOffensive().getSlash()).isEqualTo(82);
	}

	private static CorpusRow row(String name)
	{
		List<CorpusRow> rows = CorpusRow.loadAll();
		return rows.stream().filter(r -> r.name().equals(name)).findFirst()
			.orElseThrow(() -> new IllegalStateException("No corpus row named " + name));
	}
}
