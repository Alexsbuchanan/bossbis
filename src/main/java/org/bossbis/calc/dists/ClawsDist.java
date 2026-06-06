package org.bossbis.calc.dists;

import java.util.ArrayList;
import java.util.List;
import org.bossbis.calc.HitDist.AttackDistribution;
import org.bossbis.calc.HitDist.HitDistribution;
import org.bossbis.calc.HitDist.Hitsplat;
import org.bossbis.calc.HitDist.WeightedHit;

/**
 * Port of src/lib/dists/claws.ts — Dragon claws ({@link #dClawDist}) and Burning claws
 * ({@link #burningClawSpec} + {@link #burningClawDoT}) special-attack distributions.
 *
 * <p>{@code Math.trunc} (TS) is ported as a truncating {@code (int)}/{@code (long)} cast; integer
 * powers {@code (1 - acc) ** n} are repeated multiplication for determinism.
 */
public final strictfp class ClawsDist
{
	private ClawsDist() {}

	/** {@code [chance, low, high]} returned by {@code generateTotals}. */
	private static final class Totals
	{
		final double chance;
		final int low;
		final int high;

		Totals(double chance, int low, int high)
		{
			this.chance = chance;
			this.low = low;
			this.high = high;
		}
	}

	/** Integer power as repeated multiplication: {@code base ** exp}. */
	private static double pow(double base, int exp)
	{
		double acc = 1.0;
		for (int i = 0; i < exp; i++)
		{
			acc *= base;
		}
		return acc;
	}

	private static Totals generateTotals(int accRoll, int totalRolls, double acc, int max, int highOffset)
	{
		int low = (int) ((long) max * (totalRolls - accRoll) / 4);
		int high = max + low + highOffset;
		double chancePreviousRollsFail = pow(1 - acc, accRoll);
		double chanceThisRollPasses = chancePreviousRollsFail * acc;
		double chancePerDmg = chanceThisRollPasses / (high - low + 1);

		return new Totals(chancePerDmg, low, high);
	}

	public static AttackDistribution dClawDist(double acc, int max)
	{
		HitDistribution dist = new HitDistribution(new ArrayList<>());
		for (int accRoll = 0; accRoll < 4; accRoll++)
		{
			Totals t = generateTotals(accRoll, 4, acc, max, -1);
			double chancePerDmg = t.chance;
			for (int dmg = t.low; dmg <= t.high; dmg++)
			{
				switch (accRoll)
				{
					case 0:
						dist.addHit(new WeightedHit(chancePerDmg, new Hitsplat[]{
							new Hitsplat(dmg / 2),
							new Hitsplat(dmg / 4),
							new Hitsplat(dmg / 8),
							new Hitsplat(dmg / 8 + 1),
						}));
						break;

					case 1:
						dist.addHit(new WeightedHit(chancePerDmg, new Hitsplat[]{
							new Hitsplat(dmg / 2),
							new Hitsplat(dmg / 4),
							new Hitsplat(dmg / 4 + 1),
							Hitsplat.INACCURATE,
						}));
						break;

					case 2:
						dist.addHit(new WeightedHit(chancePerDmg, new Hitsplat[]{
							new Hitsplat(dmg / 2),
							new Hitsplat(dmg / 2 + 1),
							Hitsplat.INACCURATE,
							Hitsplat.INACCURATE,
						}));
						break;

					default:
						dist.addHit(new WeightedHit(chancePerDmg, new Hitsplat[]{
							new Hitsplat(dmg + 1),
							Hitsplat.INACCURATE,
							Hitsplat.INACCURATE,
							Hitsplat.INACCURATE,
						}));
						break;
				}
			}
		}

		double chanceAllFail = pow(1 - acc, 4);
		dist.addHit(new WeightedHit(chanceAllFail * 2 / 3, new Hitsplat[]{
			new Hitsplat(1, false),
			new Hitsplat(1, false),
			Hitsplat.INACCURATE,
			Hitsplat.INACCURATE,
		}));
		dist.addHit(new WeightedHit(chanceAllFail / 3, new Hitsplat[]{
			Hitsplat.INACCURATE,
			Hitsplat.INACCURATE,
			Hitsplat.INACCURATE,
			Hitsplat.INACCURATE,
		}));

		List<HitDistribution> dists = new ArrayList<>();
		dists.add(dist);
		return new AttackDistribution(dists);
	}

	public static AttackDistribution burningClawSpec(double acc, int max)
	{
		HitDistribution dist = new HitDistribution(new ArrayList<>());
		for (int accRoll = 0; accRoll < 3; accRoll++)
		{
			Totals t = generateTotals(accRoll, 3, acc, max, 0);
			double chancePerDmg = t.chance;
			for (int dmg = t.low; dmg <= t.high; dmg++)
			{
				switch (accRoll)
				{
					case 0:
						dist.addHit(new WeightedHit(chancePerDmg, new Hitsplat[]{
							new Hitsplat(dmg / 2),
							new Hitsplat(dmg / 4),
							new Hitsplat(dmg / 4),
						}));
						break;

					case 1:
						dist.addHit(new WeightedHit(chancePerDmg, new Hitsplat[]{
							new Hitsplat(dmg / 2 - 1),
							new Hitsplat(dmg / 2 - 1),
							new Hitsplat(2),
						}));
						break;

					default:
						dist.addHit(new WeightedHit(chancePerDmg, new Hitsplat[]{
							new Hitsplat(dmg - 2),
							new Hitsplat(1),
							new Hitsplat(1),
						}));
						break;
				}
			}
		}

		double chanceAllFail = pow(1 - acc, 3);
		dist.addHit(new WeightedHit(chanceAllFail / 5, new Hitsplat[]{
			Hitsplat.INACCURATE,
			Hitsplat.INACCURATE,
			Hitsplat.INACCURATE,
		}));
		dist.addHit(new WeightedHit(2 * chanceAllFail / 5, new Hitsplat[]{
			new Hitsplat(1, false),
			Hitsplat.INACCURATE,
			Hitsplat.INACCURATE,
		}));
		dist.addHit(new WeightedHit(2 * chanceAllFail / 5, new Hitsplat[]{
			new Hitsplat(1, false),
			new Hitsplat(1, false),
			Hitsplat.INACCURATE,
		}));

		List<HitDistribution> dists = new ArrayList<>();
		dists.add(dist);
		return new AttackDistribution(dists);
	}

	// truth table ; 1 = burn, 0 = no burn
	public static final int[][] BURN_MATRIX = {
		{0, 0, 0},
		{0, 0, 1},
		{0, 1, 0},
		{0, 1, 1},
		{1, 0, 0},
		{1, 0, 1},
		{1, 1, 0},
		{1, 1, 1},
	};

	public static final double[] BURN_EXPECTED = computeBurnExpected();

	private static double[] computeBurnExpected()
	{
		double[] out = new double[3];
		for (int accRoll = 0; accRoll <= 2; accRoll++)
		{
			double sum = 0.0;
			for (int[] row : BURN_MATRIX)
			{
				double burnChance = 0.15 * (accRoll + 1);
				double burn1 = (row[0] == 1 ? burnChance : (1 - burnChance));
				double burn2 = (row[1] == 1 ? burnChance : (1 - burnChance));
				double burn3 = (row[2] == 1 ? burnChance : (1 - burnChance));
				double chanceOfRow = burn1 * burn2 * burn3;

				double damage = row[0] * 10 + row[1] * 10 + row[2] * 10;
				if (row[0] == 1 && row[1] == 1)
				{
					// there's a (presumed) bug here, where if the first two hitsplats apply burn,
					// then they overlap and miss 1 damage on the first tick.
					// https://discord.com/channels/177206626514632704/1098698914498101368/1285181996896620566
					damage -= 1;
				}

				double contribution = chanceOfRow * damage;
				if (contribution == contribution && contribution != 0.0)
				{
					sum += contribution;
				}
			}
			out[accRoll] = sum;
		}
		return out;
	}

	public static double burningClawDoT(double acc)
	{
		// 10 damage burn x3 hitsplats, 15/30/45% chance per splat dependent on which roll hits
		double accumulator = 0;

		for (int accRoll = 0; accRoll < 3; accRoll++)
		{
			double prevRollsFail = pow(1 - acc, accRoll);
			double thisRollHits = prevRollsFail * acc;

			accumulator += thisRollHits * BURN_EXPECTED[accRoll];
		}
		return accumulator;
	}
}
