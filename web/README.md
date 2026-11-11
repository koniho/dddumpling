# Browser port (issue #230)

**Resuming this work? Read [the handoff](HANDOFF.md), especially the invitation-only
portal requirement before promising YouTube app testing.**

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
multiple fingers. Desktop hosts teach dragging in the boss tutorial and keyboard guide.
On a mouse, dragging a charged Dark Divide glob adds a mirrored
virtual finger and feeds the existing two-pointer pinch handler. Short stretches
do not split; releasing/canceling clears both fingers. Other bosses and touch input
keep their existing gestures. Backgrounding cancels held input, pauses play and suspends audio.
On a desktop browser's first visit, a keyboard practice card shows the actual squishy
artwork and bindings supplied by `KeyboardInput`. Inactive roster keys are dimmed and
explained. Practice cannot trigger gameplay. Completion is remembered in browser
storage; `?` reopens the guide, and the preview tools have a replay button. Touch-only
browsers do not show it automatically. Opening it during a run pauses that run and
preserves an existing pause when closing.
Browser Back follows the game's existing back handler: dismiss a panel or pause/resume
a run before leaving the page. A single reusable history guard prevents accidental
navigation during play; from the idle Adventure title, Back can leave normally.
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
quota or denied storage can prevent saving and displays a notice. This standalone mode is not cloud
sync; the separate YouTube build below uses SDK-managed saves. Browser-specific store review/share services are unavailable.

## Architecture

- `build.py` consumes `check.sh`'s explicit pure-source list, so shared code cannot
  silently drift. It includes `ios/java/.../IOSGame.java` and `IOSTouch.java` unchanged:
  despite their names, these are pure Java adapters with a tested gesture router.
- `WebGame` is the browser host. `WebPointers` translates pointer packets, preserving
  stable IDs and the departing pointer on up. `WebStore` implements all save fields.
- `WebPainter` implements `Painter` through Canvas 2D. `WebStamps` invokes the same
  cached-art generators as Android/iOS for slime banks, crystal layers, underwater
  scenery, Gator palms, forest foliage, and the native swamp tiles/reed clumps.
  Swamp tiles use `SwampLayers` bounds and the same 512-pixel viewport cap; clumps
  use `SwampGrass` dimensions, anchors and live shear. Workers generate these from
  shared artwork, and the bounded texture cache also handles viewport changes.
  Water haze uses cached alpha textures. Underwater depth tint uses reusable Canvas
  compositing surfaces, preserving edge alpha without asynchronous palette changes.
- `paint.js` provides Canvas primitives, texture meshes and a bounded 64 MiB LRU
  texture cache. Palette variants are quantized to bound recoloring work; spring
  geometry and game rules are unchanged. Measure texture warm-up and palette churn
  as well as steady frames. Cache accounting excludes browser-managed overhead.
- Geometry crosses TeaVM's JavaScript boundary by reference (read-only), and cached
  paths detect mutations in reusable spring-body buffers. Static arena artwork is
  generated in an OffscreenCanvas worker using the same Java game bundle. Dark
  Divide requests the next fracture's images ahead of the pinch. Transferred
  bitmaps are closed on eviction; unsupported/failed workers use synchronous art.
  Palette recoloring also runs in that worker; base artwork remains visible while
  a tint is pending. Main and worker texture caches each have a 64 MiB bound, and
  palette requests stop queuing at 32 pending jobs. Browser/native allocations and
  temporary bitmap transfers are additional to those cache figures.
- `WebSound` maps native sound IDs/pitches/gains to `audio.js`; `WebAssets` renders
  native samples on the build host so synthesis never blocks a gameplay frame.
- Octopulse uses the shared continuous, rounded tentacle coats on every platform:
  32 skin fills replace 768 overlapping capsule strokes, with the same physics,
  suckers and cues. Browser haze uses one affine image per segment instead of two
  clipped triangles; its mesh adapter is otherwise unchanged.
- Octopulse's cave alone is composited into a reusable 256-pixel-wide background
  surface (the width of its source artwork), then drawn once behind full-resolution
  actors. Wall drift, kelp and impact lighting update every frame. The buffer adds
  about 0.55 MiB on a phone-sized portrait viewport, outside the texture LRU. Painter
  wrappers preserve skin/monochrome effects, and a finally block restores the main
  target. Android and iOS retain their existing direct passes; their host adapters
  do not opt into this browser raster/compositing optimization.
