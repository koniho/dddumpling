#!/bin/bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
mode="${1:---full}"
case "$mode" in
  --full|--selected|--build-only) [ "$#" -eq 0 ] || shift ;;
  *) mode=--full ;; # Retain callers passing xcodebuild flags directly.
esac
if [ "$mode" != --build-only ] && [ ! -f "$IOS_ROOT/build/simulator-id" ]; then
    bash "$IOS_ROOT/scripts/boot-simulator.sh"
fi
# Test artifacts are not distributed; reuse their identity across repeat translations/runs.
export DDDUMPLING_BUILD_ID="${DDDUMPLING_BUILD_ID:-ios-check-${GITHUB_SHA:-$(git -C "$REPO_ROOT" rev-parse HEAD)}}"
started=$SECONDS
bash "$IOS_ROOT/scripts/translate.sh" Debug
xcodegen generate --spec "$IOS_ROOT/project.yml"
args=(-project "$IOS_ROOT/DDDumpling.xcodeproj" -scheme DDDumpling -configuration Debug
      -derivedDataPath "$IOS_ROOT/build/DerivedData" CODE_SIGNING_ALLOWED=NO)
if [ "$mode" = --build-only ]; then
    xcodebuild "${args[@]}" -destination 'generic/platform=iOS Simulator' \
        "ARCHS=$(uname -m)" ONLY_ACTIVE_ARCH=YES build "$@"
    echo "Simulator compile: $((SECONDS-started)) seconds"
    exit
fi
simulator="${SIMULATOR_ID:-$(cat "$IOS_ROOT/build/simulator-id")}"
args+=(-destination "platform=iOS Simulator,id=$simulator"
       -parallel-testing-enabled NO -maximum-concurrent-test-simulator-destinations 1)
selection=()
if [ "$mode" = --selected ]; then
    while IFS= read -r target; do selection+=("-only-testing:$target"); done < <(
        python3 -c 'import json,os; targets=json.loads(os.environ["IOS_TEST_TARGETS"]); assert targets; print("\n".join(targets))')
    [ "${#selection[@]}" -gt 0 ] || { echo 'No selected tests' >&2; exit 1; }
fi
xcodebuild "${args[@]}" build-for-testing "${selection[@]}" "$@"
echo "Simulator test build: $((SECONDS-started)) seconds"
ready=$SECONDS
xcrun simctl bootstatus "$simulator" -b
echo "Remaining simulator boot wait: $((SECONDS-ready)) seconds"
if [ -f "$IOS_ROOT/build/simulator-boot-end" ]; then
    boot_start=$(cat "$IOS_ROOT/build/simulator-boot-start")
    boot_end=$(cat "$IOS_ROOT/build/simulator-boot-end")
    echo "Simulator boot to readiness: $((boot_end-boot_start)) seconds"
fi
tested=$SECONDS
xcodebuild "${args[@]}" -resultBundlePath "$IOS_ROOT/build/Test-$(date +%Y%m%d-%H%M%S).xcresult" \
    test-without-building "${selection[@]}" "$@"
echo "Test runner, execution, and shutdown: $((SECONDS-tested)) seconds"
