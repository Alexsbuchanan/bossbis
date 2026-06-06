package org.bossbis.calc.dists;

import java.util.ArrayList;
import java.util.List;
import org.bossbis.calc.Constants;
import org.bossbis.calc.HitDist.HitDistribution;
import org.bossbis.calc.HitDist.HitTransformer;
import org.bossbis.calc.HitDist.Hitsplat;
import org.bossbis.calc.HitDist.WeightedHit;
import org.bossbis.calc.support.D3;
import org.bossbis.calc.types.Monster;
import org.bossbis.calc.types.MonsterAttribute;

/**
 * Port of src/lib/dists/bolts.ts — the enchanted-bolt proc distributions (opal, pearl, diamond,
 * dragonstone, onyx, ruby) plus {@link BoltContext} and {@link BoltTransformer}.
 *
 * <p>{@code Math.trunc} (TS) is ported as a truncating {@code (int)}/{@code (long)} cast.
 */
public final strictfp class BoltsDist
{
	private BoltsDist() {}

	/** Port of the {@code BoltContext} interface. */
	public static final class BoltContext
	{
		public final int rangedLvl;
		public final int maxHit;
		public final boolean alwaysMaxHit;
		public final boolean zcb;
		public final boolean spec;
		public final boolean kandarinDiary;
		public final Monster monster;

		public BoltContext(int rangedLvl, int maxHit, boolean alwaysMaxHit, boolean zcb, boolean spec,
			boolean kandarinDiary, Monster monster)
		{
			this.rangedLvl = rangedLvl;
			this.maxHit = maxHit;
			this.alwaysMaxHit = alwaysMaxHit;
			this.zcb = zcb;
			this.spec = spec;
			this.kandarinDiary = kandarinDiary;
			this.monster = monster;
		}
	}

	/** {@code BoltTransformer = (ctx: BoltContext) => HitTransformer}. */
	public interface BoltTransformer
	{
		HitTransformer apply(BoltContext ctx);
	}

	private static double kandarinFactor(BoltContext ctx)
	{
		return ctx.kandarinDiary ? 1.1 : 1.0;
	}

	private static HitTransformer bonusDamageTransform(BoltContext ctx, double chance, int bonusDmg, boolean accurateOnly)
	{
		boolean zcb = ctx.zcb;
		boolean spec = ctx.spec;
		return h ->
		{
			if (h.accurate && zcb && spec)
			{
				return HitDistribution.single(1.0, new Hitsplat[]{new Hitsplat(h.damage + bonusDmg)});
			}
			if (!h.accurate && accurateOnly)
			{
				List<WeightedHit> hits = new ArrayList<>();
				hits.add(new WeightedHit(1.0, new Hitsplat[]{h}));
				return new HitDistribution(hits);
			}
			List<WeightedHit> hits = new ArrayList<>();
			hits.add(new WeightedHit(chance, new Hitsplat[]{new Hitsplat(h.damage + bonusDmg, h.accurate)}));
			hits.add(new WeightedHit(1 - chance, new Hitsplat[]{new Hitsplat(h.damage, h.accurate)}));
			return new HitDistribution(hits);
		};
	}

	public static final BoltTransformer OPAL_BOLTS = ctx ->
	{
		int rangedLvl = ctx.rangedLvl;
		boolean zcb = ctx.zcb;
		double chance = 0.05 * kandarinFactor(ctx);
		int bonusDmg = rangedLvl / (zcb ? 9 : 10);

		return bonusDamageTransform(ctx, chance, bonusDmg, false);
	};

	public static final BoltTransformer PEARL_BOLTS = ctx ->
	{
		int rangedLvl = ctx.rangedLvl;
		boolean zcb = ctx.zcb;
		Monster monster = ctx.monster;
		double chance = 0.06 * kandarinFactor(ctx);
		int divisor = monster.getAttributes().contains(MonsterAttribute.FIERY) ? 15 : 20;
		int bonusDmg = rangedLvl / (zcb ? divisor - 2 : divisor);

		return bonusDamageTransform(ctx, chance, bonusDmg, false);
	};

	public static final BoltTransformer DIAMOND_BOLTS = ctx ->
	{
		int maxHit = ctx.maxHit;
		boolean zcb = ctx.zcb;
		boolean spec = ctx.spec;
		double chance = 0.1 * kandarinFactor(ctx);
		int effectMax = (int) ((long) maxHit * (zcb ? 126 : 115) / 100);

		HitDistribution effectDist = ctx.alwaysMaxHit
			? HitDistribution.single(1.0, new Hitsplat[]{new Hitsplat(effectMax)})
			: HitDistribution.linear(1.0, 0, effectMax);
		return h ->
		{
			if (h.accurate && zcb && spec)
			{
				return effectDist;
			}
			List<WeightedHit> hits = new ArrayList<>(effectDist.scaleProbability(chance).hits);
			hits.add(new WeightedHit(1 - chance, new Hitsplat[]{new Hitsplat(h.damage, h.accurate)}));
			return new HitDistribution(hits);
		};
	};

	public static final BoltTransformer DRAGONSTONE_BOLTS = ctx ->
	{
		int rangedLvl = ctx.rangedLvl;
		boolean zcb = ctx.zcb;
		Monster monster = ctx.monster;

		if (D3.some(monster.getAttributes(),
			attr -> attr == MonsterAttribute.FIERY || attr == MonsterAttribute.DRAGON))
		{
			// immune to dragonfire
			return h ->
			{
				List<WeightedHit> hits = new ArrayList<>();
				hits.add(new WeightedHit(1.0, new Hitsplat[]{h}));
				return new HitDistribution(hits);
			};
		}

		double chance = 0.06 * kandarinFactor(ctx);
		int bonusDmg = (int) ((long) rangedLvl * 2 / (zcb ? 9 : 10));

		return bonusDamageTransform(ctx, chance, bonusDmg, true);
	};

	public static final BoltTransformer ONYX_BOLTS = ctx ->
	{
		int maxHit = ctx.maxHit;
		boolean zcb = ctx.zcb;
		boolean spec = ctx.spec;
		Monster monster = ctx.monster;

		if (monster.getAttributes().contains(MonsterAttribute.UNDEAD))
		{
			// immune to life leech
			return h ->
			{
				List<WeightedHit> hits = new ArrayList<>();
				hits.add(new WeightedHit(1.0, new Hitsplat[]{h}));
				return new HitDistribution(hits);
			};
		}

		double chance = 0.11 * kandarinFactor(ctx);
		int effectMax = (int) ((long) maxHit * (zcb ? 132 : 120) / 100);

		HitDistribution effectDist = ctx.alwaysMaxHit
			? HitDistribution.single(1.0, new Hitsplat[]{new Hitsplat(effectMax)})
			: HitDistribution.linear(1.0, 0, effectMax);
		return h ->
		{
			if (!h.accurate)
			{
				List<WeightedHit> hits = new ArrayList<>();
				hits.add(new WeightedHit(1.0, new Hitsplat[]{h}));
				return new HitDistribution(hits);
			}
			if (zcb && spec)
			{
				return effectDist;
			}
			List<WeightedHit> hits = new ArrayList<>(effectDist.scaleProbability(chance).hits);
			hits.add(new WeightedHit(1 - chance, new Hitsplat[]{new Hitsplat(h.damage, h.accurate)}));
			return new HitDistribution(hits);
		};
	};

	public static final BoltTransformer RUBY_BOLTS = ctx ->
	{
		boolean zcb = ctx.zcb;
		boolean spec = ctx.spec;
		Monster monster = ctx.monster;
		double chance = 0.06 * kandarinFactor(ctx);
		int cap;

		if (Constants.INFINITE_HEALTH_MONSTERS.contains(ctx.monster.getId()))
		{
			cap = zcb ? 66 : 60;
		}
		else
		{
			cap = zcb ? 110 : 100;
		}

		int effectDmg = (int) ((long) monster.getInputs().getMonsterCurrentHp() * (zcb ? 22 : 20) / 100);
		HitDistribution effectHit = HitDistribution.single(1.0, new Hitsplat[]{new Hitsplat(Math.min(cap, effectDmg))});

		return h ->
		{
			if (h.accurate && zcb && spec)
			{
				return effectHit;
			}
			List<WeightedHit> hits = new ArrayList<>(effectHit.scaleProbability(chance).hits);
			hits.add(new WeightedHit(1 - chance, new Hitsplat[]{new Hitsplat(h.damage, h.accurate)}));
			return new HitDistribution(hits);
		};
	};
}
