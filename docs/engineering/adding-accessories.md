# Adding accessories

Accessories make repeat play feel like progress toward a squishy that is the
player's own. Give each new style a recognizable silhouette, a rewarding close-up
and a clear achievement goal. Accessories remain cosmetic: earn a style once,
then wear it on any number of collected squishies with independent saved outfits.

See [accessory architecture](accessories.md) for persistence, draw order and the
current reward catalog. Follow this checklist when adding a style.

## 1. Register the reward

In [Accessories.java](../../src/com/dddumpling/game/Accessories.java):

- Append one entry to every catalog array: `ID`, `NAME`, `GOAL`,
  `ACHIEVEMENT`, `COLOR` and `CATEGORY`; increase `COUNT`.
- Use a permanent, unique snake-case ID. Never insert, reorder, recycle or rename
  saved IDs. Keep existing category assignments stable too.
- Choose an existing slot and achievement. Use `count(slot)`, `item(slot, variant)`
  and `variant(item)` rather than assuming three choices or contiguous slot IDs.
  Items are globally appended; each category's variant order follows that catalog.
- Match the goal text to the actual achievement. Multiple accessories may share
  an achievement. Adding a cosmetic reward to an existing achievement does not
  require a new Apple or Google achievement.

`Wardrobe.refresh` grants rewards from persisted progress, including existing
players and cloud-restored achievements. Reuse this path; do not require replay,
sign-in, a transfer, or a manual claim. Do not grant achievement progress just to
unlock clothing. Category/all-accessory developer chips discover catalog entries
automatically.

Append-only styles need no save-format change. Ownership uses string IDs, while
outfits encode the numeric index plus one; both must remain stable. Keep within
the encoding limit tested against `Wardrobe.ITEM_MASK`. Changing identity, slot
or encoding needs an explicit migration and merge/reset tests.

## 2. Draw a recognizable style at both sizes

Route the new variant from
[AccessoryArt.java](../../src/com/dddumpling/game/AccessoryArt.java) into the
appropriate helper: `AccessoryHats`, `AccessoryEyewear`, `AccessoryFootwear`,
`AccessoryDetail` or `AccessoryFlair`. Check existing final `else` branches:
a new variant must not silently render as the previous last style.

Build the silhouette and broad shading first. Close views can add seams, weave,
laces, jewels and reflections; distant views should retain the same recognizable
shape without that fine work. Use `AccessoryArt.detail` and the projected
`Painter.accessorySize`, not raw pixel thresholds. Skip detail primitives when
detail is zero and fade them through the existing opacity wrapper otherwise.

Use shared pure Java vector primitives for Android and iOS. Keep decorative motion
bounded and clock-driven; never consume gameplay RNG. Prefer fixed-size loops and
reusable shape definitions over extra particles, blur surfaces or offscreen passes.
New pure source files must be added to `check.sh`'s `PURE` list.

For example, adding another footwear style means appending its catalog record,
adding its variant to `AccessoryFootwear.draw`, and updating its top/bounds logic.
Do not let it fall through to the runners artwork or inherit a low shoe's bounds
if it is a tall boot.

## 3. Fit artwork, thumbnails and taps together

Use the character renderer's existing live head, face and foot anchors, inside
its movement, facing and scale transforms. Never use the companion's home
position. Tune through `AccessoryAnchors` or the relevant body renderer when
needed, without moving unrelated accessories.

- Update `AccessoryArt.thumbnail` bounds so the entire visible shape is centered
  and fills its tile with a small margin. Include brims, pom-poms, ties and soles.
  Reserve no space for names in the ribbon.
- Update worn tap bounds along with the art. Picking must use the same transformed
  geometry, including mask fitting and asymmetric shapes.
- Footwear must work with narrow mushroom/cone stems, wider duck feet and all four
  animated cat paws. Keep faces readable on short bodies such as Nana and DohNut.
- Check both skins, squash/stretch, dancing and facing changes. Preserve normal
  Ninja conflict handling without changing the saved outfit.
- Keep flair and finishing glitter sparkles in front of the squishy and clothing.
  Body tint/shimmer still belongs to the existing body wrapper.

A new character renderer must forward wardrobe lookups, projected size and tap
targets through Painter wrappers, and provide its live anchors. See
[attachment extension points](accessories.md#attachment-extension-points).

## 4. Reuse the dressing interaction

Use the existing tap/drag equip, tap-selected-to-unequip, immediate save, sparkle
flash, dance and category-specific don/doff sound with its short cheer. Do not
add an extra Wear step, transfer restriction or confirmation screen. Accessory
interaction pauses narration; tapping the story starts narration again.

Use catalog counts in preview and input-test coordinates. The ribbon draws and
hit-tests only nearby choices; preserve that behavior as collections grow.
Unlocks must remain silent at startup and on return to the title. The old reward
grid remains in preview tooling; it is not an automatic player-facing notice.

Adding a **new category** is broader than adding a style. Audit `Accessories.SLOTS`,
`Wardrobe.SLOT_ID`, save/reset handling, outfit draw order, picking priority,
toy-box category spacing/dice placement, developer chips, sounds and tests.
The category row has tuned positions, and action IDs have reserved ranges;
increasing the slot count alone is insufficient.

## 5. Verify and prepare for review

For a normal visual addition, compile the full harness and run the focused suite
with selected editor/reward frames once:

```sh
./check.sh -q -s Accessories -f 214-wardrobe,214-toybox
java -Xmx512m -cp build/harness com.dddumpling.game.AccessoryPlacementPreview out/accessory-fit
```

Inspect the changed style's `item-NN.png` across every body family, Halloween
sheets, motion samples, and the actual ribbon/reward frames. Check clipping,
margins, facial obstruction, footwear placement and `DOES NOT FIT` diagnostics.
Review both large detailed and small simplified artwork. The preview's close-up
design pages are curated: add new IDs there when useful; the per-item sheets
already follow the catalog automatically.

Add focused coverage only for new behavior: catalog/reward mappings, unusual
attachment bounds or a new interaction. Existing tests already exercise developer
grants, persistence, cloud merge, reset, detail levels and scrolling. If input or
picker geometry changes, also run `./ios/scripts/test-input.sh`. Shared state or
timing changes need the broader checks described in [AGENTS.md](../../AGENTS.md).
Do not compile just the catalog against stale harness classes: Java may have
inlined the old `COUNT` in consumers.

Update the reward table in [accessories.md](accessories.md), record the work against
the accessory issue, and provide a portrait review sheet. Keep generated PNGs,
APKs and audio untracked. Build/install only when requested or covered by an
existing user instruction; use the developer build/install workflow.
