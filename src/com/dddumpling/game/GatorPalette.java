package com.dddumpling.game;

/** Moves green pigment into red without painting over the original shading. */
final class GatorPalette extends OpacityPainter {
    final float phase;
    GatorPalette(Painter p,float phase) {super(p,1);this.phase=phase;}
    int color(int c) {
        int r=(c>>>16)&255,g=(c>>>8)&255,b=c&255;
        float pigment=Math.max(0,g-r)*phase;
        return (c&0xFF000000)|(Math.round(r+pigment)<<16)|(Math.round(g-pigment)<<8)
                |Math.max(0,Math.round(b-.45f*pigment));
    }
    public boolean stamp(int art,float x,float y,float w,float h,float angle,int tint,float mix) {
        ArtStamp cache=p.stamps();
        // Green base art and neutral attack flashes share the same linear transform.
        if(mix>0 && ((tint>>>16)&255)>((tint>>>8)&255))return false;
        return cache!=null && cache.greenStamp(art,x,y,w,h,angle,color(tint),mix,phase);
    }
    static void matrix(float[] out,int tint,float mix,float phase) {
        java.util.Arrays.fill(out,0);
        float keep=1-mix;
        out[0]=out[6]=(1-phase)*keep;out[1]=out[5]=phase*keep;
        out[10]=.45f*phase*keep;out[11]=-.45f*phase*keep;out[12]=keep;
        out[4]=((tint>>>16)&255)*mix;out[9]=((tint>>>8)&255)*mix;out[14]=(tint&255)*mix;
        out[18]=1;
    }
}
