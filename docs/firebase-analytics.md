# Optional Firebase Analytics setup and privacy checklist

This document prepares the optional, consent-based Google Analytics for Firebase release. It is
not evidence that a Firebase project, Analytics property, retention setting, store declaration,
or release has been configured. Complete and record the account-side checks before distributing
an analytics-enabled build.

Analytics is configuration-gated. If the platform-specific Firebase configuration is absent, the
Android build uses the no-op analytics bridge and the iOS build does not package Firebase project
configuration. Neither platform initializes Firebase or collects Analytics data in that state.
Android Play Games configuration is separate: a Play Games-enabled Android build may continue to
use authentication and Saved Games, but the new configuration does not send gameplay counters to
the Play Games Events service. Firebase is the only destination for the optional gameplay events.

## Create the project and register both apps

1. In the [Firebase console](https://console.firebase.google.com/), create or select one project
   owned by the DDDUMPLING publisher. Use the no-cost Spark plan and enable Google Analytics for
   the project. Analytics is listed as a no-cost Firebase product; do not enable paid services.
2. Associate one Google Analytics property with the Firebase project. During setup, leave Google
   Signals and advertising features off. Do not link Google Ads or another advertising product.
3. Register the Android app in that project with the exact package name
   `com.dddumpling.game`. Download its `google-services.json`.
4. Register the Apple app in the same project with the exact, case-sensitive bundle ID
   `com.dddumpling.game.ios`. Download its `GoogleService-Info.plist`. The App Store ID is optional
   for this Analytics-only integration and must not be used to turn on an Ads link.
5. Keep the downloaded files locally at:

   ```text
   .private/firebase/google-services.json
   .private/firebase/GoogleService-Info.plist
   ```

   Firebase describes these files as project/app identifiers rather than secrets, but this
   repository deliberately keeps environment-specific configuration out of source control. Never
   substitute a configuration registered for a different package or bundle ID.

Production builds look for those paths by default. The environment variables override the default
locations when a configuration is stored elsewhere:

```sh
DDDUMPLING_FIREBASE_CONFIG=.private/firebase/google-services.json ./build.sh --production
IOS_FIREBASE_CONFIG=.private/firebase/GoogleService-Info.plist ./ios/scripts/archive.sh --unsigned
```

Use the same overrides in the signed Android bundle and iOS archive workflows when needed. A
production build with no file at the default path and no override collects no Firebase Analytics;
developer builds also keep Analytics unavailable. Firebase configuration is independent of
`DDDUMPLING_PLAY_CONFIG`; supplying the Play Games configuration must not implicitly enable
Firebase, or vice versa.

For a signed iOS release in GitHub Actions, create the repository secret
`IOS_FIREBASE_CONFIG_BASE64` containing the base64 encoding of the complete Apple plist. The
release workflow decodes it to the default `.private/firebase/GoogleService-Info.plist` path before
archiving. If the secret is absent, the workflow builds without Firebase project configuration and
Analytics remains unavailable. Do not print the decoded file in a workflow log or store it in an
artifact. `IOS_FIREBASE_CONFIG_BASE64` is a CI transport for the configuration file;
`IOS_FIREBASE_CONFIG` remains the local path override.

Official setup references: [Firebase pricing](https://firebase.google.com/pricing),
[Android registration and configuration](https://firebase.google.com/docs/android/setup),
[the Android configuration file](https://firebase.google.com/docs/android/google-services-plugin-and-file),
and [Apple registration and configuration](https://firebase.google.com/docs/ios/setup).

## Consent and SDK constraints

These requirements apply worldwide and must be verified in the final release artifacts:

- Before the player has chosen, show ALLOW and NO THANKS with equal access. Do not initialize a
  Firebase app, obtain an Analytics instance, log an event, or let an automatic initializer run.
- NO THANKS leaves Firebase uninitialized. Do not queue declined gameplay events for later upload.
- ALLOW initializes Firebase and enables collection. Persist the choice locally.
- The existing title-screen privacy control must reopen the analytics choice and policy. Revoking
  consent disables future collection and invokes the SDK's local reset. The reset clears local
  Analytics state and rotates the app-instance ID; it does not delete server history.
- Android must disable Analytics collection by default, Advertising ID collection, and default ad
  personalization in the merged manifest. Confirm no initializer can run before the consent gate.
- Apple must disable collection by default, use the `FirebaseAnalyticsCore` product without IDFA
  capability, set IDFV collection off, and keep AdSupport out of the linked app. Disable default ad
  personalization in the final `Info.plist` and confirm Firebase configuration occurs only after
  ALLOW. The repository currently pins Firebase Apple SDK `12.19.0`; re-audit these controls and
  its transitive privacy manifests whenever that version changes.
- Do not call `setUserID`/`setUserId`, set account identifiers as user properties, or send Play
  Games player IDs, save replica IDs, scores, character IDs, typed keys/words, or free text as
  Analytics parameters.
- Include Analytics only. Do not add Crashlytics, Performance Monitoring, Authentication,
  Messaging, Remote Config, Ads, or another Firebase product without a new code and disclosure
  review.

Google documents the collection switches and identifier controls separately for
[Android](https://firebase.google.com/docs/analytics/android/configure-data-collection) and
[Apple platforms](https://firebase.google.com/docs/analytics/ios/configure-data-collection).
Google's [Android Analytics reference](https://firebase.google.com/docs/reference/android/com/google/firebase/analytics/FirebaseAnalytics)
says `resetAnalyticsData()` clears on-device Analytics data and resets the app-instance ID.

## What is collected after ALLOW

DDDUMPLING sends a fixed event name and one integer parameter, `amount`, for each observation. The
amount is the counter increment or elapsed milliseconds and is never an identifier. Events already
stored in local progress are not replayed when Analytics is first allowed, after a cloud restore,
or after a reinstall.

| Area | Events and interpretation |
| --- | --- |
| Runs | Started, finished, explicitly abandoned, and stage-range at game over |
| Stages | Stage-range entered and stages completed |
| Bosses | Started, won, failed, abandoned, no-damage endings, first-hit count, first-hit millisecond sum, and one first-hit time bucket |
| Minigames | Starpath and steamer started, won, failed, or abandoned |
| Rewards | Total, new/duplicate, and source (boss, Starpath, or steamer) |

Most event amounts are `1`. The explicit `amount` must still be used for totals because it preserves
counter increments and boss timing sums if emission is later batched. In reports, sum `amount`
rather than assuming the Google Analytics event count is always the gameplay count. For a boss,
average seconds to first damage is:

```text
sum(amount for boss_<name>_first_hit_ms_total)
------------------------------------------------ / 1000
sum(amount for boss_<name>_first_hit_count)
```

Keep the matching no-damage and time-bucket totals beside that average so it does not represent
only successful attempts.

In Analytics Admin → Data display → Custom definitions, create one event-scoped custom metric:

- Metric name: `DDDUMPLING amount`
- Event parameter: `amount`
- Unit of measurement: `Standard`

One property-wide definition covers every custom event. Google says new custom metrics can take
24–48 hours to become available in reports. Do not create a user-scoped definition or a separate
definition for each event. See [Create custom metrics](https://support.google.com/analytics/answer/14239619).

After collection is enabled, the SDK also automatically assigns an app-instance ID, records app
lifecycle/screen/session events, includes app and device metadata, and derives coarse geography
from a masked IP address. Google says GA4 does not log or store the IP address itself. Disabling
Ad ID, IDFA, and IDFV does not disable the app-instance ID. The current automatic collection list
is in [Google's Firebase Analytics disclosure](https://support.google.com/analytics/answer/11582702),
with default events in [Automatically collected events](https://support.google.com/analytics/answer/9234069)
and default device/geography dimensions in
[Predefined user dimensions](https://support.google.com/analytics/answer/9268042).

## Owner-side privacy settings

Before the first analytics-enabled tester or release receives the app, the property owner must
review the live Firebase and Google Analytics settings and record the result. Console wording can
change; verify behavior rather than relying only on this checklist.

- Keep Google Signals off. Do not collect signed-in Google account identifiers for Analytics.
- Keep Google Ads and all other advertising/product links absent. Do not export audiences or key
  events to advertising services.
- Turn off every optional Google Analytics account data-sharing setting, especially Google products
  and services. In Firebase privacy settings, also turn off optional use of Firebase service data
  by non-Firebase Google services.
- Disable ads personalization for the property in addition to the app-level defaults. Do not
  create audiences, demographics/interest reports, User-ID reporting, or cross-platform joins.
- In Admin → Data settings → Data retention, select `2 months` for user and event data, the shortest
  option currently documented for a standard GA4 property. If the console shows “Reset user data
  on new activity,” turn it off so new activity does not extend user-level retention. This PR does
  not configure or prove that setting.
- Do not enable BigQuery export. Restrict property access to people who need aggregate product
  measurement or must handle a privacy/deletion request. Review access periodically.

Google states that the retention control applies to user-level and event-level data, while standard
aggregated reports are unaffected. Dashboard totals may therefore outlive the two-month detail
window. Use aggregate event totals and date ranges for balance decisions; avoid User Explorer and
per-instance analysis except when needed to investigate a deletion/privacy request. Low-volume
events may reveal a single installation even in an aggregate table, so do not publish or share
small-cell reports externally.

References: [Analytics retention](https://support.google.com/analytics/answer/7667196),
[data-sharing settings](https://support.google.com/analytics/answer/1011397),
[Google Signals](https://support.google.com/analytics/answer/9445345), and
[Firebase service-data privacy settings](https://firebase.google.com/support/privacy).

## Revocation and deletion limits

Revocation must immediately stop collection on that device and reset the SDK's local Analytics
state. It cannot retract uploads already received by Google and must not be described as deleting
server history. Previously uploaded user/event detail ages out according to the configured
retention setting; standard aggregate reports are not covered by that setting.

Google offers User Explorer and a User Deletion API for a record that the property owner can
identify. DDDUMPLING intentionally does not set a User-ID, associate Analytics with an account, or
display the SDK's app-instance ID. A player who contacts support will therefore usually not have
an identifier that lets the developer locate their pseudonymous Analytics record. State this
limit plainly. If a record can be identified, use Google's deletion tools and document the result;
Google says user deletion does not remove already aggregated measurements.

Android Play Games data has a separate deletion path through the player's Play Games profile.
Revoking Firebase Analytics does not delete the cloud save. See Google's
[Analytics privacy controls](https://support.google.com/analytics/answer/9019185),
[Analytics safeguards and user deletion](https://support.google.com/analytics/answer/6004245), and
[Play Games Services disclosure](https://developer.android.com/games/pgs/data-collection).

## Store disclosures to finish after the SDK audit

Do not publish these draft answers unchanged. Inspect the final signed AAB and iOS archive,
including merged manifests, privacy manifests, linked frameworks, runtime network report, and the
exact SDK versions. Recheck the current store questionnaires at submission time.

For Google Play Data safety, the Firebase-enabled configuration conservatively requires collection
for **Approximate location** (IP-derived geography), **App activity → App interactions** (custom
gameplay plus lifecycle/session events), and **Device or other identifiers** (app-instance ID).
Mark each as optional/user-controlled and for Analytics. Google says Analytics encrypts this data
in transit. Treat Google as a service provider and answer “not shared” only after the owner confirms
Google-products/services data sharing, Signals, advertising links, and other optional uses are off.
The Play form is global across versions, countries, and users, so it must cover the analytics build
while that build is distributed anywhere. Treat **App info and performance** as unresolved until
the final dependency and Data safety audit confirms whether SDK transport-health or other
diagnostic records fall in that category.

The Android Play Games-enabled configuration also requires a separate review for Gamer Identity,
SDK analytics/diagnostics, and Saved Games progress. It no longer sends DDDUMPLING gameplay through
the Play Games Events API, but the cloud save itself contains scores, collection counts, minigame
successes, and cumulative progress counters. Do not remove the Play Games declarations merely
because Firebase consent is declined.

For App Store privacy, use a conservative provisional classification of **Coarse Location**,
**Device ID**, and **Product Interaction**, all for Analytics. Mark these data types as linked to the
user for the draft because Apple defines linkage to include linkage through a device identifier,
and Analytics associates events and geography with an app-instance ID. Select “not used for
tracking” only after confirming the finished app has no IDFA/IDFV collection, advertising links,
Google Signals/account association, third-party data joins, or data-broker sharing. The final Xcode
privacy report and every transitive SDK privacy manifest decide whether **Other Usage Data** or
**Other Diagnostic Data** must also be declared; do not guess them away in advance.

The current iOS App Privacy answer “No, we do not collect data from this app” remains accurate only
for the SDK-free version. Change it before submitting an analytics-enabled iOS build. Apple's
questionnaire should describe the version currently available on the App Store, while Google Play
requires the sum of practices across versions currently distributed for the package.

Official store references: [Google Play Data safety instructions](https://support.google.com/googleplay/android-developer/answer/10787469),
[Firebase Analytics Play disclosure](https://support.google.com/analytics/answer/11582702),
[Apple App Privacy details](https://developer.apple.com/app-store/app-privacy-details/),
[managing App Privacy](https://developer.apple.com/help/app-store-connect/manage-app-information/manage-app-privacy),
and [Firebase's Apple disclosure guide](https://firebase.google.com/docs/ios/app-store-data-collection).

## Release evidence

Before distribution, retain a short internal record with:

- Firebase project ID, Android app ID, Apple app ID, and Analytics property ID (identifiers only;
  do not commit credentials or downloaded configuration files);
- screenshots or an account-owner note confirming Signals, product links, data sharing, ads
  personalization, and two-month retention settings;
- dependency lock versions and a final artifact inventory proving Analytics-only Firebase use;
- first-launch tests for ALLOW, NO THANKS, relaunch persistence, revocation, offline behavior, and
  separate Play Games cloud-save behavior;
- a proxy or device network check showing no Firebase request before ALLOW and no later upload of
  events generated while consent was absent;
- final Google Play Data safety and App Store privacy answers, with the date and build version
  audited; and
- a re-review of `docs/privacy.html` against that exact build before publishing the policy and
  before submitting either store form.
