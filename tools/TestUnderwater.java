package com.dddumpling.game;

final class TestUnderwater extends Check {
    static GameCore stage(Layout L) {
        GameCore c=new GameCore(new Mem(),217,true);c.startGame();c.enterStage(12);
        c.landBlend=1;c.stageBanner=c.stageGap=0;c.clock=c.skyClock=17;
        return c;
    }
    static void all(Layout L) {
        group("Underwater scenery follows the encounter, not a separate cave minigame");
        GameCore c=stage(L);
        check("underwater stage has water without a boss cave",Underwater.presence(c)==1 && Underwater.caveProgress(c)==0 && !Cave.active(c));
        c.landFrom=Underwater.LAND;c.stage=16;c.landBlend=.5f;
        check("water blends out with the land",Math.abs(Underwater.presence(c)-.5f)<.001f);
        c.state=GameCore.TITLE;
        check("water does not cover title",Underwater.presence(c)==0);
        c=TestBossRetry.fight(L,Boss.OCTOPUS);c.landBlend=1;
        check("Octopulse settles inside underwater cave",Underwater.caveProgress(c)==1);
        float sky=c.skyClock;c.update(DT,L);
        check("cave settles while its life and lighting keep animating",c.skyClock==sky && c.clock>0);
        c.boss.intro=Boss.INTRO;
        check("cave entry starts in open water with boss hidden",Underwater.caveProgress(c)==0 && Underwater.bossReveal(c.boss)==0);
        c.boss.intro=Boss.INTRO*.2f;
        check("cave settles before boss fully emerges",Underwater.caveProgress(c)==1 && Underwater.bossReveal(c.boss)>0 && Underwater.bossReveal(c.boss)<1);
        c.boss.intro=0;TestBossRetry.lose(c,L);
        check("boss win keeps the cave",Underwater.caveProgress(c)==1);
        TestBossRetry.ready(c,L);c.bossRetry.retry(c);
        check("retry replays cave arrival",Underwater.caveProgress(c)==0);
        c=TestBossRetry.fight(L,Boss.OCTOPUS);c.boss.beaten=true;BossPlay.endBoss(c,L);
        check("befriended boss keeps cave",Underwater.caveProgress(c)==1);
        c.toTitle();check("leaving encounter releases cave",Underwater.caveProgress(c)==0);
        c=stage(L);c.survival.active=true;
        check("survival keeps its own scenery",Underwater.presence(c)==0 && Underwater.caveProgress(c)==0);
        check("water tint is absent outside water",Underwater.enemyColor(c,Glyph.COLOR[0],L.h*.4f,L)==Glyph.COLOR[0]);
        geometry(L);
    }
    private static void geometry(Layout L) {
        // Count shared vector work: neither mobile renderer should need a blur or texture rebuild.
        final int[] calls={0},vertices={0};final boolean[] finite={true};
        Painter probe=(Painter)java.lang.reflect.Proxy.newProxyInstance(Painter.class.getClassLoader(),new Class<?>[]{Painter.class},(proxy,method,args)-> {
            calls[0]++;
            if(args!=null)for(Object arg:args) {
                if(arg instanceof float[])for(float x:(float[])arg){finite[0]&=Float.isFinite(x);vertices[0]++;}
                else if(arg instanceof Float)finite[0]&=Float.isFinite((Float)arg);
            }
            return null;
        });
        for(int phase=0;phase<3;phase++) {
            GameCore c=phase==0?stage(L):TestBossRetry.fight(L,Boss.OCTOPUS);
            c.landBlend=1;if(phase==1)c.boss.intro=Boss.INTRO*.5f;
            calls[0]=vertices[0]=0;
            Underwater.backdrop(probe,c,L);Underwater.veil(probe,c,L);
            check("water geometry finite and bounded in phase "+phase,finite[0] && calls[0]<360 && vertices[0]<6000);
        }
    }
}
