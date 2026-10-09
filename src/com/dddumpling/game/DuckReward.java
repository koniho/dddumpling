package com.dddumpling.game;

/** Durable one-award journal, earned collectible card, and collection parade. */
final class DuckReward extends Draw {
    static final float REVEAL_HOLD=2f;
    private static final int[] POOLS={0,2,4,5,6,7,8};
    int who=-1,targetCount,total,profile;
    long sequence;
    boolean pending,fresh,joining,joinRung;
    float joinAt,revealAt=-1f;

    static int choose(double seconds,java.util.Random random) {
        if(seconds>=300)return Collect.DUCK_FIRST+10;
        int segment=Math.max(0,Math.min(6,(int)(seconds/45)));
        return Collect.DUCK_FIRST+POOLS[segment]+random.nextInt(2);
    }
    void select(GameCore c) {
        who=choose(c.survival.seconds,c.survival.blurbs);
        fresh=!Collect.has(c.collected,who);
        targetCount=(int)Math.min(Integer.MAX_VALUE,(long)c.collectionCounts[who]+1);
        total=(int)Math.min(Integer.MAX_VALUE,(long)c.collectTotal+1);
        profile=c.survival.profile;
        sequence=Math.max(sequence,c.progress.duckSequence())+1;
        pending=true;joining=joinRung=false;
        revealAt=-1f;
        c.ducks.react(who,.7f);
        c.survival.history().prize(who);
    }
    private String journal(GameCore c) {
        String header="1;"+sequence;
        return pending?header+";"+who+";"+targetCount+";"+total+";"+(fresh?1:0)+";"+profile+";"+c.survival.adventureLand+"#"+c.survival.encode():header;
    }
    void commit(GameCore c) {
        if(!pending)return;
        // This single write contains both the selected reward and its completed run record.
        // Replaying the remaining writes uses absolute floors and an idempotent cloud event.
        if(c.store!=null)c.store.saveSurvivalAward(journal(c));
        apply(c);
    }
    private void apply(GameCore c) {
        c.prize=who;c.prizeNew=fresh;
        c.collectionCounts[who]=Math.max(c.collectionCounts[who],targetCount);
        c.collectTotal=Math.max(c.collectTotal,total);
        c.collected=Collect.add(c.collected,who);
        c.progress.duckReward(sequence,who,fresh);
        CaseUi.highlight(c,who);
        if(c.store!=null) {
            c.store.saveCollected(Collect.encode(c.collected));
            c.store.saveCollectionCounts(c.collectionCounts);
            c.store.saveCollectTotal(c.collectTotal);
            c.store.saveSurvival(c.survival.encode());
        }
    }
    void restore(GameCore c) {
        if(c.store==null)return;
        String saved=c.store.loadSurvivalAward();
        if(saved==null || saved.isEmpty() || saved.length()>52000)return;
        try {
            String[] parts=saved.split("#",-1),f=parts[0].split(";",-1);
            if(!f[0].equals("1") || !(f.length==2 && parts.length==1 || f.length==8 && parts.length==2))return;
            long seq=Long.parseLong(f[1]);if(seq<0 || seq==Long.MAX_VALUE)return;
            if(f.length==2) {sequence=seq;return;}
            int entry=Integer.parseInt(f[2]),count=Integer.parseInt(f[3]),sum=Integer.parseInt(f[4]);
            int isNew=Integer.parseInt(f[5]),savedProfile=Integer.parseInt(f[6]),land=Integer.parseInt(f[7]);
            if(entry<Collect.DUCK_FIRST || entry>=Collect.COUNT || count<1 || sum<count
                    || isNew<0 || isNew>1 || savedProfile<0 || savedProfile>2 || land<0 || land>Lands.COUNT)return;
            Survival snapshot=new Survival();snapshot.load(parts[1]);
            HighScores.Run run=snapshot.histories[savedProfile].latestRun;
            if(run==null || run.prizes.length!=1 || run.prizes[0]!=entry)return;
            sequence=seq;who=entry;targetCount=count;total=sum;fresh=isNew==1;profile=savedProfile;pending=true;
            joining=joinRung=false;revealAt=-1f;
            c.survival.load(parts[1]);
            c.survival.profile=profile;c.survival.active=c.survival.finished=true;
            c.survival.seconds=run.duration/1000.0;c.survival.ending=run.ending;
            c.survival.adventureLand=land;
            c.score=run.score;c.hits=run.hits;c.misses=run.misses;c.runWho=run.character;
            c.kidsRun=run.kids;c.runFullRoster=profile==1;c.maxCombo=run.combo;c.squishes=run.squishes;
            c.survival.newBest=run.duration>=c.survival.bestTime[profile];
            c.state=GameCore.OVER;c.lives=0;c.deathT=0;
            c.time=c.deathDuration()+GameCore.OVER_FADE+GameCore.OVER_GRACE+.1f;
            c.modes.selected=ModeSelector.SURVIVAL;
            apply(c);
        } catch(NumberFormatException ignored) { }
    }
    boolean dismiss(GameCore c) {
        if(!pending)return false;
        // Consume every tap while this flow owns the screen, including during its animations.
        if(!joining && c.overReady()) {
            beginJoin(c);
        }
        return true;
    }
    private void beginJoin(GameCore c) {
        joining=true;joinRung=false;joinAt=c.time;
        if(c.sound!=null) {
            c.sound.hush();
            c.sound.achievement();
        }
    }
    float joinProgress(GameCore c) {
        return Math.max(0,Math.min(1,(c.time-joinAt)/GameCore.PARADE_TIME));
    }
    void update(GameCore c) {
        if(!pending || c.state!=GameCore.OVER)return;
        if(!joining) {
            // Give the fully uncovered card two seconds, including after journal recovery.
            if(c.dying())return;
            if(revealAt<0) {
                revealAt=c.time;
                if(c.sound!=null)c.sound.announceSquishy(who);
            }
            if(c.time-revealAt>=REVEAL_HOLD)beginJoin(c);
            return;
        }
        float t=joinProgress(c);
        if(!joinRung && t>=Parade.JOIN_END) {
            joinRung=true;
            if(c.sound!=null)c.sound.paradeJoin();
        }
        if(t<1)return;
        pending=joining=false;
        if(c.store!=null)c.store.saveSurvivalAward(journal(c));
        c.time=c.deathDuration();
    }
    static float ease(float t) {t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    void draw(Painter p,GameCore c,Layout L) {
        if(!pending || !c.survival.active || c.state!=GameCore.OVER
                || c.deathProgress()<SurvivalEnd.REVEAL)return;
        p.fillRect(0,0,L.w,L.h,BG);
        if(joining) {
            float t=joinProgress(c);
            Parade.draw(p,c,L,1-ease((t-.90f)/.10f),t,true);
            return;
        }
        float x=L.w*.5f,y=L.h*.46f,r=L.w*.135f;
        Storybook.glowRings(p,who,x,y,r,(c.clock%1.8f));
        float bounce=1f+.045f*(float)Math.sin(c.clock*4);
        Trinket.drawReacting(p,who,x,y,r*bounce,c.clock,1,RunCompanion.VICTORY,0);
        p.text("SURVIVAL REWARD",x,L.h*.23f,type(L.unit*.79f),GOLD,Painter.CENTER,true);
        float font=Math.min(type(L.unit*1.02f),L.w*.86f/(Collect.NAME[who].length()*.73f));
        p.text(Collect.NAME[who],x,L.h*.30f,font,INK,Painter.CENTER,true);
        p.text("YOU SURVIVED "+Survival.time(c.survival.millis())+"!",x,L.h*.64f,
                type(L.unit*.72f),INK,Painter.CENTER,true);
        p.text(who==Collect.COUNT-1?"FIVE MINUTES EARNS THE CHAMPION":
                "A DUCK FROM YOUR SURVIVAL TIME",x,L.h*.69f,type(L.unit*.53f),INK_DIM,Painter.CENTER,false);
    }
}
