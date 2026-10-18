package com.dddumpling.game;

/** Ordered closed-path tracing. Crossings never advance to an unrelated part of the path. */
final class TraceGesture {
    private static final float TOLERANCE=.38f;
    private float[] points,distances;
    float length,start,position,travel,lastX,lastY;
    boolean active;
    TraceGesture() {setShape(circle());}
    static float[] circle() {
        float[] out=new float[66];
        for(int i=0;i<=32;i++) {float a=Softbody.TAU*i/32;out[i*2]=(float)Math.cos(a);out[i*2+1]=(float)Math.sin(a);}
        return out;
    }
    void setShape(float[] shape) {
        points=shape.clone();distances=new float[points.length/2];length=0;
        for(int i=1;i<distances.length;i++) {
            length+=(float)Math.hypot(points[i*2]-points[(i-1)*2],points[i*2+1]-points[(i-1)*2+1]);distances[i]=length;
        }
        clear();
    }
    void clear() {active=false;travel=0;}
    float progress() {return Math.min(1,Math.abs(travel)/(length*.94f));}
    float coordinate(float at,int axis) {
        at=(at%length+length)%length;
        for(int i=1;i<distances.length;i++)if(at<=distances[i]) {
            float f=(at-distances[i-1])/(distances[i]-distances[i-1]);
            return points[(i-1)*2+axis]+(points[i*2+axis]-points[(i-1)*2+axis])*f;
        }
        return points[axis];
    }
    boolean begin(float x,float y) {
        clear();float nearest=nearest(x,y,false);
        if(Float.isNaN(nearest))return false;
        active=true;start=position=nearest;lastX=x;lastY=y;return true;
    }
    boolean move(float x,float y) {
        if(!active)return false;
        float fromX=lastX,fromY=lastY,distance=(float)Math.hypot(x-fromX,y-fromY);
        int samples=Math.max(1,(int)Math.ceil(distance/.10f));
        if(samples>100) {clear();return false;}
        for(int sample=1;sample<=samples;sample++) {
            float f=sample/(float)samples,px=fromX+(x-fromX)*f,py=fromY+(y-fromY)*f;
            float nearest=nearest(px,py,true);
            if(Float.isNaN(nearest)) {clear();return false;}
            travel+=wrapped(nearest-position);position=nearest;
            if(progress()>=1 && Math.hypot(px-coordinate(start,0),py-coordinate(start,1))<.48f) {lastX=x;lastY=y;return true;}
        }
        lastX=x;lastY=y;return false;
    }
    private float wrapped(float distance) {
        if(distance>length*.5f)distance-=length;
        if(distance<-length*.5f)distance+=length;
        return distance;
    }
    private float nearest(float x,float y,boolean ordered) {
        float best=TOLERANCE*TOLERANCE,result=Float.NaN;
        for(int i=1;i<distances.length;i++) {
            float sx=points[(i-1)*2],sy=points[(i-1)*2+1],dx=points[i*2]-sx,dy=points[i*2+1]-sy;
            float squared=dx*dx+dy*dy;if(squared<.00001f)continue;
            float f=RainbowCat.clamp(((x-sx)*dx+(y-sy)*dy)/squared,0,1);
            float at=distances[i-1]+(distances[i]-distances[i-1])*f;
            if(ordered && Math.abs(wrapped(at-position))>.45f)continue;
            float ex=x-sx-dx*f,ey=y-sy-dy*f,error=ex*ex+ey*ey;
            if(error<best) {best=error;result=at;}
        }
        return result;
    }
}
