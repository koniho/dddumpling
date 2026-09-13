# Play Games cloud saves

Branch: `feature/play-games-progress`. This integration is opt-in at build time. Ordinary builds,
including the first Play test release, have **no Play SDK, sign-in, or network reporting**.
Developer builds exclude the SDK even if a configuration file is supplied. Production builds
record progress locally, independently of whether Play integration is configured.

## Build a Play-enabled test

1. Create the Play Games Services project and enable **Saved Games** in Play Console.
2. Link package `com.dddumpling.game` to the Android credential. Register the **Play app-signing**
   SHA-1 for installs from a Play track, and the local certificate for sideloaded test builds.
   The upload certificate is not necessarily the app-signing certificate.
3. Add tester accounts or the intended release track to Play Games testing access.
4. Copy `play-games.example.json` to `.private/play-games.json` and fill its numeric `project_id`.
5. Install Java 17+ and Gradle (validated here with Gradle 9.7.1). Gradle resolves dependencies only;
   the game's existing aapt2/javac/d8 pipeline still builds the application. Dependency versions and
   SHA-256 checksums are checked in under `tools/play-deps/`.
6. Build with:

   ```sh
   DDDUMPLING_PLAY_CONFIG=.private/play-games.json ./build.sh --production
   # Use the same variable with the signing environment documented in ../app-store/play-publishing.md:
   DDDUMPLING_PLAY_CONFIG=.private/play-games.json ./build-bundle.sh
   ```

The pinned Games SDK 21.0.0 retains Android 5 / API 21 support. SDK 22 raises its minimum API to 24.
The build merges all library manifests/resources, generates their R classes, and packages all dex
files. It fails on unsupported native dependencies instead of silently dropping them.

No real project ID is available in this workspace yet. `build/play-compile-only.json` and
`build/play-sdk-compile-only.apk` are **packaging fixtures only**: do not distribute or install them.
A successful compilation does not establish authentication or prove server-side reporting.

## Optional Firebase Analytics

Firebase Analytics is independent of Play Games cloud saves. A configured production build prompts
for permission before Firebase is initialized or any data is collected. Copy Firebase Console's
Android `google-services.json` to `.private/firebase/google-services.json`, or set
`DDDUMPLING_FIREBASE_CONFIG` to a different local path. The file is ignored; the build validates
that it contains exactly one `com.dddumpling.game` client and creates the required resources.

Firebase is not included in developer builds. Allowing analytics sends gameplay events with an
`amount` parameter, plus Firebase's app/device information, approximate location, and app-instance
ID. Advertising-ID collection and ad-personalization signals are disabled. The title screen's
Privacy link lets a player allow or withdraw permission; withdrawal disables collection and resets
Firebase analytics data without changing cloud saves or local progress.

## Save behavior

One private Play Saved Games slot, `dddumpling-progress-v1`, contains best score, highest stage,
character counts, lifetime minigame successes, and cumulative gameplay counters. It does not save
an in-progress run, temporary boss state, speed/music settings, or adaptive roster choices.

The existing preferences and collection format remain compatible. A shared legacy baseline
imports existing counts without inventing past duplicate awards. New rewards are counted per
installation. Merging takes the maximum for each installation's component, then adds components;
this preserves independent offline rewards and makes retries idempotent. Best score and highest
stage use maxima. When two devices already had separate pre-integration histories, their shared
legacy component uses the higher count; those older histories cannot be reliably deduplicated.

Writer IDs live in Android's no-backup directory. A restored installation gets a new writer ID,
so Android backup cannot clone an active counter writer. Cloud data is merged locally before it
is committed remotely; malformed, oversized, or newer-schema data blocks the sync rather than
being overwritten. Saved games are limited to 512 KiB / 4096 entries, with headroom reserved for
new metrics. Local gameplay continues if tracking storage reaches its limit or fails.

Cloud restores appear in the collection and score at the title screen, avoiding mid-run changes
to rewards and difficulty. The merged snapshot is saved locally immediately, so a restart before
returning home still restores it. Sync runs after authentication, batches changes for five seconds,
checks periodically while foregrounded, and attempts a checkpoint on pause. Google can cache
snapshot commits offline; successful SDK completion is not proof another device has received it.

The first authenticated Play player is bound to this installation's local progress. A different
player cannot inherit or upload that progress: sync pauses with an explanation. Account switching
with separate local profiles is not implemented. Return to the original Play Games profile to
resume syncing. An unconfigured build remains a local-only game.

## Before enabling for testers

- Complete real-device tests with two authorized Play accounts/devices: first install, declined
  sign-in, airplane mode, reconnect, simultaneous rewards, repeated sync, app restart, and an
  attempted account switch. Verify that cloud saves survive a reinstall with the same account.
- Verify the consent prompt, analytics events, withdrawal/reset, and cloud saves independently on
  real devices. Finalize privacy policy and Data safety disclosures before distributing a Firebase-enabled build.
- Publish Play Games configuration separately from the app when ready; uploading an APK/AAB alone
  does not publish the Play Games settings.

References: [setup](https://developer.android.com/games/pgs/console/setup),
[authentication](https://developer.android.com/games/pgs/android/android-signin),
[saved games and conflicts](https://developer.android.com/games/pgs/android/saved-games),
[SDK data disclosure](https://developer.android.com/games/pgs/data-collection).
