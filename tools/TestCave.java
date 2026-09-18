package com.dddumpling.game;

final class TestCave extends Check {
    static GameCore game(Layout L) {
        GameCore c=new GameCore(new Mem(),921L);c.caveChoice=0;c.startGame();c.jumpToStage(21,L);c.stageBanner=0f;
        return c;
    }
    static void all(Layout L) {
        group("cave expedition");
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
        v.z=2;v.phase=Cave.FORK;v.timer=0;v.fork=0;
        v.update(c,Cave.LESSON+.1f,L);check("first demonstration precedes timeout",v.phase==Cave.FORK);
        v.update(c,Cave.FORK_WAIT,L);check("fork eventually commits predictable fallback",v.phase==Cave.WALK && v.routes[0]==1);
        v.phase=Cave.FORK;v.fork=1;v.z=v.cameraZ=5;v.timer=0;
        v.tap(c,L,Cave.branchX(1,-1,5.4f)*L.w,v.screenY(5.4f,L));
        check("lit branch commits instantly",v.routes[1]==-1 && v.phase==Cave.WALK);
        v.z=5.5f;check("walker stays on chosen route",v.playerX()==Cave.branchX(1,-1,v.z));
        v.encounter(c,Cave.SHADOW);v.aim=3f;
        int lives=c.lives;v.update(c,1f,L);c.tapKey(v.response[0],L);
        check("unidentified enemy cannot be attacked",v.phase==Cave.SHADOW && v.responsePos==0);
        v.tap(c,L,v.enemyX*L.w,v.screenY(v.enemyZ,L));v.update(c,Cave.REVEAL+.01f,L);
        check("beam reveals response",v.phase==Cave.FIGHT && v.wanted()==v.response[0]);
        float enemy=v.enemyZ;v.update(c,.2f,L);check("revealed enemy advances",v.enemyZ<enemy);
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
        v.update(c,.5f,L);check("player walks back onto route",v.returnTime==0f && v.playerX()==v.pathX(v.z));
        v.encounter(c,Cave.ROCKS);float before=v.traps.x;v.traps.drag(.82f);v.traps.update(c,.01f,L);
        check("rock steering is bounded",v.traps.x-before<=CaveTraps.MAX_VX*.01f+.0001f);
        c=game(L);v=c.cave;v.openingMet=true;v.routes[0]=-1;v.met[0]=true;v.z=3.34f;c.lives=1;
        v.update(c,.1f,L);check("walking over route heart restores one life",c.lives==2 && v.hearts[0]);
        v.z=3.34f;v.update(c,.1f,L);check("heart cannot pay twice",c.lives==2);
        v.hearts[0]=false;v.z=3.34f;c.lives=GameCore.START_LIVES;v.update(c,.1f,L);
        check("heart respects life cap",c.lives==GameCore.START_LIVES);
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
        for(int frame=0;frame<60;frame++)c.update(DT,L);
        check("walker covers twice the former distance in one second",Math.abs(c.cave.z-.36f)<.001f);
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
    static void bounded(Layout L) {
        for(float pps:new float[]{4f,6f,9f}) {
            GameCore c=game(L);Bot b=new Bot(pps,.25f,.04f,true,977L);
            float t=0f;
            while(t<130f && c.state==GameCore.PLAY && !c.pendingBonus) {c.update(DT,L);b.step(c,L,DT);t+=DT;}
            System.out.printf("    cave %.0f/s: %.1fs, %d lives, exit %s%n",pps,t,c.lives,c.pendingBonus);
            check("bounded hands reach cave exit "+pps,c.pendingBonus && c.lives>0 && t>=30f && t<65f);
        }
    }
}
