package com.dddumpling.game;

/** Samples the live vector surface once; no second character render or native blur layer. */
final class AccessoryGlitter extends OpacityPainter {
    static final int ITEM=16;
    private final float[] xs,ys;
    private final boolean[] surface;
    private final float radius,clock,fade,detail;
    static AccessoryGlitter begin(Painter p,int who,float x,float y,float r,float t,float fade) {
        return p.accessory(who,Accessories.FLAIR)==ITEM?new AccessoryGlitter(p,x,y,r,t,fade):null;
    }
    AccessoryGlitter(Painter p,float x,float y,float r,float t,float fade) {
        super(p,1);radius=r;clock=t;this.fade=fade;detail=AccessoryArt.detail(p,r);
        int count=detail>0?72:12;xs=new float[count];ys=new float[count];surface=new boolean[count];
        for(int i=0;i<count;i++) {
            xs[i]=x+r*(Draw.hash(i*137+41)*1.72f-.86f);
            ys[i]=y+r*(Draw.hash(i*97+203)*1.66f-.83f);
        }
    }
    public ArtStamp stamps(){return null;}
    private boolean paint(int c) {return (c>>>24)>120*fade;}
    private void mark(int i,int c,boolean feature) {
        // Deep eye/mouth ink remains readable rather than sparkling over the expression.
        surface[i]=!feature || Math.max((c>>16)&255,Math.max((c>>8)&255,c&255))>95;
    }
    public void fillEllipse(float x,float y,float rx,float ry,int c) {
        super.fillEllipse(x,y,rx,ry,c);if(!paint(c)||rx<=0||ry<=0)return;
        for(int i=0;i<xs.length;i++){float dx=(xs[i]-x)/rx,dy=(ys[i]-y)/ry;if(dx*dx+dy*dy<.91f)mark(i,c,rx<radius*.25f && ry<radius*.30f);}
    }
    public void fillCircle(float x,float y,float r,int c){fillEllipse(x,y,r,r,c);}
    static boolean inside(float[] a,float x,float y) {
        boolean in=false;for(int i=0,j=a.length-2;i<a.length;j=i,i+=2)
            if((a[i+1]>y)!=(a[j+1]>y) && x<(a[j]-a[i])*(y-a[i+1])/(a[j+1]-a[i+1])+a[i])in=!in;
        return in;
    }
    public void fillPoly(float[] a,int c) {
        super.fillPoly(a,c);if(!paint(c))return;
        for(int i=0;i<xs.length;i++)if(inside(a,xs[i],ys[i]))mark(i,c,false);
    }
    public void fillContours(float[][] a,int c) {
        super.fillContours(a,c);if(!paint(c))return;
        for(int i=0;i<xs.length;i++){boolean in=false;for(float[] ring:a)if(inside(ring,xs[i],ys[i]))in=!in;if(in)mark(i,c,false);}
    }
    void finish() {
        for(int i=0;i<xs.length;i++)if(surface[i]) {
            float tw=.5f+.5f*(float)Math.sin(clock*(2.8f+(i%4)*.23f)+i*2.399f);
            tw=tw*tw*tw;
            float a=fade*(i<12?1:detail),size=radius*(.006f+.018f*tw);
            int color=Glyph.mix(0xFFFFD875,0xFFFFFFFF,.35f+.65f*tw);
            p.fillCircle(xs[i],ys[i],size,Draw.fadeBy(Glyph.withAlpha(color,(int)(60+195*tw)),a));
            if(tw>.60f) {
                float ray=size*(1.6f+tw);
                p.line(xs[i]-ray,ys[i],xs[i]+ray,ys[i],Draw.fadeBy(0xFFFFEDBE,a*tw),radius*.009f);
                p.line(xs[i],ys[i]-ray,xs[i],ys[i]+ray,Draw.fadeBy(0xFFFFFFFF,a*tw),radius*.009f);
            }
        }
    }
    static void preview(Painter p,float x,float y,float r,float t) {
        AccessoryGlitter g=new AccessoryGlitter(p,x,y,r,t,1);
        g.fillEllipse(x,y,r*.84f,r*.78f,0xFFBD9D6D);
        g.fillEllipse(x-r*.04f,y-r*.04f,r*.78f,r*.73f,0xFFF2D791);g.finish();
        for(int i=0;i<3;i++) {
            float xx=x+r*(-.42f+i*.38f),yy=y+r*(i%2==0?-.23f:.30f);
            float size=r*(.10f+.055f*(.5f+.5f*(float)Math.sin(t*3+i*2)));
            AccessoryArt.star(p,xx,yy,size,0xFFFFFFFF);
        }
    }
}
