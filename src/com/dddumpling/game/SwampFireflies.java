package com.dddumpling.game;

/** A small resident swarm; clears startle existing insects without spawning particles. */
final class SwampFireflies extends Draw {
    static final int COUNT=12,PER_CLEAR=4;
    static final float DURATION=2.4f;
    final float[] age=new float[COUNT],startX=new float[COUNT],startY=new float[COUNT],pushX=new float[COUNT],pushY=new float[COUNT];
    private int stage=-1;
    SwampFireflies(){reset();}
    void reset(){for(int i=0;i<COUNT;i++)age[i]=-1;stage=-1;}
    static boolean enabled(GameCore c){return LandBonus.scene(c) && Lands.forStage(c.stage)==Lands.SWAMP;}
    void clean(GameCore c){if(!enabled(c) || stage!=c.stage){reset();if(enabled(c))stage=c.stage;}}
    void update(GameCore c,float dt) {
        clean(c);if(!enabled(c))return;
        for(int i=0;i<COUNT;i++)if(age[i]>=0){age[i]+=dt;if(age[i]>=DURATION)age[i]=-1;}
    }
    static float baseX(int i,float time){return .15f+hash(341+i*37)*.70f+.018f*(float)Math.sin(time*.37f+i*2.1f);}
    static float baseY(int i,float time){return .32f+hash(344+i*37)*.34f+.008f*(float)Math.sin(time*.31f+i*1.7f);}
    float offset(int i,boolean vertical) {
        if(age[i]<0)return 0;
        float u=Math.min(1,age[i]/DURATION),rest=(1-u)*(1-u);
        return (vertical?startY[i]:startX[i])*rest+(vertical?pushY[i]:pushX[i])*(1-(float)Math.exp(-12*u))*rest;
    }
    void cleared(GameCore c,Layout L,float x,float y) {
        clean(c);if(!enabled(c) || c.cloudUi.reduceMotion)return;
        int chosen=0;
        for(int n=0;n<PER_CLEAR;n++) {
            int best=-1;float distance=Float.MAX_VALUE;
            for(int i=0;i<COUNT;i++)if((chosen&(1<<i))==0) {
                float dx=(baseX(i,c.skyClock)+offset(i,false))*L.w-x,dy=(baseY(i,c.skyClock)+offset(i,true))*L.h-y;
                float d=dx*dx+dy*dy;if(d<distance){distance=d;best=i;}
            }
            chosen|=1<<best;
            float ox=offset(best,false),oy=offset(best,true);
            float dx=(baseX(best,c.skyClock)+ox)*L.w-x,dy=(baseY(best,c.skyClock)+oy)*L.h-y;
            if(Math.abs(dx)+Math.abs(dy)<.01f){dx=best%2==0?-1:1;dy=-.5f;}
            float length=(float)Math.sqrt(dx*dx+dy*dy);
            startX[best]=ox;startY[best]=oy;pushX[best]=dx/length*.22f;pushY[best]=dy/length*.22f*L.w/L.h;age[best]=0;
        }
    }
    void draw(Painter p,GameCore c,Layout L,float fade) {
        float time=c.cloudUi.reduceMotion?0:c.skyClock;
        for(int i=0;i<COUNT;i++) {
            float x=L.w*(baseX(i,time)+(c.cloudUi.reduceMotion?0:offset(i,false)));
            float y=L.h*(baseY(i,time)+(c.cloudUi.reduceMotion?0:offset(i,true)));
            float pulse=.5f+.5f*(float)Math.sin(time*.9f+i*2.4f),light=fade*(.35f+pulse*.65f),r=L.w*(.0018f+(i%3)*.0007f);
            p.fillCircle(x,y,r*5,Glyph.withAlpha(0xFFD5E58A,(int)(light*10)));
            p.fillCircle(x,y,r*2.5f,Glyph.withAlpha(0xFFD5E58A,(int)(light*30)));
            p.fillCircle(x,y,r,Glyph.withAlpha(0xFFFFF3BA,(int)(light*220)));
        }
    }
}
