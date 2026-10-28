package com.dddumpling.game;

/** Applies a smooth grayscale palette to every primitive without changing its opacity. */
final class MonochromePainter implements Painter,ArtStamp {
    public int accessory(int who,int slot) {return p.accessory(who,slot);}
    public float accessorySize(float radius) {return p.accessorySize(radius);}
    public boolean accessoryTargets() {return p.accessoryTargets();}
    public void accessoryTarget(int slot,float[] corners) {p.accessoryTarget(slot,corners);}
    public int skin() { return p.skin(); }
    public DuckBodies ducks() { return p.ducks(); }
    public GatorCompanion gator() { return p.gator(); }
    public ArtStamp stamps() { return p.stamps()==null?null:amount==0?p.stamps():this; }
    public boolean stamp(int art,float x,float y,float w,float h,float angle,int tint,float mix) {return false;}
    public boolean waterBand(float[] xy,float radius,int alpha) {
        return p.stamps().waterBand(xy,radius,alpha,amount);
    }
    public boolean waterGlow(float x,float y,float rx,float ry,int alpha) {
        return p.stamps().waterGlow(x,y,rx,ry,alpha,amount);
    }
    public boolean reefVista(int art,float x,float y,float w,float h,int alpha) {
        return p.stamps().reefVista(art,x,y,w,h,alpha,amount);
    }
    public boolean reefVistaShaded(int art,float x,float y,float w,float h,int alpha,float shade,float mono) {
        return p.stamps().reefVistaShaded(art,x,y,w,h,alpha,shade,amount+(1-amount)*mono);
    }
    public boolean reefRock(int art,float x,float y,float w,float h,int alpha) {
        return p.stamps().reefRock(art,x,y,w,h,alpha,amount);
    }
    public boolean crystal(int layer,float x,float y,float w,float h,int alpha) {
        return p.stamps().crystal(layer,x,y,w,h,alpha,amount);
    }
    static void matrix(float[] values,float amount) {
        java.util.Arrays.fill(values,0);
        for(int row=0;row<3;row++)for(int col=0;col<3;col++)
            values[row*5+col]=(col==0?54/256f:col==1?183/256f:19/256f)*amount+(row==col?1-amount:0);
        values[18]=1;
    }
    final Painter p;
    final float amount;
    MonochromePainter(Painter p,float amount) { this.p=p; this.amount=amount; }
    int color(int c) {
        int r=(c>>>16)&255,g=(c>>>8)&255,b=c&255;
        int gray=(r*54+g*183+b*19)/256;
        return (c&0xFF000000)|((int)(r+(gray-r)*amount)<<16)
                |((int)(g+(gray-g)*amount)<<8)|(int)(b+(gray-b)*amount);
    }
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
