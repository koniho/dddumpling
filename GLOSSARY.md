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
| **push-back** | the panic swipe: shoves the bottom half of the field back and everything it would land on, once a stage | `GameCore.pushBack`, `pushReady` |
| **swipe strip** | the lit band between the danger line and the deck that advertises the gesture. Not the catchment — see **swipe catchment** | `Renderer.pushHint` |
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
| **demo** | the title screen typing a word to itself, in place of the two lines that explained it: a key lights, a bullet leaves it, a letter goes | `Demo` |
| **bullet** / **shot** | what a press fires from a key at the tile it struck. The demo fires the same one | `Renderer.bullet` |
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
| **perfect wave** / **perfect round** | a wave cleared with no wrong press *and* no life lost; gold dumpling on a glowing star, and the top of the earned-mash ladder. One definition, both readers | `GameCore.perfectRound`, `Hud.perfectStage` |

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

## Star course

The other interlude, offered after a steamer has been opened. Two thumbs steer, three keys a side.

| Say | Means | Code |
| --- | --- | --- |
| **star course** / **course** | the whole star-path interlude: ready lesson, flight, then a win or a report | `StarPath` (state), `StarScreen` (drawing), `GameCore.starFlight` |
| **flyer** | the collectible piloting it — whatever the last steamer handed over | `StarPath.who`, `StarScreen.flyer` |
| **checkpoint** / **star** | one of the twenty to collect. They carry over between attempts | `StarPath.COUNT`, `collected` |
| **pearl** | the lit centre of a checkpoint, and the thing the pickup actually aims at — the petals are decoration | `StarPath.HEART`, `pickupR` |
| **catch band** | how close counts as a catch: tight side to side, where the skill is, and a stated *number of milliseconds* tall, where the clock is | `StarPath.pickupR`, `pickupY`, `GRACE` |
| **grace** | how late a flyer may be and still take a checkpoint, in seconds — not in pixels, so it survives the spacing being changed | `StarPath.GRACE`, `closingSpeed` |
| **lookahead** | how many milliseconds of course are on screen ahead of the flyer. The spacing knob moves this faster than anything else | `TestStars.lookahead` |
| **sweep** | the long swoop across the play area that a course is made of; shaped in seconds, not per star | `StarPath.SWEEP`, `SWEEP_TIME`, `make` |
| **ripple** | the small wobble riding on the sweep, so a course is not one bare sine | `StarPath.RIPPLE` |
| **ready lesson** | the wordless beat before the flight: the keys glow a side at a time and the flyer leans, then settles to a stop on the spot it will fly from | `StarPath.ready()`, `lessonLean`, `lessonFade` |
| **launch** | the soft start: the course leaves from a standstill and takes a second to reach its pace | `StarPath.EASE_IN`, `launch`, `unlaunch` |
| **wake** | mini stars in the six letter colours trailing the flyer's steering, thicker the fuller the course — the only thing that says how far along a playthrough is without a number. Off once the flight is over | `StarScreen.wake`, `StarPath.lessonSway` |
| **pulse** | the trail breathing while a course is flown, and beads of light running up it | `StarScreen.pulse`, `routeX` |
| **whoosh** | the course leaving: the one effect in the game that swells instead of decaying | `Sfx.course`, `Sound.courseStart` |
| **tally** | the count read out at the end of an interlude nobody won — a star report or a steamer status page, pitched by how it went | `Sfx.tally`, `Sound.tally` |
| **join chord** | the new collectible taking its place in the parade line | `Sfx.join`, `Sound.paradeJoin` |
| **full stop** | the end of a run, played when the swirl clears rather than on the fatal breach. The only descending figure in the game | `Sfx.over`, `Sound.gameOver` |
| **grab beat** | the flick of slow motion each taken star lands with, the fling stroke's beat at a quarter length | `GameCore.STAR_BEAT`, `StarPath.grabbed` |
| **blast-off** | one short rising exhaust cue when the flight ends or the victory tableau begins; the continuous rocket stops at that transition | `Sfx.BLAST_OFF`, `Sound.courseFinish` |
| **ting** | the pickup note, pitched up with the count so a course is a rising ladder | `Sfx.star()`, `Audio.star` |
| **victory tableau** | what a completed course ends on: everything stops, the prize climbs out of the last star, then the parade | `StarPath.WIN_HOLD`, `winning()`, `StarScreen.victory` |
| **passenger** | a flyer nobody is steering. It must never be able to finish a course | `TestStars.flown` with steering off |
| **tracker lag** | what following the line costs, against the catch band — the measure of how hard a course is, in place of the peak speed ratio it asks for | `TestStars.trackerLag` |
| **re-roll** | a failed attempt gets a fresh line, keeping the stars already in hand | `StarPath.reroll` |
| **star-path difficulty** | each successful course advances one level (+0.6× bend rate), reaching level 10 and a 7× cap after ten wins; tuned for drag control, saved across playthroughs and cleared by Reset Difficulty | `StarPath.bendRate`, `GameCore.resetDifficultyScaling` |

