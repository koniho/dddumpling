package com.dddumpling.game;

/** Super Slime's still pool. Soft lighting uses bounded translucent paths on both hosts. */
final class SlimeBasin extends Draw {
    private SlimeBasin() {}
    static boolean retained(GameCore c) {
        return BossVictory.retained(c) && c.bossVictoryKind==Boss.SLIME;
    }
    private static Boss boss(GameCore c) {return retained(c)?c.bossVictory:c.boss;}
    static float progress(GameCore c) {
        if(BossCollect.keepsArena(c,Boss.SLIME))return 1;
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
        return L.w*SlimeBasinArt.BANK_FRACTION*(1+.025f*(float)Math.sin(time*.65f+side*2.4f));
    }
    static float bankHeight(float time,int side,Layout L) {
        return (L.deckTop-L.playTop)*(1.09f+.008f*(float)Math.sin(time*.55f+side*2.4f));
    }
    static float bankTop(float time,int side,Layout L) {
        float span=L.deckTop-L.playTop;
        return L.playTop+(span-bankHeight(time,side,L))*.5f+span*.0015f*(float)Math.sin(time*.5f+side);
    }
    static float layerWidth(float time,int side,int layer,Layout L) {
        return bankWidth(time,side,L)*(1+.014f*(float)Math.sin(time*(.31f+layer*.045f)+layer*1.9f+side*2.4f));
    }
    static float layerTop(float time,int side,int layer,Layout L) {
        return bankTop(time,side,L)+(L.deckTop-L.playTop)*.007f
                *(float)Math.sin(time*(.26f+layer*.037f)+layer*2.1f+side*1.7f);
    }
    private static void bankArt(Painter p,GameCore c,Layout L,int side,int layer,boolean lit,int alpha) {
        float t=c.clock,w=layerWidth(t,side,layer,L),h=bankHeight(t,side,L),y=layerTop(t,side,layer,L);
        float x=side==0?0:L.w-w;int art=SlimeBasinArt.bank(side,layer,lit);
        // Only the front lip needs a drag mesh; deeper layers keep their inexpensive idle motion.
        if(layer>0 || !c.slimeBanks.recoiling(t,side)) {art(p,art,x,y,w,h,alpha);return;}
        float[] offsets=c.slimeBanks.offsets(t,side,L);ArtStamp cache=p.stamps();
        if(cache==null || !cache.basinMesh(art,x,y,w,h,offsets,alpha))
            SlimeBasinArt.paint(new SlimeBanks.Warp(p,alpha,side,x,y,w,h,offsets),art,w,h);
    }
    static float surfaceLight(GameCore c,Layout L,float x,float y,float hit) {
        float strength=hit*.18f;
        for(int i=0;i<2;i++) {
            float dx=(x-c.slimeBanks.lightX[i])/L.w,dy=(y-c.slimeBanks.lightY[i])/(L.deckTop-L.playTop);
            strength=Math.max(strength,c.slimeBanks.light(c.clock,i)/(1+dx*dx*3+dy*dy*2));
        }
        return Math.min(1,strength);
    }
    static float shimmerPhase(Boss b,int ribbon) {
        // The curtain has cleared before the first highlights draw themselves onto the banks.
        if(b.intro>0)return -1;
        float age=b.age-.25f-ribbon*.18f;
        return age<0?-1:(age%5.6f)/4;
    }
    static int shimmer(Painter p,GameCore c,Layout L,Boss b,float fade) {
        if(b.beaten || retained(c) || BossCollect.keepsArena(c,Boss.SLIME))return 0;
        float[] line=c.slimeBanks.shimmer;
        int strokes=0;
        for(int ribbon=0;ribbon<3;ribbon++) {
            float phase=shimmerPhase(b,ribbon);
            if(phase<=0 || phase>=1)continue;
            float head=Math.min(1,phase*1.3f),tail=Math.max(0,phase*1.3f-.30f);
            float alpha=fade*Math.min(1,Math.min(phase/.13f,(1-phase)/.22f));
            int side=ribbon-1;
            float width=side<0?L.w:layerWidth(c.clock,side,0,L);
            float[] offsets=side<0 || !c.slimeBanks.recoiling(c.clock,side)?null:c.slimeBanks.offsets(c.clock,side,L);
            for(int j=0;j<line.length/2;j++) {
                float u=tail+(head-tail)*j/(line.length/2-1f),x,y;
                if(side<0) {
                    x=L.w*u;
                    y=L.playTop+(L.deckTop-L.playTop)*(.306f-.025f*(float)Math.sin(u*Math.PI)
                            +.005f*(float)Math.sin(u*18+.6f));
                } else {
                    u=.42f+u*.49f;
                    x=(SlimeBasinArt.inset(u,side,L.w)-L.w*.018f)*width/(L.w*SlimeBasinArt.BANK_FRACTION);
                    if(offsets!=null)x+=x/width*SlimeBanks.sample(offsets,u);
                    if(side==1)x=L.w-x;
                    y=layerTop(c.clock,side,0,L)+bankHeight(c.clock,side,L)*u;
                }
                line[j*2]=x;line[j*2+1]=y;
            }
            // Three short strokes provide soft edges without blur passes or new raster textures.
            p.polyline(line,Glyph.withAlpha(SlimeHills.MINT,(int)(alpha*18)),L.w*.020f);
            p.polyline(line,Glyph.withAlpha(SlimeHills.MINT,(int)(alpha*42)),L.w*.008f);
            p.polyline(line,Glyph.withAlpha(0xFFDFFFF0,(int)(alpha*108)),L.w*.0025f);
            strokes+=3;
        }
        return strokes;
    }
    static void draw(Painter p,GameCore c,Layout L) {
        float fade=progress(c)*(retained(c)?1:1-c.drained());
        if(fade<=.001f)return;
        Boss b=boss(c);
        float span=L.deckTop-L.playTop,t=c.clock,hit=retained(c)?0:light(b);
        if(!retained(c))hit=Math.max(hit,Math.max(c.slimeBanks.light(t,0)*.85f,c.slimeBanks.light(t,1)));
        float breath=.5f+.5f*(float)Math.sin(t*.85f);
        float cx=b.bodyX(L),floor=b.bodyY(L)+b.bodyR(L)*.80f;
        float[] shape=new float[34];
        float[][] rings={new float[66],new float[66]};
        art(p,SlimeBasinArt.SCENE,0,L.playTop,L.w,span,(int)(fade*255));
        if(hit>.01f)art(p,SlimeBasinArt.REAR_LIT,0,L.playTop,L.w,span,
                (int)(fade*surfaceLight(c,L,L.w*.5f,L.playTop+span*.27f,hit)*170));
        for(int side=0;side<2;side++)for(int layer=0;layer<SlimeBasinArt.LAYERS;layer++) {
            bankArt(p,c,L,side,layer,false,(int)(fade*255));
            if(hit>.01f) {
                int alpha=(int)(fade*surfaceLight(c,L,side==0?L.w*.15f:L.w*.85f,L.playTop+span*.48f,hit)*230);
                if(alpha>0)bankArt(p,c,L,side,layer,true,alpha);
            }
        }
        shimmer(p,c,L,b,fade);
        // Move and modulate a single smooth light texture; bank geometry stays cached.
        float glowX=L.w*.5f+(cx-L.w*.5f)*.20f;
        float glowWidth=L.w*(1.3f+hit*.65f),glowHeight=span*(1.12f+hit*.24f);
        art(p,SlimeBasinArt.LIGHT,glowX-glowWidth*.5f,floor-glowHeight*.41f,
                glowWidth,glowHeight,(int)(fade*Math.min(255,46+breath*18+hit*185)));
        // Reuse the same cached mask for a brief expanding light at the actual contact point.
        if(!retained(c))for(int i=0;i<2;i++) {
            float pulse=c.slimeBanks.light(t,i);
            if(pulse<.01f)continue;
            float reach=L.w*((i==0?1.15f:2.0f)+(1-pulse)*.65f);
            float height=reach*.72f;
            art(p,SlimeBasinArt.LIGHT,c.slimeBanks.lightX[i]-reach*.5f,c.slimeBanks.lightY[i]-height*.5f,
                    reach,height,(int)(fade*pulse*(i==0?205:255)));
        }
        // Small live glints and surface reflections retain motion without full-scene redraws.
        for(int side=0;side<2;side++)for(int i=0;i<3;i++) {
            float u=.46f+i*.19f+.006f*(float)Math.sin(t*.65f+i);
            float scale=layerWidth(t,side,0,L)/(L.w*SlimeBasinArt.BANK_FRACTION);
            float x=(SlimeBasinArt.inset(u,side,L.w)-L.w*.020f)*scale;
            if(c.slimeBanks.recoiling(t,side))x+=x/layerWidth(t,side,0,L)*SlimeBanks.sample(c.slimeBanks.offsets(t,side,L),u);
            if(side==1)x=L.w-x;
            SlimeHills.jelly(p,shape,x,layerTop(t,side,0,L)+bankHeight(t,side,L)*u,L.w*.014f*scale*(1+hit),L.w*.008f*(1+hit*.6f),
                    i,Glyph.withAlpha(SlimeHills.MINT,(int)(fade*(28+breath*18+hit*110))));
        }
        for(int i=0;i<6;i++) {
            float x=L.w*(.26f+hash(i+921)*.48f),y=L.playTop+span*(.35f+hash(i+613)*.54f);
            x+=L.w*.012f*(float)Math.sin(t*.5f+i);
            SlimeHills.jelly(p,shape,x,y,L.w*(.025f+hash(i+84)*.035f)*(1+hit*.85f),L.w*.006f*(1+hit*.5f),i,
                    Glyph.withAlpha(0xFF9BD79B,(int)(fade*(13+breath*5+hit*65))));
        }
        float reveal=SlimeArrival.reveal(b);
        float grounding=BossCollect.keepsArena(c,Boss.SLIME)?0:fade*reveal*(b.beaten?1-b.leaveProgress():1);
        art(p,SlimeBasinArt.SHADOW,cx-b.bodyR(L)*1.6f,floor-b.bodyR(L)*.30f,
                b.bodyR(L)*3.2f,b.bodyR(L)*.60f,(int)(grounding*115));
        for(int dip=0;dip<c.slimeBanks.dipAt.length;dip++)for(int wave=0;wave<2;wave++) {
            float age=t-c.slimeBanks.dipAt[dip]-wave*.18f;
            if(age<0 || age>2.4f)continue;
            float phase=age/2.4f,radius=b.bodyR(L)*(1.15f+phase*1.8f);
            float strength=Math.min(1,age/.12f)*(1-phase)*(1-phase);
            SlimeHills.ripple(p,rings,c.slimeBanks.dipX[dip],c.slimeBanks.dipY[dip],radius,radius*.25f,dip,
                    Glyph.withAlpha(SlimeHills.MINT,(int)(grounding*strength*105)),.026f);
        }
        SlimeHills.clearRipples(p,c,L,fade);
    }
    private static void art(Painter p,int art,float x,float y,float w,float h,int alpha) {
        if(alpha<=0)return;
        ArtStamp cache=p.stamps();
        if(cache!=null && cache.basin(art,x,y,w,h,alpha))return;
        p.save();p.translate(x,y);
        SlimeBasinArt.paint(new OpacityPainter(p,alpha/255f),art,w,h);p.restore();
    }
}
