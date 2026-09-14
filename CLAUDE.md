# Working on Hexatype

Read this before touching anything. It exists to save you the discoveries that cost time
the first time round.

If you have never worked on this repo before, read [ONBOARDING.md](ONBOARDING.md) first — it is the
ten-minute path from nothing to a verified change, and it tells you which parts of this file matter
most.

[GLOSSARY.md](GLOSSARY.md) names every game element and maps the plain-English term to the
code. Use those names back to the user; several differ from the identifiers (a falling word is
`Enemy`, the frenzy is `mode`, the interlude is `BONUS`), and getting them wrong wastes a round
trip working out which thing was meant.

## The one thing that matters most

**You can see and hear this game without building or installing it.** `./check.sh` runs the
whole thing headlessly: ~1480 rule assertions, then it renders real frames to `out/*.png` and
every sound to `out/sfx/*.wav`. Read the PNGs with the Read tool — the `0-*.png` sheets each
show a whole set at once (the six letters, the thirty collectibles, both vignette casts). That loop is seconds, not
minutes, and it needs no device.

### And you can have it played for you

`tools/Bot.java` is a player with stated limits — presses a second, a beat to find the next word,
a miss rate — and `TestSoak.boundedPlay` runs three tiers of it and asserts where the curve stops
them. Use it for any tuning change. The perfect-play soak beside it presses thirty times a second
and never misses, so it can only tell you the game is winnable by a god; every difficulty question
that has actually gone wrong here went wrong for hands. The frenzy wall survived several rounds of
tuning because nothing in the harness had a ceiling.

Its output is the curve, in one line per tier:

```
casual  4/s react 0.30s miss 8%  ->  stage 8.5 after 206s, 4 of 4 died
steady  6/s react 0.20s miss 4%  ->  stage 19.3 after 523s, 4 of 4 died
quick   9/s react 0.13s miss 2%  ->  stage 21.3 after 569s, 4 of 4 died
```

Two things to know before trusting it. It cannot swipe, so a FLING frenzy reaches it as a plain
typing frenzy at full strength and the push-back is never used — the pessimistic reading, which is
the useful one for a floor. And a bot that "declines" powerups is not available to write: with
nothing engaged, any press matching the drifting letter catches it, so avoidance is not a strategy
a player can have either. Frenzy fairness is therefore measured as frenzies survived per run, not
by comparison with abstinence.

This works because *all* logic and *all* drawing are pure Java behind the
[`Painter`](src/com/dddumpling/game/Painter.java) interface. The APK implements it with
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
./check.sh                      # rules + frames, no SDK needed
./check.sh -q                   # failures, diagnostics and the tally only — use this by default
./check.sh -q -r                # rules only, no frames. Seconds.
./check.sh -q -s Boss           # one suite (Rules Words Stages Stars Boss Softbody Collect Lore
                                #   Visuals Audio Power Soak)
