# Versus synth fanfare

Original melody supplied by the developer in Downloads as `Oct 16 at 20-39.m4a`
(2026-10-16, 8.30 seconds). The source remains unchanged and local; neither the
recording nor rendered audio is committed.

Source SHA-256: `18d5a91e9552c626eba98310ad637d4378c9d503fb4022e18b981aad9a64ef58`.

Decoded to mono 16 kHz PCM and analyzed with the hummed-bgm skill. The raw
candidate was 218 BPM / 109 half time (filtered: 210 / 105); the percussive
lead-in and handling noise are not pitched melody. The stable phrase starts
around 2.17 seconds, repeats F-sharp and G with dips to E, and ends in a long
fall. The arrangement preserves that contour, straightened to eighth notes at
224 BPM (double the initial arrangement), with two quick statements filling the
same 4.3-second versus screen. The contour is transposed to B/A/C in the boss
track's A-minor key, with rounded attacks, octave doubling and moving A-minor,
C-major and E-major triads beneath each melodic note. Lower A-minor synth chords
supports the phrases before an E-major stab prepares the ending.

The drum guide is the developer's newer `Oct 16 at 21-10.m4a` (6.272 seconds),
SHA-256 `512f67cd738ecc34d63ce777ecb3d0bd86fe088867932eae911d90f4488329b1`.
It remains unchanged and local. Decoded to mono 16 kHz PCM, its onset analyzer
suggests 210 BPM / 105 half time. Low-frequency thumps occur near 1.23, 1.83,
2.43, 2.99, 3.56 and 4.17 seconds; brighter snaps near 1.66, 2.10, 2.83, 3.26,
4.00 and 4.44 seconds. Interpreted as thumps on beats 1/3 and snaps on 2-and/4,
this replaces the earlier generic Saidi accompaniment. The groove is quantized
to eighth notes at the existing 224 BPM. Softer kicks on 2/4 retain the requested
four-on-the-floor pulse while the stronger 1/3 accents follow the recording.
There are no hi-hats or cymbals.

The trumpet has been replaced by a rounded sine-based synth with a soft octave,
very quiet upper partials, slight doubling, and 18 ms attacks. Chord voices stay
behind the lead. The kick has a stronger 58 Hz body and a second harmonic to
keep its punch audible on phone speakers; short filtered snare snaps follow
the beatbox rhythm.
`VersusMusic.clip()` synthesizes the actual playback PCM. The final note begins
on beat 9 and continuously slides down through the end: the lead moves from B4
to E3, and the other voices land on E2/G-sharp2/B2. This E-major dominant leads
into the boss track's opening A-minor chord. A 45 ms edge fade prevents clicks.
This is a one-shot cue, not a loop. Both native backends use
the music slider, narration ducking, and the existing lifecycle controls; boss
music resumes after the versus screen. The master targets 4600 PCM RMS with a
19000 soft ceiling; narration retains 65% of the versus music volume (ordinary
music still uses 22%). The music slider and mute remain authoritative. Rendering
is deterministic and cached.

Preview only this clip after compiling the harness:

```sh
FRAMES=versus-music java -cp build/harness com.dddumpling.game.Preview 480 1040 1 out
```

Output: `out/versus-synth-fanfare.wav`.
