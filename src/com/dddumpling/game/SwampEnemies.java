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
        e.diving=false;e.diveSpeed=e.diveBurst=e.diveStartX=e.diveStartY=0;
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
        return e.swampKind==FOG && !e.destroyed && !e.dying && i>e.fogHead && !e.gone[i];
    }

    static int unresolved(GameCore.Enemy e) {
        int count=0;
        for(int i=e.pos;i<e.word.length;i++)if(!e.gone[i])count++;
        return count;
    }

    static void progressed(GameCore c,GameCore.Enemy e,Layout L) {
        // Revealed prompts stay visible when a miss restarts the word.
        if(e.swampKind==FOG && e.pos>e.fogHead) {
            e.fogReveal=e.pos<e.word.length?REVEAL:0;
            e.fogHead=e.pos;
        }
        if(e.swampKind!=DIVE || e.diving || !e.typeable() || unresolved(e)!=1) return;
        e.diving=true;e.diveBurst=DIVE_BURST;
        e.diveStartX=c.tileX(e,e.pos,L);e.diveStartY=e.y;
        e.diveSpeed=Math.max(e.speed*3.4f,Math.max(0,L.dangerY-L.enemyR-e.y)/1.6f);
        if(c.sound!=null)c.sound.ninjaSwish(0);
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
        int head=Math.min(e.fogHead,e.word.length-1);
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
            ribbon(p,left-r*.5f,right+r*.65f,y+r*.24f,r*.46f,clock*.7f,0xFF8ABDC4,alpha*42/255);
            ribbon(p,left-r*.2f,right+r*.85f,y-r*.24f,r*.38f,-clock*.9f,0xFFB6E3D5,alpha*52/255);
            return;
        }
        // Future glyphs are omitted, so the fog can stay airy without leaking their colors.
        if(head<e.word.length-1) {
            float denseLeft=x+r*.25f-r*retreat*.6f;
            for(int k=0;k<4;k++) {
                float drift=(float)Math.sin(clock*.85f+k*2)*r*.32f;
                ribbon(p,denseLeft-r*.18f+drift,right+r*(.45f+k*.15f)+drift,
                        y+r*(-.49f+k*.31f),r*(.24f+(k%2)*.06f),clock*(k%2==0?.85f:-.7f)+k,
                        k%2==0?0xFFD8E9E6:0xFFD8E1F2,alpha*(64+k*6)/255);
            }
        }
        // Long overlapping striations keep the veil visible without a thick cloud silhouette.
        for(int k=0;k<5;k++) {
            float drift=(float)Math.sin(clock*(.65f+k*.12f)+k*1.7f)*r*.3f;
            ribbon(p,left-r*(.55f+k*.06f)+drift,right+r*(.4f+(k%3)*.22f)+drift,
                    y+r*(-.62f+k*.30f),r*(.16f+(k%2)*.07f),clock*(k%2==0?1.1f:-.9f)+k*.8f,
                    k%2==0?0xFFE5FFF4:0xFFEDF1FF,alpha*(45+(k%3)*8)/255);
        }
        for(int k=0;k<3;k++)ribbon(p,x-r*1.6f,x+r*(2.2f+k*.25f),
                y+r*(-.38f+k*.38f),r*.25f,clock*(.9f+k*.1f)+k*1.5f,0xFFE5FFF4,alpha*76/255);
        if(reveal>0 && head>0) {
            float t=1-reveal,oldX=c.tileX(e,head-1,L)+jx;
            for(int k=0;k<3;k++) {
                float drift=r*(.3f+t*(1.2f+k*.35f));
                ribbon(p,oldX+drift-r,oldX+drift+r*(1.5f+t),y-r*(.15f+k*.23f+t*.7f),
                        r*(.13f-t*.06f),clock+k,k==1?0xFFEAF9F4:MIST,(int)(alpha*reveal*.32f));
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

    static void burst(Painter p,GameCore.Enemy e,float r,int fade) {
        if(e.swampKind!=DIVE || !e.diving || !e.typeable() || e.diveBurst<=0)return;
        float x=e.diveStartX,y=e.diveStartY;
        float t=1-e.diveBurst/DIVE_BURST;
        int color=Glyph.COLOR[e.word[e.pos]],light=Glyph.mix(color,0xFFFFFFFF,.72f);
        int alpha=(int)(fade*(1-t));
        for(int k=0;k<4;k++) {
            float radius=r*(.8f+t*(4.2f-k*.65f));
            int tint=k%2==0?color:light;
            p.strokeCircle(x,y,radius,Glyph.withAlpha(tint,alpha*(220-k*25)/255),r*(.28f-k*.035f));
            p.strokeCircle(x,y,radius-r*.06f,Glyph.withAlpha(light,alpha*100/255),r*.055f);
        }
        if(t<.42f) {
            float flash=1-t/.42f;
            p.fillCircle(x,y,r*(2.3f+t*2),Glyph.withAlpha(color,(int)(alpha*flash*.35f)));
            p.fillCircle(x,y,r*1.5f,Glyph.withAlpha(light,(int)(alpha*flash*.75f)));
            p.fillCircle(x,y,r,Glyph.withAlpha(0xFFFFFFFF,(int)(alpha*flash*.9f)));
        }
        for(int k=0;k<8;k++) {
            double angle=k*Math.PI/4+e.phase;
            float dx=(float)Math.cos(angle),dy=(float)Math.sin(angle);
            float inner=r*(1.5f+t*3.1f),outer=inner+r*.65f*(1-t);
            p.line(x+dx*inner,y+dy*inner,x+dx*outer,y+dy*outer,
                    Glyph.withAlpha(k%2==0?color:light,alpha),r*.12f*(1-t));
        }
    }
}
