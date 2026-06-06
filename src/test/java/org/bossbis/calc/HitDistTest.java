package org.bossbis.calc;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.bossbis.calc.HitDist.AttackDistribution;
import org.bossbis.calc.HitDist.ChartEntry;
import org.bossbis.calc.HitDist.HitDistribution;
import org.bossbis.calc.HitDist.Hitsplat;
import org.bossbis.calc.HitDist.WeightedHit;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HitDistTest
{
	private static final double EPS = 1e-12;

	/** A small hand-built single-dist distribution: {0,1,2,3,4} each at accuracy/5 = 0.16, plus a 0.2 miss. */
	private static HitDistribution linear04()
	{
		// accuracy 0.8, min 0, max 4
		return HitDistribution.linear(0.8, 0, 4);
	}

	@Test
	void multiplyTransformerTruncatesAndPreservesAccuracy()
	{
		// 3/2 of each splat, truncating: 0->0, 1->1, 2->3, 3->4, 4->6
		HitDistribution d = linear04().transform(HitDist.multiplyTransformer(3, 2));

		// the inaccurate (miss) splat stays 0 and merges with the accurate 0 only by hash (different accurate flag)
		// expected accurate damages after transform: {0,1,3,4,6}
		assertThat(d.getMax()).isEqualTo(6);

		// expected hit = 0.16*(0+1+3+4+6) = 0.16*14 = 2.24
		assertThat(d.expectedHit()).isCloseTo(2.24, within(EPS));
	}

	@Test
	void flatLimitTransformerCapsDamage()
	{
		HitDistribution d = linear04().transform(HitDist.flatLimitTransformer(2));
		// damages clamped to [0,2]: {0,1,2,2,2}, miss at 0
		assertThat(d.getMax()).isEqualTo(2);
		// expected = 0.16*(0+1+2+2+2) = 0.16*7 = 1.12
		assertThat(d.expectedHit()).isCloseTo(1.12, within(EPS));
	}

	@Test
	void zipIsConvolutionOfTwoDists()
	{
		// two certain single hits: 2 and 3 -> zipped -> one combined hit summing to 5 at prob 1
		HitDistribution a = HitDistribution.single(1.0, new Hitsplat[]{new Hitsplat(2)});
		HitDistribution b = HitDistribution.single(1.0, new Hitsplat[]{new Hitsplat(3)});

		HitDistribution z = a.zip(b);
		assertThat(z.size()).isEqualTo(1);
		assertThat(z.getMax()).isEqualTo(5);
		assertThat(z.hits.get(0).probability).isCloseTo(1.0, within(EPS));
		assertThat(z.hits.get(0).hitsplats).hasSize(2);

		// two-element x two-element -> 4 combined hits, max = 1+1 = 2, sum of probs = 1
		HitDistribution p = new HitDistribution(new ArrayList<>(List.of(
			new WeightedHit(0.5, new Hitsplat[]{new Hitsplat(0)}),
			new WeightedHit(0.5, new Hitsplat[]{new Hitsplat(1)})
		)));
		HitDistribution q = new HitDistribution(new ArrayList<>(List.of(
			new WeightedHit(0.5, new Hitsplat[]{new Hitsplat(0)}),
			new WeightedHit(0.5, new Hitsplat[]{new Hitsplat(1)})
		)));
		HitDistribution zz = p.zip(q);
		assertThat(zz.size()).isEqualTo(4);
		double probSum = 0;
		for (WeightedHit wh : zz.hits)
		{
			probSum += wh.probability;
		}
		assertThat(probSum).isCloseTo(1.0, within(EPS));
		assertThat(zz.getMax()).isEqualTo(2);
		// expected of each independent uniform {0,1} is 0.5; sum of two = 1.0
		assertThat(zz.expectedHit()).isCloseTo(1.0, within(EPS));
	}

	@Test
	void getMaxSumsPerDistMaxes()
	{
		AttackDistribution ad = new AttackDistribution(new ArrayList<>(List.of(
			HitDistribution.single(1.0, new Hitsplat[]{new Hitsplat(5)}),
			HitDistribution.single(1.0, new Hitsplat[]{new Hitsplat(7)})
		)));
		assertThat(ad.getMax()).isEqualTo(12);
		assertThat(ad.getExpectedDamage()).isCloseTo(12.0, within(EPS));
	}

	@Test
	void asHistogramProducesContiguousBins()
	{
		// single dist, uniform 0..2 at 100% accuracy
		AttackDistribution ad = new AttackDistribution(new ArrayList<>(List.of(
			HitDistribution.linear(1.0, 0, 2)
		)));
		List<ChartEntry> hist = ad.asHistogram();
		// bins 0,1,2 each at ~1/3
		assertThat(hist).hasSize(3);
		assertThat(hist.get(0).name).isEqualTo("0");
		assertThat(hist.get(2).name).isEqualTo("2");
		double total = 0;
		for (ChartEntry e : hist)
		{
			total += e.value;
			assertThat(e.value).isCloseTo(1.0 / 3.0, within(EPS));
		}
		assertThat(total).isCloseTo(1.0, within(EPS));
	}

	@Test
	void getHashIsStableForSameDistAndDiffersForDifferent()
	{
		WeightedHit a1 = new WeightedHit(0.5, new Hitsplat[]{new Hitsplat(3, true), new Hitsplat(4, false)});
		WeightedHit a2 = new WeightedHit(0.9, new Hitsplat[]{new Hitsplat(3, true), new Hitsplat(4, false)});
		// same splats (damage + accurate) -> same hash regardless of probability
		assertThat(a1.getHash()).isEqualTo(a2.getHash());

		// accurate flag flips the hash
		WeightedHit b = new WeightedHit(0.5, new Hitsplat[]{new Hitsplat(3, false), new Hitsplat(4, false)});
		assertThat(a1.getHash()).isNotEqualTo(b.getHash());

		// different damage flips the hash
		WeightedHit c = new WeightedHit(0.5, new Hitsplat[]{new Hitsplat(3, true), new Hitsplat(5, false)});
		assertThat(a1.getHash()).isNotEqualTo(c.getHash());

		// verbatim accumulation: for [Hitsplat(3,true)] hash = (3 << 1) | 1 = 7
		WeightedHit single = new WeightedHit(1.0, new Hitsplat[]{new Hitsplat(3, true)});
		assertThat(single.getHash()).isEqualTo(BigInteger.valueOf(7));

		// flatten merges identical-hash branches: two accurate-3 hits at 0.25 each -> one at 0.5
		HitDistribution merged = new HitDistribution(new ArrayList<>(List.of(
			new WeightedHit(0.25, new Hitsplat[]{new Hitsplat(3, true)}),
			new WeightedHit(0.25, new Hitsplat[]{new Hitsplat(3, true)})
		))).flatten();
		assertThat(merged.size()).isEqualTo(1);
		assertThat(merged.hits.get(0).probability).isCloseTo(0.5, within(EPS));
	}
}
