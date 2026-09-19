package com.dddumpling.game;

final class TestCaveCart extends Check {
    static GameCore game(Layout L,Mem store){
        GameCore c=new GameCore(store,957L);c.caveChoice=0;c.startGame();c.jumpToStage(21,L);
        c.stageBanner=0;Interlude.enterBonus(c,L);return c;
    }
    static void all(Layout L){
        group("cave minecart");
        Mem store=new Mem();GameCore c=game(L,store);CaveCart m=c.cart;
        check("first cave slot replaces rhythm with minecart",m.active && !c.band.active && !c.mining.active && !c.starBonus);
        m.update(c,1);check("ready holds progress and balance",m.progress==0 && m.segment==0 && m.balance==0);
        float y=StarScreen.sliderY(L);
        check("outside slider does not steer",!m.input.down(c,L,1,L.w*.1f,L.playTop));
        check("Star Path slider starts left lean",m.input.down(c,L,2,L.w*.2f,y) && m.intent<0);
        m.input.down(c,L,3,L.w*.8f,y);m.input.move(c,L,3,L.w*.8f,y);m.input.up(3);
        check("second finger cannot steal steering",m.input.pointer==2 && m.intent<0);
        m.input.move(c,L,2,L.w*.8f,y);check("drag crosses to right lean",m.intent>0);
        m.input.up(2);check("release retains direct slider position",m.intent>0 && m.input.pointer<0);
        float lean=m.lean;
        m.input.down(c,L,4,L.w*.6f,L.playTop+CaveCartScreen.field(L)*.72f);
        check("grabbing cart does not snap lean",Math.abs(m.lean-lean)<.001f);m.input.up(4);
        m.press(c,Roster.at(c.playRosterFull(),0));check("left keys lean left",m.intent<0);
        m.press(c,Roster.at(c.playRosterFull(),Roster.count(c.playRosterFull())-1));check("right keys lean right",m.intent>0);
        Pause.open(c);float before=m.ready;c.update(2,L);check("pause freezes ride",m.ready==before && m.input.pointer<0 && m.intent==0);Pause.resume(c);
        c.openSettings();c.update(2,L);check("settings freezes ride",m.ready==before);c.closeSettings();
        for(int visit=0;visit<3;visit++){
            c=game(L,store);m=c.cart;m.ready=0;int lives=c.lives;
            m.update(c,3);
            check("idle spills without farming progress "+visit,m.spilled && m.progress==0 && c.lives==lives);
            check("spill rumble consumed once "+visit,m.scene.takeFeedback()==2 && m.scene.takeFeedback()==0);
        }
        store.mineCarts=2;c=game(L,store);check("mining carts stay separate from ride",c.cart.progress==0 && c.mining.carts==2);
        store.cartTrack=-9;c=game(L,store);check("negative track clamps",c.cart.progress==0);
        store.cartTrack=999;c=game(L,store);check("track overflow clamps",c.cart.progress==20);
        int total=c.collectTotal;c.update(.01f,L);
        check("saved completion pays and resets",c.collectTotal==total+1 && store.cartTrack==0 && c.cart.progress==0);
        check("ride earns snake prize",Collect.FAMILY[c.prize]==Collect.SNAKES);
        c.update(.1f,L);check("reward pays once",c.collectTotal==total+1);
        store=new Mem();c=game(L,store);m=c.cart;m.ready=0;
        for(int f=0;f<120;f++){m.steer(CaveCart.curve(m.progress+m.segment/CaveCart.SEGMENT));m.update(c,1f/120);}
        check("successful bend saves immediately",m.progress==1 && store.cartTrack==1);
        c=game(L,store);check("restart resumes completed track only",c.cart.progress==1 && c.cart.segment==0);
        c.toTitle();c.startGame();check("new run keeps ride progress",c.cart.progress==1);
        for(int i=0;i<20;i++)check("track preview follows shared bend "+i,Math.signum(CaveCart.curve(i+.5f))==Math.signum(CaveCart.bend(i)));
        c=game(L,new Mem());m=c.cart;m.ready=0;Ear ear=new Ear();c.sound=ear;
        m.steer(-.8f);m.update(c,.05f);check("moving cart plays new rail clatter",ear.lastCaveSound==Sfx.CART_ROLL);
        int sounds=ear.caveSounds;m.update(c,.1f);check("clatter does not repeat per frame",ear.caveSounds==sounds);
        c=game(L,new Mem());m=c.cart;m.ready=0;
        while(m.phase==CaveCart.RIDE){m.steer(CaveCart.curve(m.progress+m.segment/CaveCart.SEGMENT));m.update(c,1f/120);}
        check("perfect rider ends visit safely with partial progress",!m.spilled && !m.won && m.progress==10);
        GameCore a=game(L,new Mem()),b=game(L,new Mem());a.cart.ready=b.cart.ready=0;
        a.cart.steer(-.8f);b.cart.steer(-.8f);a.cart.update(a,.5f);
        for(int f=0;f<30;f++)b.cart.update(b,1f/60);
        check("frame partition preserves balance",Math.abs(a.cart.balance-b.cart.balance)<.001f && a.cart.progress==b.cart.progress);
        for(int point=0;point<CaveCart.TRACK;point++){
            c=game(L,new Mem());m=c.cart;m.progress=point;m.begin(c);m.ready=0;m.update(c,3);
            check("idle cannot farm saved section "+point,m.progress==point && m.spilled);
        }
        float min=1,max=0,step=0;
        for(int i=0;i<20;i++){min=Math.min(min,Math.abs(CaveCart.bend(i)));max=Math.max(max,Math.abs(CaveCart.bend(i)));}
        for(int i=1;i<20000;i++)step=Math.max(step,Math.abs(CaveCart.curve(i*.001f)-CaveCart.curve((i-1)*.001f)));
        check("track mixes gentle and sharp bends",max-min>.2f);
        check("curvature changes smoothly through turns",step<.006f);
        m=new CaveCart();m.turn=.4f;float gentle=CaveCartScreen.bank(m);m.turn=.9f;
        check("sharper curve banks the cart further",CaveCartScreen.bank(m)>gentle*1.8f);
        m.turn=-.9f;check("bank reverses with bend direction",CaveCartScreen.bank(m)<0);
        m.progress=3;m.segment=CaveCart.SEGMENT*.7f;m.turn=CaveCart.curve(3.7f);
        float z=CaveCartScreen.CART_Z,delta=.01f;
        float curvature=(CaveCartScreen.railX(m,z+delta)-2*CaveCartScreen.railX(m,z)+CaveCartScreen.railX(m,z-delta))/(delta*delta);
        check("rail curvature at the cart matches the live turn",Math.abs(curvature-m.turn*.2f*2.2f*2.2f)<.01f);
        recovery(L);
        recordedCadence(L);
        rotation(L);
        bounded(L);
    }
    private static void recovery(Layout L){
        GameCore c=game(L,new Mem());CaveCart m=c.cart;m.ready=0;m.progress=3;m.segment=.6f;
        m.balance=-1;m.steer(1);m.hold=Float.POSITIVE_INFINITY;
        m.update(c,.8f);check("red allows correction before falling",m.phase==CaveCart.RIDE && m.danger>.7f);
        m.update(c,.5f);check("continuous red eventually spills",m.spilled);
        c=game(L,new Mem());m=c.cart;m.ready=0;m.progress=3;m.segment=.6f;
        m.balance=-.9f;m.danger=.4f;m.steer(-.9f);m.lean=-.9f;m.hold=Float.POSITIVE_INFINITY;
        m.update(c,.5f);
        check("correct lean clears red countdown",m.phase==CaveCart.RIDE && m.danger==0 && Math.abs(m.balance)<CaveCart.RED);
        m.danger=.8f;Pause.open(c);c.update(3,L);check("pause freezes recovery countdown",m.danger==.8f);
        c=game(L,new Mem());m=c.cart;m.ready=0;m.segment=CaveCart.SEGMENT-.01f;
        m.update(c,.03f);check("unhandled checkpoint cannot instantly spill",m.phase==CaveCart.RIDE && m.progress==0);
    }
    private static void recordedCadence(Layout L){
        final GameCore c=game(L,new Mem());c.cart.ready=0;
        final float[] last={-10},gap={10};final int[] rolls={0};
        c.sound=new Ear(){public void caveEvent(int id){if(id==Sfx.CART_ROLL){
            gap[0]=Math.min(gap[0],c.cart.elapsed-last[0]);last[0]=c.cart.elapsed;rolls[0]++;
        }}};
        for(int frame=0;frame<310;frame++){
            c.cart.steer(CaveCart.curve(c.cart.progress+c.cart.segment/CaveCart.SEGMENT));
            c.cart.update(c,1f/120);
        }
        check("recorded roll finishes before replay",rolls[0]>=2 && gap[0]>=Sfx.build(Sfx.CART_ROLL).length/(float)Sfx.RATE-.001f);
    }
    private static void rotation(Layout L){
        Mem saved=new Mem();GameCore c=game(L,saved);
        c.cart.ready=0;c.update(2,L);c.update(3,L);
        Interlude.enterBonus(c,L);
        check("failed ride retries ride on the next stage",c.cart.active && !c.mining.active && !saved.caveMiningNext);
        c.cart.progress=CaveCart.TRACK;c.cart.begin(c);c.update(.01f,L);
        check("ride success switches to mining durably",c.caveMiningNext && saved.caveMiningNext);
        c=new GameCore(saved,77);c.caveChoice=0;c.startGame();c.jumpToStage(23,L);Interlude.enterBonus(c,L);
        check("restart remembers mining despite stage parity",c.mining.active && !c.cart.active);
        c.mining.ready=0;c.update(CaveMining.TIME+1,L);c.update(CaveMining.REPORT_TIME+1,L);Interlude.enterBonus(c,L);
        check("failed mining retries mining",c.mining.active && saved.caveMiningNext);
        c.mining.carts=CaveMining.CARTS;c.mining.begin(c);c.update(.01f,L);
        check("mining success switches back to ride durably",!c.caveMiningNext && !saved.caveMiningNext);
        c.toTitle();c.startGame();c.jumpToStage(24,L);Interlude.enterBonus(c,L);
        check("new run remembers ride despite stage parity",c.cart.active && !c.mining.active);
    }
    private static void bounded(Layout L){
        float[] rates={3,5,8},reactions={.30f,.20f,.13f},errors={.08f,.04f,.02f};
        for(int tier=0;tier<3;tier++){
            Mem store=new Mem();java.util.Random rng=new java.util.Random(704+tier);int visits=0;boolean won=false;
            while(visits++<12){
                GameCore c=game(L,store);CaveCart m=c.cart;m.ready=0;float wait=0;
                float[] seen=new float[120];int frame=0;
                while(m.phase==CaveCart.RIDE && frame<1400){
                    // Delayed view of the rail's 0.5-second lookahead, sampled by a bounded hand.
                    seen[frame%120]=CaveCart.curve(m.progress+m.segment/CaveCart.SEGMENT+.5f/CaveCart.SEGMENT);
                    wait-=1f/60;
                    if(wait<=0){int past=Math.max(0,frame-(int)(reactions[tier]*60));float target=seen[past%120];
                        m.steer(rng.nextFloat()<errors[tier]?-target:target);wait=1/rates[tier];}
                    m.update(c,1f/60);frame++;
                }
                if(m.won){won=true;break;}
            }
            check("bounded rider completes "+rates[tier],won);
            System.out.printf("    cart %.0f/s react %.2fs miss %.0f%%: %d visits, %d/20 track%n",rates[tier],reactions[tier],errors[tier]*100,visits,store.cartTrack);
        }
    }
}
