package com.dddumpling.game;

/** A fast three-tap chase opens a shuffled key challenge; failure releases a star flurry. */
final class RainbowCat {
    static final int CHASE=0, DASH=1, OPEN=2;
    static final float DASH_TIME=.34f, TRAIL_TIME=2.2f, TRAIL_LENGTH=2.9f*4;
    final Softbody pastry=new Softbody(18,151), head=new Softbody(14,152);
    final float[] route=new float[8],trailX=new float[136],trailY=new float[136],trailAge=new float[136];
    int phase,pointer=-1,taps,trailCount,at,count,captures,cue;
    final int[] prompt=new int[Glyph.COUNT];
    float x,y,vx,vy,r,left,right,top,bottom,clock,flash,turn=1;
    float routeTime,routeDuration,launchLeft,trailTick,timer;
    boolean placed,kids,escaping;
    void reset() {
        phase=CHASE;pointer=-1;taps=trailCount=at=count=captures=cue=0;placed=escaping=false;turn=1;
        x=y=vx=vy=clock=flash=routeTime=routeDuration=trailTick=timer=0;launchLeft=1.3f;
    }
    void release() {pointer=-1;}
    void place(Layout L) {
        r=L.w*.073f*1.5f;
        left=L.playLeft+r*.95f;right=L.playRight-r*.95f;
        top=L.playTop+r*1.15f;bottom=L.dangerY-r*1.10f;
        if(!placed) {
            x=left+(right-left)*.25f;y=top+(bottom-top)*.45f;
            pastry.reset(x,y,r,1.15f);head.reset(x+r,y+r*.1f,r*.65f);
            pastry.shapeMemory=head.shapeMemory=12;placed=true;
        }
    }
    void animate(float dt,Layout L) {
        place(L);clock+=dt;flash=Math.max(0,flash-dt*3);
        for(int i=trailCount-1;i>=0;i--) {
            trailAge[i]+=dt;
            if(trailAge[i]>=TRAIL_TIME)trailCount=i;
        }
        if(escaping && routeTime<routeDuration)advance(dt);
        pose(dt);
    }
    private void pose(float dt) {
        float facing=Math.abs(vx)<1?turn:vx<0?-1:1;
        turn+=(facing-turn)*(1-(float)Math.exp(-dt*14));
        pastry.moveTo(x,y);pastry.update(dt);
        head.moveTo(x+turn*r*.96f,y+r*.1f);head.update(dt);
    }
    private void chooseRoute(Layout L,java.util.Random rnd,boolean dash) {
        float targetX=x,targetY=y,bestDistance=-1;
        float minimum=L.w*(dash?.48f:.18f+rnd.nextFloat()*.25f);
        // Sample the whole arena, including runs along the same side, instead of alternating corners.
        for(int attempt=0;attempt<12;attempt++) {
            float tx=left+(right-left)*rnd.nextFloat(),ty=top+(bottom-top)*rnd.nextFloat();
            float distance=distance(x,y,tx,ty);
            if(distance>bestDistance) {targetX=tx;targetY=ty;bestDistance=distance;}
            if(distance>=minimum) {targetX=tx;targetY=ty;break;}
        }
        float speed=L.w*(kids?.82f:1.2f)*(.75f+rnd.nextFloat()*.80f);
        float duration=dash?DASH_TIME*(.82f+rnd.nextFloat()*.30f)
                :Math.max(.26f+rnd.nextFloat()*.16f,distance(x,y,targetX,targetY)/speed);
        routeTo(targetX,targetY,duration,rnd);
    }
    private void routeTo(float tx,float ty,float duration,java.util.Random rnd) {
        routeTime=0;routeDuration=duration;
        route[0]=x;route[1]=y;
        // Carry the incoming tangent into the new curve; never teleport on a tap or a turn.
        route[2]=clamp(x+vx*duration/3,left,right);route[3]=clamp(y+vy*duration/3,top,bottom);
        route[4]=clamp(tx+(rnd.nextFloat()-.5f)*(right-left)*.8f,left,right);
        route[5]=clamp(ty+(rnd.nextFloat()-.5f)*(bottom-top)*.8f,top,bottom);
        route[6]=tx;route[7]=ty;
    }
    private void advance(float dt) {
        float ox=x,oy=y;routeTime=Math.min(routeDuration,routeTime+dt);
        float u=routeTime/routeDuration,v=1-u;
        x=v*v*v*route[0]+3*v*v*u*route[2]+3*v*u*u*route[4]+u*u*u*route[6];
        y=v*v*v*route[1]+3*v*v*u*route[3]+3*v*u*u*route[5]+u*u*u*route[7];
        vx=3*(v*v*(route[2]-route[0])+2*v*u*(route[4]-route[2])+u*u*(route[6]-route[4]))/routeDuration;
        vy=3*(v*v*(route[3]-route[1])+2*v*u*(route[5]-route[3])+u*u*(route[7]-route[5]))/routeDuration;
        if(distance(ox,oy,x,y)>.001f) {
            trailTick+=dt;
            while(trailTick>=1f/60) {
                trailTick-=1f/60;
                int n=Math.min(trailCount,trailX.length-1);
                for(int i=n;i>0;i--){trailX[i]=trailX[i-1];trailY[i]=trailY[i-1];trailAge[i]=trailAge[i-1];}
                float f=dt>0?clamp(1-trailTick/dt,0,1):1;
                trailX[0]=ox+(x-ox)*f;trailY[0]=oy+(y-oy)*f;trailAge[0]=trailTick;
                trailCount=Math.min(trailX.length,trailCount+1);
            }
        }
    }
    int update(Boss b,float dt,Layout L,java.util.Random rnd) {
        place(L);
        if(phase==OPEN) {
            timer-=dt;
            if(timer<=0) {
                b.clearBolts();
                for(int i=0;i<count;i++) {
                    int slot=b.tailBolt(rnd,x,y+r*.45f);
                    if(slot>=0) {b.bglyph[slot]=prompt[i];b.bfast[slot]=true;b.bt[slot]=-i*.16f;}
                }
                cue=2;resume(L,rnd);launchLeft=4.4f;
            }
            return 0;
        }
        float remaining=dt;
        while(remaining>0) {
            if(routeDuration==0 || routeTime>=routeDuration) {
                if(phase==DASH && taps==3) {open(b);break;}
                phase=CHASE;chooseRoute(L,rnd,false);
            }
            float step=Math.min(remaining,routeDuration-routeTime);
            advance(step);remaining-=step;
        }
        pose(0);
        if(phase==OPEN || taps==3)return 0;
        launchLeft-=dt;
        if(launchLeft<=0) {
            if(b.boltCount()<3) {int slot=b.tailBolt(rnd,x,y+r*.45f);if(slot>=0)b.bfast[slot]=true;}
            launchLeft=attackInterval();
        }
        return 0;
    }
    float attackInterval() {return kids?2.3f:1.6f;}
    static float trailFade(float progress) {return clamp((1-progress)*4,0,1);}
    float promptTime() {return (count==6?6f:4.8f)*(kids?1.4f:1);}
    private void open(Boss b) {
        phase=OPEN;vx=vy=0;b.clearBolts();timer=promptTime();cue=1;flash=1;
    }
    private void resume(Layout L,java.util.Random rnd) {
        taps=at=count=0;phase=DASH;chooseRoute(L,rnd,true);launchLeft=attackInterval();
    }
    int press(Boss b,int glyph,Layout L,java.util.Random rnd) {
        if(phase!=OPEN || count==0)return Boss.REBUFF;
        if(prompt[at]!=glyph) {flash=.5f;return Boss.REBUFF;}
        b.hitX=x;b.hitY=y;head.squash(.3f);
        if(++at<count)return Boss.PART;
        captures++;resume(L,rnd);
        int result=b.damage(1);
        if(b.beaten) {escaping=true;routeTo(L.w+r*3,top-r*2,.48f,rnd);}
        return result;
    }
    boolean hit(float px,float py) {
        // The face and pastry are both valid targets, with a small forgiving margin.
        float dx=(px-x)/(r*1.28f),dy=(py-y)/(r*1.16f);
        return dx*dx+dy*dy<=1 || distance(px,py,head.centreX(),head.centreY())<r*.84f;
    }
    boolean touch(GameCore c,Layout L,int action,int id,float px,float py) {
        Boss b=c.boss;
        if(action==3){boolean owned=pointer>=0;release();return owned;}
        if(action==1 || action==6) {boolean owned=id==pointer;if(owned)release();return owned;}
        if(b.kind!=Boss.RAINBOW_CAT || !b.fighting() || c.state!=GameCore.PLAY || c.dying()
                || c.paused || c.settingsOpen || c.onboarding.briefing) {release();return false;}
        place(L);kids=c.kidsRun;
        if(action==0 || action==5) {
            if(pointer>=0 || py>=L.deckTop || py<L.playTop)return false;
            pointer=id;
            if(phase==CHASE && hit(px,py)) {
                taps++;flash=1;phase=DASH;pastry.squash(.5f);head.squash(.3f);
                if(taps==3) {
                    count=Roster.count(b.rosterFull);at=0;
                    for(int i=0;i<count;i++)prompt[i]=Roster.at(b.rosterFull,i);
                    for(int i=count-1;i>0;i--) {int j=c.rnd.nextInt(i+1),swap=prompt[i];prompt[i]=prompt[j];prompt[j]=swap;}
                    routeTo(L.w*.5f,top+(bottom-top)*.30f,DASH_TIME,c.rnd);
                } else chooseRoute(L,c.rnd,true);
                BossPlay.catTap(c,L);
            }
            return true;
        }
        return id==pointer;
    }
    String companionHint() {
        if(phase==OPEN)return "MATCH IN ORDER!";
        return phase==DASH?"FOLLOW RAINBOW!":taps==0?"TAP CAT 3 TIMES!":taps==1?"TWO TAPS TO GO!":"ONE MORE TAP!";
    }
    static float clamp(float v,float low,float high){return Math.max(low,Math.min(high,v));}
    static float distance(float ax,float ay,float bx,float by){return (float)Math.hypot(ax-bx,ay-by);}
}
