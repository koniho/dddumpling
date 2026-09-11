#!/usr/bin/env bash
set -euo pipefail
: "${DEPLOY_KEY:?Missing repository deploy key}"
: "${SITE:?Missing site}"
: "${PUBLIC_REPOSITORY:?Missing public repository}"
case "$SITE:$PUBLIC_REPOSITORY" in
  website:koniho/dddumpling-game|privacy:koniho/dddumpling-privacy) ;;
  *) echo "Unexpected publishing destination"; exit 1 ;;
esac
publish_tmp=$(mktemp -d)
trap 'rm -rf "$publish_tmp"' EXIT
umask 077
printf '%s\n' "$DEPLOY_KEY" > "$publish_tmp/key"
unset DEPLOY_KEY
cp .github/github_known_hosts "$publish_tmp/known_hosts"
export GIT_SSH_COMMAND="ssh -i $publish_tmp/key -o IdentitiesOnly=yes -o StrictHostKeyChecking=yes -o UserKnownHostsFile=$publish_tmp/known_hosts"
git clone --quiet --depth 1 --branch main "git@github.com:$PUBLIC_REPOSITORY.git" "$publish_tmp/repo"
if [ "$SITE" = website ]; then
  # Only static website files are eligible for publication.
  python3 - <<'PY'
from pathlib import Path
from html.parser import HTMLParser
from urllib.parse import urlsplit
root = Path('docs/site')
for p in root.rglob('*'):
    assert not p.is_symlink(), 'Symlink is not publishable'
    if p.is_file():
        assert p.suffix in {'.html', '.css', '.png', '.jpg', '.webp', '.svg', '.ico', '.txt'}, p
class Validate(HTMLParser):
    def handle_starttag(self, tag, attrs):
        a = dict(attrs)
        if tag == 'img': assert 'alt' in a
        for key in ('src', 'href'):
            v = a.get(key, '')
            if v and not urlsplit(v).scheme and not v.startswith('#'):
                assert (root / v).is_file() or v == './', v
Validate().feed((root / 'index.html').read_text())
PY
  rsync -a --delete --exclude='.git/' docs/site/ "$publish_tmp/repo/"
else
  grep -q 'dddumpling.play@gmail.com' docs/privacy.html
  if grep -qE '\[PUBLIC SUPPORT EMAIL\]|Draft —' docs/privacy.html; then
    echo 'Policy contains unfinished draft content'; exit 1
  fi
  cp docs/privacy.html "$publish_tmp/repo/index.html"
fi
touch "$publish_tmp/repo/.nojekyll"
git -C "$publish_tmp/repo" config user.name 'DDDUMPLING website bot'
git -C "$publish_tmp/repo" config user.email 'dddumpling.play@gmail.com'
git -C "$publish_tmp/repo" add -A
if git -C "$publish_tmp/repo" diff --cached --quiet; then
  echo "Public $SITE is already current"
else
  git -C "$publish_tmp/repo" commit -m "Update $SITE from game source"
  git -C "$publish_tmp/repo" push origin main
fi
