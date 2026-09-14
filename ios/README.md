# DDDUMPLING for iPhone

Native UIKit/Core Graphics/AVFoundation host for the existing Java game. J2ObjC translates the
same gameplay and scene sources used by Android; generated Objective-C stays in `build/`.
Initial target: portrait iPhone, iOS 15+, arm64. iPad, Mac Catalyst, Game Center, iCloud and
Android save transfer are outside this initial port. The iPhone app may run in iPad compatibility
mode; there is no native iPad layout or validation.

## Prerequisites

Validated development machine: Apple silicon, macOS 26.5.2, Xcode 26.6 (17F113), iOS 26.5 SDK
and simulator runtime. No physical iPhone was connected. Use full Xcode, not just Command Line
Tools, and install an iOS simulator runtime in Xcode Settings → Components.

Tool versions used: OpenJDK 21.0.12.1, Maven 3.9.16, XcodeGen 2.46.0. Python 3 is used for the
source manifest. J2ObjC is pinned to tag **3.1**, with a SHA-256-verified source archive in
`scripts/bootstrap.sh`. Its Maven dependency versions come from that pinned upstream source.

```sh
brew install openjdk@21 maven xcodegen
# If another Xcode is selected:
sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
./ios/scripts/bootstrap.sh
./ios/scripts/build.sh
xcrun simctl list devices available
SIMULATOR_ID=<iPhone-UDID> ./ios/scripts/run.sh
```

`env.sh` discovers Homebrew Java 21 on Apple silicon or an installed JDK 21 via `java_home`.
For another installation, export `JAVA_HOME` explicitly. Build output is
`ios/build/DerivedData/Build/Products/Debug-iphonesimulator/DDDumpling.app`.

