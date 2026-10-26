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
        clearGlows(L);
        cachedScenery(L);
        perspective(L);
        geometry(L);
    }
    private static void clearGlows(Layout L) {
        GameCore c=stage(L),control=stage(L);float x=L.w*.45f,y=L.h*.45f;
        final int[] calls={0},dots={0};final float[] centre={0},point=new float[2];
        Painter probe=(Painter)java.lang.reflect.Proxy.newProxyInstance(Painter.class.getClassLoader(),new Class<?>[]{Painter.class},(proxy,method,args)-> {
            if(method.getName().equals("fillEllipse")){calls[0]++;centre[0]=(Float)args[1];}
            if(method.getName().equals("fillCircle")) {
                if(dots[0]++==0){point[0]=(Float)args[0];point[1]=(Float)args[1];}
            }
            return null;
        });
        GameCore.Enemy e=new GameCore.Enemy();e.word=new int[]{0};e.need=new int[]{1};e.baseX=x;e.y=y;
        c.enemies.add(e);c.destroyWord(e,x,y,L,false);c.clock+=.08f;control.clock=c.clock;
        c.waterRipples.drawGlows(probe,c,L,1);
        check("real enemy clear creates an underwater glow",calls[0]>0);
        control.waterRipples.prepare(control,L);
        check("clear does not distort nearby rocks or enemies",c.waterRipples.refract(x,y)==control.waterRipples.refract(x,y)
                && Underwater.enemyShift(c,x,y,L)==Underwater.enemyShift(control,x,y,L));
        check("clear emits a bounded group of glowing plankton",dots[0]==WaterRipples.PLANKTON*2);
        float ax=point[0],ay=point[1];c.clock+=.25f;dots[0]=0;c.waterRipples.drawGlows(probe,c,L,1);
        float bx=point[0],by=point[1];c.clock+=.25f;dots[0]=0;c.waterRipples.drawGlows(probe,c,L,1);
        float cross=(bx-ax)*(point[1]-by)-(by-ay)*(point[0]-bx);
        check("plankton dispersal bends instead of following straight rays",Math.abs(cross)>.01f);
        float start=centre[0];c.skyClock+=1;c.waterRipples.drawGlows(probe,c,L,1);
        check("clear glow follows the seabed",Math.abs(centre[0]-start-L.h*Underwater.SCROLL)<.001f);
        for(int i=0;i<20;i++)c.waterRipples.cleared(c,L,x,y);
        c.clock+=.08f;calls[0]=dots[0]=0;c.waterRipples.drawGlows(probe,c,L,1);
        check("many clears keep glow and plankton drawing bounded",calls[0]>0 && calls[0]<=WaterRipples.COUNT*12
                && dots[0]<=WaterRipples.COUNT*WaterRipples.PLANKTON*2);
        c.clock+=WaterRipples.DURATION;calls[0]=dots[0]=0;c.waterRipples.drawGlows(probe,c,L,1);
        check("clear glows and plankton fully fade out",calls[0]==0 && dots[0]==0);
        c.waterRipples.cleared(c,L,x,y);c.toTitle();calls[0]=dots[0]=0;c.waterRipples.drawGlows(probe,c,L,1);
        check("return to title clears glows and plankton",calls[0]==0 && dots[0]==0);
    }
    private static void cachedScenery(Layout L) {
        GameCore c=stage(L);c.waterRipples.prepare(c,L);
        float error=0;
        for(int row=0;row<40;row++)for(int col=0;col<12;col++) {
            float x=L.w*col/11,y=L.h*row/39;
            error=Math.max(error,Math.abs(c.waterRipples.refract(x,y)-WaterPainter.shift(c.clock,x,y,L.w,1)));
        }
        check("sampled water field stays close to analytic waves",error<L.w*.0008f);
        c=TestBossRetry.fight(L,Boss.OCTOPUS);c.landBlend=1;c.clock=c.skyClock=17;
        final int[] stamps={0},draws={0},bands={0},glows={0};final boolean[] valid={true};
        final float[] rocks=new float[160];
        ArtStamp cache=new ArtStamp() {
            public boolean stamp(int art,float x,float y,float w,float h,float angle,int tint,float amount){return false;}
            public boolean reefRock(int art,float x,float y,float w,float h,int alpha) {
                int at=stamps[0]++*4;rocks[at]=x;rocks[at+1]=y;rocks[at+2]=w;rocks[at+3]=h;
                valid[0]&=art>=0 && art<Underwater.ROCK_COUNT && w>0 && h>0 && alpha>0 && alpha<=255;
                return true;
            }
            public boolean reefVista(int art,float x,float y,float w,float h,int alpha){return true;}
            public boolean waterBand(float[] xy,float r,int alpha){bands[0]++;return true;}
            public boolean waterGlow(float x,float y,float rx,float ry,int alpha){glows[0]++;return true;}
        };
        Painter probe=(Painter)java.lang.reflect.Proxy.newProxyInstance(Painter.class.getClassLoader(),new Class<?>[]{Painter.class},(proxy,method,args)-> {
            if(method.getName().equals("stamps"))return cache;
            draws[0]++;return null;
        });
        Underwater.backdrop(new OpacityPainter(probe,1),c,L);
        check("rock cache survives painter wrappers and cuts vector work",valid[0] && stamps[0]>=8 && draws[0]<240);
        float[] first=rocks.clone();int count=stamps[0];stamps[0]=0;c.clock+=1;
        Underwater.backdrop(new OpacityPainter(probe,1),c,L);
        check("ambient motion never shifts or squeezes anchored rocks",count==stamps[0] && java.util.Arrays.equals(first,rocks));
        boolean separated=true,proportioned=true;
        // Cover repeated wraps and cave arrival, not just one convenient screenshot.
        for(int phase=1;phase<3;phase++)for(int tick=0;tick<180;tick++) {
            GameCore scene=TestBossRetry.fight(L,Boss.OCTOPUS);
            scene.landBlend=1;scene.skyClock=scene.clock=tick*7.31f;
            if(phase==1)scene.boss.intro=Boss.INTRO*(1-tick/179f);
            stamps[0]=0;Underwater.backdrop(probe,scene,L);
            for(int i=0;i<stamps[0];i++) {
                int a=i*4;proportioned&=rocks[a+2]/rocks[a+3]<1.9f;
                for(int j=0;j<i;j++) {
                    int b=j*4;
                    // Conservative silhouette bounds include the soft edge, excluding cast shadows.
                    separated&=Math.abs(rocks[a]-rocks[b])>1.20f*(rocks[a+2]+rocks[b+2])
                            || Math.abs(rocks[a+1]-rocks[b+1])>1.25f*(rocks[a+3]+rocks[b+3]);
                }
            }
        }
        check("rock silhouettes stay separate through scroll wraps and cave arrival",separated);
        check("rocks retain full proportions rather than stretched shelves",proportioned);
        c=stage(L);c.waterRipples.cleared(c,L,L.w*.5f,L.h*.5f);c.clock+=.1f;draws[0]=0;
        Underwater.veil(new OpacityPainter(probe,1),c,L);
        check("soft water effects use cached textures through wrappers",bands[0]==3 && glows[0]==1 && draws[0]<40);
        check("soft texture edges are fully transparent",(Underwater.waterPixel(64,0,true)>>>24)==0
                && (Underwater.waterPixel(0,0,false)>>>24)==0 && (Underwater.waterPixel(64,64,false)>>>24)>250);
        GatorReplay replay=new GatorReplay(Boss.OCTOPUS);
        for(int tick=0;tick<24*60;tick++)replay.step(L);
        check("underwater profile reaches settled cave without playing manually",replay.core.boss.kind==Boss.OCTOPUS && Underwater.caveProgress(replay.core)==1);
    }
    private static void perspective(Layout L) {
        boolean advancing=true,accelerating=true;
        float lastY=UnderwaterVista.rootY(0),lastSize=UnderwaterVista.scale(0),lastSpeed=0;
        for(int i=1;i<=100;i++) {
            float t=i/100f,y=UnderwaterVista.rootY(t),size=UnderwaterVista.scale(t),speed=y-lastY;
            advancing&=y>lastY && size>lastSize;accelerating&=speed>=lastSpeed;
            lastY=y;lastSize=size;lastSpeed=speed;
        }
        check("reef perspective moves down and grows continuously",advancing && accelerating);
        check("reef colonies fade in at the distant plane",UnderwaterVista.visibility(0)==0
                && UnderwaterVista.visibility(.05f)<UnderwaterVista.visibility(.15f));
        // The tallest colony has completely passed the viewport before recycling.
        float top=UnderwaterVista.rootY(1)*L.h-L.w*.65f*UnderwaterVista.scale(1)*352/256*.86f;
        check("reef colonies recycle beyond the bottom",top>L.h);
        final int[] stamps={0},calls={0};final boolean[] valid={true};
        ArtStamp cache=new ArtStamp() {
            public boolean stamp(int art,float x,float y,float w,float h,float angle,int tint,float amount){return false;}
            public boolean reefVista(int art,float x,float y,float w,float h,int alpha) {
                stamps[0]++;valid[0]&=art>=0 && art<UnderwaterVista.COUNT && Float.isFinite(x+y+w+h)
                        && w>0 && h>0 && alpha>=0 && alpha<=255;return true;
            }
        };
        Painter probe=(Painter)java.lang.reflect.Proxy.newProxyInstance(Painter.class.getClassLoader(),new Class<?>[]{Painter.class},(proxy,method,args)-> {
            if(method.getName().equals("stamps"))return cache;
            calls[0]++;return null;
        });
        GameCore c=stage(L);boolean bounded=true;
        for(int i=0;i<180;i++) {
            stamps[0]=calls[0]=0;c.skyClock=i*3.17f;
            UnderwaterVista.draw(new OpacityPainter(probe,1),c,L,1);
            bounded&=stamps[0]>4 && stamps[0]<=9 && calls[0]<70;
        }
        check("detailed reef artwork stays cached and bounded throughout travel",valid[0] && bounded);
    }
    private static void geometry(Layout L) {
        // Count shared vector work: neither mobile renderer should need a blur or texture rebuild.
        final int[] calls={0},vertices={0};final boolean[] finite={true};
        ArtStamp cache=new ArtStamp() {
            public boolean stamp(int art,float x,float y,float w,float h,float angle,int tint,float amount){return false;}
            public boolean reefVista(int art,float x,float y,float w,float h,int alpha){return true;}
        };
        Painter probe=(Painter)java.lang.reflect.Proxy.newProxyInstance(Painter.class.getClassLoader(),new Class<?>[]{Painter.class},(proxy,method,args)-> {
            if(method.getName().equals("stamps"))return cache;
            calls[0]++;
            if(args!=null)for(Object arg:args) {
                if(arg instanceof float[])for(float x:(float[])arg){finite[0]&=Float.isFinite(x);vertices[0]++;}
                else if(arg instanceof Float)finite[0]&=Float.isFinite((Float)arg);
            }
            return null;
        });
        for(int art=0;art<UnderwaterVista.COUNT;art++)UnderwaterVista.paint(probe,art);
        check("cached reef paintings contain finite geometry",finite[0]);
        for(int phase=0;phase<3;phase++) {
            GameCore c=phase==0?stage(L):TestBossRetry.fight(L,Boss.OCTOPUS);
            c.landBlend=1;if(phase==1)c.boss.intro=Boss.INTRO*.5f;
            calls[0]=vertices[0]=0;
            Underwater.backdrop(probe,c,L);Underwater.veil(probe,c,L);
            // Soft reefs and central dunes remain bounded without native blur or per-frame tessellation.
            check("water geometry finite and bounded in phase "+phase+" ("+calls[0]+" draws, "+vertices[0]+" coordinates)",finite[0] && calls[0]<400 && vertices[0]<10000);
        }
    }
}
