package com.dddumpling.game;

import java.io.File;
import java.util.Arrays;

/** Compiled separately with developer controls disabled; exercises the actual production branches. */
final class TestProduction extends Check {
    public static void main(String[] args) throws Exception {
        check("production flag is compiled off", !BuildFlags.DEVELOPER);
        Layout L = new Layout();
        L.compute(640, 1400, 0, 0, 0, 0);
        GameCore reader=new GameCore(new Mem(),7190L);
        reader.releaseNotes.show(reader,L);
        check("release notes are available in production",reader.releaseNotes.open);
        reader.screenKey(0);
        check("production release notes keep key taps modal",!reader.starting());
        reader.releaseNotes.close();
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
        check("settings open on player page", c.settingsOpen && c.settingsPage==0);
        c.closeSettings();
        SettingsUi ui = new SettingsUi();
        ui.compute(L, Music.NAMES.length);
        boolean targetsGone = true;
        for (float y = 0; y < L.h; y += 7f) {
            for (float x = 0; x < L.w; x += 11f)
                targetsGone &= ui.hit(x,y) == SettingsUi.HIT_NONE;
        }
        check("production has no developer touch targets", targetsGone);
        c.setSpeed(GameCore.SPEED_MIN);
        c.setBgm(store.bgm);
        boolean roster = c.fullRoster;
        c.setNextRoster(!roster);
        c.resetDifficultyScaling();
        c.setStarDifficulty(0);
        c.tapClearCase(); c.tapClearCase();
        check("settings actions cannot change speed or music", c.speed == 1f
                && c.bgmChoice == Music.defaultChoice(false) && store.speedSaves == 0 && store.bgmSaves == 0);
        check("settings actions cannot change the roster", c.fullRoster == roster && store.rosterSaves == 0);
        check("settings actions cannot reset progression", c.steamer.opens == 5 && c.stars.wins == 3);
        check("settings actions cannot clear collections", c.collected == 1L
                && c.collectionCounts[0] == 7 && store.collectionCounts[0] == 7 && !c.clearArmed);
        check("policy accessible on title", PrivacyUi.hit(c,L,L.w-L.unit,L.dangerY));
        PlayerSettings.open(c);
        check("public settings open in production",c.settingsOpen && c.settingsPage==0);
        SettingsInput.action(c,L,1000+PlayerSettings.DEVELOPER);
        check("developer tab cannot be selected in production",c.settingsPage==0);
        SettingsInput.action(c,L,1000+PlayerSettings.KIDS);
        check("kids preference persists in production",new GameCore(store,71L).preferences.kids);
        c.preferences.kids=false;c.preferences.save(c);c.closeSettings();
        c.startGame();
        check("policy hidden in play", !PrivacyUi.hit(c,L,L.w-L.unit,L.dangerY));
        int stage = c.stage, lives = c.lives;
        c.playtestMode(Power.FLING,L);
        c.playtestStars(L);
        c.playtestSteamer(L);
        c.jumpToStage(20,L);
        c.endCurrentRun();
        check("playtest actions cannot start modes, skip stages or end a run",
                c.state == GameCore.PLAY && c.stage == stage && c.lives == lives && c.mode == -1);
        check("production stage readout is a settings target",L.inStageTap(L.w*.5f,Hud.labelY(L)));
        c.openSettings();
        check("in-run settings open only player page",c.settingsOpen && c.settingsPage==0 && c.state==GameCore.PLAY);
        float clock = c.time;
        c.update(DT,L);
        check("player settings pause production", c.time == clock);
        RasterPainter hidden = new RasterPainter(640,1400,1);
        RasterPainter normal = new RasterPainter(640,1400,1);
        Renderer.draw(hidden,c,L);
        Png.write(new File("out/production-run-settings.png"),hidden.resolve(),640,1400);
        c.closeSettings();
        Renderer.draw(normal,c,L);
        check("player settings render in production", !Arrays.equals(hidden.resolve(),normal.resolve()));
        c.update(DT,L);
        check("closing player settings resumes run",c.time>clock && c.state==GameCore.PLAY);
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
        caveGate(L);
        TestProgress.all(L);
        System.out.printf("%d passed, %d failed%n",pass,fail);
        if (fail > 0) System.exit(1);
    }
    private static void caveGate(Layout L) {
        Mem store=new Mem();store.collected=Collect.MASK;store.caveChoice=CaveDumpling.GOLDEN;
        GameCore c=new GameCore(store,981L);
        c.allLandsEnabled=true;
        check("production exposes only the original lands",LandPicker.count(c)==4);
        check("cave stays locked with all rewards and developer override",!LandPicker.unlocked(c,Cave.LAND));
        c.landSeen=14;LandPicker.updateDiscovery(c,1f);
        check("production never tours the cave",c.landDiscovery<0);
        LandPicker.select(c,Cave.LAND);
        check("production cannot select cave from picker",c.landChoice==0);
        c.landChoice=Cave.LAND;c.startGame();
        check("stale cave selection starts a normal run",c.stage==1 && !Cave.active(c));
        boolean original=true;
        for(int stage=1;stage<=120;stage++)
            original &= Lands.forStage(stage)==(stage-1)/Boss.EVERY%4 && !Cave.stage(stage);
        check("production retains original endless scenery cycle",original);
        c.stage=20;Interlude.enterBonus(c,L);c.bonusTimer=.001f;
        c.update(DT,L);c.update(DT,L);
        check("leaving stage 20 enters ordinary stage 21",c.stage==21 && c.state==GameCore.PLAY && !c.cave.running);
        for(int frame=0;frame<240;frame++)c.update(DT,L);
        check("stage 21 still spawns ordinary enemies",!c.enemies.isEmpty() && c.spawnedThisStage>0);
        c.cave.running=true;
        check("stale cave state cannot enable production cave input",!Cave.active(c)
                && !c.cave.input.down(c,L,1,L.w*.5f,L.playTop+L.w*.4f));
        c.landSeen|=1<<Cave.LAND;LandPicker.save(c);
        GameCore reload=new GameCore(store,982L);
        check("gate preserves saved cave choices and visits",reload.caveChoice==CaveDumpling.GOLDEN
                && (reload.landSeen&(1<<Cave.LAND))!=0);
    }

}
