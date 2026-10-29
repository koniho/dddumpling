# Achievement accessories

Issue #214 adds cosmetic progression: discover an appealing item, play toward its
achievement, earn it, then customize a favorite companion in the display case.
The current art is a first pass for iterative review. Scene portraits and gallery
exports belong to follow-on #215.

Open a collected squishy to see the accessory picker at the top, a large featured
squishy in the center, and its story at the bottom. Tap a worn accessory directly
on the squishy to select that category; this does not remove it. Picking uses the
renderer’s attachment geometry and follows the dance transforms across body families.
Tap an earned accessory or drag it onto the featured squishy to equip and save
immediately. Tap an already equipped card to remove it. Dragging an equipped item
onto the squishy keeps it on. Dropping elsewhere or cancelling a drag leaves the outfit unchanged.
Locked cards show their achievement goal without changing the outfit. The picker
has no separate Remove control, promotional heading or tap/drag instructions. No extra Dress Up screen
or Wear It step.
Each change plays the shared Star Path sparkle/star-outline burst, followed by a
random nonrepeating dance; cosmetic randomness is separate from gameplay RNG.
Each category has distinct synthesized don/doff foley: fabric swishes, glasses clicks,
paired soft steps, elastic mask pops, and magical Flair twinkles. Each clip includes
a short squishy cheer at the dance onset, so no delayed audio callback can outlive
the UI. Android and iOS share PCM and gain through `Sound.accessoryChange`; all
cues follow effects volume. Tapping the worn style plays its removal cue; unchanged drops and cancelled drops stay silent.
An earned style can be worn by any number of squishies at the same time. Editing
one outfit never removes items from another. One item per slot; all five combine.
No item affects gameplay. Ninja hides conflicting head/face accessories temporarily.
Equipped hats and boots replace the contextual explorer equipment in those slots.

## Starting rewards

| Slot | Style | Existing achievement |
| --- | --- | --- |
| Hat | Sunny Scout | Collect 1 squishy |
| Hat | Sprout Cap | Reach stage 5 |
| Hat | Star Crown | Win Star Path |
| Sunglasses | Rosy Outlook | Collect 10 squishies |
| Sunglasses | Sea Glass | Reach stage 10 |
| Sunglasses | Stargazer | Survive 3 minutes |
| Boots | Cloud Clogs | Win the steamer game |
| Boots | Star Steps | Survive 1 minute |
| Boots | Party Heels | Complete All Bosses |
| Mask | Ninja | Clear 5 perfect stages |
| Mask | Duck | Reach stage 20 |
| Mask | Samurai | Collect 25 squishies |
| Mask | Sky Hero | Survive 2 minutes |
| Flair | Starstruck | Beat Super Slime |
| Flair | Flutter | Visit 2 lands in one run |
| Flair | Soft Glow | Survive 5 minutes |

The collection now has three hats, three sunglasses, three footwear styles and four
masks, plus three animated flair styles. Footwear uses foam clogs, glitter shoes and rounded high heels. Fly Agaric
uses a narrower footwear attachment span to keep the shoes under its stem. The
Ninja cosmetic reuses the gameplay hood art but grants no power; Sky Hero is an
original teal/violet design with a comet emblem. Mask artwork draws under glasses
and hats. The first three mask save IDs are retained despite their new names.
Category counts and variant lookup come from `Accessories`, rather than assuming
three items per category.

Other achievements keep their existing recognition; future collections can
map additional accessory styles to them.
`Accessories` owns IDs, labels, colors and this mapping. IDs must stay stable.

## Saving and merging

`ProgressData` carries `accessory_owned_ID`, `accessory_seen_ID` and independent
`accessory_outfit_PRIZE_SLOT` maxima. Rewards backfill from local achievement
progress, including cloud-restored progress, without sign-in. One title-screen
celebration batches unlocks after tutorials and run rewards.

Each squishy/slot record encodes a logical edit clock in the high bits and the
append-only accessory index plus one in the low 10 bits. Zero low bits means an
explicitly saved removal. Cloud merges take the maximum value per slot. Concurrent
edits to different squishies or slots both survive; equal-clock edits to the same
slot resolve by accessory index. A newer removal beats an older equipped snapshot.
Writes persist before changing live outfits.

Existing `accessory_wearer_ID` records from the exclusive-transfer prototype are
migrated once per untouched slot, preserving the original visible outfit (including
its conflict winner). New outfit records, including removals, always take priority
over those legacy records, even if an old device uploads them later. Reset epochs
cover both formats. Do not reorder collectible, slot or accessory IDs; add a
migration before changing their identity or encoding.

