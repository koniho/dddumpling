#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

feature_write=false
case "${1:-}" in
  '') [[ $# == 0 ]] || { printf 'Usage: play-feature-graphic.sh [--write]\n' >&2; exit 2; } ;;
  --write) [[ $# == 1 ]] || exit 2; feature_write=true ;;
  *) printf 'Usage: play-feature-graphic.sh [--write]\n' >&2; exit 2 ;;
esac
gplay_bin="${GPLAY_BIN:-build/tools/gplay/gplay}"
[[ -x "$gplay_bin" ]] || { printf 'Run bash tools/install-gplay.sh first\n' >&2; exit 1; }
feature_sha=$(shasum -a 256 app-store/google-play/feature-graphic.png | cut -d ' ' -f 1)
[[ -z "${PLAY_FEATURE_SHA256:-}" || "$PLAY_FEATURE_SHA256" == "$feature_sha" ]] || {
  printf 'Feature graphic differs from the approved checksum\n' >&2; exit 1;
}
feature_dir=.private/play-feature/metadata
mkdir -p "$feature_dir/en-US/images"
cp app-store/google-play/feature-graphic.png "$feature_dir/en-US/images/featureGraphic.png"
feature_args=(metadata images apply --package com.dddumpling.game --dir "$feature_dir"
              --locale en-US --type featureGraphic --changes-in-review error --output json)
"$gplay_bin" metadata images validate --dir "$feature_dir" --output json
"$gplay_bin" "${feature_args[@]}" --dry-run > .private/play-feature/preview.json
jq -e '.package == "com.dddumpling.game" and all(.slots[]; .locale == "en-US" and .imageType == "featureGraphic")' \
  .private/play-feature/preview.json > /dev/null
cat .private/play-feature/preview.json
if [[ "$feature_write" == true ]]; then
  "$gplay_bin" "${feature_args[@]}" --confirm > .private/play-feature/applied.json
fi
"$gplay_bin" metadata images list --package com.dddumpling.game --type featureGraphic --output json \
  > .private/play-feature/saved.json
if [[ "$feature_write" == true ]]; then
  jq -e --arg sha "$feature_sha" \
    'any(.slots[]; .locale == "en-US" and .imageType == "featureGraphic" and .count == 1 and .images[0].sha256 == $sha)' \
    .private/play-feature/saved.json > /dev/null
  cat .private/play-feature/saved.json
  printf 'Feature graphic upload verified. Google applies its normal listing review flow.\n'
fi
