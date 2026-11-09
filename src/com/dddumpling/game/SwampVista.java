package com.dddumpling.game;

/** Quiet grass banks and fireflies, projected from the same moving water-level landscape. */
final class SwampVista extends Draw {
    static final int ROWS=16,LANES=4;
    static final float SPEED=.030f;
    private SwampVista() {}
    static float depth(float t){return t/(3-2*t);}
    static float scale(float t){return .14f+depth(t)*1.55f;}
    static float horizon(Layout L){return L.h*.43f;}
    static float rootY(Layout L,float t){return horizon(L)+depth(t)*(L.deckTop-horizon(L)+L.h*.35f);}
    static float waterDepth(Layout L,float y){return Math.max(0,(y-horizon(L))/(L.deckTop-horizon(L)+L.h*.35f));}
    static float bend(float clock,float d) {
        d=Math.max(0,Math.min(1,d));float t=3*d/(1+2*d);
        return .19f*(float)Math.sin(Math.PI*Math.sqrt(d))*(float)Math.sin(t*12-clock*SPEED*12+.6f);
    }
    private static float phase(float clock,int side,int lane){return clock*SPEED*ROWS+(side>0?.43f:0)+lane*.37f;}
    static float travel(float clock,int row,int side){return travel(clock,row,side,0);}
    static float travel(float clock,int row,int side,int lane){float p=phase(clock,side,lane);return (row+p-(float)Math.floor(p))/ROWS;}
    static int grassSeed(float clock,int row,int side,int lane){return 4391+(row-(int)Math.floor(phase(clock,side,lane)))*137+side*53+lane*701;}
    static float shoreX(Layout L,float clock,float d,int side) {
        float v=d*(L.deckTop-horizon(L)+L.h*.35f)/(L.deckTop-horizon(L));
        return .5f+bend(clock,d)+side*(.065f+.28f*(float)Math.sqrt(Math.max(0,v)));
    }
    static float grassX(Layout L,float clock,float t,int side,int lane,int seed) {
        return shoreX(L,clock,depth(t),side)+side*(.10f+lane*.14f+hash(seed+7)*.012f)*scale(t);
    }
    static float arena(GameCore c) {
        if(BossCollect.keepsArena(c,Boss.ALLIGATOR))return 1;
        Boss b=BossVictory.retained(c) && c.bossVictoryKind==Boss.ALLIGATOR?c.bossVictory:c.boss;
        if(!b.active() || b.kind!=Boss.ALLIGATOR)return 0;
        float t=Math.max(0,Math.min(1,(b.introDuration()-b.intro)/.9f));return t*t*(3-2*t);
    }
    private static int ink(int color,float fade){return Glyph.withAlpha(color,(int)(255*Math.max(0,Math.min(1,fade))));}
    private static float reveal(float t){float u=Math.min(1,t/.16f);return u*u*(3-2*u);}
    static void draw(Painter p,GameCore c,Layout L,float fade) {
        fade*=1-BossVictory.backdropDrain(c);if(fade<=.001f)return;
        float time=c.cloudUi.reduceMotion?0:c.skyClock;
        backdrop(p,L,fade);
        horizonTrees(p,L,fade);
        banks(p,c,L,fade);
        // Each tuft has a permanent world seed and bank offset. Only blade tips sway.
        for(int row=0;row<ROWS;row++)for(int side=-1;side<=1;side+=2)for(int lane=LANES-1;lane>=0;lane--) {
            int seed=grassSeed(c.skyClock,row,side,lane);
            float t=travel(c.skyClock,row,side,lane),d=depth(t),s=scale(t);
            float x=L.w*grassX(L,c.skyClock,t,side,lane,seed),y=rootY(L,t);
            float r=L.w*SwampGrass.RADIUS*s*(.8f+hash(seed)*.25f);
            if(x+r*1.4f<0 || x-r*1.4f>L.w || y-r*SwampGrass.TOP>L.deckTop)continue;
            float opacity=fade*reveal(t);if(opacity<.008f)continue;
            SwampGrass.draw(p,x,y,r,seed,time,.25f+.75f*Math.min(1,d*2.5f),opacity);
        }
        LandBonusArt.inSwamp(p,c,L,fade);
        c.swampFireflies.draw(p,c,L,fade);
    }
    private static void backdrop(Painter p,Layout L,float fade) {
        float h=horizon(L)/L.deckTop;
        for(int i=0;i<80;i++) {
            float t=i/80f,y=i==0?-L.h*.03f:(float)Math.floor(t*L.deckTop),next=(float)Math.floor((i+1)/80f*L.deckTop);
            int color=t<h?Glyph.mix(0xFF263E3B,0xFF657C68,t/h):Glyph.mix(0xFF657C68,0xFF153E43,(float)(1-Math.exp(-3.7f*(t-h)/(1-h))));
            p.fillRect(-L.w*.06f,y,L.w*1.06f,next,ink(color,fade));
        }
    }
    private static void horizonTrees(Painter p,Layout L,float fade) {
        // Distant mangroves stay in the haze rather than entering the foreground.
        for(int i=0;i<10;i++) {
            int seed=919+i*67;float x=L.w*(i<5?.02f+i*.082f:.66f+(i-5)*.082f);
            float y=horizon(L)+L.h*.008f,h=L.h*(.055f+hash(seed)*.065f),r=L.w*(.035f+hash(seed+3)*.025f);
            float lean=L.w*(hash(seed+5)-.5f)*.03f;int color=ink(0xFF3C5A4B,fade*.20f);
            p.fillPoly(new float[]{x-r*.08f,y,x+r*.08f,y,x+lean+r*.035f,y-h,x+lean-r*.035f,y-h},color);
            p.fillEllipse(x+lean-r*.35f,y-h,r,r*.45f,color);
            p.fillEllipse(x+lean+r*.60f,y-h+r*.10f,r*.8f,r*.38f,color);
            p.polyline(new float[]{x-r*.65f,y,x-r*.2f,y-h*.2f,x,y-h*.35f,x+r*.3f,y-h*.18f,x+r*.7f,y},color,L.w*.002f);
        }
    }
    private static void banks(Painter p,GameCore c,Layout L,float fade) {
        float h=horizon(L),range=L.deckTop-h;float[] edge=new float[66];
        for(int side=-1;side<=1;side+=2) {
            edge[0]=side<0?-L.w*.06f:L.w*1.06f;edge[1]=h;
            for(int i=0;i<=30;i++) {
                float y=h+i/30f*range;
                edge[(i+1)*2]=L.w*shoreX(L,c.skyClock,waterDepth(L,y),side);edge[(i+1)*2+1]=y;
            }
            edge[64]=edge[0];edge[65]=L.deckTop;
            p.fillPoly(edge,ink(0xFF324F3B,fade*.65f));
        }
        for(int i=0;i<8;i++) {
            float u=(i+.5f)/8,y=h+L.h*(i/8f-.5f)*.06f,next=h+L.h*((i+1)/8f-.5f)*.06f;
            p.fillRect(0,y,L.w,next,ink(0xFF657C68,fade*.35f*(float)Math.sin(u*Math.PI)));
        }
    }
}
