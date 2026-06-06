package org.bossbis.calc.data;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class MonsterSpellRepositoryTest
{
	@Test
	void resolvesAKnownMonster()
	{
		MonsterRepository repo = MonsterRepository.fromBundled(new Gson());
		assertThat(repo.resolve(5862, "").getName()).isEqualTo("Cerberus");
	}

	@Test
	void resolvesAKnownSpell()
	{
		SpellRepository repo = SpellRepository.fromBundled(new Gson());
		assertThat(repo.byName("Fire Bolt")).isNotNull();
	}

	/**
	 * Robustness gate: the entire monsters.json must deserialize without throwing, indexing
	 * &gt; 2000 monsters. This is the real check that every monster row's fields map correctly
	 * through Gson (e.g. the {@code style[]}/{@code max_hit}-as-String quirks) across the whole file.
	 */
	@Test
	void loadsEveryMonsterWithoutThrowingAndIndexesOverTwoThousand()
	{
		assertThatCode(() -> {
			MonsterRepository r = MonsterRepository.fromBundled(new Gson());
			assertThat(r.size()).isGreaterThan(2000);
		}).doesNotThrowAnyException();
	}
}
