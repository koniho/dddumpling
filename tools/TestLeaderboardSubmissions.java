package com.dddumpling.game;

final class TestLeaderboardSubmissions extends Check {
    static GameCore game(Mem store,String owner) {
        GameCore c=new GameCore(store,237,true);c.collected=-1L;c.leaderboards.account(owner,true);return c;
    }
    static void adventure(GameCore c,int score) {c.startGame();c.score=score;c.highScores.finish(c);}
    static void all(Layout L) {
        TestLeaderboardNotices.all(L);
        group("leaderboard catalog and completed runs");
        java.util.Set<String> apple=new java.util.HashSet<>(),google=new java.util.HashSet<>();
        for(int b=0;b<11;b++) {apple.add(LeaderboardCatalog.appleId(b));google.add(LeaderboardCatalog.googleId(b));}
        check("all 11 platform IDs unique",apple.size()==11 && google.size()==11);
        check("units never improve a player's measured time",LeaderboardCatalog.appleScore(0,1999)==1
                && LeaderboardCatalog.appleScore(3,1001)==101 && LeaderboardCatalog.googleScore(3,1001)==1001);
        check("large unit conversion cannot overflow",LeaderboardCatalog.appleScore(3,Long.MAX_VALUE)>0);
        check("only six-key non-Kids time attack ranks",LeaderboardCatalog.timeAttack(0,0)==-1
                && LeaderboardCatalog.timeAttack(2,0)==-1 && LeaderboardCatalog.timeAttack(1,6)==9);
        Mem store=new Mem();GameCore c=game(store,"account;a,1");
        adventure(c,100);check("fresh adventure completion queues",c.leaderboards.next()==10 && c.leaderboards.value(10)==100);
        adventure(c,90);check("lower adventure score cannot replace pending best",c.leaderboards.value(10)==100);
        c.leaderboards.acknowledge("other",10,100);check("wrong account ack ignored",c.leaderboards.next()==10);
        adventure(c,200);c.leaderboards.acknowledge("account;a,1",10,100);
        check("late ack retains newer better score",c.leaderboards.next()==10 && c.leaderboards.value(10)==200);
        c=game(store,"account;a,1");check("unacknowledged best survives restart",c.leaderboards.next()==10 && c.leaderboards.value(10)==200);
        c.leaderboards.acknowledge("account;a,1",10,200);c.leaderboards.acknowledge("account;a,1",10,200);
        check("duplicate success ack is harmless",c.leaderboards.next()==-1);
        c=game(store,"account;a,1");check("ack survives restart",c.leaderboards.next()==-1);
        c.startGame();c.score=300;c.leaderboards.account("account-b",true);c.highScores.finish(c);
        check("switching account cannot inherit an active run",c.leaderboards.next()==-1);
        c.leaderboards.account("account;a,1",true);check("captured owner keeps their run",c.leaderboards.value(10)==300);
        c.leaderboards.account(null,true);adventure(c,400);check("offline is not authenticated",c.leaderboards.next()==-1);
        c.leaderboards.account("account;a,1",true);check("offline run keeps last proven owner",c.leaderboards.value(10)==400);
        GameCore unknown=game(new Mem(),null);adventure(unknown,900);unknown.leaderboards.account("new",true);
        check("pre-authentication history is never assigned later",unknown.leaderboards.next()==-1);
        Mem legacy=new Mem();legacy.highScores=store.highScores;legacy.survival=store.survival;legacy.timeAttack=store.timeAttack;
        check("restored historical records are never backfilled",game(legacy,"new").leaderboards.next()==-1);
        GameCore kids=game(new Mem(),"a");kids.preferences.kids=true;adventure(kids,100);
        check("Kids adventure excluded",kids.leaderboards.next()==-1);
        GameCore retry=game(new Mem(),"a");retry.startGame();retry.score=100;retry.highScores.bossRetries=1;retry.highScores.finish(retry);
        check("retried adventure excluded but local run saved",retry.leaderboards.next()==-1 && retry.highScores.latestRun.bossRetries==1);
        GameCore checkpoint=game(new Mem(),"a");checkpoint.startGame();checkpoint.runStartLand=1;
        checkpoint.leaderboards.begin(checkpoint);checkpoint.score=100;checkpoint.highScores.finish(checkpoint);
        check("checkpoint adventure excluded",checkpoint.leaderboards.next()==-1);
        for(int profile=0;profile<3;profile++) {
            GameCore s=game(new Mem(),"a");s.modes.selected=ModeSelector.SURVIVAL;
            s.preferences.kids=profile==2;s.fullRoster=profile==1;s.startGame();s.survival.seconds=12.345;
            s.preferences.kids=!s.preferences.kids;s.fullRoster=!s.fullRoster;s.survival.finish(s);
            check("Survival uses captured profile "+profile,s.leaderboards.next()==profile && s.leaderboards.value(profile)==12345);
        }
        for(int challenge=0;challenge<7;challenge++) {
            GameCore t=game(new Mem(),"a");t.modes.selected=ModeSelector.TIME_ATTACK;t.fullRoster=true;
            t.timeAttack.selected=challenge;t.startGame();t.timeAttack.seconds=12.345;t.timeAttack.cleared=challenge==6?6:1;
            t.timeAttack.selected=0;t.timeAttack.finish(t,true);
            check("successful TA captures challenge "+challenge,t.leaderboards.next()==3+challenge && t.leaderboards.value(3+challenge)==12345);
        }
        GameCore shortRun=game(new Mem(),"a");shortRun.modes.selected=ModeSelector.SURVIVAL;shortRun.fullRoster=false;
        shortRun.startGame();shortRun.survival.seconds=.5;shortRun.survival.finish(shortRun);
        shortRun.survival.leave(shortRun);shortRun.modes.selected=ModeSelector.ADVENTURE;adventure(shortRun,100);
        check("subsecond Survival remains eligible in Google milliseconds",shortRun.leaderboards.next()==0);
        check("zero Apple seconds cannot starve other boards",shortRun.leaderboards.nextApple()==10);
        GameCore faster=game(new Mem(),"a");faster.modes.selected=ModeSelector.TIME_ATTACK;faster.fullRoster=true;
        faster.startGame();faster.timeAttack.seconds=12;faster.timeAttack.finish(faster,true);
        faster.startGame();faster.timeAttack.seconds=10;faster.timeAttack.finish(faster,true);
        faster.leaderboards.acknowledge("a",3,12000);
        check("late slow-time ack retains faster pending clear",faster.leaderboards.next()==3 && faster.leaderboards.value(3)==10000);
        faster.startGame();faster.timeAttack.seconds=15;faster.timeAttack.finish(faster,true);
        check("slower later clear cannot replace faster best",faster.leaderboards.value(3)==10000);
        faster.leaderboards.acknowledge("a",3,10000);
        check("fastest clear success completes pending submission",faster.leaderboards.next()==-1);
        GameCore zero=game(new Mem(),"a");zero.modes.selected=ModeSelector.TIME_ATTACK;zero.fullRoster=true;
        zero.startGame();zero.timeAttack.finish(zero,true);
        check("zero measured clear never submits despite display clamp",zero.leaderboards.next()==-1);
        GameCore fail=game(new Mem(),"a");fail.modes.selected=ModeSelector.TIME_ATTACK;fail.fullRoster=true;
        fail.startGame();fail.timeAttack.seconds=5;fail.timeAttack.finish(fail,false);
        check("failed Time Attack never submits",fail.leaderboards.next()==-1);
        GameCore incomplete=game(new Mem(),"a");incomplete.modes.selected=ModeSelector.TIME_ATTACK;incomplete.fullRoster=true;
        incomplete.timeAttack.selected=6;incomplete.startGame();incomplete.timeAttack.seconds=5;incomplete.timeAttack.cleared=5;incomplete.timeAttack.finish(incomplete,true);
        check("old five-boss roster cannot seed all-six board",incomplete.leaderboards.next()==-1);
        GameCore synthetic=new GameCore(new Mem(),237,false);synthetic.leaderboards.account("a",true);adventure(synthetic,900);
        check("synthetic core cannot capture ranked run",synthetic.leaderboards.next()==-1);
        GameCore suppressed=game(new Mem(),"a");suppressed.startGame();suppressed.score=900;suppressed.scoresSuppressed=true;suppressed.highScores.finish(suppressed);
        check("suppressed scores never submit",suppressed.leaderboards.next()==-1);
        GameCore disabled=game(new Mem(),"a");disabled.leaderboards.account("a",false);adventure(disabled,900);
        check("disabled native service never queues",disabled.leaderboards.next()==-1);
        Mem broken=new Mem();GameCore unhealthy=game(broken,"a");broken.leaderboardSaveFails=true;adventure(unhealthy,900);
        check("failed durable save blocks submission",unhealthy.leaderboards.next()==-1);
        Mem corrupt=new Mem();corrupt.leaderboardSubmissions="1;bad;truncated";
        check("corrupt queue fails closed",game(corrupt,"a").leaderboards.next()==-1);
    }
}
