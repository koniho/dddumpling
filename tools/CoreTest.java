package com.sram.hexatype;

/**
 * Runs every headless assertion suite. Each suite lives in its own Test* class; the shared
 * tally and helpers are in {@link Check}.
 */
final class CoreTest {

    private CoreTest() {}

    public static void main(String[] args) {
        Layout L = new Layout();
        L.compute(1080, 2340, 0, 60, 0, 90);

        TestRules.layout(L);
        TestRules.targeting(L);
        TestRules.engagement(L);
        TestRules.scoringAndStages(L);
        TestRules.breachAndGameOver(L);
        TestRules.screens(L);

        TestWords.stackedLetters(L);
        TestWords.destruction(L);
        TestWords.entranceAndPersistence(L);

        TestStages.waves(L);
        TestStages.bonusOrdering(L);
        TestStages.bonusIntro(L);
        TestStages.spinner(L);
        TestStages.bonusStatusHold(L);
        TestStages.skits(L);
        TestStages.steamerBonus(L);
        TestStages.accuracyTracking(L);
        TestStages.warningsAndHarm(L);

        TestCollect.catalogue(L);
        TestCollect.ownedSet(L);
        TestCollect.blindBox(L);
        TestCollect.winning(L);
        TestCollect.displayCase(L);
        TestCollect.screenKeys(L);
        TestCollect.clearing(L);

        TestLore.stories(L);
        TestLore.casting(L);
        TestLore.popup(L);

        TestVisuals.sky(L);
        TestVisuals.indicatorsAndGlow(L);
        TestVisuals.settings(L);

        TestAudio.audio(L);

        TestPower.drifting(L);
        TestPower.precedence(L);
        TestPower.frenzy(L);
        TestPower.flurryMode(L);
        TestPower.multiMode(L);
        TestPower.flingMode(L);
        TestPower.trail(L);
        TestPower.frenzyFallSpeed(L);
        TestPower.playtest(L);
        TestPower.soak(L);

        TestSoak.perfectPlaySurvives(L);
        TestSoak.fuzz(L);

        System.out.printf("%n%d passed, %d failed%n", Check.pass, Check.fail);
        if (Check.fail > 0) System.exit(1);
    }
}
