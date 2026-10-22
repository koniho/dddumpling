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
    }
}
