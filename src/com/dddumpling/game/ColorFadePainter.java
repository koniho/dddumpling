package com.dddumpling.game;

/** Tints and fades a complete drawing, including eyes, outlines, and highlights. */
final class ColorFadePainter implements Painter, ArtStamp {
    public int skin() { return p.skin(); }
    public int accessory(int who,int slot) {return p.accessory(who,slot);}
    public float accessorySize(float radius) {return p.accessorySize(radius);}
    public boolean accessoryTargets() {return p.accessoryTargets();}
    public void accessoryTarget(int slot,float[] corners) {p.accessoryTarget(slot,corners);}
    public DuckBodies ducks() { return p.ducks(); }
    public GatorCompanion gator() { return p.gator(); }
    public ArtStamp stamps() { return opacity==1 && p.stamps()!=null?this:null; }
    public boolean beginCave(float width,float height) {
        return p.stamps()!=null && p.stamps().beginCave(width,height);
    }
    public void endCave() {p.stamps().endCave();}
    public boolean stamp(int art,float x,float y,float w,float h,float angle,int inner,float mix) {
        ArtStamp cache=stamps()==null?null:p.stamps();
        if(cache==null)return false;
        float combined=amount+(1-amount)*mix;
        int color=combined==0?0:Glyph.mix(inner,tint,amount/combined);
        return cache.stamp(art,x,y,w,h,angle,color,combined);
    }
    final Painter p;
    public boolean greenStamp(int art,float x,float y,float w,float h,float angle,int inner,float mix,float phase) {
        ArtStamp cache=stamps()==null?null:p.stamps();
        if(cache==null)return false;
        float combined=amount+(1-amount)*mix;
        int color=combined==0?0:Glyph.mix(inner,tint,amount/combined);
        return cache.greenStamp(art,x,y,w,h,angle,color,combined,phase);
    }
    final int tint;
    final float amount, opacity;
    ColorFadePainter(Painter p,int tint,float amount,float opacity) {
        this.p=p; this.tint=tint; this.amount=amount; this.opacity=opacity;
    }
    int color(int c) {
        return Glyph.withAlpha(Glyph.mix(c,tint,amount),(int)((c>>>24)*opacity));
    }
    public boolean gradients(){return p!=null && p.gradients();}
    private int[] gradientColors(int[] colors) {
        int[] mapped=new int[colors.length];for(int i=0;i<colors.length;i++)mapped[i]=color(colors[i]);return mapped;
    }
    public boolean linearGradient(float[] polygon,float x0,float y0,float x1,float y1,int[] colors,float[] stops) {
        return gradients() && p.linearGradient(polygon,x0,y0,x1,y1,gradientColors(colors),stops);
    }
    public boolean radialGradient(float x,float y,float rx,float ry,int[] colors,float[] stops) {
        return gradients() && p.radialGradient(x,y,rx,ry,gradientColors(colors),stops);
    }
    public boolean capsuleGradient(float x,float top,float bottom,float radius,int[] colors,float[] stops) {
        return gradients() && p.capsuleGradient(x,top,bottom,radius,gradientColors(colors),stops);
    }
    public void fillPoly(float[] xy,int c) { p.fillPoly(xy,color(c)); }
    public void fillContours(float[][] xy,int c) { p.fillContours(xy,color(c)); }
    public void strokePoly(float[] xy,int c,float w) { p.strokePoly(xy,color(c),w); }
    public void fillCircle(float x,float y,float r,int c) { p.fillCircle(x,y,r,color(c)); }
    public void strokeCircle(float x,float y,float r,int c,float w) { p.strokeCircle(x,y,r,color(c),w); }
    public void arc(float x,float y,float rx,float ry,float a,float s,int c,float w) { p.arc(x,y,rx,ry,a,s,color(c),w); }
    public void fillEllipse(float x,float y,float rx,float ry,int c) { p.fillEllipse(x,y,rx,ry,color(c)); }
    public void verticalGradient(float l,float t,float r,float b,int[] colors,float[] stops) {
        p.verticalGradient(l,t,r,b,gradientColors(colors),stops);
    }
    public void fillRect(float l,float t,float r,float b,int c) { p.fillRect(l,t,r,b,color(c)); }
    public void line(float x,float y,float a,float b,int c,float w) { p.line(x,y,a,b,color(c),w); }
    public void fastPolyline(float[] xy,int c,float w) { p.fastPolyline(xy,color(c),w); }
    public void polyline(float[] xy,int c,float w) { p.polyline(xy,color(c),w); }
    public void text(String s,float x,float y,float z,int c,int a,boolean b) { p.text(s,x,y,z,color(c),a,b); }
    public void clipRect(float l,float t,float r,float b) { p.clipRect(l,t,r,b); }
    public void clipOutCircle(float x,float y,float r) { p.clipOutCircle(x,y,r); }
    public void save() { p.save(); }
    public void restore() { p.restore(); }
    public void translate(float x,float y) { p.translate(x,y); }
}
