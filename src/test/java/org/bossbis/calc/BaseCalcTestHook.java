package org.bossbis.calc;

/**
 * Tiny test hook that exposes {@link BaseCalc}'s protected, instance-level {@code trackFactor} as a
 * static for {@code BaseCalcTest}, proving the long intermediate without needing a constructed calc.
 *
 * <p>It re-implements the exact arithmetic of {@link BaseCalc#trackFactor} ({@code (int)((long) base *
 * n / d)}); the contract is locked by {@link BaseCalcTest#trackFactorUsesLongIntermediate()} and the
 * shared {@link BaseCalc.TrackHelpers#factor(int, int, int)} primitive both helpers delegate to.
 */
final class BaseCalcTestHook
{
	private BaseCalcTestHook() {}

	static int trackFactor(int base, int n, int d)
	{
		return BaseCalc.TrackHelpers.factor(base, n, d);
	}
}
