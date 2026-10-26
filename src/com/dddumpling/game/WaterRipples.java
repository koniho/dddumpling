package com.dddumpling.game;

/** Bounded, local refraction impulses. Derived coefficients are prepared once per rendered frame. */
final class WaterRipples {
    static final int COUNT=4;
    static final float DURATION=1.6f;
    private final float[] born=new float[COUNT],sky=new float[COUNT],x=new float[COUNT],y=new float[COUNT];
    private final float[] px=new float[COUNT],py=new float[COUNT],amplitude=new float[COUNT],radius2=new float[COUNT];
    private static final int COLS=8,ROWS=64;
    private final float[] field=new float[(COLS+1)*(ROWS+1)];
    final float[][] buffers=new float[96][];
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
            float fade=1-age/DURATION,r=L.w*(.28f+age*.35f);
            px[active]=x[i]*L.w;py[active]=y[i]*L.h+(c.skyClock-sky[i])*L.h*Underwater.SCROLL;
            amplitude[active]=L.w*.055f*fade*fade*(float)Math.sin(age*19);
            radius2[active]=r*r;active++;
        }
        // Evaluate waves once on a reusable grid, not for every vertex of every translucent layer.
        for(int row=0;row<=ROWS;row++)for(int col=0;col<=COLS;col++) {
            float xx=(col*2f/COLS-.5f)*L.w,yy=(row*2f/ROWS-.5f)*L.h;
            field[row*(COLS+1)+col]=WaterPainter.shift(c.clock,xx,yy,L.w,1)+shift(xx,yy,L.w);
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
    float shift(float x,float y,float width) {
        float sum=0;
        for(int i=0;i<active;i++) {
            float dx=x-px[i],dy=y-py[i],falloff=1-(dx*dx+dy*dy)/radius2[i];
            if(falloff>0)sum+=amplitude[i]*falloff*falloff;
        }
        // Fast multi-clears remain lively without moving art away from its input targets.
        return Math.max(-width*.028f,Math.min(width*.028f,sum));
    }
}
