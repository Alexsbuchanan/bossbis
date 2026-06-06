package org.bossbis.calc;

import java.util.ArrayList;
import java.util.List;
import org.bossbis.calc.HitDist.AttackDistribution;
import org.bossbis.calc.HitDist.HitDistribution;
import org.bossbis.calc.HitDist.Hitsplat;
import org.bossbis.calc.HitDist.WeightedHit;
import org.bossbis.calc.data.EquipmentRepository;
import org.bossbis.calc.data.SpellRepository;
import org.bossbis.calc.types.CombatStyle;
import org.bossbis.calc.types.Monster;
import org.bossbis.calc.types.MonsterOffensive;
import org.bossbis.calc.types.MonsterSkills;
import org.bossbis.calc.types.Player;
import org.bossbis.calc.types.Prayer;
import org.bossbis.calc.types.Prayer.PrayerData;

/**
 * Port of {@code src/lib/NPCVsPlayerCalc.ts} (weirdgloop@5e313c1) — computes NPC-vs-player metrics
 * (how much damage the NPC does to the player / damage taken). Extends {@link BaseCalc}.
 *
 * <h2>Style normalization</h2>
 * Upstream {@code monster.style} is a single lowercase {@code CombatStyleType} (one of
 * {@code stab|slash|crush|magic|ranged}, or {@code null}), produced in {@code getMonsters()} as
 * {@code m.style.join(',').toLowerCase()} gated by {@code isCombatStyleType}. The Java
 * {@link Monster#getStyle()} keeps the RAW capitalized array (e.g. {@code ["Crush"]}), so this class
 * normalizes via {@link #monsterStyle()} before every {@code monster.style} read.
 *
 * <h2>Deviations from source</h2>
 * <ul>
 *   <li><b>overhead prayers:</b> the commented-out PROTECT_FROM_X block (NPCVsPlayerCalc.ts:104-122) is
 *       left dropped, exactly as upstream — the prayer-through monsters are deferred there too.</li>
 *   <li><b>CalcDetails / track*:</b> the UI recorder is dropped (the {@link BaseCalc#track*} helpers are
 *       pure arithmetic). The {@code DetailKey} labels are documentation only.</li>
 *   <li><b>leagues:</b> none in NPCVsPlayerCalc.ts; the {@code player.leagues} paths inherited via
 *       {@link BaseCalc} are already dropped (spec §4.2).</li>
 * </ul>
 */
