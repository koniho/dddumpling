# Recorded cart rolling

`cart-roll.wav` replaces the synthesized `Sfx.CART_ROLL` sound in Cart Rush.
The squeal and tumble effects remain synthesized; they are not mislabeled as recordings.

## Source and permission

- Work: **minecart.wav**, by **wmquincy101**, published August 2, 2016.
- Source page: https://freesound.org/people/wmquincy101/sounds/351382/
- Downloaded public high-quality preview: https://cdn.freesound.org/previews/351/351382_2032854-hq.mp3
- License: **CC0 1.0 Universal**, https://creativecommons.org/publicdomain/zero/1.0/
- Retrieved September 19, 2026. The source page explicitly lists Creative Commons 0,
  permitting copying, modification, distribution and commercial use without permission.
- Courtesy credit: “Cart rolling adapted from minecart.wav by wmquincy101 (Freesound), CC0.”
  Attribution is not required by CC0.
- The author describes the 16.542-second mono recording as unprocessed, loopable and
  lo-fi, and says it could be a minecart. We describe it as a **recorded rail-style loop**;
  the actual object recorded is not documented.

`minecart-source.wav` is a losslessly decoded copy of the downloadable MP3 preview
(44,100 Hz mono, 16-bit PCM), not a claim to possess the original upload WAV.
The preview SHA-256 is `dd8afd5d4043f0f0aa3a50a6996908f9a532049fe48a5a173995eb5e4f55d49d`.

## Processing and playback

Run `python tools/prepare-cart-recording.py` to reproduce `cart-roll.wav` and
`CartRecording.java`. Python's standard library is sufficient.

Processing takes the source starting at 4.0 seconds, plays it at 78% speed for
lower pitch and weight, removes sub-35 Hz/DC energy, softens hiss above 1.1 kHz,
and emphasizes the recorded body below 230 Hz. A 12 ms entrance and 25 ms exit
prevent clicks, with peak normalized to 85%. No noise, oscillators, synthesized
rumbles or other recordings are mixed in.

The output is 1.20 seconds / 26,460 samples at 22,050 Hz mono, 16-bit PCM. Android
uses one static track per sound ID, so play at its native rate and wait at least
`CartRecording.DURATION` before retriggering. The short effect may finish after a
spill; it is not an unmanaged background loop. The root gameplay change sets this
interval without slowing visual vibration updates.

The generated Java data embeds the exact recorded samples to preserve identical
Android, iOS and headless playback without platform-specific asset loading. The
WAVs are review/reproduction artifacts and are not duplicated in the APK assets.

Validation: 181 audio assertions and 47 Android band-audio checks pass. The recording
passes the existing low-body and phone-band energy checks. Phone/headphone listening
is still the final subjective check; measurements do not establish that preference.

# Recorded cave rockfall and rumble

`cave-rumble.wav` and `cave-crash.wav` replace `Sfx.CAVE_RUMBLE` and
`Sfx.CAVE_CRASH`. They share a documented natural stone field recording, with
separate excerpts and processing for an approaching cascade and a landing impact.

- Work: **rockfall2a.wav**, by **AlanCat**, published April 24, 2017.
- Source: https://freesound.org/people/AlanCat/sounds/389303/
- Downloaded public high-quality preview: https://cdn.freesound.org/previews/389/389303_5486695-hq.mp3
- License: **CC0 1.0 Universal**, https://creativecommons.org/publicdomain/zero/1.0/
- Retrieved September 19, 2026. Source page explicitly permits modification,
  redistribution and commercial use without requesting permission.
- Courtesy credit: “Cave rockfall adapted from rockfall2a.wav by AlanCat (Freesound), CC0.”
- Author documents rocks falling down a cliff with smaller stones following,
  recorded with an Olympus LS-14, then background noise removed in Audacity.
- Original listed duration: 16.849 seconds, stereo, 44,100 Hz.
- Downloaded preview SHA-256: `db15885029d027756ef2b90114c202f4837f10a128371f493904b4886818ffcd`.

`rockfall-source.wav` is the decoded MP3 preview, not the original uploaded WAV.
Run `python tools/prepare-rock-recording.py` to rebuild the two review WAVs and
`RockRecording.java`. No synthesized noise, oscillators or added recordings are used.

The rumble takes the passage at 0.65 seconds, at 48% speed, rolls off above 500 Hz,
and emphasizes the existing body below 180 Hz. It lasts .95 seconds, with an
8 ms entrance and decaying last 40%. The crash uses the impact at 7.125 seconds,
at 65% speed, retains detail up to 1.6 kHz, emphasizes body below 180 Hz, and decays
over .46 seconds. Both remove sub-28 Hz/DC energy, fold stereo to mono, normalize
peaks to 85%, and run at 22,050 Hz. These are deliberately pitched rock recordings,
not unaltered claims about the size of the stones.

