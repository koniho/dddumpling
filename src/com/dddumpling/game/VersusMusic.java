package com.dddumpling.game;

/** Two-bar trumpet fanfare transcribed from the user's Oct 16 20:39 voice memo. */
final class VersusMusic {
    static final float BPM=112f, DURATION=4.3f;
    static final float GLIDE_START=4.5f*60f/BPM;
    // Eighth-note repeated-note contour, excluding the unpitched count-in and handling noise.
    private static final int[] MELODY={66,66,64,67,67,64,66,66,64,66};
    private static short[] cached;
    private VersusMusic() {}

    static float finalMidi(float seconds) {
        float u=Math.max(0,Math.min(1,(seconds-GLIDE_START)/(DURATION-GLIDE_START)));
        return 66-24*(float)Math.pow(u,1.12);
    }

    static synchronized short[] clip() {
        if(cached!=null)return cached;
        float[] mix=new float[(int)(Sfx.RATE*DURATION)];
        float beat=60f/BPM;
        for(int n=0;n<MELODY.length;n++) {
            float start=n*.5f*beat;
            horn(mix,start,n==MELODY.length-1?DURATION-start:beat*.43f,MELODY[n],.45f,n==MELODY.length-1);
        }
        // Quiet lower brass answers the lead without obscuring the name announcement.
        for(int bar=0;bar<2;bar++) {
            horn(mix,bar*4*beat,beat*.7f,52,.13f,false);
            horn(mix,(bar*4+2)*beat,beat*.7f,47,.11f,false);
            horn(mix,(bar*4+1.5f)*beat,beat*.32f,55,.07f,false);
        }
        for(int step=0;step<16;step++) {
            float start=step*.5f*beat;
            drum(mix,start,2,.045f,step);
            if(step%4==0)drum(mix,start,0,.32f,step);
            if(step%4==2)drum(mix,start,1,.15f,step);
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
            float pitch=glide?finalMidi(start+t):midi;
            pitch-=.35f*(float)Math.exp(-t*55); // A tiny brass scoop into each tongued note.
            float hz=440f*(float)Math.pow(2,(pitch-69)/12f);
            phase+=Math.PI*2*hz/Sfx.RATE;
            float tone=0;
            for(int harmonic=1;harmonic<=7;harmonic++) {
                float brightness=(.38f+.62f*(float)Math.exp(-t*2.8f));
                float weight=harmonic==1?1:brightness/(float)Math.pow(harmonic,.85);
                tone+=(float)Math.sin(phase*harmonic)*weight;
            }
            smooth+=(tone-smooth)*.36f;
            float attack=Math.min(1,t/.018f),release=Math.min(1,(duration-t)/.055f);
            float swell=.82f+.18f*(float)Math.exp(-t*8);
            mix[first+i]+=smooth*gain*attack*release*swell;
        }
    }

    /** Rounded kick, short snare, and soft closed hats; deterministic noise. */
    private static void drum(float[] mix,float start,int kind,float gain,int seed) {
        int first=(int)(start*Sfx.RATE);
        float length=kind==0?.2f:kind==1?.14f:.045f;
        int count=Math.min((int)(length*Sfx.RATE),mix.length-first),noise=137+seed*7919;
        float phase=0,previous=0;
        for(int i=0;i<count;i++) {
            float t=i/(float)Sfx.RATE,u=t/length;
            noise=noise*1664525+1013904223;
            float white=(noise>>>8)/8388608f-1;
            float high=white-previous;previous=white;
            phase+=Softbody.TAU*(kind==0?48+110*(float)Math.exp(-t*35):175)/Sfx.RATE;
            float voice=kind==0?(float)Math.sin(phase):kind==1?white*.65f+(float)Math.sin(phase)*.35f:high*.55f;
            mix[first+i]+=voice*gain*Math.min(1,t/.002f)*(float)Math.exp(-u*6);
        }
    }
}
