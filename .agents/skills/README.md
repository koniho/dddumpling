# Repository skills

These copies travel with the checkout. [Codex discovers local skills](https://developers.openai.com/codex/skills/)
under `.agents/skills/`;
the root `AGENTS.md` also points here. Newly installed skills are available on the next turn.
Read only the skill that matches the task and any references it requires.

| Set | Contents | Upstream |
| --- | --- | --- |
| Google Play | 18 skills for auth, listings and images, releases, reviews, vitals, products and other Play operations | [PollyGlot/google-play-cli-skills](https://github.com/PollyGlot/google-play-cli-skills) |
| Apple | The same 23 categories and 183 skills previously installed globally, including App Store, Apple Ads, growth, asset planning and platform development | [rshankras/claude-code-apple-skills](https://github.com/rshankras/claude-code-apple-skills) |

## Common tasks

| Task | Start here |
| --- | --- |
| Any Google Play command | [gplay-cli-usage](gplay-cli-usage/SKILL.md) |
| Google Play authentication | [gplay-setup](gplay-setup/SKILL.md) |
| Play listing text, screenshots or feature graphics | [gplay-metadata-sync](gplay-metadata-sync/SKILL.md) |
| Play releases and rollout | [gplay-release-flow](gplay-release-flow/SKILL.md) |
| Play reviews or app quality | [gplay-reviews](gplay-reviews/SKILL.md), [gplay-vitals](gplay-vitals/SKILL.md) |
| App Store marketing and assets | [app-store](app-store/SKILL.md), [app-store-assets](generators/app-store-assets/SKILL.md) |
| Apple Ads | [apple-search-ads](app-store/apple-search-ads/SKILL.md) |
| DDDumpling promotional illustrations | [Project generation workflow](../../ios/docs/promotional-assets.md) |

The Play skills instruct an agent how to use the separate
[gplay CLI](https://github.com/PollyGlot/google-play-cli). Installing these Markdown files
does not install the CLI or configure API access. Live Play operations need that tool and
a service account with access to the intended app. Keep credentials under ignored `.private/`
and read `gplay-setup` before configuring access. Check the installed CLI's `--help` before
using a skill's command examples.

Apple skills cover several platforms. Apply each within its stated scope; this game's
Java code and existing build flow remain the source of truth for implementation.

## Provenance and updates

[_sources/lock.json](_sources/lock.json) records repository URLs, exact commits and imported
directories. Both upstream MIT licenses are retained in `_sources/`. Imported skill files
are unchanged, including their supporting references and templates.

To update a set, select and review an upstream commit, then use Codex's skill-installer
helper with `--repo`, `--ref`, `--path` and `--dest`. The imported paths are in the lock file.
Install to a temporary directory first because the helper refuses existing destinations.
Compare the changes, replace only that set's directories, preserve its license, and update
the lock file. Verify every `SKILL.md` has its name and description, and that the referenced
files remain present. Commit the updated set and provenance together.
