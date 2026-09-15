# iOS production-parity inventory

This inventory distinguishes implemented boundaries from complete end-to-end sign-off.
“Wired” means the shared code is selected by `ios/scripts/sources.py` and has its required
native boundary. See [validation evidence](validation.md) for completed automated simulator
checks; rows marked pending still need complete human play-through or device exercise.
The row statuses below describe full acceptance, not the absence of automated coverage:
the completed checks include lifecycle/pause UI tests, 56 input assertions, four native
storage tests, six native audio tests, and nine reviewed renderer comparisons.

The production build writes `BuildFlags.DEVELOPER = false`. The scope below is
therefore the current four-land release game, rather than old experiments or
developer-panel shortcuts.

## Runtime seam

Build-gated Game Center authentication and iCloud progress sync are described in
[native game services](game-center.md). They preserve the existing Android Play Games
integration. Release builds enable them explicitly with `DDDUMPLING_GAME_CENTER=1`.

| Area | Shared code | Native boundary | Status |
| --- | --- | --- | --- |
| App lifecycle, portrait layout, safe areas, 60 Hz frame loop | `GameCore`, `Layout`, `Renderer`, `Pause` | `DDAppDelegate`, `DDGameView` | Wired; simulator lifecycle, safe-area, and resume checks pending |
| Rendering | `Painter` and all rendering classes | `DDIOSPainter` | Wired; real generated `Painter.h` protocol compile passes; visual parity pending |
| Touch routing and haptics | `IOSGame`, `IOSTouch`, `Blade`, `BossPlay`, `CaseUi`, `LandPicker` | `DDGameView` constructs stable UIKit pointer packets; `IOSGame` owns routing; `DDHost` triggers `UIImpactFeedbackGenerator` | Wired; multitouch and cancellation simulator checks pending |
| Local save | `GameCore.Store`, `Progress.Store`, `ProgressData` | `DDIOSStore` | Wired; persistence/relaunch and corrupt-save simulator checks pending |
| Sound and narration | `GameCore.Sound`, `Sfx`, `Music`, `Narration` | `DDIOSAudio` | Wired; output, interruption, and narration checks pending |
| Privacy link | `PrivacyUi` | `DDHost` calls `UIApplication openURL:` | Wired; destination-open check pending |

## Production progression

`Boss.EVERY` is five. There are exactly four authored encounters and four lands:

| Land | First playable stage | Encounter at the chapter end | Unlock rule | Shared code | Native path | Status |
| --- | ---: | --- | --- | --- | --- | --- |
| 0, slime hills | 1 | Stage 5: Slime | Available from a new save | `Lands`, `LandPicker`, `Boss`, `BossScreen`, `BossPlay` | `IOSGame`, `DDGameView`, `DDPainter` | Wired; simulator pending |
| 1, crystals | 6 | Stage 10: Dark Divide | Slime boss-friend collectible | same | same | Wired; simulator pending |
| 2, kelp | 11 | Stage 15: Octopulse | Dark Divide boss-friend collectible | same | same | Wired; simulator pending |
| 3, mushroom clouds | 16 | Stage 20: Fly Agaric | Octopulse boss-friend collectible | same | same | Wired; simulator pending |

Stages after 20 repeat scenery (`Lands.forStage`) but do not add an authored
boss. Fly Agaric is the fourth and final boss-friend collectible; it does not
unlock a fifth land. Land choice is session-only, while unlock discovery and
per-land best scores are saved.

## Screens and scene states