## Boss

Every fifth stage is a boss instead of a wave. It has to be beaten for the stage to end — there is
no way past one. No boss can be beaten by typing alone: each asks for at least two of the three
things a player can do, which are pressing a key, tapping something and dragging something.

A boss stage releases **no falling words at all** — the fight is the stage. A dragging fight reddens
past `ENRAGE_AT`, but time alone never costs a life; damage comes from the boss's visible mechanics.

| Say | Means | Code |
| --- | --- | --- |
| **boss** | the set piece on stages 5, 10, 15, 20, 25… | `Boss`, `GameCore.boss` |
| **boss stage** | a stage that has one | `Boss.isBossStage`, `EVERY` |
| **arrival card** | the name over the field before the fight starts | `Boss.INTRO`, `BossScreen.intro` |
| **window** | the stretch during which a boss can be hurt. Shut, it can only be rebuffed | `Boss.open()` |
| **breather** | the shut half of the cycle between windows | `Boss.CYCLE`, `SHOW` |
| **boss header** | the health bar, name and blurb at the top. Shares the frenzy's slot, since a boss suppresses powerups | `BossScreen.bar` |
| **blurb** | the one-line instruction under the name; it retires after five seconds | `Boss.BLURB` |
| **element** | a hit-testable thing a boss puts on the field to be tapped or dragged. Always in the upper field, because a drag may not start on a key | `Boss.ELEMS`, `elemAt`, `etype` |
| **rebuff** | the right thing at the wrong moment, or a held key. Sounds wrong, never counted as a miss, and fires no bullet | `Boss.REBUFF` |
| **boss bullet** | the shot a landed key press fires at the boss, the same one a press at a word fires. Homes toward the boss as it drifts | `GameCore.bossShot`, `Shot.atBoss`, `Boss.hitX` |
| **enrage** | the visual warning on a dragging fight: it reddens, but does no damage by itself | `Boss.ENRAGE_AT`, `ENRAGE_RAMP` |
| **slime** | boss 1. A wide, twice-as-jiggly mass of goo. A chain of letters to type, and the only thing that hurts it is a glob carried off the screen | `Boss.SLIME`, `WIDE`, `JIGGLE` |
| **split** | working a glob loose: five presses of the chain. The presses themselves take no health off it | `Boss.SPLIT_HITS`, `split`, `splitProgress` |
| **prompt** | the character on the slime. After the first two successful prompt hits, the whole lower edge folds over it while invulnerable; the surprised prompt blends into the slime color and fades away in front of the skin, then reverses that fade over 0.3 seconds as the skin relaxes. It accepts hits from the start of reappearance. Three bubbles rise in pitch on covering and descend on release. Answer it while exposed before its two-to-one-second deadline or it leads a volley | `promptT`, `promptDelay` |
| **volley** / **bolt** | three projectiles launched by a missed prompt. They take one to three presses as the slime weakens; the next prompt waits until all three are gone | `Boss.BOLTS`, `bhp`, `boltWants`, `PARRY` |
| **parry** | swatting a bolt. Scores like a hit and does not touch the boss | `Boss.PARRY`, `BossPlay.press` |
| **glob** / **wart** | what the fifth press grows from the silhouette, red and visibly joined to the slime. The boss waits wounded with no prompt or volley clock until it is dragged to the edge or fades | `Boss.E_GLOB`, `GLOB_TIME` |
| **stretch** | the skin hauled out after a dragged glob, holding it inside the body however far it goes, and snapping back when it comes free | `Softbody.pull`, `enclose`, `letGo`, `Boss.PULL_K` |
| **tongue** | the shape that stretch makes: five nodes projected past the glob, with shoulders behind them | `Softbody.NECK` |
| **drag follow** | the body walking after its own glob — a quarter of the way for free, further only as far as it must for the stretch to still reach | `Boss.DRAG_FOLLOW`, `DRAG_REACH`, `followX` |
| **rest shape** | what a body believes it is: a circle for four of the five, an ellipse twice as wide as it is tall for the slime. Deform is measured against this, so a settled body reads zero whatever shape it settled into | `Softbody.reset(.., wide)`, `wide()`, `squashAspect` |
| **jiggle** | how springy one body is: amplitudes up and whole-body damping down, never the stiffnesses | `Softbody.jiggle` |
| **mesh** | the soft body's own nodes and spokes, drawn faintly inside it so the wobble reads as physics | `Slime.mesh` |
| **dark divide** | stage 10. Split the gelatinous cube with a two-finger pinch | `Boss.SPLITTER` |
| **octopulse** | stage 15. Defend keys and drag exposed arm tips away | `Boss.OCTOPUS` |
| **fly agaric** | stage 20. Shake the cap to defeat the spore-throwing mushroom | `Boss.MUSHROOM` |
| **boss friends** | four collectible boss portraits, awarded only by defeating their matching bosses | `BossCollect`, `Interlude.awardBossPrize` |
| **soft body** | how every boss's body is built: a ring of sprung nodes under pressure, so it dents where you hit it | `Softbody`, `Boss.body` |
| **burst** | what a beaten boss goes out on | `BossScreen.burst`, `Boss.LEAVE` |

