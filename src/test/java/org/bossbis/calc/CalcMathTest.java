package org.bossbis.calc;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CalcMathTest
{
	@Test
	void iSqrtTruncatesTowardZero()
	{
		assertThat(CalcMath.iSqrt(2000)).isEqualTo(44);   // floor(sqrt(2000)) = 44
		assertThat(CalcMath.iSqrt(0)).isEqualTo(0);
		assertThat(CalcMath.iSqrt(99 * 99)).isEqualTo(99);
	}

	@Test
	void addPercentTruncates()
	{
		assertThat(CalcMath.addPercent(99, 15)).isEqualTo(99 + 14);   // trunc(99*15/100)=trunc(14.85)=14
		assertThat(CalcMath.addPercent(100, 10)).isEqualTo(110);
	}

	@Test
	void iLerpMatchesUpstream()
	{
		// iLerp(x1,x2,y1,y2,yc) = x1 + trunc((x2-x1)*(yc-y1)/(y2-y1))
		assertThat(CalcMath.iLerp(0, 100, 0, 10, 5)).isEqualTo(50);
		assertThat(CalcMath.iLerp(10, 20, 0, 4, 1)).isEqualTo(12);   // 10 + trunc(10*1/4)=10+2
	}

	@Test
	void factorAndMinMaxHoldPairs()
	{
		CalcMath.Factor f = new CalcMath.Factor(123, 100);
		assertThat(f.numerator()).isEqualTo(123);
		assertThat(f.divisor()).isEqualTo(100);
		CalcMath.MinMax mm = new CalcMath.MinMax(0, 52);
		assertThat(mm.min()).isEqualTo(0);
		assertThat(mm.max()).isEqualTo(52);
	}
}
