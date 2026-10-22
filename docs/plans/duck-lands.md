# Duck Lands — Suds Springs

Branch: `feat/duck-lands`, starting from main `46a8327`.

## Direction

The user chose a yellow squishy duck boss, a bathtub-themed land, and Bubble Riders.
**Suds Springs** is the working land name; **Sir Waddles** is the working boss name.
The user refined the composition: thick tub walls run along both screen edges as a border,
with the water filling the playfield. The name and boss fight below remain design proposals. Bubble Riders are the selected
new enemy direction. The user also requested an unmistakable, visually appealing bathtub
and asked how the environment scrolls.

A giant cozy bathtub becomes the landscape: porcelain banks, turquoise bathwater,
pastel tile cliffs, pearly foam islands, floating pink soap and a gently dripping tap.
Keep the central prompt lane uncluttered. Foam stays near the edges; water ripples are
broad and low contrast. Bubbles use translucent fills, curved pearly highlights and a
thin colored edge, with visible prompts throughout. No raster sprites or blur filters.

Distinctive scenery and skits:

- The adventure letters sit in shallow bathwater, with foam in front and bubbles rising behind.
- A companion surfs a soap bar, overbalances, then lands in a soft pile of foam.
- A companion blows a tiny bubble that grows, lifts them briefly, and pops into sparkles.
- Water sloshes gently and soap props bob at different phases; cosmetic movement never shifts
  the player keys, damage line or enemy collision geometry.
- Clear effects use mint, cream and pale pink droplets with small pearly rings. Leave the
  loud rainbow bursts to Rainbow Land.

## Scrolling and depth

The tub is a stable frame around the playfield. Its thick porcelain sidewalls run continuously along the left and right screen edges,
extending past the top and bottom of the viewport. Broad cream highlights, a shaded inner
face and a soft waterline shadow give the border depth. The chrome faucet attaches to the
edge. These stay anchored so the water fills the playfield rather than sitting inside a
small, fully outlined bathtub. The water carries the sense of travel: broad ripples drift downward slowly,
foam islands and soap drift at different speeds, and occasional bubbles rise against that
current. Offset repeat phases and horizontal placements so details do not march in columns.
No repeating faucets or entire bathtub sprites scrolling through the playfield.

Enemy movement, keys and the rescue line retain their normal coordinate space; cosmetic
current layers never move touch targets. Let floating props fade at the water boundary rather
than visibly teleport at their wrap point when integrating the live renderer. In the boss
arena, transition the current into side-to-side slosh linked to the duck's balance.

## Bubble Riders

A short, fully readable word floats inside one soap bubble. Tap its bubble once to pop the
shell, then type the visible keys normally. This is a touch shield, not hidden-word fog.
The tap removes the shell only; it never clears a prompt or changes the word order.

Initial tuning proposal:

- Begin with one or two ordinary single-press keys. Start at 10% of solo spawns in the first
  stage, rising toward 20% later. At most one shielded Rider at a time for the first pass.
- Keep the whole word and bubble inside the playfield. Float gently sideways with bounded
  sway while descending; do not use sudden rush acceleration or wrap between edges.
- Add a generous touch margin outside the visual bubble. One down/up tap on the same bubble
  pops it; dragging, extra fingers, cancellation and touches beginning on the key deck do not.
- Typing against the shell gives a gentle bubble wobble and tap cue without resetting other
  words or causing a wrong-key penalty. Target selection must still allow eligible unshielded
  enemies to be cleared. Preserve the ordinary close-to-damage-line targeting rule.
- A pop produces a brief pearly ring and droplets; the normal word continues from its current
  position and speed immediately. No extra wait, word restart or hidden reveal animation.
- Ninja swipes, Team Squish contact, Flurry hits and rescue swipes bypass/pop the shield through
  their normal action. Do not force players out of a power-up to tap a shell.
- Introduce the first Rider using the existing one-time enemy lesson, spotlighting the actual
  bubble and demonstrating its tap. Add the keyboard equivalent before enabling the type in
  production; keyboard-only players must have a reliable shield-pop action.
- Do not combine this type with fog, divebomb, rush, stacked pips or linked pairs initially.
  Keep the existing per-enemy press caps and ordinary damage/clear flow.
- Later Survival inclusion is a separate tuning step after the land mechanic is tested.

## Duck boss proposal

A yellow jelly bath duck with a huge rounded belly, little wings, orange bill, expressive
cheeks and a tiny wobbling crown. Its rest silhouette should read as a rubber duck even
without facial details. Use the shared spring-body system, layered amber underside and
broad pale highlights. The existing Sunny Duck is only a proportion/material stand-in in
this first visual study; the boss needs its own silhouette and linked head/body motion.

Proposed loop: defend bubble prompts with keys; alternating horizontal play-area swipes rock
his balance; an upward rescue-line swipe sends a wave underneath an off-balance duck and
flips him; tap his exposed belly during the belly-up opening. Wing flaps, the tilted belly
and a lit rescue bar should communicate readiness. The bar has a boss-owned wave charge,
separate from the normal once-per-stage rescue allowance. Do not ship a new fight until
its prompt targeting, pause/cancel behavior, keyboard route and bounded bot are implemented.

## Integration sequence

1. **Started:** this scope and portrait vector studies of the bathtub, Bubble Rider shell/pop,
   and duck composition. These are preview tools, not a reachable unfinished chapter.
2. Implement and test Bubble Rider state, generous tap routing in Android and iOS, keyboard
   access, power-up interactions and the contextual lesson. Keep generation disabled until
   the chapter and boss can complete normally.
3. Implement bath scenery/title/skits and the playable duck fight, with bounded players.
4. Append the chapter after Rainbow Land through `WorldCatalog`: expected stages 31–35,
   land ID 7, boss ID 6 and collectible ID 62, subject to rechecking main before integration.
   Preserve Cave's reserved slot and all saved IDs. Do not insert an existing boss as a placeholder.
5. Add the boss collectible, themed Time Attack turtle, portraits, unlock/discovery flow,
   tutorial, defeat/retry, Halloween appearance/name, game-service mappings and achievements.
   Archive/migrate all-boss records when the catalog grows; preserve per-boss records.
6. Validate Adventure and Time Attack reachability, both native input routes, new/loaded saves,
   companion art, all-boss summaries, clear/reset behavior and 30/60/120fps bounded play.

Follow [the land and boss integration checklist](../engineering/adding-lands-and-bosses.md).

## Reproduce the visual study

`tools/DuckLandPreview.java` uses the game's pure `Painter`, key artwork and existing duck
collectible renderer. Compile it with the normal headless tools, then run:

```sh
java -cp build/harness com.dddumpling.game.DuckLandPreview 393 852 out/duck-land
```

It exports portrait scenery, shield/pop and boss composition studies. The installed app
remains the requested build from main, without these prototype changes.
