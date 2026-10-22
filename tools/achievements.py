#!/usr/bin/env python3
"""Derive store achievement metadata from the shared runtime catalog."""
import json
from pathlib import Path
import re
import argparse
import csv
import io
import zipfile

ROOT = Path(__file__).resolve().parents[1]

def catalog():
    source = (ROOT / 'src/com/dddumpling/game/Achievements.java').read_text()
    result = []
    def add(key, title, description, target):
        result.append(dict(key=key, apple_id='com.dddumpling.game.'+key,
                           title=title, description=description, target=target, points=10))
    for n in map(int, re.search(r'COLLECTION = \{([^}]+)', source)[1].split(',')):
        add('collect_'+str(n), 'First Squishy' if n == 1 else str(n)+' Squishy Friends',
            'Collect '+str(n)+' distinct squishy'+('.' if n == 1 else ' friends.'), n)
    for n in map(int, re.search(r'STAGES = \{([^}]+)', source)[1].split(',')):
        add('stage_'+str(n), 'Stage '+str(n)+' Explorer', 'Reach stage '+str(n)+'.', n)
    add('steamer_first_win', 'Fresh from the Steamer', 'Win a steamer minigame.', 1)
    add('starpath_first_win', 'Star Path Success', 'Win a star path minigame.', 1)
    world = (ROOT / 'src/com/dddumpling/game/WorldCatalog.java').read_text()
    for entry in re.findall(r'new BossEntry\(([^\n]+)', world):
        strings = re.findall(r'"([^"\\]*(?:\\.[^"\\]*)*)"', entry)
        name, key = strings[0], strings[2]
        add('boss_'+key+'_first_win', name+' Victory', 'Defeat '+name+' for the first time.', 1)
    add('perfect_stages_5', 'Five Perfect Stages', 'Clear five stages perfectly across your Adventure runs.', 5)
    add('gauntlet_complete', 'Boss Gauntlet Champion', 'Complete an All Bosses Time Attack gauntlet.', 1)
    for n in map(int, re.search(r'SURVIVAL_MINUTES = \{([^}]+)', source)[1].split(',')):
        add('survival_minutes_'+str(n), str(n)+' Minute'+('' if n==1 else 's')+' of Survival',
            'Survive '+str(n)+' minute'+('' if n==1 else 's')+' in a single Survival run.', n*60)
    for n in map(int, re.search(r'LANDS = \{([^}]+)', source)[1].split(',')):
        add('run_lands_'+str(n), str(n)+' Lands in One Run',
            'Defeat the bosses of '+str(n)+' lands in a single Adventure run.', n)
    return result

def validate(path):
    value = json.loads(Path(path).read_text())
    expected = {a['key'] for a in catalog()}
    if set(value) != expected:
        raise ValueError('Achievement catalog mismatch; regenerate and provision new entries')
    ids = [value[key]['google_id'] for key in expected]
    if any(not re.fullmatch(r'[A-Za-z0-9_-]{8,200}', item) for item in ids) or len(set(ids)) != len(ids):
        raise ValueError('Each achievement needs a distinct real Google Console ID')
    for achievement in catalog():
        entry = value[achievement['key']]
        if entry['apple_id'] != achievement['apple_id'] or any(
                not re.fullmatch(r'[0-9a-f-]{36}', entry.get(field,''))
                for field in ('apple_resource_id','apple_dev_resource_id')):
            raise ValueError('Each achievement needs its provisioned Apple identifier')
        if not re.fullmatch(r'[0-9a-f]{64}', entry.get('icon_sha256','')):
            raise ValueError('Each achievement needs its uploaded icon checksum')
    return value

def export_play(destination, icons, mappings):
    """Export only unprovisioned achievements, so imports cannot duplicate old entries."""
    rows = [a for a in catalog() if not mappings.get(a['key'], {}).get('google_id')]
    if not rows:
        raise ValueError('No missing Google achievements to import')
    with zipfile.ZipFile(destination, 'w', zipfile.ZIP_DEFLATED) as archive:
        def write(name, values):
            buffer = io.StringIO()
            csv.writer(buffer).writerows(values)
            archive.writestr(name, buffer.getvalue())
        order = {a['key']: i+1 for i,a in enumerate(catalog())}
        write('AchievementsMetadata.csv', [[a['title'], a['description'], str(a['target'] > 1),
            a['target'] if a['target'] > 1 else '', 'Revealed', a['points'], order[a['key']]] for a in rows])
        write('AchievementsIconsMappings.csv', [[a['title'], a['key']+'.png'] for a in rows])
        for a in rows:
            archive.write(Path(icons)/(a['key']+'.png'), a['key']+'.png')

if __name__ == '__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--validate', action='store_true')
    parser.add_argument('--export-play', type=Path)
    parser.add_argument('--icons', type=Path, default=ROOT/'build/achievement-icons')
    args=parser.parse_args()
    mapping_path=ROOT/'store/achievements.json'
    if args.validate:
        validate(mapping_path)
        print('Achievement platform mappings complete:', len(catalog()))
    elif args.export_play:
        mappings=json.loads(mapping_path.read_text()) if mapping_path.exists() else {}
        export_play(args.export_play,args.icons,mappings)
    else:
        print(json.dumps(catalog(), indent=2))
