package org.bossbis.calc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bossbis.calc.data.EquipmentRepository;
import org.bossbis.calc.types.EquipmentPiece;
import org.bossbis.calc.types.EquipmentStats;
import org.bossbis.calc.types.ItemVars;
import org.bossbis.calc.types.Monster;
import org.bossbis.calc.types.Player;
import org.bossbis.calc.types.PlayerBonuses;
import org.bossbis.calc.types.PlayerDefensive;
import org.bossbis.calc.types.PlayerEquipment;
import org.bossbis.calc.types.PlayerOffensive;

/**
 * Port of {@code src/lib/Equipment.ts} — the equipment-bonus aggregation and attack-speed helpers
 * (weirdgloop@5e313c1). Ports {@code calculateEquipmentBonusesFromGear}, {@code calculateAttackSpeed},
 * {@code ammoApplicability} + the {@code ammoForRangedWeapons} table, and the {@code getCanonicalItem}
 * canonicalization (via the injected {@link EquipmentRepository} rather than the upstream global
 * {@code availableEquipment} constant — no {@code new Gson()} in {@code main}).
 *
 * <p>This is an instance class holding the repository so each slot can be canonicalized (alias-fold to
 * its base row) and so the blowpipe-dart item can be looked up by id, exactly mirroring upstream's use
 * of {@code availableEquipment}. {@code BaseCalc} will hold an {@code Equipment} instance.
 *
 * <p>All arithmetic is integer-truncating ({@code Math.trunc} &rarr; {@code (int)}). The Tumeken's
 * Shadow magic-strength cap is {@code Math.min(value, 1000)} as upstream.
 *
 * <h2>Deviation: leagues</h2>
 * The {@code Player} model intentionally omits the {@code leagues} state (spec §4.2 — "Fixed off:
 * leagues"). Every {@code player.leagues.six.effects.*} branch in the upstream
 * {@code calculateEquipmentBonusesFromGear} and {@code calculateAttackSpeed} (the thrown-weapon
 * accuracy +60, percentage-magic-damage, off-hand stat boost, ranged-strength-from-HP, melee-strength-
 * from-prayer, multi-hit strength, thrown-weapon melee-str scale, and the polearm/crossbow/light-weapon/
 * magic/bow attack-speed talents) is therefore dropped. This is the only divergence from the source.
 */
public final class Equipment
{
	private final EquipmentRepository repository;

	public Equipment(EquipmentRepository repository)
	{
		this.repository = repository;
	}

	/**
	 * Port of {@code AmmoApplicability} (Equipment.ts:166-173).
	 */
	public enum AmmoApplicability
	{
		/** Include the ammo slot bonuses in the equipment stats. */
		INCLUDED,
		/** Allow the ammo but do not include its ranged accuracy and strength bonuses. */
		ALLOWED,
		/** The ammo is incompatible with the weapon. */
		INVALID,
	}

