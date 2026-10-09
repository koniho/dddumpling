#!/usr/bin/env python3
"""Render two game-art references for promotional illustration, without building an app."""
import argparse
import os
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', type=Path, default=ROOT / 'out/promotional-references')
    parser.add_argument('--java-home', type=Path,
                        default=Path(os.environ['JAVA_HOME']) if os.environ.get('JAVA_HOME') else None)
    args = parser.parse_args()
    java = str(args.java_home / 'bin/java') if args.java_home else shutil.which('java')
    javac = str(args.java_home / 'bin/javac') if args.java_home else shutil.which('javac')
    if not java or not javac:
        parser.error('A JDK is required; set JAVA_HOME or pass --java-home')
    args.out = args.out.resolve()
    args.out.mkdir(parents=True, exist_ok=True)
    build = ROOT / 'build'
    build.mkdir(exist_ok=True)
    # Reuse the repository's pure-Java source list; this does not run its test suites.
    pure = (ROOT / 'check.sh').read_text().split('PURE="', 1)[1].split('"', 1)[0].split()
    with tempfile.TemporaryDirectory(prefix='promotional-references-', dir=build) as directory:
        work = Path(directory)
        flags = work / 'BuildFlags.java'
        flags.write_text('package com.dddumpling.game; final class BuildFlags { '
                         'static final boolean DEVELOPER=false; '
                         'static final String BUILD_ID="promotional-reference"; }\n')
        sources = [str(ROOT / source) for source in pure]
        sources += [str(source) for source in sorted((ROOT / 'tools').glob('*.java'))]
        subprocess.run([javac, '-nowarn', '-d', str(work), *sources, str(flags)],
                       cwd=ROOT, check=True)
        subprocess.run([java, '-Xmx512m', '-cp', str(work),
                        'com.dddumpling.game.PromotionalReferences', str(args.out)],
                       cwd=ROOT, check=True)
    print(f'Rendered artwork and Survival ending references in {args.out}')


if __name__ == '__main__':
    main()
