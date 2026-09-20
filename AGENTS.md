# Working on Hexatype

This is the default workflow. User instructions override it. Read only the references
needed for the current task; onboarding and historical notes are not mandatory startup reads.

## Keep small changes small

- Find the owning symbols with `rg`, then read narrow windows. Avoid whole large source files.
- Search [GLOSSARY.md](GLOSSARY.md) for unfamiliar game terms and use its player-facing names.
- Use [the reference index](docs/engineering/index.md) only when the task needs background.
- Inspect `git status` early. Preserve unrelated work; never include it in the task's commit.
- For a PR from a dirty or unrelated branch, isolate the patch on the intended base before
  final verification. Do not test both checkouts by default or copy whole dirty files.
- Read-only inspection is usually enough for a baseline. Run a baseline test only to reproduce
  a bug, investigate a suspected existing failure, or compare behavior the change must preserve.
- Make the smallest coherent change. Reuse existing mechanics and their tests.
- Add focused regression coverage for new behavior; do not retest every detail of a reused
  helper. No new tests for prose-only or other reversible low-impact edits.
- Update docs only when a contract, terminology, setup or workflow actually changes.
- Report the result, relevant checks and material limitations briefly. Avoid repetitive logs.

## Verification by change

Choose checks before editing. Run them once on the final branch; repeat only after a new
change, failure, or unresolved concern. A focused suite is for iteration, not an extra
required gate immediately before the same suite runs in the full check.

| Change | Verification |
| --- | --- |
| Docs or comments only | Review diff and check touched links; no game tests, renders or build |
| Small isolated rule change | Relevant suite with `./check.sh -q -r -s Stages` (replace suite) |
| Difficulty, shared state, timing, or several systems | One `./check.sh -q -r`; includes bounded play and fuzz |
| Visual or layout change | Relevant rules plus selected frames with `-q -f TAGS`, cropped with `-c` if useful |
| Audio change | Audio checks and only the relevant sound exports/listening |
| Native host or packaging change | Relevant native checks; consult that platform's docs |
| Explicit build, deployment or release | Requested pipeline's checks; do not duplicate them beforehand |

Rules-only is the default for code: `./check.sh -q -r`. Do not generate all PNGs/WAVs for
nonvisual changes. Do not run a full visual baseline just because this is a first session.
For tuning, use bounded players; perfect play alone does not establish fairness.
For visuals, inspect affected frames and their `DOES NOT FIT` diagnostics. Add a Preview
state only when the new state is not already represented. Compare hashes only when preserving
rendering is part of the task. See [rendering notes](docs/engineering/rendering.md).

Never run two checks in the same checkout concurrently: they share `build/harness` and `out/`.
Use separate worktrees if concurrent verification is necessary. Keep output quiet or in a log;
read failures and the tally rather than dumping the full log.

## Architecture and invariants

- Logic and rendering share pure Java through `Painter`. No `android.*` imports in pure files.
  Add new pure files to `check.sh`'s `PURE` list; `build.sh` discovers `src/` itself.
- Audio and persistence enter through `Sound` and `Store`; preserve the headless harness seam.
- Keep `GameCore.update` and `tapKey` together: their ordering matters. Other large files can
  split along static helpers taking `GameCore c`, following `Fx` and `Pacing`.
- Preserve RNG call order unless changing generation intentionally. Rendering uses seeded state,
  never wall-clock time or fresh randomness.
- Clear owned state on both normal completion and death, including paths above early returns.
- Existing test classes extend `Check`, renderers extend `Draw`; unqualified helpers are deliberate.
- Prefer visual cues to explanatory UI. Keep comments short and retain the reason for tuned values.
- For input, physics, timing or layout pitfalls, search the relevant [topic](docs/engineering/index.md)
  instead of loading the complete historical archive.

## Build, deploy and release

Builds and deployment are opt-in. Run `./build.sh` or `./deploy.sh` only when the user explicitly
requests that action. A code change, ticket or PR request does not request an APK or installation.
Local developer builds run a small Rules smoke suite, compile, package and verify the APK.
A passing local build is sufficient to proceed with a requested install; no extra full checks
or frame exports. Production builds and `--full-checks` retain the full gates.
Do not precede a build with duplicate checks.
See [README.md](README.md) for platform setup and [ONBOARDING.md](ONBOARDING.md) for first-use help.

For a release, follow [docs/releasing.md](docs/releasing.md): present the release notes and reset
choices, incorporate edits, and wait for explicit approval of the final notes before version
preparation, tagging or publishing. A release request does not approve unseen notes.
Use [docs/release-notes.md](docs/release-notes.md) for the catalog and required validation.

## Repository hygiene

Do not commit local audio, APKs, render output, secrets or unrelated workspace changes.
`res/raw/bgm.*` is a user-supplied local track and must never be committed.
Preserve the provenance and licenses of imported assets. Keep large explanations in topic docs,
not in this always-loaded file. Avoid adding duplicate workflow rules elsewhere.
