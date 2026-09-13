#!/usr/bin/env python3
import importlib.util
from pathlib import Path
import plistlib
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('config', Path(__file__).with_name('firebase-config.py'))
config = importlib.util.module_from_spec(spec)
spec.loader.exec_module(config)


class ConfigTests(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.source = self.root / 'input.plist'
        self.output = self.root / 'app/GoogleService-Info.plist'
        self.env = dict(TARGET_BUILD_DIR=str(self.root), UNLOCALIZED_RESOURCES_FOLDER_PATH='app',
                        CONFIGURATION='Release', PRODUCT_BUNDLE_IDENTIFIER='com.dddumpling.game.ios',
                        IOS_FIREBASE_CONFIG=str(self.source))
        self.values = dict(GOOGLE_APP_ID='1:123:ios:abc123', GCM_SENDER_ID='123', API_KEY='test-only',
                           PROJECT_ID='test-only', BUNDLE_ID='com.dddumpling.game.ios')

    def write(self):
        self.source.write_bytes(plistlib.dumps(self.values))

    def test_release_copies_matching_configuration(self):
        self.write()
        config.configure(self.env)
        self.assertEqual(plistlib.loads(self.output.read_bytes()), self.values)

    def test_debug_removes_stale_configuration_even_with_explicit_path(self):
        self.write()
        config.configure(self.env)
        self.env['CONFIGURATION'] = 'Debug'
        config.configure(self.env)
        self.assertFalse(self.output.exists())

    def test_explicit_missing_config_fails(self):
        with self.assertRaises(ValueError):
            config.configure(self.env)

    def test_wrong_bundle_removes_stale_configuration_and_fails(self):
        self.write()
        config.configure(self.env)
        self.values['BUNDLE_ID'] = 'another.app'
        self.write()
        with self.assertRaises(ValueError):
            config.configure(self.env)
        self.assertFalse(self.output.exists())

    def test_incomplete_or_wrong_platform_config_fails(self):
        for key in self.values:
            original = self.values[key]
            self.values[key] = ''
            self.write()
            with self.assertRaises(ValueError):
                config.configure(self.env)
            self.values[key] = original
        self.values['GOOGLE_APP_ID'] = '1:123:android:abc123'
        self.write()
        with self.assertRaises(ValueError):
            config.configure(self.env)


if __name__ == '__main__':
    unittest.main()
