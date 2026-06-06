package org.bossbis.calc;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ConstantsTest
{
	@Test
	void epsilonsMatchUpstream()
	{
		assertThat(Constants.TTK_DIST_EPSILON).isEqualTo(0.0001);
		assertThat(Constants.TTK_DIST_MAX_ITER_ROUNDS).isEqualTo(1000);
		assertThat(Constants.SECONDS_PER_TICK).isEqualTo(0.6);
		assertThat(Constants.DEFAULT_ATTACK_SPEED).isEqualTo(4);
	}

}
