# Privacy policy publishing

Policy source: docs/privacy.html. Public contact: dddumpling.play@gmail.com.
Published independently of the game source at:
https://koniho.github.io/dddumpling-privacy/

Public website repository: https://github.com/koniho/dddumpling-privacy
Only the policy HTML is published there.

The game repository remains private. Changes to docs/privacy.html pushed to main are automatically published by
the Publish public pages workflow. See website-publishing.md.

Enter the public HTTPS URL in Play Console. Both production and developer builds expose a PRIVACY
control on the title screen. In the original SDK-free releases it opens the policy. In an
analytics-enabled release it also reopens the analytics choice so a player can continue to decline
or revoke previously granted permission.

The policy distinguishes the original SDK-free releases, optional Firebase Analytics, and optional
Android Play Games cloud saves. A missing Firebase configuration means Firebase Analytics is not
included and no analytics data is collected by DDDUMPLING. A configured analytics build must not
be distributed until the policy is published and the account settings, consent behavior, signed
artifacts, and store forms have been checked against
[the Firebase Analytics checklist](../docs/firebase-analytics.md).

Google Play's Data safety form is package-wide and must cover the sum of practices in every version
currently distributed on Play. App Store privacy answers should describe the iOS version currently
available. Preserve the existing no-collection answer while only the SDK-free iOS build is
available, then replace it before an analytics-enabled build is submitted. Do not publish draft
answers or change either external form from this repository alone.

Sources reviewed September 12, 2026:
- https://support.google.com/googleplay/android-developer/thread/307687762/tips-and-best-practices-for-complying-with-privacy-policy-requirements?hl=en
- https://developer.android.com/identity/data/autobackup
- https://support.google.com/googleplay/android-developer/answer/10787469?hl=en
- https://support.google.com/analytics/answer/11582702?hl=en
- https://developer.apple.com/help/app-store-connect/manage-app-information/manage-app-privacy

The initial-release review covered AndroidManifest.xml, MainActivity persistence, Audio narration,
and the on-device crash handler. Its no-analytics conclusion remains evidence for those released
SDK-free artifacts, not for a future Firebase-configured build.
