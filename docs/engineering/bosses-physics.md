# Boss mechanics and physics

Historical failure notes, moved from `AGENTS.md`. Read only entries relevant to the task.
Values and file ownership may have changed; verify against current code. Current workflow
policy lives in [AGENTS.md](../../AGENTS.md).

- **A rule can be quietly carrying a job nobody wrote down.** Boss stages used to run a thin wave
  under the fight. Taking it away on request removed two things that were never stated: the only
  thing that could hurt you on four of the five bosses, and — since enraging worked by speeding the
  spawns up — the only reason to hurry. Boss-local mechanics and the belt-press charge replace them.
  Before deleting a subsystem, ask what else is leaning on it; the compiler cannot tell you that a mechanic
  was the load-bearing threat.

- **A mechanic paid for in another system's currency dies when that system does.** SUMO's swipes cost
  a charge earned by clearing words, so the moment boss stages stopped spawning words it could not be
  beaten at all. Charges come off its own belt now. Anything that spends a resource should earn it
  from inside the same fight.

- **An object created between layout passes has no position.** A glob is shed by a press, which has no
  `Layout`; the layout pass does not run until the next frame, so for one frame both the renderer and
  the hit-test read it at the origin — a red blob in the top-left corner every time the boss was hit.
  `Boss.seed` places it from the body's last known spot, and `place` is the single copy of that
  arithmetic so the two cannot disagree by a few pixels on the second frame.

- **A sustained force is not a repeated impulse.** Stretching the skin toward a dragged glob has to be
  a force inside the solver's substeps, not a velocity kick per frame — a per-frame kick pulls twice
  as hard at 120fps as at 60. And it has to be a *clamped target* rather than a range check: testing
  the raw target against the reach meant the tug switched off as soon as the glob got further than a
  radius away, so the skin twitched at the start of a drag and then let go, which is the opposite of
  the intended read. Scale it by the body radius like every other force there, or it dents a
  full-size boss and flings a thumbnail.

- **A health bar that goes up reads as cheating, not as difficulty.** The slime's globs used to crawl
  back and heal it. Watching the bar climb while playing correctly is not a hard fight, it is an
  unfair one — and it punished the player for having only so many fingers. Nothing heals now, and a
  window shutting mid-chain does not undo the presses that landed in it either.

- **When only one action scores, the other one is the fight.** A slime shed a glob per press, which
  made the chain the interesting half and the drag a chore repeated every second — five globs in the
  air with three slots to hold them. Five presses to work one loose, and the drag as the only thing
  that takes health off it, moves the whole fight into the drag and leaves the chain as what earns you
  something to fight with. Health is counted in globs carried off now, which is why the number looks
  so much smaller than the others: read `Boss.HP` against what one point of it actually costs.

- **A penalty for a mistimed press must not tax ordinary play.** The boss claimed its own letter
  whenever nothing was engaged, so starting a word that happened to begin with the drum's letter was
  read as a fumbled beat — and a landed rebuff resets that beat, so typing normally pushed the window
  away. It surfaced as the soak curve *inverting*: the quick tier finished below the steady one,
  because faster hands start more words and trip over that letter more often. `GameCore.bossClaims`
  now only hands a shut boss its letter when no word on the field wants it either; a deliberate early
  press is still punished, because then there is nothing else the press could have meant. If speed
  ever makes a tier do worse, look for a rule that is charging for a keystroke's *side effect*.

- **A set piece with no way past it needs the bot to be able to play it.** The boss on every fifth
  stage has to be beaten for the stage to end — there is deliberately no retreat. The soak bot could
  only type, and no boss can be beaten by typing alone, so all three tiers sat on stage 5 for the
  full fifteen minutes and *nobody died*: not a wall, a stalemate, and every difficulty assertion
  past stage 5 silently became a test of the first four stages. `Bot` taps, drags and shoves now,
  with its own stated limits for each (`DRAG_SPEED`, `PANIC_WARN`). It still never uses the blade or
  the panic swipe, which keeps those pessimistic. Before adding anything mandatory, ask what the
  harness's player does when it meets it.

- **Health times cycle length, never health.** The first pass at boss tuning set each one's hit count
  and its window rhythm separately, and `TRIPLETS` came out wanting six chords at one window per
  4.5s — 27 seconds of flawless play against a 22-second fuse, so a *perfect* run lost. Every boss's
  health is now set against one figure: a competent player finishes inside about fifteen of the
  `ENRAGE_AT` seconds before it turns nasty.

- **Two rules that each look right can deadlock.** An engaged word outranks the boss (the same rule
  the powerup has), and `TRIPLETS` wanted its chord inside one window. Together: whenever a window
  opened while a minion was part-typed, the chord could not be *started*, and the bot got eight
  windows and two chords out of a whole fight. The chord is timed from its own first press now
  (`CHORD_TIME`) and that boss is permanently open. When a mechanic will not fire, check whether two
  correct rules are excluding each other.

