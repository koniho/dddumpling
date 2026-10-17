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
relative attack spacing. The requested 20 BPM increase takes the arrangement
from approximately 206 to 226 BPM: every attack and the clip duration is
multiplied by 206/226, preserving the performed groove. The table below records
the original offsets before this uniform speed-up.

| Voice | Original attack offsets before tempo scaling (seconds) |
| --- | --- |
| Kick | 0, 0.605, 1.200, 1.765, 2.335, 2.940, 3.495, 3.770, 4.080 |
| Snap | 0.435, 0.875, 1.600, 2.035, 2.775, 3.215 |

This gives three thump/snap/thump/snap phrases, followed by three kick-only
beats. No filler kicks, cymbals or hi-hats are added. The synth notes share the
first twelve drum attack times and sound for 58% of each gap, leaving 42% clear.
Lower chords and higher harmonies release with the lead. Only the
final three kicks carry the continuous downward gliss, now from about 3.186 to
3.919 seconds. The ready cue and panel exit also move earlier so the boss music
starts as the gliss ends.

The lead uses an original procedural ARR1-style breathy ah voice: three vowel
formant peaks, filtered breath noise, subtle pitch motion and light sampler
grain, with a 20 ms attack. This is a synthesized approximation, with no imported
vocal sample. [NFSA reference for ARR1/SARARR](https://www.nfsa.gov.au/collection/item/sampling-sounds-fairlight-cmi).
A second airy lead follows the melody one octave higher, including the final
gliss from B5 to E4. Both voices share a 20% lower lead gain, with 0.8/0.6
equal-power weighting favoring the original register. High partials are limited
below Nyquist as the upper voice rises. Note spacing and kick levels stay fixed.
The backing voices retain the rounded sine-based synth, soft octave and quiet
upper partials. Chord voices stay
behind the lead, with two softer harmony voices an octave above the inner
chord tones (C/E above A, E/G above C, and E/G-sharp above B). They share the
beatbox attacks and glide down into G-sharp3/B3 at the ending. The kick has a
stronger 58 Hz body and a second harmonic to keep its punch audible on phone
speakers. Snare snaps follow the beatbox rhythm with a stronger pitched body, a
brief bright attack, higher gain and a slightly longer filtered tail for impact.

`VersusMusic.clip()` synthesizes the actual playback PCM. During the final
kick-only section the lead continuously slides down from B4
to E3, and the other voices land on E2/G-sharp2/B2. This E-major dominant leads
into the boss track's opening A-minor chord. A 45 ms edge fade prevents clicks.
Before a title-screen start, its music fades out over 350 ms of the companion
departure, then stays silent for the final 200 ms before the versus cue begins.
Canceling restores the title volume; the fade never changes saved preferences
or effect volume, and music mute remains authoritative.

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
