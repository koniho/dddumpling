# Automated testing uploads

GitHub's **Build Android release** workflow builds the production AAB, signed production APK,
and developer APK. The production APK is preserved from the same build used to create the AAB,
before the developer build overwrites the intermediate APK.
A `v*` tag also uploads that exact production AAB and release notes to Google Play's
closed testing track through fastlane (API ID `alpha` by default). Manual runs upload only when
**upload_to_play** is selected; **play_track** chooses `closed` (default) or `internal`.
Uploads remain drafts by default. Manual runs activate testing only with
**activate_play_release** selected; local lanes require `PLAY_RELEASE_STATUS=completed`.
To activate an already uploaded draft, run **Activate existing closed testing release**
from main with its `version_code`. This preserves its notes, requires the current
manifest code, supersedes older completed builds, and verifies the saved track without
re-uploading the bundle. Activate only after explicit user authorization.
Testing lanes do not publish to production or edit store descriptions, images, or screenshots.
New GitHub tag releases are drafts with reviewed copy from `release-notes/<version>.md`.

## Feature graphic uploads

The **Upload Google Play feature graphic** workflow runs pinned `gplay` 2.1.0 with the
existing Play service-account secret to inspect or replace only the `en-US` feature graphic.
It validates the checked-in `app-store/google-play/feature-graphic.png`, previews the delta,
uploads it and checks Google's saved checksum. Google applies its normal listing review
flow; this account requires automatic review when saving, so a save cannot be kept out of
review. The workflow refuses to cancel an existing review. It does not build or upload a
release, change a track, or edit other listing fields.

Provide the file's SHA-256 as `image_sha256`. Leave `apply` false to inspect the current
delta; set it true for an authorized upload. The service account needs **Manage store
presence** for DDDumpling in addition to its existing access.
If saving fails with HTTP 403 at `edits.commit`, open Play Console **Users and permissions**,
select the email printed by **Identify Play service account**, and add **Manage store presence**
under DDDumpling's app permissions. A successful preview or upload into an edit does not
prove permission to save the listing.

```sh
shasum -a 256 app-store/google-play/feature-graphic.png
gh workflow run play-feature-graphic.yml --ref main \
  -f image_sha256=APPROVED_SHA256 -f apply=true
```

The same CLI works locally:

```sh
bash tools/install-gplay.sh
export GPLAY_SERVICE_ACCOUNT=/absolute/private/path/service-account.json
bash tools/play-feature-graphic.sh          # Online preview
bash tools/play-feature-graphic.sh --write  # Upload and verify
```

