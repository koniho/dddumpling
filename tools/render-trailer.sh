#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Codex's Termux launcher may prepend its own older libc++; only override if FFmpeg fails.
if ! ffmpeg -version >/dev/null 2>&1; then
  termux_lib=/data/data/com.termux/files/usr/lib
  if [ -d "$termux_lib" ]; then export LD_LIBRARY_PATH="$termux_lib"; fi
  ffmpeg -version >/dev/null
fi
mkdir -p build/trailer app-store/video
if [ "${1:-}" != "--edit-only" ]; then
  if [ ! -d build/harness/com/dddumpling/game ]; then ./check.sh -q -r; fi
  javac -cp build/harness -d build/harness tools/Trailer.java tools/TrailerPlate.java
  java -Xmx512m -cp build/harness com.dddumpling.game.Trailer
  java -cp build/harness com.dddumpling.game.TrailerPlate
fi
python3 tools/trailer-edit.py
ffmpeg -y -hide_banner -loglevel error \
  -i build/trailer/gameplay-silent.mp4 -i build/trailer/soundtrack.wav \
  -filter_script:v build/trailer/portrait.filter -map 0:v -map 1:a \
  -af loudnorm=I=-16:TP=-1.5:LRA=11 \
  -c:v libx264 -preset fast -crf 19 -pix_fmt yuv420p \
  -c:a aac -b:a 160k -ar 44100 -movflags +faststart -shortest \
  app-store/video/dddumpling-gameplay-portrait.mp4
ffmpeg -y -hide_banner -loglevel error \
  -loop 1 -framerate 30 -i app-store/video/landscape-background.png \
  -i build/trailer/gameplay-silent.mp4 -i build/trailer/soundtrack.wav \
  -filter_complex_script build/trailer/landscape.filter -map '[v]' -map 2:a \
  -af loudnorm=I=-16:TP=-1.5:LRA=11 \
  -c:v libx264 -preset fast -crf 19 -pix_fmt yuv420p \
  -c:a aac -b:a 160k -ar 44100 -movflags +faststart -shortest \
  app-store/video/dddumpling-gameplay-landscape.mp4
for video in app-store/video/dddumpling-gameplay-*.mp4; do
  ffprobe -v error -show_entries stream=codec_name,width,height,avg_frame_rate \
    -show_entries format=duration,size -of json "$video"
done