Playback uses the existing effects route. The .95-second rumble is a single cue
on rockfall introduction, ending before the first falling rock reaches the floor.
The .46-second impact finishes before the existing .571-second landing spacing;
no repeat interval change or background loop is needed. Squeal, tumble, enemy
stomps and quicksand remain the existing synthesized sounds.

# Octopulse arm-wave cue

Source: user-supplied “Copy of Sep 19 at 4-09 PM.m4a”, the newest audio file
in Downloads on 2026-09-19. The user explicitly requested this recording as
Octopulse's sound when starting to wave an arm. It is not a CC0 library asset.

The original is preserved as octo-wave-source.m4a. Run
`python3 tools/prepare-octo-wave.py` to regenerate octo-wave.wav and
OctoWaveRecording.java. FFmpeg applies a two-pole low-pass filter at 1 kHz
and downmixes to mono at 22050 Hz. The 0.68–2.66s
excerpt retains both recorded sounds, with 8ms/25ms edge fades and a peak of
85% full scale. Playback preserves the recording's pitch and speed.

The embedded PCM keeps Android, iOS, and the headless renderer identical.
The effect plays once at the beginning of the arm sweep; the later strike
keeps its separate short cue.

# Ninja airy swishes

Ninja rotates three quieter takes from **Cartoon Swishes**, sound #497037 by
**Phil Strahl / PixelProphecy**, under **CC BY 4.0**.

- Source: https://freesound.org/people/PixelProphecy/sounds/497037/
- License: https://creativecommons.org/licenses/by/4.0/
- Public HQ preview: https://cdn.freesound.org/previews/497/497037_9457666-hq.mp3
- Retrieved September 28, 2026; preview SHA-256:
  `fc09c9cbb7a56297784b663cab9db7da311197bab36c1f1ab3691ed7c0bac4b0`.
- Required attribution and modification notice: [AudioCredits.txt](../../assets/AudioCredits.txt),
  included in Android assets and iOS bundle resources.

`ninja-swishes-source.wav` is the first 2.2 seconds of the public MP3 preview,
folded to mono 48 kHz PCM, not the original lossless upload. The three excerpts
start at .035, 1.115 and 1.965 seconds. They were selected in the airy cartoon
swish audition; no impact or pop is mixed in.

Run `python3 tools/prepare-ninja-swishes.py` using Python's standard library to
regenerate the three WAVs and `NinjaSwishRecording.java`. Processing trims leading
silence, applies two 2.3 kHz low-pass stages and a 130 Hz high-pass, adds 6 ms / 25 ms
edge fades, and caps peaks at 17% before converting to 22,050 Hz. Each cue lasts
180 ms. Shared embedded PCM keeps Android, iOS and harness output identical.

Cuts within 100 ms share a sound; only played cues advance the three-take rotation.
There is no deferred audio queue or gameplay RNG use. Pause holds the cooldown;
completion, death, and restart clear it. `Sfx` boosts the audition PCM by 4×
(about 12 dB), preserving its filtering and fades with peaks below 70% full scale.

# Gummy Gator parry impact

Source: user-supplied `Thud.m4a` from Downloads on 2026-10-15, explicitly requested
for Gummy Gator's parry impact. This is not a CC0 library asset. Original SHA-256:
`005a0a0ab7899377e0c89a192adc3d28f2779e4358ec24fcbb861ae1798478f9`.
The original remains in Downloads and is not checked into the repository.

Run `python3 tools/prepare-gator-parry.py /path/to/Thud.m4a` to regenerate
`GatorParryRecording.java` and the local audition `build/gator-parry.wav`.
The 0.685–1.015s excerpt removes the silent lead and tail, with 2ms/15ms edge fades,
mono 22,050 Hz PCM and an 85% peak. Pitch and playback speed are unchanged.
The shared PCM plays once at visible companion/limb contact on Android and iOS;
interrupted flights drop the pending sound. Other projectile impacts retain their cues.

# Gummy Gator attack prompts and water splash

Three prompt takes come from user-supplied `prompt.m4a` in Downloads, explicitly
requested on 2026-10-15. This recording is not a CC0 library asset. Source SHA-256:
`775011c90f6d50c2350036952cdccc0be12656f104acd542ab7f667b60c88a16`.
The excerpts are 1.505–1.730s, 2.192–2.430s and 2.977–3.225s, preserving their
original pitch and speed. Each attack chooses randomly, without repeating a take
within a two/three-hit combo or demonstration. A separate seeded audio generator
preserves gameplay RNG. Pending cues are discarded when leaving or defeated.

