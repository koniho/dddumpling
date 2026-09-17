# Cave minigames

Branch: `cave-band-minigame`.

## Cave Band

Cave expedition interludes alternate Cave Band and Dumpling Mine. Other lands retain Steamer and Star Path,
and cave visits do not alter their alternation or partial progress. The existing developer-only
cave gate remains in effect.

A dumpling plays lead guitar, with drums, bass and keys behind it. A single character-note lane
scrolls right to left across a gold strike line. Each note asks for one existing character key;
only keys in the current roster appear. Correct presses add the live guitar part. A wrong key
inside a note's window consumes that note, preventing all-key mashing from guaranteeing rewards.

Four count-in beats precede eight bars. The three original arrangements rotate by cave stage:

| Song | Arrangement | BPM, bars 1–3 / 4–6 / 7–8 |
| --- | --- | --- |
| Crystal Crunch | Heavy power-chord rock | 90 / 110 / 130 |
| Tunnel Trouble | Eighth-note punk rhythm guitar | 110 / 135 / 160 |
| Bat Outta Bedrock | Fast melodic rock with active keys | 125 / 150 / 175 |

Drum fills announce tempo changes. Hanging crystals extinguish one per completed bar; a second
row fills with successful notes. Forty hits earn a random collectible and a capped extra life.
Charge carries between attempts within the run; each attempt pays at most once and keeps excess
charge. Songs cannot be completed for a free reward by doing nothing. Failure costs no life.
A brief report and, when earned, the existing collection parade precede the next expedition.
The complete performances last approximately 16–22 seconds including the count-in.

## Timing contract

`CaveSong.at` maps musical beats to seconds. PCM events, visual note positions and hit deadlines
all use that function. The judge uses ±140 ms (±238 ms in Kids Mode), independent of tempo,
frame rate, game speed and slow motion. Device touch-event age is subtracted before judging;
a delayed valid touch can recover a note expired by a frame stall, but never a previously hit
or wrong-key-consumed note.

Android uses the finite AudioTrack's presentation timestamp, extrapolated to now, or its
playback head when timestamps are unavailable. Stale pre-pause timestamps are rejected after
resume. iOS uses the finite AVAudioPlayer's currentTime with reported output latency subtracted.
Preparation returns a waiting state; a headless run or failed audio backend uses unscaled elapsed
time. Muting leaves the transport running silently. Pausing, settings and app backgrounding
freeze audio and gameplay; leaving the interlude cancels pending preparation.

Platform references: [Android AudioTrack](https://developer.android.com/reference/android/media/AudioTrack),
[Apple currentTime](https://developer.apple.com/documentation/avfaudio/avaudioplayer/currenttime),
[Apple outputLatency](https://developer.apple.com/documentation/avfaudio/avaudiosession/outputlatency).
Speaker and Bluetooth timing still need real-device listening checks; reported hardware latency
is not a guarantee for every route. A user calibration offset can be added if device testing calls
for one. No independent gameplay timer is used to chase the audio.

## Verification

`TestCaveBand` exercises tempo boundaries, PCM duration/headroom, hit windows, repeated/wrong
presses, idle completion, carry-over, single reward payment, audio loading, frame stalls, delayed
touch input, pause/settings, mute and exit. Native input contract tests cover the iOS bridge.
Preview family `108-band-*` covers all songs, count-in, play, acceleration, win and retry.
`out/sfx/cave-band-0.wav` through `cave-band-2.wav` are complete listening exports with guitar.
The regular full rule/soak suite and production build remain required.

## Second game: Dumpling Mine

Implemented in place of the earlier Underground Stream proposal. Band plays after cave stage 21,
mining after stage 22, then they alternate. Ordinary-land routing remains unchanged.

- Cart sequence lengths are 2, 3, 4, 4, 4. A cart uses one fixed sequence of distinct active keys.
- Every complete sequence drops a visible load of rocks. Five loads fill the cart.
- Wrong input restarts only the current sequence; it cannot erase completed loads or take a life.
- The full cart hides all sequence prompts and disables mining keys. A horizontal drag starting
  on the cart commits a dispatch after 16% of the screen width. Either direction works; vertical
  drags, keys, extra fingers and small movements cannot accidentally dispatch it.
- Four dumpling helpers run in and push the committed cart offscreen over 1.4 seconds.
- A delivered cart is saved at swipe commitment, before animation, so quitting during the push
  does not lose it. A saved fifth cart recovers its pending reward on the next mining visit.
- Five delivered carts pay one collectible plus a capped extra life, then clear saved progress.
  Incomplete sequences and loose cart loads last only for the current attempt.
- An 18-second lantern clock starts after a short ready beat. Flame, warm light radius and scene
  brightness fade together. Keys stay legible as the cave darkens. Pause/settings stop the clock;
  the automatic helpers' animation also stops it. Kids Mode retains its ordinary slower clock.
- Timeout gives a short report and resumes the expedition without a life penalty. Saved carts
  remain. Success uses the existing collection parade before the next expedition.

`CaveInterlude` owns selection and reward/exit routing. `CaveMining` owns the small state machine,
`CaveMiningInput` owns the cart pointer, and `CaveMiningScreen` owns all rendering. The Store seam
adds `loadMineCarts` / `saveMineCarts`, implemented on Android and iOS.

`TestCaveMining` covers sequence lengths, five repetitions, full-cart lockout, pointer ownership,
checkpoint durability, fifth-cart recovery, one-time rewards, timeouts, dimming, pause and routing.
Bounded miners at 3/5/8 presses per second check that the reward remains reachable. Preview family
`109-mine-*` covers bright/dim lighting, falling rocks, full cart, helpers, all sequence lengths,
timeout, reward and parade. The iOS input harness covers the actual native gesture route; its
native store test includes a cart-progress round trip.

Android static audio must receive the PCM before checking for `STATE_INITIALIZED`; a newly
created static track reports `STATE_NO_STATIC_DATA`. `tools/test-band-audio.py` exercises the
real adapter against this lifecycle, including playback-clock timing across tempo changes,
mute, pause, cleanup, and a rejected PCM write. The headless check runs it automatically.

Developer Minigames includes Cave Band and Dumpling Mine launch chips for active runs; they
enter the real stage 21/22 interludes and preserve accumulated progress.

Cave rewards now have exclusive five-member families: Burrow Moles for mining and Cave Snakes
for band performances. IDs 49–58 append to the existing catalogue, keeping saved collections
compatible. Each friend has a dedicated drawing, family story and vignette; both families have
their own case row. Normal blind boxes, Star Path, cubes and boss prizes keep their pools.
