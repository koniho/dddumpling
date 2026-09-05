---
name: hummed-bgm
description: Analyze a hummed or sung monophonic recording, transcribe its pulse and melodic contour, and arrange it as seamless game background music. Use when a voice memo or rough vocal guide should become procedural or rendered BGM; do not use for speech cleanup or general audio mastering.
---

# Hummed BGM

Turn the recording into a recognizable, loop-safe arrangement while preserving the user’s musical choices.

## Analyze

1. Locate the exact recording and preserve it unchanged. If several candidates exist, compare timestamps and durations before choosing.
2. Convert compressed input to mono 16-bit PCM WAV for analysis. Prefer `ffmpeg -i input -ac 1 -ar 16000 output.wav`; unset a conflicting `LD_LIBRARY_PATH` if the local FFmpeg build requires it.
3. Run `scripts/analyze_melody.py output.wav`. Treat its BPM as a candidate, not unquestionable truth: compare the fast pulse, half-time value, note spacing, phrase length, and the user’s desired energy.
4. Inspect the opening for a percussive lead-in such as taps, claps, clicks, beatboxing, or repeated unpitched syllables. When it is regular, use its onset spacing to establish BPM and beat phase, including where the first melodic downbeat or pickup lands. Do not transcribe pitch estimates from the lead-in as melody notes; short percussive sounds can produce convincing but false high-note readings. Keep the lead-in in the finished music only when the user intended it as an audible count-in or intro.
5. Collapse vocal pitch flutter and brief octave errors into stable musical notes. Preserve the contour, repeated-note rhythm, rests, pickups, and turnaround; exact vocal microtonality is normally not part of the transcription.

## Arrange

- Quantize only enough to produce a coherent groove. Keep the analyzed phrase recognizable before adding harmony or countermelody.
- Choose harmony after the melody. Favor chords containing important sustained notes, and use tension deliberately near the turnaround.
- Match percussion to the request exactly. Distinguish beats per bar from subdivisions; for example, `kick-snare-kick-snare` is two kicks and two snares in a 4/4 bar.
- Make phrase endings audible with restrained fills, cymbal/noise accents, bass movement, or harmonic tension. Do not obscure the source melody.
- Build repetition with orchestration changes, octave changes, and dynamics instead of replacing the theme on every pass.
- For game loops, wrap note tails safely across the boundary or fade/crossfade where appropriate. Keep mix headroom for gameplay effects.

### Choose and apply the quantization grid

Quantize after tempo and phrase boundaries are established and after vocal pitch flutter and octave errors are collapsed into stable notes. Do it before choosing harmony, arranging accompaniment, or encoding the melody. Do not quantize raw analysis frames.

Choose the coarsest grid that preserves the shortest clearly intentional note or rest:

- Use eighth notes (half-beat steps in 4/4) as the default for driving game music and ordinary hummed melodies.
- Use quarter notes when the melody is deliberately broad and sparse.
- Use sixteenth notes only when repeated listening or visible onsets show intentional subdivisions that eighth notes would erase.
- Use triplet or swung grids when the performance consistently places notes there; do not force those performances onto straight eighths.

Snap both onsets and durations, including rests. For an eighth-note grid, every start and duration must be a multiple of `0.5` beats and an audible note should normally be at least `0.5` beats. Resolve collisions deliberately rather than allowing quantized notes to overlap accidentally. Finally compare the quantized phrase against the recording and restore any pickup, held note, or turnaround whose identity was lost.

## Integrate and verify

Follow the project’s established audio architecture and output format. Prefer procedural synthesis when the project already synthesizes music; otherwise render an owned audio asset in the project’s supported format.

Verify the implementation by rendering or exercising the actual game path. Check duration, peak/headroom, loop-boundary discontinuity, silence, and build/tests. When practical, export a preview the user can audition. Report the detected BPM and its plausible half-time interpretation.

The analyzer accepts mono 16-bit PCM WAV. It prints the driving BPM candidate, half-time BPM, stable MIDI/note segments, durations, RMS, and pitch-class totals.
