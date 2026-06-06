package org.bossbis.calc;

import java.util.ArrayList;
import java.util.List;
import org.bossbis.calc.CalcMath.MinMax;
import org.bossbis.calc.data.EquipmentRepository;
import org.bossbis.calc.data.SpellRepository;
import org.bossbis.calc.types.Buffs;
import org.bossbis.calc.types.CombatStyle;
import org.bossbis.calc.types.EquipmentCategory;
import org.bossbis.calc.types.EquipmentPiece;
import org.bossbis.calc.types.Monster;
import org.bossbis.calc.types.MonsterAttribute;
import org.bossbis.calc.types.Player;
import org.bossbis.calc.types.Prayer;
import org.bossbis.calc.types.Prayer.PrayerData;
import org.bossbis.calc.types.Spell;
import org.bossbis.calc.types.Spell.Spellement;
import org.bossbis.calc.types.Weakness;

/**
 * Port of {@code src/lib/PlayerVsNPCCalc.ts} (weirdgloop@5e313c1) — the player-vs-NPC calculator.
 *
 * <p>This milestone (v0.1.1, Milestones 3-4) ports the <b>accuracy half</b> only:
 * <ul>
 *   <li>{@link #getMaxAttackRoll()} dispatch (PlayerVsNPCCalc.ts:1205-1237);</li>
 *   <li>the three attack rolls — {@link #getPlayerMaxMeleeAttackRoll()} (:206-336),
 *       {@link #getPlayerMaxRangedAttackRoll()} (:545-651), {@link #getPlayerMaxMagicAttackRoll()}
 *       (:849-955) — including their ordered accuracy-only special-case multipliers.</li>
 * </ul>
 *
 * <p>Every max-hit / hit-distribution / DPS / TTK / spec method is stubbed to throw
 * {@link UnsupportedOperationException} until v0.1.2+ (the {@code maxHit} corpus rows remain skipped).
 * The defence roll and hit chance ({@code getNPCDefenceRoll}/{@code getHitChance}) land in Milestones
 * 5-6.
 *
 * <h2>Deviations from source (carried from {@link BaseCalc})</h2>
 * <ul>
 *   <li><b>leagues:</b> the {@link Player} model omits leagues state (spec §4.2), so the
 *       {@code player.leagues.six.*} branches in the attack rolls (talent prayer scaling, chinchompa
 *       leagues, the {@code getMaxAttackRoll} leagues-accuracy factor, the crossbow/ranged-damage
 *       talents) are dropped (treated as always-false / zero). This matches the {@code BaseCalc} and
 *       {@code Equipment} leagues deviations.</li>
 *   <li><b>spec attacks:</b> {@code opts.usingSpecialAttack} defaults to {@code false}; the spec-only
 *       multiplier branches are ported (for faithfulness) but are never reached in normal v0.1.1
 *       construction (the {@code BaseCalc} sanitizeInputs spec branch still throws — see its javadoc).</li>
 * </ul>
 */
