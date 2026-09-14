---
name: dddumpling-store-assets
description: Prepare DDDUMPLING app-store icons, feature graphics, screenshots, listing copy, and release notes using the existing game artwork and export tools. Use for store posting assets or preparing a game release, not unrelated gameplay changes.
---

# DDDUMPLING store assets

Work in the DDDUMPLING repository. The source copy of this skill is under skills/.
If invoked elsewhere, locate the checkout before running its tools.

## Artwork and sources

Use the actual Java game drawing code. This game's owner rejected a polished AI
dumpling illustration because it did not match the game. Preserve the current
flat character shapes, faces, pastel colors, and dark-purple backdrop unless the
user asks to change them. The chosen launcher/store icon is a slime-green dumpling.
Do not substitute generated raster artwork just because an image tool is available.

Read app-store/README.md and app-store/google-play/listing.json for posting files.
- tools/AppIcon.java exports the icon and launcher resources through Kawaii.draw.
- tools/StoreGraphic.java exports the feature graphic through TitleBubbleFont,
  Glyph, and Kawaii.
- tools/Preview.java renders screenshots through the same Renderer used on Android.
- app-store/claims.md maps listing statements to their implementation.
- docs/site/ and docs/privacy.html remain the website/policy sources.

## Export

After a source change or if the harness is missing, run ./check.sh -q -r to refresh
build/harness. Do not run check.sh concurrently with a renderer: it deletes that
directory. Then, for the assets actually requested:

    java -cp build/harness com.dddumpling.game.AppIcon
    magick app-store/google-play/icon.png -alpha on -strip PNG32:app-store/google-play/icon.png
    java -cp build/harness com.dddumpling.game.StoreGraphic

AppIcon also rewrites launcher PNGs. When a user requests only a store variant,
make a sibling exporter/output instead of accidentally changing the app icon.
Use the original game renderers to author shapes; ImageMagick is for export format,
resizing, and contact sheets.

Run selected Preview frames through check.sh when refreshing screenshots. Use
existing named frames only after checking that they reflect the current build.
Copy reviewed frames into app-store/google-play/screenshots/, preserving full
gameplay rather than presenting composite marketing art as a screenshot.

## Review and delivery

Inspect every new graphic at full size and at phone size. Check title spelling,
character identity, contrast, crops, and safe margins. Verify file dimensions,
channel type, and size with magick identify. The current exports are 512×512 RGBA
PNG for the icon and 1024×500 RGB PNG without alpha for the feature graphic.
Verify current store requirements from official documentation before asserting
compliance for a new store or changed specification.

Keep English listing text in app-store/google-play/en-US/. Preserve approved copy
unless the task includes editing it; put translations in sibling locale folders.
Do not advertise unmerged account, telemetry, or cloud-save features.

A generated file is not an uploaded Play Console asset. State exactly what was
saved or published. For authorized website publishing, copy selected public images
into docs/site/assets/ and push main; .github/workflows/publish-pages.yml publishes
that directory. Wait for success and HTTP 200 before sharing a live download link.
Google Drive is optional and not part of this workflow by default. Keep signing
files, private credentials, rejected drafts, and game source out of public assets.

## Preparing a release

When asked to prepare or create a release, include the version bump and player-facing
release notes. Follow docs/releasing.md and finish the notes before creating or pushing a tag.
Use docs/release-notes.md for the human-editable in-game catalog; every feature needs its game
phase and player purpose, with fixes grouped under one bug icon. Include the JSON and generated
copy in the release commit and run tools/release-notes.py check --version <version> before tagging. Read app-store/play-publishing.md for the current target and account setup.

1. Run `python3 tools/prepare-release.py context --output build/release-context.json`.
   Confirm the base is the last distributed version. For a manually uploaded release,
   pass `--since <commit-or-tag>`: the nearest Git tag may be older than the actual release.
   The initial Play version code 10 was built from `3451055` despite having no release tag.
2. Read the context, relevant diffs, and linked merged PR descriptions. Summarize observable
   gameplay changes and fixes into `build/release-notes.txt`. Omit publishing infrastructure,
   developer-only changes, and unmerged features. Do not copy commit subjects blindly or
   invent player benefits when the range contains only tooling changes. Report that case
   and keep release preparation within the user's intended scope.
3. Run `python3 tools/prepare-release.py prepare --notes build/release-notes.txt` to preview.
   Defaults increment the patch version and version code. Override `--version-name` and
   `--version-code` for the requested release; confirm the code has not already been uploaded
   to Play, including unpublished uploads. Never retry an upload by reusing a consumed code.
4. Once the notes accurately describe the release, repeat with `--write`. This updates
   AndroidManifest.xml and writes `app-store/google-play/en-US/changelogs/<code>.txt`.
   Review their diff together and show the notes with the version in the release summary.
   Honor existing authorization for committing, tagging and publishing; preparation itself
   does none of these. When authorized, include both files in the release commit before tagging.

Fastlane uploads the versioned notes automatically and requires 1–500 characters.
GitHub's generated release notes can stay more technical and detailed. Keep changelog files
tracked as the authoritative record of what was sent to Play; do not generate new notes
inside the upload job after the release has already been tagged.