- `host.js` owns frame scheduling, browser lifecycle, coordinates and errors.
  Native Android/iOS host and packaging paths are untouched.
- CI also runs for changes to shared Java, the reused gesture router, the pure-source
  manifest and build flags, so a native change cannot silently break browser compilation.

## Verification and profiling

```sh
node --test web/test/host.test.cjs
JAVA_HOME=/path/to/jdk21 bash ios/scripts/test-input.sh
```

The `Web port preview` workflow builds the bundle, runs host tests, and uses
Playwright Chromium **and WebKit** to check keyboard/lifecycle, persistence, resizing,
audio unlock and representative gameplay/boss scenes. It saves screenshots and
`metrics.json` as `web-preview-review-<engine>`, plus the runnable bundle as
`web-playable-preview`. This workflow does not deploy anything.

For optional local CPU rendering on Termux (Skia supports Android arm64):

```sh
npm install --prefix build/web-test-deps --no-package-lock @napi-rs/canvas@0.1.80
node web/test/render.cjs
node web/test/scenery.cjs
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
replay uses the real pinch handler, including particles, shake and edge lighting,
through all fracture phases, eight pieces and the final explosion. Browser
CI runs both and tests real DOM touch delivery; `ddd.artState()` reports worker health.

`node web/test/octopulse.mjs` runs a cave isolation comparison through the normal
RAF loop at fixed 1× and 2× density. It uses the same Playwright environment variables
as `browser.mjs`. The scene suffixes `-no-arms`, `-no-cave`, `-no-haze`, `-no-lighting`
and `-no-kelp` suppress drawing only on the isolated replay core. `-direct` bypasses
cave compositing for an A/B comparison. Masks survive replay resets and never enter
saves or ordinary gameplay. `ddd.test.renderScale(n)` fixes diagnostic density;
passing `null` restores adaptive density. Compare matched density and both starting
and ending baselines, rather than CPU submission times alone.

`node web/test/divide.mjs` samples normal RAF at fixed 1× and 2× through successive
splits and the eight-glob phase. It separates the first 0.8 seconds after each
fracture from settled frames and keeps the per-frame trace for stall analysis.
The shared crystal renderer avoids boxed-coordinate clipping and prepares light
strengths/positions once per draw; Android and iOS use those same optimizations.

Before claiming native-equivalent performance, test representative midrange Android
Chrome and iPhone Safari, then both YouTube embedded hosts when access exists. Compare
identical seeded scenes, sustained 10–15 minute sessions, input/audio latency, cold
load, cache churn and garbage collection. Stress late Survival, Cat rings/flurries,
Gator entry/parries, Slime bank recoil, Dark Divide splits and wardrobe glitter.

## YouTube Playables follow-on

Application and detailed gates: https://github.com/koniho/dddumpling/issues/230

Build the separate YouTube host with:

```sh
python3 web/build.py --playables
```

Output is `build/web-playables/`; developer Java flags and preview controls are disabled.
The official `https://www.youtube.com/game_api/v1` script loads before game code.
The standalone entry point does not load or depend on the SDK. A YouTube build opened
outside the Playables environment stops with an explanation rather than silently
falling back to localStorage. Use the official Test Suite or portal preview to run it.

`playables.js` adapts the same synchronous Store to an in-memory string dictionary,
loaded with `ytgame.game.loadData()` before constructing the game. Its saved envelope
is `{version:1, values:{...}}`; unknown versions/malformed data stop initialization
without overwriting the cloud save. New keys can be added compatibly; breaking format
changes require a migration before incrementing the version. No native/browser-local
save is imported automatically. Developer playthrough logs are excluded.

Writes coalesce briefly, send serialized snapshots one at a time and retain newer
changes during an in-flight save. Failures keep the snapshot dirty and retry with
bounded backoff while active. Pause flushes pending changes, including a newer snapshot
behind an in-flight save. Accepted synchronous writes mean queued locally in memory,
not durably synced; failures are surfaced without pretending the SDK saved them.
Writes stay queued until the game reaches readiness, so a failed initialization cannot
save partially constructed defaults. Saves are conservatively checked against the SDK's 3 MiB limit.

