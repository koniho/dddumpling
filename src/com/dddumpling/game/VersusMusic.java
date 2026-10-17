package com.dddumpling.game;

/** Four-bar trumpet fanfare transcribed from the user's Oct 16 20:39 voice memo. */
final class VersusMusic {
    static final float BPM=224f, DURATION=4.3f;
    static final float GLIDE_START=9f*60f/BPM;
    // Eighth-note repeated-note contour, excluding the unpitched count-in and handling noise.
    private static final int[] MELODY={78,78,76,79,79,76,78,78,76,78};
    private static final int KICK=0,DUM=1,TEK=2;
    // Saidi: D T - D D - T - across 1 & 2 & 3 & 4 &; the kick is a separate layer.
    private static final int[] SAIDI={DUM,TEK,-1,DUM,DUM,-1,TEK,-1};
    private static short[] cached;
    private VersusMusic() {}

    static float finalMidi(float seconds) {
        float u=Math.max(0,Math.min(1,(seconds-GLIDE_START)/(DURATION-GLIDE_START)));
        return 78-24*(float)Math.pow(u,1.12);
    }

    static synchronized short[] clip() {
        if(cached!=null)return cached;
        float[] mix=new float[(int)(Sfx.RATE*DURATION)];
        float beat=60f/BPM;
        // Two quick brass calls preserve the memo's contour at double tempo.
        for(int call=0;call<2;call++)for(int n=0;n<(call==0?9:10);n++) {
            float start=(call*4.5f+n*.5f)*beat;
            boolean glide=call==1 && n==9;
            float duration=glide?DURATION-start:beat*.41f;
            float accent=n%3==0?1.16f:1;
            horn(mix,start,duration,MELODY[n],(call==0?.34f:.43f)*accent,glide);
            horn(mix,start,duration,MELODY[n]-12,.13f*accent,glide);
        }
        // A small brass section gives the lead a celebratory fanfare attack.
        for(int bar=0;bar<3;bar++)for(int midi:new int[]{52,59,64,67})
            horn(mix,bar*4*beat,beat*.7f,midi,.085f,false);
        for(int step=0;step<32;step++) {
            float start=step*.5f*beat;
            if(step%2==0)drum(mix,start,KICK,.95f,step);
            int stroke=SAIDI[step%8];
            if(stroke>=0)drum(mix,start,stroke,stroke==DUM?.29f:.22f,step);
        }
        float peak=0;
        for(float value:mix)peak=Math.max(peak,Math.abs(value));
        cached=new short[mix.length];
        for(int i=0;i<mix.length;i++) {
            float t=i/(float)Sfx.RATE;
            float fade=Math.min(1,t/.006f)*Math.min(1,(DURATION-t)/.045f);
            cached[i]=(short)(mix[i]/Math.max(.001f,peak)*11000*fade);
        }
        cached[cached.length-1]=0;
        return cached;
    }

    private static void horn(float[] mix,float start,float duration,int midi,float gain,boolean glide) {
        int first=(int)(start*Sfx.RATE),count=Math.min((int)(duration*Sfx.RATE),mix.length-first);
        double phase=0;float smooth=0;
        for(int i=0;i<count;i++) {
            float t=i/(float)Sfx.RATE;
            float pitch=glide?finalMidi(start+t)+(midi-78):midi;
            pitch-=.35f*(float)Math.exp(-t*55); // A tiny brass scoop into each tongued note.
            float hz=440f*(float)Math.pow(2,(pitch-69)/12f);
            phase+=Math.PI*2*hz/Sfx.RATE;
            float tone=0;
            for(int harmonic=1;harmonic<=8;harmonic++) {
                float brightness=(.72f+.28f*(float)Math.exp(-t*12f));
                float weight=harmonic==1?1:brightness/(float)Math.pow(harmonic,.70);
                tone+=(float)Math.sin(phase*harmonic)*weight;
            }
            smooth+=(tone-smooth)*.58f;
            float attack=Math.min(1,t/.008f),release=Math.min(1,(duration-t)/.025f);
            float swell=.82f+.18f*(float)Math.exp(-t*8);
            mix[first+i]+=smooth*gain*attack*release*swell;
        }
    }

    /** Heavy low kick plus resonant goblet-drum strokes. No cymbals or hi-hats. */
    private static void drum(float[] mix,float start,int kind,float gain,int seed) {
        int first=(int)(start*Sfx.RATE);
        float length=kind==KICK?.24f:kind==DUM?.16f:.075f;
        int count=Math.min((int)(length*Sfx.RATE),mix.length-first),noise=137+seed*7919;
        float phase=0,sub=0,previous=0;
        for(int i=0;i<count;i++) {
            float t=i/(float)Sfx.RATE,u=t/length;
            noise=noise*1664525+1013904223;
            float white=(noise>>>8)/8388608f-1;
            float high=white-previous;previous=white;
            float hz=kind==KICK?46+140*(float)Math.exp(-t*48)
                    :kind==DUM?125+65*(float)Math.exp(-t*45):430+180*(float)Math.exp(-t*70);
            phase+=Softbody.TAU*hz/Sfx.RATE;
            sub+=Softbody.TAU*48/Sfx.RATE;
            float voice;
            if(kind==KICK) voice=(float)Math.sin(phase)*.85f+(float)Math.sin(sub)*.45f
                    +high*.12f*(float)Math.exp(-t*95);
            else voice=(float)Math.sin(phase)*.72f+(float)Math.sin(phase*1.59f)*.24f
                    +(float)Math.sin(phase*2.13f)*.10f+high*(kind==TEK?.22f:.07f)*(float)Math.exp(-t*65);
            mix[first+i]+=voice*gain*Math.min(1,t/.002f)*(float)Math.exp(-u*(kind==KICK?4.5f:6f));
        }
    }
}
