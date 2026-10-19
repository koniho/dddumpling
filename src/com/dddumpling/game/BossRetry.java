package com.dddumpling.game;

/** An Adventure loss can continue the same run before its result is committed. */
final class BossRetry extends Draw {
    boolean pending;
    private int stage=-1,score;
    void clear() {pending=false;stage=-1;score=0;}
    void checkpoint(GameCore c) {
        stage=!c.survival.active && !c.timeAttack.active && c.boss.active()?c.stage:-1;
        score=c.score;
    }
    boolean offer(GameCore c) {
        pending=stage==c.stage && c.boss.fighting() && !c.survival.active && !c.timeAttack.active;
        return pending;
    }
    void finish(GameCore c) {
        if(!pending)return;
        pending=false;c.finishDefeat();
    }
    void end(GameCore c) {
        if(!pending || !c.overReady())return;
        finish(c);c.time=c.deathDuration()+GameCore.OVER_FADE;
        if(c.sound!=null)c.sound.uiBloop();
    }
    void retry(GameCore c) {
        if(!pending || !c.overReady() || c.returnFade>0)return;
        pending=false;
        if(c.highScores.bossRetries<Integer.MAX_VALUE)c.highScores.bossRetries++;
        c.score=score;c.lives=GameCore.START_LIVES;c.combo=0;
        Pause.release(c);c.onboarding.clear();
        c.state=GameCore.PLAY;c.time=0;c.deathT=c.returnFade=0;
        c.bossVictory=null;c.bossVictoryKind=-1;
        c.enemies.clear();c.shots.clear();c.particles.clear();c.pushImpacts.clear();
        c.target=c.caretOwner=null;c.buddy.leave();
        c.pendingBonus=c.bossPrizePending=c.bossReward=false;
        c.perfectBanner=c.paradeTimer=c.bonusTimer=0;
        c.power=null;c.mode=-1;c.modeLeft=0;c.powerReplacements.clear();Blade.resetFeedback(c);
        c.pushT=c.pushSlowT=c.slowdown=c.chainT=c.sliceCall=c.warnLevel=0;
        c.shake=c.flash=c.skyGlow=0;c.bossDeathHaptic=0;
        c.missesThisStage=c.hurtThisStage=0;c.earnedMash=GameCore.MASH_HURT;
        c.companion.begin(c.runWho);
        c.enterStage(stage);
        if(c.sound!=null){c.sound.uiBloop();c.sound.frenzy(false);}
    }
    static float buttonY(Layout L,boolean retry){return L.h*(retry?.56f:.68f);}
    void tap(GameCore c,Layout L,float x,float y) {
        if(!pending || !c.overReady() || x<L.w*.15f || x>L.w*.85f)return;
        if(Math.abs(y-buttonY(L,true))<L.h*.047f)retry(c);
        else if(Math.abs(y-buttonY(L,false))<L.h*.047f)end(c);
    }
    void draw(Painter p,GameCore c,Layout L,float fade) {
        Painter q=new OpacityPainter(p,fade);float s=L.unit,cx=L.w*.5f;
        glassPanel(q,L.w*.09f,L.h*.19f,L.w*.91f,L.h*.79f,s);
        q.text("TRY AGAIN?",cx,L.h*.265f,type(s*1.35f),GOLD,Painter.CENTER,true);
        String name=Skins.bossName(p.skin(),c.bossVictoryKind);
        q.text(name,cx,L.h*.322f,Math.min(type(s*.78f),L.w*.72f/(name.length()*.73f)),INK,Painter.CENTER,true);
        TimeAttackDemo.drawRetry(q,c,L,L.h*.423f);
        float y=buttonY(L,true),half=L.h*.047f,text=type(s*1.15f);
        q.fillRect(L.w*.15f,y-half,L.w*.85f,y+half,0x334DCEAA);
        q.text("RETRY",cx,y+text*.35f,text,INK,Painter.CENTER,true);
        q.text("END RUN",cx,buttonY(L,false)+text*.35f,text,INK,Painter.CENTER,true);
        if(c.highScores.bossRetries>0)q.text("BOSS RETRIES: "+c.highScores.bossRetries,cx,L.h*.775f,type(s*.48f),GOLD,Painter.CENTER,true);
    }
}
