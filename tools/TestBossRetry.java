package com.dddumpling.game;

final class TestBossRetry extends Check {
    static GameCore fight(Layout L,int kind) {
        GameCore c=new GameCore(new Mem(),206,true);c.startGame();c.score=100;
        c.enterStage(Lands.firstStage(WorldCatalog.BOSSES[kind].land)+Boss.EVERY-1);
        c.boss.intro=0;c.boss.update(0,L,c.rnd);c.stageBanner=c.stageGap=0;
        return c;
    }
    static void lose(GameCore c,Layout L) {
        c.lives=1;c.takeHit(L.w*.5f,L);
    }
    static void ready(GameCore c,Layout L) {
        for(int frame=0;frame<600 && !c.overReady();frame++)c.update(DT,L);
    }
    static void all(Layout L) {
        group("Adventure boss rematch preserves the run and defers its result");
        for(int kind=0;kind<Boss.COUNT;kind++) {
            GameCore c=fight(L,kind);Ear ear=new Ear();c.sound=ear;
            c.highScores.prize(0);c.roundPrizes=1;c.score=170;c.hits=7;c.misses=2;
            int stage=c.stage,who=c.runWho;Boss previous=c.boss;
            lose(c,L);
            check("boss loss retains its visual arena without live combat "+kind,
                    BossVictory.sceneBoss(c)==previous && !c.boss.active() && BossVictory.backdropDrain(c)==0);
            check("specialized arena survives the boss winning "+kind,
                    kind==Boss.SLIME?SlimeBasin.progress(c)==1:kind==Boss.MUSHROOM?MushroomForest.groveProgress(c)==1
                    :kind!=Boss.SPLITTER || CrystalArena.active(c));
            check("eligible defeat waits for a decision "+kind,c.bossRetry.pending && c.highScores.latest==0 && c.state==GameCore.OVER);
            c.bossRetry.retry(c);c.bossRetry.end(c);
            check("death animation cannot be skipped "+kind,c.highScores.bossRetries==0 && c.bossRetry.pending);
            ready(c,L);c.tapGameOver(0,0,L);
            check("retry panel keeps the same arena palette "+kind,BossVictory.backdropDrain(c)==0 && BossVictory.sceneBoss(c)==previous);
            check("outside taps do not discard a retry choice "+kind,c.bossRetry.pending && c.returnFade==0);
            c.tapGameOver(L.w*.5f,BossRetry.buttonY(L,true),L);
            check("retry resets the same encounter at full health "+kind,c.state==GameCore.PLAY && c.boss.kind==kind
                    && c.boss!=previous && c.boss.hp==c.boss.hpMax && c.boss.intro>0 && c.lives==GameCore.START_LIVES);
            check("retry rolls back failed score and keeps prior progress "+kind,c.score==100 && c.stage==stage
                    && c.runWho==who && c.roundPrizes==1 && c.highScores.prizes.size()==1 && c.hits==7 && c.misses==2);
            check("retry counts exactly once and releases loss state "+kind,c.highScores.bossRetries==1 && !c.bossRetry.pending
                    && c.bossVictory==null && c.deathT==0 && c.highScores.latest==0 && c.enemies.isEmpty() && c.shots.isEmpty());
            check("retry releases the previous arena snapshot "+kind,!BossVictory.retained(c) && BossVictory.sceneBoss(c)==c.boss);
            c.bossRetry.retry(c);check("repeat activation is ignored "+kind,c.highScores.bossRetries==1);
            c.boss.intro=0;c.score=150;lose(c,L);ready(c,L);c.bossRetry.end(c);
            check("ending commits a single run with retries "+kind,!c.bossRetry.pending && c.highScores.latest==1
                    && c.highScores.latestRun.bossRetries==1 && c.highScores.latestRun.score==150 && c.returnFade==0 && c.overFade()>.999f && !c.overReady());
            c.bossRetry.end(c);c.toTitle();check("summary and title cannot record twice "+kind,c.highScores.latest==1);
            check("title releases arena retention "+kind,!BossVictory.retained(c));
            HighScores saved=new HighScores();saved.load(((Mem)c.store).highScores);
            check("retry flag survives a saved score reload "+kind,saved.latestRun!=null && saved.latestRun.bossRetries==1);
            c.startGame();check("new Adventure run clears retries "+kind,c.highScores.bossRetries==0 && !c.bossRetry.pending);
        }
        group("Retry accounting across encounters and exit routes");
        GameCore c=fight(L,Boss.SLIME);
        for(int i=0;i<2;i++){lose(c,L);ready(c,L);c.screenKey(0);c.boss.intro=0;}
        c.boss.beaten=true;BossPlay.endBoss(c,L);c.bossPrizePending=false;
        c.enterStage(10);c.boss.intro=0;lose(c,L);ready(c,L);c.bossRetry.retry(c);c.boss.intro=0;
        lose(c,L);ready(c,L);Pause.back(c);
        check("two retries then another boss retry total three",c.highScores.latestRun!=null && c.highScores.latestRun.bossRetries==3);
        check("winning a retried boss awards its collection entry once",c.highScores.latestRun.dumplings==1);
        c=fight(L,Boss.SLIME);lose(c,L);ready(c,L);c.bossRetry.end(c);
        check("declining without retries records zero",c.highScores.latestRun.bossRetries==0);
        c=fight(L,Boss.SLIME);lose(c,L);c.toTitle();
        check("direct title return finalizes pending loss",c.highScores.latest==1 && !c.bossRetry.pending);
        c=fight(L,Boss.SLIME);c.survival.active=true;
        check("Survival never offers boss retries",!c.bossRetry.offer(c));
        c.survival.active=false;c.timeAttack.active=true;
        check("Time Attack never offers boss retries",!c.bossRetry.offer(c));
        c=fight(L,Boss.SLIME);c.enterStage(6);lose(c,L);
        check("ordinary wave death still finalizes immediately",!c.bossRetry.pending && c.highScores.latest==1);
        group("High-score retry format is compatible and keeps ranking");
        c=fight(L,Boss.SLIME);c.highScores.finish(c);String old=c.highScores.encode();
        HighScores saved=new HighScores();saved.load(old);
        check("legacy history loads without a retry flag",old.startsWith("10:") && saved.latestRun.bossRetries==0);
        c.startGame();c.score=50;c.highScores.bossRetries=3;c.highScores.bosses=1<<(Boss.COUNT-1);c.highScores.finish(c);
        String modern=c.highScores.encode();saved.load(modern);
        check("retry version preserves scores and boss bitmask",modern.startsWith("12:") && saved.runs.size()==2
                && saved.runs.get(0).score==100 && saved.latestRun.bossRetries==3 && saved.latestRun.bosses==(1<<(Boss.COUNT-1)));
        check("retry format roundtrips exactly",modern.equals(saved.encode()));
        saved.load(modern.substring(0,modern.lastIndexOf(',')));
        check("truncated retry record rejected safely",saved.runs.isEmpty());
    }
}
