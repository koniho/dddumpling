#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
config="${IOS_FIREBASE_CONFIG:-$REPO_ROOT/.private/firebase/GoogleService-Info.plist}"
test -f "$config" || { echo 'Provide IOS_FIREBASE_CONFIG before live verification.' >&2; exit 1; }
export IOS_FIREBASE_CONFIG="$config"
bash "$IOS_ROOT/scripts/translate.sh" Release
xcodegen generate --spec "$IOS_ROOT/project.yml"
prepare_packages
xcodebuild -project "$IOS_ROOT/DDDumpling.xcodeproj" -scheme AnalyticsLive -configuration Release \
    -destination "platform=iOS Simulator,id=${SIMULATOR_ID:?Set SIMULATOR_ID}" \
    -derivedDataPath "$IOS_ROOT/build/DerivedData" \
    -resultBundlePath "$IOS_ROOT/build/LiveAnalytics-$(date +%Y%m%d-%H%M%S).xcresult" \
    -parallel-testing-enabled NO -onlyUsePackageVersionsFromResolvedFile \
    -only-testing:DDDumplingUITests/LiveAnalyticsTests/testConfiguredReleaseConsentAndGameplay \
    'OTHER_SWIFT_FLAGS=$(inherited) -D DDD_LIVE_ANALYTICS_TEST' \
    "ARCHS=$(uname -m)" ONLY_ACTIVE_ARCH=YES CODE_SIGNING_ALLOWED=NO test
