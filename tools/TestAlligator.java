package com.dddumpling.game;

final class TestAlligator extends Check {
    static GameCore fight(Layout L) {
        GameCore c=new GameCore(TestTimeAttack.store(),168,true);c.startGame();
        c.enemies.clear();c.power=null;c.modeLeft=0;
        c.stage=Lands.firstStage(Lands.SWAMP)+Boss.EVERY-1;
        c.boss.begin(Boss.ALLIGATOR,c.stage,c.rnd,c.playRosterFull());
        c.boss.intro=0;c.boss.update(DT,L,c.rnd);
        return c;
    }
    static void advance(GameCore c,Layout L,float seconds) {
        for(float t=0;t<seconds;t+=DT)c.boss.update(DT,L,c.rnd);
    }
    static void ready(GameCore c,Layout L) {
        for(int n=0;n<240 && (c.boss.alligator.limb<0 || c.boss.alligator.blocked || c.boss.alligator.attack<.3f);n++)c.boss.update(DT,L,c.rnd);
    }
    static void block(GameCore c,Layout L) {
        ready(c,L);Alligator a=c.boss.alligator;
        a.touch(c,L,0,168,a.tipX-L.w*.12f,a.tipY);
        a.touch(c,L,1,168,a.tipX+L.w*.12f,a.tipY);
    }
    static void all(Layout L) {
        group("Swampland progression and stable rewards");
        check("swamp has its own final boss",Boss.kindFor(Lands.firstStage(Lands.SWAMP)+4)==Boss.ALLIGATOR);
        GameCore c=fight(L);
        check("swamp scenery uses the authored land",Lands.forStage(c.stage)==Lands.SWAMP);
        check("mushroom friend opens swamp",LandPicker.unlocked(c,Lands.SWAMP));
        long old=Collect.OLD_TAG|((1L<<60)-1);
        check("existing ducks survive and old tag does not grant a gator",Collect.decode(old)==Collect.OLD_MASK && !Collect.has(Collect.decode(old),60));
        long owned=Collect.add(Collect.decode(old),Collect.ALLIGATOR_FRIEND);
        check("expanded collection round trips",Collect.decode(Collect.encode(owned))==owned);
        check("old boss and duck IDs do not move",Collect.bossPrize(Boss.MUSHROOM)==48 && Collect.DUCK_FIRST==49
                && Collect.bossPrize(Boss.ALLIGATOR)==60 && Collect.bossKind(49)==-1);
        group("alligator contacts and swipe defenses");
        float hp=c.boss.hp;int lives=c.lives;
        check("keys cannot damage during defense",c.boss.press(0,c.rnd,L)==Boss.REBUFF && c.boss.hp==hp);
        ready(c,L);Alligator a=c.boss.alligator;
        a.touch(c,L,0,4,a.tipX,a.tipY);a.touch(c,L,1,4,a.tipX+L.unit*.05f,a.tipY);
        check("tap and tiny jitter do not block",a.blocks==0 && !a.blocked);
        a.touch(c,L,0,4,L.w*.3f,L.playTop);a.touch(c,L,1,4,L.w*.7f,L.playTop);
        check("swipes elsewhere do not block or damage player",a.blocks==0 && c.lives==lives);
        int contacts=0;
        for(int i=0;i<100 && contacts==0;i++) {
            int hits=c.boss.update(DT,L,c.rnd);
            if(hits>0)check("damage waits for visible tip contact",Math.hypot(a.tipX-a.targetX,a.tipY-a.targetY)<=a.tipRadius(L)+a.targetR+.01f);
            contacts+=hits;
        }
        check("one incoming limb reports one contact",contacts==1);
        for(int i=0;i<15;i++)contacts+=c.boss.update(DT,L,c.rnd);
        check("retracting limb cannot deal repeated damage",contacts==1);
        c=fight(L);a=c.boss.alligator;
        for(int n=0;n<9;n++) {
            ready(c,L);int limb=a.limb;
            block(c,L);
            check("arms and tail rotate through defenses "+n,limb==n%3 && a.blocks==n+1);
        }
        check("ninth swipe dazes boss with no health loss",c.boss.open() && a.daze>0 && c.boss.hp==hp);
        for(int g=0;g<Glyph.COUNT;g++) {
            check("every key can hurt the dazed boss without a hidden cooldown "+g,c.boss.press(g,c.rnd,L)==Boss.HIT);
        }
        check("window has a bounded damage budget and returns to defense",a.daze==0 && c.boss.hp==hp-6 && a.blocks==0);
        for(int i=0;i<9;i++)block(c,L);
        advance(c,L,Alligator.DAZE+.1f);
        check("expired window retains earned damage",a.daze==0 && c.boss.hp==hp-6);
        a.pointer=4;Pause.release(c);check("pause releases swipe ownership",a.pointer==-1);
        c.boss.leave();check("leaving clears limbs, body and gesture",a.limb==-1 && a.blocks==0 && a.tummy==null && a.pointer==-1);
        group("alligator game wiring and bounded player");
        c=fight(L);c.lives=1;
        for(int i=0;i<400 && c.state==GameCore.PLAY;i++)c.update(DT,L);
        check("unblocked contact reaches normal game-over flow",c.state!=GameCore.PLAY && c.lives==0);
        for(int hz:new int[]{30,60,120}) {
            c=fight(L);Bot bot=new Bot(6f,.2f,.04f,false,168);float dt=1f/hz,t=0;
            while(c.boss.fighting() && t<65 && c.state==GameCore.PLAY) {bot.step(c,L,dt);c.update(dt,L);t+=dt;}
            check("bounded hands beat alligator at "+hz+"fps",c.boss.beaten && c.lives>0);
            System.out.printf("    alligator %dfps: %.1fs, %d lives%n",hz,t,c.lives);
        }
        c=fight(L);for(int i=0;i<18;i++) {block(c,L);if(c.boss.alligator.daze>0)for(int g=0;g<6;g++){advance(c,L,.16f);c.boss.press(g,c.rnd,L);}}
        c.boss.leaveT=0;BossPlay.endBoss(c,L);
        check("victory awards the mapped boss friend",c.prize==Collect.ALLIGATOR_FRIEND && Collect.has(c.collected,Collect.ALLIGATOR_FRIEND));
        group("Time Attack catalog migration");
        TimeAttack oldRuns=new TimeAttack();StringBuilder legacy=new StringBuilder("1");
        for(int p=0;p<3;p++)for(int i=0;i<5;i++)legacy.append(';').append(i==4?42000:10000+i);
        oldRuns.load(legacy.toString());
        check("legacy solo times preserved and new challenge starts clean",oldRuns.best[0][0]==10000 && oldRuns.best[0][Boss.ALLIGATOR]==0 && oldRuns.best[0][TimeAttack.ALL]==0);
        check("old four-boss records archived",oldRuns.classicBest[0]==42000 && oldRuns.classicHistory[0].runs.get(0).cleared==4);
        TimeAttack again=new TimeAttack();again.load(oldRuns.encode());
        check("archive survives subsequent saves",again.classicBest[0]==42000 && again.best[0][3]==10003);
    }
}
