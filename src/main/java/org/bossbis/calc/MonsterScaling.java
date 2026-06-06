package org.bossbis.calc;

import org.bossbis.calc.scaling.DefenceReduction;
import org.bossbis.calc.types.Monster;

/**
 * Port of {@code src/lib/MonsterScaling.ts} (weirdgloop@5e313c1) — the {@code scaleMonster} order of
 * operations that transforms the selected monster's stats for an encounter.
 *
 * <p>Upstream {@code ORDER_OF_OPERATIONS} (MonsterScaling.ts:10-17) is:
 * <pre>
 *   applyCoxScaling, applyTobScaling, applyToaScaling, applyVardScaling,
 *   applyMonsterPhases, applyDefenceReductions
 * </pre>
 *
 * <h2>v0.1.1 scope</h2>
 * Only the <b>defence-reduction</b> transform ({@link DefenceReduction#apply(Monster)}) is ported (the
 * v0.1.1 accuracy criterion). The raid party/HP/level stat-scaling transforms (CoX/ToB/ToA invocation
 * stat-scaling, Vardorvis HP-scaling, monster phases) are deferred to the phase that ships raid
 * encounters — see the {@code TODO(later)} markers below. The ToA <em>defence</em> invocation factor
 * is applied inline in {@code PlayerVsNpcCalc.getNPCDefenceRoll} (not here), so the ported def-roll
 * path is already faithful without the ToA stat-scaling transform.
 */
public final class MonsterScaling
{
	private MonsterScaling() {}

	/**
	 * Port of {@code scaleMonster} (MonsterScaling.ts:19-24) — applies the transforms in order.
	 *
	 * <p>v0.1.1 ports only the {@code applyDefenceReductions} step; the earlier raid stat-scaling
	 * transforms are deferred (see class doc).
	 */
	public static Monster scaleMonster(Monster m)
	{
		// TODO(later): raid stat-scaling — applyCoxScaling (ChambersOfXeric.ts)
		// TODO(later): raid stat-scaling — applyTobScaling (TheatreOfBlood.ts)
		// TODO(later): raid stat-scaling — applyToaScaling (TombsOfAmascut.ts);
		//   NB the ToA *defence* invocation factor is applied inline in getNPCDefenceRoll, not here.
		// TODO(later): raid stat-scaling — applyVardScaling (Vardorvis.ts)
		// TODO(later): raid stat-scaling — applyMonsterPhases (Phases.ts)
		m = DefenceReduction.apply(m);
		return m;
	}

	/**
	 * Port of {@code scaleMonsterHpOnly} (MonsterScaling.ts:27-33) — Vardorvis HP-scaling shortcut.
	 *
	 * <p>v0.1.1 defers Vardorvis HP-scaling; this returns the monster unchanged for now.
	 */
	public static Monster scaleMonsterHpOnly(Monster m)
	{
		// TODO(later): raid stat-scaling — Vardorvis HP-only scaling (applyVardScaling).
		return m;
	}
}
