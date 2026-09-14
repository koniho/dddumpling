# iPad 13-inch Release captures

Captured September 14, 2026 on iPad Pro 13-inch (M5), iPadOS 26.5, using the
optimized Release simulator app. Portrait images are 2064 × 2752 and landscape is
2752 × 2064, matching Apple's [13-inch screenshot sizes](https://developer.apple.com/help/app-store-connect/reference/app-information/screenshot-specifications/).

These are full-screen captures of the title and ordinary Stage 1 gameplay. The
portrait playfield and surrounding gutters are preserved. No scene hook, test
fixture, save injection, cropping or synthetic gameplay is used. These files have
not been uploaded to App Store Connect.

To recapture:

```sh
CONFIGURATION=Release TEST_SCHEME=DDDumplingStoreCapture \
  SIMULATOR_NAME_PREFIX='iPad Pro 13-inch' ./ios/scripts/test-simulator.sh
xcrun xcresulttool export attachments \
  --path ios/build/Test-<timestamp>.xcresult --output-path /tmp/ipad-store-captures
```

The separate UI-only scheme keeps native unit-test symbols out of the optimized
app. The simulator runner uses the host architecture, matching the bootstrapped
J2ObjC runtime. Screenshot names start with `store-ipad-` in the exported
`manifest.json`; export the matching PNGs as `01-title.png`, `02-gameplay.png`, and
`03-landscape.png`. Normalize their orientation metadata without resizing or cropping:

```sh
swift ios/scripts/normalize-screenshot.swift /tmp/ipad-store-captures/<image>.png \
  ios/store/screenshots/en-US/ipad-13/<name>.png
```

Inspect each output before committing it. Captures use the installed app's existing
progress. Close system alerts before recording; the test restores a floating window
to fullscreen and returns the simulator to portrait afterward.
