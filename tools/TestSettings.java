package com.dddumpling.game;

final class TestSettings extends Check {
    private static void kidsTiming(Layout L) {
        group("kids timing boundaries");
        GameCore c=new GameCore(new Mem(),901L);c.preferences.kids=true;c.startGame();
        c.stageGap=2f;c.enemies.clear();
        GameCore.Enemy word=new GameCore.Enemy();word.word=new int[]{0,1};
        word.gone=new boolean[2];word.goneT=new float[2];word.y=L.playTop;word.speed=100f;word.hitPulse=1f;
        c.enemies.add(word);float y=word.y,clock=c.clock;
        c.update(.1f,L);
        check("kids slows word descent only",Math.abs(word.y-y-4.5f)<.001f
                && Math.abs(c.clock-clock-.1f)<.001f && Math.abs(c.stageGap-1.9f)<.001f);
        check("kids hit feedback animates normally",Math.abs(word.hitPulse-.35f)<.001f);
        word.attacking=true;word.attackT=0;word.y=L.dangerY;
        c.update(.1f,L);
        check("kids lunge arrival follows slowed traversal",Math.abs(word.attackT-.045f)<.001f
                && Math.abs(word.y-L.dangerY-14.4f)<.001f);
        c.enemies.clear();
        GameCore.Shot shot=new GameCore.Shot();shot.dur=1f;c.shots.add(shot);
        c.update(.1f,L);
        check("kids player projectile keeps normal flight speed",Math.abs(shot.t-.1f)<.001f);
        for(int kind=0;kind<Boss.COUNT;kind++) {
            c.boss.begin(kind,5,c.rnd);float intro=c.boss.intro;
            c.update(.1f,L);
            check("kids preserves boss introduction "+kind,Math.abs(c.boss.intro-intro+.1f)<.001f);
            c.boss.intro=0;c.boss.blive[0]=true;c.boss.bt[0]=0;
            float age=c.boss.age;c.update(.1f,L);
            check("kids preserves boss action and projectile clocks "+kind,
                    Math.abs(c.boss.age-age-.1f)<.001f
                    && Math.abs(c.boss.bt[0]-.1f/(kind==Boss.RAINBOW_CAT?RainbowCat.PROJECTILE_TIME:Boss.BOLT_TIME))<.001f);
        }
        c.boss.begin(Boss.MUSHROOM,5,c.rnd);c.boss.intro=0;c.boss.mushroomCharge=.5f;
        c.update(.1f,L);
        check("kids boss charge keeps its action duration",Math.abs(c.boss.mushroomCharge-.4f)<.001f);
        c.boss.beaten=true;c.boss.leaveT=1f;c.update(.1f,L);
        check("kids boss defeat sequence keeps its duration",Math.abs(c.boss.leaveT-.9f)<.001f);
        c.boss.leave();
        for(boolean stars:new boolean[]{false,true}) {
            c.starNext=stars;Interlude.enterBonus(c,L);float timer=c.bonusTimer;
            c.update(.1f,L);
            check("kids preserves minigame and ready timing "+stars,Math.abs(c.bonusTimer-timer+.1f)<.001f);
        }
        c.state=GameCore.BONUS;c.cart.begin(c);float ready=c.cart.ready;
        c.update(.1f,L);
        check("kids cart ready sequence runs normally",Math.abs(c.cart.ready-ready+.1f)<.001f);
        c.cart.ready=0;c.update(.1f,L);
        check("kids cart ride clock runs normally",Math.abs(c.cart.elapsed-.1f)<.001f);
    }

    private static void kidsMinigames(Layout L) {
        Mem store=new Mem();store.starWins=StarPath.MAX_DIFFICULTY;
        GameCore c=new GameCore(store,583L);c.preferences.kids=true;c.startGame();
        c.steamer.opens=500;
        for(boolean panic:new boolean[]{false,true}) for(int hurt:new int[]{0,1}) for(int misses:new int[]{0,1}) {
            c.pushUsed=panic;c.hurtThisStage=hurt;c.missesThisStage=misses;c.earnedMash=1f;
            c.starNext=false;Interlude.enterBonus(c,L);
            c.update(GameCore.BONUS_ROLL+60,L);
            check("kids steamer never expires regardless of stage result",c.bonusMashing()
                    && c.bonusTimer==c.bonusRollEnd && c.steamer.goal()==10);
        }
        for(int opens:new int[]{0,5,500}) {
            c.steamer.opens=opens;
            check("kids target stays ten at lifetime wins "+opens,c.steamer.goal()==10);
        }
        c.onboarding.begin(c,Onboarding.STEAMER,L);
        check("kids steamer tutorial uses the same target",c.onboarding.practice.steamer.goal()==10);
        c.onboarding.clear();
        for(int i=0;i<19;i++)c.tapBonus(c.steamer.wanted());
        check("nineteen presses have not filled ten pips",c.steamer.hits==9 && !c.bonusSwipeReady());
        c.tapBonus(c.steamer.wanted());c.update(60,L);
        check("tenth pip waits indefinitely for the lid swipe",c.bonusSwipeReady() && c.steamer.hits==10);
        c.swipeBonus();
        check("kids swipe earns the normal reward",c.bonusPrizeWon() && c.steamer.opens==501);
        c.update(Steamer.FREE_TIME+.1f,L);c.update(GameCore.PARADE_TIME+.1f,L);
        check("unlimited time does not freeze success or the next stage",c.state==GameCore.PLAY);
        c.starNext=true;Interlude.enterBonus(c,L);
        StarPath cap=new StarPath();cap.wins=StarPath.KIDS_DIFFICULTY;
        c.stars.reroll(new java.util.Random(584L));cap.make(new java.util.Random(584L));
        check("kids course uses thirty percent difficulty at saved maximum",
                c.stars.bendRate()==cap.bendRate() && c.stars.total()==StarPath.COUNT+StarPath.KIDS_DIFFICULTY
                        && java.util.Arrays.equals(c.stars.sx,cap.sx));
        check("kids cap preserves saved normal difficulty",c.stars.wins==StarPath.MAX_DIFFICULTY
                && store.starWins==StarPath.MAX_DIFFICULTY);
        c.stars.wins=1;
        check("kids cap preserves easier courses",Math.abs(c.stars.bendRate()-1.6f)<.001f);
        c.stars.wins=StarPath.KIDS_DIFFICULTY;c.stars.recordWin();
        check("kids wins cannot exceed effective difficulty cap",c.stars.bendRate()==cap.bendRate());
        c.preferences.kids=false;c.startGame();c.stars.wins=StarPath.MAX_DIFFICULTY;
        check("normal run restores full star difficulty",Math.abs(c.stars.bendRate()-7f)<.001f);
        c.starNext=false;c.earnedMash=1f;Interlude.enterBonus(c,L);
        c.update(GameCore.BONUS_ROLL+1.01f,L);
        check("normal steamer restores saved target and its deadline",c.steamer.goal()==Steamer.MAX_GOAL
                && c.bonusHolding() && !c.bonusMashing());
    }

