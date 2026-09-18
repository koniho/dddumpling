package com.dddumpling.game;

/** Fast travel between immediate encounters; the following camera moves in both world axes. */
final class Cave {
    static final int LAND=4,WALK=0,FORK=1,SHADOW=2,FIGHT=3,ROCKS=4,SAND=5,EXIT=6,CHOOSE=7;
    static final float LENGTH=CaveRoute.LENGTH,WALK_SPEED=1.15f,FORK_WAIT=.55f,
            LESSON=.35f,REVEAL=.16f,APPROACH=1.8f;
    static final float[] FORKS=CaveRoute.FORKS;
    final int[] routes=new int[3],response=new int[5];
    final CaveDumpling walker=new CaveDumpling();
    final CaveSelection selection=new CaveSelection();
    final CaveInput input=new CaveInput();
    final CaveTraps traps=new CaveTraps();
    final CaveEffects effects=new CaveEffects();
    int phase,fork,responsePos,responseSize,seed,nextEvent;
    float z,cameraZ,aim,timer,enemyX,enemyY,enemyStartX,enemyStartY,pulse,exitTime,returnX,returnTime,focus,aimHold;
    boolean running,lessonSeen;
    static boolean stage(int n){return BuildFlags.DEVELOPER && Lands.forStage(n)==LAND;}
    static boolean active(GameCore c){return BuildFlags.DEVELOPER && c.state==GameCore.PLAY && c.cave.running;}
    void begin(GameCore c) {
        running=stage(c.stage);phase=WALK;z=cameraZ=timer=pulse=focus=aimHold=0;
        aim=CaveRoute.heading(0);fork=responsePos=nextEvent=0;lessonSeen=c.stage!=21;
        seed=c.stage;exitTime=returnTime=0;
        for(int i=0;i<routes.length;i++)routes[i]=0;
        input.release();traps.reset();effects.reset();walker.reset();selection.reset();
        if(running && c.caveChoice<0)phase=CHOOSE;
    }
    void leave(){running=false;input.release();traps.reset();effects.reset();}
    static float centre(float at){return CaveRoute.centre(at);}
    static float branchX(int junction,int side,float at){return CaveRoute.x(junction,side,at);}
    int junction(float at){for(int i=0;i<3;i++)if(at>=FORKS[i] && at<=FORKS[i]+1.4f)return i;return -1;}
    float pathX(float at){int i=junction(at);return i<0?centre(at):branchX(i,routes[i],at);}
    float pathY(float at){int i=junction(at);return i<0?CaveRoute.y(at):CaveRoute.branchY(i,routes[i],at);}
    float zoom(){return 1f+.62f*focus;}
    static float scale(Layout L){return L.w*.78f;}
    static float anchor(Layout L){return L.playTop+(L.deckTop-L.playTop)*.57f;}
    float screenX(float world,Layout L){return L.w*.5f+(world-pathX(cameraZ))*scale(L)*zoom();}
    float worldScreenY(float world,Layout L){return anchor(L)+(pathY(cameraZ)-world)*scale(L)*zoom();}
    float screenY(float at,Layout L){return worldScreenY(pathY(at),L);}
    float playerX() {
        if(phase==ROCKS)return traps.x;
        float home=.5f+(pathX(z)-pathX(cameraZ))*.78f*zoom();
        float t=Math.min(1,returnTime/.22f);return home+(returnX-home)*t*t*(3-2*t);
    }
    float playerY(Layout L){return screenY(z,L);}
    float branchScreenX(int side,Layout L){return screenX(branchX(fork,side,z+.48f),L);}
    float branchScreenY(int side,Layout L){return worldScreenY(CaveRoute.branchY(fork,side,z+.48f),L);}
    int nearestBranch(){return Math.sin(aim-CaveRoute.heading(z))<0?-1:1;}
    int event(int junction,int side){return CaveRoute.event(junction,side);}
    boolean tap(GameCore c,Layout L,float x,float y) {
        if(!active(c)||c.paused||c.settingsOpen||y<L.playTop||y>=L.deckTop)return false;
        if(phase==CHOOSE)return selection.tap(c,L,x,y);
        if(phase==ROCKS){traps.drag(x/L.w);return true;}
        aim=(float)Math.atan2(x-playerX()*L.w,playerY(L)-y);aimHold=.85f;
        if(phase==FORK) {
            float left=(float)Math.hypot(x-branchScreenX(-1,L),y-branchScreenY(-1,L));
            float right=(float)Math.hypot(x-branchScreenX(1,L),y-branchScreenY(1,L));
            choose(c,left<right?-1:1);
        }
        return true;
    }
    void choose(GameCore c,int side){routes[fork]=side<0?-1:1;lessonSeen=true;phase=WALK;if(c.sound!=null)c.sound.landShuffle();}
    float light(float x,float y) {
        float dx=x-pathX(z),dy=y-pathY(z),distance=(float)Math.hypot(dx,dy);
        float near=Math.max(0,1-distance/.65f)*.75f;
        float angle=(float)Math.atan2(dx,dy),delta=(float)Math.atan2(Math.sin(angle-aim),Math.cos(angle-aim));
        float beam=Math.max(0,1-Math.abs(delta)/.65f)*Math.max(0,1-distance/1.8f);
        return Math.min(1,.045f+near+beam);
    }
    int wanted() {
        if((phase==FIGHT||phase==SHADOW)&&responsePos<responseSize)return response[responsePos];
        if(phase==SAND)return traps.wanted();return -1;
    }
    boolean press(GameCore c,int g,Layout L) {
        if(!active(c))return false;
        if(phase==SAND)return traps.press(c,g);
        if(phase!=FIGHT&&phase!=SHADOW)return false;
        if(g!=wanted()){c.keyBad[g]=1;c.misses++;c.missesThisStage++;c.combo=0;if(c.sound!=null)c.sound.wrong();return false;}
        c.hits++;c.combo++;c.maxCombo=Math.max(c.maxCombo,c.combo);c.score+=10;pulse=1;responsePos++;
        if(c.sound!=null)c.sound.squish(g,1);
        if(responsePos==responseSize){c.squishes++;c.score+=40;Fx.explode(c,c.rnd,screenX(enemyX,L),worldScreenY(enemyY,L),L.enemyR,12,Glyph.COLOR[g]);phase=WALK;}
        return true;
    }
    void encounter(GameCore c,int kind) {
        phase=kind;timer=0;cameraZ=z;aimHold=0;c.stageBanner=0;
        if(kind==SHADOW) {
            int side=(nextEvent+seed)%2==0?-1:1;
            enemyX=enemyStartX=pathX(z)+side*.48f;enemyY=enemyStartY=pathY(z)+.30f;
            responsePos=0;responseSize=2+(seed-21)%2;
            for(int i=0;i<responseSize;i++)response[i]=Roster.at(c.playRosterFull(),(seed+i*2+nextEvent)%Roster.count(c.playRosterFull()));
            aim=(float)Math.atan2(enemyX-pathX(z),enemyY-pathY(z));
            effects.cue(c,Sfx.CAVE_AMBUSH,.65f);
        } else {
            traps.begin(c,kind,.5f);
            effects.cue(c,kind==ROCKS?Sfx.CAVE_RUMBLE:Sfx.CAVE_SINK,.6f);
        }
    }
    void update(GameCore c,float dt,Layout L) {
        effects.update(dt);
        float distance=phase==WALK&&returnTime<=0&&!c.pendingBonus?dt*WALK_SPEED*c.speed:0;
        walker.update(dt,distance);pulse=Math.max(0,pulse-dt*3);returnTime=Math.max(0,returnTime-dt);
        cameraZ+=(z-cameraZ)*Math.min(.9f,dt*12);
        float goal=phase==ROCKS||phase==SAND?1:0;
        focus+=Math.max(-dt*5,Math.min(dt*7,goal-focus));
        aimHold=Math.max(0,aimHold-dt);
        if(aimHold==0 && (phase==WALK||phase==FORK)) {
            float heading=(float)Math.atan2(pathX(z+.1f)-pathX(z),pathY(z+.1f)-pathY(z));
            float delta=(float)Math.atan2(Math.sin(heading-aim),Math.cos(heading-aim));aim+=delta*Math.min(1,dt*10);
        }
        if(c.pendingBonus)return;
        switch(phase) {
            case CHOOSE:selection.update(c,dt);break;
            case FORK:timer+=dt;if(timer>=FORK_WAIT)choose(c,nearestBranch());break;
            case SHADOW:case FIGHT:
                timer+=dt;
                if(timer>=REVEAL)phase=FIGHT;
                float rush=Math.min(1,timer/APPROACH);rush=rush*rush;
                enemyX=enemyStartX+(pathX(z)-enemyStartX)*rush;enemyY=enemyStartY+(pathY(z)-enemyStartY)*rush;
                if(timer>=APPROACH){phase=WALK;c.takeHit(playerX()*L.w,L);}break;
            case ROCKS:case SAND:traps.update(c,dt,L);break;
            case EXIT:exitTime+=dt;if(exitTime>=.6f){c.score+=100;Interlude.beginStageEnd(c);}break;
            default:
                if(returnTime>0)break;
                float next=Math.min(LENGTH,z+distance);
                // Stop at the nearest boundary even when a frame crosses several of them.
                float stop=next;int junction=-1;
                for(int i=0;i<3;i++)if(routes[i]==0&&FORKS[i]>=z&&FORKS[i]<=stop){stop=FORKS[i];junction=i;}
                if(nextEvent<CaveRoute.EVENTS_AT.length && CaveRoute.EVENTS_AT[nextEvent]<=stop) {
                    z=CaveRoute.EVENTS_AT[nextEvent];int kind=CaveRoute.encounter(nextEvent,seed,routes);nextEvent++;encounter(c,kind);break;
                }
                z=stop;
                if(junction>=0){fork=junction;phase=FORK;timer=0;break;}
                if(z>=LENGTH){phase=EXIT;if(c.sound!=null)c.sound.stageClear();}break;
        }
    }
}
