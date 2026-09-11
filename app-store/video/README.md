# Gameplay trailer

35.5 seconds, 30 fps. Two H.264/AAC MP4 exports:
- dddumpling-gameplay-landscape.mp4: 1920×1080 for a landscape preview.
- dddumpling-gameplay-portrait.mp4: 720×1280 for vertical viewing.

This is scripted capture through GameCore and Renderer using the software Painter.
It is not a phone screen recording. The same game rules, drawing code, and input
handlers run in the app; the harness uses its own bitmap font for game text.
Encounters are staged independently, and the display case is pre-populated to
show the collection. No gameplay outcomes or falling speed are changed for the
recording. The hand is a deterministic bot, not a recording of a human player.

The soundtrack uses Music.SWING_STYLE and actual Sound callback events synthesized
by Sfx. It includes no user-supplied audio or external licensed music. The speech
engine is not invoked during capture. Final audio is normalized for the trailer.

## Rebuild

Run from the repository root:

    bash tools/render-trailer.sh

After changing game source, run ./check.sh -q -r first to refresh compiled classes.
For caption/layout edits using the existing raw capture:

    bash tools/render-trailer.sh --edit-only

Sources:
- tools/Trailer.java: scene setup, capture timings, inputs, audio event mix.
- tools/TrailerPlate.java: landscape artwork from native game drawing code.
- chapters.json: captions and edit timeline; keep synchronized with Trailer.LENGTHS.
- tools/trailer-edit.py: FFmpeg filter generation.
- tools/render-trailer.sh: capture and encoding.

Intermediate footage, audio, and representative scene images stay under
build/trailer/. Review action near cuts, legibility at phone size, body cropping,
and audio before posting. A sample screenshot does not verify motion; play the
MP4 or inspect several consecutive frames for key mechanics.

This directory is not part of automatic website publishing. Publishing or uploading
the video is a separate action from rendering it.
