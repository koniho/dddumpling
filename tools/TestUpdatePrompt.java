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
        storeOffers(L);
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
    private static void storeOffers(Layout L) {
        group("optional store updates");
        Mem save=new Mem();GameCore c=new GameCore(save,210);
        c.storeUpdate.result("38");c.update(.4f,L);
        check("no update offer before first boss",!c.updatePrompt.open);
        c.best=9999;c.stage=25;c.update(.4f,L);
        check("stage or score alone cannot unlock update offers",!c.updatePrompt.open);
        save.collected=Collect.encode(Collect.add(c.collected,Collect.bossPrize(Boss.SLIME)));
        save.landState=2; // The newly unlocked Crystal land has already been introduced.
        c=new GameCore(save,210);
        check("first foreground checks store",c.storeUpdate.beginCheck(c));
        check("repeat foreground check throttled",!c.storeUpdate.beginCheck(c));
        c=new GameCore(save,210);
        check("check cooldown survives restart",!c.storeUpdate.beginCheck(c));
        save.updateTime+=StoreUpdate.CHECK_INTERVAL;
        check("store check resumes after cooldown",c.storeUpdate.beginCheck(c));
        c.storeUpdate.result(null);c.update(.4f,L);
        check("offline or current store result stays silent",!c.updatePrompt.open);
        c.state=GameCore.PLAY;c.storeUpdate.result("38");
        check("store result cannot interrupt play",!c.updatePrompt.update(c,.4f,L));
        c.state=GameCore.TITLE;c.settingsOpen=true;
        check("store offer waits for settings",!c.updatePrompt.update(c,.4f,L));
        c.settingsOpen=false;c.update(.4f,L);
        check("available update uses shared animated title notice",c.updatePrompt.open && c.updatePrompt.storeOffer);
        c.storeUpdate.result(""); // Foreground refresh while the offer is already visible.
        tap(c,L,2);c.update(.4f,L);
        check("later keeps play available and never opens store",!c.updatePrompt.open && !c.storeUpdate.takeRequest());
        c=new GameCore(save,210);c.storeUpdate.result("38");c.update(.4f,L);
        check("later persists across launches",!c.updatePrompt.open);
        save.updateTime+=StoreUpdate.REMIND_INTERVAL;
        c=new GameCore(save,210);c.storeUpdate.result("38");c.update(.4f,L);
        check("same version can remind after seven days",c.updatePrompt.open);
        tap(c,L,1);c.update(.4f,L);
        check("update requests store exactly once after exit",!c.updatePrompt.open && c.storeUpdate.takeRequest() && !c.storeUpdate.takeRequest());
        c.storeUpdate.result("39");c.update(.4f,L);
        check("new store result cannot repeat within session",!c.updatePrompt.open);
        c=new GameCore(save,210);c.storeUpdate.result("39");c.update(.4f,L);
        check("new version is not silenced by older reminder",c.updatePrompt.open);
        Pause.back(c);c.update(.4f,L);
        check("back treats store offer as Later",!c.updatePrompt.open && !c.storeUpdate.takeRequest());
        c=new GameCore(updated(),210);c.storeUpdate.result("40");c.update(.4f,L);
        check("installed news has priority",c.updatePrompt.open && !c.updatePrompt.storeOffer);
        tap(c,L,2);c.update(.4f,L);c.update(.4f,L);
        check("news and store notices never stack",!c.updatePrompt.open);
        save.storeUpdate="1\n38\ninvalid\n12";c=new GameCore(save,210);
        check("malformed reminder doesn't suppress updates",c.storeUpdate.beginCheck(c));
        save.updateTime-=StoreUpdate.REMIND_INTERVAL*2;
        check("clock rollback doesn't permanently suppress checks",c.storeUpdate.beginCheck(c));
        check("store country conversion",StoreUpdate.country("USA").equals("US") && StoreUpdate.country("GBR").equals("GB")
                && StoreUpdate.country(null).isEmpty() && StoreUpdate.country("BAD").isEmpty());
        boolean countries=true;
        for(String country:java.util.Locale.getISOCountries())
            countries&=StoreUpdate.country(new java.util.Locale("",country).getISO3Country()).equals(country);
        check("iOS-safe country map covers every ISO country",countries);
        save=new Mem();c=new GameCore(save,210);c.settingsOpen=true;c.settingsPage=1;
        String history=save.storeUpdate;
        SettingsInput.action(c,L,SettingsUi.HIT_TEST_UPDATE);c.update(.4f,L);
        check("test chip waits until settings closes",!c.updatePrompt.open && c.storeUpdate.preview);
        c.settingsOpen=false;c.update(.4f,L);
        check("test chip bypasses boss gate without unlocking progress",c.updatePrompt.open && !ModeSelector.bossUnlocked(c,Boss.SLIME));
        tap(c,L,1);c.update(.4f,L);
        check("test update opens store without changing reminder history",c.storeUpdate.takeRequest() && save.storeUpdate.equals(history));
        SettingsInput.action(c,L,SettingsUi.HIT_TEST_UPDATE);c.update(.4f,L);
        check("test chip can be repeated without waiting seven days",c.updatePrompt.open);
        tap(c,L,2);c.update(.4f,L);
        check("test later leaves store closed and reminder history alone",!c.storeUpdate.takeRequest() && save.storeUpdate.equals(history));
    }

}
