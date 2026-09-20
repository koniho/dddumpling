# Pacing and bounded players

Historical failure notes, moved from `AGENTS.md`. Read only entries relevant to the task.
Values and file ownership may have changed; verify against current code. Current workflow
policy lives in [AGENTS.md](../../AGENTS.md).

- **Tune against the real firing rate.** The per-press sky glow looked right in one frame and
  strobed in play; the 4s minigame let a masher finish it in one go, defeating the
  accumulation it was built for.

- **Two difficulty sources must never multiply.** The frenzy's pace was flat multipliers — 6x the
  spawn rate, 4x the crowd, 2x the fall — sitting on top of a ramp that had already halved the spawn
  interval, so they compounded with it. By stage 10 a frenzy asked 23 presses a second and by stage
  22 it asked 43, against maybe 8 from two thumbs. It read as the game breaking, and it was felt
  worst in FLURRY, which is the one mode buying accuracy rather than throughput. `Power.taper` now
  scales all three down along the ramp toward `LATE_RATIO`, and `SKY_RATE` is deliberately left flat:
  taper what costs the player, leave what only looks exciting, or the reward stops reading as one.
  Anywhere a difficulty path multiplies two dials, check what their product does at the far end.

- **Making a target bigger is a difficulty change, not a cosmetic one.** The star course's flyer and
  checkpoints went up by half, and the pickup test was two flyer radii — so the catch band went from
  a ninth of the screen width to a sixth, which is wider than the course's own wander. A flyer left
  dead centre with nobody touching the keys then completed courses by itself, collecting a free
  collectible and a free life; the stars carry over between attempts, so anything it can reach it
  eventually finishes. The soak bot found it, having no way to steer at all — it turned up as the
  monotonicity check failing, because a skill-independent bonus had been added to every tier.
  `StarPath.pickupR` aims at the star's drawn *heart* instead, which keeps the old number. Resize a
  target and re-derive the rule, or state deliberately that the rule is unchanged.

- **A generator tuned per step is tuned against the wrong clock.** The course used to be a random
  walk with a bounded step per checkpoint. But the scroll accelerates, so the last stars arrive three
  times as fast as the first: one bound per step meant a demand three times higher at the end than at
  the start, and it had to be small enough for the end — which is why the course could only wiggle
  down the middle. `StarPath.make` is a sine in *seconds* now, so it asks the same sideways speed
  everywhere and can span the whole play area. `TestStars` prints the peak demand against `MAX_VX`;
  the two are meant to be read together, but see the next entry for why that ratio is only printed.

- **A peak is not a demand: integrate it.** For a long time the course was held to "asks about 1.2×
  `MAX_VX`, which is the near side of uncatchable", and that number turned out to measure almost
  nothing. A sine only exceeds the steering speed over a short arc either side of its steepest point,
  so 1.2× cost a perfect tracker one hundredth of a play width — a twelfth of the catch band — and
  every pilot the harness could write collected all twenty at *every* reaction time from 0.05s to
  0.30s. The course was fully trackable and the assertion said it was at the edge of possible. What
  the player feels is the *lag* that overspeed integrates to, measured against the tolerance it has
  to fit inside; `TestStars.trackerLag` is that, held between 45% and 110% of the band, with
  `tightestWindow` — the propagated set of positions from which every remaining checkpoint is still
  reachable — as the far bound. The cliff is real, it was just being located with the wrong ruler.

- **Put the difficulty on the axis the player controls.** The pickup was a circle, so spacing the
  stars out and shortening the flight cut the time a checkpoint is level with the flyer from 117ms to
  83ms — and nothing a player does moves that number, since the climb and the scroll are both
  functions of the clock. It is not difficulty, it is how much of the difficulty is luck: the naive
  pilot went from nineteen stars a course to ten with the sideways demand unchanged. `pickupY` makes
  the catch an ellipse and `TestStars` measures the window, so the vertical stays generous and the
  demand lives where the keys are.

- **A tolerance for being late is measured in milliseconds, so store it in milliseconds.** That
  ellipse was first written as 1.7 pickup radii, which is 117ms of grace at one scroll speed and
  47ms at two and a half times it — and the next spacing change duly ate it, silently, because a
  distance says nothing about the thing it is actually buying. `StarPath.GRACE` is 0.065s and
  `pickupY` is that times `closingSpeed`, a one-frame finite difference of the gap the course is
  closing. It now survives any change to spacing, flight length or the rush curve, and the failure
  it prevents is the sort this file keeps recording: an arithmetic coupling nobody restated.

- **Spacing, timing and lookahead are one equation with two degrees of freedom.** Stars 2.5× further
  apart with the arrival rhythm unchanged means a scroll 2.5× faster, and therefore 2.5× *less*
  course on screen ahead of the flyer: 162ms of visible warning where there had been 400. There is
  no third dial — pick two. That is what set the ceiling on `COURSE_SCREENS`: at 162ms the line
  arrives inside a hand's own reaction time, so it reads as unfair rather than fast, and 5.6 (295ms)
  is where it settled. Note which side of this the harness cannot see: every pilot in `TestStars`
  knows the whole course in advance and scores exactly the same at any lookahead at all, so
  `TestStars.lookahead` prints the number for a human to judge and guards a floor that was set from
  the device.

