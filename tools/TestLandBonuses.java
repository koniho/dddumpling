package com.dddumpling.game;

final class TestLandBonuses extends Check {
    static GameCore start(Layout L,int stage,boolean force,long seed) {
        GameCore c=new GameCore(new Mem(),seed);
        c.startGame();c.onboarding.clear();c.onboarding.saved=c.onboarding.savedPowers=-1;
        c.devLandBonuses=force;c.enterStage(stage);c.stageBanner=c.stageGap=c.startFade=0;
        c.landBlend=1;c.spawnTimer=999;c.time=2;c.landBonus.update(c,1);
        return c;
    }
    static void all(Layout L) {
        group("land scenery bonuses");
        int seen=0;
        for(int seed=0;seed<40;seed++) {
            GameCore c=start(L,1,false,seed);
            for(int chapter=0;chapter<6;chapter++) {
                int offered=0,choice=-1;
                for(int offset=0;offset<4;offset++) {
                    c.enterStage(chapter*5+offset+1);c.landBonus.update(c,1);
                    if(offset==0)choice=c.landBonus.selectedStage;
                    check("one stable choice per land",c.landBonus.selectedStage==choice);
                    if(c.landBonus.visible(c)) {offered++;seen|=1<<offset;}
                }
                check("exactly one regular stage offers the bonus",offered==1);
                c.enterStage(chapter*5+5);c.landBonus.update(c,1);
                check("boss stage never offers scenery reward",!c.landBonus.visible(c));
            }
        }
        check("all four regular stages can be selected",seen==15);
        GameCore c=start(L,1,true,244);
        Ear sound=new Ear();c.sound=sound;
        int score=c.score;
        float x=LandBonus.x(c,L),y=LandBonus.y(L);
        check("forced bonus appears immediately in current stage",c.landBonus.visible(c));
        check("target stays clear of keys",L.keyAt(x,y)<0 && y+LandBonus.radius(L)*1.18f<L.deckTop);
        check("one tap grants points",c.landBonus.tap(c,L,x,y) && c.score==score+LandBonus.POINTS);
        check("repeated tap cannot pay twice",!c.landBonus.tap(c,L,x,y) && c.score==score+LandBonus.POINTS);
        check("one cheerful chime per reward",sound.collects==1);
        c.devLandBonuses=false;c.devLandBonuses=true;
        check("toggling override cannot replay reward",!c.landBonus.offered(c));
        c.enterStage(2);c.landBonus.update(c,1);
        check("override offers next regular stage",c.landBonus.visible(c));
        c.enterStage(1);c.landBonus.update(c,1);
        check("returning to collected stage does not pay again",!c.landBonus.offered(c));
        c.enterStage(5);c.landBonus.update(c,1);
        check("override never enables boss-stage bonus",!c.landBonus.visible(c));
        c.enterStage(6);c.landBonus.update(c,1);
        check("next land offers a fresh reward",c.landBonus.visible(c));
        c.paused=true;float age=c.landBonus.age;int choice=c.landBonus.selectedStage;
        c.update(1,L);
        check("pause preserves choice and clocks",c.landBonus.age==age && c.landBonus.selectedStage==choice);
        check("paused touch cannot collect",!c.landBonus.tap(c,L,LandBonus.x(c,L),y));
        c.paused=false;c.settingsOpen=true;c.update(1,L);
        check("settings preserve clocks and block collection",c.landBonus.age==age && !c.landBonus.tap(c,L,LandBonus.x(c,L),y));
        c.settingsOpen=false;c.landBonus.tap(c,L,LandBonus.x(c,L),y);
        c.state=GameCore.BONUS;c.update(0,L);
        check("interlude clears collection effect",c.landBonus.burst<0);
        c.toTitle();check("title clears owned stage",c.landBonus.stage<0 && c.landBonus.burst<0);
        for(int mode:new int[]{ModeSelector.SURVIVAL,ModeSelector.TIME_ATTACK}) {
            c=start(L,1,true,244);c.modes.restore(mode);
            check("override cannot leak into other modes",!c.landBonus.visible(c));
        }
        c=start(L,1,true,244);c.onboarding.teacher=new GameCore(null,1);
        check("practice scenes do not offer bonus",!c.landBonus.visible(c));
        c=start(L,1,true,244);c.landBonus.tap(c,L,LandBonus.x(c,L),y);c.lives=1;c.takeHit(L.w*.5f,L);
        check("death clears reward effects",c.state==GameCore.OVER && c.landBonus.stage<0 && c.landBonus.burst<0);
        GameCore a=start(L,1,true,299),b=start(L,1,true,299);
        a.landBonus.enter(a);a.landBonus.tap(a,L,LandBonus.x(a,L),y);
        check("selection and collection preserve combat RNG",a.rnd.nextLong()==b.rnd.nextLong());
        SettingsUi ui=new SettingsUi();ui.compute(L,SettingsUi.GENERAL);
        check("developer chip has its own target",ui.hit(L.w*.5f,ui.landBonusY+ui.testH*.5f)==SettingsUi.HIT_LAND_BONUSES);
        check("developer Run controls fit",ui.debuffY+ui.testH<ui.panelB);
    }
}
