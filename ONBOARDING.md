# First-use guide

[AGENTS.md](AGENTS.md) is the workflow source of truth. Read this guide only when setup or
orientation is needed; there is no mandatory full test or render on entering the repo.

1. Inspect `git status` and locate the relevant symbols with `rg`.
2. Search [GLOSSARY.md](GLOSSARY.md) if a player-facing name is unfamiliar.
3. Use the [code map](docs/engineering/code-map.md) when the owning file is unclear.
4. Select the smallest useful check from `AGENTS.md`, edit, and verify the final branch once.

## Headless development

The shared Java logic and drawing run without an SDK or device. You need a JDK (Java 17
works), Python 3 and Bash. Native setup is documented in [README.md](README.md).

```sh
./check.sh -q -r -s Stages        # focused rule checks during iteration
./check.sh -q -r                 # full rules, bounded players and fuzz
./check.sh -q -f 60 -c 0,.1,1,.45 # selected visual preview, only for relevant visual work
```

`Painter` connects shared rendering to native hosts and the headless rasterizer. Keep
platform imports out of pure Java and register new pure files in `check.sh`.
Do not run checks concurrently in one checkout; outputs share `build/harness` and `out/`.

Historical pitfalls are indexed in [docs/engineering](docs/engineering/index.md). Search the
relevant topic, rather than reading the archive. Current code takes precedence over historical
numbers and old bot limitations.

Build, deploy and release only when explicitly requested. A PR does not require installing
the game. See `AGENTS.md` for the release-note approval rule and verification defaults.

A requested local install can proceed after `./build.sh --developer` passes its smoke checks
and APK verification. Full exports and soak runs are not prerequisites for that install.
