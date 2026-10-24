package com.dddumpling.game;

/** A flowing jelly valley. Bounded shared vector layers work on both native painters. */
final class SlimeHills extends Draw {
    static final int DEEP=0xFF245953, GREEN=0xFF64BB80, MINT=0xFFB7F0B4;
    private SlimeHills() {}

    static float presence(GameCore c) {
        if(c.survival.active || c.state==GameCore.TITLE)return 0;
        float mix=Lands.blend(c);
        return (c.landFrom==0?1-mix:0)+(Lands.forStage(Lands.visualStage(c))==0?mix:0);
    }

    static void scenery(Painter p,GameCore c,Layout L,float fade) {
        fade*=1-c.drained();
        if(fade<=.001f)return;
        float[] bank=new float[70],edge=new float[66],blob=new float[66];
        float span=L.deckTop-L.playTop,clock=c.skyClock;
        // Continuous banks have no tile seams; the inner contour travels down the valley.
        for(int layer=0;layer<3;layer++)for(int side=0;side<2;side++) {
            for(int j=0;j<=32;j++) {
                float y=L.playTop-span*.12f+j*span*1.24f/32;
                float phase=y/L.w*4-clock*(.19f+layer*.10f)+side*3.7f+layer*1.3f;
                float inset=L.w*(.25f-layer*.064f+.047f*(float)Math.sin(phase)
                        +.023f*(float)Math.sin(phase*1.71f+1.4f));
                float x=side==0?inset:L.w-inset;
                bank[j*2]=edge[j*2]=x;bank[j*2+1]=edge[j*2+1]=y;
            }
            float outside=side==0?-L.w*.1f:L.w*1.1f;
            bank[66]=bank[68]=outside;bank[67]=bank[65];bank[69]=bank[1];
            p.fillPoly(bank,Glyph.withAlpha(layer==0?0xFF458577:layer==1?GREEN:DEEP,(int)(fade*(layer==0?95:150))));
            p.polyline(edge,Glyph.withAlpha(MINT,(int)(fade*(layer==1?42:20))),L.w*(layer==1?.018f:.009f));
        }
        // Shallow currents leave the prompt lane quiet, with a few moving glints.
        for(int channel=0;channel<2;channel++) {
            for(int j=0;j<=32;j++) {
                float y=L.playTop+span*j/32;
                edge[j*2]=L.w*(.43f+channel*.14f+.032f*(float)Math.sin(y/L.w*5-clock*.24f+channel*2));
                edge[j*2+1]=y;
            }
            p.polyline(edge,Glyph.withAlpha(GREEN,(int)(fade*13)),L.w*.034f);
            p.polyline(edge,Glyph.withAlpha(MINT,(int)(fade*12)),L.w*.006f);
        }
        for(int i=0;i<8;i++) {
            int seed=733+i*71,side=i%2;
            float r=L.w*(.14f+hash(seed)*.09f),travel=span+r*4;
            float y=L.playTop-r*2+(hash(seed+1)*travel+clock*L.h*(.016f+(i%3)*.004f))%travel;
            float x=L.w*(.04f+hash(seed+2)*.075f);if(side==1)x=L.w-x;
            float wobble=(float)Math.sin(c.clock*1.4f+i*2)*.035f;
            jelly(p,blob,x,y,r*(1+wobble),r*(1.10f-wobble),i,Glyph.withAlpha(DEEP,(int)(fade*135)));
            jelly(p,blob,x,y-r*.10f,r*.92f,r*(1.02f-wobble),i,Glyph.withAlpha(i%3==0?0xFF8DC57A:GREEN,(int)(fade*130)));
            jelly(p,blob,x-r*.15f,y-r*.35f,r*.64f,r*.61f,i,Glyph.withAlpha(MINT,(int)(fade*29)));
            p.arc(x-r*.12f,y-r*.2f,r*.61f,r*.70f,209,76,Glyph.withAlpha(MINT,(int)(fade*86)),L.w*.007f);
        }
        // Bubbles rise inside the jelly, swell, and pop into three small falling beads.
        for(int i=0;i<16;i++) {
            int seed=1391+i*61;float age=(c.clock*.16f+hash(seed))%1;
            float x=L.w*(.04f+hash(seed+3)*.18f);if(i%2==1)x=L.w-x;
            float y=L.playTop+hash(seed+7)*span-age*L.w*.18f;
            float r=L.w*(.010f+hash(seed+9)*.015f)*( .7f+age*.3f);
            float a=fade*Math.min(1,age*8);
            if(age<.84f) {
                p.fillEllipse(x,y,r,r*1.12f,Glyph.withAlpha(MINT,(int)(a*23)));
                p.arc(x,y,r,r*1.12f,190,245,Glyph.withAlpha(MINT,(int)(a*93)),L.w*.003f);
                p.fillCircle(x-r*.35f,y-r*.45f,r*.20f,Glyph.withAlpha(INK,(int)(a*100)));
            } else for(int bead=0;bead<3;bead++) {
                float t=(age-.84f)/.16f,angle=bead*2.1f;
                p.fillEllipse(x+(float)Math.cos(angle)*r*t*2,y+(float)Math.sin(angle)*r*t*2+t*t*r,
                        r*.23f,r*.32f,Glyph.withAlpha(MINT,(int)(a*(1-t)*100)));
            }
        }
    }

    /** One softly asymmetric contour, reused for layered mounds and airborne goo. */
    static void jelly(Painter p,float[] shape,float x,float y,float rx,float ry,float phase,int color) {
        for(int j=0;j<shape.length/2;j++) {
            float angle=j*Softbody.TAU/(shape.length/2),w=1+.055f*(float)Math.sin(angle*3+phase)
                    +.025f*(float)Math.sin(angle*5-phase);
            shape[j*2]=x+(float)Math.cos(angle)*rx*w;
            shape[j*2+1]=y+(float)Math.sin(angle)*ry*w;
        }
        p.fillPoly(shape,color);
    }
}
