package com.dddumpling.game;

final class TestLeaderboardNotices extends Check {
    private static GameCore submitted(Mem mem,long score) {
        GameCore c=TestLeaderboardSubmissions.game(mem,"a");
        TestLeaderboardSubmissions.adventure(c,(int)score);c.leaderboards.acknowledge("a",10,score);
        return c;
    }
    static void all(Layout L) {
        group("confirmed leaderboard notices");
        Mem mem=new Mem();GameCore c=submitted(mem,100);
        check("successful submission waits for confirmed rank",c.leaderboards.nextRank()==10 && c.leaderboards.nextNotice()==-1);
        c.leaderboards.confirmRank("a",10,100,90,12,false);
        c.leaderboards.confirmRank("a",10,100,100,-1,false);
        c.leaderboards.confirmRank("other",10,100,100,12,false);
        check("stale score, unranked and wrong owner stay pending",c.leaderboards.nextNotice()==-1 && c.leaderboards.nextRank()==10);
        c=TestLeaderboardSubmissions.game(mem,"a");
        check("rank lookup survives restart",c.leaderboards.nextRank()==10);
        c.leaderboards.confirmRank("a",10,100,100,12,false);
        check("confirmed rank queues once and stops reads",c.leaderboards.nextNotice()==10 && c.leaderboards.nextRank()==-1);
        check("undelivered notice remains pending across restart",TestLeaderboardSubmissions.game(mem,"a").leaderboards.nextRank()==10);
        c.leaderboardNotice.update(c,.1f);
        check("notification waits during gameplay",c.leaderboardNotice.board<0);
        c.toTitle();c.returnFade=0;c.rosterSceneT=0;c.landSeen=-1;c.onboarding.clear();
        c.leaderboardNotice.update(c,.1f);
        check("safe title hands off confirmed board and rank",c.leaderboardNotice.board==10 && c.leaderboardNotice.rank==12
                && c.leaderboardNotice.takeSystem(c)==10 && c.leaderboardNotice.takeSystem(c)==-1);
        c.leaderboardNotice.systemResult(c,"a",10,12,false);
        c.leaderboardNotice.update(c,0);
        check("denied system permission retains fallback",c.leaderboardNotice.board==10 && !c.leaderboardNotice.systemWaiting);
        c.leaderboards.confirmRank("a",10,100,100,12,false);
        check("duplicate rank callbacks cannot notify again",c.leaderboards.nextNotice()==-1);
        check("shown notification survives restart",TestLeaderboardSubmissions.game(mem,"a").leaderboards.nextRank()==-1);
        c.state=GameCore.PLAY;c.leaderboardNotice.update(c,1);
        check("fallback hidden during gameplay without expiring",!LeaderboardNotice.safe(c) && c.leaderboardNotice.age==0);
        c.state=GameCore.TITLE;
        for(int i=0;i<40;i++)c.leaderboardNotice.update(c,.1f);
        check("fallback disappears automatically",c.leaderboardNotice.board==-1);
        TestLeaderboardSubmissions.adventure(c,200);c.leaderboards.acknowledge("a",10,200);
        c.leaderboards.confirmRank("a",10,100,200,5,false);
        check("old submission callback cannot claim latest rank",c.leaderboards.nextNotice()==-1);
        c.leaderboards.confirmRank("a",10,200,200,5,false);
        c.leaderboards.account("b",true);
        check("account switch removes another player's notice",c.leaderboards.nextNotice()==-1);
        c.leaderboards.account("a",true);
        check("original owner can retry confirmation",c.leaderboards.nextRank()==10);
        c.leaderboards.confirmRank("a",10,200,200,5,false);c.toTitle();c.returnFade=0;c.rosterSceneT=0;c.landSeen=-1;
        c.leaderboardNotice.update(c,.1f);c.leaderboardNotice.takeSystem(c);
        c.leaderboardNotice.systemResult(c,"a",10,5,true);
        check("system delivery suppresses duplicate in-game banner",c.leaderboardNotice.board==-1);
        Mem old=new Mem();GameCore legacy=submitted(old,100);
        String[] rows=old.leaderboardSubmissions.split(";",-1);rows[0]="1";
        for(int i=2;i<rows.length;i++)rows[i]=String.join(",",java.util.Arrays.copyOf(rows[i].split(","),23));
        old.leaderboardSubmissions=String.join(";",rows);
        legacy=TestLeaderboardSubmissions.game(old,"a");
        check("old acknowledged history is not announced on upgrade",legacy.leaderboards.nextRank()==-1);
        GameCore ta=TestLeaderboardSubmissions.game(new Mem(),"a");ta.modes.selected=ModeSelector.TIME_ATTACK;ta.fullRoster=true;
        ta.startGame();ta.timeAttack.seconds=1.001;ta.timeAttack.cleared=1;ta.timeAttack.finish(ta,true);
        ta.leaderboards.acknowledge("a",3,1001);
        ta.leaderboards.confirmRank("a",3,1001,102,3,true);
        check("slower stale time-attack entry waits",ta.leaderboards.nextNotice()==-1);
        ta.leaderboards.confirmRank("a",3,1001,101,3,true);
        check("Apple centiseconds validate against converted time",ta.leaderboards.nextNotice()==3);
        notificationTap(L);
    }
    private static void notificationTap(Layout L) {
        GameCore c=TestLeaderboardSubmissions.game(new Mem(),"a");c.leaderboardUi.enabled=true;
        c.startGame();c.leaderboardUi.notification(2,"a");c.leaderboardUi.update(c);
        check("system tap never interrupts active run",!c.preferences.externalBusy);
        c.toTitle();c.returnFade=0;c.rosterSceneT=0;c.landSeen=-1;c.onboarding.clear();c.leaderboardUi.update(c);c.clock+=.2f;
        check("system tap opens captured board from title",c.preferences.takeExternal(c)==PlayerSettings.LEADERBOARD
                && c.leaderboardUi.take(c)==2);
        c.preferences.externalFinished(c);c.clock+=1;
        c.leaderboardUi.notification(10,"other");c.leaderboardUi.update(c);
        check("notification from another account cannot open",!c.preferences.externalBusy);
    }
}
