package com.dddumpling.game;

/** Tap, trace, then alternate a fixed left/right pair; each net can damage only the current health band. */
final class RainbowCat {
    static final int CHASE=0, DASH=1, OPEN=2, CIRCLE=3, CATCH=4, ESCAPE=5;
    static final int HITS_PER_BAND=20;
    static final float TAP_FLASH=.60f, SCARE_HOLD=.70f, SHAKE_OFF=.40f;
    static final float NET_TIME=.75f, NET_RETURN=.45f, ESCAPE_TIME=1.1f;
    static final float SWEEP_SETUP=.25f, SWEEP_LEAD=.08f, SWEEP_STEP=.288f, DOUBLE_PAIR_GAP=.12f;
    static final float DASH_TIME=.34f, TRAIL_TIME=2.2f, TRAIL_LENGTH=2.9f*4;
    final Softbody pastry=new Softbody(18,151), head=new Softbody(14,152);
    final float[] route=new float[8],trailX=new float[136],trailY=new float[136],trailAge=new float[136];
    int phase,pointer=-1,taps,trailCount,captures,cue,promptKey=-1,windowHits;
    final float[] arrivalStar=new float[16],arrivalRay=new float[8],arrivalRibbon=new float[(RainbowCatArrival.TRAIL_STEPS+1)*4];
    final TraceGesture gesture=new TraceGesture();
    final RainbowCatRocket rocket=new RainbowCatRocket();
    static final float HIT_CONE_TIME=.34f;
    final float[] hitConeLife=new float[4],hitConeX=new float[4],hitConeY=new float[4],hitConeWork=new float[8];
    final int[] hitConeKey=new int[4];
    int nextHitCone;
    float damageFloor,keyFlash;
    int hitColor,leftKey=-1,rightKey=-1;
    boolean fullRoster;
    float x,y,vx,vy,r,left,right,top,bottom,clock,flash,turn=1;
    float routeTime,routeDuration,launchLeft,trailTick,timer;
    float circleSweep,netAge,netReturn,catchX,catchY,windUp,releaseWind,netAimX,netAimY,tapFlash,scared,scaredAge;
    float traceLost,lostStart,lostTravel,traceBeat,traceProgress;
    int traceHaptic,nextMeow;
    float voiceLeft;
    float traceClock,traceX,traceY,shapeBurst,burstX,burstY,burstRadius;
    float[] burstShape,burstWork;
    int burstKind,burstStep;
    float escapeLeft,escapeX,escapeY,escapeAge;
    int flurryCount;
    boolean escapeSequence,flurryFromLeft,doubleFlurry;
    boolean circling;
    boolean placed,kids,escaping;
    void reset() {
        rocket.reset();
        phase=CHASE;pointer=-1;taps=trailCount=captures=cue=windowHits=0;promptKey=leftKey=rightKey=-1;damageFloor=keyFlash=0;hitColor=0;fullRoster=false;placed=escaping=escapeSequence=flurryFromLeft=doubleFlurry=false;flurryCount=0;turn=1;
        java.util.Arrays.fill(hitConeLife,0);nextHitCone=0;
        circleSweep=netAge=netReturn=catchX=catchY=windUp=releaseWind=netAimX=netAimY=tapFlash=scared=scaredAge=0;circling=false;gesture.clear();
        x=y=vx=vy=clock=flash=routeTime=routeDuration=trailTick=timer=0;launchLeft=1.3f;
        traceLost=lostStart=lostTravel=traceBeat=traceProgress=voiceLeft=0;traceHaptic=0;nextMeow=1;
        traceClock=traceX=traceY=shapeBurst=escapeLeft=escapeX=escapeY=escapeAge=0;burstShape=burstWork=null;
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
    void animate(Boss b,float dt,Layout L) {
        voiceLeft=Math.max(0,voiceLeft-dt);keyFlash=Math.max(0,keyFlash-dt);
        for(int i=0;i<hitConeLife.length;i++)hitConeLife[i]=Math.max(0,hitConeLife[i]-dt);
        escapeLeft=Math.max(0,escapeLeft-dt);
        traceLost=Math.max(0,traceLost-dt);traceBeat=Math.max(0,traceBeat-dt);
        shapeBurst=Math.max(0,shapeBurst-dt);
        if(shapeBurst==0)burstShape=burstWork=null;
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
            // The guide stays anchored while the cat demonstrates a miniature copy inside it.
            traceClock+=dt;
            float size=r*.52f*clamp(traceClock/.4f,0,1),along=traceClock/1.6f*gesture.length;
            traceX=gesture.coordinate(along,0)*size;traceY=gesture.coordinate(along,1)*size;
            float follow=1-(float)Math.exp(-dt*18);
            windUp+=(target-windUp)*(1-(float)Math.exp(-dt*12));
            if(circling) {netAimX+=(gesture.lastX-netAimX)*follow;netAimY+=(gesture.lastY-netAimY)*follow;}
        }
        if(phase!=CIRCLE) {
            float settle=(float)Math.exp(-dt*16);
            traceX*=settle;traceY*=settle;
        }
        RainbowCatArrival.pose(this,b,L);
        if(escapeSequence)escapeMotion(b,dt,L);
        else if(escaping && routeTime<routeDuration)advance(dt);
        pose(dt);
    }
    private void pose(float dt) {
        float facing=phase==CIRCLE || phase==CATCH || phase==OPEN || (escapeSequence && escapeAge<ESCAPE_TIME)?-1:Math.abs(vx)<1?turn:vx<0?-1:1;
        turn+=(facing-turn)*(1-(float)Math.exp(-dt*14));
        float struggle=phase==OPEN?1:phase==CATCH?clamp(netAge/NET_TIME,0,1):0;
        float wiggle=struggleX()+shakeOffset();
        pastry.moveTo(x+traceX+wiggle,y+traceY+struggleY());pastry.pull(x+r*(float)Math.sin(clock*23),y+r*.6f*(float)Math.cos(clock*19),.36f*struggle);pastry.update(dt);
        head.moveTo(x+traceX+turn*r*.88f-wiggle+(float)Math.sin(clock*31)*r*.025f*scared,y+traceY+r*.02f+struggleY()+(float)Math.sin(clock*27)*r*.10f*struggle);head.update(dt);
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
        if(escapeSequence)return 0;
        if(phase==CATCH) {
            float before=netAge;netAge+=dt;
            if(before<.34f && netAge>=.34f) {cue=3;flash=1;pastry.squash(.65f);head.squash(.25f);}
            if(netAge>=NET_TIME)open(b);
            return 0;
        }
        if(phase==OPEN) {
            timer-=dt;
            if(timer<=0)escape(b,L,rnd);
            return 0;
        }
        // Let the tap reaction read in place, then finish the escape before accepting another tap.
        if(phase==DASH && taps>0 && !reactionReady())return 0;
        float remaining=phase==CIRCLE?0:dt;
        while(remaining>0) {
            if(routeDuration==0 || routeTime>=routeDuration) {
                if(phase==DASH && taps==2) {if(reactionReady()) {phase=CIRCLE;captures=damagePhase(b);traceClock=0;gesture.setShape(TraceGesture.shape(captures),starShape());}vx=vy=0;break;}
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
    private void flurry(Boss b,java.util.Random rnd) {
        b.clearBolts();
        boolean fromLeft=rnd.nextBoolean();int keys=Roster.count(b.rosterFull);
        // The window floor identifies the phase even after its final hit changes the health band.
        doubleFlurry=damageFloor<=b.hpMax/TraceGesture.SHAPE_COUNT+.0001f;
        int repeats=doubleFlurry?2:1;
        flurryFromLeft=fromLeft;flurryCount=keys*repeats;
        // Keep close pairs, with a little breathing room when switching keys.
        float lead=(ESCAPE_TIME+SWEEP_SETUP+SWEEP_LEAD)/(Boss.BOLT_TIME*.5f);
        for(int i=0;i<flurryCount;i++) {
            int key=i/repeats,slot=b.tailBolt(rnd,x,y+r*.45f);
            if(slot>=0) {b.bglyph[slot]=Roster.at(b.rosterFull,fromLeft?key:keys-1-key);b.bfast[slot]=true;b.bt[slot]=-lead-flurryDelay(i)/(Boss.BOLT_TIME*.5f);}
        }
        cue=2;
    }
    float attackInterval() {return kids?2.3f:1.6f;}
    static float trailFade(float progress) {return clamp((1-progress)*4,0,1);}
    float promptTime() {return (fullRoster?6f:4.8f)*(kids?1.4f:1);}
    private void open(Boss b) {
        phase=OPEN;vx=vy=0;b.clearBolts();timer=promptTime();cue=1;flash=1;
        captures=damagePhase(b);damageFloor=b.hpMax*(TraceGesture.SHAPE_COUNT-captures-1)/TraceGesture.SHAPE_COUNT;
    }
    private void resume(Layout L,java.util.Random rnd) {
        netReturn=NET_RETURN;windUp=tapFlash=scared=scaredAge=0;release();taps=0;promptKey=leftKey=rightKey=-1;phase=DASH;chooseRoute(L,rnd,true);launchLeft=attackInterval();
    }
    static int damagePhase(Boss b) {
        return Math.min(TraceGesture.SHAPE_COUNT,Math.max(0,(int)Math.floor((b.hpMax-b.hp)/b.hpMax*TraceGesture.SHAPE_COUNT+.0001f)));
    }
    private void choosePair(java.util.Random rnd) {
        int n=Roster.count(fullRoster),half=n/2;
        leftKey=Roster.at(fullRoster,rnd.nextInt(half));
        rightKey=Roster.at(fullRoster,half+rnd.nextInt(n-half));
        promptKey=leftKey;
    }
    float promptX(Layout L,int glyph) {return L.w*.5f+(glyph==leftKey?-1:1)*L.enemyR*1.05f*1.65f;}
    float promptY() {return y+r*3.5f;}
    int press(Boss b,int glyph,Layout L,java.util.Random rnd) {
        if(phase!=OPEN || promptKey<0 || timer<=0)return Boss.REBUFF;
        if(promptKey!=glyph)return Boss.REBUFF;
        b.hitX=x;b.hitY=y;
        int cone=nextHitCone;nextHitCone=(cone+1)%hitConeLife.length;
        hitConeLife[cone]=HIT_CONE_TIME;hitConeKey[cone]=glyph;
        hitConeX[cone]=promptX(L,glyph);hitConeY[cone]=promptY();
        keyFlash=.22f;hitColor=Glyph.COLOR[glyph];flash=1;windowHits++;
        float remaining=Math.max(0,b.hp-damageFloor),step=b.hpMax/TraceGesture.SHAPE_COUNT/HITS_PER_BAND;
        float amount=remaining<=step+.0001f?remaining:step;
        int result=b.damage(amount);captures=damagePhase(b);
        if(b.beaten)rocket.begin(this,b);
        else if(b.hp<=damageFloor+.0001f)escape(b,L,rnd);
        else promptKey=promptKey==leftKey?rightKey:leftKey;
        return result;
    }
    private void escape(Boss b,Layout L,java.util.Random rnd) {
        flurry(b,rnd);resume(L,rnd);launchLeft=4.4f;
        escapeLeft=ESCAPE_TIME;escapeAge=0;escapeX=x;escapeY=y;escapeSequence=true;phase=ESCAPE;vx=vy=0;
        pastry.squash(.85f);head.squash(.55f);
        if(b.beaten)escaping=true;
    }
    float struggleX() {return ((float)Math.sin(clock*25)*.17f+(float)Math.sin(clock*39)*.05f)*r*caughtStrength();}
    float struggleY() {return (float)Math.sin(clock*31)*r*.075f*caughtStrength();}
    float flurryDelay(int shot) {return shot*SWEEP_STEP+(doubleFlurry?(shot/2)*DOUBLE_PAIR_GAP:0);}
    float sweepDuration() {return SWEEP_LEAD*2+flurryDelay(Math.max(0,flurryCount-1));}
    float angry() {
        if(!escapeSequence)return 0;
        return Math.min(clamp(escapeAge/.15f,0,1),clamp((ESCAPE_TIME+SWEEP_SETUP+sweepDuration()-escapeAge)/.2f,0,1));
    }
    private void straightRoute(float tx,float ty,float duration) {
        routeTime=0;routeDuration=duration;
        for(int i=0;i<4;i++) {route[i*2]=x+(tx-x)*i/3;route[i*2+1]=y+(ty-y)*i/3;}
    }
    private void escapeMotion(Boss b,float dt,Layout L) {
        float before=escapeAge,start=ESCAPE_TIME+SWEEP_SETUP,end=start+sweepDuration();
        escapeAge=Math.min(end,escapeAge+dt);
        if(escapeAge>ESCAPE_TIME) {
            if(before<=ESCAPE_TIME)straightRoute(flurryFromLeft?left:right,escapeY,SWEEP_SETUP);
            float setup=Math.min(escapeAge,start)-Math.max(before,ESCAPE_TIME);
            if(setup>0)advance(setup);
        }
        if(escapeAge>start) {
            if(before<=start) {
                straightRoute(flurryFromLeft?right:left,escapeY,sweepDuration());
                float span=route[6]-x,bow=Math.min(escapeY-top,L.w*.27f);
                // A broad rising slash: soft ends, fast middle, mirrored for either direction.
                route[2]=x+span*.02f;route[4]=x+span*.98f;
                route[3]=route[5]=escapeY-bow*4/3;
            }
            advance(escapeAge-Math.max(before,start));
        }
        // Each queued star starts at the moving cat when its turn in the sweep arrives.
        for(int i=0;i<b.bt.length;i++)if(b.bt[i]<0) {b.bsx[i]=x;b.bsy[i]=y+r*.45f;}
        if(escapeAge>=end) {
            escapeSequence=false;phase=DASH;routeDuration=routeTime=0;
            if(b.beaten)straightRoute(flurryFromLeft?L.w+r*3:-r*3,y,.38f);
        }
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
    float pouchSway() {return ((float)Math.sin(clock*10)*.66f+(float)Math.sin(clock*17)*.12f)*caughtStrength();}
    float hoopShift() {return r*.25f*(float)Math.sin(clock*10+.6f)*caughtStrength();}
    boolean starShape() {return captures==4;}
    float circleRadius() {return r*(captures==0?2.2f:2.7f);}
    boolean carries(GameCore c) {
        return c.state==GameCore.PLAY && !c.dying() && c.boss.kind==Boss.RAINBOW_CAT
                && (rocket.active || phase==CATCH || phase==OPEN || netReturn>0 || escapeLeft>0 || (phase==CIRCLE && windUp>.002f));
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
            shapeBurst=1.25f;burstKind=captures;burstShape=TraceGesture.shape(captures);
            burstStep=Math.max(1,(burstShape.length/2-1)/32);
            burstWork=new float[((burstShape.length/2-1)/burstStep+1)*2];
            burstX=circleX();burstY=circleY();burstRadius=circleRadius();
            phase=CATCH;catchX=x;catchY=y;releaseWind=windUp;timer=netAge=netReturn=0;circling=false;c.boss.clearBolts();
            fullRoster=c.boss.rosterFull;windowHits=0;choosePair(c.rnd);
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
        if(phase==ESCAPE)return doubleFlurry?"DOUBLE RAINBOW?!":"RAINBOW INCOMING!";
        if(phase==OPEN)return "LEFT, RIGHT!";
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