./check.sh -q -f 60,65          # render only these frames, skipping the sheets and the WAVs
./check.sh -q -f 60 -c 0,.1,1,.45   # ...cropped to that box of the screen, in 0..1 fractions
./check.sh 1080 2400 2          # real device size (slower, for layout checks)
./build.sh                      # gated on check.sh; produces a signed hexatype.apk
./deploy.sh                     # build + install + launch
```

`-q -r` while iterating on rules, `-f` plus `-c` when you need to *look* at something: a cropped
frame is a quarter the size of a full one and shows more of what you were checking.

`build.sh` refuses to package if any assertion fails. `sdk/android.jar` is not committed; see
README.md for the one-time fetch.

Installing needs a tap unless adb is paired over Wireless debugging (README.md). A plain
`termux-open` silently does nothing unless `allow-external-apps = true` is set in
`~/.termux/termux.properties` — no dialog, no error, and the reason only appears in logcat
under Termux's own UID.

**You cannot see this app's crashes.** Termux's logcat only shows its own UID, and there is no
adb or dumpsys here. That is why [`Crash.java`](src/com/dddumpling/game/Crash.java) renders the
stack trace on screen instead. If the user reports a crash, ask them to read that screen.

## Working cheaply

Reading this repo is what costs, not writing it. The comment style is dense on purpose, so a
"quick look" at `GameCore` or `Boss` is expensive. Habits that matter:

- **`./check.sh -q` by default.** Full output is ~2000 lines; quiet is ~40. Add `-r` while working
  on rules, `-s Boss` to run one suite.
- **Prefer a printed number to a rendered frame.** An image costs roughly a hundred times what a
  `printf` line does. `Preview` already prints per-frame diagnostics — add one rather than looking,
  and when you must look, use `-f` with `-c` so it is a crop rather than a whole screen.
- **Grep, then read a window.** `grep -n symbol file` then `sed -n 'a,bp'`. Whole-file reads of
  `GameCore` (2500 lines) or `Boss` (1400) are rarely what the question needed.
- **One task per session.** A compacted session pays for its summary on every later turn, so two
  sessions beat one long one.
- **Say what verification you want.** Frames, the soak run, doc updates and new assertions are all
  on by default here. Any of them can be skipped on request, and that is the largest single lever.

## Layout of the code

Pure (in the harness and the APK):

| File | Holds |
| --- | --- |
| `Glyph` | the six-letter palette, hexagon geometry, hue cycling |
| `Kawaii` | the six characters and their faces, plus the mood dumpling |
| `Layout` | every screen coordinate, derived from view size + insets |
| `GameCore` | the spine: state machine, the frame loop, the press router, and the state everything else works on |
| `Pacing` | the stage difficulty dials. Pure functions of stage — the one file to read when tuning |
| `Blade` | the FLING swipe: what a stroke is, when it ends, what one sweep cuts |
| `CaseUi` | browsing the display case: scroll, jump, drag, two-tap wipe |
| `Interlude` | the between-stages round: mash, course, blind box, parade |
| `BossPlay` | the boss fight's wiring — a press or drag turned into score, sound, shots and lives |
| `Words` | word generation and the press-budget rules |
| `Fx` | shots and particles |
| `Steamer` | between-stages minigame state |
| `StarPath` | the star course: how one is generated, flown, and won |
| `StarScreen` | the star course on screen, including its victory tableau |
| `Collect` | the thirty collectibles: catalogue, blind-box odds, owned-set bitmask |
| `Trinket` | draws a collectible — fifteen shapes crossed with nine finishes |
| `Cabinet` | the glass case itself: a wireframe box three-quarters on |
| `Showcase` | the display case: badge, shelf, position bar, and every touch target on it |
| `Lore` | a story per collectible, plus who is cast in its vignette |
| `Parade` | the collection marching in, the new one joining, the line marching off |
| `Storybook` | the story popup and its ten looping vignettes |
| `Power` | the powerup letter and its three modes |
| `Boss` | the every-fifth-stage boss: five mechanics, its elements, and what a press/tap/drag does |
| `BossScreen` | the boss on screen: body, health header, ornaments and its elements |
| `Softbody` | a pressurised 2D soft body — the ring of sprung nodes every boss is built on. Rests as a circle or an ellipse, with per-body springiness |
| `Slime` | draws a soft body as gooey translucent slime with a face |
| `Painter` | the drawing interface |
| `Draw` | palette + shared geometry (pill, star, hash, rainbow) — renderers extend it |
| `Sky` | background, clouds, vignette, HUD band |
| `Renderer` | frame orchestration + the play field |
| `Hud` | score/stage/lives, frenzy bar, banners |
| `Screens` | title, game over, minigame, settings |
| `RoundEnd` | how a run ends: the swirl, the haul dancing, and its flight to the case |
| `Demo` | the title screen playing itself, in place of two lines explaining how |
| `Sfx` / `Music` | procedurally synthesised effects and looping tracks |
| `Narration` | what the story popup says out loud, and the pitch and pace of each line |
| `SettingsUi` | settings panel geometry and hit-testing |

Android-only: `MainActivity`, `GameView` (input + frame loop), `CanvasPainter`, `Audio`,
`Crash`.

Harness-only in `tools/`: `Check` (base class with the tally, assertion helpers, drivers and
the Store/Sound stubs), `Test*` suites, `Bot` (a player with stated limits), `Preview`,
`RasterPainter`, `Font`, `Png`, `Wav`.

The `Test*` classes extend `Check` and the renderers extend `Draw` **so that helpers and
colours resolve unqualified**. That is deliberate: prefixing several hundred call sites buys
nothing. Follow the pattern rather than "fixing" it.

## Conventions worth keeping

- **Explain game mechanics through visual cues wherever possible.** Use character poses,
  animation, shape, color, and reactions to show what to do and what happened. Keep explanatory
  words and extra UI to a minimum; new mechanics should be understandable through play.

- Every rule change gets an assertion in the matching `Test*` suite, and every new visual
  state gets a frame in `Preview` so it can be looked at.
- **Comments are terse.** One or two lines, and only for what the code cannot say: why a value is
  what it is, what broke at the obvious value, which ordering is load-bearing. Keep the fact, drop
  the essay — no restating the code, no narrating the debugging, no paragraph where a clause will do.
  Every line of comment is a line every future reader pays for.
- Keep the *why*, especially where a value was tuned against a failure. Several constants exist at
  their value because the obvious value was wrong. Compress that to a clause: "0.35 radii — 20px at
  1080 wide, above finger jitter, below any real swipe."
- `GameCore` must stay free of `android.*`. Audio and persistence reach it through the `Sound` and
  `Store` interfaces, which is also how tests assert which effect fires when.
- Files over ~350 lines want splitting, and the seam is `Fx`'s: statics taking `GameCore c`, working
  on its fields rather than owning them. `Pacing`, `Blade`, `CaseUi`, `Interlude` and `BossPlay` all
  came out of `GameCore` that way, and no call site outside had to change.
- What must *not* be split: `GameCore.update` and `tapKey`. Their ordering is load-bearing — see the
  traps below — and scattering them hides exactly the faults this file has produced before.

## Traps that have already bitten

- **Insets lie.** In immersive mode `padT` is 0 and this device reports no cutout inset, so
  a centred HUD lands under the punch-hole camera. `Layout.topSafe` keeps its own floor.
  There is a regression assertion at 1080x2400.
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
- **Position-driven animation, not time-driven,** for anything tied to where a thing is. A
  timed entrance ramp finished while the word was still off-screen.
- **Translucent overlapping primitives double-blend.** Build a stadium as one polygon, not a
  rect plus two circles, or the caps come out darker.
- **Tune against the real firing rate.** The per-press sky glow looked right in one frame and
  strobed in play; the 4s minigame let a masher finish it in one go, defeating the
  accumulation it was built for.
- **The harness font is an ASCII subset.** `tools/Font` has one bitmap per character it knows;
  a glyph it does not have simply vanishes from the PNG, so text using one looks right on the
  device and is missing a letter in every frame you check. `?` and `'` were both added to it
  before they could be used, and `:` for COLLECTIONS. Add the glyph rather than writing around it —
  but do check, since the set is still small. Arrows and chevrons are drawn as polygons for the same reason.
