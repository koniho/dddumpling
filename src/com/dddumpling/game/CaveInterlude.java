package com.dddumpling.game;

/** Cave games alternate by stage without changing the ordinary interlude's saved alternation. */
final class CaveInterlude {
    static void playtest(GameCore c,Layout L,boolean mining) {
        if(!BuildFlags.DEVELOPER || c.state!=GameCore.PLAY || c.pendingBonus || c.starting())return;
        c.closeSettings();
        // Stage entry clears boss, frenzy and gestures before the real interlude opens.
        c.jumpToStage(mining?22:21,L);
        c.bossPrizePending=false;
        c.spawnedThisStage=c.resolvedThisStage=c.stageQuota();
        Interlude.enterBonus(c,L);
    }
    static boolean active(GameCore c) {return c.band.active || c.mining.active;}
    static boolean miningStage(int stage) {return Cave.stage(stage) && (stage-21)%2!=0;}
    static boolean enter(GameCore c) {
        if(!Cave.stage(c.stage))return false;
        c.band.stop(c);c.mining.stop();
        c.starBonus=false;c.bossReward=false;c.paradeTimer=0;c.bonusTimer=1;
        c.target=c.caretOwner=null;c.power=null;c.stageByPower=false;
        if(miningStage(c.stage)){c.mining.begin(c);c.startMusic();}
        else c.band.begin(c);
        return true;
    }
    static boolean update(GameCore c,float elapsed) {
        if(c.mining.active)return mining(c,elapsed);
        CaveBand b=c.band;
        b.update(c,elapsed);
        if(b.finished && !b.paid) {
            b.paid=true;
            if(b.won) {
                c.score+=GameCore.FREE_BONUS;
                if(c.lives<GameCore.START_LIVES)c.lives++;
                Interlude.awardBandPrize(c);
                if(c.sound!=null)c.sound.achievement();
            } else if(c.sound!=null)c.sound.tally(Math.min(20,b.charge/2));
        }
        c.bonusTimer=b.finished?b.report:1f;
        if(!b.finished || b.report>0 || parade(c,elapsed))return false;
        b.stop(c);
        return true;
    }
    private static boolean mining(GameCore c,float dt) {
        CaveMining m=c.mining;m.update(c,dt);
        if(m.phase==CaveMining.REPORT && !m.paid) {
            m.paid=true;
            if(m.won) {
                c.score+=GameCore.FREE_BONUS;
                if(c.lives<GameCore.START_LIVES)c.lives++;
                Interlude.awardMiningPrize(c);
                m.progress=0;m.save(c);
                if(c.sound!=null)c.sound.achievement();
            } else if(c.sound!=null)c.sound.tally(m.progress);
        }
        c.bonusTimer=m.phase==CaveMining.REPORT?m.report:1;
        if(m.phase!=CaveMining.REPORT || m.report>0 || parade(c,dt))return false;
        m.stop();return true;
    }
    private static boolean parade(GameCore c,float dt) {
        if(c.paradeTimer<=0)return false;
        c.paradeTimer=Math.max(0,c.paradeTimer-dt);Interlude.joinChord(c);
        return c.paradeTimer>0;
    }
    static void press(GameCore c,int g,float inputAge) {
        if(c.mining.active){c.mining.press(c,g);return;}
        if(!Roster.active(c.playRosterFull(),g))return;
        int note=c.band.press(c,g,inputAge);
        c.keyPress[g]=1;
        if(note<0)c.keyBad[g]=1;
        else if(c.sound!=null)c.sound.bandNote(c.band.song,note);
    }
}
