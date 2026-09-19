# Cave minigames

Branch: `cave-band-minigame`.

## Cave Band

Cave expedition interludes alternate Cave Band and Cart Rush. Other lands retain Steamer and Star Path,
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

## Second game: Cart Rush

Replaces the typing-and-loading mine with a full-screen perspective minecart ride. Hold/drag either
side of the field or tap the matching half of the keyboard to lean into bends. The rails preview
upcoming turns; the crew lean, slide, panic, and fly out when balance reaches its limit. Tunnel ribs,
crystals, sleepers, wheel motion, speed streaks, sparks, and camera rumble convey speed.

The track has 20 sections, each lasting 0.95 seconds. Each visit lasts at most ten seconds, after a
1.4-second ready beat. Completed sections persist immediately; partial sections restart on the next
attempt. Success awards a mole collectible and resets track progress. Existing saved mining carts
migrate to four sections each. Pausing/backgrounding releases steering and freezes the ride.

Audio uses original synthesized low cave rumble and phone-audible body harmonics. The cart has
short rail clacks, wheel scrape, and a tumble impact, alongside the deeper cave hazard effects.
Existing native effect and haptic bridges play these on Android and iOS.

`TestCaveMining` covers persistence, migration, rewards, idle failure, bounded riders, and pointer
ownership. Preview group 109 shows ready, both turn directions, danger, spill, resume, and reward.
Native input tests cover field steering and background release. Cave Band remains the other cave game.