- **Text that does not fit is reported, not eyeballed.** `RasterPainter.text` records any line
  drawn off the screen edge and `Preview` prints `DOES NOT FIT` under the frame that did it, with
  the width and the x range. It respects the current clip, because the display case deliberately
  draws a wing tile past the plaque for the clip to cut off. Since the harness font is wider than
  Quicksand, a line that fits in a PNG fits on the device — so this is the whole check. Run
  `./check.sh | grep -B1 'DOES NOT FIT'` after any text or layout change.
- **`Painter` cannot clip to a shape,** only to a rectangle. That is why a banded finish in
  `Trinket` is fitted to an ellipse and why `Collect.banded` restricts which shapes may wear
  one — there is an assertion holding the catalogue to it.
- **A drag cannot start on a key.** Telling a swipe from a tap means holding the tap back until
  the drag is ruled out, and every tap here is a keystroke — that latency is unaffordable. The
  push-back gesture therefore starts in the strip between `dangerY` and `deckTop`, which is
  outside every key hex, and `Renderer.pushHint` lights that strip so the target is findable.
  Any future gesture in play has the same constraint. Outside play it does not apply — the
  display case holds its tap until the finger lifts, because a touch on the title screen is
  browsing rather than a keystroke. `GameView.handleCase` still hands key taps straight back to
  the caller instead of swallowing them, so the deck behaves the same with the case up or down.
- **"Random" motion cannot be random.** The display case's wireframes shimmer, which wants noise
  and cannot have it: preview output is a pure function of state and every frame is hash-compared
  against the last run, so an RNG or a random walk there would make every check differ for no
  reason. `Cabinet.shimmer` is two sines off `clock` at hashed rates and phases, which reads as
  shimmer and still hashes stable. Anything that wants to look unpredictable needs this shape.
- **A cross-fade between two labelled things is not a dissolve.** Fading the case badge out and
  the case in on the same `caseFade` drew both at half strength through each other, captions and
  all, and read as a rendering fault. They run in series now — badge gone by the halfway point,
  case in from there — via `Screens.caseOut`/`caseIn`. Overlap only what has no text on it.
- **A global text scale needs the leading scaled with it.** Every text size goes through
  `Draw.type()`, one knob (`TEXT`, currently 1.34). Sizes scale; the *gaps* between stacked lines
  are still plain `unit` multiples, so the first pass had SCORE sitting on its own number and
  ACCURACY on its percentage. Where lines stack, wrap the offset in `type()` too. Two other things
  that fell out of it: long strings stop fitting (28-character prompts ran off both edges — the fix
  was shorter copy, not a smaller size), and `Screens.settings` is deliberately *excluded*, because
  its chips and rows are packed tight enough that scaled labels left their boxes. It bit again later
  anyway: `Hud.hud` kept a plain `s * 0.95f` between SCORE and its number, and at TEXT 1.34 the
  digits' caps came up three pixels through the label's baseline. Reported from a screenshot, not by
  the harness — text-on-text collision is the one thing `DOES NOT FIT` does not look for.
  `Hud.labelY`/`scoreSize` and `RasterPainter.CAP` exist so `TestVisuals.hudStacking` can assert the
  clearance across a sweep of widths instead of it being eyeballed at one value of the knob. The harness font
  is wider than Quicksand, so a line that fits in a PNG fits on the device.
- **Sharing an animation channel makes two events look identical.** A wrong press in the
  interlude set `lidPulse` and `flash` before the wrong-key check, so it pulsed the lid and
  flashed the basket exactly like a landed press — the only thing distinguishing them was the
  sound. `badPulse` is its own channel now. If two outcomes should look different, they need
  different state, not different timing on the same state.
