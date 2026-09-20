# Input, state and lifecycle

Historical failure notes, moved from `AGENTS.md`. Read only entries relevant to the task.
Values and file ownership may have changed; verify against current code. Current workflow
policy lives in [AGENTS.md](../../AGENTS.md).

- **RNG call order is load-bearing.** `Words.fill` consumes the RNG in a fixed order; change
  it and every generated word shifts. The hash comparison catches this. The order is also
  load-bearing for a rule: stacks are chosen *before* letters, because a letter cannot be
  picked until it is known whether it or its neighbour is a stack — a stack may not sit beside
  its own letter. Swapping those two loops back would silently drop that guarantee.

- **Reset state above early returns in `update()`.** `warnLevel` was reset *after* the
  `state != PLAY` return, so a fatal breach left the red edge glow stuck on forever. This has now
  bitten twice: the TEAM SQUISH squishy was only ever sent home from the PLAY half of `update()`,
  so dying mid-frenzy left it bouncing around the swirl, the summary and the title screen behind
  them. `breach()` clears it where the death happens. Anything a frenzy owns has two exits — the
  frenzy ending and the player dying — and the second one does not run the loop.

- **Removing from a list you are iterating.** A fatal breach clears the whole enemy list, so
  unlist before calling `breach()`.

- **A drag cannot start on a key.** Telling a swipe from a tap means holding the tap back until
  the drag is ruled out, and every tap here is a keystroke — that latency is unaffordable. The
  push-back gesture therefore starts in the strip between `dangerY` and `deckTop`, which is
  outside every key hex, and `Renderer.pushHint` lights that strip so the target is findable.
  Any future gesture in play has the same constraint. Outside play it does not apply — the
  display case holds its tap until the finger lifts, because a touch on the title screen is
  browsing rather than a keystroke. `GameView.handleCase` still hands key taps straight back to
  the caller instead of swallowing them, so the deck behaves the same with the case up or down.

- **A touch target is not the same object as the thing that advertises it.** The panic swipe could
  only be started in the sliver `Renderer.pushHint` lights, about a thirtieth of the screen, and a
  thumb coming up off a key overshot it constantly — so the gesture for the worst moment in the game
  was the hardest one to land. `Layout.inPushZone` is now the lower half of the field and the lit
  strip is unchanged: a hint the size of half the screen is not a hint. Nothing else claims a touch
  in there (keys are below `deckTop`, the settings tap is up at the HUD, and `GameView` runs the
  FLING blade first), which is what makes the catchment free to be generous.

- **A gap you open has to stay open.** The panic swipe used to shove only the words below the
  halfway mark, straight through whatever was above them, leaving two rows of letters on the same
  line — unreadable, and it read as a drawing fault rather than a rule. `pushBack` walks the field
  lowest-first and takes anything a shoved word would land on, which cascades, so a packed board goes
  up as one. Any rule that *moves* something has to answer for what is already where it is going.

- **A gesture delimited by the hardware is not a gesture.** A FLING stroke used to run from
  touch-down to touch-up, so a finger parked on the glass held one combo open for the whole frenzy
  and "N IN ONE!" was a number you waited for rather than earned. A swipe is a *motion*: the stroke
  now ends after `STROKE_DWELL` without a definite move, and the next move under the same finger
  wakes a fresh one. Three things fell out of it worth knowing. The stillness test has to be a
  displacement from an anchor, not a sum of per-frame travel — a finger resting on a screen reports
  a pixel or two a frame and summing that lets a tremble hold a combo open. It has to run on *real*
  time and above the early returns in `update()`, or the slow-motion beat the stroke just earned
  hands it three times the grace to stand still in, and dying mid-swipe leaves a blade lit over the
  summary. And the dwell alone still leaves a finger that wiggles instead of stopping, which is what
  `STROKE_MAX` is for. Note also what the end of a stroke has to *look* like: the edge dies away
  where it stopped (`STROKE_FADE`) and the ribbon stops growing, because a combo that resets with no
  visible cause reads as a fault.

