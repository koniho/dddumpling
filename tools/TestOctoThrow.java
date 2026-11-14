package com.dddumpling.game;

final class TestOctoThrow extends Check {
    static GameCore tear(Layout L, int removed) {
        return tear(L, removed, false);
    }

    static GameCore kidsEncounter(Layout L) {
        GameCore c=new GameCore(new Mem(),1415L);c.preferences.kids=true;c.startGame();
        c.enterStage(WorldCatalog.bossStage(Boss.OCTOPUS));
        c.stageBanner=c.stageGap=0;c.boss.intro=0;c.boss.update(0,L,c.rnd);
        return c;
    }

    static GameCore tear(Layout L, int removed, boolean kids) {
        GameCore c = kids ? kidsEncounter(L) : TestBoss.enterBoss(L, Boss.OCTOPUS, 810L + removed);
        Boss b = c.boss;
        b.octoArms = (1 << (9 - removed)) - 1;
        b.hp = 9 - removed;
        b.octoPause = 10f;
        b.octoVulnerableArm = 8 - removed;
        b.held = -3;
        b.octoDragStarted = b.octoDragCanDamage = true;
        b.dragTo(L.playLeft - L.keyR, L.playTop, L);
        return c;
    }

    static void run(Layout L) {
        kidsAssistance(L);
        group("octopulse arm throws");
        for (int removed = 1; removed <= 8; removed++) for (int parry = 0; parry < 2; parry++) {
            GameCore c = tear(L, removed);
            Boss b = c.boss;
            int expected = removed == 8 ? 0 : removed == 7 ? 3 : removed >= 5 ? 2 : 1;
            int launches = 0, events = 0, hits = 0, cleared = 0, lastLaunch = -1000;
            boolean validArm = true, waiting = true, spaced = true, tipOrigin = true, wound = true;
            float windY = Float.NaN, releaseY = Float.NaN;
            for (int frame = 0; frame < 480; frame++) {
                boolean outstanding = OctoThrow.busy(b) || b.boltCount() > 0;
                hits += b.update(DT, L, c.rnd);
                if (outstanding && (OctoThrow.busy(b) || b.boltCount() > 0))
                    waiting &= b.octoTarget < 0 && !b.octoWave;
                if (b.octoThrowArm >= 0) {
                    validArm &= (b.octoArms & (1 << b.octoThrowArm)) != 0;
                    if (b.octoThrowT > .45f && b.octoThrowT < .50f)
                        windY = b.octoY[b.octoThrowArm][Boss.OCTO_NODES - 1];
                    if (b.octoThrowT < OctoThrow.RELEASE) wound &= !b.launched;
                }
                if (b.launched) {
                    events++;
                    if (lastLaunch >= 0 && removed == 7)
                        spaced &= Math.abs((frame - lastLaunch) * DT - OctoThrow.END * .5f) < DT * 2f;
                    else spaced &= frame - lastLaunch >= 45;
                    lastLaunch = frame;
                    int tip = Boss.OCTO_NODES - 1;
                    releaseY = b.octoY[b.octoThrowArm][tip];
                    boolean found = false;
                    for (int i = 0; i < Boss.MAX_BOLTS; i++) if (b.blive[i] && b.bt[i] == 0f) {
                        launches++;
                        int arm = b.bglyph[i] == b.octoThrowGlyph2 ? b.octoThrowArm2 : b.octoThrowArm;
                        found |= Math.abs(b.bsx[i] - b.octoX[arm][tip]) < .01f
                                && Math.abs(b.bsy[i] - b.octoY[arm][tip]) < .01f;
                        validArm &= (b.octoArms & (1 << arm)) != 0;
                        validArm &= !b.keyDisabled(b.bglyph[i]);
                    }
                    tipOrigin &= found;
                    if (removed == 5 || removed == 6) {
                        check("pair launches on the same frame", launches == 2 && b.octoThrowArm2 >= 0);
                        check("pair uses different arms and keys", b.octoThrowArm != b.octoThrowArm2
                                && b.octoThrowGlyph != b.octoThrowGlyph2);
                    }
                }
                if (parry != 0) for (int i = 0; i < Boss.MAX_BOLTS; i++) if (b.blive[i]) {
                    cleared += b.press(b.bglyph[i], c.rnd, L) == Boss.PARRY ? 1 : 0;
                }
                if (!OctoThrow.busy(b) && b.boltCount() == 0 && b.octoTarget >= 0) break;
            }
            check("tear " + removed + " throws " + expected + " enemies, parry=" + parry, launches == expected);
            check("volley uses the requested number of release events", events == (expected == 2 ? 1 : expected));
            check("throw uses intact arm and enabled key", validArm);
            check("next attack waits for all throws to resolve", waiting);
            check("each throw has a wind-up and distinct release", spaced && wound && tipOrigin);
            check("thrown enemies resolve normally", parry == 0 ? hits == expected : cleared == expected && hits == 0);
            if (expected > 0) {
                check("wind-up lifts the hand before forward throw", releaseY - windY > Boss.bodyR(L));
                check("next arm wave resumes after the field clears", b.octoTarget >= 0);
            } else check("last tear defeats without retaliation", b.beaten && !OctoThrow.busy(b));
        }
        GameCore speed = TestBoss.enterBoss(L, Boss.OCTOPUS, 888L);
        Boss fast = speed.boss;
        fast.octoPause = 10f;
        fast.blive[0] = true; fast.bfast[0] = true; fast.bt[0] = 0f;
        for (int i = 0; i < 36; i++) fast.update(DT, L, speed.rnd);
        check("Octopulse projectile is halfway there after 0.6 seconds", Math.abs(fast.bt[0] - .5f) < .001f);
        int arrivals = 0;
        for (int i = 0; i < 37; i++) arrivals += fast.update(DT, L, speed.rnd);
        check("Octopulse projectile lands after 1.2 seconds", arrivals == 1 && !fast.blive[0]);
        GameCore missed = TestBoss.enterBoss(L, Boss.OCTOPUS, 889L);
        Boss slow = missed.boss;
        // Reuse a fast projectile slot for the missed-tear punishment.
        slow.bfast[0] = true;
        slow.octoVulnerableArm = 3;
        slow.octoDragTime = 1.99f;
        slow.update(DT, L, missed.rnd);
        check("missed tear launches at the original speed even in a reused slot", slow.blive[0] && !slow.bfast[0]);
        for (int i = 0; i < 72; i++) slow.update(DT, L, missed.rnd);
        check("missed-tear projectile is only halfway there after 1.2 seconds", slow.blive[0]
                && Math.abs(slow.bt[0] - .5f) < .001f);
        GameCore reset = tear(L, 7);
        reset.boss.leave();
        check("leaving clears queued throws", !OctoThrow.busy(reset.boss));
    }

