package org.bossbis.calc;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bossbis.calc.data.EquipmentRepository;
import org.bossbis.calc.data.MonsterRepository;
import org.bossbis.calc.data.SlotMapping.Slot;
import org.bossbis.calc.data.SpellRepository;
import org.bossbis.calc.state.BoostsCalculator;
import org.bossbis.calc.types.Buffs;
import org.bossbis.calc.types.CombatStyle.PlayerCombatStyle;
import org.bossbis.calc.types.DefenceReductions;
import org.bossbis.calc.types.EquipmentPiece;
import org.bossbis.calc.types.EquipmentStats;
import org.bossbis.calc.types.Monster;
import org.bossbis.calc.types.MonsterInputs;
import org.bossbis.calc.types.Player;
import org.bossbis.calc.types.PlayerEquipment;
import org.bossbis.calc.types.PlayerSkills;
import org.bossbis.calc.types.Potion;
import org.bossbis.calc.types.Prayer;

/**
 * Turns a {@link CorpusRow} into a calc-ready {@link Player} + {@link Monster}, MIRRORING upstream
 * {@code getTestPlayer} (TestUtils.ts) and {@code generateEmptyPlayer} (state.tsx):
 *
 * <ul>
 *   <li><b>skills:</b> default to all-99 ({@code generateEmptyPlayer}); the row's {@code skills} map
 *       overrides specific skills (exactly how the {@code testScenario} helper sets a single skill).</li>
 *   <li><b>equipment:</b> each gear id is resolved via {@link EquipmentRepository#resolve} and placed
 *       in its slot; the aggregated {@code bonuses}/{@code offensive}/{@code defensive} are computed
 *       via the Milestone-1 {@link Equipment} aggregation and set on the {@link Player} (upstream
 *       {@code player.bonuses = calculated.bonuses} etc.).</li>
 *   <li><b>boosts:</b> computed via {@link BoostsCalculator} from the row's potions (none -> zero
 *       deltas, matching {@code generateEmptyPlayer}'s all-zero boosts).</li>
 *   <li><b>prayers:</b> the row's prayer names map to {@link Prayer} (by the human name or the enum
 *       constant name).</li>
 *   <li><b>style:</b> set from the row's {@code {name,type,stance}}.</li>
 *   <li><b>attackSpeed:</b> {@link Equipment#calculateAttackSpeed}.</li>
 *   <li><b>buffs:</b> mirror {@code generateEmptyPlayer} (onSlayerTask=true, kandarinDiary=true,
 *       chinchompaDistance=4); these match the upstream defaults the BasicRolls test relies on.</li>
 *   <li><b>monster:</b> resolved via {@link MonsterRepository#resolve} with
 *       {@link MonsterInputs#initial()} attached (the bundled rows carry no inputs;
 *       {@code getTestPlayer} merges {@code INITIAL_MONSTER_INPUTS}).</li>
 * </ul>
 */
final class ScenarioPlayer
{
	private final EquipmentRepository equipment;
	private final MonsterRepository monsters;
	private final SpellRepository spells;
	private final Equipment aggregator;
	private final BoostsCalculator boosts;

	ScenarioPlayer(Gson gson)
	{
		this.equipment = EquipmentRepository.fromBundled(gson);
		this.monsters = MonsterRepository.fromBundled(gson);
		this.spells = SpellRepository.fromBundled(gson);
		this.aggregator = new Equipment(equipment);
		this.boosts = new BoostsCalculator();
	}

	EquipmentRepository equipmentRepository()
	{
		return equipment;
	}

	SpellRepository spellRepository()
	{
		return spells;
	}

