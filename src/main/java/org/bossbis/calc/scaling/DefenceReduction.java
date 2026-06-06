package org.bossbis.calc.scaling;

import java.util.Set;
import org.bossbis.calc.Constants;
import org.bossbis.calc.types.DefenceReductions;
import org.bossbis.calc.types.Monster;
import org.bossbis.calc.types.MonsterAttribute;
import org.bossbis.calc.types.MonsterDefensive;
import org.bossbis.calc.types.MonsterInputs;
import org.bossbis.calc.types.MonsterSkills;

/**
 * Port of {@code src/lib/scaling/DefenceReduction.ts} (weirdgloop@5e313c1) — applies the player's
 * stacked defence-reduction special attacks/spells ({@code monster.inputs.defenceReductions}) to the
 * monster's defensive stats in the EXACT upstream order. All arithmetic integer-truncates
 * ({@code Math.trunc} -> Java integer division); the reduction <b>order is load-bearing</b>.
 *
 * <p>Applied in upstream order:
 * <ol>
 *   <li>{@code accursed} (-15% def &amp; magic), else {@code vulnerability} (-10% def);</li>
 *   <li>{@code elderMaul} (each -35% def, multiplicative);</li>
 *   <li>{@code dwh} (each -30% def, multiplicative);</li>
 *   <li>{@code arclight} then {@code emberlight} (per-stack flat drain of atk/str/def computed from
 *       the <em>base</em> skills; demon-doubled factor);</li>
 *   <li>{@code tonalztic} (each -10% of magic from def);</li>
 *   <li>{@code seercull} (flat magic drain);</li>
 *   <li>{@code bgs} (flat drain cascading def -&gt; str -&gt; atk -&gt; magic -&gt; ranged);</li>
 *   <li>{@code ayak} (flat magic-defence bonus drain).</li>
 * </ol>
 *
 * <p>The per-skill {@code def} floor (a monster-specific minimum the def stat cannot drop below) is
 * ported via {@link #getDefenceFloor(Monster)}.
 */
public final class DefenceReduction
{
	private DefenceReduction() {}

	/** Numerator/denominator pair (port of {@code Factor} from src/lib/Math.ts). */
	private static final class Factor
	{
		final int num;
		final int den;

		Factor(int num, int den)
		{
			this.num = num;
			this.den = den;
		}
	}

	/** Port of {@code getDefenceFloor} (DefenceReduction.ts:22-65). */
	public static int getDefenceFloor(Monster m)
	{
		int id = m.getId();
		if (Constants.VERZIK_IDS.contains(id) || Constants.VARDORVIS_IDS.contains(id))
		{
			return m.getSkills().getDef();
		}
		if (Constants.SOTETSEG_IDS.contains(id))
		{
			return 100;
		}
		if (Constants.NIGHTMARE_IDS.contains(id))
		{
			return 120;
		}
		if (Constants.AKKHA_IDS_SET.contains(id))
		{
			return 70;
		}
		if (Constants.BABA_IDS_SET.contains(id))
		{
			return 60;
		}
		if (Constants.KEPHRI_UNSHIELDED_IDS_SET.contains(id) || Constants.KEPHRI_SHIELDED_IDS_SET.contains(id))
		{
			return 60;
		}
		if (Constants.ZEBAK_IDS_SET.contains(id))
		{
			return 50;
		}
		if (Constants.P3_WARDEN_IDS_SET.contains(id))
		{
			return 120;
		}
		if (Constants.TOA_OBELISK_IDS_SET.contains(id))
		{
			return 60;
		}
		if (Constants.NEX_IDS.contains(id))
		{
			return 250;
		}
		if (Constants.ARAXXOR_IDS.contains(id))
		{
			return 90;
		}
		if (Constants.HUEYCOATL_IDS.contains(id))
		{
			return 120;
		}
		if (Constants.YAMA_IDS.contains(id))
		{
			return 145;
		}

		// no limit
		return 0;
	}

