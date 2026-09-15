# Fast iOS feedback

`Check iOS port` runs on every PR, with selection inside the workflow. Use **iOS checks**
as the required branch-protection check: it always runs and requires success from every
selected job. Do not require a conditional job instead. No branch-protection settings
are changed automatically by this implementation.

## What runs

| Change | PR checks beyond release configuration |
| --- | --- |
| Documentation/store copy | Selection/cache contract tests only; no macOS runner |
| Shared gameplay Java | Gameplay rules, production guards, iOS input; simulator compilation without boot |
| Sound synthesis or native audio | Shared checks, simulator build, native audio/mixer tests |
| Release navigation, touch routing, settings | Shared checks, simulator build, input smoke tests |
| Native painter, fonts, shared layout | Shared checks, painter tests, scene screenshot sweep |
| Persistence | Shared checks and native storage tests |
| Lifecycle, central GameCore, toolchain/workflows, unknown code | Shared checks and complete native/UI suite |
| Push to main | Everything above plus an unsigned device archive |
| Signed release | Complete native/UI suite before creating/uploading the signed archive |

`ios/scripts/ci-plan.py` defines dependencies. Multiple categories combine their suites.
PR diffs use the merge base, include deletions and both sides of renames, and never
interpolate filenames into shell commands. Known shared game sources compile through
the actual iOS translator even when no simulator is needed; unknown code outside that
family defaults to full validation. Keep this mapping current when adding a native boundary.
Selector/gate tests run before expensive jobs. Unexpected skipped, failed, or cancelled
selected jobs fail the aggregate gate; intentionally unselected jobs must be skipped.

Fast Linux jobs and macOS work run independently after selection. The archive is a separate
job on main/full manual runs; routine PRs do not build it. There is one simulator, not a
fleet of parallel simulators. The screenshot sweep remains a launch/crash check plus manual
review artifact, not an automatic cross-platform image comparison.

## Focused runs

Choose a suite in Actions → Check iOS port → Run workflow, or:

```sh
gh workflow run ios.yml --ref your-branch -f suite=audio
# Other choices: full, build, native, input, render, storage.
```

Manual focused runs are diagnostics. They do not replace the full main/release gate.
On a Mac, run `ios/scripts/test-simulator.sh --full` or `--build-only`. For a focused test:

```sh
IOS_TEST_TARGETS='["DDDumplingTests/DDAudioTests"]' ios/scripts/test-simulator.sh --selected
```

The script still accepts normal xcodebuild test-selection flags. Build-for-testing and
test-without-building separate compilation from execution and retain xcresult artifacts.

## Reuse and timing

Simulator boot begins immediately after Xcode selection, overlapping Java/toolchain setup,
runtime-cache restore, and compilation. Logs record boot readiness, compilation, remaining
boot wait, and test-runner/execution/shutdown time. The latter still includes app installation
and test-runner overhead; do not label it all as test assertion time.

J2ObjC translation is keyed by source content/list, build configuration, prefixes, translation
scripts, pinned bootstrap, translator binaries, and Java version. Cache hits verify generated
file hashes. Misses translate into a temporary directory, remove stale outputs, and only
replace files whose contents changed, preserving unchanged mtimes. Failed translations do
not retain a valid stamp. Debug and Release cannot reuse each other's mismatched flags.

GitHub caches generated code and simulator DerivedData under matching Xcode, architecture,
and build-script/project fingerprints. An exact commit key can fall back to a compatible
prior build; Xcode still checks dependencies and selected tests always execute. A missing
cache takes the full build path. Unsigned archives and signed releases build separately.

Baseline run 34916734299 took 11m31s: 41s simulator compilation, approximately 5m10s startup,
and 1m59s UI tests. Compare warm and cold runs after rollout; no fixed speedup is assumed.

Validate changes locally with `python3 tools/test-ios-ci.py` and shell syntax checks.
macOS CI is required to validate xcodebuild/simulator behavior. Workflow edits also require
GitHub credentials with permission to update workflows (OAuth `workflow` scope).
