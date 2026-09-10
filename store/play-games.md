# Play Games events and cross-device progress

Branch: `feature/play-games-progress`. This integration is opt-in at build time. Ordinary builds,
including the first Play test release, have **no Play SDK, sign-in, or network reporting**.
Developer builds exclude the SDK even if a configuration file is supplied. Production builds
record progress locally, independently of whether Play integration is configured.

## Build a Play-enabled test

1. Create the Play Games Services project and enable **Saved Games** in Play Console.
2. Link package `com.sram.hexatype` to the Android credential. Register the **Play app-signing**
   SHA-1 for installs from a Play track, and the local certificate for sideloaded test builds.
   The upload certificate is not the app-signing certificate.
3. Add tester accounts or the intended release track to Play Games testing access.
4. Create the 76 events listed in `play-games.example.json`. Copy that file to
   `.private/play-games.json`, fill the numeric `project_id` and every event's Console-generated ID.
   IDs are configuration, not credentials; the validation rejects blank or duplicate event IDs.
5. Install Java 17+ and Gradle (validated here with Gradle 9.7.1). Gradle resolves dependencies only;
   the game's existing aapt2/javac/d8 pipeline still builds the application. Dependency versions and
   SHA-256 checksums are checked in under `tools/play-deps/`.
6. Build with:

   ```sh
   DDDUMPLING_PLAY_CONFIG=.private/play-games.json ./build.sh --production
   # Use the same variable with the signing environment documented in store/README.md:
   DDDUMPLING_PLAY_CONFIG=.private/play-games.json ./build-bundle.sh
   ```

The pinned Games SDK 21.0.0 retains Android 5 / API 21 support. SDK 22 raises its minimum API to 24.
The build merges all library manifests/resources, generates their R classes, and packages all dex
files. It fails on unsupported native dependencies instead of silently dropping them.

No real project/event IDs are available in this workspace yet. `build/play-compile-only.json` and
`build/play-sdk-compile-only.apk` are **packaging fixtures only**: do not distribute or install them.
A successful compilation does not establish authentication or prove server-side reporting.

## Event definitions

- Runs: started, finished through game over, explicitly abandoned by returning home/restarting.
- Stages: each entered stage contributes once to its range (1–4, 5–9, 10–14, 15–19, 20+).
  Game-over stage uses the same ranges. `stages_completed` counts cleared stages once.
- Each boss: started, won, failed, abandoned, and attempts ending without any damage.
- Each minigame: started, won, failed, abandoned. Boss celebrations are not minigame attempts.
- Rewards: total, new-to-the-local-case, duplicate, and originating minigame/boss.
  Cube prizes keep the source of the game that awarded them.

The events API supports cumulative counters. These are not individual analytics records with
arbitrary parameters. The app increments events only while authenticated in a configured
production build. Google's SDK handles its own batching. Events before authentication are not
replayed; neither migrated saves nor cloud restores emit historical events. Failed/ambiguous
increments are not retried by the app, to avoid duplicate reporting. Statistics are therefore
best-effort and cover authenticated play, not every installation. Process kills cannot reliably
produce an end-of-run event; explicit game-over and return-home paths do.

### Boss time to first damage

The clock starts after the arrival card, when `Boss.fighting()` becomes true. It measures foreground
elapsed time before the game's slow-motion multiplier, including time spent figuring out the
mechanic. Background time is excluded. The measurement is sampled at frame/input boundaries;
allow approximately one frame of timing error. Shield bounces, successful prompts, parries, and
other actions that leave HP unchanged do not finish the timer.

For each boss:

- `boss_<name>_first_hit_count`: attempts with a real first damage hit.
- `boss_<name>_first_hit_ms_total`: sum of their first-hit times, in milliseconds.
- One timing bucket: under 5 seconds, 5–15, 15–30, 30–60, or 60+.
- `boss_<name>_no_damage`: failed or explicitly abandoned encounters with no damage hit.

Average seconds to first hit = `first_hit_ms_total / first_hit_count / 1000`.
Keep the no-damage count beside the average so difficult encounters are not hidden by considering
only players who succeeded. A timing of zero is represented as one millisecond because Play event
increments must be positive. Buckets use inclusive lower bounds, exclusive upper bounds.

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
- Verify event counters in Console; verify bosses that get no damage as well as first-hit timing.
- Update the listing's local-only sentence and finalize privacy policy/Data safety disclosures
  for the SDK's automatic collection and the submitted gameplay events. The existing listing and
  privacy draft describe the first, SDK-free release and must not accompany an enabled build unchanged.
- Publish Play Games configuration separately from the app when ready; uploading an APK/AAB alone
  does not publish the Play Games settings.

References: [setup](https://developer.android.com/games/pgs/console/setup),
[authentication](https://developer.android.com/games/pgs/android/android-signin),
[events](https://developer.android.com/games/pgs/android/events),
[saved games and conflicts](https://developer.android.com/games/pgs/android/saved-games),
[SDK data disclosure](https://developer.android.com/games/pgs/data-collection).
