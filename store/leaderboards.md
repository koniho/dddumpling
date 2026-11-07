# Leaderboard submission and publication

Issue [#237](https://github.com/koniho/dddumpling/issues/237) defines eleven production boards.
Their non-secret IDs and units are in [leaderboards.json](leaderboards.json). Check the
inventory against runtime code with `python3 tools/leaderboards.py check`; `plan` prints
all IDs and the publication handoff. Neither command calls a store or submits scores.

## Rank notifications

After a new best is accepted, the native service reads the player's global all-time rank.
The rank must be positive, belong to the authenticated owner, and contain the submitted score
(or a better one). Failed, stale, or not-yet-ranked responses retry; upload success alone never
produces a rank alert. Pending rank reads survive restart and shown alerts are deduplicated.
Existing acknowledged scores from the older queue format are not announced on upgrade.

At a safe title/results screen, Android posts through the **Leaderboard results** notification
channel and iOS uses User Notifications. Both request system permission when first needed;
permission refusal or disabled alerts uses a short in-game banner instead. Tapping a system
notification opens its captured board once that account is authenticated and gameplay is idle.
The OS controls banner/sound delivery. These are local notifications following an app-confirmed
score, not remote monitoring while the app is closed. They need no APNs/FCM server.

## Eligible runs

Only newly completed runs enter the queue. Local high-score history, archived Time Attack
rosters, restored cloud progress, and old best values are never backfilled.

- Survival uses its captured 4 Keys, 6 Keys, or Kids profile and existing active-time clock.
- Boss Time Attack requires a successful 6 Keys, non-Kids clear. All Bosses requires all
  six current bosses. Failed, abandoned, zero-time, and earlier-roster clears do not qualify.
- Adventure requires a non-Kids run begun at the first land with zero confirmed boss retries.
  This is the conservative global eligibility policy for the single normal Adventure board;
  local rankings and retry disclosure from #206 remain unchanged. Checkpoint and retry
  categories would need separately approved boards or a deliberate policy change.

The run captures its board and previously authenticated player when it starts. Runs before
the first successful authentication stay local. Offline runs can queue for the last proven
player; they can be uploaded only while that same account is authenticated. Switching accounts
mid-run never transfers the result to the new account. The local queue retains up to eight
account records; exceeding that bound leaves additional accounts' runs local.

The queue persists each account's best result and last successfully acknowledged result.
Only a successful native callback acknowledges a value. Newer, better pending values survive
late callbacks, failures and relaunch. Platform BEST_SCORE behavior makes ambiguous retries
idempotent. Save failures block upload rather than sending an unpersisted record. The queue
is not part of the game's cloud progress snapshot. Restoring a device backup retains the
original account IDs; it does not reassign scores to a different account.

Survival milliseconds round down to Apple whole seconds. Time Attack milliseconds round up
to Apple centiseconds, avoiding a faster displayed rank than the measured time. Google gets
milliseconds for both. A sub-second Survival result has no positive Apple whole-second score;
it stays local/pending without blocking another board. Adventure uses integer points.

Both native adapters disable leaderboard browsing and submission in Dev builds because no
isolated Dev boards exist. Preview/test cores also cannot capture submissions. Enabling Play
Games for achievements in Dev does not enable these production leaderboard IDs. Game Center
leaderboards require Game Center authentication, independently of iCloud Drive availability.

## Publication status and handoff

The issue's November 6, 2026 provisioning report says all eleven Apple resources were
`PREPARE_FOR_SUBMISSION`, and all eleven Google resources were drafts with tamper protection.
That report is a provisioning snapshot, **not proof of current publication**. No leaderboard
configuration or app release was published by this implementation.

This Termux host has neither the `asc` nor `gplay` executable and no connected store tool.
Live configuration checks, isolated native service tests and publication require the configured
store workstation/accounts. This is separate from compiling and installing the offline Dev app.

1. On that workstation, verify all eleven live IDs against the inventory, score units, rank
   directions and best-score aggregation. Inspect draft and published Google versions separately;
   listing a draft does not prove the board is live. Keep the existing Gummy Gator Apple ID
   containing `mossy_maw` and the six-roster `all_6` ID.
2. Provision isolated Dev leaderboards and explicit environment routing before synthetic native
   QA. Test offline/reconnect, restart, cancellation, account switching, duplicate callbacks,
   owner mismatch, and Game Center with iCloud Drive disabled. Verify actual native presentation
   and returned scores in that environment. Do not send fixture scores to the production IDs.
3. Follow [the release workflow](../docs/releasing.md): approve final release notes/reset choices
   before preparing or publishing an app version. Include leaderboard access and the clean-run
   Adventure eligibility in the proposed notes/review information.
4. In App Store Connect, attach the eleven new Game Center leaderboard versions to the approved
   app version's Game Center review submission. Submit the app/Game Center resources for review,
   resolve validation, and verify accepted/live state. Creating the resources alone does not
   release them. Check [Apple's review guidance](https://developer.apple.com/help/app-store-connect/manage-submissions-to-app-review/submit-game-center-components).
5. In Google Play Console, review **Play Games Services → Setup and management → Publishing**
   and publish the intended configuration. The Games Configuration API (and `gplay games`)
   edits draft resources; it has no leaderboard publication API. Review the publishing summary
   because configuration publication can include other outstanding game-service changes. Follow
   [Google's publication guidance](https://developer.android.com/games/pgs/console/publish); allow at least two hours
   for Play Games configuration propagation before the app goes live.
6. Publish the approved app build through its normal release pipeline. Record actual service
   publication/review state and native smoke-test evidence in #237. Keep the ticket open until
   its native QA and release acceptance criteria are met.

Native submission uses Google's `submitScoreImmediate` and Apple's
`GKLeaderboard submitScore:context:player:leaderboardIDs:completionHandler:`. No cloud-save
merge or records-screen rendering triggers a historical score submission.
