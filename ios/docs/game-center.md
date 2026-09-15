# Native game services

Android keeps its existing `play-src/PlayBridge`: automatic Play Games sign-in checks,
merged Saved Games snapshots and best-effort Play Events. It is selected only for an
explicitly configured **production** build and refuses developer builds. This feature
does not change that behavior.

iOS enables GameKit through the **DDDUMPLING_GAME_CENTER** build setting (0 or 1).
Debug defaults to 1; Release defaults to 0 and must explicitly opt in, like Android's
configured production adapter. When enabled, it initializes the local
player at launch, presents Apple's authentication controller when needed, and keeps
the game playable when sign-in is cancelled or unavailable. There is no new dashboard
button. Authentication UI cancels touches and pauses gameplay/audio; dismissing it
leaves an interrupted run paused for the player to resume.

Debug installs appear as **DDD Dev** on the Home Screen. Tap **DEV · Game Center**
on the title screen, in settings or while paused to inspect authentication, cloud-save
status and the latest service error. Release installs retain the **DDDUMPLING** name.

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
3. Build Debug with the development team. `ios/project.yml` selects
   `GameCenter.entitlements` only when the services flag is 1.
4. Register the matching app record in App Store Connect and enable Game Center on its
   iOS version page. Sign into Game Center and iCloud on the device and enable iCloud Drive.
5. Earn progress on two development devices using the same accounts, including offline
   rewards, then reconnect and verify that counts merge once and best scores never decrease.

The development container must never be assigned to a public build. This does not transfer
Android saves to iOS. Leaderboards, achievements and multiplayer
remain separate work. GameKit has no direct Play Events analytics counterpart; the existing
progress counters are preserved inside the cloud save without adding an analytics SDK.

## Enable a production build

Configure Game Center and iCloud Documents for `com.dddumpling.game.ios`, associate
`iCloud.com.dddumpling.game.ios`, and enable Game Center on the production app's iOS
version in App Store Connect. Refresh its provisioning profile before signing.

```sh
CONFIGURATION=Release DDDUMPLING_GAME_CENTER=1 bash ios/scripts/build.sh iphoneos
DDDUMPLING_GAME_CENTER=1 bash ios/scripts/archive.sh
# Direct Xcode builds use the same setting:
# xcodebuild ... DDDUMPLING_GAME_CENTER=1
```

Supply `DEVELOPMENT_TEAM` for signed command-line builds. The flag controls both native
authentication/cloud code and signing entitlements. A value of 0 excludes GameKit service
calls and both entitlements, while retaining local progress. The iCloud container expands
from `PRODUCT_BUNDLE_IDENTIFIER`, keeping Debug and Release separate. The status button,
developer settings and diagnostic scene controls remain Debug-only.

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
The cloud suite runs under both Java build configurations; Release also checks the public
constructor with the service flag off and on.
Native tests cover authentication presentation/lifecycle and durable account binding.
The UI smoke suite disables live Game Center using `DDD_GAME_CENTER_DISABLED=1`; unit
tests inject a player instead of contacting Apple. The same environment switch is available
for offline diagnostics, and cannot enable services excluded by the build flag.

Automated tests do not establish successful Apple account provisioning or two-device sync.
Record signed-device sign-in, cancellation, account switching, conflicting offline rewards,
relaunch, update preservation and iCloud-unavailable results before production enablement.

Local verification (2026-09-15): 5,585 shared assertions and 92 production checks passed;
frame and sound hashes are unchanged. iOS input and cloud suites passed, including a
separately compiled Release gate test. The full native/UI run passed 23 tests. Debug and
Release simulator builds passed, as did an unsigned Release iPhone archive with no
GameKit service symbols or bundled development entitlements. The final focused native/UI
rerun passed 11 tests. Developer Game Center sign-in was subsequently confirmed on the
physical iPhone after enabling Game Center for the `.dev` app in App Store Connect.
Live two-device synchronization and production provisioning remain unverified.

Release opt-in verification: enabled and disabled Release simulator builds passed;
the disabled binary has no GameKit service symbols and resolves no signing entitlements.
The enabled build selects the Game Center entitlements and production bundle ID.
All eight authentication/storage tests passed under Release with test symbol visibility
enabled for XCTest. Java checks passed: 107 input, 24 cloud in each configuration and
five Release gate assertions.
The final Debug regression run passed three authentication tests and the title/status/pause
UI smoke test with the default flag enabled.

References: [initialization/configuration](https://developer.apple.com/documentation/gamekit/initializing-and-configuring-game-center),
[GameKit saved games](https://developer.apple.com/documentation/gamekit/saving-the-player-s-game-data-to-an-icloud-account).
