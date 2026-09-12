#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
simulator="${SIMULATOR_ID:-$(xcrun simctl list devices available -j | python3 -c 'import json,sys; d=json.load(sys.stdin); print(next(x["udid"] for a in d["devices"].values() for x in a if x["name"].startswith("iPhone")))')}"
CONFIGURATION=Debug bash "$IOS_ROOT/scripts/build.sh"
if ! xcrun simctl list devices booted | grep -q "$simulator"; then xcrun simctl boot "$simulator"; fi
xcrun simctl bootstatus "$simulator" -b
open -a Simulator
xcrun simctl install "$simulator" "$IOS_ROOT/build/DerivedData/Build/Products/Debug-iphonesimulator/DDDumpling.app"
xcrun simctl launch "$simulator" com.dddumpling.game.ios.dev
