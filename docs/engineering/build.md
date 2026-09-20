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

Termux cannot normally read the game's crash logcat. For a reported crash, ask for the
stack trace shown by `Crash.java` on the device. Without paired adb, the package installer
needs a tap; `termux-open` also needs `allow-external-apps = true` in Termux settings.
