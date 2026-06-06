package org.bossbis.calc;

import java.util.ArrayList;
import java.util.List;
import org.bossbis.calc.CalcMath.MinMax;
import org.bossbis.calc.data.EquipmentRepository;
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
	 * Constructs a calc with default options.
	 *
	 * <p>The {@link EquipmentRepository} is injected (mirroring {@link BaseCalc}) so canonicalization
	 * does not need a global table and {@code main} never does {@code new Gson()}.
	 */
	public PlayerVsNpcCalc(Player player, Monster monster, EquipmentRepository equipmentRepository)
	{
		this(player, monster, new CalcOpts(), equipmentRepository);
	}

	public PlayerVsNpcCalc(Player player, Monster monster, CalcOpts opts, EquipmentRepository equipmentRepository)
	{
		super(player, monster, opts, equipmentRepository);
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
		ACCURACY, STRENGTH
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
			boolean has = filter == PrayerFilter.ACCURACY ? d.factorAccuracy() != null : d.factorStrength() != null;
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
		Integer vulnObj = monster.getInputs() == null ? null : monster.getInputs().getDemonbaneVulnerability();
		int vulnerability = vulnObj == null ? 100 : vulnObj;
		int percent = trackFactor(DetailKey.PLAYER_DEMONBANE_FACTOR, weaponDemonbane, vulnerability, 100);
		return trackAddFactor(DetailKey.PLAYER_ACCURACY_DEMONBANE, attackRoll, percent, 100);
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

	public int getNPCDefenceRoll()
	{
		throw new UnsupportedOperationException(NOT_PORTED);
	}

	public double getHitChance()
	{
		throw new UnsupportedOperationException(NOT_PORTED);
	}

	public double getDisplayHitChance()
	{
		throw new UnsupportedOperationException(NOT_PORTED);
	}

	public MinMax getMinAndMax()
	{
		throw new UnsupportedOperationException(NOT_PORTED);
	}

	public int getMax()
	{
		throw new UnsupportedOperationException(NOT_PORTED);
	}

	public int getMaxHit()
	{
		throw new UnsupportedOperationException(NOT_PORTED);
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
