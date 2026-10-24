package com.dddumpling.game;

/** Muted undergrowth and an overhead canopy; all motion follows the scenery clock. */
final class MushroomForest extends Draw {
    static final int LAND=3;
    private static final int[] GREENS={0xFF71917A,0xFF91A486,0xFF587D70,0xFFACB394};
    private MushroomForest() {}

    static float groveProgress(GameCore c) {
        if(!c.boss.active() || c.boss.kind!=Boss.MUSHROOM || c.survival.active)return 0;
        float t=c.boss.introProgress();
        return t*t*(3-2*t);
    }

    // Looking upward sends the nearby foliage down, then settles into the clearing.
    private static float scroll(GameCore c,Layout L) {return groveProgress(c)*L.h*.72f;}

    static void grove(Painter p,GameCore c,Layout L) {
        float progress=groveProgress(c),fade=progress*(1-c.drained());
        if(fade<=.001f || c.state==GameCore.TITLE)return;
        float floor=c.boss.baseY(L)+Boss.bodyR(L)*3.55f;
        float lift=-(1-progress)*L.h*.72f;
        float[] shape=new float[34];
        p.save();p.clipRect(0,L.playTop,L.w,L.deckTop);p.translate(0,lift);
        // A softly lit clearing joins the boss's own living roots, without a hard horizon.
        for(int i=3;i>=0;i--)p.fillEllipse(L.w*.5f,floor,L.w*(.38f+i*.055f),L.h*(.035f+i*.018f),
                Glyph.withAlpha(0xFF9BAE86,(int)(fade*9)));
        for(int i=0;i<5;i++) {
            float x=L.w*(.13f+i*.18f),sway=(float)Math.sin(c.skyClock*.17f+i)*L.w*.025f;
            p.fillPoly(new float[]{x,L.playTop-L.h*.3f,x+L.w*.04f,L.playTop-L.h*.3f,
                    x+sway+L.w*.17f,floor,x+sway-L.w*.06f,floor},Glyph.withAlpha(0xFFCCD6A7,(int)(fade*8)));
        }
        for(int layer=0;layer<2;layer++)for(int side=-1;side<=1;side+=2) {
            float x=L.w*(side<0?.08f:.92f),w=L.w*(layer==0?.055f:.09f);
            if(layer==0)x-=side*L.w*.13f;
            float base=floor+L.h*(layer==0?-.12f:.035f);
            float top=L.playTop-L.h*.30f,lean=-side*L.w*.11f;
            int a=(int)(fade*(layer==0?60:135));
            p.fillPoly(new float[]{x-w*1.8f,base,x-w*.68f,base-L.h*.14f,
                    x-w*.48f+lean,top,x+w*.47f+lean,top,x+w*.76f,base-L.h*.17f,x+w*1.9f,base},
                    Glyph.withAlpha(layer==0?0xFF526F65:0xFF364F46,a));
            p.fillPoly(new float[]{x-w*.60f,base-L.h*.07f,x-w*.18f+lean,top,
                    x+w*.13f+lean,top,x+w*.20f,base-L.h*.13f},Glyph.withAlpha(0xFF91A17C,a/3));
            for(int root=0;root<3;root++) {
                float reach=-side*L.w*(.11f+root*.065f);
                p.polyline(new float[]{x,base-L.h*.16f,x+reach*.24f,base-L.h*.025f,
                        x+reach,base+L.h*(.015f+root*.012f)},Glyph.withAlpha(0xFF758E69,a/2),w*(.20f-root*.045f));
            }
            for(int branch=0;branch<3;branch++) {
                float by=L.playTop+L.h*(.04f+branch*.12f),reach=-side*L.w*(.26f-branch*.055f);
                p.polyline(new float[]{x,by+L.h*.12f,x+reach*.38f,by+L.h*.035f,x+reach,by-L.h*.035f},
                        Glyph.withAlpha(0xFF617C60,a*2/3),w*.23f);
                sprig(p,shape,x+reach*.55f,by+L.h*.02f,L.w*(.17f-branch*.025f),-side*.8f,1,a/2,GREENS[(branch+layer)%4]);
            }
            sprig(p,shape,x,base,L.w*.18f,-side*.65f,0,a/2,GREENS[1]);
        }
        // Warm spores drift around the clearing; the combat centre stays open.
        for(int i=0;i<16;i++) {
            float x=L.w*(.06f+hash(403+i*19)*.88f);
            float y=L.playTop+(floor-L.playTop)*hash(603+i*43)-(float)Math.sin(c.skyClock*.4f+i)*L.h*.025f;
            float glow=.5f+.5f*(float)Math.sin(c.skyClock*1.2f+i*2.1f);
            p.fillCircle(x,y,L.unit*.11f,Glyph.withAlpha(0xFFD9DEAE,(int)(fade*glow*14)));
            p.fillCircle(x,y,L.unit*.035f,Glyph.withAlpha(0xFFE6EAC2,(int)(fade*glow*100)));
        }
        p.restore();
    }

