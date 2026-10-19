package com.dddumpling.game;

/** Draw-only victory doodle: the retained boss paints a sad face after the player loses. */
final class RainbowCatTaunt extends Draw {
    static final float DURATION=4.4f, NOTE_START=.18f, NOTE_STEP=.46f;
    static final int NOTES=7;
    private static final float[] START={.22f,1.42f,1.88f,2.36f}, END={1.25f,1.75f,2.21f,3.30f};
    static float pitch(int note) {return (float)Math.pow(2,(4-note)/12.0);}
    static float ink(int stroke,float age) {return Math.max(0,Math.min(1,(age-START[stroke])/(END[stroke]-START[stroke])));}
    static float radius(Layout L) {return Math.min(L.w*.30f,(L.dangerY-L.playTop)*.29f);}
    static float point(Layout L,int stroke,float u,int axis) {
        float r=radius(L),cx=L.w*.5f,cy=L.playTop+(L.dangerY-L.playTop)*.46f;
        if(stroke==0) {float angle=-Softbody.TAU*.25f+Softbody.TAU*u;return axis==0?cx+r*(float)Math.cos(angle):cy+r*(float)Math.sin(angle);}
        if(stroke<3)return axis==0?cx+r*((stroke==1?-.38f:.38f)+.05f*(float)Math.sin(u*Math.PI)):cy+r*(-.36f+.22f*u);
        return axis==0?cx+r*.60f*(2*u-1):cy+r*(.52f-.27f*(float)Math.sin(u*Math.PI));
    }
    static float flight(RainbowCat a,Layout L,float age,int axis) {
        if(age<START[0])return mix(axis==0?a.x:a.y,point(L,0,0,axis),ease(age/START[0]));
        for(int stroke=0;stroke<4;stroke++) {
            if(age<=END[stroke])return point(L,stroke,ink(stroke,age),axis);
            if(stroke<3 && age<START[stroke+1])return mix(point(L,stroke,1,axis),point(L,stroke+1,0,axis),ease((age-END[stroke])/(START[stroke+1]-END[stroke])));
        }
        float target=axis==0?L.w*.5f+radius(L)*.60f:L.playTop+(L.dangerY-L.playTop)*.46f-radius(L)*1.35f;
        return mix(point(L,3,1,axis),target,ease((age-END[3])/.45f));
    }
    private static float mix(float a,float b,float u) {return a+(b-a)*u;}
    private static float ease(float u) {u=Math.max(0,Math.min(1,u));return u*u*(3-2*u);}
    static float laugh(float age) {
        float beat=(age-NOTE_START)/NOTE_STEP;
        return .45f+.55f*Math.max(0,(float)Math.sin(beat*Softbody.TAU));
    }
    static void draw(Painter p,GameCore c,Layout L) {
        RainbowCat a=c.bossVictory.cat;float age=c.deathDuration()-c.deathT;
        float fade=Math.min(1,c.deathT/.55f);
        Painter q=new OpacityPainter(p,fade);
        for(int stroke=0;stroke<4;stroke++) {
            float progress=ink(stroke,age);if(progress<=0)continue;
            int samples=stroke==0?64:stroke==3?32:12;
            float[] strip=new float[(samples+1)*4];
            for(int band=0;band<6;band++) {
                float lo=(band-3)*L.w*.0065f,hi=lo+L.w*.0068f;
                for(int n=0;n<=samples;n++) {
                    float u=progress*n/samples;
                    float px=point(L,stroke,u,0),py=point(L,stroke,u,1);
                    float dx=point(L,stroke,Math.min(1,u+.002f),0)-point(L,stroke,Math.max(0,u-.002f),0);
                    float dy=point(L,stroke,Math.min(1,u+.002f),1)-point(L,stroke,Math.max(0,u-.002f),1);
                    float length=Math.max(.001f,(float)Math.hypot(dx,dy)),nx=-dy/length,ny=dx/length;
                    strip[n*2]=px+nx*lo;strip[n*2+1]=py+ny*lo;
                    int back=(samples*2+1-n)*2;strip[back]=px+nx*hi;strip[back+1]=py+ny*hi;
                }
                int color=RainbowCatArt.trailColor(q,band,RainbowCatArt.COLORS[band]);
                q.fillPoly(strip,Glyph.withAlpha(color,210));
            }
            for(int spark=0;spark<8;spark++) {
                float u=(spark+.5f)/8;if(u>progress)continue;
                float pulse=.5f+.5f*(float)Math.sin(age*8+spark*2.1f+stroke);
                float x=point(L,stroke,u,0),y=point(L,stroke,u,1),r=L.w*(.0025f+.0035f*pulse);
                q.line(x-r,y,x+r,y,0xBBFFFFFF,L.w*.0025f);q.line(x,y-r,x,y+r,0xBBFFFFFF,L.w*.0025f);
            }
        }
        float x=flight(a,L,age,0),y=flight(a,L,age,1);
        float pulse=laugh(age),scale=.65f+.035f*pulse;
        Painter actor=new BossScreen.PortraitFit(q,a.x,a.y,x,y-L.w*.006f*pulse,scale);
        RainbowCatArt.laughingCat(actor,a,age,pulse);
    }
}
