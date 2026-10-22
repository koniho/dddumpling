package com.dddumpling.game;

final class TestCloudSaveUi extends Check {
    static void all(Layout L) {
        group("cloud save introduction");
        Mem store=new Mem();GameCore c=new GameCore(store,75);
        CloudSaveUi ui=c.cloudUi;
        ui.collected(Collect.STAR_FIRST);
        check("disabled cloud has no introduction",ui.pending<0 && !ui.update(c,.1f));
        ui.status(CloudSaveUi.DRIVE);
        check("existing collection does not prompt at launch",!ui.update(c,.1f));
        ui.collected(Collect.STAR_FIRST+1);c.state=GameCore.BONUS;
        check("reward celebration is not interrupted",!ui.update(c,.1f));
        c.state=GameCore.TITLE;c.releaseNotes.open=false;
        check("safe boundary opens actual collected character",ui.update(c,.5f) && ui.character==Collect.STAR_FIRST+1);
        ui.age=1f;
        float x=L.w*.5f,y=CloudSaveUi.top(L)+CloudSaveUi.unit(L)*27f;
        ui.touch(c,L,0,7,x,y);ui.touch(c,L,1,8,x,y);
        check("other pointer cannot dismiss",ui.open);
        ui.touch(c,L,1,7,x,y);
        check("not now closes and persists",!ui.open && store.cloudIntro);
        GameCore again=new GameCore(store,76);again.cloudUi.status(CloudSaveUi.DRIVE);again.cloudUi.collected(0);
        check("dismissal survives relaunch",again.cloudUi.seen && again.cloudUi.pending<0);
        GameCore ready=new GameCore(new Mem(),77);ready.cloudUi.status(CloudSaveUi.SAVED);ready.cloudUi.collected(0);
        check("available cloud suppresses intro",!ready.cloudUi.update(ready,.1f) && ready.cloudUi.seen);
        ui.status(CloudSaveUi.FAILED);c.state=GameCore.PLAY;
        check("failure notice never overlays gameplay",!ui.noticeVisible(c));
        c.state=GameCore.TITLE;
        check("failure notice at title",ui.noticeVisible(c));
        ui.show(false);ui.close(c);
        check("status panel does not reintroduce onboarding",!ui.open && ui.seen);
        ui.status(CloudSaveUi.FAILED);
        check("viewed failure stays quiet for this session",!ui.noticeVisible(c));
        ui.failureShown=false;ui.status(CloudSaveUi.PENDING);
        check("pending stays in settings",!ui.noticeVisible(c));
        ui.status(CloudSaveUi.ACCOUNT);
        float noticeY=L.topSafe+CloudSaveUi.unit(L)*2;
        ui.noticeTouch(c,L,0,7,x,noticeY);ui.noticeTouch(c,L,1,7,x,noticeY);
        check("failure notice opens the status panel",ui.open && !ui.introduction && !ui.notice);
        ui.close(c);
        check("callback never promises transfer",!ui.label().equals("SYNCED"));
        ui.playGames=true;ui.status(CloudSaveUi.SIGN_IN);ui.show(false);
        check("Android panel explains Play Games sign-in",ui.summary().contains("Play Games") && !ui.summary().contains("iCloud"));
        ui.choose(c,1);
        check("Android sign-in requests native action once",ui.takeAction()==1 && ui.takeAction()==0);
        for(int state:new int[]{CloudSaveUi.PENDING,CloudSaveUi.SAVED,CloudSaveUi.FAILED,CloudSaveUi.ACCOUNT}) {
            ui.status(state);
            check("Android cloud state has no Apple instructions "+state,!ui.summary().contains("iCloud") && !ui.summary().contains("Game Center"));
        }
        ui.status(CloudSaveUi.SAVED);
        check("Android accepted save shows connection without claiming server delivery",
                ui.shortLabel().equals("CONNECTED") && ui.label().equals("PLAY GAMES CONNECTED")
                && ui.summary().contains("background when online"));
        c.settingsOpen=true;c.settingsPage=PlayerSettings.SERVICES_PAGE;
        ui.show(false);
        for(int frame=0;frame<4;frame++)ui.update(c,.1f);
        check("manual cloud panel keeps ownership while its entrance advances",ui.open && ui.age>.35f);
        y=CloudSaveUi.top(L)+CloudSaveUi.unit(L)*23.5f;
        ui.touch(c,L,0,9,x,y);ui.touch(c,L,1,9,x,y);
        check("done returns to progress settings",!ui.open && c.settingsOpen && c.settingsPage==PlayerSettings.SERVICES_PAGE);
        ui.show(false);
        check("system back closes cloud before underlying settings",Pause.handlesBack(c) && Pause.back(c) && !ui.open && c.settingsOpen);
        c.settingsOpen=false;ui.show(false);
        check("cloud panel alone owns system back at title",Pause.handlesBack(c) && Pause.back(c) && !ui.open);
        ui.playGames=false;
        check("Apple accepted save keeps pending wording",ui.shortLabel().equals("SYNC PENDING") && ui.label().equals("CLOUD SAVE PENDING"));
    }
}
