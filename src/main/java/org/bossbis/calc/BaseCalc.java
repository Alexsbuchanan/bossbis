package org.bossbis.calc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bossbis.calc.types.BurnImmunity;
import org.bossbis.calc.types.CombatStyle;
import org.bossbis.calc.types.DefenceReductions;
import org.bossbis.calc.types.EquipmentPiece;
import org.bossbis.calc.types.Monster;
import org.bossbis.calc.types.MonsterAttribute;
import org.bossbis.calc.types.MonsterInputs;
import org.bossbis.calc.types.Player;
import org.bossbis.calc.types.PlayerEquipment;
import org.bossbis.calc.types.Spell;
import org.bossbis.calc.types.UserIssueType;
import org.bossbis.calc.data.EquipmentRepository;

/**
 * Port of {@code src/lib/BaseCalc.ts} (weirdgloop@5e313c1) — the base class the other calculators
 * extend. This milestone (v0.1.1 Task 2.1) ports the accuracy machinery only:
 * <ul>
 *   <li>the constructor + {@code DEFAULT_OPTS}, equipment canonicalization, the {@code allEquippedItems}
 *       list, and {@code sanitizeInputs} (spell/ammo validation + the {@code monsterCurrentHp} default,
 *       emitting the relevant {@link UserIssue}s);</li>
 *   <li>the {@code track*} arithmetic helpers (made PURE — the upstream {@code CalcDetails} UI recorder
 *       is dropped; the returned values are identical);</li>
 *   <li>the {@code wearing}/{@code wearingAll} checks and the full {@code isWearingX} predicate family;</li>
 *   <li>the four pure float accuracy-roll statics ({@code getNormalAccuracyRoll},
 *       {@code getFangAccuracyRoll}, {@code getConflictionGauntletsAccuracyRoll},
 *       {@code getMaxAccuracyHitChance}), ported {@code strictfp} and verbatim.</li>
 * </ul>
 *
 * <p>The max-hit / hit-distribution methods are NOT ported here (v0.1.2+); {@code PlayerVsNpcCalc}
 * (Milestone 3) extends this class and adds the attack rolls, defence roll, and hit chance.
 *
 * <h2>Aggregation</h2>
 * Upstream's {@code BaseCalc} constructor does NOT re-aggregate equipment bonuses — it reads the
 * already-aggregated {@code offensive}/{@code bonuses}/{@code defensive} that the Player builder
 * computed (upstream {@code Player extends EquipmentStats}; our {@link Player} carries the inlined
 * triple). This port mirrors that: it only canonicalizes the equipment map and builds the
 * {@code allEquippedItems} name list. The {@link EquipmentRepository} is injected purely so the
 * constructor can canonicalize (and so the eventual scaler/sub-calcs can resolve items) — no bonus
 * re-aggregation happens in {@code BaseCalc}.
 *
 * <h2>Deviations from source</h2>
 * <ul>
 *   <li><b>leagues:</b> the {@link Player} model intentionally omits the {@code leagues} state (spec
 *       §4.2). The {@code player.leagues.six.cullingSpree} disjuncts in {@code isWearingSlayerHelmet}/
 *       {@code isWearingBlackMask}/{@code isWearingImbuedBlackMask} are therefore dropped (treated as
 *       always-false). This matches the {@link Equipment} port's leagues deviation.</li>
 *   <li><b>scaleMonster (M5):</b> deferred — see the constructor's {@code // TODO(M5)} marker.</li>
 *   <li><b>spec-attack stance reset:</b> the {@code opts.usingSpecialAttack} branch of
 *       {@code sanitizeInputs} needs {@code getCombatStylesForCategory} (deferred to M3) — it throws
 *       with a {@code TODO(M3)} marker. {@code usingSpecialAttack} defaults to {@code false}, so normal
 *       v0.1.1 construction never hits it.</li>
 *   <li><b>CalcDetails:</b> the UI detail-recording side of {@code track*} is dropped; the helpers are
 *       pure arithmetic.</li>
 * </ul>
 */
