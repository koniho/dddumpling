# Hexatype glossary

Names for every element, so a request can point at one thing unambiguously. The left column
is what to say; the right is where it lives if you want to look.

Where a plain-English name differs from the code name, both are listed — that mismatch is the
main thing this file exists to record.

## The alphabet

| Say | Means | Code |
| --- | --- | --- |
| **letter** | one of the six characters | `Glyph` index 0–5 |
| **dumpling** | letter 0, warm cream | `Glyph.COLOR[0]`, `Kawaii.DUMPLING` |
| **strawberry** | letter 1, berry pink | `Kawaii.STRAWBERRY` |
| **cat** | letter 2, apricot | `Kawaii.CAT` |
| **grapes** | letter 3, grape purple | `Kawaii.GRAPES` |
| **squishy** | letter 4, mint | `Kawaii.SQUISHY` |
| **blob** | letter 5, sky blue | `Kawaii.BLOB` |
| **face** | the expression drawn on a letter | `Kawaii` |
| **mood dumpling** | the dumpling with a variable expression, used for accuracy and rewards | `Kawaii.moodDumpling` |

"Letter" and "character" mean the same thing here. `Glyph` holds the colour and hexagon
geometry; `Kawaii` draws the creature.

## Falling words

| Say | Means | Code |
| --- | --- | --- |
| **word** | one falling row of letters | `GameCore.Enemy` ← *note the mismatch* |
| **tile** | one letter cell of a word: hexagon plus character | drawn in `Renderer.enemy` |
| **head tile** | the next letter you must press — bigger and brighter | `i == e.pos` |
| **cleared letter** | already typed; stays in place but recedes | `i < e.pos` |
| **stacked letter** | needs 2–4 presses; drawn as a pile of offset copies. Never sits next to its own letter — there would be nothing to show where it ended | `e.need[i] > 1`, `Words.fill` |
| **press pips** | the dots under a stacked tile showing presses still owed | `Renderer.enemy` |
| **engaged** / **locked** word | the word you are currently typing | `GameCore.target` |
| **lock ring** | the thick white outline on an engaged head tile | `Renderer.enemy` |
| **caret** | the triangle above an engaged head tile | `Draw.caret` |
| **entrance** | words sliding in from above the top edge | `e.enterT` |
| **fly-apart** | a cleared word's letters splitting outward off screen | `e.destroyed`, `e.flyDir` |
| **flung letter** | a letter removed out of order, by FLING or MULTI | `e.gone[i]` |

## Threat

| Say | Means | Code |
| --- | --- | --- |
| **danger line** | the dashed rose line above the key deck | `Layout.dangerY` |
| **closing in** | a word entering the warning band; it agitates and the sky reddens | `e.warn`, `warnLevel` |
| **lunge** / **attack** | the final dive a word makes on crossing the line | `e.attacking` |
| **breach** | a word landing and costing a life | `GameCore.breach` |
| **squish** (a word) | clearing a word; the game-over screen counts them | `GameCore.squishes` |
| **harm** | how far health has fallen; drives the red tint and pulse. Squared, so it bites on the last life rather than the first | `GameCore.harm()` |
| **edge glow** / **vignette** | the red glow at the screen edges | `Sky.vignette` |
| **push-back** | the panic swipe: shoves the bottom half of the field back, once a stage | `GameCore.pushBack`, `pushReady` |
| **swipe strip** | the band between the danger line and the deck the gesture starts in | `Renderer.pushHint` |
| **shockwave** | the gold bands sweeping up when it lands | `Renderer.pushWave` |
| **slide** | a shoved word travelling back up over 0.4s instead of jumping | `e.slideT`, `GameCore.PUSH_SLIDE` |

## Player input

| Say | Means | Code |
| --- | --- | --- |
| **key deck** | the whole six-hexagon bar at the bottom | `Layout.keyR`, `keyX/keyY` |
| **key** / **button** | one hexagon | index 0–5, same order as the letters |
| **chevron** / **cluster** | the three-key group under one thumb | left = keys 0,1,2; right = 3,4,5 |
| **hint pulse** | the ring on the key you need next | `Renderer.keys` |
| **press ripple** | the ring expanding off a key as its press decays | same |
| **key invite** | the glowing rings sweeping across the deck on the title and settled game-over screens, in place of a "press any key" line | `Renderer.keys` |
| **demo** | the title screen typing a word to itself, in place of the two lines that explained it | `Demo` |
| **caret** | the triangle over the thing to press next — the field's head tile and the interlude's wanted letter draw the same one | `Draw.caret` |
| **case gestures** | the only drag targets outside play: tap either side of the shelf, swipe it, or drag the position bar | `GameView.handleCase`, `GameCore.caseDragTo` |

