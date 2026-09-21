# Town integration

DDDUMPLING Town is a title destination immediately left of Slime Hills. It appears after the
first earned land unlock (Slime boss friend owned and land 1 not suppressed). Developer ALL
LANDS does not grant town access. `LandPicker.TOWN` is a destination ID outside combat save
slots; `order`/`destination` map its display position without renumbering combat lands.

## Current boundary

- `GameCore.openTown`, `requestCloseTown`, `townTouch`, and `cancelTownInput` are the host
  entry points. Android and iOS normalize their touch actions to down/up/move/cancel and
  preserve pointer IDs. `Renderer` delegates the town scene while it owns the screen.
- `Town` owns tickets, decoration purchases, flowers, riders, conversations, slide physics,
  and attraction results. It samples collection ownership on entry and never grants or
  consumes collectibles. Randomness in Slime Fight is independent of combat RNG.
- A run allocates a monotonic ID through `Town.beginRun`; death or explicit End Run credits
  its score once. Interrupted runs receive no reward. Zero-score runs receive no tickets.
- `Store.loadTown`/`saveTown` transfer one versioned snapshot. Balance, purchases and reward
  claim ID save together; unsuccessful writes retain dirty state and retry after one second.
  Android uses a committed SharedPreferences string; iOS uses the existing atomic envelope.
  Town data is local to the device; existing cloud progression does not merge its wallet.
- Travel follows one path with broad up/down bends and open attraction plots on both sides.
  Short entry paths connect the plots; attraction taps target their plot artwork and walk to
  the adjacent entry on the main route. Main-layer foliage leaves these plots clear.
  Both destination taps and the velocity slider
  use the same path coordinate. A clamped camera follows the traveler across a three-screen
  meadow; POI taps subtract the same camera offset, while controls remain fixed. Background
  sky scrolls at 0.06x, hills at 0.18x/0.34x/0.52x, the main field at 1x, and
  foreground foliage at 1.35x for depth. Foreground coverage extends past both map ends. Title-font letter balloons mark the entrance.
  The neutral slider/release stops movement. Foreground
  foliage is translucent, uses idle animation only, and never receives touch impulses. Flowers grow with visits/active time. Meadow taps trigger a local,
  visit-only damped spring in world coordinates; attraction taps also retain tap-to-travel.
  Decoration touches never spend tickets. UI/modal touches do not trigger scene reactions.
  The Flowers chip cycles the saved growth stage through Sprouts (3), Blooming (12), and
  Full bloom (24), resets the growth timer, and lets normal visit/time growth continue.
  Main-layer tree roots, attraction bases, and the full path stay below `meadowTop`;
  distant trees are deliberately drawn before their hill so slopes hide their roots.
- `TownScenery`, `TownAttractions`, and `TownPlayer` own pure-Painter artwork. The town
  traveler has a broad pleated silhouette: the shared `Kawaii.DUMPLING` currently draws a
  round bao despite its name. The Star Path control renderer remains shared and unchanged.
- The basic slide is free; the first purchase is a flower arch. Collecting SLIME BUD enables
  Slime Fight. Its FUN total and best are separate from tickets and main-game score.
- The meadow's upper-right close button and Back request a short meadow-colored exit wash
  before returning to the selected town card. Attraction close/Back returns to the meadow;
  Back dismisses a conversation first. Entry reveals the meadow through the same wash.

## Checks and build scope

Use `./check.sh --town -q` for town rules and selected portrait frames, or add `-r` for rules
only. An explicitly requested local APK can use `./build.sh --developer --town` once its
change is confined to town behavior behind the unchanged interface. Shared/native changes
need broader checks; `./build.sh --developer --rules-checks` includes all rules without
unrelated frame/audio exports. Production/release gates remain unchanged.

The current scoped command selects tests and frames, but the harness still compiles the full
pure source list because town presentation reaches GameCore/StarScreen. APK packaging also
necessarily includes the main game. Do not describe this as an isolated town build yet.

## Refactor priorities found during integration

1. **Consolidate host coordination.** Extract open/close transitions, save retry, run-result
   delivery, normalized input and rendering into one `TownSession`/bridge. GameCore should
   forward lifecycle events rather than know attraction details. The new native input
   wrappers are the first step; keep both platforms on those entry points.
2. **Separate geometry from screens.** `SlimeFight` currently calls `SlimeFightScreen` hit
   regions and both modes reuse `StarScreen` slider geometry. Extract a small shared slider
   geometry/control helper and a town layout snapshot. Give TownScreen a narrow view/host
   context instead of GameCore. This breaks the dependency chain that pulls combat code
   into every town compilation while retaining the familiar slider appearance.
3. **Make the compile boundary real.** After those extractions, introduce a town dependency
   manifest and focused runner/preview. Route town-only file changes to that target, shared
   interface/dependency changes to integration checks, and unknown files to broader checks.
   Cache unchanged dependencies. Avoid a filename-only classifier before the dependency
   boundary is explicit; it would silently miss shared behavior changes.
4. **Extract attractions when adding a third.** Use one lifecycle/result contract and a
   town-owned catalogue for availability, input, update, cancel and return. Today the two
   modes are explicit switches; a registry will prevent each new attraction editing all of
   Town/TownScreen. Ownership unlocks stay collection predicates, not another achievement
   service.
5. **Move the save codec out of simulation as it grows.** A versioned TownSave with keyed
   attraction records keeps migrations and balance validation independent of rendering and
   physics. Preserve the atomic snapshot and idempotent claim contract.

The first three are the priority for efficient independent development. Registry/codec
extraction can follow the next content addition rather than expanding this playable slice.
