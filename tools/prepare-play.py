#!/usr/bin/env python3
"""Prepare optional Play Games/Firebase SDKs for the custom Android build."""
import argparse
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET
import zipfile


PACKAGE = 'com.dddumpling.game'
ANDROID = '{http://schemas.android.com/apk/res/android}'
TOOLS = '{http://schemas.android.com/tools}'


def read_json(path, label):
    try:
        value = json.loads(Path(path).read_text())
    except (OSError, json.JSONDecodeError) as error:
        raise ValueError('Could not read ' + label + ': ' + str(error)) from error
    if not isinstance(value, dict):
        raise ValueError(label + ' must contain a JSON object')
    return value


def text(value, label):
    if not isinstance(value, str) or not value:
        raise ValueError('Missing ' + label)
    return value


def play_config(path):
    value = read_json(path, 'Play Games configuration')
    project = text(value.get('project_id'), 'numeric Play Games project_id')
    if not re.fullmatch(r'[0-9]{6,30}', project):
        raise ValueError('Set the numeric Play Games project_id in the configuration')
    if set(value) - {'project_id', 'events'}:
        raise ValueError('Play Games configuration only needs project_id')
    return project


def firebase_config(path):
    value = read_json(path, 'google-services.json')
    project = value.get('project_info')
    if not isinstance(project, dict):
        raise ValueError('google-services.json has no project_info')
    number = text(project.get('project_number'), 'project_info.project_number')
    app_project = text(project.get('project_id'), 'project_info.project_id')
    if not re.fullmatch(r'[0-9]{6,30}', number):
        raise ValueError('google-services.json project_number is not numeric')
    clients = value.get('client')
    if not isinstance(clients, list):
        raise ValueError('google-services.json has no client list')
    def package(client):
        info = client.get('client_info') if isinstance(client, dict) else None
        android = info.get('android_client_info') if isinstance(info, dict) else None
        return android.get('package_name') if isinstance(android, dict) else None
    matches = [client for client in clients if package(client) == PACKAGE]
    if len(matches) != 1:
        raise ValueError('google-services.json needs exactly one client for ' + PACKAGE)
    client = matches[0]
    info = client.get('client_info')
    if not isinstance(info, dict):
        raise ValueError('google-services.json client has no client_info')
    app_id = text(info.get('mobilesdk_app_id'), 'client_info.mobilesdk_app_id')
    if not app_id.startswith('1:' + number + ':android:'):
        raise ValueError('google-services.json Android app ID does not match project_number')
    keys = client.get('api_key')
    if not isinstance(keys, list) or not keys:
        raise ValueError('google-services.json client has no api_key')
    api_key = text(keys[0].get('current_key') if isinstance(keys[0], dict) else None,
                   'api_key.current_key')
    analytics = client.get('services', {}).get('analytics_service', {})
    if isinstance(analytics, dict) and analytics.get('status') == '1':
        raise ValueError('Enable Google Analytics in the Firebase project before building')
    values = {
        'gcm_defaultSenderId': number,
        'google_api_key': api_key,
        'google_app_id': app_id,
        'google_crash_reporting_api_key': api_key,
        'project_id': app_project,
    }
    bucket = project.get('storage_bucket')
    if isinstance(bucket, str) and bucket:
        values['google_storage_bucket'] = bucket
    oauth = client.get('oauth_client')
    if isinstance(oauth, list):
        web = [entry.get('client_id') for entry in oauth if isinstance(entry, dict)
               and entry.get('client_type') == 3 and isinstance(entry.get('client_id'), str)]
        if web:
            values['default_web_client_id'] = web[0]
    return values


def compile_resources(out, values, resources):
    if not values:
        return
    directory = out / 'config-res' / 'values'
    directory.mkdir(parents=True)
    root = ET.Element('resources')
    for name in sorted(values):
        node = ET.SubElement(root, 'string', {'name': name, 'translatable': 'false'})
        node.text = values[name]
    ET.ElementTree(root).write(directory / 'services.xml', encoding='utf-8', xml_declaration=True)
    archive = out / 'config.zip'
    subprocess.run(['aapt2', 'compile', '--dir', str(directory.parent), '-o', str(archive)], check=True)
    resources.append(str(archive))


def selected(args):
    groups, config_values = [], {}
    if args.play_config:
        config_values['game_services_project_id'] = play_config(args.play_config)
    if args.firebase_config:
        config_values.update(firebase_config(args.firebase_config))
    if args.play_config and args.firebase_config:
        groups.append('combined')
    elif args.play_config:
        groups.append('play')
    elif args.firebase_config:
        groups.append('firebase')
    else:
        raise ValueError('Specify a Play Games or Firebase configuration')
    return groups, config_values


