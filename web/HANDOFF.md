# YouTube Playables handoff — 2026-11-03

## Read this first: access is the blocker

**Testing inside the YouTube Android/iOS app requires an onboarded channel and access
to the invitation-only Playables Developer Portal.** The public SDK Test Suite does
not grant that access. Portal access has not been established.

**Submission update:** on 2026-11-03 the user reported submitting the Playables interest
form. Treat it as submitted and awaiting a possible response; do not submit a duplicate.
No receipt/reference or invitation was provided. No partner agreement, portal upload
or public release was performed by this agent.

The [official interest form](https://docs.google.com/forms/d/e/1FAIpQLSdvdQ0lgIq2369aemj1O6w8R8FwGn9O5ARRGODDDUbVINCRJQ/viewform)
publishes no response timeline and says they may not respond to every submission;
they contact developers if interested. Do not invent an expected review window.

The previous agent presented in-app testing as the immediate next step without making
this prerequisite clear. The user was understandably frustrated. Do not repeat that
claim or send the user looking for an app preview link we cannot generate yet.

Once invited, upload the Playables ZIP in the portal. Its **Test** step provides a
YouTube preview link/QR code for app testing before publication. Until then, the
available routes are browser testing and the public official SDK Test Suite.

- [Portal access and preview instructions](https://developers.google.com/youtube/gaming/playables/developer_portal)
- [Playables overview and interest form](https://developers.google.com/youtube/gaming/playables)
- [Public Test Suite](https://developers.google.com/youtube/gaming/playables/test_suite)

## Where the work lives

- Tracking: [issue #230](https://github.com/koniho/dddumpling/issues/230).
- Branch: `feat/web-port-230`, pushed to origin; **not merged into main**.
- Draft [PR #232](https://github.com/koniho/dddumpling/pull/232).
- Last implementation/validation commit before this handoff: `5071e9f`.
- Phone worktree: `/data/data/com.termux/files/home/dev/dddumpling/build/worktrees/web-port-230`.
- Root phone checkout is a different branch with unrelated documentation work. Do not
  reset it or copy its dirty files into this branch. On another machine, fetch and
  check out the feature branch after inspecting local changes.
- Build instructions, architecture details and test commands: [web README](README.md).

## Completed

The actual shared Java game runs through TeaVM with browser Painter, input, audio and
Store adapters. Shared gameplay remains authoritative. Cached artwork, adaptive render
resolution and targeted Octopulse/Dark Divide optimizations are implemented; see the
README and PR for their scope and measured limitations.

Desktop controls use S D F J K L plus mouse gestures. For Dark Divide, dragging a ready
glob creates a mirrored virtual finger and uses the existing pinch/split mechanic.
Desktop tutorial narration, animation and hints explain this. Touch keeps two fingers.
Browser Back handling and first-use keyboard practice are implemented.

Two distinct builds exist:

| Artifact / output | Purpose | Persistence |
| --- | --- | --- |
| `web-playable-preview` / `build/web` | Standalone browser play; works with a local web server | Browser-local storage |
| `youtube-playables-preview` / `build/web-playables` | YouTube host; use the official Test Suite or invited portal | SDK load/save |

`python3 web/build.py --playables` enables the production SDK host. It waits for cloud
load before creating the game, protects failed loads from overwriting saves, serializes
and retries writes, and honors SDK pause/resume/mute. It intentionally stops outside a
Playables environment instead of silently substituting local storage.

YouTube settings omit share/rate/privacy links and native service cards. They retain
audio, skins, local milestone totals, reset controls, achievement tracking, accessory
unlocks and saved progression. Native store-update/review actions are blocked too.
The capability is `GameCore.externalServices`, disabled only by the Playables host;
native and standalone settings retain their prior behavior.

The user said the web variant plays great and confirmed testing it on a phone. The
exact phone/browser and coverage across Android versus iOS were not recorded. Do not
claim this establishes YouTube-app testing or validation on both mobile platforms.

## Verification and evidence

- Shared rules: **19,134 passed**, no failures.
- Both revised settings layouts rendered and inspected; no fit diagnostics.
- [Browser run for UI cleanup](https://github.com/koniho/dddumpling/actions/runs/41664909368):
  build, Chromium and WebKit all passed, including the existing boss replays.
- [Final live official SDK run](https://github.com/koniho/dddumpling/actions/runs/41665977394),
  commit `5071e9f`: **passed**. This used Google's actual live Test Suite and SDK, not
  the separate contract fixture.
- All five automated **MUST** checks passed: SDK order, initial size, readiness order,
  game readiness, and save size. The startup-under-five-seconds **SHOULD** passed.
- Live save/reload, failed-save recovery, pause/resume including resumed frames, host
  mute and the documented production Content Security Policy passed.
- Optional `sendScore` is intentionally unimplemented and its check is **untested**.
  Adventure, Survival and Time Attack have different scoring dimensions; no score
  policy was chosen. Optional ads APIs are also unimplemented.
- The first live run measured a 3.07 MiB initial bundle and 0.654 s readiness on the
  CI runner's loopback server. These numbers are not real-device/network benchmarks.
- A public test-page resource returned HTTP 429 in the first run; no game CSP violation
  or tested SDK failure resulted. Raw logs are retained with the artifacts.

The workflow's `official_suite=true` dispatch runs the live check:

```sh
gh workflow run web-port.yml --ref feat/web-port-230 -f official_suite=true
```

Evidence is in the `official-playables-review` artifact (`results.json`, screenshots
and server log). Automation: [official-playables.mjs](test/official-playables.mjs).
Loopback HTTPS/CSP server: [serve-playables.py](tools/serve-playables.py). The runner
trusts a temporary localhost certificate and permits local-network access; it does not
relax the game's CSP. A Test Suite pass is not certification or publishing approval.

## Next agent's task

1. The owner has submitted the interest form. Confirm whether a response or invitation
   has arrived; do not prepare or submit a duplicate application. Be clear about the
   access dependency before proposing any in-app testing.
2. Use the existing [promotion handoff](../app-store/marketing/experiments/223-2026-11-02/promotion-handoff.md)
   and [supercut provenance](../app-store/marketing/experiments/223-2026-11-02/store-supercut.json).
   Do not conflate a Playables application with YouTube Promote advertising.
3. Confirm audience eligibility, asset rights and business terms. The game has Kids
   Mode; the audience question remains unresolved. Renaming it is not a resolution.
   The user submitted the interest form themselves; this handoff does not authorize
   the next agent to accept additional agreements.
4. After portal onboarding, upload a reviewed bundle and use its preview link for
   actual YouTube Android/iOS tests, then complete rating/metadata/review requirements.
   This work has not approved a merge, deployment or release.

## PC playtesting instructions already given to the user

Download **web-playable-preview** from the successful workflow run, unzip it, open a
terminal inside the extracted folder and run:

```sh
python -m http.server 8766
```

Open `http://localhost:8766`. Do not open `index.html` directly or use the YouTube-only
bundle for this simple standalone flow. For local source builds use the README.

The user also asked for Termux notifications when work is done or feedback is needed:
`termux-notification --id codex-task --title '...' --content '...'` on the phone host.
