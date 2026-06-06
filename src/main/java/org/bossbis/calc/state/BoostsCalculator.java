package org.bossbis.calc.state;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.bossbis.calc.types.PlayerSkills;
import org.bossbis.calc.types.Potion;

/**
 * Port of {@code utils.ts} {@code PotionMap.calculateFn} (the 22 potion boost formulas) plus
 * {@code state.tsx:294-309} {@code recomputeBoosts} (the per-skill <em>max-combine</em> across the
 * selected potions).
 *
 * <p>Each potion's per-skill delta is {@code (int) Math.floor(base + level * pct)} for the skills it
 * affects and {@code 0} for the skills it does not. {@code computeBoosts} returns a
 * {@link PlayerSkills} whose every field is the maximum of those deltas over the selected potions
 * (or {@code 0} if no selected potion affects that skill). Negative deltas (Forgotten/Ancient brew)
 * never exceed the {@code 0} floor, matching upstream's {@code if (r > boosts[k])} accumulation.
 *
 * <p>The {@code PlayerSkills} constructor order is {@code (atk, def, hp, magic, prayer, ranged, str,
 * mining, herblore)} — note {@code str} is the seventh argument.
 */
public final class BoostsCalculator
{
	/** Each entry maps base skills -> the per-skill delta that potion grants (0 where unaffected). */
	private static final Map<Potion, Function<PlayerSkills, PlayerSkills>> FNS = new EnumMap<>(Potion.class);

	static
	{
		// Overload (+): atk/str/def/magic/ranged = floor(6 + level*0.16)
		FNS.put(Potion.OVERLOAD_PLUS, s -> deltas(
			f(6, 0.16, s.getAtk()), f(6, 0.16, s.getStr()), f(6, 0.16, s.getDef()),
			f(6, 0.16, s.getMagic()), f(6, 0.16, s.getRanged()), 0));

		// Smelling salts: atk/str/def/magic/ranged = floor(11 + level*0.16)
		FNS.put(Potion.SMELLING_SALTS, s -> deltas(
			f(11, 0.16, s.getAtk()), f(11, 0.16, s.getStr()), f(11, 0.16, s.getDef()),
			f(11, 0.16, s.getMagic()), f(11, 0.16, s.getRanged()), 0));

		// Super combat: atk/str/def = floor(5 + level*0.15); magic/ranged unaffected.
		FNS.put(Potion.SUPER_COMBAT, s -> deltas(
			f(5, 0.15, s.getAtk()), f(5, 0.15, s.getStr()), f(5, 0.15, s.getDef()), 0, 0, 0));

		// Ranging potion: ranged = floor(4 + level*0.1)
		FNS.put(Potion.RANGING, s -> deltas(
			0, 0, 0, 0, f(4, 0.1, s.getRanged()), 0));

		// Saturated heart: magic = floor(4 + level*0.1)
		FNS.put(Potion.SATURATED_HEART, s -> deltas(
			0, 0, 0, f(4, 0.1, s.getMagic()), 0, 0));

		// Imbued heart: magic = floor(1 + level*0.1)
		FNS.put(Potion.IMBUED_HEART, s -> deltas(
			0, 0, 0, f(1, 0.1, s.getMagic()), 0, 0));

		// Forgotten brew: magic = floor(3 + level*0.08); atk/str/def = floor(-2 - level*0.1) (negative).
		FNS.put(Potion.FORGOTTEN_BREW, s -> deltas(
			f(-2, -0.1, s.getAtk()), f(-2, -0.1, s.getStr()), f(-2, -0.1, s.getDef()),
			f(3, 0.08, s.getMagic()), 0, 0));

		// Super attack: atk = floor(5 + level*0.15)
		FNS.put(Potion.SUPER_ATTACK, s -> deltas(
			f(5, 0.15, s.getAtk()), 0, 0, 0, 0, 0));

		// Super strength: str = floor(5 + level*0.15)
		FNS.put(Potion.SUPER_STRENGTH, s -> deltas(
			0, f(5, 0.15, s.getStr()), 0, 0, 0, 0));

		// Ancient brew: magic = floor(2 + level*0.05); atk/str/def = floor(-2 - level*0.1) (negative).
		FNS.put(Potion.ANCIENT, s -> deltas(
			f(-2, -0.1, s.getAtk()), f(-2, -0.1, s.getStr()), f(-2, -0.1, s.getDef()),
			f(2, 0.05, s.getMagic()), 0, 0));

		// Overload: atk/str/def/magic/ranged = floor(5 + level*0.13)
		FNS.put(Potion.OVERLOAD, s -> deltas(
			f(5, 0.13, s.getAtk()), f(5, 0.13, s.getStr()), f(5, 0.13, s.getDef()),
			f(5, 0.13, s.getMagic()), f(5, 0.13, s.getRanged()), 0));

		// Magic potion: magic = +4 (fixed, level-independent).
		FNS.put(Potion.MAGIC, s -> deltas(
			0, 0, 0, 4, 0, 0));

		// Attack potion: atk = floor(3 + level*0.1)
		FNS.put(Potion.ATTACK, s -> deltas(
			f(3, 0.1, s.getAtk()), 0, 0, 0, 0, 0));

		// Strength potion: str = floor(3 + level*0.1)
		FNS.put(Potion.STRENGTH, s -> deltas(
			0, f(3, 0.1, s.getStr()), 0, 0, 0, 0));

		// Super ranging: ranged = floor(5 + level*0.15)
		FNS.put(Potion.SUPER_RANGING, s -> deltas(
			0, 0, 0, 0, f(5, 0.15, s.getRanged()), 0));

		// Super magic: magic = floor(5 + level*0.15)
		FNS.put(Potion.SUPER_MAGIC, s -> deltas(
			0, 0, 0, f(5, 0.15, s.getMagic()), 0, 0));

		// Defence potion: def = floor(3 + level*0.1)
		FNS.put(Potion.DEFENCE, s -> deltas(
			0, 0, f(3, 0.1, s.getDef()), 0, 0, 0));

		// Super defence: def = floor(5 + level*0.15)
		FNS.put(Potion.SUPER_DEFENCE, s -> deltas(
			0, 0, f(5, 0.15, s.getDef()), 0, 0, 0));

		// Ruby Harvest (moth): atk = floor(4 + level*0.15)
		FNS.put(Potion.RUBY_HARVEST, s -> deltas(
			f(4, 0.15, s.getAtk()), 0, 0, 0, 0, 0));

		// Black Warlock (moth): str = floor(4 + level*0.15)
		FNS.put(Potion.BLACK_WARLOCK, s -> deltas(
			0, f(4, 0.15, s.getStr()), 0, 0, 0, 0));

		// Sapphire Glacialis (moth): def = floor(4 + level*0.15)
		FNS.put(Potion.SAPPHIRE_GLACIALIS, s -> deltas(
			0, 0, f(4, 0.15, s.getDef()), 0, 0, 0));

		// Moonlight potion: herblore-gated atk/str/def using Math.trunc (== floor for the positive
		// values these branches produce). Unmet branches contribute 0 to the max-combine, matching
		// upstream's `undefined` (omitted) skill key.
		FNS.put(Potion.MOONLIGHT, s -> deltas(
			moonlightAtk(s), moonlightStr(s), moonlightDef(s), 0, 0, 0));
	}