Google no longer posts prebuilt J2ObjC distributions. The first bootstrap builds the translator
and runtime for the host simulator architecture, physical arm64 iPhone, and host macOS; allow
several minutes and a few GB of disk space. Subsequent builds reuse it. The bootstrap preserves
upstream warning checks but demotes Xcode 26's new implicit integer-to-float rounding diagnostic
in the unchanged runtime. It does not patch translated sources. See Google's
[requirements](https://developers.google.com/j2objc) and
[source build instructions](https://developers.google.com/j2objc/guides/building-j2objc).

`project.yml` is authoritative; XcodeGen regenerates the ignored Xcode project after translation.
You can open `ios/DDDumpling.xcodeproj` after the first build. Its pre-build phase retranslates
for the selected configuration. Do not build Debug and Release concurrently in one checkout.
If adding a Java source, add shared code to `check.sh`'s PURE manifest or iOS-only code under
`ios/java/`, then rerun `build.sh` to refresh Xcode's file list.

## Validation

GitHub Actions and optional signed/TestFlight delivery are documented in [CI setup](docs/ci.md).

```sh
source ios/scripts/env.sh
./check.sh -q -r
./check.sh -q --production
./ios/scripts/test-input.sh
SIMULATOR_ID=<iPhone-UDID> ./ios/scripts/test-simulator.sh
./ios/scripts/render-reference.sh
# After playing an installed build, check replacement without uninstalling:
SIMULATOR_ID=<iPhone-UDID> ./ios/scripts/test-update.sh
```

The input suite uses its own output directory and can run alongside the existing Java checks.
Native tests exercise durable storage and launch/touch/background scenarios.
The update check compares the existing save byte-for-byte before and after installing the built
app and then launches it. It keeps snapshots under `ios/build/Update-*`; it does not test future
save-schema migrations or replace a human progress-restoration check.
XCTest screenshots and reports are retained in timestamped `ios/build/Test-*.xcresult` bundles,
viewable in Xcode.

For matching renderer captures, launch a Debug build with `SIMCTL_CHILD_DDD_RENDER_CHECK=1`
using `xcrun simctl launch`. Nine fresh games use seed 42, a 640×1400 point layout, and exactly
30 updates at 1/60 second. Native images go into the app's Documents/render-check directory;
the Java equivalent goes into `out/ios-reference`. The baseline raster font differs from the
bundled Bungee font, and Core Graphics antialiasing differs from the Java rasterizer.

Debug-only launch environment `DDD_SCENE` accepts `title`, `play`, `case`, `stars`, `steamer`,
`fling`, `pause`, and `stage:5`, `stage:10`, `stage:15`, `stage:20`. Release ignores these hooks
and compiles shared `BuildFlags.DEVELOPER=false`. Debug and Release use separate bundle IDs
and saves (`com.dddumpling.game.ios.dev` and `com.dddumpling.game.ios`).

Set `DDD_PROFILE=1` in the Xcode scheme's launch environment to log bounded 600-draw windows:
mean/p95/max update and draw-submission CPU time, display-link callback intervals, missed 60 Hz callback
slots, and resident memory. It is quiet by default and resets on lifecycle/navigation changes.
For an installed Debug simulator app, capture a scene with:

```sh
SIMCTL_CHILD_DDD_PROFILE=1 SIMCTL_CHILD_DDD_SCENE=stage:10 \
  xcrun simctl launch --terminate-running-process \
  --stderr="$PWD/ios/build/profile-divide.log" <iPhone-UDID> com.dddumpling.game.ios.dev
# Play in Simulator, then inspect ios/build/profile-divide.log.
```

The game layer uses asynchronous Core Graphics rasterization after profiling identified
deferred path filling on the main thread. Set `DDD_SYNC_RASTER=1` in a Debug launch to compare
the old synchronous path. Shared scene drawing and gameplay still execute on the main thread;
Core Animation processes the recorded drawing commands asynchronously. See the measured
[before/after results](docs/performance.md).

UIKit may defer Core Graphics rasterization until after `drawRect:` returns, so draw-submission
timing excludes that work. Callback cadence is **not** presented FPS or physical-device
frame-pacing evidence. Scene hooks are Debug-only; profiling also works in Release. Use Instruments
Time Profiler/Core Animation on a signed physical iPhone for sustained play, missed refreshes,
memory growth and touch latency. Physical-device validation and acoustic listening remain
required before shipping. See [parity inventory](docs/parity.md), [rendering](docs/rendering.md),
and [audio/persistence](docs/audio-persistence.md). Actual results and remaining acceptance work
are recorded in [validation.md](docs/validation.md).

The UIKit host reserves a 44-point navigation strip below the top safe area so its pause
button does not cover the shared score/lives HUD. The Java renderer-reference images use zero
insets to compare drawing independently of that native navigation space.

## Resources and release

The target bundles Bungee and its SIL OFL notice. Artwork is rendered from shared code; the
1024×1024 AppIcon is regenerated with `./ios/scripts/resources.sh` using the existing launcher
composition at native resolution. Quicksand is trailer-only and omitted. Personal `res/raw/bgm.*`
tracks are not copied into the iOS release. Procedural music and effects need no external assets.
J2ObjC's runtime is Apache 2.0 and includes third-party notices. Translation collects the pinned
runtime's LICENSE/NOTICE files into the bundled `ThirdPartyNotices.txt`; review attribution
against the final archive before release.

The app uses offline local storage, with no Play SDK, account sign-in, ads, tracking or analytics.
The privacy link opens the existing policy in the system browser. `PrivacyInfo.xcprivacy` declares
local timing and sandbox file metadata uses. Before an App Store upload, review the final archive's
privacy report against [Apple's required-reason API documentation](https://developer.apple.com/documentation/bundleresources/describing-use-of-required-reason-api).

For a device build, choose your team in Xcode's Signing & Capabilities, or:

```sh
CONFIGURATION=Release DEVELOPMENT_TEAM=<team-id> ./ios/scripts/build.sh iphoneos
```

For an unsigned Release archive without signing access:

```sh
./ios/scripts/archive.sh --unsigned
```

To create a signed archive, use Product → Archive in Xcode, or the script below.
Keep signing material in Apple's normal local tooling, never in this repo.

```sh
DEVELOPMENT_TEAM=<team-id> ./ios/scripts/archive.sh
```

Open the archive in Organizer for local validation and export. No script uploads, publishes,
changes Apple accounts or provisions paid services. Store listing preparation is in
[app-store.md](docs/app-store.md). Final signing, privacy report, age rating, screenshots and
physical-device acceptance must be reviewed before distribution.

## Troubleshooting

The host initializes J2ObjC reflection/string metadata on the main thread before creating the
audio queue. This avoids a reproduced J2ObjC 3.1 `+initialize` lock inversion: the main thread
waited for `IOSClass` while the synthesis thread waited for the string category. A sampled
simulator hang confirmed both stacks. Keep this bootstrap ordering if changing app startup.

- Missing Java: set `JAVA_HOME` to a JDK 21, not the macOS `/usr/bin/java` stub.
- Simulator service errors in a sandbox: grant the local build/test command access to CoreSimulator.
- Missing runtime library: finish `bootstrap.sh`; simulator and device libraries are separate.
- Wrong architecture: the bootstrap builds the current Mac's simulator architecture. Rebootstrap
  on the target Mac; do not copy an arm64-only runtime to an Intel Mac.
- Missing generated header after adding files: rerun `build.sh`, which regenerates the project.
- Damaged/incompatible save: the app reports the problem and preserves original bytes rather than
  silently replacing progress. Back up its container before attempting recovery.

For Android verification on this Mac, the original build needs current Bash (Apple's bundled
Bash 3.2 fails on an empty array with `set -u`):

```sh
brew install bash
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
PATH="$JAVA_HOME/bin:/opt/homebrew/bin:$ANDROID_HOME/build-tools/36.0.0:$PATH" \
  /opt/homebrew/bin/bash ./build.sh --production
```

Install `platforms;android-36` and `build-tools;36.0.0` using the existing SDK's `sdkmanager`
if they are missing. This is an Android toolchain requirement, not a gameplay regression.

## TestFlight version policy

Keep `MARKETING_VERSION` fixed for TestFlight iterations (currently `0.1.17`). Only bump it when preparing a public App Store release, not for Android tags, feature builds, or beta fixes. The release workflow already supplies a unique `IOS_BUILD_NUMBER` from its run number and attempt; this updates `CURRENT_PROJECT_VERSION` without changing the marketing version. Update `ios/store/en-US/what_to_test.txt` for each beta.

Every uploaded build still undergoes Apple processing. Later builds of the same version may avoid a full TestFlight review, but immediate availability to external testers is not guaranteed. See https://developer.apple.com/help/app-store-connect/test-a-beta-version/invite-external-testers .
