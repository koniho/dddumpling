#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
configuration="${1:-Debug}"
case "$configuration" in Debug) developer=true ;; Release) developer=false ;; *) echo 'Use Debug or Release' >&2; exit 2 ;; esac
[ -x "$J2OBJC_HOME/j2objc" ] || { echo 'Run ios/scripts/bootstrap.sh first' >&2; exit 1; }
mkdir -p "$IOS_ROOT/build/generated" "$IOS_ROOT/build/flags"
python3 "$IOS_ROOT/scripts/licenses.py"
sh "$REPO_ROOT/tools/build-flags.sh" "$IOS_ROOT/build/flags" "$developer"
python3 "$IOS_ROOT/scripts/sources.py" > "$IOS_ROOT/build/sources.list"
echo "$IOS_ROOT/build/flags/com/dddumpling/game/BuildFlags.java" >> "$IOS_ROOT/build/sources.list"
export J2OBJC_HOME
python3 "$IOS_ROOT/scripts/translate-cache.py" "$configuration"
printf '%s\n' "$configuration" > "$IOS_ROOT/build/translated-configuration"
