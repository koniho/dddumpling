package com.dddumpling.game;

final class TestSwampEnemies extends Check {
    static GameCore scene(Layout L,long seed) {
        GameCore c=new GameCore(new Mem(),seed,true);c.startGame();c.jumpToStage(21,L);
        c.onboarding.saved=Onboarding.SKIPPED;c.onboarding.clear();
        c.stageGap=c.stageBanner=0;c.landBlend=1;
        c.spawnTimer=c.powerTimer=Float.POSITIVE_INFINITY;return c;
    }
    static GameCore.Enemy word(GameCore c,Layout L,int kind,float y) {
        GameCore.Enemy e=add(c,L,new int[]{0,1,4},y);
        e.swampKind=kind;e.speed=L.h*.08f;e.enterT=1;return e;
    }
    static void all(Layout L) {
        group("Swamp fog and divebomb words");
        GameCore c=scene(L,175);
        GameCore.Enemy e=word(c,L,0,L.h*.4f);
        for(int stage:new int[]{1,6,11,16,21,22,25,26,46}) {
            c.jumpToStage(stage,L);
            e.phase=.1f;SwampEnemies.assign(c,e);
            boolean swamp=Lands.forStage(stage)==Lands.SWAMP && !Boss.isBossStage(stage);
            check("fog restricted to normal swamp encounters "+stage,(e.swampKind==SwampEnemies.FOG)==swamp);
        }
        c.jumpToStage(21,L);e.phase=1.6f;SwampEnemies.assign(c,e);
        check("dive variant has its own selection band",e.swampKind==SwampEnemies.DIVE);
        c.rnd.setSeed(175);SwampEnemies.assign(c,e);
        check("variant selection consumes no extra random draws",c.rnd.nextLong()==new java.util.Random(175).nextLong());
        for(int length=2;length<=9;length++) {
            GameCore.Enemy shortWord=add(c,L,new int[length],L.h*.4f);
            java.util.Arrays.fill(shortWord.need,4);shortWord.phase=1.6f;
            SwampEnemies.assign(c,shortWord);
            int presses=0;for(int need:shortWord.need)presses+=need;
            check("dive words cap length and total presses at three "+length,
                    shortWord.swampKind==SwampEnemies.DIVE && shortWord.word.length==Math.min(3,length)
                    && presses==shortWord.word.length && shortWord.gone.length==shortWord.word.length);
        }
        for(int mode:new int[]{-1,Power.FLURRY,Power.NINJA,Power.TEAM})for(int length=2;length<=7;length++) {
            GameCore run=scene(L,175);run.mode=mode;run.modeLeft=mode<0?0:5;
            GameCore.Enemy fog=add(run,L,new int[length],L.h*.3f);fog.phase=.1f;
            fog.word[1]=1;fog.need[0]=2;
            SwampEnemies.assign(run,fog);
            check("fog caps keys and preserves pips "+mode+"/"+length,fog.swampKind==SwampEnemies.FOG
                    && fog.word.length==Math.min(3,length) && fog.word[1]==1 && fog.need[0]==2
                    && fog.need.length==fog.word.length && fog.gone.length==fog.word.length
                    && fog.goneT.length==fog.word.length && fog.goneDx.length==fog.word.length
                    && fog.goneDy.length==fog.word.length);
        }
        for(int gate=0;gate<3;gate++) {
            c.survival.active=gate==0;c.timeAttack.active=gate==1;
            c.mode=gate==2?Power.NINJA:-1;c.modeLeft=gate==2?5:0;
            SwampEnemies.assign(c,e);
            check("swamp power eligibility "+gate,gate==2?e.swampKind==SwampEnemies.DIVE:e.swampKind==0);
        }
        c=scene(L,175);e=add(c,L,new int[]{0},L.h*.4f);e.phase=.1f;SwampEnemies.assign(c,e);
        check("single prompts stay ordinary",e.swampKind==0);
        fog(L);dives(L);bounded(L);
    }
    private static void fog(Layout L) {
        GameCore c=scene(L,175);GameCore.Enemy e=word(c,L,SwampEnemies.FOG,L.h*.35f);
        e.need[0]=2;
        check("fog leaves head readable and hides future prompts",!SwampEnemies.hidden(e,0)
                && SwampEnemies.hidden(e,1) && SwampEnemies.hidden(e,2));
        c.tapKey(0,L);
        check("partial stack cannot reveal next prompt",e.pos==0 && SwampEnemies.hidden(e,1) && e.fogReveal==0);
        c.tapKey(0,L);
        check("next fog prompt immediately readable",e.pos==1 && !SwampEnemies.hidden(e,1) && e.fogReveal>0);
        c.tapKey(1,L);
        check("reveal animation never locks correct input",e.pos==2);
        float beforeMiss=e.fogReveal;
        c.tapKey(5,L);
        check("wrong input preserves revealed prompts and fog recession",e.pos==0 && e.fogHead==2
                && e.fogReveal==beforeMiss && !SwampEnemies.hidden(e,1) && !SwampEnemies.hidden(e,2));
        e.fogReveal=0;
        c.removeTile(e,0,0,-1,L);
        check("revisiting revealed head does not restart fog animation",e.pos==1 && e.fogReveal==0 && !SwampEnemies.hidden(e,1));
        c.target=e;c.tapKey(5,L);
        check("wrong-key reset skips permanently removed head tiles",
                e.pos==1 && !SwampEnemies.hidden(e,1) && !SwampEnemies.hidden(e,2));
        float reveal=e.fogReveal;c.paused=true;c.update(.1f,L);
        check("pause freezes fog reveal",e.fogReveal==reveal);
        c.paused=false;c.destroyWord(e,e.baseX,e.y,L);
        check("power clear resolves a fog word",e.destroyed && c.resolvedThisStage==1);
        c=scene(L,175);e=add(c,L,new int[]{0,1,4,2},L.h*.35f);e.swampKind=SwampEnemies.FOG;
        c.tapKey(0,L);c.tapKey(5,L);e.fogReveal=0;c.tapKey(0,L);
        check("retyping known prompts preserves the remaining fog",e.pos==1 && e.fogHead==1
                && e.fogReveal==0 && SwampEnemies.hidden(e,2) && SwampEnemies.hidden(e,3));
        c.tapKey(1,L);
        check("new progress resumes fog recession",e.fogHead==2 && e.fogReveal==SwampEnemies.REVEAL
                && !SwampEnemies.hidden(e,2) && SwampEnemies.hidden(e,3));
        Layout small=new Layout();small.compute(393,852,0,30,0,20);
        GameCore render=scene(small,175);GameCore.Enemy a=word(render,small,SwampEnemies.FOG,300);
        RasterPainter p=new RasterPainter(393,852,1);p.clear(0);Renderer.enemy(p,render,small,a);
        int[] first=p.resolve();
        a.need[1]=4;
        RasterPainter q=new RasterPainter(393,852,1);q.clear(0);Renderer.enemy(q,render,small,a);
        check("hidden stacks cannot leak through mist",java.util.Arrays.equals(first,q.resolve()));
        a.word[1]=5;a.word[2]=2;
        RasterPainter silhouettes=new RasterPainter(393,852,1);silhouettes.clear(0);Renderer.enemy(silhouettes,render,small,a);
        check("masked keys retain distinct silhouettes",!java.util.Arrays.equals(first,silhouettes.resolve()));
        a.fogHead=1;a.fogReveal=SwampEnemies.REVEAL;
        check("new reveal begins in silhouette color",SwampEnemies.tintAmount(a,1)==1 && SwampEnemies.tintAmount(a,0)==0);
        a.fogReveal=SwampEnemies.REVEAL*.5f;
        check("reveal smoothly blends through color",Math.abs(SwampEnemies.tintAmount(a,1)-.5f)<.001f);
        a.fogReveal=0;
        check("reveal ends in full key color",SwampEnemies.tintAmount(a,1)==0);
        GameCore botGame=scene(L,175);GameCore.Enemy fog=word(botGame,L,SwampEnemies.FOG,L.h*.4f);
        Bot bot=new Bot(10,.3f,0,false,175);
        for(int i=0;i<7;i++)bot.step(botGame,L,DT);
        check("bounded player reaches first hidden reveal",fog.pos==1);
        for(int i=0;i<10;i++)bot.step(botGame,L,DT);
        check("bounded player pays reaction for each revealed head",fog.pos==1);
    }
    private static void dives(Layout L) {
        GameCore c=scene(L,176);Ear ear=new Ear();c.sound=ear;
        GameCore.Enemy e=word(c,L,SwampEnemies.DIVE,L.h*.35f);e.need[1]=2;
        c.tapKey(0,L);c.tapKey(5,L);
        check("divebomb miss before the charge preserves cleared keys",e.pos==1 && !e.diving && c.target==null);
        c.tapKey(1,L);
        check("dive waits until penultimate stack fully clears",!e.diving);
        c.tapKey(1,L);
        check("last unresolved prompt triggers one whoosh",e.diving && ear.swishes==1 && ear.octoCues==0 && e.typeable());
        check("charge starts its visual burst immediately",e.diveBurst==SwampEnemies.DIVE_BURST);
        float y=e.y;c.update(.1f,L);
        check("dive immediately charges at full speed and accepts input",e.y-y>e.speed*.1f*3 && e.typeable());
        check("ring burst stays at the launch point",e.diveStartY==y && e.diveStartY<e.y);
        c.paused=true;y=e.y;float burst=e.diveBurst;c.update(.2f,L);
        check("pause freezes dive movement and launch burst",e.y==y && e.diveBurst==burst);c.paused=false;
        advance(c,L,.6f);y=e.y;c.update(DT,L);
        check("dive accelerates beyond normal approach",e.y-y>e.speed*DT*2);
        check("launch burst fades while the dive continues",e.diveBurst==0 && e.diving);
        c.tapKey(5,L);float speed=e.diveSpeed;
        check("wrong key preserves final prompt and ongoing dive",e.pos==2 && e.diving && ear.swishes==1);
        c.tapKey(5,L);
        check("another miss cannot compound acceleration or restart burst",
                e.pos==2 && e.diveSpeed==speed && ear.swishes==1 && e.diveBurst==0);
        c.tapKey(4,L);advance(c,L,1);
        check("last prompt still defeats diving word",e.destroyed && c.resolvedThisStage==1 && c.lives==GameCore.START_LIVES);
        c=scene(L,176);e=word(c,L,SwampEnemies.DIVE,L.dangerY-L.enemyR-3);
        c.tapKey(0,L);c.tapKey(1,L);
        check("dive does not brake or wait at the damage line",SwampEnemies.fall(c,e,L,.1f)>L.dangerY-L.enemyR);
        c=scene(L,176);e=word(c,L,SwampEnemies.DIVE,L.h*.6f);
        c.tapKey(0,L);c.tapKey(1,L);
        c.warnLevel=1;
        check("rescue swipe can push a diving enemy",c.pushBack(L) && e.slideT>0);
        y=e.y;advance(c,L,GameCore.PUSH_SLIDE*.5f);
        check("rescue push is not opposed by dive movement",e.y<y && e.diving);
        c=scene(L,176);e=word(c,L,SwampEnemies.DIVE,L.h*.4f);
        c.removeTile(e,2,1,0,L);
        check("removing tail tile with two left cannot trigger dive",!e.diving);
        c.removeTile(e,0,1,0,L);
        check("out-of-order clears count unresolved tiles",e.diving && e.pos==1);
        c.destroyWord(e,e.baseX,e.y,L);advance(c,L,.5f);
        check("cleared dive cannot damage player",c.lives==GameCore.START_LIVES && c.resolvedThisStage==1);
        c=scene(L,176);e=word(c,L,SwampEnemies.DIVE,L.dangerY-L.enemyR-5);
        c.lives=1;c.tapKey(0,L);c.tapKey(1,L);advance(c,L,4);
        check("dive breach follows normal fatal damage flow",c.state==GameCore.OVER && c.lives==0 && c.resolvedThisStage==1);
        Words.fill(e,3,0,new java.util.Random(1));
        check("reused words clear variant state",e.swampKind==0 && !e.diving && e.fogHead==0 && e.fogReveal==0
                && e.diveBurst==0 && e.diveStartX==0 && e.diveStartY==0);
    }
    private static void bounded(Layout L) {
        int completed=0,lost=0;int[] variants=new int[3];
        for(int seed=0;seed<12;seed++) {
            GameCore c=scene(L,1750+seed);c.spawnTimer=0;
            Bot bot=new Bot(6,.22f,.04f,false,1760+seed);
            java.util.Set<GameCore.Enemy> seen=new java.util.HashSet<GameCore.Enemy>();
            for(int frame=0;frame<60*90 && c.state==GameCore.PLAY && !c.pendingBonus;frame++) {
                c.update(DT,L);bot.step(c,L,DT);
                for(GameCore.Enemy e:c.enemies)if(seen.add(e) && e.link==null)variants[e.swampKind]++;
            }
            if(c.resolvedThisStage==c.stageQuota())completed++;
            lost+=GameCore.START_LIVES-c.lives;
        }
        check("bounded swamp waves include both variants and normal enemies",variants[0]>0 && variants[1]>0 && variants[2]>0);
        check("bounded reactive player can finish most swamp waves",completed>=8);
        System.out.println("    swamp 6/s, .22s reveal reaction, 4% misses: "+completed+"/12 waves, "+lost+" lives lost");
    }
}
