package com.dddumpling.game;

/** Tiny foley gestures followed by a happy two-syllable squishy voice. */
final class AccessorySound {
    static final float CHEER_START=WardrobeScreen.FLASH, LENGTH=.82f;
    private static final double TAU=Math.PI*2;
    private static final float[] NOTES={784,988,1175,1568,1976};
    private AccessorySound() {}
    static float[] render(int slot,boolean removing) {
        float[] out=new float[(int)(Sfx.RATE*LENGTH)];
        int seed=214+slot*71;float air=0;double phase=0;
        for(int i=0;i<out.length;i++) {
            float t=i/(float)Sfx.RATE,sample=0;
            seed=seed*1664525+1013904223;
            float noise=((seed>>>9)/4194303.5f)-1;
            air+=(noise-air)*.17f;
            float u=t/.31f;
            if(u<1) {
                float env=(float)Math.pow(Math.sin(Math.PI*u),1.5);
                float motion=removing?1-u:u;
                if(slot==Accessories.HAT) {
                    // Soft fabric fwip and a cushioned landing (or an airy lift).
                    sample=air*env*.58f+pop(t,removing?.04f:.17f,removing?330:220,.12f)*.33f;
                } else if(slot==Accessories.GLASSES) {
                    sample=pop(t,.018f,removing?970:1190,.07f)*.33f
                            +pop(t,removing?.105f:.075f,removing?740:1510,.09f)*.28f+air*env*.10f;
                } else if(slot==Accessories.BOOTS) {
                    sample=pop(t,.018f,removing?285:180,.13f)*.57f
                            +pop(t,.15f,removing?360:235,.14f)*.48f+air*env*.12f;
                } else if(slot==Accessories.MASK) {
                    phase+=TAU*(240+440*motion*motion)/Sfx.RATE;
                    sample=(float)Math.sin(phase)*env*.22f+air*env*.28f
                            +pop(t,removing?.025f:.22f,removing?420:750,.065f)*.23f;
                } else {
                    for(int k=0;k<NOTES.length;k++) {
                        float age=t-k*.045f;
                        if(age>=0) {
                            float hz=NOTES[removing?NOTES.length-1-k:k];
                            double a=TAU*hz*age;
                            sample+=(float)(Math.sin(a)+.20*Math.sin(a*2.003))
                                    *soft(age,.21f)*.22f;
                        }
                    }
                    sample+=air*env*.12f;
                }
            }
            sample*=Math.max(0,Math.min(1,(.31f-t)/.02f));
            // A little "yip-yay": rounded formants and a rising second syllable.
            for(int note=0;note<2;note++) {
                float age=t-CHEER_START-note*.135f;
                float len=note==0?.11f:.22f;
                if(age>=0 && age<len) {
                    float v=age/len,hz=note==0?560:690;
                    double a=TAU*hz*(age+.075*age*age/len);
                    float vowel=(float)(Math.sin(a)+.30*Math.sin(a*2)+.20*Math.sin(a*3)
                            +.08*Math.sin(a*4));
                    sample+=vowel*(float)Math.pow(Math.sin(Math.PI*v),1.4)*.36f;
                }
            }
            out[i]=sample;
        }
        return out;
    }
    private static float soft(float t,float length) {
        if(t<0 || t>=length)return 0;
        return (float)((1-Math.exp(-t*240))*Math.exp(-t*18))*Math.min(1,(length-t)/.025f);
    }
    private static float pop(float t,float start,float hz,float length) {
        float age=t-start;
        if(age<0 || age>=length)return 0;
        double a=TAU*hz*(age-.27*age*age/length);
        return (float)(Math.sin(a)+.18*Math.sin(a*2))*soft(age,length);
    }
}
