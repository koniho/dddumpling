#!/usr/bin/env python3
"""Summarize exported local difficulty JSONL; checkpoints are cumulative, not additive."""
import argparse
import csv
import json
import sys
from collections import Counter


def stages(path):
    latest = {}
    with open(path, encoding="utf-8") as source:
        for number, line in enumerate(source, 1):
            if not line.strip():
                continue
            try:
                row = json.loads(line)
                if row.get("schema") != 1:
                    raise ValueError("unsupported schema")
            except (ValueError, AttributeError) as error:
                print(f"Skipping line {number}: {error}", file=sys.stderr)
                continue
            event = row.get("event", "")
            if event.startswith("stage_") or event in {"checkpoint", "export_snapshot", "damage", "rescue", "power_start", "power_end"}:
                latest[(row["run"], row["attempt"])] = row
    return list(latest.values())


def summarize(row):
    correct, misses = row.get("correct", 0), row.get("misses", 0)
    seconds = row.get("stage_s", 0)
    return dict(run=row["run"], stage=row["stage"], attempt=row["attempt"],
                outcome=row["event"].removeprefix("stage_"), seconds=seconds,
                tier=f'{row.get("start_tier", row["tier"])}->{row["tier"]}',
                accuracy=round(100 * correct / (correct + misses), 1) if correct + misses else "",
                clears=row.get("clears", 0), deepest=row.get("deepest_enemy", 0),
                halfway_pct=round(100 * row.get("halfway_s", 0) / seconds, 1) if seconds else 0,
                damage=row.get("damage", 0), rescue=row.get("rescue", False),
                reason=row.get("decision", "not_assessed"), infinite_lives=row.get("infinite_lives", False))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("log")
    parser.add_argument("--csv", action="store_true")
    args = parser.parse_args()
    rows = [summarize(row) for row in stages(args.log)]
    if not rows:
        print("No stage records found.", file=sys.stderr)
        return
    if args.csv:
        writer = csv.DictWriter(sys.stdout, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)
        return
    for row in rows:
        print(f'Run {row["run"]} stage {row["stage"]} attempt {row["attempt"]}: '
              f'{row["outcome"]}, {row["seconds"]}s, tier {row["tier"]}, '
              f'accuracy {row["accuracy"]}%, clears {row["clears"]}, '
              f'deepest {row["deepest"]:.2f}, below halfway {row["halfway_pct"]}%, '
              f'{row["reason"]}' + (' [infinite lives]' if row["infinite_lives"] else ''))
    reasons = Counter(row["reason"] for row in rows if row["outcome"] == "cleared")
    print("\nStage decisions:", ", ".join(f"{reason}={count}" for reason, count in reasons.items()) or "none yet")
    print("Partial records are checkpoints, not completed stages. Depth 0=playfield top, 0.5=halfway, 1=damage line.")


if __name__ == "__main__":
    main()
