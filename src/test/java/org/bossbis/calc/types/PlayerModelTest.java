package org.bossbis.calc.types;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PlayerModelTest
{
	@Test
	void playerSkillsIncludeMiningAndHerblore()
	{
		PlayerSkills s = new PlayerSkills(99, 99, 99, 99, 99, 99, 99, 99, 99);
		assertThat(s.getMining()).isEqualTo(99);
		assertThat(s.getHerblore()).isEqualTo(99);
	}

	@Test
	void boostsAreAdditiveAndMayBeNegative()
	{
		PlayerSkills boosts = new PlayerSkills(19, 19, 0, 0, 0, 0, 0, 0, -2);
		assertThat(boosts.getHerblore()).isEqualTo(-2);
	}

	@Test
	void equipmentPieceDeserializesSnakeCaseBonuses()
	{
		// The Gson FieldNamingPolicy stays default; @SerializedName must map ranged_str/magic_str.
		String json = "{\"name\":\"Twisted bow\",\"id\":20997,\"slot\":\"weapon\",\"speed\":6,"
			+ "\"isTwoHanded\":true,\"category\":\"Bow\","
			+ "\"bonuses\":{\"str\":0,\"ranged_str\":70,\"magic_str\":0,\"prayer\":0},"
			+ "\"offensive\":{\"stab\":0,\"slash\":0,\"crush\":0,\"magic\":0,\"ranged\":70},"
			+ "\"defensive\":{\"stab\":0,\"slash\":0,\"crush\":0,\"magic\":0,\"ranged\":0}}";
		EquipmentPiece p = new Gson().fromJson(json, EquipmentPiece.class);
		assertThat(p.getId()).isEqualTo(20997);
		assertThat(p.isTwoHanded()).isTrue();
		assertThat(p.getBonuses().getRangedStr()).isEqualTo(70);   // proves @SerializedName("ranged_str")
	}
}
