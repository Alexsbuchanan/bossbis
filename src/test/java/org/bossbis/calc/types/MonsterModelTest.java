package org.bossbis.calc.types;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MonsterModelTest
{
	@Test
	void initialInputsMatchUpstream()
	{
		MonsterInputs i = MonsterInputs.initial();
		assertThat(i.getPartyMaxCombatLevel()).isEqualTo(126);
		assertThat(i.getPartySumMiningLevel()).isEqualTo(99);
		assertThat(i.getPartyMaxHpLevel()).isEqualTo(99);
		assertThat(i.getPartySize()).isEqualTo(1);
		assertThat(i.getToaInvocationLevel()).isEqualTo(0);
		assertThat(i.getDefenceReductions().getDwh()).isEqualTo(0);
		assertThat(i.getPrayers().isMagic()).isFalse();
	}

	@Test
	void monsterDeserializesSnakeCaseKeys()
	{
		String json = "{\"id\":5862,\"name\":\"Cerberus\",\"version\":\"\",\"size\":2,\"speed\":4,"
			+ "\"style\":\"crush\",\"is_slayer_monster\":true,"
			+ "\"skills\":{\"atk\":0,\"def\":100,\"hp\":600,\"magic\":1,\"ranged\":1,\"str\":0},"
			+ "\"offensive\":{\"atk\":0,\"magic\":0,\"magic_str\":0,\"ranged\":0,\"ranged_str\":0,\"str\":0},"
			+ "\"defensive\":{\"flat_armour\":0,\"stab\":0,\"slash\":0,\"crush\":0,\"magic\":0,"
			+ "\"light\":0,\"standard\":0,\"heavy\":0},\"attributes\":[]}";
		Monster m = new Gson().fromJson(json, Monster.class);
		assertThat(m.getId()).isEqualTo(5862);
		assertThat(m.isSlayerMonster()).isTrue();           // @SerializedName("is_slayer_monster")
		assertThat(m.getDefensive().getFlatArmour()).isEqualTo(0);  // @SerializedName("flat_armour")
	}
}
