# iOS CI and signed delivery

For current PR selection, focused runs, caching, and required checks, see [test selection](test-selection.md).

Verified on GitHub Actions: [run 34715180724](https://github.com/koniho/dddumpling/actions/runs/34715180724)
at code commit `a4cc6e6` passed 4,864 shared Java assertions, 80 production checks,
56 input assertions, all 13 native/UI tests, and the unsigned arm64 Release archive.
The cold macOS job took 13m43s; the fastlane configuration job took 18s. Artifacts
`ios-test-results` (about 5 MiB) and `ios-unsigned-archive` (about 12 MiB) were uploaded.
The J2ObjC cache is about 627 MiB. Signed export and TestFlight upload passed in
[release run 34732005221](https://github.com/koniho/dddumpling/actions/runs/34732005221)
for version 0.1.0, build 2.1. Apple acknowledged the upload; build processing and tester
availability are checked separately in App Store Connect.

An earlier run was cancelled after duplicate J2ObjC class warnings appeared. The test
bundle had inherited the app's static-runtime linker flags; those now belong only to
the app target. The corrected local and hosted suites pass.

GitHub Actions uses `macos-26` and explicitly selects Xcode 26.6. Java 21, Maven and
XcodeGen build the pinned J2ObjC 3.1 runtime; a cache keyed by runner architecture,
Xcode build and bootstrap script avoids rebuilding it cold. Runtime compilation is
limited to three workers to fit the standard runner. See GitHub's
[runner inventory](https://github.com/actions/runner-images/blob/main/images/macos/macos-26-arm64-Readme.md).
The completed runtime is cached before app tests begin, so a later test failure can
reuse it. Simulator test targets run serially to limit memory use on the hosted runner.

## Checks without Apple credentials

`Check iOS port` runs on relevant PR changes and pushes to `main`, or manually:

```sh
gh workflow run ios.yml --ref main
```

It runs shared Java/production/input checks, native and UI tests, and an unsigned
arm64 device Release archive. Artifacts include `.xcresult` test evidence and
`DDDumpling-unsigned.xcarchive.zip`, retained for seven days. The unsigned archive
cannot be installed directly on an iPhone. A separate Linux job validates Android
and iOS fastlane configuration without using credentials or uploading anything.

New runs cancel superseded checks for the same PR/ref. No Apple secrets are supplied
to this workflow. Do not make the path-filtered workflow a required check for unrelated
PRs without adding a corresponding always-running status job.

## One-time signing setup

Use an activated Apple Developer membership and register the production bundle ID
`com.dddumpling.game.ios`. Create its App Store Connect app record before uploading.
The existing `Gemfile`/`Gemfile.lock` pin fastlane 2.239.0 and Bundler 4.0.16 for both
Android and iOS; CI selects Ruby 3.3.

On a trusted Mac, initialize **match** with an App Store distribution certificate,
its private key and the matching provisioning profile in a **private Git repository**.
Keep this separate from the public game repository. Match encrypts its contents with
the password you choose. CI uses it read-only and fails if valid assets are missing;
it never creates or repairs certificates/profiles. Follow the
[match setup instructions](https://docs.fastlane.tools/actions/match/).

Create a dedicated SSH key for CI. Add its public key to the match repository as a
deploy key with write access disabled, and store the complete private-key contents in
the `IOS_MATCH_GIT_PRIVATE_KEY` environment secret. Do not reuse a personal SSH key.

Create an App Store Connect team API key with sufficient app-upload and signing-resource
access (App Manager or Admin, with the needed account permissions). Store the downloaded
`.p8` key as base64. Fastlane recommends API-key authentication for CI; signing still
requires the separate certificate/private key/profile. See
[authentication](https://docs.fastlane.tools/getting-started/ios/authentication/).

Configure the GitHub environment **`ios-release`** (Settings → Environments).
GitHub rejected required-reviewer protection with HTTP 422 because the current
repository billing plan does not support it. The workflow instead requires manual
dispatch from `main`; upload requires its separate explicit input. Enable required
reviewers later if the plan supports them.
The environment's deployment branch policy is configured to allow `main` only.

| Kind | Name | Value |
| --- | --- | --- |
| Variable | `IOS_TEAM_ID` | Apple's ten-character team ID |
| Variable | `IOS_MATCH_GIT_URL` | SSH URL of the private match repository, such as `git@github.com:owner/signing.git` |
| Secret | `IOS_MATCH_GIT_PRIVATE_KEY` | PEM contents of an SSH deploy key with read-only access to the match repository |
| Secret | `IOS_MATCH_PASSWORD` | Match repository encryption password |
| Secret | `IOS_APPSTORE_KEY_ID` | App Store Connect API key ID |
| Secret | `IOS_APPSTORE_ISSUER_ID` | Team API issuer ID |
| Secret | `IOS_APPSTORE_KEY_BASE64` | Base64-encoded contents of the `.p8` file |

Enter secrets in GitHub Settings or supply them to `gh secret set` through stdin; do
not paste them into workflow files, command arguments, logs, or this repository.
The ephemeral signing keychain is created by `setup_ci` and removed in an always-run
cleanup step. Its empty password follows fastlane's standard isolated-runner setup.

## Build and upload

`Build iOS release` runs **only by manual dispatch from `main`**.

```sh
# Build/export only; does not upload.
gh workflow run ios-release.yml --ref main -f upload_to_testflight=false

# Explicitly upload the new signed build to TestFlight.
gh workflow run ios-release.yml --ref main -f upload_to_testflight=true
```

The archive lane translates Java in Release, regenerates Xcode's project, applies
manual App Store signing only to the app's Release configuration, and exports
`ios/build/DDDumpling.ipa`. The workflow retains the IPA and dSYM ZIP for seven days.
It rechecks shared gameplay and production gating before building.

Build numbers are `run_number.run_attempt` (for example, `12.1`); retries therefore
produce a different number. Keep this workflow as the source of uploaded build numbers
for the current marketing version, or coordinate a new version before manually uploading
a higher number. The validator accepts at most three numeric components, first 1–9999
and subsequent components 0–99.

The separate `ios upload_testflight` lane requires the already-built IPA and API key.
It does not build again, distribute to external tester groups, change listing metadata,
or submit for App Store review. It returns after upload rather than spending runner
minutes waiting for Apple processing. Check App Store Connect for processing/compliance
status; internal-group automatic distribution settings still apply there.

Local configuration checks need no Apple account:

```sh
bundle install
bundle exec ruby tools/test-fastlane.rb
bundle exec ruby tools/test-ios-fastlane.rb
bundle exec fastlane lanes
```

Signing and TestFlight delivery cannot be end-to-end validated until the environment
has real credentials and initialized match assets. No TestFlight upload is part of
the unsigned workflow.