- **A knob documented as cosmetic has to be checked, not asserted.** `COURSE_SCREENS` was described
  in its own comment as leaving the rhythm of a course alone, and it did — but `encounterTime` took
  its lead term as `0.5 / COURSE_SCREENS`, so every arrival *time* moved with it, the sweep got
  sampled at different moments, and what came back was a different course. Dialling the spacing back
  with nothing else touched took the naive pilot from nineteen stars to thirteen, and the first
  instinct was to go hunting in the catch tolerance, which had nothing to do with it. `StarPath.LEAD`
  is frozen at 0.05 now. If a knob is meant to be cosmetic, find every expression it appears in
  before believing the comment; the ones that matter are the ones feeding a *time*.

- **Two knobs on the same speed multiply into a third nobody tuned.** The scroll here is
  `COURSE_SCREENS / FLY`, times the tail of the `RUSH` curve, times what a normalised ease-in
  borrows and pays back. Each of those was a defensible move; together they put the closing speed up
  by 60% and the timing window down by a third. Before turning the second dial, write down what the
  product does at the far end — the same trap the frenzy's flat multipliers were.

- **A retry that hands back the identical problem is a wall, not a retry.** The stars carry over
  between attempts, which is meant to mean a slower player gets there over a few stages. On the same
  line they do not: the harness pilot with quarter-second thumbs takes the same fifteen every time,
  and three of eight courses stalled at seventeen or nineteen for as many attempts as it was given.
  `StarPath.reroll` gives each attempt a fresh line and keeps the hand, which is what makes the
  carry-over a promise instead of a consolation. Note it also costs three RNG draws per interlude, so
  every generated word after the first one moves — expected, and the frame hashes show it.

- **A pilot with perfect knowledge measures reachability, not difficulty.** `TestStars.flown` knows
  exactly where every checkpoint is and is limited only by a reaction quantum, so inside the
  trackable band it scores 20/20 at every setting and cannot tell you whether a course is *hard*.
  Use it for the two questions it can answer — can this course be completed at all, and can it be
  completed by nobody-at-the-controls — and take feel to the device. Since it does not brake and
  looks one checkpoint ahead, treat what it takes as a floor, and prefer *attempts to finish* over
  stars-per-attempt: the second is a six-sample coin flip and the first is the actual promise.

- **A four-sample binary is a coin flip, not an assertion.** `TestSoak` required all four runs of
  every tier to end in a death, and the quick tier — nine presses a second, one miss in fifty —
  reaches the cap alive on about one seed in twelve. Shortening the star flight by four tenths of a
  second was enough to turn that over, because it moves every RNG draw after the first interlude. It
  allows one survivor now. If a check is a yes/no over a handful of noisy runs, either state the
  tolerance or assert on the average instead.

- **Half a fifth of a curve is a straight line.** However swoopy a scrolling course is over its
  seconds, what reads is how much of it fits on screen: at six checkpoints it looked like a diagonal
  whatever the generator did. `COURSE_SCREENS` is what that knob is — 4.2 now, so just under five
  checkpoints are in shot — bounded below by the checkpoints touching each other and above by there
  being too few of them on screen to read a line from.

- **The soak bot cannot steer, so it cannot tell you a course is playable.** `TestStars.flown` is a
  pilot with stated limits — it re-decides every *reaction* seconds and holds one thumb — and the
  same routine with steering off is the passenger the soak bot is. Three numbers come out: what quick
  thumbs take, what slow thumbs take, and what nobody-at-the-controls takes. The last one is the
  guard; see the pickup entry above for what it caught.

- **A beat that fires once can be too long to fire twenty times.** The fling stroke's slow-motion is
  0.28s, and reusing it for a star pickup would have put the end of a good course in continuous slow
  motion — and every beat lengthens the course in real time, since the course clock is scaled too.
  `GameCore.STAR_BEAT` is a quarter of it, and there is an assertion on what a clean run costs in
  real seconds. Before reusing an effect, count how often the new caller fires it.

- **A bonus larger than the scale it sits on erases the scale.** A frenzy used to add 2.6s of
  interlude on top of whatever the round earned — more than the whole 4s spread of the earned-mash
  ladder — so a hurt frenzy round out-paid a perfect calm one and the ladder communicated nothing.
  Gone. Before adding a modifier to a scored thing, check it against the *spread* of the thing it is
  modifying, not against zero.

## Choosing the simulation

Use `TestSoak.boundedPlay` for tuning: press-rate and reaction limits reveal problems that
perfect play cannot. Read the current `Bot` implementation for supported gestures and limits;
older notes above describe limitations at the time. The full rules run already includes
Soak, so do not repeat it after that run passes. Use `-q -r -s Soak` while iterating only if needed.
