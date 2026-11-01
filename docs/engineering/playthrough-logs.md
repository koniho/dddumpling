# Local difficulty playthrough logs

Developer builds automatically record Adventure runs on Android and iOS. This is structured
measurement data, not video or a deterministic replay. Production builds do not record it.
Nothing is sent to a server or added to cloud progress. No names, accounts or device IDs are collected.

Open **Developer Settings → Run → Export Playthroughs**. Android opens the file picker (choose
Downloads); iOS opens the share sheet (Save to Files). Export while playing includes a snapshot
of the current stage. Send the resulting `dddumpling-playthroughs.jsonl` for analysis.

```sh
python3 tools/analyze-playthroughs.py dddumpling-playthroughs.jsonl
python3 tools/analyze-playthroughs.py dddumpling-playthroughs.jsonl --csv > stages.csv
```

The schema version is 1. Each line identifies a run by a monotonic timestamp-based ID and stage
attempt. Run starts include app/build versions. Stages record starting/ending assistance,
key count, signed confidence, effective word/speed/spawn settings, accuracy inputs, damage,
rescue use, power-up types, ordinary/rush/fog/dive/pair clear counts and clear depth/timing.
Difficulty decisions record `strong_accuracy`, `strong_control`, `damage`, `hold`, `power_hold`
and `boss_or_cave_not_counted`. Rescue is recorded but is not a difficulty penalty. Power-up stages
always hold. The legacy `streak` / `start_streak` fields now contain signed confidence points.

Depth is normalized over the playable approach: 0 at the top, 0.5 halfway, 1 at the damage line.
`deepest_enemy` tracks the furthest active threat, not only enemies eventually cleared.
`halfway_s` measures active time with any threat below halfway; separate top-entry metrics
exclude side-entry enemies that can start lower down. A clean stage with little or no halfway
pressure is evidence the player has spare capacity. The game separately tracks whether top-entry
enemies cross halfway, even when logging is disabled, as an alternative strong-play signal. Side-entry depth does not affect that signal.

All stage measurements are cumulative. Analyze the last record per `(run, attempt)`; never sum
checkpoints. `stage_cleared` is a completed stage; death, abandonment, retries and developer jumps
have different outcomes. Missing terminal records mean interrupted runs, not wins or losses.
Infinite lives are flagged. Bosses are included for context but are not ordinary-stage samples.

The newest 256 Ki characters of complete JSONL records are retained. Older runs may be partial
at the retention boundary. Active-stage checkpoints occur every 15 seconds, plus milestone events;
pauses, settings, tutorials and minigames do not advance active time. A force-close can lose the
most recent interval. Native file writes use serial background queues and atomic replacement;
export uses the in-memory snapshot, including any data waiting to be saved.

The private file is `difficulty-playthroughs.jsonl` in Android app files and alongside the iOS
local store. `PlaythroughLog` owns the shared collector; `Store` is the persistence seam. Keep
recording off the gameplay RNG path and avoid per-input/per-frame disk writes.
