#!/usr/bin/env python3
"""Choose CI jobs and XCTest targets. Unknown code fails open to the full suite."""
import argparse
import json
import os
from pathlib import Path
import subprocess

SUITES = {
    'audio': ['DDDumplingTests/DDAudioTests', 'DDDumplingTests/DDEffectMixerTests'],
    'storage': ['DDDumplingTests/DDStoreTests'],
    'render': ['DDDumplingTests/DDPainterTests', 'DDDumplingUITests/SmokeTests/testProductionScenesRender'],
    'input': ['DDDumplingUITests/SmokeTests/testTitleStartsGameAndBackgroundPauses',
              'DDDumplingUITests/SmokeTests/testBossAcceptsPinchAndSwipeWithoutCrashing'],
}


def plan(paths, full=False, requested='auto'):
    result = dict(shared=False, simulator=False, archive=False, tests=[], reasons=[])
    selected = set()
    broad = full or requested == 'full'
    for name in paths:
        p = Path(name)
        if p.suffix.lower() == '.md' and not name.startswith(('src/', 'ios/', '.github/', 'assets/')):
            continue
        if name.startswith(('docs/', 'app-store/', 'art/', 'skills/')) or name.startswith('ios/docs/') or name == 'ios/README.md':
            continue
        if name.startswith(('fastlane/', 'Gemfile')):
            continue  # Release configuration always runs on Linux.
        result['shared'] = True
        if name.startswith('tools/') and p.name.startswith(('Test', 'Bot', 'Check', 'Preview', 'Font', 'RasterPainter')):
            continue
        result['simulator'] = True
        if name.startswith('assets/fonts/') or p.name in ('DDPainter.m', 'DDPainter.h', 'DDPainterTests.m', 'Layout.java'):
            selected.add('render')
        elif p.name in ('Sfx.java', 'Music.java', 'Narration.java', 'Audio.java', 'DDAudio.m', 'DDAudio.h',
                         'DDEffectMixer.m', 'DDEffectMixer.h', 'DDAudioTests.m', 'DDEffectMixerTests.m'):
            selected.add('audio')
        elif p.name in ('DDStore.m', 'DDStore.h', 'DDStoreTests.m', 'Progress.java', 'ProgressData.java'):
            selected.add('storage')
        elif p.name in ('IOSGame.java', 'IOSTouch.java', 'IOSInputTest.java', 'GameView.java', 'SettingsUi.java') or p.name.startswith('Release') and p.suffix == '.java':
            selected.add('input')
        elif name.startswith('src/com/dddumpling/game/') and p.suffix == '.java' and p.name not in ('GameCore.java', 'MainActivity.java', 'Pause.java', 'CanvasPainter.java'):
            pass  # Shared gameplay is tested on Linux and compiled through J2ObjC.
        else:
            broad = True
            result['reasons'].append('full fallback: ' + name)
    if requested not in ('auto', 'full'):
        if requested not in (*SUITES, 'native', 'build'):
            raise ValueError('Unknown suite: ' + requested)
        # Manual focused runs explicitly override path selection; main/release full runs do not.
        if not full:
            broad = False
            selected = {requested} if requested in SUITES else set()
            result.update(shared=True, simulator=True)
            if requested == 'native':
                result['tests'] = ['DDDumplingTests']
    if broad:
        result.update(shared=True, simulator=True, archive=full or requested == 'full',
                      tests=['DDDumplingTests', 'DDDumplingUITests'])
    else:
        result['tests'] += sorted({target for suite in selected for target in SUITES[suite]})
    return result


def gate(needs):
    expected = {'changes', 'release_configuration', 'shared', 'simulator', 'archive'}
    if set(needs) != expected or needs['changes']['result'] != 'success':
        return False
    outputs = needs['changes'].get('outputs', {})
    if any(outputs.get(key) not in ('true', 'false') for key in ('shared', 'simulator', 'archive')):
        return False
    for job in expected - {'changes'}:
        required = job == 'release_configuration' or outputs.get(job) == 'true'
        if needs[job]['result'] != ('success' if required else 'skipped'):
            return False
    return True


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--base')
    parser.add_argument('--head', default='HEAD')
    parser.add_argument('--full', action='store_true')
    parser.add_argument('--suite', default='auto')
    parser.add_argument('--gate', action='store_true')
    args = parser.parse_args()
    if args.gate:
        if not gate(json.loads(os.environ['CI_NEEDS'])):
            raise SystemExit('Selected iOS checks did not all succeed')
        print('All selected iOS checks passed')
        return
    paths = []
    if args.base:
        # No rename detection: include both old and new paths, including deletions.
        raw = subprocess.check_output(['git', 'diff', '--no-renames', '--name-only', '-z',
                                       args.base + '...' + args.head])
        paths = raw.decode().rstrip('\0').split('\0') if raw else []
    result = plan(paths, args.full, args.suite)
    print(json.dumps(result, indent=2))
    if os.environ.get('GITHUB_OUTPUT'):
        with open(os.environ['GITHUB_OUTPUT'], 'a') as out:
            for key in ('shared', 'simulator', 'archive'):
                out.write(f'{key}={str(result[key]).lower()}\n')
            out.write('tests=' + json.dumps(result['tests'], separators=(',', ':')) + '\n')
    if os.environ.get('GITHUB_STEP_SUMMARY'):
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as out:
            out.write('### iOS test selection\n```json\n' + json.dumps(result, indent=2) + '\n```\n')


if __name__ == '__main__':
    main()
