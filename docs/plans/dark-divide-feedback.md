# Dark Divide feedback

Tracking issue: [#77](https://github.com/koniho/dddumpling/issues/77).
Integration branch: `feature/dark-divide-visuals`, based on `main`.
Target this branch for follow-up Dark Divide changes.

Successful character-key hits give a short rounded bloop. Disabling a terminal cube gives a
softer falling bubble-pop. Firing jiggles the cube and nudges its soft body opposite the volley;
the spring returns it home without changing roaming speed.

Before any split, fire three staggered projectiles every 1.5 seconds (50% below the original
three-second interval). After the first split, each cube fires two every 2.1 seconds (30% below
that original interval). Later splits retain the existing single-projectile firing ramp.
Successful hits reset the struck cube's timer. Warning heat follows the current interval;
volleys wait for enough free projectile slots to launch in full.

`TestBoss` covers recoil direction/magnitude, jiggle, timers, volley sizes, stagger and capacity.
`TestAudio` checks short, tonal, decaying effects. Preview frames `75a`, `75b`, and `75c` show
unsplit firing, split-cube recoil, and the three-projectile volley. Run the bounded-player soak
for pacing changes, and device-playtest the final sound and movement feel.

Projectile interceptions also use the rounded bloop, avoiding the generic hard shell snap.
The final death sequence gathers the cubes for 0.9 seconds, shakes them with increasing intensity
for one second, then bursts into 360 tumbling cubes in six purple shades. Debris travels radially
without gravity; the existing 3.6-second defeat duration stays unchanged. `DivideDeath` owns
the deterministic pose and renderer, previewed across seven beats in `75d-divide-supernova-*`.

The doubled 360-cube burst triggers one intense 1.4-second layered bleep-and-bloop effect
at ignition on Android and iOS. Pausing freezes the trigger, and debris frames cannot replay it.

All four boss deaths now share screen shake and haptics: a heavy opening, three light
animation beats, and a heavy climax at the collapse or Dark Divide supernova. Shared per-frame
cues pause with play and clear before early returns; native adapters dispatch light/heavy
feedback without replaying it in settings, the background, or after the fight.

Screen shake includes the background clouds and land scenery. The backing fills extend
past the viewport to cover displaced edges; the HUD and modal panels remain steady.
