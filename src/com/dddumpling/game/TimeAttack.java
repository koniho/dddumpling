package com.dddumpling.game;

/** Fixed boss replays. Only playable combat contributes to these local records. */
final class TimeAttack extends Draw {
    static final int ALL=Boss.COUNT, CHOICES=Boss.COUNT+1;
    static final int SWIPE_ONLY=7;
    static final float SLIDE_TIME=.32f;
    final long[][] best=new long[3][CHOICES];
    final TimeAttackHistory[][] histories=new TimeAttackHistory[3][CHOICES];
    TimeAttackHistory.Run result;
    int defended,damage;
    long firstDamage=-1;
    int selected, challenge, current, profile, cleared;
    int previousSelected,slideDirection=1;
    float slide=1;
    boolean active, finished, won, newBest;
    double seconds;
    TimeAttack() {for(int p=0;p<3;p++)for(int i=0;i<CHOICES;i++)histories[p][i]=new TimeAttackHistory();}
    TimeAttackHistory titleHistory(GameCore c) {return histories[Survival.profile(c.preferences.kids,c.fullRoster)][selected];}
    void defended() {if(active && !finished)defended++;}
    void damaged() {
        if(!active || finished)return;
        if(damage++==0)firstDamage=Math.max(0,Math.round(seconds*1000));
    }

    static String name(int choice) {return choice==ALL?"ALL BOSSES":Boss.NAMES[choice];}
    static boolean unlocked(GameCore c,int choice) {
        return choice==ALL?ModeSelector.allBossesUnlocked(c):ModeSelector.bossUnlocked(c,choice);
    }
    boolean playable(GameCore c) {return unlocked(c,selected);}
    void choose(GameCore c,int direction) {
        previousSelected=selected;slideDirection=direction;slide=0;
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
        defended=damage=0;firstDamage=-1;result=null;
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
        long duration=millis();
        TimeAttackHistory history=histories[profile][challenge];
        result=new TimeAttackHistory.Run(history.sequence+1,duration,c.runWho,cleared,success,
                c.hits,c.misses,defended,damage,firstDamage);
        if(c.scoresSuppressed)return;
        newBest=success && (best[profile][challenge]==0 || duration<best[profile][challenge]);
        if(newBest)best[profile][challenge]=duration;
        history.add(result);
        if(c.store!=null)c.store.saveTimeAttack(encode());
    }
    void leave() {active=finished=won=newBest=false;seconds=0;cleared=current=defended=damage=0;firstDamage=-1;result=null;}
    void clearRecords() {
        for(long[] row:best)java.util.Arrays.fill(row,0);
        for(TimeAttackHistory[] row:histories)for(TimeAttackHistory history:row)history.clear();
        newBest=false;
    }
    String encode() {
        StringBuilder out=new StringBuilder("2");
        for(long[] row:best)for(long value:row)out.append(';').append(value);
        for(TimeAttackHistory[] row:histories)for(TimeAttackHistory history:row)out.append('|').append(history.encode());
        return out.toString();
    }
    void load(String saved) {
        if(saved==null || saved.length()>100000)return;
        String[] parts=saved.split("\\|",-1),fields=parts[0].split(";",-1);
        boolean legacy=fields[0].equals("1");
        if(fields.length!=1+3*CHOICES || !(legacy || fields[0].equals("2"))
                || parts.length!=(legacy?1:1+3*CHOICES))return;
        long[][] parsed=new long[3][CHOICES];
        TimeAttackHistory[][] loaded=new TimeAttackHistory[3][CHOICES];
        try {
            for(int p=0;p<3;p++)for(int i=0;i<CHOICES;i++) {
                long value=Long.parseLong(fields[1+p*CHOICES+i]);
                if(value<0 || value>86400000L)return;
                parsed[p][i]=value;
                loaded[p][i]=new TimeAttackHistory();
                if(legacy)loaded[p][i].legacy(value,i==ALL?Boss.COUNT:1);
                else if(!loaded[p][i].load(parts[1+p*CHOICES+i],i))return;
                if(!loaded[p][i].runs.isEmpty() && loaded[p][i].runs.get(0).duration!=value)return;
            }
        } catch(NumberFormatException invalid) {return;}
        for(int p=0;p<3;p++) {
            System.arraycopy(parsed[p],0,best[p],0,CHOICES);
            System.arraycopy(loaded[p],0,histories[p],0,CHOICES);
        }
    }
    static float selectY(Layout L) {return L.h*.735f;}
    int hit(GameCore c,Layout L,float x,float y) {
        if(c.modes.selected!=ModeSelector.TIME_ATTACK || !c.modes.visible(c)
                || x<L.padL || x>L.w-L.padR)return 0;
        if(Math.abs(y-selectY(L))<=L.unit*1.3f)return x<L.w*.27f?4:x>L.w*.73f?6:5;
        float top=Math.max(L.h*.635f,ModeSelector.y(L)+L.unit*2.2f);
        return y>=top && y<selectY(L)?SWIPE_ONLY:0;
    }
    void drawSelector(Painter p,GameCore c,Layout L) {
        if(c.modes.selected!=ModeSelector.TIME_ATTACK || !c.modes.visible(c))return;
        float s=L.unit,x=L.w*.5f,y=selectY(L);
        boolean ready=playable(c);
        int color=ready?INK:INK_DIM;
        TimeAttackDemo.draw(p,c,L);
        p.text(name(selected),x,y,type(s*.85f),color,Painter.CENTER,true);
        for(int d=-1;d<=1;d+=2) {
            float ax=x+d*L.w*.36f;
            p.line(ax-d*s*.2f,y-s*.5f,ax+d*s*.2f,y-s*.25f,GOLD,s*.09f);
            p.line(ax+d*s*.2f,y-s*.25f,ax-d*s*.2f,y,GOLD,s*.09f);
        }
        if(!ready)p.text(selected==ALL?"BEFRIEND ALL FOUR BOSSES":"BEFRIEND IN ADVENTURE",
                x,y+s*1.5f,type(s*.48f),INK_DIM,Painter.CENTER,true);
    }
    void result(Painter p,GameCore c,Layout L,float fade) {
        if(result==null)return;
        p=new OpacityPainter(p,fade);
        TimeAttackScores.summary(p,c,L,result,challenge,profile,L.h*.22f,L.h*.77f);
        p.text(newBest?"NEW PERSONAL BEST!":"TAP TO RETURN",L.w*.5f,L.h*.80f,type(L.unit*.65f),
                newBest?GOLD:INK,Painter.CENTER,true);
    }
}
