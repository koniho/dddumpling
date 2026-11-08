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
            check("themed entrance direction "+land,land==0?settled<entrance:settled==entrance);
            float travel=land==2?12:4;c.skyClock+=travel;float moved=LandBonus.y(c,L);
            check("bonus shares scenery travel "+land,land==2?Math.abs(moved+LandBonus.size(c,L)*.8f-L.h*UnderwaterVista.rootY(LandBonus.clamTravel(c)))<.01f:Math.abs(moved-settled-travel*LandBonus.scrollSpeed(c,L))<.01f);
            c.paused=true;c.update(.1f,L);
            check("pause holds world position "+land,LandBonus.y(c,L)==moved);
            c.paused=false;c.settingsOpen=true;c.update(.1f,L);
            check("settings hold world position "+land,LandBonus.y(c,L)==moved);
            c.settingsOpen=false;
            check("old fixed target does not collect moving scenery "+land,!c.landBonus.tap(c,L,LandBonus.x(c,L),settled));
            c.skyClock+=L.h/LandBonus.scrollSpeed(c,L);
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
}
