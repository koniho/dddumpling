package com.dddumpling.game;

/** A woodland springboard, a shared seesaw, and a parachute that becomes a hat. */
final class MushroomSkits extends Draw {
    private static final int LIGHT=0xFFFFE8CE, GILLS=0xFF82675E, CAP=0xFFD998A2;
    private static final int MOSS=0xFF71917A, LEAF=0xFFACB394;
    // Cap deformations only change scale/shear; prepare the circular samples once.
    private static final float[] RIM=new float[48];
    static {
        for(int i=0;i<24;i++) {
            RIM[i*2]=(float)Math.cos(i*Softbody.TAU/24f);
            RIM[i*2+1]=(float)Math.sin(i*Softbody.TAU/24f);
        }
    }
    private MushroomSkits() {}

    private static float phase(float t,float start,float end) {
        return Math.max(0f,Math.min(1f,(t-start)/(end-start)));
    }
    private static float ease(float t,float start,float end) {
        float v=phase(t,start,end);return v*v*(3-2*v);
    }
    private static float hop(float t,float start,float end) {
        return (float)Math.sin(phase(t,start,end)*Math.PI);
    }
    private static float impact(float t,float at) {
        if(t<at)return 0f;
        float v=1f-phase(t,at,at+.16f);return v*v;
    }
    static void draw(Painter p,int variant,float x,float y,float r,float t,int a) {
        if(a<=2 || r<=0)return;
        t=Math.max(0f,Math.min(1f,t));
        clearing(p,x,y,r,a);
        if(variant==0)springboard(p,x,y,r,t,a);
        else if(variant==1)seesaw(p,x,y,r,t,a);
        else parachute(p,x,y,r,t,a);
    }

    private static void clearing(Painter p,float x,float y,float r,int a) {
        p.fillEllipse(x,y+r*1.12f,r*1.37f,r*.14f,Glyph.withAlpha(MOSS,a/3));
        for(int side=-1;side<=1;side+=2) {
            float bx=x+side*r*.95f,by=y+r*1.10f;
            for(int i=0;i<3;i++) {
                float dx=side*r*(.12f+i*.13f),dy=-r*(.44f-i*.10f);
                p.fillPoly(new float[]{bx,by,bx+dx*.2f-r*.09f,by+dy*.65f,
                        bx+dx,by+dy,bx+dx*.65f+r*.08f,by+dy*.45f},
                        Glyph.withAlpha(i==1?LEAF:MOSS,a*3/4));
            }
        }
    }

    /** Sheared ellipses keep the spots attached as the cap tips and squashes. */
    private static void oval(Painter p,float x,float y,float dx,float dy,float rx,float ry,
            float tilt,float squash,int color) {
        float[] rim=new float[RIM.length];
        for(int i=0;i<24;i++) {
            float u=dx+RIM[i*2]*rx,v=dy+RIM[i*2+1]*ry;
            rim[i*2]=x+u*squash;rim[i*2+1]=y+v/squash+u*tilt;
        }
        p.fillPoly(rim,color);
    }
    private static void cap(Painter p,float x,float y,float r,float tilt,float squash,int a) {
        oval(p,x,y,0,0,r*1.12f,r*.57f,tilt,squash,Glyph.withAlpha(CAP,a));
        oval(p,x,y,0,r*.21f,r,r*.15f,tilt,squash,Glyph.withAlpha(GILLS,a));
        for(int k=0;k<3;k++)
            oval(p,x,y,(k-1)*r*.51f,-r*(k==1?.29f:.12f),r*.13f,r*.10f,
                    tilt,squash,Glyph.withAlpha(LIGHT,a*4/5));
    }
    private static void stem(Painter p,float x,float top,float bottom,float r,int a) {
        if(bottom<=top)return;
        p.fillEllipse(x,(top+bottom)*.5f,r*.20f,(bottom-top)*.5f,Glyph.withAlpha(LIGHT,a));
    }
    private static void spores(Painter p,float x,float y,float r,float age,int a) {
        if(age<=0f || age>=1f)return;
        for(int i=0;i<7;i++) {
            float side=(i-3)/3f;
            float px=x+side*r*(.2f+age*1.35f);
            float py=y-r*(float)Math.sin(age*Math.PI)*(.30f+.10f*(i%3));
            float size=r*(.035f+.012f*(i%3));
            int alpha=(int)(a*(1f-age));
            p.fillCircle(px,py,size*1.8f,Glyph.withAlpha(LIGHT,alpha/5));
            p.fillCircle(px,py,size,Glyph.withAlpha(i%2==0?LIGHT:LEAF,alpha));
        }
    }

