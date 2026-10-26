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
        beamLighting(L);
        cachedScenery(L);
        perspective(L);
        backdropSeams();
        monochrome(L);
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
    private static void beamLighting(Layout L) {
        GameCore c=stage(L);float start=c.clock;
        check("idle beams start without clear flashes",c.waterRipples.beamPulse(start,.5f)==0);
        c.waterRipples.cleared(c,L,L.w*.25f,L.h*.6f);
        float local=c.waterRipples.beamPulse(start+.1f,.25f);
        check("clears brighten nearby beams most",local>.8f
                && c.waterRipples.beamPulse(start+.1f,.9f)<local*.4f);
        check("beam flash rises then smoothly fades",c.waterRipples.beamPulse(start,.25f)==0
                && c.waterRipples.beamPulse(start+.7f,.25f)<local
                && c.waterRipples.beamPulse(start+1.5f,.25f)==0);
        for(int i=0;i<20;i++)c.waterRipples.cleared(c,L,L.w*.25f,L.h*.6f);
        check("rapid clears cap beam brightness",c.waterRipples.beamPulse(start+.1f,.25f)==1);
        c.toTitle();check("leaving play clears beam flashes",c.waterRipples.beamPulse(start+.1f,.25f)==0);
        float low=1,high=0;boolean gradual=true,staggered=false;
        for(int i=0;i<1200;i++)for(int beam=0;beam<5;beam++) {
            float t=i/30f,a=UnderwaterVista.beamIdle(t,beam);
            low=Math.min(low,a);high=Math.max(high,a);
            gradual&=Math.abs(a-UnderwaterVista.beamIdle(t+1/60f,beam))<.004f;
            staggered|=Math.abs(a-UnderwaterVista.beamIdle(t,(beam+1)%5))>.25f;
        }
        check("idle beam fades are slow, staggered, and visibly varied",gradual && staggered && low<.04f && high>.47f);
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
            public boolean reefVistaShaded(int art,float x,float y,float w,float h,int alpha,float shade,float mono) {
                return reefVista(art,x,y,w,h,alpha);
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
        float halfWidth=UnderwaterVista.COLUMN_WIDTH*.5f*UnderwaterVista.scale(1);
        check("reef columns recycle beyond the side edges",UnderwaterVista.columnX(1,0)+halfWidth<0
                && UnderwaterVista.columnX(1,1)-halfWidth>1);
        check("reef columns and sand emerge at mid-screen",UnderwaterVista.rootY(0)==.5f
                && UnderwaterVista.rootY(1)>1);
        boolean outward=true,staggered=true;
        for(int i=0;i<100;i++) {
            float t=i/100f;
            outward&=UnderwaterVista.columnX(t,0)>UnderwaterVista.columnX(t+.01f,0)
                    && UnderwaterVista.columnX(t,1)<UnderwaterVista.columnX(t+.01f,1);
            for(int row=0;row<UnderwaterVista.ROWS;row++)
                staggered&=UnderwaterVista.travel(i,row,0)!=UnderwaterVista.travel(i,row,1);
        }
        check("five column layers move outward on independently staggered sides",UnderwaterVista.ROWS==5 && outward && staggered);
        boolean inward=true,anchored=true;
        int crownProfiles=0;
        for(int variant=0;variant<5;variant++) {
            int left=1+variant*2,right=left+1;
            float bend=UnderwaterVista.bow(left,.5f);
            inward&=bend>0 && UnderwaterVista.bow(right,.5f)==-bend;
            anchored&=UnderwaterVista.bow(left,.95f)==0 && UnderwaterVista.bow(right,.95f)==0;
            if(UnderwaterVista.bow(left,.15f)>bend)crownProfiles++;
        }
        check("reef profiles bow inward with grounded feet",inward && anchored);
        check("most reef bends sit at the crown with occasional lower bulges",crownProfiles==4);
        float near=.93f,nearWidth=L.w*.60f*UnderwaterVista.scale(near);
        float nearTop=L.h*UnderwaterVista.rootY(near)-UnderwaterVista.columnHeight(near,nearWidth)*.68f;
        check("nearby reef crowns stretch above the screen",nearTop<0);
        check("water depth tint preserves alpha and approaches water at distance",
                UnderwaterVista.depthColor(0x7FABCDEF,0)==0x7F17445C
                && UnderwaterVista.depthColor(0x7FABCDEF,1)==0x7FABCDEF);
        int monoWater=UnderwaterVista.waterColor(1);
        check("depth tint follows monochrome",((monoWater>>>16)&255)==(monoWater&255)
                && ((monoWater>>>8)&255)==(monoWater&255));
        int dim=UnderwaterVista.shadeColor(0x7F80C040,.5f);
        check("reef shading halves RGB without changing alpha",dim==0x7F406020);
        check("column feather fades out toward the foreground",UnderwaterVista.edgeSoftness(.1f)==1
                && UnderwaterVista.edgeSoftness(.5f)>.1f && UnderwaterVista.edgeSoftness(.85f)==0);
        long bytes=0;
        for(int art=0;art<UnderwaterVista.COUNT;art++)bytes+=4L*UnderwaterVista.width(art)*UnderwaterVista.height(art);
        check("reef cache including soft silhouettes stays below 11 MiB",bytes<11*1024*1024);
        final int[] stamps={0},calls={0},solid={0};final boolean[] valid={true};
        ArtStamp cache=new ArtStamp() {
            public boolean stamp(int art,float x,float y,float w,float h,float angle,int tint,float amount){return false;}
            public boolean reefVistaShaded(int art,float x,float y,float w,float h,int alpha,float shade,float mono) {
                return reefVista(art,x,y,w,h,alpha);
            }
            public boolean reefVista(int art,float x,float y,float w,float h,int alpha) {
                if(art>0 && art<UnderwaterVista.RAY && alpha==255)solid[0]++;
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
            stamps[0]=calls[0]=solid[0]=0;c.skyClock=i*3.17f;
            UnderwaterVista.draw(new OpacityPainter(probe,1),c,L,1);
            bounded&=stamps[0]>4 && stamps[0]<=31 && calls[0]<80 && solid[0]>=4;
        }
        check("detailed reef artwork stays cached and bounded throughout travel",valid[0] && bounded);
    }
    private static void backdropSeams() {
        final double[] coverage=new double[512];
        Painter probe=(Painter)java.lang.reflect.Proxy.newProxyInstance(Painter.class.getClassLoader(),new Class<?>[]{Painter.class},(proxy,method,args)-> {
            if(method.getName().equals("fillRect")) {
                float top=(Float)args[1],bottom=(Float)args[3];int color=(Integer)args[4];
                for(int row=0;row<coverage.length;row++) {
                    double alpha=Math.max(0,Math.min(row+1,bottom)-Math.max(row,top))*(color>>>24)/255;
                    coverage[row]+=alpha*(1-coverage[row]);
                }
            }
            return null;
        });
        UnderwaterVista.paint(probe,0);
        boolean opaque=true;for(double alpha:coverage)opaque&=alpha>.999;
        check("antialiased water gradient has no transparent horizontal seams",opaque);
        boolean overlap=true,curved=false;
        for(int i=0;i<40;i++) {
            float t=i/100f,y=UnderwaterVista.rootY(t),floorDepth=(y-.48f)/.60f;
            for(int side=0;side<2;side++) {
                float x=UnderwaterVista.columnX(t,side);
                overlap&=x>UnderwaterVista.floorEdge(floorDepth,-1,5) && x<UnderwaterVista.floorEdge(floorDepth,1,5);
            }
            curved|=Math.abs(UnderwaterVista.horizon(.3f+i*.01f)-UnderwaterVista.horizon(.3f))>.002f;
        }
        check("distant seabed reaches beneath both banks with a meandering horizon",overlap && curved);
        check("distant columns blend into water and recover the dark near palette",UnderwaterVista.depthShade(.1f)<.15f
                && UnderwaterVista.depthShade(.7f)>UnderwaterVista.depthShade(.1f) && UnderwaterVista.depthShade(1)==1);
    }
    private static void monochrome(Layout L) {
        final int[] calls={0},stamps={0};final float[] received={-1};
        ArtStamp cache=(ArtStamp)java.lang.reflect.Proxy.newProxyInstance(ArtStamp.class.getClassLoader(),new Class<?>[]{ArtStamp.class},(proxy,method,args)-> {
            String name=method.getName();
            boolean supported=name.equals("reefVista") || name.equals("reefVistaShaded") || name.equals("reefRock") || name.equals("waterBand")
                    || name.equals("waterGlow") || name.equals("crystal");
            if(supported) {
                stamps[0]++;Object last=args[args.length-1];received[0]=last instanceof Float?(Float)last:0;
            }
            return supported;
        });
        Painter probe=(Painter)java.lang.reflect.Proxy.newProxyInstance(Painter.class.getClassLoader(),new Class<?>[]{Painter.class},(proxy,method,args)-> {
            if(method.getName().equals("stamps"))return cache;
            calls[0]++;return null;
        });
        for(int scene=0;scene<4;scene++) {
            GameCore c=scene==0?stage(L):scene==1?TestBossRetry.fight(L,Boss.OCTOPUS)
                    :scene==3?TestCrystalField.arenaScene(L,7):stage(L);
            if(scene==2)c.enterStage(7);
            c.landBlend=1;c.clock=c.skyClock=17;
            if(scene<2)c.waterRipples.cleared(c,L,L.w*.5f,L.h*.4f);
            c.clock+=.1f;
            int baseline=0,baselineStamps=0;
            for(float mono:new float[]{0,.01f,.5f,1,.4f}) {
                calls[0]=stamps[0]=0;received[0]=-1;
                Painter filtered=new OpacityPainter(new MonochromePainter(new OpacityPainter(probe,1),mono),1);
                if(scene<2){Underwater.backdrop(filtered,c,L);Underwater.veil(filtered,c,L);}
                else c.crystalField.draw(filtered,c,L,1);
                if(mono==0){baseline=calls[0];baselineStamps=stamps[0];}
                check("monochrome keeps cached scenery in scene "+scene+" at "+mono,
                        stamps[0]>0 && stamps[0]==baselineStamps && calls[0]<=baseline+2 && received[0]==mono);
            }
        }
        float[] matrix=new float[20];boolean colors=true;
        for(float amount:new float[]{0,.1f,.5f,1}) {
            MonochromePainter.matrix(matrix,amount);
            MonochromePainter mono=new MonochromePainter(probe,amount);
            for(int ink:new int[]{0x0073DA91,0x8073DA91,0xFFFF0055,0x80000000,0xFFFFFFFF}) {
                int expected=mono.color(ink),r=(ink>>>16)&255,g=(ink>>>8)&255,b=ink&255;
                for(int channel=0;channel<3;channel++) {
                    float actual=matrix[channel*5]*r+matrix[channel*5+1]*g+matrix[channel*5+2]*b;
                    colors&=Math.abs(actual-((expected>>>(16-channel*8))&255))<=1.1f;
                }
                colors&=matrix[18]==1 && matrix[19]==0;
            }
        }
        check("cached monochrome matrix matches vector luminance and preserves alpha",colors);
        for(int boss:new int[]{Boss.OCTOPUS,Boss.SPLITTER}) {
            GatorReplay replay=new GatorReplay(boss,true,true);boolean partial=false,full=false,arena=false;
            for(int tick=0;tick<24*60;tick++) {
                replay.step(L);partial|=replay.core.monochromeFade>0 && replay.core.monochromeFade<1;
                full|=replay.core.monochromeFade==1;arena|=replay.core.boss.fighting();
            }
            check("monochrome replay includes fades and arena for boss "+boss,partial && full && arena && replay.core.store==null);
        }
    }
    private static void geometry(Layout L) {
        // Count shared vector work: neither mobile renderer should need a blur or texture rebuild.
        final int[] calls={0},vertices={0};final boolean[] finite={true};
        ArtStamp cache=new ArtStamp() {
            public boolean stamp(int art,float x,float y,float w,float h,float angle,int tint,float amount){return false;}
            public boolean reefVistaShaded(int art,float x,float y,float w,float h,int alpha,float shade,float mono) {
                return reefVista(art,x,y,w,h,alpha);
            }
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
