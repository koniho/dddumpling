package com.dddumpling.game;

/** Fixed boss replays. Only playable combat contributes to these local records. */
final class TimeAttack extends Draw {
    static final int ALL=Boss.COUNT, CHOICES=Boss.COUNT+1;
    static final int SCENE=7;
    static final float SLIDE_TIME=.32f;
    final TimeAttackVersus versus=new TimeAttackVersus();
    final long[][] best=new long[Survival.PROFILE_COUNT][CHOICES];
    final TimeAttackHistory[][] histories=new TimeAttackHistory[Survival.PROFILE_COUNT][CHOICES];
    final java.util.TreeMap<Integer,TimeAttackRecords.Archive> archives=new java.util.TreeMap<>();
    // Named views retain access to the two shipped historical challenges.
    final long[] classicBest=TimeAttackRecords.archive(archives,4).best;
    final TimeAttackHistory[] classicHistory=TimeAttackRecords.archive(archives,4).history;
    final long[] fiveBest=TimeAttackRecords.archive(archives,5).best;
    final TimeAttackHistory[] fiveHistory=TimeAttackRecords.archive(archives,5).history;
    TimeAttackHistory.Run result;
    int defended,damage;
    private final java.util.ArrayList<TimeAttackHistory.Encounter> encounters=new java.util.ArrayList<>();
    private long encounterStart,encounterFirstDamage=-1;
    private int startHits,startMisses,startDefended,startDamage;
    long firstDamage=-1,firstBossHit=-1;
    private long encounterFirstBossHit=-1;
    int selected, challenge, current, profile, cleared;
    int previousSelected,slideDirection=1;
    float slide=1;
    boolean active, finished, won, newBest;
    double seconds;
    TimeAttack() {for(int p=0;p<Survival.PROFILE_COUNT;p++)for(int i=0;i<CHOICES;i++)histories[p][i]=new TimeAttackHistory();}
    TimeAttackHistory titleHistory(GameCore c) {return histories[Survival.profile(c.preferences.kids,c.fullRoster)][selected];}
    void defended() {if(active && !finished)defended++;}
    // Keep the original player-damage timing in saved histories for compatibility.
    void damaged() {
        if(!active || finished)return;
        long now=Math.max(0,Math.round(seconds*1000));
        if(damage++==0)firstDamage=now;
        if(encounterFirstDamage<0)encounterFirstDamage=now-encounterStart;
    }

    void bossDamaged(float before,float after) {
        if(!active || finished || !(after<before))return;
        long now=Math.max(0,Math.round(seconds*1000));
        if(firstBossHit<0)firstBossHit=now;
        if(encounterFirstBossHit<0)encounterFirstBossHit=now-encounterStart;
    }

