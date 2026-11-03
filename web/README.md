# Browser port (issue #230)

Isolated prototype of DDDumpling's actual shared game. It compiles the existing Java
core/renderer with TeaVM 0.16.0 and adds browser drawing, input, audio and persistence.
No replacement game engine or copy of gameplay rules. This work stays on
`feat/web-port-230`; it does not publish a site or change native builds.

## Build and play

Requirements: JDK 21, Maven (tested with 3.9.11), Python 3 and Node.js. The first
build downloads Maven dependencies. From the repository root:

```sh
python3 web/build.py
python3 -m http.server 8766 --bind 127.0.0.1 --directory build/web
```

Open http://127.0.0.1:8766 on the same device. `--maven /path/to/mvn` selects a Maven
installation; `--skip-audio` is for iterations after the first successful full build.
`--production` excludes developer scene switching/chips; this is not a Playables
certification flag. Output and downloaded dependencies stay under ignored `build/`.
Use HTTPS when hosting outside localhost. Hosting/uploading is a separate action.

Tap/swipe as on the phone. S D F J K L are the existing physical deck bindings;
Escape uses the existing back/pause handler. Pointer capture supports dragging and
multiple fingers. Backgrounding cancels held input, pauses play and suspends audio.
The portrait playfield is centered within the available viewport; resizing preserves
state. Drawing resolution is capped at 2x CSS pixels and 1080 pixels wide. Sustained
slow frames lower raster density toward 1x, with slow recovery after sustained
headroom. This does not relayout the game, change key hit areas or cancel touches.

The browser must receive a user interaction before sound/speech can play. The
existing synthesized/recorded effects and music are exported to WAV at build time,
then fetched/decoded as needed. No personal `res/raw/bgm.*` files are included.
Browser speech voice quality depends on the installed voices. Settings gain and mute
controls feed the Web Audio buses. The retired cave band audio adapter is not yet
implemented; caves remain unreachable through normal Adventure routing.

Progress is local to the browser's origin in `dddumpling.web.v1.*` localStorage keys.
It is separate from native phone saves. Clearing site data removes it; private mode,
quota or denied storage can prevent saving and displays a notice. This is not cloud
sync. Browser-specific store review/share services are unavailable.

## Architecture

- `build.py` consumes `check.sh`'s explicit pure-source list, so shared code cannot
  silently drift. It includes `ios/java/.../IOSGame.java` and `IOSTouch.java` unchanged:
  despite their names, these are pure Java adapters with a tested gesture router.
- `WebGame` is the browser host. `WebPointers` translates pointer packets, preserving
  stable IDs and the departing pointer on up. `WebStore` implements all save fields.
- `WebPainter` implements `Painter` through Canvas 2D. `WebStamps` invokes the same
  cached-art generators as Android/iOS for slime banks, crystal layers, underwater
  scenery, Gator palms and forest foliage. Water haze uses cached alpha textures.
- `paint.js` provides Canvas primitives, texture meshes and a bounded 64 MiB LRU
  texture cache. Palette variants are quantized to bound recoloring work; spring
  geometry and game rules are unchanged. Measure texture warm-up and palette churn
  as well as steady frames. Cache accounting excludes browser-managed overhead.
- Geometry crosses TeaVM's JavaScript boundary by reference (read-only), and cached
  paths detect mutations in reusable spring-body buffers. Static arena artwork is
  generated in an OffscreenCanvas worker using the same Java game bundle. Dark
  Divide requests the next fracture's images ahead of the pinch. Transferred
  bitmaps are closed on eviction; unsupported/failed workers use synchronous art.
- `WebSound` maps native sound IDs/pitches/gains to `audio.js`; `WebAssets` renders
  native samples on the build host so synthesis never blocks a gameplay frame.
- `host.js` owns frame scheduling, browser lifecycle, coordinates and errors.
  Native Android/iOS render and packaging paths are untouched.

## Verification and profiling

```sh
node --test web/test/host.test.cjs
JAVA_HOME=/path/to/jdk21 bash ios/scripts/test-input.sh
```

The `Web port preview` workflow builds the bundle, runs host tests, and uses
Playwright Chromium **and WebKit** to check keyboard/lifecycle, persistence, resizing,
audio unlock and representative gameplay/boss scenes. It saves screenshots and
`metrics.json` as `web-preview-review`, plus the runnable bundle as
`web-playable-preview`. This workflow does not deploy anything.

For optional local CPU rendering on Termux (Skia supports Android arm64):

```sh
npm install --prefix build/web-test-deps --no-package-lock @napi-rs/canvas@0.1.80
node web/test/render.cjs
```

This executes the actual generated JavaScript and draws review frames. It is not a
browser FPS measurement. The browser checks similarly record scripted scene CPU
cost on CI, not real-device GPU presentation time.

Use `?debug=1` for the scene selector, outfit shuffle, optional haptics and timing
readout. `?scene=stage:25&debug=1` jumps to Gator in a developer build. Debug scenes
can change local progress; use a separate browser profile/origin for benchmarks.
`ddd.stats()` reports bounded p50/p95/p99 frame intervals, CPU update+draw time,
cache size, render scale, audio state and heap where supported. `ddd.test.step()` is only for
scripted diagnostics; it bypasses display scheduling. Do not report its numbers as FPS.
`profile:divide` and `profile:octopulse` replay the existing 36-second native profiling
scenes in isolated cores without writing saves. Escape exits the replay. The Divide
replay includes all fracture phases, eight pieces and the final explosion. Browser
CI runs both and tests real DOM touch delivery; `ddd.artState()` reports worker health.

Before claiming native-equivalent performance, test representative midrange Android
Chrome and iPhone Safari, then both YouTube embedded hosts when access exists. Compare
identical seeded scenes, sustained 10–15 minute sessions, input/audio latency, cold
load, cache churn and garbage collection. Stress late Survival, Cat rings/flurries,
Gator entry/parries, Slime bank recoil, Dark Divide splits and wardrobe glitter.

## YouTube Playables follow-on

Application and detailed gates: https://github.com/koniho/dddumpling/issues/230

This is a standalone browser port, **not yet a Playables SDK integration**. The
Playables host must replace localStorage with SDK save/load, use SDK pause/audio
callbacks instead of page visibility, signal firstFrameReady/gameReady correctly,
remove incompatible external links/UI and pass certification. Keep that host policy
separate from the shared gameplay and standalone browser adapter. No application,
licensing agreement or publication has been submitted by this work.

Dependencies: https://teavm.org/docs/intro/overview.html and
https://teavm.org/docs/runtime/jso.html. Font license ships with the generated bundle.
Audio provenance remains in `audio/recorded/README.md` and the music source files.
