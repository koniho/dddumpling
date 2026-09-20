# Game timing

Kids Mode snapshots the preference at the start of a run. `GameCore.traversalRate()`
is the single source of its movement multiplier: **0.45 in Kids Mode, 1 otherwise**.
Never multiply the shared update clock by this value. The developer speed slider,
runtime setting, and persistence hooks have been removed; old saved speed values are ignored.

| Area | Apply Kids traversal multiplier? | Boundary in code |
| --- | --- | --- |
| Falling words, including frenzy words and side entrances | Yes | `GameCore.update`: downward movement |
| Word's final lunge and its arrival at the player | Yes | `GameCore.update`: `attackT` and lunge displacement stay together |
| Boss letter bolts, including spores and Divide volleys | Yes | `Boss.update`: only `ageBolts(projectileDt)` |
| Player shots toward words or bosses | Yes | `Fx.updateShots`: flight progress |
| Cave enemy approaching the player | Yes | `Cave.update`: approach after the reveal; contact deadline follows position |
| Cave player bolts | Yes | `CaveEnemy.update`: flight only, then normal-speed impact effect |
| Boss introduction, prompts, charge, attacks, arm sequences, recovery, defeat | No | `Boss.update`: ordinary `dt` for all actions and body animation |
| Stage gaps, spawn countdowns, frenzy duration | No | `GameCore.update`: ordinary `dt` |
| Steamer, Star Path, Cart Rush, Dumpling Mine | No | Normal ready, play, countdown, reward and exit timing |
| Cave travel, route choice, reveal, hazards, retreat, camera | No | Normal sequence and animation timing |
| Key feedback, particles, soft bodies, UI, music, celebrations, collection parade | No | Normal animation/audio clocks |
| Panic-swipe push-back playback | No | `slideT` resolves the player's action at normal speed |

Boss actions retain their durations. Actions that already wait for the board to clear
can still wait longer because a projectile remains airborne longer. Movement-linked
cues (word entrance/warning and cave approach stomps) follow the slowed position.

Kids Mode's other assistance remains separate: four keys, short unstacked words,
early-stage word pacing, and a 600 ms linked-pair window. These are rule choices,
not a shared time multiplier. Lives and game over remain enabled.

Existing gameplay effects are separate too: frenzy fall-rate boosts, panic-swipe
recovery, and brief earned slow-motion beats retain their own rules. Pause still freezes
play. Real-time input windows use `elapsed`, independently of simulation slow motion.

When adding movement, decide explicitly whether it belongs in this table. Pass scaled
time only to flight/movement, never to a whole boss, minigame, or animation system.
Regression coverage lives in `TestSettings.kidsTiming`, `TestCave`, and `TestCaveMining`.
