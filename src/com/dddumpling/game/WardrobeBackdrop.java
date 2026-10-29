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
                    Glyph.mix(0xFF1B1730,0xFF303850,Math.min(1,u/.67f)));
        }
        for(int i=0;i<24;i++) {
            float sx=w*((i*.618034f+.11f+t*(i%2==0?.0028f:.0015f))%1.04f-.02f);
            float sy=h*(.025f+((i*.381966f)%1)*.49f+.006f*(float)Math.sin(t*.38f+i*1.7f));
            float twinkle=.5f+.5f*(float)Math.sin(t*.7f+i*2.4f),r=w*(i%5==0?.0022f:.0013f);
            p.fillCircle(sx,sy,r*2.5f,Glyph.withAlpha(0xFFABC6EE,(int)(8+twinkle*10)));
            p.fillCircle(sx,sy,r,Glyph.withAlpha(0xFFD4E2FF,(int)(48+twinkle*55)));
        }
        cloud(p,w*((.47f+t*.013f)%1.70f-.35f),h*(.365f+.005f*(float)Math.sin(t*.31f)),w*.25f,w*.073f);
        cloud(p,w*((1.24f+t*.010f)%1.70f-.35f),h*(.425f+.006f*(float)Math.sin(t*.26f+2)),w*.29f,w*.083f);
        cloud(p,w*((.95f+t*.007f)%1.70f-.35f),h*(.07f+.004f*(float)Math.sin(t*.23f+4)),w*.23f,w*.055f);
        hill(p,w,h,ground-h*.055f,h*.060f,.12f,0xFF354B59);
        hill(p,w,h,ground-h*.025f,h*.055f,.87f,0xFF2C414C);
        hill(p,w,h,ground,h*.080f,.46f,0xFF233B43);
        // A moonlit slope keeps the hill readable without competing with accessories.
        hill(p,w,h,ground+h*.085f,h*.065f,.82f,0xFF29434A);
        float x=w*.5f,r=Storybook.heroRadius(L);
        for(int i=3;i>0;i--)p.fillEllipse(x,ground+r*.03f,r*(.64f+i*.11f),
                r*(.09f+i*.025f),Glyph.withAlpha(0xFF0B1429,9+i*3));
        // A few tiny paired blades at the sides keep the center uncluttered.
        for(int i=0;i<6;i++) {
            float xx=w*(i<3?.04f+i*.065f:.83f+(i-3)*.065f),yy=ground+h*(.035f+(i%3)*.025f);
            float sway=(float)Math.sin(t*.65f+i)*w*.004f;
            p.polyline(new float[]{xx-w*.010f+sway,yy-h*.009f,xx,yy,xx+w*.012f+sway,yy-h*.012f},
                    0x505A827D,w*.003f);
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
            p.fillPoly(a,Glyph.withAlpha(0xFF9EABCA,layer==0?28:5));
        }
    }
}