- **Two difficulty sources must never multiply.** The frenzy's pace was flat multipliers — 6x the
  spawn rate, 4x the crowd, 2x the fall — sitting on top of a ramp that had already halved the spawn
  interval, so they compounded with it. By stage 10 a frenzy asked 23 presses a second and by stage
  22 it asked 43, against maybe 8 from two thumbs. It read as the game breaking, and it was felt
  worst in FLURRY, which is the one mode buying accuracy rather than throughput. `Power.taper` now
  scales all three down along the ramp toward `LATE_RATIO`, and `SKY_RATE` is deliberately left flat:
  taper what costs the player, leave what only looks exciting, or the reward stops reading as one.
  Anywhere a difficulty path multiplies two dials, check what their product does at the far end.
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
- **A readout that outlives what it reports needs its own copy of the numbers.** `Hud.sliceCall`
  read `strokeKills` live, which was fine while a stroke could only end by lifting; once strokes end
  on their own, a fresh one starting inside the readout's 1.1s rewrote "4 IN ONE!" down to the new
  tally and then blanked it. `callKills`/`callCuts` are frozen when the announcing stroke ends.
- **A permanent warning is not a warning.** `harm()` was linear, so losing one life of three put
  a third-strength pulsing red border round the screen for the rest of the run — reported twice as
  a "stuck vignette" and it was not stuck, it was working as written. It is squared now: one life
  lost is barely visible, the last life is unmistakable. Before hunting a stuck effect, check
  whether something is simply *meant* to stay on and mistuned.
- **A speech engine intones a whole utterance, so chopping text up destroys prosody.** The first
  narration cut each story into sentences and gave every one its own pitch, expecting an arc. What
  it produced was flat fragments at arbitrary heights — the engine could not see a sentence, so it
  had nothing to intone, and the stepping between them read as lurching. The story now goes over as
  one utterance with its punctuation intact and a single pitch. The levers that genuinely reach
  prosody are the *voice* (quality varies enormously; `getVoices()` and pick) and the punctuation
  you hand over. Note also that the engine's volume parameter is a fraction of the stream, not a
  gain — the only way to make the voice louder is `Audio.duck()` holding the music down, released by
  an `UtteranceProgressListener` on the last queued utterance since there is no queue-drained
  callback.
- **A sound that repeats has to be short and has to decay.** The blade chops several times a
  swipe and the TEAM squishy squishes every couple of seconds; anything with a tail smears into a
  wash, and the achievement fanfare was doing exactly that. There are assertions on the chop's
  length, on its tail being a quarter of its head, and on its zero-crossing rate — that last one
  is a cheap stand-in for "has a tone under it rather than being a hiss", and it is the one that
  caught the first attempt at giving it body being no better than the original. The shelving chime
  is held to the same three, plus one more: it must be shorter than the gap between two landings,
  since a haul shelves several a tenth of a second apart.
- **Several things landing at once is one event, however many things there are.** The haul's flight
  home staggered its departures and then had every flyer converge on the same instant, which looked
  deliberate and was — until it needed a sound per landing, at which point a haul of four played one
  chord. Arrivals are staggered too now. If you are about to attach a sound to the end of an
  animation, check that the ends are actually distinct before writing the sound.
- **A mode gated on game state has to be the last index.** TEAM SQUISH stars a collectible, so
  it cannot be offered with an empty case. It is excluded by rolling `nextInt(COUNT - 1)`, which
  only works while it is the highest index in `Power` — there is an assertion pinning that.
- **Making a target bigger is a difficulty change, not a cosmetic one.** The star course's flyer and
  checkpoints went up by half, and the pickup test was two flyer radii — so the catch band went from
  a ninth of the screen width to a sixth, which is wider than the course's own wander. A flyer left
  dead centre with nobody touching the keys then completed courses by itself, collecting a free
  collectible and a free life; the stars carry over between attempts, so anything it can reach it
  eventually finishes. The soak bot found it, having no way to steer at all — it turned up as the
  monotonicity check failing, because a skill-independent bonus had been added to every tier.
  `StarPath.pickupR` aims at the star's drawn *heart* instead, which keeps the old number. Resize a
  target and re-derive the rule, or state deliberately that the rule is unchanged.
- **A generator tuned per step is tuned against the wrong clock.** The course used to be a random
  walk with a bounded step per checkpoint. But the scroll accelerates, so the last stars arrive three
  times as fast as the first: one bound per step meant a demand three times higher at the end than at
  the start, and it had to be small enough for the end — which is why the course could only wiggle
  down the middle. `StarPath.make` is a sine in *seconds* now, so it asks the same sideways speed
  everywhere and can span the whole play area. `TestStars` prints the peak demand against `MAX_VX`;
  the two are meant to be read together, but see the next entry for why that ratio is only printed.
