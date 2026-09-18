package com.dddumpling.game;

final class TestCave extends Check {
    static GameCore game(Layout L) {
        GameCore c=new GameCore(new Mem(),921L);c.caveChoice=0;c.startGame();c.jumpToStage(21,L);c.stageBanner=0f;
        return c;
    }
    static void feedback(Layout L) {
        GameCore c=game(L);Cave v=c.cave;Ear ear=new Ear();c.sound=ear;
        v.encounter(c,Cave.ROCKS);
        check("rock event cues once",ear.caveSounds==1 && v.effects.takeFeedback()==1 && v.effects.takeFeedback()==0);
        for(int i=0;i<CaveTraps.ROCK_COUNT;i++)check("angled rock lands on its marked lane "+i,
                v.traps.rockX(i,1)==v.traps.lanes[i]);
        check("rocks approach from both angles",v.traps.rockX(0,0)<v.traps.lanes[0] && v.traps.rockX(2,0)>v.traps.lanes[2]);
        v.traps.x=v.traps.targetX=.2f;v.update(c,CaveTraps.FALL+.01f,L);
        check("landing starts debris and strong feedback",v.effects.cursor==1 && v.effects.age[0]==0 && v.effects.takeFeedback()==2);
        int sounds=ear.caveSounds;v.update(c,.01f,L);
        check("landing never repeats per frame",ear.caveSounds==sounds && v.effects.takeFeedback()==0);
        v.phase=Cave.WALK;float age=v.effects.age[0];v.update(c,.05f,L);
        check("debris survives encounter",v.effects.age[0]>age && v.effects.age[0]<.65f);
        v.effects.cue(c,Sfx.CAVE_CRASH,1);Pause.release(c);
        check("pause clears pending cave haptic",v.effects.takeFeedback()==0);
        v.encounter(c,Cave.SAND);v.effects.takeFeedback();v.update(c,.66f,L);
        check("sinking pulses on gameplay clock",v.effects.takeFeedback()==1 && ear.lastCaveSound==Sfx.CAVE_SINK);
        v.leave();check("leaving clears rumble and debris",v.effects.rumble==0 && v.effects.age[0]>=.65f && v.effects.takeFeedback()==0);
    }
    static void all(Layout L) {
        group("cave expedition");
        feedback(L);
        selection(L);
        GameCore c=game(L);Cave v=c.cave;
        check("cave follows mushroom land",Lands.forStage(20)==3 && Lands.forStage(21)==Cave.LAND);
        check("mushroom reward unlocks cave",!LandPicker.unlocked(c,Cave.LAND));
        c.collected=Collect.add(c.collected,Collect.BOSS_FIRST+3);
        check("mushroom friend is cave entry",LandPicker.unlocked(c,Cave.LAND));
        check("stage jump enters expedition",Cave.active(c) && c.enemies.isEmpty());
        c.landChoice=Cave.LAND;c.startGame();
        check("picker and jump enter same cave",c.stage==21 && Cave.active(c) && c.cave.z==0f);
        c.stageBanner=0f;c.update(.2f,L);check("camera follows without jumping",v.z>v.cameraZ);
        c.update(.2f,L);check("camera advances toward walker",v.cameraZ>0f && v.cameraZ<v.z);
        float at=v.z;c.paused=true;c.update(1f,L);check("pause freezes expedition",v.z==at);c.paused=false;
        v.z=2;v.phase=Cave.FORK;v.timer=0;v.fork=0;v.aim=CaveRoute.heading(v.z)+.4f;v.aimHold=2;
        v.update(c,Cave.LESSON+.1f,L);check("first demonstration precedes timeout",v.phase==Cave.FORK);
        v.update(c,Cave.FORK_WAIT,L);check("fork eventually commits predictable fallback",v.phase==Cave.WALK && v.routes[0]==1);
        v.phase=Cave.FORK;v.fork=1;v.z=v.cameraZ=5;v.timer=0;
        v.tap(c,L,v.branchScreenX(-1,L),v.branchScreenY(-1,L));
        check("lit branch commits instantly",v.routes[1]==-1 && v.phase==Cave.WALK);
        v.z=5.5f;check("walker stays on chosen route",Math.abs(v.playerX()*L.w-v.screenX(Cave.branchX(1,-1,v.z),L))<.001f);
        v.encounter(c,Cave.SHADOW);v.aim=3f;
        int lives=c.lives;
        check("ambush shows playable response immediately",v.wanted()==v.response[0] && v.timer==0 && c.stageBanner==0);
        float enemyDistance=(float)Math.hypot(v.enemyX-v.pathX(v.z),v.enemyY-v.pathY(v.z));
        v.update(c,.3f,L);
        check("enemy rushes from side cover without lantern gate",v.phase==Cave.FIGHT
                && Math.hypot(v.enemyX-v.pathX(v.z),v.enemyY-v.pathY(v.z))<enemyDistance);
        c.tapKey(v.wanted(),L);int progress=v.responsePos;
        int wrong=(v.wanted()+1)%Glyph.COUNT;c.tapKey(wrong,L);
        check("wrong key keeps landed response progress",v.responsePos==progress);
        while(v.phase==Cave.FIGHT)c.tapKey(v.wanted(),L);
        check("completed response defeats enemy without damage",v.phase==Cave.WALK && c.lives==lives && c.squishes==1);
        v.encounter(c,Cave.SHADOW);v.phase=Cave.FIGHT;v.update(c,Cave.APPROACH+.01f,L);
        check("arrival costs one life",c.lives==lives-1 && v.phase==Cave.WALK);
        v.update(c,.1f,L);check("arrival cannot damage twice",c.lives==lives-1);
        v.encounter(c,Cave.SAND);v.traps.update(c,CaveTraps.WARNING,L);
        int wanted=v.wanted();c.tapKey(wanted,L);c.tapKey(wanted,L);
        check("quicksand needs alternating keys",v.traps.hits==1);
        for(int i=1;i<CaveTraps.ESCAPE_PRESSES;i++)c.tapKey(v.wanted(),L);
        check("alternating presses escape",v.phase==Cave.WALK);
        v.encounter(c,Cave.SAND);v.traps.update(c,CaveTraps.WARNING+CaveTraps.DURATION,L);
        check("quicksand timeout costs one life",c.lives==lives-2 && v.phase==Cave.WALK);
        c=game(L);v=c.cave;v.encounter(c,Cave.ROCKS);
        v.traps.x=v.traps.targetX=v.traps.lanes[0];v.traps.update(c,CaveTraps.WARNING+CaveTraps.FALL+.01f,L);
        check("rock collision resolves the trap with one hit",c.lives==2 && v.phase==Cave.WALK);
        check("trap exit keeps the player at its last position",Math.abs(v.playerX()-v.traps.x)<.0001f);
        v.update(c,.5f,L);check("player walks back onto route",v.returnTime==0f && Math.abs(v.playerX()*L.w-v.screenX(v.pathX(v.z),L))<.001f);
        v.encounter(c,Cave.ROCKS);float before=v.traps.x;v.traps.drag(.82f);v.traps.update(c,.01f,L);
        check("rock steering is bounded",v.traps.x-before<=CaveTraps.MAX_VX*.01f+.0001f);
        c=game(L);v=c.cave;v.nextEvent=CaveRoute.EVENTS_AT.length;
        for(int i=0;i<3;i++)v.routes[i]=-1;
        c.lives=1;
        for(int i=0;i<650 && v.phase==Cave.WALK;i++)v.update(c,DT,L);
        check("route has no healing pickups",c.lives==1 && v.phase==Cave.EXIT);
        c=game(L);v=c.cave;c.lives=1;v.encounter(c,Cave.SAND);v.traps.update(c,10f,L);
        check("fatal trap clears cave and touch state",c.state==GameCore.OVER && !v.running && v.traps.kind==-1);
        c.startGame();c.jumpToStage(21,L);c.jumpToStage(16,L);
        check("jumping away restores ordinary game",!Cave.active(c) && !c.cave.running);
        c=game(L);v=c.cave;v.phase=Cave.EXIT;v.z=Cave.LENGTH;v.update(c,1.5f,L);
        check("exit completes the stage",c.pendingBonus);
        int score=c.score;v.update(c,1f,L);check("exit reward pays once",c.score==score);
        for(int i=0;i<400 && c.state==GameCore.PLAY;i++)c.update(DT,L);
        check("exit reaches existing interlude",c.state==GameCore.BONUS);
        c=game(L);v=c.cave;v.encounter(c,Cave.ROCKS);
        check("key touch cannot own cave drag",!v.input.down(c,L,3,c.keyX(L,0),c.keyY(L,0)) && v.input.pointer<0);
        v.input.down(c,L,4,L.w*.5f,L.playTop+L.w*.5f);
        check("playfield owns one cave drag",v.input.pointer==4);
        v.input.up(3);check("second finger lift preserves drag",v.input.pointer==4);
        float target=v.traps.targetX;v.input.move(c,L,3,L.w*.8f);
        check("other finger cannot steer",v.traps.targetX==target);
        Pause.open(c);check("pause releases steering",v.input.pointer<0);Pause.resume(c);
        v.input.down(c,L,7,L.w*.5f,L.playTop+L.w*.5f);c.jumpToStage(16,L);
        check("stage exit releases steering",v.input.pointer<0);
        Mem legacy=new Mem();legacy.landState=6|(10<<4);
        GameCore migrated=new GameCore(legacy,922L);
        check("legacy land visits and suppression survive upgrade",migrated.landSeen==6 && migrated.landSuppressed==10);
        LandPicker.save(migrated);migrated=new GameCore(legacy,923L);
        check("rewritten land flags survive restart",migrated.landSeen==6 && migrated.landSuppressed==10);
        migrated.collected=legacy.collected=Collect.MASK;LandPicker.reset(migrated);
        migrated=new GameCore(legacy,924L);
        check("reset suppresses new cave across restart",!LandPicker.unlocked(migrated,Cave.LAND));
        LandPicker.reward(migrated,Collect.BOSS_FIRST+3);
        check("mushroom reward restores reset cave",LandPicker.unlocked(migrated,Cave.LAND));
        gauntlet(L);
        bounded(L);
    }
    static void selection(Layout L) {
        for(int finish=0;finish<CaveDumpling.COUNT;finish++) {
            Mem store=new Mem();GameCore c=new GameCore(store,950L+finish);
            c.startGame();c.jumpToStage(20,L);c.jumpToStage(21,L);
            check("first cave entry opens choice "+finish,c.cave.phase==Cave.CHOOSE && c.caveChoice==-1);
            c.update(.7f,L);
            check("selection scene keeps expedition still "+finish,c.cave.z==0f && c.lives==GameCore.START_LIVES);
            c.cave.input.down(c,L,1,CaveSelection.x(L,finish),CaveSelection.y(L,finish));
            check("each finish is selectable and saved "+finish,c.caveChoice==finish && store.caveChoice==finish);
            c.cave.selection.pick(c,(finish+1)%CaveDumpling.COUNT);
            check("departure cannot change selected finish "+finish,c.caveChoice==finish);
            c.update(.9f,L);
            check("selected explorer enters walking "+finish,c.cave.phase==Cave.WALK && c.cave.z==0f);
            c.jumpToStage(22,L);
            check("next cave keeps finish without asking again "+finish,c.cave.phase==Cave.WALK && c.caveChoice==finish);
            GameCore reload=new GameCore(store,960L);reload.startGame();reload.jumpToStage(21,L);
            check("finish survives restart "+finish,reload.caveChoice==finish && reload.cave.phase==Cave.WALK);
        }
        Mem invalid=new Mem();invalid.caveChoice=999;
        GameCore c=new GameCore(invalid,971L);c.startGame();c.jumpToStage(21,L);
        check("invalid saved finish asks for a valid choice",c.cave.phase==Cave.CHOOSE && c.caveChoice==-1);
        c.paused=true;c.update(2f,L);
        check("pause freezes entrance scene",c.cave.selection.age==0f);c.paused=false;
        c.update(.7f,L);c.cave.selection.pick(c,0);c.update(.9f,L);
        c.stageBanner=0f;
        for(int frame=0;frame<12;frame++)c.update(DT,L);
        check("walker moves over three times faster",Cave.WALK_SPEED>=1.08f && Math.abs(c.cave.z-Cave.WALK_SPEED*.2f)<.001f);
        CaveDumpling walking=new CaveDumpling(),idle=new CaveDumpling();float peakLift=0f,peakShape=0f;
        for(int i=0;i<180;i++) {
            walking.update(DT,Cave.WALK_SPEED*DT);idle.update(DT,0f);
            peakLift=Math.max(peakLift,Math.abs(walking.lift()-idle.lift()));
            peakShape=Math.max(peakShape,Math.abs(walking.squash()-idle.squash()));
        }
        System.out.printf("    walker spring lift %.3f radii, squash %.3f%n",peakLift,peakShape);
        check("walking excites vertical spring bounce",peakLift>.02f && peakLift<.25f);
        check("walking excites restrained physical squash",peakShape>.015f && peakShape<.16f);
        float prior=c.cave.walker.lift();c.paused=true;c.update(1f,L);
        check("pause freezes walker physics",c.cave.walker.lift()==prior);c.paused=false;
        c.jumpToStage(22,L);
        check("new expedition resets walker physics",Math.abs(c.cave.walker.lift())<.0001f);
    }
    static void gauntlet(Layout L) {
        float sideways=0,vertical=0,down=0,length=0;
        for(float at=.02f;at<Cave.LENGTH;at+=.02f) {
            float dx=Cave.centre(at)-Cave.centre(at-.02f),dy=CaveRoute.y(at)-CaveRoute.y(at-.02f);
            sideways+=Math.abs(dx);vertical+=Math.abs(dy);if(dy<0)down-=dy;
            length+=(float)Math.hypot(dx,dy);
        }
        check("passage snakes laterally and doubles back",sideways>vertical && down>.5f);
        check("travel is measured along the winding route",Math.abs(length-Cave.LENGTH)<.08f);
        boolean spacing=true;
        for(int i=1;i<CaveRoute.EVENTS_AT.length;i++) {
            float gap=(CaveRoute.EVENTS_AT[i]-CaveRoute.EVENTS_AT[i-1])/Cave.WALK_SPEED;
            spacing&=gap>=1f && gap<=2f;
        }
        check("seven gauntlet events with one to two seconds travel",CaveRoute.EVENTS_AT.length==7 && spacing);
        GameCore c=game(L);Cave v=c.cave;v.encounter(c,Cave.ROCKS);
        check("first rock falls immediately with a readable landing window",v.traps.rockProgress(0)==0
                && CaveTraps.FALL>=.65f && CaveTraps.FALL<.85f);
        v.update(c,.16f,L);
        check("trap closeup centers explorer and rapidly zooms",v.focus==1f && Math.abs(v.playerX()-.5f)<.001f);
        float focus=v.focus;Pause.open(c);c.update(1,L);check("pause freezes closeup",v.focus==focus);Pause.resume(c);
        v.encounter(c,Cave.SAND);check("quicksand accepts escape keys on arrival",v.wanted()>=0);
        v.traps.age=1.2f;check("quicksand visibly sinks fast",CaveScreen.sink(v)>.5f);
        v.traps.hits=4;check("escape progress visibly lifts dumpling",CaveScreen.sink(v)<.5f);
        v.encounter(c,Cave.SHADOW);c.speed=1.5f;v.update(c,.5f,L);
        check("developer travel speed does not multiply ambush deadline",Math.abs(v.timer-.5f)<.001f);
        float near=v.light(v.pathX(v.z),v.pathY(v.z));
        check("light follows the player on both axes",near>.7f && v.light(v.pathX(v.z)+3,v.pathY(v.z))<.06f);
    }
    static void bounded(Layout L) {
        for(float pps:new float[]{4f,6f,9f}) {
            int survived=0;float total=0,maxGap=0;int totalEvents=0;
            for(int stage=21;stage<=25;stage++)for(int side:new int[]{-1,1}) {
                GameCore c=game(L);c.jumpToStage(stage,L);c.stageBanner=0;
                Bot b=new Bot(pps,.25f,.04f,true,977L+stage);b.caveSide=side;
                float t=0,gap=0;int events=0;
                while(t<60 && c.state==GameCore.PLAY && !c.pendingBonus) {
                    int before=c.cave.nextEvent;
                    if(c.cave.phase==Cave.WALK||c.cave.phase==Cave.FORK)gap+=DT;
                    c.update(DT,L);b.step(c,L,DT);t+=DT;
                    if(c.cave.nextEvent>before){maxGap=Math.max(maxGap,gap);gap=0;events++;}
                }
                if(c.pendingBonus&&c.lives>0)survived++;
                total+=t;totalEvents+=events;
            }
            System.out.printf("    cave %.0f/s .25s reaction 4%% misses: %.1fs avg, %d/10 survive, longest gap %.2fs%n",pps,total/10,survived,maxGap);
            check("bounded hands survive every stage and branch "+pps,survived==10 && total/10>=12 && total/10<32);
            check("bounded players encounter all seven threats "+pps,totalEvents==70);
            check("gauntlet has no long empty walk "+pps,maxGap<2.2f);
        }
    }
}
