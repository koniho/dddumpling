#!/usr/bin/env python3
"""Collect release evidence, then prepare a version bump and reviewed Play notes."""
import argparse
import json
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET

ANDROID = '{http://schemas.android.com/apk/res/android}'


def git(root, *args):
    return subprocess.check_output(['git', '-C', str(root), *args], text=True).strip()


def current(root):
    manifest = ET.parse(root / 'AndroidManifest.xml').getroot()
    return manifest.attrib[ANDROID + 'versionName'], int(manifest.attrib[ANDROID + 'versionCode'])


def context(root, since=None):
    # Prefer the nearest reachable version tag, never a tag from an unmerged branch.
    base = since or git(root, 'describe', '--tags', '--match', 'v[0-9]*', '--abbrev=0', 'HEAD')
    base_sha = git(root, 'rev-parse', '--verify', base + '^{commit}')
    subprocess.run(['git', '-C', str(root), 'merge-base', '--is-ancestor', base_sha, 'HEAD'], check=True)
    head = git(root, 'rev-parse', 'HEAD')
    commits = []
    for sha in git(root, 'rev-list', '--reverse', base_sha + '..' + head).splitlines():
        commits.append({
            'sha': sha,
            'message': git(root, 'show', '-s', '--format=%B', sha),
            'files': git(root, 'diff-tree', '--no-commit-id', '--name-only', '-r', sha).splitlines(),
        })
    name, code = current(root)
    return {'base': base, 'base_sha': base_sha, 'head': head,
            'current_version_name': name, 'current_version_code': code,
            'working_tree': git(root, 'status', '--short'), 'commits': commits,
            'diff_stat': git(root, 'diff', '--stat', base_sha, head),
            'instruction': 'Review actual diffs and merged PRs. Write player-facing changes only; '
                           'omit build tooling, avoid unmerged features, and keep Play notes to 500 characters. '
                           'Confirm this base represents the last distributed build; use --since for manual releases.'}


def prepare(root, notes_path, name=None, code=None, write=False):
    old_name, old_code = current(root)
    if name is None:
        match = re.fullmatch(r'(\d+)\.(\d+)\.(\d+)', old_name)
        if not match:
            raise ValueError('Supply --version-name for a non-numeric current version')
        major, minor, patch = map(int, match.groups())
        name = f'{major}.{minor}.{patch + 1}'
    if not re.fullmatch(r'\d+\.\d+\.\d+(?:[-+][A-Za-z0-9.-]+)?', name) or name == old_name:
        raise ValueError('Use a new semantic version name, for example 0.1.10')
    code = old_code + 1 if code is None else code
    if not old_code < code <= 2100000000:
        raise ValueError('Version code must increase and be no greater than 2100000000')
    notes = notes_path.read_text(encoding='utf-8').strip()
    if not 1 <= len(notes) <= 500:
        raise ValueError(f'Play release notes must contain 1–500 characters (got {len(notes)})')
    destination = root / f'app-store/google-play/en-US/changelogs/{code}.txt'
    if destination.exists():
        raise ValueError(f'Refusing to overwrite existing release notes: {destination}')
    # Edit attributes in place to preserve manifest comments and formatting.
    manifest_path = root / 'AndroidManifest.xml'
    manifest = manifest_path.read_text(encoding='utf-8')
    for attribute, value in [('versionName', name), ('versionCode', str(code))]:
        manifest, count = re.subn(r'android:' + attribute + r'="[^"]*"',
                                  f'android:{attribute}="{value}"', manifest)
        if count != 1:
            raise ValueError(f'Expected one android:{attribute} attribute')
    result = {'old_version': old_name, 'version_name': name, 'version_code': code,
              'tag': 'v' + name, 'notes_file': str(destination.relative_to(root)),
              'characters': len(notes), 'notes': notes, 'written': write}
    if write:
        destination.parent.mkdir(parents=True, exist_ok=True)
        with destination.open('x', encoding='utf-8') as output:
            output.write(notes + '\n')
        try:
            manifest_path.write_text(manifest, encoding='utf-8')
        except OSError:
            destination.unlink()
            raise
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo', type=Path, default=Path(__file__).resolve().parent.parent)
    sub = parser.add_subparsers(dest='command', required=True)
    evidence = sub.add_parser('context', help='Collect changes since the previous release')
    evidence.add_argument('--since', help='Previous distributed tag or commit; defaults to nearest version tag')
    evidence.add_argument('--output', type=Path, help='Save review context instead of printing it')
    draft = sub.add_parser('prepare', help='Preview or write a version bump and reviewed notes')
    draft.add_argument('--notes', type=Path, required=True, help='UTF-8 file containing player-facing notes')
    draft.add_argument('--version-name', help='Defaults to next patch version')
    draft.add_argument('--version-code', type=int, help='Defaults to current code + 1; must be unused in Play')
    draft.add_argument('--write', action='store_true', help='Apply the preview; never commits, tags, or publishes')
    args = parser.parse_args()
    try:
        if args.command == 'context':
            result = context(args.repo.resolve(), args.since)
        else:
            result = prepare(args.repo.resolve(), args.notes, args.version_name, args.version_code, args.write)
        rendered = json.dumps(result, ensure_ascii=False, indent=2) + '\n'
        if args.command == 'context' and args.output:
            args.output.parent.mkdir(parents=True, exist_ok=True)
            args.output.write_text(rendered, encoding='utf-8')
            print(f'Release context saved to {args.output}')
        else:
            print(rendered, end='')
    except (OSError, ValueError, ET.ParseError, subprocess.CalledProcessError) as error:
        parser.exit(1, f'Release preparation failed: {error}\n')


if __name__ == '__main__':
    main()
