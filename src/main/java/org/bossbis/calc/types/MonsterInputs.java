package org.bossbis.calc.types;

import lombok.Value;

/**
 * Port of the {@code Monster.inputs} object (src/types/Monster.ts:63-132) — the per-encounter
 * UI-controlled state (ToA invocation, party scaling, defence reductions, monster prayers, ...).
 *
 * <p>{@link #initial()} returns {@code INITIAL_MONSTER_INPUTS} (src/lib/Monsters.ts:32-58). All keys
 * are camelCase, so no {@link com.google.gson.annotations.SerializedName} is needed.
 * {@code demonbaneVulnerability} and {@code phase} are nullable optionals.
 */
@Value
public class MonsterInputs
{
	boolean isFromCoxCm;
	int toaInvocationLevel;
	int toaPathLevel;
	int partyMaxCombatLevel;
	int partySumMiningLevel;
	int partyMaxHpLevel;
	int partySize;
	int monsterCurrentHp;
	DefenceReductions defenceReductions;
	Integer demonbaneVulnerability;
	String phase;
	MonsterPrayers prayers;

	/**
	 * Port of {@code INITIAL_MONSTER_INPUTS} (src/lib/Monsters.ts:32-58).
	 *
	 * <p>{@code monsterCurrentHp} defaults to the upstream literal 150; in practice it is reset to
	 * {@code monster.skills.hp} on monster select (replicated later in BaseCalc, spec §4.8).
	 */
	public static MonsterInputs initial()
	{
		return new MonsterInputs(
			false,           // isFromCoxCm
			0,               // toaInvocationLevel
			0,               // toaPathLevel
			126,             // partyMaxCombatLevel
			99,              // partySumMiningLevel
			99,              // partyMaxHpLevel
			1,               // partySize
			150,             // monsterCurrentHp
			new DefenceReductions(false, false, 0, 0, 0, 0, 0, 0, 0, 0),
			null,            // demonbaneVulnerability
			null,            // phase
			new MonsterPrayers(false, false, false));
	}
}
