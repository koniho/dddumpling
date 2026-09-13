/** Paste this file into https://script.google.com/create and run createTesterSurvey.
 * Creates one Google Form; re-running returns the same form rather than duplicating it.
 * Uses only Google Forms and script properties. Does not email testers.
 */
function createTesterSurvey() {
  const properties = PropertiesService.getScriptProperties();
  const existing = properties.getProperty('DDDUMPLING_SURVEY_FORM_ID');
  if (existing) {
    const form = FormApp.openById(existing);
    if (properties.getProperty('DDDUMPLING_SURVEY_READY') !== 'yes') {
      throw new Error('A partially created form exists. Review it before retrying: ' + form.getEditUrl());
    }
    logSurveyLinks(form);
    return;
  }

  const form = FormApp.create('DDDUMPLING — help tune the game', false);
  properties.setProperty('DDDUMPLING_SURVEY_FORM_ID', form.getId());
  form.setDescription(
    'About 5 minutes. Please answer after playing; there are no right answers, and criticism helps. ' +
    'Tell us about your actual experience, not how you imagine someone else would play. ' +
    'Choose “not tried” wherever appropriate; you do not need to reach every boss.\n\n' +
    'This survey is for adult testers or parents responding about a supervised play session. ' +
    'Please do not include names, email addresses, locations, or identifying details about children. ' +
    'We use your answers to improve controls, difficulty, and rewards. All questions are optional. ' +
    'The form does not ask for or collect email addresses; Google hosts and processes submissions under its own policies. ' +
    'Responses are not published to other testers. Privacy questions: dddumpling.play@gmail.com.'
  );
  form.setCollectEmail(false);
  form.setLimitOneResponsePerUser(false);
  form.setPublishingSummary(false);
  form.setAllowResponseEdits(false);
  form.setShuffleQuestions(false);
  form.setConfirmationMessage('Thank you! Your feedback helps us make DDDUMPLING clearer, fairer, and more fun.');

  const choice = (title, values) => form.addMultipleChoiceItem().setTitle(title).setChoiceValues(values);
  const text = title => form.addParagraphTextItem().setTitle(title);
  const grid = (title, rows, columns) => form.addGridItem().setTitle(title).setRows(rows).setColumns(columns);
  const section = title => form.addSectionHeaderItem().setTitle(title);

  section('Your play session');
  choice('Whose play experience are these answers based on?', [
    'My own play as an adult tester', 'A child’s play that I supervised as a parent',
    'Both — I will distinguish them in written answers', 'Prefer not to say'
  ]);
  choice('About how much have you played this version?', [
    'Less than 5 minutes', '5–15 minutes', '16–30 minutes', 'More than 30 minutes', 'Not sure'
  ]);
  choice('What was the highest stage you played, including any Lands skip?', [
    '1–4', '5–9', '10–14', '15–19', '20 or later', 'Not sure'
  ]);

  section('Balance and difficulty ramp');
  choice('How difficult were the opening stages (1–4)?', [
    'Much too easy', 'A little too easy', 'About right', 'A little too hard', 'Much too hard', 'Did not play / not sure'
  ]);
  choice('As you progressed, how did the difficulty increase?', [
    'Too slowly', 'At a comfortable pace', 'Too quickly', 'Unevenly — sudden spikes or drops',
    'I did not play enough consecutive stages to judge'
  ]);
  text('Where was the biggest difficulty spike or drop? Give a stage, boss, or description and what changed.');
  grid('How did these parts feel balanced?', [
    'Speed of falling words', 'Number of threats at once', 'Lives available after mistakes',
    'Help provided by power-ups', 'Time allowed in minigames'
  ], ['Too little / low', 'About right', 'Too much / high', 'Not tried / not sure']);
  text('What most often ended your run or made you stop? What would have helped?');

  section('Play mechanics');
  grid('How easy was it to understand what to do?', [
    'Typing the falling words', 'Activating a power-up', 'Using the swipe / fling power-up',
    'Knowing what to do in a minigame', 'Understanding when and why you lost a life'
  ], ['Very confusing', 'Somewhat confusing', 'Mostly clear', 'Very clear', 'Not encountered / not sure']);
  choice('Did taps, typing, swipes, and drags respond as you expected?', [
    'Always', 'Usually', 'Sometimes', 'Rarely', 'Not sure'
  ]);
  text('Which control or instruction needs improvement? Describe what you tried and what happened.');

  section('Bosses — rate only those you actually played');
  const bosses = [
    'Slime — stage 5, type letters and drag away the glob',
    'Dark Divide — stage 10, split the cube with two fingers',
    'Octopulse — stage 15, defend keys and drag arm tips',
    'Fly Agaric — stage 20, shake the mushroom cap'
  ];
  grid('How much fun was each boss?', bosses, [
    'Not fun', 'Slightly fun', 'Moderately fun', 'Very fun', 'Did not reach / not sure'
  ]);
  grid('How difficult was each boss?', bosses, [
    'Too easy', 'About right', 'Too hard', 'Could not understand the mechanic', 'Did not reach / not sure'
  ]);
  text('Which boss would you most want to replay, and why? Which boss would you change, and how?');

  section('Lands — skipping ahead from the title screen');
  choice('Before this survey, had you used Lands on the title screen to start farther into the game?', [
    'Yes, once', 'Yes, more than once', 'I saw it but did not use it',
    'I tried it but could not work out how to skip', 'I did not notice it', 'Not sure'
  ]);
  choice('If you used Lands, what was your main reason?', [
    'Avoid replaying earlier stages', 'Practise a boss or difficult section', 'Find a bigger challenge',
    'Explore later content', 'Try the feature out', 'Another reason', 'Did not use it'
  ]);
  text('If you tried Lands, did you land where you expected? What would make the feature clearer or more useful?');

  section('Display case and collecting');
  choice('How often did you open the display case during testing?', [
    'Never — I did not notice it', 'Never — I noticed it but was not interested',
    'Once', 'A few times', 'After most runs or rewards', 'Not sure'
  ]);
  grid('Which display-case activities did you try?', [
    'Browsing collected characters or items', 'Opening a character story', 'Playing story narration'
  ], ['Tried it', 'Noticed it but did not try', 'Did not notice it', 'Not sure']);
  choice('How much did collecting rewards make you want to play another run?', [
    'Not at all', 'A little', 'Somewhat', 'A lot', 'I did not notice or receive rewards'
  ]);
  choice('How did the mix of new and duplicate rewards feel?', [
    'Too many duplicates', 'About right', 'Too few duplicates', 'Did not collect enough to judge'
  ]);
  text('What would make the display case or rewards more satisfying?');

  section('One thing to fix first');
  text('If we changed just ONE thing to make the game better, what should it be?');
  form.addTextItem().setTitle('Optional bug context: app version, Android/iOS version, and phone model')
    .setHelpText('Only useful if you reported a bug. Do not include serial numbers, account details, or other identifiers.');

  form.setPublished(true);
  form.setAcceptingResponses(true);
  properties.setProperty('DDDUMPLING_SURVEY_READY', 'yes');
  logSurveyLinks(form);
}

function logSurveyLinks(form) {
  console.log('TESTER LINK: ' + form.getPublishedUrl());
  console.log('OWNER EDIT / RESPONSES: ' + form.getEditUrl());
  console.log('Before sharing, open the tester link in a signed-out browser and confirm it accepts a test response.');
}
