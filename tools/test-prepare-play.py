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
        source = (ROOT / 'src/com/dddumpling/game/Progress.java').read_text()
        declaration = re.search(r'BOSSES\s*=\s*\{([^}]+)\}', source).group(1)
        runtime = set(re.findall(r'"([^"]+)"', declaration))
        configured = {key[len('boss_'):-len('_started')]
                      for key in prepare['catalog']()
                      if key.startswith('boss_') and key.endswith('_started')}
        self.assertEqual(runtime, configured)

    def test_example_matches_build_catalog(self):
        example = json.loads((ROOT / 'store/play-games.example.json').read_text())
        self.assertEqual(set(example['events']), set(prepare['catalog']()))

    def test_draft_ids_pass_build_validation(self):
        draft = prepare['config'](ROOT / 'store/play-games.draft.json')
        self.assertEqual(draft['project_id'], '56613446939')


if __name__ == '__main__':
    unittest.main()
