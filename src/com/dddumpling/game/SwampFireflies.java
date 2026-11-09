package com.dddumpling.game;

/** A small resident swarm; clears startle existing insects without spawning particles. */
final class SwampFireflies extends Draw {
    static final int COUNT=16,PER_CLEAR=6;
    static final float DURATION=2.4f;
    private static final float[] CLUSTER_X={.24f,.76f,.35f,.66f},CLUSTER_Y={.43f,.47f,.60f,.67f};
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
    static float baseX(int i,float time) {
        int group=i/4,local=i%4;
        return CLUSTER_X[group]+.025f*(float)Math.sin(time*.13f+group*2)
                +(.018f+local*.006f)*(float)Math.cos(time*(.28f+local*.03f)+i*2.4f);
    }
    static float baseY(int i,float time) {
        int group=i/4,local=i%4;
        return CLUSTER_Y[group]+.012f*(float)Math.cos(time*.11f+group*2)
                +(.007f+local*.003f)*(float)Math.sin(time*(.28f+local*.03f)+i*2.4f);
    }
    float offset(int i,boolean vertical) {
        if(age[i]<0)return 0;
        float u=Math.min(1,age[i]/DURATION),rest=(1-u)*(1-u);
        float flutter=(float)Math.sin(u*Math.PI)*(float)Math.sin(u*Softbody.TAU*2+i)*.20f;
        float side=vertical?pushX[i]*.45f:-pushY[i];
        return (vertical?startY[i]:startX[i])*rest+(vertical?pushY[i]:pushX[i])*(1-(float)Math.exp(-12*u))*rest
                +side*flutter*rest;
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
        float time=c.cloudUi.reduceMotion?0:c.skyClock;float[] sparkle=new float[8];
        for(int i=0;i<COUNT;i++) {
            float x=L.w*(baseX(i,time)+(c.cloudUi.reduceMotion?0:offset(i,false)));
            float y=L.h*(baseY(i,time)+(c.cloudUi.reduceMotion?0:offset(i,true)));
            float burst=c.cloudUi.reduceMotion || age[i]<0?0:Math.max(0,1-age[i]/DURATION);
            burst*=burst;
            float pulse=.5f+.5f*(float)Math.sin(time*1.3f+i*2.4f),light=fade*Math.min(1,.35f+pulse*.65f+burst*.45f);
            float r=L.w*(.0018f+(i%3)*.0007f),glow=1+burst*.55f;
            p.fillCircle(x,y,r*5*glow,Glyph.withAlpha(0xFFD5E58A,(int)(light*(14+burst*14))));
            p.fillCircle(x,y,r*2.5f*glow,Glyph.withAlpha(0xFFE5EEA3,(int)(light*(36+burst*20))));
            if(!c.cloudUi.reduceMotion && (burst>.15f || pulse>.91f && i%3==0)) {
                sparkle[0]=x;sparkle[1]=y-r*2.7f;sparkle[2]=x+r*1.4f;sparkle[3]=y;
                sparkle[4]=x;sparkle[5]=y+r*2.7f;sparkle[6]=x-r*1.4f;sparkle[7]=y;
                p.fillPoly(sparkle,Glyph.withAlpha(0xFFFFF5C6,(int)(light*245)));
            } else p.fillCircle(x,y,r,Glyph.withAlpha(0xFFFFF3BA,(int)(light*230)));
        }
    }
}
