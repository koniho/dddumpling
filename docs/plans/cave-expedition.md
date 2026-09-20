# Cave expedition

Tracking issue: [#54](https://github.com/koniho/dddumpling/issues/54).
Active branch: `cave-minigames`.

## Gauntlet pacing

Developer stages 21–25 are cave expeditions; production retains its original four-land progression.
First entry keeps the saved explorer selection. Normal play uses LOFI DRIFT.

The explorer runs at .805 route units per second. Cave pacing is 70% of the original gauntlet rate.
Seven encounters repeat through a stage with roughly 1.4–2.9 seconds of travel between them.
Three forks allow a quick tap choice, with a .79-second fallback; the first has a concurrent
finger hint instead of a separate blocking lesson. There are no route heart pickups.

- Enemies spring from side cover and rush in over 2.57 seconds. Their two- or three-key
  response is active from the first animation frame; aiming the lantern is not a combat gate.
- Cave-ins zoom toward the centered explorer while the cracked floor slides up. Six rocks
  start falling at .57-second intervals, each with 1.03 seconds to land and a visible shadow.
  Relative dragging retains a 1.3-screen-width/second steering limit. Collision tolerance is
  unchanged; the close-up is visual, not a larger damage target.
- Quicksand zooms in over 300 ms, pulls the dumpling down with a frightened face and flailing
  arms, and allows eight alternating presses over 4 seconds. Progress lifts the dumpling.
- Encounters do not overlap. Each failure can charge one life; survivors resume running.
  The exit enters the alternating Cave Band / Dumpling Mine interludes.

Kids Mode slows the enemy approach and projectile flight only; traversal through the cave,
trap deadlines, zooms and effects keep normal timing. Pause freezes the encounter.
See [Game timing](../game-timing.md) for the timing boundaries.

## Winding passages and light

A sampled spline moves left, right, forward and back. Arc-length lookup keeps forward motion
consistent through bends. Branch offsets follow the local route normal, and the camera follows
both world axes. A .30-second trap zoom and .20-second release keep the action centered.

Only passage floor and cave walls fill the world; no carpet of loose stones remains. Lantern
falloff is evaluated on floor and wall facets, so empty space receives no painted light fan.
Nearby surfaces retain a small readable ambient glow. Side-cover boulders, falling rocks and
the exit remain encounter objects.

## Development seams and verification

`CaveRoute` owns the spline, forks and event schedule. `Cave` owns progress, projection and
encounters; `CaveTraps` owns steering and escape rules. `CaveTerrain` shades corridor surfaces;
`CaveScreen` stages encounters. All remain pure Java behind Painter.

`./check.sh -q -s Cave -f 106,107` tests the expedition and renders its main visual states.
Bounded tests cover stages 21–25 on both fork choices, with reaction delays, miss rates and
press limits. The rock pilot sees only rocks already falling and checks its bounded steering
trajectory at their predicted impact times. The full soak still guards the overall skill curve.

Device playtesting remains necessary for the faster motion and reaction windows. Production
is still gated pending the broader cave review.

Rocks tumble along straight and angled approaches into fixed landing markers. Each crash throws rotating shards and expanding dust that persist after the encounter. Short synthesised rumble, crash, ambush and sinking cues accompany the action. Scene rumble leaves the keyboard steady; native haptics fire once per cue, with stronger rock impacts on Android. Quicksand pulses every 0.65 seconds. Pausing clears queued haptics and leaving clears debris and rumble.

Rockfall and quicksand ease the follow camera from the explorer's current cave position to the center of the playfield over 300 ms. Magnification and camera travel share a smoothstep curve; hazard animation and input continue during the zoom. Each hazard plays its distinct entry cue once.

The rockfall ground slides up from below the playfield during that same 300 ms camera move. Low continuous scene shake lasts throughout rockfall, with stronger impact jolts layered over it; the keyboard stays steady.

## Generated runs

Each cave stage entry draws a fresh seed. A bounded spline generator varies and mirrors the
winding map, then resamples to the same travel length. Every route includes rockfall, quicksand
and enemies, with shuffled free encounters and distinct random hazards at each fork.

Enemy responses draw non-repeating adjacent keys; quicksand draws two distinct alternating keys.
Rock lanes vary within a band that guarantees reachable escape space, including 300 ms human
reaction time. Response lengths and encounter count retain their tuned limits; timing follows the shared cave pace.

The harness covers 256 generated maps and all eight branch combinations, plus 960 bounded-player
runs across five stages. All 960 survived with 250 ms reaction, 4% misses and 4, 6 or 9 presses/sec.

## Surprise enemy feedback

Ambush cover is positioned on the local corridor wall. A 371 ms step clock drives alternating
feet, body bounce, dust, thuds, scene shake and one-shot native haptics. Correct keys launch
100 ms bolts from the explorer to the enemy's tummy; impact triggers wide eyes and flailing
arms. Defeated enemies remain visible for a 650 ms reaction and retreat behind their own rock,
while normal travel resumes. Scoring and the response deadline remain independent of the animation.

Wall silhouettes use fixed world-space irregularity, with uneven polygonal rock faces, cracks
and dark seams. Facets respond to the lantern at their world positions. Floor masks keep the
walking corridor clear; wall geometry does not change collision rules or consume gameplay RNG.

The 30% pacing reduction applies to travel, fork choices, enemy approaches, stomps and rockfall/quicksand deadlines. The 300 ms zoom, steering response, bolts and hit feedback retain their responsive timing.