    static void undergrowth(Painter p,GameCore c,Layout L,float fade) {
        fade*=1-groveProgress(c)*.65f;
        float[] leaf=new float[34];
        for(int layer=0;layer<2;layer++)for(int i=0;i<10;i++) {
            int seed=901+i*53+layer*317;
            float r=L.w*(.13f+hash(seed)*.10f),side=i%2==0?1:-1;
            float x=side>0?L.w*(-.03f+hash(seed+1)*.11f):L.w*(1.03f-hash(seed+1)*.11f);
            float span=L.deckTop+r*5;
            float y=((i+hash(seed+2))*.1f*span+c.skyClock*L.h*(.012f+layer*.008f)+scroll(c,L))%span-r*2.5f;
            float sway=(float)Math.sin(c.skyClock*.43f+i)*r*.10f;
            int alpha=(int)(fade*(layer==0?38:66));
            // Ferns alternate with rounded broad leaves and slim willow sprays.
            sprig(p,leaf,x,y,r,side*(.6f+sway/r),i%3,alpha,GREENS[(i+layer)%GREENS.length]);
        }
    }

    static void canopy(Painter p,GameCore c,Layout L) {
        if(c.survival.active || c.state==GameCore.TITLE)return;
        float mix=Lands.blend(c);
        float fade=((c.landFrom==LAND?1-mix:0)+(Lands.forStage(Lands.visualStage(c))==LAND?mix:0))*(1-c.drained());
        fade=Math.max(fade,groveProgress(c)*(1-c.drained()));
        if(fade<=.001f)return;
        p.save();
        float margin=Renderer.shakeMargin(c,L);
        p.clipRect(-margin,L.playTop-margin,L.w+margin,L.deckTop);
        float[] leaf=new float[34];
        for(int i=0;i<9;i++) {
            int seed=1703+i*137;
            float side=i%2==0?1:-1;
            float r=L.w*(.23f+hash(seed)*.12f);
            float span=L.deckTop+r*4;
            float y=((i+hash(seed+3))/9f*span+c.skyClock*L.h*.034f+scroll(c,L)*1.35f)%span-r*2;
            float x=side>0?-r*.12f:L.w+r*.12f;
            float sway=(float)Math.sin(c.skyClock*.36f+i*2.3f)*.16f;
            sprig(p,leaf,x,y,r,side*(.95f+sway),i%3==0?2:1,(int)(fade*(31-14*groveProgress(c))),GREENS[i%4]);
        }
        p.restore();
    }

    private static void sprig(Painter p,float[] shape,float x,float y,float r,float lean,int kind,int a,int color) {
        int count=kind==0?6:3;
        float tipX=x+r*lean,tipY=y-r*1.45f;
        float bend=r*.16f;
        p.polyline(new float[]{x,y,x+r*lean*.35f,y-r*.65f,tipX,tipY},
                Glyph.withAlpha(0xFF91A58A,a/2),r*.018f);
        for(int j=0;j<count;j++) {
            float t=(j+.45f)/(count+.3f);
            float bx=x+(tipX-x)*t-bend*(float)Math.sin(t*Math.PI);
            float by=y+(tipY-y)*t;
            float size=r*(kind==0?.38f:.64f)*(1-t*.48f);
            for(int side=-1;side<=1;side+=2) {
                float dx=side*size*(kind==2?.57f:.91f)+lean*size*.32f;
                float dy=-size*(kind==2?1.02f:.55f);
                leaf(p,shape,bx,by,dx,dy,size*(kind==0?.17f:kind==1?.38f:.19f),color,a);
            }
        }
        leaf(p,shape,tipX-r*lean*.16f,tipY+r*.20f,r*lean*.27f,-r*.42f,r*.14f,color,a);
    }

    /** One tapered contour, with a translucent half-leaf highlight instead of stacked ovals. */
    private static void leaf(Painter p,float[] shape,float x,float y,float dx,float dy,float width,int color,int a) {
        float length=(float)Math.sqrt(dx*dx+dy*dy),nx=-dy/length,ny=dx/length;
        for(int i=0;i<17;i++) {
            float t=i<=8?i/8f:(16-i)/8f;
            float bulge=(float)Math.sin(t*Math.PI)*width*(i<=8?1:-.82f);
            shape[i*2]=x+dx*t+nx*bulge;
            shape[i*2+1]=y+dy*t+ny*bulge;
        }
        p.fillPoly(shape,Glyph.withAlpha(color,a));
        p.fillPoly(new float[]{x,y,x+dx*.40f+nx*width*.65f,y+dy*.40f+ny*width*.65f,
                x+dx*.78f+nx*width*.40f,y+dy*.78f+ny*width*.40f,x+dx,y+dy},
                Glyph.withAlpha(0xFFD0D8B2,a/3));
        p.line(x,y,x+dx*.86f,y+dy*.86f,Glyph.withAlpha(0xFFBDD0AB,a/3),width*.035f);
    }
}
