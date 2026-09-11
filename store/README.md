# Play Games integration workspace

Current store listing assets and release automation are maintained in
[app-store/](../app-store/README.md) and [Play publishing](../app-store/play-publishing.md).
The package is `com.dddumpling.game`. Version code 10 has already been uploaded; new uploads
must use a new version code. The default local build is developer mode; bundles always select
production. Current release signing comes from the existing GitHub Actions signing secrets.

For optional in-game events and cross-device saves, see [play-games.md](play-games.md) and
`play-games.example.json`. This requires separate Play Games Services configuration; fastlane's
publishing service account does not configure the in-game SDK.

The listing, preview HTML and privacy-policy draft in this directory are historical preparation
material. Use app-store/ and docs/privacy.html for current published copy. Update the published
privacy policy and Data safety answers before enabling network reporting in a distributed build.
