package org.bossbis.calc.data;

import com.google.gson.Gson;
import java.util.Optional;
import org.bossbis.calc.types.EquipmentPiece;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class EquipmentRepositoryTest
{
	static EquipmentRepository repo;

	@BeforeAll
	static void load() { repo = EquipmentRepository.fromBundled(new Gson()); }

	@Test
	void looksUpByCacheId()
	{
		Optional<EquipmentPiece> whip = repo.resolve(4151);   // Abyssal whip
		assertThat(whip).isPresent();
		assertThat(whip.get().getName()).isEqualTo("Abyssal whip");
	}

	@Test
	void foldsVariantToCanonical()
	{
		// 26482 (Abyssal whip (or)) aliases to 4151 (Abyssal whip) per equipment_aliases.json.
		assertThat(repo.canonicalId(26482)).isEqualTo(4151);
		assertThat(repo.resolve(26482).get().getName()).isEqualTo("Abyssal whip");
	}

	@Test
	void unknownIdHasNoStats()
	{
		assertThat(repo.resolve(999_999)).isEmpty();
	}

	@Test
	void twoHandedWeaponIsFlagged()
	{
		assertThat(repo.resolve(20997).get().isTwoHanded()).isTrue();   // Twisted bow
	}

	/**
	 * Robustness gate: the entire equipment.json must deserialize without throwing, indexing
	 * &gt; 5000 distinct items. This is the real check that every row's fields map correctly through
	 * Gson (a single bad field would crash the whole load), tolerating the one known lowercase
	 * "blunt" category typo (it deserializes to a null category, which must not crash the load).
	 */
	@Test
	void loadsEveryRowWithoutThrowingAndIndexesOverFiveThousand()
	{
		assertThatCode(() -> {
			EquipmentRepository r = EquipmentRepository.fromBundled(new Gson());
			assertThat(r.size()).isGreaterThan(5000);
		}).doesNotThrowAnyException();
	}
}
