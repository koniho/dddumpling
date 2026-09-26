#!/usr/bin/env python3
"""Launch configuration and deployment regression checks; never contact a device."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'


class AndroidLaunch(unittest.TestCase):
    def test_game_reuses_top_activity(self):
        manifest = ET.parse(ROOT / 'AndroidManifest.xml')
        activity = next(a for a in manifest.findall('./application/activity')
                        if a.get(ANDROID + 'name') == 'com.dddumpling.game.MainActivity')
        self.assertEqual(activity.get(ANDROID + 'launchMode'), 'singleTop')

    def test_deployment_reuses_game_without_resetting_it(self):
        for option, package in [('--developer', 'com.dddumpling.game.dev'),
                                ('--production', 'com.dddumpling.game')]:
            with self.subTest(option=option), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                shutil.copyfile(ROOT / 'deploy.sh', root / 'deploy.sh')
                commands = {
                    'build.sh': 'exit 0\n',
                    'adb': 'printf "%s\\n" "$*" >> "$LAUNCH_TEST_LOG"\n'
                           'if [ "$1" = devices ]; then printf "test-device\\tdevice\\n"; fi\n',
                    'termux-notification': 'exit 0\n',
                }
                for name, body in commands.items():
                    script = root / name
                    script.write_text('#!' + shutil.which('sh') + '\n' + body)
                    script.chmod(0o755)
                log = root / 'commands.log'
                env = dict(os.environ, PATH=str(root) + os.pathsep + os.environ['PATH'],
                           LAUNCH_TEST_LOG=str(log))
                subprocess.run(['bash', str(root / 'deploy.sh'), option], env=env,
                               check=True, capture_output=True, timeout=10)
                self.assertEqual(log.read_text().splitlines(), [
                    'devices', 'install -r hexatype.apk',
                    'shell am start --activity-single-top -n '
                    + package + '/com.dddumpling.game.MainActivity',
                ])


if __name__ == '__main__':
    unittest.main()
