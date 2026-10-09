# Promotional artwork

Create composed illustrations for App Store Header and Search Results from DDDumpling's
characters, bosses, lands and mode motifs. These are creative assets in Apple's Asset
Library. Gameplay screenshots and app previews have their own requirements and upload flow.
Apple permits creative artwork to show the game's brand and excitement without reproducing
the gameplay interface. See [Apple's guidance](https://developer.apple.com/help/app-store-connect/manage-app-information/manage-your-app-store-assets).

## Current artwork

The latest version is [v3: lands and Survival](../store/creative-assets/dddumpling-universal-v3-lands-survival.png).
It includes Slime, Octopulse and Fly Agaric; green hills, purple crystals, sea kelp and
mushrooms; and Survival's rainbow curl carrying a cream companion at game over. The main
dumpling uses the game's flat shape and face, replacing the glossy icon-style treatment in v1/v2.

The folder keeps each original generated image, its upload export and its exact prompt in
`*.provenance.json`. [Library records](../store/creative-assets/library-assets.json) contain
the uploaded IDs and last observed status. On October 8, 2026, v1 and v2 were last observed
in `PREPARE_FOR_SUBMISSION`; a later read confirmed v3 in `WAITING_FOR_REVIEW`. These scripts
upload and read status only. Treat the recorded status as a snapshot, and query Apple for
current status.

## Gather references

Start with the current source art rather than interpreting the glossy launcher icon as the
in-game style. `Kawaii`, `Trinket`, `BossCollect`, `Lands` and `SurvivalEnd` own the relevant
geometry. Search `GLOSSARY.md` for player-facing names and production availability.

Render two portrait reference images directly from the game's pure-Java renderer:

```sh
python3 tools/promotional-references.py
```

This needs Python 3 and a JDK. It writes to ignored `out/promotional-references/`, uses
isolated temporary compiler output and runs no game suites or native app builds. On a Mac
with Homebrew OpenJDK installed but no registered system JDK, pass its home explicitly:

```sh
python3 tools/promotional-references.py \
  --java-home /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
```

`game-art-and-lands.png` shows the actual main dumpling and the four production land emblems.
`survival-rainbow-curl.png` captures one authored Survival ending. Existing portrait boss
frames under `out/` can supply Slime, Dark Divide, Octopulse and Fly Agaric identities.
Capture only the reference states needed. A gameplay capture can guide a character or motif
without becoming part of the finished banner's interface.

Keep selected reference images beside the artwork in `creative-assets/references/` when
needed to preserve the generation inputs. Historical references remain as captured; rerender
into `out/` to reflect later game changes.

## Compose with imagegen

Use Codex's built-in imagegen tool for the illustration. Load and inspect each local reference
first, then identify which image is the edit target and which supply character or style references.
Keep the final image and the prompt in the repository; preserve originals when creating a revision.
The renderer supplies references, while imagegen composes the promotional scene.

A useful prompt outline:

```text
Use case: compositing / ads-marketing.
Asset: opaque App Store universal creative, landscape 16:9.
References: [banner to edit], [native character and lands board], [requested mode motif].
Main request: [the specific characters, lands or feature to highlight].
Style: match DDDumpling's flat 2D geometry, simple fills and dark-plum face strokes.
Main dumpling: slightly angular bao silhouette, short pinched tip, three pleat strokes,
  pale golden fill, inverted-V smiling eyes, W mouth, coral cheeks and one oval highlight.
Composition: hero centered horizontally, around 38% from the top, entirely inside
  x40%-60% and y23%-54%; smaller supporting characters and scenery around it.
Constraints: retain actual identities and anatomy; keep faces clear; no gameplay UI,
  phone mockups, scores, health bars, awards, prices, labels or invented environments.
Avoid: realistic food texture, plastic 3D shading and excessive neon bloom.
```

Use the [v3 prompt](../store/creative-assets/dddumpling-universal-v3-lands-survival.provenance.json)
for the full specification. Do not assume every supporting detail survives every device crop.
If the request adds several features, keep one main face and make the surrounding scenery quieter.

## Export and inspect

Verify [Apple's current specifications](https://developer.apple.com/help/app-store-connect/reference/app-information/creative-assets-specifications)
and use the current templates from [Apple's asset resources](https://developer.apple.com/app-store/asset-best-practices/).
The universal image specification checked on October 8, 2026 is **5244 × 2950 PNG without
alpha or transparency**, compatible with Header and Search Results. Dedicated header images
use 3840 × 1646; dedicated search images use 3:2 at the sizes Apple accepts.

The universal Photoshop template checked for this work has a 1402 × 962 art-safe area at
`x=1921, y=661`. It sits above the canvas center. Keep the entire main dumpling there,
including its tip and base. Download a fresh template if Apple changes it; these coordinates
describe the checked template rather than a permanent API contract.

Save a versioned source and export with macOS `sips`:

```sh
sips -z 2950 5244 path/to/generated-source.png --out path/to/universal.png
sips -g pixelWidth -g pixelHeight -g hasAlpha path/to/universal.png
sips -c 962 1402 --cropOffset 661 1921 path/to/universal.png \
  --out /tmp/dddumpling-safe-area.png
```

Inspect the full image and the safe-area crop. Check faces, boss anatomy, color/style fidelity,
readability and crowding. Use App Store Connect's Preview for actual device framing after
placement. The existing generated sources are 1672 × 941; the upload versions were resampled
to Apple's required dimensions. Resampling supplies the required pixel dimensions, not new
illustration detail. Keep that distinction in the provenance for future assets too.

## Upload to Asset Library

Use `tools/app-store-assets.py` with Python 3 and OpenSSL. It reserves an image, uploads Apple's
specified byte ranges and commits with `uploaded: true`. It checks PNG transparency, the live
specification catalog and duplicate reference names. It does not submit reviews or publish placements.

App Store Connect credentials are separate from Apple Ads credentials. Supply
`IOS_APPSTORE_KEY_ID`, `IOS_APPSTORE_ISSUER_ID` and `IOS_APPSTORE_KEY_FILE`, or create ignored
`.private/app-store-connect/config.json`:

```json
{
  "key_id": "YOUR_CONNECT_KEY_ID",
  "issuer_id": "YOUR_CONNECT_ISSUER_ID",
  "key_file": "/absolute/local/path/AuthKey_YOUR_CONNECT_KEY_ID.p8"
}
```

The first two environment names match the existing Fastlane configuration; `KEY_FILE` points
to a local key file. Keep that file, configuration and temporary upload URLs outside Git.
The default app is DDDumpling (`6811478003`); `--app-id` overrides it.

```sh
python3 tools/app-store-assets.py inspect
python3 tools/app-store-assets.py upload \
  --image ios/store/creative-assets/dddumpling-universal-v3-lands-survival.png \
  --name 'DDDumpling Universal v3 - Lands and Survival'
python3 tools/app-store-assets.py poll --asset-id ASSET_ID_FROM_UPLOAD
```

The v3 example has already been uploaded, so running that upload again should report the
duplicate name. Choose a new version name for changed artwork. The uploader saves private
reservation and upload receipts under `.private/app-store-assets/`. If processing is still
`UPLOAD_COMPLETE`, poll again; `PREPARE_FOR_SUBMISSION` means the image is ready for review.
Compare `specId` against the upload's `expectedSpecId` to confirm the intended format.

Submit the selected asset for review through Asset Library when that action is requested.
After approval, choose it through Browse Assets and enable **Use header asset in search results**
for the universal image. Assigning or publishing a placement is separate from uploading.
See [Apple's upload API](https://developer.apple.com/documentation/appstoreconnectapi/uploading-and-managing-image-assets).
