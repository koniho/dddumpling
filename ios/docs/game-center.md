# Native game services

Android keeps its existing `play-src/PlayBridge`: automatic Play Games sign-in checks,
merged Saved Games snapshots and best-effort Play Events. It is selected only for an
explicitly configured **production** build and refuses developer builds. This feature
does not change that behavior.

iOS uses GameKit in **Debug/developer builds only** for now. It initializes the local
player at launch, presents Apple's authentication controller when needed, and keeps
the game playable when sign-in is cancelled or unavailable. There is no new dashboard
button. Authentication UI cancels touches and pauses gameplay/audio; dismissing it
leaves an interrupted run paused for the player to resume.

`DDGameCenter` handles authentication/presentation; `DDGameCloud` adapts GameKit saved
games to the pure-Java `IOSCloud` coordinator. `IOSCloud` reuses `Progress` and
`ProgressData`, including their per-install counters and maximum-score conflict merge.
iOS developer saves record progress in the separate `.dev` application container.
Shared Android/harness construction retains its existing progress flag behavior.

## Configure a development device

1. Enable Game Center and iCloud Documents for the development App ID
   `com.dddumpling.game.ios.dev` in the Apple Developer account.
2. Create/associate the development iCloud container
   `iCloud.com.dddumpling.game.ios.dev` and refresh its development provisioning profile.
3. Build Debug with the development team. `ios/project.yml` assigns
   `GameCenterDebug.entitlements` only to Debug. Release has no Game Center/iCloud
   entitlement, native initialization or cloud service calls.
4. Sign into Game Center and iCloud on the device and enable iCloud Drive. Register the
   matching app record in App Store Connect if Apple's Game Center setup requires it.
5. Earn progress on two development devices using the same accounts, including offline
   rewards, then reconnect and verify that counts merge once and best scores never decrease.

The development container must never be assigned to a public build. This does not transfer
Android saves to iOS. Leaderboards, achievements, multiplayer and production activation
remain separate work. GameKit has no direct Play Events analytics counterpart; the existing
progress counters are preserved inside the cloud save without adding an analytics SDK.

## Save behavior

Local checkpoints remain authoritative offline. Sync fetches on foregrounding, debounces
changes for five seconds, and refreshes/retries after sixty seconds. Backgrounding checkpoints
the run and allows an already authenticated save to finish; it does not start periodic work.
Cloud conflicts are all decoded before local merge or upload. Invalid data blocks sync for
the session; network failures back off. Native storage failures prevent upload.

The local store binds to both the Game Center player ID and the archived iCloud identity
token. A mismatch fails closed, preserving the save instead of copying it into another
account. Each Game Center player has a separate hashed save filename inside iCloud.
Authentication and iCloud identity are rechecked before processing asynchronous results
or submitting a write. A write already handed to Apple's service cannot be recalled.

## Verification

`bash ios/scripts/test-input.sh` includes cloud tests for merging, repeated conflicts,
offline play, stale callbacks, debounce/backoff, background checkpoints and disabled gating.
Native tests cover authentication presentation/lifecycle and durable account binding.
The UI smoke suite disables live Game Center using `DDD_GAME_CENTER_DISABLED=1`; unit
tests inject a player instead of contacting Apple. The same environment switch is available
for offline developer diagnostics, and cannot enable Game Center in Release.

Automated tests do not establish successful Apple account provisioning or two-device sync.
Record signed-device sign-in, cancellation, account switching, conflicting offline rewards,
relaunch, update preservation and iCloud-unavailable results before production enablement.

Local verification (2026-09-15): 5,585 shared assertions and 92 production checks passed;
frame and sound hashes are unchanged. iOS input and cloud suites passed, including a
separately compiled Release gate test. The full native/UI run passed 23 tests. Debug and
Release simulator builds passed, as did an unsigned Release iPhone archive with no
GameKit service symbols or bundled development entitlements. The final focused native/UI
rerun passed 11 tests. The paired physical iPhone reports unavailable, so
Apple provisioning and live two-device synchronization have not been verified.

References: [initialization/configuration](https://developer.apple.com/documentation/gamekit/initializing-and-configuring-game-center),
[GameKit saved games](https://developer.apple.com/documentation/gamekit/saving-the-player-s-game-data-to-an-icloud-account).
