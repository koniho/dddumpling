#!/usr/bin/env python3
import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from zipfile import ZipFile


ROOT = Path(__file__).resolve().parent.parent
SPEC = importlib.util.spec_from_file_location('verify_firebase', ROOT / 'tools' / 'verify-firebase-bundle.py')
VERIFY = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(VERIFY)


def manifest(permission=''):
    return '''<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.dddumpling.game">
    %s <application>%s</application></manifest>''' % (permission, ''.join(
        '<meta-data android:name="%s" android:value="false" />' % name for name in VERIFY.REQUIRED_FALSE))


class VerifyFirebaseBundleTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.bundle = Path(self.temp.name) / 'game.aab'
        with ZipFile(self.bundle, 'w') as archive:
            archive.writestr('base/dex/classes.dex', b'com/google/firebase/analytics/FirebaseAnalytics;')
    def tearDown(self):
        self.temp.cleanup()
    def validate(self, xml):
        with patch.object(VERIFY.subprocess, 'check_output', return_value=xml):
            VERIFY.verify(str(self.bundle))
    def test_accepts_consent_gated_bundle(self):
        self.validate(manifest())
    def test_rejects_advertising_id_permission(self):
        xml = manifest('<uses-permission android:name="com.google.android.gms.permission.AD_ID" />')
        with self.assertRaisesRegex(AssertionError, 'Advertising ID'):
            self.validate(xml)


if __name__ == '__main__':
    unittest.main()
