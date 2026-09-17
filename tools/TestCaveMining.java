package com.dddumpling.game;

final class TestCaveMining extends Check {
    static GameCore game(Layout L,Mem store) {
        GameCore c=new GameCore(store,957L);c.caveChoice=0;c.startGame();c.jumpToStage(22,L);
        c.stageBanner=0;Interlude.enterBonus(c,L);return c;
    }
    static void fill(GameCore c) {
        CaveMining m=c.mining;
        while(m.phase==CaveMining.DIG) c.tapBonus(m.sequence[m.pos]);
    }
    static void all(Layout L) {
        group("cave mining");
        Mem store=new Mem();GameCore c=game(L,store);CaveMining m=c.mining;
        check("second cave interlude selects only mining",m.active && !c.band.active && !c.starBonus
                && !c.bonusMashing() && !c.bonusRolling() && !c.bonusHolding() && !c.bonusStatus());
        c.tapBonus(m.sequence[0]);check("ready beat does not mine",m.pos==0);
        c.update(CaveMining.READY,L);
        check("first cart alternates two different keys",m.length==2 && m.sequence[0]!=m.sequence[1]);
        c.tapBonus(m.sequence[0]);check("first key advances sequence without dropping rocks",m.pos==1 && m.loads==0);
        c.tapBonus(m.sequence[1]);check("complete sequence drops one rock load",m.pos==0 && m.loads==1 && m.falling[0]>0);
        c.tapBonus(m.sequence[0]);c.tapBonus((m.sequence[1]+1)%6);
        check("wrong key cannot complete sequence",m.loads==1 && m.pos<m.length);
        m.pos=0;
        for(int n=0;n<4;n++) {
            for(int key=0;key<m.length;key++)c.tapBonus(m.sequence[key]);
            check("cart needs all five repetitions "+n,m.loads==n+2 && m.phase==(n==3?CaveMining.FULL:CaveMining.DIG));
        }
        check("filling alone never credits a cart",m.carts==0 && store.mineSaves==0);
        for(int g=0;g<6;g++)c.tapBonus(g);
        check("full cart ignores all mining keys",m.loads==5 && m.pos==0 && m.carts==0);
        int lives=c.lives,score=c.score;
        check("key finger cannot start cart swipe",!m.input.down(c,L,1,c.keyX(L,0),c.keyY(L,0)));
        float cx=m.cartX*L.w,cy=CaveMiningScreen.cartY(L);
        check("cart catches swipe",m.input.down(c,L,2,cx,cy));
        m.input.down(c,L,3,cx,cy);m.input.move(c,L,3,cx+L.w*.3f,cy);m.input.up(3);
        check("second finger cannot move or release cart",m.input.pointer==2 && m.carts==0);
        m.input.move(c,L,2,cx+L.w*.03f,cy);m.input.up(2);
        check("short drag cannot dispatch cart",m.carts==0 && m.phase==CaveMining.FULL);
        m.update(c,.3f);cx=m.cartX*L.w;
        m.input.down(c,L,2,cx,cy);m.input.move(c,L,2,cx+L.w*.18f,cy+L.w*.3f);
        check("vertical drag cannot dispatch cart",m.carts==0);m.input.up(2);
        m.update(c,.3f);cx=m.cartX*L.w;m.input.down(c,L,2,cx,cy);m.input.move(c,L,2,cx+L.w*.18f,cy);
        check("sideways swipe dispatches and saves once",m.phase==CaveMining.PUSH && m.carts==1 && store.mineCarts==1 && store.mineSaves==1);
        m.launch(c,1);c.tapBonus(m.sequence[0]);check("pushing cannot mine or save twice",m.loads==5 && store.mineSaves==1);
        float left=m.left;m.update(c,.5f);check("helpers do not spend player's lantern time",m.left==left && m.cartX>.56f);
        c.toTitle();c.startGame();check("new run retains delivered cart",c.mining.carts==1);
        c=game(L,store);m=c.mining;check("restart restores cart and three-key difficulty",m.carts==1 && m.length==3);
        m.ready=0;
        for(int cart=1;cart<5;cart++) {
            check("sequence length rises then caps "+cart,m.length==Math.min(4,cart+2));
            boolean unique=true;
            for(int i=0;i<m.length;i++)for(int j=0;j<i;j++)if(m.sequence[i]==m.sequence[j])unique=false;
            check("sequence uses distinct available keys "+cart,unique);
            fill(c);m.launch(c,cart%2==0?1:-1);
            check("each cart saves at the committed swipe "+cart,store.mineCarts==cart+1);
            c.update(CaveMining.PUSH_TIME,L);
        }
        check("fifth cart grants exactly one reward",m.won && c.collectTotal==1 && c.paradeTimer>0);
        check("completed mine awards a mole",Collect.FAMILY[c.prize]==Collect.MOLES);
        check("reward resets saved cart progress",m.carts==0 && store.mineCarts==0);
        c.update(.1f,L);check("reward cannot repeat",c.collectTotal==1);
        c.update(CaveMining.REPORT_TIME+GameCore.PARADE_TIME+1,L);
        check("mining parade returns to next cave stage",c.state==GameCore.PLAY && c.stage==23 && !m.active);
        Interlude.enterBonus(c,L);check("next cave interlude returns to band",c.band.active && !m.active);
        store.mineCarts=5;c=game(L,store);int prior=c.collectTotal;c.update(.01f,L);
        check("restart after fifth swipe still grants pending reward",c.mining.won && c.collectTotal==prior+1 && store.mineCarts==0);
        store.mineCarts=2;c=game(L,store);m=c.mining;m.ready=0;fill(c);
        m.input.down(c,L,2,m.cartX*L.w,cy);Pause.open(c);float before=m.left;c.update(8,L);c.tapBonus(0);
        check("pause freezes lantern and releases cart finger",m.left==before && m.input.pointer<0 && m.carts==2);Pause.resume(c);
        c.openSettings();c.update(8,L);check("settings freezes lantern",m.left==before);c.closeSettings();
        c.update(CaveMining.TIME+.01f,L);
        check("timeout gives no life loss and saves only delivered carts",m.phase==CaveMining.REPORT && !m.won && m.carts==2 && store.mineCarts==2 && c.lives==lives);
        c.update(CaveMining.REPORT_TIME+.01f,L);check("timeout leaves interlude",c.state==GameCore.PLAY);
        store.mineCarts=-9;c=game(L,store);check("negative saved carts clamp",c.mining.carts==0);
        store.mineCarts=999;c=game(L,store);check("overflow saved carts clamp",c.mining.carts==5);
        c=game(L,new Mem());m=c.mining;m.ready=0;
        float bright=CaveMiningScreen.light(m);c.update(9,L);
        check("lantern visibly dims over attempt",CaveMiningScreen.light(m)<bright*.6f);
        c.toTitle();c.startGame();c.jumpToStage(19,L);Interlude.enterBonus(c,L);
        check("ordinary lands retain ordinary games",!CaveInterlude.active(c));
        c=game(L,new Mem());c.preferences.kids=true;c.startGame();c.jumpToStage(22,L);Interlude.enterBonus(c,L);
        m=c.mining;m.carts=3;m.begin(c);m.ready=0;
        boolean starters=true;for(int g:m.sequence)starters &= Roster.active(false,g);
        check("four-key mode has an achievable four-long sequence",starters && m.length==4);
        c.update(2f,L);check("kids mode grants a slower lantern",Math.abs(m.left-(CaveMining.TIME-.9f))<.01f);
        bounded(L);
    }
    private static void bounded(Layout L) {
        float[] rates={3,5,8},reactions={.30f,.20f,.13f},errors={.08f,.04f,.02f};
        for(int tier=0;tier<rates.length;tier++) {
            float rate=rates[tier];java.util.Random hand=new java.util.Random(704+tier);
            Mem store=new Mem();int attempts=0,keys=0;boolean earned=false;
            while(attempts<6) {
                GameCore c=game(L,store);CaveMining m=c.mining;m.ready=0;attempts++;
                float wait=reactions[tier];int phase=m.phase;
                for(int t=0;t<3000 && m.phase!=CaveMining.REPORT;t++) {
                    c.update(1f/rate,L);
                    if(m.phase!=phase){phase=m.phase;wait=reactions[tier];}
                    if(wait>0){wait-=1f/rate;continue;}
                    if(m.digging()) {
                        int key=m.sequence[m.pos];
                        if(hand.nextFloat()<errors[tier])key=Roster.randomExcept(c.playRosterFull(),key,hand);
                        c.tapBonus(key);keys++;
                    } else if(m.swipeReady())m.launch(c,1);
                }
                if(m.won){earned=c.collectTotal>0;break;}
            }
            check("bounded miner can earn reward "+rate,earned && attempts<6);
            System.out.printf("    mining %.0f/s react %.2fs miss %.0f%%: reward in %d visits, %d keys%n",
                    rate,reactions[tier],errors[tier]*100,attempts,keys);
        }
    }
}