- **A peak is not a demand: integrate it.** For a long time the course was held to "asks about 1.2×
  `MAX_VX`, which is the near side of uncatchable", and that number turned out to measure almost
  nothing. A sine only exceeds the steering speed over a short arc either side of its steepest point,
  so 1.2× cost a perfect tracker one hundredth of a play width — a twelfth of the catch band — and
  every pilot the harness could write collected all twenty at *every* reaction time from 0.05s to
  0.30s. The course was fully trackable and the assertion said it was at the edge of possible. What
  the player feels is the *lag* that overspeed integrates to, measured against the tolerance it has
  to fit inside; `TestStars.trackerLag` is that, held between 45% and 110% of the band, with
  `tightestWindow` — the propagated set of positions from which every remaining checkpoint is still
  reachable — as the far bound. The cliff is real, it was just being located with the wrong ruler.
- **Put the difficulty on the axis the player controls.** The pickup was a circle, so spacing the
  stars out and shortening the flight cut the time a checkpoint is level with the flyer from 117ms to
  83ms — and nothing a player does moves that number, since the climb and the scroll are both
  functions of the clock. It is not difficulty, it is how much of the difficulty is luck: the naive
  pilot went from nineteen stars a course to ten with the sideways demand unchanged. `pickupY` makes
  the catch an ellipse and `TestStars` measures the window, so the vertical stays generous and the
  demand lives where the keys are.
- **A tolerance for being late is measured in milliseconds, so store it in milliseconds.** That
  ellipse was first written as 1.7 pickup radii, which is 117ms of grace at one scroll speed and
  47ms at two and a half times it — and the next spacing change duly ate it, silently, because a
  distance says nothing about the thing it is actually buying. `StarPath.GRACE` is 0.065s and
  `pickupY` is that times `closingSpeed`, a one-frame finite difference of the gap the course is
  closing. It now survives any change to spacing, flight length or the rush curve, and the failure
  it prevents is the sort this file keeps recording: an arithmetic coupling nobody restated.
- **Spacing, timing and lookahead are one equation with two degrees of freedom.** Stars 2.5× further
  apart with the arrival rhythm unchanged means a scroll 2.5× faster, and therefore 2.5× *less*
  course on screen ahead of the flyer: 162ms of visible warning where there had been 400. There is
  no third dial — pick two. That is what set the ceiling on `COURSE_SCREENS`: at 162ms the line
  arrives inside a hand's own reaction time, so it reads as unfair rather than fast, and 5.6 (295ms)
  is where it settled. Note which side of this the harness cannot see: every pilot in `TestStars`
  knows the whole course in advance and scores exactly the same at any lookahead at all, so
  `TestStars.lookahead` prints the number for a human to judge and guards a floor that was set from
  the device.
- **A knob documented as cosmetic has to be checked, not asserted.** `COURSE_SCREENS` was described
  in its own comment as leaving the rhythm of a course alone, and it did — but `encounterTime` took
  its lead term as `0.5 / COURSE_SCREENS`, so every arrival *time* moved with it, the sweep got
  sampled at different moments, and what came back was a different course. Dialling the spacing back
  with nothing else touched took the naive pilot from nineteen stars to thirteen, and the first
  instinct was to go hunting in the catch tolerance, which had nothing to do with it. `StarPath.LEAD`
  is frozen at 0.05 now. If a knob is meant to be cosmetic, find every expression it appears in
  before believing the comment; the ones that matter are the ones feeding a *time*.
- **Two knobs on the same speed multiply into a third nobody tuned.** The scroll here is
  `COURSE_SCREENS / FLY`, times the tail of the `RUSH` curve, times what a normalised ease-in
  borrows and pays back. Each of those was a defensible move; together they put the closing speed up
  by 60% and the timing window down by a third. Before turning the second dial, write down what the
  product does at the far end — the same trap the frenzy's flat multipliers were.
- **A retry that hands back the identical problem is a wall, not a retry.** The stars carry over
  between attempts, which is meant to mean a slower player gets there over a few stages. On the same
  line they do not: the harness pilot with quarter-second thumbs takes the same fifteen every time,
  and three of eight courses stalled at seventeen or nineteen for as many attempts as it was given.
  `StarPath.reroll` gives each attempt a fresh line and keeps the hand, which is what makes the
  carry-over a promise instead of a consolation. Note it also costs three RNG draws per interlude, so
  every generated word after the first one moves — expected, and the frame hashes show it.
- **A demo drawn onto a position snaps when the demo stops.** The ready lesson's lean was a sine
  added to the flyer's drawn x, so the first frame of the flight put it back at the middle from
  wherever the swing had got to — up to a tenth of the screen, instantly, and reported as the
  character jumping. Nothing was wrong with the state; the state had never moved. Anything animating
  a position for show has to be brought home before the thing owning that position takes over:
  `lessonFade` eases both the lean and the key glow out over the last half-second of the beat, so
  the lesson ends on a flyer standing still where it is about to fly from.
- **A pilot with perfect knowledge measures reachability, not difficulty.** `TestStars.flown` knows
  exactly where every checkpoint is and is limited only by a reaction quantum, so inside the
  trackable band it scores 20/20 at every setting and cannot tell you whether a course is *hard*.
  Use it for the two questions it can answer — can this course be completed at all, and can it be
  completed by nobody-at-the-controls — and take feel to the device. Since it does not brake and
  looks one checkpoint ahead, treat what it takes as a floor, and prefer *attempts to finish* over
  stars-per-attempt: the second is a six-sample coin flip and the first is the actual promise.