- **A predicate used for a hint must mean "this will work", not "this is mine".** `Boss.wants` was
  `open() && asksFor()`, and `asksFor` includes a triplets head that is awake but already struck —
  because pressing one is plainly aimed at the boss and should earn a rebuff rather than fall through
  to a word. But `wants` is what rings the key hint, so it advertised a key that could only be
  refused: the bot pressed it, was rebuffed, and pressed it again, losing ten chords out of ten. The
  claim and the invitation are two questions and need two methods.

- **Anything that lays out per frame must run above the early returns too.** `Boss.layoutElems`
  sat below the arrival-card return, so on the very frame the card ended — the first frame a tap is
  accepted — every element was still at the origin and a tap on one hit nothing. Same shape as the
  `warnLevel` bug, in a different method.

- **Resolve the kill before the side effects.** A `MAGPIE` killed by the final press still ran its
  "drop the key" step afterwards, putting an element on the field carrying key `-1` — an array index
  waiting to happen, and it happened on the first run of the assertions. Anything a hit triggers has
  to ask whether that hit was the last one.

- **A boss owns more than any other set piece, so its cleanup is bigger.** A body, a health bar, up
  to three draggable elements, a finger mid-drag, and one of the player's own keys held hostage.
  `Boss.leave()` clears every field it writes, not just the ones drawn today, and `TestBoss.cleanup`
  walks all five bosses through both exits — beaten, and the player dying mid-fight — then checks the
  frames *after* the death, since that is where the old versions of this bug were visible.

- **"Always" is a promise about geometry, so only geometry can keep it.** The glob has to stay inside
  the slime however far it is dragged. The tug that stretches the skin is a spring balanced against
  the pressure and the pull toward round, so where it settles is wherever those three happen to
  agree — which looks right most of the time and lets go of the glob exactly when the drag is going
  well. `Softbody.enclose` projects five nodes past the glob every substep instead, and drops the
  inward part of their velocity as it does: without that, the stretched skin springs in against the
  constraint every substep and the two grind, storing energy that comes out as an explosion on
  release. If an effect has to hold *always*, a force will not do it.

- **Bound what a constraint has to cover, or it stops being a body.** Containment alone would make a
  glob dragged to the far wall a spike two-thirds of the way across the field. `Boss.updateFollow`
  walks the whole body after its own glob — a quarter of the way for free, and further only as far as
  it must for the stretch to still reach — so the constraint is never asked for more than a body's
  worth of tongue. Which is also the better read: the slime is hauled along after the piece being
  stolen, rather than standing still while it goes.

- **A measure that means one thing for a circle can mean nothing for an ellipse.** `Softbody.deform`
  was the spread of the node radii about their *mean*. The moment a body could rest as an ellipse, a
  settled slime read 0.24 of deform standing perfectly still — five times what a solid hit is — so it
  was drawn permanently whited-out at full rim weight, and the assertion that a hit deforms it was
  measuring the shape rather than the hit. It is measured against the rest radii now, so it is zero
  for anything settled whatever shape it settled into. When you generalise a shape, re-read every
  quantity derived from it.

- **A bound in units of the wrong axis is an effect that never happens.** The drag follow's reach was
  a multiple of the body radius, which on a body twice as wide as it is tall meant a sideways drag
  was handed to the follow while the glob was still deep inside the goo — the body walked the whole
  way and the skin had nothing left to do, which is the one thing the mechanic exists to show.
  `Softbody.restToward` gives the rest radius along the drag, and the reach is a multiple of that.

- **A frame-per-hit flag stops working the moment two can land together.** `Boss.update` returned
  a boolean meaning "the player takes a hit". A volley of three bolts launched on one frame arrives on
  one frame, so that flag would have charged one life for three. It returns a count now, and the
  bolts carry a stagger so they are spaced in the first place — the count is the correctness fix, the
  stagger is the design one.

- **Feedback has to exist wherever the health bar does not move.** Four presses out of every five at
  the slime take no health off it — they work a glob loose — so without the pip row under the body a
  correct run of presses looks like a run of presses that did nothing. Any mechanic with hidden
  progress needs somewhere to show it.

- **A test that measures one thing has to defend the others.** `settleSlime` compares the boss's
  health with and without a drag. Adding a volley to the split made both runs end in death, so both
  returned zero and the comparison passed for the wrong reason — it was measuring survival, not
  health. It tops lives up every frame now. Any A/B on a long run wants the same treatment.

- **Repeated presses on one boss projectile do not require repeated recognition.** The test bot
  should keep its press-rate limit but only pay the reaction delay when choosing a new target.
  Prioritize bolts by arrival time, not glyph index; otherwise a visual change that shifts the
  seeded run can look like a pacing regression caused by the boss.