	/** Port of {@code applyDefenceReductions} (DefenceReduction.ts:67-170). */
	public static Monster apply(Monster m)
	{
		MonsterSkills baseSkills = m.getSkills();
		int defenceFloor = getDefenceFloor(m);
		// Upstream always carries inputs.defenceReductions; in this port the bundled monster rows are
		// assembled with inputs separately, so guard against an unassembled (null) inputs block by
		// treating it as "no reductions" (identity).
		MonsterInputs inputs = m.getInputs();
		if (inputs == null || inputs.getDefenceReductions() == null)
		{
			return m;
		}
		DefenceReductions reductions = inputs.getDefenceReductions();

		if (reductions.isAccursed())
		{
			m = newSkills(m, defenceFloor,
				m.getSkills().getDef() * 17 / 20,
				null, null,
				m.getSkills().getMagic() * 17 / 20,
				null, null);
		}
		else if (reductions.isVulnerability())
		{
			// todo tome of water increases this to 15% reduction, but how do we handle that?
			m = newSkills(m, defenceFloor,
				m.getSkills().getDef() * 9 / 10,
				null, null, null, null, null);
		}

		for (int i = 0; i < reductions.getElderMaul(); i++)
		{
			m = newSkills(m, defenceFloor,
				m.getSkills().getDef() - (m.getSkills().getDef() * 35 / 100),
				null, null, null, null, null);
		}
		for (int i = 0; i < reductions.getDwh(); i++)
		{
			m = newSkills(m, defenceFloor,
				m.getSkills().getDef() - (m.getSkills().getDef() * 3 / 10),
				null, null, null, null, null);
		}

		Set<MonsterAttribute> attrs = m.getAttributes();
		boolean isDemon = attrs != null && attrs.contains(MonsterAttribute.DEMON);
		m = reduceArclight(m, defenceFloor, baseSkills, reductions.getArclight(), isDemon ? new Factor(2, 20) : new Factor(1, 20));
		m = reduceArclight(m, defenceFloor, baseSkills, reductions.getEmberlight(), isDemon ? new Factor(3, 20) : new Factor(1, 20));

		for (int i = 0; i < reductions.getTonalztic(); i++)
		{
			m = newSkills(m, defenceFloor,
				m.getSkills().getDef() - (m.getSkills().getMagic() / 10),
				null, null, null, null, null);
		}

		if (reductions.getSeercull() > 0)
		{
			m = newSkills(m, defenceFloor,
				null, null, null,
				m.getSkills().getMagic() - reductions.getSeercull(),
				null, null);
		}

		int bgsDmg = reductions.getBgs();
		if (bgsDmg > 0)
		{
			// order matters here: def -> str -> atk -> magic -> ranged
			BgsState st = new BgsState(m, bgsDmg, defenceFloor);
			st.apply(Skill.DEF);
			st.apply(Skill.STR);
			st.apply(Skill.ATK);
			st.apply(Skill.MAGIC);
			st.apply(Skill.RANGED);
			m = st.monster;
		}

		if (reductions.getAyak() > 0 && m.getDefensive().getMagic() > 0)
		{
			int newMagicDef = Math.max(0, m.getDefensive().getMagic() - reductions.getAyak());
			m = withMagicDefence(m, newMagicDef);
		}

		return m;
	}

	/** Port of the {@code reduceArclight} closure (DefenceReduction.ts:110-122). */
	private static Monster reduceArclight(Monster monster, int defenceFloor, MonsterSkills baseSkills, int iter, Factor f)
	{
		if (iter == 0)
		{
			return monster;
		}
		int newAtk = monster.getSkills().getAtk() - (iter * ((f.num * baseSkills.getAtk() / f.den) + 1));
		int newStr = monster.getSkills().getStr() - (iter * ((f.num * baseSkills.getStr() / f.den) + 1));
		int newDef = monster.getSkills().getDef() - (iter * ((f.num * baseSkills.getDef() / f.den) + 1));
		return newSkills(monster, defenceFloor, newDef, newAtk, newStr, null, null, null);
	}

	/**
	 * Port of the {@code newSkills} closure (DefenceReduction.ts:72-84): writes the supplied skill
	 * values, flooring {@code def} at {@code defenceFloor} and every other skill at 0 (only the keys
	 * supplied are floored/written, matching upstream's {@code keys(skills).forEach}). A {@code null}
	 * argument means "do not touch this skill".
	 */
	private static Monster newSkills(Monster m, int defenceFloor,
		Integer def, Integer atk, Integer str, Integer magic, Integer ranged, Integer hp)
	{
		MonsterSkills s = m.getSkills();
		int newDef = def == null ? s.getDef() : Math.max(defenceFloor, def);
		int newAtk = atk == null ? s.getAtk() : Math.max(0, atk);
		int newStr = str == null ? s.getStr() : Math.max(0, str);
		int newMagic = magic == null ? s.getMagic() : Math.max(0, magic);
		int newRanged = ranged == null ? s.getRanged() : Math.max(0, ranged);
		int newHp = hp == null ? s.getHp() : Math.max(0, hp);
		return withSkills(m, new MonsterSkills(newAtk, newDef, newHp, newMagic, newRanged, newStr));
	}

	private enum Skill
	{
		DEF, STR, ATK, MAGIC, RANGED
	}

	/** Mutable carrier for the cascading BGS drain (DefenceReduction.ts:136-156). */
	private static final class BgsState
	{
		private Monster monster;
		private int bgsDmg;
		private final int defenceFloor;

		BgsState(Monster monster, int bgsDmg, int defenceFloor)
		{
			this.monster = monster;
			this.bgsDmg = bgsDmg;
			this.defenceFloor = defenceFloor;
		}

		void apply(Skill k)
		{
			int startLevel = read(k);
			Monster next = write(k, startLevel - bgsDmg);
			int after = readFrom(next, k);
			if (after > 0)
			{
				// if a skill fails to drain to 0, even if because of a drain floor, the bgs stops
				bgsDmg = 0;
			}
			else
			{
				bgsDmg -= startLevel;
			}
			monster = next;
		}

		private int read(Skill k)
		{
			return readFrom(monster, k);
		}

		private static int readFrom(Monster m, Skill k)
		{
			MonsterSkills s = m.getSkills();
			switch (k)
			{
				case DEF: return s.getDef();
				case STR: return s.getStr();
				case ATK: return s.getAtk();
				case MAGIC: return s.getMagic();
				case RANGED: return s.getRanged();
				default: throw new IllegalArgumentException("unknown skill " + k);
			}
		}

		private Monster write(Skill k, int value)
		{
			switch (k)
			{
				case DEF: return newSkills(monster, defenceFloor, value, null, null, null, null, null);
				case STR: return newSkills(monster, defenceFloor, null, null, value, null, null, null);
				case ATK: return newSkills(monster, defenceFloor, null, value, null, null, null, null);
				case MAGIC: return newSkills(monster, defenceFloor, null, null, null, value, null, null);
				case RANGED: return newSkills(monster, defenceFloor, null, null, null, null, value, null);
				default: throw new IllegalArgumentException("unknown skill " + k);
			}
		}
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
