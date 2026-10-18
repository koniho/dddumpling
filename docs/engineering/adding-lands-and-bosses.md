# Adding lands and bosses

Implementation map from [issue #168](https://github.com/koniho/dddumpling/issues/168),
Swampland / Gummy Gator. This lists the actual integration points; adding art and a
`Boss` constant alone does not make a playable, persistent encounter.

## Stable identities and progression

- Append land and boss IDs. `Lands` owns colors, scenery dispatch, stage mapping,
  availability, starting stage and the boss whose friend unlocks a land.
- Do not assume land ID equals chapter index. Cave keeps land/save slot 4 and is
  unreachable while its introduction is reworked; Swampland keeps slot 5 and occupies
  stages 21–25 in every build. Gummy Gator appears once, on that chapter's last stage.
- Use `Lands.firstStage`, `Lands.forStage`, `Lands.playable` and `Lands.unlockBoss`.
  `GameCore.startGame`, title scenery and `Progress.beginRun` must agree on the mapping.
- `LandPicker` owns unlocks, suppression after resets, discovery and saved land scores.
  Awarding Agaric Bud unlocks Swampland; preserve old land bits.
- Display names can change without migrating persisted analytics IDs: Gummy Gator keeps
  the original `mossy_maw` event ID in `Progress.BOSSES`.
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
- Alligator tuning: choose a random left arm, right arm or tail attack, excluding the previous
  limb. Choose once during the gap so the eye tell matches the committed attack. 0.60s tell, 0.85s reach, 0.38s
  recoil, 0.25s gap at full health. The tell coils and holds the limb; cubic strike
  progress accelerates into contact without changing those durations. All four durations shorten with damage, using
  speed `1 + 0.85 * (1 - max(0.75, healthFraction)³)` (up to 1.4914×, reached at 75% health);
  each attack keeps its starting speed. Later difficulty comes from overlapping combos.
  Swipe at least 8% of screen width in any direction across the glowing
  tip, with 7% width of extra catch tolerance. Unblocked tip contact damages the companion.
  Every parry opens a 0.85s attack window with a slumped head, loose arms and dizzy eyes.
  Four separate parry-and-hit rounds without taking player damage extend the opening to 3.4s,
  capped at 25% of maximum health (18 hits including the triggering hit). Only the first hit
  in each short opening counts. Player damage resets the streak and invalidates the current
  opening; a fresh parry is needed to start earning rounds again. Long-window completion
  resets the streak, which has no pip display. The first
  cycle shows a swipe marker; later attacks use
  a limb wind-up and brighter palm/tail scales. Jointed arms idle independently; the tail
  sweeps below the screen between sides during rest and daze, with pose shared by drawing
  and collision. Wandering paws commit their attack origin at the tell and blend back on recoil.
  The long torso continues below the screen; segment springs curve its lower half toward
  the tail's current side and follow smoothly as it crosses. Daze
  and damage trigger wriggling plus bounded, clock-driven splash bursts without gameplay RNG.
  An early key press crosses the existing arms, pauses the current limb (without cancelling
  its progress), then fires one random one-letter bolt after 0.30s. Further blocked presses
  during the counter or 1.5s after its projectile launches only bounce; they neither queue
  replies nor extend the cooldown. The block/tail animation takes 0.65s.
  Matching bolts have input priority. At most one reply waits for a free live-bolt slot;
  death and leaving clear the reply and cooldown. The entrance lasts 4.8s; other bosses retain 1.6s.
  His eyes narrow and focus on the companion in the final 0.28s (or final two long-window
  hits); no countdown or stop text is shown. Late key presses trigger counter projectiles.
  Below half health, each new attack is a shuffled two-limb combo, increasing to three
  limbs strictly below quarter health, with 0.52s between
  starts. Limbs keep independent wind-up, contact and recoil; the entire combo can damage
  the player only once. Parries still open short windows while other limbs continue, and
  earning the long dizzy window staggers remaining limbs and cancels queued strikes.
- Do not add a mandatory mechanic without updating both `Check.bossPlay` (reachability)
  and `Bot` (limited reaction, input speed and errors). An ignorant bot can silently stall
  every later-stage pacing test.

## Art, onboarding and rewards

- `AlligatorArt` uses the shared `Softbody` solver for head and tummy, layered green skin,
  pale belly plates and the same limb endpoints used by collision. Shared portraits feed
  display case, companion, boss selector, summaries and rewards.
- Collectible physics live in `GameCore` (`GatorCompanion`), update on the game clock,
  and reach every portrait through `Painter.gator()`. Forward that accessor through
  painter wrappers. Drawing must not step springs; forward mood, look and Ninja state
  from `BossCollect` and trigger reactions from run events and story taps.
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
  so Lil Chomp is appended at 60 while the original boss friends stay at 45–48.
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

## Rainbow Land (#151)

Rainbow Land appends land ID 6 after Swampland: stages 26–30, unlocked by Lil Chomp.
Rainbow Cat appends boss ID 5; Sprinkle Paws appends collectible ID 61. Cave retains
its unreachable ID 4. Scenery repeats after stage 30 and the post-boss ramp starts there.

`RainbowCat` owns the single-pointer chase, yarn and cage state. Yarn wins overlapping
hit targets; an existing grab cannot transfer objects. Historical and final-lift samples
route through the same pure touch method on Android and iOS. Cage latching consumes the
capture gesture through lift, while other fingers can answer its prompt. Yarn rests during
cage prompts. Three captures finish the fight; prompts grow from three to five keys.
Wrong keys do not reset progress. Escape returns to chase without damage. Yarn contact
costs one heart, resets safely and cannot repeatedly damage across frames. Kids Mode uses
slower pursuit, larger catches, longer yarn travel and a nine-second cage deadline.

`RainbowCatArt` traces a frosted pastry body, pointed ears, paws and tail with shaded
vector shapes. Body and head use separate `Softbody` instances advanced on the game clock.
The user's Nyan Cat sprite sheet (Pictures/file_00000000a3c081f7894ea924c0332460.png)
was inspected as an appearance/pose reference; no raster sprites are included or rendered.
`RainbowLand` supplies scenery, title arches, picker artwork and stage vignettes.

The 62-entry collection uses tag `0x8...`, preserving the older 61-entry `0x6...` and
60-entry `0x5...` decoders. Their tag bits must never become Rainbow Cat ownership.
Time Attack save v7 carries its boss/profile counts and archives old all-boss records by roster size;
six-boss records start empty. Existing individual records keep their IDs. Transient
onboarding introduction bits use a long now that lesson IDs extend past 31; persisted
extra lesson bits retain their original positions.


## Authoritative content counts

`WorldCatalog` owns chapter order, boss metadata and skin names, land colors, friend IDs,
and saved death IDs. Append rows without renumbering existing IDs. `Boss.COUNT` and
`Lands.COUNT` derive from those rows; stages, unlocks, rewards, analytics and death blurbs
use catalog lookups. Visual dispatch and bespoke mechanics still need implementation.
`Collect.NAME`, `Glyph.NAME`, `Skins.NAMES`, `Power.OFFERED`/`DEBUFFS` and the named
Survival profiles own their respective counts. Fixed geometry and authored effect tuning
are not catalog sizes.

Time Attack v7, Survival v3 and run-history v10/v11 store their dimensions. Older decoders
retain historical widths on purpose. New all-boss runs cannot inherit a shorter roster's
record. Audio preserves the original five-boss/six-key ID banks and appends extra voices;
native iOS callers use runtime getters rather than translated constant macros.

Storage is still bounded: collectibles have 63 bits beneath their tag, land state has
eight-bit visit/suppression fields, and tutorial introductions have 64 transient bits.
Catalog regression checks fail before the first two capacities overflow; widen/migrate
these formats before exceeding them. Do not simply increase a count and shift saved bits.

Rainbow Land uses more Rush followers (92% of eligible preceding words, 39% of eligible
power-up spawns) and Divebomb words (36% of eligible multi-key candidates). Existing
spawn phases choose variants without extra RNG draws. Enemy clears use vibrant rainbow
particles and star bursts. The experimental rainbow wakes have been removed.
