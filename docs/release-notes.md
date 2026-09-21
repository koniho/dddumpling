# Writing in-game release notes

Use this guide to add or edit notes. For every release request, work through the proposed entries interactively with the user and obtain explicit approval of the final notes before proceeding with version preparation, tagging, or publishing. For publishing and tagging, use [Executing a release](releasing.md).

You edit plain JSON in [release-notes/releases.json](../release-notes/releases.json). You do not need to edit Java, count rows, or position icons. The tool wraps the copy and updates the game. The source and game keep the full authored history, newest first. Players scroll down to reach older releases; adding a release never removes earlier entries from the list.

## Add a release

1. Create a draft, substituting the planned release version:

   ```sh
   python3 tools/release-notes.py new 0.1.20
   ```

2. Open `build/release-0.1.20.json` in any text editor. First collect minor improvements into one `misc` entry; the draft starts with that group. Add one change object per substantial player-facing feature and remove empty groups. Combine redundant entries and choose `autoReset` for every entry before adding the draft. Use only changes included in the release. For example:

   ```json
   {
     "version": "0.1.20",
     "changes": [
       {
         "icon": "pair",
         "title": "Better together",
         "autoReset": true,
         "where": "Normal waves, stage 16+",
         "why": "Press both pals together to pop the pair. A little teamwork for your thumbs."
       },
       {
         "icon": "bugs",
         "title": "Happy little fixes",
         "autoReset": false,
         "fixes": [
           {
             "where": "After Star Path",
             "why": "Your flyer stays gone after blasting off."
           }
         ]
       }
     ]
   }
   ```

   This is a format example, not a claim that those changes belong to 0.1.20. Replace it with the actual release contents. Remove the bugs object if there are no standalone player-facing fixes; otherwise put those fixes inside that single object. Fixes and polish that complete a feature introduced in this release belong with that feature.

3. Add the finished draft and read the result:

   ```sh
   python3 tools/release-notes.py add build/release-0.1.20.json
   python3 tools/release-notes.py preview
   ./check.sh -q -s Visuals -f 103-release
   ```

   `add` validates the draft, puts it first in the source, and updates the game's generated copy. It never changes the app version, commits, tags, or uploads anything. It refuses a duplicate release version. The preview prints exactly the text the game uses.

4. Review each entry with the user: title, phase, benefit, icon/demo, and reset choice. Incorporate their edits, then present the final grouped notes and applicable store summaries for approval. Review the images under `out/103-release-*.png`, then try the steamer entry in the app. `RESET NEWS` in developer settings restores the corner steamer’s star, lid pops, and extra steam until the notes are opened. Check the feature context, icon, copy, wrapped rows, and Back navigation. The `wrapped` preview frames deliberately use an overfilled test catalog, not real release contents.

5. Commit `release-notes/releases.json` and `src/com/dddumpling/game/ReleaseContent.java` with the release preparation. Finish the [release checklist](releasing.md) before tagging.

## Edit existing copy

Edit the relevant object in `release-notes/releases.json`, then run:

```sh
python3 tools/release-notes.py sync
python3 tools/release-notes.py preview
```

`ReleaseContent.java` is generated: don't edit it directly. `check.sh` fails with the repair command if the JSON and game copy disagree. Changes to a feature are stored in that release's own entry, so reusing an icon does not rewrite older releases.

## What to write

Exclude features and changes that are available only behind development flags or in developer builds. Verify availability in the intended production build before drafting notes; merging code into main does not make a feature release-ready. Apply this rule to in-game notes and all store, TestFlight, GitHub, itch, and dev-blog release summaries.

- **autoReset:** required `true` or `false`. Choose whether this entry’s demo should return to its starting state two seconds after activation. The draft leaves this as `null` so you must decide before adding it.
- **title:** a short, playful name, up to 24 characters.
- **where:** for a bug fix that is already clear on its own, use an empty string to omit the context line. Otherwise, where the player encounters it, up to 32 characters. Name the phase and stage gate when relevant: “On the title screen”, “Star Path bonus rounds”, or “Power-ups, stage 16+”.
- **why:** what changed and what that gives the player. A sentence or two is enough. Prefer “Press both pals together… teamwork for your thumbs” over “Improved linked-pair logic”.

Use straight quotes and plain English punctuation. The tool wraps `why` automatically; don't insert line breaks. Keep each point brief. If the tool says a popup is too long, combine related fixes or shorten the explanation. Do not remove the phase just to make it fit.

Keep the list free of feature labels; the context belongs inside the popup. Include observable gameplay changes, not build infrastructure or unfinished work. Don't invent player benefits from commit titles: check the actual change. Bugs use one cute bug icon per release, with a `where` and `why` for each fix.

## Decide whether the demo resets