- **A four-sample binary is a coin flip, not an assertion.** `TestSoak` required all four runs of
  every tier to end in a death, and the quick tier — nine presses a second, one miss in fifty —
  reaches the cap alive on about one seed in twelve. Shortening the star flight by four tenths of a
  second was enough to turn that over, because it moves every RNG draw after the first interlude. It
  allows one survivor now. If a check is a yes/no over a handful of noisy runs, either state the
  tolerance or assert on the average instead.
- **Half a fifth of a curve is a straight line.** However swoopy a scrolling course is over its
  seconds, what reads is how much of it fits on screen: at six checkpoints it looked like a diagonal
  whatever the generator did. `COURSE_SCREENS` is what that knob is — 4.2 now, so just under five
  checkpoints are in shot — bounded below by the checkpoints touching each other and above by there
  being too few of them on screen to read a line from.
- **An effect wants the direction with room, not the honest one.** The flyer's wake should trail
  straight down: it is climbing a course that comes down at it. But it spends nearly all of a flight
  within a tile or two of the danger line, so a downward wake is a wake in the last second of the
  last attempt, and letting it spill over that line puts a spray of colour across the keys.
  `StarScreen.wake` trails the *steering* instead, which has the whole width to play with and draws
  what the thumbs are doing. Two more things fell out of it: it has to start clear of the flyer's own
  aura or half of it is hidden behind the character it comes off, and it is left out of the victory
  tableau entirely, because at full strength it fell straight down through the prize's name.
- **A scene with no sound reads as a scene the game stopped caring about.** Four moments used to
  pass in silence: a course leaving the line, the count at the end of one nobody won, the new
  collectible joining the parade, and the end of a run. Each is a *transition*, which is exactly
  where sound is easiest to forget and most missed. Two things to know if you add more. The moment
  matters as much as the effect — the run's full stop plays when the swirl clears, not on the fatal
  breach, because that frame already has the damage drip on it and two effects on one frame is one
  of them wasted. And a phase change has to be caught as a change: `StarPath.launched` and
  `reported` are set by comparing the phase predicates either side of the frame's own countdown,
  since nothing downstream can tell that the lesson has *just* ended.
- **The soak bot cannot steer, so it cannot tell you a course is playable.** `TestStars.flown` is a
  pilot with stated limits — it re-decides every *reaction* seconds and holds one thumb — and the
  same routine with steering off is the passenger the soak bot is. Three numbers come out: what quick
  thumbs take, what slow thumbs take, and what nobody-at-the-controls takes. The last one is the
  guard; see the pickup entry above for what it caught.
- **A beat that fires once can be too long to fire twenty times.** The fling stroke's slow-motion is
  0.28s, and reusing it for a star pickup would have put the end of a good course in continuous slow
  motion — and every beat lengthens the course in real time, since the course clock is scaled too.
  `GameCore.STAR_BEAT` is a quarter of it, and there is an assertion on what a clean run costs in
  real seconds. Before reusing an effect, count how often the new caller fires it.
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
- **A bonus larger than the scale it sits on erases the scale.** A frenzy used to add 2.6s of
  interlude on top of whatever the round earned — more than the whole 4s spread of the earned-mash
  ladder — so a hurt frenzy round out-paid a perfect calm one and the ladder communicated nothing.
  Gone. Before adding a modifier to a scored thing, check it against the *spread* of the thing it is
  modifying, not against zero.
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
- **Do not derive a cast list from another cast list.** The third figure in a story vignette
  was `PARTNER[PARTNER[i]]` at first, which looks clever and is wrong: most pairings here are
  mutual, so it handed back the entry itself and the scene drew one character twice. It read as
  a drawing bug. `Lore.THIRD` is an explicit table now, with an assertion that all three slots
  differ.
- **A new interlude phase silently zeroes every test that taps.** Adding the spinner made
  `tapBonus` a no-op for the first two seconds of BONUS, so suites that pressed straight after
  `advanceToBonus` scored nothing and still passed their own weaker checks. `advanceToMash`
  exists for that; prefer it whenever a test means to press.
- **A silhouette must be fully colourless.** The leaf and stem colours were left as
  themselves at first, so every blacked-out fruit had a bright green leaf on it and gave
  itself away.
- **A rule can be quietly carrying a job nobody wrote down.** Boss stages used to run a thin wave
  under the fight. Taking it away on request removed two things that were never stated: the only
  thing that could hurt you on four of the five bosses, and — since enraging worked by speeding the
  spawns up — the only reason to hurry. Boss-local mechanics and the belt-press charge replace them.
  Before deleting a subsystem, ask what else is leaning on it; the compiler cannot tell you that a mechanic
  was the load-bearing threat.
