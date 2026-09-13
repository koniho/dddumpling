# DDDUMPLING tester survey

Ready-to-create Google Form: [tools/create-tester-survey.js](../tools/create-tester-survey.js).
The form is intended for adult community testers and parents, not unsupervised child respondents.
It asks about actual play rather than asking adults to predict what children will enjoy.

## Create and get the link

1. Open https://script.google.com/create while signed into the Google account that should own responses.
2. Replace the starter code with the complete script above, save, and run `createTesterSurvey`.
3. Authorize Google Forms access for this script. It creates and publishes a form; it does not email testers.
4. Copy **TESTER LINK** from the execution log. Keep the **OWNER EDIT / RESPONSES** link private.
5. Open the tester link signed out and submit a test response. If your account restricts respondents,
   use the form's Publish/Manage controls to allow anyone with the link, then retest. Delete your test response.

Re-running the completed script prints the same links without duplicating the form.
A failed partial creation is retained for inspection rather than silently replaced.

## Scope and privacy

Approximately five minutes, with optional questions and explicit not-tried choices. Covers opening
difficulty, ramp and spikes, threat speed/density, lives, power-ups, minigames, input reliability,
fun and difficulty for all four named bosses, Lands discovery/use, display case usage and rewards.

No email collection, one-response sign-in restriction, public response summary, file upload,
location question, or child-identifying fields. Google still hosts and processes the form;
do not describe it as free of all provider data processing or guaranteed anonymous.
Keep raw answers private, remove accidental personal details, and retain only feedback needed
for the testing work. Assess adult and parent-observed answers separately when tuning difficulty.

No live form has been created by the repository script until it is run in the owner's Google account.
Local syntax checks do not verify Google authorization, publishing, or actual response delivery.

API reference: https://developers.google.com/apps-script/reference/forms/form