Splash: **Big Water Splash** by **qubodup**, published October 12, 2018.
- Source: https://freesound.org/people/qubodup/sounds/442773/
- License: **CC0 1.0 Universal**, https://creativecommons.org/publicdomain/zero/1.0/
- Public HQ preview: https://cdn.freesound.org/previews/442/442773_71257-hq.mp3
- Retrieved October 15, 2026; SHA-256:
  `b8227adae409996ceee3481c7057ceda717575fa7569cec5d9d0ee1b94a8fe33`.
- Author describes a large splash mixed from four CC0 recordings; source links
  are retained on the source page. Commercial use and modification are permitted.

Run `python3 tools/prepare-gator-cues.py /path/to/prompt.m4a /path/to/442773_71257-hq.mp3`
to reproduce `GatorCueRecording.java` and the four audition WAVs in `build/`.
The originals remain outside source control. Both recordings pass through a
two-pole 4 kHz low-pass filter. PCM is mono 22,050 Hz, normalized per take to 85%, with 2ms leading fades and 12ms prompt / 45ms splash trailing
fades. The complete 2.172-second splash is retained. Android and iOS play the same
embedded PCM: normal speed at the defeat splash, 70% speed (lower pitch, 3.10s)
at the first entry droplets. Playback gain is 42% for prompts and 60% for both
splashes. No repeated water loop or added synthesis.

# Gummy Gator daze-cancel growls

User-supplied `Taunt1.m4a` and `Taunt2.m4a`, requested October 16, 2026. These
are user recordings, not CC0 library assets; originals remain in Downloads.
- Taunt1 SHA-256: `182ab421be485e7c71549749ae3af748cc7eb74620d2b119585aabe1cf4097a5`.
- Taunt2 SHA-256: `3ff1c0e7b4819f519d3f7c4db8bc8fd05b2593a754a4bdf19b444b62d313bbba`.

`python3 tools/prepare-gator-taunts.py /path/to/Taunt1.m4a /path/to/Taunt2.m4a`
regenerates `GatorTauntRecording.java` and six local audition WAVs in `build/`.
Cuts: Taunt1 0.845–1.105s, 1.150–1.440s, 1.480–2.145s;
Taunt2 1.010–1.230s, 1.305–1.525s, 1.635–1.860s. The last Taunt1 growl retains
its quieter articulation, rather than splitting breath valleys into extra cues.
Both use a two-pole 4 kHz low-pass, mono 22,050 Hz PCM, 3ms/15ms edge fades,
85% peak normalization and 55% playback gain. Pitch and speed are unchanged.

Taunt1 plays on the two-hit transition; Taunt2 on the three-hit transition.
The first growl starts 120ms after daze cancel. Remaining starts preserve the
original recording's spacing. The flex holds through the performance; the combo
demonstration begins 180ms after its last tail. A baked 10ms RMS envelope drives
the mouth opening and head bob/tilt, with a spring squash at each growl onset.
Pause freezes the shared clock. Leave, restart and defeat clear queued growls;
muted events are consumed rather than deferred. Audio does not use gameplay RNG.

# Super Slime squishes

Source: user-supplied `Squish .m4a` in Downloads on 2026-10-16, explicitly requested
for Super Slime's damage and attacks (#195). It is not a CC0 library asset. Original
SHA-256: `208290c9f76b0499698433c71774cac6e6411ea59cae76983a69b8b86cc48847`.
The source and review WAVs stay local; only generated shared PCM is included in the game.

Run `python3 tools/prepare-slime-squishes.py '/path/to/Squish .m4a'` to regenerate
`SlimeSquishRecording.java` and six audition WAVs in `build/slime-squishes/`.
Impact-aligned source cuts in recording order: 0.82–1.19, 1.48–1.84, 3.10–3.58,
4.43–5.05, 6.10–6.36 and 7.98–8.16 seconds. The heavy drag-damage takes end
before the second squeeze, so each damage event has only one beat. These remove preparation noise,
including roughly 0.8 seconds before the heavier squishes. Trim and reset timestamps
before processing. Raise playback pitch seven semitones (48,000 → 71,919 Hz rate
reinterpretation), resample to 48 kHz, add 3 ms/20 ms edge fades, and apply the
two-pole 4 kHz low-pass before resampling to 22,050 Hz PCM16. This also shortens
the clips to approximately 0.12–0.41 seconds (drag damage: 0.12–0.17 seconds). A common 2x gain (+6 dB) preserves
the recordings' relative intensity.

Layer a synthesized falling bubble at the attack: roughly 1,010–860 Hz down
toward 210 Hz, with a decaying 18 Hz pitch wobble, 45 ms amplitude decay,
2 ms onset, soft second harmonic and a final 15 ms fade. The stronger cartoon
“boip” shares the same onset as the recording; there is no echo or second attack.
Validate an audible onset within 30 ms, click-free edges and clipping headroom.

