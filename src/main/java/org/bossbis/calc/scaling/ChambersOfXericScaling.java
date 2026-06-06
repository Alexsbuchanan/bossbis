package org.bossbis.calc.scaling;

import java.util.ArrayList;
import java.util.List;
import org.bossbis.calc.CalcMath;
import org.bossbis.calc.Constants;
import org.bossbis.calc.types.Monster;
import org.bossbis.calc.types.MonsterAttribute;
import org.bossbis.calc.types.MonsterInputs;
import org.bossbis.calc.types.MonsterSkills;

/**
 * Port of {@code src/lib/scaling/ChambersOfXeric.ts} (weirdgloop@5e313c1) — Chambers of Xeric party /
 * combat-level / HP stat-scaling ({@code applyCoxScaling}).
 *
 * <p>All arithmetic integer-truncates ({@code Math.trunc} -> Java integer division / {@code (int)}
 * cast). The XERICIAN attribute gates the whole transform; non-Xerician monsters pass through
 * unchanged. Singles vs multi vs Olm scaling is selected by the per-monster id sets in
 * {@link Constants}.
 */
public final class ChambersOfXericScaling
{
	private ChambersOfXericScaling() {}

	private static final int CM_SCALE_PERCENT = 50;

	/** Port of the {@code SkillMeta} interface (ChambersOfXeric.ts:19-27). */
	private static final class SkillMeta
	{
		final List<String> offensives;
		final int baseOffensive;
		final List<String> defensives;
		final int baseDefensive;
		final int baseHp;

		SkillMeta(List<String> offensives, int baseOffensive, List<String> defensives, int baseDefensive, int baseHp)
		{
			this.offensives = offensives;
			this.baseOffensive = baseOffensive;
			this.defensives = defensives;
			this.baseDefensive = baseDefensive;
			this.baseHp = baseHp;
		}
	}

	private static int skill(MonsterSkills s, String k)
	{
		switch (k)
		{
			case "atk": return s.getAtk();
			case "str": return s.getStr();
			case "ranged": return s.getRanged();
			case "magic": return s.getMagic();
			case "def": return s.getDef();
			case "hp": return s.getHp();
			default: throw new IllegalArgumentException("unknown skill " + k);
		}
	}

	/** Port of {@code getSkillMeta} (ChambersOfXeric.ts:32-64). */
	private static SkillMeta getSkillMeta(Monster m)
	{
		boolean magicIsDefensive = Constants.COX_MAGIC_IS_DEFENSIVE_IDS.contains(m.getId());

		List<String> offensivesAll = new ArrayList<>();
		offensivesAll.add("atk");
		offensivesAll.add("str");
		offensivesAll.add("ranged");
		List<String> defensivesAll = new ArrayList<>();
		defensivesAll.add("def");
		if (magicIsDefensive)
		{
			defensivesAll.add("magic");
		}
		else
		{
			offensivesAll.add("magic");
		}

		MonsterSkills s = m.getSkills();
		// don't scale skills set at 1, they should remain at 1
		List<String> offensives = new ArrayList<>();
		for (String k : offensivesAll)
		{
			if (skill(s, k) != 1)
			{
				offensives.add(k);
			}
		}
		List<String> defensives = new ArrayList<>();
		for (String k : defensivesAll)
		{
			if (skill(s, k) != 1)
			{
				defensives.add(k);
			}
		}

		// determine the "base" stat of remaining skills (they should all be the same since they're linked)
		int baseOffensive = maxSkill(s, offensives);
		int baseDefensive = maxSkill(s, defensives);

		int baseHp = Constants.GUARDIAN_IDS.contains(m.getId())
			? 151 + (int) ((long) m.getInputs().getPartySumMiningLevel() / m.getInputs().getPartySize())
			: s.getHp();

		return new SkillMeta(offensives, baseOffensive, defensives, baseDefensive, baseHp);
	}

