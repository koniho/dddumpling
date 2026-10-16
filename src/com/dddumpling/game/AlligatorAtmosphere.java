package com.dddumpling.game;

/** Clock-driven fireflies and a bounded splash; no combat state or gameplay randomness. */
final class AlligatorAtmosphere extends Draw {
    static final float FALL_AT=.65f, SUBMERGED_AT=2.1f, SPLASH_AT=1.25f;
    static final int FIREFLIES=24, DROPLETS=128;
    private static final int[] WATER={0xFF368E99,0xFF56C4CB,0xFF91DFCC,0xFF72B5DA,0xFFB7F1DE};
    private AlligatorAtmosphere() {}
    private static float sample(int i,int salt) {
        int n=(i+1)*374761393+salt*668265263;
        n=(n^(n>>>13))*1274126177;
        return ((n^(n>>>16))&65535)/65535f;
    }
    static float waterY(Layout L) {return L.dangerY-L.unit*.1f;}
    static float fall(float seconds) {
        float t=Math.max(0,Math.min(1,(seconds-FALL_AT)/(SUBMERGED_AT-FALL_AT)));
        return t*t;
    }
    static float offset(Layout L,float seconds) {return L.h*fall(seconds);}
    static float waveX(Layout L,int arm,float time) {
        float side=arm==0?-1:1;
        return L.w*(.5f+side*(.28f+.12f*(float)Math.sin(time*5.5f+arm*Math.PI)));
    }
    static float waveY(Layout L,int arm,float time) {
        return L.playTop+(L.dangerY-L.playTop)*(.28f+.14f*(float)Math.cos(time*5.5f+arm*Math.PI));
    }
    static void fireflies(Painter p,Layout L,float time,float fade,boolean front) {
        if(fade<=0)return;
        for(int i=0;i<FIREFLIES;i++) {
            if((i%3!=0)!=front)continue;
            float phase=i*2.39996f;
            float x=L.w*(.08f+.84f*sample(i,1)+.045f*(float)Math.sin(time*.65f+phase));
            float y=L.playTop+(L.dangerY-L.playTop)*(.08f+.80f*sample(i,2)
                    +.055f*(float)Math.sin(time*.9f+phase*1.7f));
            float pulse=.3f+.7f*(.5f+.5f*(float)Math.sin(time*(1.6f+sample(i,3))+phase));
            float r=L.unit*(.065f+.055f*sample(i,4));
            p.fillCircle(x,y,r*4,fadeBy(0xFFDFFF8B,fade*pulse*.09f));
            p.fillCircle(x,y,r*2,fadeBy(0xFFF1F9A6,fade*pulse*.28f));
            p.fillCircle(x,y,r,fadeBy(0xFFFFFFD3,fade*(.45f+.55f*pulse)));
        }
    }
    static void water(Painter p,Layout L,float seconds,float alpha,boolean front) {
        float fade=alpha*AlligatorArrival.ramp(seconds,0,.3f)
                *(1-AlligatorArrival.ramp(seconds,2.85f,Boss.LEAVE));
        if(fade<=0)return;
        float y=waterY(L),x=L.w*.5f;
        if(!front) {
            p.fillEllipse(x,y+L.unit*.6f,L.w*.58f,L.unit*2,fadeBy(0xFF286B73,fade*.64f));
            p.fillEllipse(x,y+L.unit*.2f,L.w*.48f,L.unit*1.1f,fadeBy(0xFF56B5B4,fade*.42f));
        } else for(int i=0;i<4;i++) {
            float age=seconds-SPLASH_AT-i*.17f;
            if(age<0 || age>1.45f)continue;
            float u=age/1.45f;
            p.arc(x,y+L.unit*(.25f+i*.12f),L.w*(.10f+u*.61f),L.unit*(.3f+u*1.9f),
                    0,360,fadeBy(WATER[i],fade*(1-u)*.8f),L.unit*(.12f+.10f*(1-u)));
        }
        for(int i=0;i<DROPLETS;i++) {
            if((i%3!=0)!=front)continue;
            float u=(seconds-SPLASH_AT-sample(i,1)*.35f)/(.7f+sample(i,2)*.55f);
            if(u<=0 || u>=1)continue;
            float side=sample(i,3)-.5f;
            float xx=x+L.w*side*(.65f+u*.65f);
            float yy=y-L.h*(.055f+.22f*sample(i,4))*4*u*(1-u)+L.unit*u*u;
            float r=L.unit*(.12f+.8f*sample(i,5)*sample(i,5))*(1-u*.4f);
            float opacity=fade*AlligatorArrival.ramp(u,0,.05f)*(1-AlligatorArrival.ramp(u,.65f,1));
            p.fillCircle(xx,yy,r,fadeBy(WATER[i%WATER.length],opacity*.65f));
            p.fillCircle(xx-r*.22f,yy-r*.25f,r*.48f,fadeBy(0xFFDFFFF2,opacity*.55f));
        }
    }
}
