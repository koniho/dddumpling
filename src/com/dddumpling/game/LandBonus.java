package com.dddumpling.game;

import java.util.Random;

/** One scenery reward per Adventure chapter, with a separate random stream. */
final class LandBonus {
    static final int POINTS=100;
    static final float REVEAL=.65f, ENTRANCE=1.4f, BURST=1.25f;
    final Random random;
    int chapter=-1,stage=-1,selectedStage=-1,collectedStages;
    float age,skyAt,burst=-1;

    LandBonus(long seed) {random=new Random(seed^0x4C414E4447494654L);}
    void reset() {chapter=stage=selectedStage=-1;collectedStages=0;age=skyAt=0;burst=-1;}
    void enter(GameCore c) {
        if(!c.modes.adventure()) {reset();return;}
        int next=(Math.max(1,c.stage)-1)/Boss.EVERY;
        if(next!=chapter) {
            chapter=next;selectedStage=next*Boss.EVERY+1+random.nextInt(4);collectedStages=0;
        }
        if(stage!=c.stage) {stage=c.stage;restartEntrance(c);burst=-1;}
    }
    void restartEntrance(GameCore c) {age=0;skyAt=c.skyClock;}
    float entrance() {float u=Math.max(0,Math.min(1,(age-REVEAL)/ENTRANCE));return 1-(1-u)*(1-u)*(1-u);}
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
        return offered(c) && !c.powerActive() && age>=REVEAL
                && !c.onboarding.briefing && c.onboarding.practice==null;
    }
    void clean(GameCore c) {if(!scene(c))burst=-1;}
    void update(GameCore c,float dt) {
        if(!scene(c))return;
        // Also handles test fixtures or mode transitions that set the stage directly.
        if(stage!=c.stage)enter(c);
        if(!c.powerActive())age+=dt;
        if(burst>=0) {burst+=dt;if(burst>=BURST)burst=-1;}
    }
    static float radius(Layout L) {return L.w*.078f;}
    static float x(GameCore c,Layout L) {return L.w*(Lands.forStage(c.stage)%2==0?.16f:.84f);}
    static float scrollSpeed(GameCore c,Layout L) {
        switch(Lands.forStage(c.stage)) {
            case 0:return L.h*SlimeHills.SCROLL_RATE;
            case 1:return L.w*CrystalField.SPEED[2];
            case 2:return L.h*Underwater.SCROLL;
            case 3:return L.h*.028f;
            case Lands.RAINBOW:return L.h*RainbowLand.BAND_SCROLL_RATE;
            default:return (L.deckTop+radius(L)*6)*.022f;
        }
    }
    static float y(GameCore c,Layout L) {
        LandBonus b=c.landBonus;boolean bubble=Lands.forStage(c.stage)==0;
        float home=L.playTop+(L.deckTop-L.playTop)*(bubble?.32f:.16f);
        float entrance=c.cloudUi.reduceMotion?0:1-b.entrance();
        float offset=bubble?radius(L)*1.8f:L.playTop-radius(L)*1.3f-home;
        return home+Math.max(0,c.skyClock-b.skyAt)*scrollSpeed(c,L)+offset*entrance;
    }
    boolean tap(GameCore c,Layout L,float x,float y) {
        if(c.paused || c.settingsOpen || c.cloudUi.open || !visible(c) || x<0 || x>L.w || y<L.playTop || y>=L.deckTop)return false;
        float dx=x-x(c,L),dy=y-y(c,L),r=radius(L)*1.18f;
        if(dx*dx+dy*dy>r*r || L.keyAt(x,y)>=0)return false;
        collectedStages|=bit();burst=0;
        c.score=(int)Math.min(Integer.MAX_VALUE,(long)c.score+POINTS);
        c.companion.react(RunCompanion.WORD,1f);
        if(c.sound!=null)c.sound.landBonus(Lands.forStage(c.stage));
        return true;
    }
}
