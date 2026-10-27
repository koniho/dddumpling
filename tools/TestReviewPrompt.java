package com.dddumpling.game;

final class TestReviewPrompt extends Check {
    private static GameCore ready(Mem save) {
        save.collected=Collect.encode(Collect.add(0,Collect.bossPrize(Boss.SLIME)));save.landState=2;
        GameCore c=new GameCore(save,213);c.reviewPrompt.available=true;
        return c;
    }
    static void run(Layout L) {
        group("native review timing");
        Mem save=new Mem();save.appVersion="android:0.1.36:38";
        GameCore c=ready(save);ReviewPrompt r=c.reviewPrompt;
        c.startGame();
        check("playing records one session",r.sessions==1 && r.days==1);
        r.play(c);save.updateTime+=1000;r.play(c);
        check("quick restarts do not inflate sessions",r.sessions==1);
        save.updateTime+=ReviewPrompt.SESSION_GAP;r.play(c);
        check("second session alone is insufficient",!r.eligible(c));
        save.updateTime+=ReviewPrompt.DAY;r.play(c);
        check("three sessions over two days qualify",r.sessions==3 && r.days==2 && r.eligible(c));
        c=ready(save);r=c.reviewPrompt;
        check("play history survives app restart",r.sessions==3 && r.days==2 && r.eligible(c));
        check("no review without a fresh milestone",r.takeRequest(c)==0);
        r.milestone(c);c.state=GameCore.PLAY;
        check("milestone waits for gameplay to finish",r.takeRequest(c)==0 && r.pending);
        c.state=GameCore.TITLE;c.settingsOpen=true;
        check("review waits for settings",r.takeRequest(c)==0);
        c.settingsOpen=false;c.highScoreScreen.open=true;
        check("review waits for summary",r.takeRequest(c)==0);
        c.highScoreScreen.open=false;c.onboarding.briefing=true;
        check("review waits for tutorials",r.takeRequest(c)==0);
        c.onboarding.briefing=false;c.updatePrompt.pending=true;
        check("news takes precedence",r.takeRequest(c)==0);
        c.updatePrompt.pending=false;c.storeUpdate.shown=true;
        check("review never stacks after another notice",r.takeRequest(c)==0);
        c.storeUpdate.shown=false;
        check("eligible milestone requests native flow once",r.takeRequest(c)==1 && r.takeRequest(c)==0);
        check("review reserves the session notice slot",c.storeUpdate.shown);
        c=ready(save);r=c.reviewPrompt;r.milestone(c);
        check("attempt persists even if store suppressed sheet",!r.eligible(c) && r.takeRequest(c)==0);
        save.appVersion="android:0.1.37:39";
        check("new version does not bypass cooldown",!r.eligible(c));
        save.updateTime-=ReviewPrompt.DAY;
        check("clock rollback cannot bypass cooldown",!r.eligible(c));
        save.updateTime=r.lastAttempt+ReviewPrompt.COOLDOWN;
        check("new version qualifies after ninety days",r.eligible(c));
        save.appVersion="android:0.1.36:999";
        check("build-only update cannot repeat same marketing version",!r.eligible(c));
        save.appVersion="android:0.1.37:39";c.collected=0;
        check("first boss is required even with enough sessions",!r.eligible(c));
        c=ready(save);c.startGame();c.reviewPrompt.pending=true;c.lives=1;c.takeHit(L.w*.5f,L);
        check("defeat cancels an earlier victory request",!c.reviewPrompt.pending);

        c=ready(new Mem());r=c.reviewPrompt;
        SettingsUi ui=new SettingsUi();ui.compute(L,SettingsUi.PROGRESS);
        if(BuildFlags.DEVELOPER) {
            check("test review chip has its own target",ui.hit((ui.testChipL(0,2)+ui.testChipR(0,2))*.5f,ui.reviewPromptY+ui.testH*.5f)==SettingsUi.HIT_TEST_REVIEW);
            check("reset review chip has its own target",ui.hit((ui.testChipL(1,2)+ui.testChipR(1,2))*.5f,ui.reviewPromptY+ui.testH*.5f)==SettingsUi.HIT_RESET_REVIEW);
            Mem dev=(Mem)c.store;String history=dev.reviewPrompt;
            c.settingsOpen=true;SettingsInput.action(c,L,SettingsUi.HIT_TEST_REVIEW);
            check("developer request waits for settings exit",r.takeRequest(c)==0 && r.preview);
            c.settingsOpen=false;
            check("developer preview bypasses thresholds exactly once",r.takeRequest(c)==2 && r.takeRequest(c)==0);
            check("preview does not change real attempt history",history.equals(dev.reviewPrompt));
            r.play(c);r.lastAttempt=dev.updateTime;
            SettingsInput.action(c,L,SettingsUi.HIT_RESET_REVIEW);
            check("developer reset clears only review state",r.sessions==0 && r.lastAttempt==0 && ModeSelector.bossUnlocked(c,Boss.SLIME));
        } else {
            r.sessions=3;r.test();r.reset(c);
            check("production cannot preview or reset review history",!r.preview && r.sessions==3);
        }
        Mem damaged=new Mem();damaged.reviewPrompt="1\n3\n2\nbad\n0\n0\nversion\nend";
        check("malformed review history is ignored",new GameCore(damaged,213).reviewPrompt.sessions==0);
    }
}
