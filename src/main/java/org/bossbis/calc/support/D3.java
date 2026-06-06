package org.bossbis.calc.support;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/** Port of the d3-array functions used by src/lib. Summation order is preserved exactly. */
public final strictfp class D3
{
	private D3() {}

	public static double sum(Iterable<Double> values)
	{
		return sum(values, v -> v);
	}

	public static <T> double sum(Iterable<T> values, ToDoubleFunction<T> accessor)
	{
		double acc = 0.0;
		for (T v : values)
		{
			double d = accessor.applyAsDouble(v);
			if (d == d && d != 0.0)   // skip NaN (d!=d) and 0; left-to-right
			{
				acc += d;
			}
		}
		return acc;
	}

	public static OptionalDouble max(Iterable<Double> values)
	{
		OptionalDouble m = OptionalDouble.empty();
		for (Double v : values)
		{
			if (v != null && !v.isNaN())
			{
				m = (!m.isPresent() || v > m.getAsDouble()) ? OptionalDouble.of(v) : m;
			}
		}
		return m;
	}

	public static OptionalDouble min(Iterable<Double> values)
	{
		OptionalDouble m = OptionalDouble.empty();
		for (Double v : values)
		{
			if (v != null && !v.isNaN())
			{
				m = (!m.isPresent() || v < m.getAsDouble()) ? OptionalDouble.of(v) : m;
			}
		}
		return m;
	}

	public static <T> boolean some(Iterable<T> values, Predicate<T> test)
	{
		for (T v : values)
		{
			if (test.test(v))
			{
				return true;
			}
		}
		return false;
	}

	public static List<Integer> range(int start, int stop)
	{
		List<Integer> out = new ArrayList<>(Math.max(0, stop - start));
		for (int i = start; i < stop; i++)
		{
			out.add(i);
		}
		return out;
	}

	public static <A, B> List<int[]> cross(List<A> a, List<B> b)
	{
		// Only the int-pair form is used in src/lib (distribution zip); keep it minimal + a-major.
		List<int[]> out = new ArrayList<>(a.size() * b.size());
		for (A x : a)
		{
			for (B y : b)
			{
				out.add(new int[]{((Number) x).intValue(), ((Number) y).intValue()});
			}
		}
		return out;
	}
}
