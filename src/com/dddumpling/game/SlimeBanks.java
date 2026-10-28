package com.dddumpling.game;

/** Bounded bank springs and impact lights; no simulation or gameplay randomness. */
final class SlimeBanks {
    static final int ROWS=8;
    final float[] shimmer=new float[36];
    private final float[] hitAt={-100,-100},hitY={.5f,.5f};
    private final float[][] offsets={new float[ROWS+1],new float[ROWS+1]};
    private final float[] lightAt={-100,-100};
    final float[] lightX=new float[2],lightY=new float[2];
    private float previousY,previousX,descending,previousTime=-1,lastDip=-100;
    final float[] dipAt={-100,-100,-100},dipX=new float[3],dipY=new float[3];
    private int nextDip;
    void reset(){
        hitAt[0]=hitAt[1]=lightAt[0]=lightAt[1]=lastDip=-100;
        previousTime=-1;descending=0;nextDip=0;
        java.util.Arrays.fill(dipAt,-100);
    }
    boolean recoiling(float time,int side) {return time>=hitAt[side] && time-hitAt[side]<4;}
    void updateDip(GameCore c,Layout L) {
        Boss b=c.boss;
        if(c.state!=GameCore.PLAY || b.kind!=Boss.SLIME || !b.fighting() || b.body==null) {
            previousTime=-1;descending=0;return;
        }
        float y=b.body.bottom(),x=b.body.centreX();
        if(previousTime>=0 && c.clock>previousTime) {
            float delta=y-previousY;
            // A true downward-to-upward turn, not tiny spring jitter or an arrival jump.
            if(delta<0 && descending>L.w*.0015f && c.clock-lastDip>1.4f && b.hurt<.1f) {
                dipAt[nextDip]=c.clock;dipX[nextDip]=previousX;
                // The rendered skirt can cover the prompt below the physical spring body.
                float bottom=y;float[] skirt=BossScreen.slimePromptOutline(L,b);
                for(int i=1;i<skirt.length;i+=2)bottom=Math.max(bottom,skirt[i]);
                dipY[nextDip]=bottom-b.bodyR(L)*.025f;
                nextDip=(nextDip+1)%dipAt.length;lastDip=c.clock;
            }
            if(delta>0)descending+=delta;else if(delta<0)descending=0;
        }
        previousY=y;previousX=x;previousTime=c.clock;
    }
    void flash(float time,float x,float y,boolean drag) {
        int i=drag?1:0;lightAt[i]=time;lightX[i]=x;lightY[i]=y;
    }
    float light(float time,int i) {
        float age=time-lightAt[i];
        if(age<0)return 0;
        float fade=Math.max(0,1-age/(i==0?.55f:.95f));
        return fade*fade;
    }
    void hit(float time,float x,float y,Layout L) {
        flash(time,x,y,true);
        int side=x<L.w*.5f?0:1;hitAt[side]=time;
        hitY[side]=Math.max(.08f,Math.min(.92f,(y-SlimeBasin.bankTop(time,side,L))/SlimeBasin.bankHeight(time,side,L)));
    }
    float kick(float time,int side,float u,Layout L) {
        float age=time-hitAt[side];if(age<0 || age>4)return 0;
        // Spread the impact through neighboring shelves while keeping the opposite bank still.
        float d=(u-hitY[side])/.24f,falloff=Math.max(0,1-d*d);
        return -L.w*.23f*(float)(Math.sin(age*15)*Math.exp(-age*1.35f))*falloff*falloff;
    }
    float[] offsets(float time,int side,Layout L) {
        for(int row=0;row<=ROWS;row++) {
            float u=row/(float)ROWS;
            offsets[side][row]=L.w*.003f*(float)Math.sin(u*7.5f-time*.7f+side*2.4f)
                    +kick(time,side,u,L);
        }
        return offsets[side];
    }
    static float sample(float[] offsets,float u) {
        float row=Math.max(0,Math.min(ROWS,u*ROWS));int i=Math.min(ROWS-1,(int)row);
        return offsets[i]+(offsets[i+1]-offsets[i])*(row-i);
    }
    /** Shared software fallback maps the same bank contours onto the cached texture's mesh. */
    static final class Warp extends OpacityPainter {
        private final float x,y,w,h;private final int side;private final float[] offsets;
        private float tx,ty;private final float[] stack=new float[16];private int sp;
        Warp(Painter p,int alpha,int side,float x,float y,float w,float h,float[] offsets) {
            super(p,alpha/255f);this.side=side;this.x=x;this.y=y;this.w=w;this.h=h;this.offsets=offsets;
        }
        public void fillPoly(float[] shape,int c) {
            float[] out=new float[shape.length];
            for(int i=0;i<shape.length;i+=2) {
                float xx=shape[i]+tx,yy=shape[i+1]+ty,offset=sample(offsets,yy/h);
                out[i]=x+xx+(side==0?xx/w:xx/w-1)*offset;out[i+1]=y+yy;
            }
            p.fillPoly(out,color(c));
        }
        public void save(){stack[sp++]=tx;stack[sp++]=ty;}
        public void restore(){ty=stack[--sp];tx=stack[--sp];}
        public void translate(float x,float y){tx+=x;ty+=y;}
        // Scenery already clips the playfield. The inner bank image margin is transparent.
        public void clipRect(float l,float t,float r,float b){}
    }
}
