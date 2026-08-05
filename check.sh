#!/data/data/com.termux/files/usr/bin/bash
# Runs the game headlessly: rule assertions, then PNG frame renders into out/.
# Needs no Android SDK and no install — only the pure-Java half of the codebase.
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
src/com/sram/hexatype/Collect.java
src/com/sram/hexatype/Power.java
src/com/sram/hexatype/Buddy.java
src/com/sram/hexatype/GameCore.java
src/com/sram/hexatype/Painter.java
src/com/sram/hexatype/SettingsUi.java
src/com/sram/hexatype/Draw.java
src/com/sram/hexatype/Sky.java
src/com/sram/hexatype/Skits.java
src/com/sram/hexatype/Shape.java
src/com/sram/hexatype/Basket.java
src/com/sram/hexatype/Parade.java
src/com/sram/hexatype/Finish.java
src/com/sram/hexatype/Trinket.java
src/com/sram/hexatype/Lore.java
src/com/sram/hexatype/Showcase.java
src/com/sram/hexatype/Storybook.java
src/com/sram/hexatype/Hud.java
src/com/sram/hexatype/Screens.java
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