Make this choice while writing each entry, based on what the player should see and try. Set `autoReset: true` when an interaction consumes or finishes the demo and a fresh attempt helps: the shuffle pickup and partner enemies currently reset after two seconds. Set `autoReset: false` when keeping the result helps exploration: land travel keeps its selected land, and the smaller illustrations do not reset automatically.

The choice belongs to the entry, not its icon. Reusing an icon in another release can have a different choice. Every entry, including a bug group, requires an explicit boolean; the tool rejects missing or undecided values. This setting is authoring metadata and adds no player-facing text.

During review, activate the demo, wait longer than two seconds, and check that it resets or retains its state as chosen. An automatic reset returns it to the initial state once; it does not keep resetting while idle. Reopening a feature always starts it fresh.

## Combine redundant entries

Review the release as a whole before generating or approving its notes. Use one entry per distinct player-facing change, not one per commit, PR, animation, or implementation step. Combine related behavior, visual polish, sound, and fixes into the feature they introduce. Keep only the context and benefit needed to understand it; combining entries does not mean packing every development detail into one longer paragraph.

For example, 0.1.17 introduces the partner enemy. Its bond flex, power-up interactions, arm polish, and related fixes are part of that introduction, so it has just the first “Better together” entry and its interactive pair demo. They do not need separate icons or a duplicate bug entry.

Keep a separate entry only when it communicates a distinct change the player needs to know. Group remaining standalone fixes under one bug icon, and omit that icon when none remain. Before tagging, read all entries together and remove repeated information; apply the same grouping to store and dev-blog release summaries.

## Available icons

| `icon` value | Use for | Illustration |
| --- | --- | --- |
| `scores` | Saved high-score runs | Glowing score rows and a gold star |
| `octopulse` | Octopulse battle changes | Waving Octopulse portrait |
| `swipe` | Rescue-swipe lesson | Shared instruction hand swiping up from a pulsing bar |
| `settings` | Player settings | Hexagon with three menu lines |
| `news` | Exploring release notes | Little steamer |
| `flurry` | Flurry pickup feedback | Circular rainbow |
| `travel` | Choosing or traveling between lands | Interactive land travel |
| `stars` | Star Path challenges | Star and trail |
| `shuffle` | Mystery power-up pickups | Interactive pickup shuffle |
| `disguise` | Incognito or Monochrome | Disguised characters |
| `slime` | Slime boss changes | Slime and prompt |
| `pair` | Linked-pair gameplay | Interactive linked pair |
| `flex` | A pair rejecting a single hit | Flexed arm |
| `team` | Linked pairs during power-ups | Frenzy character |
| `misc` | Small improvements grouped into one entry | Dumpling with little sparkles |
| `bugs` | Standalone fixes not already covered by a feature | Cute bug |

An icon also chooses its illustration. Reuse one only when its meaning and demo match the change. A genuinely new mechanic needs a matching drawing/demo in `ReleaseChange` and a supported tool icon; ask for that as part of implementing the mechanic. Normal copy updates and new releases using existing icons require no Java edits.

## Before tagging

```sh
python3 tools/release-notes.py check --version 0.1.20
```

This verifies the source, generated game copy, and newest release version. It cannot prove a feature shipped or that a sentence is accurate: review those against the release diff. Summarize the same reviewed changes for Play, TestFlight, GitHub, and itch as applicable; the in-game catalog does not automatically update those destinations.

## Small improvements

Group minor polish under one `misc` entry titled "Little improvements". Use an
`improvements` array of `where`/`why` objects, just as the bug entry uses `fixes`.
Keep each point brief and retain its game-phase context. Use `autoReset: false`
for the small sparkle illustration. Standalone fixes still belong under `bugs`;
changes with a substantial new mechanic can keep their own feature entry.

```json
{
  "icon": "misc",
  "title": "Little improvements",
  "autoReset": false,
  "improvements": [
    {"where": "On the title screen", "why": "New lands get a little tour."},
    {"where": "In the release book", "why": "Tap outside the list to close it."}
  ]
}
```

Collect these points from the actual release diff before the interactive note review.
Review them together as one entry and keep the same grouping in destination summaries.

## Release icon review image

Before asking for final release-note approval, generate a sheet of the proposed release's
icons from the actual game renderer, including animation samples and each reset choice:

```sh
python3 tools/release-notes.py review-image build/release-0.1.23.json
```

Use the intended version's draft path. This does not alter the catalog or prepare a version.
Review the PNG together with the notes and destination summaries. Regenerate after icon or
reset edits. On Android the command requests the system image viewer through a readable content URI.
Confirm the image actually appears; a successful command alone does not prove it opened.
If no viewer appears, show the PNG inline for review and retry opening it in Files.
Do this immediately after generation, before approval.
Use `--no-open` only for CI or when opening applications is unavailable. On other systems,
open the printed PNG path. The sheet shows artwork samples; also check interactive demos
in the release book and wait more than two seconds to verify their reset choices.
