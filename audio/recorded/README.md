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
OctoWaveRecording.java. FFmpeg downmixes to mono at 22050 Hz. The 0.68–2.66s
excerpt retains both recorded sounds, with 8ms/25ms edge fades and a peak of
85% full scale. Playback preserves the recording's pitch and speed.

The embedded PCM keeps Android, iOS, and the headless renderer identical.
The effect plays once at the beginning of the arm sweep; the later strike
keeps its separate short cue.
