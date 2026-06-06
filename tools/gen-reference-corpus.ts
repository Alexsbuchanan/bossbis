// Reference corpus generator.
//
// v0.1.0: reads every tools/scenarios/*.json, validates each is a well-formed
// parity row, and writes them (sorted by name) into the Java test corpus at
// src/test/resources/parity/parity-corpus.json.
//
// The rows are hand-transcribed from upstream's Jest tests (src/tests/calc/*),
// so each carries a real, upstream-asserted expected value (e.g. maxHit, maxAttackRoll).
//
// Automated generation from the LIVE upstream calc is deferred to v0.1.1 — it
// requires stubbing weirdgloop's PNG/asset imports the way their jest.config.ts
// moduleNameMapper does. See tools/README.md.
//
// Usage: npm run gen-corpus

import { readFileSync, readdirSync, mkdirSync, writeFileSync } from "node:fs";
import { resolve, dirname, join } from "node:path";

const here = import.meta.dirname;
const scenarioDir = resolve(here, "scenarios");
const outPath = resolve(here, "..", "src", "test", "resources", "parity", "parity-corpus.json");

interface CorpusRow {
  name: string;
  source?: string;
  exercises: string[];
  inputs: {
    player: {
      skills: Record<string, number>;
      prayers: string[];
      style: { name: string; type: string | null; stance: string | null };
      spell?: string;
      equipment: Record<string, number>;
    };
    monster: {
      id: number;
      version: string;
      // Optional per-encounter inputs. Absent -> the Java ScenarioPlayer applies
      // MonsterInputs.initial(). Only the fields a row needs must be present.
      inputs?: {
        toaInvocationLevel?: number;
        defenceReductions?: {
          vulnerability?: boolean;
          accursed?: boolean;
          elderMaul?: number;
          dwh?: number;
          arclight?: number;
          emberlight?: number;
          bgs?: number;
          tonalztic?: number;
          seercull?: number;
          ayak?: number;
        };
      };
    };
  };
  expected: Record<string, number>;
  weirdgloopCommit: string;
}

function fail(file: string, msg: string): never {
  throw new Error(`Invalid scenario ${file}: ${msg}`);
}

