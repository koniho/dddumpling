# DDDUMPLING

A kawaii typing-attack game for Android and iPhone. Android builds run in Termux or with a
desktop Android SDK; gameplay and drawing are shared Java.

A native iPhone port shares the Java gameplay and renderer through J2ObjC. See
[iOS build and simulator instructions](ios/README.md) and the
[port's feature/validation inventory](ios/docs/parity.md).

> Production: **DDDUMPLING** (`com.dddumpling.game`). Local builds default to
> **DDDUMPLING Dev** (`com.dddumpling.game.dev`), with separate scores and collections.
> Both variants write `hexatype.apk`, the legacy output filename.

## Play

Match falling character prompts by tapping the honeycomb keys. The adaptive deck starts with
four keys and can expand to six. Clear enemies before they cross the danger line, and use a
rescue swipe to push threats back. Choose your first squishy companion on a new game;
short, interactive tutorials introduce the controls and new mechanics as you encounter them.

Three modes share the same squishy characters, with separate records and run summaries:

- **Adventure:** travel through themed lands, collect companions, play Steamer and Star Path
  minigames, and fight a boss every five stages. Losing a boss fight offers a retry; the run
  record keeps the retry count.
- **Survival:** face an escalating enemy mix against scrolling rainbow bands. Each power-up
  is available once per run, and rescue swipes recharge. Rush enemies arrive after 90 seconds;
  all enemy types are available after two minutes. Each run awards one collectible duck,
  chosen from a time-based pool, with a special duck for reaching five minutes.
- **Boss Time Attack:** challenge unlocked bosses individually or as an all-boss run, with
  animated versus introductions, timed records, and per-boss performance details.

Beat your first Adventure boss to unlock the other modes. Boss Time Attack opponents unlock
as you defeat them; the all-boss option requires every boss. Your selected mode and Adventure
land are remembered between app launches.

### Lands, enemies, and bosses

Adventure progresses through slime hills, crystals, underwater scenery, mushrooms, swamp,
and rainbow lands. Boss encounters introduce **Super Slime**, **Dark Divide**, **Octopulse**,
**Fly Agaric**, **Gummy Gator**, and **Rainbow Cat**. Each has its own mix of key prompts,
taps, swipes, or tracing, with spring bodies that bounce and react to hits.

Later stages mix longer and multipress words with linked pairs, fog-hidden prompts,
shooting-star rush enemies, and divebomb enemies that accelerate on their final key.
FLURRY, NINJA, and TEAM SQUISH power-ups change how you clear the crowd; mystery pickups
can also bring INCOGNITO or MONOCHROME debuffs.

The shared [world catalog](src/com/dddumpling/game/WorldCatalog.java) defines the land/boss
mapping. Cave content is reserved and is not a reachable Adventure land. DDDUMPLING Town
and its ticket readout remain developer-only.

### Collect and customize

Free squishies from the Steamer, collect Star Path rewards, earn Survival ducks, and add
miniature boss friends to your display case. Collected characters can become your companion.
Pan around the case and tap a collectible once to center it and open its story. It floats
above the story with colorful rings; another tap makes it react. Duplicate counts persist.
The [collectible catalog](src/com/dddumpling/game/Collect.java) defines the characters and
reward families.

Choose the original look or **Halloween Dress Up** in settings. Halloween changes character
art, colors, and boss names while keeping gameplay the same.

### Settings and progress

Open the title's squishy gear, or pause during a run, to reach settings. Music and effects
have separate volume controls. The **Progress** tab groups achievements, cloud sync,
Reset High Scores, and Reset Tutorials. Its animated trophy case and cloud open the relevant
progress controls. Platform services depend on the build configuration: Google Play Games
on Android and Game Center/iCloud on iPhone. See the platform setup links below.

**Kids Mode** applies to the next run: four keys, slower enemy approach, and words capped at
six presses including multipress keys. Projectiles keep their normal speed, linked pairs
keep their standard timing, and lives and game over still apply. The Steamer is untimed with
a ten-pip goal; Star Path caps at 30% of its difficulty ladder without erasing normal-mode
progression.

The title's **What's New** steamer opens release history and interactive demonstrations.
Swipe between change pages or tap their neighboring page edges. After an update, returning
players get an invitation to see what's new.

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
the concise workflow defaults; [engineering notes](docs/engineering/index.md) hold topic-specific pitfalls.

## The fast loop

**You can see and hear this game without building or installing it.** No SDK, no device:

```sh
./check.sh -q -r             # default code check: rules only, no frames
./check.sh -q -r -s Boss     # focused suite for iteration
./check.sh --town -q         # Town rules and selected Town/attraction frames
./check.sh -q --production   # when production behavior is affected
./check.sh -q                # full visual/audio export, only when needed
./check.sh -q -f 60,65        # only these frames, skipping the sheets and the WAVs
./check.sh -q -f 60 -c 0,.1,1,.45   # ...cropped to that box, in 0..1 fractions
./check.sh 1080 2400 2        # a specific screen size and supersampling factor
```

Without `-r`, it does two things:

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
be compared selectively; see the [rendering notes](docs/engineering/rendering.md).

## Android requirements

The build targets API 36 and supports Android 7.0+ (API 24+). Desktop builds need a JDK (Java 17
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
pkg install openjdk-17 python bash aapt2 d8 apksigner zip unzip curl android-tools gradle
```

The build reads `sdk/android.jar` when present, otherwise
`$ANDROID_HOME/platforms/android-36/android.jar`. For Termux, copy API 36's `android.jar`
from an SDK installation into `sdk/`. SDK files are not committed.

> Termux ships an `ecj` wrapper, but it hardcodes `-7` and a nonexistent classpath, so the
> build uses `javac --release 8` instead.

## Build

Build and deploy only when explicitly requested. Ordinary code and PR work uses the
change-appropriate checks in [AGENTS.md](AGENTS.md); documentation changes need no game run.

```sh
./build.sh                 # local developer APK: rule smoke checks + build
./build.sh --developer --town # Town-scoped checks + installable developer APK
./build.sh --developer --rules-checks # full rules, no exports + developer APK
./build.sh --developer --full-checks # full checks and exports when explicitly needed
./build.sh --production    # production APK: full verification
```

The pipeline is `aapt2 compile` → `aapt2 link` → `javac` → `d8` → `zip` → `apksigner`.
Local developer builds run the Rules smoke suite, then compile, package, sign and verify the
APK. A passing build is sufficient for a requested local install; no full export or soak run
is required. Production and `--full-checks` builds retain full headless/render checks and
production privacy assertions. Logs are in `build/check.log` and, for full verification,
`build/production-check.log`.

Output is a signed `hexatype.apk` with bundled fonts. Legacy personal `res/raw/bgm.*` files
are excluded from packaging.

Without a supplied signing key, the build creates `build/debug.keystore` on first run. Keep
the same key to update an existing installation without uninstalling it.
Local Termux developer builds retain at least the installed Dev app's version code in the
generated manifest, so an older checkout can update it without changing release metadata.

## Install

With an adb-connected Android device:

```sh
adb install -r hexatype.apk
adb shell am start --activity-single-top -n com.dddumpling.game.dev/com.dddumpling.game.MainActivity
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

Unattended installs need a privileged transport. ADB can talk to the very device Termux is
running on over Wireless debugging:

1. Settings → Developer options → enable **Wireless debugging**.
2. Tap **Pair device with pairing code**. Note the `IP:PORT` and the six-digit code.
3. `adb pair <ip>:<pair-port>` and enter the code.
4. `adb connect <ip>:<port>` — the port on the main Wireless debugging screen, which is
   *different* from the pairing port.
5. `adb devices` should list one `device`.

Pairing persists, but the port changes when Wireless debugging is toggled or the device
reboots, so step 4 may need repeating. After that `./deploy.sh` is fully unattended, and
`adb install -r` preserves the best score, the settings and the collection.

If ADB is unavailable, an already configured and authorized Shizuku/rish shell can also
install and launch the APK. Investigate existing local transports before falling back to a
manual install. `deploy.sh` does not automatically select rish. Keep the existing developer
signing key and package; do not uninstall the app to work around an update failure. See
[build troubleshooting](docs/engineering/build.md).

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
| Modes and world catalog | `ModeSelector`, `WorldCatalog`, `Lands`, `Survival`, `TimeAttack` |
| Bosses and minigames | `Boss`, `BossPlay`, `BossRetry`, `Interlude`, `Steamer`, `StarPath` |
| Collection and progress | `Collect`, `BossCollect`, `CaseUi`, `Progress`, `ProgressData`, `Achievements`, `CloudSaveUi` |
| Shared drawing | `Painter`, `Draw`, `Renderer`, `Hud`, `Screens`, and scene renderers |
| Settings and release book | `PlayerSettings`, `SettingsInput`, `DevSettings`, `SkinSettings`, `ReleaseNotes` |
| Android host | `MainActivity`, `GameView`, `CanvasPainter`, `Audio`, `Crash` |
| Android services | `local-src/` (offline), `play-src/` (Play Games SDK and integration) |
| iPhone host and translation | `ios/` |
| Headless harness | `tools/CoreTest.java`, `tools/Test*.java`, `tools/Bot.java`, `tools/Preview.java` |
| Harness output | `tools/RasterPainter.java`, `tools/Font.java`, `tools/Png.java`, `tools/Wav.java` |

Keep shared code free of `android.*` imports. Add new shared files to `check.sh`'s `PURE`
manifest, which iOS translation also uses. The Android build discovers Java files under
`src/`, the selected service directory, and generated sources automatically.

## Tuning

Start with `Pacing`, then the relevant mechanic (`Power`, `StarPath`, `Boss`, or `Steamer`).
Run `./check.sh -q -r` for difficulty changes; its bounded bots measure what finite
press rates and reaction times can survive. Read the tuned constants' comments and
[AGENTS.md](AGENTS.md) before changing them.

The harness font is an ASCII subset; add missing glyphs to `tools/Font.java` when introducing
text. See [GLOSSARY.md](GLOSSARY.md) for mechanic names and their code locations.

## Licensing

Characters and scenery use shared procedural drawing. Audio combines synthesized music and
effects, user recordings, and licensed third-party recordings. Preserve the source and
modification details in [recording provenance](audio/recorded/README.md) and the bundled
[audio credits](assets/AudioCredits.txt), including required attribution. The game font is
[Bungee](https://fonts.google.com/specimen/Bungee) under the SIL Open Font License, whose
text ships alongside it in `assets/fonts/Bungee-OFL.txt`. Quicksand remains bundled for
the trailer tools, with its license in `assets/fonts/OFL.txt`.

Legacy `res/raw/bgm.*` files remain gitignored and are excluded from builds. Custom music is
no longer supported. Music follows the game scene, including dedicated boss, frenzy, and
versus arrangements. Player settings retain music volume and mute. The iPhone release bundles
its J2ObjC runtime notices separately.

## GitHub releases

Before version preparation, tagging, or publishing, follow [Executing a release](docs/releasing.md).
Review the final in-game and destination notes with the user and obtain explicit approval before
proceeding. The [writing guide](docs/release-notes.md) covers the editable release catalog.

Tag pushes (`v*`) and manual runs of **Build Android release** produce three signed files:

- `DDDUMPLING-<ref>.aab`: production bundle for Google Play.
- `DDDUMPLING-<ref>.apk`: production APK for direct installation.
- `DDDUMPLING-<ref>-developer.apk`: developer APK for direct installation.

Tag builds attach all three to a draft GitHub Release; manual builds provide workflow artifacts.
A manual `source_tag` rebuild produces artifacts without publishing a release or uploading to Play.
The workflow runs developer and production harness checks, uses API 36 build tools, and signs
with repository secrets. It builds and verifies an offline production APK for direct/itch
distribution, then a Play-enabled AAB using the checked-in Play Games configuration. The
developer APK includes the Play SDK. Keep the signing key backed up: updates must match the
installed app's certificate.

Production and developer apps can be installed together, with separate scores, collections, and
preferences. Developer builds expose playtest controls; production disables those at compile time.
Player settings remain available in both.

### Google Play builds

`./build-bundle.sh` creates `build/DDDUMPLING.aab`, targeting API 36. Set `HEXATYPE_KEYSTORE`,
`HEXATYPE_KEY_ALIAS`, `HEXATYPE_KEYSTORE_PASSWORD` and optionally `HEXATYPE_KEY_PASSWORD` to the
upload signing key. CI uses the existing `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEY_ALIAS`,
`ANDROID_KEYSTORE_PASSWORD` and `ANDROID_KEY_PASSWORD` secrets. Keep this key backed up privately.
`./build-bundle.sh --unsigned` is for local bundle validation only, not Play upload.

Version tags also upload the signed AAB to **Google Play closed testing** through fastlane,
leaving the release in draft unless activation is explicitly requested.
Manual workflow runs have an optional `upload_to_play` checkbox (off by default) and a
`play_track` choice of closed or internal. The closed track defaults to API ID `alpha`; set
repository variable `PLAY_CLOSED_TRACK` for a custom track.
See [Play publishing setup](app-store/play-publishing.md) for service-account credentials,
versioned release notes, and retry instructions. Production promotion is a separate, explicit
step through the production-release workflow; uploading a build alone does not publish it.
Itch publishing and Apple delivery use their own workflows.

### Play Games integration

Standalone production builds record progress locally by default. To enable Google Play Games
achievements, events, and cross-device saves, supply the matching configuration through
`DDDUMPLING_PLAY_CONFIG`. The release AAB uses `store/play-games.draft.json`. Developer builds
always include the SDK and require Gradle to resolve its pinned dependencies. Sign-in and reporting
stay off until a matching Dev configuration is supplied. See [Play Games setup and save semantics](store/play-games.md).

TestFlight upload, tester distribution, and App Store submission are separate steps. Production
submission still goes through Apple review. For iPhone CI and TestFlight delivery, see
[iOS CI](ios/docs/ci.md) and the
[TestFlight version policy](ios/README.md#testflight-version-policy).