## Powerup

| Say | Means | Code |
| --- | --- | --- |
| **powerup letter** | the single glowing letter drifting horizontally | `Power`, `GameCore.power` |
| **frenzy** | the 15-second period after catching one | `mode`, `modeLeft` ← *mismatch* |
| **mode bar** | the name, blurb and countdown at the top during a frenzy | `Hud.modeBar` |
| **frenzy taper** | how much of a frenzy's extra pace survives at this point on the ramp: all of it on stage 1, down to twice the stage's own by stage 11 | `Power.taper`, `LATE_RATIO` |
| **bounded player** | a bot with stated hands — presses a second, reaction, miss rate — that the difficulty curve is asserted against | `Bot`, `TestSoak.boundedPlay` |
| **FLURRY** | every key is a wildcard; letters and keys go rainbow | `Power.FLURRY` |
| **FLING** | the blade: a swipe cuts every letter it sweeps past | `Power.FLING` |
| **blade** | the cutting edge itself, drawn along the last stretch of the stroke | `Renderer.blade`, `GameCore.BLADE` |
| **stroke** | one blade *motion*, not one touch: it starts where the finger starts moving and ends when it stops. One touch can hold several | `GameCore.beginStroke`, `sliceTo`, `endStroke` |
| **dwell** | the beat of stillness that ends a stroke, so holding a finger down cannot hold a combo open | `GameCore.STROKE_DWELL`, `STROKE_MOVE`, `strokeIdle` |
| **stroke cap** | the backstop behind the dwell: the longest one stroke may run, for a finger that wiggles rather than stops | `GameCore.STROKE_MAX`, `strokeAge` |
| **dying blade** | the edge left behind for a moment where a stroke ended, so the end of a swipe is seen and not inferred | `GameCore.STROKE_FADE`, `strokeFade` |
| **slow-motion beat** | the brief slowdown a stroke earns by taking two or more words | `GameCore.slowdown`, `SLOW_RATE` |
| **slice call** | the "N IN ONE!" readout during that beat. Shows the counts frozen when the stroke that earned it ended, not the live ones | `Hud.sliceCall`, `GameCore.callKills` |
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
| **lifetime collections** | every basket ever opened, duplicates counted — the number under the position bar that keeps climbing after the case is full | `GameCore.collectTotal` |
| **earned mash** | how long the round bought at the steamer, and the only thing that sets it: 5s perfect, 4s unhurt, 3s hurt, 1s if the panic swipe was used | `GameCore.mashEarned`, `MASH_*` |
| **winded** | the field at a quarter fall speed for three seconds after a panic swipe, ramping back up | `GameCore.PUSH_SLOW`, `pushSlowT` |
| **swipe catchment** | where a panic swipe may start: the lower half of the field, much wider than the strip that advertises it | `Layout.inPushZone` |
| **shelving** | one of the haul reaching the case at the end of its trip, and the chime that says so | `RoundEnd.arrival`, `Sound.collect`, `Sfx.collect` |
| **game over screen** | score, accuracy dumpling, best. Fades up after the hold; GAME OVER is yellow, not rose | `Screens.gameOver` |
| **accuracy dumpling** | the face that reflects accuracy: tear below 60%, sparkles above 90% | `Screens.accuracy` |
| **settings panel** | opened by tapping the stage readout; pauses the game | `Screens.settings` |
| **minigame difficulty** | Minigames settings tab; saved Star Path level, applied next attempt, raised by wins | `SettingsUi.MINIGAMES`, `GameCore.setStarDifficulty` |
| **stage readout** | the "STAGE n" text — also the settings button | `Layout.inStageTap` |
| **speed slider** | the 0.5×–1.5× pacing control | `SettingsUi` |
| **stage jump** | the ±1 / ±5 steppers in the settings panel that jump straight to a stage, so a boss can be reached without playing twenty stages. Steps of five because bosses land on every fifth | `SettingsUi.STAGE_STEP`, `GameCore.jumpToStage` |

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
- **frenzy** vs **interlude** vs **boss** — the frenzy is the 15 s powerup period *during* play; the
  interlude is the minigame *between* stages; a boss *replaces* a stage's wave every fifth stage.
  Only one set piece runs at a time: a boss stage releases no powerups at all.
