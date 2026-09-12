# iOS port validation — 2026-09-12

Implementation branch: `ios/shared-java-port`, referencing issue #9. The port runs the existing
Java game through J2ObjC with native drawing, audio, storage and input. No Android gameplay or
renderer source was changed. This is a working simulator port and unsigned device archive;
physical-device acceptance and App Store distribution remain unfinished.

## Completed checks

| Check | Command/evidence | Result |
| --- | --- | --- |
| Unmodified Java baseline | `./check.sh -q -r` with Java 21 | 4,845 passed, 0 failed |
| Production gating | `./check.sh -q --production` | 80 passed, 0 failed |
| iOS gesture/lifecycle adapter | `./ios/scripts/test-input.sh` | 56 passed, 0 failed |
| Native audio | iPhone 17e / iOS 26.5 XCTest | 6 tests passed: PCM pitch, effect pool, transient/speech pause, route gate, zero boss charge, actual running Float32 varispeed rocket engine |
| Native storage | Same XCTest run | 4 tests passed: all fields and replica reopen, corrupt/empty/incompatible/checksum-invalid preservation, failed write reporting |
| UI smoke suite | `SIMULATOR_ID=171B67DE-A512-4095-BB6D-66B001F98EF5 ./ios/scripts/test-simulator.sh` | 3 tests passed: title→play/background pause; case/four bosses/stars/steamer launches; native pinch/swipe smoke test |
| Final native pause button | iPhone 17 Pro / iOS 26.5, `-only-testing:DDDumplingUITests/SmokeTests/testTitleStartsGameAndBackgroundPauses` | Passed explicit pause, resume and background-return checks after navigation spacing correction |
| Renderer parity | `./ios/scripts/render-reference.sh` plus Debug `DDD_RENDER_CHECK=1` | Nine matching-seed native/Java scene pairs reviewed; expected Bungee and antialiasing differences |
| Simulator build | `./ios/scripts/build.sh` | Passed arm64 simulator compile/link/install/launch |
| Device Release archive | `./ios/scripts/archive.sh --unsigned` | Passed; arm64 iPhone archive, production bundle ID, developer flag false, fonts/icon/privacy/license resources present |
| Install over existing app | `SIMULATOR_ID=57F475C1-B8F8-45F5-8990-5D9BC5687A50 ./ios/scripts/test-update.sh` | Passed; existing save byte-identical after reinstall, app launch accepted; snapshots in `ios/build/Update-20260912-134758` |
| Android production build | Homebrew Bash 5.3.15, Android API/build tools 36, `bash ./build.sh --production` | 4,845 + 80 assertions passed again; signed APK verified as `com.dddumpling.game` / DDDUMPLING |

Final full native/UI result bundle after asynchronous rasterization:
`ios/build/Test-20260912-135634.xcresult` (13 tests, zero failures, iPhone 17 Pro).
The earlier iPhone 17e suite is `ios/build/Test-20260912-133549.xcresult` (13 tests, zero
failures). The separate pause/button result is `ios/build/Test-20260912-133923.xcresult`. An earlier
iPhone 17 Pro run also passed the initial 2 storage + 3 UI tests. Result bundles and generated
translations remain ignored build artifacts. The nine-pair [comparison sheet](render-comparison.png)
is committed for review; detailed observations are in [rendering.md](rendering.md).

The XCTest pinch test demonstrates native gesture delivery without a crash; successful charged
Dark Divide pinching and concurrent deck/drag ownership are asserted in the Java input suite.
These are not claims that a human completed every boss through UIKit.

The unsigned arm64 Release archive was rebuilt successfully after enabling asynchronous
rasterization and adding opt-in profiling (`/tmp/dddumpling-async-archive.log`).

## Failures found and corrected

- Xcode 26 introduced an upstream J2ObjC Hashtable float-rounding warning under `-Werror`.
  The reproducible bootstrap demotes only that diagnostic while preserving upstream warnings.
- J2ObjC generates the contour selector `fillContoursWithFloatArray2:withInt:`; the painter was
  corrected against the actual header before native visual testing.
- Native activation setter recursion, pause overlap with lives, and touch/history/lifecycle
  edge cases were fixed during integration review.
- Audio review corrected zero-charge loop shutdown, blocked-route playback, effect-pool
  retention/playhead reuse, and the rocket engine's required Float32 format. The original audio
  test double also needed a valid WAV initializer; the corrected test actually runs in Simulator.
- Repeated scene launches exposed a startup deadlock, not just a test timeout. A process sample
  showed main-thread string-category initialization waiting on `IOSClass` while the background
  audio thread held `IOSClass` and waited on the string category. `main.m` initializes both on one
  thread before any audio queue exists. The full scene suite then passed.
- Apple's bundled Bash 3.2 fails the existing Android build's empty-array expansion under
  `set -u`; installing Bash 5.3.15 resolved it without Android script changes.

## Performance evidence and limits

Follow-up profiling now measures callback cadence and resident memory as well as
draw submission. It found substantial deferred Core Graphics raster work and callback
intervals above the 16.67 ms target in Simulator. Moving rasterization off the main thread
improved title callback means from 24–25 ms to 16.67 ms and early Dark Divide to about 17 ms.
This is now the default host behavior. See [performance.md](performance.md)
for hardware, per-scene results, stack-sample evidence and measurement limits.

Simulator title drawing logged 0.34 ms draw / 0.01 ms update and a 1.04 ms CPU maximum in one
600-frame sample. An early Dark Divide launch logged an 87.67 ms maximum, with subsequent
600-frame windows at 2.08–7.55 ms maximum. Those windows include idle/death states and competing
simulator/test work, and exclude compositor time. They establish that native rendering runs,
not stable 60 fps or a physical-iPhone feasibility pass. The display link requests 60 Hz.

No sustained memory-growth profile, input-to-display latency measurement, or physical-device
thermal/frame-pacing run has been completed. Use Instruments on the chosen minimum supported
iPhone and record device/OS, normal stage, stretched Slime, split Dark Divide, and star-flight
sessions before deciding the performance gate is met. The current Core Graphics backend has
an identified simulator rasterization bottleneck; the physical-device feasibility gate
remains open before selecting a different Painter backend.

## Remaining work before shipping

- Physical iPhone: visual review, real two-thumb play, all boss victories, complete minigames,
  sustained frame pacing/memory, touch latency and haptic feel.
- Human audio listening: all music transitions/effects/narration, real calls/headphones and
  route restoration. Native tests check mechanics, not acoustic equivalence or voice quality.
- End-to-end collection/story/progression traversal, save preservation across an app update,
  and final Release interaction checks on the minimum supported OS. Simulator installation
  preservation passes for the current schema; future migrations and human progress restoration
  across a version update remain unverified.
- Signing: user has enrolled in Apple Developer, but `security find-identity -v -p codesigning`
  reports **0 valid identities** on this Mac, and `xcrun devicectl list devices` reports none.
  Add the Apple ID/team and a local signing certificate in Xcode; no account changes were made.
- Signed archive validation, final privacy/runtime review, store metadata/screenshots and
  accessibility review. No App Store Connect or TestFlight upload was performed.
- Hosted CI now passes shared Java (4,864 assertions on the updated main baseline),
  production (80), input (56), all 13 native/UI tests, and the unsigned Release archive.
  See [CI results and signing setup](ci.md). Signed CI export/upload remain unverified.

Game Center/iCloud, Android save transfer and native iPad layout remain explicitly outside the
initial release scope. The full feature inventory is [parity.md](parity.md).
