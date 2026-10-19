package com.dddumpling.game;

/** Translucent rainbow bands, with shared arches for the title and land icon. */
final class RainbowLand extends Draw {
    static final float BATTLE_SKY_RATE=3f, BAND_SCROLL_RATE=.035f;
    static boolean active(GameCore c) {
        return !c.survival.active && !c.timeAttack.active && Lands.forStage(c.stage)==Lands.RAINBOW;
    }
    private static final int[] BAND_ORDER=bandOrder();
    private static int[] bandOrder() {
        int[] order=new int[8];
        for(int i=0;i<order.length;i++) {
            int at=i;
            while(at>0 && hash(order[at-1]*719+151)>hash(i*719+151)){order[at]=order[at-1];at--;}
            order[at]=i;
        }
        return order;
    }
    static void backdrop(Painter p,GameCore c,Layout L,float fade) {
        if(fade<=0)return;
        float margin=Renderer.shakeMargin(c,L),spacing=L.w/(BAND_ORDER.length-2);
        p.save();p.clipRect(-margin,-margin,L.w+margin,L.deckTop);
        for(int column:BAND_ORDER) {
            float radius=L.w*(.155f+.045f*hash(column*89+7));
            float straight=L.h*1.12f-radius*2,pitch=straight*.90f;
            float travel=c.skyClock*L.h*BAND_SCROLL_RATE*(.85f+.30f*hash(column*41+5))+hash(column*61+3)*pitch;
            int cycle=(int)Math.floor(travel/pitch);
            float offset=travel-cycle*pitch;
            for(int row=-1;row<=2;row++) {
                int generation=cycle-row,seed=column*719+generation*977+151;
                float x=(column-.5f+(hash(seed)-.5f)*.8f)*spacing;
                float top=offset+row*pitch-straight*.5f,bottom=top+straight;
                if(bottom+radius*1.2f<-margin || top-radius*1.2f>L.deckTop)continue;
                int color=CLEAR_COLORS[Math.floorMod(column*5+generation,CLEAR_COLORS.length)];
                // Layer faint concentric silhouettes to feather both sides and rounded ends.
                for(int feather=0;feather<8;feather++)
                    band(p,x,top,bottom,radius*(1.20f-feather*.065f),
                            Glyph.withAlpha(color,(int)((4+feather*1.2f)*fade)));
            }
        }
        p.restore();
    }
    private static void band(Painter p,float x,float top,float bottom,float r,int color) {
        // One continuous capsule avoids dark overlap seams between the body and end caps.
        int steps=16;float[] shape=new float[(steps+1)*4];
        for(int i=0;i<=steps;i++) {
            float a=(float)Math.PI+i*(float)Math.PI/steps,dx=(float)Math.cos(a)*r,dy=(float)Math.sin(a)*r;
            shape[i*2]=x+dx;shape[i*2+1]=top+dy;
            shape[(steps+1+i)*2]=x-dx;shape[(steps+1+i)*2+1]=bottom-dy;
        }
        p.fillPoly(shape,color);
    }
    static void prop(Painter p,float x,float y,float r,int alpha,float t,boolean silhouette) {
        float bob=(float)Math.sin(t*1.8f)*r*.10f;
        for(int i=0;i<RainbowCatArt.COLORS.length;i++)RainbowCatArt.arch(p,x,y+bob,r*(1.1f-i*.10f),r*(1.0f-i*.10f),
                Glyph.withAlpha(silhouette?0xFF443B64:RainbowCatArt.COLORS[i],alpha),r*.13f);
        for(int side=-1;side<=1;side+=2)
            cloud(p,x+side*r*.84f,y+bob,r*.48f,r*.32f,alpha,silhouette,0,4);
    }
    private static final int[] CLOUD_COLORS={0xFFBAA2E8,0xFFCCE2F5,0xFFE9E0FF,0xFFFFF7FF};
    /** Each depth layer is one traced puff, so overlapping lobes do not become opaque. */
    static void cloud(Painter p,float x,float y,float rx,float ry,int alpha,boolean silhouette,int from,int to) {
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

    static void skit(Painter p,int variant,float x,float y,float r,float t,int a) {
        LandPlaySkits.rainbow(p,variant,x,y,r,t,a);
    }
    static void title(Painter p,float x,float y,float s,float span,float rise,float clock,float fade,boolean front) {
        if(!front)return;
        float root=y-s*.10f+(1-rise)*s*4f;
        int alpha=(int)(fade*rise*148),bands=RainbowCatArt.COLORS.length;
        float half=span*.96f,bandWidth=s*.36f;
        p.save();p.clipRect(x-span*1.65f,y-s*3.5f,x+span*1.65f,y+s*.30f);
        // Extend beyond the ground so every band ends on the same horizontal plane.
        for(int band=0;band<bands;band++) {
            int steps=40;float[] ribbon=new float[(steps+1)*4];
            for(int i=0;i<=steps;i++) {
                float u=(i/(float)steps*2-1)*1.6f,cy=root-s*1.8f*(1-u*u),slope=s*3.6f*u/half;
                float ny=1f/(float)Math.sqrt(1+slope*slope),nx=-slope*ny;
                float outer=(band-bands*.5f)*bandWidth,inner=outer+bandWidth;
                ribbon[i*2]=x+u*half+nx*outer;ribbon[i*2+1]=cy+ny*outer;
                ribbon[(steps*2+1-i)*2]=x+u*half+nx*inner;ribbon[(steps*2+1-i)*2+1]=cy+ny*inner;
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
