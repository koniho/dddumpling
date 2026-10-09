package com.dddumpling.game;

/** A single rainbow current expands into a white handoff to the earned collectible. */
final class SurvivalEnd extends Draw {
    static final float DURATION=3.2f, REVEAL=.78f, SOUND_START=.10f;
    private static final int[] COLORS={0xFFFF8EA9,0xFFFFBC82,0xFFFFE993,0xFFA6EBAD,
            0xFF8EDDEB,0xFFABA6F3,0xFFE3A4EF};
    private SurvivalEnd() {}
    static boolean ownsCompanion(GameCore c) {return c.survival.active && c.state==GameCore.OVER;}
    static float white(float t) {
        return t<REVEAL?DuckReward.ease((t-.60f)/(REVEAL-.60f)):
                1-DuckReward.ease((t-REVEAL)/(1-REVEAL));
    }
    static void draw(Painter p,GameCore c,Layout L) {
        if(!ownsCompanion(c) || !c.dying())return;
        float t=c.deathProgress();
        if(t<REVEAL) {
            float enter=DuckReward.ease(t/.30f),grow=DuckReward.ease((t-.30f)/.30f);
            float x=L.w*(1.18f-.68f*enter),y=L.h*(.55f-.05f*grow);
            float height=L.unit*1.2f+(L.h*1.35f-L.unit*1.2f)*grow;
            // Shared boundaries keep the seven bands joined as they expand vertically.
            for(int k=0;k<7;k++) {
                float[] strip=new float[100];
                for(int j=0;j<25;j++) {
                    float dx=(j/24f-.5f)*L.w*1.5f;
                    float wave=(float)Math.sin(dx/L.w*8-t*4)*L.unit*.20f;
                    strip[j*2]=strip[(49-j)*2]=x+dx;
                    strip[j*2+1]=y+(k/7f-.5f)*height+wave;
                    strip[(49-j)*2+1]=y+((k+1)/7f-.5f)*height+wave+1;
                }
                p.fillPoly(strip,fadeBy(COLORS[k],DuckReward.ease(t/.08f)));
            }
        }
        p.fillRect(0,0,L.w,L.h,fadeBy(0xFFFFFFFF,white(t)));
    }
}
