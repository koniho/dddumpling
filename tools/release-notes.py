#!/usr/bin/env python3
"""Edit player-facing release notes without editing Java. See docs/release-notes.md."""
import argparse
import json
import os
import shutil
import subprocess
from pathlib import Path
import re
import textwrap
from urllib.parse import quote

ROOT = Path(__file__).resolve().parent.parent
SOURCE = Path('release-notes/releases.json')
OUTPUT = Path('src/com/dddumpling/game/ReleaseContent.java')
ICONS = ('travel', 'stars', 'bugs', 'shuffle', 'disguise', 'slime', 'pair', 'flex', 'team', 'news', 'flurry', 'misc', 'settings', 'swipe', 'scores', 'octopulse')
ART = {'travel': 8, 'shuffle': 9, 'pair': 9}


def text(value, label, limit=180):
    if not isinstance(value, str) or not value.strip() or len(value) > limit:
        raise ValueError(f'{label}: enter 1–{limit} characters')
    if any(ord(c) < 32 or ord(c) > 126 for c in value):
        raise ValueError(f'{label}: use plain English punctuation (straight quotes, no emoji)')
    return value.strip()


def keys(value, required, label):
    if not isinstance(value, dict) or set(value) != set(required):
        raise ValueError(f'{label}: expected fields {", ".join(required)}')


def validate(data):
    keys(data, ('releases',), 'catalog')
    if not isinstance(data['releases'], list) or not data['releases']:
        raise ValueError('Add at least one release')
    seen = set()
    previous = None
    for release in data['releases']:
        keys(release, ('version', 'changes'), 'release')
        version = release['version']
        if not isinstance(version, str) or not re.fullmatch(r'\d+\.\d+\.\d+', version) or version in seen:
            raise ValueError('Release versions must be unique, for example 0.1.20')
        number = tuple(map(int, version.split('.')))
        if previous is not None and number >= previous:
            raise ValueError('Keep releases newest first')
        previous = number
        seen.add(version)
        if not isinstance(release['changes'], list) or not release['changes']:
            raise ValueError(f'{version}: add at least one change')
        groups = set()
        for change in release['changes']:
            if not isinstance(change, dict) or change.get('icon') not in ICONS:
                raise ValueError(f'{version}: choose an icon from {", ".join(ICONS)}')
            label = f'{version}/{change["icon"]}'
            if change['icon'] in ('bugs', 'misc'):
                icon = change['icon']
                if icon in groups:
                    raise ValueError(f'{version}: group entries under one {icon} icon')
                groups.add(icon)
                field = 'fixes' if icon == 'bugs' else 'improvements'
                keys(change, ('icon', 'title', 'autoReset', field), label)
                if not isinstance(change[field], list) or not change[field]:
                    raise ValueError(f'{label}: add at least one {field} entry')
                for point in change[field]:
                    keys(point, ('where', 'why'), label)
            else:
                keys(change, ('icon', 'title', 'autoReset', 'where', 'why'), label)
            if type(change['autoReset']) is not bool:
                raise ValueError(f'{label}: choose autoReset true or false (restart two seconds after activation)')
            text(change['title'], label + ' title', 24)
            rows, contexts = lines(change)
            height = ART.get(change['icon'], 5) + 6.0 + (len(rows) - 1) * 1.1 + sum(contexts[1:]) * .4
            if height > 23.5:
                raise ValueError(f'{label}: too much copy for the popup; shorten or combine the points')
    return data


def lines(change):
    rows, contexts = [], []
    field = {'bugs': 'fixes', 'misc': 'improvements'}.get(change['icon'])
    points = change[field] if field else [change]
    for point in points:
        if change['icon'] != 'bugs' or point.get('where') != '':
            rows.append(text(point.get('where'), 'where', 32).upper())
            contexts.append(True)
        wrapped = textwrap.wrap(text(point.get('why'), 'why'), width=32, break_long_words=False)
        if any(len(line) > 32 for line in wrapped):
            raise ValueError('why: use shorter words so the line fits')
        rows.extend(wrapped)
        contexts.extend([False] * len(wrapped))
    return rows, contexts


def array(values):
    return '{' + ','.join(json.dumps(v, ensure_ascii=True) for v in values) + '}'


def render(data):
    validate(data)
    releases = data['releases']
    changes, groups = [], []
    for release in releases:
        groups.append(list(range(len(changes), len(changes) + len(release['changes']))))
        changes.extend(release['changes'])
    fields = [
        ('String[]', 'VERSIONS', array([r['version'] for r in releases])),
        ('int[][]', 'ITEMS', '{' + ','.join(array(g) for g in groups) + '}'),
        ('int[]', 'ICONS', '{' + ','.join('ReleaseChange.' + c['icon'].upper() for c in changes) + '}'),
        ('boolean[]', 'AUTO_RESET', array([c['autoReset'] for c in changes])),
        ('String[]', 'TITLES', array([c['title'].upper() for c in changes])),
        ('String[][]', 'TEXT', '{\n        ' + ',\n        '.join(array(lines(c)[0]) for c in changes) + '\n    }'),
        ('boolean[][]', 'CONTEXT', '{' + ','.join(array(lines(c)[1]) for c in changes) + '}'),
    ]
    return ('package com.dddumpling.game;\n\n'
            '// Generated from release-notes/releases.json by tools/release-notes.py sync.\n'
            'final class ReleaseContent {\n' + ''.join(
                f'    static final {kind} {name}={value};\n' for kind, name, value in fields) + '}\n')


