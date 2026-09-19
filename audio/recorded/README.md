# Octopulse arm-wave cue

Source: user-supplied “Copy of Sep 19 at 4-09 PM.m4a”, the newest audio file
in Downloads on 2026-09-19. The user explicitly requested this recording as
Octopulse's sound when starting to wave an arm. It is not a CC0 library asset.

The original is preserved as octo-wave-source.m4a. Run
`python3 tools/prepare-octo-wave.py` to regenerate octo-wave.wav and
OctoWaveRecording.java. FFmpeg downmixes to mono at 22050 Hz. The 0.68–2.66s
excerpt retains both recorded sounds, with 8ms/25ms edge fades and a peak of
85% full scale. Playback preserves the recording's pitch and speed.

The embedded PCM keeps Android, iOS, and the headless renderer identical.
The effect plays once at the beginning of the arm sweep; the later strike
keeps its separate short cue.
