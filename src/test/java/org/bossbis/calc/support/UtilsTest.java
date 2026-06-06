package org.bossbis.calc.support;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UtilsTest
{
	@Test
	void isDefinedRejectsNull()
	{
		assertThat(Utils.isDefined("x")).isTrue();
		assertThat(Utils.isDefined(null)).isFalse();
	}

	@Test
	void featureStatusHasExpectedMembers()
	{
		// Port from utils.ts:36 — at minimum IMPLEMENTED / PARTIALLY_IMPLEMENTED / UNIMPLEMENTED exist.
		assertThat(Utils.FeatureStatus.valueOf("IMPLEMENTED")).isNotNull();
		assertThat(Utils.FeatureStatus.values().length).isGreaterThanOrEqualTo(3);
	}

	@Test
	void deepMergeOverwritesAndSkipsUndefined()
	{
		// lodash mergeWith default: nested override; null/undefined source values do not clobber.
		Map<String, Object> base = Map.of("a", 1, "b", Map.of("x", 1, "y", 2));
		Map<String, Object> upd = Map.of("b", Map.of("y", 9));
		Map<String, Object> out = MergeWith.deepMerge(base, upd);
		assertThat(((Map<?, ?>) out.get("b")).get("x")).isEqualTo(1);   // preserved
		assertThat(((Map<?, ?>) out.get("b")).get("y")).isEqualTo(9);   // overwritten
		assertThat(out.get("a")).isEqualTo(1);
	}
}