public strictfp class NpcVsPlayerCalc extends BaseCalc
{
	/**
	 * Subset of upstream {@code DetailKey} used by the ported NPC rolls. The {@link BaseCalc} {@code track*}
	 * helpers are pure arithmetic (the CalcDetails recorder is dropped), so these labels are documentation
	 * only — they preserve the upstream key names for reviewability.
	 */
	private static final class DetailKey
	{
		private DetailKey() {}

		static final String PLAYER_DEFENCE_ROLL_LEVEL = "Player defence roll (level)";
		static final String PLAYER_DEFENCE_ROLL_LEVEL_PRAYER = "Player defence roll (level prayer)";
		static final String PLAYER_DEFENCE_ROLL_LEVEL_TORAGS = "Player defence roll (level torags)";
		static final String PLAYER_DEFENCE_ROLL_MAGIC_LEVEL = "Player defence roll (magic level)";
		static final String PLAYER_DEFENCE_ROLL_MAGIC_LEVEL_PRAYER = "Player defence roll (magic level prayer)";
		static final String PLAYER_DEFENCE_ROLL_EFFECTIVE_LEVEL = "Player defence roll (effective level)";
		static final String PLAYER_DEFENCE_ROLL_GEAR_BONUS = "Player defence roll (gear bonus)";
		static final String PLAYER_DEFENCE_ROLL_FINAL = "Player defence roll";

		static final String NPC_ACCURACY_ROLL_BASE = "NPC accuracy roll (base)";
		static final String NPC_ACCURACY_ROLL_BONUS = "NPC accuracy roll (bonus)";
		static final String NPC_ACCURACY_ROLL_FINAL = "NPC accuracy roll";
	}

	private final SpellRepository spellRepository;

	private AttackDistribution memoizedDist;

	private PlayerVsNpcCalc memoizedPlayerVsNPCCalc;

	public NpcVsPlayerCalc(Player player, Monster monster, EquipmentRepository equipmentRepository)
	{
		this(player, monster, new CalcOpts(), equipmentRepository, null);
	}

	public NpcVsPlayerCalc(Player player, Monster monster, EquipmentRepository equipmentRepository,
		SpellRepository spellRepository)
	{
		this(player, monster, new CalcOpts(), equipmentRepository, spellRepository);
	}

	public NpcVsPlayerCalc(Player player, Monster monster, CalcOpts opts,
		EquipmentRepository equipmentRepository, SpellRepository spellRepository)
	{
		super(player, monster, opts, equipmentRepository);
		this.spellRepository = spellRepository;
	}

	/** Port of {@code getDistribution} (NPCVsPlayerCalc.ts:26-32). Memoized. */
	public AttackDistribution getDistribution()
	{
		if (this.memoizedDist == null)
		{
			this.memoizedDist = getDistributionImpl();
		}
		return this.memoizedDist;
	}

	/**
	 * Port of {@code getPlayerVsNPCCalc} (NPCVsPlayerCalc.ts:34-43). Memoized. Builds a forward
	 * (player-vs-NPC) calc with monster scaling disabled, used by {@link #getAverageDamageTaken()}.
	 */
	public PlayerVsNpcCalc getPlayerVsNPCCalc()
	{
		if (this.memoizedPlayerVsNPCCalc == null)
		{
			CalcOpts forward = new CalcOpts();
			forward.loadoutName = this.opts.loadoutName + "/forward";
			forward.disableMonsterScaling = true;
			this.memoizedPlayerVsNPCCalc = new PlayerVsNpcCalc(
				this.player, this.baseMonster, forward, this.equipmentRepository, this.spellRepository);
		}
		return this.memoizedPlayerVsNPCCalc;
	}

	/** Port of {@code getPlayerDefensiveBonus} (NPCVsPlayerCalc.ts:45-67). */
	private int getPlayerDefensiveBonus()
	{
		String style = monsterStyle();
		if (CombatStyle.CRUSH.equals(style))
		{
			return player.getDefensive().getCrush();
		}
		if (CombatStyle.STAB.equals(style))
		{
			return player.getDefensive().getStab();
		}
		if (CombatStyle.SLASH.equals(style))
		{
			return player.getDefensive().getSlash();
		}
		if (CombatStyle.MAGIC.equals(style))
		{
			return player.getDefensive().getMagic();
		}
		if (CombatStyle.RANGED.equals(style))
		{
			return player.getDefensive().getRanged();
		}
		return 0;
	}

	/** Port of {@code getDistributionImpl} (NPCVsPlayerCalc.ts:69-115). */
	public AttackDistribution getDistributionImpl()
	{
		double acc = getHitChance();
		int max = getNPCMaxHit();
		int bonus = getPlayerDefensiveBonus();

		// Standard linear
		HitDistribution standardHitDist = HitDistribution.linear(acc, 0, max);
		List<HitDistribution> dists = new ArrayList<>();
		dists.add(standardHitDist);
		AttackDistribution dist = new AttackDistribution(dists);

		if (wearing("Elysian spirit shield"))
		{
			List<WeightedHit> hits = new ArrayList<>();
			hits.addAll(standardHitDist.scaleProbability(0.3).hits);
			for (WeightedHit h : standardHitDist.scaleProbability(0.7).hits)
			{
				Hitsplat[] mapped = new Hitsplat[h.hitsplats.length];
				for (int i = 0; i < h.hitsplats.length; i++)
				{
					Hitsplat d = h.hitsplats[i];
					int reduction = Math.max(1, (int) ((double) d.damage / 4));
					mapped[i] = new Hitsplat(Math.max(0, d.damage - reduction), d.accurate);
				}
				hits.add(new WeightedHit(h.probability, mapped));
			}
			List<HitDistribution> elysian = new ArrayList<>();
			elysian.add(new HitDistribution(hits));
			dist = new AttackDistribution(elysian);
		}

		if (wearing("Dinh's bulwark", "Dinh's blazing bulwark") && "Block".equals(styleName()))
		{
			// Monster damage reduced by 20%
			dist = dist.scaleDamage(0.8);
		}

		if (isWearingJusticiarArmour())
		{
			double reduction = 1 - (double) bonus / 3000;
			List<WeightedHit> hits = new ArrayList<>();
			for (WeightedHit h : dist.dists.get(0).hits)
			{
				Hitsplat[] mapped = new Hitsplat[h.hitsplats.length];
				for (int i = 0; i < h.hitsplats.length; i++)
				{
					Hitsplat s = h.hitsplats[i];
					mapped[i] = new Hitsplat((int) (s.damage * reduction), s.accurate);
				}
				hits.add(new WeightedHit(h.probability, mapped));
			}
			List<HitDistribution> justiciar = new ArrayList<>();
			justiciar.add(new HitDistribution(hits));
			dist = new AttackDistribution(justiciar);
		}

		// There's some monsters that can hit through prayers, but let's worry about that later
		// (the overhead-prayer PROTECT_FROM_X block is dropped, exactly as upstream).

		return dist;
	}

	/** Port of {@code getPlayerDefenceRoll} (NPCVsPlayerCalc.ts:127-167). */
	public int getPlayerDefenceRoll()
	{
		String stance = styleStance();
		String style = monsterStyle();
		PlayerSkillsView skills = new PlayerSkillsView();
		PlayerBoostsView boosts = new PlayerBoostsView();
		List<PrayerData> prayers = prayerData();

		int stanceBonus = 0;
		if (CombatStyle.DEFENSIVE.equals(stance))
		{
			stanceBonus += 3;
		}
		if (CombatStyle.CONTROLLED.equals(stance))
		{
			stanceBonus += 1;
		}
		if (CombatStyle.LONGRANGE.equals(stance))
		{
			stanceBonus += 3;
		}

		int bonus = getPlayerDefensiveBonus();

		int effectiveLevel = trackAdd(DetailKey.PLAYER_DEFENCE_ROLL_LEVEL, skills.def, boosts.def);
		int numerator = 0;
		for (PrayerData p : prayers)
		{
			if (p.factorDefence() != null)
			{
				numerator += p.factorDefence().numerator() - 100;
			}
		}
		effectiveLevel = trackFactor(DetailKey.PLAYER_DEFENCE_ROLL_LEVEL_PRAYER, effectiveLevel, numerator + 100, 100);

		if (isWearingTorags())
		{
			int currentHealth = skills.hp + boosts.hp;
			int missingHealth = (int) ((Math.round((double) (skills.hp - currentHealth) / skills.hp * 100) / 100.0) * 100);
			effectiveLevel = trackFactor(DetailKey.PLAYER_DEFENCE_ROLL_LEVEL_TORAGS, effectiveLevel, 1 + missingHealth, 100);
		}

		if (CombatStyle.MAGIC.equals(style))
		{
			int effectiveMagicLevel = trackAdd(DetailKey.PLAYER_DEFENCE_ROLL_MAGIC_LEVEL, skills.magic, boosts.magic);
			for (PrayerData p : prayers)
			{
				if (p.factorDefenceMagic() != null && "magic".equals(p.combatStyle()))
				{
					effectiveMagicLevel = trackFactor(DetailKey.PLAYER_DEFENCE_ROLL_MAGIC_LEVEL_PRAYER,
						effectiveMagicLevel, p.factorDefenceMagic().numerator(), p.factorDefenceMagic().divisor());
				}
			}

			effectiveLevel = (int) ((long) effectiveMagicLevel * 7 / 10) + (int) ((long) effectiveLevel * 3 / 10);
		}

		effectiveLevel = trackAdd(DetailKey.PLAYER_DEFENCE_ROLL_EFFECTIVE_LEVEL, effectiveLevel, stanceBonus);
		int gearBonus = trackAdd(DetailKey.PLAYER_DEFENCE_ROLL_GEAR_BONUS, bonus, 64);

		return track(DetailKey.PLAYER_DEFENCE_ROLL_FINAL, (8 + effectiveLevel) * gearBonus);
	}

	/** Port of {@code getNPCMaxAttackRoll} (NPCVsPlayerCalc.ts:172-194). */
	public int getNPCMaxAttackRoll()
	{
		String style = monsterStyle();
		MonsterSkills skills = monster.getSkills();
		MonsterOffensive bonuses = monster.getOffensive();

		int roll = 0;
		int bonus = 0;

		if (CombatStyle.SLASH.equals(style) || CombatStyle.CRUSH.equals(style) || CombatStyle.STAB.equals(style))
		{
			roll = trackAdd(DetailKey.NPC_ACCURACY_ROLL_BASE, 9, skills.getAtk());
			bonus = trackAdd(DetailKey.NPC_ACCURACY_ROLL_BONUS, bonuses.getAtk(), 64);
		}
		if (CombatStyle.RANGED.equals(style))
		{
			roll = trackAdd(DetailKey.NPC_ACCURACY_ROLL_BASE, 9, skills.getRanged());
			bonus = trackAdd(DetailKey.NPC_ACCURACY_ROLL_BONUS, bonuses.getRanged(), 64);
		}
		if (CombatStyle.MAGIC.equals(style))
		{
			roll = trackAdd(DetailKey.NPC_ACCURACY_ROLL_BASE, 9, skills.getMagic());
			bonus = trackAdd(DetailKey.NPC_ACCURACY_ROLL_BONUS, bonuses.getMagic(), 64);
		}

		return track(DetailKey.NPC_ACCURACY_ROLL_FINAL, roll * bonus);
	}

	/** Port of {@code getNPCMaxHit} (NPCVsPlayerCalc.ts:199-236). */
	public int getNPCMaxHit()
	{
		String style = monsterStyle();
		MonsterSkills skills = monster.getSkills();
		MonsterOffensive bonuses = monster.getOffensive();
		String name = monster.getName();
		boolean isCustomMonster = monster.getId() == -1;

		int maxHit = 0;
		if (CombatStyle.SLASH.equals(style) || CombatStyle.CRUSH.equals(style) || CombatStyle.STAB.equals(style))
		{
			maxHit = (int) (((long) (9 + skills.getStr()) * (bonuses.getStr() + 64) + 320) / 640);
		}
		if (CombatStyle.RANGED.equals(style))
		{
			maxHit = (int) (((long) (9 + skills.getRanged()) * (bonuses.getRangedStr() + 64) + 320) / 640);
		}
		if (CombatStyle.MAGIC.equals(style))
		{
			maxHit = (int) (((long) (9 + skills.getMagic()) * (bonuses.getMagicStr() + 64) + 320) / 640);
		}

		// Some monsters have a hardcoded max hit. Let's overwrite the max hit with the real value here.
		Integer hardcoded = Constants.NPC_HARDCODED_MAX_HIT.get(monster.getId());
		if (hardcoded != null)
		{
			maxHit = hardcoded;
		}
		else if (CombatStyle.MAGIC.equals(style) && monster.getMaxHit() != null
			&& monsterWikiMaxHit() != null && maxHit != monsterWikiMaxHit() && !isCustomMonster)
		{
			// For now, if the monster is using the magic attack style and the wiki max hit differs from
			// the standard calculation, use the wiki value (a lot of magic monsters hardcode max hit).
			maxHit = monsterWikiMaxHit();
		}

		// Some monsters have a reduced max hit under specific conditions.
		if ("Aberrant spectre".equals(name) && (wearing("Nose peg") || isWearingSlayerHelmet()))
		{
			maxHit = 8;
		}
		if ("Dust devil".equals(name) && (wearing("Face mask") || isWearingSlayerHelmet()))
		{
			maxHit = 8;
		}
		if ("Banshee".equals(name) && (wearing("Earmuffs") || isWearingSlayerHelmet()))
		{
			maxHit = 2;
		}
		if ("Wall beast".equals(name) && (wearing("Spiny helmet") || isWearingSlayerHelmet()))
		{
			maxHit = 4;
		}
		if ("Sourhog".equals(name) && (wearing("Reinforced goggles") || isWearingSlayerHelmet()))
		{
			maxHit = 6;
		}
		if (("Ice troll female".equals(name) || "Thrower Troll".equals(name) || "Thrower troll (Trollheim)".equals(name))
			&& wearing("Neitiznot shield"))
		{
			maxHit = 2;
		}

		return maxHit;
	}

	/** Port of {@code getHitChance} (NPCVsPlayerCalc.ts:241-251). */
	public double getHitChance()
	{
		if (Constants.ALWAYS_ACCURATE_MONSTERS.contains(monster.getId()))
		{
			// This NPC will always hit the player with their attacks
			return 1;
		}

		int atk = getNPCMaxAttackRoll();
		int def = getPlayerDefenceRoll();

		return BaseCalc.getNormalAccuracyRoll(atk, def);
	}

	/** Port of {@code getDpt} (NPCVsPlayerCalc.ts:256-258): expected damage per tick at the NPC's speed. */
	public double getDpt()
	{
		return getDistribution().getExpectedDamage() / monster.getSpeed();
	}

	/** Port of {@code getDps} (NPCVsPlayerCalc.ts:263-265): damage-per-second = dpt / SECONDS_PER_TICK. */
	public double getDps()
	{
		return getDpt() / Constants.SECONDS_PER_TICK;
	}

	/**
	 * Port of {@code getAverageDamageTaken} (NPCVsPlayerCalc.ts:270-273): {@code ttk * getDps()} where
	 * {@code ttk} is the forward {@link PlayerVsNpcCalc#getTtk()}.
	 *
	 * <p>Upstream returns {@code ttk ? ttk * this.getDps() : undefined} — JS truthiness treats
	 * {@code 0}/{@code NaN} as falsy. This port returns {@code Double.NaN} for that falsy case (the parity
	 * sweep drops non-finite expected fields, so {@code avgDmgTaken} appears in the corpus only when ttk
	 * was finite and nonzero).
	 */
	public double getAverageDamageTaken()
	{
		double ttk = getPlayerVsNPCCalc().getTtk();
		if (ttk == 0 || Double.isNaN(ttk))
		{
			return Double.NaN;
		}
		return ttk * getDps();
	}

	// ---------------------------------------------------------------------------------------------
	// helpers
	// ---------------------------------------------------------------------------------------------

	/**
	 * Replicates upstream {@code getMonsters()} style normalization (Monsters.ts:9,17):
	 * {@code m.style.join(',').toLowerCase()} kept iff it is a valid {@code CombatStyleType}
	 * ({@code stab|slash|crush|magic|ranged}), else {@code null}.
	 */
	private String monsterStyle()
	{
		List<String> raw = monster.getStyle();
		if (raw == null || raw.isEmpty())
		{
			return null;
		}
		String joined = String.join(",", raw).toLowerCase(java.util.Locale.ROOT);
		return CombatStyle.COMBAT_STYLE_TYPES.contains(joined) ? joined : null;
	}

	/** Upstream {@code monster.maxHit} (a normalized {@code number} from {@code parseInt(max_hit)}, NaN→0). */
	private Integer monsterWikiMaxHit()
	{
		String s = monster.getMaxHit();
		if (s == null)
		{
			return null;
		}
		// parseInt semantics: leading integer prefix, NaN -> 0 (Monsters.ts:8,18).
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("^\\s*([+-]?\\d+)").matcher(s);
		if (m.find())
		{
			try
			{
				return Integer.valueOf(m.group(1));
			}
			catch (NumberFormatException e)
			{
				return 0;
			}
		}
		return 0;
	}

	private String styleStance()
	{
		return player.getStyle() == null ? null : player.getStyle().getStance();
	}

	private String styleName()
	{
		return player.getStyle() == null ? null : player.getStyle().getName();
	}

	private List<PrayerData> prayerData()
	{
		List<PrayerData> out = new ArrayList<>();
		List<Prayer> prayers = player.getPrayers();
		if (prayers != null)
		{
			for (Prayer p : prayers)
			{
				out.add(p.data());
			}
		}
		return out;
	}

	/** Inlined view of {@code this.player.skills} (def/hp/magic) used by {@link #getPlayerDefenceRoll()}. */
	private final class PlayerSkillsView
	{
		final int def = player.getSkills().getDef();
		final int hp = player.getSkills().getHp();
		final int magic = player.getSkills().getMagic();
	}

	/** Inlined view of {@code this.player.boosts} (def/hp/magic) used by {@link #getPlayerDefenceRoll()}. */
	private final class PlayerBoostsView
	{
		final int def = player.getBoosts().getDef();
		final int hp = player.getBoosts().getHp();
		final int magic = player.getBoosts().getMagic();
	}
}