## Stages

| Say | Means | Code |
| --- | --- | --- |
| **stage** | one difficulty step | `GameCore.stage` |
| **wave** | the fixed set of words a stage releases | `stageQuota()` |
| **stage pips** | the dots in the HUD, one per word in the wave | `Hud.hud` |
| **breather** | the pause after a wave before the next arrives | `stageGap`, `STAGE_GAP` |
| **difficulty ramp** | how far up the curve a stage sits. A stage is worth 5/9 of a step, so what used to land at stage 6 lands at 10 | `GameCore.RAMP`, `ramp()` |
| **stage banner** | the big "STAGE n / FASTER NOW" text | `Hud.stageBanner` |
| **perfect wave** | a wave cleared with no wrong press; gold dumpling on a glowing star | `Hud.perfectStage` |

## The collection

| Say | Means | Code |
| --- | --- | --- |
| **collectible** / **squishy** | one of the thirty things you can win | `Collect` index 0–29 |
| **display case** | the glass cabinet of collectibles, opened from the title screen | `Showcase`, `GameCore.caseOpen`, `caseIndex` |
| **case badge** | the small case in the middle of the title screen that opens it | `Showcase.icon`, `inIcon` |
| **cabinet** | how the case is drawn: a box three-quarters on, shimmering wireframes over translucent panes | `Showcase.caseBox` |
| **shelf** | the filmstrip row inside the case: one focused entry plus a neighbour each side | `Showcase.WINGS` |
| **position bar** | the scroll bar under the shelf, and a handle: drag it to jump. Ticks mark what is collected | `Showcase.scrollbar`, `barIndexAt` |
| **silhouette** | how an uncollected entry is drawn — outlined shape, flat fill, question mark | `Trinket.draw` with `known == false` |
| **family** | which of the three shelves an entry belongs to: mystery dumplings, squishy fruits, squeeze globs | `Collect.FAMILY` |
| **tier** | rarity: common, uncommon, rare, chase, grail. Sets the frame colour and the odds | `Collect.TIER` |
| **shape** | one of fifteen bodies (bao, glob, wedge, cone…) | `Collect.SHAPE` |
| **finish** | one of nine surfaces (matte, glitter, holo, galaxy, metallic, clear, glow, tie-dye, confetti) | `Collect.FINISH` |
| **banded finish** | a finish drawn as bands, so it only sits on a round shape | `Collect.banded` |
| **prize** | what the steamer just handed over | `GameCore.prize`, `prizeNew` |
| **duplicate** | a prize already in the case; pays score instead | `GameCore.DUPE_BONUS` |
| **clear collection** | the settings button that empties the case, behind a confirming tap | `GameCore.tapClearCase`, `clearArmed` |
| **story** | the popup a collected entry opens: where it lives, its family, and the joke | `Lore`, `Storybook` |
| **setting** | the one-line "where it lives" under the name | `Lore.WHERE` |
| **beat** | the looping vignette over a story — one of ten, cast from the family | `Lore.BEAT`, `Storybook.beat` |
| **partner** / **third** | the other two characters in a beat, always from the same family | `Lore.PARTNER`, `Lore.THIRD` |

## Interlude (the between-stages minigame)

| Say | Means | Code |
| --- | --- | --- |
| **interlude** | the whole between-stages minigame | `BONUS` state ← *mismatch* |
| **steamer** | the dim sum basket, drawn in three-quarter view | `Steamer` (state), `Basket` (drawing) |
| **back pass** / **front pass** | the far wall and interior, then the near wall over the squishy | `Basket.back`, `Basket.front` |
| **rim** | the ellipse at the top of the basket, the widest part | `rimY`, `rimRy` in `Screens.bonus` |
| **near wall** | the front of the basket, drawn over the squishy's lower third | `Basket.front` |
| **slats** | the woven bands round the wall | `Basket.slat` |
| **lid** | the flat woven disc that lifts as you mash | `Basket.lid`, `steamer.lidOpen()` |
| **rainbow dumpling** | the one trapped inside; unidentified until the lid is off | `Screens.bonus` |
| **spinner** | the two-second draw for this round's pair at the start of the interlude | `Steamer.rolled`, `GameCore.bonusRolling` |
| **countdown** | the large clock under the steamer | `Screens.countdown`, `GameCore.bonusLeft` |
| **beat on zero** | the second after the clock runs out, before anything fades | `GameCore.bonusHolding`, `BONUS_HOLD` |
| **rebuff** | what a wrong press gets: the basket jolts rose, the lid does not budge | `Steamer.badPulse` |
| **steamer damage** | presses landed, carried across interludes | `steamer.hits` |
| **reveal** | freeing it: the prize climbs out with its name, tier and NEW badge | `Screens.prizeLabel` |
| **escape** | the won prize climbing out of the basket; all that is left of a won round | `GameCore.bonusEscape`, `Steamer.FREE_TIME` |
| **parade** | what closes a winning interlude: the collection marches in, the new one joins the line, they march off | `Parade`, `GameCore.bonusParading` |
| **line** | the row of collectibles in the parade, capped at what fits | `Parade.LINE` |
| **companions** | the already-collected ones that turn out for the parade | `Parade.companions` |
| **movements** | the parade's three parts: in from the left, the join, off to the right | `Parade.IN_END`, `JOIN_END` |

