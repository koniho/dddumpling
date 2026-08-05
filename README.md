# Hexatype

A kawaii typing-attack game for Android, built entirely on-device in Termux — no Gradle, no
Android Studio, no PC.

Cute words fall from the sky. Each "letter" is one of six characters — dumpling, strawberry,
cat, grapes, squishy, blob — with its own colour and face. You type by tapping the matching
hexagon in the honeycomb deck at the bottom: three keys under the left thumb, three under the
right. Clear a word before it reaches the danger line, or it lunges and costs you a life.

Between stages a dim sum steamer arrives and spins for two of your six keys; alternate them to
prise the lid off. The dumpling inside is a blind box — freeing it hands over one of thirty
collectible squishies, from commons up to a single gold grail. The collection is kept for good
and shown in the display case on the title screen, where the outer two keys scroll the shelf
and the inner four start a run. Anything you have not won yet is a silhouette behind a question
mark; tap one you have and it tells you where it lives and what its family gets up to. Win one
and the collection parades: they march in from the left, the newcomer drops into the end of the
line, and the next stage waits until they have all marched off to the right.

## Where to look

Three documents, and they do not overlap. Read the one that answers your question:

| Question | Document |
| --- | --- |
| What is this thing called in the code? | [GLOSSARY.md](GLOSSARY.md) |
| How do I work on it without breaking it? | [CLAUDE.md](CLAUDE.md) |
| How do I build, run or check it? | this file |

`GLOSSARY.md` exists because several plain-English names differ from the identifiers — a
falling word is an `Enemy`, the frenzy is `mode`, the interlude is `BONUS`. `CLAUDE.md` carries
the conventions and, more usefully, the traps that have already cost time.

## The fast loop

**You can see and hear this game without building or installing it.** No SDK, no device:

```sh
./check.sh              # rule assertions, then frame renders at 640x1400
./check.sh 1080 2400 2  # a specific screen size and supersampling factor
```

It does two things:

1. **Rule assertions** (`tools/CoreTest.java`) — 784 of them across eleven suites: layout
   geometry, hit-testing, targeting, scoring, stage pacing, the interlude's phases and both paths through it, the
   collectible catalogue and its odds, the stories, plus a two-minute perfect-play run and a
   ten-minute random-input fuzz.
2. **Frame renders** (`tools/Preview.java`) — drives the real state machine into every
   interesting state and writes each to `out/`. Numbered files are single frames; the `0-`
   files are review sheets that show a whole set at once:

   | Sheet | Shows |
   | --- | --- |
   | `0-characters.png` | the six letters at display and tile size, plain and struck |
   | `0-collect.png` | all thirty collectibles as collected |
   | `0-collect-unknown.png` | the same thirty as unknown silhouettes |
   | `0-skits.png` | the ten stage vignettes, sampled through each |
   | `0-beats.png` | the ten story vignettes, sampled through each |

   Sounds go to `out/sfx/*.wav` — every effect and every music loop.

Reading the PNGs is the point: that loop is seconds, and it needs no device.

This works because all logic and all drawing are pure Java behind the
[`Painter`](src/com/sram/hexatype/Painter.java) interface. The APK implements it with
`android.graphics.Canvas`; `tools/RasterPainter` implements it with a software rasterizer.
**One render path, two backends** — so a PNG from the harness is what the phone draws. Renders
are deterministic (fixed RNG seeds, no wall clock), so a change that should not alter them can
be proved not to; `CLAUDE.md` has the recipe.

## Requirements

Termux on an `aarch64` Android device:

```sh
pkg install openjdk-17 aapt2 d8 apksigner zip
```

Plus `android.jar` to compile against, which is not packaged. Fetch it once:

```sh
mkdir -p sdk
curl -o /tmp/platform35.zip \
    https://dl.google.com/android/repository/platform-35_r02.zip
unzip -o -j /tmp/platform35.zip 'android-35/android.jar' -d sdk
rm /tmp/platform35.zip
```

That is a ~64 MB download for a 27 MB jar; it is deliberately not committed.

> Termux ships an `ecj` wrapper, but it hardcodes `-7` and a nonexistent classpath, so the
> build uses `javac --release 8` instead.

