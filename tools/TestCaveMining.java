package com.dddumpling.game;

final class TestCaveMining extends Check {
    static GameCore game(Layout L,Mem store){
        GameCore c=new GameCore(store,957L);c.caveChoice=0;c.startGame();c.jumpToStage(22,L);
        c.stageBanner=0;Interlude.enterBonus(c,L);return c;
    }
    static void all(Layout L){
        group("cave minecart");
        Mem store=new Mem();GameCore c=game(L,store);CaveMining m=c.mining;
        check("second cave slot is minecart",m.active && !c.band.active && !c.starBonus);
        m.update(c,1);check("ready holds progress and balance",m.progress==0 && m.segment==0 && m.balance==0);
        float y=(L.playTop+L.deckTop)*.5f;
        check("keyboard cannot claim field drag",!m.input.down(c,L,1,c.keyX(L,0),c.keyY(L,0)));
        check("field starts left lean",m.input.down(c,L,2,L.w*.2f,y) && m.intent<0);
        m.input.down(c,L,3,L.w*.8f,y);m.input.move(c,L,3,L.w*.8f,y);m.input.up(3);
        check("second finger cannot steal steering",m.input.pointer==2 && m.intent<0);
        m.input.move(c,L,2,L.w*.8f,y);check("drag crosses to right lean",m.intent>0);
        m.input.up(2);check("release centres intent",m.intent==0 && m.input.pointer<0);
        m.press(c,Roster.at(c.playRosterFull(),0));check("left keys lean left",m.intent<0);
        m.press(c,Roster.at(c.playRosterFull(),Roster.count(c.playRosterFull())-1));check("right keys lean right",m.intent>0);
        Pause.open(c);float before=m.ready;c.update(2,L);check("pause freezes ride",m.ready==before && m.input.pointer<0 && m.intent==0);Pause.resume(c);
        c.openSettings();c.update(2,L);check("settings freezes ride",m.ready==before);c.closeSettings();
        for(int visit=0;visit<3;visit++){
            c=game(L,store);m=c.mining;m.ready=0;int lives=c.lives;
            m.update(c,2);
            check("idle spills without farming progress "+visit,m.spilled && m.progress==0 && c.lives==lives);
            check("spill rumble consumed once "+visit,m.scene.takeFeedback()==2 && m.scene.takeFeedback()==0);
        }
        store.mineCarts=2;c=game(L,store);check("legacy carts migrate to track",c.mining.progress==8);
        store.mineTrack=-9;c=game(L,store);check("negative track clamps",c.mining.progress==0);
        store.mineTrack=999;c=game(L,store);check("track overflow clamps",c.mining.progress==20);
        int total=c.collectTotal;c.update(.01f,L);
        check("saved completion pays and resets",c.collectTotal==total+1 && store.mineTrack==0 && c.mining.progress==0);
        check("ride retains mole prize",Collect.FAMILY[c.prize]==Collect.MOLES);
        c.update(.1f,L);check("reward pays once",c.collectTotal==total+1);
        store=new Mem();c=game(L,store);m=c.mining;m.ready=0;
        for(int f=0;f<120;f++){m.steer(CaveMining.curve(m.progress+m.segment/CaveMining.SEGMENT));m.update(c,1f/120);}
        check("successful bend saves immediately",m.progress==1 && store.mineTrack==1);
        c=game(L,store);check("restart resumes completed track only",c.mining.progress==1 && c.mining.segment==0);
        c.toTitle();c.startGame();check("new run keeps ride progress",c.mining.progress==1);
        for(int i=0;i<20;i++)check("track preview follows shared bend "+i,Math.signum(CaveMining.curve(i+.5f))==Math.signum(CaveMining.bend(i)));
        c=game(L,new Mem());m=c.mining;m.ready=0;Ear ear=new Ear();c.sound=ear;
        m.steer(-.8f);m.update(c,.05f);check("moving cart plays new rail clatter",ear.lastCaveSound==Sfx.CART_ROLL);
        int sounds=ear.caveSounds;m.update(c,.1f);check("clatter does not repeat per frame",ear.caveSounds==sounds);
        c=game(L,new Mem());m=c.mining;m.ready=0;
        while(m.phase==CaveMining.RIDE){m.steer(CaveMining.curve(m.progress+m.segment/CaveMining.SEGMENT));m.update(c,1f/120);}
        check("perfect rider ends visit safely with partial progress",!m.spilled && !m.won && m.progress==10);
        GameCore a=game(L,new Mem()),b=game(L,new Mem());a.mining.ready=b.mining.ready=0;
        a.mining.steer(-.8f);b.mining.steer(-.8f);a.mining.update(a,.5f);
        for(int f=0;f<30;f++)b.mining.update(b,1f/60);
        check("frame partition preserves balance",Math.abs(a.mining.balance-b.mining.balance)<.001f && a.mining.progress==b.mining.progress);
        for(int point=0;point<CaveMining.TRACK;point++){
            c=game(L,new Mem());m=c.mining;m.progress=point;m.begin(c);m.ready=0;m.update(c,2);
            check("idle cannot farm saved section "+point,m.progress==point && m.spilled);
        }
        bounded(L);
    }
    private static void bounded(Layout L){
        float[] rates={3,5,8},reactions={.30f,.20f,.13f},errors={.08f,.04f,.02f};
        for(int tier=0;tier<3;tier++){
            Mem store=new Mem();java.util.Random rng=new java.util.Random(704+tier);int visits=0;boolean won=false;
            while(visits++<12){
                GameCore c=game(L,store);CaveMining m=c.mining;m.ready=0;float wait=0;
                float[] seen=new float[120];int frame=0;
                while(m.phase==CaveMining.RIDE && frame<1400){
                    // Delayed view of the rail's 0.5-second lookahead, sampled by a bounded hand.
                    seen[frame%120]=CaveMining.curve(m.progress+m.segment/CaveMining.SEGMENT+.5f/CaveMining.SEGMENT);
                    wait-=1f/60;
                    if(wait<=0){int past=Math.max(0,frame-(int)(reactions[tier]*60));float target=seen[past%120];
                        m.steer(rng.nextFloat()<errors[tier]?-target:target);wait=1/rates[tier];}
                    m.update(c,1f/60);frame++;
                }
                if(m.won){won=true;break;}
            }
            check("bounded rider completes "+rates[tier],won);
            System.out.printf("    cart %.0f/s react %.2fs miss %.0f%%: %d visits, %d/20 track%n",rates[tier],reactions[tier],errors[tier]*100,visits,store.mineTrack);
        }
    }
}
