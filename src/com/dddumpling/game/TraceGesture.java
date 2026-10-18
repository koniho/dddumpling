package com.dddumpling.game;

/** Ordered closed-path tracing. Crossings never advance to an unrelated part of the path. */
final class TraceGesture {
    static final int SHAPE_COUNT=5;
    private static final float TOLERANCE=.52f;
    private float[] points,distances;
    float length,start,position,travel,lastX,lastY;
    boolean active,straightStar;
    TraceGesture() {setShape(circle());}
    static float[] circle() {
        float[] out=new float[66];
        for(int i=0;i<=32;i++) {float a=Softbody.TAU*i/32;out[i*2]=(float)Math.cos(a);out[i*2+1]=(float)Math.sin(a);}
        return out;
    }
    static float[] star() {
        float[] out=new float[12];
        for(int i=0;i<=5;i++) {
            float angle=-Softbody.TAU*.25f+Softbody.TAU*(i*2%5)/5;
            out[i*2]=(float)Math.cos(angle);out[i*2+1]=(float)Math.sin(angle);
        }
        return out;
    }
    static float[] shape(int capture) {
        if(capture<=0)return circle();
        if(capture==4)return star();
        if(capture==2)return new float[]{0,-1,1,0,0,1,-1,0,0,-1};
        float[] out=new float[130];
        for(int i=0;i<=64;i++) {
            double t=(capture==1?Math.PI:Math.PI*.5)+Softbody.TAU*i/64;
            if(capture==1) {
                out[i*2]=(float)Math.pow(Math.sin(t),3);
                out[i*2+1]=-(float)((13*Math.cos(t)-5*Math.cos(2*t)-2*Math.cos(3*t)-Math.cos(4*t)+2.5)/14.5);
            } else {
                out[i*2]=(float)Math.sin(t);out[i*2+1]=-(float)Math.sin(2*t)*.8f;
            }
        }
        return out;
    }
    void setShape(float[] shape) {setShape(shape,false);}
    void setShape(float[] shape,boolean forgivingStar) {
        straightStar=forgivingStar;
        points=shape.clone();distances=new float[points.length/2];length=0;
        for(int i=1;i<distances.length;i++) {
            length+=(float)Math.hypot(points[i*2]-points[(i-1)*2],points[i*2+1]-points[(i-1)*2+1]);distances[i]=length;
        }
        clear();
    }
    void clear() {active=false;travel=0;}
    float progress() {return Math.min(1,Math.abs(travel)/(length*(straightStar?.90f:.94f)));}
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
            if(progress()>=1 && Math.hypot(px-coordinate(start,0),py-coordinate(start,1))<(straightStar?.80f:TOLERANCE+.10f)) {lastX=x;lastY=y;return true;}
        }
        lastX=x;lastY=y;return false;
    }
    private float wrapped(float distance) {
        if(distance>length*.5f)distance-=length;
        if(distance<-length*.5f)distance+=length;
        return distance;
    }
    private float nearest(float x,float y,boolean ordered) {
        float tolerance=straightStar?.70f:TOLERANCE;
        float best=tolerance*tolerance,result=Float.NaN;
        for(int i=1;i<distances.length;i++) {
            float sx=points[(i-1)*2],sy=points[(i-1)*2+1],dx=points[i*2]-sx,dy=points[i*2+1]-sy;
            float squared=dx*dx+dy*dy;if(squared<.00001f)continue;
            float f=RainbowCat.clamp(((x-sx)*dx+(y-sy)*dy)/squared,0,1);
            float at=distances[i-1]+(distances[i]-distances[i-1])*f;
            // Allow rounded star tips while still excluding unrelated edges at crossings.
            if(ordered && Math.abs(wrapped(at-position))>(straightStar?.85f:.45f))continue;
            float ex=x-sx-dx*f,ey=y-sy-dy*f,error=ex*ex+ey*ey;
            if(error<best) {best=error;result=at;}
        }
        return result;
    }
}
