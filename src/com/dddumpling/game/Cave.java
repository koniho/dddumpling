package com.dddumpling.game;

/** Fast travel between immediate encounters; the following camera moves in both world axes. */
final class Cave {
    static final int LAND=4,WALK=0,FORK=1,SHADOW=2,FIGHT=3,ROCKS=4,SAND=5,EXIT=6,CHOOSE=7;
    static final float PACE=.70f,LENGTH=CaveRoute.LENGTH,WALK_SPEED=1.15f*PACE,FORK_WAIT=.55f/PACE,
            LESSON=.35f,REVEAL=.16f,APPROACH=1.8f/PACE,HAZARD_ZOOM=.30f;
    static final float[] FORKS=CaveRoute.FORKS;
    final int[] routes=new int[3],response=new int[5];
    final CaveRoute route=new CaveRoute();
    final java.util.Random random=new java.util.Random(0);
    long runSeed;
    final CaveDumpling walker=new CaveDumpling();
    final CaveSelection selection=new CaveSelection();
    final CaveInput input=new CaveInput();
    final CaveTraps traps=new CaveTraps();
    final CaveEnemy enemy=new CaveEnemy();
    final CaveEffects effects=new CaveEffects();
    int phase,fork,responsePos,responseSize,seed,nextEvent;
    float z,cameraZ,aim,timer,enemyX,enemyY,enemyStartX,enemyStartY,pulse,exitTime,returnX,returnTime,focus,aimHold;
    float zoomAge,zoomFromCamera,zoomFromFocus;
    boolean running,lessonSeen;
    static boolean stage(int n){return BuildFlags.DEVELOPER && Lands.forStage(n)==LAND;}
    static boolean active(GameCore c){return BuildFlags.DEVELOPER && c.state==GameCore.PLAY && c.cave.running;}
    void begin(GameCore c) {
        running=stage(c.stage);phase=WALK;z=cameraZ=timer=pulse=focus=aimHold=0;
        if(running){runSeed=c.rnd.nextLong();random.setSeed(runSeed);route.make(random);}
        aim=route.heading(0);fork=responsePos=nextEvent=0;lessonSeen=c.stage!=21;
        seed=c.stage;exitTime=returnTime=zoomAge=zoomFromCamera=zoomFromFocus=0;
        for(int i=0;i<routes.length;i++)routes[i]=0;
        input.release();traps.reset();effects.reset();enemy.reset();walker.reset();selection.reset();
        if(running && c.caveChoice<0)phase=CHOOSE;
    }
    void leave(){running=false;input.release();traps.reset();effects.reset();enemy.reset();}
    float centre(float at){return route.centre(at);}
    float branchX(int junction,int side,float at){return route.x(junction,side,at);}
    int junction(float at){for(int i=0;i<3;i++)if(at>=FORKS[i] && at<=FORKS[i]+1.4f)return i;return -1;}
    float pathX(float at){int i=junction(at);return i<0?centre(at):branchX(i,routes[i],at);}
    float pathY(float at){int i=junction(at);return i<0?route.y(at):route.branchY(i,routes[i],at);}
    float zoom(){return 1f+.62f*focus;}
    static float scale(Layout L){return L.w*.78f;}
    static float anchor(Layout L){return L.playTop+(L.deckTop-L.playTop)*.57f;}
    float screenX(float world,Layout L){return L.w*.5f+(world-pathX(cameraZ))*scale(L)*zoom();}
    float worldScreenY(float world,Layout L){return anchor(L)-(L.deckTop-L.playTop)*.07f*focus+(pathY(cameraZ)-world)*scale(L)*zoom();}
    float screenY(float at,Layout L){return worldScreenY(pathY(at),L);}
    float playerX() {
        if(phase==ROCKS)return traps.x+hazardOffset();
        float home=.5f+(pathX(z)-pathX(cameraZ))*.78f*zoom();
        float t=Math.min(1,returnTime/.22f);return home+(returnX-home)*t*t*(3-2*t);
    }
    float hazardOffset(){return (pathX(z)-pathX(cameraZ))*.78f*zoom();}
    float playerY(Layout L){return screenY(z,L);}
    float branchScreenX(int side,Layout L){return screenX(branchX(fork,side,z+.48f),L);}
    float branchScreenY(int side,Layout L){return worldScreenY(route.branchY(fork,side,z+.48f),L);}
    int nearestBranch(){return Math.sin(aim-route.heading(z))<0?-1:1;}
    int event(int junction,int side){return route.event(junction,side);}
    boolean tap(GameCore c,Layout L,float x,float y) {
        if(!active(c)||c.paused||c.settingsOpen||y<L.playTop||y>=L.deckTop)return false;
        if(phase==CHOOSE)return selection.tap(c,L,x,y);
        if(phase==ROCKS){traps.drag(x/L.w-hazardOffset());return true;}
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
        enemy.fire(this,L,Glyph.COLOR[g]);
        if(c.sound!=null)c.sound.squish(g,1);
        if(responsePos==responseSize){c.squishes++;c.score+=40;enemy.defeated(this);phase=WALK;}
        return true;
    }
    void encounter(GameCore c,int kind) {
        phase=kind;timer=0;aimHold=0;c.stageBanner=0;
        zoomAge=0;zoomFromCamera=cameraZ;zoomFromFocus=focus;
        if(kind==SHADOW) {
            int side=random.nextBoolean()?-1:1;
            enemy.reset();float ahead=Math.min(LENGTH,z+.55f);
            float dx=pathX(ahead+.02f)-pathX(ahead-.02f),dy=pathY(ahead+.02f)-pathY(ahead-.02f);
            float length=Math.max(.001f,(float)Math.hypot(dx,dy));
            enemyX=enemyStartX=pathX(ahead)+side*dy/length*.19f;
            enemyY=enemyStartY=pathY(ahead)-side*dx/length*.19f;
            responsePos=0;responseSize=2+(seed-21)%2;
            int count=Roster.count(c.playRosterFull()),previous=-1;
            for(int i=0;i<responseSize;i++) {
                int pick=random.nextInt(count-(i>0?1:0));
                if(i>0 && pick>=previous)pick++;
                response[i]=Roster.at(c.playRosterFull(),pick);previous=pick;
            }
            aim=(float)Math.atan2(enemyX-pathX(z),enemyY-pathY(z));
            effects.cue(c,Sfx.CAVE_AMBUSH,.65f);
        } else {
            traps.begin(c,kind,.5f);
            effects.cue(c,kind==ROCKS?Sfx.CAVE_RUMBLE:Sfx.CAVE_SINK,.6f);
        }
    }
    void update(GameCore c,float dt,Layout L) {
        effects.update(dt);enemy.update(c,dt);
        float distance=phase==WALK&&returnTime<=0&&!c.pendingBonus?dt*WALK_SPEED:0;
        walker.update(dt,distance);pulse=Math.max(0,pulse-dt*3);returnTime=Math.max(0,returnTime-dt);
        if(phase==ROCKS||phase==SAND) {
            zoomAge=Math.min(HAZARD_ZOOM,zoomAge+dt);
            float t=zoomAge/HAZARD_ZOOM,ease=t*t*(3-2*t);
            cameraZ=zoomFromCamera+(z-zoomFromCamera)*ease;
            focus=zoomFromFocus+(1-zoomFromFocus)*ease;
        } else {
            cameraZ+=(z-cameraZ)*Math.min(.9f,dt*12);
            focus=Math.max(0,focus-dt*5);
        }
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
                // Reveal is a sequence; only the approach toward the player slows.
                float revealDt=Math.min(dt,Math.max(0,REVEAL-timer));
                timer+=revealDt+(dt-revealDt)*c.traversalRate();
                if(timer>=REVEAL)phase=FIGHT;
                float rush=Math.min(1,timer/APPROACH);rush=rush*rush;
                enemyX=enemyStartX+(pathX(z)-enemyStartX)*rush;enemyY=enemyStartY+(pathY(z)-enemyStartY)*rush;
                enemy.stomp(c);
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
                    z=CaveRoute.EVENTS_AT[nextEvent];int kind=route.encounter(nextEvent,routes);nextEvent++;encounter(c,kind);break;
                }
                z=stop;
                if(junction>=0){fork=junction;phase=FORK;timer=0;break;}
                if(z>=LENGTH){phase=EXIT;if(c.sound!=null)c.sound.stageClear();}break;
        }
    }
}
