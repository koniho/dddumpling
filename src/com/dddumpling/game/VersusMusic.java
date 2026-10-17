package com.dddumpling.game;

/** Four-bar trumpet fanfare transcribed from the user's Oct 16 20:39 voice memo. */
final class VersusMusic {
    static final float BPM=224f, DURATION=4.3f;
    static final float ANNOUNCER_GAIN=.65f;
    private static final float MASTER_PEAK=19000f,MASTER_RMS=6800f;
    static final float GLIDE_START=9f*60f/BPM;
    // Eighth-note repeated-note contour, excluding the unpitched count-in and handling noise.
    private static final int[] MELODY={71,71,69,72,72,69,71,71,69,71};
    private static final int KICK=0,DUM=1,TEK=2;
    // Saidi: D T - D D - T - across 1 & 2 & 3 & 4 &; the kick is a separate layer.
    private static final int[] SAIDI={DUM,TEK,-1,DUM,DUM,-1,TEK,-1};
    private static short[] cached;
    private VersusMusic() {}

    static float finalMidi(float seconds) {
        return glideMidi(71,52,seconds);
    }

    private static float glideMidi(int from,int to,float seconds) {
        float u=Math.max(0,Math.min(1,(seconds-GLIDE_START)/(DURATION-GLIDE_START)));
        return from+(to-from)*(float)Math.pow(u,1.12);
    }

    static synchronized short[] clip() {
        if(cached!=null)return cached;
        float[] mix=new float[(int)(Sfx.RATE*DURATION)];
        float beat=60f/BPM;
        // Two quick brass calls preserve the memo's contour at double tempo.
        for(int call=0;call<2;call++)for(int n=0;n<(call==0?9:10);n++) {
            float start=(call*4.5f+n*.5f)*beat;
            boolean glide=call==1 && n==9;
            float duration=glide?DURATION-start:beat*.49f;
            float accent=n%3==0?1.16f:1;
            horn(mix,start,duration,MELODY[n],(call==0?.34f:.43f)*accent,glide?52:MELODY[n]);
            horn(mix,start,duration,MELODY[n]-12,.24f*accent,glide?40:MELODY[n]-12);
            // A minor, C major and E major; the final E chord resolves into boss A minor.
            int third=MELODY[n]==69?60:64,fifth=MELODY[n]==69?64:MELODY[n]==72?67:68;
            horn(mix,start,duration,third,.23f*accent,glide?44:third);
            horn(mix,start,duration,fifth,.21f*accent,glide?47:fifth);
        }
        // A small brass section gives the lead a celebratory fanfare attack.
        for(int bar=0;bar<3;bar++)for(int midi:bar<2?new int[]{45,52,57,60}:new int[]{52,59,64,68})
            horn(mix,bar*4*beat,beat*1.5f,midi,.12f,midi);
        for(int step=0;step<32;step++) {
            float start=step*.5f*beat;
            if(step%2==0)drum(mix,start,KICK,1.10f,step);
            int stroke=SAIDI[step%8];
            if(stroke>=0)drum(mix,start,stroke,stroke==DUM?.29f:.22f,step);
        }
        float peak=0;
        for(float value:mix)peak=Math.max(peak,Math.abs(value));
        // Average-level mastering keeps the brass present between the kick transients.
        float low=0,high=32;
        for(int pass=0;pass<16;pass++) {
            float gain=(low+high)*.5f;double energy=0;
            for(float sample:mix) {
                double mastered=MASTER_PEAK*Math.tanh(sample/Math.max(.001f,peak)*gain);
                energy+=mastered*mastered;
            }
            if(Math.sqrt(energy/mix.length)<MASTER_RMS)low=gain;else high=gain;
        }
        float gain=(low+high)*.5f;
        cached=new short[mix.length];
        for(int i=0;i<mix.length;i++) {
            float t=i/(float)Sfx.RATE;
            float fade=Math.min(1,t/.006f)*Math.min(1,(DURATION-t)/.045f);
            cached[i]=(short)(MASTER_PEAK*Math.tanh(mix[i]/Math.max(.001f,peak)*gain)*fade);
        }
        cached[cached.length-1]=0;
        return cached;
    }

    private static void horn(float[] mix,float start,float duration,int midi,float gain,int endMidi) {
        int first=(int)(start*Sfx.RATE),count=Math.min((int)(duration*Sfx.RATE),mix.length-first);
        double phase=0;float smooth=0;
        for(int i=0;i<count;i++) {
            float t=i/(float)Sfx.RATE;
            float pitch=midi==endMidi?midi:glideMidi(midi,endMidi,start+t);
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
