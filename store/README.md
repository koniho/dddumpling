# Google Play submission preparation

Prepared September 10, 2026. This is a submission workspace, not confirmation of Play approval.

## Current build

- Package: `com.sram.hexatype` (keep this to preserve app identity).
- Prepared version: `0.1.9`, version code `10`; the published GitHub release remains `v0.1.8`.
- Target SDK: Android 16 / API 36. Minimum: Android 5 / API 21.
- Category is declared as game in the Android manifest.
- Production bundles compile out access to the entire settings pane and all developer actions.
- Java-only app: no native libraries or advertising/analytics SDKs.
- App bundle: `build/DDDUMPLING.aab`.
- Local installable APK: `hexatype.apk`.

## Build and sign

`./build-bundle.sh --unsigned` runs the full build/checks and validates a bundle with pinned,
SHA-256-verified bundletool 1.18.3. Unsigned output is for inspection, not Play upload.

A separate Play upload key has been generated locally in `.private/play-upload.keystore`, with its
password in `.private/play-upload.password`. Both are gitignored, with restrictive permissions.
Back up this directory securely before relying on it for releases. The public certificate is
`build/play-upload-certificate.pem`; only the certificate is appropriate to share with Play.

To reproduce a signed upload bundle locally:

```sh
export HEXATYPE_KEYSTORE=.private/play-upload.keystore
export HEXATYPE_KEY_ALIAS=dddumpling-upload
export HEXATYPE_KEYSTORE_PASSWORD="$(cat .private/play-upload.password)"
./build-bundle.sh
unset HEXATYPE_KEYSTORE_PASSWORD
```

The script refuses to silently create a fallback upload key. Existing APK builds continue using
their existing app-signing key unless the explicit signing environment overrides it.

**Play App Signing is a separate setup choice.** The upload key authenticates uploads; it does not
have to be the app-signing key. To permit existing sideloaded installations to upgrade to the Play
version without losing app data, configure Play with the existing app-signing key. A new
Google-generated app-signing key will not match those existing installations. Complete this choice
before enrolling the app or distributing a test release. Never upload private keys to this repo.

## Listing materials

- `listing-en-US.md`: draft title, short/full description and release notes.
- `privacy-policy-draft.md`: source-grounded draft, requiring developer/contact details and hosting.
- `out/play-store/icon-512.png`: icon matching the existing launcher artwork.
- `out/play-store/feature-1024x500.png`: original game lettering and characters.
- `out/play-store/screenshots/`: 1080 × 2400 frames rendered by the game's actual drawing code.
  Review on-device captures before final submission; the harness uses a different text font.

Regenerate artwork after running `./check.sh`:

```sh
java -cp build/harness com.sram.hexatype.StoreAssets
FRAMES=1-title,2-wave,40k,76-boss,95-boss-friend-2 java -Xmx512m -cp build/harness \
  com.sram.hexatype.Preview 1080 2400 2 out/play-store/screenshots
```

## Console decisions still needed

1. Complete developer-account identity, contact and device verification.
2. Create the app as DDDUMPLING, English, game. Decide price, countries and target age groups;
   cute artwork alone is not an audience declaration. Complete the content-rating questionnaire.
3. Configure Play App Signing with the intended app-signing certificate and register the upload key.
4. Supply a support email and developer name. Host the finalized privacy policy on a public HTTPS
   page and link it from the app and Play listing before public submission.
5. Complete App content. Current code has no ads, purchases or login; all features are accessible
   without credentials. Data Safety candidate answers: no developer collection or sharing of user
   data. Review Android backup and the external device speech engine when making final declarations.
6. Upload the signed AAB to the chosen test track, inspect Play's checks, and test on real devices
   including Android 16 (insets, gesture navigation, resume, audio and collection persistence).
7. For a new personal account, complete a closed test with at least 12 testers opted in continuously
   for 14 days, then apply for production access. Internal testing does not satisfy this closed test.
8. Review the final listing and rollout settings before submitting for review/publication.

No Play Console access is connected here; verification and Console submission are not completed.

## Official references

- [Target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878)
- [Command-line bundle building](https://developer.android.com/build/building-cmdline)
- [Play App Signing](https://developer.android.com/studio/publish/app-signing)
- [Preview asset specifications](https://support.google.com/googleplay/android-developer/answer/9866151)
- [Data safety guidance](https://support.google.com/googleplay/android-developer/answer/10787469)
- [New personal-account testing](https://support.google.com/googleplay/android-developer/answer/14151465)
