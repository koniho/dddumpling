#!/bin/bash
# Reinstall over an existing simulator save; never uninstall or seed synthetic progress.
set -euo pipefail
source "$(dirname "$0")/env.sh"
simulator="${SIMULATOR_ID:?Set SIMULATOR_ID to a booted iPhone simulator}"
app="${1:-$IOS_ROOT/build/DerivedData/Build/Products/Debug-iphonesimulator/DDDumpling.app}"
bundle=$(/usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' "$app/Info.plist")
container=$(xcrun simctl get_app_container "$simulator" "$bundle" data)
relative="Library/Application Support/$bundle/game-store-v1.plist"
xcrun simctl terminate "$simulator" "$bundle" 2>/dev/null || true
save="$container/$relative"
if [ ! -s "$save" ]; then
    echo 'Launch and play the installed app first; this check requires an existing save.' >&2
    exit 1
fi
result="$IOS_ROOT/build/Update-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$result"
cp "$save" "$result/before.plist"
xcrun simctl install "$simulator" "$app"
container=$(xcrun simctl get_app_container "$simulator" "$bundle" data)
cmp "$result/before.plist" "$container/$relative"
cp "$container/$relative" "$result/after-install.plist"
xcrun simctl launch "$simulator" "$bundle"
echo "PASS: existing save is byte-identical after install; app launch accepted. Evidence: $result"
echo 'This checks installation preservation, not future schema migration or complete gameplay restoration.'
