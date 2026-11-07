package com.dddumpling.game;

final class TestLeaderboardUi extends Check {
    static void all(Layout L) {
        group("leaderboard navigation");
        GameCore c=new GameCore(new Mem(),237L);
        c.highScoreScreen.show(c);c.highScoreScreen.update(HighScoreScreen.ENTRY_TIME);
        LeaderboardUi ui=c.leaderboardUi;
        ui.request(c);
        check("unavailable service keeps local records usable",!c.preferences.externalBusy && ui.take(c)==-1);
        ui.enabled=true;
        check("adventure board",LeaderboardUi.board(c)==LeaderboardCatalog.ADVENTURE);
        c.preferences.kids=true;
        check("kids adventure remains local",LeaderboardUi.board(c)==-1 && !ui.canOpen(c));
        c.modes.selected=ModeSelector.SURVIVAL;
        check("kids survival board",LeaderboardUi.board(c)==LeaderboardCatalog.SURVIVAL_KIDS);
        c.preferences.kids=false;c.fullRoster=false;
        check("four-key survival board",LeaderboardUi.board(c)==LeaderboardCatalog.SURVIVAL_STARTER);
        c.fullRoster=true;
        check("six-key survival board",LeaderboardUi.board(c)==LeaderboardCatalog.SURVIVAL_FULL);
        c.modes.selected=ModeSelector.TIME_ATTACK;
        for(int i=0;i<=TimeAttack.ALL;i++) {
            c.timeAttack.selected=i;
            check("time attack challenge board "+i,LeaderboardUi.board(c)==LeaderboardCatalog.TIME_ATTACK_BASE+i);
        }
        c.fullRoster=false;
        check("four-key time attack remains local",LeaderboardUi.board(c)==-1);
        c.fullRoster=true;c.preferences.kids=true;
        check("kids time attack remains local",LeaderboardUi.board(c)==-1);
        c.preferences.kids=false;c.timeAttack.selected=0;
        String records=c.highScores.encode();
        SettingsInput input=new SettingsInput();float x=L.w*.5f,y=LeaderboardUi.y(L);
        input.touch(c,L,0,5,x,y);input.touch(c,L,3,5,x,y);input.touch(c,L,1,5,x,y);
        check("cancelled leaderboard tap cannot launch",!c.preferences.externalBusy);
        input.touch(c,L,0,6,x,y);input.touch(c,L,1,6,x,y);
        c.clock+=.2f;
        check("leaderboard tap queues native action",c.preferences.externalBusy
                && c.preferences.takeExternal(c)==PlayerSettings.LEADERBOARD);
        c.timeAttack.selected=TimeAttack.ALL;ui.request(c);
        check("request retains original challenge and blocks duplicate",ui.take(c)==LeaderboardCatalog.TIME_ATTACK_BASE);
        c.preferences.externalFinished(c);
        check("browsing never edits local records",records.equals(c.highScores.encode()));
        c.clock+=.6f;ui.request(c);c.highScoreScreen.closing=true;
        check("closing records cancels pending presentation",ui.take(c)==-1 && !c.preferences.externalBusy);
        c.highScoreScreen.closing=false;c.preferences.externalAction=0;
        c.highScoreScreen.selected=0;
        check("leaderboard accessible from summary",c.highScoreScreen.hit(c,L,x,y)==HighScoreScreen.LEADERBOARD);
        for(int[] dims:new int[][]{{320,568},{393,852},{1080,2400}}) {
            Layout l=new Layout();l.compute(dims[0],dims[1],0,24,0,24);
            check("footer does not overlap records "+dims[0],
                    LeaderboardUi.y(l)-HighScoreScreen.size(l)*1.05f>HighScoreScreen.listBottom(l));
        }
    }
}
