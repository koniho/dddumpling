package com.dddumpling.game;

final class TestSurvival extends Check {
    static Mem store() {
        Mem m=new Mem();m.collected=1L|(1L<<Collect.BOSS_FIRST);m.tutorials|=Onboarding.SKIPPED;
        m.best=789;return m;
    }
    static GameCore start(Mem store,long seed) {
        GameCore c=new GameCore(store,seed,true);c.landSeen=LandPicker.stateMask();
        c.landChoice=1;c.fullRoster=false;c.modes.selected=ModeSelector.SURVIVAL;
        c.startGame();return c;
    }
    static void all(Layout L) {
        Mem m=store();GameCore c=start(m,149);
        check("Survival starts from normal selector",c.state==GameCore.PLAY && c.survival.active && c.stage==1);
        check("Survival preserves Adventure land choice",c.landChoice==1 && c.runStartLand==0 && c.best==0);
        long prizes=c.collected;String history=m.highScores;
        c.update(.01f,.25f,L);double before=c.survival.seconds;
        check("Survival counts active elapsed time",Math.abs(before-.25)<.001);
        c.slowdown=1;c.update(.01f,.25f,L);
        check("slow motion does not slow the record clock",Math.abs(c.survival.seconds-.5)<.001);
        Pause.open(c);before=c.survival.seconds;c.update(1,1,L);
        check("pause excludes elapsed time",c.survival.seconds==before);
        Pause.resume(c);PlayerSettings.open(c);c.update(1,1,L);
        check("settings exclude elapsed time",c.survival.seconds==before);c.closeSettings();
        c.spawnedThisStage=c.stageQuota();c.enemies.clear();c.shots.clear();
        check("clearing Adventure quota cannot end Survival",!c.stageCleared());
        c.spawnTimer=0;c.update(DT,L);
        check("Survival spawns beyond the ordinary quota",c.liveEnemies()>0 && !c.pendingBonus && !c.boss.active());
        GameCore.Enemy e=c.enemies.get(0);c.spawnTimer=10;
        c.destroyWord(e,e.baseX,e.y,L);
        check("ordinary clear requests immediate replacement",c.spawnTimer<=.18f);
        c.startFrenzy(Power.NINJA,L);c.modeLeft=.001f;c.update(DT,L);
        check("ending power keeps Survival continuous",!c.powerActive() && !c.stageByPower && !c.pendingBonus && !c.stageCleared());
        c.pushUsed=true;c.survival.rescueLeft=29.9f;c.update(.01f,.2f,L);
        check("rescue recharges without an interlude",!c.pushUsed && c.survival.rescueLeft==0);
        int calm=c.survival.background();float travel=c.travelSeconds(),spawn=c.spawnInterval();
        c.survival.seconds=300;c.survival.update(c,.01f);
        check("difficulty and distinct sky intensify together",c.travelSeconds()<travel && c.spawnInterval()<spawn
                && c.survival.background()!=calm && c.maxEnemies()==5 && c.stage==16);
        c.survival.seconds=1000;c.survival.update(c,.01f);
        check("difficulty has a bounded ceiling",c.stage==19 && c.travelSeconds()==8f && c.maxEnemies()==5);
        c.score=1234;c.lives=1;c.takeHit(L.w*.5f,L);
        check("death saves only Survival records",c.state==GameCore.OVER && c.survival.finished
                && m.best==789 && m.highScores.equals(history) && c.collected==prizes
                && c.progress.maximum("highest_stage")==0 && c.townRunTickets==0);
        int profile=c.survival.profile;long record=c.survival.bestTime[profile];before=c.survival.seconds;
        c.update(10,10,L);
        check("death animation cannot increase time",c.survival.seconds==before && c.survival.bestTime[profile]==record);
        GameCore loaded=new GameCore(m,150,true);
        check("Survival records persist separately",loaded.survival.bestTime[profile]==record && loaded.survival.bestScore[profile]==1234);
        c.survival.resultTap(c,L,L.w*.5f,Survival.resultY(L,true));
        check("retry starts the regular launch with Survival selected",c.starting() && !c.survival.active && c.modes.selected==ModeSelector.SURVIVAL);
        c.startGame();check("retry resets time and transient state",c.survival.active && c.survival.seconds==0 && !c.pushUsed && c.stage==1);
        c.toTitle();check("return restores Adventure progress and keeps mode choice",c.landChoice==1
                && c.best==c.landBests[1] && !c.survival.active && c.modes.selected==ModeSelector.SURVIVAL);
        c.modes.back(c);c.startGame();check("Adventure still starts at its selected land",!c.survival.active && c.stage==6);
        records(L);effects(L);bounded(L);fuzz(L);
    }
    private static void effects(Layout L) {
        GameCore calm=start(store(),155),vivid=start(store(),155);
        vivid.survival.seconds=Survival.RAMP_SECONDS;
        GameCore.Enemy a=add(calm,L,new int[]{0,1,2},L.h*.4f);
        GameCore.Enemy b=add(vivid,L,new int[]{0,1,2},L.h*.4f);
        calm.destroyWord(a,a.baseX,a.y,L);vivid.destroyWord(b,b.baseX,b.y,L);
        check("maximum Survival clears emit ten times the particles",vivid.particles.size()==calm.particles.size()*10);
        check("explosion intensity cannot alter gameplay RNG",vivid.rnd.nextLong()==calm.rnd.nextLong());
        for(int i=0;i<60;i++)vivid.survival.wordBurst(vivid,L.w*.5f,L.h*.5f,L.enemyR,20,Draw.GOLD);
        check("simultaneous clears have a bounded particle budget",vivid.particles.size()<=4200);
        Fx.updateParticles(vivid,1f);
        check("large clear effects expire normally",vivid.particles.isEmpty());
    }
    private static void records(Layout L) {
        Mem m=store();GameCore c=start(m,152);
        c.survival.seconds=12.345;c.score=500;Pause.open(c);Pause.action(c,2);Pause.action(c,2);
        check("ending early saves time and returns cleanly",c.state==GameCore.TITLE && !c.survival.active && c.survival.bestTime[0]==12345);
        c.preferences.kids=true;c.startGame();c.survival.seconds=30;c.score=800;c.highScores.finish(c);c.toTitle();
        c.preferences.kids=false;c.fullRoster=true;c.startGame();c.survival.seconds=20;c.score=600;c.highScores.finish(c);c.toTitle();
        check("Kids and both normal decks have independent records",c.survival.bestTime[0]==12345
                && c.survival.bestTime[1]==20000 && c.survival.bestTime[2]==30000);
        check("time format is stable",Survival.time(65000).equals("1:05"));
        Survival saved=new Survival();saved.load(c.survival.encode());
        check("all record profiles round trip",saved.encode().equals(c.survival.encode()));
        for(String bad:new String[]{"","future","1;1,2;3,4;-1,8","1;1,2;3,4;5,no"}) {
            saved.load(bad);check("bad Survival record stays empty",saved.bestTime[0]==0 && saved.bestScore[2]==0);
        }
        c.resetHighScores();GameCore reset=new GameCore(m,153,true);
        check("score reset also clears Survival records",reset.survival.bestTime[0]==0 && reset.survival.bestScore[2]==0);
    }
    private static void bounded(Layout L) {
        for(float presses:new float[]{2.5f,5f,8f}) {
            float total=0;int deaths=0;
            for(int seed=0;seed<3;seed++) {
                GameCore c=start(store(),1490+seed);
                Bot bot=new Bot(presses,.18f,.02f,true,1490+seed);
                int frame=0;
                while(c.state==GameCore.PLAY && frame++<60*360) {
                    c.update(DT,L);bot.step(c,L,DT);
                    if(c.state==GameCore.BONUS || c.boss.active() || c.cave.running)break;
                }
                check("bounded Survival has no boss or minigame",c.state!=GameCore.BONUS && !c.boss.active() && !c.cave.running);
                check("bounded Survival keeps rewards isolated",c.collected==store().collected && c.progress.maximum("highest_stage")==0);
                total+=c.survival.seconds;if(c.state==GameCore.OVER)deaths++;
            }
            System.out.printf("    Survival %.1f presses/s: mean %.1fs, %d/3 deaths%n",presses,total/3,deaths);
            check("bounded player gets a playable opening",total/3>15);
        }
    }
    private static void fuzz(Layout L) {
        GameCore c=start(store(),154);java.util.Random random=new java.util.Random(154);
        int deaths=0;boolean sane=true;
        for(int i=0;i<60*180;i++) {
            c.update(DT,L);
            if(c.state==GameCore.OVER) {c.toTitle();c.startGame();deaths++;}
            if(random.nextInt(5)==0)c.tapKey(random.nextInt(Glyph.COUNT),L);
            if(random.nextInt(800)==0) {Pause.open(c);c.update(.1f,L);Pause.resume(c);}
            sane &= c.state==GameCore.PLAY || c.state==GameCore.OVER;
            sane &= !c.boss.active() && !c.pendingBonus && !Double.isNaN(c.survival.seconds);
        }
        check("Survival fuzz survives repeated deaths and pauses",sane && deaths>0);
    }
}
