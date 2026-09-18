# Cave expedition

Tracking issue: [#54](https://github.com/koniho/dddumpling/issues/54).
Active branch: `cave-minigames`.

## Gauntlet pacing

Developer stages 21–25 are cave expeditions; production retains its original four-land progression.
First entry keeps the saved explorer selection. Normal play uses LOFI DRIFT.

The explorer now runs at 1.15 route units per second, over three times the former .36.
Seven encounters repeat through a stage with roughly 1–2 seconds of travel between them.
Three forks allow a quick tap choice, with a .55-second fallback; the first has a concurrent
finger hint instead of a separate blocking lesson. There are no route heart pickups.

- Enemies spring from side cover and rush in over 1.8 seconds. Their two- or three-key
  response is active from the first animation frame; aiming the lantern is not a combat gate.
- Cave-ins zoom toward the centered explorer and crack the floor immediately. Six rocks
  start falling at .40-second intervals, each with .72 seconds to land and a visible shadow.
  Relative dragging retains a 1.3-screen-width/second steering limit. Collision tolerance is
  unchanged; the close-up is visual, not a larger damage target.
- Quicksand zooms in immediately, pulls the dumpling down with a frightened face and flailing
  arms, and allows eight alternating presses over 2.8 seconds. Progress lifts the dumpling.
- Encounters do not overlap. Each failure can charge one life; survivors resume running.
  The exit enters the alternating Cave Band / Dumpling Mine interludes.

Encounter deadlines do not multiply the developer travel-speed setting. Kids Mode and pause
retain the shared game-clock behavior. Zooms and effects freeze along with their encounters.

## Winding passages and light

A sampled spline moves left, right, forward and back. Arc-length lookup keeps forward motion
consistent through bends. Branch offsets follow the local route normal, and the camera follows
both world axes. A fast .14-second trap zoom and .20-second release keep the action centered.

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
