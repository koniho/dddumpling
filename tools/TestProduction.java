package com.dddumpling.game;

import java.io.File;
import java.util.Arrays;

/** Compiled separately with developer controls disabled; exercises the actual production branches. */
final class TestProduction extends Check {
    public static void main(String[] args) throws Exception {
        check("production flag is compiled off", !BuildFlags.DEVELOPER);
        Layout L = new Layout();
        L.compute(640, 1400, 0, 0, 0, 0);
        Mem store = new Mem();
        store.speed = GameCore.SPEED_MAX;
        store.bgm = (Music.defaultChoice(false) + 1) % Music.NAMES.length;
        store.collected = 1L;
        store.collectionCounts[0] = 7;
        store.collectTotal = 7;
        store.steamerOpens = 5;
        store.starWins = 3;
        GameCore c = new GameCore(store, 412L);
        check("production ignores saved developer speed", c.speed == 1f);
        check("production uses default music", c.bgmChoice == Music.defaultChoice(false));
        check("production retains progression and collection counts",
                c.collectionCounts[0] == 7 && c.steamer.opens == 5 && c.stars.wins == 3);
        c.openSettings();
        check("settings cannot open", !c.settingsOpen);
        SettingsUi ui = new SettingsUi();
        ui.compute(L, Music.NAMES.length);
        boolean targetsGone = true;
        for (float y = 0; y < L.h; y += 7f) {
            for (float x = 0; x < L.w; x += 11f)
                targetsGone &= !L.inStageTap(x,y) && ui.hit(x,y) == SettingsUi.HIT_NONE;
        }
        check("production has no settings touch targets", targetsGone);
        c.setSpeed(GameCore.SPEED_MIN);
        c.setBgm(store.bgm);
        boolean roster = c.fullRoster;
        c.setNextRoster(!roster);
        c.resetDifficultyScaling();
        c.tapClearCase(); c.tapClearCase();
        check("settings actions cannot change speed or music", c.speed == 1f
                && c.bgmChoice == Music.defaultChoice(false) && store.speedSaves == 0 && store.bgmSaves == 0);
        check("settings actions cannot change the roster", c.fullRoster == roster && store.rosterSaves == 0);
        check("settings actions cannot reset progression", c.steamer.opens == 5 && c.stars.wins == 3);
        check("settings actions cannot clear collections", c.collected == 1L
                && c.collectionCounts[0] == 7 && store.collectionCounts[0] == 7 && !c.clearArmed);
        check("policy accessible on title", PrivacyUi.hit(c,L,L.w-L.unit,L.dangerY-2f*L.unit));
        c.startGame();
        check("policy hidden in play", !PrivacyUi.hit(c,L,L.w-L.unit,L.dangerY-2f*L.unit));
        int stage = c.stage, lives = c.lives;
        c.playtestMode(Power.FLING,L);
        c.playtestStars(L);
        c.playtestSteamer(L);
        c.jumpToStage(20,L);
        c.endCurrentRun();
        check("playtest actions cannot start modes, skip stages or end a run",
                c.state == GameCore.PLAY && c.stage == stage && c.lives == lives && c.mode == -1);
        c.settingsOpen = true;
        float clock = c.time;
        c.update(DT,L);
        check("a stale settings flag cannot pause production", c.time > clock);
        RasterPainter hidden = new RasterPainter(640,1400,1);
        RasterPainter normal = new RasterPainter(640,1400,1);
        Renderer.draw(hidden,c,L);
        c.settingsOpen = false;
        Renderer.draw(normal,c,L);
        check("even a forced settings flag draws no panel", Arrays.equals(hidden.resolve(),normal.resolve()));
        int[] before = normal.resolve();
        Screens.settings(normal,c,L);
        check("direct panel rendering is disabled", Arrays.equals(before,normal.resolve()));
        File out = new File("out"); out.mkdirs();
        Png.write(new File(out,"production-play.png"),normal.resolve(),640,1400);
        Interlude.awardPrize(c);
        Interlude.awardStarPrize(c);
        Interlude.awardBossPrize(c,Boss.OCTOPUS);
        check("normal production rewards still work", c.collectTotal == 10
                && c.collectionCounts[Collect.BOSS_FIRST + Boss.OCTOPUS] == 1);
        Interlude.enterBonus(c,L);
        c.bonusTimer = 0.001f;
        c.update(DT,L);
        check("normal stage progression still works", c.stage == stage + 1);
        TestProgress.all(L);
        System.out.printf("%d passed, %d failed%n",pass,fail);
        if (fail > 0) System.exit(1);
    }
}
