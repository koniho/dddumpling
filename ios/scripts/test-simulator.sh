#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
configuration="${CONFIGURATION:-Debug}"
simulator="${SIMULATOR_ID:-$(xcrun simctl list devices available -j | python3 -c 'import json,sys; d=json.load(sys.stdin); prefix=sys.argv[1]; matches=[x["udid"] for a in d["devices"].values() for x in a if x["name"].startswith(prefix)]; sys.exit("No available simulator matching " + prefix) if not matches else print(matches[0])' "${SIMULATOR_NAME_PREFIX:-iPhone}")}"
bash "$IOS_ROOT/scripts/translate.sh" "$configuration"
xcodegen generate --spec "$IOS_ROOT/project.yml"
xcodebuild -project "$IOS_ROOT/DDDumpling.xcodeproj" -scheme "${TEST_SCHEME:-DDDumpling}" -configuration "$configuration" \
    -destination "platform=iOS Simulator,id=$simulator" -derivedDataPath "$IOS_ROOT/build/DerivedData" \
    -resultBundlePath "$IOS_ROOT/build/Test-$(date +%Y%m%d-%H%M%S).xcresult" \
    -parallel-testing-enabled NO -maximum-concurrent-test-simulator-destinations 1 \
    "ARCHS=$(uname -m)" CODE_SIGNING_ALLOWED=NO test "$@"