## Powerup

| Say | Means | Code |
| --- | --- | --- |
| **powerup letter** | the single glowing letter drifting horizontally | `Power`, `GameCore.power` |
| **frenzy** | the 15-second period after catching one | `mode`, `modeLeft` ← *mismatch* |
| **mode bar** | the name, blurb and countdown at the top during a frenzy | `Hud.modeBar` |
| **FLURRY** | every key is a wildcard; letters and keys go rainbow | `Power.FLURRY` |
| **FLING** | the blade: a swipe cuts every letter it sweeps past | `Power.FLING` |
| **blade** | the cutting edge itself, drawn along the last stretch of the stroke | `Renderer.blade`, `GameCore.BLADE` |
| **stroke** | one touch-down to touch-up of the blade | `GameCore.beginStroke`, `sliceTo`, `endStroke` |
| **slow-motion beat** | the brief slowdown a stroke earns by taking two or more words | `GameCore.slowdown`, `SLOW_RATE` |
| **slice call** | the "N IN ONE!" readout during that beat | `Hud.sliceCall` |
| **MULTI** | one press chains through every matching letter, hop by hop | `Power.MULTI` |
| **TEAM SQUISH** | one of your collectibles bounces round the field squishing words | `Power.TEAM` |
| **squishy** / **buddy** | the collectible fighting for you during it | `Buddy`, `GameCore.buddy` |
| **bubble** | the glowing shell round it, brighter and bigger with every word it takes | `Buddy.radius`, `glow` |
| **charge** | the squishy accelerating at the word a press aimed it at | `Buddy.charge`, `GameCore.teamStrike` |
| **turn rate** | how fast the squishy can swing its heading: a full lap in 0.7s. Steering only — bounces are instant | `Buddy.TURN_TIME`, `TURN_RATE` |
| **wind-up** | speed easing between drift and charge instead of stepping; 0.35s either way | `Buddy.SPIN_UP` |
| **cornering** | the speed a turn gives up — a hard one aims at about half drift speed, and winds back up coming out | `Buddy.CORNER` |
| **chain** | that run of hops: a bolt drawn between them, each hop worth more than the last | `GameCore.multiStrike`, `Renderer.chain` |
| **hop** | one link of a chain, and one strike point | `GameCore.chainX`, `chainShown` |
| **call-out** | the big "N IN ONE!" / "N CHAINED!" payoff text | `Hud.sliceCall`, `Hud.chainCall` |
| **sparkle trail** | the rainbow ribbon following the blade during FLING | `Fx.sparkle`, `GameCore.TRAIL_RATE` |
| **fling hint** | the instructional finger shown until you first touch | `Renderer.flingHint` |
| **playtest chips** | the FLURRY/FLING/MULTI buttons in settings | `SettingsUi.HIT_TEST` |

## Background and screen effects

| Say | Means | Code |
| --- | --- | --- |
| **sky** | everything behind the play field | `Sky` |
| **cloud layer** | one of three parallax bands; layer 2 is in front of the words | `Sky.CLOUD_FRONT_LAYER` |
| **sky glow** | the clouds washing with a letter's colour on a hit, or yellow on a clear | `skyGlow` |
| **screen flash** | the full-screen colour wash | `flash`, `flashColor` |
| **screen shake** | the whole-field jolt | `shake` |
| **HUD band** | the soft strip that words emerge from behind | `Sky.hudBacking` |
| **scrim** | the dimming over the sky on the full-screen states | `Screens.scrim` |
| **text scale** | one multiplier on every text size on screen; the settings panel opts out | `Draw.TEXT`, `Draw.type` |

## Screens