    /** Paws lift with the leap, then open into a pleased landing pose. */
    private static void friend(Painter p,int who,float x,float y,float r,int a,
            float squash,float happy,float lift) {
        int col=Glyph.withAlpha(Glyph.COLOR[who],a);
        for(int side=-1;side<=1;side+=2) {
            p.fillEllipse(x+side*r*.43f,y+r*.77f/squash,r*.22f,r*.13f,col);
            p.line(x+side*r*.66f*squash,y+r*.20f,
                    x+side*r*(1.06f+.12f*lift),y+r*(.34f-.95f*lift),col,r*.18f);
            p.fillCircle(x+side*r*(1.06f+.12f*lift),y+r*(.34f-.95f*lift),r*.14f,col);
        }
        Skits.face(p,who,x,y,r,a,squash,happy);
    }

    private static void springboard(Painter p,float x,float y,float r,float t,int a) {
        float press=hop(t,.03f,.28f),land=impact(t,.66f),leap=hop(t,.28f,.66f);
        float squash=1f+.32f*press+.26f*land;
        float cy=y+r*(.23f+.16f*(press+land)),cr=r*.86f;
        stem(p,x,cy,y+r*1.12f,r,a);
        cap(p,x,cy,cr,0,squash,a);
        float bodySquash=1f+.24f*(press+land)-leap*.13f;
        float py=cy-cr*.57f/squash-r*.42f/bodySquash-r*leap*.72f;
        friend(p,Kawaii.DUMPLING,x,py,r*.42f,a,bodySquash,
                .25f+.75f*ease(t,.24f,.42f),leap+.5f*ease(t,.76f,.86f));
        spores(p,x,cy+r*.13f,r,(t-.28f)/.30f,a);
        spores(p,x,cy+r*.13f,r,(t-.66f)/.30f,a);
    }

    private static void seesaw(Painter p,float x,float y,float r,float t,int a) {
        float left=impact(t,.32f),right=impact(t,.62f);
        float tilt=.24f*(hop(t,.05f,.32f)-hop(t,.32f,.62f))+.20f*(right-left);
        float cy=y+r*.19f,cr=r*1.08f;
        stem(p,x,cy,y+r*1.12f,r,a);
        cap(p,x,cy,cr,tilt,1f,a);
        for(int side=-1;side<=1;side+=2) {
            float leap=side<0?hop(t,.05f,.32f):hop(t,.32f,.62f);
            float settle=side<0?left:right;
            float squash=1f+.23f*settle-leap*.12f;
            float dx=side*r*.67f;
            float surface=cy-cr*.57f*(float)Math.sqrt(1f-dx*dx/(cr*cr*1.12f*1.12f))+dx*tilt;
            friend(p,side<0?Kawaii.CAT:Kawaii.STRAWBERRY,x+dx,
                    surface-r*.36f/squash-r*leap*.48f,r*.36f,a,squash,
                    .35f+.65f*ease(t,side<0?.15f:.38f,side<0?.30f:.55f),
                    leap+.65f*ease(t,.70f,.82f));
        }
        spores(p,x-r*.67f,cy,r*.55f,(t-.32f)/.26f,a);
        spores(p,x+r*.67f,cy,r*.55f,(t-.62f)/.28f,a);
    }

    private static void parachute(Painter p,float x,float y,float r,float t,int a) {
        float arrive=ease(t,0,.53f),hat=ease(t,.58f,.74f),land=impact(t,.53f);
        float sway=(float)Math.sin(arrive*Math.PI*3f)*(1f-arrive);
        float px=x+r*sway*.48f,py=y+r*(-.22f+arrive*.88f+land*.07f);
        float cy=py-r*(1.02f-hat*.61f),cr=r*(.67f-hat*.10f);
        p.fillEllipse(x,y+r*1.08f,r*(.28f+arrive*.28f),r*.08f,Glyph.withAlpha(GILLS,a/3));
        stem(p,px,cy,py-r*.14f,r*.48f,(int)(a*(1f-hat)));
        friend(p,Kawaii.STRAWBERRY,px,py,r*.43f,a,1f+land*.24f,
                .2f+.8f*ease(t,.53f,.72f),1f-.55f*hat);
        // Hands hold the stalk during flight; the empty cap settles over the leafy crown.
        if(hat<1f)for(int side=-1;side<=1;side+=2)
            p.line(px+side*r*.48f,py-r*.22f,px,py-r*.49f,
                    Glyph.withAlpha(Glyph.COLOR[Kawaii.STRAWBERRY],(int)(a*(1f-hat))),r*.07f);
        cap(p,px,cy,cr,sway*.16f,1f+hat*.12f,a);
        spores(p,x,y+r*.98f,r*.85f,(t-.53f)/.35f,a);
    }
}
