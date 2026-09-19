#!/bin/sh
# Headless run: rule assertions, then PNG frame renders into out/. No SDK, no install.
#
# /bin/sh is the only shebang that exists both in Termux and on desktop Linux; sh then finds bash.
# shellcheck shell=bash
#
# Usage: check.sh [-q] [-s SUITE] [-f TAGS] [-c BOX] [-r] [W H SS]
#   -q        only failures, printed diagnostics and the tally
#   -s SUITE  run only suites whose name contains SUITE (Boss, Power, Soak, ...)
#   -f TAGS   render only frames whose name starts with one of TAGS (comma-separated); also
#             skips the character/skit/collect sheets and the WAVs
#   -c BOX    crop each frame to x0,y0,x1,y1, as 0..1 fractions
#   -r        rules only, no frames
if [ -z "${BASH_VERSION:-}" ]; then exec bash "$0" "$@"; fi
set -euo pipefail
cd "$(dirname "$0")"

PRODUCTION=0
QUIET=0
SUITE=
FRAMES=
CROP=
RULES_ONLY=0
while [ $# -gt 0 ]; do
    case "$1" in
        --production) PRODUCTION=1; shift ;;
        -q|--quiet)  QUIET=1; shift ;;
        -s|--suite)  SUITE="$2"; shift 2 ;;
        -f|--frames) FRAMES="$2"; shift 2 ;;
        -c|--crop)   CROP="$2"; shift 2 ;;
        -r|--rules)  RULES_ONLY=1; shift ;;
        *) break ;;
    esac
done

# Every pure-Java file, by hand. A new pure file has to be added here or the harness fails to
# compile while the APK builds fine.
python3 tools/release-notes.py check >/dev/null

