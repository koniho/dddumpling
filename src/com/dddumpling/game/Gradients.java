package com.dddumpling.game;

/** Shared ramp profiles; alpha is accumulated before replacing overlapping soft fills. */
final class Gradients {
    static final int SAMPLES=33;
    static final float[] STOPS=new float[SAMPLES];
    static {for(int i=0;i<SAMPLES;i++)STOPS[i]=i/(SAMPLES-1f);}
    private Gradients() {}
    static int alpha(int color,float alpha) {return Glyph.withAlpha(color,Math.round(Math.max(0,Math.min(255,alpha))));}
    static int sample(int[] colors,float[] stops,float u) {
        if(u<=stops[0])return colors[0];int n=colors.length-1;
        if(u>=stops[n])return colors[n];int at=0;while(u>stops[at+1])at++;
        float t=(u-stops[at])/(stops[at+1]-stops[at]);int a=colors[at],b=colors[at+1];
        return alpha(Glyph.mix(a,b,t),(a>>>24)+((b>>>24)-(a>>>24))*t);
    }
    static int[] bell(int color,float peak) {
        int[] colors=new int[SAMPLES];
        for(int i=0;i<SAMPLES;i++){float t=STOPS[i]*2-1,v=Math.max(0,1-t*t);colors[i]=alpha(color,peak*v*v);}return colors;
    }
    static int[] nested(int color,float each,int count,float inner) {
        int[] colors=new int[SAMPLES];
        for(int i=0;i<SAMPLES;i++) {
            float layers=count*Math.min(1,(1-STOPS[i])/(1-inner));
            colors[i]=alpha(color,255*(1-(float)Math.pow(1-each/255f,layers)));
        }
        return colors;
    }
    static float[] rect(float l,float t,float r,float b) {return new float[]{l,t,r,t,r,b,l,b};}
    // Clip only the internal joins; each gradient's geometry extends past its clip edge.
    static void capsulePieces(Painter p,float x,float top,float bottom,float radius,int[] colors,float[] stops) {
        int n=colors.length;int[] across=new int[n*2-1];float[] positions=new float[across.length];
        for(int i=0;i<n;i++) {
            across[n-1-i]=across[n-1+i]=colors[i];
            positions[n-1-i]=(1-stops[i])*.5f;positions[n-1+i]=(1+stops[i])*.5f;
        }
        p.save();p.clipRect(x-radius,top,x+radius,bottom);
        p.linearGradient(rect(x-radius,top-radius,x+radius,bottom+radius),x-radius,top,x+radius,top,across,positions);p.restore();
        p.save();p.clipRect(x-radius,top-radius,x+radius,top);
        p.radialGradient(x,top,radius,radius,colors,stops);p.restore();
        p.save();p.clipRect(x-radius,bottom,x+radius,bottom+radius);
        p.radialGradient(x,bottom,radius,radius,colors,stops);p.restore();
    }
}
