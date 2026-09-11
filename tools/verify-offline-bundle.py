#!/usr/bin/env python3
"""Reject a release bundle containing Play Games SDK code or network permissions."""
import subprocess
import sys
import xml.etree.ElementTree as ET
from zipfile import ZipFile

bundle = sys.argv[1] if len(sys.argv) > 1 else 'build/DDDUMPLING.aab'
manifest = subprocess.check_output([
    'java', '-jar', 'sdk/bundletool-all-1.18.3.jar', 'dump', 'manifest',
    '--bundle=' + bundle, '--module=base'], text=True)
root = ET.fromstring(manifest)
assert root.get('package') == 'com.dddumpling.game', 'Not the production application ID'
for node in root.iter():
    for value in node.attrib.values():
        assert value not in ('android.permission.INTERNET', 'android.permission.ACCESS_NETWORK_STATE'), \
            'Network permission in offline release'
        assert 'com.google.android.gms' not in value, 'Google Play services manifest entry'
        assert 'PlayApplication' not in value, 'Play Games application enabled'
with ZipFile(bundle) as archive:
    dex = [name for name in archive.namelist() if name.endswith('.dex')]
    assert dex, 'Bundle has no executable code'
    for name in dex:
        data = archive.read(name)
        assert b'com/google/android/gms/' not in data, 'Google Play services code in bundle'
        assert b'PlayApplication;' not in data, 'Play Games application in bundle'
print('Verified offline production bundle: no Play Games SDK or network permissions')
