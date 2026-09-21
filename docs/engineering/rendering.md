# Rendering and layout

Historical failure notes, moved from `AGENTS.md`. Read only entries relevant to the task.
Values and file ownership may have changed; verify against current code. Current workflow
policy lives in [AGENTS.md](../../AGENTS.md).

- **Insets lie.** In immersive mode `padT` is 0 and this device reports no cutout inset, so
  a centred HUD lands under the punch-hole camera. `Layout.topSafe` keeps its own floor.
  There is a regression assertion at 1080x2400.

- **Position-driven animation, not time-driven,** for anything tied to where a thing is. A
  timed entrance ramp finished while the word was still off-screen.

- **Translucent overlapping primitives double-blend.** Build a stadium as one polygon, not a
  rect plus two circles, or the caps come out darker.

- **The harness font is an ASCII subset.** `tools/Font` has one bitmap per character it knows;
  a glyph it does not have simply vanishes from the PNG, so text using one looks right on the
  device and is missing a letter in every frame you check. `?` and `'` were both added to it
  before they could be used, and `:` for COLLECTIONS. Add the glyph rather than writing around it —
  but do check, since the set is still small. Arrows and chevrons are drawn as polygons for the same reason.

- **Text that does not fit is reported, not eyeballed.** `RasterPainter.text` records any line
  drawn off the screen edge and `Preview` prints `DOES NOT FIT` under the frame that did it, with
  the width and the x range. It respects the current clip, because the display case deliberately
  draws a wing tile past the plaque for the clip to cut off. Since the harness font is wider than
  Quicksand, a line that fits in a PNG fits on the device — so this is the whole check. Check the selected preview output for `DOES NOT FIT` after text or layout changes.

- **`Painter` cannot clip to a shape,** only to a rectangle. That is why a banded finish in
  `Trinket` is fitted to an ellipse and why `Collect.banded` restricts which shapes may wear
  one — there is an assertion holding the catalogue to it.

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

- **A readout that outlives what it reports needs its own copy of the numbers.** `Hud.sliceCall`
  read `strokeKills` live, which was fine while a stroke could only end by lifting; once strokes end
  on their own, a fresh one starting inside the readout's 1.1s rewrote "4 IN ONE!" down to the new
  tally and then blanked it. `callKills`/`callCuts` are frozen when the announcing stroke ends.

- **A permanent warning is not a warning.** `harm()` was linear, so losing one life of three put
  a third-strength pulsing red border round the screen for the rest of the run — reported twice as
  a "stuck vignette" and it was not stuck, it was working as written. It is squared now: one life
  lost is barely visible, the last life is unmistakable. Before hunting a stuck effect, check
  whether something is simply *meant* to stay on and mistuned.

- **A demo drawn onto a position snaps when the demo stops.** The ready lesson's lean was a sine
  added to the flyer's drawn x, so the first frame of the flight put it back at the middle from
  wherever the swing had got to — up to a tenth of the screen, instantly, and reported as the
  character jumping. Nothing was wrong with the state; the state had never moved. Anything animating
  a position for show has to be brought home before the thing owning that position takes over:
  `lessonFade` eases both the lean and the key glow out over the last half-second of the beat, so
  the lesson ends on a flyer standing still where it is about to fly from.

- **An effect wants the direction with room, not the honest one.** The flyer's wake should trail
  straight down: it is climbing a course that comes down at it. But it spends nearly all of a flight
  within a tile or two of the danger line, so a downward wake is a wake in the last second of the
  last attempt, and letting it spill over that line puts a spray of colour across the keys.
  `StarScreen.wake` trails the *steering* instead, which has the whole width to play with and draws
  what the thumbs are doing. Two more things fell out of it: it has to start clear of the flyer's own
  aura or half of it is hidden behind the character it comes off, and it is left out of the victory
  tableau entirely, because at full strength it fell straight down through the prize's name.

- **Do not derive a cast list from another cast list.** The third figure in a story vignette
  was `PARTNER[PARTNER[i]]` at first, which looks clever and is wrong: most pairings here are
  mutual, so it handed back the entry itself and the scene drew one character twice. It read as
  a drawing bug. `Lore.THIRD` is an explicit table now, with an assertion that all three slots
  differ.

- **A silhouette must be fully colourless.** The leaf and stem colours were left as
  themselves at first, so every blacked-out fruit had a bright green leaf on it and gave
  itself away.

- **`strokePoly` double-blends at every vertex.** A translucent stroke round a 72-point soft-body
  outline comes out as a dotted line. Every other stroke in this game is a handful of points, so
  nothing met it before; `Slime`'s rim is opaque for that reason. The boss's open-window star met it
  too, at twenty vertices — layered *fills*, widest and faintest first, is how every halo in this
  game is built, and the burst had already learnt it.

- **A layout offset read off a live body can be pushed anywhere.** The wanted-letter badge sits a
  fixed multiple of the body's height above it, and the whole header column is derived against the
  *resting* height. A body can be stretched taller than it rests — drag a glob at the ceiling and the
  goo follows it up — so an uncapped badge rides that straight through the blurb it is asserted to
  clear. `min(live, resting)` upward; free to follow a squash downward, since that only opens the gap.

- **A preview that looks for state has to create it.** `Preview.bossFrames` looked for a draggable
  glob before pressing anything, which was fine while every press shed one. Five presses per glob and
  it silently found nothing, skipped three frames, and left the three stale PNGs from the last run
  sitting in `out/` — a green tick and yesterday's picture. Check the timestamps in the `out/` listing
  when a frame looks unchanged.

## Deterministic comparison

For a rendering-sensitive refactor expected to preserve visuals, compare only affected
frames before and after. Do not run full exports for an ordinary nonvisual edit.
Save checksums outside `out/` (for example under `$TMPDIR`), rerun the same frame selection,
and compare. Add affected WAVs only for audio work. Fixed seeds make the comparison useful;
changed RNG draw order can intentionally change later frames.
