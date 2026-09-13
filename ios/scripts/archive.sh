#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
case "${1:-}" in ''|--unsigned) ;; *) echo 'Usage: archive.sh [--unsigned]' >&2; exit 2 ;; esac
bash "$IOS_ROOT/scripts/translate.sh" Release
xcodegen generate --spec "$IOS_ROOT/project.yml"
prepare_packages
args=(-project "$IOS_ROOT/DDDumpling.xcodeproj" -scheme DDDumpling -configuration Release
      -destination 'generic/platform=iOS' -derivedDataPath "$IOS_ROOT/build/Device"
      -archivePath "$IOS_ROOT/build/DDDumpling.xcarchive" -onlyUsePackageVersionsFromResolvedFile)
if [ "${1:-}" = --unsigned ]; then
    args+=(CODE_SIGNING_ALLOWED=NO)
elif [ -n "${DEVELOPMENT_TEAM:-}" ]; then
    args+=("DEVELOPMENT_TEAM=$DEVELOPMENT_TEAM")
fi
xcodebuild "${args[@]}" archive