- **A mechanic paid for in another system's currency dies when that system does.** SUMO's swipes cost
  a charge earned by clearing words, so the moment boss stages stopped spawning words it could not be
  beaten at all. Charges come off its own belt now. Anything that spends a resource should earn it
  from inside the same fight.
- **An object created between layout passes has no position.** A glob is shed by a press, which has no
  `Layout`; the layout pass does not run until the next frame, so for one frame both the renderer and
  the hit-test read it at the origin — a red blob in the top-left corner every time the boss was hit.
  `Boss.seed` places it from the body's last known spot, and `place` is the single copy of that
  arithmetic so the two cannot disagree by a few pixels on the second frame.
- **A sustained force is not a repeated impulse.** Stretching the skin toward a dragged glob has to be
  a force inside the solver's substeps, not a velocity kick per frame — a per-frame kick pulls twice
  as hard at 120fps as at 60. And it has to be a *clamped target* rather than a range check: testing
  the raw target against the reach meant the tug switched off as soon as the glob got further than a
  radius away, so the skin twitched at the start of a drag and then let go, which is the opposite of
  the intended read. Scale it by the body radius like every other force there, or it dents a
  full-size boss and flings a thumbnail.
- **A health bar that goes up reads as cheating, not as difficulty.** The slime's globs used to crawl
  back and heal it. Watching the bar climb while playing correctly is not a hard fight, it is an
  unfair one — and it punished the player for having only so many fingers. Nothing heals now, and a
  window shutting mid-chain does not undo the presses that landed in it either.
- **When only one action scores, the other one is the fight.** A slime shed a glob per press, which
  made the chain the interesting half and the drag a chore repeated every second — five globs in the
  air with three slots to hold them. Five presses to work one loose, and the drag as the only thing
  that takes health off it, moves the whole fight into the drag and leaves the chain as what earns you
  something to fight with. Health is counted in globs carried off now, which is why the number looks
  so much smaller than the others: read `Boss.HP` against what one point of it actually costs.
- **A penalty for a mistimed press must not tax ordinary play.** The boss claimed its own letter
  whenever nothing was engaged, so starting a word that happened to begin with the drum's letter was
  read as a fumbled beat — and a landed rebuff resets that beat, so typing normally pushed the window
  away. It surfaced as the soak curve *inverting*: the quick tier finished below the steady one,
  because faster hands start more words and trip over that letter more often. `GameCore.bossClaims`
  now only hands a shut boss its letter when no word on the field wants it either; a deliberate early
  press is still punished, because then there is nothing else the press could have meant. If speed
  ever makes a tier do worse, look for a rule that is charging for a keystroke's *side effect*.
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
- **A set piece with no way past it needs the bot to be able to play it.** The boss on every fifth
  stage has to be beaten for the stage to end — there is deliberately no retreat. The soak bot could
  only type, and no boss can be beaten by typing alone, so all three tiers sat on stage 5 for the
  full fifteen minutes and *nobody died*: not a wall, a stalemate, and every difficulty assertion
  past stage 5 silently became a test of the first four stages. `Bot` taps, drags and shoves now,
  with its own stated limits for each (`DRAG_SPEED`, `PANIC_WARN`). It still never uses the blade or
  the panic swipe, which keeps those pessimistic. Before adding anything mandatory, ask what the
  harness's player does when it meets it.
- **Health times cycle length, never health.** The first pass at boss tuning set each one's hit count
  and its window rhythm separately, and `TRIPLETS` came out wanting six chords at one window per
  4.5s — 27 seconds of flawless play against a 22-second fuse, so a *perfect* run lost. Every boss's
  health is now set against one figure: a competent player finishes inside about fifteen of the
  `ENRAGE_AT` seconds before it turns nasty.
- **Two rules that each look right can deadlock.** An engaged word outranks the boss (the same rule
  the powerup has), and `TRIPLETS` wanted its chord inside one window. Together: whenever a window
  opened while a minion was part-typed, the chord could not be *started*, and the bot got eight
  windows and two chords out of a whole fight. The chord is timed from its own first press now
  (`CHORD_TIME`) and that boss is permanently open. When a mechanic will not fire, check whether two
  correct rules are excluding each other.
- **A predicate used for a hint must mean "this will work", not "this is mine".** `Boss.wants` was
  `open() && asksFor()`, and `asksFor` includes a triplets head that is awake but already struck —
  because pressing one is plainly aimed at the boss and should earn a rebuff rather than fall through
  to a word. But `wants` is what rings the key hint, so it advertised a key that could only be
  refused: the bot pressed it, was rebuffed, and pressed it again, losing ten chords out of ten. The
  claim and the invitation are two questions and need two methods.
- **Anything that lays out per frame must run above the early returns too.** `Boss.layoutElems`
  sat below the arrival-card return, so on the very frame the card ended — the first frame a tap is
  accepted — every element was still at the origin and a tap on one hit nothing. Same shape as the
  `warnLevel` bug, in a different method.
- **Resolve the kill before the side effects.** A `MAGPIE` killed by the final press still ran its
  "drop the key" step afterwards, putting an element on the field carrying key `-1` — an array index
  waiting to happen, and it happened on the first run of the assertions. Anything a hit triggers has
  to ask whether that hit was the last one.
