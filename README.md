# DDDUMPLING

A kawaii typing-attack game for Android and iPhone. Android builds run in Termux or with a
desktop Android SDK; gameplay and drawing are shared Java.

A native iPhone port shares the Java gameplay and renderer through J2ObjC. See
[iOS build and simulator instructions](ios/README.md) and the
[port's feature/validation inventory](ios/docs/parity.md).

> Production: **DDDUMPLING** (`com.dddumpling.game`). Local builds default to
> **DDDUMPLING Dev** (`com.dddumpling.game.dev`), with separate scores and collections.
> Both variants write `hexatype.apk`, the legacy output filename.

Cute words fall from the sky. Each "letter" is one of six characters — dumpling, strawberry,
cat, grapes, squishy, blob — with its own colour and face. You type by tapping the matching
hexagon in the honeycomb deck at the bottom. The adaptive deck starts with four keys and can
expand to six. Clear a word before it reaches the danger line, or it lunges and costs you a life.
Once a stage, when something is bearing down on the line, swipe up from the lower half of the
playfield above the keys to shove threatening words back, including a committed lunge.

Between-stage minigames include a dim sum steamer and a steerable star course.
The steamer picks two active keys; alternate them, then swipe the lid away to claim the prize.
The dumpling inside is a blind box — freeing it hands over one of thirty
collectible squishies, from commons up to a single gold grail. The collection is kept for good
and shown in the display case: a shimmering glass cabinet that opens from the small case on the title screen.
Pan freely in any direction through the animated collectibles, without snapping to cells.
Fruits share one row, candies share another, and defeated bosses join their own row.
Each boss victory awards its matching miniature character and replaces the next minigame with
a confetti-filled welcome scene; repeated victories pay a duplicate bonus. The glass catches the light as you move.
Navigation ticks as the highlighted tile changes; its character grows, bounces, and sparkles.
The highlighted character shows how many times it has been collected, including duplicates.
Counts persist across runs for every reward source. Clearing the collection also clears these counts.
Tap a visible tile to pan it into the center, then tap it again for its story;
every key starts a run instead, and puts the case away first if it is open. Anything you have not
won yet is a silhouette behind a question mark; tap one you have and it tells you where it
lives and what its family gets up to. Win one
and the collection parades: they march in from the left, the newcomer drops into the end of the
line, and the next stage waits until they have all marched off to the right.

Boss fights appear at stages 5, 10, 15, and 20: Slime, Dark Divide, Octopulse, and Fly Agaric.
Each combines key defense with a physical gesture, using soft bodies that wobble and deform.
A victory unlocks that boss's miniature friend in the display case and celebrates the new
character before moving straight to the next stage.

The full collection has 49 characters: 30 blind-box squishies, five starlings, ten gelatinous
cubes, and four boss friends. Unlocked lands can be selected from the title; the fifth land,
the cave expedition, is developer-only.

FLURRY, FLING, and TEAM SQUISH frenzies change how you clear words. From stage 11, mystery
pickups can also bring INCOGNITO or MONOCHROME debuffs. From stage 16, linked friends ask for
both partner keys within 200 ms. MULTI is retired from the offered powers.

Player settings, available from the title and in-run stage readout, provide independent music
and effects volume/mute controls, the privacy policy, and Kids Mode. Kids Mode applies to the
next run: slower enemy and projectile traversal, four keys, short unstacked words, and more time for linked friends;
boss actions, animations, and minigames keep normal timing;
lives and game over remain. The title's What's new steamer opens the release history and demos.

## Where to look

Start with the document that answers your question:

| Question | Document |
| --- | --- |
| I am new here, what do I do first? | [ONBOARDING.md](ONBOARDING.md) |
| What is this thing called in the code? | [GLOSSARY.md](GLOSSARY.md) |
| How do I work on it without breaking it? | [AGENTS.md](AGENTS.md) |
| How do I build, run or check it? | this file |

`GLOSSARY.md` exists because several plain-English names differ from the identifiers — a
falling word is an `Enemy`, the frenzy is `mode`, the interlude is `BONUS`. `AGENTS.md` carries
the conventions and, more usefully, the traps that have already cost time.

## The fast loop

**You can see and hear this game without building or installing it.** No SDK, no device:

```sh
./check.sh -q                # rules + frames at 640x1400; quiet output
./check.sh -q --production   # production configuration
./check.sh -q -r             # rules only, no frames
./check.sh -q -s Boss         # one suite
./check.sh -q -f 60,65        # only these frames, skipping the sheets and the WAVs
./check.sh -q -f 60 -c 0,.1,1,.45   # ...cropped to that box, in 0..1 fractions
./check.sh 1080 2400 2        # a specific screen size and supersampling factor
```

It does two things:

1. **Rule assertions** (`tools/CoreTest.java`, `tools/Test*.java`): layout
   geometry, hit-testing, targeting, scoring, stage pacing, the interlude's phases and both paths through it, the
   collectible catalogue and its odds, stories, settings, releases, and persistence. Soak checks
   include perfect play, players with bounded reaction/press rates, and random-input fuzz.
   Production checks run through `tools/TestProduction.java`.
2. **Frame renders** (`tools/Preview.java`) — drives the real state machine into every
   interesting state and writes each to `out/`. Numbered files are single frames; the `0-`
   files are review sheets that show a whole set at once:

   | Sheet | Shows |
   | --- | --- |
   | `0-characters.png` | the six letters at display and tile size, plain and struck |
   | `0-collect.png` | collectible catalogue as collected |
   | `0-collect-unknown.png` | collectible catalogue as unknown silhouettes |
   | `0-skits.png` | the ten stage vignettes, sampled through each |
   | `0-beats.png` | the ten story vignettes, sampled through each |

   Sounds go to `out/sfx/*.wav` — every effect and every music loop.

Inspect the PNGs for visual changes; use `-r` for faster rules-only iteration. The harness
needs a JDK (Java 17 works), Python 3, and Bash. Avoid concurrent runs in one checkout:
they share `build/harness` and `out/`.

This works because all logic and all drawing are pure Java behind the
[`Painter`](src/com/dddumpling/game/Painter.java) interface. The APK implements it with
`android.graphics.Canvas`; `tools/RasterPainter` implements it with a software rasterizer.
The iPhone host implements the same interface with Core Graphics. The harness exercises the
shared drawing path; native fonts and antialiasing can differ. Renders
are deterministic (fixed RNG seeds, no wall clock), so a change that should not alter them can
be proved not to; `AGENTS.md` has the recipe.

## Android requirements

The build targets API 36 and supports Android API 21+. Desktop builds need a JDK (Java 17
works), Python 3, current Bash, `zip`/`unzip`, and Android command-line tools. Set
`ANDROID_HOME` to the SDK directory and install the platform and tools used by CI:

```sh
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
    "platforms;android-36" "build-tools;36.0.0" "platform-tools"
export PATH="$ANDROID_HOME/build-tools/36.0.0:$ANDROID_HOME/platform-tools:$PATH"
```

On macOS, put a current Bash (for example, Homebrew Bash) on `PATH`; Apple's bundled Bash 3.2
is too old for parts of the build. Bundle creation also needs `curl` and `sha256sum`.

Termux on an `aarch64` Android device:

```sh
pkg install openjdk-17 python bash aapt2 d8 apksigner zip unzip curl android-tools
```

The build reads `sdk/android.jar` when present, otherwise
`$ANDROID_HOME/platforms/android-36/android.jar`. For Termux, copy API 36's `android.jar`
from an SDK installation into `sdk/`. SDK files are not committed.

> Termux ships an `ecj` wrapper, but it hardcodes `-7` and a nonexistent classpath, so the
> build uses `javac --release 8` instead.

## Build

```sh
./build.sh                 # developer APK (default)
./build.sh --production    # production APK
```

The pipeline is `aapt2 compile` → `aapt2 link` → `javac` → `d8` → `zip` → `apksigner`.
It runs both developer and production harness checks before packaging and stops on failed
assertions. Logs are in `build/check.log` and `build/production-check.log`.

Output is a signed `hexatype.apk` with bundled fonts. Legacy personal `res/raw/bgm.*` files
are excluded from packaging.

Without a supplied signing key, the build creates `build/debug.keystore` on first run. Keep
the same key to update an existing installation without uninstalling it.

## Install

With an adb-connected Android device:

```sh
adb install -r hexatype.apk
adb shell am start -n com.dddumpling.game.dev/com.dddumpling.game.MainActivity
```

For production, use `com.dddumpling.game/com.dddumpling.game.MainActivity` instead.
Updating with the same package and signing key preserves app data.

From Termux:

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

`./deploy.sh` is the Termux build/install/launch helper (its shebang is Termux-specific).
It defaults to developer mode; use `./deploy.sh --production` for production. With adb connected
it installs and launches automatically; otherwise it falls back to the installer dialog.

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

Without adb, Termux's `logcat` is restricted to its own UID.
[`Crash.java`](src/com/dddumpling/game/Crash.java) also renders a stack trace on screen.
With adb connected, use `adb logcat` to inspect device crashes.

## Layout of the code

| Area | Main files |
| --- | --- |
| Shared rules and state | `GameCore`, `Pacing`, `Words`, `Roster`, `LinkedPairs`, `Power` |
| Bosses and minigames | `Boss`, `BossPlay`, `Interlude`, `Steamer`, `StarPath` |
| Collection and progress | `Collect`, `BossCollect`, `CaseUi`, `Progress`, `ProgressData` |
| Shared drawing | `Painter`, `Draw`, `Renderer`, `Hud`, `Screens`, and scene renderers |
| Settings and release book | `PlayerSettings`, `SettingsInput`, `DevSettings`, `ReleaseNotes` |
| Android host | `MainActivity`, `GameView`, `CanvasPainter`, `Audio`, `Crash` |
| Android services | `local-src/` (offline), `play-src/` (optional Play integration) |
| iPhone host and translation | `ios/` |
| Headless harness | `tools/CoreTest.java`, `tools/Test*.java`, `tools/Bot.java`, `tools/Preview.java` |
| Harness output | `tools/RasterPainter.java`, `tools/Font.java`, `tools/Png.java`, `tools/Wav.java` |

Keep shared code free of `android.*` imports. Add new shared files to `check.sh`'s `PURE`
manifest, which iOS translation also uses. The Android build discovers Java files under
`src/`, the selected service directory, and generated sources automatically.

## Tuning

Start with `Pacing`, then the relevant mechanic (`Power`, `StarPath`, `Boss`, or `Steamer`).
Run `./check.sh -q -s Soak -r` for difficulty changes: the bounded bots measure what finite
press rates and reaction times can survive. Read the tuned constants' comments and
[AGENTS.md](AGENTS.md) before changing them.

The harness font is an ASCII subset; add missing glyphs to `tools/Font.java` when introducing
text. See [GLOSSARY.md](GLOSSARY.md) for mechanic names and their code locations.

## Licensing

Characters, effects, music, collectibles and stories are all original and procedurally
generated. The game font is
[Bungee](https://fonts.google.com/specimen/Bungee) under the SIL Open Font License, whose
text ships alongside it in `assets/fonts/Bungee-OFL.txt`. Quicksand remains bundled for
the trailer tools, with its license in `assets/fonts/OFL.txt`.

Legacy `res/raw/bgm.*` files remain gitignored and are excluded from builds. Custom music is
no longer supported. Music follows the game scene: MOOG SWING normally and LOFI DRIFT in caves,
with dedicated boss, frenzy, and Cave Band arrangements. Player settings retain music volume
and mute. The iPhone release bundles its J2ObjC runtime notices separately.

## GitHub releases

Before version preparation, tagging, or publishing, follow [Executing a release](docs/releasing.md).
Review the final in-game and destination notes with the user and obtain explicit approval before
proceeding. The [writing guide](docs/release-notes.md) covers the editable release catalog.

Tag pushes (`v*`) and manual runs of **Build Android release** produce three signed files:

- `DDDUMPLING-<ref>.aab`: production bundle for Google Play.
- `DDDUMPLING-<ref>.apk`: production APK for direct installation.
- `DDDUMPLING-<ref>-developer.apk`: developer APK for direct installation.

Tag builds attach all three to the GitHub Release; manual builds provide workflow artifacts.
A manual `source_tag` rebuild produces artifacts without publishing a release or uploading to Play.
The workflow runs developer and production harness checks, uses API 36 build tools, and signs
with repository secrets. It explicitly disables Play integration and verifies offline production
artifacts. Keep the signing key backed up: updates must match the installed app's certificate.

Production and developer apps can be installed together, with separate scores, collections, and
preferences. Developer builds expose playtest controls; production disables those at compile time.
Player settings remain available in both.

### Google Play builds

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

For iPhone CI and TestFlight delivery, see [iOS CI](ios/docs/ci.md) and the
[TestFlight version policy](ios/README.md#testflight-version-policy).
