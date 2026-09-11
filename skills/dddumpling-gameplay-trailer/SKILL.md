---
name: dddumpling-gameplay-trailer
description: Capture and edit DDDUMPLING gameplay videos with its real Java renderer, synthesized game audio, and reusable FFmpeg pipeline. Use for gameplay trailers or refreshing existing video exports.
---

# DDDUMPLING gameplay trailer

Use the DDDUMPLING checkout and read app-store/video/README.md. Keep this distinction
clear: the existing pipeline records scripted input through GameCore and Renderer.
It is not a phone recording. The software Painter uses its own bitmap font; Android
uses its device font renderer. Do not claim device capture when delivering it.

## Existing tools

- tools/Trailer.java stages encounters, drives real inputs, renders RGB frames into
  FFmpeg, and mixes actual Sound callbacks with Music.SWING_STYLE.
- tools/TrailerPlate.java draws the landscape backing using the game's own art.
- app-store/video/chapters.json supplies timed captions; timings must match
  Trailer.LENGTHS, including the final end card.
- tools/trailer-edit.py generates filters and text files, avoiding shell escaping
  problems from caption contents.
- tools/render-trailer.sh produces portrait and landscape H.264/AAC MP4s.

The current cut is 35.5 seconds at 30 fps. Scenes cover tapping, a powerup, Octopulse,
Dark Divide, Fly Agaric, Starpath, the collection, and an end card. Treat these as
an editable cut, not a mandatory storyboard for future requests.

## Capture and edit

Refresh the harness with ./check.sh -q -r after changing game source. Do not run
check.sh concurrently with capture: it deletes build/harness. Then run:

    bash tools/render-trailer.sh

For caption or composition changes using the existing silent video and soundtrack:

    bash tools/render-trailer.sh --edit-only

Intermediates go to build/trailer/; final videos and maintained edit sources go to
app-store/video/. The script prints codec, dimension, frame-rate and duration data.

If FFmpeg cannot load a C++ symbol on Termux, check LD_LIBRARY_PATH before upgrading
packages. Codex's launcher can prepend a directory with an incompatible libc++.
The wrapper tests FFmpeg first, then scopes the Termux system library path to its
own process if needed. Do not change global shell or Codex configuration for this.

## Authenticity and editing choices

Stage encounters using existing playtest/setup methods and real inputs. Do not
alter runtime difficulty to manufacture impressive outcomes. Mark a filled display
case and bot-driven footage as staged in the production notes. Use additional phone
capture if the user specifically wants device footage and a paired device exists.

A fast bot can clear every character near the ceiling, leaving footage visually
empty. Adjust its attention/cadence or the chosen recording window, not spawn rules.
Show the action that a caption promises. Check the boss's body near large shake
angles and examine pinch/drag sequences across multiple frames.

Use the existing synthesized Music/Sfx unless the user supplies another track
with authorization. Never silently include res/raw/bgm or a user's reference
recording. Audio normalization and fades are in the encoding wrapper.

## Review and handoff

Review representative frames from every scene and consecutive frames around
gestures and cuts. Listen to the soundtrack when audio playback is available;
otherwise disclose that limit and inspect levels, clipping and stream presence.
A static contact sheet alone does not verify animation. Use FFmpeg to extract
frames and ffprobe to verify duration, dimensions, fps, H.264/AAC, and audio/video
alignment. Check captions and the gameplay HUD at phone size. A decodable MP4
does not establish that gameplay is readable.

Keep rebuild commands and footage provenance with the artifacts. Report local
video links and what was verified. Rendering does not authorize uploading to
YouTube, Google Drive, the public website, or Play Console. The current public
page publisher only accepts static website asset types; do not drop MP4s into
docs/site/ without intentionally updating that workflow for an authorized publish.
