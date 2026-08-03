# Working on Hexatype

Read this before touching anything. It exists to save you the discoveries that cost time
the first time round.

## The one thing that matters most

**You can see and hear this game without building or installing it.** `./check.sh` runs the
whole thing headlessly: ~390 rule assertions, then it renders real frames to `out/*.png` and
every sound to `out/sfx/*.wav`. Read the PNGs with the Read tool. That loop is seconds, not
minutes, and it needs no device.

This works because *all* logic and *all* drawing are pure Java behind the
[`Painter`](src/com/sram/hexatype/Painter.java) interface. The APK implements it with
`android.graphics.Canvas`; `tools/RasterPainter` implements it with a software rasterizer.
One render path, two backends — a PNG from the harness is what the phone draws.

**Never break that seam.** If you put an `android.*` import in a pure file, the harness stops
compiling and you lose your eyes. The pure set is listed in `check.sh` as `PURE`; add new pure
files there. `build.sh` globs `src/`, so it needs no updating.

### Rendering is deterministic — exploit it

Preview output is a pure function of state (fixed RNG seeds, no wall clock). So for any change
that *should not* alter rendering — a refactor, a rename — prove it:

```sh
./check.sh && md5sum out/*.png out/sfx/*.wav > /tmp/before.txt
# ...make the change...
./check.sh && md5sum out/*.png out/sfx/*.wav > /tmp/after.txt
diff /tmp/before.txt /tmp/after.txt   # must be empty
```

That caught more than the assertions did during the big refactor. Note the corollary: any
intentional visual or audio change *will* alter those hashes, which is fine — just know which
you are doing.

## Build and deploy

```sh
./check.sh              # rules + frames, no SDK needed
./check.sh 1080 2400 2  # render at real device size (slower, use when checking layout)
./build.sh              # gated on check.sh; produces a signed hexatype.apk
./deploy.sh             # build + install + launch
```

`build.sh` refuses to package if any assertion fails. `sdk/android.jar` is not committed; see
README.md for the one-time fetch.

Installing needs a tap unless adb is paired over Wireless debugging (README.md). A plain
`termux-open` silently does nothing unless `allow-external-apps = true` is set in
`~/.termux/termux.properties` — no dialog, no error, and the reason only appears in logcat
under Termux's own UID.

**You cannot see this app's crashes.** Termux's logcat only shows its own UID, and there is no
adb or dumpsys here. That is why [`Crash.java`](src/com/sram/hexatype/Crash.java) renders the
stack trace on screen instead. If the user reports a crash, ask them to read that screen.

## Layout of the code

Pure (in the harness and the APK):

| File | Holds |
| --- | --- |
| `Glyph` | the six-letter palette, hexagon geometry, hue cycling |
| `Kawaii` | the six characters and their faces, plus the mood dumpling |
| `Layout` | every screen coordinate, derived from view size + insets |
| `GameCore` | all rules: state machine, waves, targeting, scoring, powerup |
| `Words` | word generation and the press-budget rules |
| `Fx` | shots and particles |
| `Steamer` | between-stages minigame state |
| `Power` | the powerup letter and its three modes |
| `Painter` | the drawing interface |
| `Draw` | palette + shared geometry (pill, star, hash, rainbow) — renderers extend it |
| `Sky` | background, clouds, vignette, HUD band |
| `Renderer` | frame orchestration + the play field |
| `Hud` | score/stage/lives, frenzy bar, banners |
| `Screens` | title, game over, minigame, settings |
| `Sfx` / `Music` | procedurally synthesised effects and looping tracks |
| `SettingsUi` | settings panel geometry and hit-testing |

Android-only: `MainActivity`, `GameView` (input + frame loop), `CanvasPainter`, `Audio`,
`Crash`.

Harness-only in `tools/`: `Check` (base class with the tally, assertion helpers, drivers and
the Store/Sound stubs), `Test*` suites, `Preview`, `RasterPainter`, `Font`, `Png`, `Wav`.

The `Test*` classes extend `Check` and the renderers extend `Draw` **so that helpers and
colours resolve unqualified**. That is deliberate: prefixing several hundred call sites buys
nothing. Follow the pattern rather than "fixing" it.

## Conventions worth keeping

- Every rule change gets an assertion in the matching `Test*` suite, and every new visual
  state gets a frame in `Preview` so it can be looked at.
- Comments explain *why*, especially where a value was tuned against a failure. Several
  constants exist at their value because the obvious value was wrong; the comment says so.
- `GameCore` must stay free of `android.*`. Audio and persistence reach it through the
  `Sound` and `Store` interfaces, which is also how tests assert which effect fires when.
- Files over ~350 lines want splitting. `GameCore` is the current outlier at ~1050.

## Traps that have already bitten

- **Insets lie.** In immersive mode `padT` is 0 and this device reports no cutout inset, so
  a centred HUD lands under the punch-hole camera. `Layout.topSafe` keeps its own floor.
  There is a regression assertion at 1080x2400.
- **RNG call order is load-bearing.** `Words.fill` consumes the RNG in a fixed order; change
  it and every generated word shifts. The hash comparison catches this.
- **Reset state above early returns in `update()`.** `warnLevel` was reset *after* the
  `state != PLAY` return, so a fatal breach left the red edge glow stuck on forever.
- **Removing from a list you are iterating.** A fatal breach clears the whole enemy list, so
  unlist before calling `breach()`.
- **Position-driven animation, not time-driven,** for anything tied to where a thing is. A
  timed entrance ramp finished while the word was still off-screen.
- **Translucent overlapping primitives double-blend.** Build a stadium as one polygon, not a
  rect plus two circles, or the caps come out darker.
- **Tune against the real firing rate.** The per-press sky glow looked right in one frame and
  strobed in play; the 4s minigame let a masher finish it in one go, defeating the
  accumulation it was built for.
- Termux's `ecj` hardcodes `-7`; use `javac --release 8`. `aapt2 link` takes compiled
  resources positionally, not via `-R`.

## Licensing line

Characters, effects and music are all original and procedurally generated — no assets to
license. `res/raw/bgm.*` is gitignored: a user-supplied track stays on their device and must
never be committed, since this repo is shared.