	/**
	 * Port of {@code commonAmmoCategories} (Equipment.ts:48-87) — incremental bow/bolt/javelin tiers.
	 */
	private static Map<String, List<Integer>> commonAmmoCategories()
	{
		Map<String, List<Integer>> ret = new HashMap<>();
		ret.put("bow_t1", idList(
			882, 883, 5616, 5622, 598, 942,       // Bronze arrow + variants
			884, 885, 5617, 5623, 2532, 2533,     // Iron arrow + variants
			22227, 22228, 22229, 22230));         // barb assault
		ret.put("cb_t1", idList(
			877, 878, 6061, 6062, 879, 9236));    // Bronze bolts + variants, opal bolts + (e)
		ret.put("javelin", idList(
			825, 831, 5642, 5648,                 // Bronze javelin + variants
			826, 832, 5643, 5649,                 // Iron javelin + variants
			827, 833, 5644, 5650,                 // Steel javelin + variants
			828, 834, 5645, 5651,                 // Mithril javelin + variants
			829, 835, 5646, 5652,                 // Adamant javelin + variants
			830, 836, 5647, 5653,                 // Rune javelin + variants
			21318, 21320, 21322, 21324,           // Amethyst javelin + variants
			19484, 19486, 19488, 19490));         // Dragon javelin + variants

		// Bows
		ret.put("bow_t5", concat(ret.get("bow_t1"), idList(886, 887, 5618, 5624, 2534, 2535))); // Steel arrow + variants
		ret.put("bow_t20", concat(ret.get("bow_t5"), idList(888, 889, 5619, 5625, 2536, 2537))); // Mithril arrow + variants
		ret.put("bow_t30", concat(ret.get("bow_t20"), idList(890, 891, 5620, 5626, 2538, 2539))); // Adamant arrow + variants
		ret.put("bow_t40", concat(ret.get("bow_t30"), idList(892, 893, 5621, 5627, 78, 2540, 2541))); // Rune arrow + variants, ice arrows
		ret.put("bow_t50", concat(ret.get("bow_t40"), idList(21326, 21332, 21334, 21336, 4160, 21328, 21330))); // Amethyst arrow + variants, broad arrows
		ret.put("bow_t60", concat(ret.get("bow_t50"), idList(11212, 11227, 11228, 11229, 11217, 11222))); // Dragon arrow + variants

		// Bolts
		ret.put("cb_t16", concat(ret.get("cb_t1"), idList(9139, 9286, 9293, 9300, 9335, 9237))); // Blurite bolts + variants, jade bolts + (e)
		ret.put("cb_t26", concat(ret.get("cb_t16"), idList(9140, 9287, 9294, 9301, 880, 9238, 9145, 9292, 9299, 9306))); // Iron bolts + variants, pearl bolts + (e), silver bolts
		ret.put("cb_t31", concat(ret.get("cb_t26"), idList(9141, 9288, 9295, 9302, 9336, 9239))); // Steel bolts + variants, topaz bolts + (e)
		ret.put("cb_t36", concat(ret.get("cb_t31"), idList(9142, 9289, 9296, 9303, 9337, 9240, 9338, 9241))); // Mithril bolts + variants, sapphire/emerald bolts + (e)
		ret.put("cb_t46", concat(ret.get("cb_t36"), idList(9143, 9290, 9297, 9304, 9339, 9242, 9340, 9243))); // Adamant bolts + variants, ruby/diamond bolts + (e)
		ret.put("cb_t61", concat(ret.get("cb_t46"), idList(9144, 9291, 9298, 9305, 11875, 21316, 9341, 9244, 9342, 9245))); // Runite bolts + variants, broad bolts, amethyst broad bolts, dragonstone/onyx bolts + (e)
		ret.put("cb_t64", concat(ret.get("cb_t61"), idList(21905, 21924, 21926, 21928, 21955, 21932, 21957, 21934, 21959, 21936, 21961, 21938, 21963, 21940, 21965, 21942, 21967, 21944, 21969, 21946, 21971, 21948, 21973, 21950))); // Dragon bolts + variants, many gem-tipped bolts
		return ret;
	}

	/**
	 * Port of {@code ammoForRangedWeapons} (Equipment.ts:93-164) — weapon id &rarr; valid ammo ids.
	 * An empty list means the weapon must not be used with any ammo slot item.
	 */
	private static final Map<Integer, List<Integer>> AMMO_FOR_RANGED_WEAPONS = buildAmmoForRangedWeapons();

