package com.dddumpling.game;

/** First-use power guidance shares the live run and learns only successful mechanics. */
final class TestPowerTutorials extends Check {
    static GameCore fresh(Layout L,Mem store) {
        store.tutorials=store.powerTutorials=0;store.collected=1;
        GameCore c=new GameCore(store,114);c.startGame();c.stage=6;c.stageBanner=0;c.spawnTimer=9999;
        c.enemies.clear();return c;
    }
    static void use(GameCore c,Layout L,int kind) {
        c.enemies.clear();c.target=null;
        GameCore.Enemy e=add(c,L,new int[]{0},L.playTop+L.enemyR*4);
        if(kind==Power.FLING) {
            float x=c.tileX(e,0,L);
            c.beginStroke(x-L.enemyR*2,e.y);c.sliceTo(x+L.enemyR*2,e.y,L);c.endStroke();
        } else c.tapKey(kind==Power.FLURRY?1:0,L);
    }
    static void all(Layout L) {
        stackHints(L);
        for(int kind:Power.OFFERED) {
            Mem store=new Mem();GameCore c=fresh(L,store);Ear ear=new Ear();c.sound=ear;
            GameCore.Enemy e=add(c,L,new int[]{0},L.playTop+L.enemyR*4);
            TestPower.place(c,L,kind,0);c.tapPower(c.power.x,c.power.y,L);
            float left=c.modeLeft,clock=c.clock,y=e.y;int score=c.score;
            int message=Onboarding.powerSpeech(kind);
            c.update(DT,L);
            check("power pickup opens its own live lesson "+kind,c.onboarding.powerGuide && c.onboarding.briefing
                    && c.onboarding.speech==message && c.onboarding.practice==null && ear.explanations==1);
            c.update(.8f,L);
            check("power explanation freezes timer and enemies "+kind,c.modeLeft==left && c.clock==clock
                    && e.y==y && c.score==score && c.onboarding.companionTravel==1);
            TestOnboarding.touch(c,L,0,c.keyX(L,0),c.keyY(L,0));
            TestOnboarding.touch(c,L,1,c.keyX(L,0),c.keyY(L,0));
            check("power explanation consumes gameplay touches "+kind,c.onboarding.briefing && e.pos==0);
            Pause.open(c);c.update(1,L);Pause.resume(c);
            check("background pause preserves power explanation "+kind,c.onboarding.briefing && c.modeLeft==left);
            TestOnboarding.acknowledge(c,L);c.update(DT,L);
            check("acknowledgement resumes but does not learn power "+kind,c.modeLeft<left && c.onboarding.powerGuide
                    && !c.onboarding.learned(message) && store.powerTutorials==0);
            use(c,L,kind);c.update(DT,L);
            check("successful mechanic retires power guide "+kind,c.onboarding.learned(message) && !c.onboarding.powerGuide
                    && c.onboarding.companionTravel<1 && c.onboarding.companionTravel>0);
            GameCore loaded=new GameCore(store,115);
            check("power success survives restart independently "+kind,loaded.onboarding.learned(message)
                    && store.powerTutorials==(1<<(message-TutorialSpeech.POWER_FLURRY)));
            c.startFrenzy(kind,L);c.update(DT,L);
            check("learned power does not interrupt later pickups "+kind,!c.onboarding.briefing);
            c.onboarding.reset(c);
            check("reset tutorials also clears power steps without clearing collection "+kind,store.powerTutorials==0
                    && store.collected==1 && !new GameCore(store,115).onboarding.learned(message));

            c=fresh(L,new Mem());c.startFrenzy(kind,L);use(c,L,kind);c.update(DT,L);
            check("success before popup skips power explanation "+kind,c.onboarding.learned(message) && !c.onboarding.briefing);
            c=fresh(L,new Mem());c.startFrenzy(kind,L);
            if(kind==Power.FLING) { c.beginStroke(L.w*.5f,L.playTop);c.sliceTo(L.w*.7f,L.playTop,L);c.endStroke(); }
            else c.tapKey(0,L);
            check("empty action does not learn power "+kind,!c.onboarding.learned(message));
            c.update(DT,L);TestOnboarding.acknowledge(c,L);c.modeLeft=0;c.update(DT,L);
            check("expired power clears reminder without learning it "+kind,!c.onboarding.powerGuide && !c.onboarding.learned(message));
            c.startFrenzy(kind,L);c.update(DT,L);
            check("unlearned expired power can be explained next time "+kind,c.onboarding.briefing);
            c.lives=1;c.takeHit(L.w*.5f,L);c.update(DT,L);
            check("death clears all power guidance "+kind,c.state==GameCore.OVER && !c.onboarding.powerGuide
                    && !c.onboarding.briefing && c.onboarding.companionTravel==0);
        }
        for(int kind:new int[]{Power.INCOGNITO,Power.MONOCHROME,Power.MULTI}) {
            GameCore c=fresh(L,new Mem());
            if(kind==Power.MULTI)c.startFrenzy(kind,L);else c.startDebuff(kind);
            c.update(DT,L);
            check("debuffs and retired powers do not create tutorials "+kind,!c.onboarding.briefing && !c.onboarding.powerGuide);
        }
        GameCore c=fresh(L,new Mem());c.onboarding.skip(c);
        c.startFrenzy(Power.FLURRY,L);c.update(DT,L);
        check("Skip All suppresses power guidance",!c.onboarding.briefing);
        c=fresh(L,new Mem());c.startFrenzy(Power.FLURRY,L);c.update(DT,L);
        c.startFrenzy(Power.FLING,L);c.update(DT,L);
        check("replaced power cannot leave stale explanation",c.onboarding.briefing && c.onboarding.speech==TutorialSpeech.POWER_FLING);

        Mem store=new Mem();c=fresh(L,store);
        c.onboarding.learn(c,TutorialSpeech.WAIT);int oldFlags=store.tutorials;
        for(int kind:Power.OFFERED)c.onboarding.learn(c,Onboarding.powerSpeech(kind));
        check("power flags cannot overflow into older lesson bits",store.tutorials==oldFlags && store.powerTutorials==7);
        for(int kind:Power.OFFERED) {
            c=fresh(L,new Mem());Power p=TestPower.place(c,L,kind,0);
            p.mystery=p.hit=true;p.hitT=Power.SELECT_TIME-DT*.5f;
            c.update(DT,L);c.update(DT,L);
            check("mystery reveal introduces its actual power "+kind,c.onboarding.briefing
                    && c.onboarding.speech==Onboarding.powerSpeech(kind) && c.modeLeft==Power.DURATION);
        }
    }
    private static void stackHints(Layout L) {
        for(int kind:new int[]{Power.FLING,Power.TEAM})for(int acknowledged=0;acknowledged<2;acknowledged++) {
            Mem store=new Mem();GameCore c=fresh(L,store);c.stage=2;
            c.onboarding.learn(c,Onboarding.powerSpeech(kind));
            Ear ear=new Ear();c.sound=ear;
            GameCore.Enemy e=add(c,L,new int[]{0},new int[]{2},L.playTop+L.enemyR*4);
            c.update(DT,L);
            check("stack lesson starts before bypass power "+kind+"/"+acknowledged,c.onboarding.hintKind==Onboarding.STACK_HINT);
            if(acknowledged==1)TestOnboarding.acknowledge(c,L);
            else TestOnboarding.touch(c,L,0,L.w*.5f,TutorialSpeech.buttonY(L));
            int hushes=ear.hushes;
            c.startFrenzy(kind,L);c.update(DT,L);
            check("bypass power dismisses stack modal or reminder "+kind+"/"+acknowledged,
                    c.onboarding.hintKind==0 && !c.onboarding.briefing && (acknowledged==1 || ear.hushes>hushes));
            if(acknowledged==0) {
                check("dismissed stack popup retains pending gesture ownership "+kind,c.onboarding.ownsTouch);
                TestOnboarding.touch(c,L,1,c.keyX(L,0),c.keyY(L,0));
                check("pending release cannot act on underlying enemy "+kind,e.done==0 && !c.onboarding.ownsTouch);
            }
            c.update(DT,L);
            check("stack lesson stays suppressed and unlearned during power "+kind+"/"+acknowledged,
                    !c.onboarding.briefing && c.onboarding.hintKind==0 && c.onboarding.eligible(Onboarding.STACK_HINT)
                    && (store.tutorials&Onboarding.STACK_HINT)==0);
            c.modeLeft=0;c.mode=-1;c.buddy.leave();c.update(DT,L);
            check("stack lesson remains available after power ends "+kind+"/"+acknowledged,
                    c.onboarding.briefing && c.onboarding.hintKind==Onboarding.STACK_HINT);
        }
        GameCore c=fresh(L,new Mem());c.stage=2;
        add(c,L,new int[]{0},new int[]{2},L.playTop+L.enemyR*4);c.update(DT,L);
        c.startFrenzy(Power.FLURRY,L);c.update(DT,L);
        check("FLURRY retains an existing repeated-tap explanation",c.onboarding.briefing
                && c.onboarding.hintKind==Onboarding.STACK_HINT);
        c.startFrenzy(Power.FLING,L);c.update(DT,L);
        check("unlearned bypass power still gets its own explanation",c.onboarding.hintKind==0
                && c.onboarding.briefing && c.onboarding.speech==TutorialSpeech.POWER_FLING);
    }
}