    private static void panelTiming(Layout L) {
        GameCore settings=new GameCore(new Mem(),915L),scores=new GameCore(new Mem(),916L),news=new GameCore(new Mem(),917L);
        PlayerSettings.open(settings);scores.highScoreScreen.show(scores);news.releaseNotes.show(news,L);
        news.releaseNotes.update(ReleaseTransition.GATHER_TIME,L);
        check("release steamer lead-in ends before the shared slide",Math.abs(news.releaseNotes.transition.listX(L)-L.w)<.001f);
        float half=PlayerSettings.PANEL_TIME*.5f;
        settings.update(half,L);scores.update(half,L);news.update(half,L);
        float travel=settings.preferences.panelOffset(L)/L.w;
        check("three panels share midpoint timing and easing",Math.abs(travel-.5f)<.0001f
                && Math.abs(-scores.highScoreScreen.offsetY(L)/HighScoreScreen.bottom(L)-travel)<.0001f
                && Math.abs(news.releaseNotes.transition.listX(L)/L.w-travel)<.0001f);
        settings.update(half+.00001f,L);scores.update(half+.00001f,L);news.update(half+.00001f,L);
        check("three panels settle after one shared duration",!settings.preferences.panelMoving()
                && !scores.highScoreScreen.moving() && !news.releaseNotes.transition.moving());
        PlayerSettings.close(settings);scores.highScoreScreen.back(scores);news.releaseNotes.close();
        settings.update(half,L);scores.update(half,L);news.update(half,L);
        check("three exits share midpoint timing",Math.abs(settings.preferences.panelOffset(L)/L.w-.5f)<.0001f
                && Math.abs(-scores.highScoreScreen.offsetY(L)/HighScoreScreen.bottom(L)-.5f)<.0001f
                && Math.abs(news.releaseNotes.transition.listX(L)/L.w-.5f)<.0001f);
        settings.update(half+.00001f,L);scores.update(half+.00001f,L);news.update(half+.00001f,L);
        check("three exits finish after one shared duration",!settings.settingsOpen && !scores.highScoreScreen.open && !news.releaseNotes.open);
    }
    private static void kidsToggle(Layout L) {
        Mem mem=new Mem();GameCore c=new GameCore(mem,911L);c.startGame();PlayerSettings.open(c);
        c.update(PlayerSettings.PANEL_TIME,L);
        SettingsInput.action(c,L,1000+PlayerSettings.KIDS);
        check("kids choice persists immediately while handle starts at old position",new GameCore(mem,912L).preferences.kids
                && c.preferences.kidsPosition(c.clock)==0f && !c.kidsRun);
        c.update(PlayerSettings.KIDS_TOGGLE_TIME*.5f,L);
        float position=c.preferences.kidsPosition(c.clock),lift=c.preferences.kidsLift(c.clock);
        check("pear slides and hops between positions",position>0f && position<1f && lift>0f);
        SettingsInput.action(c,L,1000+PlayerSettings.KIDS);
        check("rapid retoggle continues from visible pose",Math.abs(c.preferences.kidsPosition(c.clock)-position)<.0001f
                && Math.abs(c.preferences.kidsLift(c.clock)-lift)<.0001f && !c.preferences.kids);
        c.update(PlayerSettings.KIDS_TOGGLE_TIME+.01f,L);
        check("off animation settles and saves",c.preferences.kidsPosition(c.clock)==0f
                && c.preferences.kidsLift(c.clock)==0f && !new GameCore(mem,913L).preferences.kids);
        SettingsInput.action(c,L,1000+PlayerSettings.KIDS);c.update(PlayerSettings.KIDS_TOGGLE_TIME+.01f,L);
        PlayerSettings loaded=new GameCore(mem,914L).preferences;
        check("on animation and restart settle at young pear",c.preferences.kidsPosition(c.clock)==1f
                && c.preferences.kidsLift(c.clock)==0f && loaded.kidsPosition(0f)==1f && loaded.kidsLift(0f)==0f);
    }
    private static void kidsSwipe(Layout L) {
        Mem store=new Mem();GameCore c=new GameCore(store,915L);c.startGame();PlayerSettings.open(c);
        c.update(PlayerSettings.PANEL_TIME,L);
        SettingsInput input=new SettingsInput();float s=PlayerSettings.unit(L);
        float center=PlayerSettings.right(L)-s*3.2f,left=center-s,right=center+s,y=PlayerSettings.row(L,2);
        input.touch(c,L,0,7,left,y);
        input.touch(c,L,2,99,right,y);input.touch(c,L,1,99,right,y);
        check("another finger cannot swipe the kids switch",!c.preferences.kids);
        input.touch(c,L,2,7,right,y);
        check("kids swipe waits for release",!c.preferences.kids);
        input.touch(c,L,1,7,right,y);
        check("right swipe enables and saves kids mode for the next run",c.preferences.kids
                && new GameCore(store,916L).preferences.kids && !c.kidsRun);
        check("swipe uses the existing animated toggle",c.preferences.kidsPosition(c.clock)==0f);
        input.touch(c,L,0,7,left,y);input.touch(c,L,2,7,right,y);input.touch(c,L,1,7,right,y);
        check("another right swipe leaves kids mode enabled",c.preferences.kids);
        input.touch(c,L,0,7,right,y);input.touch(c,L,1,7,left,y);
        check("left swipe disables even without an intermediate move",!c.preferences.kids
                && !new GameCore(store,917L).preferences.kids);
        input.touch(c,L,0,7,left,y);input.touch(c,L,2,7,right,y);input.touch(c,L,3,7,right,y);
        input.touch(c,L,1,7,right,y);
        check("cancelled kids swipe leaves the saved choice alone",!c.preferences.kids);
        input.touch(c,L,0,7,left,y);input.touch(c,L,2,7,left,y+s);
        input.touch(c,L,2,7,right,y);input.touch(c,L,1,7,right,y);
        check("vertical drag cannot become a kids swipe on release",!c.preferences.kids);
        input.touch(c,L,0,7,left,y);input.touch(c,L,2,7,left+L.unit*.5f,y);
        input.touch(c,L,1,7,left+L.unit*.5f,y);
        check("short drags do not toggle kids mode",!c.preferences.kids);
        input.touch(c,L,0,7,left,y);input.touch(c,L,1,7,left+L.unit*.1f,y);
        check("tap with normal finger jitter still toggles kids mode",c.preferences.kids);
    }