	/**
	 * Resolves the row's monster with {@code INITIAL_MONSTER_INPUTS} attached. If the row carries a
	 * {@code monster.inputs} block, its defence reductions / ToA invocation level override the initial
	 * inputs (absent fields keep their {@link MonsterInputs#initial()} defaults).
	 */
	Monster monster(CorpusRow row)
	{
		CorpusRow.MonsterInput m = row.inputs().monster();
		Monster base = monsters.resolve(m.id(), m.version());
		if (base == null)
		{
			throw new IllegalStateException("No monster row for id " + m.id() + " version " + m.version());
		}
		return new Monster(
			base.getId(), base.getName(), base.getImage(), base.getVersion(), base.getSize(),
			base.getSpeed(), base.getStyle(), base.getMaxHit(), base.getSkills(), base.getOffensive(),
			base.getDefensive(), base.getAttributes(), base.getWeakness(), base.getImmunities(),
			base.isSlayerMonster(), monsterInputs(m.inputs()));
	}

	/** Builds {@link MonsterInputs} from the row's optional inputs block (defaults to {@code initial()}). */
	private static MonsterInputs monsterInputs(CorpusRow.MonsterInputsInput in)
	{
		MonsterInputs init = MonsterInputs.initial();
		if (in == null)
		{
			return init;
		}
		int toaInv = in.toaInvocationLevel() == null ? init.getToaInvocationLevel() : in.toaInvocationLevel();
		DefenceReductions dr = init.getDefenceReductions();
		CorpusRow.DefenceReductionsInput d = in.defenceReductions();
		if (d != null)
		{
			dr = new DefenceReductions(
				d.vulnerability(), d.accursed(), d.elderMaul(), d.dwh(), d.arclight(),
				d.emberlight(), d.bgs(), d.tonalztic(), d.seercull(), d.ayak());
		}
		return new MonsterInputs(
			init.isFromCoxCm(), toaInv, init.getToaPathLevel(),
			init.getPartyMaxCombatLevel(), init.getPartySumMiningLevel(), init.getPartyMaxHpLevel(),
			init.getPartySize(), init.getMonsterCurrentHp(), dr,
			init.getDemonbaneVulnerability(), init.getPhase(), init.getPrayers());
	}

	/** Builds the calc-ready Player for the row against the given (already-resolved) monster. */
	Player player(CorpusRow row, Monster monster)
	{
		CorpusRow.PlayerInput in = row.inputs().player();

		PlayerSkills skills = skills(in.skills());
		PlayerSkills boostDeltas = boosts.computeBoosts(skills, potions(in));
		PlayerEquipment eq = equipment(in.equipment());
		PlayerCombatStyle style = style(in.style());
		List<Prayer> prayers = prayers(in.prayers());
		org.bossbis.calc.types.Spell spell = in.spell() == null ? null : spells.byName(in.spell());

		// generateEmptyPlayer buffs defaults; the row's buffs block may override onSlayerTask
		// (the sweep varies on-task vs off-task for the salve/black-mask multiplier paths).
		boolean onSlayerTask = true;
		if (in.buffs() != null && in.buffs().onSlayerTask() != null)
		{
			onSlayerTask = in.buffs().onSlayerTask();
		}
		Buffs buffs = new Buffs(
			onSlayerTask,
			false,  // inWilderness
			false,  // forinthrySurge
			0,      // soulreaperStacks
			0,      // baAttackerLevel
			4,      // chinchompaDistance (4 = optimal "medium fuse")
			true,   // kandarinDiary
			false,  // chargeSpell
			false,  // markOfDarknessSpell
			false); // usingSunfireRunes

		// First build a Player with raw stats so the aggregator can read its equipment/style/spell.
		Player raw = new Player(
			"test", style, skills, boostDeltas, eq, Constants.DEFAULT_ATTACK_SPEED,
			prayers, buffs, spell, null, null, null);

		EquipmentStats agg = aggregator.calculateEquipmentBonusesFromGear(raw, monster);
		int attackSpeed = aggregator.calculateAttackSpeed(raw, monster);

		return new Player(
			raw.getName(), raw.getStyle(), raw.getSkills(), raw.getBoosts(), raw.getEquipment(),
			attackSpeed, raw.getPrayers(), raw.getBuffs(), raw.getSpell(),
			agg.getBonuses(), agg.getOffensive(), agg.getDefensive());
	}

