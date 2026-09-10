#!/bin/sh
# Build an Android App Bundle through the same tested code/resource pipeline as the APK.
if [ -z "${BASH_VERSION:-}" ]; then exec bash "$0" "$@"; fi
set -euo pipefail
cd "$(dirname "$0")"
UNSIGNED=0
if [ "${1:-}" = --unsigned ]; then UNSIGNED=1; shift; fi
[ "$#" -eq 0 ] || { echo 'usage: ./build-bundle.sh [--unsigned]' >&2; exit 1; }
if [ "$UNSIGNED" -eq 0 ]; then
    : "${HEXATYPE_KEYSTORE:?Set the upload keystore, or use --unsigned for local validation}"
    : "${HEXATYPE_KEY_ALIAS:?Set the upload key alias}"
    : "${HEXATYPE_KEYSTORE_PASSWORD:?Set the upload keystore password}"
    [ -f "$HEXATYPE_KEYSTORE" ] || { echo 'Upload keystore not found' >&2; exit 1; }
fi
BUNDLETOOL=sdk/bundletool-all-1.18.3.jar
BUNDLETOOL_SHA=a099cfa1543f55593bc2ed16a70a7c67fe54b1747bb7301f37fdfd6d91028e29
mkdir -p sdk
if [ ! -f "$BUNDLETOOL" ]; then
    curl --fail --location --retry 3 --output "$BUNDLETOOL" \
        https://github.com/google/bundletool/releases/download/1.18.3/bundletool-all-1.18.3.jar
fi
printf '%s  %s\n' "$BUNDLETOOL_SHA" "$BUNDLETOOL" | sha256sum --check --status
./build.sh --production
# Convert the already-linked APK so its SDK/version/resources cannot drift from the bundle.
aapt2 convert --output-format proto -o build/bundle-res.apk build/base.apk
python3 - <<'PY'
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
with ZipFile('build/bundle-res.apk') as source, ZipFile('build/base-module.zip', 'w', ZIP_DEFLATED) as module:
    for entry in source.infolist():
        name = entry.filename
        if name.startswith('META-INF/') or name.endswith('.dex'):
            continue
        if name == 'AndroidManifest.xml':
            name = 'manifest/AndroidManifest.xml'
        module.writestr(name, source.read(entry))
    for dex in sorted(Path('build').glob('classes*.dex')):
        module.write(dex, 'dex/' + dex.name)
PY
java -jar "$BUNDLETOOL" build-bundle --modules=build/base-module.zip \
    --output=build/DDDUMPLING.aab --overwrite
java -jar "$BUNDLETOOL" validate --bundle=build/DDDUMPLING.aab
if [ "$UNSIGNED" -eq 0 ]; then
    export HEXATYPE_KEYSTORE_PASSWORD
    export HEXATYPE_KEY_PASSWORD=${HEXATYPE_KEY_PASSWORD:-$HEXATYPE_KEYSTORE_PASSWORD}
    jarsigner -keystore "$HEXATYPE_KEYSTORE" -storepass:env HEXATYPE_KEYSTORE_PASSWORD \
        -keypass:env HEXATYPE_KEY_PASSWORD build/DDDUMPLING.aab "$HEXATYPE_KEY_ALIAS"
    jarsigner -verify build/DDDUMPLING.aab
    echo 'Signed Play upload bundle: build/DDDUMPLING.aab'
else
    echo 'Unsigned validation bundle: build/DDDUMPLING.aab (sign before Play upload)'
fi