def prepare(args):
    groups, config_values = selected(args)

    out = Path('build/play')
    if out.exists():
        shutil.rmtree(out)
    out.mkdir(parents=True)
    resolved = Path('tools/play-deps/build/resolved')
    env = dict(os.environ, GRADLE_USER_HOME=str(Path('build/gradle-cache').resolve()))
    subprocess.run(['gradle', '-p', 'tools/play-deps', '--no-daemon', 'resolve'], env=env, check=True)
    jars, manifests, packages, resources = [], [], [], []
    for group in groups:
        for dependency in sorted((resolved / group).iterdir()):
            if dependency.suffix == '.jar':
                jars.append(str(dependency.resolve()))
                continue
            if dependency.suffix != '.aar':
                raise ValueError('Unsupported dependency: ' + dependency.name)
            directory = out / (group + '-' + dependency.stem)
            directory.mkdir()
            with zipfile.ZipFile(dependency) as aar:
                for info in aar.infolist():
                    target = (directory / info.filename).resolve()
                    if not target.is_relative_to(directory.resolve()):
                        raise ValueError('Unsafe archive path')
                aar.extractall(directory)
            if (directory / 'jni').exists():
                raise ValueError('Native dependency requires explicit ABI packaging')
            manifest = directory / 'AndroidManifest.xml'
            manifests.append(str(manifest))
            packages.append(ET.parse(manifest).getroot().attrib['package'])
            jars += [str(j.resolve()) for j in directory.rglob('*.jar') if j.name != 'lint.jar']
            res = directory / 'res'
            if res.exists() and any(res.rglob('*.*')):
                dest = out / (group + '-' + dependency.stem + '.zip')
                subprocess.run(['aapt2', 'compile', '--dir', str(res), '-o', str(dest)], check=True)
                resources.append(str(dest))
            if (directory / 'assets').exists():
                shutil.copytree(directory / 'assets', out / 'assets', dirs_exist_ok=True)

    ET.register_namespace('android', ANDROID[1:-1])
    ET.register_namespace('tools', TOOLS[1:-1])
    tree = ET.parse('AndroidManifest.xml')
    root = tree.getroot()
    app = root.find('application')
    if args.play_config:
        ET.SubElement(root, 'uses-permission', {ANDROID + 'name': 'android.permission.INTERNET'})
        app.set(ANDROID + 'name', 'com.dddumpling.game.PlayApplication')
        ET.SubElement(app, 'meta-data', {ANDROID + 'name': 'com.google.android.gms.games.APP_ID',
                                         ANDROID + 'value': '@string/game_services_project_id'})
    if args.firebase_config:
        ET.SubElement(root, 'uses-permission', {ANDROID + 'name': 'com.google.android.gms.permission.AD_ID',
                                                TOOLS + 'node': 'remove'})
        for provider in ['com.google.firebase.provider.FirebaseInitProvider',
                         'com.google.android.gms.measurement.AppMeasurementContentProvider']:
            ET.SubElement(app, 'provider', {ANDROID + 'name': provider, TOOLS + 'node': 'remove'})
        ET.SubElement(app, 'meta-data', {ANDROID + 'name': 'firebase_analytics_collection_enabled',
                                         ANDROID + 'value': 'false'})
        ET.SubElement(app, 'meta-data', {ANDROID + 'name': 'google_analytics_adid_collection_enabled',
                                         ANDROID + 'value': 'false'})
        ET.SubElement(app, 'meta-data', {ANDROID + 'name': 'google_analytics_default_allow_ad_storage',
                                         ANDROID + 'value': 'false'})
        ET.SubElement(app, 'meta-data', {ANDROID + 'name': 'google_analytics_default_allow_ad_user_data',
                                         ANDROID + 'value': 'false'})
        ET.SubElement(app, 'meta-data', {ANDROID + 'name': 'google_analytics_default_allow_ad_personalization_signals',
                                         ANDROID + 'value': 'false'})
    tree.write(out / 'main.xml', encoding='utf-8', xml_declaration=True)
    merger = str((resolved / 'merger' / '*').resolve())
    subprocess.run(['java', '-cp', merger, 'com.android.manifmerger.Merger',
                    '--main', str(out / 'main.xml'), '--libs', ':'.join(manifests),
                    '--placeholder', 'applicationId=' + PACKAGE,
                    '--property', 'MIN_SDK_VERSION=21', '--property', 'TARGET_SDK_VERSION=36',
                    '--remove-tools-declarations', '--out', str(out / 'AndroidManifest.xml')], check=True)
    compile_resources(out, config_values, resources)
    (out / 'jars.txt').write_text('\n'.join(jars) + '\n')
    (out / 'classpath.txt').write_text(os.pathsep.join(jars))
    (out / 'resources.txt').write_text('\n'.join(resources) + '\n')
    (out / 'packages.txt').write_text(':'.join(dict.fromkeys(packages)))
    print('Prepared ' + ', '.join(groups) + ' SDKs with ' + str(len(jars))
          + ' jars and ' + str(len(resources)) + ' resource archives')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--play-config')
    parser.add_argument('--firebase-config')
    prepare(parser.parse_args())
