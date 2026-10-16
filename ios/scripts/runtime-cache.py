#!/usr/bin/env python3
"""Reuse a verified J2ObjC distribution without invoking its timestamp-based make rules."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import shutil
import sys


def digest(path):
    result = hashlib.sha256()
    with path.open('rb') as source:
        for block in iter(lambda: source.read(1024 * 1024), b''):
            result.update(block)
    return result.hexdigest()


def fingerprint(root):
    scripts = ('bootstrap.sh', 'env.sh', 'runtime-cache.py')
    inputs = {name: digest(root / 'ios/scripts' / name) for name in scripts}
    for command in (['uname', '-m'], ['xcodebuild', '-version'], ['java', '-version']):
        inputs[' '.join(command)] = subprocess.check_output(command, stderr=subprocess.STDOUT).decode().strip()
    return hashlib.sha256(json.dumps(inputs, sort_keys=True).encode()).hexdigest()


def outputs(dist):
    required = ['j2objc', 'include/JreEmulation.h',
                *(f'lib/{platform}/libjre_emul.a' for platform in ('iphone', 'simulator', 'macosx'))]
    if (not all((dist / name).is_file() and (dist / name).stat().st_size for name in required)
            or not os.access(dist / 'j2objc', os.X_OK)
            or not any((dist / 'lib').glob('j2objc*.jar'))):
        raise ValueError('Incomplete J2ObjC distribution')
    return {str(path.relative_to(dist)): digest(path)
            for path in sorted(dist.rglob('*')) if path.is_file()}


def build(dist, key, archive, command):
    stamp = dist.parent / '.dddumpling-runtime.json'
    try:
        prior = json.loads(stamp.read_text())
        if isinstance(prior, dict) and prior.get('key') == key and outputs(dist) == prior.get('outputs'):
            print('J2ObjC runtime: verified cache; skipping compilation', flush=True)
            return
    except (OSError, ValueError):
        pass
    # Never trust output left by an interrupted build, even when its key matches.
    stamp.unlink(missing_ok=True)
    print('J2ObjC runtime: rebuilding missing, changed, or unverified distribution', flush=True)
    # Make does not reliably invalidate intermediates when compiler settings change.
    if dist.parent.exists():
        shutil.rmtree(dist.parent)
    subprocess.run(['tar', '-xzf', str(archive), '-C', str(dist.parent.parent)], check=True)
    subprocess.run(command, cwd=dist.parent, check=True)
    result = outputs(dist)
    stamp.write_text(json.dumps({'key': key, 'outputs': result}, sort_keys=True))
    print('J2ObjC runtime: build verified and recorded', flush=True)


if __name__ == '__main__':
    root = Path(__file__).resolve().parents[2]
    key = fingerprint(root)
    if sys.argv[1:] == ['key']:
        print(key)
    elif len(sys.argv) > 4 and sys.argv[1] == 'build':
        build(Path(sys.argv[2]), key, Path(sys.argv[3]), sys.argv[4:])
    else:
        raise SystemExit('Usage: runtime-cache.py key | build DIST ARCHIVE COMMAND [ARG ...]')
