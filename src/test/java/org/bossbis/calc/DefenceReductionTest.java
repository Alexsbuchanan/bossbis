package org.bossbis.calc;

import com.google.gson.Gson;
import org.bossbis.calc.data.MonsterRepository;
import org.bossbis.calc.scaling.DefenceReduction;
import org.bossbis.calc.types.DefenceReductions;
import org.bossbis.calc.types.Monster;
import org.bossbis.calc.types.MonsterInputs;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit gate for {@link DefenceReduction} (port of {@code src/lib/scaling/DefenceReduction.ts}).
 *
 * <p>Expected reduced-defence values are derived by running the verbatim upstream
 * {@code applyDefenceReductions} formula against the bundled Abyssal demon (id 415, def 135). The
 * truncating reduction order is load-bearing; these cases lock the single-reduction arithmetic.
 */
class DefenceReductionTest
{
	private static final MonsterRepository REPO = MonsterRepository.fromBundled(new Gson());

	private static Monster abyssalDemon(DefenceReductions reductions)
	{
		Monster base = REPO.resolve(415, "Standard");
		assertThat(base).as("Abyssal demon (Standard) must be in the bundled monsters.json").isNotNull();
		MonsterInputs in = MonsterInputs.initial();
		MonsterInputs inputs = new MonsterInputs(
			in.isFromCoxCm(), in.getToaInvocationLevel(), in.getToaPathLevel(),
			in.getPartyMaxCombatLevel(), in.getPartySumMiningLevel(), in.getPartyMaxHpLevel(),
			in.getPartySize(), in.getMonsterCurrentHp(), reductions,
			in.getDemonbaneVulnerability(), in.getPhase(), in.getPrayers());
		return new Monster(
			base.getId(), base.getName(), base.getImage(), base.getVersion(), base.getSize(),
			base.getSpeed(), base.getStyle(), base.getMaxHit(), base.getSkills(), base.getOffensive(),
			base.getDefensive(), base.getAttributes(), base.getWeakness(), base.getImmunities(),
			base.isSlayerMonster(), inputs);
	}

	private static DefenceReductions reductions(int dwh, int bgs)
	{
		return new DefenceReductions(false, false, 0, dwh, 0, 0, bgs, 0, 0, 0);
	}

	@Test
	void noReductionLeavesDefenceUnchanged()
	{
		Monster out = DefenceReduction.apply(abyssalDemon(reductions(0, 0)));
		assertThat(out.getSkills().getDef()).isEqualTo(135);
	}

	@Test
	void dwhReducesDefenceBy30PercentTruncated()
	{
		// 135 - trunc(135 * 3 / 10) = 135 - 40 = 95
		Monster out = DefenceReduction.apply(abyssalDemon(reductions(1, 0)));
		assertThat(out.getSkills().getDef()).isEqualTo(95);
	}

	@Test
	void stackedDwhAppliesMultiplicativelyInOrder()
	{
		// 135 -> 95 -> 67 -> 47
		Monster out = DefenceReduction.apply(abyssalDemon(reductions(3, 0)));
		assertThat(out.getSkills().getDef()).isEqualTo(47);
	}

	@Test
	void bgsDrainsDefenceFlat()
	{
		// 135 - 50 = 85
		Monster out = DefenceReduction.apply(abyssalDemon(reductions(0, 50)));
		assertThat(out.getSkills().getDef()).isEqualTo(85);
	}
}
