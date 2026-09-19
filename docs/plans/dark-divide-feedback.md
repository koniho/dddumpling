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
