#!/usr/bin/env python3
"""Exercise profiling orchestration without a device or real waits."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class GatorProfile(unittest.TestCase):
    def run_profile(self, behavior):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'tools').mkdir()
            shutil.copy(ROOT / 'tools/profile-gator.sh', root / 'tools')
            mock = root / 'rish'
            mock.write_text('#!' + shutil.which('python3') + '\n' + '''
import os, pathlib, shlex, sys
root=pathlib.Path(os.environ['MOCK_ROOT'])
command=sys.argv[2]
with (root/'calls').open('a') as log: log.write(command+'\\n')
if command.startswith('am start'):
    args=shlex.split(command)
    mode=args[args.index('gator_profile')+1]
    token=args[args.index('gator_profile_token')+1] if 'gator_profile_token' in args else ''
    (root/'state').write_text(mode+' '+token)
elif command.startswith('logcat') and os.environ['MOCK_BEHAVIOR']!='old':
    mode,token=(root/'state').read_text().split()
    print('ready='+mode+' token='+token)
    if os.environ['MOCK_BEHAVIOR']=='complete': print('loop=1 token='+token)
elif 'framestats' in command:
    print('Total frames rendered: 2160\\n50th percentile: 16ms')
''')
            mock.chmod(0o755)
            sleep = root / 'sleep'
            sleep.write_text('#!' + shutil.which('sh') + '\nexit 0\n')
            sleep.chmod(0o755)
            env = dict(os.environ, PATH=str(root) + os.pathsep + os.environ['PATH'],
                       MOCK_ROOT=str(root), MOCK_BEHAVIOR=behavior, GATOR_TRANSPORT='rish')
            result = subprocess.run(['sh', str(root / 'tools/profile-gator.sh')], env=env,
                                    capture_output=True, text=True, timeout=10)
            calls = (root / 'calls').read_text()
            self.assertTrue(calls.rstrip().endswith('--es gator_profile stop'))
            self.assertNotIn('pm clear', calls)
            self.assertNotIn('force-stop', calls)
            return result, calls

    def test_both_renderers_capture_and_restore(self):
        result, calls = self.run_profile('complete')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(calls.count('framestats'), 2)
        self.assertEqual(calls.count('gator_profile vector'), 2)
        self.assertEqual(calls.count('gator_profile cached'), 2)

    def test_old_binary_never_reports_a_capture(self):
        result, calls = self.run_profile('old')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('Replay did not start', result.stderr)
        self.assertNotIn('framestats', calls)

    def test_incomplete_loop_rejects_comparison(self):
        result, calls = self.run_profile('stalled')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('did not finish a full replay', result.stderr)
        self.assertEqual(calls.count('framestats'), 1)


if __name__ == '__main__':
    unittest.main()