	public PlayerSkills computeBoosts(PlayerSkills base, Set<Potion> potions)
	{
		int atk = 0, str = 0, def = 0, magic = 0, ranged = 0, prayer = 0;
		for (Potion p : potions)
		{
			Function<PlayerSkills, PlayerSkills> fn = FNS.get(p);
			if (fn == null)
			{
				continue;
			}
			PlayerSkills d = fn.apply(base);
			atk = Math.max(atk, d.getAtk());
			str = Math.max(str, d.getStr());
			def = Math.max(def, d.getDef());
			magic = Math.max(magic, d.getMagic());
			ranged = Math.max(ranged, d.getRanged());
			prayer = Math.max(prayer, d.getPrayer());
		}
		// hp/mining/herblore boost deltas are 0 for every ported potion.
		return new PlayerSkills(atk, def, 0, magic, prayer, ranged, str, 0, 0);
	}

	/** {@code (int) Math.floor(base + level * pct)} — matches upstream's positive-input Math.floor. */
	private static int f(int base, double pct, int level)
	{
		return (int) Math.floor(base + level * pct);
	}

	/**
	 * Builds a per-skill delta tuple in {@link PlayerSkills} field order
	 * {@code (atk, def, hp, magic, prayer, ranged, str, mining, herblore)}; note the argument order
	 * here is {@code (atk, str, def, magic, ranged, prayer)} for readability, reordered on construct.
	 */
	private static PlayerSkills deltas(int atk, int str, int def, int magic, int ranged, int prayer)
	{
		return new PlayerSkills(atk, def, 0, magic, prayer, ranged, str, 0, 0);
	}

	// Moonlight potion — utils.ts:405-430. Math.trunc on positive inputs; unmet tiers -> 0.
	private static int moonlightAtk(PlayerSkills s)
	{
		if (s.getHerblore() >= 45)
		{
			return (int) (5 + s.getAtk() * 0.15);
		}
		if (s.getHerblore() >= 3)
		{
			return (int) (3 + s.getAtk() * 0.10);
		}
		return 0;
	}

	private static int moonlightStr(PlayerSkills s)
	{
		if (s.getHerblore() >= 55)
		{
			return (int) (5 + s.getStr() * 0.15);
		}
		if (s.getHerblore() >= 12)
		{
			return (int) (3 + s.getStr() * 0.10);
		}
		return 0;
	}

	private static int moonlightDef(PlayerSkills s)
	{
		if (s.getHerblore() >= 70)
		{
			return (int) (7 + s.getDef() * 0.20);
		}
		if (s.getHerblore() >= 66)
		{
			return (int) (5 + s.getDef() * 0.15);
		}
		if (s.getHerblore() >= 30)
		{
			return (int) (3 + s.getDef() * 0.10);
		}
		return 0;
	}
}
