#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
output="$IOS_ROOT/build/resources"
mkdir -p "$output"
python3 "$IOS_ROOT/scripts/sources.py" > "$output/sources.list"
sh "$REPO_ROOT/tools/build-flags.sh" "$output/flags" false
javac -nowarn -d "$output/classes" @"$output/sources.list" \
    "$output/flags/com/dddumpling/game/BuildFlags.java" \
    "$REPO_ROOT/tools/RasterPainter.java" "$REPO_ROOT/tools/Font.java" "$REPO_ROOT/tools/Png.java" \
    "$IOS_ROOT/tests/IOSAssets.java"
java -cp "$output/classes" com.dddumpling.game.IOSAssets \
    "$IOS_ROOT/DDDumpling/Assets.xcassets/AppIcon.appiconset/AppIcon.png"