PURE="src/com/dddumpling/game/OctoWaveRecording.java
src/com/dddumpling/game/PushLesson.java
src/com/dddumpling/game/DivideDeath.java
src/com/dddumpling/game/SettingsArt.java
src/com/dddumpling/game/PlayerSettings.java
src/com/dddumpling/game/DevSettings.java
src/com/dddumpling/game/SettingsInput.java
src/com/dddumpling/game/ProgressData.java
src/com/dddumpling/game/Progress.java
src/com/dddumpling/game/PrivacyUi.java
src/com/dddumpling/game/ReleaseNotes.java
src/com/dddumpling/game/ReleaseChange.java
src/com/dddumpling/game/ReleaseContent.java
src/com/dddumpling/game/ReleaseTransition.java
src/com/dddumpling/game/OpacityPainter.java
src/com/dddumpling/game/MushroomDeath.java
src/com/dddumpling/game/ReleaseMascot.java
src/com/dddumpling/game/Pause.java
src/com/dddumpling/game/Glyph.java
src/com/dddumpling/game/Roster.java
src/com/dddumpling/game/Kawaii.java
src/com/dddumpling/game/Layout.java
src/com/dddumpling/game/Sfx.java
src/com/dddumpling/game/CartRecording.java
src/com/dddumpling/game/RockRecording.java
src/com/dddumpling/game/Music.java
src/com/dddumpling/game/Words.java
src/com/dddumpling/game/Fx.java
src/com/dddumpling/game/Steamer.java
src/com/dddumpling/game/StarPath.java
src/com/dddumpling/game/Collect.java
src/com/dddumpling/game/Power.java
src/com/dddumpling/game/Boss.java
src/com/dddumpling/game/Softbody.java
src/com/dddumpling/game/Buddy.java
src/com/dddumpling/game/GameCore.java
src/com/dddumpling/game/EnemyEntry.java
src/com/dddumpling/game/Pacing.java
src/com/dddumpling/game/LinkedPairs.java
src/com/dddumpling/game/LinkedPairArt.java
src/com/dddumpling/game/Blade.java
src/com/dddumpling/game/CaseUi.java
src/com/dddumpling/game/BossCollect.java
src/com/dddumpling/game/Interlude.java
src/com/dddumpling/game/BossPlay.java
src/com/dddumpling/game/Painter.java
src/com/dddumpling/game/MonochromePainter.java
src/com/dddumpling/game/ColorFadePainter.java
src/com/dddumpling/game/SettingsUi.java
src/com/dddumpling/game/Draw.java
src/com/dddumpling/game/Sky.java
src/com/dddumpling/game/CaveDumpling.java
src/com/dddumpling/game/CaveSelection.java
src/com/dddumpling/game/CaveRoute.java
src/com/dddumpling/game/CaveInput.java
src/com/dddumpling/game/CaveCollect.java
src/com/dddumpling/game/CaveSong.java
src/com/dddumpling/game/CaveCart.java
src/com/dddumpling/game/CaveCartInput.java
src/com/dddumpling/game/CaveCartScene.java
src/com/dddumpling/game/CaveCartScreen.java
src/com/dddumpling/game/CaveMining.java
src/com/dddumpling/game/CaveMiningInput.java
src/com/dddumpling/game/CaveMiningScreen.java
src/com/dddumpling/game/CaveMiningScene.java
src/com/dddumpling/game/CaveBand.java
src/com/dddumpling/game/CaveInterlude.java
src/com/dddumpling/game/CaveBandScreen.java
src/com/dddumpling/game/Cave.java
src/com/dddumpling/game/CaveTraps.java
src/com/dddumpling/game/CaveArt.java
src/com/dddumpling/game/CaveTerrain.java
src/com/dddumpling/game/CaveEffects.java
src/com/dddumpling/game/CaveEnemy.java
src/com/dddumpling/game/CaveScreen.java
src/com/dddumpling/game/Lands.java
src/com/dddumpling/game/SeaSkits.java
src/com/dddumpling/game/LandPicker.java
src/com/dddumpling/game/LandDiscovery.java
src/com/dddumpling/game/Slime.java
src/com/dddumpling/game/Skits.java
src/com/dddumpling/game/Shape.java
src/com/dddumpling/game/Basket.java
src/com/dddumpling/game/Parade.java
src/com/dddumpling/game/Finish.java
src/com/dddumpling/game/Trinket.java
src/com/dddumpling/game/Cabinet.java
src/com/dddumpling/game/Launch.java
src/com/dddumpling/game/RoundEnd.java
src/com/dddumpling/game/Demo.java
src/com/dddumpling/game/Lore.java
src/com/dddumpling/game/Narration.java
src/com/dddumpling/game/Showcase.java
src/com/dddumpling/game/Storybook.java
src/com/dddumpling/game/Hud.java
src/com/dddumpling/game/TitleBubbleFont.java
src/com/dddumpling/game/Screens.java
src/com/dddumpling/game/StarScreen.java
src/com/dddumpling/game/BossScreen.java
src/com/dddumpling/game/BossVictory.java
src/com/dddumpling/game/SlimeGuide.java
src/com/dddumpling/game/Renderer.java"

rm -rf build/harness
mkdir -p build/harness out
# shellcheck disable=SC2086
DEVELOPER=true
[ "$PRODUCTION" = 0 ] || DEVELOPER=false
sh tools/build-flags.sh build/harness-flags "$DEVELOPER"
javac -nowarn -d build/harness $PURE tools/*.java build/harness-flags/com/dddumpling/game/BuildFlags.java

# Quiet drops the per-assertion ok lines and the per-frame wrote lines, keeping failures, the
# indented printf diagnostics, DOES NOT FIT and the tally.
filter() {
    if [ "$QUIET" = 1 ]; then grep -Ev "^  ok |^  wrote " || true; else cat; fi
}

if [ "$PRODUCTION" = 1 ]; then
    java -cp build/harness com.dddumpling.game.TestProduction | filter
    exit 0
fi

[ "$QUIET" = 1 ] || echo "=== rules ==="
java -cp build/harness com.dddumpling.game.CoreTest "$SUITE" | filter
if [ -z "$SUITE" ] || [[ "CaveBand Audio" == *"$SUITE"* ]]; then python3 tools/test-band-audio.py; fi

if [ "$RULES_ONLY" = 1 ]; then exit 0; fi

[ "$QUIET" = 1 ] || printf '\n=== frames ===\n'
FRAMES="$FRAMES" CROP="$CROP" java -Xmx512m -cp build/harness com.dddumpling.game.Preview \
    "${1:-640}" "${2:-1400}" "${3:-3}" out | filter
[ "$QUIET" = 1 ] || ls -la out/*.png