    private static void transitions(Layout L) {
        GameCore c=new GameCore(new Mem(),910L);c.startGame();
        float time=c.time,gap=c.stageGap;
        PlayerSettings.open(c);
        check("settings starts offscreen right",c.preferences.panelOffset(L)==L.w);
        SettingsInput input=new SettingsInput();float x=L.w*.28f,y=PlayerSettings.socialY(L);
        input.touch(c,L,0,1,x,y);input.touch(c,L,1,1,x,y);
        check("moving panel cannot launch external flow",!c.preferences.externalBusy);
        c.update(PlayerSettings.PANEL_TIME*.5f,L);
        check("settings slides into place while run stays frozen",c.preferences.panelOffset(L)>0f
                && c.preferences.panelOffset(L)<L.w && c.time==time && c.stageGap==gap);
        c.update(PlayerSettings.PANEL_TIME*.5f,L);
        check("settings settles at its hit targets",!c.preferences.panelMoving() && c.preferences.panelOffset(L)==0f);
        PlayerSettings.close(c);c.update(PlayerSettings.PANEL_TIME*.5f,L);
        float offset=c.preferences.panelOffset(L);
        check("exit remains modal and moves right",c.settingsOpen && offset>0f && offset<L.w);
        Pause.back(c);
        check("repeated back does not restart exit",c.preferences.panelOffset(L)==offset && !c.paused);
        input.touch(c,L,0,1,x,y);input.touch(c,L,1,1,x,y);c.tapKey(0,L);
        check("exit consumes gameplay and settings input",!c.preferences.externalBusy && c.time==time && c.stageGap==gap);
        c.update(PlayerSettings.PANEL_TIME*.5f,L);
        check("exit resumes only after panel leaves",!c.settingsOpen && c.time==time && c.stageGap==gap);
        c.update(.05f,L);check("game resumes on following tick",c.time>time);
        PlayerSettings.open(c);c.update(PlayerSettings.PANEL_TIME*.25f,L);PlayerSettings.close(c);
        c.update(PlayerSettings.PANEL_TIME*.25f,L);
        check("back during entrance reverses and closes",!c.settingsOpen);
    }
    private static void social(Layout L) {
        GameCore c=new GameCore(new Mem(),909L);PlayerSettings.open(c);
        float y=PlayerSettings.socialY(L);
        check("share target",PlayerSettings.hit(c,L,L.w*.28f,y)==PlayerSettings.SHARE);
        check("rate target",PlayerSettings.hit(c,L,L.w*.72f,y)==PlayerSettings.RATE);
        check("social rows fit above privacy",y+PlayerSettings.unit(L)*2.4f<PlayerSettings.row(L,3)-PlayerSettings.unit(L)*1.1f);
        check("production invitation",PlayerSettings.invitation(PlayerSettings.PUBLIC_ANDROID_URL).equals(
                "Come play DDDUMPLING with me! https://play.google.com/store/apps/details?id=com.dddumpling.game"));
        SettingsInput.action(c,L,1000+PlayerSettings.SHARE);
        check("tap begins feedback before dispatch",c.preferences.externalBusy && c.preferences.takeExternal(c)==0);
        c.update(.2f,L);
        check("share dispatch consumed once",c.preferences.takeExternal(c)==PlayerSettings.SHARE && c.preferences.takeExternal(c)==0);
        SettingsInput.action(c,L,1000+PlayerSettings.RATE);
        check("busy host ignores additional tap",c.preferences.externalAction==0);
        c.preferences.externalFinished(c);
        SettingsInput.action(c,L,1000+PlayerSettings.RATE);
        check("return cooldown blocks double tap",!c.preferences.externalBusy);
        c.update(.6f,L);SettingsInput.action(c,L,1000+PlayerSettings.RATE);c.update(.2f,L);
        check("rate can launch after dismissal",c.preferences.takeExternal(c)==PlayerSettings.RATE);
    }
    private static void settingsTap(GameCore c,Layout L,float x,float y) {
        SettingsInput input=new SettingsInput();
        input.touch(c,L,0,7,x,y);input.touch(c,L,1,7,x,y);
    }
    static void scoreReset(Layout L) {
        group("score reset");
        Mem save=new Mem();save.collected=3L;save.caseIndex=1;save.landState=(1<<30)|2;
        GameCore c=new GameCore(save,111L,true);
        Ear ear=new Ear();c.sound=ear;
        c.preferences.kids=true;c.preferences.music=.4f;c.preferences.save(c);
        c.startGame();c.score=9000;LandPicker.recordBest(c);c.highScores.finish(c);
        c.progress.checkpoint(c.score);c.toTitle();
        c.landBests[1]=12000;save.saveLandBest(1,12000);
        byte[] oldCloud=c.progress.snapshot();
        long collected=c.collected;int landState=save.landState;int prefs=save.loadPlayerSettings();
        PlayerSettings.open(c);c.preferences.updatePanel(c,PlayerSettings.PANEL_TIME);
        ScoreReset ui=c.preferences.scoreReset;
        settingsTap(c,L,L.w*.5f,PlayerSettings.resetY(L));
        check("reset action asks before clearing",ui.confirming && c.best==9000 && save.scoreResets==0);
        settingsTap(c,L,L.w*.28f,ScoreReset.buttonsY(L));
        check("cancel leaves records and animation untouched",!ui.active() && c.highScores.latestRun!=null && save.scoreResets==0);
        settingsTap(c,L,L.w*.5f,PlayerSettings.resetY(L));Pause.back(c);
        check("back cancels confirmation but keeps settings",!ui.active() && c.settingsOpen && !c.preferences.panelClosing);
        settingsTap(c,L,L.w*.5f,PlayerSettings.resetY(L));
        settingsTap(c,L,L.w*.72f,ScoreReset.buttonsY(L));
        check("confirmed reset clears every score surface",c.best==0 && c.landBests[1]==0
                && c.highScores.runs.isEmpty() && c.highScores.latestRun==null && !c.highScores.unread
                && c.highScoreScreen.selected==-1 && !c.highScoreScreen.open);
        check("reset starts one celebration",ui.left==ScoreReset.TIME && save.scoreResets==1);
        check("confirmed reset has its own cue",ear.scoreResetConfirms==1 && ear.uiBloops==0);
        for(int i=0;i<3;i++) settingsTap(c,L,L.w*.72f,ScoreReset.buttonsY(L));
        SettingsInput.action(c,L,1000+PlayerSettings.KIDS);Pause.back(c);
        check("celebration blocks overlapping reset and navigation",ui.left==ScoreReset.TIME
                && save.scoreResets==1 && c.settingsOpen && c.preferences.kids);
        c.update(ScoreReset.LEAD+.001f,L);
        check("first wipe starts one brush sound",ear.scoreResetBrushes==1 && ear.lastScoreResetBrush==0);
        c.update(ScoreReset.STROKE_TIME+ScoreReset.STROKE_GAP,L);
        check("second wipe starts one brush sound",ear.scoreResetBrushes==2 && ear.lastScoreResetBrush==1);
        c.update(ScoreReset.STROKE_TIME+ScoreReset.STROKE_GAP,L);
        check("third wipe starts one brush sound",ear.scoreResetBrushes==3 && ear.lastScoreResetBrush==2);
        c.progress.checkpoint(c.score);LandPicker.recordBest(c);c.highScores.finish(c);
        check("old run cannot immediately repopulate cleared scores",c.best==0
                && c.highScores.runs.isEmpty() && c.progress.maximum("best_score")==0);
        c.update(ScoreReset.TIME+.01f,L);
        check("celebration returns control to settings",!ui.active() && c.settingsOpen && !c.preferences.panelMoving());
        GameCore reopened=new GameCore(save,112L,true);
        check("reset survives restart",reopened.best==0 && reopened.landBests[1]==0
                && reopened.highScores.runs.isEmpty() && reopened.highScores.latestRun==null);
        check("non-score save is untouched",reopened.collected==collected && reopened.caseIndex==1
                && save.landState==landState && save.loadPlayerSettings()==prefs);
        try {
            reopened.progress.restore(oldCloud,reopened);
            check("stale sync cannot resurrect a reset score",reopened.best==0 && reopened.progress.maximum("best_score")==0);
        } catch(Exception failure) { check("stale restore decodes",false); }
        reopened.startGame();reopened.score=321;LandPicker.recordBest(reopened);
        reopened.highScores.finish(reopened);reopened.progress.finishRun(reopened.score,false);
        check("next run establishes new records",reopened.best==321 && reopened.highScores.latestRun.score==321);
        check("new records survive another restart",new GameCore(save,113L,true).best==321);
        PlayerSettings.open(reopened);reopened.preferences.updatePanel(reopened,PlayerSettings.PANEL_TIME);
        save.resetFails=true;
        settingsTap(reopened,L,L.w*.5f,PlayerSettings.resetY(L));
        settingsTap(reopened,L,L.w*.72f,ScoreReset.buttonsY(L));
        check("failed save keeps scores and offers retry without celebration",reopened.best==321
                && reopened.preferences.scoreReset.failed && reopened.preferences.scoreReset.left==0f);
        // Resetting a paused live run leaves play state and collectibles alone.
        save.resetFails=false;reopened.preferences.scoreReset.cancel();reopened.closeSettings();
        reopened.startGame();reopened.score=44;int stage=reopened.stage;
        PlayerSettings.open(reopened);reopened.preferences.updatePanel(reopened,PlayerSettings.PANEL_TIME);
        reopened.preferences.scoreReset.action(reopened,ScoreReset.OPEN);
        reopened.preferences.scoreReset.action(reopened,ScoreReset.CONFIRM);
        reopened.update(ScoreReset.TIME+.1f,L);
        check("live reset does not restart gameplay",reopened.state==GameCore.PLAY && reopened.score==44 && reopened.stage==stage);
    }

