import copy
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('notes', Path(__file__).with_name('release-notes.py'))
notes = importlib.util.module_from_spec(spec)
spec.loader.exec_module(notes)


class ReleaseNotes(unittest.TestCase):
    def setUp(self):
        catalog = notes.load(notes.ROOT / notes.SOURCE)
        self.data = {'releases': [r for r in catalog['releases'] if r['version'] in ('0.1.19', '0.1.18', '0.1.17')]}
        notes.validate(catalog)

    def test_context_and_player_purpose_survive_generation(self):
        for release in self.data['releases']:
            for change in release['changes']:
                rows, styles = notes.lines(change)
                self.assertEqual(len(rows), len(styles))
                self.assertTrue(styles[0])
                self.assertTrue(any(not flag for flag in styles))
                self.assertTrue(all(len(row) <= 32 for row in rows))
        self.assertIn('STAGE 11+ POWER-UPS', notes.render(self.data))

    def test_reset_choice_is_required_and_generated_per_entry(self):
        for value in (None, 'true', 2):
            data = copy.deepcopy(self.data)
            data['releases'][0]['changes'][0]['autoReset'] = value
            with self.assertRaisesRegex(ValueError, 'autoReset'):
                notes.render(data)
        data = copy.deepcopy(self.data)
        del data['releases'][0]['changes'][0]['autoReset']
        with self.assertRaisesRegex(ValueError, 'autoReset'):
            notes.render(data)
        data['releases'][0]['changes'][0]['autoReset'] = True
        self.assertIn('AUTO_RESET={true,', notes.render(data))
        data['releases'][0]['changes'][0]['autoReset'] = False
        self.assertIn('AUTO_RESET={false,', notes.render(data))

    def test_bug_context_can_be_omitted_without_blank_lines(self):
        change = {'icon': 'bugs', 'title': 'Happy little fixes', 'autoReset': False,
                  'fixes': [{'where': '', 'why': 'Partner pairs count as one stage enemy.'},
                            {'where': 'After a Star Path win', 'why': 'No more empty Star Path replays.'}]}
        rows, styles = notes.lines(change)
        self.assertEqual(rows[0], 'Partner pairs count as one stage')
        self.assertFalse(styles[0])
        self.assertNotIn('', rows)
        self.assertEqual(sum(styles), 1)
        notes.validate({'releases': [{'version': '1.0.0', 'changes': [change]}]})

    def test_actionable_copy_errors(self):
        for field in ('where', 'why'):
            data = copy.deepcopy(self.data)
            data['releases'][0]['changes'][0][field] = ''
            with self.assertRaisesRegex(ValueError, field):
                notes.render(data)
        data = copy.deepcopy(self.data)
        data['releases'][0]['changes'][0]['icon'] = 'typo'
        with self.assertRaisesRegex(ValueError, 'choose an icon'):
            notes.render(data)
        data = copy.deepcopy(self.data)
        data['releases'][0]['changes'].append(copy.deepcopy(data['releases'][0]['changes'][-1]))
        with self.assertRaisesRegex(ValueError, 'one bugs icon'):
            notes.render(data)
        data = copy.deepcopy(self.data)
        data['releases'][0]['changes'][-1]['fixes'] *= 12
        with self.assertRaisesRegex(ValueError, 'too much copy'):
            notes.render(data)

    def test_human_draft_add_sync_and_pre_tag_check(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / notes.SOURCE).parent.mkdir(parents=True)
            (root / notes.OUTPUT).parent.mkdir(parents=True)
            (root / notes.SOURCE).write_text(json.dumps(self.data))
            def run(*args):
                return subprocess.run([sys.executable, str(notes.ROOT / 'tools/release-notes.py'),
                    '--repo', str(root), *args], text=True, capture_output=True)
            self.assertEqual(run('sync').returncode, 0)
            self.assertEqual(run('check', '--version', '0.1.19').returncode, 0)
            self.assertIn('before tagging', run('check', '--version', '0.1.20').stderr)
            self.assertEqual(run('new', '0.1.20').returncode, 0)
            draft = root / 'build/release-0.1.20.json'
            self.assertIsNone(notes.load(draft)['changes'][0]['autoReset'])
            original = (root / notes.SOURCE).read_text()
            self.assertNotEqual(run('add', str(draft)).returncode, 0)
            self.assertEqual((root / notes.SOURCE).read_text(), original)
            change = copy.deepcopy(self.data['releases'][0])
            change['version'] = '0.1.20'
            draft.write_text(json.dumps(change))
            self.assertEqual(run('add', str(draft)).returncode, 0)
            self.assertEqual(len(notes.load(root / notes.SOURCE)['releases']), len(self.data['releases']) + 1)
            generated = (root / notes.OUTPUT).read_text()
            self.assertIn('"0.1.20","0.1.19","0.1.18"', generated)
            self.assertEqual(run('check', '--version', '0.1.20').returncode, 0)
            self.assertNotEqual(run('add', str(draft)).returncode, 0)
            (root / notes.OUTPUT).write_text('stale')
            self.assertIn('stale', run('check').stderr)
            self.assertEqual(run('sync').returncode, 0)


if __name__ == '__main__':
    unittest.main()
