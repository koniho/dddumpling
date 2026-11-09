package com.dddumpling.game;

import java.util.Random;

/** One scenery reward per Adventure chapter, with a separate random stream. */
final class LandBonus {
    static final int POINTS=100;
    static final float MIN_DELAY=4.5f, MAX_DELAY=8f, BUBBLE_RISE=2.8f, BURST=1.25f;
    final Random random;
    int chapter=-1,stage=-1,selectedStage=-1,collectedStages;
    float age,skyAt,burst=-1,delay=MIN_DELAY,sideX,entryDepth,seabedLane,seabedStart;
    int bubbleId;
    long visitSeed;

    LandBonus(long seed) {random=new Random(seed^0x4C414E4447494654L);}
    void reset() {chapter=stage=selectedStage=-1;collectedStages=0;age=skyAt=0;burst=-1;delay=MIN_DELAY;sideX=entryDepth=seabedLane=seabedStart=0;bubbleId=0;visitSeed=0;}
    void enter(GameCore c) {
        if(!c.modes.adventure()) {reset();return;}
        int next=(Math.max(1,c.stage)-1)/Boss.EVERY;
        if(next!=chapter) {
            chapter=next;selectedStage=next*Boss.EVERY+1+random.nextInt(4);collectedStages=0;visitSeed=random.nextLong();
        }
        if(stage!=c.stage) {
            stage=c.stage;
            Random placement=new Random(visitSeed^((long)stage*0x5343454E455259L));
            delay=MIN_DELAY+(MAX_DELAY-MIN_DELAY)*placement.nextFloat();
            bubbleId=placement.nextInt(10000);
            float inset=.12f+.08f*placement.nextFloat();sideX=placement.nextBoolean()?inset:1-inset;
            entryDepth=1.1f+.7f*placement.nextFloat();
            seabedLane=placement.nextFloat()*1.6f-.8f;seabedStart=.12f+.10f*placement.nextFloat();
            restartEntrance(c);burst=-1;
        }
    }
    void restartEntrance(GameCore c) {age=0;skyAt=c.skyClock;}
    int bubbleSeed() {return 1391+bubbleId*83;}
    float bubbleRise(GameCore c) {return c.cloudUi.reduceMotion?1:Math.max(0,Math.min(1,(age-delay)/BUBBLE_RISE));}
    static boolean opening(GameCore c) {return c.stageBanner>0 || c.stageGap>0 || c.startFade>0;}
    static boolean scene(GameCore c) {
        return c.state==GameCore.PLAY && c.modes.adventure() && !c.survival.active
                && !c.timeAttack.active && !c.boss.active() && !Cave.active(c)
                && c.onboarding.teacher==null && Lands.playable(Lands.forStage(c.stage));
    }
    boolean forced(GameCore c) {return BuildFlags.DEVELOPER && c.devLandBonuses;}
    private int bit() {return 1<<((stage-1)%Boss.EVERY);}
    boolean offered(GameCore c) {
        return scene(c) && stage==c.stage && c.stage%Boss.EVERY!=0
                && (forced(c) || stage==selectedStage) && (collectedStages&bit())==0;
    }
    boolean visible(GameCore c) {
        return offered(c) && !c.powerActive() && !opening(c) && age>delay
                && !c.onboarding.briefing && c.onboarding.practice==null;
    }
    void clean(GameCore c) {if(!scene(c))burst=-1;}
    void update(GameCore c,float dt) {
        if(!scene(c))return;
        // Also handles test fixtures or mode transitions that set the stage directly.
        if(stage!=c.stage)enter(c);
        if(!c.powerActive() && !opening(c))age+=dt;
        if(age<=delay)skyAt=c.skyClock;
        if(burst>=0) {burst+=dt;if(burst>=BURST)burst=-1;}
    }
    static float radius(Layout L) {return L.w*.078f;}
    static float clamTravel(GameCore c) {return Math.min(1,c.landBonus.seabedStart+Math.max(0,c.skyClock-c.landBonus.skyAt)*(.20f/12));}
    static float flamingoTravel(GameCore c) {
        return Math.min(1,.12f+(c.landBonus.entryDepth-1.1f)*.10f+Math.max(0,c.skyClock-c.landBonus.skyAt)*SwampVista.SPEED);
    }
    static float size(GameCore c,Layout L) {
        int land=Lands.forStage(c.stage);
        if(land==Underwater.LAND)return radius(L)*UnderwaterVista.scale(clamTravel(c));
        if(land==Lands.SWAMP)return radius(L)*(.24f+1.10f*SwampVista.depth(flamingoTravel(c)));
        if(land==Lands.RAINBOW) {
            float u=c.landBonus.burst<0?0:c.cloudUi.reduceMotion?1:Math.min(1,c.landBonus.burst/.22f);
            return radius(L)*(.75f+.25f*u*u*(3-2*u));
        }
        return radius(L);
    }
    private static float coinOrbit(GameCore c,Layout L,boolean vertical) {
        if(Lands.forStage(c.stage)!=Lands.RAINBOW || c.cloudUi.reduceMotion)return 0;
        float settle=c.landBonus.burst<0?1:Math.max(0,1-c.landBonus.burst/.24f);
        float angle=(c.landBonus.age-c.landBonus.delay)*1.4f;
        return radius(L)*.12f*settle*(float)(vertical?Math.sin(angle):Math.cos(angle));
    }
    static float x(GameCore c,Layout L) {
        int land=Lands.forStage(c.stage);
        if(land==0)return SlimeBubbles.surfaceX(c.landBonus.bubbleSeed(),L);
        if(land==Underwater.LAND)return L.w*(.5f+c.landBonus.seabedLane*(.04f+.30f*UnderwaterVista.depth(clamTravel(c))));
        if(land==Lands.SWAMP)return L.w*(.5f+SwampVista.bend(c.skyClock,SwampVista.depth(flamingoTravel(c)))+(c.landBonus.sideX<.5f?-1:1)*(.13f+.38f*SwampVista.depth(flamingoTravel(c))));
        return L.w*c.landBonus.sideX+coinOrbit(c,L,false);
    }
    static float scrollSpeed(GameCore c,Layout L) {
        switch(Lands.forStage(c.stage)) {
            case 0:return L.h*SlimeHills.SCROLL_RATE;
            case 1:return L.w*CrystalField.SPEED[2];
            case 2:return L.h*Underwater.SCROLL;
            case 3:return ForestTerrain.speed(L);
            case Lands.RAINBOW:return L.h*RainbowLand.BAND_SCROLL_RATE;
            default:return (L.deckTop+radius(L)*6)*.022f;
        }
    }
    static float surfaceY(GameCore c,Layout L) {
        return SlimeBubbles.surfaceY(c.landBonus.bubbleSeed(),L)+Math.max(0,c.skyClock-c.landBonus.skyAt)*scrollSpeed(c,L);
    }
    static float y(GameCore c,Layout L) {
        LandBonus b=c.landBonus;
        if(Lands.forStage(c.stage)==0)return SlimeBubbles.emergeY(surfaceY(c,L),radius(L),b.bubbleRise(c));
        if(Lands.forStage(c.stage)==Underwater.LAND)return L.h*UnderwaterVista.rootY(clamTravel(c))-size(c,L)*.8f;
        if(Lands.forStage(c.stage)==Lands.SWAMP)return SwampVista.rootY(L,flamingoTravel(c),x(c,L))-size(c,L)*.86f;
        return L.playTop-radius(L)*b.entryDepth+Math.max(0,c.skyClock-b.skyAt)*scrollSpeed(c,L)+coinOrbit(c,L,true);
    }
    boolean tap(GameCore c,Layout L,float x,float y) {
        if(c.paused || c.settingsOpen || c.cloudUi.open || !visible(c) || x<0 || x>L.w || y<L.playTop || y>=L.deckTop)return false;
        if(Lands.forStage(c.stage)==0 && bubbleRise(c)<1 && y>surfaceY(c,L))return false;
        float dx=x-x(c,L),dy=y-y(c,L),r=Math.max(size(c,L)*1.18f,L.unit*1.25f);
        if(dx*dx+dy*dy>r*r || L.keyAt(x,y)>=0)return false;
        collectedStages|=bit();burst=0;
        c.score=(int)Math.min(Integer.MAX_VALUE,(long)c.score+POINTS);
        c.companion.react(RunCompanion.WORD,1f);
        if(c.sound!=null)c.sound.landBonus(Lands.forStage(c.stage));
        return true;
    }
}
