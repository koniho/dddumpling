#!/usr/bin/env python3
"""Reuse the harness manifest to exclude Android-only sources."""
from pathlib import Path
import re
root = Path(__file__).resolve().parents[2]
manifest = re.search(r'^PURE="(.*?)"', (root / 'check.sh').read_text(), re.M | re.S)
if not manifest:
    raise SystemExit('Cannot find shared Java manifest in check.sh')
for source in manifest.group(1).split():
    print(root / source)
for source in sorted((root / 'ios/java').rglob('*.java')):
    print(source)
