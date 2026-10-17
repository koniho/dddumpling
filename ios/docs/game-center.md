# Native game services

Android keeps its existing `play-src/PlayBridge`: automatic Play Games sign-in checks,
merged Saved Games snapshots and best-effort Play Events. It is selected only for an
explicitly configured **production** build and refuses developer builds. This feature
does not change that behavior.

iOS enables GameKit through the **DDDUMPLING_GAME_CENTER** build setting (0 or 1).
Debug and Release both default to 0 and must explicitly opt in. When enabled, it initializes the local
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
3. Build Debug with `DDDUMPLING_GAME_CENTER=1` and the development team. `ios/project.yml` selects
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
# Fastlane's signed archive uses the same opt-in:
DDDUMPLING_GAME_CENTER=1 bundle exec fastlane ios archive
```

Supply `DEVELOPMENT_TEAM` for signed command-line builds. The flag controls both native
authentication/cloud code, framework linking and signing entitlements. A value of 0 excludes
GameKit linking, service calls and Game Center/iCloud entitlements, while retaining local progress.
Unset or empty script values also leave services disabled; other values are rejected.
The iCloud container expands
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
Run `bash ios/scripts/test-simulator.sh --full` for the default-disabled build, then
`DDDUMPLING_GAME_CENTER=1 bash ios/scripts/test-simulator.sh --full` for the enabled build.
The build-gate test verifies that an injected player cannot bypass a disabled compilation.
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
The original Debug regression run passed three authentication tests and the title/status/pause
UI smoke test with the then-default flag enabled. Current builds require explicit opt-in.

Default-off completion (2026-09-16, issue #63): 5,684 shared and 100 production assertions,
144 iOS input assertions, 24 cloud assertions in each Java configuration and five production
cloud-gate assertions passed. All 475 current preview PNG/WAV artifacts match main exactly.
The default Debug simulator passed 20 native and three UI tests; explicit opt-in passed nine
authentication/storage tests and the title/status/pause UI test. Fastlane flag tests and ten
CI/cache contract tests passed. Both unsigned Release device archives built successfully.
Binary inspection found no GameKit linkage or service symbols in the default Debug/Release
builds; the enabled Release binary links GameKit. Resolved Release settings omit entitlements
by default and select `GameCenter.entitlements` with the production bundle ID on opt-in.
Two-device sync and production provisioning remain unverified; issue #75 tracks that work.

Production setup inspection (2026-10-14, issue #75): enabled `GAME_CENTER` and `ICLOUD`
(`XCODE_6`) through the App Store Connect API for production bundle resource `LW49Y2G84A`
(`com.dddumpling.game.ios`), then verified both capabilities by API and in the Developer portal.
Created and attached `iCloud.com.dddumpling.game.ios` (DDDumpling Production Saves), without
assigning the development container. Regenerated `match AppStore com.dddumpling.game.ios`
using the existing distribution certificate `8F45S3FDPB`. Apple reports profile `CZAHQAA639`
(UUID `ff1cbba2-6b20-4629-bddf-a015b6683f1a`) as `ACTIVE`.
The encrypted profile is committed and remotely verified in the private signing repository
at `18d32e5ed98c58f2b6b5e4bfb93ca2b912d7d980`. Its CI deploy key remains read-only.

Built the signed Release archive at `ios/build/GameServicesProduction.xcarchive` from
`0d5c81c` with `DDDUMPLING_GAME_CENTER=1`, manual Apple Distribution signing and the
regenerated profile. `xcodebuild archive` succeeded and `codesign --verify --deep --strict`
passed. The signed app contains `com.apple.developer.game-center = true`, `CloudDocuments`,
and only `iCloud.com.dddumpling.game.ios` in both iCloud and ubiquity container arrays.
Its application identifier is `QC7LYR5635.com.dddumpling.game.ios`; no development container
is present. Evidence and build logs are local under `.private/game-services/`.

App Store Connect currently has Game Center disabled for the released iOS version `0.1.17`;
enable it for the next version when that version is prepared. No new store version, upload
or release was created.

Physical iPhone 12 validation (2026-10-14): installed a production-ID Release build with
Game Center enabled and Apple Development signing, without uninstalling. The existing save
was backed up and remained byte-for-byte identical immediately after installation.
`devicectl` confirmed launch (PID 944). After launch, the save contains `cloudPlayer` and
`cloudIdentity` bindings that were absent in the backup. The code only writes these after
checking authenticated Game Center identity and iCloud availability, confirming account
binding on this device. Console output did not capture the authentication callback and
debugger inspection did not complete. Successful cloud transfer, two-device synchronization
and the remaining physical-device failure/recovery matrix are unverified. Issue #75 remains open.

Only one physical device is available. The agreed remaining sync test uses that iPhone
and the iPhone 17 Pro Max simulator (iOS 26.5), with the same Game Center and iCloud accounts.
Two-physical-device synchronization remains an explicit release limitation. The enabled
production-ID Release simulator build succeeded with `ARCHS=arm64 ONLY_ACTIVE_ARCH=YES`
to match the local J2ObjC library. Installation preserved the existing simulator data,
which was backed up first. The app launched and rendered its title screen; Apple requested
account verification in Settings. After verification, the runtime log confirmed
`Game Center: Connected`. LLDB inspection of `gameCloud.status` returned
`iCloud Drive unavailable`; the simulator save still has no cloud account bindings.
After enabling iCloud Drive, both the simulator and physical iPhone reported `Synced`
through runtime inspection. Their Game Center player identifiers and production container
configuration match; the user confirmed the same iCloud account. Their authoritative progress
records remain different after repeated foreground sync cycles. The phone retains best score
15875/stage 7 and the simulator 19183/stage 5. The user reports that iCloud storage is full,
which can prevent uploads and is the current suspected blocker. Cross-device transfer remains
unverified. The current `Synced` status only reflects a successful GameKit save callback;
it must not be presented as proof of server upload or cross-device convergence.

After the user freed storage, an explicit Simulator iCloud sync still showed no convergence.
Inspection of the saved-game package initially returned `NSURLUbiquitousItemIsUploadedKey = 0`
and upload error `NSCocoaErrorDomain 4355` (iCloud account inaccessible). After relaunching
the simulator app and triggering sync again, the error cleared but the package still reported
uploading, not uploaded. The physical app was also relaunched successfully and authenticated.
Local progress was preserved; cross-device transfer is not yet established.
The phone's saved-game package subsequently reported uploaded (`IsUploaded = 1`,
`IsUploading = 0`) after relaunch. The simulator was restarted without erasing its data
and authenticated again; progress had not converged at the first post-restart check.

References: [initialization/configuration](https://developer.apple.com/documentation/gamekit/initializing-and-configuring-game-center),
[GameKit saved games](https://developer.apple.com/documentation/gamekit/saving-the-player-s-game-data-to-an-icloud-account).

Player UI (2026-10-15): enabled iOS builds expose Cloud saves beside Privacy policy in
player Settings. A newly collected squishy queues a one-time introduction featuring that
character. It waits until the reward celebration and tutorials have finished, then uses an
empty stage break or the title screen. Existing collections alone do not prompt at launch.
Not now and Set up cloud saves remember the introduction locally; already available cloud
saves suppress it. Setup retries Game Center sign-in on explicit request and explains the
iCloud Drive settings path. Reduce Motion keeps the character and panel stationary.
Interactive Game Center sign-in waits for an explicit setup action; authentication of an
already signed-in player can proceed automatically without presenting a controller.

Routine pending status stays in the Cloud saves panel. Reported failures or account mismatch
can show a nonblocking title-screen notice, dismissed for the session after viewing the panel.
The panel describes GameKit saves as pending rather than claiming verified server upload.
System-level upload failures that GameKit does not report still require upload-state monitoring;
the full-storage case above remains a release validation item.

### Repeatable cloud UI checks

Debug builds accept `DDD_SCENE=cloud:intro`, `cloud:sign-in`, `cloud:drive`,
`cloud:pending`, `cloud:failed`, `cloud:account`, or `cloud:notice`. Launch with
`DDD_GAME_CENTER_DISABLED=1` to isolate presentation from live services. These scenes
pin the displayed status across native refreshes and discard authentication actions.
Release builds ignore the scenes. They exercise the real panel, input routing and
device-local introduction dismissal; they do not prove cloud transfer. There is no
simulated “uploaded” state because upload monitoring is still separate work.

`SmokeTests.testCloudOnboardingAndStatusPanels` checks all six panels, setup navigation
and dismissal, retaining native screenshots. `testCloudFailureNoticeOpensStatus`
checks the title notice's link to the failure panel. Run these through
`ios/scripts/test-simulator.sh --selected` with `IOS_TEST_TARGETS` set to their full
`DDDumplingUITests/SmokeTests/…` identifiers. The shared Settings suite covers
celebration timing, collected-character selection, dismissal persistence, pointer
ownership and once-per-session failure notices. `ios/scripts/test-input.sh` checks
preview isolation and verifies that Release cannot activate the simulated scenes.

2026-10-17 validation: 337 Settings checks, 569 iOS input checks, 28 cloud checks in
each configuration, and six production gates passed. The six-panel native UI test
passed setup and dismissal; the separate failure-notice test passed after isolating
the developer fixture from stored tutorial state. Five native Game Center tests passed.
Portrait simulator screenshots were inspected. On the paired iPhone 16 Pro Max, the
signed Debug build installed successfully and its developer save was byte-identical
before and immediately after the update. Physical UI tests could not initialize:
iOS returned “Authentication canceled. Canceled by user.” Unlocking the phone and
authorizing UI testing is still required; installation is not a physical UI test pass.
