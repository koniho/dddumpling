# Analytics consent validation

`analytics-consent-iphone.png` is an actual iPhone 17 Pro simulator capture from the final consent
UI test. The Debug-only preview backend uses the real shared renderer, accessible controls, and
consent storage without initializing Firebase. It is a development screenshot, not a store asset
or proof of uploaded events.

Local validation on September 12, 2026:

- Shared game: 4,909 assertions; production: 92 assertions; iOS input: 64 assertions.
- Existing harness images and audio retain their baseline SHA-1 hashes.
- iPhone simulator: 14 native tests and four UI tests passed. The final targeted consent UI test
  also passed after the layout refinements, including both choices, restart, reopening Privacy,
  withdrawal, and continued gameplay.
- iOS Release simulator build and unsigned device archive passed. The device archive has the
  production bundle ID, disabled collection/ad consent/IDFV flags, and no linked AdSupport or
  AppTrackingTransparency frameworks.
- The unconfigured archive contains no `GoogleService-Info.plist`. SDK manifests include
  FirebaseInstallations' unlinked Other Diagnostic Data declaration. The app manifest also
  declares the optional analytics interaction, app-instance identifier, and coarse location types.
- Android SDK-free, Firebase-only, and combined Firebase/Play APK packaging passed. A real
  Firebase-configured unsigned AAB passed the Bundletool manifest/dex verifier, with no AD_ID
  permission or automatic Firebase initialization providers. Build fixtures were removed.

The real Firebase app configuration, console privacy/retention settings, and live event delivery
remain unverified. No Firebase-enabled build has been uploaded to TestFlight or Google Play by
this change. See [setup and disclosure instructions](../../../docs/firebase-analytics.md).