	/** Port of {@code max(skills, (k) => m.skills[k]) ?? 1} (d3 max returns undefined for empty -> 1). */
	private static int maxSkill(MonsterSkills s, List<String> keys)
	{
		if (keys.isEmpty())
		{
			return 1;
		}
		int best = Integer.MIN_VALUE;
		for (String k : keys)
		{
			best = Math.max(best, skill(s, k));
		}
		return best;
	}

	/** Port of {@code applySinglesCoxScaling} (ChambersOfXeric.ts:68-101). */
	private static Monster applySinglesCoxScaling(Monster m)
	{
		MonsterInputs inputs = m.getInputs();
		SkillMeta meta = getSkillMeta(m);

		// scaling factors based on clamped inputs
		int hpScaler = Math.max(Math.min(inputs.getPartyMaxCombatLevel(), 126), 60);
		int statScaler = Math.max(Math.min(inputs.getPartyMaxHpLevel(), 99), 55);

		// increase everything for cm
		if (inputs.isFromCoxCm())
		{
			statScaler = CalcMath.addPercent(statScaler, CM_SCALE_PERCENT);
			hpScaler = CalcMath.addPercent(hpScaler, CM_SCALE_PERCENT);
		}

		// determine the new values for each applicable stat
		StatChanges changes = new StatChanges();
		changes.hp = Math.max((int) ((long) meta.baseHp * hpScaler / 126), 5);
		for (String o : meta.offensives)
		{
			changes.set(o, Math.max((int) ((long) meta.baseOffensive * statScaler / 99), 1));
		}
		for (String d : meta.defensives)
		{
			changes.set(d, Math.max((int) ((long) meta.baseDefensive * statScaler / 99), 1));
		}

		return withSkills(m, changes.apply(m.getSkills()));
	}

	/** Port of {@code applyMultiCoxScaling} (ChambersOfXeric.ts:104-168). */
	private static Monster applyMultiCoxScaling(Monster m)
	{
		MonsterInputs inputs = m.getInputs();
		int id = m.getId();
		SkillMeta meta = getSkillMeta(m);

		// clamp a bunch of input values
		int partySize = Math.min(Math.max(inputs.getPartySize(), 1), 100);
		int partySizeM1 = partySize - 1;
		int highestComLevel = Math.max(Math.min(inputs.getPartyMaxCombatLevel(), 126), 60);
		int highestHp = Math.max(Math.min(55 + (int) (44L * inputs.getPartyMaxHpLevel() / 99), 99), 55);

		// scale base stats by party member stats
		int offensive = (int) ((long) meta.baseOffensive * highestHp / 99);
		int defensive = (int) ((long) meta.baseDefensive * highestHp / 99);
		int hp = (int) ((long) meta.baseHp * highestComLevel / 126);

		// scale everything based on party size in varying ways
		int offensiveScalePct = 100 + CalcMath.iSqrt(partySizeM1) * 7 + partySizeM1;
		offensive = (int) ((long) offensive * offensiveScalePct / 100);

		int defensiveScalePct = 100 + CalcMath.iSqrt(partySizeM1) + (int) ((long) partySizeM1 * 7 / 10);
		defensive = (int) ((long) defensive * defensiveScalePct / 100);

		hp += hp * (int) ((long) partySize * 50 / 100);

		// increase all stats for cm (with some exceptions)
		if (inputs.isFromCoxCm())
		{
			offensive = CalcMath.addPercent(offensive, CM_SCALE_PERCENT);

			if (!Constants.GLOWING_CRYSTAL_IDS_SET.contains(id))
			{
				hp = CalcMath.addPercent(hp, CM_SCALE_PERCENT);
			}

			if (Constants.GLOWING_CRYSTAL_IDS_SET.contains(id))
			{
				// not scaled
			}
			else if (Constants.TEKTON_IDS_SET.contains(id)) // tekton gets special treatment to make specs easier
			{
				if (partySize < 4) // especially for small party sizes
				{
					defensive = CalcMath.addPercent(defensive, 20);
				}
				else
				{
					defensive = CalcMath.addPercent(defensive, 35);
				}
			}
			else
			{
				defensive = CalcMath.addPercent(defensive, CM_SCALE_PERCENT);
			}
		}

		// make the changeset (and clamp for sanity)
		StatChanges changes = new StatChanges();
		changes.hp = Math.max(Math.min(hp, 30_000), 50);
		for (String o : meta.offensives)
		{
			changes.set(o, Math.max(Math.min(offensive, 5_000), 50));
		}
		for (String d : meta.defensives)
		{
			changes.set(d, Math.max(Math.min(defensive, 20_000), 50));
		}

		return withSkills(m, changes.apply(m.getSkills()));
	}

