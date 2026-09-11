#!/bin/sh
# Builds a signed, installable APK entirely on-device in Termux. No Gradle, no PC.
#   aapt2 compile/link -> ecj -> d8 -> zip -> apksigner
# See check.sh for why the shebang is /bin/sh with a re-exec rather than a path to bash.
# shellcheck shell=bash
if [ -z "${BASH_VERSION:-}" ]; then exec bash "$0" "$@"; fi
set -euo pipefail
cd "$(dirname "$0")"

DEVELOPER=true
case "${1:---developer}" in
    --developer) ;;
    --production) DEVELOPER=false ;;
    *) echo "usage: ./build.sh [--developer|--production]" >&2; exit 1 ;;
esac
[ "$#" -le 1 ] || exit 1

SDK=sdk/android.jar
if [ ! -f "$SDK" ] && [ -n "${ANDROID_HOME:-}" ]; then
    SDK="$ANDROID_HOME/platforms/android-36/android.jar"
fi
OUT=build
APK=hexatype.apk
MIN=21
TGT=36
KS=${HEXATYPE_KEYSTORE:-$OUT/debug.keystore}
KS_ALIAS=${HEXATYPE_KEY_ALIAS:-hexatype}
KS_STORE_PASS=${HEXATYPE_KEYSTORE_PASSWORD:-android}
KS_KEY_PASS=${HEXATYPE_KEY_PASSWORD:-$KS_STORE_PASS}

[ -z "${HEXATYPE_KEYSTORE:-}" ] || [ -f "$KS" ] || { echo "Signing keystore not found: $KS" >&2; exit 1; }

[ -f "$SDK" ] || { echo "missing $SDK - see README.md"; exit 1; }

mkdir -p "$OUT"
echo ">> rules + frame renders"
bash ./check.sh >"$OUT/check.log" 2>&1 || { tail -30 "$OUT/check.log"; exit 1; }
grep -E '^[0-9]+ passed' "$OUT/check.log"
bash ./check.sh --production -q >"$OUT/production-check.log" 2>&1 || { tail -30 "$OUT/production-check.log"; exit 1; }
grep -E '^[0-9]+ passed' "$OUT/production-check.log"

rm -rf "$OUT/classes" "$OUT/gen" "$OUT/res.zip" "$OUT/base.apk" "$OUT"/classes*.dex
mkdir -p "$OUT/classes" "$OUT/gen"
sh tools/build-flags.sh "$OUT/gen" "$DEVELOPER"

PLAY_CONFIG=${DDDUMPLING_PLAY_CONFIG:-}
PLAY_JARS=()
PLAY_RES=()
PLAY_LINK=()
PLATFORM_SRC=local-src
MANIFEST=AndroidManifest.xml
APP_ID=com.dddumpling.game
APP_LABEL=DDDUMPLING
if [ "$DEVELOPER" = true ]; then
    APP_ID=com.dddumpling.game.dev
    APP_LABEL="DDDUMPLING Dev"
    MANIFEST="$OUT/developer-manifest.xml"
    sed 's|@string/app_name"|@string/app_name_dev"|' AndroidManifest.xml > "$MANIFEST"
fi
if [ "$DEVELOPER" = false ] && [ -n "$PLAY_CONFIG" ]; then
    python tools/prepare-play.py --config "$PLAY_CONFIG"
    PLATFORM_SRC=play-src
    MANIFEST="$OUT/play/AndroidManifest.xml"
    mapfile -t PLAY_JARS < "$OUT/play/jars.txt"
    mapfile -t PLAY_RES < "$OUT/play/resources.txt"
    PLAY_LINK=(--extra-packages "$(cat "$OUT/play/packages.txt")" --auto-add-overlay)
    if [ -d "$OUT/play/assets" ]; then PLAY_LINK+=(-A "$OUT/play/assets"); fi
fi

echo ">> resources"
aapt2 compile --dir res -o "$OUT/res.zip"
aapt2 link -o "$OUT/base.apk" -I "$SDK" \
    --manifest "$MANIFEST" --rename-manifest-package "$APP_ID" --custom-package com.dddumpling.game \
    --java "$OUT/gen" --min-sdk-version "$MIN" --target-sdk-version "$TGT" \
    -A assets \
    "${PLAY_LINK[@]}" "${PLAY_RES[@]}" "$OUT/res.zip"

TASK_CP="$SDK"
for PLAY_JAR in "${PLAY_JARS[@]}"; do TASK_CP="$TASK_CP:$PLAY_JAR"; done

echo ">> java"
find src "$PLATFORM_SRC" "$OUT/gen" -name '*.java' >"$OUT/sources.txt"
# Note: Termux's `ecj` wrapper hardcodes -7 and a bogus -cp, so javac is the sane choice.
javac -nowarn -Xlint:none -encoding UTF-8 --release 8 \
    -cp "$TASK_CP" -d "$OUT/classes" @"$OUT/sources.txt"

echo ">> dex"
find "$OUT/classes" -name '*.class' >"$OUT/classes.txt"
# A fresh directory avoids carrying extra dex files over from an SDK-enabled build.
mkdir -p "$OUT/dex"
find "$OUT/dex" -name 'classes*.dex' -delete
d8 --lib "$SDK" --min-api "$MIN" --output "$OUT/dex" @"$OUT/classes.txt" "${PLAY_JARS[@]}"
find "$OUT" -maxdepth 1 -name 'classes*.dex' -delete
cp "$OUT"/dex/classes*.dex "$OUT/"

echo ">> package + sign"
(cd "$OUT" && zip -q -j base.apk classes*.dex)
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -storepass "$KS_STORE_PASS" \
    -keypass "$KS_KEY_PASS" -alias "$KS_ALIAS" -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Hexatype Debug, O=Local, C=US" 2>/dev/null
apksigner sign --ks "$KS" --ks-key-alias "$KS_ALIAS" \
    --ks-pass "pass:$KS_STORE_PASS" --key-pass "pass:$KS_KEY_PASS" \
    --out "$APK" "$OUT/base.apk"
apksigner verify "$APK" && echo ">> signature ok"
aapt2 dump badging "$APK" > "$OUT/apk-info.txt"
grep -Fq "package: name='$APP_ID'" "$OUT/apk-info.txt"
grep -Fq "application-label:'$APP_LABEL'" "$OUT/apk-info.txt"
grep -Fq "launchable-activity: name='com.dddumpling.game.MainActivity'" "$OUT/apk-info.txt"
echo ">> identity ok: $APP_ID ($APP_LABEL)"

ls -la "$APK"
echo
echo "install it with:  termux-open $(pwd)/$APK"
