#!/usr/bin/env python3
"""Verify the consent-gated Firebase shape of a production Android bundle."""
import subprocess
import sys
import xml.etree.ElementTree as ET
from zipfile import ZipFile


BUNDLETOOL = 'sdk/bundletool-all-1.18.3.jar'
ANDROID = '{http://schemas.android.com/apk/res/android}'
REQUIRED_FALSE = {
    'firebase_analytics_collection_enabled',
    'google_analytics_adid_collection_enabled',
    'google_analytics_default_allow_ad_storage',
    'google_analytics_default_allow_ad_user_data',
    'google_analytics_default_allow_ad_personalization_signals',
}
REMOVED_PROVIDERS = {
    'com.google.firebase.provider.FirebaseInitProvider',
    'com.google.android.gms.measurement.AppMeasurementContentProvider',
}


def verify(bundle):
    manifest = subprocess.check_output([
        'java', '-jar', BUNDLETOOL, 'dump', 'manifest', '--bundle=' + bundle, '--module=base'], text=True)
    root = ET.fromstring(manifest)
    assert root.get('package') == 'com.dddumpling.game', 'Not the production application ID'
    permissions = {node.get(ANDROID + 'name') for node in root.findall('uses-permission')}
    assert 'com.google.android.gms.permission.AD_ID' not in permissions, 'Advertising ID permission in Firebase bundle'
    app = root.find('application')
    metadata = {node.get(ANDROID + 'name'): node.get(ANDROID + 'value') for node in app.findall('meta-data')}
    for name in REQUIRED_FALSE:
        assert metadata.get(name) == 'false', name + ' must be false before consent'
    providers = {node.get(ANDROID + 'name') for node in app.findall('provider')}
    assert not providers.intersection(REMOVED_PROVIDERS), 'Firebase initialized before player consent'
    with ZipFile(bundle) as archive:
        dex = [name for name in archive.namelist() if name.endswith('.dex')]
        assert dex, 'Bundle has no executable code'
        payload = b''.join(archive.read(name) for name in dex)
    assert b'com/google/firebase/analytics/FirebaseAnalytics;' in payload, 'Firebase Analytics code missing'
    print('Verified Firebase production bundle: consent-gated analytics and advertising controls')


if __name__ == '__main__':
    verify(sys.argv[1] if len(sys.argv) > 1 else 'build/DDDUMPLING.aab')
