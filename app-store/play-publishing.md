# Automated internal testing uploads

GitHub's **Build Android release** workflow builds the production AAB and developer APK.
A `v*` tag also uploads that exact production AAB and release notes to Google Play's
`internal` track through fastlane. Manual runs upload only when **upload_to_play** is selected.
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

1. Increment `android:versionCode` in `AndroidManifest.xml`; use a value never uploaded to Play.
   Version code **10** was uploaded manually for the initial release and must not be uploaded again.
2. Update `android:versionName` and create
   `app-store/google-play/en-US/changelogs/<versionCode>.txt` (1–500 characters).
3. Commit and push. Push a matching version tag, such as `v0.1.10` for versionName `0.1.10`.
4. Watch all three workflow jobs. `upload_internal` must succeed to confirm Play accepted the build;
   a successful GitHub release alone does not confirm a Play upload.

A manual run with **upload_to_play** unchecked tests builds and fastlane configuration without
contacting Play. It is the correct verification before the credential has been added.
With the checkbox selected, the run publishes the selected ref's AAB to internal testing.
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

The tests exercise missing credentials, mismatched tags, missing/oversized notes and internal-only
upload options without network calls. GitHub uses Ruby 3.3 and locked gem dependencies.
`bundle exec fastlane android internal` performs a real upload and needs the signed bundle and
`GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` environment variable.

References:
- https://developers.google.com/android-publisher/getting_started
- https://docs.fastlane.tools/actions/supply/
