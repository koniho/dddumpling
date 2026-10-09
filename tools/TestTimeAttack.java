package com.dddumpling.game;

final class TestTimeAttack extends Check {
    static Mem store() {
        Mem m=TestSurvival.store();m.collected=Collect.encode(Collect.MASK);return m;
    }
    static GameCore start(int choice) {
        GameCore c=new GameCore(store(),150,true);c.landSeen=LandPicker.stateMask();
        c.fullRoster=false;c.modes.restore(ModeSelector.TIME_ATTACK);c.timeAttack.selected=choice;c.startGame();return c;
    }
    static void beat(GameCore c,Layout L) {
        c.boss.beaten=true;c.boss.leaveT=0;
        BossPlay.endBoss(c,L);
    }
    static void all(Layout L) {
        for(int boss=0;boss<Boss.COUNT;boss++) {
            GameCore played=start(boss);
            Bot.Result run=new Bot(6f,.20f,.04f,false,150+boss).play(played,L,180);
            check("bounded player completes or loses real encounter "+boss,
                    played.state==GameCore.OVER && played.timeAttack.finished && played.timeAttack.seconds>0);
            check("only successful bounded clears set records "+boss,
                    (played.timeAttack.best[0][boss]>0)==played.timeAttack.won);
            System.out.println("    Time Attack "+Boss.NAMES[boss]+": "+TimeAttack.time(played.timeAttack.millis())
                    +(played.timeAttack.won?" clear":" loss")+", "+run.presses+" presses");
        }
        GameCore c=start(Boss.SLIME);Mem m=(Mem)c.store;
        check("Time Attack starts real selected boss",c.state==GameCore.PLAY && c.timeAttack.active
                && c.boss.kind==Boss.SLIME && c.stage==5 && c.lives==GameCore.START_LIVES);
        check("Time Attack skips wave, banner and minigame",c.enemies.isEmpty() && !c.pendingBonus
                && c.stageBanner==0 && !c.stageCleared() && !Cave.active(c));
        c.update(.01f,.25f,L);
        check("arrival card is not timed",c.timeAttack.seconds==0);
        c.boss.intro=0;c.update(.01f,.25f,L);
        check("playable combat uses real elapsed time",Math.abs(c.timeAttack.seconds-.25)<.001);
        Pause.open(c);c.update(1,1,L);Pause.resume(c);
        check("paused time excluded",Math.abs(c.timeAttack.seconds-.25)<.001);
        PlayerSettings.open(c);c.update(1,1,L);c.closeSettings();
        check("settings time excluded",Math.abs(c.timeAttack.seconds-.25)<.001);
        c.boss.beaten=true;c.boss.leaveT=Boss.LEAVE;c.update(.01f,.25f,L);
        check("defeat animation not timed",Math.abs(c.timeAttack.seconds-.25)<.001);
        c.timeAttack.seconds=12.345;long owned=c.collected;String adventure=m.highScores;
        beat(c,L);
        check("successful boss ends at result with local best",c.state==GameCore.OVER && c.timeAttack.won
                && c.timeAttack.newBest && c.timeAttack.best[c.timeAttack.profile][Boss.SLIME]==12345);
        check("replay does not award Adventure boss or change records",c.collected==owned
                && c.roundPrizes==0 && m.highScores.equals(adventure) && m.best==789
                && c.progress.maximum("highest_stage")==0);
        String saved=m.timeAttack;c.timeAttack.finish(c,true);
        check("finishing twice is inert",saved.equals(m.timeAttack));
        c.toTitle();check("title clears session only",!c.timeAttack.active && c.timeAttack.seconds==0
                && c.modes.selected==ModeSelector.TIME_ATTACK && c.timeAttack.best[0][0]==12345);
        c.startGame();c.timeAttack.seconds=15;beat(c,L);
        check("slower retry keeps personal best",!c.timeAttack.newBest && c.timeAttack.best[0][0]==12345);
        c.toTitle();c.startGame();c.timeAttack.seconds=10;beat(c,L);
        check("faster retry updates personal best",c.timeAttack.newBest && c.timeAttack.best[0][0]==10000);
        GameCore reloaded=new GameCore(m,151);
        check("records survive restart",reloaded.timeAttack.best[0][0]==10000);
        c.toTitle();c.startGame();c.boss.intro=0;c.timeAttack.seconds=2;c.lives=1;c.takeHit(L.w*.5f,L);
        check("failed attempt never replaces clear record",c.state==GameCore.OVER && c.timeAttack.finished
                && !c.timeAttack.won && c.timeAttack.best[0][0]==10000);
        c.toTitle();c.startGame();Pause.open(c);Pause.action(c,2);Pause.action(c,2);
        check("pause exit clears boss and timer without recording",c.state==GameCore.TITLE
                && !c.timeAttack.active && !c.boss.active() && c.timeAttack.best[0][0]==10000);
        c.modes.restore(ModeSelector.ADVENTURE);c.startGame();
        check("mode switch starts ordinary Adventure",!c.timeAttack.active && c.stage==1 && !c.boss.active());
        c=start(TimeAttack.ALL);c.lives=1;
        for(int boss=0;boss<Boss.COUNT;boss++) {
            check("all-boss order is fixed "+boss,c.boss.kind==boss && c.boss.intro>0);
            c.update(.01f,10,L);
            check("between-boss introduction excluded "+boss,c.timeAttack.seconds==boss*5);
            c.boss.intro=0;c.timeAttack.tick(c,5);c.pushUsed=true;beat(c,L);
            if(boss+1<Boss.COUNT)check("boss clear carries lives and reloads rescue "+boss,
                    c.lives==Math.min(GameCore.START_LIVES,boss+2) && !c.pushUsed && c.state==GameCore.PLAY);
        }
        check("all-boss saves only full challenge record",c.timeAttack.won && c.timeAttack.cleared==4
                && c.timeAttack.best[0][TimeAttack.ALL]==20000 && c.timeAttack.best[0][0]==0);
        for(int profile=0;profile<3;profile++) {
            c=new GameCore(store(),152);c.preferences.kids=profile==2;c.fullRoster=profile==1;
            c.modes.restore(ModeSelector.TIME_ATTACK);c.timeAttack.selected=Boss.OCTOPUS;c.startGame();
            c.timeAttack.seconds=8+profile;beat(c,L);
            for(int other=0;other<3;other++)check("profile records separated "+profile+"/"+other,
                    c.timeAttack.best[other][Boss.OCTOPUS]==(other==profile?(8+profile)*1000:0));
        }
        c=new GameCore(TestSurvival.store(),154);c.modes.restore(ModeSelector.TIME_ATTACK);
        c.timeAttack.selected=Boss.MUSHROOM;c.startGame();
        check("locked selected boss cannot start",c.state==GameCore.TITLE && !c.timeAttack.active);
        c.timeAttack.selected=TimeAttack.ALL;c.startGame();
        check("all-boss requires every boss",c.state==GameCore.TITLE && !c.timeAttack.active);
        c.timeAttack.selected=0;float y=TimeAttack.selectY(L);
        c.modes.touch(c,L,0,42,L.w*.9f,y);c.modes.touch(c,L,1,42,L.w*.9f,y);
        check("boss selector shares native touch routing",c.timeAttack.selected==1 && c.modes.selected==ModeSelector.TIME_ATTACK);
        c.modes.touch(c,L,0,42,L.w*.9f,y);Pause.release(c);c.modes.touch(c,L,1,42,L.w*.9f,y);
        check("cancelled boss touch does not move selection",c.timeAttack.selected==1);
        c.timeAttack.best[0][0]=999;c.timeAttack.load("1;garbage");
        check("malformed records leave valid records intact",c.timeAttack.best[0][0]==999);
        c.timeAttack.load(new TimeAttack().encode().replaceFirst(";0",";-1"));
        check("negative times rejected atomically",c.timeAttack.best[0][0]==999);
        check("record reset includes Time Attack",c.resetHighScores() && c.timeAttack.best[0][0]==0
                && ((Mem)c.store).timeAttack.isEmpty());
    }
}