public abstract strictfp class BaseCalc
{
	/** Port of {@code CalcOpts} (BaseCalc.ts:23-38) merged with {@code DEFAULT_OPTS} (BaseCalc.ts:48-59). */
	public static final class CalcOpts
	{
		public String loadoutName = "unknown";
		public boolean detailedOutput = false;
		public boolean disableMonsterScaling = false;
		public boolean usingSpecialAttack = false;
		public boolean isBlindBag = false;
		public int blindBagDistance = 1;
		public int blindBagUniques = 1;
		public boolean isEcho = false;
		/** Internal-only flag (see {@code InternalOpts}, BaseCalc.ts:40-46): sub-calc skips init. */
		public boolean noInit = false;
		// overrides {accuracy, attackRoll, defenceRoll, maxHit} are ported with the methods that read
		// them (M3+); not needed for construction.
	}

	/** Port of {@code UserIssue} (src/types/State.ts) — the tuple appended by {@code addIssue}. */
	public static final class UserIssue
	{
		public final UserIssueType type;
		public final String message;
		public final String loadout;

		UserIssue(UserIssueType type, String message, String loadout)
		{
			this.type = type;
			this.message = message;
			this.loadout = loadout;
		}
	}

	protected final CalcOpts opts;

	/** The repository used to canonicalize equipment (injected; mirrors upstream's global table). */
	protected final EquipmentRepository equipmentRepository;

	// The player that we're using for this calculation
	protected Player player;

	// The monster that we're using for this calculation
	protected Monster monster;

	// The original monster passed in to the calculator before scaling was applied
	protected Monster baseMonster;

	// List of the names of all equipped items (for quick checks)
	protected List<String> allEquippedItems = new ArrayList<>();

	public final List<UserIssue> userIssues = new ArrayList<>();

	/**
	 * Port of the {@code BaseCalc} constructor (BaseCalc.ts:83-102).
	 *
	 * <p>The {@link EquipmentRepository} is injected (mirroring the {@link Equipment} Milestone-1
	 * pattern) rather than constructed from a global so that {@code main} never does {@code new Gson()}.
	 *
	 * @param equipmentRepository injected so canonicalization does not need a global table.
	 */
	protected BaseCalc(Player player, Monster monster, CalcOpts opts, EquipmentRepository equipmentRepository)
	{
		this.opts = opts == null ? new CalcOpts() : opts;
		this.equipmentRepository = equipmentRepository;

		this.player = player;
		this.baseMonster = monster;
		// scaleMonster (M5): applies defence reductions (raid stat-scaling deferred — see MonsterScaling).
		// baseMonster stays unscaled; `monster` is the scaled view unless scaling is disabled.
		this.monster = (this.opts.disableMonsterScaling || this.opts.noInit) ? monster : scaleMonster(monster);

		if (!this.opts.noInit || this.opts.isBlindBag)
		{
			canonicalizeEquipment();
			this.allEquippedItems = collectEquippedItemNames(this.player.getEquipment());
			sanitizeInputs();
		}
	}

	/**
	 * Port of {@code scaleMonster} (BaseCalc.ts:17,95) — delegates to {@link MonsterScaling#scaleMonster}.
	 * v0.1.1 applies only the defence-reduction transform; raid stat-scaling is deferred (see
	 * {@link MonsterScaling}).
	 */
	private static Monster scaleMonster(Monster monster)
	{
		return MonsterScaling.scaleMonster(monster);
	}

	private static List<String> collectEquippedItemNames(PlayerEquipment eq)
	{
		List<String> names = new ArrayList<>();
		for (EquipmentPiece piece : slots(eq))
		{
			if (piece != null)
			{
				names.add(piece.getName() == null ? "" : piece.getName());
			}
		}
		return names;
	}

	private static List<EquipmentPiece> slots(PlayerEquipment eq)
	{
		return Arrays.asList(
			eq.getHead(), eq.getCape(), eq.getNeck(), eq.getAmmo(), eq.getWeapon(),
			eq.getBody(), eq.getShield(), eq.getLegs(), eq.getHands(), eq.getFeet(), eq.getRing());
	}

	// ---------------------------------------------------------------------------------------------
	// track* helpers (BaseCalc.ts:104-145). Pure arithmetic — the CalcDetails recorder is dropped.
	// ---------------------------------------------------------------------------------------------

	/**
	 * Shared truncating-arithmetic primitives, kept package-visible so a test hook can lock the exact
	 * long-intermediate behaviour without constructing a calc. The instance {@code track*} helpers
	 * delegate here.
	 */
	static final class TrackHelpers
	{
		private TrackHelpers() {}

		/** {@code Math.trunc(base * n / d)} with a long intermediate (BaseCalc.ts:110). */
		static int factor(int base, int n, int d)
		{
			return (int) ((long) base * n / d);
		}

		/** {@code Math.trunc((effectiveLevel * gearBonus + 320) / 640)} (BaseCalc.ts:119). */
		static int maxHitFromEffective(int effectiveLevel, int gearBonus)
		{
			return (int) (((long) effectiveLevel * gearBonus + 320) / 640);
		}

		/** {@code base + Math.trunc(base * n / d)} (BaseCalc.ts:131-132). */
		static int addFactor(int base, int n, int d)
		{
			int addend = (int) ((long) base * n / d);
			return base + addend;
		}
	}

	/** Port of {@code track} (BaseCalc.ts:104-107) — identity (the detail recorder is dropped). */
	protected <T> T track(String label, T value)
	{
		return value;
	}

	/** Port of {@code trackFactor} (BaseCalc.ts:109-115). Long intermediate. */
	protected int trackFactor(String label, int base, int n, int d)
	{
		return TrackHelpers.factor(base, n, d);
	}

	/** Port of {@code trackMaxHitFromEffective} (BaseCalc.ts:117-122). */
	protected int trackMaxHitFromEffective(String label, int effectiveLevel, int gearBonus)
	{
		return TrackHelpers.maxHitFromEffective(effectiveLevel, gearBonus);
	}

	/** Port of {@code trackAdd} (BaseCalc.ts:124-128). */
	protected int trackAdd(String label, int base, int addend)
	{
		return base + addend;
	}

	/** Port of {@code trackAddFactor} (BaseCalc.ts:130-137). */
	protected int trackAddFactor(String label, int base, int n, int d)
	{
		return TrackHelpers.addFactor(base, n, d);
	}

	// ---------------------------------------------------------------------------------------------
	// Equipment canonicalization (BaseCalc.ts:148-153)
	// ---------------------------------------------------------------------------------------------

	/** Port of {@code canonicalizeEquipment} (BaseCalc.ts:148-153). Uses the injected repository. */
	protected void canonicalizeEquipment()
	{
		PlayerEquipment canonical = canonicalizeEquipment(this.player.getEquipment());
		this.player = withEquipment(this.player, canonical);
	}

	private PlayerEquipment canonicalizeEquipment(PlayerEquipment in)
	{
		return new PlayerEquipment(
			canon(in.getHead()),
			canon(in.getCape()),
			canon(in.getNeck()),
			canon(in.getAmmo()),
			canon(in.getWeapon()),
			canon(in.getBody()),
			canon(in.getShield()),
			canon(in.getLegs()),
			canon(in.getHands()),
			canon(in.getFeet()),
			canon(in.getRing()));
	}

	private EquipmentPiece canon(EquipmentPiece piece)
	{
		if (piece == null)
		{
			return piece;
		}
		int canonicalId = equipmentRepository.canonicalId(piece.getId());
		if (piece.getId() == canonicalId)
		{
			return piece;
		}
		EquipmentPiece canonical = equipmentRepository.resolve(canonicalId).orElse(null);
		if (canonical == null)
		{
			return piece;
		}
		// Keep the canonical row's stats but carry over the original piece's itemVars.
		return new EquipmentPiece(
			canonical.getName(),
			canonical.getId(),
			canonical.getWeight(),
			canonical.getVersion(),
			canonical.getSlot(),
			canonical.getImage(),
			canonical.getSpeed(),
			canonical.getCategory(),
			canonical.isTwoHanded(),
			piece.getItemVars(),
			canonical.getBonuses(),
			canonical.getOffensive(),
			canonical.getDefensive());
	}

	// ---------------------------------------------------------------------------------------------
	// Accuracy roll formulas (BaseCalc.ts:155-213) — pure float math, ported verbatim, strictfp.
	// ---------------------------------------------------------------------------------------------

	/** Port of {@code getNormalAccuracyRoll} (BaseCalc.ts:155-168). */
	public static strictfp double getNormalAccuracyRoll(double atk, double def)
	{
		if (atk < 0)
		{
			atk = Math.min(0, atk + 2);
		}
		if (def < 0)
		{
			def = Math.min(0, def + 2);
		}

		if (atk >= 0 && def >= 0)
		{
			return normalStdRoll(atk, def);
		}
		if (atk >= 0 && def < 0)
		{
			return 1 - 1 / (-def + 1) / (atk + 1);
		}
		if (atk < 0 && def >= 0)
		{
			return 0;
		}
		if (atk < 0 && def < 0)
		{
			return normalStdRoll(-def, -atk);
		}
		return 0;
	}

	private static strictfp double normalStdRoll(double attack, double defence)
	{
		return attack > defence
			? 1 - ((defence + 2) / (2 * (attack + 1)))
			: attack / (2 * (defence + 1));
	}

	/** Port of {@code getFangAccuracyRoll} (BaseCalc.ts:170-186). */
	public static strictfp double getFangAccuracyRoll(double atk, double def)
	{
		if (atk < 0)
		{
			atk = Math.min(0, atk + 2);
		}
		if (def < 0)
		{
			def = Math.min(0, def + 2);
		}
		if (atk >= 0 && def >= 0)
		{
			return fangStdRoll(atk, def, def);
		}
		if (atk >= 0 && def < 0)
		{
			return 1 - 1 / (-def + 1) / (atk + 1);
		}
		if (atk < 0 && def >= 0)
		{
			return 0;
		}
		if (atk < 0 && def < 0)
		{
			return fangRvRoll(-def, -atk, def);
		}
		return 0;
	}

	// stdRoll captures the (possibly pre-clamp) `def` in upstream's closure; pass it explicitly.
	private static strictfp double fangStdRoll(double attack, double defence, double defClosure)
	{
		return attack > defClosure
			? 1 - (defence + 2) * (2 * defence + 3) / (attack + 1) / (attack + 1) / 6
			: attack * (4 * attack + 5) / 6 / (attack + 1) / (defence + 1);
	}

	private static strictfp double fangRvRoll(double attack, double defence, double defClosure)
	{
		return attack < defClosure
			? attack * (defence * 6 - 2 * attack + 5) / 6 / (defence + 1) / (defence + 1)
			: 1 - (defence + 2) * (2 * defence + 3) / 6 / (defence + 1) / (attack + 1);
	}

	/** Port of {@code getConflictionGauntletsAccuracyRoll} (BaseCalc.ts:188-192). */
	public static strictfp double getConflictionGauntletsAccuracyRoll(double atk, double def)
	{
		double singleRoll = getNormalAccuracyRoll(atk, def);
		double doubleRoll = getFangAccuracyRoll(atk, def);
		return doubleRoll / (1 + doubleRoll - singleRoll);
	}

	/** Port of {@code getMaxAccuracyHitChance} (BaseCalc.ts:194-213). */
	public static strictfp double getMaxAccuracyHitChance(double atk, double def)
	{
		if (atk < 0)
		{
			atk = Math.min(0, atk + 2);
		}
		if (def < 0)
		{
			def = Math.min(0, def + 2);
		}

		if (atk >= 0 && def >= 0)
		{
			return maxAccStdRoll(atk, def);
		}
		if (atk >= 0 && def < 0)
		{
			return 1;
		}
		if (atk < 0 && def >= 0)
		{
			return 0;
		}
		if (atk < 0 && def < 0)
		{
			return maxAccStdRoll(-def, -atk);
		}
		return 0;
	}

	private static strictfp double maxAccStdRoll(double attack, double defence)
	{
		return attack > defence ? 1 : attack / (defence + 1);
	}

	// ---------------------------------------------------------------------------------------------
	// wearing / wearingAll (BaseCalc.ts:220-233)
	// ---------------------------------------------------------------------------------------------

	/** Port of {@code wearing} for a single item (BaseCalc.ts:220-225). */
	protected boolean wearing(String item)
	{
		return allEquippedItems.contains(item);
	}

	/** Port of {@code wearing} for "any of" (BaseCalc.ts:220-225). */
	protected boolean wearing(String... items)
	{
		for (String i : items)
		{
			if (allEquippedItems.contains(i))
			{
				return true;
			}
		}
		return false;
	}

	/** Port of {@code wearingAll} (BaseCalc.ts:231-233). */
	protected boolean wearingAll(String... items)
	{
		for (String i : items)
		{
			if (!allEquippedItems.contains(i))
			{
				return false;
			}
		}
		return true;
	}

	// ---------------------------------------------------------------------------------------------
	// isWearingX predicate family (BaseCalc.ts:238 onward)
	// ---------------------------------------------------------------------------------------------

	/** Port of {@code isUsingMeleeStyle} (BaseCalc.ts:238-240). */
	protected boolean isUsingMeleeStyle()
	{
		String type = styleType();
		return CombatStyle.SLASH.equals(type) || CombatStyle.CRUSH.equals(type) || CombatStyle.STAB.equals(type);
	}

	/** Port of {@code isWearingVoidRobes} (BaseCalc.ts:246-250). */
	protected boolean isWearingVoidRobes()
	{
		return wearing("Void knight top", "Void knight top (or)", "Elite void top", "Elite void top (or)")
			&& wearing("Void knight robe", "Void knight robe (or)", "Elite void robe", "Elite void robe (or)")
			&& wearing("Void knight gloves");
	}

	/** Port of {@code isWearingEliteVoidRobes} (BaseCalc.ts:256-260). */
	protected boolean isWearingEliteVoidRobes()
	{
		return wearing("Elite void top", "Elite void top (or)")
			&& wearing("Elite void robe", "Elite void robe (or)")
			&& wearing("Void knight gloves");
	}

	/** Port of {@code isWearingMeleeVoid} (BaseCalc.ts:266-268). */
	protected boolean isWearingMeleeVoid()
	{
		return isWearingVoidRobes() && wearing("Void melee helm", "Void melee helm (or)");
	}

	/** Port of {@code isWearingEliteRangedVoid} (BaseCalc.ts:274-276). */
	protected boolean isWearingEliteRangedVoid()
	{
		return isWearingEliteVoidRobes() && wearing("Void ranger helm", "Void ranger helm (or)");
	}

	/** Port of {@code isWearingEliteMagicVoid} (BaseCalc.ts:282-284). */
	protected boolean isWearingEliteMagicVoid()
	{
		return isWearingEliteVoidRobes() && wearing("Void mage helm", "Void mage helm (or)");
	}

	/** Port of {@code isWearingRangedVoid} (BaseCalc.ts:290-292). */
	protected boolean isWearingRangedVoid()
	{
		return isWearingVoidRobes() && wearing("Void ranger helm", "Void ranger helm (or)");
	}

	/** Port of {@code isWearingMagicVoid} (BaseCalc.ts:298-300). */
	protected boolean isWearingMagicVoid()
	{
		return isWearingVoidRobes() && wearing("Void mage helm", "Void mage helm (or)");
	}

	/** Port of {@code isWearingSlayerHelmet} (BaseCalc.ts:306-308). leagues disjunct dropped (spec §4.2). */
	protected boolean isWearingSlayerHelmet()
	{
		return wearing("Slayer helmet", "Slayer helmet (i)");
	}

	/** Port of {@code isWearingBlackMask} (BaseCalc.ts:314-316). leagues disjunct dropped (spec §4.2). */
	protected boolean isWearingBlackMask()
	{
		return isWearingImbuedBlackMask() || wearing("Black mask", "Slayer helmet");
	}

	/** Port of {@code isWearingImbuedBlackMask} (BaseCalc.ts:322-324). leagues disjunct dropped (spec §4.2). */
	protected boolean isWearingImbuedBlackMask()
	{
		return wearing("Black mask (i)", "Slayer helmet (i)", "V's helm");
	}

	/** Port of {@code isWearingSmokeStaff} (BaseCalc.ts:330-332). */
	protected boolean isWearingSmokeStaff()
	{
		return wearing("Smoke battlestaff", "Mystic smoke staff", "Twinflame staff");
	}

	/** Port of {@code isWearingTzhaarWeapon} (BaseCalc.ts:338-340). */
	protected boolean isWearingTzhaarWeapon()
	{
		return wearing("Tzhaar-ket-em", "Tzhaar-ket-om", "Tzhaar-ket-om (t)", "Toktz-xil-ak", "Toktz-xil-ek", "Toktz-mej-tal");
	}

	/** Port of {@code isWearingObsidian} (BaseCalc.ts:346-348). */
	protected boolean isWearingObsidian()
	{
		return wearingAll("Obsidian helmet", "Obsidian platelegs", "Obsidian platebody");
	}

	/** Port of {@code isWearingBerserkerNecklace} (BaseCalc.ts:354-356). */
	protected boolean isWearingBerserkerNecklace()
	{
		return wearing("Berserker necklace", "Berserker necklace (or)");
	}

	/** Port of {@code isWearingCrystalBow} (BaseCalc.ts:362-364). */
	protected boolean isWearingCrystalBow()
	{
		return wearing("Crystal bow") || anyEquippedItemContains("Bow of faerdhinen");
	}

	/** Port of {@code isWearingFang} (BaseCalc.ts:370-372). */
	protected boolean isWearingFang()
	{
		return wearing("Osmumten's fang", "Osmumten's fang (or)");
	}

	/** Port of {@code isWearingAccursedSceptre} (BaseCalc.ts:374-376). */
	protected boolean isWearingAccursedSceptre()
	{
		return wearing("Accursed sceptre", "Accursed sceptre (a)");
	}

	/** Port of {@code isWearingBlowpipe} (BaseCalc.ts:378-380). */
	protected boolean isWearingBlowpipe()
	{
		return wearing("Toxic blowpipe", "Blazing blowpipe");
	}

	/** Port of {@code isWearingGodsword} (BaseCalc.ts:382-384). */
	protected boolean isWearingGodsword()
	{
		return wearing("Ancient godsword", "Armadyl godsword", "Bandos godsword", "Saradomin godsword", "Zamorak godsword");
	}

	/** Port of {@code isWearingScythe} (BaseCalc.ts:390-392). */
	protected boolean isWearingScythe()
	{
		return wearing("Scythe of vitur") || anyEquippedItemContains("of vitur");
	}

	/** Port of {@code isWearingTwoHitWeapon} (BaseCalc.ts:397-405). */
	protected boolean isWearingTwoHitWeapon()
	{
		return wearing("Torag's hammers", "Sulphur blades", "Glacial temotli", "Earthbound tecpatl", "Infernal tecpatl");
	}

	/** Port of {@code isWearingKeris} (BaseCalc.ts:411-413). */
	protected boolean isWearingKeris()
	{
		return anyEquippedItemContains("Keris");
	}

	/** Port of {@code isWearingDharok} (BaseCalc.ts:419-421). */
	protected boolean isWearingDharok()
	{
		return wearingAll("Dharok's helm", "Dharok's platebody", "Dharok's platelegs", "Dharok's greataxe");
	}

	/** Port of {@code isWearingVeracs} (BaseCalc.ts:427-429). */
	protected boolean isWearingVeracs()
	{
		return wearingAll("Verac's helm", "Verac's brassard", "Verac's plateskirt", "Verac's flail");
	}

	/** Port of {@code isWearingKarils} (BaseCalc.ts:435-437). */
	protected boolean isWearingKarils()
	{
		return wearingAll("Karil's coif", "Karil's leathertop", "Karil's leatherskirt", "Karil's crossbow", "Amulet of the damned");
	}

	/** Port of {@code isWearingAhrims} (BaseCalc.ts:444-446). */
	protected boolean isWearingAhrims()
	{
		return wearingAll("Ahrim's staff", "Ahrim's hood", "Ahrim's robetop", "Ahrim's robeskirt", "Amulet of the damned");
	}

	/** Port of {@code isWearingTorags} (BaseCalc.ts:452-454). */
	protected boolean isWearingTorags()
	{
		return wearingAll("Torag's helm", "Torag's platebody", "Torag's platelegs", "Torag's hammers", "Amulet of the damned");
	}

	/** Port of {@code isWearingBloodMoonSet} (BaseCalc.ts:456-458). */
	protected boolean isWearingBloodMoonSet()
	{
		return wearingAll("Dual macuahuitl", "Blood moon helm", "Blood moon chestplate", "Blood moon tassets");
	}

	/** Port of {@code isWearingSilverWeapon} (BaseCalc.ts:465-489). */
	protected boolean isWearingSilverWeapon()
	{
		EquipmentPiece ammo = player.getEquipment().getAmmo();
		if (ammo != null && ammo.getName() != null && ammo.getName().startsWith("Silver bolts")
			&& CombatStyle.RANGED.equals(styleType()))
		{
			return true;
		}

		return isUsingMeleeStyle() && wearing(
			"Blessed axe",
			"Ivandis flail",
			"Blisterwood flail",
			"Silver sickle",
			"Silver sickle (b)",
			"Emerald sickle",
			"Emerald sickle (b)",
			"Enchanted emerald sickle (b)",
			"Ruby sickle (b)",
			"Enchanted ruby sickle (b)",
			"Blisterwood sickle",
			"Silverlight",
			"Darklight",
			"Arclight",
			"Rod of ivandis",
			"Wolfbane");
	}

	/** Port of {@code wearingVampyrebane} (BaseCalc.ts:495-504). */
	protected boolean wearingVampyrebane(MonsterAttribute tier)
	{
		boolean t2 = tier == MonsterAttribute.VAMPYRE2;
		if (!(t2 || isUsingMeleeStyle()))
		{
			return false;
		}
		List<String> items = new ArrayList<>();
		if (t2)
		{
			items.add("Rod of ivandis");
		}
		items.add("Ivandis flail");
		items.add("Blisterwood sickle");
		items.add("Blisterwood flail");
		return wearing(items.toArray(new String[0]));
	}

	/** Port of {@code isWearingMsb} (BaseCalc.ts:506-508). */
	protected boolean isWearingMsb()
	{
		return wearing("Magic shortbow", "Magic shortbow (i)");
	}

	/** Port of {@code isWearingMlb} (BaseCalc.ts:510-512). */
	protected boolean isWearingMlb()
	{
		return wearing("Magic longbow", "Magic comp bow");
	}

	/** Port of {@code isWearingLeafBladedWeapon} (BaseCalc.ts:518-543). */
	protected boolean isWearingLeafBladedWeapon()
	{
		if (isUsingMeleeStyle() && wearing("Leaf-bladed battleaxe", "Leaf-bladed spear", "Leaf-bladed sword"))
		{
			return true;
		}

		if (player.getSpell() != null && "Magic Dart".equals(player.getSpell().getName()))
		{
			return true;
		}

		if (wearing("Broad arrows", "Broad bolts", "Amethyst broad bolts") && CombatStyle.RANGED.equals(styleType()))
		{
			return true;
		}

		return false;
	}

	/** Port of {@code isWearingCorpbaneWeapon} (BaseCalc.ts:549-578). */
	protected boolean isWearingCorpbaneWeapon()
	{
		EquipmentPiece weapon = player.getEquipment().getWeapon();
		boolean isStab = CombatStyle.STAB.equals(styleType());
		if (weapon == null)
		{
			return false;
		}

		if (isWearingFang())
		{
			return isStab;
		}

		String name = weapon.getName() == null ? "" : weapon.getName();

		if (name.endsWith("halberd"))
		{
			return isStab;
		}

		// https://twitter.com/JagexAsh/status/1777673598099968104
		if (name.contains("spear") && !name.equals("Blue moon spear"))
		{
			return isStab;
		}

		if (CombatStyle.MAGIC.equals(styleType()))
		{
			return true;
		}

		if (name.equals("King's barrage"))
		{
			return true;
		}

		return false;
	}

	/** Port of {@code isRevWeaponBuffApplicable} (BaseCalc.ts:580-595). */
	protected boolean isRevWeaponBuffApplicable()
	{
		EquipmentPiece weapon = player.getEquipment().getWeapon();
		if (!player.getBuffs().isInWilderness() || weapon == null || !"Charged".equals(weapon.getVersion()))
		{
			return false;
		}

		String type = styleType();
		if (CombatStyle.MAGIC.equals(type))
		{
			return wearing("Accursed sceptre", "Accursed sceptre (a)", "Thammaron's sceptre", "Thammaron's sceptre (a)");
		}
		if (CombatStyle.RANGED.equals(type))
		{
			return wearing("Craw's bow", "Webweaver bow");
		}
		return wearing("Ursine chainmace", "Viggora's chainmace");
	}

	/** Port of {@code isWearingRatBoneWeapon} (BaseCalc.ts:601-607). */
	protected boolean isWearingRatBoneWeapon()
	{
		return wearing("Bone mace", "Bone shortbow", "Bone staff");
	}

	/** Port of {@code isChargeSpellApplicable} (BaseCalc.ts:609-624). */
	protected boolean isChargeSpellApplicable()
	{
		if (!player.getBuffs().isChargeSpell())
		{
			return false;
		}

		String spellName = player.getSpell() == null ? null : player.getSpell().getName();
		if ("Saradomin Strike".equals(spellName))
		{
			return wearing("Saradomin cape", "Imbued saradomin cape", "Saradomin max cape", "Imbued saradomin max cape");
		}
		if ("Claws of Guthix".equals(spellName))
		{
			return wearing("Guthix cape", "Imbued guthix cape", "Guthix max cape", "Imbued guthix max cape");
		}
		if ("Flames of Zamorak".equals(spellName))
		{
			return wearing("Zamorak cape", "Imbued zamorak cape", "Zamorak max cape", "Imbued zamorak max cape");
		}
		return false;
	}

	/** Port of {@code isWearingJusticiarArmour} (BaseCalc.ts:630-636). */
	protected boolean isWearingJusticiarArmour()
	{
		return wearingAll("Justiciar faceguard", "Justiciar chestguard", "Justiciar legguards");
	}

	/** Port of {@code isUsingDemonbane} (BaseCalc.ts:638-649). */
	protected boolean isUsingDemonbane()
	{
		String type = styleType();
		if (CombatStyle.MAGIC.equals(type))
		{
			return player.getSpell() != null && player.getSpell().getName() != null
				&& player.getSpell().getName().contains("Demonbane");
		}
		if (CombatStyle.RANGED.equals(type))
		{
			return wearing("Scorching bow");
		}
		return wearing("Silverlight", "Darklight", "Arclight", "Emberlight", "Bone claws", "Burning claws", "Infernal tecpatl");
	}

	/** Port of {@code isUsingAbyssal} (BaseCalc.ts:651-654). */
	protected boolean isUsingAbyssal()
	{
		return isUsingMeleeStyle()
			&& wearing("Abyssal bludgeon", "Abyssal dagger", "Abyssal whip", "Abyssal tentacle");
	}

	/** Port of {@code isWearingOgreBow} (BaseCalc.ts:656-658). */
	protected boolean isWearingOgreBow()
	{
		return wearing("Ogre bow", "Comp ogre bow");
	}

	/** Port of {@code tdUnshieldedBonusApplies} (BaseCalc.ts:660-678). */
	protected boolean tdUnshieldedBonusApplies()
	{
		if (!"Tormented Demon".equals(monster.getName())
			|| !"Unshielded".equals(monster.getInputs() == null ? null : monster.getInputs().getPhase()))
		{
			return false;
		}

		String type = styleType();
		if (CombatStyle.MAGIC.equals(type))
		{
			return player.getSpell() != null;
		}
		if (CombatStyle.RANGED.equals(type))
		{
			EquipmentPiece weapon = player.getEquipment().getWeapon();
			return weapon != null
				&& CombatStyle.HEAVY.equals(CombatStyle.getRangedDamageType(weapon.getCategory()));
		}
		if (CombatStyle.CRUSH.equals(type))
		{
			return true;
		}
		return false;
	}

	/** Port of {@code isAmmoInvalid} (BaseCalc.ts:680-682). */
	protected boolean isAmmoInvalid()
	{
		EquipmentPiece weapon = player.getEquipment().getWeapon();
		EquipmentPiece ammo = player.getEquipment().getAmmo();
		Integer weaponId = weapon == null ? null : weapon.getId();
		Integer ammoId = ammo == null ? null : ammo.getId();
		return Equipment.ammoApplicability(weaponId, ammoId) == Equipment.AmmoApplicability.INVALID;
	}

	/** Port of {@code isImmuneToNormalBurns} (BaseCalc.ts:684-687). */
	protected boolean isImmuneToNormalBurns()
	{
		BurnImmunity burn = monster.getImmunities() == null ? null : monster.getImmunities().getBurn();
		return burn == BurnImmunity.NORMAL || isImmuneToStrongBurns();
	}

	/** Port of {@code isImmuneToStrongBurns} (BaseCalc.ts:689-692). */
	protected boolean isImmuneToStrongBurns()
	{
		BurnImmunity burn = monster.getImmunities() == null ? null : monster.getImmunities().getBurn();
		return burn == BurnImmunity.STRONG || Constants.IMMUNE_TO_BURN_DAMAGE_NPC_IDS.contains(monster.getId());
	}

	/** Port of {@code isSlayerMonster} (BaseCalc.ts:694-696). */
	protected boolean isSlayerMonster()
	{
		return monster.isSlayerMonster();
	}

	/** Port of {@code addIssue} (BaseCalc.ts:698-700). */
	protected void addIssue(UserIssueType type, String message)
	{
		userIssues.add(new UserIssue(type, message, opts.loadoutName));
	}

	/** Port of {@code demonbaneVulnerability} (BaseCalc.ts:702-714). */
	protected int demonbaneVulnerability()
	{
		MonsterInputs inputs = monster.getInputs();
		if (monster.getId() == -1 && inputs != null && inputs.getDemonbaneVulnerability() != null)
		{
			return inputs.getDemonbaneVulnerability();
		}
		if ("Duke Sucellus".equals(monster.getName()))
		{
			return 70;
		}
		if (Constants.YAMA_IDS.contains(monster.getId()))
		{
			return 120;
		}
		if (Constants.YAMA_VOID_FLARE_IDS.contains(monster.getId()))
		{
			return 200;
		}
		return 100;
	}

	// ---------------------------------------------------------------------------------------------
	// sanitizeInputs (BaseCalc.ts:716-826)
	// ---------------------------------------------------------------------------------------------

	private void sanitizeInputs()
	{
		PlayerEquipment eq = player.getEquipment();

		if (monster.getAttributes() != null && monster.getAttributes().contains(MonsterAttribute.DEMON))
		{
			// make sure demonbane effectiveness is set and uses the right value
			monster = withInputs(monster, withDemonbaneVulnerability(monster.getInputs(), demonbaneVulnerability()));
		}

		// make sure monsterCurrentHp is set and valid
		MonsterInputs inputs = monster.getInputs();
		int currentHp = inputs == null ? 0 : inputs.getMonsterCurrentHp();
		int maxHp = monster.getSkills() == null ? 0 : monster.getSkills().getHp();
		if (currentHp <= 0 || currentHp > maxHp)
		{
			monster = withInputs(monster, withMonsterCurrentHp(monster.getInputs(), maxHp));
		}

		// specs are never manual cast, although the base loadout can be at the same time
		if (opts.usingSpecialAttack)
		{
			// TODO(M3): this branch needs getCombatStylesForCategory (deferred to Milestone 3) to reset
			// the player's style/spell for spec attacks. opts.usingSpecialAttack defaults to false, so
			// normal v0.1.1 construction never reaches here; spec is out of scope for v0.1.1.
			throw new UnsupportedOperationException(
				"not ported: spec-attack stance reset needs getCombatStylesForCategory (M3)");
		}

		// we should do clone-edits here to prevent affecting ui state
		String stance = styleStance();
		if (!Constants.CAST_STANCES.contains(stance))
		{
			player = withSpell(player, null);
		}

		if (!"Manual Cast".equals(styleStance()) && isAmmoInvalid())
		{
			if (eq.getAmmo() != null && eq.getAmmo().getName() != null)
			{
				addIssue(UserIssueType.EQUIPMENT_WRONG_AMMO, "This ammo does not work with your current weapon.");
			}
			else
			{
				addIssue(UserIssueType.EQUIPMENT_MISSING_AMMO, "Your weapon requires ammo to use.");
			}
		}

		// Certain spells require specific weapons to be equipped
		String spellName = player.getSpell() == null ? null : player.getSpell().getName();
		if (
			("Iban Blast".equals(spellName) && !wearing("Iban's staff", "Iban's staff (u)"))
			|| ("Saradomin Strike".equals(spellName) && !wearing("Saradomin staff", "Staff of light"))
			|| ("Claws of Guthix".equals(spellName) && !wearing("Guthix staff", "Void knight mace", "Staff of balance"))
			|| ("Flames of Zamarok".equals(spellName) && !wearing("Zamorak staff", "Staff of the dead", "Toxic staff of the dead", "Thammaron's sceptre (a)", "Accursed sceptre (a)"))
			|| ("Magic Dart".equals(spellName) && !wearing("Slayer's staff", "Slayer's staff (e)", "Staff of the dead", "Toxic staff of the dead", "Staff of light", "Staff of balance")))
		{
			player = withSpell(player, null);
			addIssue(UserIssueType.SPELL_WRONG_WEAPON, "This spell needs a specific weapon equipped to cast.");
		}

		// Certain spells can only be cast on specific monsters
		spellName = player.getSpell() == null ? null : player.getSpell().getName();
		boolean hasDemon = monster.getAttributes() != null && monster.getAttributes().contains(MonsterAttribute.DEMON);
		boolean hasUndead = monster.getAttributes() != null && monster.getAttributes().contains(MonsterAttribute.UNDEAD);
		if (
			(spellName != null && spellName.contains("Demonbane") && !hasDemon)
			|| ("Crumble Undead".equals(spellName) && !hasUndead))
		{
			player = withSpell(player, null);
			addIssue(UserIssueType.SPELL_WRONG_MONSTER, "This spell cannot be cast on the selected monster.");
		}

		// some weapons are only available to use against certain monsters
		String version = monster.getVersion();
		boolean dawnbringerWrong = wearing("Dawnbringer")
			&& (!"Verzik Vitur".equals(monster.getName()) || version == null || !version.contains("Phase 1"));
		boolean holyWaterWrong = wearing("Holy water")
			&& !(monster.getAttributes() != null && monster.getAttributes().contains(MonsterAttribute.DEMON));
		if (dawnbringerWrong || holyWaterWrong)
		{
			addIssue(UserIssueType.WEAPON_WRONG_MONSTER, "This weapon cannot be used against the select monster.");
		}

		// Some set effects are currently not accounted for
		if (
			wearingAll("Blue moon helm", "Blue moon chestplate", "Blue moon tassets", "Blue moon spear")
			|| wearingAll("Eclipse moon helm", "Eclipse moon chestplate", "Eclipse moon tassets", "Eclipse atlatl"))
		{
			addIssue(UserIssueType.EQUIPMENT_SET_EFFECT_UNSUPPORTED, "The calculator currently does not account for your equipment set effect.");
		}
		if (wearing("Ring of recoil") || wearing("Ring of suffering (i)") || wearing("Ring of suffering"))
		{
			addIssue(UserIssueType.RING_RECOIL_UNSUPPORTED, "The calculator does not account for recoil damage.");
		}
		if (wearing("Echo boots"))
		{
			addIssue(UserIssueType.FEET_RECOIL_UNSUPPORTED, "The calculator does not account for recoil damage.");
		}
	}

	// ---------------------------------------------------------------------------------------------
	// helpers
	// ---------------------------------------------------------------------------------------------

	private String styleType()
	{
		return player.getStyle() == null ? null : player.getStyle().getType();
	}

	private String styleStance()
	{
		return player.getStyle() == null ? null : player.getStyle().getStance();
	}

	private boolean anyEquippedItemContains(String sub)
	{
		for (String name : allEquippedItems)
		{
			if (name != null && name.contains(sub))
			{
				return true;
			}
		}
		return false;
	}

	private static Player withEquipment(Player p, PlayerEquipment eq)
	{
		return new Player(
			p.getName(), p.getStyle(), p.getSkills(), p.getBoosts(), eq, p.getAttackSpeed(),
			p.getPrayers(), p.getBuffs(), p.getSpell(),
			p.getBonuses(), p.getOffensive(), p.getDefensive());
	}

	private static Player withSpell(Player p, Spell spell)
	{
		return new Player(
			p.getName(), p.getStyle(), p.getSkills(), p.getBoosts(), p.getEquipment(), p.getAttackSpeed(),
			p.getPrayers(), p.getBuffs(), spell,
			p.getBonuses(), p.getOffensive(), p.getDefensive());
	}

	private static Monster withInputs(Monster m, MonsterInputs inputs)
	{
		return new Monster(
			m.getId(), m.getName(), m.getImage(), m.getVersion(), m.getSize(), m.getSpeed(),
			m.getStyle(), m.getMaxHit(), m.getSkills(), m.getOffensive(), m.getDefensive(),
			m.getAttributes(), m.getWeakness(), m.getImmunities(), m.isSlayerMonster(), inputs);
	}

	private static MonsterInputs withDemonbaneVulnerability(MonsterInputs in, Integer value)
	{
		MonsterInputs base = in == null ? MonsterInputs.initial() : in;
		return new MonsterInputs(
			base.isFromCoxCm(), base.getToaInvocationLevel(), base.getToaPathLevel(),
			base.getPartyMaxCombatLevel(), base.getPartySumMiningLevel(), base.getPartyMaxHpLevel(),
			base.getPartySize(), base.getMonsterCurrentHp(), base.getDefenceReductions(),
			value, base.getPhase(), base.getPrayers());
	}

	private static MonsterInputs withMonsterCurrentHp(MonsterInputs in, int hp)
	{
		MonsterInputs base = in == null ? MonsterInputs.initial() : in;
		return new MonsterInputs(
			base.isFromCoxCm(), base.getToaInvocationLevel(), base.getToaPathLevel(),
			base.getPartyMaxCombatLevel(), base.getPartySumMiningLevel(), base.getPartyMaxHpLevel(),
			base.getPartySize(), hp, base.getDefenceReductions(),
			base.getDemonbaneVulnerability(), base.getPhase(), base.getPrayers());
	}
}
