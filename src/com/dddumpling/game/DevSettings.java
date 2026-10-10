package com.dddumpling.game;

/** Related developer controls stay on one page, behind the public settings tab. */
final class DevSettings extends Draw {
    static void chip(Painter p, SettingsUi u, float y, int i, int n, String text, float s, boolean on) {
        float l=u.testChipL(i,n),r=u.testChipR(i,n);
        p.fillRect(l,y,r,y+u.testH,on?0x554DCEAA:0x22FFFFFF);
        p.text(text,(l+r)*.5f,y+u.testH*.66f,s*.57f,INK,Painter.CENTER,true);
    }
    static void label(Painter p,SettingsUi u,float y,String text,float s) {
        p.text(text,u.optionL(),y,s*.62f,INK_DIM,Painter.LEFT,true);
    }
    static void draw(Painter p,GameCore c,Layout L) {
        if(!BuildFlags.DEVELOPER) return;
        float s=PlayerSettings.unit(L); SettingsUi u=new SettingsUi();
        u.compute(L,c.settingsTab);
        for(int i=0;i<4;i++) {
            p.fillRect(u.tabL(i),u.tabY,u.tabR(i),u.tabY+u.tabH,i==c.settingsTab?0x554DCEAA:0x18FFFFFF);
            p.text(SettingsUi.TABS[i],(u.tabL(i)+u.tabR(i))*.5f,u.titleY,s*.52f,INK,Painter.CENTER,true);
        }
        boolean running=c.state==GameCore.PLAY && !c.pendingBonus;
        Painter actions=running?p:new OpacityPainter(p,.3f);
        if(!running && c.settingsTab!=SettingsUi.PROGRESS) p.text(c.state==GameCore.BONUS?"FINISH THE MINIGAME FIRST":"START A RUN TO PLAYTEST",
                L.w*.5f,u.panelT+s*8.2f,s*.50f,INK_DIM,Painter.CENTER,false);
        if(c.settingsTab==SettingsUi.MINIGAMES) {
            label(p,u,u.difficultyStepLabelY,"STAR PATH DIFFICULTY",s);
            Painter difficulty=c.state==GameCore.BONUS?new OpacityPainter(p,.3f):p;
            chip(difficulty,u,u.difficultyStepY,0,3,"EASIER",s,false);
            chip(p,u,u.difficultyStepY,1,3,(c.stars.wins+1)+" / "+(StarPath.MAX_DIFFICULTY+1),s,true);
            chip(difficulty,u,u.difficultyStepY,2,3,"HARDER",s,false);
            label(p,u,u.testLabelY,"PLAY MINIGAME",s);
            chip(actions,u,u.testY,0,2,"STAR PATH",s,false);
            chip(actions,u,u.testY,1,2,"STEAMER",s,false);
            chip(actions,u,u.caveY,0,2,"CART RUSH",s,false);
            chip(actions,u,u.caveY,1,2,"DUMPLING MINE",s,false);
            label(p,u,u.difficultyLabelY,"MINIGAME DIFFICULTY",s);
            chip(difficulty,u,u.difficultyY,0,1,"RESET MINIGAME DIFFICULTY",s,false);
            label(p,u,u.clearLabelY,"STEAMER TARGET: "+c.steamer.goal(),s);
            return;
        }
        if(c.settingsTab==SettingsUi.POWERS) {
            label(p,u,u.testLabelY,"PLAY FRENZY",s);
            for(int i=0;i<Power.OFFERED.length;i++) chip(actions,u,u.testY,i,Power.OFFERED.length,Power.CHIP[Power.offeredAt(i)],s,false);
            label(p,u,u.debuffY-s*.6f,"PLAY DEBUFF",s);
            for(int i=0;i<2;i++) chip(actions,u,u.debuffY,i,2,Power.NAMES[Power.INCOGNITO+i],s,false);
            return;
        }
        if(c.settingsTab==SettingsUi.PROGRESS) {
            label(p,u,u.debuffY-s*.6f,"LAND DISCOVERY",s);
            chip(p,u,u.debuffY,0,2,"ALL LANDS",s,false);
            chip(p,u,u.debuffY,1,2,"RESET LANDS",s,false);
            label(p,u,u.difficultyLabelY,"RELEASE BOOK",s);
            chip(p,u,u.difficultyY,0,1,"RESET NEWS",s,false);
            label(p,u,u.clearLabelY,"COLLECTION: "+Collect.owned(c.collected)+" / "+Collect.COUNT,s);
            chip(p,u,u.clearY,0,1,c.clearArmed?"TAP AGAIN TO EMPTY":"EMPTY COLLECTION",s,c.clearArmed);
            chip(p,u,u.collectAllY,0,1,"COLLECT ALL",s,Collect.complete(c.collected));
            chip(p,u,u.resetProgressY,0,1,c.resetProgressArmed?"TAP AGAIN: START FRESH":"CLEAR ALL PROGRESS",s,c.resetProgressArmed);
            return;
        }
        label(p,u,u.stageLabelY,"STAGE "+c.stage,s);
        for(int i=0;i<4;i++) {
            int step=SettingsUi.STAGE_STEP[i];
            chip(actions,u,u.stageY,i,4,(step<0?"BACK ":"NEXT ")+Math.abs(step),s,false);
        }
        label(p,u,u.runLabelY,"RUN CONTROLS",s);
        chip(p,u,u.runY,0,2,c.fullRoster?"NEXT RUN: 6 KEYS":"NEXT RUN: 4 KEYS",s,false);
        chip(actions,u,u.runY,1,2,"END RUN",s,false);
    }
    static void resetProgress(GameCore c) {
        if(!BuildFlags.DEVELOPER)return;
        if(!c.resetProgressArmed) {c.resetProgressArmed=true;return;}
        // Finish live activities before clearing their saved rewards and checkpoints.
        c.toTitle();
        if(!c.resetHighScores()) {c.resetProgressArmed=false;return;}
        c.closeSettings();c.boss.leave();c.starter.clear();
        c.progress.resetForDeveloper();
        c.clearArmed=true;CaseUi.tapClear(c);
        c.resetDifficultyScaling();
        c.onboarding.reset(c);
        LandPicker.reset(c);c.landSuppressed=0;LandPicker.save(c);
        c.modes.restore(ModeSelector.ADVENTURE);c.preferences.kids=false;c.preferences.save(c);
        c.fullRoster=c.runFullRoster=c.rosterLeavePending=false;
        c.earlyLosses=c.rosterScene=0;c.rosterSceneT=0;
        c.caveChoice=-1;c.caveMiningNext=false;c.cart.progress=c.mining.carts=0;
        c.town.leave();c.town.load(new Town().save());c.townOpen=false;
        c.townRunId=0;c.townRunTickets=0;
        c.survival.reward.sequence=0;c.survival.reward.who=-1;
        c.survival.reward.joining=c.survival.reward.joinRung=false;
        c.runWho=0;c.pickerT=0;
        c.stage=1;c.score=0;c.lives=GameCore.START_LIVES;c.runStartLand=0;
        c.pendingBonus=false;c.returnFade=0;c.launchFromStarter=false;
        c.power=null;c.mode=-1;c.modeLeft=0;c.buddy.leave();
        c.debuffLeft=c.monochromeFade=c.incognitoMorph=0;
        c.flash=c.skyGlow=c.shake=c.bossDeathHaptic=c.starBlastHaptic=0;
        c.bossPrizePending=c.bossReward=false;c.prizeNew=false;
        c.steamer.reset();c.bonusTimer=c.paradeTimer=0;
        c.particles.clear();c.pushImpacts.clear();
        c.releaseNotes.close();c.releaseMascot.read(c);
        if(c.store!=null) {
            c.store.saveCaseIndex(0);c.store.saveRosterState(0);
            c.store.saveCaveChoice(-1);c.store.saveCaveMiningNext(false);
            c.store.saveCartTrack(0);c.store.saveMineCarts(0);
            c.store.saveTown(c.town.save());
        }
        c.progress.seed(c);
        c.startMusic();
    }

}
