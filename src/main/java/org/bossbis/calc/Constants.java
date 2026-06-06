package org.bossbis.calc;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Port of src/lib/constants.ts. Scalar constants plus the ID/stance membership sets needed by the
 * ported calc paths (the bulk of the ID arrays are still ported lazily alongside their consumers).
 */
public final class Constants
{
	private Constants() {}

	public static final double SECONDS_PER_TICK = 0.6;
	public static final int DEFAULT_ATTACK_SPEED = 4;
	public static final double TTK_DIST_EPSILON = 0.0001;
	public static final int TTK_DIST_MAX_ITER_ROUNDS = 1000;

	/** Port of BLOWPIPE_IDS (constants.ts:3-10) — toxic blowpipe + leagues variants (charged ids). */
	public static final Set<Integer> BLOWPIPE_IDS = setOf(
		12926, // regular
		28688, // blazing
		31575, // camphor
		31579, // ironwood
		31583, // rosewood
		30374  // drygore
	);

	/** Port of AUTOCAST_STANCES (constants.ts:442). */
	public static final List<String> AUTOCAST_STANCES = Collections.unmodifiableList(
		Arrays.asList("Autocast", "Defensive Autocast"));

	/** Port of CAST_STANCES (constants.ts:443) — autocast stances plus the pseudo "Manual Cast". */
	public static final List<String> CAST_STANCES = Collections.unmodifiableList(
		Stream.concat(AUTOCAST_STANCES.stream(), Stream.of("Manual Cast"))
			.collect(Collectors.toList()));

	// --- Tombs of Amascut monster id constituents (constants.ts:15-82) -------------------------

	private static final List<Integer> AKKHA_IDS = idList(11789, 11790, 11791, 11792, 11793, 11794, 11795, 11796);
	private static final List<Integer> AKKHA_SHADOW_IDS = idList(11797, 11798, 11799);
	private static final List<Integer> BABA_IDS = idList(11778, 11779, 11780);
	private static final List<Integer> KEPHRI_SHIELDED_IDS = idList(11719);
	private static final List<Integer> KEPHRI_UNSHIELDED_IDS = idList(11721);
	private static final List<Integer> KEPHRI_OVERLORD_IDS = idList(11724, 11725, 11726);
	private static final List<Integer> ZEBAK_IDS = idList(11730, 11732, 11733);
	private static final List<Integer> TOA_OBELISK_IDS = idList(11751, 11750, 11752);
	private static final List<Integer> P2_WARDEN_IDS = idList(11753, 11754, 11756, 11757);
	private static final List<Integer> TOA_WARDEN_CORE_EJECTED_IDS = idList(11755, 11758);
	private static final List<Integer> P3_WARDEN_IDS = idList(11761, 11763, 11762, 11764);

	/** Port of TOMBS_OF_AMASCUT_PATH_MONSTER_IDS (constants.ts:65-73). */
	public static final Set<Integer> TOMBS_OF_AMASCUT_PATH_MONSTER_IDS = setOf(
		AKKHA_IDS, AKKHA_SHADOW_IDS, BABA_IDS, KEPHRI_SHIELDED_IDS, KEPHRI_UNSHIELDED_IDS,
		KEPHRI_OVERLORD_IDS, ZEBAK_IDS);

	/** Port of TOMBS_OF_AMASCUT_MONSTER_IDS (constants.ts:76-82). */
	public static final Set<Integer> TOMBS_OF_AMASCUT_MONSTER_IDS = setOf(
		TOMBS_OF_AMASCUT_PATH_MONSTER_IDS, TOA_OBELISK_IDS, P2_WARDEN_IDS, TOA_WARDEN_CORE_EJECTED_IDS,
		P3_WARDEN_IDS);

	/** Port of P2_WARDEN_IDS (constants.ts) — used by the ranged attack roll (tbow double-scaling). */
	public static final Set<Integer> P2_WARDEN_IDS_SET = setOf(P2_WARDEN_IDS);

	/** Port of KEPHRI_OVERLORD_IDS (constants.ts) — used by getNPCDefenceRoll's ToA invocation guard. */
	public static final Set<Integer> KEPHRI_OVERLORD_IDS_SET = setOf(KEPHRI_OVERLORD_IDS);

	// --- Royal Titans boss ids (constants.ts:422-425) — used by the ranged attack roll ----------------

	/** Port of TITAN_BOSS_IDS (constants.ts:422-425). */
	public static final Set<Integer> TITAN_BOSS_IDS = setOf(
		12596,  // Fire elemental (Royal Titans)
		14147   // Ice elemental (Royal Titans)
	);

	// --- Yama ids (constants.ts:436,539) — used by BaseCalc.demonbaneVulnerability -----------------

	/** Port of YAMA_IDS (constants.ts:539). */
	public static final Set<Integer> YAMA_IDS = setOf(14176);

	/** Port of YAMA_VOID_FLARE_IDS (constants.ts:436). */
	public static final Set<Integer> YAMA_VOID_FLARE_IDS = setOf(14179);

	// --- Burn-immune NPC ids (constants.ts:373-378) — used by BaseCalc.isImmuneToStrongBurns -------

	private static final List<Integer> TEKTON_IDS = idList(7540, 7543, 7544, 7545);
	private static final List<Integer> GLOWING_CRYSTAL_IDS = idList(7568);
	private static final List<Integer> DUSK_IDS = idList(
		7851, 7854, 7855, 7882, 7883, 7886,   // dusk first form
		7887, 7888, 7889);                     // dusk second form
	private static final List<Integer> WARRIORS_GUILD_CYCLOPES = idList(
		2463, 2465, 2467,                      // L56
		2464, 2466, 2468,                      // L76
		2137, 2138, 2139, 2140, 2141, 2142);   // L106

	/** Port of IMMUNE_TO_BURN_DAMAGE_NPC_IDS (constants.ts:373-378). */
	public static final Set<Integer> IMMUNE_TO_BURN_DAMAGE_NPC_IDS = setOf(
		TEKTON_IDS, GLOWING_CRYSTAL_IDS, DUSK_IDS, WARRIORS_GUILD_CYCLOPES);

	private static List<Integer> idList(int... ids)
	{
		List<Integer> out = new java.util.ArrayList<>(ids.length);
		for (int id : ids)
		{
			out.add(id);
		}
		return Collections.unmodifiableList(out);
	}

	private static Set<Integer> setOf(int... ids)
	{
		Set<Integer> out = new HashSet<>();
		for (int id : ids)
		{
			out.add(id);
		}
		return Collections.unmodifiableSet(out);
	}

	@SafeVarargs
	private static Set<Integer> setOf(Iterable<Integer>... groups)
	{
		Set<Integer> out = new HashSet<>();
		for (Iterable<Integer> g : groups)
		{
			for (Integer id : g)
			{
				out.add(id);
			}
		}
		return Collections.unmodifiableSet(out);
	}
}
