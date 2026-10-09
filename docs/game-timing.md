# Game timing

Kids Mode snapshots the preference at the start of a run. `GameCore.traversalRate()`
is the single source of its movement multiplier: **0.45 in Kids Mode, 1 otherwise**.
Never multiply the shared update clock by this value. The developer speed slider,
runtime setting, and persistence hooks have been removed; old saved speed values are ignored.

| Area | Apply Kids traversal multiplier? | Boundary in code |
| --- | --- | --- |
| Falling words, including frenzy words and side entrances | Yes | `GameCore.update`: downward movement |
| Word's final lunge and its arrival at the player | Yes | `GameCore.update`: `attackT` and lunge displacement stay together |
| Boss letter bolts, including spores and Divide volleys | No | `Boss.update`: normal `dt` for projectile flight |
| Player shots toward words or bosses | No | `Fx.updateShots`: normal `dt` for flight progress |
| Cave enemy approaching the player | Yes | `Cave.update`: approach after the reveal; contact deadline follows position |
| Cave player bolts | No | `CaveEnemy.update`: normal flight and impact timing |
| Boss introduction, prompts, charge, attacks, arm sequences, recovery, defeat | No | `Boss.update`: ordinary `dt` for all actions and body animation |
| Stage gaps, spawn countdowns, frenzy duration | No | `GameCore.update`: ordinary `dt` |
| Steamer, Star Path, Cart Rush, Dumpling Mine | No | Normal animation timing; Kids Steamer holds its play phase until completion |
| Cave travel, route choice, reveal, hazards, retreat, camera | No | Normal sequence and animation timing |
| Key feedback, particles, soft bodies, UI, music, celebrations, collection parade | No | Normal animation/audio clocks |
| Panic-swipe push-back playback | No | `slideT` resolves the player's action at normal speed |

Boss actions and projectile flights retain their durations. Movement-linked
cues (word entrance/warning and cave approach stomps) follow the slowed position.

Kids Mode's other assistance remains separate: four keys and early-stage movement/spawn pacing. Word length
and stack odds follow the actual stage, with a six-press total cap including multipress
keys. Linked pairs use the same 200 ms window as normal mode. These are rule choices,
not a shared time multiplier. Lives and game over remain enabled.

Kids Mode gives the Steamer unlimited play time and a fixed ten-pip target, independent of
lifetime wins. Its spinner, lid escape and parade still finish normally. It caps Star Path at 30%
of its normal difficulty ladder (level 3 of 10). Saved Star Path progression is preserved
for normal mode; easier saved levels stay easier. Animation and flight clocks remain normal.

Existing gameplay effects are separate too: frenzy fall-rate boosts, panic-swipe
recovery, and brief earned slow-motion beats retain their own rules. Pause still freezes
play. Real-time input windows use `elapsed`, independently of simulation slow motion.

When adding movement, decide explicitly whether it belongs in this table. Pass scaled
time only to flight/movement, never to a whole boss, minigame, or animation system.
Regression coverage lives in `TestSettings`, `TestWords`,
`TestLinkedPairs`, `TestCave`, and `TestCaveMining`.