function validate(file: string, row: unknown): CorpusRow {
  if (typeof row !== "object" || row === null) {
    fail(file, "not a JSON object");
  }
  const r = row as Record<string, unknown>;

  if (typeof r.name !== "string" || r.name.length === 0) {
    fail(file, "missing string 'name'");
  }
  if (!Array.isArray(r.exercises) || r.exercises.length === 0 || !r.exercises.every((e) => typeof e === "string")) {
    fail(file, "'exercises' must be a non-empty string array");
  }
  if (typeof r.weirdgloopCommit !== "string" || r.weirdgloopCommit.length !== 40) {
    fail(file, "'weirdgloopCommit' must be a 40-char commit hash");
  }
  if (typeof r.expected !== "object" || r.expected === null || Object.keys(r.expected as object).length === 0) {
    fail(file, "'expected' must be a non-empty object");
  }
  for (const [k, v] of Object.entries(r.expected as object)) {
    if (typeof v !== "number" || !Number.isFinite(v)) {
      fail(file, `expected.${k} must be a finite number`);
    }
  }

  const inputs = r.inputs as Record<string, unknown> | undefined;
  if (typeof inputs !== "object" || inputs === null) {
    fail(file, "missing 'inputs' object");
  }

  const monster = inputs.monster as Record<string, unknown> | undefined;
  if (typeof monster !== "object" || monster === null || typeof monster.id !== "number" || typeof monster.version !== "string") {
    fail(file, "'inputs.monster' must have numeric 'id' and string 'version'");
  }

  // Optional per-encounter monster inputs (defence reductions, ToA invocation level).
  if (monster.inputs !== undefined) {
    const mi = monster.inputs as Record<string, unknown>;
    if (typeof mi !== "object" || mi === null) {
      fail(file, "'inputs.monster.inputs' must be an object when present");
    }
    if (mi.toaInvocationLevel !== undefined && (typeof mi.toaInvocationLevel !== "number" || !Number.isInteger(mi.toaInvocationLevel))) {
      fail(file, "'monster.inputs.toaInvocationLevel' must be an integer");
    }
    if (mi.defenceReductions !== undefined) {
      const dr = mi.defenceReductions as Record<string, unknown>;
      if (typeof dr !== "object" || dr === null) {
        fail(file, "'monster.inputs.defenceReductions' must be an object");
      }
      const boolKeys = ["vulnerability", "accursed"];
      const intKeys = ["elderMaul", "dwh", "arclight", "emberlight", "bgs", "tonalztic", "seercull", "ayak"];
      for (const [k, v] of Object.entries(dr)) {
        if (boolKeys.includes(k)) {
          if (typeof v !== "boolean") {
            fail(file, `defenceReductions.${k} must be a boolean`);
          }
        } else if (intKeys.includes(k)) {
          if (typeof v !== "number" || !Number.isInteger(v)) {
            fail(file, `defenceReductions.${k} must be an integer`);
          }
        } else {
          fail(file, `unknown defenceReductions key '${k}'`);
        }
      }
    }
  }

  const player = inputs.player as Record<string, unknown> | undefined;
  if (typeof player !== "object" || player === null) {
    fail(file, "missing 'inputs.player'");
  }
  const skills = player.skills as Record<string, unknown> | undefined;
  if (typeof skills !== "object" || skills === null) {
    fail(file, "missing 'inputs.player.skills'");
  }
  for (const key of ["atk", "str", "def", "hp", "ranged", "magic", "prayer", "mining", "herblore"]) {
    if (typeof skills[key] !== "number") {
      fail(file, `skills.${key} must be a number (default 99 per getTestPlayer)`);
    }
  }
  if (!Array.isArray(player.prayers) || !player.prayers.every((p) => typeof p === "string")) {
    fail(file, "'player.prayers' must be a string array");
  }
  // Optional potions (Potion enum names) and buffs override block (sweep rows).
  if (player.potions !== undefined) {
    if (!Array.isArray(player.potions) || !player.potions.every((p) => typeof p === "string")) {
      fail(file, "'player.potions' must be a string array of Potion enum names");
    }
  }
  if (player.buffs !== undefined) {
    const b = player.buffs as Record<string, unknown>;
    if (typeof b !== "object" || b === null) {
      fail(file, "'player.buffs' must be an object");
    }
    if (b.onSlayerTask !== undefined && typeof b.onSlayerTask !== "boolean") {
      fail(file, "'player.buffs.onSlayerTask' must be a boolean");
    }
  }
  const style = player.style as Record<string, unknown> | undefined;
  if (typeof style !== "object" || style === null || typeof style.name !== "string") {
    fail(file, "'player.style' must have a string 'name'");
  }
  const equipment = player.equipment as Record<string, unknown> | undefined;
  if (typeof equipment !== "object" || equipment === null) {
    fail(file, "missing 'player.equipment'");
  }
  for (const [slot, id] of Object.entries(equipment)) {
    if (typeof id !== "number" || !Number.isInteger(id)) {
      fail(file, `equipment.${slot} must be an integer item id`);
    }
  }

  return row as CorpusRow;
}

// Collects every *.json under dir, recursing one level into subdirectories (so
// the hand-transcribed rows in tools/scenarios/*.json AND the generated rows in
// tools/scenarios/sweep/*.json are both folded in). Returns relative paths.
function collectJson(dir: string): string[] {
  const out: string[] = [];
  let entries;
  try {
    entries = readdirSync(dir, { withFileTypes: true });
  } catch {
    return out;
  }
  for (const e of entries) {
    if (e.isDirectory()) {
      for (const sub of collectJson(join(dir, e.name))) {
        out.push(join(e.name, sub));
      }
    } else if (e.isFile() && e.name.endsWith(".json")) {
      out.push(e.name);
    }
  }
  return out.sort();
}

function main(): void {
  const files = collectJson(scenarioDir);

  const rows: CorpusRow[] = [];
  const seenNames = new Set<string>();
  for (const f of files) {
    const full = join(scenarioDir, f);
    let parsed: unknown;
    try {
      parsed = JSON.parse(readFileSync(full, "utf8"));
    } catch (e) {
      throw new Error(`Failed to parse ${f}: ${(e as Error).message}`);
    }
    const row = validate(f, parsed);
    if (seenNames.has(row.name)) {
      throw new Error(`Duplicate scenario name '${row.name}' in ${f}`);
    }
    seenNames.add(row.name);
    rows.push(row);
  }

  // Deterministic order: sort by row name.
  rows.sort((a, b) => a.name.localeCompare(b.name, "en"));

  mkdirSync(dirname(outPath), { recursive: true });
  writeFileSync(outPath, JSON.stringify(rows, null, 2) + "\n", "utf8");
  console.log(`Validated ${rows.length} scenario file(s) from ${scenarioDir}.`);
  console.log(`Wrote ${rows.length} row(s) to ${outPath}`);
}

main();
