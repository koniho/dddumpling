#!/usr/bin/env python3
"""Unit checks for configuration validation before the resolver touches the network."""
import argparse
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest


ROOT = Path(__file__).resolve().parent.parent
SPEC = importlib.util.spec_from_file_location('prepare_play', ROOT / 'tools' / 'prepare-play.py')
PREPARE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(PREPARE)


class PreparePlayTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.play = self.write('play.json', {'project_id': '123456789012'})
        self.firebase = self.write('google-services.json', {
            'project_info': {'project_number': '123456789012', 'project_id': 'dddumpling-test'},
            'client': [{'client_info': {'mobilesdk_app_id': '1:123456789012:android:abc123',
                         'android_client_info': {'package_name': 'com.dddumpling.game'}},
                        'api_key': [{'current_key': 'AIzaSyDddumplingFirebaseTestKey'}],
                        'services': {'analytics_service': {'status': '2'}}}],
        })
    def tearDown(self):
        self.temp.cleanup()
    def write(self, name, value):
        path = self.root / name
        path.write_text(json.dumps(value))
        return str(path)
    def selected(self, play=None, firebase=None):
        return PREPARE.selected(argparse.Namespace(play_config=play, firebase_config=firebase))
    def test_firebase_resources_are_deterministic(self):
        groups, values = self.selected(firebase=self.firebase)
        self.assertEqual(['firebase'], groups)
        self.assertEqual('1:123456789012:android:abc123', values['google_app_id'])
        self.assertEqual('123456789012', values['gcm_defaultSenderId'])
    def test_combined_uses_one_resolved_group(self):
        groups, values = self.selected(self.play, self.firebase)
        self.assertEqual(['combined'], groups)
        self.assertEqual('123456789012', values['game_services_project_id'])
    def test_rejects_missing_or_wrong_firebase_client(self):
        bad = json.loads(Path(self.firebase).read_text())
        bad['client'][0]['client_info']['android_client_info']['package_name'] = 'com.example.wrong'
        path = self.write('wrong.json', bad)
        with self.assertRaisesRegex(ValueError, 'exactly one client'):
            self.selected(firebase=path)
    def test_rejects_unconfigured_analytics(self):
        bad = json.loads(Path(self.firebase).read_text())
        bad['client'][0]['services']['analytics_service']['status'] = '1'
        path = self.write('disabled.json', bad)
        with self.assertRaisesRegex(ValueError, 'Enable Google Analytics'):
            self.selected(firebase=path)
    def test_accepts_legacy_play_event_mapping(self):
        path = self.write('events.json', {'project_id': '123456789012', 'events': {}})
        self.assertEqual((['play'], {'game_services_project_id': '123456789012'}), self.selected(play=path))
    def test_rejects_mismatched_android_app_id(self):
        bad = json.loads(Path(self.firebase).read_text())
        bad['client'][0]['client_info']['mobilesdk_app_id'] = '1:999999999999:android:abc123'
        path = self.write('app-id.json', bad)
        with self.assertRaisesRegex(ValueError, 'does not match project_number'):
            self.selected(firebase=path)
    def test_rejects_malformed_client_without_a_traceback(self):
        bad = json.loads(Path(self.firebase).read_text())
        bad['client'][0]['client_info'] = 'not an object'
        path = self.write('malformed.json', bad)
        with self.assertRaisesRegex(ValueError, 'exactly one client'):
            self.selected(firebase=path)


if __name__ == '__main__':
    unittest.main()