    private static void infiniteLives(Layout L) {
        group("developer infinite lives");
        SettingsUi ui=new SettingsUi();ui.compute(L,SettingsUi.GENERAL);
        int hit=ui.hit(L.w*.5f,ui.infiniteLivesY+ui.testH*.5f);
        check("infinite lives chip fits below run controls",hit==SettingsUi.HIT_INFINITE_LIVES
                && ui.infiniteLivesY>ui.runY+ui.runH && ui.infiniteLivesY+ui.testH<ui.panelB);
        for(int mode=0;mode<ModeSelector.NAMES.length;mode++) {
            Mem save=TestTimeAttack.store();GameCore c=new GameCore(save,151,true);
            c.modes.restore(mode);c.startGame();c.lives=1;
            check("infinite lives defaults off "+mode,!c.devInfiniteLives);
            SettingsInput.action(c,L,hit);
            check("enabling restores hearts in current run "+mode,c.devInfiniteLives && c.lives==GameCore.START_LIVES);
            int damage=c.hurtThisStage;
            for(int i=0;i<8;i++)c.takeHit(L.w*.5f,L);
            check("hits keep feedback without ending run "+mode,c.state==GameCore.PLAY
                    && c.lives==GameCore.START_LIVES && c.hurtThisStage==damage+8 && c.flash>0 && c.shake>0);
            c.toTitle();c.startGame();
            check("toggle survives playthroughs in this session "+mode,c.devInfiniteLives);
            SettingsInput.action(c,L,hit);c.lives=1;c.takeHit(L.w*.5f,L);
            check("disabling restores ordinary death "+mode,!c.devInfiniteLives && c.state==GameCore.OVER && c.lives==0);
            c.toTitle();c.startGame();SettingsInput.action(c,L,hit);c.endCurrentRun();
            check("manual end run still works "+mode,c.state==GameCore.OVER && c.lives==0);
            check("new app session starts with ordinary lives "+mode,!new GameCore(save,152,true).devInfiniteLives);
            DevSettings.resetProgress(c);DevSettings.resetProgress(c);
            check("fresh player reset clears infinite lives "+mode,!c.devInfiniteLives);
        }
    }
    private static void developerBossCompletion(Layout L) {
        group("developer boss completion");
        for(boolean tracked:new boolean[]{false,true}) {
            Mem save=new Mem();
            int slime=Collect.bossPrize(Boss.SLIME);
            save.collected=Collect.encode(Collect.add(1L,slime));
            save.collectionCounts[0]=2;save.collectionCounts[slime]=3;save.collectTotal=5;
            GameCore c=new GameCore(save,179,tracked);
            LandPicker.reset(c);
            c.clearArmed=c.resetProgressArmed=true;
            SettingsUi ui=new SettingsUi();ui.compute(L,SettingsUi.PROGRESS);
            int hit=ui.hit(L.w*.5f,ui.completeBossesY+ui.testH*.5f);
            check("boss completion chip fits between land and news controls",hit==SettingsUi.HIT_COMPLETE_BOSSES
                    && ui.completeBossesY>ui.debuffY+ui.testH
                    && ui.completeBossesY+ui.testH<ui.difficultyLabelY
                    && ui.resetProgressY+ui.testH<ui.panelB);
            SettingsInput.action(c,L,hit);
            long expected=Collect.add(1L,slime);
            for(int boss=0;boss<Boss.COUNT;boss++)expected=Collect.add(expected,Collect.bossPrize(boss));
            check("completion preserves existing collectibles and only adds boss friends",c.collected==expected
                    && c.collectionCounts[0]==2 && c.collectionCounts[slime]==3
                    && c.collectTotal==5+Boss.COUNT-1 && !c.clearArmed && !c.resetProgressArmed);
            SettingsInput.action(c,L,hit);
            check("completing bosses twice does not add duplicate rewards",c.collectTotal==5+Boss.COUNT-1);
            for(GameCore restored:new GameCore[]{c,new GameCore(save,180,tracked)}) {
                check("boss completion unlocks modes persistently",ModeSelector.allBossesUnlocked(restored)
                        && ModeSelector.unlocked(restored,ModeSelector.SURVIVAL)
                        && ModeSelector.unlocked(restored,ModeSelector.TIME_ATTACK));
                boolean lands=true;
                for(int land=0;land<Lands.COUNT;land++)
                    if(Lands.playable(land))lands &= LandPicker.unlocked(restored,land);
                check("completion restores boss land unlocks after reset lands",lands);
                check("completion leaves run and records alone",restored.state==GameCore.TITLE
                        && restored.score==0 && restored.best==0 && restored.highScores.runs.isEmpty()
                        && restored.progress.count("boss_slime_won")==0);
            }
        }
    }
    private static void developerReset(Layout L) {
        group("developer fresh player reset");
        for(boolean tracked:new boolean[]{false,true}) for(int mode=0;mode<ModeSelector.NAMES.length;mode++) {
            Mem save=new Mem();save.collected=Collect.encode(Collect.MASK);
            java.util.Arrays.fill(save.collectionCounts,2);save.collectTotal=120;
            save.best=999;java.util.Arrays.fill(save.landBests,999);
            save.landState=LandPicker.stateMask();save.steamerOpens=9;save.starWins=8;
            save.cartTrack=2;save.mineCarts=2;save.caveChoice=1;save.caveMiningNext=true;
            Town town=new Town();town.tickets=20;town.visits=4;town.archBuilt=true;save.townState=town.save();
            GameCore c=new GameCore(save,170,tracked);c.preferences.music=.35f;c.preferences.kids=true;
            c.modes.restore(mode);c.preferences.save(c);c.startGame();
            if(mode==ModeSelector.ADVENTURE)c.playtestSteamer(L);
            if(mode==ModeSelector.SURVIVAL)c.playtestMode(Power.NINJA,L);
            int oldState=c.state;
            c.score=123;c.openSettings();c.settingsTab=SettingsUi.PROGRESS;
            SettingsUi ui=new SettingsUi();ui.compute(L,SettingsUi.PROGRESS);
            check("fresh start chip has its own target",ui.hit(L.w*.5f,ui.resetProgressY+ui.testH*.5f)==SettingsUi.HIT_RESET_PROGRESS
                    && ui.resetProgressY+ui.testH<ui.panelB);
            SettingsInput.action(c,L,SettingsUi.HIT_RESET_PROGRESS);
            check("first tap only arms the reset",c.resetProgressArmed && c.collected==Collect.MASK && c.state==oldState);
            SettingsInput.action(c,L,SettingsUi.HIT_GENERAL);
            check("changing tab cancels reset confirmation",!c.resetProgressArmed);
            SettingsInput.action(c,L,SettingsUi.HIT_RESET_PROGRESS);c.closeSettings();
            check("closing settings cancels confirmation",!c.resetProgressArmed);
            c.openSettings();c.settingsTab=SettingsUi.PROGRESS;
            SettingsInput.action(c,L,SettingsUi.HIT_RESET_PROGRESS);
            SettingsInput.action(c,L,SettingsUi.HIT_RESET_PROGRESS);
            check("reset exits the run to a fresh title",c.state==GameCore.TITLE && !c.settingsOpen
                    && !c.survival.active && !c.timeAttack.active && !c.boss.active() && !c.powerActive()
                    && !c.pendingBonus && c.modes.adventure() && !c.fullRoster);
            for(GameCore fresh:new GameCore[]{c,new GameCore(save,171,tracked)}) {
                check("empty collection and tutorials restore starter flow",Starter.eligible(fresh)
                        && fresh.collected==0 && fresh.collectTotal==0 && fresh.onboarding.saved==0 && fresh.onboarding.savedPowers==0);
                check("other modes relock",!ModeSelector.unlocked(fresh,ModeSelector.SURVIVAL)
                        && !ModeSelector.unlocked(fresh,ModeSelector.TIME_ATTACK));
                check("lands and scores reset",fresh.landSeen==0 && fresh.landSuppressed==0 && fresh.best==0
                        && fresh.highScores.runs.isEmpty() && fresh.progress.maximum("highest_stage")==0);
                check("difficulty and cave progress reset",fresh.steamer.opens==0 && fresh.stars.wins==0
                        && fresh.cart.progress==0 && fresh.mining.carts==0 && fresh.caveChoice==-1 && !fresh.caveMiningNext);
                check("town and roster reset",fresh.town.tickets==0 && fresh.town.visits==0 && !fresh.town.archBuilt && !fresh.fullRoster);
                check("sound retained and kids mode restored to default",Math.abs(fresh.preferences.music-.35f)<.001f && !fresh.preferences.kids);
                fresh.beginStart();check("first play opens choose your squishy",fresh.starter.open);
            }
            check("all score stores and pending rewards cleared",save.highScores.isEmpty() && save.survival.isEmpty()
                    && save.timeAttack.isEmpty() && save.survivalAward.isEmpty());
        }
    }

