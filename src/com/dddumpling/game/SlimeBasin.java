package com.dddumpling.game;

/** Super Slime's still pool. Soft lighting uses bounded translucent paths on both hosts. */
final class SlimeBasin extends Draw {
    private SlimeBasin() {}
    static boolean retained(GameCore c) {
        return c.state==GameCore.OVER && c.bossVictoryKind==Boss.SLIME && c.bossVictory!=null;
    }
    private static Boss boss(GameCore c) {return retained(c)?c.bossVictory:c.boss;}
    static float progress(GameCore c) {
        Boss b=boss(c);
        if(c.state==GameCore.TITLE || c.survival.active || !b.active() || b.kind!=Boss.SLIME)return 0;
        // Complete the scenery change behind the opaque curtain, before it starts draining.
        float t=Math.max(0,Math.min(1,SlimeArrival.elapsed(b)/SlimeArrival.RELEASE));
        return t*t*(3-2*t);
    }
    static float light(Boss b) {
        return Math.min(1,Math.max(b.hurt,Math.max(b.slimeKeyPulse*2,b.slimeDragPulse*3)));
    }
    static void draw(Painter p,GameCore c,Layout L) {
        float fade=progress(c)*(retained(c)?1:1-c.drained());
        if(fade<=.001f)return;
        Boss b=boss(c);
        float span=L.deckTop-L.playTop,t=c.clock,hit=retained(c)?0:light(b);
        float breath=.5f+.5f*(float)Math.sin(t*.85f);
        float cx=b.bodyX(L),floor=b.bodyY(L)+b.bodyR(L)*.80f;
        float[] shape=new float[66],bank=new float[102],edge=new float[98],shine=new float[196];
        float[][] rings={new float[66],new float[66]};
        p.fillRect(0,L.playTop,L.w,L.deckTop,Glyph.withAlpha(0xFF173C36,(int)(fade*255)));
        // Overlapping low-opacity contours soften the light without a full-screen blur pass.
        for(int i=15;i>=0;i--) {
            float k=i/15f;
            SlimeHills.jelly(p,shape,L.w*.5f+(cx-L.w*.5f)*.25f,floor+span*.04f,
                    L.w*(.20f+k*.65f),span*(.13f+k*.55f),.6f,
                    Glyph.withAlpha(0xFF78CA7B,(int)(fade*(7+breath*2+hit*6))));
        }
        for(int side=0;side<2;side++)for(int layer=0;layer<4;layer++) {
            for(int j=0;j<=48;j++) {
                float u=j/48f,y=L.playTop+span*u;
                float wave=(float)Math.sin(u*10+side*2.1f+layer*.32f)*.045f
                        +(float)Math.sin(u*19+side*.8f-layer*.40f)*.023f;
                float motion=(float)Math.sin(t*.65f+u*9+side)*.006f;
                float inset=L.w*(.22f-layer*.055f+wave+motion
                        +.070f*(float)Math.cos(u*Softbody.TAU));
                float x=side==0?inset:L.w-inset;
                bank[j*2]=edge[j*2]=x;bank[j*2+1]=edge[j*2+1]=y;
            }
            bank[98]=bank[100]=side==0?-L.w*.1f:L.w*1.1f;
            bank[99]=L.deckTop;bank[101]=L.playTop;
            int color=layer==0?0xFF64BA8A:layer==1?0xFF8ADAA0:layer==2?0xFF479B77:0xFF285C50;
            color=Glyph.mix(color,SlimeHills.MINT,hit*.18f);
            p.fillPoly(bank,Glyph.withAlpha(color,(int)(fade*(layer==0?100:145))));
            // Filled ribbons avoid repeated translucent line-cap overdraw on native hosts.
            for(int halo=0;halo<2;halo++) {
                float width=L.w*(halo==0?.022f:.007f)*(side==0?-1:1);
                for(int j=0;j<=48;j++) {
                    shine[j*2]=edge[j*2];shine[j*2+1]=edge[j*2+1];
                    shine[(97-j)*2]=edge[j*2]+width;shine[(97-j)*2+1]=edge[j*2+1];
                }
                p.fillPoly(shine,Glyph.withAlpha(SlimeHills.MINT,
                        (int)(fade*(layer==1?60:24)*(halo==0?.35f:1)*(1+hit*.7f))));
            }
            // Traveling tapered reflections stay attached to the bank contour.
            if(layer==1)for(int glint=0;glint<4;glint++) {
                float u=.09f+glint*.24f+.014f*(float)Math.sin(t*.6f+glint+side);
                int j=Math.min(45,(int)(u*48));
                float x=edge[j*2],y=edge[j*2+1];
                SlimeHills.jelly(p,shape,x+(side==0?-1:1)*L.w*.023f,y,
                        L.w*.014f,span*(.020f+.009f*(float)Math.sin(t+glint)),glint,
                        Glyph.withAlpha(0xFFC4F6C6,(int)(fade*(45+breath*22+hit*45))));
            }
        }
        // The rear jelly shelf grounds the basin, with a few hanging curtain remnants.
        for(int side=0;side<2;side++)for(int i=0;i<3;i++) {
            float x=L.w*(.035f+i*.045f);if(side==1)x=L.w-x;
            float length=span*(.13f-i*.033f)*(1+.025f*(float)Math.sin(t*.9f+i));
            SlimeHills.jelly(p,shape,x,L.playTop+length*.27f,L.w*(.05f-i*.008f),length,
                    i+side,Glyph.withAlpha(0xFF65BB85,(int)(fade*100)));
            p.arc(x-L.w*.008f,L.playTop+length*.63f,L.w*.016f,length*.25f,15,115,
                    Glyph.withAlpha(SlimeHills.MINT,(int)(fade*80)),L.w*.005f);
        }
        // Quiet broken reflections drift in place; the middle remains open for prompts.
        for(int i=0;i<9;i++) {
            float x=L.w*(.24f+hash(i+921)*.52f),y=L.playTop+span*(.28f+hash(i+613)*.64f);
            x+=L.w*.014f*(float)Math.sin(t*.5f+i);
            SlimeHills.jelly(p,shape,x,y,L.w*(.025f+hash(i+84)*.035f),L.w*.007f,i,
                    Glyph.withAlpha(0xFF9BD79B,(int)(fade*(10+breath*5+hit*12))));
        }
        float reveal=SlimeArrival.reveal(b);
        float grounding=fade*reveal*(b.beaten?1-b.leaveProgress():1);
        for(int i=4;i>=0;i--)p.fillEllipse(cx,floor,b.bodyR(L)*(1.1f+i*.13f),
                b.bodyR(L)*(.13f+i*.035f),Glyph.withAlpha(0xFF10352C,(int)(grounding*14)));
        for(int wave=0;wave<3;wave++) {
            float phase=(t*.19f+wave/3f)%1;
            float radius=b.bodyR(L)*(1.03f+phase*.8f+hit*.3f);
            SlimeHills.ripple(p,rings,cx,floor,radius,radius*.23f,wave+t*.13f,
                    Glyph.withAlpha(SlimeHills.MINT,(int)(grounding*(1-phase)*(28+hit*40))),.035f);
        }
    }
}