- **window** vs **breather** — a boss's window is when it can be hurt; the breather is the shut half
  between windows. Not to be confused with the stage **breather**, which is the pause between waves.
- **element** vs **tile** vs **key** — an element is a boss's own touch target; a tile is a cell of a
  falling word; a key is one of the six hexagons. Only elements are dragged in play.
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

## Linked friends (stage 16 onward)

Each ordinary wave from stage 16 contains two linked pairs: one opens the wave, and another
arrives after three stage enemies have spawned (the opening pair and two solos). Each pair has one key from each thumb's keyboard
group. Each pair occupies one stage slot and two physical crowd slots; a blocked entrance defers the pair rather
than replacing it with an ordinary enemy. Fruit keys grow curling vines; the cat and blue creature
have padded paws, while the other characters have soft hands. Key-colored limbs intertwine at the center, with vines passing
behind and in front of the partner.
Press either key, then its partner within 200 ms of elapsed play time (independent of speed
settings and slow motion). Input resolves the chord immediately; projectiles are visual only.
A single press keeps both faces visible with determined expressions and pulses the thickness
of both intertwined limbs. Idle arms are nearly straight with very subtle movement. Elbows hold a smoothly curved flex pose with
a slight bulge on rejection; both keys ease a little toward each other and briefly thicken their hexagon outlines; a bright color
front travels from the clasp toward both characters. The first press plays the regular miss sound instead of a hit. A missed window renews that resistance pose without replaying the sound. The unpressed
partner's gold highlight remains visible; there is no countdown circle, text, or checkmark. Arms and vines
have varying thickness along their length, solid hexagon-colored fills with stronger underside shadows and upper highlights for rounded volume, and key-colored outlines matching the hexagons. Limbs render behind the characters
and are cut out around both complete character tiles, including during the flex. A missed window restores the first key without a life penalty
or score; both enemies still descend. A successful chord credits one stage enemy while retaining both character rewards; the clasp opens, the characters peel outward with their hands, and a short curved whoosh marks the release point. Stage credit waits until both halves are cleared or breached, even if the bond breaks. The retired MULTI mode releases
links, and a breached partner cannot leave the other waiting forever.

