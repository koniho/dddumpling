# Audio and narration

Historical failure notes, moved from `AGENTS.md`. Read only entries relevant to the task.
Values and file ownership may have changed; verify against current code. Current workflow
policy lives in [AGENTS.md](../../AGENTS.md).

- **A speech engine intones a whole utterance, so chopping text up destroys prosody.** The first
  narration cut each story into sentences and gave every one its own pitch, expecting an arc. What
  it produced was flat fragments at arbitrary heights — the engine could not see a sentence, so it
  had nothing to intone, and the stepping between them read as lurching. The story now goes over as
  one utterance with its punctuation intact and a single pitch. The levers that genuinely reach
  prosody are the *voice* (quality varies enormously; `getVoices()` and pick) and the punctuation
  you hand over. Note also that the engine's volume parameter is a fraction of the stream, not a
  gain — the only way to make the voice louder is `Audio.duck()` holding the music down, released by
  an `UtteranceProgressListener` on the last queued utterance since there is no queue-drained
  callback.

- **A sound that repeats has to be short and has to decay.** The blade chops several times a
  swipe and the TEAM squishy squishes every couple of seconds; anything with a tail smears into a
  wash, and the achievement fanfare was doing exactly that. There are assertions on the chop's
  length, on its tail being a quarter of its head, and on its zero-crossing rate — that last one
  is a cheap stand-in for "has a tone under it rather than being a hiss", and it is the one that
  caught the first attempt at giving it body being no better than the original. The shelving chime
  is held to the same three, plus one more: it must be shorter than the gap between two landings,
  since a haul shelves several a tenth of a second apart.

- **Several things landing at once is one event, however many things there are.** The haul's flight
  home staggered its departures and then had every flyer converge on the same instant, which looked
  deliberate and was — until it needed a sound per landing, at which point a haul of four played one
  chord. Arrivals are staggered too now. If you are about to attach a sound to the end of an
  animation, check that the ends are actually distinct before writing the sound.

- **A scene with no sound reads as a scene the game stopped caring about.** Four moments used to
  pass in silence: a course leaving the line, the count at the end of one nobody won, the new
  collectible joining the parade, and the end of a run. Each is a *transition*, which is exactly
  where sound is easiest to forget and most missed. Two things to know if you add more. The moment
  matters as much as the effect — the run's full stop plays when the swirl clears, not on the fatal
  breach, because that frame already has the damage drip on it and two effects on one frame is one
  of them wasted. And a phase change has to be caught as a change: `StarPath.launched` and
  `reported` are set by comparing the phase predicates either side of the frame's own countdown,
  since nothing downstream can tell that the lesson has *just* ended.
