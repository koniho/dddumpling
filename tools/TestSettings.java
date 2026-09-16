package com.dddumpling.game;

final class TestSettings extends Check {
    static void all(Layout L) {
        group("player settings");
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
        c.preferences.musicMuted=false;c.setBgm(Music.OFF);
        SettingsInput.action(c,L,1000+PlayerSettings.MUSIC_MUTE);
        check("one unmute restores music from the old OFF track",c.bgmChoice==Music.defaultChoice(false) && !c.preferences.musicMuted);
        PlayerSettings.open(c);
        c.screenKey(0);
        check("settings block title keys",c.settingsOpen && !c.starting());
        check("Back closes public settings",Pause.back(c) && !c.settingsOpen);
        int[] actions={SettingsUi.HIT_GAMEOVER,SettingsUi.HIT_STAGE+2,SettingsUi.HIT_TEST,
                SettingsUi.HIT_TEST+SettingsUi.TEST_STARS,SettingsUi.HIT_TEST+SettingsUi.TEST_STEAMER,SettingsUi.HIT_DEBUFF};
        for(int state:new int[]{GameCore.TITLE,GameCore.OVER,GameCore.BONUS}) {
            c.state=state;int stage=c.stage;
            for(int h:actions) { SettingsInput.action(c,L,h);check("run-only action blocked in state "+state+" hit "+h,c.state==state && c.stage==stage && !c.powerActive()); }
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
            check("kids words stay short and unstacked at stage "+stage,c.maxWordLen()==2 && c.stackChance()==0f && c.maxEnemies()==3);
        }
        c.unlockRoster();check("kids run cannot promote to six keys",!c.runFullRoster);
        c.startGame();check("normal mode restores next run",!c.kidsRun && c.maxWordLen()==Pacing.maxWordLen(c.stage));
        for(int[] size:new int[][]{{320,568},{393,852},{640,960},{1080,2400}}) {
            Layout l=new Layout();l.compute(size[0],size[1],0,0,0,0);
            PlayerSettings.open(c);
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
