package com.dddumpling.game;

final class TestSteamerCompanion extends Check {
    static GameCore start(Layout L) {
        GameCore c=new GameCore(new Mem(),241L);c.collected=Collect.MASK;c.caseIndex=8;c.startGame();
        c.kidsRun=true;Interlude.enterBonus(c,L);return c;
    }
    static void pair(GameCore c) {c.tapBonus(c.steamer.wanted());c.tapBonus(c.steamer.wanted());}
    static void all(Layout L) {
        group("Steamer companion fan");
        GameCore c=start(L);SteamerCompanion fan=c.companion.steamer;
        check("Steamer helper starts at home",SteamerCompanion.x(c,L)==RunCompanion.x(L)
                && SteamerCompanion.y(c,L)==RunCompanion.y(L));
        c.time=SteamerCompanion.TRAVEL*.5f;
        check("helper travels above home toward basket",SteamerCompanion.y(c,L)<RunCompanion.y(L)
                && SteamerCompanion.radius(c,L)>RunCompanion.radius(c,L));
        c.time=SteamerCompanion.TRAVEL;
        check("working helper matches Star Path size",Math.abs(SteamerCompanion.radius(c,L)-StarPath.flyerR(L))<.001f);
        check("helper sits under basket clear of the pip meter",SteamerCompanion.y(c,L)>Screens.steamerBaseY(L)
                && SteamerCompanion.y(c,L)+StarPath.flyerR(L)<Screens.steamerProgressY(L));
        check("helper sits left of countdown with airflow space",SteamerCompanion.x(c,L)==L.w*.20f
                && SteamerCompanion.y(c,L)-Screens.steamerBaseY(L)>StarPath.flyerR(L)*3);
        fan.update(c,.2f);
        check("idle fan keeps a slow motion",fan.phase>0 && fan.speed()==SteamerCompanion.IDLE_SPEED);
        check("idle has a narrow stroke and no gusts",fan.sweep()<.15f && fan.gust==0);
        pair(c);check("shuffling cannot accelerate fan",fan.effort==0);
        c.bonusTimer=c.bonusRollEnd;
        int wrong=0;while(wrong==c.steamer.leftKey || wrong==c.steamer.rightKey)wrong++;
        c.tapBonus(wrong);check("wrong key cannot accelerate fan",fan.effort==0);
        c.tapBonus(c.steamer.wanted());check("half pair cannot accelerate fan",fan.effort==0);
        c.tapBonus(c.steamer.wanted());check("scored pip accelerates fan",c.steamer.hits==1 && fan.effort>0);
        check("scored pip widens fan and emits gusts",fan.sweep()>.8f && fan.gust==1);
        float gust=fan.gust;
        float phase=fan.phase,effort=fan.effort;c.paused=true;advance(c,L,.3f);c.paused=false;
        check("pause freezes fan and effort",fan.phase==phase && fan.effort==effort && fan.gust==gust);
        fan.update(c,.7f);
        check("stopping scoring quickly removes gusts and narrows stroke",fan.gust==0 && fan.sweep()<.15f);
        GameCore fast=start(L),slow=start(L);fast.bonusTimer=fast.bonusRollEnd;slow.bonusTimer=slow.bonusRollEnd;
        for(int i=0;i<3;i++) {pair(fast);advance(fast,L,.1f);pair(slow);advance(slow,L,.6f);}
        check("faster scored-pip cadence drives faster fanning",fast.companion.steamer.speed()>slow.companion.steamer.speed());
        advance(fast,L,3f);check("no new pips returns fan to idle",fast.companion.steamer.speed()==SteamerCompanion.IDLE_SPEED);
        c.steamer.hits=c.steamer.goal();c.steamer.swipeReady=true;effort=fan.effort;
        c.tapBonus(c.steamer.wanted());check("lid reminders do not accelerate fan",fan.effort==effort);
        c.swipeBonus();c.steamer.freedT=Steamer.FREE_TIME-SteamerCompanion.RETURN;
        check("reward sends helper home",SteamerCompanion.travel(c)==0);
        c.toTitle();check("leaving clears fan state",fan.phase==0 && fan.effort==0 && fan.gust==0);
        c=start(L);c.cloudUi.reduceMotion=true;
        check("reduced motion places helper without travel",SteamerCompanion.travel(c)==1);
        c.onboarding.begin(c,Onboarding.STEAMER,L);
        check("tutorial inherits reduced motion",c.onboarding.practice.cloudUi.reduceMotion);
        c.onboarding.practice.companion.steamer.effort=1;c.onboarding.sceneWait=0;
        c.onboarding.briefing=true;c.onboarding.speech=TutorialSpeech.LIFT;
        advance(c,L,3f);
        check("tutorial explanation lets unused fanning return to idle",c.onboarding.practice.companion.steamer.effort==0);
        c=start(L);c.time=2;c.bonusTimer=GameCore.MASH_END-SteamerCompanion.RETURN;
        check("time-up returns helper before report",SteamerCompanion.travel(c)==0);
        c.starBonus=true;check("Star Path does not show Steamer helper",!SteamerCompanion.scene(c));
    }
}