	private static Map<Integer, List<Integer>> buildAmmoForRangedWeapons()
	{
		Map<String, List<Integer>> c = commonAmmoCategories();
		Map<Integer, List<Integer>> m = new HashMap<>();
		// todo(wgs): scorching bow arrows
		m.put(11708, c.get("bow_t1"));   // Cursed goblin bow
		m.put(23357, c.get("bow_t1"));   // Rain bow
		m.put(9705, idList(9706));       // Training bow
		m.put(841, c.get("bow_t1"));     // Shortbow
		m.put(839, c.get("bow_t1"));     // Longbow
		m.put(843, c.get("bow_t5"));     // Oak shortbow
		m.put(845, c.get("bow_t5"));     // Oak longbow
		m.put(4236, c.get("bow_t5"));    // Signed oak bow
		m.put(849, c.get("bow_t20"));    // Willow shortbow
		m.put(847, c.get("bow_t20"));    // Willow longbow
		m.put(10280, c.get("bow_t20"));  // Willow comp bow
		m.put(853, c.get("bow_t30"));    // Maple shortbow
		m.put(851, c.get("bow_t30"));    // Maple longbow
		m.put(2883, idList(2866, 4773, 4778, 4783, 4788, 4793));               // Ogre bow
		m.put(4827, idList(2866, 4773, 4778, 4783, 4788, 4793, 4798, 4803));   // Comp ogre bow
		m.put(857, c.get("bow_t40"));    // Yew shortbow
		m.put(855, c.get("bow_t40"));    // Yew longbow
		m.put(10282, c.get("bow_t40"));  // Yew comp bow
		m.put(28794, c.get("bow_t50"));  // Bone shortbow
		m.put(6724, c.get("bow_t50"));   // Seercull
		m.put(861, c.get("bow_t50"));    // Magic shortbow
		m.put(12788, c.get("bow_t50"));  // Magic shortbow (i)
		m.put(859, c.get("bow_t50"));    // Magic longbow
		m.put(10284, c.get("bow_t50"));  // Magic comp bow
		m.put(11235, c.get("bow_t60"));  // Dark bow
		m.put(27853, c.get("bow_t60"));  // Dark bow (bh)
		m.put(12424, c.get("bow_t60"));  // 3rd age bow
		m.put(27610, c.get("bow_t60"));  // Venator bow
		m.put(27612, c.get("bow_t60"));  // Venator bow (uncharged)
		m.put(20997, c.get("bow_t60"));  // Twisted bow
		m.put(29591, c.get("bow_t60"));  // Scorching bow
		m.put(837, c.get("cb_t1"));      // Crossbow
		m.put(767, c.get("cb_t1"));      // Phoenix crossbow
		m.put(9174, c.get("cb_t1"));     // Bronze crossbow
		m.put(9176, c.get("cb_t16"));    // Blurite crossbow
		m.put(9177, c.get("cb_t26"));    // Iron crossbow
		m.put(9179, c.get("cb_t31"));    // Steel crossbow
		m.put(9181, c.get("cb_t36"));    // Mithril crossbow
		m.put(9183, c.get("cb_t46"));    // Adamant crossbow
		m.put(9185, c.get("cb_t61"));    // Rune crossbow
		m.put(21902, c.get("cb_t64"));   // Dragon crossbow
		m.put(19478, c.get("javelin"));  // Light ballista
		m.put(19481, c.get("javelin"));  // Heavy ballista
		m.put(8880, concat(c.get("cb_t16"), idList(9140, 9287, 9294, 9301, 8882))); // Dorgeshuun crossbow
		m.put(10156, idList(10158, 10159)); // Hunters' crossbow
		m.put(4734, idList(4740));       // Karil's crossbow (undmg)
		m.put(21012, c.get("cb_t64"));   // Dragon hunter crossbow
		m.put(11785, c.get("cb_t64"));   // Armadyl crossbow
		m.put(26374, c.get("cb_t64"));   // Zaryte crossbow
		m.put(33251, c.get("cb_t64"));   // King's barrage todo(leagues): replace id from cache
		m.put(12924, idList());          // Toxic blowpipe (empty)
		m.put(12926, idList());          // Toxic blowpipe (charged)
		m.put(22547, idList());          // Craw's bow (empty)
		m.put(22550, idList());          // Craw's bow (charged)
		m.put(23983, idList());          // Crystal bow (empty)
		m.put(23985, idList());          // Crystal bow (inactive)
		m.put(24123, idList());          // Crystal bow (new)
		m.put(27652, idList());          // Webweaver bow (empty)
		m.put(27655, idList());          // Webweaver bow (charged)
		m.put(25862, idList());          // Bow of faerdhinen (empty)
		m.put(25865, idList());          // Bow of faerdhinen (charged)
		m.put(10149, idList(10142));     // Swamp lizard, Guam tar
		m.put(10146, idList(10143));     // Orange salamander, Marrentill tar
		m.put(10147, idList(10144));     // Red salamander, Tarromin tar
		m.put(10148, idList(10145));     // Black salamander, Harralander tar
		m.put(28834, idList(28837));     // Tecu salamander, Irit tar
		m.put(28869, idList(28872, 28878)); // Hunters' sunlight crossbow
		m.put(29000, idList(28991));     // Eclipse atlatl
		m.put(33245, c.get("bow_t60"));  // Nature's recurve
		return Collections.unmodifiableMap(m);
	}

