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
        GameCore c=title();Ear ear=new Ear();c.sound=ear;
        check("title defaults to Adventure",c.modes.adventure() && c.modes.visible(c) && c.modes.playable(c));
        long collection=c.collected;int best=c.best,land=c.landChoice;
        check("Adventure shows its lands",LandPicker.visible(c));
        tap(c,L,1);
        check("text arrow selects Survival with sound",c.modes.selected==ModeSelector.SURVIVAL && ear.uiBloops==1);
        check("other modes immediately stop land input",!LandPicker.visible(c) && !LandPicker.down(c,L,L.w*.5f,LandPicker.cardY(L)));
        c.update(ModeSelector.CHANGE*.5f,L);
        check("mode change animates land departure",c.modes.transition>0 && c.modes.transition<1
                && c.modes.adventureFade>0 && c.modes.adventureFade<1);
        c.update(ModeSelector.CHANGE,L);
        check("land departure finishes",c.modes.adventureFade==0);
        check("Adventure records cannot be opened as Survival records",!HighScoreScreen.entryHit(c,L,L.w*.5f,L.h*.292f));
        c.highScoreScreen.show(c);
        check("record entry cannot bypass the mode guard",!c.highScoreScreen.open);
        tap(c,L,0);c.screenKey(0);c.beginStart();c.startGame();
        check("unfinished mode cannot confirm or start Adventure",!c.starting() && c.state==GameCore.TITLE
                && c.modes.unavailable>0 && ear.collects==0 && ear.starts==0);
        c.openCase();c.update(.5f,L);
        check("display case stays shared and covers mode selector",c.caseOpen && !c.modes.visible(c));
        c.closeCase();c.update(.5f,L);
        check("closing case restores the browsed mode",c.modes.selected==ModeSelector.SURVIVAL && c.modes.visible(c));
        tap(c,L,1);c.update(ModeSelector.CHANGE,L);
        check("Time Attack also hides lands",c.modes.selected==ModeSelector.TIME_ATTACK && !LandPicker.visible(c));
        check("browsing preserves Adventure progress",c.landChoice==land && c.best==best && c.collected==collection);
        c.toTitle();
        check("returning to title retains the mode",c.modes.selected==ModeSelector.TIME_ATTACK);
        check("Back restores Adventure",Pause.back(c) && c.modes.adventure());
        c.update(ModeSelector.CHANGE,L);tap(c,L,0);
        check("confirmation has its own chime and pulse",ear.collects==1 && c.modes.confirmation==1);
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
        check("boss unlocks do not expose unfinished gameplay",!ModeSelector.implemented(ModeSelector.SURVIVAL)
                && !ModeSelector.implemented(ModeSelector.TIME_ATTACK));

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
                    ModeSelector.y(phone)+phone.unit*1.1f<LandPicker.cardY(phone)-phone.h*.05f
                    && !Showcase.inIcon(phone,c.clock,phone.w*.5f,ModeSelector.y(phone)));
        }
    }
}
