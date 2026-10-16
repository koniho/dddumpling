---
name: found-sounds-extraction
description: Extract individual sound effects from a user recording, preserving intensity variations and optionally filtering them for game audio. Use for recorded squishes, thuds, growls, splashes, and similar effects rather than composing background music.
---

# Found sounds extraction

Prepare separate clips from a recording without changing the source. Distinguish extraction from game integration: connect clips to gameplay only when requested, and preserve any separate recording the user plans for another event.

## Find and segment

- Locate the named source in the user's indicated folder. On Termux, Downloads is usually `/storage/emulated/0/Download` or `~/storage/downloads`. Use quoted paths; names can contain trailing spaces before extensions.
- Inspect duration, sample rate and channels with `ffprobe`. Decode a working WAV for inspection. Listen to the recording and inspect its waveform when possible.
- Use `silencedetect` as a candidate boundary finder, not as an automatic clip count. Quiet preparation, wet tails and multiple transients can belong to one squish. Compare thresholds and merge internal pauses before choosing ranges.
- Preserve recording order when the sounds progress in intensity. Leave small lead/tail margins so attacks and decays survive; trim handling noise separately. Record exact source start/end times.

On Termux, FFmpeg may fail with a `libplacebo.so`/C++ symbol error because the session supplies a conflicting `LD_LIBRARY_PATH`. Retry only that command with `env -u LD_LIBRARY_PATH ffmpeg ...` (and `ffprobe`). This does not require changing system libraries. The helper below uses that workaround on Termux.

## Export and filter

Use [scripts/extract.py](scripts/extract.py) with a reviewed JSON list of segments:

```json
[
  {"name": "squish-01", "start": 0.69, "end": 1.19},
  {"name": "squish-02", "start": 1.38, "end": 1.84}
]
```

```sh
python3 scripts/extract.py '/path/to/recording.m4a' cuts.json --out /path/to/output --lowpass 4000
```

The times above only illustrate the format; derive cuts from the current recording. The helper exports mono 48 kHz PCM16 WAVs into `original/`, with optional filtered copies in `filtered/`, a source/cut manifest and a ZIP of the requested output. Use a fresh output directory.

- Trim **before** resetting timestamps and applying fades: `atrim=start=...:end=...,asetpts=PTS-STARTPTS,afade=...`. Applying clip-relative fades before trimming can silently export silence, particularly when output-side `-ss` is used.
- Preserve relative levels by default. Do not independently normalize an intensity ladder. If more gain is wanted, use a shared gain with adequate headroom and record it.
- Apply requested filters to copies, retaining the unfiltered clips. A two-pole low-pass is a reasonable gentle default; record cutoff and slope. Clarify ambiguous units: 4 Hz is not 4 kHz, and removes virtually all audible content.
- Avoid denoising, compression, time stretching or pitch changes unless requested or necessary and explained.

## Validate and deliver

Check format, duration, peak/RMS and audible energy **for every clip**. A successful FFmpeg exit is insufficient. Compare each clip with its source interval, check the opening transient and tail, and verify filtered samples differ while timing and levels remain appropriate. Listen to representative quiet and strong clips when playback is available. Do not claim listening validation based only on a waveform.

Keep the source filename/hash, cut times and processing in the manifest. Preserve known attribution/license information for imported sounds; do not assume that a file in Downloads is freely redistributable. Put working recordings and previews in an ignored artifact directory, not a code commit. Add game assets only through the repository's normal asset/provenance workflow when integration is requested.

Provide the clips/ZIP and state whether they are merely prepared or integrated. Honor the user's existing notification preference when complete.
