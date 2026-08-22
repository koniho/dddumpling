#!/bin/sh
# Runs the game headlessly: rule assertions, then PNG frame renders into out/.
# Needs no Android SDK and no install — only the pure-Java half of the codebase.
#
# /bin/sh is the only interpreter path that exists both in Termux and on a desktop Linux
# box: Termux has no /usr/bin/env and no /bin/bash, a desktop has no
# /data/data/com.termux/... — so the shebang finds sh and sh finds bash on PATH.
# shellcheck shell=bash
if [ -z "${BASH_VERSION:-}" ]; then exec bash "$0" "$@"; fi
set -euo pipefail
cd "$(dirname "$0")"

PURE="src/com/sram/hexatype/Glyph.java
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
src/com/sram/hexatype/Renderer.java"

rm -rf build/harness
mkdir -p build/harness out
# shellcheck disable=SC2086
javac -nowarn -d build/harness $PURE tools/*.java

echo "=== rules ==="
java -cp build/harness com.sram.hexatype.CoreTest

echo
echo "=== frames ==="
java -Xmx512m -cp build/harness com.sram.hexatype.Preview "${1:-640}" "${2:-1400}" "${3:-3}" out
ls -la out/*.png
