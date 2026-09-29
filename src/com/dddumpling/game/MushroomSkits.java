package com.dddumpling.game;

/** Spotted caps become springboards, seesaws, and tiny parachutes. */
final class MushroomSkits extends Draw {
    private static final int LIGHT=0xFFFFE3C6, GILLS=0xFF775968;
    private MushroomSkits() {}

    private static float phase(float t,float start,float end) {
        return Math.max(0f,Math.min(1f,(t-start)/(end-start)));
    }
    private static float hop(float t,float start,float end) {
        return (float)Math.sin(phase(t,start,end)*Math.PI);
    }
    private static float impact(float t,float at) {
        if(t<at)return 0f;
        float v=1f-phase(t,at,at+.18f);return v*v;
    }
    static void draw(Painter p,int variant,float x,float y,float r,float t,int a) {
        if(a<=2 || r<=0)return;
        t=Math.max(0f,Math.min(1f,t));
        if(variant==0)springboard(p,x,y,r,t,a);
        else if(variant==1)seesaw(p,x,y,r,t,a);
        else parachute(p,x,y,r,t,a);
    }

    /** Sheared ellipses keep the familiar spotted cap together as it tips and squashes. */
    private static void oval(Painter p,float x,float y,float dx,float dy,float rx,float ry,
            float tilt,float squash,int color) {
        float[] rim=new float[48];
        for(int i=0;i<24;i++) {
            float angle=i*Softbody.TAU/24f;
            float u=dx+(float)Math.cos(angle)*rx,v=dy+(float)Math.sin(angle)*ry;
            rim[i*2]=x+u*squash;rim[i*2+1]=y+v/squash+u*tilt;
        }
        p.fillPoly(rim,color);
    }
    private static void cap(Painter p,float x,float y,float r,float tilt,float squash,int a) {
        int col=Glyph.withAlpha(Glyph.mix(Sky.CLOUD_TINT[1],Lands.TINT[3],.5f),a);
        oval(p,x,y,0,0,r*1.12f,r*.65f,tilt,squash,col);
        oval(p,x,y,0,r*.21f,r,r*.15f,tilt,squash,Glyph.withAlpha(GILLS,a));
        for(int k=0;k<3;k++)
            oval(p,x,y,(k-1)*r*.51f,-r*(k==1?.31f:.12f),r*.12f,r*.12f,
                    tilt,squash,Glyph.withAlpha(LIGHT,a*2/3));
    }
    private static void stem(Painter p,float x,float top,float bottom,float r,int a) {
        if(bottom<=top)return;
        p.fillEllipse(x,(top+bottom)*.5f,r*.20f,(bottom-top)*.5f,Glyph.withAlpha(LIGHT,a*2/3));
    }
    private static void spores(Painter p,float x,float y,float r,float age,int a) {
        if(age<=0f || age>=1f)return;
        for(int i=0;i<11;i++) {
            float side=(i-5)/5f;
            float px=x+side*r*(.2f+age*1.55f);
            float py=y-r*(float)Math.sin(age*Math.PI)*(.45f+.12f*(i%3));
            float size=r*(.035f+.012f*(i%3))*(1f+age*.5f);
            int alpha=(int)(a*(1f-age));
            p.fillCircle(px,py,size*1.8f,Glyph.withAlpha(LIGHT,alpha/5));
            p.fillCircle(px,py,size,Glyph.withAlpha(i%2==0?LIGHT:0xFFF4B7D0,alpha));
        }
    }

    private static void springboard(Painter p,float x,float y,float r,float t,int a) {
        float press=hop(t,0,.28f),land=impact(t,.70f),leap=hop(t,.28f,.70f);
        float squash=1f+.40f*press+.33f*land;
        float cy=y+r*(.15f+.16f*(press+land)),cr=r*.86f;
        stem(p,x,cy,y+r*1.25f,r,a);
        cap(p,x,cy,cr,0,squash,a);
        float bodySquash=1f+.22f*(press+land)-leap*.14f;
        float py=cy-cr*.65f/squash-r*.42f/bodySquash-r*leap*1.05f;
        Skits.face(p,Kawaii.DUMPLING,x,py,r*.42f,a,bodySquash,1f);
        spores(p,x,cy+r*.13f,r,(t-.28f)/.34f,a);
        spores(p,x,cy+r*.13f,r*1.2f,(t-.70f)/.3f,a);
    }

    private static void seesaw(Painter p,float x,float y,float r,float t,int a) {
        float left=impact(t,.34f),right=impact(t,.66f),tilt=.32f*(right-left);
        float cy=y+r*.05f,cr=r*1.1f;
        stem(p,x,cy,y+r*1.25f,r,a);
        cap(p,x,cy,cr,tilt,1f,a);
        for(int side=-1;side<=1;side+=2) {
            float leap=side<0?hop(t,.06f,.34f):hop(t,.34f,.66f);
            float squash=1f+.2f*(side<0?left:right)-leap*.15f;
            float dx=side*r*.70f;
            float surface=cy-cr*.65f*(float)Math.sqrt(1f-dx*dx/(cr*cr*1.12f*1.12f))+dx*tilt;
            Skits.face(p,side<0?Kawaii.CAT:Kawaii.STRAWBERRY,x+dx,
                    surface-r*.38f/squash-r*leap*.60f,r*.38f,a,squash,1f);
        }
        spores(p,x-r*.70f,cy,r*.72f,(t-.34f)/.3f,a);
        spores(p,x+r*.70f,cy,r*.72f,(t-.66f)/.34f,a);
    }

    private static void parachute(Painter p,float x,float y,float r,float t,int a) {
        float arrive=phase(t,0,.60f),hat=phase(t,.64f,.78f),land=impact(t,.60f);
        float sway=(float)Math.sin(arrive*Math.PI*3f)*(1f-arrive);
        float px=x+r*sway*.60f,py=y+r*(-.72f+arrive*1.10f+land*.10f);
        float cy=py-r*(1.10f-hat*.72f),cr=r*(.67f-hat*.10f);
        p.fillEllipse(x,y+r*.88f,r*(.28f+arrive*.35f),r*.09f,Glyph.withAlpha(GILLS,a/3));
        stem(p,px,cy,py-r*.12f,r*.55f,(int)(a*(1f-hat)));
        if(hat<1f)for(int side=-1;side<=1;side+=2)
            p.polyline(new float[]{px+side*r*.31f,py,px+side*r*.35f,py-r*.35f,
                    px,py-r*.49f},Glyph.withAlpha(Glyph.COLOR[Kawaii.STRAWBERRY],(int)(a*(1f-hat))),r*.06f);
        Skits.face(p,Kawaii.STRAWBERRY,px,py,r*.43f,a,1f+land*.25f,1f);
        cap(p,px,cy,cr,sway*.20f,1f+hat*.12f,a);
        spores(p,x,y+r*.73f,r,(t-.60f)/.4f,a);
    }
}
