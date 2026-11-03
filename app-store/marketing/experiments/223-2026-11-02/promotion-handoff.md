# Promotion activity handoff — November 2, 2026 evening

Resume with the [experiment overview](README.md), [publication ledger](publication-ledger.csv),
[measurement log](measurement-log.csv) and [supercut provenance](store-supercut.json).
This note records the phone-side conversation; do not overwrite the PC agent's verified ledger.

## YouTube Promote: setup in progress, launch unverified

The user is setting up a paid promotion on the phone. Screenshots at 20:32 and
20:34 America/Chicago show Website visits selected, the Meowcula net-catch Short,
and the promotion creative form. They do not show a submitted/active campaign,
campaign ID, accepted budget or targeting. The description showed `theorifice`,
but the exact selected video/channel ID has not been confirmed.

Suggested settings, not recorded approvals:

- Goal: Website visits, to explore player acquisition rather than subscriber growth.
- Creative: Meowcula capture, net breaking and rocket escape.
- Headline: `DDDumpling: Tap & Swipe Game` (within the displayed 40-character limit).
- Description: `Catch cats. Dress up squishies. Play free.`
- Audience: US, English, ages 18–34, all genders.
- Interests: closest available options to Mobile games, Casual games and Arcade games.
- Budget: US$30 total over seven days, not a daily budget. No paid launch was performed
  by this agent and no specific spend approval has been recorded in this conversation.
- Destination supplied: https://koniho.github.io/dddumpling-game/

The screenshot had an empty URL error and an overlength headline; final values are
not yet verified. User is navigating the actual promotion UI. Do not create a duplicate.

### Attribution gap to resolve

The supplied homepage returns HTTP 200 and has both store buttons, but its buttons
currently use generic store URLs without campaign tags. Do not claim this promotion
has install attribution. The existing `download-youtube.html` page is for the shared
organic `youtube_profile` campaign; reusing it for paid traffic would mix results.
Before launch, prefer a dedicated paid destination with Android UTM and Apple ct/pt
links registered in [campaign links](../../campaign-links.md). Verify actual redirects
and any reporting limits. Landing-page query parameters alone do not establish
store/install tracking.

Next operator: confirm exact video/channel, final destination, targeting, currency,
total budget, dates and the user's spend approval; record campaign ID/status and
actual start time. Record spend, impressions, named paid-view metrics, website clicks
and attributable installs where available. Do not treat clicks as installs or missing
data as zero. Keep paid results separate from organic Shorts and concurrent store edits.

## Website supercut

The user authorized replacing the homepage's older trailer with the existing supercut.
Both trailer links in `docs/site/index.html` are updated in this change to:
https://www.youtube.com/watch?v=nAevNW-4zY8

This is the existing 27-second store supercut, recorded as an unlisted upload on
@dddumplingplay in `store-supercut.json`. YouTube oEmbed returned the title
`DDDumpling | Tap, Swipe, Collect & Dress Up` and author `DDDumpling` during this work.
No new upload is required. Publish through the existing public-pages workflow and
verify the live page before reporting completion. Site layout and store controls are unchanged.

## Future video preferences

- Full game name: `DDDumpling: Tap & Swipe Game`.
- Much larger Bungee game-font overlays; start around 100–120 px at 1080 px width,
  wrap as needed and review at phone size.
- Fade gameplay behind the overlay, then restore it.
- Add a cute actual-game dumpling squashing and springing back beside the overlay.
- Preserve source resolution, frame timing and game audio; keep overlays clear of
  platform controls and the gameplay payoff.

Earlier detailed overlay/YouTube guidance was committed locally as `c92a78a` on
`docs/android-local-install` in `app-store/video/README.md`; it is not asserted merged
to main. The preferences above make the handoff self-contained.

Three additional phone-made wardrobe clips are in `/sdcard/Movies/` and media-indexed:
`dddumpling-pear-drop-dress-up.mp4`, `dddumpling-seashell-googly-eyes.mp4`, and
`dddumpling-dohnut-outfit-shuffle.mp4`. These are separate exports from the PC agent's
scheduled creative set. They have not been uploaded by this agent; do not substitute
them for scheduled files without reviewing the comparison design.

## YouTube Playables

[Issue #230](https://github.com/koniho/dddumpling/issues/230) captures the application,
web-port feasibility, renderer/cache risks, performance gates, SDK integration,
audience/ads constraints and official sources. Application not submitted; no port started.
Apply/evaluate before committing to a full port. Playables is another distribution
channel, not an assumed app-install funnel: in-game external links are prohibited.

## Immediate next actions

1. Verify website deployment contains the supercut link.
2. Resolve paid attribution and confirm the user's final campaign settings before launch.
3. Read back actual publication and store review/live statuses; existing scheduled and
   review records do not establish that posts or store changes are live now.
4. Continue 48-hour/seven-day observations from actual publication times per the ledger.
5. Prepare the Playables application and obtain approval before submitting declarations.
