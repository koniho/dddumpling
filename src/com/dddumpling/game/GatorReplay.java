package com.dddumpling.game;

/** Isolated, fixed-step developer scene; no store, rewards, or player input. */
final class GatorReplay {
    static final float STEP=1f/60f;
    static final int LENGTH=36*60;
    GameCore core;
    int tick,loops;
    private float remainder;
    GatorReplay() {reset();}
    private void reset() {
        core=new GameCore(null,168,false);
        core.collected=Collect.MASK;core.landSeen=LandPicker.stateMask();
        core.onboarding.saved=Onboarding.SKIPPED;core.onboarding.savedPowers=-1;
        core.modes.restore(ModeSelector.TIME_ATTACK);core.timeAttack.selected=Boss.ALLIGATOR;
        core.startGame();core.stageBanner=0;core.landBlend=1;tick=0;
    }
    void update(float elapsed,Layout l) {
        remainder+=Math.max(0,Math.min(.1f,elapsed));
        while(remainder>=STEP) {remainder-=STEP;step(l);}
    }
    void step(Layout l) {
        if(tick>=LENGTH) {loops++;reset();}
        Boss b=core.boss;Alligator a=b.alligator;
        if(tick==10*60 || tick==20*60) {
            b.hp=b.hpMax*(tick==10*60?.5f:.25f)+.5f;
            b.damage(1);
        }
        if(tick==29*60) {
            a.staggerCombo();a.longWindow=true;a.daze=Alligator.DAZE;a.windowDamage=0;
        }
        // Allow a real player hit in each phase; otherwise defend using the game's touch path.
        boolean defend=tick%600<480;
        if(b.fighting() && !a.showoff() && defend) {
            if(a.comboActive) {
                for(Alligator.Strike strike:a.combo)if(strike.limb>=0 && !strike.blocked && strike.attack>=.40f) {
                    swipe(l,strike.tipX,strike.tipY);break;
                }
            } else if(a.limb>=0 && !a.blocked && a.attack>=.40f)swipe(l,a.tipX,a.tipY);
            float floor=b.hpMax*(tick<600?.60f:tick<1200?.35f:.12f);
            if(a.daze>0 && b.hp>floor && tick%12==0 && (!a.windowHit || a.longWindow))core.tapKey(0,l);
        }
        core.lives=3;core.update(STEP,l);tick++;
    }
    private void swipe(Layout l,float x,float y) {
        core.boss.alligator.touch(core,l,0,168,x-l.w*.12f,y);
        core.boss.alligator.touch(core,l,1,168,x+l.w*.12f,y);
    }
}
