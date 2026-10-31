package com.dddumpling.game;

/** Rotation, reward and run lifecycle around the shared packing puzzle. */
final class SquishyFitInterlude {
    static boolean enter(GameCore c,Layout L) {
        if(!c.squishyFit.next)return false;
        c.starBonus=false;c.bossReward=false;c.paradeTimer=0;c.bonusTimer=1;
        c.target=c.caretOwner=null;c.power=null;c.stageByPower=false;
        c.squishyFit.begin(c,L);c.progress.startMinigame("squishy_fit");
        if(c.sound!=null)c.sound.stageClear();
        return true;
    }
    static boolean update(GameCore c,Layout L,float dt,float elapsed) {
        SquishyFit m=c.squishyFit;m.update(dt,elapsed);
        if(m.finished && !m.paid) {
            m.paid=true;c.progress.finishMinigame(m.won);
            if(m.won) {
                c.score+=GameCore.FREE_BONUS;if(c.lives<GameCore.START_LIVES)c.lives++;
                Interlude.awardFitPrize(c);
                c.companion.react(RunCompanion.VICTORY,1);
                if(c.sound!=null)c.sound.achievement();
            }else if(c.sound!=null)c.sound.tally(Math.round(m.coverage*100));
        }
        c.bonusTimer=m.finished?m.report:1;
        if(!m.finished || m.report>0)return false;
        if(c.paradeTimer>0) {
            c.paradeTimer=Math.max(0,c.paradeTimer-elapsed);Interlude.joinChord(c);
            if(c.paradeTimer>0)return false;
        }
        if(m.won){m.completed++;m.pieces.clear();m.coverage=0;m.next=false;c.starNext=false;}
        m.stop();return true;
    }
    static void playtest(GameCore c,Layout L) {
        if(!BuildFlags.DEVELOPER || c.state!=GameCore.PLAY || c.pendingBonus)return;
        c.closeSettings();
        c.jumpToStage(c.boss.active()?Math.max(1,c.stage-1):c.stage,L);
        c.bossPrizePending=false;c.cave.running=false;
        c.spawnedThisStage=c.resolvedThisStage=c.stageQuota();
        c.earnedMash=GameCore.MASH_PERFECT;c.starNext=false;c.squishyFit.next=true;
        Interlude.enterBonus(c,L);
    }
}
