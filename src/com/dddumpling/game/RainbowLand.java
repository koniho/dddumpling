package com.dddumpling.game;

/** Floating sherbet islands and layered rainbow arches. */
final class RainbowLand extends Draw {
    static boolean active(GameCore c) {
        return !c.survival.active && !c.timeAttack.active && Lands.forStage(c.stage)==Lands.RAINBOW;
    }
    static void enemyTrail(Painter p,GameCore c,GameCore.Enemy e,Layout L,int fade) {
        if(!active(c) || fade<=0)return;
        float t=e.destroyed?Math.min(1,e.destroyT/GameCore.DESTROY_TIME):0;
        if(t>=1)return;
        float spread=1-(1-t)*(1-t),clock=e.destroyed?e.clearClock:c.clock;
        float x=e.destroyed?e.clearX:c.enemyCentreX(e),y=(e.destroyed?e.clearY:e.y)-rim(L);
        fade=(int)(fade*(1-t)*(1-t));
        float width=Math.min(L.w*.72f,L.wordWidth(e.word.length)*.85f)*(1+2.1f*spread);
        float length=L.enemyR*(e.diving?6.5f:e.fastSingle?5f:3.6f);
        length=Math.min(length,Math.max(0,(e.destroyed?e.clearY:e.y)+L.enemyR));
        float[] shape=new float[8];
        int bands=RainbowCatArt.COLORS.length;
        for(int band=0;band<bands;band++)for(int part=0;part<3;part++) {
            float a=part/3f,b=(part+1)/3f;
            float wa=width*(1-a*.38f),wb=width*(1-b*.38f);
            float ax=x+(float)Math.sin(clock*2+e.phase-a*2)*L.enemyR*a*.32f;
            float bx=x+(float)Math.sin(clock*2+e.phase-b*2)*L.enemyR*b*.32f;
            shape[0]=ax+wa*(band/(float)bands-.5f);shape[1]=y-a*length;
            shape[2]=ax+wa*((band+1f)/bands-.5f);shape[3]=shape[1];
            shape[4]=bx+wb*((band+1f)/bands-.5f);shape[5]=y-b*length;
            shape[6]=bx+wb*(band/(float)bands-.5f);shape[7]=shape[5];
            p.fillPoly(shape,Glyph.withAlpha(RainbowCatArt.COLORS[band],(int)(fade*.29f*(1-b*.78f))));
        }
    }
    private static float rim(Layout L) {return L.enemyR*.25f;}
    static void prop(Painter p,float x,float y,float r,int alpha,float t,boolean silhouette) {
        float bob=(float)Math.sin(t*1.8f)*r*.10f;
        for(int i=0;i<RainbowCatArt.COLORS.length;i++)RainbowCatArt.arch(p,x,y+bob,r*(1.1f-i*.10f),r*(1.0f-i*.10f),
                Glyph.withAlpha(silhouette?0xFF443B64:RainbowCatArt.COLORS[i],alpha),r*.13f);
        for(int side=-1;side<=1;side+=2)
            cloud(p,x+side*r*.84f,y+bob,r*.48f,r*.32f,alpha,silhouette,0,4);
    }
    private static final int[] CLOUD_COLORS={0xFFBAA2E8,0xFFCCE2F5,0xFFE9E0FF,0xFFFFF7FF};
    /** Each depth layer is one traced puff, so overlapping lobes do not become opaque. */
    private static void cloud(Painter p,float x,float y,float rx,float ry,int alpha,boolean silhouette,int from,int to) {
        for(int layer=from;layer<to;layer++) {
            float scale=1.12f-layer*.08f,xx=x+rx*(layer-1.5f)*.045f,yy=y-ry*layer*.055f;
            float[] outline=new float[132];
            for(int i=0;i<=32;i++) {
                float u=i/16f-1,cap=(float)Math.sqrt(Math.max(0,1-u*u));
                float top=cap*.40f;
                for(int lobe=0;lobe<3;lobe++) {
                    float centre=(lobe-1)*.48f,local=(u-centre)/(.48f+(lobe==1?.05f:0));
                    if(Math.abs(local)<1)top=Math.max(top,(float)Math.sqrt(1-local*local)*(lobe==1?1:.78f));
                }
                outline[i*2]=xx+rx*u*scale;outline[i*2+1]=yy-ry*top*scale;
                outline[(65-i)*2]=xx+rx*u*scale;outline[(65-i)*2+1]=yy+ry*cap*.43f*scale;
            }
            int color=silhouette?0xFF443B64:CLOUD_COLORS[layer];
            p.fillPoly(outline,Glyph.withAlpha(color,(int)(alpha*(.09f+layer*.035f))));
        }
    }

    static void skit(Painter p,int variant,float x,float y,float r,float t,float a) {
        prop(p,x,y+r*.3f,r,(int)(a*220),t,false);
        float loop=t*2,hop=Math.abs((float)Math.sin(loop));
        RainbowCatArt.portrait(p,x+(float)Math.sin(loop*.6f)*r*.6f,y-r*(.4f+hop*.5f),r*.54f,t,true,a);
        RainbowCatArt.yarn(new OpacityPainter(p,a),x+r*(float)Math.cos(loop),y+r*.12f,r*.22f,t,false);
    }
    static void title(Painter p,float x,float y,float s,float span,float rise,float clock,float fade,boolean front) {
        if(!front)return;
        float root=y-s*.10f+(1-rise)*s*4f;
        int alpha=(int)(fade*rise*148),bands=RainbowCatArt.COLORS.length;
        float half=span*.96f,bandWidth=s*.36f;
        p.save();p.clipRect(x-span-s*.65f,y-s*3.1f,x+span+s*.65f,y+s*1.35f);
        // A broad parabolic ribbon keeps a gentle slope at both ends and even band thickness.
        for(int band=0;band<bands;band++) {
            float[] ribbon=new float[100];
            for(int i=0;i<=24;i++) {
                float u=i/12f-1,cy=root-s*1.8f*(1-u*u),slope=s*3.6f*u/half;
                float ny=1f/(float)Math.sqrt(1+slope*slope),nx=-slope*ny;
                float outer=(band-bands*.5f)*bandWidth,inner=outer+bandWidth;
                ribbon[i*2]=x+u*half+nx*outer;ribbon[i*2+1]=cy+ny*outer;
                ribbon[(49-i)*2]=x+u*half+nx*inner;ribbon[(49-i)*2+1]=cy+ny*inner;
            }
            p.fillPoly(ribbon,Glyph.withAlpha(RainbowCatArt.COLORS[band],alpha));
        }
        p.restore();
    }
    static final int[] CLEAR_COLORS={0xFFFF3C80,0xFFFF8C28,0xFFFFE33C,0xFF38E884,0xFF39CFFF,0xFFAE64FF};
    static void clearColors(GameCore c,int from) {
        if(!active(c))return;
        for(int i=from;i<c.particles.size();i++)c.particles.get(i).color=CLEAR_COLORS[i%CLEAR_COLORS.length];
    }
}
