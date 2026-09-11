# Onboarding: get productive on DDDUMPLING in ten minutes

You are an agent that has just been dropped into this repository. This file is the shortest path
from nothing to a verified change. It does not repeat the other documents — it tells you what to do,
in order, and where to look when you need more.

> **The single most important fact:** you can see and hear this game, and have it played for you, in
> seconds, without a device, an emulator, or an Android SDK. If you find yourself reasoning about
> what the game looks like or how hard it is, stop — run the harness and find out.

## The four documents

Read them in this order, and only as far as you need:

| Document | Read it when |
| --- | --- |
| **this file** | now, all of it — it is short |
| [CLAUDE.md](CLAUDE.md) | before you touch code. The "Traps that have already bitten" section is the accumulated cost of every mistake made here. It is long because it is worth it |
| [GLOSSARY.md](GLOSSARY.md) | when the user names a thing you cannot find. Several plain-English names differ from the identifiers — a falling word is `Enemy`, the frenzy is `mode`, the interlude is `BONUS` |
| [README.md](README.md) | for build, install and deploy mechanics |

## Minute 0: prove the loop works

```sh
./check.sh
```

That is the whole development loop. It takes seconds, needs no SDK and no device, and does two
things:

1. **Runs every rule assertion** — around 1,360 of them across a dozen suites, ending in
   `N passed, 0 failed`. If that number is not `0 failed` before you have changed anything, stop and
   say so; you are not looking at a clean tree.
2. **Renders real frames** to `out/*.png` and every sound to `out/sfx/*.wav`.

Then actually look at one:

```
Read out/1-title.png
```

Use the `Read` tool on the PNGs. This is not optional colour — it is the primary way visual work is
checked here, and a change nobody looked at is a change nobody verified. The `0-*.png` files are
review sheets that show a whole set at once (all six letters, all thirty collectibles, both vignette
casts).

## Minute 2: understand the one architectural rule

**All logic and all drawing are pure Java behind the [`Painter`](src/com/dddumpling/game/Painter.java)
interface.** The APK implements `Painter` with `android.graphics.Canvas`; the harness implements it
with a software rasterizer. One render path, two backends — so a PNG from the harness is what the
phone draws.

Consequences you must respect:

- **Never put an `android.*` import in a pure file.** If you do, the harness stops compiling and you
  lose your eyes. The pure set is listed as `PURE` in `check.sh`; a new pure file has to be added
  there by hand. `build.sh` globs `src/`, so it needs no updating.
- Audio and persistence reach the rules through the `Sound` and `Store` interfaces, which is also
  how tests assert which effect fires when.
- Rendering is **deterministic** — fixed seeds, no wall clock. Exploit that (see below).

## Minute 3: the trick that catches what assertions miss

Preview output is a pure function of state. So for any change that *should not* alter rendering — a
refactor, a rename, an extraction — prove it:

```sh
./check.sh && md5sum out/*.png out/sfx/*.wav > /tmp/before.txt
# ...make the change...
./check.sh && md5sum out/*.png out/sfx/*.wav > /tmp/after.txt
diff /tmp/before.txt /tmp/after.txt   # must be empty
```

The corollary matters just as much: any *intentional* visual or audio change **will** alter those
hashes. That is fine — just know which of the two you are doing before you run it.

This is also why "random" motion cannot use an RNG or a wall clock anywhere in the draw path. Use
sines at hashed rates and phases off the passed-in clock; `Cabinet.shimmer` and `Softbody`'s breath
are the patterns to copy.

## Minute 5: let the bot answer difficulty questions

`tools/Bot.java` is a player with **stated limits** — presses a second, a beat to find the next
word, a miss rate, and a drag speed. `TestSoak.boundedPlay` runs three tiers of it and asserts where
the curve stops them:

```
casual  4/s react 0.30s miss 8%  ->  stage 9.3 after 222s, 4 of 4 died
steady  6/s react 0.20s miss 4%  ->  stage 20.0 after 492s, 4 of 4 died
quick   9/s react 0.13s miss 2%  ->  stage 21.0 after 520s, 4 of 4 died
```

**Use this for any tuning change.** The perfect-play soak beside it presses thirty times a second
and never misses, so it can only tell you the game is winnable by a god; every difficulty question
that has actually gone wrong here went wrong for *hands*.

Two things to know before trusting it: it never uses the FLING blade and never spends the panic
swipe, which is the pessimistic reading and the useful one for a floor. It *does* tap, drag and
shove a boss, because a boss has to be beaten for its stage to end — a bot that could only type
would measure a stalemate rather than a difficulty curve.

## Minute 7: the conventions that will be checked

- **Every rule change gets an assertion** in the matching `Test*` suite. Every new visual state gets
  a frame in `tools/Preview.java` so it can be looked at.
- **Comments are terse and explain *why*.** One or two lines. Many constants are at their value
  because the obvious value was wrong, and the comment says so — but as a clause, not a paragraph.
  Keep the fact, drop the essay: no restating the code, no narrating the debugging. Every comment
  line is one every future reader pays for, and reading is what costs on this repo.
