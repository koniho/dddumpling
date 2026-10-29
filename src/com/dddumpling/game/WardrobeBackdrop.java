package com.dddumpling.game;

/** A quiet outdoor stage; bounded vector layers shared by both native renderers. */
final class WardrobeBackdrop {
    static void draw(Painter canvas,GameCore c,Layout L,float fade) {
        Painter p=fade<1?new OpacityPainter(canvas,fade):canvas;
        float w=L.w,h=L.h,t=c.clock;
        float ground=(WardrobeToybox.bottom(L,true)+Storybook.panelTop(L))*.5f+Storybook.heroRadius(L)*.90f;
        // Narrow opaque bands avoid extra surfaces or native blur passes.
        for(int i=0;i<128;i++) {
            float u=i/127f;
            p.fillRect(0,h*i/128f,w,h*(i+1)/128f+.5f,
                    Glyph.mix(0xFF52769D,0xFFD0E9DE,Math.min(1,u/.67f)));
        }
        cloud(p,w*(.12f+.045f*(float)Math.sin(t*.045f)),h*.365f,w*.25f,w*.073f);
        cloud(p,w*(.89f+.035f*(float)Math.sin(t*.037f+2)),h*.425f,w*.29f,w*.083f);
        cloud(p,w*(.60f+.055f*(float)Math.sin(t*.029f+4)),h*.07f,w*.23f,w*.055f);
        hill(p,w,h,ground-h*.055f,h*.060f,.12f,0xFFA4CFBB);
        hill(p,w,h,ground-h*.025f,h*.055f,.87f,0xFF8DBDA5);
        hill(p,w,h,ground,h*.080f,.46f,0xFF78A98B);
        // A broad pale slope catches the light without competing with accessories.
        hill(p,w,h,ground+h*.085f,h*.065f,.82f,0xFF85B596);
        float x=w*.5f,r=Storybook.heroRadius(L);
        for(int i=3;i>0;i--)p.fillEllipse(x,ground+r*.03f,r*(.64f+i*.11f),
                r*(.09f+i*.025f),Glyph.withAlpha(0xFF385C58,9+i*3));
        // A few tiny paired blades at the sides keep the center uncluttered.
        for(int i=0;i<6;i++) {
            float xx=w*(i<3?.04f+i*.065f:.83f+(i-3)*.065f),yy=ground+h*(.035f+(i%3)*.025f);
            float sway=(float)Math.sin(t*.65f+i)*w*.004f;
            p.polyline(new float[]{xx-w*.010f+sway,yy-h*.009f,xx,yy,xx+w*.012f+sway,yy-h*.012f},
                    0x50628D7C,w*.003f);
        }
    }
    private static void hill(Painter p,float w,float h,float y,float height,float center,int color) {
        float[] pts=new float[70];
        for(int i=0;i<=32;i++) {
            float x=i/32f,u=(x-center)*1.18f;
            pts[i*2]=x*w;pts[i*2+1]=y+height*u*u;
        }
        pts[66]=w;pts[67]=h;pts[68]=0;pts[69]=h;p.fillPoly(pts,color);
    }
    private static final float[] CLOUD_X={-.66f,-.28f,.20f,.63f};
    private static final float[] CLOUD_W={.34f,.43f,.46f,.37f};
    private static final float[] CLOUD_H={.50f,.84f,1f,.62f};
    private static void cloud(Painter p,float x,float y,float w,float h) {
        for(int layer=3;layer>=0;layer--) {
            float[] a=new float[100];float extra=layer*.035f;
            for(int i=0;i<25;i++) {
                float u=-1+i/12f,edge=(float)Math.sqrt(Math.max(0,1-u*u));
                float crest=0;
                for(int lobe=0;lobe<4;lobe++){
                    float v=(u-CLOUD_X[lobe])/CLOUD_W[lobe];
                    crest=Math.max(crest,CLOUD_H[lobe]*(float)Math.sqrt(Math.max(0,1-v*v)));
                }
                a[i*2]=x+u*w*(1+extra);a[i*2+1]=y-h*(crest+edge*extra*2);
                int j=49-i;a[j*2]=x+u*w*(1+extra);a[j*2+1]=y+edge*h*(.27f+extra);
            }
            p.fillPoly(a,Glyph.withAlpha(0xFFFFFAF0,layer==0?120:16));
        }
    }
}
