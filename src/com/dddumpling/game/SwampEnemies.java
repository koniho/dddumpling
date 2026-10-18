package com.dddumpling.game;

/** Swamp word variants share ordinary targeting, clears, rescue, and damage. */
final class SwampEnemies {
    static final int NONE=0, FOG=1, DIVE=2;
    static final float REVEAL=.225f;
    static final float DIVE_BURST=.5f;
    static final int FOG_INK=0xFF53636F;
    private SwampEnemies() {}

    static float tintAmount(GameCore.Enemy e,int i) {
        if(e.swampKind!=FOG || i!=e.fogHead || e.destroyed || e.dying)return 0;
        float t=Math.max(0,Math.min(1,e.fogReveal/REVEAL));
        return t*t*(3-2*t);
    }

    static void silhouette(Painter p,int glyph,float x,float y,float r,int fade) {
        p.strokePoly(Glyph.hex(x,y,r),Glyph.withAlpha(FOG_INK,fade*95/255),r*.055f);
        Kawaii.draw(new FogSilhouette(p),glyph,x,y,r*.60f,FOG_INK,1f,0.1f);
    }
    private static final class FogSilhouette extends OpacityPainter {
        FogSilhouette(Painter p) {super(p,1);}
        int color(int c) {return (c>>>24)==0?0:FOG_INK;}
        public ArtStamp stamps() {return null;}
    }

    static void reset(GameCore.Enemy e) {
        e.swampKind=NONE;e.fogHead=0;e.fogReveal=0;
        e.diving=false;e.diveSpeed=e.diveBurst=e.diveStartX=e.diveStartY=0;
    }

    static void assign(GameCore c,GameCore.Enemy e) {
        reset(e);
        if(c.survival.active || c.timeAttack.active || c.boss.active() || Cave.active(c)
                || Boss.isBossStage(c.stage) || (Lands.forStage(c.stage)!=Lands.SWAMP && Lands.forStage(c.stage)!=Lands.RAINBOW)
                || e.link!=null || e.word.length<2) return;
        // Reuse the uniform spawn phase: adding a variant does not shuffle future words.
        float pick=e.phase/6.283f;
        boolean intro=(c.stage-1)%Boss.EVERY==0;
        e.swampKind=Lands.forStage(c.stage)==Lands.RAINBOW?(pick<.36f?DIVE:NONE):pick<(intro?.20f:.25f)?FOG:pick<(intro?.35f:.45f)?DIVE:NONE;
        if(e.swampKind==FOG || e.swampKind==DIVE) {
            // Both variants keep at most three generated keys; only dives strip stacked pips.
            int length=Math.min(3,e.word.length);
            e.word=java.util.Arrays.copyOf(e.word,length);
            e.need=java.util.Arrays.copyOf(e.need,length);
            if(e.swampKind==DIVE)java.util.Arrays.fill(e.need,1);
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

    /** The same bank surrounds the whole row; keys reveal between its depth layers. */
    static void fog(Painter p,GameCore c,GameCore.Enemy e,Layout L,int layer) {
        fog(p,e,L.enemyR,L.enemyStep,c.enemyCentreX(e),c.clock,layer);
    }

    static void fog(Painter p,GameCore.Enemy e,float baseRadius,float step,float centre,float time,int layer) {
        if(e.swampKind!=FOG)return;
        float r=baseRadius*(.62f+.38f*e.enterT);
        if(e.destroyed)centre=e.clearX;
        float x=centre-(e.word.length-1)*step*.5f;
        float left=x-r*2.5f,right=centre+(e.word.length-1)*step*.5f+r*2.5f;
        float y=e.destroyed?e.clearY:e.y,clock=time*2.2f+e.phase;
        int alpha=(int)(90+165*e.enterT);
        if(layer==0) {
            fogRibbon(p,e,r,0,left-r*.5f,right+r*.65f,y+r*.24f,r*.46f,clock*.7f,0xFF8ABDC4,alpha*42/255);
            fogRibbon(p,e,r,1,left-r*.2f,right+r*.85f,y-r*.24f,r*.38f,-clock*.9f,0xFFB6E3D5,alpha*52/255);
        } else if(layer==1) {
            // These denser strands sit over hidden silhouettes, beneath revealed keys.
            for(int k=0;k<4;k++) {
                float drift=(float)Math.sin(clock*(.85f+k*.13f)+k*2)*r*.90f;
                fogRibbon(p,e,r,2+k,left+r*.5f+drift,right+r*(.45f+k*.15f)+drift,
                        y+r*(-.49f+k*.31f+.12f*(float)Math.sin(clock*1.3f+k)),r*(.24f+(k%2)*.06f),clock*(k%2==0?.85f:-.7f)+k,
                        k%2==0?0xFFD8E9E6:0xFFD8E1F2,alpha*(64+k*6)/255);
            }
        } else {
            // A lighter foreground veil stays above even the readable keys.
            for(int k=0;k<5;k++) {
                float drift=(float)Math.sin(clock*(.65f+k*.12f)+k*1.7f)*r*.95f;
                fogRibbon(p,e,r,6+k,left-r*(.55f+k*.06f)+drift,right+r*(.4f+(k%3)*.22f)+drift,
                        y+r*(-.62f+k*.30f+.12f*(float)Math.sin(clock*.9f+k*2)),r*(.16f+(k%2)*.07f),clock*(k%2==0?1.1f:-.9f)+k*.8f,
                        k%2==0?0xFFE5FFF4:0xFFEDF1FF,alpha*(45+(k%3)*8)/255);
            }
            for(int k=0;k<3;k++)fogRibbon(p,e,r,11+k,left+r*.9f,right-r*(.3f-k*.25f),
                    y+r*(-.38f+k*.38f),r*.25f,clock*(.9f+k*.1f)+k*1.5f,0xFFE5FFF4,alpha*76/255);
        }
    }

    private static void fogRibbon(Painter p,GameCore.Enemy e,float r,int id,float left,float right,
            float y,float radius,float phase,int color,int alpha) {
        if(e.destroyed) {
            float t=Math.min(1,e.destroyT/GameCore.DESTROY_TIME);
            if(t>=1)return;
            float ease=1-(1-t)*(1-t);
            // Move the original strand intact, keeping its shape, phase and depth.
            float drift=(id%2==0?-1:1)*r*ease*(4f+(id%3)*.65f);
            left+=drift;right+=drift;y-=r*ease*(.25f+(id%4)*.12f);
            alpha=(int)(alpha*(1-t)*(1-t));
        }
        ribbon(p,left,right,y,radius,phase,color,alpha);
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

    static void burst(Painter p,GameCore.Enemy e,float r,int fade) {
        if(e.swampKind!=DIVE || !e.diving || !e.typeable() || e.diveBurst<=0)return;
        float x=e.diveStartX,y=e.diveStartY;
        float t=1-e.diveBurst/DIVE_BURST;
        int color=Skins.keyColor(p,e.word[e.pos]),light=Glyph.mix(color,0xFFFFFFFF,.72f);
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
