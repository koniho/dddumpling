#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
simulator="${SIMULATOR_ID:-$(xcrun simctl list devices available -j | python3 -c 'import json,sys; d=json.load(sys.stdin); print(next(x["udid"] for a in d["devices"].values() for x in a if x["name"].startswith("iPhone")))')}"
bash "$IOS_ROOT/scripts/translate.sh" Debug
xcodegen generate --spec "$IOS_ROOT/project.yml"
xcodebuild -project "$IOS_ROOT/DDDumpling.xcodeproj" -scheme DDDumpling -configuration Debug \
    -destination "platform=iOS Simulator,id=$simulator" -derivedDataPath "$IOS_ROOT/build/DerivedData" \
    -resultBundlePath "$IOS_ROOT/build/Test-$(date +%Y%m%d-%H%M%S).xcresult" \
    -parallel-testing-enabled NO -maximum-concurrent-test-simulator-destinations 1 \
    CODE_SIGNING_ALLOWED=NO test "$@"
