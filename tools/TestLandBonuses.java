package com.dddumpling.game;

final class TestLandBonuses extends Check {
    static void ready(GameCore c) {
        c.stageBanner=c.stageGap=c.startFade=0;
        c.landBonus.update(c,c.landBonus.delay+LandBonus.BUBBLE_RISE+.1f);c.skyClock+=4;
    }
    static GameCore start(Layout L,int stage,boolean force,long seed) {
        GameCore c=new GameCore(new Mem(),seed);
        c.startGame();c.onboarding.clear();c.onboarding.saved=c.onboarding.savedPowers=-1;
        c.devLandBonuses=force;c.enterStage(stage);c.stageBanner=c.stageGap=c.startFade=0;
        c.landBlend=1;c.spawnTimer=999;c.time=2;ready(c);
        return c;
    }
    static void all(Layout L) {
        group("land scenery bonuses");
        collectionArt(L);
        int seen=0;
        for(int seed=0;seed<40;seed++) {
            GameCore c=start(L,1,false,seed);
            for(int chapter=0;chapter<6;chapter++) {
                int offered=0,choice=-1;
                for(int offset=0;offset<4;offset++) {
                    c.enterStage(chapter*5+offset+1);ready(c);
                    if(offset==0)choice=c.landBonus.selectedStage;
                    check("one stable choice per land",c.landBonus.selectedStage==choice);
                    if(c.landBonus.visible(c)) {offered++;seen|=1<<offset;}
                }
                check("exactly one regular stage offers the bonus",offered==1);
                c.enterStage(chapter*5+5);ready(c);
                check("boss stage never offers scenery reward",!c.landBonus.visible(c));
            }
        }
        check("all four regular stages can be selected",seen==15);
        GameCore c=start(L,1,true,244);
        Ear sound=new Ear();c.sound=sound;
        int score=c.score;
        float x=LandBonus.x(c,L),y=LandBonus.y(c,L);
        check("forced bonus appears after the delayed entrance",c.landBonus.visible(c));
        check("target stays clear of keys",L.keyAt(x,y)<0 && y+LandBonus.radius(L)*1.18f<L.deckTop);
        check("one tap grants points",c.landBonus.tap(c,L,x,y) && c.score==score+LandBonus.POINTS);
        check("repeated tap cannot pay twice",!c.landBonus.tap(c,L,x,y) && c.score==score+LandBonus.POINTS);
        check("one cheerful chime per reward",sound.landBonuses==1 && sound.lastBonusLand==0);
        c.devLandBonuses=false;c.devLandBonuses=true;
        check("toggling override cannot replay reward",!c.landBonus.offered(c));
        c.enterStage(2);ready(c);
        check("override offers next regular stage",c.landBonus.visible(c));
        c.enterStage(1);ready(c);
        check("returning to collected stage does not pay again",!c.landBonus.offered(c));
        c.enterStage(5);ready(c);
        check("override never enables boss-stage bonus",!c.landBonus.visible(c));
        c.enterStage(6);ready(c);
        check("next land offers a fresh reward",c.landBonus.visible(c));
        c.paused=true;float age=c.landBonus.age;int choice=c.landBonus.selectedStage;
        c.update(1,L);
        check("pause preserves choice and clocks",c.landBonus.age==age && c.landBonus.selectedStage==choice);
        check("paused touch cannot collect",!c.landBonus.tap(c,L,LandBonus.x(c,L),y));
        c.paused=false;c.settingsOpen=true;c.update(1,L);
        check("settings preserve clocks and block collection",c.landBonus.age==age && !c.landBonus.tap(c,L,LandBonus.x(c,L),y));
        c.settingsOpen=false;check("collection starts before interlude",c.landBonus.tap(c,L,LandBonus.x(c,L),LandBonus.y(c,L)));
        c.state=GameCore.BONUS;c.update(0,L);
        check("interlude clears collection effect",c.landBonus.burst<0);
        c.toTitle();check("title clears owned stage",c.landBonus.stage<0 && c.landBonus.burst<0);
        for(int mode:new int[]{ModeSelector.SURVIVAL,ModeSelector.TIME_ATTACK}) {
            c=start(L,1,true,244);c.modes.restore(mode);
            check("override cannot leak into other modes",!c.landBonus.visible(c));
        }
        c=start(L,1,true,244);c.onboarding.teacher=new GameCore(null,1);
        check("practice scenes do not offer bonus",!c.landBonus.visible(c));
        c=start(L,1,true,244);c.landBonus.tap(c,L,LandBonus.x(c,L),y);c.lives=1;c.takeHit(L.w*.5f,L);
        check("death clears reward effects",c.state==GameCore.OVER && c.landBonus.stage<0 && c.landBonus.burst<0);
        GameCore a=start(L,1,true,299),b=start(L,1,true,299);
        a.landBonus.enter(a);a.landBonus.tap(a,L,LandBonus.x(a,L),y);
        check("selection and collection preserve combat RNG",a.rnd.nextLong()==b.rnd.nextLong());
        for(int land:new int[]{0,1,2,3,Lands.SWAMP,Lands.RAINBOW}) {
            c=start(L,Lands.firstStage(land),true,244);c.landBonus.age=c.landBonus.delay;
            float entrance=LandBonus.y(c,L);c.landBonus.age+=LandBonus.BUBBLE_RISE;
            float settled=LandBonus.y(c,L);
            check("themed entrance direction "+land,land==0?settled<entrance:land==Lands.RAINBOW?Math.abs(settled-entrance)<LandBonus.radius(L)*.3f:settled==entrance);
            float travel=land==2 || land==Lands.SWAMP?12:4;c.skyClock+=travel;float moved=LandBonus.y(c,L);
            check("bonus shares scenery travel "+land,land==Lands.SWAMP?Math.abs(moved+LandBonus.size(c,L)*.86f-SwampVista.rootY(L,LandBonus.flamingoTravel(c),LandBonus.x(c,L)))<.01f:land==2?Math.abs(moved+LandBonus.size(c,L)*.8f-L.h*UnderwaterVista.rootY(LandBonus.clamTravel(c)))<.01f:Math.abs(moved-settled-travel*LandBonus.scrollSpeed(c,L))<.01f);
            c.paused=true;c.update(.1f,L);
            check("pause holds world position "+land,LandBonus.y(c,L)==moved);
            c.paused=false;c.settingsOpen=true;c.update(.1f,L);
            check("settings hold world position "+land,LandBonus.y(c,L)==moved);
            c.settingsOpen=false;
            check("old fixed target does not collect moving scenery "+land,!c.landBonus.tap(c,L,LandBonus.x(c,L),settled));
            c.skyClock+=land==Lands.SWAMP?80:L.h/LandBonus.scrollSpeed(c,L);
            check("bonus cannot collect below deck "+land,!c.landBonus.tap(c,L,LandBonus.x(c,L),LandBonus.y(c,L)));
            c.landBonus.restartEntrance(c);c.landBonus.age=c.landBonus.delay+LandBonus.BUBBLE_RISE;c.skyClock+=4;
            Ear cue=new Ear();c.sound=cue;
            check("moving target collects at displayed position "+land,c.landBonus.tap(c,L,LandBonus.x(c,L),LandBonus.y(c,L))
                    && cue.landBonuses==1 && cue.lastBonusLand==land);
        }
        c=start(L,1,true,244);c.landBonus.restartEntrance(c);c.stageBanner=2;
        c.landBonus.update(c,6);
        check("stage opening cannot reveal or age bonus",c.landBonus.age==0 && !c.landBonus.visible(c));
        c.stageBanner=0;c.landBonus.update(c,c.landBonus.delay-.1f);
        check("bonus remains hidden during opening gameplay",!c.landBonus.visible(c));
        c.landBonus.update(c,.2f);
        check("bonus starts emerging only after delay",c.landBonus.visible(c));
        float surface=LandBonus.surfaceY(c,L);
        check("submerged bonus cannot be tapped through waterline",!c.landBonus.tap(c,L,LandBonus.x(c,L),surface+LandBonus.radius(L)*.3f));
        SlimeBubbles.Bubble reference=new SlimeBubbles.Bubble();c.slimeBubbles.spawn(reference,c.landBonus.bubbleId,L);
        check("bonus uses ambient bubble spawn region",Math.abs(LandBonus.x(c,L)-reference.x)<.01f && Math.abs(surface-reference.surfaceY)<.01f);
        c.landBonus.age=c.landBonus.delay+LandBonus.BUBBLE_RISE*.25f;
        reference.age=reference.riseTime*.25f;reference.r=LandBonus.radius(L);
        check("bonus rises through the same surface curve",Math.abs(LandBonus.y(c,L)-reference.centreY())<.01f);
        c.cloudUi.reduceMotion=true;
        check("reduced motion retains surface anchor",Math.abs(LandBonus.y(c,L)-(surface-LandBonus.radius(L)))<.01f);
        java.util.HashSet<Integer> positions=new java.util.HashSet<>(),delays=new java.util.HashSet<>();
        for(int seed=0;seed<40;seed++)for(int land:new int[]{0,1,2,3,Lands.SWAMP,Lands.RAINBOW}) {
            c=start(L,Lands.firstStage(land),true,seed);LandBonus bonus=c.landBonus;
            float delay=bonus.delay,x0=LandBonus.x(c,L),side=bonus.sideX;int id=bonus.bubbleId;
            positions.add((int)(x0*10));delays.add((int)(delay*100));
            check("random delay stays within safe window",delay>=LandBonus.MIN_DELAY && delay<=LandBonus.MAX_DELAY);
            check("side scenery keeps safe inset",side>=.12f && side<=.20f || side>=.80f && side<=.88f);
            check("random entry begins above playfield",bonus.entryDepth>=1.1f && bonus.entryDepth<=1.8f);
            if(land==0)check("bubble stays inside ambient region",x0>=L.w*.30f && x0<=L.w*.70f);
            if(land==2) {
                float far=LandBonus.size(c,L),depth=LandBonus.clamTravel(c);
                c.skyClock+=16;
                check("clam grows as seabed approaches",LandBonus.size(c,L)>far && LandBonus.clamTravel(c)>depth);
                check("clam stays on central seabed",LandBonus.x(c,L)>L.w*.25f && LandBonus.x(c,L)<L.w*.75f);
            }
            c.paused=true;c.update(1,L);c.paused=false;c.settingsOpen=true;c.update(1,L);c.settingsOpen=false;
            bonus.restartEntrance(c);
            check("pause settings and developer restart never reroll",bonus.delay==delay && bonus.sideX==side && bonus.bubbleId==id);
            int stage=c.stage;c.enterStage(stage+1);c.enterStage(stage);
            check("return to same stage preserves random placement",bonus.delay==delay && bonus.sideX==side && bonus.bubbleId==id);
        }
        check("entry positions and timing vary across visits",positions.size()>20 && delays.size()>20);
        java.util.HashSet<Integer> sounds=new java.util.HashSet<>();
        for(int land:new int[]{0,1,2,3,Lands.SWAMP,Lands.RAINBOW}) {
            short[] pcm=Sfx.build(Sfx.landBonusId(land));sounds.add(java.util.Arrays.hashCode(pcm));
            check("themed cue is short and click-free "+land,pcm.length<Sfx.RATE && pcm[0]==0 && Math.abs(pcm[pcm.length-1])<8);
        }
        check("all six lands have distinct tap sounds",sounds.size()==6);
        SettingsUi ui=new SettingsUi();ui.compute(L,SettingsUi.GENERAL);
        check("developer chip has its own target",ui.hit(L.w*.5f,ui.landBonusY+ui.testH*.5f)==SettingsUi.HIT_LAND_BONUSES);
        check("developer Run controls fit",ui.debuffY+ui.testH<ui.panelB);
    }
    private static final class ArtProbe extends OpacityPainter {
        boolean face,coinShimmer;int coinAlpha;float coinRadius;float[] body;float clamX,clamY,clamR,acornY,squirrelY,logY;
        int order,squirrelOrder,leafOrder,squirrelDraws,rippleCount,lipOrder,acornOrder,pawOrder;float lipY,pawY,flamingoX,flamingoY,headX,headY;boolean closedEye,legSeen;float rippleY,legEndY;
        final float forestRadius,squirrelRadius;
        ArtProbe(Layout L){this(L,LandBonus.radius(L));}
        ArtProbe(Layout L,float radius){super(new RasterPainter(1,1,1),1);forestRadius=radius;squirrelRadius=LandBonus.radius(L)*LandBonusArt.SQUIRREL_SCALE;}
        public void fillPoly(float[] xy,int color) {
            int rgb=color&0xffffff;order++;
            if(xy.length==66 && (rgb==0x71917a || rgb==0x91a486 || rgb==0x587d70 || rgb==0xacb394))leafOrder=order;
            if(xy.length==20 && Math.abs((xy[8]-xy[0])/forestRadius-.76f)<.001f){lipOrder=order;lipY=xy[5];}
            if(rgb==0xffd879)face=true;
            if(rgb==0xfff8d9)coinShimmer=true;
            if(rgb==0x69a13d && xy.length==68 || rgb==(CrystalField.edgeColor(0)&0xffffff) && xy.length==12)body=xy.clone();
        }
        public void fillCircle(float x,float y,float radius,int color) {
            if(Math.abs(radius/forestRadius-.20f)<.001f){headX=x;headY=y;}
        }
        public void arc(float x,float y,float rx,float ry,float start,float sweep,int color,float width) {
            if(Math.abs(rx/forestRadius-.06f)<.001f && Math.abs(ry/forestRadius-.04f)<.001f && start==190)closedEye=true;
            if(Math.abs(rx/forestRadius-.14f)<.001f && Math.abs(ry/forestRadius-.04f)<.001f && start==0) {
                rippleY=y;rippleCount++;
            }
        }
        public void line(float x,float y,float xx,float yy,int color,float width) {
            if(!legSeen && Math.abs(x-xx)<.07f*forestRadius && yy-y>forestRadius*.4f){legEndY=yy;legSeen=true;}
        }
        public void fillEllipse(float x,float y,float rx,float ry,int color) {
            order++;
            if(Math.abs(rx/forestRadius-.51f)<.001f && Math.abs(ry/forestRadius-.32f)<.001f){flamingoX=x;flamingoY=y;}
            if((color&0xffffff)==0xffdc67){coinRadius=ry/.76f;coinAlpha=color>>>24;}
            if((color&0xffffff)==0xb57ad0){clamX=x;clamY=y;clamR=rx;}
            if(Math.abs(rx/squirrelRadius-.135f)<.001f && Math.abs(ry/squirrelRadius-.19f)<.001f){acornY=y;acornOrder=order;}
            if(Math.abs(rx/squirrelRadius-.11f)<.001f && Math.abs(ry/squirrelRadius-.10f)<.001f){pawY=y;pawOrder=order;}
            if(Math.abs(rx/squirrelRadius-.32f)<.001f && Math.abs(ry/squirrelRadius-.39f)<.001f) {
                squirrelY=y;squirrelOrder=order;squirrelDraws++;
            }
            if(Math.abs(rx/forestRadius-.40f)<.001f && Math.abs(ry/forestRadius-.50f)<.001f)logY=y;
        }
    }
    private static ArtProbe art(GameCore c,Layout L) {
        ArtProbe p=new ArtProbe(L,Lands.forStage(c.stage)==Lands.SWAMP?LandBonus.size(c,L):LandBonus.radius(L));LandBonusArt.beneathCrystal(p,c,L);LandBonusArt.beneathForest(p,c,L,1);LandBonusArt.inSwamp(p,c,L,1);LandBonusArt.draw(p,c,L);return p;
    }
    private static float turn(float[] a,float[] b) {
        int j=(a.length/4)*2;
        return (a[j]-a[0])*(b[j+1]-b[1])-(a[j+1]-a[1])*(b[j]-b[0]);
    }
    private static void collectionArt(Layout L) {
        GameCore coin=start(L,Lands.firstStage(Lands.RAINBOW),true,244);
        float full=LandBonus.radius(L);ArtProbe small=art(coin,L);
        check("idle coin is three-quarter size and translucent",Math.abs(small.coinRadius-full*.75f)<.01f && small.coinAlpha<64);
        check("idle coin shimmers",small.coinShimmer);
        coin.cloudUi.reduceMotion=true;float anchorX=LandBonus.x(coin,L),anchorY=LandBonus.y(coin,L);
        coin.cloudUi.reduceMotion=false;
        float orbitX=LandBonus.x(coin,L),orbitY=LandBonus.y(coin,L);
        for(int phase=0;phase<4;phase++) {
            coin.landBonus.age+= (float)Math.PI/2/1.4f;
            float dx=LandBonus.x(coin,L)-anchorX,dy=LandBonus.y(coin,L)-anchorY;
            if(phase==0)check("coin drifts around its scenery anchor",Math.abs(LandBonus.x(coin,L)-orbitX)+Math.abs(LandBonus.y(coin,L)-orbitY)>full*.1f);
            check("coin idle motion follows a small circle",Math.abs(Math.sqrt(dx*dx+dy*dy)-full*.12f)<.01f);
        }
        check("coin collects at orbiting position",coin.landBonus.tap(coin,L,LandBonus.x(coin,L),LandBonus.y(coin,L)));
        coin.landBonus.burst=.25f;ArtProbe collected=art(coin,L);
        check("collected coin reaches full size and opacity",LandBonus.size(coin,L)==full && collected.coinAlpha==255 && collected.coinShimmer);
        coin.landBonus.burst=-1;coin.cloudUi.reduceMotion=true;float stillX=LandBonus.x(coin,L),stillY=LandBonus.y(coin,L);
        coin.landBonus.age+=1;
        check("reduced motion keeps coin position steady",LandBonus.x(coin,L)==stillX && LandBonus.y(coin,L)==stillY);
        GameCore flamingo=start(L,Lands.firstStage(Lands.SWAMP),true,244);
        float birdRadius=LandBonus.size(flamingo,L);ArtProbe resting=art(flamingo,L);
        flamingo.landBonus.age+=1.1f;ArtProbe breathing=art(flamingo,L);
        check("idle flamingo breathes and gently shifts its head",Math.abs(breathing.flamingoY-resting.flamingoY)>.01f && Math.abs(breathing.headX-resting.headX)+Math.abs(breathing.headY-resting.headY)>birdRadius*.01f);
        check("idle flamingo keeps feet and ripples grounded",breathing.rippleY==resting.rippleY && Math.abs(breathing.legEndY-resting.legEndY)<.01f);
        flamingo.landBonus.age=11.12f;check("idle flamingo occasionally blinks",art(flamingo,L).closedEye);
        flamingo.cloudUi.reduceMotion=true;ArtProbe quiet=art(flamingo,L);
        flamingo.landBonus.age+=1.1f;ArtProbe quietLater=art(flamingo,L);
        check("reduced motion holds flamingo idle pose",quiet.flamingoY==quietLater.flamingoY && quiet.headX==quietLater.headX && quiet.headY==quietLater.headY && !quietLater.closedEye);
        flamingo.cloudUi.reduceMotion=false;
        flamingo.landBonus.tap(flamingo,L,LandBonus.x(flamingo,L),LandBonus.y(flamingo,L));
        flamingo.landBonus.burst=.36f;ArtProbe jumping=art(flamingo,L);
        check("flamingo has one small ripple per leg",resting.rippleCount==2 && jumping.rippleCount==2);
        check("flamingo waterline does not jump or scale",resting.rippleY==jumping.rippleY);
        check("flamingo legs emerge above the waterline",Math.abs(resting.legEndY-resting.rippleY)<.01f && jumping.legEndY<jumping.rippleY-birdRadius*.2f);
        flamingo.cloudUi.reduceMotion=true;ArtProbe grounded=art(flamingo,L);
        check("reduced motion keeps flamingo grounded",Math.abs(grounded.legEndY-grounded.rippleY)<.01f);
        GameCore c=start(L,6,true,244);
        check("crystal is faceless before collection",!art(c,L).face);
        c.landBonus.tap(c,L,LandBonus.x(c,L),LandBonus.y(c,L));
        check("crystal face appears on the tap frame",art(c,L).face);
        for(int stage:new int[]{1,6}) {
            c=start(L,stage,true,244);float[] idle=art(c,L).body;
            c.landBonus.tap(c,L,LandBonus.x(c,L),LandBonus.y(c,L));
            c.landBonus.burst=.12f;float[] left=art(c,L).body;
            c.landBonus.burst=.36f;float[] right=art(c,L).body;
            check("collected bonus sways in both directions "+stage,turn(idle,left)*turn(idle,right)<0);
            check("collected bonus bounces upward "+stage,left[1]<idle[1] && right[1]<idle[1]);
            c.cloudUi.reduceMotion=true;c.landBonus.burst=.12f;left=art(c,L).body;
            c.landBonus.burst=.36f;right=art(c,L).body;
            check("reduced motion holds collection pose "+stage,java.util.Arrays.equals(left,right));
        }
        c=start(L,11,true,244);c.landBonus.tap(c,L,LandBonus.x(c,L),LandBonus.y(c,L));
        ArtProbe far=art(c,L);c.landBonus.burst=.6f;ArtProbe near=art(c,L);
        check("clam grows into foreground",near.clamR>far.clamR*2);
        check("clam collection centers on playfield",Math.abs(near.clamX-L.w*.5f)<.01f);
        c.cloudUi.reduceMotion=true;c.landBonus.burst=0;far=art(c,L);c.landBonus.burst=.6f;near=art(c,L);
        check("reduced motion avoids clam zoom",far.clamR==near.clamR && far.clamY==near.clamY);
        for(float burst:new float[]{-1,.36f}) {
            c=start(L,16,true,244);c.landBonus.burst=burst;
            ArtProbe layers=new ArtProbe(L);
            Lands.scenery(layers,c,L);LandBonusArt.draw(layers,c,L);
            check("forest bonus is drawn once beneath translucent leaves "+burst,
                    layers.squirrelDraws==1 && layers.leafOrder>layers.squirrelOrder);
        }
        c=start(L,16,true,244);ArtProbe idle=art(c,L);
        check("forest squirrel holds an acorn before collection",idle.acornY>0 && idle.squirrelY>0);
        check("log front lip covers the body but sits behind paws and acorn",idle.lipOrder>idle.squirrelOrder && idle.pawOrder>idle.lipOrder && idle.acornOrder>idle.lipOrder);
        check("idle squirrel paws rest on the hollow lip",Math.abs(idle.pawY-idle.lipY)<LandBonus.radius(L)*.1f);
        c.landBonus.tap(c,L,LandBonus.x(c,L),LandBonus.y(c,L));
        c.landBonus.burst=.12f;ArtProbe hopping=art(c,L);
        c.landBonus.burst=.4f;ArtProbe raised=art(c,L);
        check("squirrel raises acorn above its body",raised.acornY-raised.squirrelY<idle.acornY-idle.squirrelY-LandBonus.radius(L)*.75f);
        check("squirrel bounces during the lift",hopping.squirrelY<raised.squirrelY);
        check("forest log stays grounded during collection",idle.logY==hopping.logY && idle.logY==raised.logY);
        c.cloudUi.reduceMotion=true;c.landBonus.burst=.12f;hopping=art(c,L);
        c.landBonus.burst=.6f;raised=art(c,L);
        check("reduced motion holds squirrel and raised acorn steady",hopping.squirrelY==raised.squirrelY && hopping.acornY==raised.acornY && raised.acornY<raised.squirrelY);
        c=start(L,6,true,244);
        c.skyClock=c.landBonus.skyAt+(LandBonus.radius(L)*c.landBonus.entryDepth-2)/LandBonus.scrollSpeed(c,L);
        RasterPainter raster=new RasterPainter((int)L.w,(int)L.h,1);
        LandBonusArt.beneathCrystal(raster,c,L);
        int above=0;int[] pixels=raster.resolve();
        for(int y=0;y<(int)L.playTop;y++)for(int x=0;x<(int)L.w;x++)if((pixels[y*(int)L.w+x]>>>24)>0)above++;
        check("crystal crosses header without clipping",above>0);
        for(int side=0;side<2;side++)for(int layer=0;layer<SlimeBasinArt.LAYERS;layer++) {
            float top=SlimeBasin.layerTop(2,side,layer,L);
            check("slime banks extend past screen top",top<0);
            check("slime banks still cover scene bottom",top+SlimeBasin.bankHeight(2,side,L)>L.deckTop);
        }
        // Every scenery layer must be free to paint behind the HUD.
        for(int land:new int[]{0,1,2,3,Lands.SWAMP,Lands.RAINBOW}) {
            c=start(L,Lands.firstStage(land),true,244);
            final float[] top={0};
            Painter probe=new OpacityPainter(new RasterPainter(1,1,1),1) {
                public void clipRect(float l,float t,float r,float b){top[0]=Math.max(top[0],t);}
            };
            Lands.backdrop(probe,c,L);Lands.scenery(probe,c,L);MushroomForest.canopy(probe,c,L);
            check("land scenery never clips at header "+land,top[0]<=0);
        }
    }
}
