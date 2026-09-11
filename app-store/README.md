# App-store posting assets

Reusable listing copy and upload assets for DDDUMPLING. The English descriptions
are the wording prepared for the first Google Play listing, preserved verbatim.

## Google Play

| Console field | Source |
| --- | --- |
| App name | [title.txt](google-play/en-US/title.txt) |
| Short description | [short-description.txt](google-play/en-US/short-description.txt) |
| Full description | [full-description.txt](google-play/en-US/full-description.txt) |
| Package, contact emails, and public URLs | [listing.json](google-play/listing.json) |
| App icon | [icon.png](google-play/icon.png) |
| Feature graphic | [feature-graphic.png](google-play/feature-graphic.png) |
| Phone screenshots | [screenshots/](google-play/screenshots/) |

Copy text directly from the plain-text files; they contain no Markdown headings
or formatting markers. This directory contains posting material, not an APK or
Android App Bundle. The feature graphic is a 1024×500 RGB PNG; the icon is a 512×512 RGBA PNG.

## Source and updates

- The icon is exported from the actual game drawing code by tools/AppIcon.java.
  It uses the slime-green dumpling variant selected for the listing.
- The feature graphic is exported with tools/StoreGraphic.java, using the game's
  title lettering and character renderer. After compiling the harness, run
  java -cp build/harness com.dddumpling.game.StoreGraphic to regenerate it.
- Screenshots are full frames from the shared game rendering harness, copied
  from the website assets. Their source frames are out/2-wave.png,
  out/76-boss-octopulse.png, out/54-stars-flight.png, and out/40k-case-bosses.png.
  Refresh the posting copies when the corresponding game visuals change.
- The website source remains in docs/site/ and the privacy policy source remains
  in docs/privacy.html. See [website publishing](website-publishing.md) and
  [privacy publishing](privacy-publishing.md) for live URLs and automation.
  Editing this asset directory does not publish changes to either website.
- Listing statements describe the initial offline release. See
  [claims and source references](claims.md) before changing the description.

## Localization

Keep English copy in google-play/en-US/. Add a sibling locale folder, such as
es-ES/, with title.txt, short-description.txt, and full-description.txt when a
reviewed translation is ready. Keep DDDUMPLING as the brand name. Translate
screenshots with visible text alongside the listing when appropriate.

Store translations do not translate the game's hardcoded text. Review in-game
translations and layout separately before advertising language support.

## Video and reusable skills

See [video/README.md](video/README.md) for the gameplay trailer, scene sources,
and export commands. Repository skills are under skills/dddumpling-store-assets/
and skills/dddumpling-gameplay-trailer/.
