# Simulator profiling — 2026-09-12

Host: Apple M5, 32 GiB RAM (`Mac17,2`), macOS 26.5.2. App: Debug arm64 on
iPhone 17 Pro Simulator / iOS 26.5, Xcode 26.6. One app ran at a time, with builds
and XCTest stopped during the samples. These are short diagnostic runs, not a
physical-device benchmark or sustained-play acceptance result.

`DDD_PROFILE=1` reports bounded windows of 600 draws. “Draw-submit” measures only
the call into the shared renderer: UIKit can record drawing commands and rasterize
them later, outside that timer. Callback intervals measure host scheduling, not
which frames the compositor actually presents. Resident memory includes shared
pages and differs from the physical footprint reported by Instruments or `sample`.

## Original synchronous layer

| Scene/window | Mean update ms | Mean draw-submit ms | Callback mean / p95 ms | Resident MiB | State at end |
| --- | ---: | ---: | ---: | ---: | --- |
| Title, first | 0.01 | 0.43 | 24.99 / 37.67 | 191.4 | Title |
| Title, fourth | 0.00 | 0.41 | 23.96 / 37.84 | 191.5 | Title |
| Dark Divide, first | 0.54 | 1.31 | 51.27 / 100.34 | 199.2 | Game over |
| Dark Divide, second | 0.00 | 1.32 | 33.33 / 47.65 | 187.2 | Game over |
| Stars, first | 0.23 | 0.85 | 34.74 / 42.06 | 199.9 | Star course |
| Stars, second | 0.10 | 0.90 | 39.65 / 83.97 | 200.4 | Game over |

Raw logs remain in ignored `ios/build/profile-{title,divide,stars}.log`. The first
boss/star windows include startup and gameplay transitions; they do not measure
a continuously stressed boss. Peak update calls reached 56.92 ms for Divide and
46.37 ms for Stars; the cause of those individual spikes has not been isolated.

A three-second title process sample found 1,117 of 1,993 main-thread stack samples
inside deferred Core Graphics display-list rasterization (`CGDisplayListDrawInContextDelegate`),
mostly path filling and alpha blending. A second sample after the boss run reached
game over found 946 of 1,501 there. These are stack sample proportions, not exact
CPU-time percentages. Local samples: `/tmp/dddumpling-profile-title.sample.txt` and
`/tmp/dddumpling-profile-divide.sample.txt`.

The short title run's resident memory settled near 191.5 MiB; this is insufficient
to rule out leaks. The title process sample reported a 57.4 MiB physical footprint
(66.1 MiB peak), illustrating why resident size must not be presented as exclusive
app memory.

The deferred raster work explains why earlier sub-millisecond draw timers did not
establish 60 fps. Measure the same scenes using Time Profiler, Core Animation and
Allocations on a signed physical iPhone before deciding whether Core Graphics meets
the release target or an accelerated Painter backend is necessary.

## Asynchronous rasterization

Enabling the layer's `drawsAsynchronously` property improved callback pacing without
changing the shared renderer. UIKit still records the game's drawing on the main
thread; Core Animation executes those commands asynchronously. Apple's
[property documentation](https://developer.apple.com/documentation/quartzcore/calayer/drawsasynchronously)
describes this behavior and recommends measuring before enabling it.

| Scene/window | Mean update ms | Mean draw-submit ms | Callback mean / p95 ms | Resident MiB | State at end |
| --- | ---: | ---: | ---: | ---: | --- |
| Title, first | 0.01 | 1.03 | 16.68 / 16.67 | 183.5 | Title |
| Title, third | 0.01 | 1.08 | 16.67 / 16.67 | 177.1 | Title |
| Dark Divide, first | 0.35 | 1.73 | 17.01 / 16.67 | 181.4 | Boss play, one life |
| Dark Divide, second | 0.19 | 1.72 | 16.94 / 16.67 | 181.6 | Game over |
| Dark Divide, third | 0.01 | 1.74 | 16.67 / 16.67 | 181.6 | Game over |

Raw logs: `ios/build/profile-{title,divide}-async.log`. Title and early boss screenshots
were visually reviewed, including text, translucent layers, contours and the pause/HUD
spacing. The runs start from the same Debug scene seed but progress at different callback
rates, so window contents are not identical workloads. No human played the boss to victory.

The asynchronous layer is now the default for Debug and Release. Debug launches can set
`DDD_SYNC_RASTER=1` for a synchronous comparison; `DDD_PROFILE=1` enables measurements in
either configuration. The temporary experiment used `DDD_ASYNC_RASTER=1`, which is no longer
needed or read by the final host.

This improves simulator scheduling; it does not prove presented 60 fps, physical-device
latency or thermal behavior. Occasional boss-update spikes remain (56.56 ms maximum in
the first asynchronous window); their source needs a separate event-level trace. The
asynchronous star scene has not received a timing run, though the UI suite exercises it.

## Rapid sound effects

Transient effects run on a dedicated serial queue so player preparation and playback do not
block touch handling or drawing. Each pitch variant reuses up to six players; at saturation
it recycles a busy player. At most twelve requests may be pending, and requests older than
100 ms are discarded. Pausing invalidates pending requests and queues a stop for active effects.
Native audio tests cover worker execution, saturation reuse and cancellation across a pause.
Repeated misses and FLING still need a physical-device feel check; haptic cost is unmeasured.

## Physical-device follow-up — 2026-09-13

The player reports FLING is much improved after moving audio off the main thread, but rapid
presses still stutter, especially during FLURRY. A 31.3-second Time Profiler attachment to
the Debug build on iPhone 16 Pro Max / iOS 26.6.1 captured 9,784 main-thread CPU samples.
7,139 (73%) included `DDGameView.drawRect:`, 20 included input packet handling, and three
included `UIImpactFeedbackGenerator`'s impact call. Color-component evaluation appeared in
850 main-thread samples (8.7%). These are inclusive CPU samples, not elapsed frame times;
the capture does not measure waiting threads or establish a FLURRY-only workload.

`DDIOSPainter` now reuses native Device RGB colors in a fixed 1,024-slot cache instead of
rebuilding them for each shape. Alpha remains part of the key; collisions replace entries.
Native tests compare fill/stroke pixels against the original RGB setters and exercise 10,000
color replacements. Thirteen native tests pass. The rendering improvement still needs a
device retest; haptics are unchanged, and their small CPU sample count does not rule out waits.
