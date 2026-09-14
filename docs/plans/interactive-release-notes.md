# Interactive release notes MVP

Review branch: `feature/interactive-release-notes`.

The kawaii steamer opens a compact release list for the latest three releases (v0.1.20, v0.1.19 and v0.1.18 for this release). Each release has left-aligned change icons that wrap to additional rows when needed; all its fixes share one cute bug icon. Only the icons open feature pages; versions and surrounding space are not links. No feature labels on the list. Vertical swipes scroll when the content exceeds the available height, without selecting an icon.

Each icon opens its own content-sized popup, with a playful title, the phase of the game where it applies, and a brief explanation of the player benefit. Land travel, the mystery pickup, and linked pairs retain their isolated interactive demos. Smaller highlights have compact illustrations. The back arrow preserves list scroll. Feature pages have no Replay or All releases footer; each entry explicitly chooses `autoReset` at authoring time. The current shuffle and linked-pair demos reset after two seconds, ready to try again. Land travel keeps the selected land, and smaller illustrations do not automatically restart. Reopening a feature also starts it fresh. The title scene continues animating behind lightly translucent panels; gameplay input remains blocked. Demos do not change live gameplay, RNG, or progress.

The normal title screen stays usable when a new build arrives. Its corner steamer draws attention with a turning, pulsing star behind it, a repeatedly popping lid, and extra rising steam. This continues across title visits until the player opens the release notes; only that tap persists the generated build ID as read. Outside taps keep their normal title behavior. RESET NEWS in developer settings restores the attention animation.

Review spacing, icon clarity, popup fit, and the three interactive demos. Reviewed through the in-game iterations; merge and release authorized on 2026-09-14.

Entrance gathers the steamer at screen centre, then sends it left as the list arrives from the right. Exit takes half the entrance duration. The list slides off to the right while the steamer slides in from the left at its normal corner size and height. Moving panels ignore taps; Back can reverse an entrance without a jump.

The steamer has no arms. Its lid lifts on the opening tap, and idle wisps rise and fade continuously. Selecting a change slides the list left and its feature in from the right; Back reverses this while preserving list scroll. Feature input waits until the slide settles.

Wrapped rows expand the release group and move later releases down. The list scroll range includes every wrapped row, and returning from a feature preserves the scroll position.

Author notes in `release-notes/releases.json` using `tools/release-notes.py`; see [the writing guide](../release-notes.md). Finish in-game and destination notes before tagging, following [Executing a release](../releasing.md).
