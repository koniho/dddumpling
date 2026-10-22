# Game services achievements

`Achievements.java` owns permanent goals and reads the mergeable `Progress` document.
`tools/achievements.py` derives store metadata and validates `store/achievements.json`.
The 26 goals cover collection 1/10/25, stages 5/10/20/30, both minigame first wins,
every boss's first win, five perfect Adventure stages across runs, an All Bosses Time
Attack win, Survival 1/2/3/4/5 minutes in one run, and 2/3/4/5 lands cleared in one
Adventure run. Each land requires its boss to be beaten in that run. Kids mode and
all roster profiles qualify. Permanent achievements survive score resets.

Existing collection and minigame progress qualifies. Retained Survival and current-roster
Time Attack records seed milestones on load. Perfect-stage and same-run-land achievements
start recording with this implementation; old saves lack evidence to reconstruct them.
Boss wins in either Adventure or Time Attack qualify.

Google uses standard achievements for one-step goals and incremental achievements otherwise.
The adapter submits absolute `setStepsImmediate` values and `unlockImmediate`, acknowledging
only successful callbacks. Apple submits absolute percentages through `GKAchievement`.
Both retry pending progress on a ten-second foreground interval and evaluate restored saves.
Local progress is durable offline; callbacks never add gameplay progress. Reporting requires
the authenticated player to match the local save owner. Game Center achievements do not
require iCloud Drive. Developer Apple IDs use `com.dddumpling.game.dev.`; production IDs use
`com.dddumpling.game.`. Android developer builds include the SDK but keep reporting disabled
until a matching Dev configuration is supplied; see [developer testing](play-games.md#side-by-side-developer-testing).

## Adding content

1. Append a stable boss analytics ID in `WorldCatalog`, or add a permanent milestone to
   `Achievements`. Never rename existing platform identifiers or change earned goals.
2. Run `python3 tools/achievements.py` to inspect metadata. Update metadata generation for
   a new kind of goal; boss-first-win entries derive automatically from the world catalog.
3. After the normal rules check compiles the harness, render icons with
   `java -cp build/harness com.dddumpling.game.AchievementIcons build/achievement-icons`.
   These original game-art PNGs are generated assets, not committed binaries.
4. Generate only missing Google definitions with
   `python3 tools/achievements.py --export-play build/achievements-import.zip`.
   Import the ZIP in Play Console's Achievements page and save as draft. Read the real
   IDs back into `store/achievements.json`; do not reimport already provisioned entries.
5. Create matching Apple definitions and en-US localizations in both app configurations.
   Upload each icon to its achievement localization. Record the returned Apple resource
   IDs and verify image processing completes. Record the uploaded PNG's SHA-256 as
   `icon_sha256`. Use the App Store Connect API or Console.
6. Run `python3 tools/test-prepare-play.py`. Missing platform mappings fail normal rules
   checks and enabled service builds. Provisioning and publishing are separate steps.

Console drafts and compile tests do not establish live unlock delivery. Before release,
test signed-in unlocks, offline/reconnect, cloud restore, duplicate replay and account changes
on real service-enabled builds. Publish the Google configuration and submit Apple achievements
for review as part of the release process, after release approval.
