# Working on Hexatype

Read this before touching anything. It exists to save you the discoveries that cost time
the first time round.

[GLOSSARY.md](GLOSSARY.md) names every game element and maps the plain-English term to the
code. Use those names back to the user; several differ from the identifiers (a falling word is
`Enemy`, the frenzy is `mode`, the interlude is `BONUS`), and getting them wrong wastes a round
trip working out which thing was meant.

## The one thing that matters most

**You can see and hear this game without building or installing it.** `./check.sh` runs the
whole thing headlessly: ~985 rule assertions, then it renders real frames to `out/*.png` and
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
| `Collect` | the thirty collectibles: catalogue, blind-box odds, owned-set bitmask |
| `Trinket` | draws a collectible — fifteen shapes crossed with nine finishes |
| `Cabinet` | the glass case itself: a wireframe box three-quarters on |
| `Showcase` | the display case: badge, shelf, position bar, and every touch target on it |
| `Lore` | a story per collectible, plus who is cast in its vignette |
| `Parade` | the collection marching in, the new one joining, the line marching off |
| `Storybook` | the story popup and its ten looping vignettes |
| `Power` | the powerup letter and its three modes |
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
- Termux's `ecj` hardcodes `-7`; use `javac --release 8`. `aapt2 link` takes compiled
  resources positionally, not via `-R`.

## Licensing line

Characters, effects and music are all original and procedurally generated — no assets to
license. `res/raw/bgm.*` is gitignored: a user-supplied track stays on their device and must
never be committed, since this repo is shared.
