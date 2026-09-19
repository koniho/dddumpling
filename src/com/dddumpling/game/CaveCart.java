package com.dddumpling.game;

/** The former rhythm slot now holds a checkpointed minecart balance ride. */
final class CaveCart {
    static final int TRACK=20, RIDE=0, REPORT=1;
    static final float READY=1.4f, RUN_TIME=10f, SEGMENT=.95f, REPORT_TIME=2.3f, LEAN_SPEED=4.8f;
    static final float RED=.52f, RED_GRACE=1.25f;
    final CaveCartInput input=new CaveCartInput(this);
    final CaveCartScene scene=new CaveCartScene();
    int progress,phase;
    float ready,elapsed,segment,report,lean,intent,hold,balance,aligned,turn,grace,danger,rollTick,squealTick;
    boolean active,won,paid,spilled;
    void begin(GameCore c){
        active=true;won=paid=spilled=false;phase=RIDE;ready=READY;
        elapsed=segment=report=lean=intent=hold=balance=aligned=rollTick=squealTick=0;
        danger=0;grace=.25f;input.release();scene.reset();turn=curve(progress);
        if(progress>=TRACK)finish(c,true,false);
    }
    static float bend(int index){
        if(index<0)return 0;
        int side=(index%4==0||index%4==3)?-1:1;
        return side*(.62f+.32f*Draw.hash(index*71+13));
    }
    static float curve(float at){
        int i=(int)at;float t=Math.min(1,(at-i)/.55f);t=t*t*(3-2*t);
        return bend(i-1)+(bend(i)-bend(i-1))*t;
    }
    boolean accepts(GameCore c){return active && c.state==GameCore.BONUS && !c.paused && !c.settingsOpen && phase==RIDE;}
    void steer(float value){intent=Math.max(-1,Math.min(1,value));hold=.7f;}
    void press(GameCore c,int g){
        if(!accepts(c)||!Roster.active(c.playRosterFull(),g))return;
        int count=Roster.count(c.playRosterFull()),index=0;
        while(index<count && Roster.at(c.playRosterFull(),index)!=g)index++;
        steer(index<count/2?-.85f:.85f);c.keyPress[g]=1;
    }
    void save(GameCore c){if(c.store!=null)c.store.saveCartTrack(progress);}
    void update(GameCore c,float dt){
        if(!active)return;
        // Fixed substeps make a delayed frame obey the same balance and checkpoint rules.
        while(dt>0){float step=Math.min(dt,1f/120);tick(c,step);dt-=step;}
    }
    private void tick(GameCore c,float dt){
        scene.update(dt);
        if(phase==REPORT){report=Math.max(0,report-dt);return;}
        if(input.pointer<0){hold=Math.max(0,hold-dt);if(hold==0)intent=0;}
        lean+=Math.max(-LEAN_SPEED*dt,Math.min(LEAN_SPEED*dt,intent-lean));
        if(ready>0){ready=Math.max(0,ready-dt);return;}
        elapsed+=dt;segment+=dt;grace=Math.max(0,grace-dt);
        turn=curve(progress+Math.min(segment/SEGMENT,.99999f));
        balance+=((turn-lean)*2.6f-balance*1.15f)*dt;
        balance=Math.max(-1,Math.min(1,balance));
        danger=Math.abs(balance)>RED?danger+dt:0;
        if(Math.abs(turn-lean)<.40f && Math.abs(lean)>.25f && turn*lean>0)aligned+=dt;
        rollTick-=dt;squealTick-=dt;
        scene.rumble=Math.max(scene.rumble,.16f+.06f*(float)Math.sin(scene.clock*24));
        if(rollTick<=0){rollTick=CartRecording.DURATION;if(c.sound!=null)c.sound.caveEvent(Sfx.CART_ROLL);}
        if(Math.abs(balance)>RED && squealTick<=0){squealTick=.65f;scene.feedback=1;scene.rumble=.6f;if(c.sound!=null)c.sound.caveEvent(Sfx.CART_SQUEAL);}
        if(grace==0 && danger>=RED_GRACE){finish(c,false,true);return;}
        if(segment>=SEGMENT){
            if(aligned<.24f)segment=SEGMENT-.00001f;
            else {
                segment-=SEGMENT;aligned=0;progress=Math.min(TRACK,progress+1);save(c);
                scene.feedback=1;scene.pulse=.35f;
                if(progress>=TRACK){finish(c,true,false);return;}
            }
        }
        if(elapsed>=RUN_TIME)finish(c,false,false);
    }
    private void finish(GameCore c,boolean success,boolean fall){
        phase=REPORT;won=success;spilled=fall;report=REPORT_TIME;input.release();
        if(fall){scene.rumble=1;scene.feedback=2;scene.spillAge=0;if(c.sound!=null)c.sound.caveEvent(Sfx.CART_TUMBLE);}
        else if(c.sound!=null)c.sound.caveEvent(Sfx.MINING_CHEER);
    }
    void stop(){active=false;input.release();scene.reset();}
}