The HTML loading screen signals `firstFrameReady`; the interactive game/keyboard guide
signals `gameReady` once. Failed loads offer Retry. SDK pause/resume exclusively controls
host suspension in YouTube: no page visibility/focus handlers or browser history guard.
Pause releases input, stops RAF/audio, aborts pending audio fetches and terminates the
art worker; queued art resumes in a new worker while completed main-thread textures stay
cached. Manual in-game pause is preserved. YouTube mute gates music, effects, loops and
speech independently of in-game volume controls. Muted/pause-stale effects cannot play
when decoding finishes. Audio unlock is attempted on SDK-enabled startup but never
blocks readiness on an autoplay permission promise.

`node web/test/playables.mjs` runs the production bundle with an SDK contract fixture
in Chromium/WebKit (CI serves it at `/playables/`). It checks delayed/failed load,
readiness, cloud restore/save/reload, paused rendering/input/worker behavior, host mute,
and absence of standalone storage/history/visibility handling. Unit tests cover save
races/retries and pending audio. **These are not official certification results.**

The Playables settings omit store/share/privacy links and native cloud/service cards.
The Progress tab keeps the local milestone tally and reset controls; achievement tracking,
accessory unlocks and saved progression still use the shared game systems. Native and
standalone settings retain their existing behavior. Store update/review requests are also
gated at the action boundary.

Before submission, run the official SDK Test Suite and portal preview on desktop,
mobile web, YouTube Android and YouTube iOS. Remaining product work includes
audience/rights review, metadata/rating and
sustained physical-device checks. External URL opening is blocked in the SDK host;
those entries are also omitted from settings. No application, licensing agreement,
upload or publication has been submitted. Optional score reporting and ads are not
implemented; multiple game modes need an explicit score choice before `sendScore`.

SDK references (checked 2026-11-03):
- https://developers.google.com/youtube/gaming/playables/reference/getting_started
- https://developers.google.com/youtube/gaming/playables/reference/sdk
- https://developers.google.com/youtube/gaming/playables/certification/requirements_integration
- https://developers.google.com/youtube/gaming/playables/reference/test_suite_guide

Dependencies: https://teavm.org/docs/intro/overview.html and
https://teavm.org/docs/runtime/jso.html. Font license ships with the generated bundle.
Audio provenance remains in `audio/recorded/README.md` and the music source files.

### Official SDK validation

The **Web port preview** workflow has an `official_suite` dispatch input. When enabled,
it builds the production bundle and runs `web/test/official-playables.mjs` against
Google's live Test Suite and SDK, without replacing either with mocks. The runner serves
the bundle over loopback HTTPS with YouTube's documented CSP, records the suite's MUST
and SHOULD statuses, and exercises save/reload, failed-save recovery, pause/resume and
mute. Reports and screenshots are uploaded as `official-playables-review`. This is an
integration test, not a platform certification or approval.

The runner trusts its temporary localhost certificate and permits access from the public
Test Suite to its loopback server. It does not relax the game's CSP. A live Google service
or Test Suite UI change can fail this optional job independently of the game.

For manual PC testing, download and unzip the `youtube-playables-preview` artifact.
Serve it with `web/tools/serve-playables.py --directory PATH --cert CERT --key KEY` using
a locally trusted localhost certificate. Open `https://localhost:8767/` once to verify
certificate access, then paste that URL into the official Test Suite's **Game URL** field.
Allow local-network access if the browser asks. The standalone `web-playable-preview`
artifact instead works with `python -m http.server 8766` and `http://localhost:8766/`;
it does not require the YouTube SDK or Test Suite.

On 2026-11-03, [live official validation](https://github.com/koniho/dddumpling/actions/runs/41664937731)
passed all five automated MUST checks and the five-second startup SHOULD check in
headless desktop Chromium. The suite measured a 3.07 MiB initial bundle and 0.654 s
initial readiness on the CI runner's loopback server; these are not device/network
performance claims. Save restoration, failed-save recovery, pause/resume and mute also
passed, with no game CSP violations. `sendScore` remains intentionally unimplemented
and its optional check is untested. Embedded Android/iOS testing and platform approval
remain separate gates.
