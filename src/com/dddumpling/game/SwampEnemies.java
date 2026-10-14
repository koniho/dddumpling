package com.dddumpling.game;

/** Swamp word variants share ordinary targeting, clears, rescue, and damage. */
final class SwampEnemies {
    static final int NONE=0, FOG=1, DIVE=2;
    static final float REVEAL=.32f;
    static final int MIST=0xFFB9D9CC, AMBER=0xFFFFCA68;
    private SwampEnemies() {}

    static void reset(GameCore.Enemy e) {
        e.swampKind=NONE;e.fogHead=0;e.fogReveal=0;
        e.diving=false;e.diveSpeed=0;
    }

    static void assign(GameCore c,GameCore.Enemy e) {
        reset(e);
        if(c.survival.active || c.timeAttack.active || c.boss.active() || Cave.active(c)
                || c.powerActive() || Lands.forStage(c.stage)!=Lands.SWAMP || e.word.length<2) return;
        // Reuse the uniform spawn phase: adding a variant does not shuffle future words.
        float pick=e.phase/6.283f;
        boolean intro=(c.stage-1)%Boss.EVERY==0;
        e.swampKind=pick<(intro?.20f:.25f)?FOG:pick<(intro?.35f:.45f)?DIVE:NONE;
        if(e.swampKind==DIVE) {
            // Keep the generated key order and RNG stream, but make the quick charge a short word.
            int length=Math.min(3,e.word.length);
            e.word=java.util.Arrays.copyOf(e.word,length);
            e.need=new int[length];java.util.Arrays.fill(e.need,1);
            e.gone=new boolean[length];e.goneT=new float[length];
            e.goneDx=new float[length];e.goneDy=new float[length];
        }
    }

    static boolean hidden(GameCore.Enemy e,int i) {
        return e.swampKind==FOG && !e.destroyed && !e.dying && i>e.pos && !e.gone[i];
    }

    static int unresolved(GameCore.Enemy e) {
        int count=0;
        for(int i=e.pos;i<e.word.length;i++)if(!e.gone[i])count++;
        return count;
    }

    static void progressed(GameCore c,GameCore.Enemy e,Layout L) {
        if(e.swampKind==FOG && e.pos!=e.fogHead) {
            e.fogReveal=e.pos>e.fogHead && e.pos<e.word.length?REVEAL:0;
            e.fogHead=e.pos;
        }
        if(e.swampKind!=DIVE || e.diving || !e.typeable() || unresolved(e)!=1) return;
        e.diving=true;
        e.diveSpeed=Math.max(e.speed*3.4f,Math.max(0,L.dangerY-L.enemyR-e.y)/1.6f);
        if(c.sound!=null)c.sound.octoCue();
    }

    static float fall(GameCore c,GameCore.Enemy e,Layout L,float dt) {
        float elapsed=dt*c.traversalRate();
        if(!e.diving)return PowerRush.yAfter(e,e.speed*c.fallRate()*elapsed);
        return e.y+e.diveSpeed*c.fallRate()*elapsed;
    }

    static void cloud(Painter p,float x,float y,float r,float clock,int fade) {
        float drift=(float)Math.sin(clock*2.3f+x*.03f)*r*.07f;
        p.fillEllipse(x,y+r*.23f,r*1.02f,r*.36f,Glyph.withAlpha(MIST,fade*85/255));
        p.fillCircle(x-r*.48f,y+drift,r*.40f,Glyph.withAlpha(MIST,fade*160/255));
        p.fillCircle(x,y-r*.16f-drift,r*.54f,Glyph.withAlpha(0xFFE3F4E8,fade*170/255));
        p.fillCircle(x+r*.48f,y+drift,r*.38f,Glyph.withAlpha(MIST,fade*165/255));
        p.fillEllipse(x,y+r*.20f,r*.88f,r*.30f,Glyph.withAlpha(MIST,fade*155/255));
        p.line(x-r*.95f,y+r*.38f,x-r*.5f,y+r*.38f,Glyph.withAlpha(MIST,fade*120/255),r*.07f);
    }

    static void accent(Painter p,GameCore c,GameCore.Enemy e,float x,float y,float r,int fade) {
        if(!e.typeable())return;
        if(e.swampKind==FOG && e.fogReveal>0) {
            float t=1-e.fogReveal/REVEAL;
            for(int side=-1;side<=1;side+=2)
                p.fillEllipse(x+side*r*(.9f+t*.9f),y-r*t*.7f,r*(.32f-t*.18f),r*.16f,
                        Glyph.withAlpha(MIST,(int)(fade*(1-t)*.65f)));
        }
        if(e.swampKind!=DIVE)return;
        float pulse=e.diving?.65f+.35f*(float)Math.sin(c.clock*19):.6f;
        int col=Glyph.withAlpha(AMBER,(int)(fade*pulse));
        float marker=y+r*1.18f;
        p.line(x-r*.24f,marker-r*.10f,x,marker+r*.10f,col,r*.12f);
        p.line(x,marker+r*.10f,x+r*.24f,marker-r*.10f,col,r*.12f);
        if(!e.diving)return;
        p.strokeCircle(x,y,r*1.07f,col,r*.09f);
        for(int k=0;k<3;k++) {
            float tx=x+(k-1)*r*.62f;
            p.line(tx,y-r*(1.3f+k*.22f),tx,y-r*(2.1f+k*.4f),
                    Glyph.withAlpha(AMBER,fade*(95-k*20)/255),r*(.16f-k*.025f));
        }
    }
}
