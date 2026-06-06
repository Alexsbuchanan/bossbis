package org.bossbis.calc.support;

import java.util.List;
import java.util.OptionalDouble;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class D3ParityTest
{
	@Test
	void sumIsLeftToRightAndSkipsNonFinite()
	{
		assertThat(D3.sum(List.of())).isEqualTo(0.0);                       // empty -> 0
		assertThat(D3.sum(List.of(1.0, 2.0, 3.0))).isEqualTo(6.0);
		assertThat(D3.sum(List.of(1.0, Double.NaN, 2.0))).isEqualTo(3.0);   // NaN skipped
	}

	@Test
	void maxAndMinReturnEmptyForNoValues()
	{
		assertThat(D3.max(List.of())).isEqualTo(OptionalDouble.empty());
		assertThat(D3.max(List.of(3.0, 9.0, 4.0))).isEqualTo(OptionalDouble.of(9.0));
		assertThat(D3.min(List.of(3.0, 9.0, 4.0))).isEqualTo(OptionalDouble.of(3.0));
	}

	@Test
	void someShortCircuits()
	{
		assertThat(D3.some(List.of(1, 2, 3), x -> x > 2)).isTrue();
		assertThat(D3.some(List.of(1, 2, 3), x -> x > 9)).isFalse();
	}

	@Test
	void rangeIsHalfOpen()
	{
		assertThat(D3.range(0, 4)).containsExactly(0, 1, 2, 3);
		assertThat(D3.range(2, 2)).isEmpty();
	}

	@Test
	void crossIsAMajorCartesianProduct()
	{
		List<int[]> p = D3.cross(List.of(0, 1), List.of(10, 20));
		assertThat(p).hasSize(4);
		assertThat(p.get(0)).containsExactly(0, 10);
		assertThat(p.get(1)).containsExactly(0, 20);
		assertThat(p.get(2)).containsExactly(1, 10);
		assertThat(p.get(3)).containsExactly(1, 20);
	}
}