- The `Test*` classes extend `Check` and the renderers extend `Draw` **so helpers and colours resolve
  unqualified.** That is deliberate. Follow the pattern rather than "fixing" it.
- Files over ~350 lines want splitting, and the seam is `Fx`'s: statics taking `GameCore c` that
  work on its fields rather than owning them. `Pacing`, `Blade`, `CaseUi`, `Interlude` and `BossPlay`
  all came out of `GameCore` that way. What must *not* be split is `GameCore.update` and `tapKey` —
  their ordering is load-bearing.
- Prefer the plain-English names from `GLOSSARY.md` when talking to the user.

## The five traps that catch people first

The full list is in `CLAUDE.md` and you should read it. These five are the ones that bite soonest:

1. **Reset state above the early returns in `update()`.** A death never reaches the `PLAY` half of
   the loop, so anything a set piece owns must be cleared where the death happens — `GameCore.die()`
   — or it stays on screen over the swirl, the summary and the title screen. This has bitten four
   times: a stuck edge glow, the TEAM SQUISH squishy, a lit blade, and a boss's held key.
2. **The harness font is an ASCII subset.** `tools/Font` draws nothing at all for a character it
   does not know, so text using one looks right on the device and is silently missing a letter in
   every frame you check. Add the glyph, or use a polygon (which is why every arrow and chevron here
   is drawn, not typed).
3. **Text that does not fit is reported, not eyeballed.** Run
   `./check.sh | grep -B1 'DOES NOT FIT'` after any text or layout change. But note what it cannot
   see: **text on top of other text**, since both lines fit fine. Where lines stack, expose the
   offsets as named methods and assert the clearance across a sweep of sizes —
   `TestVisuals.hudStacking` and `TestBoss.stacking` are the patterns.
4. **A global text scale needs the leading scaled with it.** Every size goes through `Draw.type()`
   (`TEXT`, currently 1.34). Sizes scale; plain `unit` gaps between stacked lines do not. Wrap the
   offset in `type()` too.
5. **RNG call order is load-bearing.** `Words.fill` consumes the RNG in a fixed order; change it and
   every generated word shifts. Anything that adds a draw earlier in a frame moves every word after
   it — expected, and the frame hashes will show it.

## Where things live

Start from the table in `CLAUDE.md`, which lists every file and what it holds. The shape to have in
your head:

- `GameCore` — all the rules: state machine, waves, targeting, scoring, and the seams to audio and
  storage. Free of `android.*`, always.
- `Layout` — every screen coordinate, derived from view size and insets.
- `Renderer` / `Hud` / `Screens` — frame orchestration, the readouts, the full-screen states.
- One pair of files per set piece: state and rules in one, drawing in the other. `Steamer`/`Basket`,
  `StarPath`/`StarScreen`, `Boss`/`BossScreen`, `Softbody`/`Slime`. Copy this split for anything new.
- Android-only: `MainActivity`, `GameView` (touch and the frame loop), `CanvasPainter`, `Audio`,
  `Crash`.

## Gestures: the constraint that shapes all input

**A drag cannot start on a key.** Telling a drag from a tap means holding the tap back until the drag
is ruled out, and every tap in play is a keystroke — that latency is unaffordable. So:

- The panic swipe starts in the lower half of the field (`Layout.inPushZone`), which is outside every
  key hex.
- Boss elements live in the *upper* field, where nothing else claims a touch, which is what lets a
  drag on one commit on the frame the finger lands.
- Outside play the constraint lifts: the display case holds its tap until the finger lifts, because a
  touch on the title screen is browsing rather than a keystroke.

Any new gesture in play has to answer this question before anything else.

## Building and deploying

```sh
./check.sh              # rules + frames, no SDK needed
./check.sh -q           # failures, diagnostics and the tally only — use this by default
./check.sh -q -r        # rules only, no frames. Seconds.
./check.sh -q -s Boss   # one suite
./check.sh -q -f 60 -c 0,.1,1,.45   # one frame, cropped. Cheaper to look at than a whole screen.
./check.sh 1080 2400 2  # render at real device size; use when checking layout
./build.sh              # gated on check.sh; produces a signed hexatype.apk
./deploy.sh             # build + install + launch
```

`build.sh` refuses to package if any assertion fails. `sdk/android.jar` is not committed — see
README.md for the one-time fetch. The user's standing preference is to **run `./deploy.sh` when work
is done**, without asking.

## When it crashes on device

**You cannot see this app's crashes.** Termux's logcat only shows its own UID, and there is no adb or
dumpsys here. That is why [`Crash.java`](src/com/dddumpling/game/Crash.java) renders the stack trace on
screen. If the user reports a crash, ask them to read that screen.

## Working alongside other agents

This repo is often worked by more than one agent at once. Two things follow:

- `check.sh` begins with `rm -rf build/harness`, so two concurrent runs clobber each other and
  produce nonsense errors like `NoClassDefFoundError: Kawaii`. If you see that, you are racing
  someone; re-run, or compile to a private output directory.
- Prefer a git worktree for anything substantial, and say which files you are editing.
