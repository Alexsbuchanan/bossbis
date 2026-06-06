// Reference corpus generator.
//
// v0.0 scaffold: writes an empty corpus and proves the Node/TS setup works.
// In v0.1.0 this imports weirdgloop's calc (via the @/ path alias into tools/upstream/src)
// and TestUtils.getTestPlayer to emit rows with the computed boosts delta + full Monster.inputs.
//
// Usage: npm run gen-corpus

import { writeFileSync, readdirSync, mkdirSync } from "node:fs";
import { resolve, dirname } from "node:path";

const here = import.meta.dirname;
const scenarioDir = resolve(here, "scenarios");
const outPath = resolve(here, "..", "src", "test", "resources", "parity", "parity-corpus.json");

function scenarioCount(): number {
  try {
    return readdirSync(scenarioDir).filter((f) => f.endsWith(".json")).length;
  } catch {
    return 0;
  }
}

function main(): void {
  const n = scenarioCount();
  console.log(`Found ${n} scenario file(s) in ${scenarioDir}.`);
  if (n === 0) {
    console.log("(v0.0 scaffold: writing empty corpus.)");
  }
  mkdirSync(dirname(outPath), { recursive: true });
  writeFileSync(outPath, JSON.stringify([], null, 2) + "\n", "utf8");
  console.log(`Wrote 0 rows to ${outPath}`);
}

main();
