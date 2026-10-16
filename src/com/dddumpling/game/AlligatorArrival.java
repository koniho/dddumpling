package com.dddumpling.game;

/** One opaque bottom-up entrance with bounded, deterministic water droplets. */
final class AlligatorArrival extends Draw {
    static final int PARTICLES=480;
    static final float SPLASH_AT=.025f;
    private static final int[] COLORS={0xFF348C9C,0xFF55BFC5,0xFF86DACD,
            0xFF427EB0,0xFF72BADB,0xFFB5ECE0,0xFF54AFA2};
    private static final float[] DROPS=new float[PARTICLES*7];
    private static final int[] HIGHLIGHTS=new int[COLORS.length];
    static {
        for(int i=0;i<PARTICLES;i++) {
            int k=i*7;float size=sample(i,7);
            DROPS[k]=SPLASH_AT+.49f*sample(i,1);DROPS[k+1]=.24f+.22f*sample(i,2);
            DROPS[k+2]=.06f+.88f*sample(i,3);DROPS[k+3]=(sample(i,4)-.5f)*.55f;
            DROPS[k+4]=.91f+.05f*sample(i,5);DROPS[k+5]=.055f+.21f*sample(i,6);
            DROPS[k+6]=.12f+.90f*size*size;
        }
        for(int i=0;i<COLORS.length;i++)HIGHLIGHTS[i]=Glyph.mix(COLORS[i],0xFFD5F8EE,.5f);
    }
    private AlligatorArrival() {}
    static float shake(float before,float after) {
        if(before<.88f && after>=.88f)return .65f;
        if(before<.48f && after>=.48f)return .45f;
        return before<.20f && after>=.20f?.30f:0;
    }
    static float ramp(float t,float start,float end) {
        float u=Math.max(0,Math.min(1,(t-start)/(end-start)));return u*u*(3-2*u);
    }
    static float offset(Layout L,float t) {return L.h*(1-ramp(t,0,.88f));}
    private static float sample(int i,int salt) {
        int n=(i+1)*374761393+salt*668265263;
        n=(n^(n>>>13))*1274126177;
        return ((n^(n>>>16))&65535)/65535f;
    }
    static void particles(Painter p,Layout L,float t,boolean front) {
        if(t<=0 || t>=1)return;
        for(int i=0;i<PARTICLES;i++) {
            if((i%3!=0)!=front)continue;
            int k=i*7;
            float u=(t-DROPS[k])/DROPS[k+1];
            if(u<=0 || u>=1)continue;
            float x=L.w*(DROPS[k+2]+DROPS[k+3]*u);
            float y=L.h*DROPS[k+4]-L.h*DROPS[k+5]*4*u*(1-u)+L.h*.10f*u*u;
            float r=L.unit*DROPS[k+6]*(1-.3f*u);
            float opacity=ramp(u,0,.10f)*(1-ramp(u,.65f,1));
            if(x+r<0 || x-r>L.w || y-r>L.h || y+r<0)continue;
            int color=COLORS[i%COLORS.length];
            p.fillCircle(x,y,r,fadeBy(color,opacity*.42f));
            p.fillCircle(x-r*.22f,y-r*.25f,r*.55f,
                    fadeBy(HIGHLIGHTS[i%COLORS.length],opacity*.32f));
        }
    }
}
