package com.dddumpling.game;

/** Swamp word variants share ordinary targeting, clears, rescue, and damage. */
final class SwampEnemies {
    static final int NONE=0, FOG=1, DIVE=2;
    static final float REVEAL=.32f;
    static final float DIVE_BURST=.5f;
    static final int MIST=0xFFB9D9CC, AMBER=0xFFFFCA68;
    private SwampEnemies() {}

    static void reset(GameCore.Enemy e) {
        e.swampKind=NONE;e.fogHead=0;e.fogReveal=0;
        e.diving=false;e.diveSpeed=e.diveBurst=0;
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
        e.diving=true;e.diveBurst=DIVE_BURST;
        e.diveSpeed=Math.max(e.speed*3.4f,Math.max(0,L.dangerY-L.enemyR-e.y)/1.6f);
        if(c.sound!=null)c.sound.octoCue();
    }

    static float fall(GameCore c,GameCore.Enemy e,Layout L,float dt) {
        float elapsed=dt*c.traversalRate();
        if(!e.diving)return PowerRush.yAfter(e,e.speed*c.fallRate()*elapsed);
        return e.y+e.diveSpeed*c.fallRate()*elapsed;
    }

    /** One drifting bank wraps the word; its leading edge retreats after each clear. */
    static void fog(Painter p,GameCore c,GameCore.Enemy e,Layout L,float jx,float jy,
            float swell,int fade,boolean front) {
        if(e.swampKind!=FOG)return;
        float vanish=e.destroyed?Math.min(1,e.destroyT/GameCore.DESTROY_TIME)
                :e.dying?Math.min(1,e.deathT/GameCore.SHOT_TIME):0;
        if(vanish>=1)return;
        int head=Math.min(e.pos,e.word.length-1);
        float reveal=e.fogReveal/REVEAL;
        float retreat=reveal*reveal*(3-2*reveal);
        float r=L.enemyR*swell;
        float x=c.tileX(e,head,L)+jx;
        float left=x-r*.95f;
        if(head>0)left-=(x-jx-c.tileX(e,head-1,L))*retreat;
        float right=c.tileX(e,e.word.length-1,L)+jx+r*.9f;
        float y=e.y+jy-r*vanish*.9f;
        float clock=c.clock+e.phase;
        int alpha=(int)(fade*(1-vanish));
        if(!front) {
            ribbon(p,left-r*.22f,right+r*.22f,y+r*.08f,r*1.45f,clock*.7f,0xFF8ABDC4,alpha*42/255);
            ribbon(p,left,right,y-r*.13f,r*1.12f,-clock*.9f,0xFFB6E3D5,alpha*60/255);
            return;
        }
        // Future glyphs are omitted, so the fog can stay airy without leaking their colors.
        if(head<e.word.length-1) {
            float denseLeft=x+r*.25f-r*retreat*.6f;
            ribbon(p,denseLeft,right,y-r*.06f,r*1.05f,clock*.8f,0xFFD8E9E6,alpha*100/255);
            ribbon(p,denseLeft+r*.2f,right-r*.12f,y+r*.19f,r*.72f,-clock,0xFFD8E1F2,alpha*60/255);
        }
        ribbon(p,left,right,y+r*.36f,r*.42f,clock*1.5f,0xFFDBF4E8,alpha*45/255);
        ribbon(p,left-r*.08f,right+r*.12f,y-r*.38f,r*.32f,-clock*1.2f,0xFFEDF1FF,alpha*38/255);
        // A thin veil crosses the active key while keeping its face and stack pips readable.
        ribbon(p,x-r*1.12f,x+r*1.25f,y+r*.04f,r*.65f,clock*1.8f,0xFFE5FFF4,alpha*30/255);
        if(reveal>0 && head>0) {
            float t=1-reveal,oldX=c.tileX(e,head-1,L)+jx;
            for(int k=0;k<3;k++) {
                float drift=r*(.3f+t*(1.2f+k*.35f));
                p.fillEllipse(oldX+drift,y-r*(.15f+k*.23f+t*.7f),
                        r*(.6f+t*.4f),r*(.22f-t*.1f),
                        Glyph.withAlpha(k==1?0xFFEAF9F4:MIST,(int)(alpha*reveal*.32f)));
            }
        }
    }

