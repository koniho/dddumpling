# Let more players meet the bosses

*DDDUMPLING dev blog · November 1, 2026*

Playtesting DDDUMPLING with kids made a problem clear: the difficulty ramp was too severe, especially for players who were still learning the keys. Familiarity with your own game makes it easy to underestimate how much a new player has to process. Recognizing a character, finding its matching key, reading the next prompt and deciding which enemy needs attention all take practice.

That matters because some of DDDUMPLING's biggest payoffs come later. The bosses are characters to discover, with their own personalities, physical interactions and squishy celebrations. We want less experienced players to have a real chance to reach them. The ordinary stages need to build the skills and confidence that get players there.

The design goal is a dynamic difficulty system that keeps players challenged while making that journey more accessible. A player who is comfortably clearing the screen should feel the game ask more of them. Someone still learning should have room to learn without repeatedly losing before the good stuff.

The first decision was to keep spawn pressure intact. Clearing a steady stream of enemies is a large part of the game's energy. Instead, assistance adjusts how complicated each enemy is and how quickly it approaches.

At the most assisted setting, an enemy requires at most three presses and uses at most two different key symbols, moving at 75% of its stage's normal speed. A middle setting relaxes those limits. Normal restores the existing difficulty ramp, which remains the ceiling. New players start with four keys; the additional two arrive after strong play and the first boss milestone. Returning players keep their earned roster and saved assistance, with an early-loss fallback to help when a new run goes badly.

Getting those settings right was only half the work. We also had to decide what good play looks like.

Our first attempt required consecutive strong stages with high accuracy. Local playthrough logs exposed how easily that could get stuck. One recording cleared twelve ordinary stages without damage or a rescue, yet stayed at maximum assistance. A clean Ninja stage had too few counted key presses to qualify. Other stages narrowly missed the accuracy threshold and erased earlier progress, even when enemies barely entered the playfield.

That led to a useful observation: if enemies never make it halfway down the screen, the player probably has spare capacity. We now consider that control of the playfield alongside accuracy. Side-entry enemies are excluded from that particular signal because they can arrive lower down already.

We also stopped treating rescue use as a failure. It is a tool we give players, and using it well should not automatically count against them. Power-up stages hold assistance rather than judging performance under a different set of rules. Boss encounters stay separate from ordinary-stage assessment too.

The next recording exposed another problem: holding during power-ups was working, but it left too few eligible stages to remove assistance promptly. We made the adjustment asymmetric. One strong ordinary stage now removes one level of help. Three damaged ordinary stages, without a strong stage clearing that negative confidence, add a level of help. Borderline stages hold steady. Run-ending ordinary-stage losses retain a more immediate fallback.

In the latest test, the player started on Light Help and reached Normal after the first eligible stage. Later waves created noticeably more pressure, while a single hit did not immediately undo the promotion. That is closer to the behavior we want.

These recordings are local developer logs, exported manually for analysis. They help us understand what happened; they cannot tell us everything about how a player felt, or whether a loss was deliberate. More playtesting with less experienced players is still essential.

The bosses should feel like an exciting destination players can work toward. Our job is to make the path demanding enough to be satisfying, and flexible enough that more players get to discover what is waiting at the end of it.