public strictfp class PlayerVsNpcCalc extends BaseCalc
{
	/**
	 * Repository over the bundled spells, used by {@link #getSpellMaxHit(Spell, int)} to resolve the
	 * elemental strike/bolt/blast/wave/surge tiers by magic level. May be {@code null} for loadouts that
	 * never need a tier lookup (no spell, or a non-elemental spell); the magic max-hit path requires it
	 * for elemental spells.
	 */
	private final SpellRepository spellRepository;

	/**
	 * Constructs a calc with default options.
	 *
	 * <p>The {@link EquipmentRepository} is injected (mirroring {@link BaseCalc}) so canonicalization
	 * does not need a global table and {@code main} never does {@code new Gson()}.
	 */
	public PlayerVsNpcCalc(Player player, Monster monster, EquipmentRepository equipmentRepository)
	{
		this(player, monster, new CalcOpts(), equipmentRepository, null);
	}

	public PlayerVsNpcCalc(Player player, Monster monster, CalcOpts opts, EquipmentRepository equipmentRepository)
	{
		this(player, monster, opts, equipmentRepository, null);
	}

	/**
	 * Constructs a calc with the {@link SpellRepository} needed for the magic max-hit elemental-tier
	 * lookups (see {@link #getSpellMaxHit(Spell, int)}).
	 */
	public PlayerVsNpcCalc(Player player, Monster monster, EquipmentRepository equipmentRepository,
		SpellRepository spellRepository)
	{
		this(player, monster, new CalcOpts(), equipmentRepository, spellRepository);
	}

	public PlayerVsNpcCalc(Player player, Monster monster, CalcOpts opts,
		EquipmentRepository equipmentRepository, SpellRepository spellRepository)
	{
		super(player, monster, opts, equipmentRepository);
		this.spellRepository = spellRepository;
	}

	/**
	 * Subset of upstream {@code DetailKey} (src/lib/CalcDetails.ts) used by the ported attack rolls.
	 * The {@link BaseCalc} {@code track*} helpers are pure arithmetic (the CalcDetails recorder is
	 * dropped), so these labels are documentation only — they preserve the upstream key names 1:1 for
	 * reviewability.
	 */
	private static final class DetailKey
	{
		private DetailKey() {}

		static final String DAMAGE_LEVEL = "Level";

		// Melee max hit (getPlayerMaxMeleeHit)
		static final String DAMAGE_LEVEL_PRAYER = "Damage level (prayer)";
		static final String DAMAGE_LEVEL_SOULREAPER_BONUS = "Damage level (soulreaper bonus)";
		static final String DAMAGE_LEVEL_SOULREAPER = "Damage level (soulreaper)";
		static final String DAMAGE_EFFECTIVE_LEVEL = "Effective damage level";
		static final String DAMAGE_EFFECTIVE_LEVEL_HOLY_WATER = "Damage effective level holy water";
		static final String DAMAGE_EFFECTIVE_LEVEL_VOID = "Effective damage level (void)";
		static final String DAMAGE_GEAR_BONUS = "Damage gear bonus";
		static final String MAX_HIT_BASE = "Base max hit";
		static final String MAX_HIT_FORINTHRY_SURGE = "Max hit (forinthry surge)";
		static final String MAX_HIT_SALVE = "Max hit (salve)";
		static final String MAX_HIT_BLACK_MASK = "Max hit (black mask)";
		static final String MAX_HIT_DEMONBANE = "Max hit (demonbane)";
		static final String MAX_HIT_OBSIDIAN = "Max hit (obsidian)";
		static final String MAX_HIT_KERIS = "Max hit (keris)";
		static final String MAX_HIT_GOLEMBANE = "Max hit (golembane)";
		static final String MAX_HIT_REV_WEAPON = "Max hit (rev weapon)";
		static final String MAX_HIT_NEZIKCHENED = "Max hit Nezikchened";
		static final String MAX_HIT_TONALZTICS = "Max hit tonalztics";
		static final String MAX_HIT_WARDENS = "Max hit wardens";
		static final String MIN_HIT_WARDENS = "Min hit wardens";
		static final String WARDENS_ACCURACY_DELTA = "Wardens accuracy delta";
		static final String WARDENS_DMG_MODIFIER = "Wardens damage modifier";
		static final String MAX_HIT_LEAFY = "Max hit (leafy)";
		static final String MAX_HIT_COLOSSALBLADE = "Max hit (colossal blade)";
		static final String MAX_HIT_RATBANE = "Max hit (ratbane)";
		static final String MAX_HIT_INQ = "Max hit (inquisitor)";
		static final String MIN_HIT_FANG = "Min hit (fang)";
		static final String MAX_HIT_FANG = "Max hit (fang)";
		static final String MAX_HIT_SPEC = "Max hit (spec)";
		static final String MIN_HIT_SPEC = "Min hit (spec)";
		static final String MAX_HIT_GODSWORD_SPEC = "Max hit (godsword spec)";
		static final String REPIRATORY_SYSTEM_MIN_HIT = "Min hit (respiratory system)";
		static final String MIN_HIT_FINAL = "Min hit";
		static final String MAX_HIT_FINAL = "Max hit";

		static final String PLAYER_ACCURACY_LEVEL = "Effective accuracy level";
		static final String PLAYER_ACCURACY_LEVEL_PRAYER = "Effective accuracy level (prayer)";
		static final String PLAYER_ACCURACY_EFFECTIVE_LEVEL = "Effective accuracy level";
		static final String PLAYER_ACCURACY_EFFECTIVE_LEVEL_VOID = "Effective accuracy level (void)";
		static final String PLAYER_ACCURACY_GEAR_BONUS = "Gear bonus";
		static final String PLAYER_ACCURACY_ROLL_BASE = "Base attack roll";
		static final String PLAYER_ACCURACY_ROLL_FINAL = "Attack roll";
		static final String PLAYER_ACCURACY_ROLL_MAGIC_PERCENT = "Attack roll (magic percent)";
		static final String PLAYER_ACCURACY_FORINTHRY_SURGE = "Attack roll (forinthry surge)";
		static final String PLAYER_ACCURACY_SALVE = "Attack roll (salve)";
		static final String PLAYER_ACCURACY_BLACK_MASK = "Attack roll (black mask)";
		static final String PLAYER_ACCURACY_OBSIDIAN = "Attack roll (obsidian)";
		static final String PLAYER_ACCURACY_REV_WEAPON = "Attack roll (rev weapon)";
		static final String PLAYER_ACCURACY_DEMONBANE = "Attack roll (demonbane)";
		static final String PLAYER_ACCURACY_DRAGONHUNTER = "Attack roll (dragon hunter)";
		static final String PLAYER_ACCURACY_KERIS = "Attack roll (keris)";
		static final String PLAYER_ACCURACY_VAMPYREBANE = "Attack roll (vampyrebane)";
		static final String PLAYER_ACCURACY_EFARITAY = "Attack roll (efaritay)";
		static final String PLAYER_ACCURACY_GOLEMBANE = "Attack roll (golembane)";
		static final String PLAYER_ACCURACY_INQ = "Attack roll (inquisitor)";
		static final String PLAYER_ACCURACY_SPEC = "Attack roll (spec)";
		static final String PLAYER_ACCURACY_CHINCHOMPA = "Attack roll (chinchompa)";
		static final String PLAYER_ACCURACY_TITANS_RANGED = "Attack roll (royal titans ranged)";
		static final String PLAYER_ACCURACY_SMOKE_BATTLESTAFF = "Attack roll (smoke battlestaff)";
		static final String PLAYER_ACCURACY_TOME = "Attack roll (tome)";
		static final String PLAYER_ACCURACY_SPELLEMENT = "Attack roll (spellement)";
		static final String PLAYER_ACCURACY_SPELLEMENT_BONUS = "Attack roll (spellement bonus)";
		static final String PLAYER_DEMONBANE_FACTOR = "Demonbane factor";
		static final String MAX_HIT_DRAGONHUNTER = "Max hit (dragon hunter)";

		// Magic max hit (getPlayerMaxMagicHit)
		static final String MAX_HIT_MAGIC_DMG = "Max hit (magic damage bonus)";
		static final String MAX_HIT_SPELLEMENT_BONUS = "Max hit (spellement bonus)";
		static final String MAX_HIT_SPELLEMENT = "Max hit (spellement)";
		static final String MIN_HIT_SUNFIRE = "Min hit (sunfire)";
		static final String MAX_HIT_TOME = "Max hit (tome)";

		static final String NPC_DEFENCE_ROLL_LEVEL = "NPC defence roll (level)";
		static final String NPC_DEFENCE_ROLL_EFFECTIVE_LEVEL = "NPC defence roll (effective level)";
		static final String NPC_DEFENCE_STAT_BONUS = "NPC defence roll (stat bonus)";
		static final String NPC_DEFENCE_ROLL_BASE = "NPC defence roll (base)";
		static final String NPC_DEFENCE_ROLL_TOA = "NPC defence roll (toa)";
		static final String NPC_DEFENCE_ROLL_FINAL = "NPC defence roll";

		// Hit chance (getHitChance / getDisplayHitChance)
		static final String PLAYER_ACCURACY_BASE = "Base hit chance";
		static final String PLAYER_ACCURACY_DAWNBRINGER = "Hit chance (dawnbringer)";
		static final String PLAYER_ACCURACY_SCURRIUS_RAT = "Hit chance (scurrius rat)";
		static final String PLAYER_ACCURACY_TD = "Hit chance (tormented demon)";
		static final String PLAYER_ACCURACY_ROYAL_TITAN_ELEMENTAL = "Hit chance (royal titan elemental)";
		static final String PLAYER_ACCURACY_FANG_TOA = "Hit chance (fang toa)";
		static final String PLAYER_ACCURACY_FANG = "Hit chance (fang)";
		static final String PLAYER_ACCURACY_CONFLICTION_GAUNTLETS = "Hit chance (confliction gauntlets)";
		static final String PLAYER_ACCURACY_BRIMSTONE = "Hit chance (brimstone)";
		static final String PLAYER_ACCURACY_FINAL = "Hit chance";
	}

	// =================================================================================================
	// NPC defence roll
	// =================================================================================================

	/**
	 * Port of {@code getNPCDefenceRoll} (PlayerVsNPCCalc.ts:147-205). Computes the monster's defence
	 * roll against the player's current combat style.
	 *
	 * <p>Returns {@code long}: the roll uses long intermediates ({@link #trackFactorLong}) per the
	 * {@code trackFactor} contract — the ToA invocation factor can push the product past 32 bits.
	 *
	 * <p>The {@code opts.overrides.defenceRoll} short-circuit is dropped (no overrides in this model).
	 * The {@code usingSpecialAttack} defence-style overrides are ported verbatim (cheap and faithful),
	 * though {@code usingSpecialAttack} defaults to {@code false} in v0.1.1.
	 */
	public long getNPCDefenceRoll()
	{
		String defenceStyle = styleType();
		if (opts.usingSpecialAttack)
		{
			if (wearing(
				"Dragon claws",
				"Dragon dagger",
				"Dragon halberd",
				"Dragon longsword",
				"Dragon scimitar",
				"Crystal halberd",
				"Abyssal dagger",
				"Saradomin sword",
				"Arkan blade") || isWearingGodsword())
			{
				defenceStyle = CombatStyle.SLASH;
			}
			else if (wearing("Arclight", "Emberlight", "Dragon sword"))
			{
				defenceStyle = CombatStyle.STAB;
			}
			else if (wearing("Voidwaker", "Saradomin's blessed sword"))
			{
				// doesn't really matter for voidwaker since it's 100% accuracy but eh
				defenceStyle = CombatStyle.MAGIC;
			}
			else if (wearing("Dragon mace"))
			{
				defenceStyle = CombatStyle.CRUSH;
			}
		}

		int level = track(DetailKey.NPC_DEFENCE_ROLL_LEVEL,
			CombatStyle.MAGIC.equals(defenceStyle)
				&& !Constants.USES_DEFENCE_LEVEL_FOR_MAGIC_DEFENCE_NPC_IDS.contains(monster.getId())
				? monster.getSkills().getMagic()
				: monster.getSkills().getDef());
		int effectiveLevel = trackAdd(DetailKey.NPC_DEFENCE_ROLL_EFFECTIVE_LEVEL, level, 9);

		int bonus;
		if (CombatStyle.RANGED.equals(defenceStyle))
		{
			EquipmentPiece weapon = player.getEquipment().getWeapon();
			String rangedType = CombatStyle.getRangedDamageType(weapon == null ? null : weapon.getCategory());
			if (CombatStyle.MIXED.equals(rangedType))
			{
				bonus = (monster.getDefensive().getLight()
					+ monster.getDefensive().getStandard()
					+ monster.getDefensive().getHeavy()) / 3;
			}
			else
			{
				bonus = monsterRangedDefensiveFor(rangedType);
			}
		}
		else
		{
			bonus = monsterDefensiveFor(defenceStyle != null ? defenceStyle : CombatStyle.CRUSH);
		}

		int statBonus = trackAdd(DetailKey.NPC_DEFENCE_STAT_BONUS, defenceStyle != null ? bonus : 0, 64);
		long defenceRoll = trackFactorLong(DetailKey.NPC_DEFENCE_ROLL_BASE, effectiveLevel, statBonus, 1);

		boolean isCustomMonster = monster.getId() == -1;

		if (((Constants.TOMBS_OF_AMASCUT_MONSTER_IDS.contains(monster.getId())
				&& !Constants.KEPHRI_OVERLORD_IDS_SET.contains(monster.getId())) || isCustomMonster)
			&& toaInvocationLevel() != 0)
		{
			defenceRoll = trackFactorLong(DetailKey.NPC_DEFENCE_ROLL_TOA, defenceRoll,
				250 + toaInvocationLevel(), 250);
		}

		return track(DetailKey.NPC_DEFENCE_ROLL_FINAL, defenceRoll);
	}

	/** Long-intermediate {@code trackFactor} for the defence roll (the ToA factor can exceed 32 bits). */
	private long trackFactorLong(String label, long base, int n, int d)
	{
		return track(label, base * n / d);
	}

	/** Reads {@code monster.defensive[style]} for a melee/magic style (the ranged split is handled inline). */
	private int monsterDefensiveFor(String style)
	{
		if (style == null)
		{
			return 0;
		}
		switch (style)
		{
			case CombatStyle.STAB:
				return monster.getDefensive().getStab();
			case CombatStyle.SLASH:
				return monster.getDefensive().getSlash();
			case CombatStyle.CRUSH:
				return monster.getDefensive().getCrush();
			case CombatStyle.MAGIC:
				return monster.getDefensive().getMagic();
			default:
				return 0;
		}
	}

	/** Reads {@code monster.defensive[rangedType]} for a ranged damage subtype (light/standard/heavy). */
	private int monsterRangedDefensiveFor(String rangedType)
	{
		if (rangedType == null)
		{
			return 0;
		}
		switch (rangedType)
		{
			case CombatStyle.LIGHT:
				return monster.getDefensive().getLight();
			case CombatStyle.STANDARD:
				return monster.getDefensive().getStandard();
			case CombatStyle.HEAVY:
				return monster.getDefensive().getHeavy();
			default:
				return 0;
		}
	}

	private int toaInvocationLevel()
	{
		return monster.getInputs() == null ? 0 : monster.getInputs().getToaInvocationLevel();
	}

	// =================================================================================================
	// Attack rolls (the ported accuracy half)
	// =================================================================================================

	/**
	 * Port of {@code getMaxAttackRoll} (PlayerVsNPCCalc.ts:1205-1237). Dispatches by the player's
	 * combat-style type; the {@code overrides.attackRoll} short-circuit and the leagues all-style
	 * accuracy factor are dropped (no overrides/leagues in this model).
	 */
	public int getMaxAttackRoll()
	{
		if (!CombatStyle.MANUAL_CAST.equals(styleStance()) && isAmmoInvalid())
		{
			return track(DetailKey.PLAYER_ACCURACY_ROLL_FINAL, 0);
		}

		String style = styleType();
		int atkRoll = 0;
		if (isUsingMeleeStyle())
		{
			atkRoll = getPlayerMaxMeleeAttackRoll();
		}
		if (CombatStyle.RANGED.equals(style))
		{
			atkRoll = getPlayerMaxRangedAttackRoll();
		}
		if (CombatStyle.MAGIC.equals(style))
		{
			atkRoll = getPlayerMaxMagicAttackRoll();
		}

		return track(DetailKey.PLAYER_ACCURACY_ROLL_FINAL, atkRoll);
	}

	/** Port of {@code getPlayerMaxMeleeAttackRoll} (PlayerVsNPCCalc.ts:206-336). */
	private int getPlayerMaxMeleeAttackRoll()
	{
		String stanceType = styleType();
		String stance = styleStance();

		int effectiveLevel = trackAdd(DetailKey.DAMAGE_LEVEL,
			player.getSkills().getAtk(), player.getBoosts().getAtk());

		for (PrayerData p : getCombatPrayers(PrayerFilter.ACCURACY))
		{
			effectiveLevel = trackFactor(DetailKey.PLAYER_ACCURACY_LEVEL_PRAYER, effectiveLevel,
				p.factorAccuracy().numerator(), p.factorAccuracy().divisor());
		}

		int stanceBonus = 8;
		if (CombatStyle.ACCURATE.equals(stance))
		{
			stanceBonus += 3;
		}
		else if (CombatStyle.CONTROLLED.equals(stance))
		{
			stanceBonus += 1;
		}

		effectiveLevel = trackAdd(DetailKey.PLAYER_ACCURACY_EFFECTIVE_LEVEL, effectiveLevel, stanceBonus);

		boolean isWearingVoid = isWearingMeleeVoid();
		if (isWearingVoid)
		{
			effectiveLevel = trackFactor(DetailKey.PLAYER_ACCURACY_EFFECTIVE_LEVEL_VOID, effectiveLevel, 11, 10);
		}

		int gearBonus = trackAdd(DetailKey.PLAYER_ACCURACY_GEAR_BONUS,
			stanceType != null ? offensiveFor(stanceType) : 0, 64);
		int baseRoll = trackFactor(DetailKey.PLAYER_ACCURACY_ROLL_BASE, effectiveLevel, gearBonus, 1);
		int attackRoll = baseRoll;

		List<MonsterAttribute> mattrs = attributes();

		if (wearing("Crystal blessing"))
		{
			int crystalPieces = (wearing("Crystal helm") ? 1 : 0)
				+ (wearing("Crystal legs") ? 2 : 0)
				+ (wearing("Crystal body") ? 3 : 0);
			attackRoll = (int) ((long) attackRoll * (20 + crystalPieces) / 20);
		}

		// These bonuses do not stack with each other
		if (wearing("Amulet of avarice") && monster.getName() != null && monster.getName().startsWith("Revenant"))
		{
			int num = player.getBuffs().isForinthrySurge() ? 27 : 24;
			attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_FORINTHRY_SURGE, attackRoll, num, 20);
		}
		else if (wearing("Salve amulet (e)", "Salve amulet(ei)") && mattrs.contains(MonsterAttribute.UNDEAD))
		{
			attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_SALVE, attackRoll, 6, 5);
		}
		else if (wearing("Salve amulet", "Salve amulet(i)") && mattrs.contains(MonsterAttribute.UNDEAD))
		{
			attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_SALVE, attackRoll, 7, 6);
		}
		else if (isWearingBlackMask() && isSlayerMonster() && player.getBuffs().isOnSlayerTask())
		{
			attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_BLACK_MASK, attackRoll, 7, 6);
		}

		if (isWearingTzhaarWeapon() && isWearingObsidian())
		{
			int obsidianBonus = trackFactor(DetailKey.PLAYER_ACCURACY_OBSIDIAN, baseRoll, 1, 10);
			attackRoll = trackAdd(DetailKey.PLAYER_ACCURACY_OBSIDIAN, attackRoll, obsidianBonus);
		}

		if (isRevWeaponBuffApplicable())
		{
			attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_REV_WEAPON, attackRoll, 3, 2);
		}
		if (wearing("Arclight", "Emberlight") && mattrs.contains(MonsterAttribute.DEMON))
		{
			attackRoll = applyDemonbane(attackRoll, 70);
		}
		if (wearing("Bone claws", "Burning claws") && mattrs.contains(MonsterAttribute.DEMON))
		{
			attackRoll = applyDemonbane(attackRoll, 5);
		}
		if (mattrs.contains(MonsterAttribute.DRAGON))
		{
			if (wearing("Dragon hunter lance"))
			{
				attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_DRAGONHUNTER, attackRoll, 6, 5);
			}
			else if (wearing("Dragon hunter wand"))
			{
				attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_DRAGONHUNTER, attackRoll, 7, 4);
			}
		}
		if (wearing("Keris partisan of breaching") && mattrs.contains(MonsterAttribute.KALPHITE))
		{
			attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_KERIS, attackRoll, 133, 100);
		}
		if (wearing("Keris partisan of the sun")
			&& Constants.TOMBS_OF_AMASCUT_MONSTER_IDS.contains(monster.getId())
			&& currentHp() < (monster.getSkills().getHp() / 4))
		{
			attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_KERIS, attackRoll, 5, 4);
		}
		if (wearing("Blisterwood flail", "Blisterwood sickle") && MonsterAttribute.isVampyre(mattrs))
		{
			attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_VAMPYREBANE, attackRoll, 21, 20);
		}
		if (isWearingSilverWeapon() && wearing("Efaritay's aid") && MonsterAttribute.isVampyre(mattrs))
		{
			attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_EFARITAY, attackRoll, 23, 20);
		}

		if (wearing("Granite hammer") && mattrs.contains(MonsterAttribute.GOLEM))
		{
			attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_GOLEMBANE, attackRoll, 13, 10);
		}

		// Inquisitor's armour set gives bonuses when using the crush attack style
		if (CombatStyle.CRUSH.equals(stanceType))
		{
			int inqPieces = 0;
			for (String v : allEquippedItems)
			{
				if ("Inquisitor's great helm".equals(v)
					|| "Inquisitor's hauberk".equals(v)
					|| "Inquisitor's plateskirt".equals(v))
				{
					inqPieces++;
				}
			}

			if (inqPieces > 0)
			{
				if (wearing("Inquisitor's mace"))
				{
					inqPieces *= 5;
				}
				else if (inqPieces == 3)
				{
					inqPieces = 5;
				}
				attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_INQ, attackRoll, 200 + inqPieces, 200);
			}
		}

		if (opts.usingSpecialAttack)
		{
			if (isWearingGodsword())
			{
				attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_SPEC, attackRoll, 2, 1);
			}
			else if (isWearingFang() || wearing("Arkan blade") || wearing("Granite hammer"))
			{
				attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_SPEC, attackRoll, 3, 2);
			}
			else if (wearing("Elder maul", "Dragon mace", "Dragon sword", "Dragon scimitar", "Abyssal whip"))
			{
				attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_SPEC, attackRoll, 5, 4);
			}
			else if (wearing("Dragon dagger"))
			{
				attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_SPEC, attackRoll, 23, 20);
			}
			else if (wearing("Abyssal dagger"))
			{
				attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_SPEC, attackRoll, 5, 4);
			}
			else if (wearing("Soulreaper axe"))
			{
				int stacks = Math.max(0, Math.min(5, player.getBuffs().getSoulreaperStacks()));
				attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_SPEC, attackRoll, 100 + 6 * stacks, 100);
			}
			else if (wearing("Brine sabre"))
			{
				attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_SPEC, attackRoll, 2, 1);
			}
			else if (wearing("Barrelchest anchor"))
			{
				attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_SPEC, attackRoll, 2, 1);
			}
		}

		return attackRoll;
	}

	/** Port of {@code getPlayerMaxRangedAttackRoll} (PlayerVsNPCCalc.ts:545-651). */
	private int getPlayerMaxRangedAttackRoll()
	{
		String stance = styleStance();
		String styleName = styleName();

		int effectiveLevel = track(DetailKey.PLAYER_ACCURACY_LEVEL,
			player.getSkills().getRanged() + player.getBoosts().getRanged());
		for (PrayerData p : getCombatPrayers(PrayerFilter.ACCURACY))
		{
			effectiveLevel = trackFactor(DetailKey.PLAYER_ACCURACY_LEVEL_PRAYER, effectiveLevel,
				p.factorAccuracy().numerator(), p.factorAccuracy().divisor());
		}

		if (CombatStyle.ACCURATE.equals(stance))
		{
			effectiveLevel += 3;
		}

		effectiveLevel += 8;

		if (isWearingRangedVoid())
		{
			effectiveLevel = (int) ((long) effectiveLevel * 11 / 10);
		}

		int attackRoll = (int) ((long) effectiveLevel * (player.getOffensive().getRanged() + 64));

		if (isWearingCrystalBow())
		{
			int crystalPieces = (wearing("Crystal helm") ? 1 : 0)
				+ (wearing("Crystal legs") ? 2 : 0)
				+ (wearing("Crystal body") ? 3 : 0);
			attackRoll = (int) ((long) attackRoll * (20 + crystalPieces) / 20);
		}

		List<MonsterAttribute> mattrs = attributes();

		if (wearing("Amulet of avarice") && monster.getName() != null && monster.getName().startsWith("Revenant"))
		{
			int num = player.getBuffs().isForinthrySurge() ? 27 : 24;
			attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_FORINTHRY_SURGE, attackRoll, num, 20);
		}
		else if (wearing("Salve amulet(ei)") && mattrs.contains(MonsterAttribute.UNDEAD))
		{
			attackRoll = (int) ((long) attackRoll * 6 / 5);
		}
		else if (wearing("Salve amulet(i)") && mattrs.contains(MonsterAttribute.UNDEAD))
		{
			attackRoll = (int) ((long) attackRoll * 7 / 6);
		}
		else if (isWearingImbuedBlackMask() && isSlayerMonster() && player.getBuffs().isOnSlayerTask())
		{
			attackRoll = (int) ((long) attackRoll * 23 / 20);
		}

		if (wearing("Twisted bow"))
		{
			int cap = mattrs.contains(MonsterAttribute.XERICIAN) ? 350 : 250;
			int tbowMagic = Math.min(cap, Math.max(monster.getSkills().getMagic(), monster.getOffensive().getMagic()));
			attackRoll = tbowScaling(attackRoll, tbowMagic, true);
			if (Constants.P2_WARDEN_IDS_SET.contains(monster.getId()))
			{
				attackRoll = tbowScaling(attackRoll, tbowMagic, true);
			}
		}
		if (isRevWeaponBuffApplicable())
		{
			attackRoll = (int) ((long) attackRoll * 3 / 2);
		}
		if (wearing("Dragon hunter crossbow") && mattrs.contains(MonsterAttribute.DRAGON))
		{
			attackRoll = (int) ((long) attackRoll * 13 / 10);
		}
		EquipmentPiece weapon = player.getEquipment().getWeapon();
		if (weapon != null && weapon.getCategory() == EquipmentCategory.CHINCHOMPA)
		{
			int distance = Math.min(7, Math.max(1, player.getBuffs().getChinchompaDistance()));

			int numerator = 4;
			if ("Short fuse".equals(styleName))
			{
				if (distance >= 7)
				{
					numerator = 2;
				}
				else if (distance >= 4)
				{
					numerator = 3;
				}
			}
			else if ("Medium fuse".equals(styleName))
			{
				if (distance < 4 || distance >= 7)
				{
					numerator = 3;
				}
			}
			else if ("Long fuse".equals(styleName))
			{
				if (distance < 4)
				{
					numerator = 2;
				}
				else if (distance < 7)
				{
					numerator = 3;
				}
			}

			attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_CHINCHOMPA, attackRoll, numerator, 4);
		}

		if (wearing("Scorching bow") && mattrs.contains(MonsterAttribute.DEMON))
		{
			attackRoll = applyDemonbane(attackRoll, 30);
		}

		if (opts.usingSpecialAttack)
		{
			if (wearing("Zaryte crossbow", "Webweaver bow") || isWearingBlowpipe())
			{
				attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_SPEC, attackRoll, 2, 1);
			}
			else if (isWearingMsb())
			{
				attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_SPEC, attackRoll, 10, 7);
			}
			else if (wearing("Heavy ballista", "Light ballista"))
			{
				attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_SPEC, attackRoll, 5, 4);
			}
			else if (wearing("Rosewood blowpipe"))
			{
				attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_SPEC, attackRoll, 4, 5);
			}
		}

		if (Constants.TITAN_BOSS_IDS.contains(monster.getId())
			&& "Out of Melee Range".equals(phase()))
		{
			attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_TITANS_RANGED, attackRoll, 6, 1);
		}

		return attackRoll;
	}

	/** Port of {@code getPlayerMaxMagicAttackRoll} (PlayerVsNPCCalc.ts:849-955). */
	private int getPlayerMaxMagicAttackRoll()
	{
		String stance = styleStance();

		int effectiveLevel = track(DetailKey.PLAYER_ACCURACY_LEVEL,
			player.getSkills().getMagic() + player.getBoosts().getMagic());
		for (PrayerData p : getCombatPrayers(PrayerFilter.ACCURACY))
		{
			effectiveLevel = trackFactor(DetailKey.PLAYER_ACCURACY_LEVEL_PRAYER, effectiveLevel,
				p.factorAccuracy().numerator(), p.factorAccuracy().divisor());
		}

		if (CombatStyle.ACCURATE.equals(stance))
		{
			effectiveLevel += 2;
		}

		effectiveLevel += 9;

		if (isWearingMagicVoid())
		{
			effectiveLevel = (int) ((long) effectiveLevel * 29 / 20);
		}

		List<MonsterAttribute> mattrs = attributes();
		int magicBonus = player.getOffensive().getMagic();

		int baseRoll = (int) ((long) effectiveLevel * (magicBonus + 64));
		int attackRoll = baseRoll;

		EquipmentPiece weapon = player.getEquipment().getWeapon();
		boolean usingPoweredStaff = weapon != null
			&& weapon.getCategory() == EquipmentCategory.POWERED_STAFF
			&& !CombatStyle.MANUAL_CAST.equals(stance);
		if (usingPoweredStaff && wearing("Crystal blessing"))
		{
			int crystalPieces = (wearing("Crystal helm") ? 1 : 0)
				+ (wearing("Crystal legs") ? 2 : 0)
				+ (wearing("Crystal body") ? 3 : 0);
			attackRoll = (int) ((long) attackRoll * (20 + crystalPieces) / 20);
		}

		int additiveBonus = 0;
		boolean blackMaskBonus = false;
		if (wearing("Amulet of avarice") && monster.getName() != null && monster.getName().startsWith("Revenant"))
		{
			additiveBonus = trackAdd(DetailKey.PLAYER_ACCURACY_FORINTHRY_SURGE, additiveBonus,
				player.getBuffs().isForinthrySurge() ? 35 : 20);
		}
		else if (wearing("Salve amulet(ei)") && mattrs.contains(MonsterAttribute.UNDEAD))
		{
			additiveBonus = trackAdd(DetailKey.PLAYER_ACCURACY_SALVE, additiveBonus, 20);
		}
		else if (wearing("Salve amulet(i)") && mattrs.contains(MonsterAttribute.UNDEAD))
		{
			additiveBonus = trackAdd(DetailKey.PLAYER_ACCURACY_SALVE, additiveBonus, 15);
		}
		else if (isWearingImbuedBlackMask() && isSlayerMonster() && player.getBuffs().isOnSlayerTask())
		{
			blackMaskBonus = true;
		}

		if (wearing("Efaritay's aid") && MonsterAttribute.isVampyre(mattrs) && isWearingSilverWeapon())
		{
			additiveBonus = trackAdd(DetailKey.PLAYER_ACCURACY_EFARITAY, additiveBonus, 15);
		}

		if (isWearingSmokeStaff() && player.getSpell() != null
			&& player.getSpell().getSpellbook() == Spell.Spellbook.STANDARD)
		{
			additiveBonus = trackAdd(DetailKey.PLAYER_ACCURACY_SMOKE_BATTLESTAFF, additiveBonus, 10);
		}

		if (additiveBonus != 0)
		{
			attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_ROLL_MAGIC_PERCENT, attackRoll,
				100 + additiveBonus, 100);
		}

		if (mattrs.contains(MonsterAttribute.DRAGON))
		{
			if (wearing("Dragon hunter crossbow"))
			{
				attackRoll = trackFactor(DetailKey.MAX_HIT_DRAGONHUNTER, attackRoll, 13, 10);
			}
			else if (wearing("Dragon hunter lance"))
			{
				attackRoll = trackFactor(DetailKey.MAX_HIT_DRAGONHUNTER, attackRoll, 6, 5);
			}
			else if (wearing("Dragon hunter wand"))
			{
				attackRoll = trackFactor(DetailKey.MAX_HIT_DRAGONHUNTER, attackRoll, 7, 4);
			}
		}

		if (blackMaskBonus)
		{
			attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_BLACK_MASK, attackRoll, 23, 20);
		}

		Spell spell = player.getSpell();
		if (spell != null && spell.getName() != null && spell.getName().contains("Demonbane")
			&& mattrs.contains(MonsterAttribute.DEMON))
		{
			int demonbanePercent = player.getBuffs().isMarkOfDarknessSpell() ? 40 : 20;
			if (wearing("Purging staff"))
			{
				demonbanePercent *= 2;
			}
			attackRoll = applyDemonbane(attackRoll, demonbanePercent);
		}
		if (isRevWeaponBuffApplicable())
		{
			attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_REV_WEAPON, attackRoll, 3, 2);
		}
		if (wearing("Tome of water") && (getSpellement() == Spellement.WATER || isBindSpell(spell)))
		{
			attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_TOME, attackRoll, 6, 5);
		}

		if (opts.usingSpecialAttack)
		{
			if (isWearingAccursedSceptre())
			{
				attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_SPEC, attackRoll, 3, 2);
			}
			else if (wearing("Volatile nightmare staff"))
			{
				attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_SPEC, attackRoll, 3, 2);
			}
			else if (wearing("Eye of ayak"))
			{
				attackRoll = trackFactor(DetailKey.PLAYER_ACCURACY_SPEC, attackRoll, 2, 1);
			}
		}

		Spellement spellement = getSpellement();
		Weakness weakness = getMonsterWeakness();
		if (spellement != null && weakness != null && spellement == weakness.getElement())
		{
			int bonus = trackFactor(DetailKey.PLAYER_ACCURACY_SPELLEMENT_BONUS, baseRoll,
				weakness.getSeverity(), 100);
			attackRoll = trackAdd(DetailKey.PLAYER_ACCURACY_SPELLEMENT, attackRoll, bonus);
		}

		return attackRoll;
	}

	// =================================================================================================
	// Attack-roll helpers (ported from PlayerVsNPCCalc.ts)
	// =================================================================================================

	/** Selector for {@link #getCombatPrayers(PrayerFilter)} (mirrors the {@code keyof PrayerData} filter). */
	private enum PrayerFilter
	{
		ACCURACY, STRENGTH, MAGIC_DAMAGE_BONUS
	}

	/** Port of {@code getCombatPrayers} (PlayerVsNPCCalc.ts:1144-1157). */
	private List<PrayerData> getCombatPrayers(PrayerFilter filter)
	{
		String style = styleType();
		String wantStyle;
		if (isUsingMeleeStyle())
		{
			wantStyle = "melee";
		}
		else if (CombatStyle.RANGED.equals(style))
		{
			wantStyle = "ranged";
		}
		else
		{
			wantStyle = "magic";
		}

		List<PrayerData> out = new ArrayList<>();
		List<Prayer> prayers = player.getPrayers();
		if (prayers == null)
		{
			return out;
		}
		for (Prayer p : prayers)
		{
			PrayerData d = p.data();
			if (!wantStyle.equals(d.combatStyle()))
			{
				continue;
			}
			boolean has;
			switch (filter)
			{
				case ACCURACY:
					has = d.factorAccuracy() != null;
					break;
				case STRENGTH:
					has = d.factorStrength() != null;
					break;
				case MAGIC_DAMAGE_BONUS:
					has = d.magicDamageBonus() != null;
					break;
				default:
					has = false;
			}
			if (has)
			{
				out.add(d);
			}
		}
		return out;
	}

	/**
	 * Port of {@code trackAddFactor(attackRoll, demonbaneFactor(percent))} —
	 * {@code demonbaneFactor} (PlayerVsNPCCalc.ts:2617-2621) scales the weapon's demonbane percent by
	 * the monster's {@code demonbaneVulnerability} (defaulting to 100), then {@code trackAddFactor}
	 * (BaseCalc.ts:130-137) adds {@code attackRoll * percent / 100} to {@code attackRoll}.
	 */
	private int applyDemonbane(int attackRoll, int weaponDemonbane)
	{
		return applyDemonbane(DetailKey.PLAYER_ACCURACY_DEMONBANE, attackRoll, weaponDemonbane);
	}

	/** As {@link #applyDemonbane(int, int)} but with an explicit label (the melee max hit uses {@code MAX_HIT_DEMONBANE}). */
	private int applyDemonbane(String label, int value, int weaponDemonbane)
	{
		Integer vulnObj = monster.getInputs() == null ? null : monster.getInputs().getDemonbaneVulnerability();
		int vulnerability = vulnObj == null ? 100 : vulnObj;
		int percent = trackFactor(DetailKey.PLAYER_DEMONBANE_FACTOR, weaponDemonbane, vulnerability, 100);
		return trackAddFactor(label, value, percent, 100);
	}

	/** Port of the static {@code tbowScaling} (PlayerVsNPCCalc.ts:2639-2648). */
	private static int tbowScaling(int current, int magic, boolean accuracyMode)
	{
		int factor = accuracyMode ? 10 : 14;
		int base = accuracyMode ? 140 : 250;

		int t2 = (3 * magic - factor) / 100;
		int inner = (3 * magic / 10) - (10 * factor);
		int t3 = (inner * inner) / 100;

		int bonus = base + t2 - t3;
		return (int) ((long) current * bonus / 100);
	}

	/** Port of {@code isBindSpell} (src/types/Spell.ts:20-24). */
	private static boolean isBindSpell(Spell spell)
	{
		if (spell == null || spell.getName() == null)
		{
			return false;
		}
		String n = spell.getName();
		return "Bind".equals(n) || "Snare".equals(n) || "Entangle".equals(n);
	}

	/** Port of {@code getSpellement} (PlayerVsNPCCalc.ts:2732-2751); leagues branches dropped. */
	private Spellement getSpellement()
	{
		Spell spell = player.getSpell();
		if (spell == null)
		{
			return null;
		}
		return spell.getElement();
	}

	/** Port of {@code getMonsterWeakness} (PlayerVsNPCCalc.ts:2788-2810); leagues quadrant/devil's branches retained. */
	private Weakness getMonsterWeakness()
	{
		Spellement spellement = getSpellement();
		Weakness baseWeakness = monster.getWeakness();
		if (spellement == null)
		{
			return null;
		}

		boolean shadowflame = wearing("Shadowflame quadrant");
		boolean devils = wearing("Devil's element");
		if (!shadowflame && !devils)
		{
			return baseWeakness;
		}

		boolean usingRightSpell = shadowflame
			|| (baseWeakness != null && baseWeakness.getElement() == spellement);
		int baseSeverity = (baseWeakness != null && usingRightSpell) ? baseWeakness.getSeverity() : 0;
		int devilsBonus = devils ? 30 : 0;
		return new Weakness(spellement, baseSeverity + devilsBonus);
	}

	// =================================================================================================
	// Small accessors mirroring the TS field reads
	// =================================================================================================

	private String styleType()
	{
		return player.getStyle() == null ? null : player.getStyle().getType();
	}

	private String styleStance()
	{
		return player.getStyle() == null ? null : player.getStyle().getStance();
	}

	private String styleName()
	{
		return player.getStyle() == null ? null : player.getStyle().getName();
	}

	private String phase()
	{
		return monster.getInputs() == null ? null : monster.getInputs().getPhase();
	}

	private int currentHp()
	{
		return monster.getInputs() == null ? 0 : monster.getInputs().getMonsterCurrentHp();
	}

	private List<MonsterAttribute> attributes()
	{
		if (monster.getAttributes() == null)
		{
			return new ArrayList<>();
		}
		return new ArrayList<>(monster.getAttributes());
	}

	private int offensiveFor(String styleType)
	{
		switch (styleType)
		{
			case CombatStyle.STAB:
				return player.getOffensive().getStab();
			case CombatStyle.SLASH:
				return player.getOffensive().getSlash();
			case CombatStyle.CRUSH:
				return player.getOffensive().getCrush();
			case CombatStyle.MAGIC:
				return player.getOffensive().getMagic();
			case CombatStyle.RANGED:
				return player.getOffensive().getRanged();
			default:
				return 0;
		}
	}

	// =================================================================================================
	// Stubs — max-hit / distribution / DPS / TTK / spec (v0.1.2+)
	// =================================================================================================

	private static final String NOT_PORTED = "not ported until v0.1.2+";

	// =================================================================================================
	// Hit chance
	// =================================================================================================

	/**
	 * Port of {@code getDisplayHitChance} (PlayerVsNPCCalc.ts:1239-1260). Equals {@link #getHitChance()}
	 * except for the Brimstone-ring magic 0.75/0.25 blend (the displayed accuracy averages the normal
	 * and the 10%-reduced-defence rolls).
	 */
	public double getDisplayHitChance()
	{
		double hitChance = getHitChance();

		if (hitChance == 1.0 || hitChance == 0.0)
		{
			// probably a special effect
			return hitChance;
		}

		int atk = getMaxAttackRoll();
		long def = getNPCDefenceRoll();

		if (CombatStyle.MAGIC.equals(styleType()) && wearing("Brimstone ring"))
		{
			double effectHitChance = track(DetailKey.PLAYER_ACCURACY_BRIMSTONE,
				BaseCalc.getNormalAccuracyRoll(atk, def * 9 / 10));

			hitChance = 0.75 * hitChance + 0.25 * effectHitChance;
		}

		return hitChance;
	}

	/**
	 * Port of {@code getHitChance} (PlayerVsNPCCalc.ts:1262-1373). Combines {@link #getMaxAttackRoll()}
	 * against {@link #getNPCDefenceRoll()} via {@link BaseCalc#getNormalAccuracyRoll(double, double)},
	 * with the ordered special accuracy paths (guaranteed-accuracy monsters, Royal-Titan elementals,
	 * always-max-hit monsters, spec overrides, Fang, Confliction gauntlets).
	 *
	 * <p>Deviations: the {@code opts.overrides.accuracy} short-circuit is dropped (no overrides in this
	 * model), and the leagues talent branches (crossbow double-accuracy, max-accuracy-from-range) are
	 * dropped per the class-level leagues deviation.
	 */
	public double getHitChance()
	{
		int monsterId = monster.getId();

		if (Constants.GUARANTEED_ACCURACY_MONSTERS.contains(monsterId))
		{
			return track(DetailKey.PLAYER_ACCURACY_FINAL, 1.0);
		}

		if (Constants.DOOM_OF_MOKHAIOTL_IDS.contains(monsterId) && !"Normal".equals(phase()))
		{
			return track(DetailKey.PLAYER_ACCURACY_FINAL, 1.0);
		}

		if (Constants.VERZIK_P1_IDS_SET.contains(monsterId) && wearing("Dawnbringer"))
		{
			track(DetailKey.PLAYER_ACCURACY_DAWNBRINGER, 1.0);
			return track(DetailKey.PLAYER_ACCURACY_FINAL, 1.0);
		}

		if (Constants.P2_WARDEN_IDS_SET.contains(monsterId))
		{
			return track(DetailKey.PLAYER_ACCURACY_FINAL, 1.0);
		}

		// Giant rat (Scurrius)
		if (monsterId == 7223 && !CombatStyle.MANUAL_CAST.equals(styleStance()))
		{
			track(DetailKey.PLAYER_ACCURACY_SCURRIUS_RAT, 1.0);
			return track(DetailKey.PLAYER_ACCURACY_FINAL, 1.0);
		}

		if ("Tormented Demon".equals(monster.getName()) && !"Shielded".equals(phase()))
		{
			track(DetailKey.PLAYER_ACCURACY_TD, 1.0);
			return track(DetailKey.PLAYER_ACCURACY_FINAL, 1.0);
		}

		// Ice elemental (Royal Titans) Fire elemental (Royal Titans)
		if (Constants.TITAN_ELEMENTAL_IDS.contains(monsterId) && CombatStyle.MAGIC.equals(styleType()))
		{
			double accuracy = Math.min(1.0, Math.max(0, player.getOffensive().getMagic()) / 100.0 + 0.3);
			if (isWearingEliteMagicVoid() || isWearingMagicVoid())
			{
				accuracy = Math.min(1.0, accuracy * 1.45);
			}
			track(DetailKey.PLAYER_ACCURACY_ROYAL_TITAN_ELEMENTAL, accuracy);
			return track(DetailKey.PLAYER_ACCURACY_FINAL, accuracy);
		}

		// Eclipse Moon clone phase
		if (Constants.ECLIPSE_MOON_IDS.contains(monsterId) && "Clone".equals(monster.getVersion())
			&& isUsingMeleeStyle())
		{
			return track(DetailKey.PLAYER_ACCURACY_FINAL, 1.0);
		}

		if (CombatStyle.MAGIC.equals(styleType()) && Constants.ALWAYS_MAX_HIT_MONSTERS_MAGIC.contains(monsterId))
		{
			return track(DetailKey.PLAYER_ACCURACY_FINAL, 1.0);
		}
		if (CombatStyle.RANGED.equals(styleType()) && Constants.ALWAYS_MAX_HIT_MONSTERS_RANGED.contains(monsterId))
		{
			return track(DetailKey.PLAYER_ACCURACY_FINAL, 1.0);
		}
		if (isUsingMeleeStyle() && Constants.ALWAYS_MAX_HIT_MONSTERS_MELEE.contains(monsterId))
		{
			return track(DetailKey.PLAYER_ACCURACY_FINAL, 1.0);
		}

		if (opts.usingSpecialAttack && wearing("Voidwaker", "Dawnbringer"))
		{
			return track(DetailKey.PLAYER_ACCURACY_FINAL, 1.0);
		}

		if (opts.usingSpecialAttack && (wearing("Seercull") || isWearingMlb()))
		{
			if (isAmmoInvalid())
			{
				return track(DetailKey.PLAYER_ACCURACY_FINAL, 0.0);
			}
			return track(DetailKey.PLAYER_ACCURACY_FINAL, 1.0);
		}

		int atk = getMaxAttackRoll();
		long def = getNPCDefenceRoll();

		double hitChance = track(DetailKey.PLAYER_ACCURACY_BASE,
			BaseCalc.getNormalAccuracyRoll(atk, def));

		boolean fangAccuracy = isWearingFang() && CombatStyle.STAB.equals(styleType());
		boolean drygoreAccuracy = wearing("Drygore blowpipe") && !CombatStyle.MANUAL_CAST.equals(styleStance());
		if (fangAccuracy || drygoreAccuracy)
		{
			if (fangAccuracy && Constants.TOMBS_OF_AMASCUT_MONSTER_IDS.contains(monsterId))
			{
				hitChance = track(DetailKey.PLAYER_ACCURACY_FANG_TOA,
					1 - Math.pow(1 - hitChance, 2));
			}
			else
			{
				hitChance = track(DetailKey.PLAYER_ACCURACY_FANG,
					BaseCalc.getFangAccuracyRoll(atk, def));
			}
		}

		EquipmentPiece weapon = player.getEquipment().getWeapon();
		boolean twoHanded = weapon != null && weapon.isTwoHanded();
		if (wearing("Confliction gauntlets") && CombatStyle.MAGIC.equals(styleType()) && !twoHanded)
		{
			hitChance = track(DetailKey.PLAYER_ACCURACY_CONFLICTION_GAUNTLETS,
				BaseCalc.getConflictionGauntletsAccuracyRoll(atk, def));
		}

		return track(DetailKey.PLAYER_ACCURACY_FINAL, hitChance);
	}

	/**
	 * Port of {@code getPlayerMaxMeleeHit} (PlayerVsNPCCalc.ts:342-543). Computes the player's melee
	 * {@code [min, max]} hit, applying — in upstream order — the strength prayers, Soulreaper-axe stacks,
	 * stance bonus, melee void, the base max from effective strength, then the ordered damage
	 * multipliers and additive bonuses (Crystal blessing, the don't-stack avarice/salve/black-mask
	 * group, demonbane weapons, Obsidian, dragonbane, keris, golembane, rev weapon, silverlight/infernal
	 * demonbane, leaf-bladed, colossal blade, ratbane, Inquisitor crush, Fang shrink, the spec
	 * multipliers, and the Respiratory-system min hit). Returns {@link MinMax}.
	 *
	 * <p>Deviations: the leagues talent branches (min-hit distance, percentage-max-hit distance, melee
	 * damage %, blindbag) are dropped per the class-level leagues deviation.
	 */
	private MinMax getPlayerMaxMeleeHit()
	{
		String stanceType = styleType();
		String stance = styleStance();
		Buffs buffs = player.getBuffs();

		int baseLevel = trackAdd(DetailKey.DAMAGE_LEVEL,
			player.getSkills().getStr(), player.getBoosts().getStr());
		int effectiveLevel = baseLevel;

		for (PrayerData p : getCombatPrayers(PrayerFilter.STRENGTH))
		{
			if ("Burst of Strength".equals(p.name()) && effectiveLevel <= 20)
			{
				effectiveLevel = trackAdd(DetailKey.DAMAGE_LEVEL_PRAYER, effectiveLevel, 1);
			}
			else
			{
				effectiveLevel = trackFactor(DetailKey.DAMAGE_LEVEL_PRAYER, effectiveLevel,
					p.factorStrength().numerator(), p.factorStrength().divisor());
			}
		}

		if (wearing("Soulreaper axe") && !opts.usingSpecialAttack)
		{
			// does not stack multiplicatively with prayers
			int stacks = Math.max(0, Math.min(5, buffs.getSoulreaperStacks()));
			int bonus = trackFactor(DetailKey.DAMAGE_LEVEL_SOULREAPER_BONUS, baseLevel, stacks * 6, 100);
			effectiveLevel = trackAdd(DetailKey.DAMAGE_LEVEL_SOULREAPER, effectiveLevel, bonus);
		}

		int stanceBonus = 8;
		if (CombatStyle.AGGRESSIVE.equals(stance))
		{
			stanceBonus += 3;
		}
		else if (CombatStyle.CONTROLLED.equals(stance))
		{
			stanceBonus += 1;
		}

		effectiveLevel = trackAdd(DetailKey.DAMAGE_EFFECTIVE_LEVEL, effectiveLevel, stanceBonus);

		if (isWearingMeleeVoid())
		{
			effectiveLevel = trackFactor(DetailKey.DAMAGE_EFFECTIVE_LEVEL_VOID, effectiveLevel, 11, 10);
		}

		int gearBonus = trackAdd(DetailKey.DAMAGE_GEAR_BONUS, player.getBonuses().getStr(), 64);
		int baseMax = trackMaxHitFromEffective(DetailKey.MAX_HIT_BASE, effectiveLevel, gearBonus);
		int minHit = 0;
		int maxHit = baseMax;

		if (wearing("Crystal blessing"))
		{
			int crystalPieces = (wearing("Crystal helm") ? 1 : 0)
				+ (wearing("Crystal legs") ? 2 : 0)
				+ (wearing("Crystal body") ? 3 : 0);
			maxHit = (int) ((long) maxHit * (40 + crystalPieces) / 40);
		}

		// Specific bonuses that are applied from equipment
		List<MonsterAttribute> mattrs = attributes();

		// These bonuses do not stack with each other
		if (wearing("Amulet of avarice") && monster.getName() != null && monster.getName().startsWith("Revenant"))
		{
			int num = buffs.isForinthrySurge() ? 27 : 24;
			maxHit = trackFactor(DetailKey.MAX_HIT_FORINTHRY_SURGE, maxHit, num, 20);
		}
		else if (wearing("Salve amulet (e)", "Salve amulet(ei)") && mattrs.contains(MonsterAttribute.UNDEAD))
		{
			maxHit = trackFactor(DetailKey.MAX_HIT_SALVE, maxHit, 6, 5);
		}
		else if (wearing("Salve amulet", "Salve amulet(i)") && mattrs.contains(MonsterAttribute.UNDEAD))
		{
			maxHit = trackFactor(DetailKey.MAX_HIT_SALVE, maxHit, 7, 6);
		}
		else if (isWearingBlackMask() && isSlayerMonster() && buffs.isOnSlayerTask())
		{
			maxHit = trackFactor(DetailKey.MAX_HIT_BLACK_MASK, maxHit, 7, 6);
		}

		if (wearing("Arclight", "Emberlight") && mattrs.contains(MonsterAttribute.DEMON))
		{
			maxHit = applyDemonbane(DetailKey.MAX_HIT_DEMONBANE, maxHit, 70);
		}
		if (wearing("Bone claws", "Burning claws") && mattrs.contains(MonsterAttribute.DEMON))
		{
			maxHit = applyDemonbane(DetailKey.MAX_HIT_DEMONBANE, maxHit, 5);
		}
		if (isWearingTzhaarWeapon() && isWearingObsidian())
		{
			int obsidianBonus = trackFactor(DetailKey.MAX_HIT_OBSIDIAN, baseMax, 1, 10);
			maxHit = trackAdd(DetailKey.MAX_HIT_OBSIDIAN, maxHit, obsidianBonus);
		}
		if (wearing("Dragon hunter lance") && mattrs.contains(MonsterAttribute.DRAGON))
		{
			maxHit = trackFactor(DetailKey.MAX_HIT_DRAGONHUNTER, maxHit, 6, 5);
		}
		if (wearing("Dragon hunter wand") && mattrs.contains(MonsterAttribute.DRAGON))
		{
			// still applies to dhw when wand bashing
			maxHit = trackFactor(DetailKey.MAX_HIT_DRAGONHUNTER, maxHit, 7, 5);
		}
		if (isWearingKeris() && mattrs.contains(MonsterAttribute.KALPHITE))
		{
			if (wearing("Keris partisan of amascut"))
			{
				maxHit = trackFactor(DetailKey.MAX_HIT_KERIS, maxHit, 115, 100);
			}
			else
			{
				maxHit = trackFactor(DetailKey.MAX_HIT_KERIS, maxHit, 133, 100);
			}
		}
		if (wearing("Barronite mace") && mattrs.contains(MonsterAttribute.GOLEM))
		{
			maxHit = trackFactor(DetailKey.MAX_HIT_GOLEMBANE, maxHit, 23, 20);
		}
		if (wearing("Granite hammer") && mattrs.contains(MonsterAttribute.GOLEM))
		{
			maxHit = trackFactor(DetailKey.MAX_HIT_GOLEMBANE, maxHit, 13, 10);
		}
		if (isRevWeaponBuffApplicable())
		{
			maxHit = trackFactor(DetailKey.MAX_HIT_REV_WEAPON, maxHit, 3, 2);
		}
		if (wearing("Silverlight", "Darklight", "Silverlight (dyed)") && mattrs.contains(MonsterAttribute.DEMON))
		{
			maxHit = applyDemonbane(DetailKey.MAX_HIT_DEMONBANE, maxHit, 60);
		}
		if (wearing("Infernal tecpatl") && mattrs.contains(MonsterAttribute.DEMON))
		{
			maxHit = applyDemonbane(DetailKey.MAX_HIT_DEMONBANE, maxHit, 10);
		}

		if (wearing("Leaf-bladed battleaxe") && mattrs.contains(MonsterAttribute.LEAFY))
		{
			maxHit = trackFactor(DetailKey.MAX_HIT_LEAFY, maxHit, 47, 40);
		}
		if (wearing("Colossal blade"))
		{
			maxHit = trackAdd(DetailKey.MAX_HIT_COLOSSALBLADE, maxHit, Math.min(monster.getSize() * 2, 10));
		}

		if (isWearingRatBoneWeapon() && mattrs.contains(MonsterAttribute.RAT))
		{
			// applies before inq, tested 2024-01-25, str level 99 str gear 112
			maxHit = trackAdd(DetailKey.MAX_HIT_RATBANE, maxHit, 10);
		}
		// Inquisitor's armour set gives bonuses when using the crush attack style
		if (CombatStyle.CRUSH.equals(stanceType))
		{
			int inqPieces = 0;
			for (String v : allEquippedItems)
			{
				if ("Inquisitor's great helm".equals(v)
					|| "Inquisitor's hauberk".equals(v)
					|| "Inquisitor's plateskirt".equals(v))
				{
					inqPieces++;
				}
			}

			if (inqPieces > 0)
			{
				if (wearing("Inquisitor's mace"))
				{
					// 2.5% per piece, no full-set bonus
					inqPieces *= 5;
				}
				else if (inqPieces == 3)
				{
					// 1.0% extra for full set when not using inq mace
					inqPieces = 5;
				}
				maxHit = trackFactor(DetailKey.MAX_HIT_INQ, maxHit, 200 + inqPieces, 200);
			}
		}

		if (isWearingFang())
		{
			int shrink = (int) ((long) maxHit * 3 / 20);
			minHit = track(DetailKey.MIN_HIT_FANG, shrink);
			if (opts.usingSpecialAttack)
			{
				// not reduced during spec, but min hit is changed as usual
				track(DetailKey.MAX_HIT_SPEC, maxHit);
			}
			else
			{
				maxHit = trackAdd(DetailKey.MAX_HIT_FANG, maxHit, -shrink);
			}
		}

		if (opts.usingSpecialAttack)
		{
			if (isWearingGodsword())
			{
				maxHit = trackFactor(DetailKey.MAX_HIT_GODSWORD_SPEC, maxHit, 11, 10);
			}

			if (wearing("Bandos godsword", "Saradomin sword"))
			{
				maxHit = trackFactor(DetailKey.MAX_HIT_SPEC, maxHit, 11, 10);
			}
			else if (wearing("Armadyl godsword", "Dragon sword", "Dragon longsword", "Saradomin's blessed sword"))
			{
				maxHit = trackFactor(DetailKey.MAX_HIT_SPEC, maxHit, 5, 4);
			}
			else if (wearing("Dragon mace", "Dragon warhammer", "Arkan blade"))
			{
				maxHit = trackFactor(DetailKey.MAX_HIT_SPEC, maxHit, 3, 2);
			}
			else if (wearing("Voidwaker"))
			{
				minHit = trackFactor(DetailKey.MIN_HIT_SPEC, maxHit, 1, 2);
				maxHit = trackAdd(DetailKey.MAX_HIT_SPEC, maxHit, minHit);
			}
			else if (wearing("Dragon halberd", "Crystal halberd"))
			{
				maxHit = trackFactor(DetailKey.MAX_HIT_SPEC, maxHit, 11, 10);
			}
			else if (wearing("Dragon dagger"))
			{
				maxHit = trackFactor(DetailKey.MAX_HIT_SPEC, maxHit, 23, 20);
			}
			else if (wearing("Abyssal dagger"))
			{
				maxHit = trackFactor(DetailKey.MAX_HIT_SPEC, maxHit, 17, 20);
			}
			else if (wearing("Abyssal bludgeon"))
			{
				int prayerMissing = Math.max(-player.getBoosts().getPrayer(), 0);
				maxHit = trackFactor(DetailKey.MAX_HIT_SPEC, maxHit, 100 + (prayerMissing / 2), 100);
			}
			else if (wearing("Barrelchest anchor"))
			{
				maxHit = trackFactor(DetailKey.MAX_HIT_SPEC, maxHit, 110, 100);
			}
			else if (isWearingBloodMoonSet())
			{
				minHit = trackFactor(DetailKey.MIN_HIT_SPEC, maxHit, 1, 4);
				maxHit = trackAdd(DetailKey.MAX_HIT_SPEC, maxHit, minHit);
			}
			else if (wearing("Soulreaper axe"))
			{
				int stacks = Math.max(0, Math.min(5, player.getBuffs().getSoulreaperStacks()));
				maxHit = trackFactor(DetailKey.MAX_HIT_SPEC, maxHit, 100 + 6 * stacks, 100);
			}
		}

		if ("Respiratory system".equals(monster.getName()))
		{
			minHit = trackAdd(DetailKey.REPIRATORY_SYSTEM_MIN_HIT, minHit, (int) ((long) maxHit / 2));
		}

		return new MinMax(minHit, maxHit);
	}

	/**
	 * Port of {@code getMinAndMax} (PlayerVsNPCCalc.ts:1163-1200). Dispatches by combat-style type;
	 * melee is ported (the ranged/magic branches still throw {@link UnsupportedOperationException} until
	 * v0.1.3). The {@code overrides.maxHit} short-circuit is dropped (no overrides in this model).
	 */
	public MinMax getMinAndMax()
	{
		if (!CombatStyle.MANUAL_CAST.equals(styleStance()) && isAmmoInvalid())
		{
			return new MinMax(0, 0);
		}

		String style = styleType();

		int min = 0;
		int max = 0;
		if (isUsingMeleeStyle())
		{
			MinMax mm = getPlayerMaxMeleeHit();
			min = mm.min();
			max = mm.max();
		}
		if (CombatStyle.RANGED.equals(style))
		{
			MinMax mm = getPlayerMaxRangedHit();
			min = mm.min();
			max = mm.max();
		}
		if (CombatStyle.MAGIC.equals(style))
		{
			MinMax mm = getPlayerMaxMagicHit();
			min = mm.min();
			max = mm.max();
		}

		if (min > max)
		{
			max = min;
		}

		// some cursed (literally, cursed amulet of magic) stuff throws this off
		if (min <= 0)
		{
			min = 0;
		}
		if (max <= 0)
		{
			max = 0;
		}

		track(DetailKey.MIN_HIT_FINAL, min);
		track(DetailKey.MAX_HIT_FINAL, max);
		return new MinMax(min, max);
	}

	public int getMax()
	{
		throw new UnsupportedOperationException(NOT_PORTED);
	}

	public int getMaxHit()
	{
		return getMinAndMax().max();
	}

	/**
	 * Port of {@code getPlayerMaxRangedHit} (PlayerVsNPCCalc.ts:656-847). Computes the player's ranged
	 * {@code [min, max]} hit, applying — in upstream order — the str-scaling weapons (Eclipse atlatl /
	 * Hunter's spear), the Holy-water early-return, the special-attack MSB/MLB/Seercull/Ogre-bow
	 * early-return (+10, ammo-only str), the ranged strength prayers (Sharp Eye force-1 case), the
	 * Accurate stance +3, ranged void ({@code 9/8} elite, {@code 11/10} normal), the base max from
	 * effective strength, then the ordered ranged damage multipliers (Crystal armour+bow, the
	 * don't-stack avarice/salve/imbued-black-mask group, Twisted bow scaling, rev weapon, dragon hunter
	 * crossbow, Scorching bow demonbane, ratbane, Tonalztics, the spec multipliers, the P2-Warden
	 * damage modifier, and the Respiratory-system min hit). Returns {@link MinMax}.
	 *
	 * <p>Deviations: the leagues talent branches (talent-buffed ranged prayers, crossbow-slow-big-hits,
	 * percentage-ranged-damage, bow min/max-hit stacking) are dropped per the class-level leagues
	 * deviation. Bolt-enchant proc damage (ruby/diamond/etc.) is NOT here — it lives in the distribution
	 * pipeline ({@code dists/bolts.ts}, v0.1.4).
	 */
	private MinMax getPlayerMaxRangedHit()
	{
		String stance = styleStance();

		boolean scalesWithStr = wearing("Eclipse atlatl", "Hunter's spear");
		int effectiveLevel = scalesWithStr
			? player.getSkills().getStr() + player.getBoosts().getStr()
			: player.getSkills().getRanged() + player.getBoosts().getRanged();
		track(DetailKey.DAMAGE_LEVEL, effectiveLevel);

		List<MonsterAttribute> mattrs = attributes();

		if (wearing("Holy water"))
		{
			if (!mattrs.contains(MonsterAttribute.DEMON))
			{
				// can't be used against non-demons
				return new MinMax(0, 0);
			}

			// similar to msb + mlb + seercull below
			effectiveLevel = trackAdd(DetailKey.DAMAGE_EFFECTIVE_LEVEL_HOLY_WATER, effectiveLevel, 10);

			EquipmentPiece weapon = player.getEquipment().getWeapon();
			int str = 64 + (weapon == null ? 0 : weapon.getBonuses().getRangedStr());
			int maxHit = trackMaxHitFromEffective(DetailKey.MAX_HIT_BASE, effectiveLevel, str);

			if (mattrs.contains(MonsterAttribute.DEMON))
			{
				maxHit = applyDemonbane(DetailKey.MAX_HIT_DEMONBANE, maxHit, 60);
			}
			if ("Nezikchened".equals(monster.getName()))
			{
				maxHit = trackAdd(DetailKey.MAX_HIT_NEZIKCHENED, maxHit, 5);
			}

			return new MinMax(0, maxHit);
		}

		if ((opts.usingSpecialAttack && (isWearingMsb() || isWearingMlb() || wearing("Seercull")))
			|| isWearingOgreBow())
		{
			// why +10 when that's not used anywhere else? who knows
			effectiveLevel += 10;

			// ignores other gear
			EquipmentPiece ammo = player.getEquipment().getAmmo();
			int bonusStr = ammo == null ? 0 : ammo.getBonuses().getRangedStr();
			int maxHit = (int) (((long) effectiveLevel * (bonusStr + 64) + 320) / 640);

			// end early, it ignores all other gear and bonuses
			return new MinMax(0, maxHit);
		}

		for (PrayerData p : getCombatPrayers(PrayerFilter.STRENGTH))
		{
			int num = p.factorStrength().numerator();
			int div = p.factorStrength().divisor();
			if ("Sharp Eye".equals(p.name()) && (int) ((long) effectiveLevel * num / div) == effectiveLevel)
			{
				// force 1 level gain
				effectiveLevel = trackAdd(DetailKey.DAMAGE_LEVEL_PRAYER, effectiveLevel, 1);
			}
			else
			{
				effectiveLevel = trackFactor(DetailKey.DAMAGE_LEVEL_PRAYER, effectiveLevel, num, div);
			}
		}

		if (CombatStyle.ACCURATE.equals(stance))
		{
			effectiveLevel += 3;
		}

		effectiveLevel += 8;

		if (isWearingEliteRangedVoid())
		{
			effectiveLevel = (int) ((long) effectiveLevel * 9 / 8);
		}
		else if (isWearingRangedVoid())
		{
			effectiveLevel = (int) ((long) effectiveLevel * 11 / 10);
		}

		int bonusStr = scalesWithStr ? player.getBonuses().getStr() : player.getBonuses().getRangedStr();
		int baseMax = trackMaxHitFromEffective(DetailKey.MAX_HIT_BASE, effectiveLevel, 64 + bonusStr);
		int minHit = 0;
		int maxHit = baseMax;

		// tested this in-game, slayer helmet (i) + crystal legs + crystal body + bowfa, on accurate, no rigour, 99 ranged
		// max hit is 36, but would be 37 if placed after slayer helm
		if (isWearingCrystalBow())
		{
			int crystalPieces = (wearing("Crystal helm") ? 1 : 0)
				+ (wearing("Crystal legs") ? 2 : 0)
				+ (wearing("Crystal body") ? 3 : 0);
			maxHit = (int) ((long) maxHit * (40 + crystalPieces) / 40);
		}

		boolean needRevWeaponBonus = isRevWeaponBuffApplicable();
		boolean needDragonbane = wearing("Dragon hunter crossbow") && mattrs.contains(MonsterAttribute.DRAGON);
		boolean needDemonbane = wearing("Scorching bow") && mattrs.contains(MonsterAttribute.DEMON);

		// Specific bonuses that are applied from equipment
		Buffs buffs = player.getBuffs();
		if (wearing("Amulet of avarice") && monster.getName() != null && monster.getName().startsWith("Revenant"))
		{
			int num = buffs.isForinthrySurge() ? 27 : 24;
			maxHit = trackFactor(DetailKey.MAX_HIT_FORINTHRY_SURGE, maxHit, num, 20);
		}
		else if ((wearing("Salve amulet(ei)") || (scalesWithStr && wearing("Salve amulet (e)")))
			&& mattrs.contains(MonsterAttribute.UNDEAD))
		{
			maxHit = (int) ((long) maxHit * 6 / 5);
		}
		else if ((wearing("Salve amulet(i)") || (scalesWithStr && wearing("Salve amulet")))
			&& mattrs.contains(MonsterAttribute.UNDEAD))
		{
			maxHit = (int) ((long) maxHit * 7 / 6);
		}
		else if (scalesWithStr && isWearingBlackMask() && isSlayerMonster() && buffs.isOnSlayerTask())
		{
			maxHit = (int) ((long) maxHit * 7 / 6);
		}
		else if (isWearingImbuedBlackMask() && isSlayerMonster() && buffs.isOnSlayerTask())
		{
			int numerator = 23;
			// these are additive with slayer only
			if (needRevWeaponBonus)
			{
				needRevWeaponBonus = false;
				numerator += 10;
			}
			if (needDragonbane)
			{
				needDragonbane = false;
				numerator += 5;
			}
			if (needDemonbane)
			{
				needDemonbane = false;
				numerator += 6;
			}
			maxHit = trackFactor(DetailKey.MAX_HIT_BLACK_MASK, maxHit, numerator, 20);
		}

		if (wearing("Twisted bow"))
		{
			int cap = mattrs.contains(MonsterAttribute.XERICIAN) ? 350 : 250;
			int tbowMagic = Math.min(cap, Math.max(monster.getSkills().getMagic(), monster.getOffensive().getMagic()));
			maxHit = tbowScaling(maxHit, tbowMagic, false);
		}

		// multiplicative if not with slayer helm
		if (needRevWeaponBonus)
		{
			maxHit = (int) ((long) maxHit * 3 / 2);
		}
		if (needDragonbane)
		{
			maxHit = (int) ((long) maxHit * 5 / 4);
		}
		if (needDemonbane)
		{
			maxHit = applyDemonbane(DetailKey.MAX_HIT_DEMONBANE, maxHit, 30);
		}

		if (isWearingRatBoneWeapon() && mattrs.contains(MonsterAttribute.RAT))
		{
			maxHit = trackAdd(DetailKey.MAX_HIT_RATBANE, maxHit, 10);
		}

		if (wearing("Tonalztics of ralos"))
		{
			// rolls 75% of max hit, but can hit twice
			// double hit is implemented in hit distribution
			maxHit = trackFactor(DetailKey.MAX_HIT_TONALZTICS, maxHit, 3, 4);
		}

		if (opts.usingSpecialAttack)
		{
			if (isWearingBlowpipe())
			{
				maxHit = trackFactor(DetailKey.MAX_HIT_SPEC, maxHit, 3, 2);
			}
			else if (wearing("Webweaver bow"))
			{
				int maxReduction = (int) ((long) maxHit * 6 / 10);
				maxHit = trackAdd(DetailKey.MAX_HIT_SPEC, maxHit, -maxReduction);
			}
			else if (wearing("Heavy ballista", "Light ballista"))
			{
				maxHit = trackFactor(DetailKey.MAX_HIT_SPEC, maxHit, 5, 4);
			}
			else if (wearing("Rosewood blowpipe"))
			{
				maxHit = trackFactor(DetailKey.MAX_HIT_SPEC, maxHit, 11, 10);
			}
		}

		if (opts.usingSpecialAttack)
		{
			if (wearing("Dark bow"))
			{
				boolean descentOfDragons = wearing("Dragon arrow");
				minHit = track(DetailKey.MIN_HIT_SPEC, descentOfDragons ? 8 : 5);
				int dmgFactor = descentOfDragons ? 15 : 13;
				maxHit = trackFactor(DetailKey.MAX_HIT_SPEC, maxHit, dmgFactor, 10);
			}
		}

		if (Constants.P2_WARDEN_IDS_SET.contains(monster.getId()))
		{
			MinMax warded = applyP2WardensDamageModifier(maxHit);
			minHit = warded.min();
			maxHit = warded.max();
		}

		if ("Respiratory system".equals(monster.getName()))
		{
			minHit = trackAdd(DetailKey.REPIRATORY_SYSTEM_MIN_HIT, minHit, (int) ((long) maxHit / 2));
		}

		return new MinMax(minHit, maxHit);
	}

	/**
	 * Port of {@code applyP2WardensDamageModifier} (PlayerVsNPCCalc.ts:2709-2730). Takes only the max
	 * hit (the destructured min is ignored upstream) and returns the lerp-scaled {@code [min, max]}.
	 */
	private MinMax applyP2WardensDamageModifier(int max)
	{
		// 1/3 of enemy defence is removed from accuracy
		long reducedNpcDefence = getNPCDefenceRoll() / 3;
		int accuracyDelta = track(DetailKey.WARDENS_ACCURACY_DELTA,
			(int) Math.max(getMaxAttackRoll() - reducedNpcDefence, 0));

		// remaining accuracy provides a % dmg modifier from 15% - 40% based on lerp from 0 to 42k MAR
		int modifier = track(DetailKey.WARDENS_DMG_MODIFIER,
			Math.max(Math.min(CalcMath.iLerp(15, 40, 0, 42_000, accuracyDelta), 40), 15));

		int maxPctRange = 20;
		return new MinMax(
			// these apply the % separately
			// in effect, we're dealing between [15-35, 40-60]% of normal damage
			track(DetailKey.MIN_HIT_WARDENS, (int) ((long) max * modifier / 100)),
			track(DetailKey.MAX_HIT_WARDENS, (int) ((long) max * (modifier + maxPctRange) / 100)));
	}

	/**
	 * Port of {@code getPlayerMaxMagicHit} (PlayerVsNPCCalc.ts:960-1139). Computes the player's magic
	 * {@code [min, max]} hit: the base max from {@link #getSpellMaxHit(Spell, int)} (with the Magic Dart
	 * special), or the powered-staff if/else chain (Starter staff, the tridents, Thammaron's, Accursed,
	 * Sanguinesti, Dawnbringer, Tumeken's shadow, Eye of ayak, Lithic, Warped, Bone staff, the nightmare
	 * spec staves, crystal/corrupted staves, salamanders), then — in upstream order — the chaos/god
	 * gauntlets flat bolt bonus, the charge-spell bonus, the magic-damage-bonus {@code %} from gear/buffs
	 * ({@code bonuses.magic_str} + smoke staff + salve/avarice + the magic-damage prayers, applied at
	 * {@code /1000}), the powered-staff Crystal-blessing, imbued-black-mask, dragonbane, rev weapon, the
	 * spec multipliers, the spellement weakness bonus, sunfire min hit, the tomes, and the P2-Warden /
	 * Respiratory-system transforms. Returns {@link MinMax}.
	 *
	 * <p>Deviations: the leagues magic-attack-speed-powered branch (a {@code -8} max-hit adjustment) is
	 * dropped per the class-level leagues deviation.
	 */
	private MinMax getPlayerMaxMagicHit()
	{
		int minHit = 0;
		int maxHit = 0;
		int magicLevel = player.getSkills().getMagic() + player.getBoosts().getMagic();
		Spell spell = player.getSpell();

		// Specific bonuses that are applied from equipment
		List<MonsterAttribute> mattrs = attributes();
		Buffs buffs = player.getBuffs();

		if (spell != null)
		{
			maxHit = getSpellMaxHit(spell, magicLevel);
			if ("Magic Dart".equals(spell.getName()))
			{
				if (wearing("Slayer's staff (e)") && isSlayerMonster() && buffs.isOnSlayerTask())
				{
					maxHit = (int) (13 + (long) magicLevel / 6);
				}
				else
				{
					maxHit = (int) (10 + (long) magicLevel / 10);
				}
			}
		}
		else if (wearing("Starter staff"))
		{
			maxHit = 8;
		}
		else if (wearing("Trident of the seas", "Trident of the seas (e)"))
		{
			maxHit = Math.max(1, (int) ((long) magicLevel / 3 - 5));
		}
		else if (wearing("Thammaron's sceptre"))
		{
			maxHit = Math.max(1, (int) ((long) magicLevel / 3 - 8));
		}
		else if (wearing("Accursed sceptre") || (wearing("Accursed sceptre (a)") && opts.usingSpecialAttack))
		{
			maxHit = Math.max(1, (int) ((long) magicLevel / 3 - 6));
		}
		else if (wearing("Trident of the swamp", "Trident of the swamp (e)"))
		{
			maxHit = Math.max(1, (int) ((long) magicLevel / 3 - 2));
		}
		else if (wearing("Sanguinesti staff", "Holy sanguinesti staff"))
		{
			maxHit = Math.max(1, (int) ((long) magicLevel / 3 - 1));
		}
		else if (wearing("Dawnbringer"))
		{
			maxHit = Math.max(1, (int) ((long) magicLevel / 6 - 1));
			if (opts.usingSpecialAttack)
			{
				// guaranteed hit between 75-150, ignores bonuses
				return new MinMax(75, 150);
			}
		}
		else if (wearing("Tumeken's shadow"))
		{
			maxHit = Math.max(1, (int) ((long) magicLevel / 3) + 1);
		}
		else if (wearing("Eye of ayak"))
		{
			maxHit = Math.max(1, (int) ((long) magicLevel / 3) - 6);
		}
		else if (wearing("Lithic sceptre"))
		{
			maxHit = Math.max(10, (int) ((long) magicLevel / 3) - 10);
		}
		else if (wearing("Warped sceptre"))
		{
			maxHit = Math.max(1, (int) ((8L * magicLevel + 96) / 37));
		}
		else if (wearing("Bone staff"))
		{
			// although the +10 is technically a ratbane bonus, the weapon can't be used against non-rats
			// and shows this max hit against the combat dummy as well
			maxHit = Math.max(1, (int) ((long) magicLevel / 3) - 5) + 10;
		}
		else if (wearing("Eldritch nightmare staff") && opts.usingSpecialAttack)
		{
			maxHit = Math.max(1, Math.min(44, (int) ((99L + 44 * magicLevel) / 99)));
		}
		else if (wearing("Volatile nightmare staff") && opts.usingSpecialAttack)
		{
			maxHit = Math.max(1, Math.min(58, (int) ((99L + 58 * magicLevel) / 99)));
		}
		else if (wearing("Crystal staff (basic)", "Corrupted staff (basic)"))
		{
			maxHit = 23;
		}
		else if (wearing("Crystal staff (attuned)", "Corrupted staff (attuned)"))
		{
			maxHit = 31;
		}
		else if (wearing("Crystal staff (perfected)", "Corrupted staff (perfected)"))
		{
			maxHit = 39;
		}
		else if (wearing("Swamp lizard"))
		{
			maxHit = (int) (((long) magicLevel * (56 + 64) + 320) / 640);
		}
		else if (wearing("Orange salamander"))
		{
			maxHit = (int) (((long) magicLevel * (59 + 64) + 320) / 640);
		}
		else if (wearing("Red salamander"))
		{
			maxHit = (int) (((long) magicLevel * (77 + 64) + 320) / 640);
		}
		else if (wearing("Black salamander"))
		{
			maxHit = (int) (((long) magicLevel * (92 + 64) + 320) / 640);
		}
		else if (wearing("Tecu salamander"))
		{
			maxHit = (int) (((long) magicLevel * (104 + 64) + 320) / 640);
		}

		if (maxHit == 0)
		{
			// at this point either they've selected a 0-dmg spell
			// or they picked a staff-casting option without choosing a spell
			return new MinMax(0, 0);
		}
		track(DetailKey.MAX_HIT_BASE, maxHit);

		if (opts.usingSpecialAttack && wearing("Eye of ayak"))
		{
			maxHit = trackFactor(DetailKey.MAX_HIT_SPEC, maxHit, 13, 10);
		}

		if (wearing("Chaos gauntlets") && spell != null && spell.getName() != null
			&& spell.getName().toLowerCase().contains("bolt"))
		{
			maxHit += 3;
		}
		if (isChargeSpellApplicable())
		{
			maxHit += 10;
		}

		// We need the basehit value for the elemental bonus later.
		int baseMax = maxHit;
		int magicDmgBonus = player.getBonuses().getMagicStr();

		if (isWearingSmokeStaff() && spell != null && spell.getSpellbook() == Spell.Spellbook.STANDARD)
		{
			magicDmgBonus += 100;
		}

		boolean blackMaskBonus = false;
		if (wearing("Salve amulet(ei)") && mattrs.contains(MonsterAttribute.UNDEAD))
		{
			magicDmgBonus += 200;
		}
		else if (wearing("Salve amulet(i)") && mattrs.contains(MonsterAttribute.UNDEAD))
		{
			magicDmgBonus += 150;
		}
		else if (wearing("Amulet of avarice") && monster.getName() != null
			&& monster.getName().startsWith("Revenant"))
		{
			magicDmgBonus += buffs.isForinthrySurge() ? 350 : 200;
		}
		else if (isWearingImbuedBlackMask() && isSlayerMonster() && buffs.isOnSlayerTask())
		{
			blackMaskBonus = true;
		}

		for (PrayerData p : getCombatPrayers(PrayerFilter.MAGIC_DAMAGE_BONUS))
		{
			magicDmgBonus += p.magicDamageBonus().intValue();
		}

		maxHit = trackAddFactor(DetailKey.MAX_HIT_MAGIC_DMG, maxHit, magicDmgBonus, 1000);

		EquipmentPiece weapon = player.getEquipment().getWeapon();
		boolean usingPoweredStaff = weapon != null
			&& weapon.getCategory() == EquipmentCategory.POWERED_STAFF
			&& !CombatStyle.MANUAL_CAST.equals(styleStance());
		if (usingPoweredStaff && wearing("Crystal blessing"))
		{
			int crystalPieces = (wearing("Crystal helm") ? 1 : 0)
				+ (wearing("Crystal legs") ? 2 : 0)
				+ (wearing("Crystal body") ? 3 : 0);
			maxHit = (int) ((long) maxHit * (40 + crystalPieces) / 40);
		}

		if (blackMaskBonus)
		{
			maxHit = (int) ((long) maxHit * 23 / 20);
		}

		if (mattrs.contains(MonsterAttribute.DRAGON))
		{
			// this still applies to dhl and dhcb when autocasting
			if (wearing("Dragon hunter lance"))
			{
				maxHit = trackFactor(DetailKey.MAX_HIT_DRAGONHUNTER, maxHit, 6, 5);
			}
			else if (wearing("Dragon hunter wand"))
			{
				maxHit = trackFactor(DetailKey.MAX_HIT_DRAGONHUNTER, maxHit, 7, 5);
			}
			else if (wearing("Dragon hunter crossbow"))
			{
				maxHit = trackFactor(DetailKey.MAX_HIT_DRAGONHUNTER, maxHit, 5, 4);
			}
		}

		if (isRevWeaponBuffApplicable())
		{
			maxHit = (int) ((long) maxHit * 3 / 2);
		}

		if (opts.usingSpecialAttack)
		{
			if (isWearingAccursedSceptre())
			{
				maxHit = trackFactor(DetailKey.MAX_HIT_SPEC, maxHit, 3, 2);
			}
		}

		Spellement spellement = getSpellement();
		Weakness weakness = getMonsterWeakness();
		if (spellement != null && weakness != null && spellement == weakness.getElement())
		{
			int bonus = trackFactor(DetailKey.MAX_HIT_SPELLEMENT_BONUS, baseMax,
				weakness.getSeverity(), 100);
			maxHit = trackAdd(DetailKey.MAX_HIT_SPELLEMENT, maxHit, bonus);
		}

		if (buffs.isUsingSunfireRunes() && canUseSunfireRunes(spell))
		{
			// sunfire runes are applied pre-tome
			minHit = trackFactor(DetailKey.MIN_HIT_SUNFIRE, maxHit, 1, 10);
		}

		EquipmentPiece shield = player.getEquipment().getShield();
		boolean shieldCharged = shield != null && "Charged".equals(shield.getVersion());
		if ((wearing("Tome of fire") && shieldCharged && getSpellement() == Spellement.FIRE)
			|| (wearing("Tome of water") && shieldCharged && getSpellement() == Spellement.WATER)
			|| (wearing("Tome of earth") && shieldCharged && getSpellement() == Spellement.EARTH))
		{
			maxHit = trackFactor(DetailKey.MAX_HIT_TOME, maxHit, 11, 10);
		}

		if (Constants.P2_WARDEN_IDS_SET.contains(monster.getId()))
		{
			MinMax warded = applyP2WardensDamageModifier(maxHit);
			minHit = warded.min();
			maxHit = warded.max();
		}

		if ("Respiratory system".equals(monster.getName()))
		{
			minHit = trackAdd(DetailKey.REPIRATORY_SYSTEM_MIN_HIT, minHit, (int) ((long) maxHit / 2));
		}

		return new MinMax(minHit, maxHit);
	}

	/**
	 * Port of {@code getSpellMaxHit} (src/types/Spell.ts:26-66). Returns {@code spell.max_hit}, except for
	 * the elemental strike/bolt/blast/wave/surge spells whose damage tier is resolved by the player's magic
	 * level via {@link SpellRepository#byName}. The tier thresholds are ported verbatim.
	 */
	private int getSpellMaxHit(Spell spell, int magicLevel)
	{
		if (spell.getElement() == null || "Flames of Cerberus".equals(spell.getName()))
		{
			return spell.getMaxHit();
		}

		String name = spell.getName() == null ? "" : spell.getName();
		String[] parts = name.split(" ");
		String spellClass = parts.length > 1 ? parts[1] : "";
		switch (spellClass)
		{
			case "Strike":
				if (magicLevel >= 13)
				{
					return spellMaxHitByName("Fire " + spellClass);
				}
				if (magicLevel >= 9)
				{
					return spellMaxHitByName("Earth " + spellClass);
				}
				if (magicLevel >= 5)
				{
					return spellMaxHitByName("Water " + spellClass);
				}
				return spellMaxHitByName("Wind " + spellClass);

			case "Bolt":
				if (magicLevel >= 35)
				{
					return spellMaxHitByName("Fire " + spellClass);
				}
				if (magicLevel >= 29)
				{
					return spellMaxHitByName("Earth " + spellClass);
				}
				if (magicLevel >= 23)
				{
					return spellMaxHitByName("Water " + spellClass);
				}
				return spellMaxHitByName("Wind " + spellClass);

			case "Blast":
				if (magicLevel >= 59)
				{
					return spellMaxHitByName("Fire " + spellClass);
				}
				if (magicLevel >= 53)
				{
					return spellMaxHitByName("Earth " + spellClass);
				}
				if (magicLevel >= 47)
				{
					return spellMaxHitByName("Water " + spellClass);
				}
				return spellMaxHitByName("Wind " + spellClass);

			case "Wave":
				if (magicLevel >= 75)
				{
					return spellMaxHitByName("Fire " + spellClass);
				}
				if (magicLevel >= 70)
				{
					return spellMaxHitByName("Earth " + spellClass);
				}
				if (magicLevel >= 65)
				{
					return spellMaxHitByName("Water " + spellClass);
				}
				return spellMaxHitByName("Wind " + spellClass);

			case "Surge":
				if (magicLevel >= 95)
				{
					return spellMaxHitByName("Fire " + spellClass);
				}
				if (magicLevel >= 90)
				{
					return spellMaxHitByName("Earth " + spellClass);
				}
				if (magicLevel >= 85)
				{
					return spellMaxHitByName("Water " + spellClass);
				}
				return spellMaxHitByName("Wind " + spellClass);

			default:
				throw new IllegalStateException("No dynamic max hit available for " + name);
		}
	}

	/** Looks up the {@code max_hit} of a named elemental spell via the injected {@link SpellRepository}. */
	private int spellMaxHitByName(String name)
	{
		if (spellRepository == null)
		{
			throw new IllegalStateException(
				"SpellRepository required for elemental spell tier lookup: " + name);
		}
		Spell s = spellRepository.byName(name);
		if (s == null)
		{
			throw new IllegalStateException("No spell named " + name);
		}
		return s.getMaxHit();
	}

	/** Port of {@code canUseSunfireRunes} (src/types/Spell.ts:68-70). */
	private static boolean canUseSunfireRunes(Spell spell)
	{
		return spell != null && spell.getElement() == Spellement.FIRE;
	}

	public Object getDistribution()
	{
		throw new UnsupportedOperationException(NOT_PORTED);
	}

	public double getDps()
	{
		throw new UnsupportedOperationException(NOT_PORTED);
	}

	public double getTtk()
	{
		throw new UnsupportedOperationException(NOT_PORTED);
	}

	public PlayerVsNpcCalc getSpecCalc()
	{
		throw new UnsupportedOperationException(NOT_PORTED);
	}
}
