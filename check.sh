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

QUIET=0
SUITE=
FRAMES=
CROP=
RULES_ONLY=0
while [ $# -gt 0 ]; do
    case "$1" in
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
PURE="src/com/sram/hexatype/Glyph.java
src/com/sram/hexatype/Roster.java
src/com/sram/hexatype/Kawaii.java
src/com/sram/hexatype/Layout.java
src/com/sram/hexatype/Sfx.java
src/com/sram/hexatype/Music.java
src/com/sram/hexatype/Words.java
src/com/sram/hexatype/Fx.java
src/com/sram/hexatype/Steamer.java
src/com/sram/hexatype/StarPath.java
src/com/sram/hexatype/Collect.java
src/com/sram/hexatype/Power.java
src/com/sram/hexatype/Boss.java
src/com/sram/hexatype/Softbody.java
src/com/sram/hexatype/Buddy.java
src/com/sram/hexatype/GameCore.java
src/com/sram/hexatype/Pacing.java
src/com/sram/hexatype/Blade.java
src/com/sram/hexatype/CaseUi.java
src/com/sram/hexatype/Interlude.java
src/com/sram/hexatype/BossPlay.java
src/com/sram/hexatype/Painter.java
src/com/sram/hexatype/SettingsUi.java
src/com/sram/hexatype/Draw.java
src/com/sram/hexatype/Sky.java
src/com/sram/hexatype/Slime.java
src/com/sram/hexatype/Skits.java
src/com/sram/hexatype/Shape.java
src/com/sram/hexatype/Basket.java
src/com/sram/hexatype/Parade.java
src/com/sram/hexatype/Finish.java
src/com/sram/hexatype/Trinket.java
src/com/sram/hexatype/Cabinet.java
src/com/sram/hexatype/Launch.java
src/com/sram/hexatype/RoundEnd.java
src/com/sram/hexatype/Demo.java
src/com/sram/hexatype/Lore.java
src/com/sram/hexatype/Narration.java
src/com/sram/hexatype/Showcase.java
src/com/sram/hexatype/Storybook.java
src/com/sram/hexatype/Hud.java
src/com/sram/hexatype/Screens.java
src/com/sram/hexatype/StarScreen.java
src/com/sram/hexatype/BossScreen.java
src/com/sram/hexatype/BossVictory.java
src/com/sram/hexatype/Renderer.java"

rm -rf build/harness
mkdir -p build/harness out
# shellcheck disable=SC2086
javac -nowarn -d build/harness $PURE tools/*.java

# Quiet drops the per-assertion ok lines and the per-frame wrote lines, keeping failures, the
# indented printf diagnostics, DOES NOT FIT and the tally.
filter() {
    if [ "$QUIET" = 1 ]; then grep -Ev "^  ok |^  wrote " || true; else cat; fi
}

[ "$QUIET" = 1 ] || echo "=== rules ==="
java -cp build/harness com.sram.hexatype.CoreTest "$SUITE" | filter

if [ "$RULES_ONLY" = 1 ]; then exit 0; fi

[ "$QUIET" = 1 ] || printf '\n=== frames ===\n'
FRAMES="$FRAMES" CROP="$CROP" java -Xmx512m -cp build/harness com.sram.hexatype.Preview \
    "${1:-640}" "${2:-1400}" "${3:-3}" out | filter
[ "$QUIET" = 1 ] || ls -la out/*.png
