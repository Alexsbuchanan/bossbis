package org.bossbis.calc;

import org.bossbis.calc.scaling.ChambersOfXericScaling;
import org.bossbis.calc.scaling.DefenceReduction;
import org.bossbis.calc.scaling.Phases;
import org.bossbis.calc.scaling.TheatreOfBloodScaling;
import org.bossbis.calc.scaling.TombsOfAmascutScaling;
import org.bossbis.calc.scaling.VardorvisScaling;
import org.bossbis.calc.types.Monster;

/**
 * Port of {@code src/lib/MonsterScaling.ts} (weirdgloop@5e313c1) — the {@code scaleMonster} order of
 * operations that transforms the selected monster's stats for an encounter.
 *
 * <p>Upstream {@code ORDER_OF_OPERATIONS} (MonsterScaling.ts:10-17) is, in order:
 * <pre>
 *   applyCoxScaling, applyTobScaling, applyToaScaling, applyVardScaling,
 *   applyMonsterPhases, applyDefenceReductions
 * </pre>
 *
 * <p>All six transforms are ported. The ToA <em>defence</em> invocation factor is applied inline in
 * {@code PlayerVsNpcCalc.getNPCDefenceRoll} (not here), mirroring upstream.
 */
public final class MonsterScaling
{
	private MonsterScaling() {}

	/** Port of {@code scaleMonster} (MonsterScaling.ts:19-24) — applies the transforms in order. */
	public static Monster scaleMonster(Monster m)
	{
		m = ChambersOfXericScaling.apply(m);
		m = TheatreOfBloodScaling.apply(m);
		m = TombsOfAmascutScaling.apply(m);
		m = VardorvisScaling.apply(m);
		m = Phases.apply(m);
		m = DefenceReduction.apply(m);
		return m;
	}

	/**
	 * Port of {@code scaleMonsterHpOnly} (MonsterScaling.ts:27-33) — a shortcut that re-scales only the
	 * monster's current-HP-dependent stats (Vardorvis str/def) without re-running the full chain.
	 */
	public static Monster scaleMonsterHpOnly(Monster m)
	{
		if ("Vardorvis".equals(m.getName()))
		{
			return VardorvisScaling.apply(m);
		}
		return m;
	}
}
