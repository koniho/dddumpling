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
        check("kids slows player projectile flight",Math.abs(shot.t-.045f)<.001f);
        for(int kind=0;kind<Boss.COUNT;kind++) {
            c.boss.begin(kind,5,c.rnd);float intro=c.boss.intro;
            c.update(.1f,L);
            check("kids preserves boss introduction "+kind,Math.abs(c.boss.intro-intro+.1f)<.001f);
            c.boss.intro=0;c.boss.blive[0]=true;c.boss.bt[0]=0;
            float age=c.boss.age;c.update(.1f,L);
            check("kids preserves boss action clock but slows bolts "+kind,
                    Math.abs(c.boss.age-age-.1f)<.001f
                    && Math.abs(c.boss.bt[0]-.045f/Boss.BOLT_TIME)<.001f);
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
        for(boolean panic:new boolean[]{false,true}) for(int hurt:new int[]{0,1}) for(int misses:new int[]{0,1}) {
            c.pushUsed=panic;c.hurtThisStage=hurt;c.missesThisStage=misses;c.earnedMash=1f;
            c.starNext=false;Interlude.enterBonus(c,L);
            check("kids always earns five mash seconds",c.mashEarned()==5f && c.bonusRollEnd-GameCore.MASH_END==5f);
        }
        c.starNext=true;Interlude.enterBonus(c,L);
        StarPath cap=new StarPath();cap.wins=StarPath.KIDS_DIFFICULTY;
        c.stars.reroll(new java.util.Random(584L));cap.make(new java.util.Random(584L));
        check("kids course uses thirty percent difficulty at saved maximum",
                c.stars.bendRate()==cap.bendRate() && java.util.Arrays.equals(c.stars.sx,cap.sx));
        check("kids cap preserves saved normal difficulty",c.stars.wins==StarPath.MAX_DIFFICULTY
                && store.starWins==StarPath.MAX_DIFFICULTY);
        c.stars.wins=1;
        check("kids cap preserves easier courses",Math.abs(c.stars.bendRate()-1.6f)<.001f);
        c.stars.wins=StarPath.KIDS_DIFFICULTY;c.stars.recordWin();
        check("kids wins cannot exceed effective difficulty cap",c.stars.bendRate()==cap.bendRate());
        c.preferences.kids=false;c.startGame();c.stars.wins=StarPath.MAX_DIFFICULTY;
        check("normal run restores full star difficulty",Math.abs(c.stars.bendRate()-7f)<.001f);
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
    static void all(Layout L) {
        panelTiming(L);
        kidsToggle(L);
        transitions(L);
        social(L);
        kidsTiming(L);
        kidsMinigames(L);
        group("player settings");
        GameCore title=new GameCore(new Mem(),203L);
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
            check("kids word complexity follows the stage with a six-press cap "+stage,
                    c.maxWordLen()==Pacing.maxWordLen(stage) && c.stackChance()==Pacing.stackChance(stage)
                    && c.maxPresses()==6 && c.maxEnemies()==3);
        }
        c.unlockRoster();check("kids run cannot promote to six keys",!c.runFullRoster);
        c.startGame();check("normal mode restores next run",!c.kidsRun && c.maxWordLen()==Pacing.maxWordLen(c.stage));
        for(int[] size:new int[][]{{320,568},{393,852},{640,960},{1080,2400}}) {
            Layout l=new Layout();l.compute(size[0],size[1],0,0,0,0);
            PlayerSettings.open(c);c.update(PlayerSettings.PANEL_TIME,l);
            float s=PlayerSettings.unit(l), r=l.keyR;
            check("sliding characters fit at both endpoints "+size[0],PlayerSettings.trackL(l)-r>=PlayerSettings.left(l)
                    && PlayerSettings.trackR(l)+r<=PlayerSettings.right(l));
            check("audio handles clear labels and next rows "+size[0],PlayerSettings.row(l,0)+s*3-r>PlayerSettings.row(l,0)+s*.3f
                    && PlayerSettings.row(l,1)+s*3+r+s*.8f<PlayerSettings.row(l,2));
            check("mute and slider have distinct hit areas "+size[0],PlayerSettings.hit(c,l,PlayerSettings.right(l)-s*3,PlayerSettings.row(l,0))==PlayerSettings.MUSIC_MUTE
                    && PlayerSettings.hit(c,l,l.w*.5f,PlayerSettings.row(l,0)+s*3)==PlayerSettings.MUSIC);
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
        preview.preferences.effectsMuted=true;
        slider.touch(preview,L,2,1,left+span*.5f,effectY);
        check("muted effects retain level without preview",sample.squishes==2 && preview.preferences.effects==.5f && sample.effectsVolume==0f);
        slider.cancel();
        slider.touch(preview,L,0,1,left+span*.5f,PlayerSettings.row(L,0)+PlayerSettings.unit(L)*3f);
        check("music slider does not preview effects",sample.squishes==2);
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
