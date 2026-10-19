package com.dddumpling.game;

/** Scenery is the toy: six short setup, surprise and recovery vignettes. */
final class LandPlaySkits extends Draw {
    private static final int WATER=0xFF8DE3C8, PAD=0xFF6FC18C, LIGHT=0xFFCFF5AF;
    private LandPlaySkits() {}
    private static float phase(float t,float start,float end) {
        return Math.max(0,Math.min(1,(t-start)/(end-start)));
    }
    private static float hop(float t,float start,float end) {
        return (float)Math.sin(phase(t,start,end)*Math.PI);
    }
    private static float hit(float t,float at) {
        float u=1-phase(t,at,at+.14f);return t<at?0:u*u;
    }
    static void swamp(Painter p,int variant,float x,float y,float r,float t,int a) {
        if(a<=0 || r<=0)return;
        p=new OpacityPainter(p,a/255f);t=Math.max(0,Math.min(1,t));
        p.fillEllipse(x,y+r*.86f,r*2.15f,r*.36f,0x66357B75);
        p.arc(x,y+r*.84f,r*1.9f,r*.25f,0,180,0x886CCAB5,r*.035f);
        // Reeds frame the action without competing with the characters.
        for(int side=-1;side<=1;side+=2)for(int i=0;i<2;i++) {
            float xx=x+side*r*(1.75f+i*.23f),lean=r*.12f*(float)Math.sin(t*7+i);
            p.polyline(new float[]{xx,y+r*.8f,xx+lean,y,xx+lean*1.5f,y-r*(.5f+i*.3f)},PAD,r*.045f);
            p.line(xx+lean*1.5f,y-r*(.5f+i*.3f),xx+lean*1.5f,y-r*(.25f+i*.3f),0xFF9A756B,r*.13f);
        }
        if(variant==0)padHop(p,x,y,r,t);
        else if(variant==1)wade(p,x,y,r,t);
        else fireflies(p,x,y,r,t);
    }
    private static void pad(Painter p,float x,float y,float r,float squash) {
        p.fillEllipse(x,y+r*.06f,r,r*.29f,0xFF408575);
        p.fillEllipse(x,y,r*(1+squash*.12f),r*(.23f-squash*.05f),PAD);
        p.line(x-r*.65f,y-r*.04f,x+r*.65f,y-r*.04f,LIGHT,r*.035f);
        p.fillPoly(new float[]{x,y,x+r*.85f,y-r*.12f,x+r*.9f,y+r*.09f},0xFF397F71);
    }
    private static void splash(Painter p,float x,float y,float r,float age,boolean stars) {
        if(age<=0 || age>=1)return;
        for(int i=0;i<10;i++) {
            float side=(i-4.5f)/4.5f,px=x+side*r*1.6f*age;
            float py=y-r*4*age*(1-age)*(.6f+.17f*(i%3));
            int color=fadeBy(stars?RainbowLand.CLEAR_COLORS[i%6]:i%2==0?WATER:LIGHT,1-age);
            float size=r*(.065f+.015f*(i%3))*(1-age*.4f);
            if(stars)p.fillPoly(star(px,py,size*1.3f,size*.55f,4,age*3+i),color);
            else {
                p.fillEllipse(px,py,size,size*1.5f,color);
                p.fillCircle(px-size*.2f,py-size*.4f,size*.3f,fadeBy(0xFFFFFFFF,(1-age)*.7f));
            }
        }
        p.arc(x,y,r*(.25f+age*1.3f),r*(.06f+age*.2f),0,180,fadeBy(WATER,1-age),r*.035f);
    }
    private static void face(Painter p,int glyph,float x,float y,float r,float squash,float joy) {
        Skits.face(p,glyph,x,y,r,255,squash,joy);
    }
    private static void padHop(Painter p,float x,float y,float r,float t) {
        float first=hit(t,.32f),second=hit(t,.69f);
        for(int i=0;i<3;i++)pad(p,x+(i-1)*r*1.2f,y+r*.65f,r*.60f,i==1?first:second);
        float px=x+r*(-1.2f+1.2f*phase(t,.06f,.32f)+1.2f*phase(t,.40f,.69f));
        float leap=hop(t,.06f,.32f)*1.05f+hop(t,.40f,.69f)*1.45f;
        face(p,Kawaii.DUMPLING,px,y+r*(.15f-leap+(first+second)*.12f),r*.45f,
                1+.35f*(first+second)-leap*.12f,1);
        splash(p,x,y+r*.63f,r,(t-.32f)/.28f,false);
        splash(p,x+r*1.2f,y+r*.63f,r*.75f,(t-.69f)/.3f,false);
        // The last pad wobbles; our spectator bounces in sympathy.
        face(p,Kawaii.STRAWBERRY,x-r*1.95f,y+r*(.2f-hop(t,.70f,.94f)*.35f),r*.32f,1+second*.2f,1);
    }
    private static void fireflies(Painter p,float x,float y,float r,float t) {
        float chase=phase(t,.08f,.6f),jump=hop(t,.36f,.68f),land=hit(t,.68f);
        float px=x+r*(-1.1f+chase*1.7f),py=y+r*(.15f-jump*.9f+land*.12f);
        pad(p,x+r*.5f,y+r*.7f,r*.95f,land);
        face(p,Kawaii.BLOB,px,py,r*.46f,1-jump*.15f+land*.25f,t>.68f?1:.6f);
        float crown=phase(t,.65f,.9f);
        for(int i=0;i<6;i++) {
            float angle=t*9+i*Softbody.TAU/6;
            float fx=x+r*(.6f+(float)Math.cos(angle)*.95f),fy=y-r*(.5f+.65f*(float)Math.sin(angle));
            float targetX=px+(i-2.5f)*r*.16f,targetY=py-r*(.62f+.12f*(1-Math.abs(i-2.5f)/2.5f));
            fx+=(targetX-fx)*crown;fy+=(targetY-fy)*crown;
            float glow=.65f+.35f*(float)Math.sin(t*28+i);
            p.fillCircle(fx,fy,r*.18f,fadeBy(LIGHT,.14f*glow));
            p.fillCircle(fx,fy,r*.09f,fadeBy(LIGHT,.28f*glow));
            p.fillCircle(fx,fy,r*.035f,fadeBy(0xFFFFFFC5,glow));
        }
        splash(p,px,y+r*.65f,r*.9f,(t-.68f)/.28f,false);
    }
    private static void wade(Painter p,float x,float y,float r,float t) {
        float travel=phase(t,.06f,.86f),stride=travel*4;
        float step=stride-(float)Math.floor(stride),lift=(float)Math.sin(step*Math.PI);
        // Pause on each planted foot, then lean forward into the next sticky step.
        float distance=((float)Math.floor(stride)+step*step*(3-2*step))/4;
        float px=x+r*(-1.25f+distance*2.4f),water=y+r*.52f;
        float py=y+r*(.36f-lift*.19f),settle=hop(t,.86f,1);
        for(int i=0;i<4;i++) {
            float age=(t-(.06f+i*.2f))/.30f;
            splash(p,x+r*(-1.25f+i*.6f),water,r*.46f,age,false);
        }
        // Submerge the lower body; the moving meniscus sits in front of the cut.
        p.save();p.clipRect(x-r*3,y-r*3,x+r*3,water+r*.045f);
        face(p,Kawaii.BLOB,px+r*.10f*lift,py,r*.48f,1+.12f*lift-.08f*settle,t>.84f?1:.35f);
        p.restore();
        p.fillEllipse(px,water+r*.1f,r*.62f,r*.13f,0x66357B75);
        for(int wake=0;wake<3;wake++) {
            float width=r*(.58f+wake*.26f+lift*.12f);
            p.arc(px-r*wake*.18f,water+r*.06f,width,r*(.09f+wake*.055f),5,170,
                    fadeBy(WATER,.75f-wake*.19f),r*.035f);
        }
        // One stubborn pad clings like a tiny hat, wobbling with every step.
        pad(p,px+r*.10f*lift,py-r*.41f,r*.33f,.3f*lift);
        for(int i=0;i<3;i++) {
            float fx=px+r*(.55f+i*.21f),fy=y-r*(.35f+.25f*(float)Math.sin(t*9+i));
            float glow=.65f+.35f*(float)Math.sin(t*28+i);
            p.fillCircle(fx,fy,r*.15f,fadeBy(LIGHT,.18f*glow));
            p.fillCircle(fx,fy,r*.06f,fadeBy(LIGHT,.4f*glow));
            p.fillCircle(fx,fy,r*.028f,fadeBy(0xFFFFFFC5,glow));
        }
    }
    static void rainbow(Painter p,int variant,float x,float y,float r,float t,int a) {
        if(a<=0 || r<=0)return;
        p=new OpacityPainter(p,a/255f);t=Math.max(0,Math.min(1,t));
        if(variant==0)trampoline(p,x,y,r,t);
        else if(variant==1)slide(p,x,y,r,t);
        else starCatch(p,x,y,r,t);
    }
    private static void cloud(Painter p,float x,float y,float r,float squash) {
        RainbowLand.cloud(p,x,y,r*(1+squash*.2f),r*(.48f-squash*.12f),255,false,0,4);
    }
    private static void trampoline(Painter p,float x,float y,float r,float t) {
        float press=hop(t,.04f,.26f),leap=hop(t,.26f,.72f),land=hit(t,.72f);
        cloud(p,x,y+r*.7f,r*1.4f,press+land);
        cloud(p,x-r*1.65f,y+r*.45f,r*.55f,0);
        cloud(p,x+r*1.65f,y+r*.45f,r*.55f,0);
        float py=y+r*(.05f+press*.23f-leap*1.65f+land*.13f);
        face(p,Kawaii.STRAWBERRY,x,py,r*.48f,1+press*.3f+land*.35f-leap*.15f,1);
        splash(p,x,y+r*.47f,r*1.2f,(t-.26f)/.30f,true);
        splash(p,x,y+r*.47f,r*1.3f,(t-.72f)/.28f,true);
        // Landing squeezes a tiny rainbow out of the cloud.
        float reveal=phase(t,.72f,.90f);
        if(reveal>0)for(int i=0;i<6;i++)p.arc(x,y+r*.65f,r*(.25f+i*.12f)*reveal,
                r*(.18f+i*.08f)*reveal,180,180,fadeBy(RainbowLand.CLEAR_COLORS[i],.7f),r*.075f);
    }
    private static void slide(Painter p,float x,float y,float r,float t) {
        cloud(p,x-r*1.35f,y-r*.8f,r*.65f,0);
        float landing=hit(t,.73f);
        cloud(p,x+r*1.3f,y+r*.7f,r*.85f,landing);
        for(int band=0;band<6;band++) {
            float[] ribbon=new float[34];
            for(int i=0;i<=16;i++) {
                float u=i/16f;
                ribbon[i*2]=x+r*(-1.35f+2.65f*u);
                ribbon[i*2+1]=y+r*(-.75f+1.2f*u*u+band*.065f);
            }
            p.polyline(ribbon,fadeBy(RainbowLand.CLEAR_COLORS[band],.8f),r*.075f);
        }
        float u=phase(t,.15f,.73f);u=u*u;
        float px=x+r*(-1.35f+2.65f*u),py=y+r*(-1.12f+1.2f*u*u+landing*.15f);
        face(p,Kawaii.CAT,px,py,r*.40f,1+landing*.35f,.6f+.4f*u);
        splash(p,x+r*1.3f,y+r*.6f,r,(t-.73f)/.27f,true);
    }
    private static void starCatch(Painter p,float x,float y,float r,float t) {
        float jump=hop(t,.30f,.66f),land=hit(t,.66f);
        cloud(p,x,y+r*.72f,r*1.5f,land);
        float py=y+r*(.15f-jump*.75f+land*.12f);
        face(p,Kawaii.DUMPLING,x,py,r*.46f,1-jump*.15f+land*.3f,1);
        for(int i=0;i<3;i++) {
            float catchAt=.48f+i*.07f,fall=phase(t,.05f+i*.06f,catchAt);
            float sx=x+(i-1)*r*(1.4f-.95f*fall),sy=y+r*(-1.7f+1.02f*fall);
            if(t>catchAt) {
                float settle=phase(t,catchAt,catchAt+.18f);
                sx+=(x+(i-1)*r*.35f-sx)*settle;sy+=(py-r*(.55f+(i==1?.18f:0))-sy)*settle;
            }
            float size=r*(.19f+.04f*(float)Math.sin(t*12+i));
            p.fillCircle(sx,sy,size*1.6f,0x22FFF0A0);
            p.fillPoly(star(sx,sy,size,size*.48f,5,(1-fall)*2),RainbowLand.CLEAR_COLORS[i*2]);
            splash(p,sx,sy,r*.45f,(t-catchAt)/.22f,true);
        }
    }
}
