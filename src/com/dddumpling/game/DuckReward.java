package com.dddumpling.game;

/** Durable one-award journal and the continuous rainbow ride into the collection reveal. */
final class DuckReward extends Draw {
    private static final int[] POOLS={0,2,4,5,6,7,8};
    private static final int[] COLORS={0xFFFF8EA9,0xFFFFBC82,0xFFFFE993,0xFFA6EBAD,0xFF8EDDEB,0xFFABA6F3,0xFFE3A4EF};
    int who=-1,targetCount,total,profile;
    long sequence;
    boolean pending,fresh;

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
        pending=true;
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
        if(!pending || !c.overReady())return false;
        pending=false;
        if(c.store!=null)c.store.saveSurvivalAward(journal(c));
        c.time=c.deathDuration();
        if(c.sound!=null)c.sound.achievement();
        return true;
    }
    static float ease(float t) {t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    float[] pose(GameCore c,Layout L) {
        float t=c.deathProgress(),enter=ease((t-.10f)/.38f),settle=ease((t-.55f)/.45f);
        float x=L.w*(1.18f-.43f*enter-.25f*settle);
        float y=L.h*(.55f-.09f*settle);
        float r=L.w*(.07f+.065f*settle);
        return new float[]{x,y,r};
    }
    void draw(Painter p,GameCore c,Layout L) {
        if(!pending || !c.survival.active || c.state!=GameCore.OVER)return;
        float t=c.deathProgress(),reveal=ease((t-.72f)/.28f);
        float[] pose=pose(c,L);float x=pose[0],y=pose[1],r=pose[2];
        // The same rider and wave settle into the reveal while the companion sails away.
        for(int k=6;k>=0;k--) {
            float[] pts=new float[50];
            for(int j=0;j<25;j++) {
                float dx=(j/24f-.5f)*L.w*1.4f;
                pts[j*2]=x+dx;
                pts[j*2+1]=y+r*.86f+k*L.unit*.12f+(float)Math.sin(dx/L.w*8+c.clock*2)*L.unit*.20f;
            }
            p.polyline(pts,Glyph.withAlpha(COLORS[k],210),L.unit*.18f);
        }
        Storybook.glowRings(p,who,x,y,r,(c.clock%1.8f));
        float bounce=1f+.045f*(float)Math.sin(c.clock*4);
        Trinket.drawReacting(p,who,x,y,r*bounce,c.clock,1,RunCompanion.VICTORY,0);
        if(reveal>0) {
            p.text(who==Collect.COUNT-1?"FIVE-MINUTE CHAMPION!":"A RAINBOW RIDER!",L.w*.5f,L.h*.23f,
                    type(L.unit*.79f),fadeBy(GOLD,reveal),Painter.CENTER,true);
            float font=Math.min(type(L.unit*1.02f),L.w*.86f/(Collect.NAME[who].length()*.73f));
            p.text(Collect.NAME[who],L.w*.5f,L.h*.30f,font,fadeBy(INK,reveal),Painter.CENTER,true);
            p.text(fresh?"JOINS THE COLLECTION":"BACK IN THE LINE",L.w*.5f,L.h*.65f,
                    type(L.unit*.67f),fadeBy(Collect.TIER_COLOR[Collect.TIER[who]],reveal),Painter.CENTER,true);
            p.text("COLLECTED "+c.collectionCounts[who],L.w*.5f,L.h*.70f,type(L.unit*.53f),
                    fadeBy(INK_DIM,reveal),Painter.CENTER,false);
            if(c.overReady())p.text("TAP TO CONTINUE",L.w*.5f,L.h*.75f,type(L.unit*.57f),INK_DIM,Painter.CENTER,false);
        }
    }
}
