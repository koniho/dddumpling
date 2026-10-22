#!/usr/bin/env python3
"""Keep Play configuration aligned with the runtime's persisted event names."""
import json
from pathlib import Path
import re
import runpy
import unittest

ROOT = Path(__file__).resolve().parents[1]
prepare = runpy.run_path(str(ROOT / 'tools/prepare-play.py'))


class PlayCatalogTest(unittest.TestCase):
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

    def test_draft_missing_ids_block_enabled_build(self):
        draft = json.loads((ROOT / 'store/play-games.draft.json').read_text())
        self.assertEqual(set(draft['events']), set(prepare['catalog']()))
        if any(not value for value in draft['events'].values()):
            with self.assertRaisesRegex(ValueError, 'blank placeholders'):
                prepare['config'](ROOT / 'store/play-games.draft.json')
        else:
            prepare['config'](ROOT / 'store/play-games.draft.json')

    def test_future_boss_and_duplicate_ids(self):
        source = (ROOT / 'src/com/dddumpling/game/WorldCatalog.java').read_text()
        future = source + '\nnew BossEntry(0,0,0,0,"Future","Costume","future_boss",'
        self.assertIn('future_boss', prepare['boss_names'](future))
        with self.assertRaises(ValueError):
            prepare['boss_names'](future + '\nnew BossEntry(0,0,0,0,"Other","Costume","future_boss",')


if __name__ == '__main__':
    unittest.main()
