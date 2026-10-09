package com.dddumpling.game;

/** Fixed boss replays. Only playable combat contributes to these local records. */
final class TimeAttack extends Draw {
    static final int ALL=Boss.COUNT, CHOICES=Boss.COUNT+1;
    final long[][] best=new long[3][CHOICES];
    int selected, challenge, current, profile, cleared;
    boolean active, finished, won, newBest;
    double seconds;

    static String name(int choice) {return choice==ALL?"ALL BOSSES":Boss.NAMES[choice];}
    static boolean unlocked(GameCore c,int choice) {
        return choice==ALL?ModeSelector.allBossesUnlocked(c):ModeSelector.bossUnlocked(c,choice);
    }
    boolean playable(GameCore c) {return unlocked(c,selected);}
    void choose(GameCore c,int direction) {
        selected=(selected+direction+CHOICES)%CHOICES;
        c.modes.confirmation=1;
        if(c.sound!=null)c.sound.uiBloop();
    }
    void focusUnlocked(GameCore c) {
        if(playable(c))return;
        for(int i=0;i<Boss.COUNT;i++)if(unlocked(c,i)) {selected=i;return;}
    }
    long millis() {return Math.max(1,Math.round(seconds*1000));}
    static String time(long millis) {
        if(millis<=0)return "--";
        return Survival.time(millis)+"."+String.format(java.util.Locale.ROOT,"%02d",millis/10%100);
    }
    String title(GameCore c) {
        return "BEST "+time(best[Survival.profile(c.preferences.kids,c.fullRoster)][selected]);
    }
    void begin(GameCore c) {
        active=true;finished=won=newBest=false;seconds=0;cleared=0;
        challenge=selected;profile=Survival.profile(c.kidsRun,c.runFullRoster);
        encounter(c,challenge==ALL?0:challenge);
    }
    private void encounter(GameCore c,int which) {
        current=which;c.stage=(which+1)*Boss.EVERY;
        c.enemies.clear();c.shots.clear();c.target=null;c.power=null;
        c.pendingBonus=c.bossReward=c.bossPrizePending=false;
        c.stageGap=c.stageBanner=0;c.spawnedThisStage=c.resolvedThisStage=0;
        c.pushUsed=false;c.pushT=c.pushSlowT=0;
        c.boss.begin(which,c.stage,c.rnd,c.playRosterFull());
        if(c.sound!=null)c.sound.bossMusic(true);
    }
    void tick(GameCore c,float elapsed) {
        if(!active || finished || c.state!=GameCore.PLAY || !c.boss.fighting()
                || c.paused || c.settingsOpen || elapsed<=0 || !Float.isFinite(elapsed))return;
        seconds+=elapsed;
    }
    void bossEnded(GameCore c,boolean beaten) {
        c.boss.leave();
        if(!beaten) {finish(c,false);return;}
        cleared++;
        if(challenge==ALL && current+1<Boss.COUNT) {
            c.lives=Math.min(GameCore.START_LIVES,c.lives+1);
            encounter(c,current+1);return;
        }
        finish(c,true);
        c.state=GameCore.OVER;c.deathT=0;c.time=c.deathDuration();
        c.companion.react(RunCompanion.BOSS_HIT,1f);
        c.shots.clear();c.target=null;
        c.pushT=c.pushSlowT=c.shake=c.flash=0;
        if(c.sound!=null)c.sound.collect(4);
    }
    void finish(GameCore c,boolean success) {
        if(!active || finished)return;
        finished=true;won=success;
        if(!success || c.scoresSuppressed)return;
        long duration=millis();
        newBest=best[profile][challenge]==0 || duration<best[profile][challenge];
        if(newBest)best[profile][challenge]=duration;
        if(c.store!=null)c.store.saveTimeAttack(encode());
    }
    void leave() {active=finished=won=newBest=false;seconds=0;cleared=0;current=0;}
    void clearRecords() {for(long[] row:best)java.util.Arrays.fill(row,0);newBest=false;}
    String encode() {
        StringBuilder out=new StringBuilder("1");
        for(long[] row:best)for(long value:row)out.append(';').append(value);
        return out.toString();
    }
    void load(String saved) {
        if(saved==null)return;
        String[] fields=saved.split(";",-1);
        if(fields.length!=1+3*CHOICES || !fields[0].equals("1"))return;
        long[][] parsed=new long[3][CHOICES];
        try {
            for(int p=0;p<3;p++)for(int i=0;i<CHOICES;i++) {
                long value=Long.parseLong(fields[1+p*CHOICES+i]);
                if(value<0 || value>86400000L)return;
                parsed[p][i]=value;
            }
        } catch(NumberFormatException invalid) {return;}
        for(int p=0;p<3;p++)System.arraycopy(parsed[p],0,best[p],0,CHOICES);
    }
    static float selectY(Layout L) {return L.h*.735f;}
    int hit(GameCore c,Layout L,float x,float y) {
        if(c.modes.selected!=ModeSelector.TIME_ATTACK || !c.modes.visible(c)
                || Math.abs(y-selectY(L))>L.unit*1.3f)return 0;
        return x<L.w*.27f?4:x>L.w*.73f?6:5;
    }
    void drawSelector(Painter p,GameCore c,Layout L) {
        if(c.modes.selected!=ModeSelector.TIME_ATTACK || !c.modes.visible(c))return;
        float s=L.unit,x=L.w*.5f,y=selectY(L);
        boolean ready=playable(c);
        int color=ready?INK:INK_DIM;
        if(selected<ALL)Trinket.draw(p,Collect.BOSS_FIRST+selected,x,y-s*3.5f,s*1.6f,c.clock,ready,1);
        else for(int i=0;i<Boss.COUNT;i++)Trinket.draw(p,Collect.BOSS_FIRST+i,
                x+(i-1.5f)*s*2.6f,y-s*3.3f,s*1.15f,c.clock,unlocked(c,i),1);
        p.text(name(selected),x,y,type(s*.85f),color,Painter.CENTER,true);
        for(int d=-1;d<=1;d+=2) {
            float ax=x+d*L.w*.36f;
            p.line(ax-d*s*.2f,y-s*.5f,ax+d*s*.2f,y-s*.25f,GOLD,s*.09f);
            p.line(ax+d*s*.2f,y-s*.25f,ax-d*s*.2f,y,GOLD,s*.09f);
        }
        p.text(ready?Survival.profileName(Survival.profile(c.preferences.kids,c.fullRoster))
                :selected==ALL?"BEFRIEND ALL FOUR BOSSES":"BEFRIEND IN ADVENTURE",
                x,y+s*1.5f,type(s*.48f),ready?GOLD:INK_DIM,Painter.CENTER,true);
    }
    void result(Painter p,GameCore c,Layout L,float fade) {
        p=new OpacityPainter(p,fade);float s=L.unit,x=L.w*.5f;
        p.text(won?"BOSS BUDDIES!":"TIME FOR A REMATCH",x,L.h*.25f,type(s*1.15f),GOLD,Painter.CENTER,true);
        p.text(name(challenge),x,L.h*.32f,type(s*.8f),INK,Painter.CENTER,true);
        Trinket.drawReacting(p,c.runWho,x,L.h*.425f,s*2.1f,c.clock,1,
                won?8:5,0);
        p.text(time(millis()),x,L.h*.535f,type(s*1.9f),INK,Painter.CENTER,true);
        p.text(won?(newBest?"NEW PERSONAL BEST!":"CLEAR TIME"):"ATTEMPT TIME",x,L.h*.58f,
                type(s*.65f),won?GOLD:INK_DIM,Painter.CENTER,true);
        p.text("BEST "+time(best[profile][challenge])+" / "+Survival.profileName(profile),x,L.h*.63f,
                type(s*.62f),INK_DIM,Painter.CENTER,true);
        p.text("BOSSES CLEARED "+cleared+(challenge==ALL?" / "+Boss.COUNT:" / 1"),x,L.h*.68f,
                type(s*.6f),INK_DIM,Painter.CENTER,true);
        p.text("TAP TO RETURN",x,L.h*.75f,type(s*.6f),INK,Painter.CENTER,true);
    }
}