def load(path):
    return json.loads(path.read_text(encoding='utf-8'))


def review_image(root, draft, open_android=True):
    """Compile current game artwork against the draft, without changing the catalog."""
    validate({'releases': [draft]})
    build = root / 'build/release-review'
    build.mkdir(parents=True, exist_ok=True)
    generated = build / 'ReleaseContent.java'
    generated.write_text(render({'releases': [draft]}), encoding='utf-8')
    pure = re.search(r'PURE="(.*?)"', (root / 'check.sh').read_text(), re.S).group(1).split()
    sources = [str(root / name) for name in pure if name != str(OUTPUT)]
    subprocess.run(['sh', 'tools/build-flags.sh', str(build / 'flags'), 'false'], cwd=root, check=True)
    subprocess.run(['javac', '-nowarn', '-d', str(build), *sources,
                    str(generated), str(build / 'flags/com/dddumpling/game/BuildFlags.java'),
                    *[str(root / 'tools' / (name + '.java')) for name in
                      ('RasterPainter', 'Font', 'Png', 'ReleaseReview')]], cwd=root, check=True)
    output = root / ('build/release-' + draft['version'] + '-icons.png')
    subprocess.run(['java', '-cp', str(build), 'com.dddumpling.game.ReleaseReview', str(output)],
                   cwd=root, check=True)
    print(f'Release icon review: {output}', flush=True)
    if open_android and (os.environ.get('ANDROID_ROOT') or Path('/system/bin/app_process').exists()):
        opener = shutil.which('termux-open')
        if opener:
            activity = shutil.which('am')
            if activity:
                # Direct launch reports failures; termux-open hides its broadcast result.
                uri = 'content://com.termux.files' + quote(str(output), safe='/')
                subprocess.run([activity, 'start', '-a', 'android.intent.action.VIEW',
                                '-d', uri, '-t', 'image/png', '--grant-read-uri-permission'], check=True)
            else:
                subprocess.run([opener, '--view', '--content-type', 'image/png', str(output)], check=True)
            print('Requested Android image viewer; confirm the sheet is visible before review.')
        else:
            print('Android file viewer unavailable; open the PNG in Files manually.')
    return output


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo', type=Path, default=ROOT)
    sub = parser.add_subparsers(dest='command', required=True)
    sub.add_parser('sync', help='Validate the JSON and update the game copy')
    check = sub.add_parser('check', help='Check copy and generated Java without changing files')
    check.add_argument('--version', help='Also require this version as the newest in-game release')
    sub.add_parser('preview', help='Read the notes as a player would')
    new = sub.add_parser('new', help='Create an editable draft in build/')
    new.add_argument('version')
    add = sub.add_parser('add', help='Add a completed draft as the newest release and sync')
    add.add_argument('draft', type=Path)
    review = sub.add_parser('review-image', help='Render draft icons and open the Android file viewer')
    review.add_argument('draft', type=Path)
    review.add_argument('--no-open', action='store_true', help='Generate only (CI or desktop review)')
    args = parser.parse_args()
    root = args.repo.resolve()
    try:
        if args.command == 'review-image':
            review_image(root, load(args.draft), not args.no_open)
            return
        if args.command == 'new':
            if not re.fullmatch(r'\d+\.\d+\.\d+', args.version):
                raise ValueError('Use a version such as 0.1.20')
            path = root / f'build/release-{args.version}.json'
            path.parent.mkdir(parents=True, exist_ok=True)
            with path.open('x', encoding='utf-8') as output:
                json.dump({'version': args.version, 'changes': [
                    {'icon': 'misc', 'title': 'Little improvements', 'autoReset': None,
                     'improvements': [{'where': '', 'why': ''}]}]}, output, indent=2)
                output.write('\n')
            print(f'Edit {path}, then run: python3 tools/release-notes.py add {path}')
            return
        data = load(root / SOURCE)
        if args.command == 'add':
            draft = load(args.draft)
            validate({'releases': [draft]})
            data['releases'].insert(0, draft)
        generated = render(data)
        if args.command == 'check':
            if args.version and data['releases'][0]['version'] != args.version:
                raise ValueError(f'Finish notes for {args.version} before tagging; newest notes are {data["releases"][0]["version"]}')
            if not (root / OUTPUT).exists() or (root / OUTPUT).read_text() != generated:
                raise ValueError('Game copy is stale. Run: python3 tools/release-notes.py sync')
            print('Release notes checked')
        elif args.command == 'preview':
            for release in data['releases']:
                print('\n' + release['version'])
                for change in release['changes']:
                    print('\n  ' + change['title'])
                    print('\n'.join('    ' + row for row in lines(change)[0]))
        else:
            if args.command == 'add':
                (root / SOURCE).write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')
            (root / OUTPUT).write_text(generated, encoding='utf-8')
            print(f'Updated {OUTPUT}; review with: ./check.sh -q -s Visuals -f 103-release')
    except (ValueError, OSError, KeyError, TypeError, subprocess.CalledProcessError) as error:
        parser.exit(1, f'Release notes: {error}\n')


if __name__ == '__main__':
    main()
