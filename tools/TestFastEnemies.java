package com.dddumpling.game;

final class TestFastEnemies extends Check {
    static GameCore scene(Layout L,int stage,long seed) {
        GameCore c=new GameCore(new Mem(),seed);c.startGame();c.jumpToStage(stage,L);
        c.onboarding.saved=Onboarding.SKIPPED;c.onboarding.clear();
        c.stageGap=c.stageBanner=0;c.landBlend=1;c.powerTimer=Float.POSITIVE_INFINITY;
        c.spawnTimer=Float.POSITIVE_INFINITY;return c;
    }
    static GameCore.Enemy runner(GameCore c,Layout L,float y) {
        GameCore.Enemy e=add(c,L,new int[]{0,1,4},new int[]{3,2,1},y);
        c.fastEnemyNext=true;e.phase=5.5f;FastEnemies.assign(c,e);e.enterT=1;
        e.speed=2*(L.dangerY+L.enemyR*2.2f)/c.travelSeconds();return e;
    }
    static void all(Layout L) {
        group("fast single-key enemies");
        for(int stage:new int[]{1,15,16,17,20,21,24,25,46,80,81}) {
            GameCore c=scene(L,stage,180);c.spawnedThisStage=2;
            GameCore.Enemy e=runner(c,L,L.h*.35f);
            boolean expected=stage>=16 && !Boss.isBossStage(stage);
            check("runner stage eligibility "+stage,e.fastSingle==expected);
            if(expected)check("runner is exactly one unstacked press "+stage,
                    e.word.length==1 && e.totalPresses()==1 && e.need[0]==1 && e.word[0]==0);
        }
        for(int slot=0;slot<8;slot++) {
            GameCore c=scene(L,16,180);c.spawnedThisStage=slot;
            check("stage 16 reserves only the third spawn for runner "+slot,runner(c,L,L.h*.3f).fastSingle==(slot==2));
        }
        GameCore c=scene(L,17,180);
        for(int gate=0;gate<4;gate++) {
            c.survival.active=gate==0;c.timeAttack.active=gate==1;
            c.mode=gate==2?Power.NINJA:-1;c.modeLeft=gate==2?5:0;
            GameCore.Enemy e=add(c,L,new int[]{0,1},L.h*.3f);e.phase=5.5f;
            if(gate==3)e.link=new GameCore.Enemy();
            FastEnemies.assign(c,e);check("runner exclusions "+gate,!e.fastSingle && e.word.length==2);
        }
        c=scene(L,17,180);GameCore.Enemy e=runner(c,L,L.h*.3f);
        c.rnd.setSeed(180);FastEnemies.assign(c,e);
        check("selection consumes no gameplay randomness",c.rnd.nextLong()==new java.util.Random(180).nextLong());
        Words.fill(e,3,0,new java.util.Random(180));check("reused word loses runner identity",!e.fastSingle);
        cadence(L);movement(L);completion(L);waves(L);
    }
    private static void cadence(Layout L) {
        GameCore c=scene(L,16,180);c.spawnedThisStage=1;c.spawnTimer=0;
        c.update(.001f,L);
        check("intro word schedules an early runner",c.fastEnemyNext && c.enemies.size()==1
                && !c.enemies.get(0).fastSingle && c.spawnTimer<=c.spawnInterval()*.251f);
        float gap=c.spawnTimer;
        advance(c,L,gap+.12f);
        boolean found=false;
        for(GameCore.Enemy e:c.enemies)found|=e.fastSingle;
        check("runner follows its word before the ordinary interval",found && c.spawnedThisStage==3);
        check("runner restores normal following gap",!c.fastEnemyNext && c.spawnTimer>c.spawnInterval()*.8f);

        c=scene(L,17,180);c.pairSpawnSlots=0;c.spawnedThisStage=1;
        GameCore.Enemy previous=add(c,L,new int[]{0,1,4},L.h*.3f);previous.phase=5.5f;
        check("later longer word schedules early runner",FastEnemies.nextDelay(c,L)==c.spawnInterval()*.25f && c.fastEnemyNext);
        c.enemies.clear();
        GameCore.Enemy next=new GameCore.Enemy();Words.fill(next,3,0,c.rnd);next.phase=0;
        FastEnemies.assign(c,next);
        check("runner choice survives preceding word clearing and candidate phase",next.fastSingle);
        c.enemies.add(next);c.spawnedThisStage++;
        check("runners cannot chain more runners",FastEnemies.nextDelay(c,L)==c.spawnInterval() && !c.fastEnemyNext);
        c.enemies.clear();c.enemies.add(previous);c.pairSpawnSlots=1<<c.spawnedThisStage;
        check("pair slot retains ordinary timing",FastEnemies.nextDelay(c,L)==c.spawnInterval() && !c.fastEnemyNext);
        c.pairSpawnSlots=0;c.spawnedThisStage=c.stageQuota();
        check("quota end cannot schedule extra runner",FastEnemies.nextDelay(c,L)==c.spawnInterval() && !c.fastEnemyNext);
        c.fastEnemyNext=true;c.jumpToStage(18,L);
        check("new stage clears scheduled runner",!c.fastEnemyNext);
        c.fastEnemyNext=true;c.playtestMode(Power.NINJA,L);
        check("power activation clears scheduled runner",!c.fastEnemyNext);
    }
    private static void movement(Layout L) {
        GameCore c=scene(L,16,180);c.spawnedThisStage=2;c.spawnTimer=0;c.update(.001f,L);
        check("normal spawn path produces the intro runner",c.enemies.size()==1 && c.enemies.get(0).fastSingle);
        GameCore.Enemy fast=c.enemies.get(0);
        float ordinary=(L.dangerY+L.enemyR*2.2f)/c.travelSeconds();
        check("spawned runner gets twice ordinary speed",Math.abs(fast.speed-ordinary*2)<.01f);
        GameCore.Enemy plain=add(c,L,new int[]{0,1,4},-L.enemyR*2.2f);plain.speed=ordinary;
        fast.y=plain.y;EnemyEntry.top(plain,L);
        float tiny=.0001f;
        float fastEntry=PowerRush.yAfter(fast,fast.speed*tiny)-fast.y;
        float plainEntry=PowerRush.yAfter(plain,plain.speed*tiny)-plain.y;
        check("header boost does not double its peak arrival speed",Math.abs(fastEntry/plainEntry-1)<.02f);
        fast.y=plain.y=Math.max(fast.rushEndY,plain.rushEndY)+L.enemyR;
        for(boolean kids:new boolean[]{false,true}) {
            c.kidsRun=kids;
            float fastMove=SwampEnemies.fall(c,fast,L,.1f)-fast.y;
            float plainMove=SwampEnemies.fall(c,plain,L,.1f)-plain.y;
            check("readable approach is twice normal under traversal modifiers "+kids,Math.abs(fastMove/plainMove-2)<.001f);
        }
        c=scene(L,17,180);fast=runner(c,L,L.h*.2f);
        plain=add(c,L,new int[]{0,1,4},L.h*.4f);plain.speed=fast.speed*.5f;
        check("same-lane overtaking is rejected",!EnemyEntry.clear(fast,c,L));
        fast.baseX=L.playRight-L.enemyR;plain.baseX=L.playLeft+L.enemyR;
        check("separate lanes permit a faster threat",EnemyEntry.clear(fast,c,L));
        c=scene(L,16,180);c.spawnedThisStage=2;
        plain=add(c,L,new int[]{0,1,4},L.h*.3f);plain.baseX=L.w*.3f;
        plain.speed=(L.dangerY+L.enemyR*2.2f)/c.travelSeconds();
        c.spawnTimer=0;c.update(.001f,L);
        boolean mixed=false;for(GameCore.Enemy e:c.enemies)mixed|=e.fastSingle;
        check("a runner can spawn while a longer word is still active",mixed && c.enemies.contains(plain));
    }
    private static void completion(Layout L) {
        GameCore c=scene(L,17,180);GameCore.Enemy e=runner(c,L,L.h*.4f);
        c.tapKey(0,L);advance(c,L,1);
        check("one key clears and resolves a runner",e.destroyed && c.resolvedThisStage==1 && c.lives==GameCore.START_LIVES);
        c=scene(L,17,180);e=runner(c,L,L.h*.6f);c.warnLevel=1;
        float y=e.y;check("rescue can push a runner",c.pushBack(L));advance(c,L,GameCore.PUSH_SLIDE*.5f);
        check("rescue overrides its fast descent",e.y<y && e.fastSingle);
        c=scene(L,17,180);e=runner(c,L,L.dangerY-L.enemyR-2);c.lives=1;
        advance(c,L,2);check("runner breach follows normal fatal flow",c.state==GameCore.OVER && c.lives==0 && c.resolvedThisStage==1);
        c=scene(L,17,180);e=runner(c,L,L.h*.4f);c.destroyWord(e,e.baseX,e.y,L);advance(c,L,1);
        check("power clearing a runner resolves once",c.resolvedThisStage==1 && c.lives==GameCore.START_LIVES);
    }
    private static void waves(Layout L) {
        int completed=0,lost=0,fast=0,longWords=0,pairs=0;
        for(int stage:new int[]{16,17,21})for(int seed=0;seed<6;seed++) {
            GameCore c=scene(L,stage,1800+seed);c.spawnTimer=0;
            Bot bot=new Bot(6,.22f,.04f,false,1810+seed);
            java.util.Set<GameCore.Enemy> seen=new java.util.HashSet<GameCore.Enemy>();
            for(int frame=0;frame<60*90 && c.state==GameCore.PLAY && !c.pendingBonus;frame++) {
                c.update(DT,L);bot.step(c,L,DT);
                for(GameCore.Enemy e:c.enemies) {
                    if(seen.add(e)) {
                        if(e.fastSingle)fast++;
                        else if(e.link!=null)pairs++;
                        else if(e.word.length>1)longWords++;
                    }
                }
            }
            if(c.resolvedThisStage==c.stageQuota())completed++;
            lost+=GameCore.START_LIVES-c.lives;
        }
        check("mixed waves contain runners, long words and pairs",fast>0 && longWords>0 && pairs>0);
        check("bounded player completes most mixed waves",completed>=14);
        System.out.println("    runners 6/s, .22s reaction, 4% misses: "+completed+"/18 waves, "+lost+" lives lost, "+fast+" runners");
    }
}
