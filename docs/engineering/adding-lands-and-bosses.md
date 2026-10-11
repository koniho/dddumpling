# Adding lands and bosses

Implementation map from [issue #168](https://github.com/koniho/dddumpling/issues/168),
Swampland / Mossy Maw. This lists the actual integration points; adding art and a
`Boss` constant alone does not make a playable, persistent encounter.

## Stable identities and progression

- Append land and boss IDs. `Lands` owns colors, scenery dispatch, stage mapping,
  production availability, starting stage and the boss whose friend unlocks a land.
- Do not assume land ID equals chapter index. Cave keeps land/save slot 4 and is
  developer-only; Swampland keeps slot 5. It occupies stages 26–30 in developer
  builds and 21–25 in production. Mossy Maw appears once, on that chapter's last stage.
- Use `Lands.firstStage`, `Lands.forStage`, `Lands.playable` and `Lands.unlockBoss`.
  `GameCore.startGame`, title scenery and `Progress.beginRun` must agree on the mapping.
- `LandPicker` owns unlocks, suppression after resets, discovery and saved land scores.
  Awarding Agaric Bud unlocks Swampland (and the developer cave); preserve old land bits.
- Boss constants index parallel arrays in `Boss`: names, blurbs, faces, health, cycles,
  width and jiggle. Add the `kindFor` encounter and a realistic enrage warning.

## Fight lifecycle and input

- Keep new mechanics in an owned helper (`Alligator` here). Integrate `begin`, `update`,
  `press`, `open` / `asksFor`, and `leave` through `Boss`.
- Place geometry before intro/defeat early returns. Drawing and collision must share it.
  Clear pointers and active hazards on pause, death, defeat and restart, including
  retained boss-victory snapshots.
- `BossPlay` connects verdicts to score, shots, sounds, progress and rewards. Return damage
  only on actual contact, then use the ordinary `takeHit` / game-over / stage-clear flow.
- Route touch input in **both** `GameView` and `ios/java/.../IOSGame`, after overlay/pause
  handling and before competing field gestures. Track pointer IDs, historical move
  samples, final lift movement and cancellation. Preserve key-deck input and keyboard taps.
- Alligator tuning: alternate left arm, right arm, tail; 0.60s tell, 0.85s reach, 0.38s
  recoil, 0.25s gap. Swipe at least 8% of screen width in any direction across the glowing
  tip, with 7% width of extra catch tolerance. Unblocked tip contact damages the companion.
  Nine successful blocks open a 3.4s any-key window, capped at six hits. Twelve HP requires
  two windows. Misses preserve accumulated blocks; an expired window resets block progress.
- Do not add a mandatory mechanic without updating both `Check.bossPlay` (reachability)
  and `Bot` (limited reaction, input speed and errors). An ignorant bot can silently stall
  every later-stage pacing test.

## Art, onboarding and rewards

- `AlligatorArt` uses the shared `Softbody` solver for head and tummy, layered green skin,
  pale belly plates and the same limb endpoints used by collision. Shared portraits feed
  display case, companion, boss selector, summaries and rewards.
- `Lands.prop`, `Lands.skit` and `AdventureTitle` need scenery/title dispatch; reuse a
  land helper such as `Swamp`. Do not consume gameplay RNG from drawing.
- Add `BossScreen` tint/body dispatch, intro text and defeat visuals. `BossVictory` also
  renders a retained live boss after player death. Check all three lifecycle states.
- Add `TutorialSpeech` copy, pages and demonstration, plus `Onboarding.bossSteps` and a
  durable learned bit. Preserve existing bits (including the pending companion bit).
- Add the stable analytics key in `Progress.BOSSES`; test first-hit and completion tracking.
- Add a boss taunt in `Sfx`; its table is indexed by `Boss.COUNT`. Reuse existing feedback
  for block/daze/hits unless new audio is needed. Native sound banks use those IDs too; keep the iOS taunt guard tied to `DDBoss_COUNT`.
- Boss friends require rows in **every** `Collect` and `Lore` table; portrait dispatch in
  `BossCollect` / `Trinket`; award through `Interlude`; and collection tests.
  Duck reward bounds must use `DUCK_FIRST + DUCK_COUNT`, never the end of the catalog.
  Use `Collect.bossPrize` / `bossKind`, never `BOSS_FIRST + boss`: ducks occupy IDs 49–59,
  so Mossy Bud is appended at 60 while the original boss friends stay at 45–48.
- Collection ID 60 required a save-tag migration: old 60-bit data tagged `0x5...` decodes
  without interpreting its tag as ownership. New 61-bit data uses `0x6...`. Future catalog
  expansion must revisit this packed representation before allocating additional bits.
- Death blurbs are persisted numeric IDs. Append new blurbs and explicitly map boss IDs;
  do not shift old scenery-death meanings when the boss count grows.

## Time Attack and history

- Register the boss in normal selector, all-boss run, title skit, availability and stats.
  `TimeAttack.encounter` must choose the correct Adventure stage for scenery.
- Increasing `Boss.COUNT` changes `ALL`, choice-array sizes and save length. Version 4
  loads old versions 1–3 and preserves individual boss records. Previous four-boss records
  are retained in `classicBest` / `classicHistory` and re-saved; the five-boss challenge
  starts a new comparable record. The existing score UI shows the current challenge;
  a UI for browsing that legacy archive is not implemented.
- Include the archive in clear-progress/reset behavior. Test migration and re-save, not
  just fresh saves. Test per-boss and all-boss completion, defeat and accuracy/defense stats.

## Validation and delivery

- Add pure helper files to `check.sh`'s `PURE` list. `build.sh` finds Android sources, but
  the harness list is explicit. The iOS Java build also needs to compile the same helpers.
- Add focused collision, state/cleanup, save and progression tests (`TestAlligator`).
  Run bounded fights at 30/60/120fps; verify there is no tap/jitter or parked-finger block.
- Add selected portrait previews (`168-` frames): attack, tail, daze, defeat and title.
  Inspect frames and `DOES NOT FIT` diagnostics at small and regular phone sizes.
- Check production land routing separately: a developer build cannot prove hidden-land
  behavior. Validate native iOS input where the available toolchain allows it.
- For requested install after shared changes use `./build.sh --developer --rules-checks`:
  one full rules/bounded/fuzz gate, then packaging. Reuse `build/debug.keystore` and install
  `com.dddumpling.game.dev` with data preserved. Never commit generated APK/audio/frames.
