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
        float largest=0;boolean moving=false;
        for(int frame=0;frame<120;frame++) {
            float t=frame*.1f;
            for(int row=0;row<20;row++) {
                float y=L.h*row/20f,a=WaterPainter.shift(t,L.w*.5f,y,L.w,1);
                largest=Math.max(largest,Math.abs(a));
                moving|=Math.abs(a-WaterPainter.shift(t+1,L.w*.5f,y,L.w,1))>L.w*.001f;
            }
        }
        check("refraction moves but stays below one percent of screen width",moving && largest<=L.w*.0091f);
        c=stage(L);float x=L.w*.5f,y=L.h*.4f;
        check("enemy refraction is much smaller than scenery",Math.abs(Underwater.enemyShift(c,x,y,L))<=L.w*.0022f);
        c.survival.active=true;
        check("enemy refraction is absent in other modes",Underwater.enemyShift(c,x,y,L)==0);
        ripples(L);
        geometry(L);
    }
    private static void ripples(Layout L) {
        GameCore c=stage(L);float x=L.w*.45f,y=L.h*.45f;
        GameCore.Enemy e=new GameCore.Enemy();e.word=new int[]{0};e.need=new int[]{1};e.baseX=x;e.y=y;
        c.enemies.add(e);c.destroyWord(e,x,y,L,false);c.clock+=.08f;c.waterRipples.prepare(c,L);
        check("real enemy clear increases local refraction",Math.abs(c.waterRipples.shift(e.clearX,y,L.w))>L.w*.01f);
        check("clear refraction leaves distant scenery alone",c.waterRipples.shift(e.clearX,y+L.h*.7f,L.w)==0);
        for(int i=0;i<20;i++)c.waterRipples.cleared(c,L,x,y);
        c.clock+=.08f;c.waterRipples.prepare(c,L);
        check("many clears have a capped refraction amplitude",Math.abs(c.waterRipples.shift(x,y,L.w))<=L.w*.0281f);
        c.clock+=WaterRipples.DURATION;c.waterRipples.prepare(c,L);
        check("local clear wobble fully settles",c.waterRipples.shift(x,y,L.w)==0);
        c.waterRipples.cleared(c,L,x,y);c.toTitle();c.waterRipples.prepare(c,L);
        check("return to title clears water impulses",c.waterRipples.shift(x,y,L.w)==0);
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
            check("water geometry finite and bounded in phase "+phase+" ("+calls[0]+" draws, "+vertices[0]+" coordinates)",finite[0] && calls[0]<360 && vertices[0]<6000);
        }
    }
}
