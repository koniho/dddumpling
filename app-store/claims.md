# Listing statements and source references

Recorded September 11, 2026 for the initial offline release. These references
help keep future listing revisions aligned with the actual build.

| Statement | Implementation or evidence |
| --- | --- |
| Tap character keys to clear falling sequences | src/com/dddumpling/game/GameCore.java; Words.java; Glyph.java |
| Combos and transforming powerups | GameCore.java; Power.java; Blade.java; Buddy.java |
| Boss interactions and collectible boss friends | Boss.java; BossPlay.java; BossCollect.java; BossVictory.java |
| Dark Divide splits; Octopulse tentacles; Fly Agaric shaking | Boss.java and BossScreen.java |
| Starpath and steamer minigames | StarPath.java; Steamer.java; Interlude.java |
| Character collection, display case, and stories | Collect.java; Showcase.java; CaseUi.java; Storybook.java; Lore.java |
| Local best scores and progress | MainActivity.java private SharedPreferences |
| Offline, no ads or account required | Current AndroidManifest.xml has no internet permission; current build has no ad, account, or analytics integration |

Java filenames without a directory above are under src/com/dddumpling/game/.
The gameplay tests are in tools/Test*.java and run through check.sh.

These statements remain a historical record for the initial SDK-free release. Do not reuse the
offline/no-analytics row for an optional Firebase-configured build. That build offers analytics
only after an explicit ALLOW choice; NO THANKS and missing Firebase configuration collect nothing
through Firebase. See [the setup and disclosure checklist](../docs/firebase-analytics.md).

The Android Play Games configuration is independent. A Play Games-enabled build may authenticate
and synchronize one private cloud save even when Firebase analytics is declined. Its current
configuration does not report gameplay through the Play Games Events service. Review account,
cloud-save, offline, privacy-policy, and Play Data safety wording against the final release artifact.

No statements about supported languages, release availability, pricing, ratings,
or online synchronization have been added to the descriptions.
