#!/usr/bin/env python3
"""Validate the publication inventory against runtime IDs; never writes to a store."""
import argparse
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def check(manifest):
    source = (ROOT / 'src/com/dddumpling/game/LeaderboardCatalog.java').read_text()
    arrays = {}
    for name in ('APPLE', 'GOOGLE'):
        body = re.search(r'\b' + name + r'=\{(.*?)\};', source, re.S).group(1)
        arrays[name] = re.findall(r'"([^"\n]+)"', body)
    boards = manifest['boards']
    assert len(boards) == 11, 'Expected exactly eleven ranked boards'
    assert manifest['development_configured'] is False, 'Dev requires separately provisioned IDs and runtime routing'
    assert manifest['apple_app_id'] == '6811478003'
    assert manifest['google_project_id'] == '56613446939'
    for index, board in enumerate(boards):
        assert board['index'] == index, 'Board order must match runtime catalog'
        assert board['apple_id'] == 'com.dddumpling.game.' + arrays['APPLE'][index], 'Apple ID drift'
        assert board['google_id'] == arrays['GOOGLE'][index], 'Google ID drift'
        assert board['order'] == ('ascending' if 3 <= index <= 9 else 'descending'), 'Rank direction drift'
        assert board['apple_unit'] == ('seconds' if index < 3 else 'centiseconds' if index < 10 else 'points'), 'Apple unit drift'
        assert board['google_unit'] == ('milliseconds' if index < 10 else 'points'), 'Google unit drift'
    for platform in ('apple_id', 'google_id'):
        assert len({board[platform] for board in boards}) == 11, 'Duplicate platform ID'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['check', 'plan'])
    args = parser.parse_args()
    manifest = json.loads((ROOT / 'store/leaderboards.json').read_text())
    check(manifest)
    print('11 leaderboard IDs, rank directions and platform units match runtime code.')
    if args.action == 'plan':
        print('Provisioning snapshot only: 2026-11-06; live publication has NOT been verified.')
        for board in manifest['boards']:
            print(f"{board['index']:2} {board['name']}: Apple {board['apple_id']} / Google {board['google_id']}")
        print('Required: verify live configurations, isolated Dev service QA, then approved release workflow.')
        print('Apple: submit all eleven leaderboard versions for Game Center review with the approved app version.')
        print('Google: publish Play Games Services configuration in Play Console; draft APIs cannot publish it.')
        print('See store/leaderboards.md. This command performs no remote writes or score submissions.')


if __name__ == '__main__':
    main()
