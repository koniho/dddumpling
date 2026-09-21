#!/bin/sh
# Builds a signed, installable APK entirely on-device in Termux. No Gradle, no PC.
#   aapt2 compile/link -> ecj -> d8 -> zip -> apksigner
# See check.sh for why the shebang is /bin/sh with a re-exec rather than a path to bash.
# shellcheck shell=bash
if [ -z "${BASH_VERSION:-}" ]; then exec bash "$0" "$@"; fi
set -euo pipefail
cd "$(dirname "$0")"

DEVELOPER=true
FULL_CHECKS=false
TOWN_CHECKS=false
RULES_CHECKS=false
for option in "$@"; do
    case "$option" in
        --developer) DEVELOPER=true ;;
        --production) DEVELOPER=false ;;
        --full-checks) FULL_CHECKS=true ;;
        --town) TOWN_CHECKS=true ;;
        --rules-checks) RULES_CHECKS=true ;;
        *) echo "usage: ./build.sh [--developer|--production] [--town|--rules-checks|--full-checks]" >&2; exit 1 ;;
    esac
done

[ "$TOWN_CHECKS" = false ] || [ "$FULL_CHECKS" = false ] || {
    echo "--town cannot be combined with --full-checks" >&2
    exit 1
}
[ "$RULES_CHECKS" = false ] || { [ "$TOWN_CHECKS" = false ] && [ "$FULL_CHECKS" = false ]; } || {
    echo "--rules-checks cannot be combined with --town or --full-checks" >&2
    exit 1
}
if [ "$DEVELOPER" = false ] && { [ "$TOWN_CHECKS" = true ] || [ "$RULES_CHECKS" = true ]; }; then
    echo "--town and --rules-checks are developer-build gates; production uses its full checks" >&2
    exit 1
fi

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
if [ "$TOWN_CHECKS" = true ]; then
    echo ">> town rules + selected frames"
    bash ./check.sh --town -q >"$OUT/check.log" 2>&1 || { tail -30 "$OUT/check.log"; exit 1; }
    grep -E '^[0-9]+ passed' "$OUT/check.log"
elif [ "$RULES_CHECKS" = true ]; then
    echo ">> full rules checks"
    bash ./check.sh -q -r >"$OUT/check.log" 2>&1 || { tail -30 "$OUT/check.log"; exit 1; }
    grep -E '^[0-9]+ passed' "$OUT/check.log"
elif [ "$DEVELOPER" = true ] && [ "$FULL_CHECKS" = false ]; then
    echo ">> local rule smoke checks"
    bash ./check.sh -q -r -s Rules >"$OUT/check.log" 2>&1 || { tail -30 "$OUT/check.log"; exit 1; }
    grep -E '^[0-9]+ passed' "$OUT/check.log"
else
    echo ">> rules + frame renders"
    bash ./check.sh >"$OUT/check.log" 2>&1 || { tail -30 "$OUT/check.log"; exit 1; }
    grep -E '^[0-9]+ passed' "$OUT/check.log"
    bash ./check.sh --production -q >"$OUT/production-check.log" 2>&1 || { tail -30 "$OUT/production-check.log"; exit 1; }
    grep -E '^[0-9]+ passed' "$OUT/production-check.log"
fi

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
    # Local checkouts can lag an installed Dev build. Keep release metadata unchanged.
    if command -v pm >/dev/null 2>&1; then
        INSTALLED_DEV_APK=$(pm path "$APP_ID" 2>/dev/null | sed -n 's/^package://p' | head -n 1 || true)
        if [ -r "$INSTALLED_DEV_APK" ]; then
            INSTALLED_DEV_CODE=$(aapt2 dump badging "$INSTALLED_DEV_APK" 2>/dev/null | sed -n "s/.* versionCode='\([0-9]*\)'.*/\1/p" | head -n 1)
            SOURCE_DEV_CODE=$(sed -n 's/.*android:versionCode="\([0-9]*\)".*/\1/p' "$MANIFEST")
            if [[ "$INSTALLED_DEV_CODE" =~ ^[0-9]+$ ]] && [ "$INSTALLED_DEV_CODE" -gt "$SOURCE_DEV_CODE" ]; then
                sed -i "s/android:versionCode=\"$SOURCE_DEV_CODE\"/android:versionCode=\"$INSTALLED_DEV_CODE\"/" "$MANIFEST"
                echo ">> local Dev version code: $INSTALLED_DEV_CODE (matches installed app)"
            fi
        fi
    fi
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
RESOURCE_FILES=()
while IFS= read -r -d '' RESOURCE_FILE; do RESOURCE_FILES+=("$RESOURCE_FILE"); done < <(find res -type f ! -path 'res/raw/bgm.*' -print0)
aapt2 compile "${RESOURCE_FILES[@]}" -o "$OUT/res.zip"
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
