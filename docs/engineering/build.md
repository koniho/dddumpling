# Build and device troubleshooting

Historical failure notes, moved from `AGENTS.md`. Read only entries relevant to the task.
Values and file ownership may have changed; verify against current code. Current workflow
policy lives in [AGENTS.md](../../AGENTS.md).

- Termux's `ecj` hardcodes `-7`; use `javac --release 8`. `aapt2 link` takes compiled
  resources positionally, not via `-R`.


Build or deploy only when explicitly requested; neither is routine PR verification.
Local developer builds run only Rules smoke checks before compiling and verifying the APK.
That passing build is sufficient for a requested install. Production builds and developer
`--full-checks` builds retain the full checks. Do not add a duplicate export before a build.
See [README](../../README.md) for SDK setup, signing, device pairing and installation.
For local DDDUMPLING Dev updates, reuse `build/debug.keystore`. A valid signature does not
prove an update is compatible: compare the actual installed certificate and version code.
The local developer build floors its generated version code at the installed Dev version;
tracked production/release metadata is unchanged.

Termux cannot normally read the game's crash logcat. For a reported crash, ask for the
stack trace shown by `Crash.java` on the device. Without paired adb, the package installer
needs a tap; `termux-open` also needs `allow-external-apps = true` in Termux settings.

Developer Android builds keep bounded, app-private `runtime-diagnostics.log` files (current
and previous) with tutorial events, return-to-title callers, lifecycle events and crashes.
Android 11+ also supplies recent process-exit reasons. Nothing is uploaded; no saves are reset.
To inspect/copy the log without ADB, bring the existing activity forward from Termux:

```sh
am start --user 0 --activity-single-top -n com.dddumpling.game.dev/com.dddumpling.game.MainActivity --ez diagnostics true
```

Use **Copy log** in the dialog and paste it into the bug report. Opening it pauses an active
run. A different PID indicates a new process; another `activity-create` in the same PID
indicates activity recreation; `to-title` includes the in-game caller. Exit reasons are
historical, so correlate their timestamps rather than treating every listed exit as new.
