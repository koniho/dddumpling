# Achievement accessories

Issue #214 adds cosmetic progression: discover an appealing item, play toward its
achievement, earn it, then customize a favorite companion in the display case.
The current art is a first pass for iterative review. Scene portraits and gallery
exports belong to follow-on #215.

Open a collected squishy's story, then **Dress Up**. Selecting a card previews it
without changing the save. Wear It equips it; Remove returns it to the wardrobe.
Transfer asks before moving an item from another squishy. One globally owned copy
of each style can have one wearer, with one item per slot. All four slots combine.
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
| Boots | Puddle Hoppers | Win the steamer game |
| Boots | Cloud Steps | Survive 1 minute |
| Boots | Comet Kicks | Complete All Bosses |
| Mask | Petal Party | Clear 5 perfect stages |
| Mask | Moonbeam | Reach stage 20 |
| Mask | Confetti | Collect 25 squishies |

Other achievements keep their existing rewards/recognition; future collections can
map additional accessories to them. They do not silently award duplicate copies.
`Accessories` owns IDs, labels, colors and this mapping. IDs must stay stable.

## Saving and merging

`ProgressData` carries `accessory_owned_ID`, `accessory_seen_ID` and
`accessory_wearer_ID` maxima. Old saves need no new format. Rewards backfill from
local achievement progress, including cloud-restored progress, without sign-in.
One title-screen celebration batches unlocks after tutorials and run rewards.

Wearer values encode a logical edit clock in the high bits and the existing,
append-only collectible index plus one in the low 10 bits. Zero low bits means
unequipped. Cloud merges take the maximum value. Equal clocks resolve by wearer
index; two items claiming the same wearer/slot resolve by record then item index.
Replacement retires every competing claim so removing the replacement cannot
resurrect a previously hidden item. Writes persist before changing live outfits.
Do not reorder collectible or accessory IDs. Add a migration if the owner range
or inventory semantics change.

Score and tutorial resets preserve the wardrobe. Developer **Reset Outfits**
clears wardrobe records behind a separate merge epoch; stale cloud outfits cannot
resurrect them. Retained achievements regrant eligible items on reconciliation.
**Clear All Progress** clears the underlying achievements too. Developer builds
can use **Gift Accessory**, **All Accessories**, category chips (**All Hats**, **All Shades**,
**All Boots**, **All Masks**), and **Reset Outfits** in Progress
without recording fake achievement progress or reporting to platform services.

## Attachment extension points

`Painter.accessory(who, slot)` follows every character-local transform and color
wrapper. `AccessoryArt.outfit` takes the actual head, face and feet positions from
the character being drawn. Ordinary shapes reuse `Trinket`'s face offsets and
`AccessoryAnchors`' hat/feet offsets. Ducks and boss friends supply their animated
head positions directly. No lookup uses the companion's home location.

When adding a body shape, extend `AccessoryAnchors` and inspect all four slots.
When adding a custom character renderer, call `AccessoryArt.outfit` with its live
anchors, inside its facing/scale transform, and forward wardrobe lookups through
any new Painter wrapper. Unknown silhouettes must not display accessories.
Turtles are not currently in `Collect`; their eventual renderer must supply anchors
through this same hook. Add them to the attachment coverage test and review sheet.

The small vector pieces use the shared Android/iOS drawing seam, with no blur,
new bitmap caches, or per-frame achievement scans. Rewards reconcile only after a
progress revision. `TestAccessories` covers rules, merge conflicts, reset and UI;
`214-` preview frames show the editor, transfer, rewards, designs and body families.