- **A mode gated on game state has to be the last index.** TEAM SQUISH stars a collectible, so
  it cannot be offered with an empty case. It is excluded by rolling `nextInt(COUNT - 1)`, which
  only works while it is the highest index in `Power` — there is an assertion pinning that.

- **Resolve, then play back.** The MULTI chain takes every tile on the press and only *reveals*
  the hops over the following moment, from stored positions. Animating the removals would mean
  holding references to tiles that a fall, a word finishing or the frenzy ending could invalidate
  underneath the chain — the failure mode this file has hit most. Prefer this shape for anything
  that wants to look sequential. The push-back is the same shape: the swipe settles the threat on
  the frame it lands and only the travel is spread over `PUSH_SLIDE`. Note which half owns what —
  the slide moves the real `e.y`, since targeting and the blade have to agree with what is on
  screen, but it skips the warn recompute and the breach check while a word is on its way up.

- **One word, one meaning.** "Perfect" was asked two different questions by two different pieces of
  code: the gold dumpling wanted no wrong presses, and the earned mash wanted no wrong presses *and*
  no damage. So a word that fell past untouched cost a life and still won the flawless-wave
  celebration — no press had been wrong, so by that reading nothing was. `GameCore.perfectRound` is
  the single definition now and both read it. If two features share a word, they have to share the
  predicate as well, or the word is doing no work.

- **Two copies of a duration is one too many.** The freed-prize escape had its length in
  `Steamer` and the number inlined again in `Screens` to drive the climb, so lengthening it in
  one place broke the animation in the other. `Steamer.FREE_TIME` is the only copy now. Worth a
  grep for a bare float before tuning any timing.

- **A phase nobody named is a screen that draws nothing.** Cutting the status report out of a
  winning interlude left `bonusTimer` running through a window where none of the phase
  predicates were true. It happened to work, because the renderer fell through to the
  celebration — but only by luck. `bonusEscape` names it, and a test now walks both paths
  through an interlude asserting exactly one phase holds on every frame.

- **A per-frame "previous position" is already the current one by draw time.** The blade edge
  is drawn between where the finger was last frame and where it is now, and reusing the trail's
  own `trailPrev` for that gave a zero-length line every time — `updateTrail` brings it up to
  the finger before the frame is drawn. `bladeFromX/Y` is captured before that happens.

- **Loading a preference is not the same as applying it.** `GameCore` read the music choice out
  of the store into `bgmChoice` and never told the audio backend, which fell back to its own
  first track on every launch — so both the stored choice and the first-run default did nothing
  until the player opened settings and picked something. `startMusic()` announces it, and
  `GameView` calls that once the sound is attached. Any future setting that a backend has to
  act on needs the same push; the store round-trip alone proves nothing.

- **A new interlude phase silently zeroes every test that taps.** Adding the spinner made
  `tapBonus` a no-op for the first two seconds of BONUS, so suites that pressed straight after
  `advanceToBonus` scored nothing and still passed their own weaker checks. `advanceToMash`
  exists for that; prefer it whenever a test means to press.

- **A dispatch on `hit >= CONSTANT` breaks the moment you add a higher constant.**
  `SettingsUi`'s hit codes are ranges, and `GameView` tested them in ascending order — so adding
  `HIT_STAGE = 300` above `HIT_TEST = 200` meant every stage chip was read as a playtest chip and
  dropped the player into a frenzy instead. It has to be checked highest-first, and there is now an
  assertion that a stage chip does not resolve to the playtest row.

- **A debug jump must arrive the way play arrives.** `GameCore.enterStage` is shared by
  `advanceStage` and the settings panel's stage jump, deliberately: a jump that sets a stage up a
  little differently from the way the game sets one up is a tool that hides the bug you are using it
  to hunt. The jump adds only the clearing-up — field, frenzy, boss, in-flight gestures — because it
  can be taken mid-stage, and it leaves the score and lives alone on purpose, since looking at a
  late stage on two lives is the thing it is for.
