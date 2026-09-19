# Cave minigames

Caves start with **Cart Rush** and switch to **Dumpling Mine** only after success, then back
after mining succeeds. Failure retries the same game after the next stage. The selection and
each game's progress persist across runs and app restarts.
Cart Rush replaces the cave rhythm game. The digging game retains its existing behavior.
Other lands keep Steamer and Star Path.

## Cart Rush

Drag the Star Path slider or grab the cart to lean; the slider replaces the keyboard.
Incoming rails preview turns and fade into darkness at the far end. Foreground tunnel arches
pass over the rails and riders. A balance meter with a safe center and danger ends replaces
the turn arrow; its marker follows the passengers' outward drift. The camera follows behind the cart: rear panel, edge-on wheel treads,
and the backs of the passengers. Rails integrate the same smooth, varying curvature used by
balance and cart banking; sharper bends tilt the cart further. The crew lean, panic, and spill out if balance is lost. Tunnel ribs,
crystals, sleepers, wheel movement, speed streaks, sparks, sound, and haptics convey speed.

Twenty sections earn a snake. Completed sections save immediately; unfinished sections restart
next visit. Visits last up to ten seconds after a 1.4-second ready beat. Existing ride progress
retains its saved key, but mining carts no longer migrate into ride progress. Each game resets
only its own progress after paying its reward.

The red balance warning lasts 1.25 seconds before a spill. Returning to the safe region resets
the countdown. Unhandled checkpoints wait for steering instead of causing instant failure.

## Dumpling Mine

The original digging game remains: two-, three-, then four-key sequences, five mined wall
segments per cart, a swipe to send the loaded cart away, and five delivered carts per mole.
The lantern dims as time runs out. Prompts run down the wall, hits fly into the pile, the miner
walks to the next segment, and friends load and haul the cart. Idle cheering is visual only;
the repeating voice cue is removed. Delivered carts persist.

## Verification

TestCaveCart covers steering, independent saves, checkpoints, rewards, pause, and bounded players.
TestCaveMining retains the digging regressions. Native input checks exercise both games.
Preview 109 shows mining; 111 shows the ride. Legacy rhythm synthesis and transport tests remain
for the retained audio code, but Cave Band is no longer selected by stages or developer chips.
