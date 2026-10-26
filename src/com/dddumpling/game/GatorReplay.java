package com.dddumpling.game;

/** Isolated, fixed-step developer scene; no store, rewards, or player input. */
final class GatorReplay {
    static final float STEP=1f/60f;
    static final int LENGTH=36*60;
    GameCore core;
    int tick,loops;
    private float remainder;
    private final boolean slime;
    private final int kind;
    GatorReplay() {this(false);}
    GatorReplay(boolean slime) {this(slime?Boss.SLIME:Boss.ALLIGATOR);}
    GatorReplay(int kind) {this.kind=kind;this.slime=kind==Boss.SLIME;reset();}
    private void reset() {
        core=new GameCore(null,168,false);
        core.collected=Collect.MASK;core.landSeen=LandPicker.stateMask();
        core.onboarding.saved=Onboarding.SKIPPED;core.onboarding.savedPowers=-1;
        core.modes.restore(ModeSelector.TIME_ATTACK);core.timeAttack.selected=kind;
        if(kind==Boss.OCTOPUS)core.modes.restore(ModeSelector.ADVENTURE);
        core.startGame();
        if(kind==Boss.OCTOPUS){core.enterStage(Lands.firstStage(Underwater.LAND)+1);core.stageGap=0;}
        core.stageBanner=0;core.landBlend=1;tick=0;
    }
    void update(float elapsed,Layout l) {
        remainder+=Math.max(0,Math.min(.1f,elapsed));
        while(remainder>=STEP) {remainder-=STEP;step(l);}
    }
    void step(Layout l) {
        if(tick>=LENGTH) {loops++;reset();}
        if(kind==Boss.OCTOPUS) {
            if(tick==18*60){core.enterStage(WorldCatalog.bossStage(Boss.OCTOPUS));core.stageBanner=0;}
            if(tick%90==0 && !core.enemies.isEmpty()) {
                GameCore.Enemy e=core.enemies.get(0);core.destroyWord(e,core.enemyCentreX(e),e.y,l,false);
            }
            core.lives=3;core.update(STEP,l);tick++;return;
        }
        Boss b=core.boss;
        if(slime) {
            if(b.fighting() && tick%18==0)for(int key=0;key<Glyph.COUNT;key++)
                if(b.wants(key)){core.tapKey(key,l);break;}
            if(b.fighting() && tick%240==0)b.hurt=1;
            core.lives=3;core.update(STEP,l);tick++;return;
        }
        if(kind==Boss.SPLITTER) {
            boolean finish=tick>=28*60;
            if(b.fighting() && tick%(finish?30:150)==0)for(int n=0;n<Boss.DIVIDE_NODES;n++) {
                if(!b.nodeActive(n) || (!finish && b.nodeDepth(n)>=Boss.DIVIDE_LEVELS))continue;
                b.pieceHits[n]=Boss.DIVIDE_HITS;
                if(b.beginPinch(100))b.pinch(100*(Boss.DIVIDE_SCALE+.02f));
                break;
            }
            core.lives=3;core.update(STEP,l);tick++;return;
        }
        Alligator a=b.alligator;
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