Stage 16 gives 15% longer travel and spawn intervals and caps the field at four enemies.
Stage 17 retains 7.5% timing relief; stage 18 returns to the ordinary curve.
Rules: `LinkedPairs`; visuals: `LinkedPairArt`; difficulty: `Pacing.lessonRelief`.

Linked pairs keep their hands connected during FLING, FLURRY, and TEAM SQUISH. One TEAM SQUISH collision clears both and sends them spinning around their clasp offscreen. FLING protects their bodies and requires a cut through the joined hands to clear both. A body-only cut plays the miss sound and flexes both characters once per stroke. FLURRY accepts any two distinct buttons within 200 ms; repeating one button cannot clear a pair. Entering these powers resets a pending half-press.

From stage 16, each active power-up starts its own repeating spawn pattern: one linked pair, then two solo enemies. The pattern continues beyond the ordinary wave quota and uses the current power-up pacing and crowd cap.

### Mystery pickups (stage 11+)

The floating pickup cycles through eligible powers plus INCOGNITO and MONOCHROME. A direct tap chooses the result randomly at collection time and starts a 0.55-second roulette selection, followed by a one-second selected-icon reveal with an outward bloom for powers or inward violet wisps for debuffs; repeated taps do not reroll it. Team Squish requires a collected character, and MULTI remains excluded. Before stage 11 pickups keep their existing fixed outcomes.

INCOGNITO puts pixel sunglasses on enemy and player characters, then morphs them into one shared round disguise differentiated only by key color. The 0.75-second transformation reverses when it expires. MONOCHROME fades the entire play area to grayscale over 0.6 seconds and fades back afterward. Both debuffs last eight seconds, preserve normal input and stage difficulty, pause with the game, and clear on stage changes or a new run. They do not trigger frenzy spawn/fall boosts or clear the stage on expiration.

Stage 11 introduces mystery pickups with 15% longer travel/spawn intervals; stage 12 retains 7.5% relief and stage 13 returns to the normal ramp.

## Land travel

The title land picker spaces icons apart and moves one adjacent unlocked land per swipe. Each
change takes 0.85 seconds: the Adventure Dumpling pops up on the trailing side of the previous
land, follows a gentle arc toward the destination with soft shuffling footsteps, and settles out.
Icon centres alternate vertically by 30% of the full icon height; the journey follows both heights. Leftward
travel mirrors the entrance. Rapid swipes queue separate journeys; tapping a distant icon visits
each intervening unlocked land. Discovery animations pause while travelling.

`LandPicker` owns input and choreography; `GameCore.landTravelQueue`, `landTravelFrom`, and
`landTravelT` hold the session-only journey state.

