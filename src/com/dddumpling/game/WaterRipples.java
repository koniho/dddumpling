package com.dddumpling.game;

/** Ambient water refraction and bounded clear glows, prepared once per rendered frame. */
final class WaterRipples {
    static final int COUNT=4;
    static final int PLANKTON=8;
    static final float DURATION=1.8f;
    private static final float GLOW_TIME=1.05f;
    private final float[] born=new float[COUNT],sky=new float[COUNT],x=new float[COUNT],y=new float[COUNT];
    private final float[] px=new float[COUNT],py=new float[COUNT],brightness=new float[COUNT],radius=new float[COUNT];
    private final float[] ages=new float[COUNT],phase=new float[COUNT];
    private static final int COLS=8,ROWS=64;
    private final float[] field=new float[(COLS+1)*(ROWS+1)];
    final float[][] buffers=new float[96][];
    final float[] kelpBlade=new float[36],kelpShadow=new float[36],kelpEdge=new float[36];
    final float[] hazeRibbon=new float[36],hazeCentre=new float[18];
    private static final float[] SIN_X=new float[COLS+1],COS_X=new float[COLS+1];
    static {for(int col=0;col<=COLS;col++) {
        float phase=(col*2f/COLS-.5f)*2.1f;
        SIN_X[col]=(float)Math.sin(phase);COS_X[col]=(float)Math.cos(phase);
    }}
    private int next,active;
    private float prepared=-1,preparedSky=-1,preparedWidth=-1,preparedHeight=-1;
    WaterRipples(){reset();}
    void reset(){java.util.Arrays.fill(born,-100);next=active=0;prepared=-1;}
    void cleared(GameCore c,Layout L,float cx,float cy) {
        if(c.state!=GameCore.PLAY || c.survival.active || Underwater.presence(c)<=0)return;
        born[next]=c.clock;sky[next]=c.skyClock;x[next]=cx/L.w;y[next]=cy/L.h;
        next=(next+1)%COUNT;prepared=-1;
    }
    void prepare(GameCore c,Layout L) {
        if(prepared==c.clock && preparedSky==c.skyClock && preparedWidth==L.w && preparedHeight==L.h)return;
        prepared=c.clock;preparedSky=c.skyClock;preparedWidth=L.w;preparedHeight=L.h;active=0;
        for(int i=0;i<COUNT;i++) {
            float age=c.clock-born[i];if(age<0 || age>=DURATION)continue;
            float fade=Math.max(0,1-age/GLOW_TIME);
            px[active]=x[i]*L.w;py[active]=y[i]*L.h+(c.skyClock-sky[i])*L.h*Underwater.SCROLL;
            brightness[active]=fade*fade*Math.min(1,.35f+age/.05f);
            radius[active]=L.w*(.12f+age*.14f);
            ages[active]=age;phase[active]=born[i]*.7f+x[i]*7+y[i]*3;active++;
        }
        // Separate x and y phases: three trig calls per row instead of two per grid cell.
        for(int row=0;row<=ROWS;row++) {
            float yy=(row*2f/ROWS-.5f)*L.h/L.w;
            float base=L.w*.006f*(float)Math.sin(yy*7.5f-c.clock*.85f);
            float phase=yy*13+c.clock*1.1f;
            float sy=L.w*.003f*(float)Math.sin(phase),cy=L.w*.003f*(float)Math.cos(phase);
            for(int col=0;col<=COLS;col++)field[row*(COLS+1)+col]=base+sy*COS_X[col]+cy*SIN_X[col];
        }
    }

    float refract(float x,float y) {
        float gx=Math.max(0,Math.min(COLS-.001f,(x/preparedWidth+.5f)*COLS*.5f));
        float gy=Math.max(0,Math.min(ROWS-.001f,(y/preparedHeight+.5f)*ROWS*.5f));
        int ix=(int)gx,iy=(int)gy,at=iy*(COLS+1)+ix;float u=gx-ix,v=gy-iy;
        float top=field[at]+(field[at+1]-field[at])*u;
        float bottom=field[at+COLS+1]+(field[at+COLS+2]-field[at+COLS+1])*u;
        return top+(bottom-top)*v;
    }
    void drawGlows(Painter p,GameCore c,Layout L,float fade) {
        if(fade<=0)return;
        prepare(c,L);
        for(int i=0;i<active;i++) {
            ArtStamp cache=p.stamps();
            boolean cached=brightness[i]<=0 || (cache!=null && cache.waterGlow(px[i],py[i],
                    radius[i]*2.8f,radius[i]*2.8f*.72f,Math.round(46*fade*brightness[i])));
            float previous=0;
            for(int layer=0;!cached && layer<12;layer++) {
                float distance=2.8f*(1-layer/12f);
                float opacity=46*fade*brightness[i]*(float)Math.exp(-.5f*distance*distance);
                int alpha=Math.round(opacity)-Math.round(previous);previous=opacity;
                if(alpha<=0)continue;
                int color=Glyph.mix(0xFF49B5CC,0xFFBBF5EB,layer/11f);
                p.fillEllipse(px[i],py[i],radius[i]*distance,radius[i]*distance*.72f,
                        Glyph.withAlpha(color,alpha));
            }
            plankton(p,L,i,fade);
        }
    }
    private void plankton(Painter p,Layout L,int burst,float fade) {
        float age=ages[burst],life=1-age/DURATION;
        float spread=1-(float)Math.exp(-age*1.7f);
        for(int j=0;j<PLANKTON;j++) {
            float angle=j*Softbody.TAU/PLANKTON+phase[burst];
            float dx=(float)Math.cos(angle),dy=(float)Math.sin(angle);
            float travel=L.w*(.13f+.04f*(j%3))*spread;
            float bend=L.w*.023f*spread*((float)Math.sin(age*(4+j%3)+angle)-(float)Math.sin(angle));
            float xx=px[burst]+dx*travel-dy*bend;
            float yy=py[burst]+dy*travel*.75f+dx*bend-age*L.h*.008f;
            float shimmer=.78f+.22f*(float)Math.sin(age*8+angle);
            float r=L.w*(.0025f+(j%3)*.0007f),alpha=fade*life*life*shimmer;
            int color=j%3==0?0xFFB7F5CC:j%3==1?0xFF6ED8EE:0xFFD1F7EE;
            p.fillCircle(xx,yy,r*3.6f,Glyph.withAlpha(color,(int)(alpha*22)));
            p.fillCircle(xx,yy,r,Glyph.withAlpha(color,(int)(alpha*205)));
        }
    }
}