The installer downloads the platform binary into ignored `build/tools/gplay/` and checks
its pinned release checksum. It supports macOS and Linux, Intel and ARM. `GPLAY_BIN` can
select an existing CLI; optional `PLAY_FEATURE_SHA256` pins the approved image locally.
Only the English feature graphic is copied into an ignored metadata tree under `.private/`.
The workflow retains the preview and current saved checksum as artifacts, never the credential.
See [image uploads](https://developers.google.com/android-publisher/api-ref/rest/v3/edits.images/upload)
and [commit behavior](https://developers.google.com/android-publisher/api-ref/rest/v3/edits/commit).

## Direct APK distribution (itch.io)

The itch workflow creates a fresh hidden staging channel by default. Select **activate**
only to update the live Android channel. Hidden uploads use a unique channel because
butler cannot hide a new build on an existing channel.

Upload `DDDUMPLING-v<version>.apk` directly to itch.io and select Android. The
`-developer.apk` is a separate debugging app and is not the public game build.
The production APK uses the existing CI Android signing key. Updates to sideloaded copies
must use that same key. Google Play may sign its distributed APKs with a different Play App
Signing key, so installation over a Play copy is not guaranteed.

For an existing tag, run **Build Android release** from the pipeline branch or main with
`source_tag` set to, for example, `v0.1.15`. This checks out that exact tag and produces
downloadable workflow artifacts. Tag rebuilds do not upload to Play or overwrite GitHub
release assets, even if `upload_to_play` is selected. Ordinary future version-tag releases
publish all three files to GitHub, while only the AAB goes to Google Play.

## One-time account setup

1. Create or select a Google Cloud project and enable the **Google Play Android Developer API**
   (`androidpublisher.googleapis.com`). No link between the Cloud project and Play account is required.
2. Create a service account, for example `dddumpling-releases`. Cloud project roles are not needed
   solely for Play publishing. Copy its `...iam.gserviceaccount.com` email address.
3. In Play Console **Users and permissions**, invite that address. Limit app access to
   DDDUMPLING (`com.dddumpling.game`), with **View app information (read-only)** and
   **Release apps to testing tracks**. Production publishing and administrator permissions are unnecessary.
4. In Google Cloud, open the service account's **Keys → Add key → Create new key → JSON**.
5. In the private GitHub repository's **Settings → Secrets and variables → Actions**, create
   `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`. Paste the entire JSON file into the secret value.
   Do not commit the key or paste it in a conversation. Keep a local copy only in private storage.

GitHub secret page: https://github.com/koniho/dddumpling/settings/secrets/actions

The upload job reports a clear failure if the secret is missing. APK/AAB release artifacts remain
available even if Play authentication or publishing fails. Never put credentials in workflow
artifacts. Rotate the key in Google Cloud and replace the GitHub secret when necessary.

## Each new release

1. Prepare the bump and notes using the release preparation tool below. Use an
   `android:versionCode` never uploaded to Play.
   Version code **10** was uploaded manually for the initial release and must not be uploaded again.
2. Update `android:versionName` and create
   `app-store/google-play/en-US/changelogs/<versionCode>.txt` (1–500 characters).
3. Commit and push. Push a matching version tag, such as `v0.1.10` for versionName `0.1.10`.
4. Watch all three workflow jobs. `upload_testing` must succeed to confirm Play accepted the build;
   a successful GitHub release alone does not confirm a Play upload.

A manual run with **upload_to_play** unchecked tests builds and fastlane configuration without
contacting Play. It is the correct verification before the credential has been added.
With the checkbox selected, the run publishes the selected ref's AAB to the chosen testing track.
Fastlane checks the package, matching tag, version-specific notes, bundle existence and credential
format before contacting Google. Google enforces version-code uniqueness.

If Google accepted an upload but a later operation failed, inspect Play Console before retrying:
re-uploading an already used version code is rejected. Complete the existing release in the console
or prepare a new version. Public rollout requires an explicitly authorized production promotion.

## Local configuration checks

On a supported Ruby installation:

```sh
bundle install
bundle exec ruby tools/test-fastlane.rb
bundle exec fastlane lanes
```

The tests exercise missing credentials, mismatched tags, missing/oversized notes and closed/internal-only
upload options without network calls. GitHub uses Ruby 3.3 and locked gem dependencies.
`PLAY_TEST_CHANNEL=closed bundle exec fastlane android testing` performs a real upload and needs the signed bundle and
`GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` environment variable.

References:
- https://developers.google.com/android-publisher/getting_started
- https://docs.fastlane.tools/actions/supply/

After adding or rotating the GitHub secret, manually run **Verify Google Play access**.
It authenticates, lists track IDs/version codes, and verifies that the configured closed track exists
without uploading or publishing a release.
A successful check confirms API access and track existence; publishing permissions are fully exercised on the next
new-version upload. This workflow can also be used to diagnose account or API setup errors.

## Closed-track setup

In Play Console, finish the app setup, then open **Testing → Closed testing** and create/manage
its track. Configure countries and the tester email list or Google Group. Share its opt-in link
once the closed release is available. Testers already enrolled in internal testing must opt out
of internal testing before opting into the closed test.

The default closed track's API ID is `alpha`; custom tracks may have a different ID from their
display label. **Verify Google Play access** lists the exact IDs. Set the GitHub Actions repository
variable `PLAY_CLOSED_TRACK` to the desired closed track ID if it is not `alpha`.
The automation rejects the reserved `production`, `beta` (open testing), and `internal` destinations
as closed targets. It does not create tracks or manage tester invitations.

Version code 10 can be selected from the existing artifact library in Play Console for the initial
closed release; do not upload it again. Future new versions go to closed testing on version tags.
Google may require review or completion of app-content declarations before a closed release is
available. A successful upload does not establish the 12-tester/14-day production-access requirement.

## Prepare release notes and version together

Follow [Executing a release](../docs/releasing.md): finish the in-game notes and store summaries
before creating or pushing a tag. Use the separate [writing guide](../docs/release-notes.md) to
add notes with game-phase context and player purpose. The tag must include the reviewed JSON,
generated game copy, and version-specific store notes.

The store-assets skill includes this automatically when preparing a release. The tool gathers
history and safely writes the result; the agent synthesizes player-facing wording from the evidence.
It does not call an external text-generation service or publish anything.

```sh
python3 tools/prepare-release.py context --output build/release-context.json
# Use --since <last-distributed-commit> if the last Play release had no Git tag.
# Review that context and relevant diffs; write concise player-facing notes to build/release-notes.txt.
python3 tools/prepare-release.py prepare --notes build/release-notes.txt
python3 tools/prepare-release.py prepare --notes build/release-notes.txt --write
```

`prepare` previews by default, increments the patch version/code, and accepts explicit
`--version-name` and `--version-code` overrides. It rejects empty/oversized notes, non-increasing
codes, and overwriting an existing changelog. It cannot determine remotely consumed version codes;
check Play before selecting one. Review and commit the manifest/changelog together before tagging.
For the first release after version code 10, use `context --since 3451055` as the historical baseline.

Run `python3 tools/test-prepare-release.py` to verify the tool in temporary Git repositories.

Manual-build artifacts use `DDDUMPLING-build-<run ID>` so branch names containing slashes are safe.
Tagged release filenames continue to use the version tag.

Developer APKs use `com.dddumpling.game.dev` and the launcher name **DDDUMPLING Dev**.
Production AABs retain `com.dddumpling.game`. Both can be installed together with separate saves.
The build checks the packaged application ID, label and launch activity after signing.

Release builds currently force Play Games configuration off. Before publication,
`tools/verify-offline-bundle.py` checks the production AAB for Google Play services
code, Play Games application metadata, and network permissions. Enabling Play Games
in a future release requires deliberately updating this workflow guard.
The access check also lists uploaded APK/AAB version codes, including artifacts
not assigned to a testing track.

## Retry an upload without rebuilding

If publishing fails, run **Verify Google Play access** first and inspect the uploaded
version codes and track state. If the failed transaction did not retain the new
version, **Upload existing release to closed testing** can upload its signed GitHub
release AAB using the latest publishing fixes. The tag must match the manifest
version on the selected ref. This workflow checks the offline bundle again and
publishes only to the configured closed track. Do not use it to re-upload a
version already retained by Play.

Fastlane stages only the selected English changelog into a temporary metadata
folder. The store-assets root also contains `screenshots`, which must never be
passed to Fastlane as a language directory.

## Promote uploaded builds to production

After explicit user authorization, run **Promote uploaded builds to production** from main.
Select the store(s), existing Play version code, Apple marketing version and exact Apple
build number. Leave `apply` false for a read-only preflight; set it true to promote the
existing Play bundle to production and/or submit the existing Apple build for automatic
release after approval. No binaries are rebuilt or uploaded. The workflow checks the
manifest and approved notes, preserves existing store assets, and verifies saved state.
Store review and processing may still delay public availability.

## Update listing text

**Update Google Play listing text** previews or publishes the checked-in `en-US` title,
short description and full description. It stages only those three fields, reads the
current listing before applying, and verifies other text and the video URL are preserved.
Run with `apply=false` to review the live diff, then `apply=true` for an authorized update.
The workflow uses normal Google review and refuses to cancel a review already in progress.