## Build

```sh
./build.sh
```

The pipeline is `aapt2 compile` → `aapt2 link` → `javac` → `d8` → `zip` → `apksigner`, and
takes a few seconds. It runs `check.sh` first and refuses to package if any assertion fails.

Output is a signed `hexatype.apk` of about 260 KB: a 130 KB dex and the 125 KB bundled
Quicksand face. A personal music track dropped into `res/raw/` is added to that and will
dominate it — see [Licensing](#licensing).

A debug keystore is generated at `build/debug.keystore` on first run. It is throwaway — delete
it and a new one appears.

## Install

```sh
termux-open hexatype.apk
```

This hands the APK to Android's package installer via Termux's FileProvider. Two one-time
prerequisites:

- Allow "install unknown apps" for Termux.
- Set `allow-external-apps = true` in `~/.termux/termux.properties`, then run
  `termux-reload-settings`. Without it Termux's content provider refuses to hand the file over
  and **nothing at all happens** — no dialog, no error. The reason only shows up in `logcat`
  under Termux's own UID.

## Unattended install

`./deploy.sh` builds, installs and launches in one step. With adb connected it needs no taps at
all; otherwise it falls back to the installer dialog.

Termux cannot install packages itself — that needs a privileged permission — so adb is the only
way to skip the dialog. It can talk to the very device it is running on over Wireless
debugging:

1. Settings → Developer options → enable **Wireless debugging**.
2. Tap **Pair device with pairing code**. Note the `IP:PORT` and the six-digit code.
3. `adb pair <ip>:<pair-port>` and enter the code.
4. `adb connect <ip>:<port>` — the port on the main Wireless debugging screen, which is
   *different* from the pairing port.
5. `adb devices` should list one `device`.

Pairing persists, but the port changes when Wireless debugging is toggled or the device
reboots, so step 4 may need repeating. After that `./deploy.sh` is fully unattended, and
`adb install -r` preserves the best score, the settings and the collection.

Notifications are optional: `deploy.sh` calls `termux-notification` if the Termux:API app is
installed and silently skips it otherwise. Termux add-ons must come from the same source as
Termux itself — mixing F-Droid and GitHub builds fails with a signature mismatch.

**You cannot see this app's crashes.** Termux's `logcat` only shows its own UID, and there is
no `dumpsys` here, so [`Crash.java`](src/com/sram/hexatype/Crash.java) renders the stack trace
on screen instead. If it crashes, read that screen.

## Layout of the code

Everything in the first group is pure Java — no `android.*` import anywhere in it — which is
what lets the harness compile and drive it. `check.sh` lists that set as `PURE`; a new pure
file has to be added there. `build.sh` globs `src/`, so it needs no updating.

| Pure | Holds |
| --- | --- |
| `Glyph` | the six-letter palette, hexagon geometry, hue cycling |
| `Kawaii` | the six characters and their faces, plus the mood dumpling |
| `Layout` | every screen coordinate, derived from view size and insets |
| `GameCore` | all rules: state machine, waves, targeting, scoring, powerup, collection |
| `Words` | word generation and the press-budget rules |
| `Fx` | shots and particles |
| `Power` | the powerup letter and its four frenzy modes |
| `Buddy` | the squishy that fights during TEAM SQUISH |
| `Steamer` | interlude state, and the spinner that picks its key pair |
| `Collect` | the thirty collectibles: catalogue, blind-box odds, owned-set bitmask |
| `Lore` | a story per collectible, and who is cast in its vignette |
| `Painter` | the drawing interface |
| `Draw` | palette and shared geometry — every renderer extends it |
| `Sky` | background, clouds, vignette, HUD band |
| `Renderer` | frame orchestration and the play field |
| `Hud` | score, stage, lives, frenzy bar, banners |
| `Screens` | title, game over, interlude, settings |
| `Skits` | the ten stage-banner vignettes |
| `Basket` | the steamer, drawn in three-quarter view |
| `Parade` | the collection marching in, the new one joining, the line marching off |
| `Trinket` / `Shape` / `Finish` | a collectible: face, one of fifteen bodies, one of nine surfaces |
| `Showcase` | the display case on the title screen |
| `Storybook` | the story popup and its ten looping vignettes |
| `SettingsUi` | settings-panel geometry and hit-testing |
| `Sfx` / `Music` | procedurally synthesised effects and looping tracks |

| Android-only | Holds |
| --- | --- |
| `MainActivity` | fullscreen setup and `SharedPreferences` persistence |
| `GameView` | touch, window insets, the frame loop |
| `CanvasPainter` | `Painter` → `android.graphics.Canvas` |
| `Audio` | `AudioTrack` playback of the synthesised sound |
| `Crash` | draws a stack trace on screen, since logcat is unavailable |

| Harness only, in `tools/` | Holds |
| --- | --- |
| `Check` | the tally, assertion helpers, simulation drivers, `Store`/`Sound` stubs |
| `CoreTest` | runs every `Test*` suite |
| `Test*` | the suites: rules, words, stages, visuals, audio, power, collect, lore, soak |
| `Preview` | drives the state machine and writes the frames and sheets |
| `RasterPainter` | `Painter` → `int[]` framebuffer |
| `Font` | 5x7 bitmap font — **ASCII subset only**, see below |
| `Png` / `Wav` | minimal writers |

`tools/` shares the `com.sram.hexatype` package so it can reach package-private state, but it
is compiled separately and is not in `build.sh`'s source list.

One thing worth knowing before you write any on-screen text: `tools/Font` has a bitmap per
character it knows and silently draws nothing for anything else. Text using a character it
lacks looks right on the device and is missing a letter in every PNG you check. Add the glyph
to `Font` rather than working around it.

## Tuning

Stage pacing is one method per dial in `GameCore`, so difficulty is a numbers change rather
than a rewrite. All of them take the player's speed setting into account where it applies.

| Method or constant | Controls |
| --- | --- |
| `travelSeconds()` | how long a word takes to fall to the danger line |
| `spawnInterval()` | seconds between spawns |
| `maxEnemies()` / `crowdCap()` | words on screen at once, and the frenzy multiple of it |
| `minWordLen()` / `maxWordLen()` | word length range |
| `stageQuota()` | words released per stage |
| `stackChance()` | odds a tile needs more than one press |
| `START_LIVES`, `STEAMER_HITS` | lives, and presses needed to free the dumpling |
| `BONUS_ROLL`, `BONUS_TIME`, `BONUS_HOLD`, `BONUS_STATUS` | the interlude's four phases |
| `Steamer.FREE_TIME`, `PARADE_TIME` | the two segments of a win: the escape, then the parade |
| `Parade.IN_END`, `JOIN_END` | how the parade splits into marching in, joining, marching off |
| `BLADE`, `SLOW_KILLS`, `SLOW_TIME`, `SLOW_RATE` | how wide FLING cuts, and the slow-motion beat |
| `CHAIN_STEP`, `CHAIN_TIME`, `CHAIN_REVEAL` | what a MULTI hop pays, and how the chain plays back |
| `Buddy.SPEED`, `GROW`, `CHARGE_RATE` | how the TEAM SQUISH squishy moves and grows |
| `Collect.TIER_WEIGHT` | blind-box rarity odds |

Several constants are at their value because the obvious value was wrong, and the comment says
so. Read it before changing one.

To add a seventh letter: a colour in `Glyph.COLOR`, a case in `Kawaii.draw`, and a wider key
layout in `Layout.compute` — `Glyph.COUNT` drives the rest.

To add a collectible: one row across the parallel arrays in `Collect`, one in `Lore`, and bump
`Collect.COUNT`. The assertions will tell you what you missed.

## Licensing

Characters, effects, music, collectibles and stories are all original and procedurally
generated — there are no assets to license. The bundled font is
[Quicksand](https://fonts.google.com/specimen/Quicksand) under the SIL Open Font License, whose
text ships alongside it in `assets/fonts/OFL.txt`.

`res/raw/bgm.*` is gitignored on purpose: a user-supplied track stays on that device and must
never be committed, since this repo is shared. Drop one in and it becomes the MY TRACK option
in settings — and the default, since `Music.defaultChoice` prefers it whenever the file is
present. Pick something else in settings and that choice sticks.
