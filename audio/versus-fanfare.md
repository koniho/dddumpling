# Versus trumpet fanfare

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
same 4.3-second versus screen. The trumpet lead is an octave higher, with crisp
attacks, octave doubling and short E-minor brass-section stabs.

A heavy kick lands on every quarter note throughout four bars. Separate goblet-drum
voices play Saidi `D T - D D - T -` on eighth notes, with the distinctive middle
pair of dum strokes. There are no hi-hats or cymbals. Rhythm reference:
[MaqamWorld: Saidi](https://www.maqamworld.com/en/iqaa/saidi.php) and
[Saidi notation](https://rythm.organology.net/rhythms/saidi).

`VersusMusic.clip()` synthesizes the actual playback PCM. The final note begins
on beat 9 and continuously slides down two octaves through the end, with a
45 ms edge fade. This is a one-shot cue, not a loop. Both native backends use
the music slider, narration ducking, and the existing lifecycle controls; boss
music resumes after the versus screen. Rendering is deterministic and cached.

Preview only this clip after compiling the harness:

```sh
FRAMES=versus-music java -cp build/harness com.dddumpling.game.Preview 480 1040 1 out
```

Output: `out/versus-trumpet-fanfare.wav`.
