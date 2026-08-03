# Hexatype

A kawaii typing-attack game for Android, built entirely on-device in Termux — no Gradle,
no Android Studio, no PC.

Cute words fall from the sky. Each "letter" is one of six characters — dumpling,
strawberry, cat, grapes, squishy, blob — with its own colour and face. You type by tapping
the matching hexagon in the honeycomb deck at the bottom: three keys under the left thumb,
three under the right. Clear a word before it reaches the danger line, or it lunges and
costs you a life.

## Requirements

Termux on an `aarch64` Android device:

```sh
pkg install openjdk-17 aapt2 d8 apksigner zip
```

Plus `android.jar` to compile against, which is not packaged. Fetch it once:

```sh
cd hexatype
mkdir -p sdk
curl -o /tmp/platform35.zip \
    https://dl.google.com/android/repository/platform-35_r02.zip
unzip -o -j /tmp/platform35.zip 'android-35/android.jar' -d sdk
rm /tmp/platform35.zip
```

That is a ~64 MB download for a 27 MB jar; it is deliberately not committed.

> Termux ships an `ecj` wrapper, but it hardcodes `-7` and a nonexistent classpath, so the
> build uses `javac --release 8` instead.

## Build

```sh
./build.sh
```

The pipeline is `aapt2 compile` → `aapt2 link` → `javac` → `d8` → `zip` → `apksigner`,
which takes a few seconds. It runs `check.sh` first and refuses to package if any rule
assertion fails. Output is a signed, installable `hexatype.apk` (~25 KB).

A debug keystore is generated at `build/debug.keystore` on first run. It is throwaway —
delete it and a new one appears.

## Install

```sh
termux-open hexatype.apk
```

This hands the APK to Android's package installer via Termux's FileProvider. Two one-time
prerequisites:

- Allow "install unknown apps" for Termux.
- Set `allow-external-apps = true` in `~/.termux/termux.properties`, then run
  `termux-reload-settings`. Without it Termux's content provider refuses to hand the file
  over and **nothing at all happens** — no dialog, no error. The reason only shows up in
  `logcat` under Termux's own UID.

## Unattended install

`./deploy.sh` builds, installs and launches in one step. With adb connected it needs no
taps at all; otherwise it falls back to the installer dialog.

Termux cannot install packages itself — that needs a privileged permission — so the only
way to skip the dialog is adb. It can talk to the very device it is running on over
Wireless debugging:

1. Settings → Developer options → enable **Wireless debugging**.
2. Tap **Pair device with pairing code**. Note the `IP:PORT` and the six-digit code.
3. `adb pair <ip>:<pair-port>` and enter the code.
4. `adb connect <ip>:<port>` — the port on the main Wireless debugging screen, which is
   *different* from the pairing port.
5. `adb devices` should list one `device`.

Pairing persists, but the port changes when Wireless debugging is toggled or the device
reboots, so step 4 may need repeating. After that `./deploy.sh` is fully unattended and
`adb install -r` preserves the best score and settings.

Notifications are optional: `deploy.sh` calls `termux-notification` if the Termux:API app
is installed, and silently skips it otherwise. Note that Termux add-ons must be installed
from the same source as Termux itself — mixing F-Droid and GitHub builds fails with a
signature mismatch.

## Verify without installing

`./check.sh` runs the game headlessly and needs neither the Android SDK nor a device:

```sh
./check.sh              # rules + frames at 640x1400
./check.sh 1080 2340 2  # a specific screen size and supersampling factor
```

It does two things:

1. **Rule assertions** (`tools/CoreTest.java`) — layout geometry, hit-testing, targeting,
   scoring, stage pacing, breach and game-over transitions, plus a 120-second perfect-play
   run and a 10-minute random-input fuzz.
2. **Frame renders** (`tools/Preview.java`) — drives the real state machine into the title
   screen, a live wave, a struck letter, a late stage, a stage-up banner, the low-health
   danger state, a mid-lunge attack, and game over, writing each to `out/*.png`.
   `out/0-characters.png` is a sheet of all six characters at display and tile size.

This works because all game logic and all drawing are pure Java behind the
[`Painter`](src/com/sram/hexatype/Painter.java) interface. The APK implements it with
`android.graphics.Canvas`; the harness implements it with a software rasterizer. **One
render path, two backends** — so a PNG from the harness is what the phone draws.

## Layout

```
src/com/sram/hexatype/
  Glyph.java          palette, hexagon geometry, colour cycling      (pure)
  Kawaii.java         the six characters and their faces             (pure)
  Layout.java         all screen geometry, derived from view size    (pure)
  GameCore.java       rules: state machine, stages, targeting, sim   (pure)
  Painter.java        drawing interface                              (pure)
  Renderer.java       the entire scene, drawn against a Painter      (pure)
  CanvasPainter.java  Painter -> android.graphics.Canvas
  GameView.java       frame loop, touch, window insets
  MainActivity.java   fullscreen setup, best-score persistence
tools/                harness only, never shipped in the APK
  CoreTest.java       rule assertions
  Preview.java        frame renders
  RasterPainter.java  Painter -> int[] framebuffer
  Font.java           5x7 bitmap font for harness text
  Png.java            minimal PNG writer
```

The `tools/` sources share the `com.sram.hexatype` package so they can reach
package-private state, but they are compiled separately and are not in `build.sh`'s
source list.

## Extending it

Stage pacing is one method per dial in `GameCore`, so tuning difficulty is a numbers
change rather than a rewrite:

| Method | Controls |
| --- | --- |
| `travelSeconds()` | how long a word takes to fall |
| `spawnInterval()` | seconds between spawns |
| `maxEnemies()` | words on screen at once |
| `maxWordLen()` / `minWordLen()` | word length range |

`KILLS_PER_STAGE` sets how quickly stages advance. To add a seventh letter, add its colour
to `Glyph.COLOR`, a case to `Kawaii.draw`, and widen the key layout in `Layout.compute` —
`Glyph.COUNT` drives the rest.