    private static void ribbon(Painter p,float left,float right,float y,float radius,float phase,int color,int alpha) {
        final int steps=48;
        float[] points=new float[(steps+1)*4];
        for(int i=0;i<=steps;i++) {
            float t=i/(float)steps,x=left+(right-left)*t;
            float taper=(float)Math.sin(Math.PI*t);
            float wave=(float)Math.sin(t*10+phase)*.25f+(float)Math.sin(t*19-phase*.8f)*.12f;
            float middle=y+radius*wave;
            float width=radius*taper*(.76f+.22f*(float)Math.sin(t*10-phase)
                    +.13f*(float)Math.sin(t*19+phase*.8f));
            points[i*2]=x;points[i*2+1]=middle-width;
            int back=(steps*2+1-i)*2;
            points[back]=x;points[back+1]=middle+radius*taper*(.72f+.19f*(float)Math.cos(t*17+phase));
        }
        p.fillPoly(points,Glyph.withAlpha(color,alpha));
    }

    static void hardhat(Painter p,float x,float y,float r,int fade) {
        float top=y-r*.54f;
        p.fillEllipse(x,top,r*.61f,r*.42f,Glyph.withAlpha(0xFF936127,fade));
        p.fillEllipse(x-r*.025f,top-r*.045f,r*.55f,r*.36f,Glyph.withAlpha(AMBER,fade));
        p.fillEllipse(x-r*.20f,top-r*.14f,r*.22f,r*.10f,Glyph.withAlpha(0xFFFFF4C4,fade*190/255));
        p.line(x,top-r*.34f,x,top+r*.02f,Glyph.withAlpha(0xFFFFE89B,fade),r*.13f);
        p.line(x-r*.35f,top-r*.18f,x-r*.38f,top+r*.08f,Glyph.withAlpha(0xFFD69A3E,fade),r*.055f);
        p.line(x+r*.35f,top-r*.18f,x+r*.38f,top+r*.08f,Glyph.withAlpha(0xFFD69A3E,fade),r*.055f);
        p.fillEllipse(x,top+r*.18f,r*.72f,r*.13f,Glyph.withAlpha(0xFF936127,fade));
        p.fillEllipse(x,top+r*.12f,r*.73f,r*.12f,Glyph.withAlpha(0xFFFFDA7A,fade));
    }

    static void accent(Painter p,GameCore c,GameCore.Enemy e,float x,float y,float r,int fade) {
        if(!e.typeable())return;
        if(e.swampKind!=DIVE || !e.diving)return;
        float pulse=.65f+.35f*(float)Math.sin(c.clock*19);
        int col=Glyph.withAlpha(AMBER,(int)(fade*pulse));
        p.strokeCircle(x,y,r*1.09f,col,r*.12f);
        for(int k=0;k<7;k++) {
            float t=(c.clock*(2.5f+k*.13f)+k*.37f)%1;
            float tx=x+(k-3)*r*.37f;
            float tip=y-r*(1.1f+t*.9f);
            int a=(int)(fade*(1-t)*.75f);
            p.line(tx,tip,tx,tip-r*(1.3f+(k%3)*.5f),Glyph.withAlpha(AMBER,a),r*.13f);
            p.line(tx,tip,tx,tip-r*.8f,Glyph.withAlpha(0xFFFFF6DC,a),r*.045f);
        }
        if(e.diveBurst>0) {
            float t=1-e.diveBurst/DIVE_BURST;
            int a=(int)(fade*(1-t));
            for(int k=0;k<3;k++)p.strokeCircle(x,y,r*(1.1f+t*(2.7f-k*.5f)),
                    Glyph.withAlpha(k==1?0xFFFFF6DC:AMBER,a*(180-k*35)/255),r*(.15f-k*.03f));
            if(t<.3f)p.fillCircle(x,y,r*1.1f,Glyph.withAlpha(0xFFFFF6DC,(int)(a*(1-t/.3f)*.4f)));
            for(int k=0;k<10;k++) {
                double angle=k*Math.PI/5+e.phase;
                float dx=(float)Math.cos(angle),dy=(float)Math.sin(angle);
                float inner=r*(1.3f+t*1.8f),outer=inner+r*(.65f+(k%3)*.2f)*(1-t);
                p.line(x+dx*inner,y+dy*inner,x+dx*outer,y+dy*outer,
                        Glyph.withAlpha(k%2==0?AMBER:0xFFFFF6DC,a),r*.11f*(1-t));
            }
        }
    }
}
