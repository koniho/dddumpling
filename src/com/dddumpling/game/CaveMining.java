package com.dddumpling.game;

/** Delivered carts are durable; loose rocks and an unfinished sequence belong to this visit. */
final class CaveMining {
    static final int CARTS=5, LOADS=5, DIG=0, FULL=1, PUSH=2, REPORT=3, ADVANCE=4;
    static final float TIME=18f, READY=1.1f, PUSH_TIME=1.25f, LOAD_TIME=.55f, WALK_TIME=.48f, REPORT_TIME=2.3f, DROP=.42f;
    final int[] sequence=new int[4];
    final float[] falling=new float[LOADS];
    final CaveMiningScene scene=new CaveMiningScene();
    final CaveMiningInput input=new CaveMiningInput();
    int carts,length,pos,loads,phase,direction=1;
    float left,ready,travel,report,strike,bad,cartX=.25f,pushStart;
    boolean active,won,paid;
    void begin(GameCore c) {
        active=true;won=paid=false;phase=DIG;left=TIME;ready=READY;report=travel=strike=bad=0;
        input.release();scene.reset();newCart(c);
        if(carts>=CARTS)finish(true);
    }
    private void newCart(GameCore c) {
        length=Math.min(4,2+carts);pos=loads=0;cartX=.25f;
        for(int i=0;i<falling.length;i++)falling[i]=0;
        int[] keys=new int[Roster.count(c.playRosterFull())];
        for(int i=0;i<keys.length;i++)keys[i]=Roster.at(c.playRosterFull(),i);
        for(int i=0;i<length;i++) {
            int pick=i+c.rnd.nextInt(keys.length-i),key=keys[pick];keys[pick]=keys[i];keys[i]=key;
            sequence[i]=key;
        }
    }
    boolean digging() {return active && phase==DIG && ready<=0;}
    boolean swipeReady() {return active && phase==FULL;}
    boolean accepts(GameCore c) {return c.state==GameCore.BONUS && active && !c.paused && !c.settingsOpen;}
    void press(GameCore c,int g) {
        if(!accepts(c) || !digging() || !Roster.active(c.playRosterFull(),g))return;
        c.keyPress[g]=1;
        if(g!=sequence[pos]) {
            pos=g==sequence[0]?1:0;bad=1;c.keyBad[g]=1;
            if(c.sound!=null)c.sound.wrong();
            return;
        }
        scene.hit(c,pos/(float)(length-1));pos++;strike=.6f;
        if(c.sound!=null)c.sound.squish(g,1);
        if(pos==length) {
            c.onboarding.learn(c,TutorialSpeech.DIG);
            pos=0;falling[loads++]=DROP;strike=1;
            if(c.sound!=null)c.sound.clearWord();
            phase=ADVANCE;travel=0;input.release();
        }
    }
    void launch(GameCore c,int dir) {
        if(!accepts(c) || !swipeReady())return;
        c.onboarding.learn(c,TutorialSpeech.CART);
        direction=dir<0?-1:1;pushStart=cartX;travel=0;phase=PUSH;input.release();
        // Save when the swipe commits, so leaving during the helpers' animation cannot lose a cart.
        carts=Math.min(CARTS,carts+1);save(c);
        if(c.sound!=null)c.sound.paradeJoin();
    }
    void save(GameCore c) {if(c.store!=null)c.store.saveMineCarts(carts);}
    void update(GameCore c,float dt) {
        scene.update(c,dt);
        strike=Math.max(0,strike-dt*3);bad=Math.max(0,bad-dt*4);
        for(int i=0;i<loads;i++)falling[i]=Math.max(0,falling[i]-dt);
        if(phase==REPORT){report=Math.max(0,report-dt);return;}
        if(ready>0){ready=Math.max(0,ready-dt);return;}
        if(phase==ADVANCE) {
            travel=Math.min(WALK_TIME,travel+dt);
            if(travel>=WALK_TIME){scene.distance+=CaveMiningScene.STRIDE;travel=0;phase=loads==LOADS?FULL:DIG;}
            return;
        }
        if(phase==PUSH) {
            travel=Math.min(PUSH_TIME,travel+dt);
            float t=Math.max(0,(travel-LOAD_TIME)/(PUSH_TIME-LOAD_TIME));cartX=pushStart+direction*(1.65f*t*t);
            if(travel>=PUSH_TIME) {
                if(carts>=CARTS)finish(true);
                else {newCart(c);phase=DIG;}
            }
            return;
        }
        if(input.pointer<0)cartX+=(.25f-cartX)*Math.min(1,dt*12);
        left=Math.max(0,left-dt);
        if(left<=0)finish(false);
    }
    private void finish(boolean success) {
        phase=REPORT;won=success;report=REPORT_TIME;input.release();
    }
    void stop(){active=false;input.release();scene.reset();}
}
