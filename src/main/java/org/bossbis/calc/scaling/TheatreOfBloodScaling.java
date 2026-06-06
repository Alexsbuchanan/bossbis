package org.bossbis.calc.scaling;

import org.bossbis.calc.Constants;
import org.bossbis.calc.types.Monster;
import org.bossbis.calc.types.MonsterInputs;
import org.bossbis.calc.types.MonsterSkills;

/**
 * Port of {@code src/lib/scaling/TheatreOfBlood.ts} (weirdgloop@5e313c1) — Theatre of Blood HP-only
 * party scaling ({@code applyTobScaling}). ToB scales hp and nothing else; normal/hard mode use the
 * {@code (partySize+3)/8} formula, entry mode uses the per-party-size factor table. All arithmetic
 * integer-truncates.
 */
public final class TheatreOfBloodScaling
{
	private TheatreOfBloodScaling() {}

	/** Port of {@code ENTRY_MODE_SCALING} (TheatreOfBlood.ts:5-11): partySize -> [num, den]. */
	private static int[] entryModeFactor(int partySize)
	{
		switch (partySize)
		{
			case 1: return new int[]{10, 40};
			case 2: return new int[]{19, 40};
			case 3: return new int[]{27, 40};
			case 4: return new int[]{34, 40};
			case 5: return new int[]{40, 40};
			default: throw new IllegalArgumentException("partySize out of range: " + partySize);
		}
	}

	/** Port of {@code applyTobScaling} (TheatreOfBlood.ts:13-41). */
	public static Monster apply(Monster m)
	{
		MonsterInputs inputs = m.getInputs();

		// tob only scales hp and nothing else
		if (Constants.TOB_MONSTER_IDS.contains(m.getId()))
		{
			int partySize = Math.min(5, Math.max(3, inputs.getPartySize()));
			return withHp(m, (int) ((long) m.getSkills().getHp() * (partySize + 3) / 8));
		}

		if (Constants.TOB_EM_MONSTER_IDS.contains(m.getId()))
		{
			int partySize = Math.min(5, Math.max(1, inputs.getPartySize()));
			int[] factor = entryModeFactor(partySize);
			return withHp(m, (int) ((long) m.getSkills().getHp() * factor[0] / factor[1]));
		}

		return m;
	}

	private static Monster withHp(Monster m, int hp)
	{
		MonsterSkills s = m.getSkills();
		MonsterSkills ns = new MonsterSkills(s.getAtk(), s.getDef(), hp, s.getMagic(), s.getRanged(), s.getStr());
		return new Monster(
			m.getId(), m.getName(), m.getImage(), m.getVersion(), m.getSize(), m.getSpeed(),
			m.getStyle(), m.getMaxHit(), ns, m.getOffensive(), m.getDefensive(),
			m.getAttributes(), m.getWeakness(), m.getImmunities(), m.isSlayerMonster(), m.getInputs());
	}
}