FLURRY activation sends a translucent seven-band circular rainbow outward from the pickup
position for 1.6 seconds. Mystery pickups wait for their selection to finish; the origin stays
at the original pickup. The ring ends with the power or run. `GameCore.powerBurstX/Y` preserve
the origin and `Renderer.flurryBurst` draws it without changing gameplay timing.

## Release book

The title’s “What’s new” entry opens a “What’s cooking?” book for the full authored release
history, newest first; scroll down to reach older entries. Each release groups icons for its individual
changes, left-aligned and without feature labels. Extra icons wrap into additional rows; the list scrolls vertically when needed. Each release groups all fixes under one cute bug icon. Lift on an icon to open its content-sized popup; drag to scroll. Tap outside the release-list window to close it. Opening, closing, and entering an item give one soft bloop and light haptic.
Land travel, the mystery pickup, and the 200 ms linked pair have interactive demos. Smaller
highlights have compact illustrations. Every feature has its game phase and a short player-facing purpose. All Releases or Back preserves list scroll;
close, or Back from the list, returns to the title. Demos own an isolated `GameCore` with no store.
`ReleaseChange` draws icons; `ReleaseNotes` owns geometry, demo state and input.
The human-editable `release-notes/releases.json` generates `ReleaseContent` through `tools/release-notes.py`;
see `docs/release-notes.md` for writing and `docs/releasing.md` for the pre-tag checklist.
The title scene keeps animating behind translucent panels while gameplay input stays blocked.
`ReleaseTransition` moves the steamer to centre, then off left as the book arrives from the right;
closing sends the list off right while the steamer enters from the left at its normal corner size and height. Feature pages slide in from
the right as the list leaves left; Back reverses this without losing the list position. MVP scope is in `docs/plans/interactive-release-notes.md`.

The **What’s new steamer** (`ReleaseMascot`) has a cute face, no arms, rising steam, and a lid that lifts when tapped. For an unread build, a star turns behind it, the lid pops repeatedly, and extra steam rises while the normal title screen stays usable. Tapping it opens the release list and saves the build as read; until then, the animation returns on title visits. **RESET NEWS** in developer settings restores this unread state.

The **land discovery tour** (`LandDiscovery`) uses the swipe traveler’s size and arc, sliding through newly unlocked lands in sequence. Each first arrival pulses that land’s color and persists its seen flag; later visits do not repeat the glow. Covered title scenes pause the tour.

The developer settings **ALL LANDS** chip enables every land for the current session without granting collectibles. **RESET LANDS** clears that override. The dotted walking trail is fixed relative to the lands and stops outside their silhouettes. Rightward swipe movement is preserved; leftward travel follows that fixed curve in reverse.

## Cave expedition

The developer-only fifth land, after Mushroom Land, begins at stage 21. Production retains
its original four-land progression. The **explorer dumpling** automatically
walks along the cave path while a following camera looks ahead. Tap the playfield to aim the
**lantern beam**; illuminate a branch at a **fork** to choose it. Three fading lights show its
limited pause. A finger demonstrates the first fork before that countdown begins.

**Shadow enemies** reveal a character-key response in the beam, then approach the explorer.
Completing the response defeats them; reaching the explorer costs a life. **Route hearts** heal
one life when walked over. A **cave-in** uses sideways dragging to dodge falling rocks and their
landing shadows; **quicksand** uses alternating character keys to escape. Reaching the **exit**
completes the stage. `CaveRoute`, `Cave`, `CaveTraps`, `CaveInput`, `CaveScreen`, and `CaveArt`
separate route content, rules, controls, and drawing. See [cave design](docs/plans/cave-expedition.md).

The **explorer selection** (`CaveSelection`) appears on first cave entry. Choose cream, rainbow,
golden, silver, sparkly mint, or purple; that finish persists across cave levels and restarts.
`CaveDumpling` draws the chosen finish and uses a normalized `Softbody` to give each walking step
a small spring-driven lift and squash. Cave walking speed is .36 route units per second.
