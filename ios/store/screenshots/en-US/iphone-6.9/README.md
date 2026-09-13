# iPhone 6.9-inch Release captures

These PNGs were captured from the Release simulator build on iPhone 17 Pro Max
(`81A826FB-818E-47B4-9A4C-246902E00563`), at 1320 x 2868 pixels. They use the
normal app UI and do not use `DDD_SCENE` or other debug scene hooks.

To recapture:

```bash
CONFIGURATION=Release bash ios/scripts/build.sh
xcrun simctl bootstatus 81A826FB-818E-47B4-9A4C-246902E00563 -b
xcrun simctl install 81A826FB-818E-47B4-9A4C-246902E00563 \
  ios/build/DerivedData/Build/Products/Release-iphonesimulator/DDDumpling.app
xcrun simctl launch --terminate-running-process \
  81A826FB-818E-47B4-9A4C-246902E00563 com.dddumpling.game.ios
```

Wait for transient simulator notifications to disappear before each capture. Capture the title,
tap one of the lower character keys to start Stage 1 for gameplay, and from the title tap the
moving Display Case badge to open the collection. Save each frame with:

```bash
xcrun simctl io 81A826FB-818E-47B4-9A4C-246902E00563 screenshot <output>.png
sips -g pixelWidth -g pixelHeight <output>.png
```
