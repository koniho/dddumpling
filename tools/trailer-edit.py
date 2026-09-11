#!/usr/bin/env python3
"""Build caption filters from the maintained trailer chapter list."""
import json
from pathlib import Path

root = Path(__file__).resolve().parent.parent
chapters = json.loads((root / "app-store/video/chapters.json").read_text())
out = root / "build/trailer"
out.mkdir(parents=True, exist_ok=True)
font = "assets/fonts/Quicksand.ttf"
landscape = []
portrait = ["scale=540:1182:flags=lanczos", "pad=720:1280:90:70:color=0x1b1730"]
previous = 0
for i, (start, end, title, detail) in enumerate(chapters):
    assert start == previous and end > start, "Chapters must be contiguous"
    previous = end
    (out / f"caption-{i}.txt").write_text(title)
    (out / f"detail-{i}.txt").write_text(detail)
    enable = f"gte(t,{start})*lt(t,{end})"
    for stem, size, color, y in [
        ("caption", 48, "9ee65b", 665), ("detail", 29, "f6f1ff", 745)
    ]:
        landscape.append(
            f"drawtext=fontfile={font}:textfile=build/trailer/{stem}-{i}.txt:"
            f"fontsize={size}:fontcolor=0x{color}:x=145:y={y}:enable='{enable}'"
        )
    portrait.append(
        f"drawtext=fontfile={font}:textfile=build/trailer/caption-{i}.txt:"
        f"fontsize=27:fontcolor=0x9ee65b:x=(w-text_w)/2:y=23:enable='{enable}'"
    )
fade = ["fade=t=in:st=0:d=0.2", f"fade=t=out:st={previous - 0.5}:d=0.5"]
(out / "landscape.filter").write_text(
    "[0:v][1:v]overlay=1360:15:shortest=1," + ",".join(landscape + fade) + "[v]"
)
(out / "portrait.filter").write_text(",".join(portrait + fade))
print(f"Prepared {len(chapters)} chapters, {previous:.1f} seconds.")
