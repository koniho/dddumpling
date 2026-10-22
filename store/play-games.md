# Play Games events and cross-device progress

This integration is opt-in at build time. The release workflow enables it for new
Play upload bundles using `store/play-games.draft.json`. The itch download has
**no Play SDK, sign-in, or network reporting**. Developer builds always include the SDK,
but without a Dev configuration they disable its initialization and all reporting. Production builds
record progress locally, independently of whether Play integration is configured.

## Build a Play-enabled test

1. Create the Play Games Services project and enable **Saved Games** in Play Console.
2. Link package `com.dddumpling.game` to the Android credential. Register the **Play app-signing**
   SHA-1 for installs from a Play track, and the local certificate for sideloaded test builds.
   The upload certificate is not necessarily the app-signing certificate.
3. Add tester accounts or the intended release track to Play Games testing access.
4. Create the events listed in `play-games.example.json`. Copy that file to
   `.private/play-games.json`, fill the numeric `project_id` and every event's Console-generated ID.
   IDs are configuration, not credentials; the validation rejects blank or duplicate event IDs.
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

## Side-by-side developer testing

Use `com.dddumpling.game.dev` and the existing local debug keystore. Register an Android OAuth
credential for that exact package and certificate SHA-1, then link it in Play Games Console.
Do not replace the Play app-signing credential. Enable Saved Games and add the testing account
to both the OAuth audience and Play Games tester list as applicable. An SDK-enabled APK alone
does not register the package or establish sign-in access.

A separate Play Games project is recommended: achievements and events belong to the project,
so developer cheats can affect real achievements if the projects are shared. A shared project
requires a dedicated test account. Dev cloud saves always use `dddumpling-dev-progress-v1`,
while production retains `dddumpling-progress-v1`.

Generate a configuration template for a separate test project:

```sh
mkdir -p .private
python3 tools/prepare-play.py --example --package-name com.dddumpling.game.dev > .private/play-games-dev.json
```

Fill `project_id`, every `events` ID and every `achievements` ID with that project's Console
values. The achievement mapping is keyed by the same stable IDs as `store/achievements.json`,
but its values are only Google achievement IDs. No Apple metadata is needed for this test project.
Keep `package_name` set to `com.dddumpling.game.dev`.

To intentionally share the existing project with a test account, copy `store/play-games.draft.json`
and add `"package_name": "com.dddumpling.game.dev"`. Its existing achievement mappings are reused.

```sh
DDDUMPLING_PLAY_CONFIG=.private/play-games-dev.json ./build.sh --developer
```

This keeps developer controls, enables local progress tracking and Play reporting, and builds
DDDUMPLING Dev with the normal developer checks and signing key. Configuration is validated
before checks or dependency downloads. With no configuration, Dev still packages the SDK but
omits project IDs, disables the SDK auto-init provider, and keeps sign-in/reporting off.
A progress reset does not reset remote Google achievements or erase a cloud save.

The local phone debug certificate SHA-1 (2026-10-22) is
`8A:14:84:D5:AA:65:8C:62:0D:CB:B0:24:C8:61:95:C7:1A:2A:80:90`.
Read the certificate from the key used for each installation before registering other machines.

Production setup (2026-10-14): linked Play Games Services to the existing Cloud project
`dddumpling-5695b`, project number/application ID `56613446939`. This reuses the Firebase
project's underlying Cloud project; it does not add a Firebase SDK or link Firebase to PGS.
The PGS configuration is still draft. On 2026-10-17, Saved Games was enabled and saved;
Console confirmed the Google Drive API is enabled. After explicit approval of the Google
API Services User Data Policy acknowledgement, OAuth consent was created with an External,
Testing audience. The saved scopes are `games`, `games_lite`, and `drive.appdata`.
Both OAuth and PGS tester lists contain `koniho@gmail.com` and `dddumpling.play@gmail.com`.

The Android OAuth client `DDDumpling Play signing` is linked to a saved draft PGS credential:

- Package: `com.dddumpling.game`
- Client ID: `56613446939-9fm7ajevtkeg2c7kinlb0k4vef5p9vv5.apps.googleusercontent.com`
- Play deployment certificate SHA-1: `48:EA:AB:E1:8D:76:BF:00:2D:B2:86:36:F7:CA:24:2B:39:90:16:AA`

The SHA-1 was read from Play Console's downloaded `deployment_cert.der`; its SHA-256
matches the certificate in Console's Digital Asset Links JSON. This is not the upload key.
The draft properties reuse the store title, short description, icon and feature graphic,
with Arcade as the category. Console reports credentials and events ready to publish with
no remaining property warnings. Saved changes are available to authorized testers; the PGS
project and OAuth consent are not publicly published. No Firebase SDK was added.

Real Android sign-in, event delivery and cross-device save validation remain unverified.
No Android device was attached at the 2026-10-17 check. Before a public release, complete
that validation, prepare the listing/privacy disclosures below, and publish both the OAuth
consent audience and PGS configuration. A draft-ready console is not a runtime test pass.

The original 90 hidden draft events were created on 2026-10-17. Rainbow Cat's 12 hidden
draft counters were created and read back on 2026-10-22; Console now shows 102 events.
Their non-secret IDs are in `play-games.draft.json`, with a local copy in ignored
`.private/play-games.json`. The original 90 Console IDs are preserved.
These IDs do not enable a release build or publish the PGS project. The catalog includes
Gummy Gator's 12 counters under the persisted `mossy_maw` prefix; that prefix must not be
renamed when changing player-facing text. `python3 tools/test-prepare-play.py` checks that
all runtime bosses and directly emitted events have definitions and the example matches
the build validator. Starter and Survival rewards have their own source counters too.

Boss event definitions are generated from the stable analytics IDs in `WorldCatalog`.
After adding a boss, regenerate the example with `python3 tools/prepare-play.py --example`
and update each configuration with `python3 tools/prepare-play.py --sync-config PATH`.
This preserves IDs and adds missing keys with blank IDs; it does not create Console events.
Create each missing event in Play Console as Hidden, Number with zero decimals, and save
as draft. Read back its actual generated ID into the draft and local configurations; never
invent IDs or replace an existing mapping. Event creation is a separate Console step, not
an automatic side effect of adding a boss. Publishing remains a separate release action.
Normal rules checks now reject missing, blank, duplicate or obsolete draft mappings as well
as stale examples, so adding blank placeholders cannot satisfy the future-boss check.
Stage counters already cover arbitrary future stages through the `20_plus` range.
Per-enemy counters are not implemented; adding enemies does not currently provide enemy-level
telemetry. Stable enemy identities, common lifecycle hooks and emission tests are still needed.

`build/play-compile-only.json` and
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
mechanic. Background and in-game pause time are excluded. The measurement is sampled at frame/input boundaries;
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
