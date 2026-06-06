package org.bossbis.calc.scaling;

import org.bossbis.calc.Constants;
import org.bossbis.calc.types.Monster;
import org.bossbis.calc.types.MonsterInputs;
import org.bossbis.calc.types.MonsterSkills;

/**
 * Port of {@code src/lib/scaling/TombsOfAmascut.ts} (weirdgloop@5e313c1) — Tombs of Amascut scaling
 * ({@code applyToaScaling}). ToA multiplies <em>rolled</em> values, not stats — except for HP, which
 * is scaled here by invocation level, path level, and party size (with rounding). The ToA invocation
 * <em>defence</em> factor is applied inline in {@code PlayerVsNpcCalc.getNPCDefenceRoll}, not here.
 * All arithmetic integer-truncates.
 */
public final class TombsOfAmascutScaling
{
	private TombsOfAmascutScaling() {}

	/** Port of {@code applyToaScaling} (TombsOfAmascut.ts:4-51). */
	public static Monster apply(Monster m)
	{
		MonsterInputs inputs = m.getInputs();

		// toa multiplies rolled values, not stats, except for hp
		if (!Constants.TOMBS_OF_AMASCUT_MONSTER_IDS.contains(m.getId()))
		{
			return m;
		}

		boolean coreEjected = Constants.TOA_WARDEN_CORE_EJECTED_IDS_SET.contains(m.getId());

		int newHp = m.getSkills().getHp();
		if (coreEjected)
		{
			newHp = 4500;
		}

		int invoFactor = (int) ((long) (coreEjected ? 1 : 4) * inputs.getToaInvocationLevel() / 10);
		newHp += (int) ((long) newHp * invoFactor / 100);

		int pathLevel = Math.min(6, Math.max(0, inputs.getToaPathLevel()));
		if (Constants.TOMBS_OF_AMASCUT_PATH_MONSTER_IDS.contains(m.getId()) && pathLevel >= 1)
		{
			int pathLevelFactor = 3 + 5 * inputs.getToaPathLevel();
			newHp = (int) ((long) newHp * (100 + pathLevelFactor) / 100);
		}

		int partySize = Math.min(8, Math.max(1, inputs.getPartySize()));
		if (partySize >= 2)
		{
			int partyFactor = 9 * (partySize >= 3 ? 2 : 1);
			if (partySize >= 4)
			{
				partyFactor += 6 * (partySize - 3);
			}

			newHp = (int) ((long) newHp * (10 + partyFactor) / 10);
		}

		// some rounding, for once
		if (newHp > 100)
		{
			int roundTo = newHp > 300 ? 10 : 5;
			newHp = (int) ((long) (newHp + (int) ((long) roundTo / 2)) / roundTo) * roundTo;
		}

		MonsterSkills s = m.getSkills();
		MonsterSkills ns = new MonsterSkills(s.getAtk(), s.getDef(), newHp, s.getMagic(), s.getRanged(), s.getStr());
		return new Monster(
			m.getId(), m.getName(), m.getImage(), m.getVersion(), m.getSize(), m.getSpeed(),
			m.getStyle(), m.getMaxHit(), ns, m.getOffensive(), m.getDefensive(),
			m.getAttributes(), m.getWeakness(), m.getImmunities(), m.isSlayerMonster(), m.getInputs());
	}
}
