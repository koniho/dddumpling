package com.dddumpling.game;

/** Small travelling refractions in vector geometry; no framebuffer or per-pixel filter. */
final class WaterPainter extends OpacityPainter {
    private final float strength;
    private final WaterRipples ripples;
    private final float[][] buffers;
    WaterPainter(Painter p,GameCore c,Layout L,float strength) {
        super(p,1);this.strength=strength;
        this.ripples=c.waterRipples;buffers=ripples.buffers;ripples.prepare(c,L);
    }
    static float shift(float clock,float x,float y,float width,float strength) {
        return width*strength*(.006f*(float)Math.sin(y/width*7.5f-clock*.85f)
                +.003f*(float)Math.sin(y/width*13f+x/width*2.1f+clock*1.1f));
    }
    private float dx(float x,float y) {return ripples.refract(x,y)*strength;}
    Painter anchored() {return p;}
    private float[] warped(float[] points) {
        float[] work;
        if(points.length>=buffers.length)work=new float[points.length];
        else {
            work=buffers[points.length];
            if(work==null)buffers[points.length]=work=new float[points.length];
        }
        for(int i=0;i<points.length;i+=2) {
            work[i]=points[i]+dx(points[i],points[i+1]);work[i+1]=points[i+1];
        }
        return work;
    }
    public void fillPoly(float[] xy,int c) {p.fillPoly(warped(xy),c);}
    public void strokePoly(float[] xy,int c,float w) {p.strokePoly(warped(xy),c,w);}
    public void polyline(float[] xy,int c,float w) {p.polyline(warped(xy),c,w);}
    public void fillEllipse(float x,float y,float rx,float ry,int c) {p.fillEllipse(x+dx(x,y),y,rx,ry,c);}
    public void fillCircle(float x,float y,float r,int c) {p.fillCircle(x+dx(x,y),y,r,c);}
    public void strokeCircle(float x,float y,float r,int c,float w) {p.strokeCircle(x+dx(x,y),y,r,c,w);}
    public void line(float x,float y,float a,float b,int c,float w) {p.line(x+dx(x,y),y,a+dx(a,b),b,c,w);}
    public void arc(float x,float y,float rx,float ry,float start,float sweep,int c,float w) {p.arc(x+dx(x,y),y,rx,ry,start,sweep,c,w);}
}
