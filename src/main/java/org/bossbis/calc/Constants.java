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

	// --- USES_DEFENCE_LEVEL_FOR_MAGIC_DEFENCE_NPC_IDS constituents (constants.ts:256-326) ----------

	private static final List<Integer> ICE_DEMON_IDS = idList(7584, 7585);
	private static final List<Integer> VERZIK_P1_IDS = idList(
		10830, 10831, 10832,   // em
		8369, 8370, 8371,      // norm
		10847, 10848, 10849);  // hmt
	private static final List<Integer> FRAGMENT_OF_SEREN_IDS = idList(8917, 8918, 8919, 8920);

	/** Port of VERZIK_IDS (constants.ts:90-95) — also a defence-floor set. */
	public static final Set<Integer> VERZIK_IDS = setOf(
		VERZIK_P1_IDS,
		idList(10833, 10834, 10835),   // verzik entry mode
		idList(8372, 8373, 8374),      // verzik normal mode
		idList(10850, 10851, 10852));  // verzik hard mode

	/**
	 * Port of USES_DEFENCE_LEVEL_FOR_MAGIC_DEFENCE_NPC_IDS (constants.ts:319-326) — NPCs whose magic
	 * defence is rolled against the defence stat (not the magic stat). Used by getNPCDefenceRoll.
	 */
	public static final Set<Integer> USES_DEFENCE_LEVEL_FOR_MAGIC_DEFENCE_NPC_IDS = setOf(
		ICE_DEMON_IDS,
		new java.util.ArrayList<>(VERZIK_IDS),
		FRAGMENT_OF_SEREN_IDS,
		idList(11709, 11712),   // baboon brawler
		idList(9118));          // rabbit (prifddinas)

	// --- Defence-floor id sets (DefenceReduction.getDefenceFloor; constants.ts) --------------------

	/** Port of VARDORVIS_IDS (constants.ts:493). */
	public static final Set<Integer> VARDORVIS_IDS = setOf(12223, 12224, 12228, 12425, 12426, 13656);

	/** Port of SOTETSEG_IDS (constants.ts). */
	public static final Set<Integer> SOTETSEG_IDS = setOf(8387, 8388, 10867, 10868);

	/** Port of NIGHTMARE_IDS (constants.ts) — Nightmare + Phosani's. */
	public static final Set<Integer> NIGHTMARE_IDS = setOf(
		378, 9425, 9426, 9427, 9428, 9429, 9430, 9431, 9432, 9433, 9460,            // nightmare
		377, 9423, 9416, 9417, 9418, 9419, 9420, 9421, 9422, 9424, 11153, 11154, 11155); // phosani's

	/** Port of NEX_IDS (constants.ts). */
	public static final Set<Integer> NEX_IDS = setOf(11278, 11279, 11280, 11281, 11282);

	/** Port of ARAXXOR_IDS (constants.ts). */
	public static final Set<Integer> ARAXXOR_IDS = setOf(13668);

	/** Port of HUEYCOATL_IDS (constants.ts:516-522) — head + body + tail. */
	public static final Set<Integer> HUEYCOATL_IDS = setOf(
		idList(14009, 14010, 14013),  // head
		idList(14017),                // body
		idList(14014));               // tail

	/** Defence-floor id sets reusing the ToA constituents above. */
	public static final Set<Integer> AKKHA_IDS_SET = setOf(AKKHA_IDS);
	public static final Set<Integer> BABA_IDS_SET = setOf(BABA_IDS);
	public static final Set<Integer> KEPHRI_SHIELDED_IDS_SET = setOf(KEPHRI_SHIELDED_IDS);
	public static final Set<Integer> KEPHRI_UNSHIELDED_IDS_SET = setOf(KEPHRI_UNSHIELDED_IDS);
	public static final Set<Integer> ZEBAK_IDS_SET = setOf(ZEBAK_IDS);
	public static final Set<Integer> TOA_OBELISK_IDS_SET = setOf(TOA_OBELISK_IDS);
	public static final Set<Integer> P3_WARDEN_IDS_SET = setOf(P3_WARDEN_IDS);

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

	// --- Hit-chance guard id sets (constants.ts) — used by getHitChance ----------------------------

	/** Port of VERZIK_P1_IDS (constants.ts:84-89) — used by getHitChance (Dawnbringer guaranteed accuracy). */
	public static final Set<Integer> VERZIK_P1_IDS_SET = setOf(VERZIK_P1_IDS);

	/** Port of TITAN_ELEMENTAL_IDS (constants.ts:427-430) — Royal Titans elementals (magic accuracy override). */
	public static final Set<Integer> TITAN_ELEMENTAL_IDS = setOf(
		14150,  // Fire elemental (Royal Titans)
		14151   // Ice elemental (Royal Titans)
	);

	/** Port of ECLIPSE_MOON_IDS (constants.ts:547). */
	public static final Set<Integer> ECLIPSE_MOON_IDS = setOf(13012);

	/** Port of INFINITE_HEALTH_MONSTERS (constants.ts:543-545) — monsters with infinite HP (ruby-bolt cap). */
	public static final Set<Integer> INFINITE_HEALTH_MONSTERS = setOf(
		14779 // gemstone crab
	);

	/** Port of DOOM_OF_MOKHAIOTL_IDS (constants.ts:531). */
	public static final Set<Integer> DOOM_OF_MOKHAIOTL_IDS = setOf(14707);

	/**
	 * Port of GUARANTEED_ACCURACY_MONSTERS (constants.ts:482-484) — NPCs the player has 100% accuracy
	 * against. Used by getHitChance.
	 */
	public static final Set<Integer> GUARANTEED_ACCURACY_MONSTERS = setOf(
		5916  // Spawn (abyssal sire)
	);

	/**
	 * Port of ALWAYS_MAX_HIT_MONSTERS (constants.ts:457-477) — NPCs the player always max-hits with the
	 * matching combat style (100% accuracy via that style). Used by getHitChance.
	 */
	public static final Set<Integer> ALWAYS_MAX_HIT_MONSTERS_MELEE = setOf(
		idList(11710, 11713),   // baboon thrower
		idList(12814),          // frem warband archer
		TOA_WARDEN_CORE_EJECTED_IDS,
		new java.util.ArrayList<>(YAMA_VOID_FLARE_IDS));

	public static final Set<Integer> ALWAYS_MAX_HIT_MONSTERS_RANGED = setOf(
		idList(11711, 11714),   // baboon mage
		idList(12815),          // frem warband seer
		idList(11717),          // cursed baboon
		idList(11715),          // baboon shaman
		new java.util.ArrayList<>(YAMA_VOID_FLARE_IDS));

	public static final Set<Integer> ALWAYS_MAX_HIT_MONSTERS_MAGIC = setOf(
		idList(11709, 11712),   // baboon brawler
		idList(12816),          // frem warband berserker
		idList(14151, 14150),   // Royal titans elementals
		new java.util.ArrayList<>(YAMA_VOID_FLARE_IDS));

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

	// --- Distribution / applyNpcTransforms / isImmune id sets (constants.ts) -------------------------

	private static final List<Integer> ABYSSAL_PORTAL_IDS = idList(7533);
	private static final List<Integer> OLM_HEAD_IDS_LIST = idList(7551, 7554);
	private static final List<Integer> OLM_MELEE_HAND_IDS_LIST = idList(7552, 7555);
	private static final List<Integer> OLM_MAGE_HAND_IDS_LIST = idList(7550, 7553);
	private static final List<Integer> ZULRAH_IDS_LIST = idList(2042, 2043, 2044);

	/** Port of ONE_HIT_MONSTERS (constants.ts:448-452) — NPCs that always die in one player hit. */
	public static final Set<Integer> ONE_HIT_MONSTERS = setOf(
		7223,   // Giant rat (Scurrius)
		8584,   // Flower
		11193); // Flower (A Night at the Theatre)

	/** Port of TEKTON_IDS (constants.ts:180-183). */
	public static final Set<Integer> TEKTON_IDS_SET = setOf(TEKTON_IDS);

	/** Port of GUARDIAN_IDS (constants.ts:189-192) — CoX Guardians (pickaxe-only). */
	public static final Set<Integer> GUARDIAN_IDS = setOf(7569, 7571, 7570, 7572);

	/** Port of GLOWING_CRYSTAL_IDS (constants.ts:249-251). */
	public static final Set<Integer> GLOWING_CRYSTAL_IDS_SET = setOf(GLOWING_CRYSTAL_IDS);

	/** Port of ICE_DEMON_IDS (constants.ts:256-259). */
	public static final Set<Integer> ICE_DEMON_IDS_SET = setOf(ICE_DEMON_IDS);

	/** Port of OLM_HEAD_IDS (constants.ts:198-201). */
	public static final Set<Integer> OLM_HEAD_IDS = setOf(OLM_HEAD_IDS_LIST);

	/** Port of OLM_MELEE_HAND_IDS (constants.ts:206-209). */
	public static final Set<Integer> OLM_MELEE_HAND_IDS = setOf(OLM_MELEE_HAND_IDS_LIST);

	/** Port of OLM_MAGE_HAND_IDS (constants.ts:214-217). */
	public static final Set<Integer> OLM_MAGE_HAND_IDS = setOf(OLM_MAGE_HAND_IDS_LIST);

	/** Port of NIGHTMARE_TOTEM_IDS (constants.ts:306-309) — take double magic damage. */
	public static final Set<Integer> NIGHTMARE_TOTEM_IDS = setOf(
		9434, 9437, 9440, 9443,
		9435, 9438, 9441, 9444);

	/** Port of ZULRAH_IDS (constants.ts:341-343). */
	public static final Set<Integer> ZULRAH_IDS = setOf(ZULRAH_IDS_LIST);

	/** Port of VESPULA_IDS (constants.ts:269-271). */
	public static final Set<Integer> VESPULA_IDS = setOf(7530, 7531, 7532);

	/** Port of IMMUNE_TO_MELEE_DAMAGE_NPC_IDS (constants.ts:348-355). */
	public static final Set<Integer> IMMUNE_TO_MELEE_DAMAGE_NPC_IDS = setOf(
		idList(494),                       // kraken
		ABYSSAL_PORTAL_IDS,
		idList(7706),                      // zuk
		idList(7708),                      // Jal-MejJak
		idList(12214, 12215, 12219),       // leviathan
		ZULRAH_IDS_LIST);

	/** Port of IMMUNE_TO_NON_SALAMANDER_MELEE_DAMAGE_NPC_IDS (constants.ts:357-360) — aviansie. */
	public static final Set<Integer> IMMUNE_TO_NON_SALAMANDER_MELEE_DAMAGE_NPC_IDS = setOf(
		3169, 3170, 3171, 3172, 3173, 3174, 3175, 3176, 3177, 3178, 3179, 3180, 3181, 3182, 3183,
		7037);

	/** Port of IMMUNE_TO_RANGED_DAMAGE_NPC_IDS (constants.ts:365-370). */
	public static final Set<Integer> IMMUNE_TO_RANGED_DAMAGE_NPC_IDS = setOf(
		TEKTON_IDS, DUSK_IDS, GLOWING_CRYSTAL_IDS, WARRIORS_GUILD_CYCLOPES);

	/** Port of IMMUNE_TO_MAGIC_DAMAGE_NPC_IDS (constants.ts:383-386). */
	public static final Set<Integer> IMMUNE_TO_MAGIC_DAMAGE_NPC_IDS = setOf(
		DUSK_IDS, WARRIORS_GUILD_CYCLOPES);

	/** Port of BA_ATTACKER_MONSTERS (constants.ts:394-418) — Barbarian Assault attacker-level targets. */
	public static final Set<Integer> BA_ATTACKER_MONSTERS = setOf(
		// fighters
		1667, 5739, 5740, 5741, 5742, 5743, 5744, 5745, 5746, 5747,
		// rangers
		1668, 5757, 5758, 5759, 5760, 5761, 5762, 5763, 5764, 5765);

	/** Port of HUEYCOATL_TAIL_IDS (constants.ts:518). */
	public static final Set<Integer> HUEYCOATL_TAIL_IDS = setOf(14014);

	/** Port of HUEYCOATL_PHASE_IDS (constants.ts:525) — head + tail (body can't receive pillar buff). */
	public static final Set<Integer> HUEYCOATL_PHASE_IDS = setOf(
		idList(14009, 14010, 14013),  // head
		idList(14014));               // tail

	/** Port of ABYSSAL_SIRE_TRANSITION_IDS (constants.ts:536). */
	public static final Set<Integer> ABYSSAL_SIRE_TRANSITION_IDS = setOf(5886, 5889, 5891);

	/**
	 * Port of ALWAYS_ACCURATE_MONSTERS (constants.ts:489-494) — NPCs whose attacks always hit the player.
	 * Used by {@link NpcVsPlayerCalc#getHitChance()}.
	 */
	public static final Set<Integer> ALWAYS_ACCURATE_MONSTERS = setOf(
		931,    // Thrower Troll
		4135,   // Thrower troll (Trollheim)
		7691,   // Jal-Nib
		12335   // Wighted Leech
	);

	/**
	 * Port of NPC_HARDCODED_MAX_HIT (constants.ts:502-505) — NPCs whose max hit is hardcoded (the
	 * standard formula doesn't apply). Used by {@link NpcVsPlayerCalc#getNPCMaxHit()}.
	 */
	public static final java.util.Map<Integer, Integer> NPC_HARDCODED_MAX_HIT;
	static
	{
		java.util.Map<Integer, Integer> m = new java.util.HashMap<>();
		m.put(5947, 10); // Spinolyp (1)
		m.put(5961, 10); // Spinolyp (2)
		NPC_HARDCODED_MAX_HIT = Collections.unmodifiableMap(m);
	}

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
