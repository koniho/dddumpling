package com.dddumpling.game;

/** A flowing jelly valley. Bounded shared vector layers work on both native painters. */
final class SlimeHills extends Draw {
    static final int DEEP=0xFF245953, GREEN=0xFF64BB80, MINT=0xFFB7F0B4;
    static final float RIPPLE_TIME=1.8f, SCROLL_RATE=.020f;
    static final int MAX_RIPPLES=8;
    private SlimeHills() {}

    static void cleared(GameCore c,float x,float y,int color,Layout L) {
        if(c.state!=GameCore.PLAY || c.survival.active || Lands.forStage(c.stage)!=0)return;
        int count=0;
        for(GameCore.Particle q:c.particles)if(q.slimeRipple && ++count>=MAX_RIPPLES)return;
        GameCore.Particle q=new GameCore.Particle();
        q.slimeRipple=true;q.x=x;q.y=y;q.color=color;q.size=L.enemyR;
        q.slimeRippleSky=c.skyClock;
        q.life=q.max=RIPPLE_TIME;c.particles.add(q);
    }

    static float rippleY(GameCore c,GameCore.Particle q,Layout L) {
        return q.y+(c.skyClock-q.slimeRippleSky)*L.h*SCROLL_RATE;
    }

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
        // Background-tinted currents stay subordinate to the prompts.
        int stream=Glyph.mix(Lands.BG[0],GREEN,.35f),glint=Glyph.mix(Lands.BG[0],MINT,.23f);
        for(int channel=0;channel<5;channel++) {
            for(int j=0;j<=32;j++) {
                float y=L.playTop+span*j/32;
                edge[j*2]=L.w*(.29f+channel*.105f+(.026f+channel*.003f)*(float)Math.sin(y/L.w*(4.3f+channel*.27f)-clock*(.20f+channel*.018f)+channel*2));
                edge[j*2+1]=y;
            }
            p.polyline(edge,Glyph.withAlpha(stream,(int)(fade*18)),L.w*(.030f+(channel%3)*.012f));
            p.polyline(edge,Glyph.withAlpha(glint,(int)(fade*10)),L.w*.006f);
        }
        clearRipples(p,c,L,fade);
        for(int i=0;i<8;i++) {
            int seed=733+i*71,side=i%2;
            float r=L.w*(.14f+hash(seed)*.09f),travel=span+r*4;
            float y=L.playTop-r*2+(hash(seed+1)*travel+clock*L.h*(SCROLL_RATE+(i%3-1)*.004f))%travel;
            float x=L.w*(.04f+hash(seed+2)*.075f);if(side==1)x=L.w-x;
            float wobble=(float)Math.sin(c.clock*1.4f+i*2)*.035f;
            jelly(p,blob,x,y,r*(1+wobble),r*(1.10f-wobble),i,Glyph.withAlpha(DEEP,(int)(fade*135)));
            jelly(p,blob,x,y-r*.10f,r*.92f,r*(1.02f-wobble),i,Glyph.withAlpha(i%3==0?0xFF8DC57A:GREEN,(int)(fade*130)));
            jelly(p,blob,x-r*.15f,y-r*.35f,r*.64f,r*.61f,i,Glyph.withAlpha(MINT,(int)(fade*29)));
            p.arc(x-r*.12f,y-r*.2f,r*.61f,r*.70f,209,76,Glyph.withAlpha(MINT,(int)(fade*86)),L.w*.007f);
        }
        c.slimeBubbles.draw(p,c,L,fade);
    }

    /** Clear reactions stay beneath enemies; the idle surface has no pressure swells. */
    private static void clearRipples(Painter p,GameCore c,Layout L,float fade) {
        float[][] ring=null;
        for(GameCore.Particle q:c.particles) {
            if(!q.slimeRipple)continue;
            if(ring==null)ring=new float[][]{new float[66],new float[66]};
            float age=1-q.life/q.max,y=rippleY(c,q,L);
            for(int wave=0;wave<2;wave++) {
                float t=(age-wave*.15f)/(1-wave*.15f);
                if(t<=0 || t>=1)continue;
                float strength=(float)Math.sin(Math.PI*t)*(1-t)*fade;
                float r=q.size*(.6f+6.2f*t);
                int color=Glyph.mix(GREEN,q.color,.65f);
                ripple(p,ring,q.x,y,r,r*.64f,q.x*.01f+t*.35f,
                        Glyph.withAlpha(color,(int)(strength*54)),.055f);
                ripple(p,ring,q.x,y,r*1.055f,r*.675f,q.x*.01f+t*.35f,
                        Glyph.withAlpha(color,(int)(strength*16)),.085f);
            }
        }
    }

    private static void ripple(Painter p,float[][] ring,float x,float y,float rx,float ry,
            float phase,int color,float thickness) {
        for(int j=0;j<33;j++) {
            float angle=j*Softbody.TAU/33,w=1+.06f*(float)Math.sin(angle*3+phase)
                    +.025f*(float)Math.sin(angle*5-phase);
            float dx=(float)Math.cos(angle)*rx*w,dy=(float)Math.sin(angle)*ry*w;
            ring[0][j*2]=x+dx;ring[0][j*2+1]=y+dy;
            ring[1][j*2]=x+dx*(1-thickness);ring[1][j*2+1]=y+dy*(1-thickness);
        }
        p.fillContours(ring,color);
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