| Say | Means | Code |
| --- | --- | --- |
| **title screen** | the opening screen; the real key deck stays lit as the tutorial, and the case badge sits in the middle | `Screens.title` |
| **title fade** | the title screen dissolving on a start press, before play begins | `GameCore.startFade`, `starting()` |
| **screen keys** | all six do the same thing on the full-screen states: start from the title, back to the title from game over | `GameCore.screenKey` |
| **death hold** | the beat after the last life: the world stays up and drains before the summary | `GameCore.DEATH_TIME`, `dying()`, `drained()` |
| **swirl** | the words still on the field spiralling away with trails through the hold | `RoundEnd.swirl` |
| **haul** | the dumplings one run freed, as against the whole case | `GameCore.roundPrizes` |
| **haul dance** | the haul bouncing in the middle of the summary once it has faded up | `RoundEnd.dance` |
| **flight home** | the haul carrying itself to the case with star trails, on the way to the title | `RoundEnd.homeward`, `GameCore.HOME_TIME` |
| **game over screen** | score, accuracy dumpling, best. Fades up after the hold; GAME OVER is yellow, not rose | `Screens.gameOver` |
| **accuracy dumpling** | the face that reflects accuracy: tear below 60%, sparkles above 90% | `Screens.accuracy` |
| **settings panel** | opened by tapping the stage readout; pauses the game | `Screens.settings` |
| **stage readout** | the "STAGE n" text — also the settings button | `Layout.inStageTap` |
| **speed slider** | the 0.5×–1.5× pacing control | `SettingsUi` |

## Sound

| Say | Means | Code |
| --- | --- | --- |
| **squish** | the per-letter press sound, pitched per letter | `Sfx.SQUISH_0 + n` |
| **chop** | the cut of the FLING blade, one per letter. Replaces the word-clear tone on a cut word | `Sfx.CHOP` |
| **zap** | the lightning crack of one MULTI chain hop, climbing in pitch along the chain | `Sfx.ZAP` |
| **drip** | taking damage | `Sfx.DRIP` |
| **word clear** | finishing a word by typing it. A word the blade cut rings no clear tone — its chops are its sound | `Sfx.CLEAR` |
| **thunk** | a wrong press | `Sfx.WRONG` |
| **achievement** | catching a powerup, freeing the dumpling | `Sfx.ACHIEVEMENT` |
| **start tone** | new game | `Sfx.START` |
| **stage clear tone** | a stage ending normally | `Sfx.STAGE_CLEAR` |
| **power clear tone** | a frenzy ending a stage; replaces the stage tone | `Sfx.POWER_CLEAR` |
| **BGM** | the looping track: MOOG SWING, LOFI DRIFT, CHIP MARCH, OFF, MY TRACK | `Music.NAMES` |
| **frenzy track** | the faster four-on-the-floor variant | `Music.loop(style, true)` |

## Terms that are easy to mix up

- **letter** vs **tile** — a letter is one of the six characters; a tile is a specific cell of
  a specific word. "Make letters bigger" changes all six everywhere; "make tiles bigger"
  changes the falling words only.
- **word** vs **wave** vs **stage** — a word is one falling row; a wave is all the words in a
  stage; a stage is the difficulty step.
- **frenzy** vs **interlude** — frenzy is the 15 s powerup period *during* play; the interlude
  is the steamer minigame *between* stages.
- The interlude has **two paths**, and exactly one phase is true at any moment:
  - **lost** — spinner (2 s, no presses) → mash → beat on zero (1 s) → status hold (1.5 s,
    whose tail is the fade-out) → play.
  - **won** — spinner → mash → **escape** (the prize climbing out) → **parade** → play. Winning
    ends the round on the spot, so there is no beat on zero and no status report; the parade
    announces the stage instead.
  - `bonusRolling`, `bonusMashing`, `bonusHolding`, `bonusStatus`, `bonusEscape`,
    `bonusParading`. There is an assertion that exactly one holds on every frame of both paths.
- **sky glow** vs **screen flash** vs **edge glow** — the clouds tinting, a full-screen wash,
  and the red border respectively. All three fire at different moments.
- **engaged** vs **locked** — the same thing; either is fine.
- **letter** vs **collectible** — the six letters are what you type; the thirty collectibles are
  what you win. They share the kawaii look and nothing else: separate palettes, separate
  drawing code (`Kawaii` vs `Trinket`), separate counts.
- **shape** vs **finish** vs **tier** — the body, the surface on it, and how rare it is. A
  request to "make the holo ones brighter" is a finish; "make the buns rounder" is a shape.
- **skit** vs **beat** — both are little vignettes of characters interacting, and they are
  different things. A **skit** plays under a stage banner, runs once, and uses the six letters
  (`Skits`). A **beat** loops inside a story popup and uses the collectibles (`Storybook`).