| Production screen or scene | Shared implementation | Native adapter | Status and required simulator exercise |
| --- | --- | --- | --- |
| Title: animated logo, self-playing lesson, deck, privacy affordance, launch transition | `Screens.title`, `TitleBubbleFont`, `Demo`, `Launch`, `PrivacyUi`, `Renderer` | `DDGameView`, `IOSGame`, `DDPainter`, `DDHost` | Wired; start a run, touch the logo, and open the privacy URL |
| Land picker and first-visit discovery card | `LandPicker`, `Lands` | `IOSGame` stable-pointer drag routing | Wired; unlock each available card and tap/drag-select it |
| Main play: sky, scenery, falling words, six-key deck, HUD, shots, particles, powerups, stage banners | `GameCore`, `Renderer`, `Sky`, `Words`, `EnemyEntry`, `Hud`, `Fx`, `Power`, `Kawaii`, `Shape`, `Draw` | `DDGameView` frame loop; `DDIOSPainter`; `IOSGame` key/power routing | Wired; play a normal stage through a clear and a loss |
| Pause, end-run confirmation, background pause and return | `Pause`, `GameCore`, `Screens` | UIKit pause button in `DDGameView`, app lifecycle callbacks, `IOSGame.back/background` | Wired; button, app switch, background/foreground, and back navigation |
| Steamer interlude: roll, mash, lid, prize/reveal, result report | `Interlude`, `Steamer`, `Screens.bonus`, `Parade` | `IOSGame` mash and lid-drag routing; `DDIOSAudio`; `DDIOSPainter` | Wired; win and lose one round |
| Starpath interlude: ready card, flyer, course, win/loss report and prize | `Interlude`, `StarPath`, `StarScreen`, `Screens` | `IOSGame` flyer/slider drag routing; `DDIOSAudio`; `DDIOSPainter` | Wired; complete and fail a course |
| Boss-friend reveal and parade | `Interlude`, `BossCollect`, `Parade`, `Trinket`, `Collect` | `IOSGame`, `DDIOSAudio`, `DDIOSPainter`, `DDIOSStore` | Wired; beat every boss and confirm collectible/land unlock after relaunch |
| Game-over death, haul dance, home-to-case transition, summary/replay | `RoundEnd`, `Screens.gameOver`, `GameCore`, `Trinket` | `IOSGame`, `DDGameView`, `DDIOSAudio`, `DDPainter` | Wired; lose normally and after a boss victory sequence |
| Display case: catalogue, shelf, arrows, position bar, focused entry | `Showcase`, `Cabinet`, `CaseUi`, `Collect`, `Trinket` | `IOSGame.handleCase`, `DDPainter` | Wired; browse by taps and two-axis drag, then close with a deck key |
| Story popup and looping vignette | `Storybook`, `Lore`, `Narration` | `IOSGame`, `DDIOSAudio` speech, `DDPainter` | Wired; open a collected entry, hear narration, dismiss it |
| Roster join/leave scene | `Roster`, `GameCore`, `Renderer` | `IOSGame`, `DDIOSAudio`, `DDIOSStore`, `DDPainter` | Wired; earn the Slime unlock and verify it survives relaunch |

## Boss inventory

| Boss | Production mechanic | Shared implementation | Native route | Status |
| --- | --- | --- | --- | --- |
| Slime | Match prompt letters, break the chain, and drag released globs off the field; includes shield/bolt interactions | `Boss`, `BossPlay`, `BossScreen`, `Slime`, `Softbody`, `BossVictory` | `IOSGame.handleBoss`, `DDIOSAudio`, `DDPainter` | Wired; tap, glob drag, shield/bolt, victory pending |
| Dark Divide | Match its prompt then spread two fingers to split charged bodies | same, plus `Fx` | `IOSGame.handleBoss` pinch ownership and haptic | Wired; real two-finger pinch, cancellation, and victory pending |
| Octopulse | Answer reach prompts while handling its moving reach and impacts | same | `IOSGame` tap route and delayed haptic | Wired; reach, wrong-key hit, impact, and victory pending |
| Fly Agaric | Drag its cap back and forth; handle shakes and spore volley | same | `IOSGame` drag ownership and `DDIOSAudio` | Wired; carry, second-finger deck tap, spores, and victory pending |

There is no fifth production boss. References to `Boss.FACE` are the character-face
mapping for the four bosses, not an additional encounter.

## Production gestures

| Gesture | Shared logic | iOS route | Status |
| --- | --- | --- | --- |
| Any deck-key tap; two thumbs | `GameCore.tapKey`, `screenKey`, `keyAt` | UIKit `touchesBegan`/pointer packets → `IOSGame.touch` | Wired; pending |
| Title logo reaction and land-card tap/drag | `GameCore` title springs, `LandPicker` | `IOSGame` title and land-pointer handlers | Wired; pending |
| Display-case taps, arrows, position/entry selection, free two-axis shelf drag | `Showcase`, `CaseUi` | `IOSGame.handleCase` | Wired; pending |
| Story dismissal | `GameCore.closeStory`, `Storybook` | `IOSGame.touch` | Wired; pending |
| Pause button, pause choices, back, background cancellation | `Pause` | `DDGameView.navigateBack/setActive`, `IOSGame.background/back` | Wired; pending |
| Direct powerup tap | `GameCore.tapPower`, `Power` | `IOSGame.touch` before field gestures | Wired; pending |
| FLING blade, including coalesced move samples | `Blade`, `GameCore.beginStroke/sliceTo/endStroke` | `IOSGame.handleFling` | Wired; pending |
| Panic upward PUSH | `GameCore.swipeUp`, `Layout.inPushZone` | `IOSGame.handlePush` | Wired; pending |
| Boss element tap, drag ownership, two-finger pinch, and cancel | `BossPlay`, `Boss` | `IOSGame.handleBoss` | Wired; pending |
| Steamer key mash and armed-lid upward drag | `Interlude`, `Steamer` | `IOSGame.touch`, `handleBonusSwipe` | Wired; pending |
| Starpath flyer/slider direct steering and release/cancel | `StarPath`, `StarScreen` | `IOSGame.handleStarDrag` | Wired; pending |

