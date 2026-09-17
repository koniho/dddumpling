package com.dddumpling.game;

/**
 * Runs every headless assertion suite. Each suite is a Test* class; the tally and helpers are in
 * {@link Check}.
 *
 * One arg, optional: a substring of a group name below. Only matching groups run, which is what
 * {@code check.sh -s} passes.
 */
final class CoreTest {

    private CoreTest() {}

    private static String only;

    private static void group(String name, Runnable r) {
        if (only == null || name.toLowerCase().contains(only)) r.run();
    }

    public static void main(String[] args) {
        Layout L = new Layout();
        L.compute(1080, 2340, 0, 60, 0, 90);
        only = args.length > 0 && !args[0].isEmpty() ? args[0].toLowerCase() : null;

        group("Settings", () -> TestSettings.all(L));
        group("Cave", () -> TestCave.all(L));
        group("Cave Band", () -> TestCaveBand.all(L));
        group("Cave Mining", () -> TestCaveMining.all(L));
        group("Mystery pickups", () -> TestMystery.all(L));
        group("Linked pairs", () -> TestLinkedPairs.all(L));
        group("Progress", () -> TestProgress.all(L));

        group("Back", () -> TestBack.navigation(L));
        group("Rules", () -> {
            TestRules.layout(L);
            TestRules.targeting(L);
            TestRules.engagement(L);
            TestRules.scoringAndStages(L);
            TestRules.breachAndGameOver(L);
            TestRules.screens(L);
        });
        group("Roster", () -> TestRoster.adaptive(L));
        group("Words", () -> {
            TestWords.stackedLetters(L);
            TestWords.destruction(L);
            TestWords.entranceAndPersistence(L);
        });
        group("Stages", () -> {
            TestStages.waves(L);
            TestStages.bonusOrdering(L);
            TestStages.bonusIntro(L);
            TestStages.spinner(L);
            TestStages.bonusStatusHold(L);
            TestStages.skits(L);
            TestStages.mashEarned(L);
            TestStages.steamerBonus(L);
        });
        group("Stars", () -> {
            TestStars.game(L);
        });
        group("Boss", () -> {
            TestBoss.cadence(L);
            TestBoss.frame(L);
            TestBoss.renderEffects(L);
            TestBoss.winning(L);
            TestBoss.precedence(L);
            TestBoss.slime(L);
            TestBoss.bolts(L);

            TestBoss.divider(L);
            TestBoss.octopus(L);
            TestBoss.mushroom(L);
            TestBoss.stacking(L);
            TestBoss.stageJump(L);
            TestBoss.cleanup(L);
        });
        group("Softbody", () -> {
            TestSoftbody.physics(L);
        });
        group("Stages", () -> {
            TestStages.accuracyTracking(L);
            TestStages.warningsAndHarm(L);
            TestStages.pushBack(L);
            TestStages.pushBackRelief(L);
        });
        group("Collect", () -> {
            TestCollect.catalogue(L);
            TestCollect.ownedSet(L);
            TestCollect.blindBox(L);
            TestCollect.winning(L);
            TestCollect.parade(L);
            TestCollect.displayCase(L);
            TestCollect.caseTouch(L);
            TestCollect.screenKeys(L);
            TestCollect.startFade(L);
            TestCollect.sendOff(L);
            TestCollect.clearing(L);
        });
        group("Lore", () -> {
            TestLore.stories(L);
            TestLore.casting(L);
            TestLore.popup(L);
            TestLore.narration(L);
        });
        group("Visuals", () -> {
            TestVisuals.sky(L);
            TestVisuals.titleScreen(L);
            TestVisuals.indicatorsAndGlow(L);
            TestVisuals.hudStacking(L);
            TestVisuals.starStacking(L);
            TestVisuals.deathIsGreen(L);
            TestVisuals.settings(L);
        });
        group("Audio", () -> {
            TestAudio.audio(L);
            TestAudio.musicChoice(L);
            TestAudio.frenzySounds(L);
            TestAudio.haulLanding(L);
            TestAudio.starPickup(L);
            TestAudio.interludeSounds(L);
        });
        group("Power", () -> {
            TestSideEntry.all(L);
            TestFrenzyRefill.all(L);
            TestPower.drifting(L);
            TestPower.precedence(L);
            TestPower.frenzy(L);
            TestPower.flurryMode(L);
            TestPower.multiMode(L);
            TestPower.chain(L);
            TestPower.teamMode(L);
            TestPower.flingMode(L);
            TestPower.blade(L);
            TestPower.strokeEnd(L);
            TestPower.modeSpread(L);
            TestPower.trail(L);
            TestPower.frenzyFallSpeed(L);
            TestPower.frenzyTaper(L);
            TestPower.playtest(L);
            TestPower.soak(L);
        });
        group("Soak", () -> {
            TestSoak.perfectPlaySurvives(L);
            TestSoak.boundedPlay(L);
            TestSoak.fuzz(L);
        });
        System.out.printf("%n%d passed, %d failed%n", Check.pass, Check.fail);
        if (Check.fail > 0) System.exit(1);
    }
}