    static String name(int choice,int skin) {return choice==ALL?"ALL BOSSES":Skins.bossName(skin,choice);}
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
        defended=damage=0;firstDamage=firstBossHit=-1;result=null;encounters.clear();
        challenge=selected;profile=Survival.profile(c.kidsRun,c.runFullRoster);
        encounter(c,challenge==ALL?0:challenge);
    }
    private void encounter(GameCore c,int which) {
        encounterStart=Math.round(seconds*1000);encounterFirstDamage=encounterFirstBossHit=-1;
        startHits=c.hits;startMisses=c.misses;startDefended=defended;startDamage=damage;
        current=which;c.stage=WorldCatalog.bossStage(which);
        c.enemies.clear();c.shots.clear();c.target=null;c.power=null;
        c.pendingBonus=c.bossReward=c.bossPrizePending=false;
        c.stageGap=c.stageBanner=0;c.spawnedThisStage=c.resolvedThisStage=0;
        c.pushUsed=false;c.pushT=c.pushSlowT=0;
        c.boss.begin(which,c.stage,c.rnd,c.playRosterFull());
        versus.begin(c,which);
        if(c.sound!=null)c.sound.versusMusic();
    }
    void tick(GameCore c,float elapsed) {
        if(!active || finished || c.state!=GameCore.PLAY || !c.boss.fighting()
                || c.paused || c.settingsOpen || elapsed<=0 || !Float.isFinite(elapsed))return;
        seconds+=elapsed;
    }
    void bossEnded(GameCore c,boolean beaten) {
        c.boss.leave();
        if(!beaten) {finish(c,false);return;}
        snapshot(c,true,challenge!=ALL || current+1==Boss.COUNT);
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
    private void snapshot(GameCore c,boolean beaten,boolean last) {
        if(!encounters.isEmpty() && encounters.get(encounters.size()-1).boss==current)return;
        long end=last?millis():Math.round(seconds*1000);
        encounters.add(new TimeAttackHistory.Encounter(current,end-encounterStart,beaten,
                c.hits-startHits,c.misses-startMisses,defended-startDefended,damage-startDamage,encounterFirstDamage,encounterFirstBossHit));
    }
    void finish(GameCore c,boolean success) {
        if(!active || finished)return;
        snapshot(c,success,true);
        versus.clear();finished=true;won=success;
        long duration=millis();
        TimeAttackHistory history=histories[profile][challenge];
        result=new TimeAttackHistory.Run(history.sequence+1,duration,c.runWho,cleared,success,
                c.hits,c.misses,defended,damage,firstDamage,firstBossHit,encounters.toArray(new TimeAttackHistory.Encounter[0]));
        if(c.scoresSuppressed)return;
        newBest=success && (best[profile][challenge]==0 || duration<best[profile][challenge]);
        if(newBest)best[profile][challenge]=duration;
        history.add(result);
        if(c.store!=null)c.store.saveTimeAttack(encode());
    }
    void leave() {versus.clear();active=finished=won=newBest=false;seconds=0;cleared=current=defended=damage=0;firstDamage=encounterFirstDamage=firstBossHit=encounterFirstBossHit=-1;encounterStart=0;
        startHits=startMisses=startDefended=startDamage=0;result=null;encounters.clear();}
    void clearRecords() {
        for(TimeAttackRecords.Archive a:archives.values())a.clear();
        for(long[] row:best)java.util.Arrays.fill(row,0);
        for(TimeAttackHistory[] row:histories)for(TimeAttackHistory history:row)history.clear();
        newBest=false;
    }
    String encode() {return TimeAttackRecords.encode(this);}
    void load(String saved) {TimeAttackRecords.load(this,saved);}

    static float selectY(Layout L) {return L.h*.735f;}
    static float nameY(Layout L) {return selectY(L)+L.unit*2.3f;}
    int hit(GameCore c,Layout L,float x,float y) {
        if(c.modes.selected!=ModeSelector.TIME_ATTACK || !ModeSelector.unlocked(c,ModeSelector.TIME_ATTACK) || !c.modes.visible(c)
                || x<L.padL || x>L.w-L.padR)return 0;
        if(Math.abs(y-selectY(L))<=L.unit*1.3f)return x<L.w*.27f?4:x>L.w*.73f?6:5;
        float top=Math.max(L.h*.635f,ModeSelector.y(L)+L.unit*2.2f);
        return y>=top && y<nameY(L)+L.unit*.4f?SCENE:0;
    }
    void drawSelector(Painter p,GameCore c,Layout L) {
        if(c.modes.selected!=ModeSelector.TIME_ATTACK || !ModeSelector.unlocked(c,ModeSelector.TIME_ATTACK) || !c.modes.visible(c))return;
        float s=L.unit,x=L.w*.5f,y=selectY(L);
        boolean ready=playable(c);
        int color=ready?INK:INK_DIM;
        TimeAttackDemo.draw(p,c,L);
        p.text(name(selected,p.skin()),x,nameY(L),type(s*.85f),color,Painter.CENTER,true);
        for(int d=-1;d<=1;d+=2) {
            float ax=x+d*L.w*.36f;
            p.line(ax-d*s*.2f,y-s*.5f,ax+d*s*.2f,y-s*.25f,GOLD,s*.09f);
            p.line(ax+d*s*.2f,y-s*.25f,ax-d*s*.2f,y,GOLD,s*.09f);
        }
        if(!ready)p.text(selected==ALL?"BEFRIEND ALL BOSSES":"BEFRIEND IN ADVENTURE",
                x,nameY(L)+s*1.5f,type(s*.48f),INK_DIM,Painter.CENTER,true);
    }
    void result(Painter p,GameCore c,Layout L,float fade) {
        if(result==null)return;
        p=new OpacityPainter(p,fade);
        TimeAttackScores.summary(p,c,L,result,challenge,profile,L.h*.22f,L.h*.77f);
        p.text(newBest?"NEW PERSONAL BEST!":"TAP TO RETURN",L.w*.5f,L.h*.80f,type(L.unit*.65f)*HighScoreScreen.SUMMARY_TEXT_SCALE,
                newBest?GOLD:INK,Painter.CENTER,true);
    }
}
