# Versus synth fanfare

Original melody supplied by the developer in Downloads as `Oct 16 at 20-39.m4a`
(2026-10-16, 8.30 seconds). The source remains unchanged and local; neither the
recording nor rendered audio is committed.

Source SHA-256: `18d5a91e9552c626eba98310ad637d4378c9d503fb4022e18b981aad9a64ef58`.

Decoded to mono 16 kHz PCM and analyzed with the hummed-bgm skill. The raw
candidate was 218 BPM / 109 half time (filtered: 210 / 105); the percussive
lead-in and handling noise are not pitched melody. The stable phrase starts
around 2.17 seconds, repeats F-sharp and G with dips to E, and ends in a long
fall. Its B/A/C contour is transposed into the boss track's A-minor key and
rephrased into three four-note calls following the beatbox attacks. Moving
A-minor, C-major and E-major harmonies support the soft synth lead.

The drum guide is the developer's newer `Oct 16 at 21-10.m4a` (6.272 seconds),
SHA-256 `512f67cd738ecc34d63ce777ecb3d0bd86fe088867932eae911d90f4488329b1`.
It remains unchanged and local. Decoded to mono 16 kHz PCM, its onset analyzer
suggests 210 BPM / 105 half time; measured quarter-note spacing is roughly
0.29 seconds (about 206 BPM / 103 half time). The recording is the timing source:
remove the lead-in before the first thump near 1.225 seconds and preserve the
relative attack times, rather than forcing it to the previous 224 BPM grid.

| Voice | Attack times in the arranged clip (seconds) |
| --- | --- |
| Kick | 0, 0.605, 1.200, 1.765, 2.335, 2.940, 3.495, 3.770, 4.080 |
| Snap | 0.435, 0.875, 1.600, 2.035, 2.775, 3.215 |

This gives three thump/snap/thump/snap phrases, followed by three kick-only
beats. No filler kicks, cymbals or hi-hats are added. The synth notes share the
first twelve drum attack times and sustain almost to the next attack. Only the
final three kicks carry the continuous downward gliss, from 3.495 to 4.3 seconds.

The trumpet has been replaced by a rounded sine-based synth with a soft octave,
very quiet upper partials, slight doubling, and 18 ms attacks. Chord voices stay
behind the lead. The kick has a stronger 58 Hz body and a second harmonic to
keep its punch audible on phone speakers; short filtered snare snaps follow
the beatbox rhythm.

`VersusMusic.clip()` synthesizes the actual playback PCM. During the final
kick-only section the lead continuously slides down from B4
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
