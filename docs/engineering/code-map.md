# Code map

Starting points, not a required reading list. Use `rg` to confirm current ownership.

Pure (in the harness and the APK):

| File | Holds |
| --- | --- |
| `Glyph` | the six-letter palette, hexagon geometry, hue cycling |
| `Kawaii` | the six characters and their faces, plus the mood dumpling |
| `Layout` | every screen coordinate, derived from view size + insets |
| `GameCore` | the spine: state machine, the frame loop, the press router, and the state everything else works on |
| `Pacing` | the stage difficulty dials. Pure functions of stage — the one file to read when tuning |
| `Blade` | the FLING swipe: what a stroke is, when it ends, what one sweep cuts |
| `CaseUi` | browsing the display case: scroll, jump, drag, two-tap wipe |
| `Interlude` | the between-stages round: mash, course, blind box, parade |
| `BossPlay` | the boss fight's wiring — a press or drag turned into score, sound, shots and lives |
| `Words` | word generation and the press-budget rules |
| `Fx` | shots and particles |
| `Steamer` | between-stages minigame state |
| `StarPath` | the star course: how one is generated, flown, and won |
| `StarScreen` | the star course on screen, including its victory tableau |
| `Collect` | the thirty collectibles: catalogue, blind-box odds, owned-set bitmask |
| `Trinket` | draws a collectible — fifteen shapes crossed with nine finishes |
| `Cabinet` | the glass case itself: a wireframe box three-quarters on |
| `Showcase` | the display case: badge, shelf, position bar, and every touch target on it |
| `Lore` | a story per collectible, plus who is cast in its vignette |
| `Parade` | the collection marching in, the new one joining, the line marching off |
| `Storybook` | the story popup and its ten looping vignettes |
| `Power` | the powerup letter and its three modes |
| `Boss` | the every-fifth-stage boss: five mechanics, its elements, and what a press/tap/drag does |
| `BossScreen` | the boss on screen: body, health header, ornaments and its elements |
| `Softbody` | a pressurised 2D soft body — the ring of sprung nodes every boss is built on. Rests as a circle or an ellipse, with per-body springiness |
| `Slime` | draws a soft body as gooey translucent slime with a face |
| `Painter` | the drawing interface |
| `Draw` | palette + shared geometry (pill, star, hash, rainbow) — renderers extend it |
| `Sky` | background, clouds, vignette, HUD band |
| `Renderer` | frame orchestration + the play field |
| `Hud` | score/stage/lives, frenzy bar, banners |
| `Screens` | title, game over, minigame, settings |
| `RoundEnd` | how a run ends: the swirl, the haul dancing, and its flight to the case |
| `Demo` | the title screen playing itself, in place of two lines explaining how |
| `Sfx` / `Music` | procedurally synthesised effects and looping tracks |
| `Narration` | what the story popup says out loud, and the pitch and pace of each line |
| `SettingsUi` | settings panel geometry and hit-testing |

Android-only: `MainActivity`, `GameView` (input + frame loop), `CanvasPainter`, `Audio`,
`Crash`.

Harness-only in `tools/`: `Check` (base class with the tally, assertion helpers, drivers and
the Store/Sound stubs), `Test*` suites, `Bot` (a player with stated limits), `Preview`,
`RasterPainter`, `Font`, `Png`, `Wav`.

The `Test*` classes extend `Check` and the renderers extend `Draw` **so that helpers and
colours resolve unqualified**. That is deliberate: prefixing several hundred call sites buys
nothing. Follow the pattern rather than "fixing" it.