	// ---------------------------------------------------------------------------------------------

	/** All-99 base skills with the row's overrides applied (mirrors getTestPlayer's skill merge). */
	private static PlayerSkills skills(Map<String, Integer> overrides)
	{
		int atk = get(overrides, "atk", 99);
		int def = get(overrides, "def", 99);
		int hp = get(overrides, "hp", 99);
		int magic = get(overrides, "magic", 99);
		int prayer = get(overrides, "prayer", 99);
		int ranged = get(overrides, "ranged", 99);
		int str = get(overrides, "str", 99);
		int mining = get(overrides, "mining", 99);
		int herblore = get(overrides, "herblore", 99);
		return new PlayerSkills(atk, def, hp, magic, prayer, ranged, str, mining, herblore);
	}

	private static int get(Map<String, Integer> m, String key, int def)
	{
		Integer v = m == null ? null : m.get(key);
		return v == null ? def : v;
	}

	/**
	 * Resolves the row's potions (by {@link Potion} enum name) into a set fed to {@link BoostsCalculator}.
	 * Empty when the row omits potions -> all-zero boost deltas, matching generateEmptyPlayer.
	 */
	private static Set<Potion> potions(CorpusRow.PlayerInput in)
	{
		List<String> names = in.potions();
		if (names.isEmpty())
		{
			return Collections.emptySet();
		}
		Set<Potion> out = java.util.EnumSet.noneOf(Potion.class);
		for (String n : names)
		{
			out.add(Potion.valueOf(n));
		}
		return out;
	}

	private PlayerEquipment equipment(Map<String, Integer> bySlot)
	{
		EquipmentPiece[] slots = new EquipmentPiece[Slot.values().length];
		for (Map.Entry<String, Integer> e : bySlot.entrySet())
		{
			Slot slot = org.bossbis.calc.data.SlotMapping.fromWeirdgloop(e.getKey());
			slots[slot.ordinal()] = resolve(e.getValue());
		}
		return new PlayerEquipment(
			slots[Slot.HEAD.ordinal()],
			slots[Slot.CAPE.ordinal()],
			slots[Slot.NECK.ordinal()],
			slots[Slot.AMMO.ordinal()],
			slots[Slot.WEAPON.ordinal()],
			slots[Slot.BODY.ordinal()],
			slots[Slot.SHIELD.ordinal()],
			slots[Slot.LEGS.ordinal()],
			slots[Slot.HANDS.ordinal()],
			slots[Slot.FEET.ordinal()],
			slots[Slot.RING.ordinal()]);
	}

	private EquipmentPiece resolve(int id)
	{
		return equipment.resolve(id).orElseThrow(
			() -> new IllegalArgumentException("No equipment row for id " + id));
	}

	private static PlayerCombatStyle style(Map<String, Object> style)
	{
		return new PlayerCombatStyle(
			str(style, "name"), str(style, "type"), str(style, "stance"));
	}

	private static String str(Map<String, Object> m, String key)
	{
		Object v = m == null ? null : m.get(key);
		return v == null ? null : String.valueOf(v);
	}

	/** Maps prayer names (human name from PrayerData, or the enum constant name) to {@link Prayer}. */
	private static List<Prayer> prayers(List<String> names)
	{
		if (names == null || names.isEmpty())
		{
			return new ArrayList<>();
		}
		Set<String> wanted = new HashSet<>(names);
		List<Prayer> out = new ArrayList<>();
		for (Prayer p : Prayer.values())
		{
			if (wanted.contains(p.name()) || wanted.contains(p.data().name()))
			{
				out.add(p);
			}
		}
		if (out.size() != names.size())
		{
			throw new IllegalArgumentException("Unknown prayer name(s) in " + names + " -> resolved " + out);
		}
		return out;
	}
}