    private static void seasonalSkins(Layout L) {
        bossNames(L);
        group("seasonal skins");
        Mem save=new Mem();GameCore c=new GameCore(save,200L);Ear ear=new Ear();c.sound=ear;
        check("old saves use default skin",c.preferences.skin==Skins.DEFAULT);
        c.startGame();c.score=123;c.preferences.music=.35f;c.preferences.effectsMuted=true;
        int stage=c.stage;PlayerSettings.open(c);c.update(PlayerSettings.PANEL_TIME,L);
        settingsTap(c,L,L.w*.5f,SkinSettings.buttonY(L));
        check("skin gallery opens from player settings",c.preferences.skinsOpen);
        settingsTap(c,L,L.w*.5f,SkinSettings.cardTop(L,1)+PlayerSettings.unit(L)*6);
        check("gallery selects Halloween without altering run",c.preferences.skin==Skins.HALLOWEEN && c.score==123 && c.stage==stage);
        GameCore restored=new GameCore(save,201L);
        check("skin and other preferences persist together",restored.preferences.skin==Skins.HALLOWEEN
                && restored.preferences.music==.35f && restored.preferences.effectsMuted);
        c.onboarding.begin(c,Onboarding.CORE,L);
        check("tutorial practice inherits selected skin",c.onboarding.practice.preferences.skin==Skins.HALLOWEEN);
        SkinSettings.choose(c,Skins.DEFAULT);
        check("changing skin updates an existing practice",c.onboarding.practice.preferences.skin==Skins.DEFAULT);
        check("switching back preserves progress",c.score==123 && c.stage==stage);
        PlayerSettings prefs=new PlayerSettings();prefs.load(PlayerSettings.DEFAULT|(7<<19));
        check("unknown saved skin falls back safely",prefs.skin==Skins.DEFAULT && prefs.music==1f);
        check("Halloween narration uses the selected boss name",Narration.versus(0,Boss.SLIME,Skins.HALLOWEEN).contains("super ghost"));
        RasterPainter raster=new RasterPainter(80,80,1);
        Painter themed=Skins.painter(raster,Skins.HALLOWEEN);
        check("skin context survives opacity and color effects",new OpacityPainter(new ColorFadePainter(themed,0xFFFFFFFF,.3f,1),.5f).skin()==Skins.HALLOWEEN
                && new MonochromePainter(themed,1).skin()==Skins.HALLOWEEN);
        check("render skin selection does not leak to another painter",raster.skin()==Skins.DEFAULT && Skins.painter(themed,Skins.DEFAULT).skin()==Skins.DEFAULT);
        check("Super Ghost has a seasonal display name",Skins.bossName(Skins.HALLOWEEN,Boss.SLIME).equals("SUPER GHOST")
                && Skins.bossName(Skins.DEFAULT,Boss.SLIME).equals("SUPER SLIME"));
    }
    private static final class NamePainter extends OpacityPainter {
        final java.util.List<String> labels=new java.util.ArrayList<>();
        NamePainter(int skin) {super(Skins.painter(new RasterPainter(1,1,1),skin),1);}
        public void text(String text,float x,float y,float size,int color,int align,boolean bold) {labels.add(text);}
    }
    private static void bossNames(Layout L) {
        group("skin-aware boss names across screens");
        check("Halloween cat is Meowcula",Skins.bossName(Skins.HALLOWEEN,Boss.RAINBOW_CAT).equals("MEOWCULA")
                && Skins.bossTitle(Skins.HALLOWEEN,Boss.RAINBOW_CAT).equals("Meowcula")
                && Skins.bossName(Skins.DEFAULT,Boss.RAINBOW_CAT).equals("RAINBOW CAT"));
        for(int skin=0;skin<Skins.COUNT;skin++)for(int boss=0;boss<Boss.COUNT;boss++) {
            String expected=Skins.bossName(skin,boss);
            GameCore c=TestTimeAttack.start(boss);c.preferences.skin=skin;
            NamePainter p=new NamePainter(skin);
            c.toTitle();c.timeAttack.drawSelector(p,c,L);
            check("title resolves boss name before drawing "+skin+"/"+boss,p.labels.contains(expected));
            c.startGame();c.timeAttack.versus.time=1.4f;
            p.labels.clear();c.timeAttack.versus.draw(p,c,L);
            boolean words=true;for(String word:expected.split(" "))words &= p.labels.contains(word);
            check("VS resolves the name before splitting it into words "+skin+"/"+boss,words);
            check("announcer agrees with displayed opponent "+skin+"/"+boss,
                    Narration.versus(c.runWho,boss,skin).endsWith(expected.toLowerCase(java.util.Locale.ROOT)+"!"));
            c.boss.intro=Boss.INTRO*.5f;
            p.labels.clear();BossScreen.intro(p,c,L);
            check("arrival title agrees with selection "+skin+"/"+boss,p.labels.contains(expected));
            c.boss.intro=0;
            p.labels.clear();BossScreen.bar(p,c,L);
            check("combat header agrees with selection "+skin+"/"+boss,p.labels.contains(expected));
        }
        check("unknown skin inherits default boss names",Skins.bossName(99,Boss.SLIME).equals("SUPER SLIME"));
        check("inactive boss has no name",Skins.bossName(Skins.HALLOWEEN,-1).isEmpty());
        check("tutorial uses the same seasonal boss name",TutorialSpeech.spoken(TutorialSpeech.CLOSED,Skins.HALLOWEEN).contains("Super Ghost"));
        NamePainter literal=new NamePainter(Skins.DEFAULT);
        Skins.painter(literal,Skins.HALLOWEEN).text("SUPER SLIME",0,0,1,0,0,false);
        check("skin painter never rewrites authored text",literal.labels.contains("SUPER SLIME"));
    }
    private static void skinGameplayParity(Layout L) {
        group("cosmetic skin gameplay parity");
        for(int scene=0;scene<3+Boss.COUNT;scene++) {
            GameCore[] runs=new GameCore[2];
            for(int skin=0;skin<2;skin++) {
                GameCore c=new GameCore(TestTimeAttack.store(),200L+scene,true);
                c.preferences.skin=skin;c.fullRoster=false;c.landSeen=LandPicker.stateMask();
                if(scene==1)c.modes.restore(ModeSelector.SURVIVAL);
                if(scene==2){c.modes.restore(ModeSelector.TIME_ATTACK);c.timeAttack.selected=TimeAttack.ALL;}
                c.startGame();
                if(scene>=3){c.stage=Boss.EVERY;c.boss.begin(scene-3,c.stage,c.rnd);}
                runs[skin]=c;
            }
            boolean same=true;
            for(int frame=0;frame<1200;frame++) {
                for(GameCore c:runs) {
                    if(frame%12==0)c.tapKey(Roster.STARTERS[(frame/12)%4],L);
                    c.update(DT,L);
                }
                GameCore a=runs[0],b=runs[1];
                same &= a.state==b.state && a.score==b.score && a.stage==b.stage
                        && a.lives==b.lives && a.hits==b.hits && a.misses==b.misses
                        && a.combo==b.combo && a.boss.hp==b.boss.hp && a.boss.intro==b.boss.intro
                        && a.collected==b.collected && a.spawnTimer==b.spawnTimer
                        && a.survival.seconds==b.survival.seconds && a.enemies.size()==b.enemies.size();
                if(a.enemies.size()==b.enemies.size())for(int i=0;i<a.enemies.size();i++) {
                    GameCore.Enemy x=a.enemies.get(i),y=b.enemies.get(i);
                    same &= x.y==y.y && x.speed==y.speed && java.util.Arrays.equals(x.word,y.word)
                            && java.util.Arrays.equals(x.gone,y.gone);
                }
            }
            check("same inputs preserve gameplay, rewards and RNG in scene "+scene,
                    same && runs[0].rnd.nextLong()==runs[1].rnd.nextLong());
        }
    }
    private static void servicesTab(Layout L) {
        group("sync and achievements settings tab");
        GameCore c=new GameCore(new Mem(),9123L);PlayerSettings.open(c);c.preferences.panelEntrance=1f;
        c.preferences.achievementsEnabled=true;c.cloudUi.status(CloudSaveUi.PENDING);
        float x=(PlayerSettings.tabEdge(L,1)+PlayerSettings.tabEdge(L,2))*.5f;
        float y=PlayerSettings.top(L)+PlayerSettings.unit(L)*4f;
        SettingsInput input=new SettingsInput();
        input.touch(c,L,0,1,x,y);input.touch(c,L,1,1,x,y);
        check("public services tab responds to taps",c.settingsPage==PlayerSettings.SERVICES_PAGE);
        x=L.w*.5f;y=PlayerSettings.serviceY(L,0);
        input.touch(c,L,0,1,x,y);input.touch(c,L,1,1,x,y);
        check("cloud card opens status without leaving its tab",c.cloudUi.open && c.settingsPage==PlayerSettings.SERVICES_PAGE);
        c.cloudUi.close(c);y=PlayerSettings.serviceY(L,1);
        input.touch(c,L,0,1,x,y);input.touch(c,L,1,1,x,y);c.update(.2f,L);
        check("achievement card dispatches platform action",c.preferences.takeExternal(c)==PlayerSettings.ACHIEVEMENTS);
        c.preferences.externalFinished(c);
        c.preferences.achievementsEnabled=false;c.cloudUi.status(CloudSaveUi.OFF);
        check("unavailable services have no active buttons",
                PlayerSettings.hit(c,L,x,PlayerSettings.serviceY(L,0))==0 && PlayerSettings.hit(c,L,x,y)==0);
        float music=c.preferences.music;
        input.touch(c,L,0,1,x,PlayerSettings.row(L,0)+PlayerSettings.unit(L)*3f);
        input.touch(c,L,2,1,PlayerSettings.trackR(L),PlayerSettings.row(L,0)+PlayerSettings.unit(L)*3f);
        input.touch(c,L,1,1,PlayerSettings.trackR(L),PlayerSettings.row(L,0)+PlayerSettings.unit(L)*3f);
        check("hidden player controls cannot react on services tab",c.preferences.music==music);
        SettingsInput.action(c,L,1000+PlayerSettings.PLAYER);
        c.preferences.achievementsEnabled=true;c.cloudUi.status(CloudSaveUi.SAVED);
        check("player footer now only opens privacy",PlayerSettings.hit(c,L,x,PlayerSettings.row(L,3))==PlayerSettings.PRIVACY);
        check("services stay inside panel",PlayerSettings.serviceY(L,1)+PlayerSettings.unit(L)*5.5f<PlayerSettings.bottom(L));
    }
    static void all(Layout L) {
        servicesTab(L);
        seasonalSkins(L);
        skinGameplayParity(L);
        infiniteLives(L);
        developerBossCompletion(L);
        TestCloudSaveUi.all(L);
        developerReset(L);
        scoreReset(L);
        panelTiming(L);
        kidsToggle(L);
        kidsSwipe(L);
        transitions(L);
        social(L);
        kidsTiming(L);
        kidsMinigames(L);
        group("player settings");
        GameCore title=new GameCore(new Mem(),203L);
        check("settings attention starts off until a feature requests it",!title.preferences.notificationPending);
        check("title settings hit target follows danger line",PrivacyUi.hit(title,L,L.w-2f*L.unit,L.dangerY));
        check("old settings position no longer opens settings",!PrivacyUi.hit(title,L,L.w-2f*L.unit,L.dangerY-2f*L.unit));
        check("release steamer sits on danger line",ReleaseMascot.y(L)==L.dangerY
                && title.releaseMascot.hit(L,title.releaseMascot.x(L),L.dangerY));
        Mem m=new Mem(); GameCore c=new GameCore(m,204L); Ear e=new Ear();c.sound=e;
        c.startMusic();
        check("audio defaults enable both channels at full volume",e.musicVolume==1f && e.effectsVolume==1f);
        c.preferences.music=.25f;c.preferences.effects=.8f;c.preferences.musicMuted=true;c.preferences.kids=true;c.preferences.save(c);
        check("mute is independent and retains slider level",e.musicVolume==0f && e.effectsVolume==.8f && c.preferences.music==.25f);
        GameCore loaded=new GameCore(m,205L);loaded.sound=e;loaded.startMusic();
        check("player preferences survive restart",loaded.preferences.music==.25f && loaded.preferences.effects==.8f
                && loaded.preferences.musicMuted && loaded.preferences.kids && e.musicVolume==0f);
        loaded.preferences.musicMuted=false;loaded.preferences.effectsMuted=true;loaded.preferences.save(loaded);
        check("unmute restores chosen volume without enabling other channel",e.musicVolume==.25f && e.effectsVolume==0f);
        c.preferences.musicMuted=true;
        SettingsInput.action(c,L,1000+PlayerSettings.MUSIC_MUTE);
        check("mute button restores saved volume",!c.preferences.musicMuted && e.musicVolume==.25f);
        int musicCalls=e.musicCalls;
        for(int oldTrack=100;oldTrack<=104;oldTrack++) SettingsInput.action(c,L,oldTrack);
        check("retired music actions cannot change playback or mute",e.musicCalls==musicCalls
                && !c.preferences.musicMuted && e.musicVolume==.25f);
        PlayerSettings.open(c);
        c.screenKey(0);
        check("settings block title keys",c.settingsOpen && !c.starting());
        check("Back starts settings exit",Pause.back(c) && c.preferences.panelClosing);
        c.update(PlayerSettings.PANEL_TIME,L);
        check("Back closes public settings",!c.settingsOpen);
        int[] actions={SettingsUi.HIT_GAMEOVER,SettingsUi.HIT_STAGE+2,SettingsUi.HIT_TEST,
                SettingsUi.HIT_TEST+SettingsUi.TEST_STARS,SettingsUi.HIT_TEST+SettingsUi.TEST_STEAMER,SettingsUi.HIT_DEBUFF,
                SettingsUi.HIT_TEST+SettingsUi.TEST_BAND,SettingsUi.HIT_TEST+SettingsUi.TEST_MINE};
        for(int state:new int[]{GameCore.TITLE,GameCore.OVER,GameCore.BONUS}) {
            c.state=state;int stage=c.stage;
            for(int h:actions) { SettingsInput.action(c,L,h);check("run-only action blocked in state "+state+" hit "+h,c.state==state && c.stage==stage && !c.powerActive()); }
        }
        for(boolean mine:new boolean[]{false,true}) {
            GameCore run=new GameCore(new Mem(),220L);run.startGame();run.jumpToStage(20,L);
            run.mining.carts=2;run.cart.progress=7;run.starNext=true;
            run.openSettings();run.update(PlayerSettings.PANEL_TIME,L);run.settingsPage=1;run.settingsTab=SettingsUi.MINIGAMES;
            SettingsUi ui=new SettingsUi();ui.compute(L,SettingsUi.MINIGAMES);
            int col=mine?1:0;
            float x=(ui.testChipL(col,2)+ui.testChipR(col,2))*.5f,y=ui.caveY+ui.testH*.5f;
            SettingsInput input=new SettingsInput();input.touch(run,L,0,1,x,y);input.touch(run,L,1,1,x,y);
            check("cave chip launches requested game "+mine,run.state==GameCore.BONUS && !run.settingsOpen
                    && run.cart.active==!mine && run.mining.active==mine && run.stage==(mine?22:21));
            check("cave shortcut clears boss and retains progress "+mine,!run.boss.active()
                    && run.enemies.isEmpty() && run.mining.carts==2 && run.cart.progress==7 && run.starNext);
            for(int tick=0;tick<4 && run.state==GameCore.BONUS;tick++)run.update(100,L);
            check("cave shortcut exits into next expedition "+mine,run.state==GameCore.PLAY
                    && run.stage==(mine?23:22) && !CaveInterlude.active(run));
        }
        c.state=GameCore.BONUS;c.stars.wins=3;c.steamer.opens=5;
        SettingsInput.action(c,L,SettingsUi.HIT_RESET_DIFFICULTY);
        SettingsInput.action(c,L,SettingsUi.HIT_HARDER);
        check("active minigame difficulty cannot be rewritten",c.stars.wins==3 && c.steamer.opens==5);
        c.state=GameCore.TITLE;c.preferences.kids=true;c.closeSettings();c.startGame();
        check("kids mode snapshots for the run and uses four keys",c.kidsRun && !c.runFullRoster);
        c.preferences.kids=false;c.preferences.save(c);
        check("changing mode waits for next run",c.kidsRun);
        for(int stage:new int[]{1,9,16,30}) {
            c.stage=stage;
            check("kids word complexity follows the stage and land press cap "+stage,
                    c.maxWordLen()==Pacing.maxWordLen(stage) && c.stackChance()==Pacing.stackChance(stage)
                    && c.maxPresses()==(Lands.forStage(stage)==Lands.RAINBOW?5:6) && c.maxEnemies()==3);
        }
        c.unlockRoster();check("kids run cannot promote to six keys",!c.runFullRoster);
        c.startGame();check("normal mode restores next run",!c.kidsRun && c.maxWordLen()==Pacing.maxWordLen(c.stage));
        for(int[] size:new int[][]{{320,568},{393,852},{640,960},{1080,2400}}) {
            Layout l=new Layout();l.compute(size[0],size[1],0,0,0,0);
            PlayerSettings.open(c);c.update(PlayerSettings.PANEL_TIME,l);
            float s=PlayerSettings.unit(l), r=PlayerSettings.knobR(c,l);
            check("sliding characters fit at both endpoints "+size[0],PlayerSettings.trackL(l)-r>=PlayerSettings.contentL(l)-.001f
                    && PlayerSettings.trackR(l)+r<=PlayerSettings.contentR(l)+.001f);
            check("audio handles clear labels and next rows "+size[0],PlayerSettings.row(l,0)+s*3-r>PlayerSettings.row(l,0)+s*.3f
                    && PlayerSettings.row(l,1)+s*3+r+s*.8f<PlayerSettings.row(l,2));
            check("mute and slider have distinct hit areas "+size[0],PlayerSettings.hit(c,l,PlayerSettings.right(l)-s*3,PlayerSettings.row(l,0))==PlayerSettings.MUSIC_MUTE
                    && PlayerSettings.hit(c,l,l.w*.5f,PlayerSettings.row(l,0)+s*3)==PlayerSettings.MUSIC);
            float skins=SkinSettings.buttonY(l);
            check("skin preview has clear space between kids and social cards "+size[0],
                    skins-s*2.25f>PlayerSettings.row(l,2)+s*1.1f
                    && skins+s*2.25f<PlayerSettings.socialY(l)-s*2.1f);
            check("whole skin card opens its picker "+size[0],
                    PlayerSettings.hit(c,l,l.w*.5f,skins-s*2.1f)==SkinSettings.OPEN
                    && PlayerSettings.hit(c,l,l.w*.5f,skins+s*2.1f)==SkinSettings.OPEN);
            check("gallery clears footer and settings bottom "+size[0],
                    SkinSettings.cardTop(l,1)+SkinSettings.cardHeight(l)<SkinSettings.backY(l)-s*1.3f
                    && SkinSettings.backY(l)+s*.8f<PlayerSettings.bottom(l));
            SettingsInput input=new SettingsInput();float y=PlayerSettings.row(l,1)+s*3;
            input.touch(c,l,0,7,PlayerSettings.trackL(l),y);
            input.touch(c,l,2,99,PlayerSettings.trackR(l),y);
            check("another finger cannot move volume",c.preferences.effects==0f);
            input.touch(c,l,2,7,PlayerSettings.trackR(l),y);
            input.touch(c,l,1,7,PlayerSettings.trackR(l),y);
            check("owning finger reaches full volume",c.preferences.effects==1f && !c.preferences.effectsMuted);
            input.touch(c,l,0,7,l.w*.5f,PlayerSettings.row(l,2));
            input.touch(c,l,2,7,l.w*.5f,PlayerSettings.row(l,3));
            check("dragging off a switch cannot trigger a link",!input.touch(c,l,1,7,l.w*.5f,PlayerSettings.row(l,3)));
        }
        GameCore preview=new GameCore(new Mem(),206L);Ear sample=new Ear();preview.sound=sample;
        PlayerSettings.open(preview);preview.update(PlayerSettings.PANEL_TIME,L);
        SettingsInput slider=new SettingsInput();
        float left=PlayerSettings.trackL(L),span=PlayerSettings.trackR(L)-left;
        float effectY=PlayerSettings.row(L,1)+PlayerSettings.unit(L)*3f;
        slider.touch(preview,L,0,1,left+span*.5f,effectY);
        check("effects slider previews slime squish at updated gain",sample.squishes==1
                && sample.lastGlyph==Kawaii.BLOB && sample.squishVolume==.5f);
        slider.touch(preview,L,2,1,left+span*.5f,effectY);
        check("stationary slider does not repeat preview",sample.squishes==1);
        slider.touch(preview,L,2,1,left+span*.75f,effectY);
        check("drag previews the new gain",sample.squishes==2 && sample.squishVolume==.75f);
        slider.touch(preview,L,2,1,left,effectY);
        check("zero effects volume stays silent",sample.squishes==2 && sample.effectsVolume==0f);
        preview.preferences.musicMuted=preview.preferences.effectsMuted=true;
        preview.preferences.save(preview);
        slider.touch(preview,L,2,1,left,effectY);
        check("unchanged slider leaves mute enabled",preview.preferences.effectsMuted && sample.squishes==2);
        slider.touch(preview,L,2,1,left+span*.5f,effectY);
        check("effects adjustment unmutes and previews at new volume",!preview.preferences.effectsMuted
                && preview.preferences.effects==.5f && sample.effectsVolume==.5f && sample.squishes==3
                && sample.squishVolume==.5f);
        check("effects adjustment preserves music mute",preview.preferences.musicMuted && sample.musicVolume==0f);
        GameCore restored=new GameCore(preview.store,207L);
        check("effects adjustment persists volume and mute",!restored.preferences.effectsMuted
                && restored.preferences.effects==.5f && restored.preferences.musicMuted);
        preview.preferences.effectsMuted=true;preview.preferences.save(preview);
        slider.cancel();
        float musicY=PlayerSettings.row(L,0)+PlayerSettings.unit(L)*3f;
        slider.touch(preview,L,0,1,left+span*.5f,musicY);
        check("music adjustment unmutes only music without effects preview",!preview.preferences.musicMuted
                && sample.musicVolume==.5f && preview.preferences.effectsMuted && sample.effectsVolume==0f
                && sample.squishes==3);
        restored=new GameCore(preview.store,208L);
        check("music adjustment persists volume and mute",!restored.preferences.musicMuted
                && restored.preferences.music==.5f && restored.preferences.effectsMuted);
        preview.preferences.musicMuted=true;preview.preferences.save(preview);
        slider.touch(preview,L,2,1,left,musicY);
        check("setting muted music to zero unmutes but stays silent",!preview.preferences.musicMuted
                && preview.preferences.music==0f && sample.musicVolume==0f);
        group("kids bounded play");
        float normalStages=0,kidsStages=0;int kidsSurvived=0;
        for(int seed=0;seed<4;seed++) for(int mode=0;mode<2;mode++) {
            GameCore run=new GameCore(new Mem(),500+seed);run.preferences.kids=mode==1;run.startGame();
            Bot.Result result=new Bot(2.5f,.4f,.15f,true,900+seed).play(run,L,900f);
            if(mode==0) normalStages+=result.stage;
            else { kidsStages+=result.stage;if(!result.died) kidsSurvived++; }
        }
        System.out.printf("    2.5 presses/s, .4s reaction, 15%% misses: normal stage %.1f, kids stage %.1f, kids survivors %d/4%n",normalStages/4,kidsStages/4,kidsSurvived);
        check("kids hands progress farther than normal",kidsStages>normalStages);
        check("kids hands can beat the first boss",kidsStages/4>=6f);
    }
}
