#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
listing_write=false
case "${1:-}" in
  '') [[ $# == 0 ]] || exit 2 ;;
  --write) [[ $# == 1 ]] || exit 2; listing_write=true ;;
  *) printf 'Usage: play-listing-text.sh [--write]\n' >&2; exit 2 ;;
esac
gplay_bin="${GPLAY_BIN:-build/tools/gplay/gplay}"
mkdir -p .private
listing_root=$(mktemp -d .private/play-listing-XXXXXX)
listing_dir="$listing_root/desired"
"$gplay_bin" metadata pull --package com.dddumpling.game --dir "$listing_root/before" --output json
mkdir -p "$listing_dir/en-US"
cp app-store/google-play/en-US/title.txt "$listing_dir/en-US/title.txt"
cp app-store/google-play/en-US/short-description.txt "$listing_dir/en-US/short_description.txt"
cp app-store/google-play/en-US/full-description.txt "$listing_dir/en-US/full_description.txt"
"$gplay_bin" metadata validate --dir "$listing_dir" --output json
listing_args=(metadata apply --package com.dddumpling.game --dir "$listing_dir" --changes-in-review error --output json)
"$gplay_bin" "${listing_args[@]}" --dry-run > "$listing_root/preview.json"
cat "$listing_root/preview.json"
if [[ "$listing_write" == true ]]; then
  "$gplay_bin" "${listing_args[@]}" --confirm > "$listing_root/applied.json"
  cat "$listing_root/applied.json"
  "$gplay_bin" metadata pull --package com.dddumpling.game --dir "$listing_root/saved" --output json
  python3 - "$listing_root" <<'PY'
import sys
from pathlib import Path
root = Path(sys.argv[1])
expected = {p.relative_to(root/'before'):p.read_text().strip() for p in (root/'before').rglob('*.txt')}
expected.update({p.relative_to(root/'desired'):p.read_text().strip() for p in (root/'desired').rglob('*.txt')})
actual = {p.relative_to(root/'saved'):p.read_text().strip() for p in (root/'saved').rglob('*.txt')}
assert expected == actual, 'Saved listing differs from the three requested text changes'
print('Verified English title and descriptions; other listing text and video preserved.')
PY
fi
