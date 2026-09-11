import importlib.util
from pathlib import Path
import subprocess
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('release', Path(__file__).with_name('prepare-release.py'))
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)


class ReleasePreparation(unittest.TestCase):
    def test_preview_write_and_rejections(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            manifest = root / 'AndroidManifest.xml'
            original = '<manifest xmlns:android="http://schemas.android.com/apk/res/android" android:versionName="0.1.9" android:versionCode="10"><!-- keep --></manifest>'
            manifest.write_text(original)
            notes = root / 'draft.txt'
            notes.write_text('Smoother boss animations.')
            preview = release.prepare(root, notes)
            self.assertEqual((preview['version_name'], preview['version_code']), ('0.1.10', 11))
            self.assertEqual(manifest.read_text(), original)
            self.assertFalse((root / preview['notes_file']).exists())
            for code in (9, 10, 2100000001):
                with self.assertRaises(ValueError):
                    release.prepare(root, notes, code=code, write=True)
            notes.write_text('x' * 501)
            with self.assertRaises(ValueError):
                release.prepare(root, notes, write=True)
            self.assertEqual(manifest.read_text(), original)
            notes.write_text('• Improved controls.')
            result = release.prepare(root, notes, write=True)
            self.assertEqual(release.current(root), ('0.1.10', 11))
            self.assertIn('<!-- keep -->', manifest.read_text())
            self.assertEqual((root / result['notes_file']).read_text(), '• Improved controls.\n')
            manifest.write_text(original)
            with self.assertRaises(ValueError):
                release.prepare(root, notes, write=True)
            self.assertEqual(manifest.read_text(), original)

    def test_context_excludes_unmerged_work(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            def git(*args):
                return release.git(root, *args)
            git('init', '-q')
            git('config', 'user.email', 'test@example.invalid')
            git('config', 'user.name', 'Test')
            (root / 'AndroidManifest.xml').write_text('<manifest xmlns:android="http://schemas.android.com/apk/res/android" android:versionName="1.0.0" android:versionCode="1"/>')
            git('add', '.')
            git('commit', '-qm', 'First release')
            git('tag', 'v1.0.0')
            main = git('branch', '--show-current')
            git('checkout', '-qb', 'unmerged')
            git('commit', '--allow-empty', '-qm', 'Unreleased cloud saves')
            git('tag', 'v9.0.0')
            git('checkout', '-q', main)
            git('commit', '--allow-empty', '-qm', 'Improve controls')
            evidence = release.context(root)
            self.assertEqual(evidence['base'], 'v1.0.0')
            self.assertEqual([c['message'] for c in evidence['commits']], ['Improve controls'])
            self.assertEqual(release.context(root, 'HEAD')['commits'], [])
            with self.assertRaises(subprocess.CalledProcessError):
                release.context(root, 'v9.0.0')


if __name__ == '__main__':
    unittest.main()
