package org.bossbis.calc.scaling;

import org.bossbis.calc.CalcMath;
import org.bossbis.calc.types.Monster;
import org.bossbis.calc.types.MonsterInputs;
import org.bossbis.calc.types.MonsterSkills;

/**
 * Port of {@code src/lib/scaling/Vardorvis.ts} (weirdgloop@5e313c1) — Vardorvis strength/defence
 * linearly scale throughout the fight based on the monster's current HP. The version selects the
 * start/end ranges; {@code lerp} maps current HP (in {@code [0, maxHp]}) onto each range.
 */
public final class VardorvisScaling
{
	private VardorvisScaling() {}

	/** Port of the {@code VardNumbers} interface (Vardorvis.ts:4-8). */
	private static final class VardNumbers
	{
		final int maxHp;
		final int strStart;
		final int strEnd;
		final int defStart;
		final int defEnd;

		VardNumbers(int maxHp, int strStart, int strEnd, int defStart, int defEnd)
		{
			this.maxHp = maxHp;
			this.strStart = strStart;
			this.strEnd = strEnd;
			this.defStart = defStart;
			this.defEnd = defEnd;
		}
	}

	/** Port of {@code getVardNumbers} (Vardorvis.ts:10-33). */
	private static VardNumbers getVardNumbers(Monster m)
	{
		String version = m.getVersion();
		if ("Quest".equals(version))
		{
			return new VardNumbers(500, 210, 280, 180, 130);
		}
		if ("Awakened".equals(version))
		{
			return new VardNumbers(1400, 391, 522, 268, 181);
		}
		return new VardNumbers(700, 270, 360, 215, 145);
	}

	/** Port of {@code applyVardScaling} (Vardorvis.ts:35-53). */
	public static Monster apply(Monster m)
	{
		MonsterInputs inputs = m.getInputs();

		if (!"Vardorvis".equals(m.getName()))
		{
			return m;
		}

		// vard's strength and defence scale linearly throughout the fight based on hp
		VardNumbers ranges = getVardNumbers(m);
		// Number.isFinite(inputs.monsterCurrentHp) — our int is always finite; upstream falls back to
		// maxHp only for a NaN/Infinity current HP, which cannot occur with an int field.
		int currHp = inputs.getMonsterCurrentHp();

		int newStr = CalcMath.lerp(currHp, ranges.maxHp, 0, ranges.strStart, ranges.strEnd);
		int newDef = CalcMath.lerp(currHp, ranges.maxHp, 0, ranges.defStart, ranges.defEnd);

		MonsterSkills s = m.getSkills();
		MonsterSkills ns = new MonsterSkills(s.getAtk(), newDef, s.getHp(), s.getMagic(), s.getRanged(), newStr);
		return new Monster(
			m.getId(), m.getName(), m.getImage(), m.getVersion(), m.getSize(), m.getSpeed(),
			m.getStyle(), m.getMaxHit(), ns, m.getOffensive(), m.getDefensive(),
			m.getAttributes(), m.getWeakness(), m.getImmunities(), m.isSlayerMonster(), m.getInputs());
	}
}
