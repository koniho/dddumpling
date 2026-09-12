#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/env.sh"

output="${1:-$REPO_ROOT/out/ios-reference}"
work="$IOS_ROOT/build/reference"
mkdir -p "$work/classes" "$work/flags" "$output"
sh "$REPO_ROOT/tools/build-flags.sh" "$work/flags" true
python3 "$IOS_ROOT/scripts/sources.py" > "$work/sources.list"
printf '%s\n' \
  "$REPO_ROOT/tools/Font.java" \
  "$REPO_ROOT/tools/Png.java" \
  "$REPO_ROOT/tools/RasterPainter.java" \
  "$IOS_ROOT/tests/IOSReference.java" \
  "$work/flags/com/dddumpling/game/BuildFlags.java" >> "$work/sources.list"
javac -nowarn -d "$work/classes" @"$work/sources.list"
java -cp "$work/classes" com.dddumpling.game.IOSReference "$output"
