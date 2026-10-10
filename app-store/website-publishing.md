# Public pages

Website: https://koniho.github.io/dddumpling-game/
Privacy: https://koniho.github.io/dddumpling-privacy/

Edit docs/site/ for the website, or docs/privacy.html for the policy. Push changes
to main. The Publish public pages workflow publishes both sites using separate,
repository-scoped deploy keys. GitHub Pages then serves the updated main branches
of koniho/dddumpling-game and koniho/dddumpling-privacy. No game source is copied.
The workflow can also be run manually from main.

Secrets in the private game repository:
- WEBSITE_DEPLOY_KEY: write access only to the public website repository.
- PRIVACY_DEPLOY_KEY: write access only to the public privacy repository.

To rotate a key, replace its public deploy key in the destination repository and
the corresponding Actions secret in the private source repository. Private keys
are not checked into any repository.

The static website uses no scripts, analytics, cookies, or external fonts.
Screenshots come from the shared game rendering harness. Store buttons link to the
public Google Play, App Store and itch listings. The trailer opens on YouTube.

The Survival, Time Attack and Sunny Duck screenshots come from the approved 0.1.26
render output: `149-survival-150`, `150-combat` and `156-ducks-reveal-0`. They are
copied unchanged; the webpage crops the duck frame with CSS.

**Website checks** renders four responsive widths with Chromium, checks image loading,
navigation and overflow, and runs axe accessibility checks. Download its
`website-preview` artifact to review the hero, collection section and full pages
before merging visual changes. Reduced-motion preferences disable all decorative animation.
