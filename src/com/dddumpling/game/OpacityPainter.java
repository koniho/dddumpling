package com.dddumpling.game;

/** A light translucency for overlay art, shared by both drawing backends. */
final class OpacityPainter implements Painter {
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
    public void polyline(float[] xy,int c,float w) { p.polyline(xy,color(c),w); }
    public void text(String s,float x,float y,float z,int c,int a,boolean b) { p.text(s,x,y,z,color(c),a,b); }
    public void clipRect(float l,float t,float r,float b) { p.clipRect(l,t,r,b); }
    public void clipOutCircle(float x,float y,float r) { p.clipOutCircle(x,y,r); }
    public void save() { p.save(); }
    public void restore() { p.restore(); }
    public void translate(float x,float y) { p.translate(x,y); }
}
