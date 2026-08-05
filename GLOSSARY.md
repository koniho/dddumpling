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
| **stacked letter** | needs 2–4 presses; drawn as a pile of offset copies | `e.need[i] > 1` |
| **press pips** | the dots under a stacked tile showing presses still owed | `Renderer.enemy` |
| **engaged** / **locked** word | the word you are currently typing | `GameCore.target` |
| **lock ring** | the thick white outline on an engaged head tile | `Renderer.enemy` |
| **caret** | the triangle above an engaged head tile | same |
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
| **harm** | how far health has fallen; drives the red tint and pulse | `GameCore.harm()` |
| **edge glow** / **vignette** | the red glow at the screen edges | `Sky.vignette` |

## Player input

| Say | Means | Code |
| --- | --- | --- |
| **key deck** | the whole six-hexagon bar at the bottom | `Layout.keyR`, `keyX/keyY` |
| **key** / **button** | one hexagon | index 0–5, same order as the letters |
| **chevron** / **cluster** | the three-key group under one thumb | left = keys 0,1,2; right = 3,4,5 |
| **hint pulse** | the ring on the key you need next | `Renderer.keys` |
| **press ripple** | the ring expanding off a key as its press decays | same |

## Stages

| Say | Means | Code |
| --- | --- | --- |
| **stage** | one difficulty step | `GameCore.stage` |
| **wave** | the fixed set of words a stage releases | `stageQuota()` |
| **stage pips** | the dots in the HUD, one per word in the wave | `Hud.hud` |
| **breather** | the pause after a wave before the next arrives | `stageGap`, `STAGE_GAP` |
| **stage banner** | the big "STAGE n / FASTER NOW" text | `Hud.stageBanner` |
| **perfect wave** | a wave cleared with no wrong press; gold dumpling on a glowing star | `Hud.perfectStage` |

## The collection

| Say | Means | Code |
| --- | --- | --- |
| **collectible** / **squishy** | one of the thirty things you can win | `Collect` index 0–29 |
| **display case** | the shelf of collectibles on the title screen | `Showcase`, `GameCore.caseIndex` |
| **shelf** | the filmstrip row inside the case: one focused entry plus a neighbour each side | `Showcase.WINGS` |
| **position bar** | the scroll bar under the shelf; ticks mark what is collected | `Showcase.scrollbar` |
| **silhouette** | how an uncollected entry is drawn — outlined shape, flat fill, question mark | `Trinket.draw` with `known == false` |
| **family** | which of the three shelves an entry belongs to: mystery dumplings, squishy fruits, squeeze globs | `Collect.FAMILY` |
| **tier** | rarity: common, uncommon, rare, chase, grail. Sets the frame colour and the odds | `Collect.TIER` |
| **shape** | one of fifteen bodies (bao, glob, wedge, cone…) | `Collect.SHAPE` |
| **finish** | one of nine surfaces (matte, glitter, holo, galaxy, metallic, clear, glow, tie-dye, confetti) | `Collect.FINISH` |
| **banded finish** | a finish drawn as bands, so it only sits on a round shape | `Collect.banded` |
| **prize** | what the steamer just handed over | `GameCore.prize`, `prizeNew` |
| **duplicate** | a prize already in the case; pays score instead | `GameCore.DUPE_BONUS` |
| **clear collection** | the settings button that empties the case, behind a confirming tap | `GameCore.tapClearCase`, `clearArmed` |

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
| **steamer damage** | presses landed, carried across interludes | `steamer.hits` |
| **reveal** | freeing it: the prize climbs out with its name, tier and NEW badge | `Screens.prizeLabel` |

## Powerup

| Say | Means | Code |
| --- | --- | --- |
| **powerup letter** | the single glowing letter drifting horizontally | `Power`, `GameCore.power` |
| **frenzy** | the 15-second period after catching one | `mode`, `modeLeft` ← *mismatch* |
| **mode bar** | the name, blurb and countdown at the top during a frenzy | `Hud.modeBar` |
| **FLURRY** | every key is a wildcard; letters and keys go rainbow | `Power.FLURRY` |
| **FLING** | drag letters bodily off the screen | `Power.FLING` |
| **MULTI** | one press clears every matching letter everywhere | `Power.MULTI` |
| **sparkle trail** | the rainbow ribbon following your finger during FLING | `Fx.sparkle` |
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

## Screens

| Say | Means | Code |
| --- | --- | --- |
| **title screen** | the opening screen; the real key deck stays lit as the tutorial, and it holds the display case | `Screens.title` |
| **start keys** | the inner four keys, which begin a run | `GameCore.startKey` |
| **browse keys** | the outer two, which scroll the display case | `GameCore.scrollCase` |
| **game over screen** | score, accuracy dumpling, best | `Screens.gameOver` |
| **accuracy dumpling** | the face that reflects accuracy: tear below 60%, sparkles above 90% | `Screens.accuracy` |
| **settings panel** | opened by tapping the stage readout; pauses the game | `Screens.settings` |
| **stage readout** | the "STAGE n" text — also the settings button | `Layout.inStageTap` |
| **speed slider** | the 0.5×–1.5× pacing control | `SettingsUi` |

## Sound

| Say | Means | Code |
| --- | --- | --- |
| **squish** | the per-letter press sound, pitched per letter | `Sfx.SQUISH_0 + n` |
| **drip** | taking damage | `Sfx.DRIP` |
| **word clear** | finishing a word | `Sfx.CLEAR` |
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
- The interlude runs four phases off one countdown, in this order: **spinner** (2 s, no
  presses), **mash** (the minigame proper), **beat on zero** (1 s), **status hold** (1.5 s,
  whose tail is the fade-out). `GameCore.bonusRolling/bonusMashing/bonusHolding/bonusStatus`
  are the four, and exactly one is true at a time.
- **sky glow** vs **screen flash** vs **edge glow** — the clouds tinting, a full-screen wash,
  and the red border respectively. All three fire at different moments.
- **engaged** vs **locked** — the same thing; either is fine.
- **letter** vs **collectible** — the six letters are what you type; the thirty collectibles are
  what you win. They share the kawaii look and nothing else: separate palettes, separate
  drawing code (`Kawaii` vs `Trinket`), separate counts.
- **shape** vs **finish** vs **tier** — the body, the surface on it, and how rare it is. A
  request to "make the holo ones brighter" is a finish; "make the buns rounder" is a shape.
