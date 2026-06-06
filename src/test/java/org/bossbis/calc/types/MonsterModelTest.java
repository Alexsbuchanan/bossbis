package org.bossbis.calc.types;

import com.google.gson.Gson;
import java.util.List;
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
			+ "\"style\":[\"Crush\"],\"is_slayer_monster\":true,"
			+ "\"skills\":{\"atk\":0,\"def\":100,\"hp\":600,\"magic\":1,\"ranged\":1,\"str\":0},"
			+ "\"offensive\":{\"atk\":0,\"magic\":0,\"magic_str\":0,\"ranged\":0,\"ranged_str\":0,\"str\":0},"
			+ "\"defensive\":{\"flat_armour\":0,\"stab\":0,\"slash\":0,\"crush\":0,\"magic\":0,"
			+ "\"light\":0,\"standard\":0,\"heavy\":0},\"attributes\":[]}";
		Monster m = new Gson().fromJson(json, Monster.class);
		assertThat(m.getId()).isEqualTo(5862);
		assertThat(m.isSlayerMonster()).isTrue();           // @SerializedName("is_slayer_monster")
		assertThat(m.getDefensive().getFlatArmour()).isEqualTo(0);  // @SerializedName("flat_armour")
	}

	/**
	 * Deserializes a REAL-shaped monsters.json row: {@code style} is an array of capitalized
	 * strings, {@code attributes} are lowercase strings (string-valued enums), {@code max_hit}
	 * is a String. Proves the Gson mapping (List style, String maxHit, @SerializedName on
	 * MonsterAttribute) loads real data without nulls/parse failures.
	 */
	@Test
	void monsterDeserializesRealShapedRow()
	{
		String json = "{\"id\":7037,\"name\":\"Vorkath\",\"version\":\"\",\"size\":7,\"speed\":4,"
			+ "\"style\":[\"Crush\"],\"max_hit\":\"23\",\"is_slayer_monster\":true,"
			+ "\"skills\":{\"atk\":0,\"def\":214,\"hp\":750,\"magic\":150,\"ranged\":308,\"str\":0},"
			+ "\"offensive\":{\"atk\":0,\"magic\":0,\"magic_str\":0,\"ranged\":0,\"ranged_str\":0,\"str\":0},"
			+ "\"defensive\":{\"flat_armour\":0,\"stab\":26,\"slash\":108,\"crush\":108,\"magic\":240,"
			+ "\"light\":230,\"standard\":230,\"heavy\":230},"
			+ "\"attributes\":[\"undead\",\"spectral\"]}";
		Monster m = new Gson().fromJson(json, Monster.class);
		assertThat(m.getStyle()).isEqualTo(List.of("Crush"));   // array → List<String>
		assertThat(m.getMaxHit()).isEqualTo("23");              // @SerializedName("max_hit"), String
		// Non-null/non-empty proves @SerializedName makes Gson match the lowercase enum values.
		assertThat(m.getAttributes())
			.containsExactlyInAnyOrder(MonsterAttribute.UNDEAD, MonsterAttribute.SPECTRAL);
	}

	/**
	 * Real monsters.json contains edge rows where {@code style} is {@code null} (39 rows) and
	 * {@code max_hit} is a JSON number {@code 0} (37 rows). Both must deserialize without throwing:
	 * Gson sets a null List and coerces the numeric max_hit into its String form. Locks the choice
	 * of {@code List<String>}/{@code String} against a regression to a stricter type.
	 */
	@Test
	void monsterDeserializesNullStyleAndNumericMaxHit()
	{
		String json = "{\"id\":1,\"name\":\"Edge\",\"version\":\"\",\"size\":1,\"speed\":4,"
			+ "\"style\":null,\"max_hit\":0,\"is_slayer_monster\":false,"
			+ "\"skills\":{\"atk\":0,\"def\":0,\"hp\":1,\"magic\":1,\"ranged\":1,\"str\":0},"
			+ "\"offensive\":{\"atk\":0,\"magic\":0,\"magic_str\":0,\"ranged\":0,\"ranged_str\":0,\"str\":0},"
			+ "\"defensive\":{\"flat_armour\":0,\"stab\":0,\"slash\":0,\"crush\":0,\"magic\":0,"
			+ "\"light\":0,\"standard\":0,\"heavy\":0},\"attributes\":[]}";
		Monster m = new Gson().fromJson(json, Monster.class);
		assertThat(m.getStyle()).isNull();          // null array → null List, no crash
		assertThat(m.getMaxHit()).isEqualTo("0");   // numeric max_hit coerced to String
	}
}