The translated `IOSInputTest` covers packet identity, lifecycle cancellation,
land dragging, Starpath dragging, boss drag/pinch ownership, and FLING history.
It is implementation evidence only until it is run as part of the iOS build.

## Saved fields

`DDIOSStore` stores a checksummed, atomically written binary plist in Application
Support. The following `GameCore.Store` and `Progress.Store` values are wired:

| Field | Shared owner | Native key or representation | Status |
| --- | --- | --- | --- |
| Progress event/counter blob | `ProgressData`, `Progress` | `progress` byte array plus `progressWriter` replica ID | Wired; relaunch/corruption pending |
| Global best | `GameCore` | `best` | Wired; pending |
| Land discovery/suppression bits | `LandPicker`, `GameCore` | `landState` | Wired; pending |
| Per-land bests | `LandPicker`, `GameCore` | `best` for land 0, `landBest.N` otherwise | Wired; pending |
| Developer speed | `GameCore` | `speed` | Persisted but excluded from production UI |
| Developer music selection | `GameCore`, `Music` | `bgm` | Persisted but production uses its fixed default choice |
| Collected-item bitmask | `Collect`, `GameCore` | `collected` | Wired; pending |
| Per-item duplicate counts | `Collect`, `GameCore` | `collectionCounts` | Wired; pending |
| Lifetime reward total | `GameCore`, `Interlude` | `collectTotal` | Wired; pending |
| Lifetime steamer wins | `Steamer`, `GameCore` | `steamerOpens` | Wired; pending |
| Starpath wins/difficulty | `StarPath`, `GameCore` | `starWins` | Wired; pending |
| Adaptive roster state | `GameCore`, `Roster` | `roster` | Wired; pending |

## Audio, font, and assets

| Runtime content | Shared source | iOS implementation | Status |
| --- | --- | --- | --- |
| Six squish voices; drip, clear, wrong, achievement, start, stage-clear, power-clear, chop, zap, collect, star, course-start, tally, parade-join, roster-join, game-over | `Sfx`, `GameCore.Sound` | `DDIOSAudio` synthesises PCM through translated `Sfx` and plays overlapping `AVAudioPlayer` instances | Wired; event-by-event listening pending |
| Slime laugh/damage/split, Dark Divide damage/split/deactivate and three boings, bolt pop/death, shield bounce, Octopulse cue/lock, Fly Agaric shake/spore, four boss taunts | `Sfx`, `BossPlay`, `GameCore.Sound` | `DDIOSAudio` | Wired; each boss sequence pending |
| Normal, frenzy, and boss music loops; rocket and slime-charge beds | `Music`, `Sfx` | `DDIOSAudio` rebuilds/mixes AVFoundation players | Wired; transitions and background interruption pending |
| Collectible narration and hush | `Narration`, `GameCore.Sound` | `AVSpeechSynthesizer`, with music ducking | Wired; voice/rate and dismiss pending |
| Game text | `CanvasPainter` contract, `Painter`, all renderer text calls | Bundled `assets/fonts/Bungee-Regular.ttf`, `Info.plist` `UIAppFonts`, `DDIOSPainter` Core Text baseline rendering | Wired; Bungee registration/baseline visual check pending |
| Gameplay art | Vector procedures in `Renderer`, `Kawaii`, `Shape`, `Trinket`, `Lands`, boss and screen renderers | `DDIOSPainter`; no raster gameplay art is required | Wired; visual review pending |
| iOS pause glyph | — | Apple `pause.fill` SF Symbol in `DDGameView` | Wired; visual check pending |

The iOS target currently packages Bungee and its license text. `Quicksand.ttf` and
the `res/` launcher artwork are Android-side resources, not iOS gameplay assets.
No external music or sound file is required for the shipped procedural soundtrack;
an optional bundled `bgm` file is only a developer custom-music path.

## Explicitly excluded from production scope

- The developer settings panel, speed/music controls, stage jumps, test-mode chips,
  collection wipe, roster toggle, reset controls, and debug scene environment variable
  are all guarded by `BuildFlags.DEVELOPER`.
- The developer-only direct Starpath/Steamer/power-mode entry points are not release
  navigation paths.
- Android `MainActivity`, `GameView`, `CanvasPainter`, `Audio`, and `Crash` are replaced
  by the iOS host/adapters above.
- Legacy or hypothetical bosses beyond the four entries in `Boss.COUNT` are not part of
  this inventory or release validation.
