# DDDUMPLING

A kawaii typing-attack game for Android, built entirely on-device in Termux — no Gradle, no
Android Studio, no PC.

A native iPhone port shares the Java gameplay and renderer through J2ObjC. See
[iOS build and simulator instructions](ios/README.md) and the
[port's feature/validation inventory](ios/docs/parity.md).

> The game and launcher name is DDDUMPLING; the Android package is `com.dddumpling.game`.
> This pre-store package change installs as a separate app from earlier development builds,
> with separate scores and collections. The build output is still named `hexatype.apk`.

Cute words fall from the sky. Each "letter" is one of six characters — dumpling, strawberry,
cat, grapes, squishy, blob — with its own colour and face. You type by tapping the matching
hexagon in the honeycomb deck at the bottom: three keys under the left thumb, three under the
right. Clear a word before it reaches the danger line, or it lunges and costs you a life. Once a stage, when something is bearing down on the line, you can swipe up out
of the strip just below it to shove the whole bottom half of the field back — a lunge already
committed included.

Between stages a dim sum steamer arrives and spins for two of your six keys; alternate them to
prise the lid off. The dumpling inside is a blind box — freeing it hands over one of thirty
collectible squishies, from commons up to a single gold grail. The collection is kept for good
and shown in the display case: a shimmering glass cabinet that opens from the small case on the title screen.
Pan freely in any direction through the animated collectibles, without snapping to cells.
Fruits share one row, candies share another, and defeated bosses join their own row.
Each boss victory awards its matching miniature character and replaces the next minigame with
a confetti-filled welcome scene; repeated victories pay a duplicate bonus. The glass catches the light as you move.
Navigation ticks as the highlighted tile changes; its character grows, bounces, and sparkles.
The highlighted character shows how many times it has been collected, including duplicates.
Counts persist across runs for every reward source. Older saves begin at one per owned character,
since past duplicates cannot be reconstructed. Clearing the collection also clears these counts.
Tap a visible tile to pan it into the center, then tap it again for its story;
every key starts a run instead, and puts the case away first if it is open. Anything you have not
won yet is a silhouette behind a question
mark; tap one you have and it tells you where it lives and what its family gets up to. Win one
and the collection parades: they march in from the left, the newcomer drops into the end of the
line, and the next stage waits until they have all marched off to the right.

Boss fights appear at stages 5, 10, 15, and 20: Slime, Dark Divide, Octopulse, and Fly Agaric.
Each combines key defense with a physical gesture, using soft bodies that wobble and deform.
A victory unlocks that boss's miniature friend in the display case and celebrates the new
character before moving straight to the next stage.

## Where to look

Four documents, and they do not overlap. Read the one that answers your question:

| Question | Document |
| --- | --- |
| I am new here, what do I do first? | [ONBOARDING.md](ONBOARDING.md) |
| What is this thing called in the code? | [GLOSSARY.md](GLOSSARY.md) |
| How do I work on it without breaking it? | [CLAUDE.md](CLAUDE.md) |
| How do I build, run or check it? | this file |

`GLOSSARY.md` exists because several plain-English names differ from the identifiers — a
falling word is an `Enemy`, the frenzy is `mode`, the interlude is `BONUS`. `CLAUDE.md` carries
the conventions and, more usefully, the traps that have already cost time.

## The fast loop

**You can see and hear this game without building or installing it.** No SDK, no device:

```sh
./check.sh                    # rule assertions, then frame renders at 640x1400
./check.sh -q                 # failures, diagnostics and the tally only
./check.sh -q -r              # rules only, no frames. Seconds.
./check.sh -q -s Boss         # one suite
./check.sh -q -f 60,65        # only these frames, skipping the sheets and the WAVs
./check.sh -q -f 60 -c 0,.1,1,.45   # ...cropped to that box, in 0..1 fractions
./check.sh 1080 2400 2        # a specific screen size and supersampling factor
```

It does two things:

1. **Rule assertions** (`tools/CoreTest.java`) — ~1480 across thirteen suites: layout
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
[`Painter`](src/com/dddumpling/game/Painter.java) interface. The APK implements it with
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

Output is a signed `hexatype.apk` with bundled fonts. A personal music track dropped into
`res/raw/` is added to that and will dominate it — see [Licensing](#licensing).

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
no `dumpsys` here, so [`Crash.java`](src/com/dddumpling/game/Crash.java) renders the stack trace
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
| `Pacing` | the stage difficulty dials, as pure functions of stage |
| `EnemyEntry` | side-entry arcs that ease into vertical lanes; checks future row spacing before spawning |
| `Blade` | the FLING swipe: what a stroke is and what one sweep cuts |
| `CaseUi` | browsing the display case |
| `Interlude` | the between-stages round |
| `BossPlay` | the boss fight wired to score, sound, shots and lives |
| `Words` | word generation and the press-budget rules |
| `Fx` | shots and particles |
| `Power` | powerup modes, stage-scaled spawn rates, and bounded replenishment after fast clears |
| `Boss` | the every-fifth-stage boss: five mechanics, its touch elements, and its rules |
| `BossScreen` | the boss on screen: body, health header, ornaments and elements |
| `Softbody` | a pressurised 2D soft body — the sprung node ring every boss is built on |
| `Slime` | draws a soft body as gooey translucent slime with a face |
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
| `Cabinet` | the glass case itself: a wireframe box three-quarters on |
| `Showcase` | the display case: badge, shelf, position bar, and every touch target on it |
| `Storybook` | the story popup and its ten looping vignettes |
| `SettingsUi` | settings-panel geometry and hit-testing |
| `Sfx` / `Music` | procedurally synthesised effects and looping tracks |
| `Narration` | what the story popup says out loud, and the pitch and pace of each line |

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

`tools/` shares the `com.dddumpling.game` package so it can reach package-private state, but it
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
| `PUSH_LIFT`, `PUSH_TIME` | how far the panic swipe shoves, and how long its wave shows |
| `Steamer.FREE_TIME`, `PARADE_TIME` | the two segments of a win: the escape, then the parade |
| `Parade.IN_END`, `JOIN_END` | how the parade splits into marching in, joining, marching off |
| `BLADE`, `SLOW_KILLS`, `SLOW_TIME`, `SLOW_RATE` | how wide FLING cuts, and the slow-motion beat |
| `CHAIN_STEP`, `CHAIN_TIME`, `CHAIN_REVEAL` | what a MULTI hop pays, and how the chain plays back |
| `Buddy.SPEED`, `GROW`, `CHARGE_RATE` | how the TEAM SQUISH squishy moves and grows |
| `Collect.TIER_WEIGHT` | blind-box rarity odds |
| `Boss.EVERY` | how often a boss stage comes round |
| `Boss.CYCLE` / `SHOW` | each boss's open/shut rhythm — read together with its health, never alone |
| `Boss.HP` | health per boss, counted in that boss's own currency. Health × cycle length is the figure to check |
| `Boss.ENRAGE_AT` / `ENRAGE_RAMP` | when a dragging fight starts looking urgent, and how fast the warning winds up; it does no damage |
| `Boss.SPLIT_HITS` | presses of the slime's chain that tear one glob loose |
| `Boss.WIDE` / `JIGGLE` | each boss's rest shape and springiness — only the slime is anything but 1 |
| `Boss.DRAG_FOLLOW` / `DRAG_REACH` | how the body and the skin split the work of staying wrapped around a dragged glob |

Several constants are at their value because the obvious value was wrong, and the comment says
so. Read it before changing one.

To add a seventh letter: a colour in `Glyph.COLOR`, a case in `Kawaii.draw`, and a wider key
layout in `Layout.compute` — `Glyph.COUNT` drives the rest.

To add a collectible: one row across the parallel arrays in `Collect`, one in `Lore`, and bump
`Collect.COUNT`. The assertions will tell you what you missed.

## Licensing

Characters, effects, music, collectibles and stories are all original and procedurally
generated — there are no assets to license. The game font is
[Bungee](https://fonts.google.com/specimen/Bungee) under the SIL Open Font License, whose
text ships alongside it in `assets/fonts/Bungee-OFL.txt`. Quicksand remains bundled for
the trailer tools, with its license in `assets/fonts/OFL.txt`.

`res/raw/bgm.*` is gitignored on purpose: a user-supplied track stays on that device and must
never be committed, since this repo is shared. Drop one in and it becomes the MY TRACK option
in settings — and the default, since `Music.defaultChoice` prefers it whenever the file is
present. Pick something else in settings and that choice sticks.


## GitHub releases

GitHub Actions builds an APK on demand and whenever a version tag beginning with `v` is pushed. A
manual run leaves a downloadable workflow artifact; a tag run also creates a GitHub Release and
attaches a source-labelled APK.

```sh
git tag v0.2.0
git push origin v0.2.0
```

The workflow runs the complete test and render harness through `build.sh`, installs Android API 36
build tools, and signs with the project keystore stored as encrypted GitHub repository secrets. Keep
that keystore backed up: Android will not install an update signed with a different key over an
existing installation.

### Google Play builds

Tag pushes (`v*`) and manual runs of **Build Android release** produce two signed files:

- `DDDUMPLING-<ref>.aab`: production bundle to upload to Play Console.
- `DDDUMPLING-<ref>-developer.apk`: direct installation with developer controls enabled.

Tag builds attach both files to the GitHub Release; manual builds provide them in the workflow
artifact. Production uses `com.dddumpling.game` / **DDDUMPLING**. Developer builds use
`com.dddumpling.game.dev` / **DDDUMPLING Dev**, so both can be installed together even when
Play App Signing uses a different key. Scores, collections and preferences are separate;
existing production saves are not copied into the developer app.

`./build.sh` defaults to developer mode for local installs; `./build.sh --production` disables
settings and playtest actions at compile time and ignores saved developer speed/music preferences.
Both builds include the title screen privacy-policy link. Scores and collections remain local.

`./build-bundle.sh` creates `build/DDDUMPLING.aab`, targeting API 36. Set `HEXATYPE_KEYSTORE`,
`HEXATYPE_KEY_ALIAS`, `HEXATYPE_KEYSTORE_PASSWORD` and optionally `HEXATYPE_KEY_PASSWORD` to the
upload signing key. CI uses the existing `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEY_ALIAS`,
`ANDROID_KEYSTORE_PASSWORD` and `ANDROID_KEY_PASSWORD` secrets. Keep this key backed up privately.
`./build-bundle.sh --unsigned` is for local bundle validation only, not Play upload.

Version tags also publish the signed AAB to **Google Play closed testing** through fastlane.
Manual workflow runs have an optional `upload_to_play` checkbox (off by default) and a
`play_track` choice of closed or internal. The closed track defaults to API ID `alpha`; set
repository variable `PLAY_CLOSED_TRACK` for a custom track.
See [Play publishing setup](app-store/play-publishing.md) for service-account credentials,
versioned release notes, and retry instructions. Public releases remain manual.

### Optional Play Games integration

Production builds record progress locally. To include Google Play Games events and cross-device
saves, configure the project/event IDs and build with `DDDUMPLING_PLAY_CONFIG`. Developer builds
exclude the SDK. See [Play Games setup and save semantics](store/play-games.md).
