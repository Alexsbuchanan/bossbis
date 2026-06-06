#!/usr/bin/env bash
# Fail if the calc package uses nondeterministic randomness (breaks parity).
set -euo pipefail
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CALC_DIR="$REPO_ROOT/src/main/java/org/bossbis/calc"

if [ ! -d "$CALC_DIR" ]; then
	echo "check-determinism: calc dir not present yet (ok at v0.0)"
	exit 0
fi

# Patterns that introduce nondeterminism into the calc.
if grep -rnE 'Math\.random|new[[:space:]]+Random|ThreadLocalRandom|new[[:space:]]+SecureRandom' "$CALC_DIR"; then
	echo "check-determinism: FAIL — nondeterministic RNG found in org.bossbis.calc"
	exit 1
fi

echo "check-determinism: OK — no RNG in org.bossbis.calc"
