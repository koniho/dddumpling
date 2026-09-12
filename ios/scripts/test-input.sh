#!/bin/bash
# Independent output directory: safe alongside check.sh and the native build.
set -euo pipefail
source "$(dirname "$0")/env.sh"
cd "$REPO_ROOT"
TEST_BUILD="$REPO_ROOT/build/ios-input-tests"
mkdir -p "$TEST_BUILD"
sh tools/build-flags.sh "$TEST_BUILD/flags" true
python3 ios/scripts/sources.py > "$TEST_BUILD/sources.txt"
javac -nowarn -d "$TEST_BUILD/classes" @"$TEST_BUILD/sources.txt" \
    "$TEST_BUILD/flags/com/dddumpling/game/BuildFlags.java" \
    tools/Check.java tools/Font.java ios/tests/IOSInputTest.java
java -cp "$TEST_BUILD/classes" com.dddumpling.game.IOSInputTest
