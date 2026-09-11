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
Screenshots come from the shared game rendering harness. Replace the playtest
preparation message with the real Play Store link when the listing is accessible.
