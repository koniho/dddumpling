# Automated testing uploads

GitHub's **Build Android release** workflow builds the production AAB and developer APK.
A `v*` tag also uploads that exact production AAB and release notes to Google Play's
closed testing track through fastlane (API ID `alpha` by default). Manual runs upload only when
**upload_to_play** is selected; **play_track** chooses `closed` (default) or `internal`.
No lane publishes to production or edits store descriptions, images, or screenshots.

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
or prepare a new version. Public rollout remains a manual Play Console action.

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
