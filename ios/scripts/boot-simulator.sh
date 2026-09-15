#!/bin/bash
set -euo pipefail
# Run immediately after selecting Xcode so boot overlaps toolchain setup and compilation.
root="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$root/build"
simulator="${SIMULATOR_ID:-$(xcrun simctl list devices available -j | python3 -c 'import json,sys; d=json.load(sys.stdin); print(next(x["udid"] for a in d["devices"].values() for x in a if x["name"].startswith("iPhone")))')}"
printf '%s\n' "$simulator" > "$root/build/simulator-id"
rm -f "$root/build/simulator-boot-end"
printf '%s\n' "$(date +%s)" > "$root/build/simulator-boot-start"
state="$(xcrun simctl list devices -j | python3 -c 'import json,sys; d=json.load(sys.stdin); print(next(x["state"] for a in d["devices"].values() for x in a if x["udid"]==sys.argv[1]))' "$simulator")"
if [ "$state" != Booted ]; then xcrun simctl boot "$simulator"; fi
# Record readiness independently while other setup/build work continues.
nohup bash -c 'xcrun simctl bootstatus "$1" -b && date +%s > "$2"' _ "$simulator" \
    "$root/build/simulator-boot-end" > "$root/build/simulator-boot.log" 2>&1 &
# The test script also checks readiness before launching tests.
