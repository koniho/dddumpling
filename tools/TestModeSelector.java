package com.dddumpling.game;

final class TestModeSelector extends Check {
    static GameCore title() {
        Mem store=new Mem();store.collected=1L|(1L<<Collect.BOSS_FIRST);
        GameCore c=new GameCore(store,148);c.landSeen=LandPicker.stateMask();
        c.landChoice=1;c.best=c.landBests[1]=456;
        return c;
    }
    static void tap(GameCore c,Layout L,int direction) {
        float x=direction==0?L.w*.5f:ModeSelector.arrowX(L,direction),y=ModeSelector.y(L);
        c.modes.touch(c,L,0,12,x,y);c.modes.touch(c,L,1,12,x,y);
    }
    static void all(Layout L) {
        swipePreview(L);
        persistence(L);
        skit(L);
        GameCore c=title();Ear ear=new Ear();c.sound=ear;
        check("title defaults to Adventure",c.modes.adventure() && c.modes.visible(c) && c.modes.playable(c));
        long collection=c.collected;int best=c.best,land=c.landChoice;
        check("Adventure shows its lands",LandPicker.visible(c));
        check("enlarged label edges remain confirmation targets",c.modes.hit(c,L,L.w*.13f,ModeSelector.y(L))==2
                && c.modes.hit(c,L,L.w*.87f,ModeSelector.y(L))==2);
        tap(c,L,1);
        check("text arrow selects Survival with sound",c.modes.selected==ModeSelector.SURVIVAL && ear.uiBloops==1);
        check("other modes immediately stop land input",!LandPicker.visible(c) && !LandPicker.down(c,L,L.w*.5f,LandPicker.cardY(L)));
        c.update(ModeSelector.CHANGE*.5f,L);
        check("mode change animates land departure",c.modes.transition>0 && c.modes.transition<1
                && c.modes.adventureFade>0 && c.modes.adventureFade<1);
        c.update(ModeSelector.CHANGE,L);
        check("land departure finishes",c.modes.adventureFade==0);
        check("Survival has a high-score entry",HighScoreScreen.entryHit(c,L,L.w*.5f,L.h*.292f));
        check("Survival entry uses its own records",HighScoreScreen.records(c)==c.survival.titleHistory(c)
                && HighScoreScreen.records(c)!=c.highScores);
        tap(c,L,0);
        check("unlocked Survival confirms with a chime",c.modes.playable(c) && ear.collects==1);
        c.openCase();c.update(.5f,L);
        check("display case stays shared and covers mode selector",c.caseOpen && !c.modes.visible(c));
        c.closeCase();c.update(.5f,L);
        check("closing case restores the browsed mode",c.modes.selected==ModeSelector.SURVIVAL && c.modes.visible(c));
        tap(c,L,1);c.update(ModeSelector.CHANGE,L);
        check("Time Attack also hides lands",c.modes.selected==ModeSelector.TIME_ATTACK && !LandPicker.visible(c));
        c.timeAttack.selected=Boss.MUSHROOM;
        tap(c,L,0);c.screenKey(0);c.beginStart();c.startGame();
        check("locked Time Attack boss cannot start Adventure",!c.starting() && c.state==GameCore.TITLE
                && c.modes.unavailable>0 && ear.collects==1 && ear.starts==0);
        check("browsing preserves Adventure progress",c.landChoice==land && c.best==best && c.collected==collection);
        c.toTitle();
        check("returning to title retains the mode",c.modes.selected==ModeSelector.TIME_ATTACK);
        check("Back restores Adventure",Pause.back(c) && c.modes.adventure());
        c.update(ModeSelector.CHANGE,L);tap(c,L,0);
        check("confirmation has its own chime and pulse",ear.collects==2 && c.modes.confirmation==1);
        check("returning restores lands and original record",LandPicker.visible(c) && c.best==best && c.landChoice==land);
        c.screenKey(0);
        check("normal keys still launch Adventure",c.starting());

        GameCore locked=new GameCore(new Mem(),149);
        check("new modes require boss victories",!ModeSelector.unlocked(locked,ModeSelector.SURVIVAL)
                && !ModeSelector.unlocked(locked,ModeSelector.TIME_ATTACK));
        locked.collected=1L<<(Collect.BOSS_FIRST+Boss.OCTOPUS);
        check("each Time Attack boss keeps its own unlock",ModeSelector.unlocked(locked,ModeSelector.TIME_ATTACK)
                && !ModeSelector.unlocked(locked,ModeSelector.SURVIVAL)
                && ModeSelector.bossUnlocked(locked,Boss.OCTOPUS) && !ModeSelector.bossUnlocked(locked,Boss.SLIME)
                && !ModeSelector.allBossesUnlocked(locked));
        for(int boss=0;boss<Boss.COUNT;boss++)locked.collected|=1L<<(Collect.BOSS_FIRST+boss);
        check("boss rush requires all authored bosses",ModeSelector.allBossesUnlocked(locked));
        check("all shipped modes are implemented",ModeSelector.implemented(ModeSelector.SURVIVAL)
                && ModeSelector.implemented(ModeSelector.TIME_ATTACK));

        c=title();float x=ModeSelector.arrowX(L,1),y=ModeSelector.y(L);
        c.modes.touch(c,L,0,8,x,y);c.modes.touch(c,L,3,8,x,y);c.modes.touch(c,L,1,8,x,y);
        check("cancelled mode tap is inert",c.modes.adventure());
        c.modes.touch(c,L,0,8,x,y);c.modes.touch(c,L,5,9,x,y);c.modes.touch(c,L,1,8,x,y);
        check("second finger cancels mode tap",c.modes.adventure());
        c.modes.touch(c,L,0,8,L.w*.5f,y);
        c.modes.touch(c,L,2,8,L.w*.5f-L.unit*3,y);
        c.modes.touch(c,L,2,8,L.w*.1f,y);c.modes.touch(c,L,1,8,L.w*.1f,y);
        check("one swipe changes exactly one mode",c.modes.selected==ModeSelector.SURVIVAL);
        c.modes.touch(c,L,0,8,x,y);Pause.release(c);c.modes.touch(c,L,1,8,x,y);
        check("lifecycle release cancels selector ownership",c.modes.pointer==-1 && c.modes.selected==ModeSelector.SURVIVAL);
        c.onboarding.titleGuide=true;c.onboarding.speech=TutorialSpeech.LANDS;
        check("active tutorial owns title before mode selector",!c.modes.visible(c));
        int selected=c.modes.selected;tap(c,L,1);
        check("tutorial cannot be skipped by mode input",c.modes.selected==selected);

        Mem first=new Mem();first.tutorials=first.powerTutorials=0;
        GameCore fresh=new GameCore(first,151);
        check("first friend flow has no extra mode choice",!fresh.modes.visible(fresh));
        fresh.beginStart();
        check("initial key still opens first squishy selection",fresh.starter.open);
        for(int width:new int[]{320,393,640}) {
            Layout phone=new Layout();phone.compute(width,width*1.775f,0,0,0,0);
            c=title();
            check("mode touches clear land and case targets at "+width,
                    ModeSelector.y(phone)+phone.unit*1.75f<LandPicker.cardY(phone)-phone.h*.05f
                    && !Showcase.inIcon(phone,c.clock,phone.w*.5f,ModeSelector.y(phone)));
        }
    }
    private static void swipePreview(Layout L) {
        GameCore c=title();Ear ear=new Ear();c.sound=ear;
        float x=L.w*.5f,y=ModeSelector.y(L);
        c.modes.touch(c,L,0,1,x,y);c.modes.touch(c,L,2,1,x-L.w*.12f,y);
        check("partial mode swipe follows finger without selecting",c.modes.adventure()
                && Math.abs(c.modes.swipeOffset+.12f)<.001f && ear.uiBloops==0);
        c.modes.touch(c,L,1,1,x-L.w*.12f,y);
        check("short mode swipe does not tap or save",c.modes.adventure() && c.modes.confirmation==0
                && new GameCore((Mem)c.store,1483).modes.adventure() && c.modes.swipeOffset<0);
        c.modes.update(c,ModeSelector.SWIPE_RETURN*.5f);
        check("short swipe animates toward its starting position",c.modes.swipeOffset<0 && c.modes.swipeOffset>-.12f);
        c.modes.update(c,ModeSelector.SWIPE_RETURN);
        check("short swipe finishes centered",c.modes.swipeOffset==0 && c.modes.adventure());
        c.modes.touch(c,L,0,1,x,y);c.modes.touch(c,L,2,1,x-L.w*.25f,y);
        check("long mode swipe waits for release",c.modes.adventure());
        c.modes.touch(c,L,1,1,x-L.w*.25f,y);
        check("long mode swipe commits once with feedback",c.modes.selected==ModeSelector.SURVIVAL
                && ear.uiBloops==1 && c.modes.swipeOffset==0);
        c.modes.update(c,ModeSelector.CHANGE);
        c.modes.touch(c,L,0,1,x,y);c.modes.touch(c,L,2,1,x+L.w*.25f,y);
        c.modes.touch(c,L,2,1,x+L.w*.08f,y);c.modes.touch(c,L,1,1,x+L.w*.08f,y);
        check("pulling back below threshold cancels selection",c.modes.selected==ModeSelector.SURVIVAL && ear.uiBloops==1);
        c.modes.update(c,ModeSelector.SWIPE_RETURN);
        c.modes.touch(c,L,0,1,x,y);c.modes.touch(c,L,1,1,x+L.w*.25f,y);
        check("long right swipe returns to previous mode",c.modes.adventure() && ear.uiBloops==2);
        c.modes.restore(ModeSelector.SURVIVAL);c.clock=1;
        c.modes.touch(c,L,0,1,x,y);c.modes.touch(c,L,2,1,x-L.w*.12f,y);
        check("Survival stripes remain while dragging its title",stripeCaps(c,L)>0);
        c.modes.touch(c,L,1,1,x-L.w*.12f,y);c.modes.update(c,ModeSelector.SWIPE_RETURN*.5f);
        check("Survival stripes remain while title returns",stripeCaps(c,L)>0);
        c.modes.select(c,ModeSelector.ADVENTURE,-1);c.modes.update(c,ModeSelector.CHANGE*.3f);
        check("Survival stripes remain on departing title",stripeCaps(c,L)>0);
    }
    private static int stripeCaps(GameCore c,Layout L) {
        int[] count={0};
        Painter p=(Painter)java.lang.reflect.Proxy.newProxyInstance(Painter.class.getClassLoader(),
                new Class<?>[]{Painter.class},(proxy,method,args)->{
                    if(method.getName().equals("fillCircle")) {
                        float y=((Number)args[1]).floatValue(),r=((Number)args[2]).floatValue();
                        if(y<ModeSelector.y(L)-L.unit && y>ModeSelector.y(L)-L.unit*3
                                && Math.abs(r-L.unit*.19f)<.001f)count[0]++;
                    }
                    return null;
                });
        c.modes.draw(p,c,L);return count[0];
    }
    private static void persistence(Layout L) {
        GameCore c=title();Mem saved=(Mem)c.store;
        c.preferences.music=.35f;c.preferences.effectsMuted=true;c.preferences.kids=true;
        tap(c,L,1);
        GameCore reopened=new GameCore(saved,1481);
        check("mode selection survives app reopening without a confirmation tap",
                reopened.modes.selected==ModeSelector.SURVIVAL && reopened.modes.previous==ModeSelector.SURVIVAL
                && reopened.modes.transition==1 && reopened.modes.adventureFade==0);
        check("saving mode preserves audio and Kids preferences",reopened.preferences.music==.35f
                && reopened.preferences.effectsMuted && reopened.preferences.kids);
        reopened.preferences.music=.6f;reopened.preferences.save(reopened);
        check("saving another preference retains the selected mode",new GameCore(saved,1482).modes.selected==ModeSelector.SURVIVAL);
        c.startGame();c.lives=1;c.takeHit(L.w*.5f,L);c.update(10,10,L);c.dismissGameOver();
        c.update(GameCore.PARADE_TIME,GameCore.PARADE_TIME,L);c.toTitle();
        check("completed run retains the mode in memory and on reopen",c.modes.selected==ModeSelector.SURVIVAL
                && new GameCore(saved,1483).modes.selected==ModeSelector.SURVIVAL);
        c.modes.back(c);
        check("Back saves Adventure as the next launch mode",new GameCore(saved,1484).modes.adventure());
        c=title();saved=(Mem)c.store;c.modes.select(c,ModeSelector.TIME_ATTACK,1);
        reopened=new GameCore(saved,1485);
        check("unlocked Time Attack remains playable after reopening",
                reopened.modes.selected==ModeSelector.TIME_ATTACK && reopened.modes.playable(reopened));
        saved.playerSettings=PlayerSettings.DEFAULT;
        check("older preference saves default to Adventure",new GameCore(saved,1486).modes.adventure());
        saved.playerSettings=35 | (100<<7) | (3<<17);
        reopened=new GameCore(saved,1487);
        check("invalid saved mode falls back without losing volume",reopened.modes.adventure() && reopened.preferences.music==.35f);
    }
    private static void skit(Layout L) {
        GameCore c=title(),control=title();c.collected|=1L<<1;c.caseIndex=1;
        check("Survival skit uses the selected collected squishy",SurvivalDemo.companion(c)==1);
        c.caseIndex=Collect.COUNT-1;
        check("unowned case entries use an owned squishy for the skit",Collect.has(c.collected,SurvivalDemo.companion(c)));
        Painter sink=(Painter)java.lang.reflect.Proxy.newProxyInstance(Painter.class.getClassLoader(),
                new Class<?>[]{Painter.class},(proxy,method,args)->null);
        int settings=((Mem)c.store).playerSettings;
        for(int i=0;i<160;i++) {c.clock=i*.05f;SurvivalDemo.draw(sink,c,L,1f);}
        check("title skit leaves combat, saves and gameplay randomness untouched",c.state==GameCore.TITLE
                && c.enemies.isEmpty() && c.particles.isEmpty() && c.shots.isEmpty() && c.target==null
                && ((Mem)c.store).playerSettings==settings && c.rnd.nextLong()==control.rnd.nextLong());
    }
}
