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

Validation: 179 audio assertions and 47 Android band-audio checks pass. The recording
passes the existing low-body and phone-band energy checks. Phone/headphone listening
is still the final subjective check; measurements do not establish that preference.
