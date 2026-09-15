# Cave expedition

Tracking issue: [#54](https://github.com/koniho/dddumpling/issues/54).

Feature branch: `feature/cave-expedition`.

## First playable version

The cave is gated by `BuildFlags.DEVELOPER`. Production retains four playable lands and its
original scenery cycle after stage 20; cave choices and discovery save slots remain intact.

In developer builds, the fifth land follows Mushroom Land at stage 21. Its picker entry unlocks from the mushroom
boss friend. Stages 21–25 use expeditions; stage 26 resumes the existing endless waves.
The developer ALL LANDS chip or a stage jump can reach it immediately.

First entry opens a short explorer-selection scene: cream, rainbow, golden, silver, sparkly mint,
or purple. Tapping one saves it across cave levels and app restarts, then animates it into the
entrance. No choice is saved until a player selects one.

The camera follows the automatically walking dumpling. Tap the playfield to aim its lantern.
At a fork, tapping a branch commits that route. Three fading lights show the three-second wait;
when it expires, the branch nearest the beam is selected. The first stage's first fork shows a
finger tapping a branch, a swinging beam, and a dotted path before the countdown begins.
A real tap dismisses that demonstration immediately.

Enemies wait as eyes in the dark. Holding the beam over one for 250 ms reveals its response;
it then approaches for 4.5 seconds at normal speed. Complete the character-key response before
it arrives. Wrong keys keep progress and consume time. Arrival costs one life, once.

Forks offer different encounters and hearts. Walking over a heart restores one life up to the
normal maximum. The heart sits within the junction's lookahead, so recovery can inform a choice.

- Cave-in: a 1.2-second dust/drag warning, then six rocks with visible landing shadows. Drag
  sideways anywhere in the playfield, using the star course's relative-drag interaction.
- Quicksand: a 1.2-second warning, then alternate the two illustrated keys fourteen times.
- Each trap has six active seconds and can cost at most one life. Failed traps let surviving
  players continue. Encounters never overlap.

The exit completes the stage and enters the normal interlude. The first expedition takes roughly
44–46 seconds for the current bounded test players. Walking now covers .36 route units per second, twice the original .18. Encounter and trap clocks
retain their existing durations. The walker uses normalized Softbody springs for step-driven
lift and squash, which settle when walking stops and freeze on pause.

The longer stages require a longer full-run
soak budget; they do not add pressure to the old falling-word difficulty ramp.

## Development seams

- `CaveSelection` / `CaveDumpling`: first-entry selection, saved finishes, and spring-driven walking.
- `CaveRoute`: authored fork contents, hearts, and path geometry.
- `Cave`: route progress, lantern identification, encounters, combat, and completion.
- `CaveTraps`: timed trap rules and bounded steering.
- `CaveInput`: platform-free pointer ownership, shared touch commands, and cancellation.
- `CaveScreen` / `CaveArt`: drawing with the existing Painter/Kawaii helpers, layered brown stone,
  light-brown path edges, warm lantern light, and darker unlit areas.

GameCore retains frame-loop and key-routing order. Cave rules run in the PLAY section before
ordinary word spawning. Input is released on pause, settings, trap completion, stage changes,
and death. Android and iOS route their touch packets through `CaveInput`; both native stores
persist the explorer choice. No new Android dependency reaches a pure file.

## Validation and iteration

`./check.sh -q -s Cave -f 106,107` exercises the expedition and renders its main visual states.
The bounded bot pays its normal reaction time and press budget. For rocks, it can react only to
already-visible falling rocks and is subject to the same steering speed cap.

This version uses one authored route with three forks across the five cave stages, varying the
combat responses. Existing synthesized effects and selected music are reused. Device playtesting
should guide later route variety, enemy designs, sound changes, and trap difficulty.
