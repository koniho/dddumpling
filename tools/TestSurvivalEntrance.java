package com.dddumpling.game;

final class TestSurvivalEntrance extends Check {
    static GameCore title() {
        GameCore c=new GameCore(TestSurvival.store(),236,true);
        c.landSeen=LandPicker.stateMask();c.caseIndex=0;c.modes.selected=ModeSelector.SURVIVAL;
        return c;
    }
    static void toAge(GameCore c,Layout L,float age) {
        for(int i=0;i<1000 && c.state==GameCore.TITLE && Launch.elapsed(c)<age;i++) {
            float dt=Math.min(1f/60,age-Launch.elapsed(c));c.update(dt,dt,L);
        }
    }
    static void all(Layout L) {
        group("Survival title entrance");
        GameCore c=title();Ear ear=new Ear();c.sound=ear;
        check("selecting Survival does not launch its entrance",!SurvivalEntrance.active(c) && ear.survivalAnnouncements==0);
        c.beginStart();float firstY=SurvivalEntrance.baseline(c,L);
        check("launch takes over the selector at its existing position",firstY==ModeSelector.y(L));
        toAge(c,L,SurvivalEntrance.RISE*.5f);
        float risingY=SurvivalEntrance.baseline(c,L);
        check("selector title rises during companion greeting",risingY<firstY && SurvivalEntrance.progress(c)==0);
        toAge(c,L,SurvivalEntrance.RISE);
        float topY=SurvivalEntrance.baseline(c,L);
        check("selector title arrives near top before rainbow",topY<risingY && topY<L.h*.2f);
        check("Survival adds a rainbow sweep after the companion",SurvivalEntrance.active(c)
                && Math.abs(Launch.duration(c)-Launch.TIME-SurvivalEntrance.SWEEP)<.001f);
        toAge(c,L,Launch.TIME-.03f);
        check("companion name precedes rainbow announcement",ear.nameCalls==1 && ear.survivalAnnouncements==0
                && SurvivalEntrance.progress(c)==0 && c.survival.seconds==0 && c.state==GameCore.TITLE);
        check("title holds near top through companion send-off",SurvivalEntrance.baseline(c,L)==topY);
        toAge(c,L,Launch.TIME+.03f);
        check("rainbow onset announces Survival exactly once",ear.survivalAnnouncements==1
                && SurvivalEntrance.progress(c)>0 && c.state==GameCore.TITLE && c.enemies.isEmpty());
        float remaining=c.launchT,entry=SurvivalEntrance.progress(c);
        c.paused=true;c.update(.5f,.5f,L);c.paused=false;
        PlayerSettings.open(c);c.update(.5f,.5f,L);c.closeSettings();
        check("paused and settings time hold the entrance",c.launchT==remaining && SurvivalEntrance.progress(c)==entry);
        toAge(c,L,Launch.TIME+SurvivalEntrance.SWEEP*.65f);
        check("resume cannot replay speech or grant survival time",ear.survivalAnnouncements==1
                && c.survival.seconds==0 && !c.survival.active && c.enemies.isEmpty());
        float previous=topY;
        for(float t:new float[]{.1f,.3f,.5f,.75f,.95f}) {
            c.launchT=SurvivalEntrance.SWEEP*(1-t);
            float y=SurvivalEntrance.baseline(c,L),edge=L.h*1.15f*SurvivalEntrance.progress(c);
            check("title travels down just ahead of rainbow "+t,y>previous && y-SurvivalEntrance.height(L)>edge);
            previous=y;
        }
        check("title fully clears bottom before handoff",SurvivalEntrance.baseline(c,L)-SurvivalEntrance.height(L)>L.h);
        c.cloudUi.reduceMotion=true;
        check("reduced motion holds title position while fading",SurvivalEntrance.baseline(c,L)==topY);
        c.cloudUi.reduceMotion=false;
        c.update(.2f,.2f,L);
        check("combat begins with entrance complete and clock zero",c.state==GameCore.PLAY && c.survival.active
                && c.survival.seconds==0 && c.survival.entrance()==1 && !SurvivalEntrance.active(c));
        c.update(.01f,.1f,L);
        check("only active combat advances Survival",Math.abs(c.survival.seconds-.1)<.001 && ear.survivalAnnouncements==1);
        c.toTitle();c.returnFade=0;c.beginStart();toAge(c,L,Launch.TIME+.03f);
        check("another title start announces anew",ear.survivalAnnouncements==2);
        int hushes=ear.hushes;c.cancelStart();c.update(4,4,L);
        check("cancellation clears entrance and speech",!c.starting() && !SurvivalEntrance.active(c)
                && ear.hushes>hushes && ear.survivalAnnouncements==2);
        c.beginStart();toAge(c,L,Launch.TIME+.03f);
        check("cancelled entrance can start fresh",ear.survivalAnnouncements==3);
        c.cancelStart();c.modes.selected=ModeSelector.ADVENTURE;c.beginStart();
        check("Adventure keeps its original send-off",Launch.duration(c)==Launch.TIME && !SurvivalEntrance.active(c));
        toAge(c,L,Launch.TIME+.05f);
        check("Adventure has no Survival announcement",ear.survivalAnnouncements==3 && c.state==GameCore.PLAY);
        for(int fps:new int[]{30,60,120}) {
            GameCore run=title();Ear sound=new Ear();run.sound=sound;run.beginStart();
            int frames=0;
            while(run.state==GameCore.TITLE && frames++<fps*5)run.update(1f/fps,1f/fps,L);
            check("entrance timing and sound are frame-rate independent "+fps,run.state==GameCore.PLAY
                    && Math.abs(frames/(float)fps-Launch.duration(run))<.04f && sound.survivalAnnouncements==1
                    && sound.nameCalls==1 && run.survival.seconds==0 && run.survival.entrance()==1);
        }
        GameCore skipped=title();Ear skippedSound=new Ear();skipped.sound=skippedSound;skipped.beginStart();
        skipped.update(5,5,L);
        check("skipped entire entrance does not queue stale mode speech",skipped.state==GameCore.PLAY
                && skippedSound.survivalAnnouncements==0 && skipped.survival.seconds==0);
    }
}
