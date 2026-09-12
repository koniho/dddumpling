#!/bin/bash
# Builds only the translator and Java runtime; no protobuf toolchain is needed.
set -euo pipefail
source "$(dirname "$0")/env.sh"
version=3.1
checksum=f19e77e8a83113323f1701139701978ceb579e6f997bcb5638d15faee3e192b4
archive="$IOS_ROOT/vendor/j2objc-$version.tar.gz"
mkdir -p "$IOS_ROOT/vendor"
command -v mvn >/dev/null || { echo 'Install Maven: brew install maven' >&2; exit 1; }
command -v xcodegen >/dev/null || { echo 'Install XcodeGen: brew install xcodegen' >&2; exit 1; }
if [ ! -f "$archive" ]; then
    curl --fail --location --retry 3 "https://github.com/google/j2objc/archive/refs/tags/$version.tar.gz" -o "$archive"
fi
echo "$checksum  $archive" | shasum -a 256 -c -
if [ ! -d "$IOS_ROOT/vendor/j2objc-$version" ]; then tar -xzf "$archive" -C "$IOS_ROOT/vendor"; fi
cd "$IOS_ROOT/vendor/j2objc-$version"
case "$(uname -m)" in arm64) architectures='simulator64 iphone64 macosx64' ;; *) architectures='simulator iphone64 macosx' ;; esac
# Xcode 26 adds a warning for Java's specified float rounding in Hashtable.
# Keep upstream warnings, demoting only that diagnostic; never edit generated sources.
warnings='-Wall -Werror -Wshorten-64-to-32 -Wimplicit-function-declaration -Wmissing-field-initializers -Wduplicate-method-match -Wno-unused-variable -Wno-nullability-completeness -Wno-unused-but-set-variable -Wno-error=implicit-const-int-float-conversion'
make -j"${JOBS:-8}" translator_dist jre_emul_dist J2OBJC_ARCHS="$architectures" "CC_WARNINGS=$warnings"
