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
    static float bankWidth(float time,int side,Layout L) {
        return L.w*SlimeBasinArt.BANK_FRACTION*(1+.025f*(float)Math.sin(time*.72f+side*2.4f));
    }
    static float bankHeight(float time,int side,Layout L) {
        return (L.deckTop-L.playTop)*(1.018f+.008f*(float)Math.sin(time*.59f+side*2.4f));
    }
    static float bankTop(float time,int side,Layout L) {
        float span=L.deckTop-L.playTop;
        return L.playTop+(span-bankHeight(time,side,L))*.5f+span*.002f*(float)Math.sin(time*.47f+side);
    }
    static void draw(Painter p,GameCore c,Layout L) {
        float fade=progress(c)*(retained(c)?1:1-c.drained());
        if(fade<=.001f)return;
        Boss b=boss(c);
        float span=L.deckTop-L.playTop,t=c.clock,hit=retained(c)?0:light(b);
        float breath=.5f+.5f*(float)Math.sin(t*.85f);
        float cx=b.bodyX(L),floor=b.bodyY(L)+b.bodyR(L)*.80f;
        float[] shape=new float[34];
        float[][] rings={new float[66],new float[66]};
        art(p,SlimeBasinArt.SCENE,0,L.playTop,L.w,span,(int)(fade*255));
        // Each bank breathes around its outer edge; the central pool never stretches.
        for(int side=0;side<2;side++) {
            float width=bankWidth(t,side,L);
            art(p,side==0?SlimeBasinArt.LEFT_BANK:SlimeBasinArt.RIGHT_BANK,
                    side==0?0:L.w-width,bankTop(t,side,L),width,bankHeight(t,side,L),(int)(fade*255));
        }
        // Move and modulate a single smooth light texture; bank geometry stays cached.
        float glowX=L.w*.5f+(cx-L.w*.5f)*.20f;
        art(p,SlimeBasinArt.LIGHT,glowX-L.w*.65f,floor-span*.46f,
                L.w*1.3f,span*1.12f,(int)(fade*(46+breath*18+hit*100)));
        // Small live glints and surface reflections retain motion without full-scene redraws.
        for(int side=0;side<2;side++)for(int i=0;i<3;i++) {
            float u=.20f+i*.29f+.006f*(float)Math.sin(t*.65f+i);
            float scale=bankWidth(t,side,L)/(L.w*SlimeBasinArt.BANK_FRACTION);
            float x=(SlimeBasinArt.inset(u,side,L.w)-L.w*.020f)*scale;if(side==1)x=L.w-x;
            SlimeHills.jelly(p,shape,x,bankTop(t,side,L)+bankHeight(t,side,L)*u,L.w*.014f*scale,L.w*.008f,
                    i,Glyph.withAlpha(SlimeHills.MINT,(int)(fade*(28+breath*18+hit*36))));
        }
        for(int i=0;i<6;i++) {
            float x=L.w*(.26f+hash(i+921)*.48f),y=L.playTop+span*(.35f+hash(i+613)*.54f);
            x+=L.w*.012f*(float)Math.sin(t*.5f+i);
            SlimeHills.jelly(p,shape,x,y,L.w*(.025f+hash(i+84)*.035f),L.w*.006f,i,
                    Glyph.withAlpha(0xFF9BD79B,(int)(fade*(13+breath*5+hit*18))));
        }
        float reveal=SlimeArrival.reveal(b);
        float grounding=fade*reveal*(b.beaten?1-b.leaveProgress():1);
        art(p,SlimeBasinArt.SHADOW,cx-b.bodyR(L)*1.6f,floor-b.bodyR(L)*.30f,
                b.bodyR(L)*3.2f,b.bodyR(L)*.60f,(int)(grounding*115));
        for(int wave=0;wave<3;wave++) {
            float phase=(t*.19f+wave/3f)%1;
            float radius=b.bodyR(L)*(1.03f+phase*.8f+hit*.3f);
            SlimeHills.ripple(p,rings,cx,floor,radius,radius*.23f,wave+t*.13f,
                    Glyph.withAlpha(SlimeHills.MINT,(int)(grounding*(1-phase)*(28+hit*40))),.035f);
        }
    }
    private static void art(Painter p,int art,float x,float y,float w,float h,int alpha) {
        if(alpha<=0)return;
        ArtStamp cache=p.stamps();
        if(cache!=null && cache.basin(art,x,y,w,h,alpha))return;
        p.save();p.translate(x,y);
        SlimeBasinArt.paint(new OpacityPainter(p,alpha/255f),art,w,h);p.restore();
    }
}
