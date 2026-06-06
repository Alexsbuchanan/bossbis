package org.bossbis.calc;

import lombok.Value;
import lombok.experimental.Accessors;

/** Port of src/lib/Math.ts. All integer ops truncate toward zero (JS Math.trunc), never floor. */
public final strictfp class CalcMath
{
	private CalcMath() {}

	/** [factor, divisor]; prayer/effect multipliers are denominator-of-100 so they combine additively. */
	@Value
	@Accessors(fluent = true)
	public static class Factor { int numerator; int divisor; }

	/** [min, max] inclusive hit bounds. */
	@Value
	@Accessors(fluent = true)
	public static class MinMax { int min; int max; }

	public static int iSqrt(int x)
	{
		return (int) Math.sqrt(x);   // sqrt is non-negative; (int) == trunc
	}

	public static int addPercent(int x, int pct)
	{
		return x + (int) ((long) x * pct / 100);
	}

	public static int iLerp(int x1, int x2, int y1, int y2, int yc)
	{
		return x1 + (int) ((long) (x2 - x1) * (yc - y1) / (y2 - y1));
	}

	public static int lerp(int curr, int srcStart, int srcEnd, int dstStart, int dstEnd)
	{
		long srcRange = srcEnd - srcStart;
		long dstRange = dstEnd - dstStart;
		long currNorm = curr - srcStart;
		return (int) (currNorm * dstRange / srcRange + dstStart);
	}
}
