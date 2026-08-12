#!/usr/bin/env bash
# Builds a signed, installable APK entirely on-device in Termux. No Gradle, no PC.
#   aapt2 compile/link -> ecj -> d8 -> zip -> apksigner
set -euo pipefail
cd "$(dirname "$0")"

SDK=sdk/android.jar
if [ ! -f "$SDK" ] && [ -n "${ANDROID_HOME:-}" ]; then
    SDK="$ANDROID_HOME/platforms/android-35/android.jar"
fi
OUT=build
APK=hexatype.apk
MIN=21
TGT=35
KS=$OUT/debug.keystore

[ -f "$SDK" ] || { echo "missing $SDK - see README.md"; exit 1; }

echo ">> rules + frame renders"
bash ./check.sh >"$OUT/check.log" 2>&1 || { tail -30 "$OUT/check.log"; exit 1; }
grep -E '^[0-9]+ passed' "$OUT/check.log"

rm -rf "$OUT/classes" "$OUT/gen" "$OUT/res.zip" "$OUT/base.apk" "$OUT/classes.dex"
mkdir -p "$OUT/classes" "$OUT/gen"

echo ">> resources"
aapt2 compile --dir res -o "$OUT/res.zip"
aapt2 link -o "$OUT/base.apk" -I "$SDK" \
    --manifest AndroidManifest.xml \
    --java "$OUT/gen" --min-sdk-version "$MIN" --target-sdk-version "$TGT" \
    -A assets \
    "$OUT/res.zip"

echo ">> java"
find src "$OUT/gen" -name '*.java' >"$OUT/sources.txt"
# Note: Termux's `ecj` wrapper hardcodes -7 and a bogus -cp, so javac is the sane choice.
javac -nowarn -Xlint:none -encoding UTF-8 --release 8 \
    -cp "$SDK" -d "$OUT/classes" @"$OUT/sources.txt" 2>&1 | grep -v 'source value 8' || true

echo ">> dex"
find "$OUT/classes" -name '*.class' >"$OUT/classes.txt"
d8 --lib "$SDK" --min-api "$MIN" --output "$OUT" @"$OUT/classes.txt"

echo ">> package + sign"
(cd "$OUT" && zip -q -j base.apk classes.dex)
[ -f "$KS" ] || keytool -genkeypair -keystore "$KS" -storepass android -keypass android \
    -alias hexatype -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Hexatype Debug, O=Local, C=US" 2>/dev/null
apksigner sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
    --out "$APK" "$OUT/base.apk"
apksigner verify "$APK" && echo ">> signature ok"

ls -la "$APK"
echo
echo "install it with:  termux-open $(pwd)/$APK"
