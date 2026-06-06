package org.bossbis.calc;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bossbis.calc.support.D3;

/**
 * Port of src/lib/HitDist.ts (the hit-distribution pipeline) — {@link Hitsplat}, {@link WeightedHit},
 * {@link HitDistribution}, {@link AttackDistribution}, the transformer factories, and the
 * probabilistic-delay machinery.
 *
 * <p>Determinism rules (spec §4.7) are load-bearing:
 * <ul>
 *   <li>{@link WeightedHit#getHash()} uses {@link BigInteger} ({@code acc <<= 8} accumulation) to
 *       reproduce upstream's {@code bigint} keys exactly (it dedupes distribution branches);</li>
 *   <li>{@link HitDistribution#withProbabilisticDelays} packs {@code (getSum() & 0xFFFFFF) | (delay << 24)}
 *       and unpacks with the verbatim {@code 0x8F000000} mask, both on signed Java {@code int}
 *       (a known upstream quirk — replicated, not "fixed");</li>
 *   <li>{@link D3} preserves d3-array's left-to-right summation / cross order.</li>
 * </ul>
 *
 * <p>{@code Math.trunc(x)} (TS) is ported as a Java {@code (int)}/{@code (long)} truncating cast.
 */
public final strictfp class HitDist
{
	private HitDist() {}

	/** {@code HitTransformer = (hitsplat: Hitsplat) => HitDistribution}. */
	public interface HitTransformer
	{
		HitDistribution apply(Hitsplat hitsplat);
	}

	/** {@code ProbabilisticDelay = [probability, delay]}. */
	public static final class ProbabilisticDelay
	{
		public final double probability;
		public final int delay;

		public ProbabilisticDelay(double probability, int delay)
		{
			this.probability = probability;
			this.delay = delay;
		}
	}

	/** {@code WeaponDelayProvider = (wh: WeightedHit) => ProbabilisticDelay[]}. */
	public interface WeaponDelayProvider
	{
		List<ProbabilisticDelay> apply(WeightedHit wh);
	}

	/** {@code DelayedHit = [wh: WeightedHit, delay]}. */
	public static final class DelayedHit
	{
		public final WeightedHit wh;
		public final int delay;

		public DelayedHit(WeightedHit wh, int delay)
		{
			this.wh = wh;
			this.delay = delay;
		}
	}

	/** Port of {@code TransformOpts}. */
	public static final class TransformOpts
	{
		public final boolean transformInaccurate;

		public TransformOpts(boolean transformInaccurate)
		{
			this.transformInaccurate = transformInaccurate;
		}
	}

	public static final TransformOpts DEFAULT_TRANSFORM_OPTS = new TransformOpts(true);

	/** Port of {@code ChartEntry & { name, value }} as returned by {@link AttackDistribution#asHistogram}. */
	public static final class ChartEntry
	{
		public final String name;
		public final double value;

		public ChartEntry(String name, double value)
		{
			this.name = name;
			this.value = value;
		}
	}

	public static final class Hitsplat
	{
		public static final Hitsplat INACCURATE = new Hitsplat(0, false);

		public final int damage;
		public final boolean accurate;

		public Hitsplat(int damage)
		{
			this(damage, true);
		}

		public Hitsplat(int damage, boolean accurate)
		{
			this.damage = damage;
			this.accurate = accurate;
		}

		public HitDistribution transform(HitTransformer t)
		{
			return transform(t, DEFAULT_TRANSFORM_OPTS);
		}

		public HitDistribution transform(HitTransformer t, TransformOpts opts)
		{
			if (!this.accurate && !opts.transformInaccurate)
			{
				List<WeightedHit> hits = new ArrayList<>();
				hits.add(new WeightedHit(1.0, new Hitsplat[]{this}));
				return new HitDistribution(hits);
			}
			return t.apply(this);
		}
	}

	public static final class WeightedHit
	{
		public final double probability;
		public final Hitsplat[] hitsplats;

		private Integer cachedSum;

		public WeightedHit(double probability, Hitsplat[] hitsplats)
		{
			this.probability = probability;
			this.hitsplats = hitsplats;
		}

		public WeightedHit scale(double factor)
		{
			return new WeightedHit(this.probability * factor, this.hitsplats.clone());
		}

		public WeightedHit zip(WeightedHit other)
		{
			Hitsplat[] combined = new Hitsplat[this.hitsplats.length + other.hitsplats.length];
			System.arraycopy(this.hitsplats, 0, combined, 0, this.hitsplats.length);
			System.arraycopy(other.hitsplats, 0, combined, this.hitsplats.length, other.hitsplats.length);
			return new WeightedHit(this.probability * other.probability, combined);
		}

		/** {@code shift(): [WeightedHit, WeightedHit]} — head (first splat) + tail (1.0, remaining splats). */
		public WeightedHit[] shift()
		{
			Hitsplat[] tail = new Hitsplat[this.hitsplats.length - 1];
			System.arraycopy(this.hitsplats, 1, tail, 0, tail.length);
			return new WeightedHit[]{
				new WeightedHit(this.probability, new Hitsplat[]{this.hitsplats[0]}),
				new WeightedHit(1.0, tail),
			};
		}

		public HitDistribution transform(HitTransformer t)
		{
			return transform(t, DEFAULT_TRANSFORM_OPTS);
		}

		public HitDistribution transform(HitTransformer t, TransformOpts opts)
		{
			if (this.hitsplats.length == 1)
			{
				return this.hitsplats[0].transform(t, opts)
					.scaleProbability(this.probability);
			}

			// recursively zip first hitsplat with remaining hitsplats
			WeightedHit[] shifted = this.shift();
			WeightedHit head = shifted[0];
			WeightedHit tail = shifted[1];
			return head.transform(t, opts)
				.zip(tail.transform(t, opts));
		}

		public boolean anyAccurate()
		{
			return D3.some(java.util.Arrays.asList(this.hitsplats), h -> h.accurate);
		}

		public int getSum()
		{
			if (this.cachedSum == null)
			{
				this.cachedSum = (int) D3.sum(java.util.Arrays.asList(this.hitsplats), h -> h.damage);
			}
			return this.cachedSum;
		}

		public double getExpectedValue()
		{
			return this.probability * this.getSum();
		}

		public BigInteger getHash()
		{
			BigInteger acc = BigInteger.ZERO;
			for (Hitsplat hitsplat : this.hitsplats)
			{
				acc = acc.shiftLeft(8);
				acc = acc.or(BigInteger.valueOf(hitsplat.damage));
				acc = acc.shiftLeft(1);
				acc = acc.or(hitsplat.accurate ? BigInteger.ONE : BigInteger.ZERO);
			}
			return acc;
		}

		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < this.hitsplats.length; i++)
			{
				if (i > 0)
				{
					sb.append(',');
				}
				sb.append(this.hitsplats[i].damage);
			}
			return "WeightedHit(p: " + this.probability + ", h: [" + sb + "], s: " + this.getSum() + ")";
		}
	}

	public static final class HitDistribution
	{
		public final List<WeightedHit> hits;

		public HitDistribution(List<WeightedHit> hits)
		{
			this.hits = hits;
		}

		public void addHit(WeightedHit w)
		{
			this.hits.add(w);
		}

		public void addHits(List<WeightedHit> w)
		{
			this.hits.addAll(w);
		}

		public HitDistribution zip(HitDistribution other)
		{
			List<int[]> crossed = D3.cross(this.hits.size(), other.hits.size());
			List<WeightedHit> out = new ArrayList<>(crossed.size());
			for (int[] pair : crossed)
			{
				out.add(this.hits.get(pair[0]).zip(other.hits.get(pair[1])));
			}
			return new HitDistribution(out);
		}

		public HitDistribution wideTransform(HitTransformer t)
		{
			return wideTransform(t, DEFAULT_TRANSFORM_OPTS);
		}

		public HitDistribution wideTransform(HitTransformer t, TransformOpts opts)
		{
			HitDistribution d = new HitDistribution(new ArrayList<>());
			for (WeightedHit h : this.hits)
			{
				for (WeightedHit transformed : h.transform(t, opts).hits)
				{
					d.addHit(transformed);
				}
			}
			return d;
		}

		public HitDistribution transform(HitTransformer t)
		{
			return transform(t, DEFAULT_TRANSFORM_OPTS);
		}

		public HitDistribution transform(HitTransformer t, TransformOpts opts)
		{
			return this.wideTransform(t, opts).flatten();
		}

		public HitDistribution scaleProbability(double factor)
		{
			List<WeightedHit> out = new ArrayList<>(this.hits.size());
			for (WeightedHit h : this.hits)
			{
				out.add(h.scale(factor));
			}
			return new HitDistribution(out);
		}

		public HitDistribution scaleDamage(double factor)
		{
			return scaleDamage(factor, 1);
		}

		public HitDistribution scaleDamage(double factor, double divisor)
		{
			List<WeightedHit> out = new ArrayList<>(this.hits.size());
			for (WeightedHit h : this.hits)
			{
				Hitsplat[] splats = new Hitsplat[h.hitsplats.length];
				for (int i = 0; i < splats.length; i++)
				{
					Hitsplat s = h.hitsplats[i];
					splats[i] = new Hitsplat((int) (s.damage * factor / divisor), s.accurate);
				}
				out.add(new WeightedHit(h.probability, splats));
			}
			return new HitDistribution(out);
		}

		/** Merges the probabilities of hits with identical damage values. */
		public HitDistribution flatten()
		{
			Map<BigInteger, Double> acc = new LinkedHashMap<>();
			Map<BigInteger, Hitsplat[]> hitLists = new LinkedHashMap<>();
			for (WeightedHit hit : this.hits)
			{
				BigInteger hash = hit.getHash();
				Double prev = acc.get(hash);
				if (prev == null)
				{
					acc.put(hash, hit.probability);
					hitLists.put(hash, hit.hitsplats);
				}
				else
				{
					acc.put(hash, prev + hit.probability);
				}
			}

			HitDistribution d = new HitDistribution(new ArrayList<>());
			for (Map.Entry<BigInteger, Double> e : acc.entrySet())
			{
				double prob = e.getValue();
				if (prob > 0)
				{
					d.addHit(new WeightedHit(prob, hitLists.get(e.getKey())));
				}
			}
			return d;
		}

		/** Converts multi-hits into a single cumulative damage total. */
		public HitDistribution cumulative()
		{
			HitDistribution d = new HitDistribution(new ArrayList<>());
			Map<Integer, Double> acc = new LinkedHashMap<>();
			for (WeightedHit hit : this.hits)
			{
				// if 1 splat is accurate, treat the whole hit as accurate
				// if inaccurate, take the bitwise inverse so that we have a number key that's distinct, we'll undo it later
				int key = hit.anyAccurate() ? hit.getSum() : ~hit.getSum();
				Double prev = acc.get(key);
				if (prev == null)
				{
					acc.put(key, hit.probability);
				}
				else
				{
					acc.put(key, prev + hit.probability);
				}
			}

			for (Map.Entry<Integer, Double> e : acc.entrySet())
			{
				int key = e.getKey();
				double prob = e.getValue();
				boolean accurate = key >= 0;
				int dmg = accurate ? key : ~key;
				if (prob > 0)
				{
					d.addHit(new WeightedHit(prob, new Hitsplat[]{new Hitsplat(dmg, accurate)}));
				}
			}

			return d;
		}

		public double expectedHit()
		{
			return D3.sum(this.hits, WeightedHit::getExpectedValue);
		}

		public int size()
		{
			return this.hits.size();
		}

		public int getMin()
		{
			List<Double> sums = new ArrayList<>(this.hits.size());
			for (WeightedHit h : this.hits)
			{
				sums.add((double) h.getSum());
			}
			return (int) D3.min(sums).getAsDouble();
		}

		public int getMax()
		{
			List<Double> sums = new ArrayList<>(this.hits.size());
			for (WeightedHit h : this.hits)
			{
				sums.add((double) h.getSum());
			}
			return (int) D3.max(sums).getAsDouble();
		}

		public List<DelayedHit> withProbabilisticDelays(WeaponDelayProvider delayProvider)
		{
			List<DelayedHit> hits = new ArrayList<>();
			for (WeightedHit wh : this.hits)
			{
				List<ProbabilisticDelay> delays = delayProvider.apply(wh);
				for (ProbabilisticDelay pd : delays)
				{
					hits.add(new DelayedHit(
						new WeightedHit(
							wh.probability * pd.probability,
							new Hitsplat[]{new Hitsplat(wh.getSum(), wh.anyAccurate())}
						),
						pd.delay
					));
				}
			}

			// dedupe the results and merge entries
			List<DelayedHit> d = new ArrayList<>();
			Map<Integer, Double> acc = new LinkedHashMap<>();
			for (DelayedHit dh : hits)
			{
				WeightedHit wh = dh.wh;
				int key = (wh.getSum() & 0xFFFFFF) | (dh.delay << 24);
				Double prev = acc.get(key);
				if (prev == null)
				{
					acc.put(key, wh.probability);
				}
				else
				{
					acc.put(key, prev + wh.probability);
				}
			}

			for (Map.Entry<Integer, Double> e : acc.entrySet())
			{
				int key = e.getKey();
				double prob = e.getValue();
				int delay = (key & 0x8F000000) >> 24;
				int dmg = key & 0xFFFFFF;
				d.add(new DelayedHit(new WeightedHit(prob, new Hitsplat[]{new Hitsplat(dmg, true)}), delay));
			}

			return d;
		}

		public static HitDistribution linear(double accuracy, int minimum, int maximum)
		{
			HitDistribution d = new HitDistribution(new ArrayList<>());
			double hitProb = accuracy / (maximum - minimum + 1);
			for (int i = minimum; i <= maximum; i++)
			{
				d.addHit(new WeightedHit(hitProb, new Hitsplat[]{new Hitsplat(i)}));
			}
			d.addHit(new WeightedHit(1 - accuracy, new Hitsplat[]{Hitsplat.INACCURATE}));
			return d;
		}

		public static HitDistribution single(double accuracy, Hitsplat[] hitsplats)
		{
			List<WeightedHit> initial = new ArrayList<>();
			initial.add(new WeightedHit(accuracy, hitsplats));
			HitDistribution d = new HitDistribution(initial);
			if (accuracy != 1.0)
			{
				d.addHit(new WeightedHit(1 - accuracy, new Hitsplat[]{Hitsplat.INACCURATE}));
			}
			return d;
		}
	}

	public static final class AttackDistribution
	{
		public final List<HitDistribution> dists;

		private HitDistribution zipped;
		private HitDistribution singleHitsplat;

		public AttackDistribution(List<HitDistribution> dists)
		{
			this.dists = dists;
		}

		public HitDistribution getZipped()
		{
			if (this.zipped == null)
			{
				HitDistribution prev = this.dists.get(0);
				for (int i = 1; i < this.dists.size(); i++)
				{
					prev = prev.zip(this.dists.get(i));
				}
				this.zipped = prev;
			}
			return this.zipped;
		}

		public HitDistribution getSingleHitsplat()
		{
			if (this.singleHitsplat == null)
			{
				HitDistribution prev = this.dists.get(0);
				for (int i = 1; i < this.dists.size(); i++)
				{
					prev = prev.zip(this.dists.get(i)).cumulative();
				}
				this.singleHitsplat = prev;
			}
			return this.singleHitsplat;
		}

		public void addDist(HitDistribution d)
		{
			this.dists.add(d);
		}

		public AttackDistribution transform(HitTransformer t)
		{
			return transform(t, DEFAULT_TRANSFORM_OPTS);
		}

		public AttackDistribution transform(HitTransformer t, TransformOpts opts)
		{
			return this.map(d -> d.transform(t, opts));
		}

		public AttackDistribution flatten()
		{
			return this.map(HitDistribution::flatten);
		}

		public AttackDistribution scaleProbability(double factor)
		{
			return this.map(d -> d.scaleProbability(factor));
		}

		public AttackDistribution scaleDamage(double factor)
		{
			return scaleDamage(factor, 1);
		}

		public AttackDistribution scaleDamage(double factor, double divisor)
		{
			return this.map(d -> d.scaleDamage(factor, divisor));
		}

		public int getMin()
		{
			List<Double> mins = new ArrayList<>(this.dists.size());
			for (HitDistribution d : this.dists)
			{
				mins.add((double) d.getMin());
			}
			return (int) D3.sum(mins);
		}

		public int getMax()
		{
			List<Double> maxes = new ArrayList<>(this.dists.size());
			for (HitDistribution d : this.dists)
			{
				maxes.add((double) d.getMax());
			}
			return (int) D3.sum(maxes);
		}

		public double getExpectedDamage()
		{
			List<Double> expected = new ArrayList<>(this.dists.size());
			for (HitDistribution d : this.dists)
			{
				expected.add(d.expectedHit());
			}
			return D3.sum(expected);
		}

		public List<ChartEntry> asHistogram()
		{
			return asHistogram(false);
		}

		public List<ChartEntry> asHistogram(boolean hideMisses)
		{
			HitDistribution dist = this.getSingleHitsplat();

			Map<Integer, Double> hitMap = new LinkedHashMap<>();
			for (WeightedHit h : dist.hits)
			{
				if (!hideMisses || h.anyAccurate())
				{
					int sum = h.getSum();
					hitMap.put(sum, (hitMap.getOrDefault(sum, 0.0)) + h.probability);
				}
			}

			List<ChartEntry> ret = new ArrayList<>();
			for (int i = 0; i <= dist.getMax(); i++)
			{
				Double prob = hitMap.get(i);
				if (prob == null)
				{
					ret.add(new ChartEntry(Integer.toString(i), 0));
				}
				else
				{
					ret.add(new ChartEntry(Integer.toString(i), prob));
				}
			}

			return ret;
		}

		private AttackDistribution map(java.util.function.Function<HitDistribution, HitDistribution> m)
		{
			List<HitDistribution> mapped = new ArrayList<>(this.dists.size());
			for (HitDistribution d : this.dists)
			{
				mapped.add(m.apply(d));
			}
			return new AttackDistribution(mapped);
		}
	}

	public static HitTransformer flatLimitTransformer(int maximum)
	{
		return flatLimitTransformer(maximum, 0);
	}

	public static HitTransformer flatLimitTransformer(int maximum, int minimum)
	{
		return h ->
		{
			List<WeightedHit> hits = new ArrayList<>();
			hits.add(new WeightedHit(1.0, new Hitsplat[]{
				new Hitsplat(Math.max(minimum, Math.min(h.damage, maximum)), h.accurate)
			}));
			return new HitDistribution(hits);
		};
	}

	public static HitTransformer linearMinTransformer(int maximum)
	{
		return linearMinTransformer(maximum, 0);
	}

	public static HitTransformer linearMinTransformer(int maximum, int offset)
	{
		return h ->
		{
			HitDistribution d = new HitDistribution(new ArrayList<>());
			double prob = 1.0 / (maximum + 1);
			for (int i = 0; i <= maximum; i++)
			{
				d.addHit(new WeightedHit(
					prob,
					new Hitsplat[]{new Hitsplat(Math.min(h.damage, i + offset), h.accurate)}
				));
			}
			return d.flatten();
		};
	}

	public static HitTransformer cappedRerollTransformer(int limit, int rollMax)
	{
		return cappedRerollTransformer(limit, rollMax, 0);
	}

	public static HitTransformer cappedRerollTransformer(int limit, int rollMax, int offset)
	{
		return h ->
		{
			if (h.damage <= limit)
			{
				List<WeightedHit> hits = new ArrayList<>();
				hits.add(new WeightedHit(1.0, new Hitsplat[]{h}));
				return new HitDistribution(hits);
			}

			HitDistribution d = new HitDistribution(new ArrayList<>());
			double prob = 1.0 / (rollMax + 1);
			for (int i = 0; i <= rollMax; i++)
			{
				d.addHit(new WeightedHit(
					prob,
					new Hitsplat[]{new Hitsplat(h.damage > limit ? i + offset : h.damage, h.accurate)}
				));
			}
			return d.flatten();
		};
	}

	public static HitTransformer multiplyTransformer(int numerator)
	{
		return multiplyTransformer(numerator, 1, 0);
	}

	public static HitTransformer multiplyTransformer(int numerator, int divisor)
	{
		return multiplyTransformer(numerator, divisor, 0);
	}

	public static HitTransformer multiplyTransformer(int numerator, int divisor, int minimum)
	{
		return h ->
		{
			int dmg = (int) ((long) numerator * h.damage / divisor);
			if (minimum != 0)
			{
				if (h.damage >= minimum)
				{
					// if the value started above the minimum, make sure it doesn't drop below
					dmg = Math.max(minimum, dmg);
				}
				else
				{
					// if the value started below the minimum, make sure it isn't reduced, but respect increases
					dmg = Math.max(h.damage, dmg);
				}
			}
			List<WeightedHit> hits = new ArrayList<>();
			hits.add(new WeightedHit(1.0, new Hitsplat[]{new Hitsplat(dmg, h.accurate)}));
			return new HitDistribution(hits);
		};
	}

	public static HitTransformer divisionTransformer(int divisor)
	{
		return divisionTransformer(divisor, 0);
	}

	public static HitTransformer divisionTransformer(int divisor, int minimum)
	{
		return multiplyTransformer(1, divisor, minimum);
	}

	public static HitTransformer flatAddTransformer(int addend)
	{
		return flatAddTransformer(addend, 0);
	}

	public static HitTransformer flatAddTransformer(int addend, int minimum)
	{
		return h ->
		{
			List<WeightedHit> hits = new ArrayList<>();
			hits.add(new WeightedHit(1.0, new Hitsplat[]{
				new Hitsplat(Math.max(minimum, h.damage + addend), h.accurate)
			}));
			return new HitDistribution(hits);
		};
	}
}
