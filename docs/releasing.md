# Executing a release

**Every release request starts with interactive release-note review with the user. Wait for their explicit approval of the final notes before version preparation, tagging, or publishing. A request to release does not approve notes they have not seen.** The tag must contain the notes that describe its build. Do not tag first and figure out the wording afterward.

For adding in-game notes, use the separate [release-notes writing guide](release-notes.md). This document covers release order; platform credentials and upload details remain in [Play publishing](../app-store/play-publishing.md) and [iOS](../ios/README.md).

## 1. Establish the release contents

Work from the intended main commit and identify the last distributed build:

```sh
python3 tools/prepare-release.py context --output build/release-context.json
```

Use `--since <last-distributed-tag-or-commit>` if the nearest tag was not the last shipped build. Review the relevant diffs and merged PRs. Include only the changes in this release; unmerged branch work is not shipped work.

## 2. Review the notes interactively before the version/tag step

- Present each proposed entry to the user: title, game-phase context, player benefit, icon/demo, and automatic-reset choice. Work through edits one entry at a time (or in small groups if they prefer). After the entries are settled, show the final grouped notes and destination summaries and obtain explicit approval to proceed. If release contents change afterward, review the affected notes again. Read-only research and draft previews may continue while waiting; do not interpret silence as approval.
- Follow [Writing in-game release notes](release-notes.md). Each feature must say **where in the game it applies** and **what changes for the player or why it matters**. Keep the voice light.
- Coalesce redundant entries before approval: one entry per distinct player-facing change. Fold a new feature’s related behavior, polish, and fixes into that entry; keep the copy brief. Put only remaining standalone fixes under one bug icon, omitting it when none remain. See [Combine redundant entries](release-notes.md#combine-redundant-entries).
- Decide `autoReset` for every entry while creating the notes; see [reset guidance](release-notes.md#decide-whether-the-demo-resets). Verify the choice by activating each demo and waiting more than two seconds.
- Review the popup renders and interactions. Confirm the newest in-game release has the planned shared release version.
- Write the Play summary in `build/release-notes.txt` using the same reviewed facts; keep it within the existing 500-character limit.
- Update `ios/store/en-US/what_to_test.txt` for TestFlight, and prepare the intended GitHub/itch release copy before tagging when those channels are in scope.

The JSON catalog keeps historical notes. All authored releases remain available in the vertically scrollable in-game list. It does not publish store notes or bump versions.

## 3. Prepare and verify the release commit

```sh
python3 tools/prepare-release.py prepare --notes build/release-notes.txt
# Inspect the planned version/code, then apply the preparation:
python3 tools/prepare-release.py prepare --notes build/release-notes.txt --write
# Substitute the actual planned version below:
python3 tools/release-notes.py check --version 0.1.20
./check.sh -q
```

Confirm the Android code is unused before preparing it. Review the manifest, version-specific Play changelog, in-game JSON/generated copy, and any TestFlight notes together. Follow the platform's required build checks and commit these files before tagging. If the release scope changes during review, update the notes and check again.

For TestFlight, keep the iOS marketing version unchanged and increment the build number unless preparing a public App Store version. The shared in-game release number follows the release tag; it need not equal that retained iOS marketing version.

## 4. Tag the verified main commit

When carrying out an authorized release, merge the reviewed preparation into main, check that the intended main commit includes it, and run the version check again before tagging. For example:

```sh
python3 tools/release-notes.py check --version 0.1.20
git tag v0.1.20
git push origin v0.1.20
```

Use the actual release version. Verify the tag matches the manifest and notes, and monitor the Android, iOS, and itch workflows requested for the release. A successful build or tag push alone does not confirm an upload or tester availability. Use the platform publishing docs for retries; don't move a published tag to repair missing notes or reuse a consumed build/version code.
