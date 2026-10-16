#!/usr/bin/env python3
"""Export reviewed cuts with trim-before-fade ordering and non-silent validation."""
import argparse
import array
import hashlib
import json
import math
import os
from pathlib import Path
import re
import subprocess
import sys
import wave
import zipfile


def inspect(path, duration):
    with wave.open(str(path)) as wav:
        if (wav.getnchannels(), wav.getsampwidth(), wav.getframerate()) != (1, 2, 48000):
            raise ValueError(f'Unexpected format: {path}')
        if abs(wav.getnframes() / 48000 - duration) > .003:
            raise ValueError(f'Unexpected duration: {path}')
        samples = array.array('h', wav.readframes(wav.getnframes()))
    if sys.byteorder != 'little':
        samples.byteswap()
    peak = max(map(abs, samples), default=0)
    rms = math.sqrt(sum(v*v for v in samples) / max(1, len(samples)))
    if peak < 32 or rms < 1:
        raise ValueError(f'Unexpectedly silent export; inspect cut/fade/filter settings: {path}')
    return {'peak_dbfs': round(20*math.log10(peak/32768), 2),
            'rms_dbfs': round(20*math.log10(rms/32768), 2)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source', type=Path)
    parser.add_argument('cuts', type=Path)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--lowpass', type=float, help='Cutoff in Hz; 4000 means 4 kHz')
    args = parser.parse_args()
    if args.lowpass is not None and not 0 < args.lowpass < 24000:
        parser.error('Low-pass cutoff must be between 0 and 24000 Hz')
    env = os.environ.copy()
    if env.get('PREFIX', '').startswith('/data/data/com.termux/'):
        env.pop('LD_LIBRARY_PATH', None)
    duration = float(subprocess.check_output(['ffprobe', '-v', 'error', '-show_entries',
        'format=duration', '-of', 'default=noprint_wrappers=1:nokey=1', str(args.source)], env=env))
    cuts = json.loads(args.cuts.read_text())
    names = set()
    if not isinstance(cuts, list) or not cuts:
        parser.error('Cuts must be a non-empty JSON list')
    for cut in cuts:
        name = cut['name']
        if not re.fullmatch(r'[a-zA-Z0-9_-]+', name) or name in names:
            parser.error('Each cut needs a unique filename-safe name')
        names.add(name)
        if not 0 <= cut['start'] < cut['end'] <= duration + .001:
            parser.error(f'Cut outside recording: {name}')
    args.out.mkdir(parents=True, exist_ok=False)
    original = args.out / 'original'; original.mkdir()
    selected = original
    if args.lowpass:
        selected = args.out / 'filtered'; selected.mkdir()
    manifest = {'source': args.source.name,
                'source_sha256': hashlib.sha256(args.source.read_bytes()).hexdigest(),
                'format': '48 kHz mono PCM16; original levels',
                'fades_ms': [3, 20], 'lowpass_hz': args.lowpass, 'lowpass_poles': 2 if args.lowpass else None,
                'clips': []}
    for cut in cuts:
        length = cut['end'] - cut['start']
        fade_in, fade_out = min(.003, length/4), min(.020, length/4)
        wav = original / (cut['name'] + '.wav')
        filters = (f'atrim=start={cut["start"]}:end={cut["end"]},asetpts=PTS-STARTPTS,'
                   f'afade=t=in:d={fade_in},afade=t=out:st={length-fade_out}:d={fade_out}')
        subprocess.run(['ffmpeg', '-v', 'error', '-n', '-i', str(args.source), '-map', '0:a:0',
            '-af', filters, '-ac', '1', '-ar', '48000', '-c:a', 'pcm_s16le', str(wav)], env=env, check=True)
        entry = dict(cut, duration=round(length, 6), original=inspect(wav, length))
        if args.lowpass:
            target = selected / wav.name
            subprocess.run(['ffmpeg', '-v', 'error', '-n', '-i', str(wav), '-af',
                f'lowpass=f={args.lowpass}:p=2', '-c:a', 'pcm_s16le', str(target)], env=env, check=True)
            entry['filtered'] = inspect(target, length)
            with wave.open(str(wav)) as before, wave.open(str(target)) as after:
                if before.readframes(before.getnframes()) == after.readframes(after.getnframes()):
                    raise ValueError(f'Filter did not change audio: {wav.name}')
        manifest['clips'].append(entry)
    record = args.out / 'clips.json'
    record.write_text(json.dumps(manifest, indent=2) + '\n')
    with zipfile.ZipFile(args.out / 'clips.zip', 'w', zipfile.ZIP_DEFLATED) as pack:
        for wav in sorted(selected.glob('*.wav')):
            pack.write(wav, wav.name)
        pack.write(record, record.name)
    print(f'Exported and verified {len(cuts)} clips: {args.out / "clips.zip"}')


if __name__ == '__main__':
    main()
