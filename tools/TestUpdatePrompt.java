package com.dddumpling.game;

final class TestUpdatePrompt extends Check {
    static Mem updated() {
        Mem save=new Mem();save.releaseSeen="old-catalog";
        save.updateState="1\nandroid:0.1.1:2\n0.1.1\n0,0";
        save.appVersion="android:0.1.31:32";
        return save;
    }
    static void tap(GameCore c,Layout L,int choice) {
        float y=UpdatePrompt.buttonY(L,choice);
        c.updatePrompt.touch(c,L,0,7,L.w*.5f,y);
        c.updatePrompt.touch(c,L,1,7,L.w*.5f,y);
    }
    static void run(Layout L) {
        group("update prompt installation history");
        Mem fresh=new Mem();fresh.tutorials=fresh.powerTutorials=0;fresh.releaseSeen="";
        GameCore first=new GameCore(fresh,204);first.update(.4f,L);
        check("fresh install never queues update news",!first.updatePrompt.pending && !first.updatePrompt.open
                && fresh.updateState.contains(fresh.appVersion));
        fresh.appVersion="test:hotfix:2";
        check("fresh install followed by same-catalog hotfix stays quiet",!new GameCore(fresh,204).updatePrompt.pending);
        Mem legacy=new Mem();legacy.best=456;legacy.releaseSeen="";
        GameCore migrated=new GameCore(legacy,204);migrated.update(.4f,L);
        check("legacy progress with no history establishes silent baseline",!migrated.updatePrompt.open && legacy.best==456);
        Mem save=updated();GameCore c=new GameCore(save,204);Ear ear=new Ear();c.sound=ear;
        String seen=save.releaseSeen;
        c.update(.4f,L);
        check("skipped releases yield one modal on the title",c.updatePrompt.open && c.updatePrompt.slide==1f && ear.uiBloops==1);
        c.screenKey(0);c.beginStart();
        check("prompt blocks title controls and score entry",!c.starting() && !HighScoreScreen.entryHit(c,L,L.w*.5f,HighScoreScreen.titleY(L)));
        c.updatePrompt.touch(c,L,0,1,L.w*.5f,UpdatePrompt.buttonY(L,1));
        c.updatePrompt.touch(c,L,3,1,L.w*.5f,UpdatePrompt.buttonY(L,1));
        check("cancelled prompt gesture has no action",c.updatePrompt.pending && !c.releaseNotes.open);
        tap(c,L,2);
        check("skip acknowledges immediately without marking news read",!c.updatePrompt.pending && seen.equals(save.releaseSeen) && ear.uiBloops==2);
        GameCore restart=new GameCore(save,204);restart.update(.4f,L);
        check("restart after skip stays quiet and unread",!restart.updatePrompt.open && restart.releaseMascot.unread);
        save.appVersion="android:0.1.32:33";
        check("build-only update with unchanged catalog does not repeat skipped prompt",!new GameCore(save,204).updatePrompt.pending);
        c.update(.4f,L);c.screenKey(0);
        check("skip restores normal title play",c.starting());

        save=updated();c=new GameCore(save,204);c.update(.4f,L);tap(c,L,1);
        check("read choice acknowledges before exit completes",!new GameCore(save,204).updatePrompt.pending);
        c.update(.4f,L);
        check("read opens newest illustrated release using existing navigation",c.releaseNotes.open && !c.releaseNotes.listing
                && c.releaseNotes.page==0 && c.releaseNotes.feature==0 && save.releaseSeen.equals(ReleaseNotes.VERSIONS[0]));
        save.appVersion="ios:0.1.40:45";
        restart=new GameCore(save,204);restart.update(.4f,L);
        check("same catalog stays read across platform marketing builds",!restart.updatePrompt.open && !restart.releaseMascot.unread);
        save.best=789;save.highScores="keep scores";
        int tutorials=save.tutorials,powers=save.powerTutorials;long collected=save.collected;
        String history=save.updateState.split("\n")[1];seen=save.releaseSeen;
        restart.updatePrompt.rearm(restart);
        check("reset preserves installation and unrelated save state",save.updateState.split("\n")[1].equals(history)
                && save.releaseSeen.equals(seen) && save.best==789 && save.highScores.equals("keep scores")
                && save.tutorials==tutorials && save.powerTutorials==powers && save.collected==collected);
        restart=new GameCore(save,204);restart.update(.4f,L);
        check("developer rearm survives restart and highlights read news",restart.updatePrompt.open && restart.releaseMascot.unread);
        tap(restart,L,2);restart.update(.4f,L);restart=new GameCore(save,204);restart.update(.4f,L);
        check("rearmed skip is once-only but keeps explicit highlight",!restart.updatePrompt.open && restart.releaseMascot.unread && save.releaseSeen.equals(seen));
        restart.updatePrompt.rearm(restart);restart.update(.4f,L);tap(restart,L,1);restart.update(.4f,L);
        check("rearmed read clears forced attention",!restart.updatePrompt.highlight && !restart.releaseMascot.unread);

        group("update prompt overlay ordering");
        for(int blocker=0;blocker<12;blocker++) {
            c=new GameCore(updated(),204);
            switch(blocker) {
                case 0:c.settingsOpen=true;break;
                case 1:c.caseOpen=true;break;
                case 2:c.highScoreScreen.open=true;break;
                case 3:c.releaseNotes.open=true;break;
                case 4:c.starter.open=true;break;
                case 5:c.onboarding.titleGuide=true;break;
                case 6:c.onboarding.briefing=true;break;
                case 7:c.state=GameCore.PLAY;break;
                case 8:c.paused=true;break;
                case 9:c.returnFade=.3f;break;
                case 10:c.homeT=.3f;break;
                case 11:c.modes.transition=.3f;break;
            }
            check("pending update waits for overlay "+blocker,!c.updatePrompt.update(c,.4f,L) && c.updatePrompt.pending);
        }
        c=new GameCore(updated(),204);c.settingsOpen=true;c.update(.4f,L);
        c.settingsOpen=false;c.update(.4f,L);
        check("deferred update appears once settings closes",c.updatePrompt.open);
        c=new GameCore(fresh,204);c.updatePrompt.rearm(c);c.update(.4f,L);
        check("developer reset preserves first squishy priority",c.updatePrompt.pending && !c.updatePrompt.open);
        c.onboarding.saved=Onboarding.SKIPPED;c.onboarding.savedPowers=127;c.update(.4f,L);
        check("developer reset appears after onboarding even on a fresh save",c.updatePrompt.open);
        c=new GameCore(updated(),204);c.update(.4f,L);
        check("back belongs to update modal",Pause.handlesBack(c) && Pause.back(c) && !c.updatePrompt.pending);
        for(int[] dimensions:new int[][]{{320,568},{393,852},{1080,2400}}) {
            Layout box=new Layout();box.compute(dimensions[0],dimensions[1],0,0,0,0);
            check("portrait prompt clears safe top and danger line "+dimensions[0],UpdatePrompt.top(box)>=box.topSafe
                    && UpdatePrompt.top(box)+12.5f*UpdatePrompt.size(box)<box.dangerY);
        }
    }
}
