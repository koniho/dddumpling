# Privacy policy publishing

Policy source: docs/privacy.html. Public contact: dddumpling.play@gmail.com.
Published independently of the game source at:
https://koniho.github.io/dddumpling-privacy/

Public website repository: https://github.com/koniho/dddumpling-privacy
Only the policy HTML is published there.

The game repository remains private. Changes to docs/privacy.html pushed to main are automatically published by
the Publish public pages workflow. See website-publishing.md.

Enter the public HTTPS URL in Play Console. Both production and developer builds expose a PRIVACY link on the title screen.
The policy covers the current offline main branch, not the unmerged Play Games
integration. Re-audit the final release bundle and Data safety form before release.

Sources reviewed September 11, 2026:
- https://support.google.com/googleplay/android-developer/thread/307687762/tips-and-best-practices-for-complying-with-privacy-policy-requirements?hl=en
- https://developer.android.com/identity/data/autobackup
- https://support.google.com/googleplay/android-developer/answer/10787469?hl=en

Code reviewed: AndroidManifest.xml, MainActivity persistence, Audio narration,
and the on-device Crash handler. No internet permission or analytics SDK in main.
