#!/usr/bin/env python3
"""Install only a matching production Firebase config; never reuse a stale bundle copy."""
import os
from pathlib import Path
import plistlib


def configure(env):
    target = Path(env['TARGET_BUILD_DIR']) / env['UNLOCALIZED_RESOURCES_FOLDER_PATH']
    output = target / 'GoogleService-Info.plist'
    output.unlink(missing_ok=True)
    if env.get('CONFIGURATION') != 'Release':
        print('Firebase analytics: disabled in developer builds')
        return
    source = Path(env.get('IOS_FIREBASE_CONFIG') or
                  str(Path(__file__).resolve().parents[2] / '.private/firebase/GoogleService-Info.plist'))
    if not source.is_file():
        if env.get('IOS_FIREBASE_CONFIG'):
            raise ValueError('IOS_FIREBASE_CONFIG does not name a readable file')
        print('Firebase analytics: disabled (no project configuration)')
        return
    with source.open('rb') as stream:
        config = plistlib.load(stream)
    for key in ('GOOGLE_APP_ID', 'GCM_SENDER_ID', 'API_KEY', 'PROJECT_ID', 'BUNDLE_ID'):
        if not isinstance(config.get(key), str) or not config[key].strip():
            raise ValueError('Firebase configuration is missing ' + key)
    if config['BUNDLE_ID'] != env['PRODUCT_BUNDLE_IDENTIFIER']:
        raise ValueError('Firebase BUNDLE_ID does not match the built app')
    if not config['GOOGLE_APP_ID'].startswith('1:' + config['GCM_SENDER_ID'] + ':ios:'):
        raise ValueError('Firebase GOOGLE_APP_ID does not match the Apple app/sender')
    target.mkdir(parents=True, exist_ok=True)
    output.write_bytes(plistlib.dumps(config))
    print('Firebase analytics: configured; collection requires player consent')


if __name__ == '__main__':
    configure(os.environ)
