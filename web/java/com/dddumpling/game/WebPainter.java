package com.dddumpling.game;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSByRef;
import org.teavm.jso.JSObject;

/** Canvas host. Cached scenery is generated through the same pure vector artwork. */
final class WebPainter implements Painter {
    JSObject context;
    private final WebStamps cache;
    WebPainter(JSObject context, boolean cached) {this.context=context;cache=cached?new WebStamps(this):null;}
    void warm(int i) {if(i<ArtStamp.COUNT)WebStamps.image("gator",i);else if(i<ArtStamp.COUNT+3)WebStamps.image("crystal",i-ArtStamp.COUNT);}
    public ArtStamp stamps() {return cache;}
    public void fillPoly(float[] p,int c) {poly(context,p,c,0,true);}
    public void strokePoly(float[] p,int c,float w) {poly(context,p,c,w,true);}
    public void polyline(float[] p,int c,float w) {poly(context,p,c,w,false);}
    public void fillContours(float[][] paths,int c) {begin(context);for(float[] p:paths)contour(context,p);finish(context,c);}
    public void fillCircle(float x,float y,float r,int c) {ellipse(context,x,y,r,r,c,0);}
    public void strokeCircle(float x,float y,float r,int c,float w) {ellipse(context,x,y,r,r,c,w);}
    public void fillEllipse(float x,float y,float rx,float ry,int c) {ellipse(context,x,y,rx,ry,c,0);}
    public void arc(float x,float y,float rx,float ry,float start,float sweep,int c,float w) {arc(context,x,y,rx,ry,start,sweep,c,w);}
    public void fillRect(float l,float t,float r,float b,int c) {rect(context,l,t,r,b,c);}
    public void verticalGradient(float l,float t,float r,float b,int[] colors,float[] stops) {gradient(context,l,t,r,b,colors,stops);}
    @JSBody(params={"c","l","t","r","b","colors","stops"},script="ddd.paint.verticalGradient(c,l,t,r,b,colors,stops);") private static native void gradient(JSObject c,float l,float t,float r,float b,@JSByRef int[] colors,@JSByRef float[] stops);
    public void line(float x,float y,float a,float b,int c,float w) {line(context,x,y,a,b,c,w);}
    public void text(String s,float x,float y,float z,int c,int a,boolean b) {text(context,s,x,y,z,c,a);}
    public void clipRect(float l,float t,float r,float b) {clip(context,l,t,r,b);}
    public void clipOutCircle(float x,float y,float r) {exclude(context,x,y,r);}
    public void save() {save(context);}
    public void restore() {restore(context);}
    public void translate(float x,float y) {translate(context,x,y);}
    @JSBody(params={"c","p","color","width","closed"},script="ddd.paint.poly(c,p,color,width,closed);") private static native void poly(JSObject c,@JSByRef float[] p,int color,float width,boolean closed);
    @JSBody(params="c",script="c.beginPath();") private static native void begin(JSObject c);
    @JSBody(params={"c","p"},script="ddd.paint.contour(c,p);") private static native void contour(JSObject c,@JSByRef float[] p);
    @JSBody(params={"c","color"},script="c.fillStyle=ddd.paint.color(color);c.fill('evenodd');") private static native void finish(JSObject c,int color);
    @JSBody(params={"c","x","y","rx","ry","color","w"},script="ddd.paint.ellipse(c,x,y,rx,ry,color,w);") private static native void ellipse(JSObject c,float x,float y,float rx,float ry,int color,float w);
    @JSBody(params={"c","x","y","rx","ry","a","s","color","w"},script="ddd.paint.arc(c,x,y,rx,ry,a,s,color,w);") private static native void arc(JSObject c,float x,float y,float rx,float ry,float a,float s,int color,float w);
    @JSBody(params={"c","l","t","r","b","color"},script="c.fillStyle=ddd.paint.color(color);c.fillRect(l,t,r-l,b-t);") private static native void rect(JSObject c,float l,float t,float r,float b,int color);
    @JSBody(params={"c","x","y","a","b","color","w"},script="ddd.paint.line(c,x,y,a,b,color,w);") private static native void line(JSObject c,float x,float y,float a,float b,int color,float w);
    @JSBody(params={"c","s","x","y","z","color","a"},script="c.font=z+'px Bungee';c.fillStyle=ddd.paint.color(color);c.textAlign=a<0?'left':a>0?'right':'center';c.fillText(s,x,y);") private static native void text(JSObject c,String s,float x,float y,float z,int color,int a);
    @JSBody(params={"c","l","t","r","b"},script="c.beginPath();c.rect(l,t,r-l,b-t);c.clip();") private static native void clip(JSObject c,float l,float t,float r,float b);
    @JSBody(params={"c","x","y","r"},script="c.beginPath();c.rect(-100000,-100000,200000,200000);c.moveTo(x+r,y);c.arc(x,y,r,0,Math.PI*2);c.clip('evenodd');") private static native void exclude(JSObject c,float x,float y,float r);
    @JSBody(params="c",script="c.save();") private static native void save(JSObject c);
    @JSBody(params="c",script="c.restore();") private static native void restore(JSObject c);
    @JSBody(params={"c","x","y"},script="c.translate(x,y);") private static native void translate(JSObject c,float x,float y);
}
