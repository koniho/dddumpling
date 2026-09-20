# Audio and local persistence

`DDIOSAudio` is the iOS implementation of `GameCore.Sound`. The game continues to
synthesise every PCM buffer in translated `Sfx` and `Music`; AVFoundation only mixes
those buffers. Short effects cache float PCM and use `DDEffectMixer`: one persistent engine
with twelve reusable player nodes and varispeed units, operated on a serial effects queue.
Finishing an effect leaves the graph alive; a saturated voice is interrupted for reuse.
Music and the boss-charge bed retain WAV playback, while the rocket has its own engine.
Music is rebuilt only when the synthesized style or audible arrangement changes.
Repeated scene selections preserve the player and its position, including paused playback
and pending renders. Band playback invalidates that selection so normal music can return.
Boss PCM is balanced to 115% of the matching synthesized stage track's RMS level,
with soft-limited peaks at 38% of full scale to leave room for effects. Both platforms
use the shared `Music.BOSS_GAIN`; player mute and temporary ducking still apply.
The host sets `setActive:` after it has selected the scene soundtrack. The adapter
also pauses for app backgrounding, interruptions, and unplugged output routes, and only
resumes an interruption when iOS says that resumption is appropriate. Narration uses the
system English voice at the Java-defined pitch and timing and ducks music while speaking.

`DDIOSStore` stores all `GameCore.Store` fields in one binary plist beneath Application
Support. The values plist is enclosed with a format marker and SHA-256 checksum; it is
written with `NSDataWritingAtomic`, so an interrupted write leaves the previous complete
file in place. Invalid, truncated, or altered envelopes put the store in read-only mode;
the original bytes are never overwritten and `error` gives the host a player-facing reason.
The host should present that error once after creating the store, then allow the game to run
without further local writes rather than silently replacing a recoverable save.
The progress replica identifier is generated once and saved with the same envelope. The save
is local-only and contains no account identity or network data.
