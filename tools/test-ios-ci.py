#!/usr/bin/env python3
"""Regression contracts for selective CI and safe translation reuse; no Xcode required."""
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import shutil
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


def module(name):
    spec = importlib.util.spec_from_file_location(name, ROOT / 'ios/scripts' / (name + '.py'))
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result


ci = module('ci-plan')
cache = module('translate-cache')


class Selection(unittest.TestCase):
    def test_docs_need_no_mac(self):
        for path in ['README.md', 'docs/releasing.md', 'ios/docs/ci.md', 'app-store/notes.txt']:
            result = ci.plan([path])
            self.assertFalse(result['simulator'], path)
            self.assertFalse(result['shared'], path)

    def test_gameplay_compiles_without_booting(self):
        result = ci.plan(['src/com/dddumpling/game/Pacing.java'])
        self.assertTrue(result['shared'])
        self.assertTrue(result['simulator'])
        self.assertEqual(result['tests'], [])
        self.assertFalse(result['archive'])

    def test_native_dependencies(self):
        for path, suite in [('src/com/dddumpling/game/Sfx.java', 'audio'),
                            ('src/com/dddumpling/game/ReleaseNotes.java', 'input'),
                            ('src/com/dddumpling/game/ProgressData.java', 'storage'),
                            ('src/com/dddumpling/game/Layout.java', 'render'),
                            ('assets/fonts/Bungee-Regular.ttf', 'render'),
                            ('ios/tests/DDAudioTests.m', 'audio')]:
            self.assertEqual(ci.plan([path])['tests'], sorted(ci.SUITES[suite]), path)

    def test_union_and_unknown_fallback(self):
        result = ci.plan(['ios/DDDumpling/DDAudio.m', 'ios/DDDumpling/DDStore.m'])
        self.assertEqual(result['tests'], sorted(ci.SUITES['audio'] + ci.SUITES['storage']))
        for path in ['ios/DDDumpling/DDGameView.m', 'new-code/new.swift', 'ios/scripts/ci-plan.py',
                     '.github/workflows/ios.yml', 'src/com/dddumpling/game/GameCore.java']:
            result = ci.plan([path])
            self.assertEqual(result['tests'], ['DDDumplingTests', 'DDDumplingUITests'])
            self.assertFalse(result['archive'])

    def test_full_main_and_focused_manual(self):
        result = ci.plan(['README.md'], full=True)
        self.assertTrue(result['archive'])
        self.assertEqual(result['tests'], ['DDDumplingTests', 'DDDumplingUITests'])
        self.assertEqual(ci.plan([], requested='audio')['tests'], sorted(ci.SUITES['audio']))
        self.assertEqual(ci.plan([], requested='native')['tests'], ['DDDumplingTests'])
        self.assertEqual(ci.plan([], requested='build')['tests'], [])
        self.assertTrue(ci.plan([], requested='full')['archive'])
        with self.assertRaises(ValueError):
            ci.plan([], requested='typo')

    def test_gate_rejects_missing_or_skipped_required_jobs(self):
        needs = {'changes': {'result': 'success', 'outputs': {'shared': 'true', 'simulator': 'false', 'archive': 'false'}},
                 'release_configuration': {'result': 'success'}, 'shared': {'result': 'success'},
                 'simulator': {'result': 'skipped'}, 'archive': {'result': 'skipped'}}
        self.assertTrue(ci.gate(needs))
        for status in ['failure', 'cancelled', 'skipped']:
            needs['shared']['result'] = status
            self.assertFalse(ci.gate(needs))
        needs['shared']['result'] = 'success'
        del needs['simulator']
        self.assertFalse(ci.gate(needs))

    def test_deleted_and_renamed_paths_are_detected(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            def git(*args):
                return subprocess.check_output(['git', '-C', temp, *args], stderr=subprocess.DEVNULL)
            git('init'); git('config', 'user.name', 'test'); git('config', 'user.email', 'test@example.invalid')
            (root / 'DDAudio.m').write_text('old')
            git('add', '.'); git('commit', '-m', 'base')
            base = git('rev-parse', 'HEAD').decode().strip()
            git('mv', 'DDAudio.m', 'README.md'); git('commit', '-m', 'rename')
            env = os.environ.copy()
            env.pop('GITHUB_OUTPUT', None); env.pop('GITHUB_STEP_SUMMARY', None)
            result = subprocess.check_output(['python3', str(ROOT / 'ios/scripts/ci-plan.py'), '--base', base], cwd=temp, env=env)
            self.assertEqual(json.loads(result)['tests'], sorted(ci.SUITES['audio']))


class SimulatorScript(unittest.TestCase):
    def test_compile_only_focused_and_full_execution(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            scripts = root / 'ios/scripts'; scripts.mkdir(parents=True)
            build = root / 'ios/build'; build.mkdir()
            binary = root / 'bin'; binary.mkdir()
            for name in ('env.sh', 'test-simulator.sh'):
                shutil.copyfile(ROOT / 'ios/scripts' / name, scripts / name)
            (scripts / 'translate.sh').write_text('#!/bin/bash\nexit 0\n')
            (scripts / 'boot-simulator.sh').write_text('#!/bin/bash\necho unexpected-boot >&2; exit 1\n')
            fake = '#!/usr/bin/env python3\nimport json,os,sys\nwith open(os.environ["CALL_LOG"],"a") as out: out.write(json.dumps(sys.argv)+"\\n")\n'
            for name in ('xcodebuild', 'xcodegen', 'xcrun'):
                p = binary / name; p.write_text(fake); p.chmod(0o755)
            log = root / 'calls'
            env = dict(os.environ, PATH=str(binary)+os.pathsep+os.environ['PATH'],
                       JAVA_HOME=str(root / 'java'), CALL_LOG=str(log), SIMULATOR_ID='test-device',
                       DDDUMPLING_BUILD_ID='test-build')
            command = ['bash', str(scripts / 'test-simulator.sh')]
            subprocess.run(command + ['--build-only'], env=env, check=True, stdout=subprocess.DEVNULL)
            calls = [json.loads(line) for line in log.read_text().splitlines()]
            self.assertFalse(any(Path(call[0]).name == 'xcrun' for call in calls))
            self.assertTrue(any('build' in call and 'generic/platform=iOS Simulator' in call
                                and 'ONLY_ACTIVE_ARCH=YES' in call
                                and any(arg.startswith('ARCHS=') for arg in call) for call in calls))
            log.write_text('')
            (build / 'simulator-id').write_text('test-device')
            env['IOS_TEST_TARGETS'] = '["DDDumplingTests/DDAudioTests"]'
            subprocess.run(command + ['--selected'], env=env, check=True, stdout=subprocess.DEVNULL)
            calls = [json.loads(line) for line in log.read_text().splitlines()]
            compiles = [call for call in calls if 'build-for-testing' in call]
            tests = [call for call in calls if 'test-without-building' in call]
            self.assertEqual(len(compiles), 1); self.assertEqual(len(tests), 1)
            self.assertIn('-only-testing:DDDumplingTests/DDAudioTests', tests[0])
            self.assertLess(calls.index(compiles[0]), calls.index(tests[0]))
            self.assertTrue(any('bootstatus' in call for call in calls))
            for flags in (['--full'], []):
                log.write_text('')
                subprocess.run(command + flags, env=env, check=True, stdout=subprocess.DEVNULL)
                calls = [json.loads(line) for line in log.read_text().splitlines()]
                compiles = [call for call in calls if 'build-for-testing' in call]
                tests = [call for call in calls if 'test-without-building' in call]
                self.assertEqual(len(compiles), 1); self.assertEqual(len(tests), 1)
                self.assertFalse(any(arg.startswith('-only-testing:') for call in calls for arg in call))
                self.assertLess(calls.index(compiles[0]), calls.index(tests[0]))


class BuildIdentity(unittest.TestCase):
    def test_explicit_test_identity_and_default_release_identity(self):
        with tempfile.TemporaryDirectory() as temp:
            command = ['sh', str(ROOT / 'tools/build-flags.sh'), temp, 'true']
            env = dict(os.environ, DDDUMPLING_BUILD_ID='ios-check-abc123')
            subprocess.run(command, env=env, check=True)
            output = Path(temp) / 'com/dddumpling/game/BuildFlags.java'
            first = output.read_text()
            subprocess.run(command, env=env, check=True)
            self.assertEqual(first, output.read_text())
            self.assertIn('ios-check-abc123', first)
            del env['DDDUMPLING_BUILD_ID']
            subprocess.run(command, env=env, check=True)
            self.assertNotIn('ios-check-', output.read_text())
            env['DDDUMPLING_BUILD_ID'] = 'bad"identity'
            self.assertNotEqual(subprocess.run(command, env=env, stderr=subprocess.DEVNULL).returncode, 0)


class Translation(unittest.TestCase):
    def test_reuse_invalidation_deletion_and_failure(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            build = root / 'ios/build'
            build.mkdir(parents=True)
            for name in ['ios/prefixes.properties', 'ios/scripts/translate.sh', 'ios/scripts/translate-cache.py',
                         'ios/scripts/sources.py', 'ios/scripts/bootstrap.sh', 'src/A.java']:
                p = root / name; p.parent.mkdir(parents=True, exist_ok=True); p.write_text('one')
            source = root / 'src/A.java'
            (build / 'sources.list').write_text(str(source) + '\n')
            translator = root / 'fake-translator'
            translator.write_text('#!/usr/bin/env python3\nimport pathlib,sys\np=pathlib.Path(sys.argv[sys.argv.index("-d")+1]); (p/"A.m").write_text("generated")\n')
            translator.chmod(0o755)
            cache.translate(root, 'Debug', translator, ['v1'])
            output = build / 'generated/A.m'
            stamp = build / 'translation-cache.json'
            first_key = json.loads(stamp.read_text())['key']
            os.utime(output, (1000, 1000))
            cache.translate(root, 'Debug', translator, ['v1'])
            self.assertEqual(output.stat().st_mtime, 1000)
            source.write_text('two')
            cache.translate(root, 'Debug', translator, ['v1'])
            self.assertNotEqual(json.loads(stamp.read_text())['key'], first_key)
            self.assertEqual(output.stat().st_mtime, 1000)  # Identical output need not recompile.
            (build / 'generated/Removed.m').write_text('stale')
            cache.translate(root, 'Debug', translator, ['v1'])
            self.assertFalse((build / 'generated/Removed.m').exists())
            output.write_text('corrupt')
            cache.translate(root, 'Debug', translator, ['v1'])
            self.assertEqual(output.read_text(), 'generated')
            debug_key = json.loads(stamp.read_text())['key']
            cache.translate(root, 'Release', translator, ['v1'])
            self.assertNotEqual(json.loads(stamp.read_text())['key'], debug_key)
            translator.write_text('#!/usr/bin/env python3\nraise SystemExit(1)\n')
            with self.assertRaises(subprocess.CalledProcessError):
                cache.translate(root, 'Release', translator, ['v2'])
            self.assertFalse(stamp.exists())


if __name__ == '__main__':
    unittest.main()
