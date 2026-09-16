#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
case "${DDDUMPLING_GAME_CENTER:-}" in ''|0|1) ;; *) echo 'DDDUMPLING_GAME_CENTER must be 0 or 1' >&2; exit 2 ;; esac
case "${1:-}" in ''|--unsigned) ;; *) echo 'Usage: archive.sh [--unsigned]' >&2; exit 2 ;; esac
bash "$IOS_ROOT/scripts/translate.sh" Release
xcodegen generate --spec "$IOS_ROOT/project.yml"
args=(-project "$IOS_ROOT/DDDumpling.xcodeproj" -scheme DDDumpling -configuration Release
      -destination 'generic/platform=iOS' -derivedDataPath "$IOS_ROOT/build/Device"
      -archivePath "$IOS_ROOT/build/DDDumpling.xcarchive")
if [ "${1:-}" = --unsigned ]; then
    args+=(CODE_SIGNING_ALLOWED=NO)
elif [ -n "${DEVELOPMENT_TEAM:-}" ]; then
    args+=("DEVELOPMENT_TEAM=$DEVELOPMENT_TEAM")
fi
if [ -n "${DDDUMPLING_GAME_CENTER:-}" ]; then
    args+=("DDDUMPLING_GAME_CENTER=$DDDUMPLING_GAME_CENTER")
fi
xcodebuild "${args[@]}" archive