Light takes 1–2 rotate at projectile impact, medium takes 3–4 at a glob-splitting
impact, heavy takes 5–6 on drag damage, and takes 4–6 on attack-volley launch.
Selection uses a resettable cosmetic counter, not gameplay RNG. Android and iOS
use the same baked PCM at native playback rate and 90% effect gain. Existing cover/charge cues and
death audio remain unchanged; a separate splat recording is planned for death.

A separate synthesized, higher two-beat boip plays 0.4 seconds after the loose glob
makes Super Slime vulnerable to drag damage. Beats are 140 ms apart, with a higher
second note and soft falling pitch. Pause holds the delay; losing vulnerability,
defeat and leaving cancel it. Muted cues are consumed without replaying later.
The eyes, brows and open mouth pulse on the same shared 140 ms beat clock, with a
larger second reaction. The expression clock pauses and cancels with the cue.

Successful drag damage also plays one synthesized 920 ms boip at the damage event,
layered with the single-beat recorded squish. It falls from about 940 Hz toward
190 Hz with a soft pitch wobble, 300 ms amplitude decay and 90 ms ending fade,
at 60% effect gain. Holding or continuing an already-completed drag does not
retrigger it; attack volleys retain their existing shorter squishes.

# Rainbow Cat meows

User-performed recording **Oct 18 at 14_19.m4a**, recorded October 18, 2026,
provided explicitly for Rainbow Cat integration. Source SHA-256:
`59b1332edfeb1e6b499c08c6ac38d2c82d01e1210d29f5d4c1f566afa68bbd82`. The original stays in the user's Recordings folder;
it is not a third-party library asset or a CC0 claim.

[Reviewed cuts and measurements](rainbow-cat-cuts.json) retain 24 excerpts:
21 short meows, one longer meow for net struggling, and the annotated hurt and
surprised takes. A local Whisper tiny.en speech review identified a spoken aside
in the original 12.34–15.08 second cut, missed by the embedded recorder transcript.
That whole cut is listed as excluded in the manifest and is no longer packaged. Embedded transcript metadata identifies
spoken instructions around 19.38–22.08 and 25.29–27.21 seconds. Those annotations,
preparation silence, and the final handling noise are excluded. Boundaries use
waveform valleys and comparison of -32 dB and -23 dB silence candidates; continuous
meow phrases are split at articulation valleys rather than counting silence as sounds.

The found-sounds extraction skill exported 48 kHz mono review WAVs with
3 ms / 20 ms edge fades. `python3 tools/prepare-cat-meows.py /path/to/source.m4a`
verifies the original hash and regenerates `CatMeowRecording.java` and 22,050 Hz
mono review WAVs in `build/cat-meows/game`. No pitch change, normalization,
compression or low-pass filter: relative recording levels are preserved.
Every excerpt is checked for duration, audible energy, unclipped peaks and
silent endpoints. These measurements do not substitute for phone/headphone review.

The surprise sound follows accepted taps and net catches; hurt follows completed
key sequences. The retained longer meow plays when the net prompt
opens. Short takes cycle through launches and flurries without consuming gameplay
randomness. Routine calls wait until the current recording finishes; important
surprise/hurt reactions take priority. The entrance reuses the full longer meow as a battle cry, starting with the foreground rainbow burst.
Android and iOS use the same PCM at original speed and the effects-volume setting.


The final defeat hit uses **meow-long-low.m4a**, supplied by the user for this event.
[Defeat clip provenance and cut](rainbow-cat-defeat.json) records the source hash and
0.640–4.096 second interval. Preparation silence is trimmed; the complete vocal tail
is retained with 3 ms / 35 ms edge fades and original pitch. At the user’s request,
it is peak-normalized to 85% full scale (the standard effect level), with no compression or filtering.
`python3 tools/prepare-cat-defeat.py /path/to/meow-long-low.m4a` regenerates the
22,050 Hz mono PCM in `CatDefeatRecording.java` and an ignored review WAV.
The 3.456-second game clip receives +17.58 dB gain, measuring -1.41 dBFS peak and -12.52 dBFS RMS;
it has silent endpoints. Waveform and energy checks were used, not a listening claim.
The original recording and review WAVs stay local. This is user-provided material,
not a third-party sound library or a CC0 attribution.

Rainbow Cat's loss taunt reuses take 01 as seven meows descending by one semitone
each (+4 through -2), with short edge fades and a shared normalized peak.
Flurry releases reuse take 07 in a whole-tone bank (-12 through +10 semitones);
shorter volleys use the upper notes, and each shot plays its next note on release.
Both platforms use the same resampled PCM. The original recordings remain unchanged.
