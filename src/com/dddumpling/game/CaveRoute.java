package com.dddumpling.game;

/** Arc-length sampling makes lateral bends cost the same travel time as straight passages. */
final class CaveRoute {
    static final float LENGTH=10.6f;
    static final float[] FORKS={2f,5f,8f};
    static final float[] EVENTS_AT={.8f,2.75f,4.2f,5.75f,7.2f,8.75f,10.1f};
    private static final int[][] EVENTS={{Cave.ROCKS,Cave.SHADOW},{Cave.SAND,Cave.ROCKS},{Cave.SHADOW,Cave.SAND}};
    private static final float[] X={0f,.6f,1.5f,.8f,-.55f,-1.4f,-.6f,.8f,1.5f,.4f,-.8f};
    private static final float[] Y={0f,.55f,.45f,1.3f,1.5f,1.05f,2.3f,2.4f,1.9f,3.1f,3.2f};
    private static final int SAMPLES=240;
    private static final float[] PX=new float[SAMPLES+1],PY=new float[SAMPLES+1],DIST=new float[SAMPLES+1];
    static {
        for(int i=0;i<=SAMPLES;i++) {
            float t=i*(X.length-1f)/SAMPLES;
            PX[i]=curve(X,t);PY[i]=curve(Y,t);
            if(i>0)DIST[i]=DIST[i-1]+(float)Math.hypot(PX[i]-PX[i-1],PY[i]-PY[i-1]);
        }
        float k=LENGTH/DIST[SAMPLES];
        for(int i=0;i<=SAMPLES;i++){PX[i]*=k;PY[i]*=k;DIST[i]*=k;}
    }
    private CaveRoute() {}
    private static float curve(float[] p,float at) {
        int i=Math.min(p.length-2,(int)at);float t=at-i;
        float a=p[Math.max(0,i-1)],b=p[i],c=p[i+1],d=p[Math.min(p.length-1,i+2)];
        return .5f*((2*b)+(-a+c)*t+(2*a-5*b+4*c-d)*t*t+(-a+3*b-3*c+d)*t*t*t);
    }
    private static float sample(float[] values,float at) {
        at=Math.max(0,Math.min(LENGTH,at));int lo=0,hi=SAMPLES;
        while(hi-lo>1){int mid=(lo+hi)/2;if(DIST[mid]<at)lo=mid;else hi=mid;}
        float t=(at-DIST[lo])/(DIST[hi]-DIST[lo]);return values[lo]+(values[hi]-values[lo])*t;
    }
    static float centre(float at) {return sample(PX,at);}
    static float y(float at) {return sample(PY,at);}
    static float heading(float at) {return (float)Math.atan2(centre(at+.035f)-centre(at-.035f),y(at+.035f)-y(at-.035f));}
    static float offset(int fork,int side,float at) {
        float t=Math.max(0,Math.min(1,(at-FORKS[fork])/1.4f));
        return side*.26f*(float)Math.sin(t*Math.PI);
    }
    static float x(int fork,int side,float at) {return centre(at)+offset(fork,side,at)*(float)Math.cos(heading(at));}
    static float branchY(int fork,int side,float at) {return y(at)-offset(fork,side,at)*(float)Math.sin(heading(at));}
    static int event(int fork,int side) {return EVENTS[fork][side<0?0:1];}
    static int encounter(int index,int stage,int[] routes) {
        if(index%2==1) {int fork=index/2;return event(fork,routes[fork]);}
        if(index==0 || index==4)return Cave.SHADOW;
        return (index+stage)%2==0?Cave.ROCKS:Cave.SAND;
    }
}
