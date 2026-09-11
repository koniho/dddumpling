#!/usr/bin/env python3
"""Prepare the optional SDK for the existing aapt2/javac/d8 build, using Gradle only to resolve jars."""
import argparse
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET
import zipfile


def catalog():
    keys = ['runs_started', 'runs_finished', 'runs_abandoned', 'stages_completed']
    for group in ['stage_reached', 'run_end']:
        keys += [group + '_' + bucket for bucket in ['1_4', '5_9', '10_14', '15_19', '20_plus']]
    for boss in ['slime', 'dark_divide', 'octopulse', 'fly_agaric']:
        keys += ['boss_' + boss + '_' + suffix for suffix in [
            'started', 'won', 'failed', 'abandoned', 'no_damage', 'first_hit_count',
            'first_hit_ms_total', 'first_hit_under_5s', 'first_hit_5_15s',
            'first_hit_15_30s', 'first_hit_30_60s', 'first_hit_60s_plus']]
    for game in ['steamer', 'starpath']:
        keys += [game + '_' + suffix for suffix in ['started', 'won', 'failed', 'abandoned']]
    keys += ['rewards_' + suffix for suffix in ['total', 'new', 'duplicate', 'steamer', 'starpath', 'boss']]
    return keys


def config(path):
    value = json.loads(Path(path).read_text())
    project = value.get('project_id', '')
    events = value.get('events', {})
    if not isinstance(project, str) or not re.fullmatch(r'[0-9]{6,30}', project):
        raise ValueError('Set the numeric Play Games project_id in the configuration')
    if set(events) != set(catalog()):
        raise ValueError('Event keys must match store/play-games.example.json exactly')
    if any(not isinstance(v, str) or not re.fullmatch(r'[A-Za-z0-9_-]{8,200}', v) for v in events.values()):
        raise ValueError('Every event needs its real Play Console ID; blank placeholders are not allowed')
    if len(set(events.values())) != len(events):
        raise ValueError('Each event must have a distinct Play Console ID')
    return value


def prepare(args):
    cfg = config(args.config)
    out = Path('build/play')
    if out.exists():
        shutil.rmtree(out)
    out.mkdir(parents=True)
    resolved = Path('tools/play-deps/build/resolved')
    env = dict(os.environ, GRADLE_USER_HOME=str(Path('build/gradle-cache').resolve()))
    subprocess.run(['gradle', '-p', 'tools/play-deps', '--no-daemon', 'resolve'], env=env, check=True)
    jars, manifests, packages, resources = [], [], [], []
    for dependency in sorted((resolved / 'play').iterdir()):
        if dependency.suffix == '.jar':
            jars.append(str(dependency.resolve()))
            continue
        if dependency.suffix != '.aar':
            raise ValueError('Unsupported dependency: ' + dependency.name)
        directory = out / dependency.stem
        directory.mkdir()
        with zipfile.ZipFile(dependency) as aar:
            for info in aar.infolist():
                target = (directory / info.filename).resolve()
                if not target.is_relative_to(directory.resolve()):
                    raise ValueError('Unsafe archive path')
            aar.extractall(directory)
        if (directory / 'jni').exists():
            raise ValueError('Native dependency requires explicit ABI packaging')
        manifests.append(str(directory / 'AndroidManifest.xml'))
        packages.append(ET.parse(manifests[-1]).getroot().attrib['package'])
        jars += [str(j.resolve()) for j in directory.rglob('*.jar')]
        res = directory / 'res'
        if res.exists() and any(res.rglob('*.*')):
            dest = out / (dependency.stem + '.zip')
            subprocess.run(['aapt2', 'compile', '--dir', str(res), '-o', str(dest)], check=True)
            resources.append(str(dest))
        if (directory / 'assets').exists():
            shutil.copytree(directory / 'assets', out / 'assets', dirs_exist_ok=True)
    android = '{http://schemas.android.com/apk/res/android}'
    ET.register_namespace('android', android[1:-1])
    tree = ET.parse('AndroidManifest.xml')
    root = tree.getroot()
    ET.SubElement(root, 'uses-permission', {android + 'name': 'android.permission.INTERNET'})
    app = root.find('application')
    app.set(android + 'name', 'com.dddumpling.game.PlayApplication')
    ET.SubElement(app, 'meta-data', {android + 'name': 'com.google.android.gms.games.APP_ID',
                                    android + 'value': '@string/game_services_project_id'})
    tree.write(out / 'main.xml', encoding='utf-8', xml_declaration=True)
    merger = str((resolved / 'merger' / '*').resolve())
    subprocess.run(['java', '-cp', merger, 'com.android.manifmerger.Merger',
                    '--main', str(out / 'main.xml'), '--libs', ':'.join(manifests),
                    '--placeholder', 'applicationId=com.dddumpling.game',
                    '--property', 'MIN_SDK_VERSION=21', '--property', 'TARGET_SDK_VERSION=36',
                    '--remove-tools-declarations', '--out', str(out / 'AndroidManifest.xml')], check=True)
    res = out / 'config-res' / 'values'
    res.mkdir(parents=True)
    (res / 'play.xml').write_text('<resources><string name="game_services_project_id" translatable="false">' + cfg['project_id'] + '</string></resources>')
    subprocess.run(['aapt2', 'compile', '--dir', str(res.parent), '-o', str(out / 'config.zip')], check=True)
    resources.append(str(out / 'config.zip'))
    java = Path('build/gen/com/dddumpling/game/PlayConfig.java')
    java.parent.mkdir(parents=True, exist_ok=True)
    java.write_text('package com.dddumpling.game;\nfinal class PlayConfig {\n'
                   'static String event(String name) { switch (name) {\n' + ''.join(
                       'case ' + json.dumps(k) + ': return ' + json.dumps(v) + ';\n' for k, v in cfg['events'].items()) +
                   'default: return null; } }\n}\n')
    (out / 'jars.txt').write_text('\n'.join(jars) + '\n')
    (out / 'classpath.txt').write_text(os.pathsep.join(jars))
    (out / 'resources.txt').write_text('\n'.join(resources) + '\n')
    (out / 'packages.txt').write_text(':'.join(packages))
    print('Prepared Play Games SDK with', len(jars), 'jars and', len(resources), 'resource archives')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--config')
    parser.add_argument('--example', action='store_true')
    args = parser.parse_args()
    if args.example:
        print(json.dumps({'project_id': '', 'events': dict.fromkeys(catalog(), '')}, indent=2))
    else:
        prepare(args)