    private static void readyStrike(Boss b) {
        b.octoTarget=0;b.octoAttackArm=0;b.octoSweep=b.octoCharge=1;b.octoReach=0;
    }

    private static void kidsAssistance(Layout L) {
        group("kids octopulse assistance");
        for(int stolen:new int[]{0,1<<1,(1<<1)|(1<<4)}) {
            GameCore c=kidsEncounter(L);Boss b=c.boss;readyStrike(b);b.disabledKeys=stolen;
            float normalWindow=Math.max(.36f,.925f-Integer.bitCount(stolen)*.125f);
            for(int frame=0;frame<(int)((normalWindow+.15f)/DT);frame++)b.update(DT,L,c.rnd);
            check("kids response remains open past normal deadline "+stolen,b.octoTarget==0 && b.octoReach<1);
            c.tapKey(0,L);
            check("late-but-valid kids response exposes the arm "+stolen,b.octoVulnerableArm==0);
            GameCore miss=kidsEncounter(L);readyStrike(miss.boss);miss.boss.disabledKeys=stolen;
            for(int frame=0;frame<(int)((normalWindow*2+.1f)/DT);frame++)miss.boss.update(DT,L,miss.rnd);
            check("kids response still has a finite doubled deadline "+stolen,miss.boss.octoTarget<0);
        }
        GameCore early=kidsEncounter(L);Boss e=early.boss;
        for(int frame=0;frame<300 && e.octoTarget<0;frame++)e.update(DT,L,early.rnd);
        int key=e.octoTarget,lives=early.lives;
        early.tapKey(key,L);
        check("kids early press only gives timing feedback",e.octoFeedback==Boss.OCTO_EARLY
                && early.lives==lives && e.octoLash==0 && e.disabledKeys==0 && e.octoVulnerableArm<0);
        for(int frame=0;frame<120 && e.octoReach<0;frame++)e.update(DT,L,early.rnd);
        early.tapKey(key,L);
        check("kids can retry the same key when the arm is ready",e.octoVulnerableArm>=0 && early.lives==lives);
        for(int frame=0;frame<150;frame++)e.update(DT,L,early.rnd);
        check("kids arm remains grabbable after two and a half seconds",e.octoVulnerableArm>=0
                && Math.abs(e.octoDragProgress()-.625f)<.01f);
        int arm=e.octoVulnerableArm,tip=Boss.OCTO_NODES-1;
        early.grabBoss(e.octoX[arm][tip],e.octoY[arm][tip]);
        early.dragBoss(L.w*.5f,(L.playTop+L.dangerY)*.5f,L);
        early.dragBoss(L.playLeft-L.keyR,L.playTop,L);
        check("a late kids grab can still tear off the arm",(e.octoArms&(1<<arm))==0);

        GameCore escaped=kidsEncounter(L);Boss b=escaped.boss;readyStrike(b);escaped.tapKey(0,L);
        int arrivals=0,maxBolts=0;
        for(int frame=0;frame<60*8;frame++) {
            arrivals+=b.update(DT,L,escaped.rnd);maxBolts=Math.max(maxBolts,b.boltCount());
            if(frame==180)check("missed kids grab has not expired at three seconds",b.octoVulnerableArm==0);
            if(frame==245)check("missed kids grab expires after four seconds",b.octoVulnerableArm<0 && b.boltCount()==1);
        }
        check("missed kids grab retaliates with one projectile",arrivals==1 && maxBolts==1 && b.octoFlurryLeft==0);

        for(int removed=1;removed<=8;removed++)for(boolean parry:new boolean[]{false,true}) {
            GameCore c=tear(L,removed,true);b=c.boss;
            int hits=0,cleared=0,launches=0,max=0;
            boolean waited=true;
            for(int frame=0;frame<60*8;frame++) {
                hits+=b.update(DT,L,c.rnd);max=Math.max(max,b.boltCount());
                if(b.launched)launches++;
                if(OctoThrow.busy(b) || b.boltCount()>0)waited&=b.octoTarget<0;
                if(parry)for(int i=0;i<Boss.MAX_BOLTS;i++)if(b.blive[i])
                    cleared+=b.press(b.bglyph[i],c.rnd,L)==Boss.PARRY?1:0;
                if(!OctoThrow.busy(b) && b.boltCount()==0)break;
            }
            int expected=removed==8?0:1;
            check("kids tear has one resolvable retaliation "+removed+"/"+parry,
                    launches==expected && max==expected && waited
                    && (parry?cleared==expected && hits==0:hits==expected));
        }
    }
}
