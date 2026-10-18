package com.dddumpling.game;

/** Two taps and a traced shape let the companion net the cat for a key challenge; failure releases a star flurry. */
final class RainbowCat {
    static final int CHASE=0, DASH=1, OPEN=2, CIRCLE=3, CATCH=4;
    static final float TAP_FLASH=.60f, SCARE_HOLD=.70f, SHAKE_OFF=.40f;
    static final float NET_TIME=.75f, NET_RETURN=.45f;
    static final float DASH_TIME=.34f, TRAIL_TIME=2.2f, TRAIL_LENGTH=2.9f*4;
    final Softbody pastry=new Softbody(18,151), head=new Softbody(14,152);
    final float[] route=new float[8],trailX=new float[136],trailY=new float[136],trailAge=new float[136];
    int phase,pointer=-1,taps,trailCount,at,count,captures,cue;
    final TraceGesture gesture=new TraceGesture();
    final int[] prompt=new int[Glyph.COUNT];
    float x,y,vx,vy,r,left,right,top,bottom,clock,flash,turn=1;
    float routeTime,routeDuration,launchLeft,trailTick,timer;
    float circleSweep,netAge,netReturn,catchX,catchY,windUp,releaseWind,netAimX,netAimY,tapFlash,scared,scaredAge;
    float traceLost,lostStart,lostTravel,traceBeat,traceProgress;
    int traceHaptic,nextMeow;
    float voiceLeft;
    boolean circling;
    boolean placed,kids,escaping;
    void reset() {
        phase=CHASE;pointer=-1;taps=trailCount=at=count=captures=cue=0;placed=escaping=false;turn=1;
        circleSweep=netAge=netReturn=catchX=catchY=windUp=releaseWind=netAimX=netAimY=tapFlash=scared=scaredAge=0;circling=false;gesture.clear();
        x=y=vx=vy=clock=flash=routeTime=routeDuration=trailTick=timer=0;launchLeft=1.3f;
        traceLost=lostStart=lostTravel=traceBeat=traceProgress=voiceLeft=0;traceHaptic=0;nextMeow=1;
    }
    private void endStroke() {pointer=-1;circling=false;circleSweep=0;gesture.clear();}
    void release() {endStroke();traceLost=traceBeat=traceProgress=0;traceHaptic=0;}
    int takeTraceHaptic() {int h=traceHaptic;traceHaptic=0;return h;}
    private void loseTrace(GameCore c,float start,float travel) {
        lostStart=start;lostTravel=travel;traceLost=.70f;traceHaptic=2;
        circling=false;circleSweep=0;gesture.clear();
        if(c.sound!=null)c.sound.wrong();
    }
    void meow(GameCore c,int variant,boolean priority) {
        if(!priority && voiceLeft>0)return;
        voiceLeft=CatMeowRecording.duration(variant)+.08f;
        if(c.sound!=null)c.sound.catMeow(variant);
    }
    void shortMeow(GameCore c) {
        if(voiceLeft>0)return;
        meow(c,nextMeow,false);nextMeow=(nextMeow+5)%CatMeowRecording.SHORT_COUNT;
    }
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
        voiceLeft=Math.max(0,voiceLeft-dt);
        traceLost=Math.max(0,traceLost-dt);traceBeat=Math.max(0,traceBeat-dt);
        place(L);clock+=dt;netReturn=Math.max(0,netReturn-dt);flash=Math.max(0,flash-dt*3);
        for(int i=trailCount-1;i>=0;i--) {
            trailAge[i]+=dt;
            if(trailAge[i]>=TRAIL_TIME)trailCount=i;
        }
        tapFlash=Math.max(0,tapFlash-dt);
        if(taps>0 && tapFlash==0 && phase!=OPEN && phase!=CATCH) {
            scaredAge=Math.min(SCARE_HOLD+SHAKE_OFF,scaredAge+dt);
            scared=Math.min(clamp(scaredAge/.12f,0,1),clamp(1-(scaredAge-SCARE_HOLD)/SHAKE_OFF,0,1));
        }
        float target=phase==CIRCLE && circling?gesture.progress():0;
        if(phase==CIRCLE) {
            float follow=1-(float)Math.exp(-dt*18);
            windUp+=(target-windUp)*(1-(float)Math.exp(-dt*12));
            if(circling) {netAimX+=(gesture.lastX-netAimX)*follow;netAimY+=(gesture.lastY-netAimY)*follow;}
        }
        if(escaping && routeTime<routeDuration)advance(dt);
        pose(dt);
    }
    private void pose(float dt) {
        float facing=phase==CIRCLE || phase==CATCH || phase==OPEN?-1:Math.abs(vx)<1?turn:vx<0?-1:1;
        turn+=(facing-turn)*(1-(float)Math.exp(-dt*14));
        float struggle=phase==OPEN?1:phase==CATCH?clamp(netAge/NET_TIME,0,1):0;
        float wiggle=(float)Math.sin(clock*18)*r*.065f*struggle+shakeOffset();
        pastry.moveTo(x+wiggle,y);pastry.pull((float)Math.sin(clock*15),.5f,.13f*struggle);pastry.update(dt);
        head.moveTo(x+turn*r*.88f-wiggle+(float)Math.sin(clock*31)*r*.025f*scared,y+r*.02f+(float)Math.sin(clock*21)*r*.055f*struggle);head.update(dt);
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
        if(phase==CATCH) {
            float before=netAge;netAge+=dt;
            if(before<.34f && netAge>=.34f) {cue=3;flash=1;pastry.squash(.65f);head.squash(.25f);}
            if(netAge>=NET_TIME)open(b);
            return 0;
        }
        if(phase==OPEN) {
            timer-=dt;
            if(timer<=0) {
                flurry(b,rnd,false);resume(L,rnd);launchLeft=4.4f;
            }
            return 0;
        }
        // Let the tap reaction read in place, then finish the escape before accepting another tap.
        if(phase==DASH && taps>0 && !reactionReady())return 0;
        float remaining=phase==CIRCLE?0:dt;
        while(remaining>0) {
            if(routeDuration==0 || routeTime>=routeDuration) {
                if(phase==DASH && taps==2) {if(reactionReady()) {phase=CIRCLE;gesture.setShape(TraceGesture.shape(captures),starShape());}vx=vy=0;break;}
                phase=CHASE;chooseRoute(L,rnd,false);
            }
            float step=Math.min(remaining,routeDuration-routeTime);
            advance(step);remaining-=step;
        }
        pose(0);
        if(phase==DASH && taps==2)return 0;
        launchLeft-=dt;
        if(launchLeft<=0) {
            if(b.boltCount()<3) {int slot=b.tailBolt(rnd,x,y+r*.45f);if(slot>=0)b.bfast[slot]=true;}
            launchLeft=attackInterval();
        }
        return 0;
    }
    private void flurry(Boss b,java.util.Random rnd,boolean afterDamage) {
        b.clearBolts();
        boolean fromLeft=rnd.nextBoolean();int keys=Roster.count(b.rosterFull);
        // Six keys need a readable cadence after a successful prompt, including at 30 FPS.
        float lead=afterDamage?.30f/(Boss.BOLT_TIME*.5f):0,stagger=afterDamage?.24f:.16f;
        for(int i=0;i<keys;i++) {
            int slot=b.tailBolt(rnd,x,y+r*.45f);
            if(slot>=0) {b.bglyph[slot]=Roster.at(b.rosterFull,fromLeft?i:keys-1-i);b.bfast[slot]=true;b.bt[slot]=-lead-i*stagger;}
        }
        cue=2;
    }
    float attackInterval() {return kids?2.3f:1.6f;}
    static float trailFade(float progress) {return clamp((1-progress)*4,0,1);}
    float promptTime() {return (count==6?6f:4.8f)*(kids?1.4f:1);}
    private void open(Boss b) {
        phase=OPEN;vx=vy=0;b.clearBolts();timer=promptTime();cue=1;flash=1;
    }
    private void resume(Layout L,java.util.Random rnd) {
        netReturn=NET_RETURN;windUp=tapFlash=scared=scaredAge=0;release();taps=at=count=0;phase=DASH;chooseRoute(L,rnd,true);launchLeft=attackInterval();
    }
    int press(Boss b,int glyph,Layout L,java.util.Random rnd) {
        if(phase!=OPEN || count==0)return Boss.REBUFF;
        if(prompt[at]!=glyph) {flash=.5f;return Boss.REBUFF;}
        b.hitX=x;b.hitY=y;head.squash(.3f);
        if(++at<count)return Boss.PART;
        captures++;int result=b.damage(1);
        flurry(b,rnd,true);resume(L,rnd);launchLeft=4.4f;
        if(b.beaten) {escaping=true;routeTo(L.w+r*3,top-r*2,.48f,rnd);}
        return result;
    }
    boolean reactionReady() {return tapFlash==0 && (taps==0 || scaredAge>=.12f*.65f);}
    boolean canTap() {return phase==CHASE && reactionReady();}
    float shakeOffset() {
        if(phase==OPEN || phase==CATCH || scaredAge<=SCARE_HOLD)return 0;
        float u=clamp((scaredAge-SCARE_HOLD)/SHAKE_OFF,0,1);
        return (float)(Math.sin(u*Math.PI*6)*Math.sin(u*Math.PI))*r*.18f;
    }
    boolean hit(float px,float py) {
        // Expand both targets by 15% without enlarging the artwork or changing movement.
        float touchRadius=r*1.15f;
        float dx=(px-x)/(touchRadius*1.28f),dy=(py-y)/(touchRadius*1.16f);
        return dx*dx+dy*dy<=1 || distance(px,py,head.centreX(),head.centreY())<touchRadius*.84f;
    }
    float circleX() {return x;}
    float circleY() {return y;}
    float caughtStrength() {
        return phase==OPEN?1:phase==CATCH?clamp((netAge-.34f)/.20f,0,1):0;
    }
    float pouchSway() {return (float)Math.sin(clock*6)*.38f*caughtStrength();}
    float hoopShift() {return r*.16f*(float)Math.sin(clock*6+.6f)*caughtStrength();}
    boolean starShape() {return captures==4;}
    float circleRadius() {return r*(captures==0?2.2f:2.7f);}
    boolean carries(GameCore c) {
        return c.state==GameCore.PLAY && !c.dying() && c.boss.kind==Boss.RAINBOW_CAT
                && (phase==CATCH || phase==OPEN || netReturn>0 || (phase==CIRCLE && windUp>.002f));
    }
    float netAmount() {
        float u=phase==CATCH?clamp(netAge/.34f,0,1):netReturn>0?netReturn/NET_RETURN:1;
        return u*u*(3-2*u);
    }
    private void circlePoint(GameCore c,Layout L,float px,float py) {
        if(!circling)return;
        float start=gesture.start,travel=gesture.travel;
        if(py<L.playTop || py>=L.deckTop) {loseTrace(c,start,travel);return;}
        boolean complete=gesture.move((px-circleX())/circleRadius(),(py-circleY())/circleRadius());
        if(!gesture.active) {loseTrace(c,start,travel);return;}
        circling=gesture.active;circleSweep=gesture.travel/gesture.length*Softbody.TAU;
        if(gesture.progress()>=traceProgress+.035f && traceBeat<=0) {
            traceProgress=gesture.progress();traceBeat=.09f;traceHaptic=Math.max(traceHaptic,1);
        }
        if(complete) {
            traceHaptic=2;traceLost=0;
            phase=CATCH;catchX=x;catchY=y;releaseWind=windUp;timer=netAge=netReturn=0;circling=false;c.boss.clearBolts();
            count=Roster.count(c.boss.rosterFull);at=0;
            for(int i=0;i<count;i++)prompt[i]=Roster.at(c.boss.rosterFull,i);
            for(int i=count-1;i>0;i--) {int j=c.rnd.nextInt(i+1),swap=prompt[i];prompt[i]=prompt[j];prompt[j]=swap;}
            c.companion.react(RunCompanion.BOSS_HIT,1);
            meow(c,CatMeowRecording.SURPRISED,true);
            if(c.sound!=null)c.sound.ninjaSwish(1);
        }
    }
    boolean touch(GameCore c,Layout L,int action,int id,float px,float py) {
        Boss b=c.boss;
        if(action==3){boolean owned=pointer>=0;release();return owned;}
        if(b.kind!=Boss.RAINBOW_CAT || !b.fighting() || c.state!=GameCore.PLAY || c.dying()
                || c.paused || c.settingsOpen || c.onboarding.briefing) {release();return false;}
        place(L);kids=c.kidsRun;
        if(action==1 || action==6) {
            boolean owned=id==pointer;
            if(owned) {
                if(phase==CIRCLE) {
                    circlePoint(c,L,px,py);
                    if(phase==CIRCLE && circling)loseTrace(c,gesture.start,gesture.travel);
                }
                endStroke();
            }
            return owned;
        }
        if(action==0 || action==5) {
            if(pointer>=0 || py>=L.deckTop || py<L.playTop)return false;
            pointer=id;
            if(canTap() && hit(px,py)) {
                taps++;flash=1;tapFlash=TAP_FLASH;scared=scaredAge=0;phase=DASH;vx=vy=0;pastry.squash(.5f);head.squash(.3f);
                if(taps==2)routeTo(L.w*.5f,top+(bottom-top)*.32f,DASH_TIME,c.rnd);
                else chooseRoute(L,c.rnd,true);
                BossPlay.catTap(c,L);
            } else if(phase==CIRCLE) {
                circling=gesture.begin((px-circleX())/circleRadius(),(py-circleY())/circleRadius());
                if(circling) {traceLost=traceProgress=0;traceBeat=.09f;traceHaptic=1;}
                else loseTrace(c,0,0);
                if(circling && windUp<.01f) {netAimX=gesture.lastX;netAimY=gesture.lastY;}
                circleSweep=0;catchX=x;catchY=y;
            }
            return true;
        }
        if(action==2 && id==pointer && phase==CIRCLE)circlePoint(c,L,px,py);
        return id==pointer;
    }
    String companionHint() {
        if(phase==OPEN)return "MATCH IN ORDER!";
        if(phase==CATCH)return "GOT YOU!";
        if(phase==CIRCLE) {
            if(traceLost>0)return "TRACE LOST! TRY AGAIN!";
            switch(captures) {
                case 0:return "CIRCLE THE CAT!";
                case 1:return "TRACE THE HEART!";
                case 2:return "TRACE THE DIAMOND!";
                case 3:return "TRACE THE EIGHT!";
                default:return "TRACE THE STAR!";
            }
        }
        return phase==DASH || tapFlash>0?"FOLLOW RAINBOW!":taps==0?"TAP CAT TWICE!":"ONE MORE TAP!";
    }
    static float clamp(float v,float low,float high){return Math.max(low,Math.min(high,v));}
    static float distance(float ax,float ay,float bx,float by){return (float)Math.hypot(ax-bx,ay-by);}
}