- **A boss owns more than any other set piece, so its cleanup is bigger.** A body, a health bar, up
  to three draggable elements, a finger mid-drag, and one of the player's own keys held hostage.
  `Boss.leave()` clears every field it writes, not just the ones drawn today, and `TestBoss.cleanup`
  walks all five bosses through both exits — beaten, and the player dying mid-fight — then checks the
  frames *after* the death, since that is where the old versions of this bug were visible.
- **`strokePoly` double-blends at every vertex.** A translucent stroke round a 72-point soft-body
  outline comes out as a dotted line. Every other stroke in this game is a handful of points, so
  nothing met it before; `Slime`'s rim is opaque for that reason. The boss's open-window star met it
  too, at twenty vertices — layered *fills*, widest and faintest first, is how every halo in this
  game is built, and the burst had already learnt it.
- **"Always" is a promise about geometry, so only geometry can keep it.** The glob has to stay inside
  the slime however far it is dragged. The tug that stretches the skin is a spring balanced against
  the pressure and the pull toward round, so where it settles is wherever those three happen to
  agree — which looks right most of the time and lets go of the glob exactly when the drag is going
  well. `Softbody.enclose` projects five nodes past the glob every substep instead, and drops the
  inward part of their velocity as it does: without that, the stretched skin springs in against the
  constraint every substep and the two grind, storing energy that comes out as an explosion on
  release. If an effect has to hold *always*, a force will not do it.
- **Bound what a constraint has to cover, or it stops being a body.** Containment alone would make a
  glob dragged to the far wall a spike two-thirds of the way across the field. `Boss.updateFollow`
  walks the whole body after its own glob — a quarter of the way for free, and further only as far as
  it must for the stretch to still reach — so the constraint is never asked for more than a body's
  worth of tongue. Which is also the better read: the slime is hauled along after the piece being
  stolen, rather than standing still while it goes.
- **A measure that means one thing for a circle can mean nothing for an ellipse.** `Softbody.deform`
  was the spread of the node radii about their *mean*. The moment a body could rest as an ellipse, a
  settled slime read 0.24 of deform standing perfectly still — five times what a solid hit is — so it
  was drawn permanently whited-out at full rim weight, and the assertion that a hit deforms it was
  measuring the shape rather than the hit. It is measured against the rest radii now, so it is zero
  for anything settled whatever shape it settled into. When you generalise a shape, re-read every
  quantity derived from it.
- **A bound in units of the wrong axis is an effect that never happens.** The drag follow's reach was
  a multiple of the body radius, which on a body twice as wide as it is tall meant a sideways drag
  was handed to the follow while the glob was still deep inside the goo — the body walked the whole
  way and the skin had nothing left to do, which is the one thing the mechanic exists to show.
  `Softbody.restToward` gives the rest radius along the drag, and the reach is a multiple of that.
- **A frame-per-hit flag stops working the moment two can land together.** `Boss.update` returned
  a boolean meaning "the player takes a hit". A volley of three bolts launched on one frame arrives on
  one frame, so that flag would have charged one life for three. It returns a count now, and the
  bolts carry a stagger so they are spaced in the first place — the count is the correctness fix, the
  stagger is the design one.
- **Feedback has to exist wherever the health bar does not move.** Four presses out of every five at
  the slime take no health off it — they work a glob loose — so without the pip row under the body a
  correct run of presses looks like a run of presses that did nothing. Any mechanic with hidden
  progress needs somewhere to show it.
- **A layout offset read off a live body can be pushed anywhere.** The wanted-letter badge sits a
  fixed multiple of the body's height above it, and the whole header column is derived against the
  *resting* height. A body can be stretched taller than it rests — drag a glob at the ceiling and the
  goo follows it up — so an uncapped badge rides that straight through the blurb it is asserted to
  clear. `min(live, resting)` upward; free to follow a squash downward, since that only opens the gap.
- **A test that measures one thing has to defend the others.** `settleSlime` compares the boss's
  health with and without a drag. Adding a volley to the split made both runs end in death, so both
  returned zero and the comparison passed for the wrong reason — it was measuring survival, not
  health. It tops lives up every frame now. Any A/B on a long run wants the same treatment.
- **A preview that looks for state has to create it.** `Preview.bossFrames` looked for a draggable
  glob before pressing anything, which was fine while every press shed one. Five presses per glob and
  it silently found nothing, skipped three frames, and left the three stale PNGs from the last run
  sitting in `out/` — a green tick and yesterday's picture. Check the timestamps in the `out/` listing
  when a frame looks unchanged.
- Termux's `ecj` hardcodes `-7`; use `javac --release 8`. `aapt2 link` takes compiled
  resources positionally, not via `-R`.

## Licensing line

Characters, effects and music are all original and procedurally generated — no assets to
license. `res/raw/bgm.*` is gitignored: a user-supplied track stays on their device and must
never be committed, since this repo is shared.
