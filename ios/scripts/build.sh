#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
configuration="${CONFIGURATION:-Debug}"
case "${DDDUMPLING_GAME_CENTER:-}" in ''|0|1) ;; *) echo 'DDDUMPLING_GAME_CENTER must be 0 or 1' >&2; exit 2 ;; esac
sdk="${1:-iphonesimulator}"
case "$sdk" in iphonesimulator|iphoneos) ;; *) echo 'Use iphonesimulator or iphoneos' >&2; exit 2 ;; esac
bash "$IOS_ROOT/scripts/translate.sh" "$configuration"
xcodegen generate --spec "$IOS_ROOT/project.yml"
args=(-project "$IOS_ROOT/DDDumpling.xcodeproj" -scheme DDDumpling -configuration "$configuration"
      -sdk "$sdk" -derivedDataPath "$IOS_ROOT/build/DerivedData")
if [ "$sdk" = iphonesimulator ]; then
    args+=(-destination 'generic/platform=iOS Simulator' "ARCHS=$(uname -m)" CODE_SIGNING_ALLOWED=NO)
else
    args+=(-destination 'generic/platform=iOS')
    [ -z "${DEVELOPMENT_TEAM:-}" ] || args+=("DEVELOPMENT_TEAM=$DEVELOPMENT_TEAM")
fi
if [ -n "${DDDUMPLING_GAME_CENTER:-}" ]; then
    args+=("DDDUMPLING_GAME_CENTER=$DDDUMPLING_GAME_CENTER")
fi
xcodebuild "${args[@]}" build
