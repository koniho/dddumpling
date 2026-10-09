#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

gplay_release=2.1.0
case "$(uname -s):$(uname -m)" in
  Darwin:arm64) gplay_platform=darwin_arm64; gplay_checksum=296de05234f5aa3851c2b84178f0e45d34c59a6d02051c2588ed8d37e38b0805 ;;
  Darwin:x86_64) gplay_platform=darwin_amd64; gplay_checksum=c21487daf2b0528544c9bc5c44fdb09fbded769b70887770885b25aa2db5d86e ;;
  Linux:x86_64) gplay_platform=linux_amd64; gplay_checksum=4ecdb2d7559470fcdbdbecb06832f341d270048eca304dee75d161943543adc0 ;;
  Linux:aarch64|Linux:arm64) gplay_platform=linux_arm64; gplay_checksum=10a77337bf6376b9badc597f5c82ead236ad3fc3dbf40a3ad1934aa3ae14fc1d ;;
  *) printf 'Unsupported gplay platform\n' >&2; exit 1 ;;
esac
gplay_archive="gplay_${gplay_release}_${gplay_platform}.tar.gz"
mkdir -p build/tools/gplay
curl -fsSL "https://github.com/PollyGlot/google-play-cli/releases/download/v${gplay_release}/${gplay_archive}" \
  --output "build/tools/gplay/$gplay_archive"
cd build/tools/gplay
printf '%s  %s\n' "$gplay_checksum" "$gplay_archive" | shasum -a 256 -c -
tar -xzf "$gplay_archive" gplay
./gplay version
