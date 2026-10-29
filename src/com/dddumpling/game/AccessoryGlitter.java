package com.dddumpling.game;

/** Samples the live vector surface once; no second character render or native blur layer. */
final class AccessoryGlitter extends OpacityPainter {
    static final int ITEM=16;
    private final float[] xs,ys;
    private final boolean[] surface;
    private final float radius,clock,fade,detail,sweep;
    private final boolean sweeping;
    private static final float[] OVAL=new float[48];
    static {for(int i=0;i<24;i++){float a=i*Softbody.TAU/24;OVAL[i*2]=(float)Math.cos(a);OVAL[i*2+1]=(float)Math.sin(a);}}
    static AccessoryGlitter begin(Painter p,int who,float x,float y,float r,float t,float fade) {
        return p.accessory(who,Accessories.FLAIR)==ITEM?new AccessoryGlitter(p,x,y,r,t,fade):null;
    }
    AccessoryGlitter(Painter p,float x,float y,float r,float t,float fade) {
        super(p,1);radius=r;clock=t;this.fade=fade;detail=AccessoryArt.detail(p,r);
        float phase=(clock%4.6f)/2.8f;sweeping=detail>0 && radius>0 && phase<=1;
        sweep=x+y*.42f+radius*(-1.8f+phase*3.6f);
        int count=detail>0?200:32;xs=new float[count];ys=new float[count];surface=new boolean[count];
        for(int i=0;i<count;i++) {
            xs[i]=x+r*(Draw.hash(i*137+41)*1.72f-.86f);
            ys[i]=y+r*(Draw.hash(i*97+203)*1.66f-.83f);
        }
    }
    public ArtStamp stamps(){return null;}
    static int gold(int c) {
        int red=(c>>16)&255,green=(c>>8)&255,blue=c&255;
        if(Math.max(red,Math.max(green,blue))<80)return c;
        float light=(red*.299f+green*.587f+blue*.114f)/255f;
        float shade=.38f+.62f*light;
        int gold=0xFF000000|((int)(255*shade)<<16)|((int)(205*shade)<<8)|(int)(91*shade);
        // Preserve the highlights and existing depth, with a hint of the original body hue.
        float strength=light>.90f?.50f:.78f;
        return Glyph.withAlpha(Glyph.mix(c,gold,strength),c>>>24);
    }
    int color(int c) {
        int base=gold(c);
        if(base==c)return base;
        float glow=.025f+.025f*(.5f+.5f*(float)Math.sin(clock*1.6f));
        return Glyph.withAlpha(Glyph.mix(base,0xFFFFE7A1,glow),c>>>24);
    }
    /** Clip a surface to one side of a diagonal; shared geometry preserves holes. */
    static float[] clip(float[] a,float edge,boolean lower) {
        if(a.length<6)return new float[0];
        float[] out=new float[a.length+4];int n=0;
        for(int i=0,j=a.length-2;i<a.length;j=i,i+=2) {
            float u=a[i]+a[i+1]*.42f,v=a[j]+a[j+1]*.42f;
            boolean in=lower?u>=edge:u<=edge,was=lower?v>=edge:v<=edge;
            if(in!=was) {
                float t=(edge-v)/(u-v);
                if(n+4>out.length)out=java.util.Arrays.copyOf(out,out.length*2);
                out[n++]=a[j]+(a[i]-a[j])*t;out[n++]=a[j+1]+(a[i+1]-a[j+1])*t;
            }
            if(in) {
                if(n+2>out.length)out=java.util.Arrays.copyOf(out,out.length*2);
                out[n++]=a[i];out[n++]=a[i+1];
            }
        }
        return java.util.Arrays.copyOf(out,n);
    }
    private void shimmer(float[][] shapes,int c) {
        if(!sweeping || !paint(c))return;
        float min=Float.MAX_VALUE,max=-Float.MAX_VALUE,top=Float.MAX_VALUE,bottom=-Float.MAX_VALUE;
        for(float[] a:shapes)for(int i=0;i<a.length;i+=2){float u=a[i]+a[i+1]*.42f;min=Math.min(min,u);max=Math.max(max,u);top=Math.min(top,a[i+1]);bottom=Math.max(bottom,a[i+1]);}
        if(max<sweep-radius*.44f || min>sweep+radius*.44f || max-min<radius*.30f || bottom-top<radius*.20f)return;
        for(int layer=0;layer<3;layer++) {
            float width=radius*(.44f-layer*.15f);
            float[][] bands=new float[shapes.length][];boolean visible=false;
            for(int i=0;i<shapes.length;i++) {
                bands[i]=clip(clip(shapes[i],sweep-width,true),sweep+width,false);
                visible|=bands[i].length>=6;
            }
            if(visible)p.fillContours(bands,Glyph.withAlpha(0xFFFFE9A3,(int)((c>>>24)*(20+layer*11)/255f*detail)));
        }
    }
    private boolean paint(int c) {return (c>>>24)>120*fade;}
    private void mark(int i,int c,boolean feature) {
        // Deep eye/mouth ink remains readable rather than sparkling over the expression.
        surface[i]=!feature || Math.max((c>>16)&255,Math.max((c>>8)&255,c&255))>95;
    }
    public void fillEllipse(float x,float y,float rx,float ry,int c) {
        super.fillEllipse(x,y,rx,ry,c);if(!paint(c)||rx<=0||ry<=0)return;
        if(sweeping && rx>radius*.23f && ry>radius*.20f) {
            float[] shape=new float[OVAL.length];
            for(int i=0;i<shape.length;i+=2){shape[i]=x+rx*OVAL[i];shape[i+1]=y+ry*OVAL[i+1];}
            shimmer(new float[][]{shape},c);
        }
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
        if(sweeping)shimmer(new float[][]{a},c);
        for(int i=0;i<xs.length;i++)if(inside(a,xs[i],ys[i]))mark(i,c,false);
    }
    public void fillContours(float[][] a,int c) {
        super.fillContours(a,c);if(!paint(c))return;
        if(sweeping)shimmer(a,c);
        for(int i=0;i<xs.length;i++){boolean in=false;for(float[] ring:a)if(inside(ring,xs[i],ys[i]))in=!in;if(in)mark(i,c,false);}
    }
    void finish() {
        for(int i=0;i<xs.length;i++)if(surface[i]) {
            float tw=.5f+.5f*(float)Math.sin(clock*(2.8f+(i%4)*.23f)+i*2.399f);
            tw=tw*tw*tw;
            float a=fade*(i<32?1:detail),size=radius*(.0035f+.009f*tw);
            int color=Glyph.mix(0xFFFFBE36,0xFFFFF0B0,.45f+.55f*tw);
            p.fillCircle(xs[i],ys[i],size,Draw.fadeBy(Glyph.withAlpha(color,(int)(135+120*tw)),a));
            if(tw>.76f && i%3==0) {
                float ray=size*(2.2f+tw);
                p.line(xs[i]-ray,ys[i],xs[i]+ray,ys[i],Draw.fadeBy(0xFFFFD15B,a*tw),radius*.006f);
                p.line(xs[i],ys[i]-ray,xs[i],ys[i]+ray,Draw.fadeBy(0xFFFFF0B3,a*tw),radius*.006f);
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