Score and tutorial resets preserve the wardrobe. Developer **Reset Outfits**
clears wardrobe records behind a separate merge epoch; stale cloud outfits cannot
resurrect them. Retained achievements regrant eligible items on reconciliation.
**Clear All Progress** clears the underlying achievements too. Developer builds
can use **Gift Accessory**, **All Accessories**, category chips (**All Hats**, **All Shades**,
**All Boots**, **All Masks**, **All Flair**), and **Reset Outfits** in Progress
without recording fake achievement progress or reporting to platform services.

## Attachment extension points

`Painter.accessory(who, slot)` follows every character-local transform and color
wrapper. `AccessoryArt.outfit` takes the actual head, face and feet positions from
the character being drawn. Ordinary shapes reuse `Trinket`'s face offsets and
`AccessoryAnchors`' hat/feet offsets. Ducks and boss friends supply their animated
head positions directly. No lookup uses the companion's home location.

When adding a body shape, extend `AccessoryAnchors` and inspect all slots.
When adding a custom character renderer, call `AccessoryArt.outfit` with its live
anchors, inside its facing/scale transform, and forward wardrobe lookups through
any new Painter wrapper. Forward optional `accessoryTargets`/`accessoryTarget`
geometry through wrappers too, transforming its corners with the artwork.
`AccessoryPicking` traces those regions only on taps; ordinary rendering skips
them. New categories need hit-region bounds and an overlap priority. Unknown
silhouettes must not display accessories.
Turtles are not currently in `Collect`; their eventual renderer must supply anchors
through this same hook. Add them to the attachment coverage test and review sheet.

The small vector pieces use the shared Android/iOS drawing seam, with no blur,
new bitmap caches, or per-frame achievement scans. Rewards reconcile only after a
progress revision. `TestAccessories` covers rules, merge conflicts, reset and UI;
`214-` preview frames show the editor, shared outfits, rewards, designs and body families.

## Animated flair

Flair is an independent fifth slot: orbiting glowing stars, fluttering butterflies,
or a softly pulsing mint/lilac aura. It follows the current character transform and
scale, remains visible during Ninja mode, and is cosmetic only. `AccessoryFlair`
uses a fixed number of clock-driven vector primitives, without blur surfaces,
gameplay particles, physics state or RNG calls. Decorative orbits keep the face
clear. New styles inherit normal retroactive achievement unlocks and unlimited use.

## Size-aware artwork

`Painter.accessorySize` reports projected radius divided by viewport width. Renderer
supplies the viewport; portrait fitting includes its scale and color/opacity wrappers
forward the value. This keeps Android pixels and iOS points on the same detail curve.
Accessory trim fades in between 6.5% and 12% of viewport width. Small views skip fine
ornament, reduce mask contours and use fewer Flair glow layers; silhouettes, colors,
attachments and orbit timing stay consistent. Large views add seams, jewels, glints
and shading. Star Crown uses a velvet center, five gold points and inset jewels.

Glitter is a fourth Flair style, appended as `glitter_flair` and unlocked by visiting
three lands in a run. Existing achievement holders receive it through the normal
backfill. It samples the live body vector fills and draws anchored gold/white twinkles
before expressions and clothing (the cat portrait excludes dark face ink). Small views
sample 32 sites and large views up to 200; no extra character render, framebuffer or blur
is used. Tapping a glittered body selects Flair. Developer Flair/all chips include it.
The dress-up backdrop fully hides the display-case labels after opening; picker and
story share the same margins and the hero centers within the actual remaining space.

Googly Eyes is the fourth eyewear style, appended as `googly_eyes`, earned by beating
Octopulse. Existing wins backfill it automatically. Independent clock-driven pupils
stay inside white domes; large views add rounded rim shading/reflections and small
views skip that trim. It uses existing eyewear attachments, equip sound and save logic.

Selecting/browsing an accessory or beginning a card drag hushes story narration;
subsequent outfit edits stay quiet. Tap the story text to replay from the beginning.
Closing and opening a story resets this state. Cosmetic sound effects remain active.

Dress-up and reward panels share an outer frame with equal margins beyond conservative
status/navigation safe areas (or larger native insets). Reward names wrap within their
columns. Gold Glitter tints body fills/highlights while retaining shading and dark facial
ink; denser gold flecks use bounded samples and only a few bright cross-shaped glints.

Gold Glitter also has a diagonal golden sheen that crosses the body, then rests before
repeating. Three nested bands clip against existing vector fills/contours, preserving
holes and following the same body transforms. Small views use only a subtle gold pulse;
no clipping work, new particles or blur surfaces are needed there.
