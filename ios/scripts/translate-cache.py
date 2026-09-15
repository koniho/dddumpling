#!/usr/bin/env python3
"""Cache whole-program translation; preserve mtimes for identical generated files."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def outputs(directory):
    return {str(p.relative_to(directory)): digest(p) for p in sorted(directory.rglob('*')) if p.is_file()}


def fingerprint(root, configuration, sources, toolchain):
    data = {'configuration': configuration, 'sources': [(str(p.relative_to(root)), digest(p)) for p in sources],
            'toolchain': toolchain}
    for name in ('ios/prefixes.properties', 'ios/scripts/translate.sh', 'ios/scripts/translate-cache.py',
                 'ios/scripts/sources.py', 'ios/scripts/bootstrap.sh'):
        data[name] = digest(root / name)
    return hashlib.sha256(json.dumps(data, sort_keys=True).encode()).hexdigest()


def translate(root, configuration, translator, toolchain):
    build = root / 'ios/build'
    generated = build / 'generated'
    stamp = build / 'translation-cache.json'
    sources = [Path(line) for line in (build / 'sources.list').read_text().splitlines()]
    key = fingerprint(root, configuration, sources, toolchain)
    try:
        prior = json.loads(stamp.read_text()) if stamp.exists() else {}
    except (ValueError, OSError):
        prior = {}
    if not isinstance(prior, dict):
        prior = {}
    if prior.get('key') == key and prior.get('outputs') and outputs(generated) == prior['outputs']:
        print('J2ObjC translation: cached')
        return
    # A failed translator must never leave a valid stamp for partial output.
    stamp.unlink(missing_ok=True)
    with tempfile.TemporaryDirectory(dir=build, prefix='translate-') as temporary:
        target = Path(temporary)
        subprocess.run([str(translator), '--prefixes', str(root / 'ios/prefixes.properties'),
                        '-sourcepath', ':'.join(str(root / p) for p in ('src', 'ios/java', 'ios/build/flags')),
                        '-d', str(target), '@' + str(build / 'sources.list')], check=True)
        files = outputs(target)
        if not files:
            raise RuntimeError('J2ObjC produced no files')
        generated.mkdir(parents=True, exist_ok=True)
        for relative in outputs(generated).keys() - files.keys():
            (generated / relative).unlink()
        for relative, content in files.items():
            destination = generated / relative
            if not destination.exists() or digest(destination) != content:
                destination.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(target / relative, destination)
        stamp.write_text(json.dumps({'key': key, 'outputs': files}, sort_keys=True))
    print('J2ObjC translation: regenerated; unchanged file timestamps preserved')


if __name__ == '__main__':
    root = Path(__file__).resolve().parents[2]
    translator = Path(os.environ['J2OBJC_HOME']) / 'j2objc'
    # Include the actual translator jars as well as the pinned bootstrap script.
    jars = sorted((translator.parent / 'lib').glob('j2objc*.jar'))
    toolchain = [digest(translator), *(digest(p) for p in jars),
                 subprocess.check_output(['java', '-version'], stderr=subprocess.STDOUT).decode()]
    translate(root, sys.argv[1], translator, toolchain)
