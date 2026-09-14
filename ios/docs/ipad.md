# Native iPad support

iPad uses a 390 × 800 logical portrait canvas, uniformly fitted and centered inside the
window's safe area after reserving a 44-point native navigation strip. The pause button
retains its native 44 × 44 target above the score/lives HUD. Wide windows have side gutters;
narrow windows scale the same canvas without moving live Java gameplay objects.
iPhone keeps its existing point coordinates, safe-area insets and portrait orientation.

`DDViewport` supplies the rendering transform and its inverse for every current and coalesced
touch position. Retained history positions are stored in logical coordinates. Touches must
begin inside the playfield, but an accepted drag may leave it, preserving boss edge gestures.
A geometry or safe-area change sends `ACTION_CANCEL` using the old viewport, then clears
native pointer ownership before installing the new viewport. Fingers already down cannot
resume a cancelled gesture through later move/up callbacks.

The app uses `UIWindowScene` lifecycle and window sizing. iPad declares all four interface
orientations separately from iPhone. Multiple game windows are disabled because they would
share one progress file. There is no `UIRequiresFullScreen` portrait lock. This follows
[Apple's guidance on resizable iPad apps](https://developer.apple.com/documentation/technotes/tn3192-migrating-your-app-from-the-deprecated-uirequiresfullscreen-key).
The release workflow now intentionally verifies `UIDeviceFamily == [1, 2]` in the exported IPA.

## Validation

### Completion on current main

Rebased onto `fb243e9` on September 14, 2026, bringing in the persistent audio mixer,
updated Star Path, linked pairs, mystery pickups and release book without modifying
shared Java sources. Xcode 26.6 / iOS 26.5 validation:

- Shared harness: 5,328 assertions; production checks: 82; iOS input: 92, all passed.
- Pro and mini: native adapters, both landscape directions, upside-down rotation,
  scaled controls, pause/backgrounding, collection, bosses and minigames passed.
  Both devices completed the actual window-handle resize and fullscreen restoration.
- The release book opens through mapped touches, survives a resize to 320 × 700,
  opens a feature page and returns to the list through native Back.
- iPhone 17e: all 21 native tests and all three applicable UI tests passed; iPad-only
  tests and the Release-only screenshot capture were skipped by the Debug UI run.
- The native packaging check reads the built `Info.plist`, verifying `[1, 2]`,
  portrait-only iPhone, all four iPad orientations and the resizable scene configuration.
  It reads raw bytes because `NSBundle.infoDictionary` resolves device-qualified keys.
- CI now runs native and UI tests on both iPhone and iPad. Choose a local device with
  `SIMULATOR_ID` or `SIMULATOR_NAME_PREFIX='iPad mini'`.
- Release capture passed on 13-inch Pro using the ordinary title and Stage 1, with
  Debug scene hooks absent. [Store PNGs and recapture commands](../store/screenshots/en-US/ipad-13/README.md)
  include portrait and landscape, with rotation metadata normalized into full-resolution RGB pixels.
- The installed Release save remained byte-identical after reinstall and the app relaunched.
  This checks installation preservation, not complete progress restoration or schema migration.
- `./ios/scripts/archive.sh --unsigned` passed. Both the simulator Release app and
  archived arm64 app declare `[1, 2]`, portrait iPhone and all four iPad orientations.

Local result bundles under `ios/build/`: `Test-20260914-141033.xcresult` (Pro),
`Test-20260914-141407.xcresult` (mini), `Test-20260914-141707.xcresult` (iPhone UI),
`Test-20260914-141905.xcresult` (iPhone native rerun), and
`Test-20260914-142136.xcresult` (Release captures).
The initial iPhone packaging test read filtered `NSBundle` metadata and failed; its raw-plist
replacement passed all 21 native tests. Save snapshots: `Update-20260914-142349`.

Hosted CI dropped the pitch/gain test's effect under the existing 100 ms stale-impact
cutoff, even after prewarming. Dispatch tests now control the effect clock; production
still uses `CACurrentMediaTime()` and the same cutoff. A separate regression advances
the clock before execution and during mixer preparation, checking both stale drops and
that a fresh effect still plays afterward.

### Original implementation evidence

Validated on Xcode 26.6 / iOS 26.5 simulators, September 12, 2026:

- Shared Java harness: 4,864 assertions passed; iOS packet/gesture suite: 56 passed.
- Native viewport tests cover mini/13-inch Pro portrait and landscape, 320 × 700 narrow
  windows, and 600 × 350 short windows with asymmetric safe areas.
- Native input-boundary test exercises simultaneous pointers, current/coalesced/retained
  samples, pointer-up identity, cancellation before resizing, ignored stale callbacks,
  fresh input after resizing and gutter rejection. Existing native save/audio tests pass.
- A native test opens a collected character's story through viewport-mapped touches,
  resizes the view while the story is open, and dismisses it through the new viewport.
  Its collection exists only in memory inside the test. Scene-disconnect coverage verifies
  that invalidating the display link releases the discarded view.
- UI tests cover title-to-play input, both landscape directions, upside-down orientation,
  pause/resume and backgrounding, collection, four bosses, stars, steamer and fling.
- The iPadOS 26 native resize handle was exercised on iPad mini and 13-inch Pro: fullscreen to floating
  window, centered aspect ratio, title key input and the native pause target in the smaller
  window, then restoration by double-tapping the title bar. `WindowTests` requires
  **Settings → Multitasking & Gestures → Windowed Apps** (the simulator default).
- iPhone 17e regression: native tests and all three applicable UI tests passed; iPad-only
  UI tests skipped as intended.

Representative validation captures: [mini portrait](ipad-evidence/mini-portrait.png),
[mini landscape](ipad-evidence/mini-landscape.png), and
[mini floating window](ipad-evidence/mini-window.png); [Pro portrait](ipad-evidence/pro-portrait.png),
[Pro landscape](ipad-evidence/pro-landscape.png), [Pro floating window](ipad-evidence/pro-window.png),
and [story test fixture](ipad-evidence/story-fixture.png).

The final native suite passed all 15 tests on Pro. One preceding mini run hit an allocator
fault in the unchanged `DDAudioTests.testRocketUsesAStartedFloatVarispeedEngine`; the story
and viewport tests passed after Xcode restarted the host. A complete native rerun on Pro
passed, including the audio test. The earlier mini/Pro/iPhone runs also passed that audio
test. This intermittent simulator audio failure remains recorded rather than treated as
evidence of physical-device audio stability.

These older Debug captures remain test evidence. The separate September 14 Release captures
above are the store assets; none have been uploaded to App Store Connect.

## Physical validation still required

Real two-thumb play, all boss victories and minigame completion, audio/haptic feel, external
audio interruptions, sustained frame pacing/thermal/memory behavior, and save restoration
across an installed app update remain physical-device checks. The original iPhone release
has the same outstanding physical validation. This branch does not upload a new build.
