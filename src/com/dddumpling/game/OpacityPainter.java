package com.dddumpling.game;

/** A light translucency for overlay art, shared by both drawing backends. */
class OpacityPainter implements Painter, ArtStamp {
    public int skin() { return p.skin(); }
    public int accessory(int who,int slot) {return p.accessory(who,slot);}
    public float accessorySize(float radius) {return p.accessorySize(radius);}
    public boolean accessoryTargets() {return p.accessoryTargets();}
    public void accessoryTarget(int slot,float[] corners) {p.accessoryTarget(slot,corners);}
    public DuckBodies ducks() { return p.ducks(); }
    public GatorCompanion gator() { return p.gator(); }
    // Per-primitive fading is intentionally retained during transitions.
    public ArtStamp stamps() { return amount==1 && p.stamps()!=null?this:null; }
    public boolean stamp(int art,float x,float y,float w,float h,float angle,int tint,float mix) {
        ArtStamp cache=stamps()==null?null:p.stamps();
        return cache!=null && cache.stamp(art,x,y,w,h,angle,tint,mix);
    }
    public boolean forest(int art,float x,float y,float r,float lean,int tint,int alpha) {
        ArtStamp cache=stamps()==null?null:p.stamps();
        return cache!=null && cache.forest(art,x,y,r,lean,tint,alpha);
    }
    public boolean waterBand(float[] xy,float radius,int alpha) {
        ArtStamp cache=stamps()==null?null:p.stamps();
        return cache!=null && cache.waterBand(xy,radius,alpha);
    }
    public boolean waterGlow(float x,float y,float rx,float ry,int alpha) {
        ArtStamp cache=stamps()==null?null:p.stamps();
        return cache!=null && cache.waterGlow(x,y,rx,ry,alpha);
    }
    public boolean reefVista(int art,float x,float y,float w,float h,int alpha) {
        ArtStamp cache=stamps()==null?null:p.stamps();
        return cache!=null && cache.reefVista(art,x,y,w,h,alpha);
    }
    public boolean reefVistaShaded(int art,float x,float y,float w,float h,int alpha,float shade,float mono) {
        ArtStamp cache=stamps()==null?null:p.stamps();
        return cache!=null && cache.reefVistaShaded(art,x,y,w,h,alpha,shade,mono);
    }
    public boolean reefRock(int art,float x,float y,float w,float h,int alpha) {
        ArtStamp cache=stamps()==null?null:p.stamps();
        return cache!=null && cache.reefRock(art,x,y,w,h,alpha);
    }
    public boolean waterBand(float[] xy,float radius,int alpha,float mono) {
        ArtStamp cache=stamps()==null?null:p.stamps();
        return cache!=null && cache.waterBand(xy,radius,alpha,mono);
    }
    public boolean waterGlow(float x,float y,float rx,float ry,int alpha,float mono) {
        ArtStamp cache=stamps()==null?null:p.stamps();
        return cache!=null && cache.waterGlow(x,y,rx,ry,alpha,mono);
    }
    public boolean reefVista(int art,float x,float y,float w,float h,int alpha,float mono) {
        ArtStamp cache=stamps()==null?null:p.stamps();
        return cache!=null && cache.reefVista(art,x,y,w,h,alpha,mono);
    }
    public boolean reefRock(int art,float x,float y,float w,float h,int alpha,float mono) {
        ArtStamp cache=stamps()==null?null:p.stamps();
        return cache!=null && cache.reefRock(art,x,y,w,h,alpha,mono);
    }
    public boolean crystal(int art,float x,float y,float w,float h,int alpha,float mono) {
        ArtStamp cache=stamps()==null?null:p.stamps();
        return cache!=null && cache.crystal(art,x,y,w,h,alpha,mono);
    }
    public boolean crystal(int layer,float x,float y,float w,float h,int alpha) {
        ArtStamp cache=stamps()==null?null:p.stamps();
        return cache!=null && cache.crystal(layer,x,y,w,h,alpha);
    }
    public boolean basin(int art,float x,float y,float w,float h,int alpha) {
        ArtStamp cache=stamps()==null?null:p.stamps();
        return cache!=null && cache.basin(art,x,y,w,h,alpha);
    }
    public boolean basinMesh(int art,float x,float y,float w,float h,float[] offsets,int alpha) {
        ArtStamp cache=stamps()==null?null:p.stamps();
        return cache!=null && cache.basinMesh(art,x,y,w,h,offsets,alpha);
    }
    final Painter p;
    final float amount;
    OpacityPainter(Painter p,float amount) { this.p=p; this.amount=amount; }
    int color(int c) { return Glyph.withAlpha(c,Math.round((c>>>24)*amount)); }
    public void fillPoly(float[] xy,int c) { p.fillPoly(xy,color(c)); }
    public void fillContours(float[][] xy,int c) { p.fillContours(xy,color(c)); }
    public void strokePoly(float[] xy,int c,float w) { p.strokePoly(xy,color(c),w); }
    public void fillCircle(float x,float y,float r,int c) { p.fillCircle(x,y,r,color(c)); }
    public void strokeCircle(float x,float y,float r,int c,float w) { p.strokeCircle(x,y,r,color(c),w); }
    public void arc(float x,float y,float rx,float ry,float a,float s,int c,float w) { p.arc(x,y,rx,ry,a,s,color(c),w); }
    public void fillEllipse(float x,float y,float rx,float ry,int c) { p.fillEllipse(x,y,rx,ry,color(c)); }
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
