#!/usr/bin/env python3
"""Keep Play configuration aligned with the runtime's persisted event names."""
import json
from pathlib import Path
import re
import runpy
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
prepare = runpy.run_path(str(ROOT / 'tools/prepare-play.py'))
achievements = runpy.run_path(str(ROOT / 'tools/achievements.py'))


class PlayCatalogTest(unittest.TestCase):
    def test_achievement_platform_mappings_are_complete(self):
        achievements['validate'](ROOT / 'store/achievements.json')

    def test_future_boss_requires_achievement_provisioning(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            source = root / 'src/com/dddumpling/game'
            source.mkdir(parents=True)
            for name in ('Achievements.java', 'WorldCatalog.java'):
                (source/name).write_text((ROOT/'src/com/dddumpling/game'/name).read_text())
            world = source/'WorldCatalog.java'
            world.write_text(world.read_text() + '\nnew BossEntry(0,0,0,0,"Future","Costume","future_boss",')
            with patch.dict(achievements['catalog'].__globals__, ROOT=root):
                keys = [a['key'] for a in achievements['catalog']()]
                self.assertIn('boss_future_boss_first_win', keys)
                with self.assertRaisesRegex(ValueError, 'catalog mismatch'):
                    achievements['validate'](ROOT / 'store/achievements.json')

    def test_direct_runtime_events_have_console_definitions(self):
        source = (ROOT / 'src/com/dddumpling/game/Progress.java').read_text()
        events = set(re.findall(r'event\("([^"]+)"\)', source))
        self.assertFalse(events - set(prepare['catalog']()))

    def test_every_runtime_boss_has_console_events(self):
        runtime = set(prepare['boss_names']())
        configured = {key[len('boss_'):-len('_started')]
                      for key in prepare['catalog']()
                      if key.startswith('boss_') and key.endswith('_started')}
        self.assertEqual(runtime, configured)

    def test_example_matches_build_catalog(self):
        example = json.loads((ROOT / 'store/play-games.example.json').read_text())
        self.assertEqual(set(example['events']), set(prepare['catalog']()))

    def test_draft_has_real_ids_for_every_runtime_event(self):
        draft = json.loads((ROOT / 'store/play-games.draft.json').read_text())
        self.assertEqual(set(draft['events']), set(prepare['catalog']()))
        prepare['config'](ROOT / 'store/play-games.draft.json')

    def test_future_boss_and_duplicate_ids(self):
        source = (ROOT / 'src/com/dddumpling/game/WorldCatalog.java').read_text()
        future = source + '\nnew BossEntry(0,0,0,0,"Future","Costume","future_boss",'
        self.assertIn('future_boss', prepare['boss_names'](future))
        with self.assertRaises(ValueError):
            prepare['boss_names'](future + '\nnew BossEntry(0,0,0,0,"Other","Costume","future_boss",')

    def test_unprovisioned_counter_is_rejected(self):
        draft = json.loads((ROOT / 'store/play-games.draft.json').read_text())
        draft['events']['boss_rainbow_cat_started'] = ''
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'unprovisioned.json'
            path.write_text(json.dumps(draft))
            with self.assertRaisesRegex(ValueError, 'blank placeholders'):
                prepare['config'](path)


if __name__ == '__main__':
    unittest.main()
