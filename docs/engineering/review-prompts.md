# Store review prompts

`ReviewPrompt` owns local eligibility and attempt history for both platforms. It counts
run starts separated by at least 30 minutes as separate sessions. After three sessions
across two UTC dates and the first boss collectible, a later boss win or newly earned
collectible can arm a request. Returning to a clear title screen consumes that request.
Tutorials, summaries, settings, cloud UI and update notices take priority. A defeat
cancels an earlier pending milestone; a new survival collectible can arm its own request.

Automatic requests are limited to once per marketing version and at least 90 days apart.
The host asks the native store; the store decides whether to display its sheet. Persisted
history records an attempt, never a submitted rating. The history is device-local and
excluded from cloud saves. Existing Settings review links still open the store listing.

Google Play builds use Play In-App Review. App Store builds use StoreKit. Automatic
requests are disabled for developer builds, offline Android builds and iOS sandbox
receipts. Failures or store suppression do not interrupt play or open a fallback popup.

## Developer checks

On **Settings → Developer → Progress**, tap **TEST REVIEW**, close Settings, and return
to the title screen. Once tutorials or other active notices finish, a clearly labeled
native preview appears. It bypasses eligibility without changing real attempt history.
**Try store flow** requests the real native sheet, which may still be suppressed:
Google Play generally requires a recognized store install/account, while TestFlight
suppresses review sheets. The preview is not a simulated rating submission.

**RESET REVIEW** clears session counts, dates and review cooldown only. **CLEAR ALL
PROGRESS** also resets review history. Neither changes an actual store rating.

`TestReviewPrompt` covers eligibility, persistence, timing, cooldown and developer guards.
The iOS input tests cover the bridge and chip interaction. Settings preview frames 121
and 122 cover the developer Progress layout. Native iOS compilation requires the Mac CI.
