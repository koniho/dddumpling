#!/usr/bin/env python3
"""Bundle the pinned runtime's license/notice files alongside the font's OFL."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
upstream = root / 'vendor/j2objc-3.1'
files = [upstream / 'LICENSE']
names = {'LICENSE', 'LICENSE.txt', 'NOTICE', 'NOTICE.txt'}
files += sorted(p for p in (upstream / 'jre_emul').rglob('*')
                if p.is_file() and p.name in names and 'build_result' not in p.parts)
output = root / 'build/ThirdPartyNotices.txt'
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text('\n\n'.join(str(p.relative_to(upstream)) + '\n\n' +
                             p.read_text(errors='replace') for p in files))
