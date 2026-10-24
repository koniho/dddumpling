package com.dddumpling.game;

/** Muted undergrowth and an overhead canopy; all motion follows the scenery clock. */
final class MushroomForest extends Draw {
    static final int LAND=3;
    private static final int[] GREENS={0xFF71917A,0xFF91A486,0xFF587D70,0xFFACB394};
    private MushroomForest() {}

    static void undergrowth(Painter p,GameCore c,Layout L,float fade) {
        float[] leaf=new float[34];
        for(int layer=0;layer<2;layer++)for(int i=0;i<10;i++) {
            int seed=901+i*53+layer*317;
            float r=L.w*(.13f+hash(seed)*.10f),side=i%2==0?1:-1;
            float x=side>0?L.w*(-.03f+hash(seed+1)*.11f):L.w*(1.03f-hash(seed+1)*.11f);
            float span=L.deckTop+r*5;
            float y=((i+hash(seed+2))*.1f*span+c.skyClock*L.h*(.012f+layer*.008f))%span-r*2.5f;
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
            float y=((i+hash(seed+3))/9f*span+c.skyClock*L.h*.034f)%span-r*2;
            float x=side>0?-r*.12f:L.w+r*.12f;
            float sway=(float)Math.sin(c.skyClock*.36f+i*2.3f)*.16f;
            sprig(p,leaf,x,y,r,side*(.95f+sway),i%3==0?2:1,(int)(fade*31),GREENS[i%4]);
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
