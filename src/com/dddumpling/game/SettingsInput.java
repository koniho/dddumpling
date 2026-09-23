package com.dddumpling.game;

/** One gesture owner and action router for both native settings panels. */
final class SettingsInput {
    private int pressed, pointer=-1;
    private float downX, downY, offset;
    private boolean moved;
    void cancel() { pressed=0; pointer=-1; }
    static boolean runAction(int hit) {
        return hit==SettingsUi.HIT_GAMEOVER || hit>=SettingsUi.HIT_TEST;
    }
    static boolean enabled(GameCore c,int hit) {
        if(c.state==GameCore.BONUS && (hit==SettingsUi.HIT_EASIER || hit==SettingsUi.HIT_HARDER
                || hit==SettingsUi.HIT_RESET_DIFFICULTY)) return false;
        return !runAction(hit) || (c.state==GameCore.PLAY && !c.pendingBonus && !c.starting());
    }
    private static boolean slider(int hit) {
        return hit==1000+PlayerSettings.MUSIC || hit==1000+PlayerSettings.EFFECTS;
    }
    private int hit(GameCore c,Layout L,float x,float y) {
        if(c.highScoreScreen.open) return c.highScoreScreen.hit(c,L,x,y);
        int player=PlayerSettings.hit(c,L,x,y);
        if(player!=0) return 1000+player;
        if(c.settingsPage!=1 || !BuildFlags.DEVELOPER) return 0;
        SettingsUi u=new SettingsUi();u.compute(L,c.settingsTab);
        int h=u.hit(x,y);return enabled(c,h)?h:0;
    }
    // Android and IOSTouch share action numbers. Only the owning finger may drag or release.
    boolean touch(GameCore c,Layout L,int action,int id,float x,float y) {
        if(action==3 || (c.settingsOpen && c.preferences.panelMoving())) { cancel();return false; }
        if(action==0) {
            pointer=id; pressed=hit(c,L,x,y); downX=x;downY=y;moved=false;offset=0;
            if(pressed==1000+PlayerSettings.MUSIC || pressed==1000+PlayerSettings.EFFECTS) {
                float v=pressed==1000+PlayerSettings.MUSIC?c.preferences.music:c.preferences.effects;
                float knob=PlayerSettings.trackL(L)+(PlayerSettings.trackR(L)-PlayerSettings.trackL(L))*v;
                if(Math.abs(x-knob)<=L.keyR*c.keyScale()) offset=knob-x;
            }
            if(slider(pressed)) drag(c,L,x+offset);
        } else if(id==pointer && action==2) {
            if(Math.abs(x-downX)+Math.abs(y-downY)>L.unit*.4f) moved=true;
            if(slider(pressed)) drag(c,L,x+offset);
        } else if(id==pointer && (action==1 || action==6)) {
            int h=pressed;cancel();
            if(!moved && h!=0 && h==hit(c,L,x,y) && !slider(h)) return action(c,L,h);
        }
        return false;
    }
    private void drag(GameCore c,Layout L,float x) {
        float v=PlayerSettings.volumeAt(L,x);
        if(pressed==1000+PlayerSettings.MUSIC) {
            if(c.preferences.music==v) return;
            c.preferences.music=v;
        } else {
            if(c.preferences.effects==v) return;
            c.preferences.effects=v;
        }
        c.preferences.save(c);
        if(pressed==1000+PlayerSettings.EFFECTS && !c.preferences.effectsMuted && v>0f && c.sound!=null)
            c.sound.squish(Kawaii.BLOB,0);
    }
    static boolean action(GameCore c,Layout L,int h) {
        if(c.highScoreScreen.open) { c.highScoreScreen.action(c,h);return false; }
        if(h>=1000) {
            switch(h-1000) {
                case PlayerSettings.CLOSE: PlayerSettings.close(c);break;
                case PlayerSettings.PLAYER: c.settingsPage=0;c.clearArmed=false;break;
                case PlayerSettings.DEVELOPER: if(BuildFlags.DEVELOPER) { c.settingsPage=1;c.clearArmed=false; } break;
                case PlayerSettings.MUSIC_MUTE:
                    c.preferences.musicMuted=!c.preferences.musicMuted;
                    c.preferences.save(c);break;
                case PlayerSettings.EFFECTS_MUTE: c.preferences.effectsMuted=!c.preferences.effectsMuted;c.preferences.save(c);break;
                case PlayerSettings.KIDS: c.preferences.toggleKids(c);break;
                case PlayerSettings.SHARE:
                case PlayerSettings.RATE: c.preferences.requestExternal(c,h-1000);break;
                case PlayerSettings.PRIVACY: return true;
                default: break;
            }
            return false;
        }
        if(!BuildFlags.DEVELOPER || !enabled(c,h)) return false;
        if(h!=SettingsUi.HIT_CLEAR) c.clearArmed=false;
        if(h==SettingsUi.HIT_CLOSE || h==SettingsUi.HIT_OUTSIDE) PlayerSettings.close(c);
        else if(h==SettingsUi.HIT_GENERAL) c.settingsTab=SettingsUi.GENERAL;
        else if(h==SettingsUi.HIT_MINIGAMES) c.settingsTab=SettingsUi.MINIGAMES;
        else if(h==SettingsUi.HIT_POWERS) c.settingsTab=SettingsUi.POWERS;
        else if(h==SettingsUi.HIT_PROGRESS) c.settingsTab=SettingsUi.PROGRESS;
        else if(h==SettingsUi.HIT_EASIER || h==SettingsUi.HIT_HARDER) c.setStarDifficulty(c.stars.wins+(h==SettingsUi.HIT_EASIER?-1:1));
        else if(h==SettingsUi.HIT_CLEAR) c.tapClearCase();
        else if(h==SettingsUi.HIT_ROSTER) c.setNextRoster(!c.fullRoster);
        else if(h==SettingsUi.HIT_GAMEOVER) c.endCurrentRun();
        else if(h==SettingsUi.HIT_RESET_SWIPE) {
            c.pushLesson.seen=false;
            if(c.store!=null) c.store.savePushLessonSeen(false);
        }
        else if(h==SettingsUi.HIT_RESET_NEWS) c.releaseMascot.reset(c);
        else if(h==SettingsUi.HIT_ALL_LANDS) LandPicker.enableAll(c);
        else if(h==SettingsUi.HIT_RESET_LANDS) LandPicker.reset(c);
        else if(h==SettingsUi.HIT_RESET_DIFFICULTY) c.resetDifficultyScaling();
        else if(h>=SettingsUi.HIT_STAGE && h<SettingsUi.HIT_STAGE+SettingsUi.STAGE_STEP.length)
            c.jumpToStage(c.stage+SettingsUi.STAGE_STEP[h-SettingsUi.HIT_STAGE],L);
        else if(h>=SettingsUi.HIT_DEBUFF && h<SettingsUi.HIT_DEBUFF+2) c.playtestDebuff(Power.INCOGNITO+h-SettingsUi.HIT_DEBUFF);
        else if(h>=SettingsUi.HIT_TEST && h<SettingsUi.HIT_TEST+SettingsUi.TEST_CHIPS) {
            int i=h-SettingsUi.HIT_TEST;
            if(i==SettingsUi.TEST_STARS) c.playtestStars(L);
            else if(i==SettingsUi.TEST_STEAMER) c.playtestSteamer(L);
            else if(i==SettingsUi.TEST_BAND || i==SettingsUi.TEST_MINE)
                CaveInterlude.playtest(c,L,i==SettingsUi.TEST_MINE);
            else c.playtestMode(Power.offeredAt(i),L);
        }
        return false;
    }
}