	/** Port of {@code applyOlmScaling} (ChambersOfXeric.ts:170-191). */
	public static Monster applyOlmScaling(Monster m)
	{
		boolean lhand = Constants.OLM_MELEE_HAND_IDS.contains(m.getId());
		boolean rhand = Constants.OLM_MAGE_HAND_IDS.contains(m.getId());

		// partySize - 3 * extraPhases, basically
		int partySizeScaleFactor = Math.min(m.getInputs().getPartySize() - 1, 50)
			- 3 * (int) ((long) Math.min(m.getInputs().getPartySize(), 50) / 8);

		Monster base = applyMultiCoxScaling(m);
		int magic = rhand ? (int) ((long) base.getSkills().getMagic() / 2) : base.getSkills().getMagic();
		int hp = (lhand || rhand)
			? 600 + 300 * partySizeScaleFactor
			: 800 + 400 * partySizeScaleFactor;

		MonsterSkills bs = base.getSkills();
		MonsterSkills ns = new MonsterSkills(bs.getAtk(), bs.getDef(), hp, magic, bs.getRanged(), bs.getStr());
		return withSkills(base, ns);
	}

	/** Port of {@code applyCoxScaling} (ChambersOfXeric.ts:193-207) — the default export. */
	public static Monster apply(Monster m)
	{
		java.util.Set<MonsterAttribute> attrs = m.getAttributes();
		if (attrs == null || !attrs.contains(MonsterAttribute.XERICIAN))
		{
			return m;
		}

		if (Constants.COX_USE_SINGLES_SCALING_IDS.contains(m.getId()))
		{
			return applySinglesCoxScaling(m);
		}

		if (Constants.OLM_IDS.contains(m.getId()))
		{
			return applyOlmScaling(m);
		}

		return applyMultiCoxScaling(m);
	}

	/** Mirrors the upstream {@code Partial<MonsterSkills>} changeset (only set keys are written). */
	private static final class StatChanges
	{
		Integer atk;
		Integer str;
		Integer ranged;
		Integer magic;
		Integer def;
		Integer hp;

		void set(String k, int v)
		{
			switch (k)
			{
				case "atk": atk = v; break;
				case "str": str = v; break;
				case "ranged": ranged = v; break;
				case "magic": magic = v; break;
				case "def": def = v; break;
				case "hp": hp = v; break;
				default: throw new IllegalArgumentException("unknown skill " + k);
			}
		}

		MonsterSkills apply(MonsterSkills s)
		{
			return new MonsterSkills(
				atk == null ? s.getAtk() : atk,
				def == null ? s.getDef() : def,
				hp == null ? s.getHp() : hp,
				magic == null ? s.getMagic() : magic,
				ranged == null ? s.getRanged() : ranged,
				str == null ? s.getStr() : str);
		}
	}

	private static Monster withSkills(Monster m, MonsterSkills skills)
	{
		return new Monster(
			m.getId(), m.getName(), m.getImage(), m.getVersion(), m.getSize(), m.getSpeed(),
			m.getStyle(), m.getMaxHit(), skills, m.getOffensive(), m.getDefensive(),
			m.getAttributes(), m.getWeakness(), m.getImmunities(), m.isSlayerMonster(), m.getInputs());
	}
}
