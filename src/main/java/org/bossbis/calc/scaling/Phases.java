package org.bossbis.calc.scaling;

import org.bossbis.calc.Constants;
import org.bossbis.calc.types.Monster;
import org.bossbis.calc.types.MonsterDefensive;
import org.bossbis.calc.types.MonsterSkills;

/**
 * Port of {@code src/lib/scaling/Phases.ts} (weirdgloop@5e313c1) — per-encounter phase stat overrides
 * ({@code applyMonsterPhases}). Currently: Araxxor "Enraged" raises def/magic/ranged; Yama (non-Enraged
 * version) sets magic defence to 60 (tank using magic) or -30. Gated on {@code inputs.phase}.
 */
public final class Phases
{
	private Phases() {}

	/** Port of {@code applyMonsterPhases} (Phases.ts:4-34). */
	public static Monster apply(Monster m)
	{
		String phase = m.getInputs() == null ? null : m.getInputs().getPhase();
		if (phase == null || phase.isEmpty())
		{
			return m;
		}

		if (Constants.ARAXXOR_IDS.contains(m.getId()) && "Enraged".equals(phase))
		{
			MonsterSkills s = m.getSkills();
			MonsterSkills ns = new MonsterSkills(
				s.getAtk(), s.getDef() + 35, s.getHp(), s.getMagic() + 28, s.getRanged() + 31, s.getStr());
			return withSkills(m, ns);
		}

		// yama goes to 60 magic defence if the tank is using magic
		if (Constants.YAMA_IDS.contains(m.getId()) && !"Enraged".equals(m.getVersion()))
		{
			int mdef = "Tank using magic".equals(phase) ? 60 : -30;
			return withMagicDefence(m, mdef);
		}

		return m;
	}

	private static Monster withSkills(Monster m, MonsterSkills skills)
	{
		return new Monster(
			m.getId(), m.getName(), m.getImage(), m.getVersion(), m.getSize(), m.getSpeed(),
			m.getStyle(), m.getMaxHit(), skills, m.getOffensive(), m.getDefensive(),
			m.getAttributes(), m.getWeakness(), m.getImmunities(), m.isSlayerMonster(), m.getInputs());
	}

	private static Monster withMagicDefence(Monster m, int magicDef)
	{
		MonsterDefensive d = m.getDefensive();
		MonsterDefensive nd = new MonsterDefensive(
			d.getFlatArmour(), d.getStab(), d.getSlash(), d.getCrush(), magicDef,
			d.getLight(), d.getStandard(), d.getHeavy());
		return new Monster(
			m.getId(), m.getName(), m.getImage(), m.getVersion(), m.getSize(), m.getSpeed(),
			m.getStyle(), m.getMaxHit(), m.getSkills(), m.getOffensive(), nd,
			m.getAttributes(), m.getWeakness(), m.getImmunities(), m.isSlayerMonster(), m.getInputs());
	}
}