	/**
	 * Port of {@code ammoApplicability} (Equipment.ts:181-196). Returns whether the given ammo id is
	 * valid ammo for the given ranged weapon id. {@code null} weapon/ammo are treated as upstream's
	 * {@code undefined}.
	 */
	public static AmmoApplicability ammoApplicability(Integer weaponId, Integer ammoId)
	{
		List<Integer> validAmmo = AMMO_FOR_RANGED_WEAPONS.get(weaponId == null ? -1 : weaponId);

		// The weapon does not use ammo
		if (validAmmo == null || validAmmo.isEmpty())
		{
			return AmmoApplicability.ALLOWED;
		}

		// weapon requires ammo, and we have one that matches the list
		if (ammoId != null && validAmmo.contains(ammoId))
		{
			return AmmoApplicability.INCLUDED;
		}

		// weapon requires ammo, but we don't have a matching one
		return AmmoApplicability.INVALID;
	}

	/**
	 * Port of {@code getCanonicalItem} (Equipment.ts:209-226) — folds a piece to its canonical base
	 * row (preserving the original {@code itemVars}). Uses the injected repository in place of the
	 * upstream {@code availableEquipment} global. Returns the original piece when it is already
	 * canonical or has no canonical row.
	 */
	private EquipmentPiece getCanonicalItem(EquipmentPiece piece)
	{
		int canonicalId = repository.canonicalId(piece.getId());
		if (piece.getId() == canonicalId)
		{
			return piece;
		}

		EquipmentPiece canonical = repository.resolve(canonicalId).orElse(null);
		if (canonical == null)
		{
			return piece;
		}

		// Mirror upstream: keep the canonical row's stats but carry over the original piece's itemVars.
		ItemVars vars = piece.getItemVars();
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
			vars,
			canonical.getBonuses(),
			canonical.getOffensive(),
			canonical.getDefensive());
	}

	/**
	 * Port of {@code getCanonicalEquipment} (Equipment.ts:228-244) — canonicalizes every populated slot.
	 */
	private PlayerEquipment getCanonicalEquipment(PlayerEquipment in)
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
		return piece == null ? null : getCanonicalItem(piece);
	}

	/**
	 * Port of {@code calculateAttackSpeed} (Equipment.ts:249-313). League-talent branches are dropped
	 * (the model has no {@code leagues} state — see class javadoc).
	 */
	public int calculateAttackSpeed(Player player, Monster monster)
	{
		EquipmentPiece weapon = player.getEquipment().getWeapon();
		int attackSpeed = weapon != null && weapon.getSpeed() != 0
			? weapon.getSpeed()
			: Constants.DEFAULT_ATTACK_SPEED;

		String stance = player.getStyle() == null ? null : player.getStyle().getStance();
		String type = player.getStyle() == null ? null : player.getStyle().getType();

		if ("ranged".equals(type) && "Rapid".equals(stance))
		{
			attackSpeed -= 1;
		}
		else if (Constants.CAST_STANCES.contains(stance))
		{
			if (weapon != null && "Harmonised nightmare staff".equals(weapon.getName())
				&& player.getSpell() != null && "standard".equals(player.getSpell().getSpellbook())
				&& !"Manual Cast".equals(stance))
			{
				attackSpeed = 4;
			}
			else if (weapon != null && "Twinflame staff".equals(weapon.getName()))
			{
				attackSpeed = 6;
			}
			else
			{
				attackSpeed = 5;
			}
		}

		// Giant rat (Scurrius)
		if (monster.getId() == 7223 && !"Manual Cast".equals(stance))
		{
			String weaponName = weapon == null ? "" : weapon.getName();
			if (Arrays.asList("Bone mace", "Bone shortbow", "Bone staff").contains(weaponName))
			{
				attackSpeed = 1;
			}
		}

		return Math.max(attackSpeed, 1);
	}

	/**
	 * Port of {@code calculateEquipmentBonusesFromGear} (Equipment.ts:315-477). Canonicalizes the
	 * equipment, sums each slot's bonuses/offensive/defensive (gating the ammo slot's {@code ranged_str}
	 * and {@code offensive.ranged} by {@link #ammoApplicability}), then applies the ordered special
	 * cases. League-talent branches are dropped (see class javadoc). Returns the aggregated triple;
	 * the attack speed is computed via {@link #calculateAttackSpeed} (upstream stores it on the same
	 * {@code EquipmentBonuses} object — our {@link EquipmentStats} carries only the stat triple, so the
	 * attack speed is exposed via {@link #calculateAttackSpeed}, mirroring the substrate's
	 * {@code Player.attackSpeed} field).
	 */
	public EquipmentStats calculateEquipmentBonusesFromGear(Player player, Monster monster)
	{
		// bonuses
		int str = 0;
		int magicStr = 0;
		int rangedStr = 0;
		int prayer = 0;
		// offensive
		int offSlash = 0;
		int offStab = 0;
		int offCrush = 0;
		int offRanged = 0;
		int offMagic = 0;
		// defensive
		int defSlash = 0;
		int defStab = 0;
		int defCrush = 0;
		int defRanged = 0;
		int defMagic = 0;

		// canonicalize all items first, otherwise ammoApplicability etc calls may return incorrect results later
		PlayerEquipment eq = getCanonicalEquipment(player.getEquipment());

		Integer weaponId = eq.getWeapon() == null ? null : eq.getWeapon().getId();

		for (EquipmentPiece piece : slots(eq))
		{
			if (piece == null)
			{
				continue;
			}

			// skip over ammo slot's ranged bonuses if it is not used by the bow
			boolean applyRangedStats = !"ammo".equals(piece.getSlot())
				|| ammoApplicability(weaponId, piece.getId()) == AmmoApplicability.INCLUDED;

			PlayerBonuses b = piece.getBonuses();
			if (b != null)
			{
				str += b.getStr();
				magicStr += b.getMagicStr();
				if (applyRangedStats)
				{
					rangedStr += b.getRangedStr();
				}
				prayer += b.getPrayer();
			}

			PlayerOffensive o = piece.getOffensive();
			if (o != null)
			{
				offStab += o.getStab();
				offSlash += o.getSlash();
				offCrush += o.getCrush();
				offMagic += o.getMagic();
				if (applyRangedStats)
				{
					offRanged += o.getRanged();
				}
			}

			PlayerDefensive d = piece.getDefensive();
			if (d != null)
			{
				defStab += d.getStab();
				defSlash += d.getSlash();
				defCrush += d.getCrush();
				defMagic += d.getMagic();
				defRanged += d.getRanged();
			}
		}

		// --- ordered special cases (Equipment.ts:369-472) -----------------------------------------

		// blowpipe dart ranged_str
		if (weaponId != null && Constants.BLOWPIPE_IDS.contains(weaponId))
		{
			ItemVars vars = eq.getWeapon().getItemVars();
			Integer dartId = vars == null ? null : vars.getBlowpipeDartId();
			EquipmentPiece dart = dartId == null ? null : repository.resolve(dartId).orElse(null);
			if (dart != null && dart.getBonuses() != null)
			{
				rangedStr += dart.getBonuses().getRangedStr();
			}
			// else: todo warn user
		}

		boolean toa = Constants.TOMBS_OF_AMASCUT_MONSTER_IDS.contains(monster.getId());
		String stance = player.getStyle() == null ? null : player.getStyle().getStance();

		// Crystal blessing (ammo slot)
		if (eq.getAmmo() != null && "Crystal blessing".equals(eq.getAmmo().getName()))
		{
			int matchingPieces = 0;
			matchingPieces += startsWithCrystal(eq.getHead()) ? 1 : 0;
			matchingPieces += startsWithCrystal(eq.getBody()) ? 1 : 0;
			matchingPieces += startsWithCrystal(eq.getLegs()) ? 1 : 0;

			magicStr += matchingPieces * 20;
			offMagic += matchingPieces * 20;
		}

		// Tumeken's shadow magic ×3/×4 (cap magic_str at 1000)
		if (eq.getWeapon() != null && "Tumeken's shadow".equals(eq.getWeapon().getName())
			&& !"Manual Cast".equals(stance))
		{
			int factor = toa ? 4 : 3;
			magicStr = Math.min(1000, magicStr * factor);
			offMagic *= factor;
		}

		// Keris partisan of amascut penalty (outside ToA)
		if (eq.getWeapon() != null && "Keris partisan of amascut".equals(eq.getWeapon().getName())
			&& !toa)
		{
			str -= 22;
			offStab -= 50;
		}

		// Dinh's bulwark str-from-defence
		if (eq.getWeapon() != null
			&& ("Dinh's bulwark".equals(eq.getWeapon().getName())
				|| "Dinh's blazing bulwark".equals(eq.getWeapon().getName())))
		{
			int defenceSum = defStab + defSlash + defCrush + defRanged;
			str += Math.max(0, (int) ((defenceSum - 800) / 12) - 38);
		}

		// Virtus (ancient spellbook, cast stance)
		if (player.getSpell() != null && "ancient".equals(player.getSpell().getSpellbook())
			&& Constants.CAST_STANCES.contains(stance))
		{
			int virtusPieces = 0;
			virtusPieces += nameContains(eq.getHead(), "Virtus") ? 1 : 0;
			virtusPieces += nameContains(eq.getBody(), "Virtus") ? 1 : 0;
			virtusPieces += nameContains(eq.getLegs(), "Virtus") ? 1 : 0;
			magicStr += 30 * virtusPieces;
		}

		// Void mage is a visible bonus of 5%
		if (nameEquals(eq.getHead(), "Void mage helm")
			&& nameEquals(eq.getBody(), "Elite void top")
			&& nameEquals(eq.getLegs(), "Elite void robe")
			&& nameEquals(eq.getHands(), "Void knight gloves"))
		{
			magicStr += 50;
		}

		// Dizana's quiver (charged) — uses the RAW (non-canonical) weapon/ammo ids, matching upstream
		EquipmentPiece cape = eq.getCape();
		boolean dizanasQuiverCharged = cape != null
			&& ("Dizana's max cape".equals(cape.getName())
				|| "Blessed dizana's quiver".equals(cape.getName())
				|| ("Dizana's quiver".equals(cape.getName()) && "Charged".equals(cape.getVersion())));
		if (dizanasQuiverCharged)
		{
			Integer rawWeaponId = player.getEquipment().getWeapon() == null
				? null : player.getEquipment().getWeapon().getId();
			Integer rawAmmoId = player.getEquipment().getAmmo() == null
				? null : player.getEquipment().getAmmo().getId();
			if (ammoApplicability(rawWeaponId, rawAmmoId) == AmmoApplicability.INCLUDED)
			{
				offRanged += 10;
				rangedStr += 1;
			}
		}

		return new EquipmentStats(
			new PlayerBonuses(str, rangedStr, magicStr, prayer),
			new PlayerOffensive(offStab, offSlash, offCrush, offMagic, offRanged),
			new PlayerDefensive(defStab, defSlash, defCrush, defMagic, defRanged));
	}

	private static boolean nameEquals(EquipmentPiece p, String name)
	{
		return p != null && name.equals(p.getName());
	}

	private static boolean nameContains(EquipmentPiece p, String sub)
	{
		return p != null && p.getName() != null && p.getName().contains(sub);
	}

	private static boolean startsWithCrystal(EquipmentPiece p)
	{
		return p != null && p.getName() != null && p.getName().startsWith("Crystal ");
	}

	private static List<EquipmentPiece> slots(PlayerEquipment eq)
	{
		return Arrays.asList(
			eq.getHead(), eq.getCape(), eq.getNeck(), eq.getAmmo(), eq.getWeapon(),
			eq.getBody(), eq.getShield(), eq.getLegs(), eq.getHands(), eq.getFeet(), eq.getRing());
	}

	private static List<Integer> idList(int... ids)
	{
		List<Integer> out = new ArrayList<>(ids.length);
		for (int id : ids)
		{
			out.add(id);
		}
		return out;
	}

	private static List<Integer> concat(List<Integer> a, List<Integer> b)
	{
		List<Integer> out = new ArrayList<>(a.size() + b.size());
		out.addAll(a);
		out.addAll(b);
		return out;
	}
}
