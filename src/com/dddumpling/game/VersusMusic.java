package com.dddumpling.game;

/** Soft synth fanfare with melody and beatbox rhythm from the user's Oct 16 voice memos. */
final class VersusMusic {
    static final float BPM=224f, DURATION=4.3f;
    static final float ANNOUNCER_GAIN=.65f;
    private static final float MASTER_PEAK=19000f,MASTER_RMS=4600f;
    static final float GLIDE_START=9f*60f/BPM;
    // Eighth-note repeated-note contour, excluding the unpitched count-in and handling noise.
    private static final int[] MELODY={71,71,69,72,72,69,71,71,69,71};
    private static final int KICK=0,SNAP=1;
    // Beatbox: accented thumps on 1/3, dry snaps on 2-and/4. Softer kicks fill 2/4.
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
        // Two quick synth calls preserve the memo's contour at double tempo.
        for(int call=0;call<2;call++)for(int n=0;n<(call==0?9:10);n++) {
            float start=(call*4.5f+n*.5f)*beat;
            boolean glide=call==1 && n==9;
            float duration=glide?DURATION-start:beat*.49f;
            float accent=n%3==0?1.16f:1;
            synth(mix,start,duration,MELODY[n],(call==0?.22f:.25f)*accent,glide?52:MELODY[n]);
            synth(mix,start,duration,MELODY[n]-12,.11f*accent,glide?40:MELODY[n]-12);
            // A minor, C major and E major; the final E chord resolves into boss A minor.
            int third=MELODY[n]==69?60:64,fifth=MELODY[n]==69?64:MELODY[n]==72?67:68;
            synth(mix,start,duration,third,.10f*accent,glide?44:third);
            synth(mix,start,duration,fifth,.09f*accent,glide?47:fifth);
        }
        // Warm lower chords keep the fanfare full without the trumpet edge.
        for(int bar=0;bar<3;bar++)for(int midi:bar<2?new int[]{45,52,57,60}:new int[]{52,59,64,68})
            synth(mix,bar*4*beat,beat*1.5f,midi,.05f,midi);
        for(int step=0;step<32;step++) {
            float start=step*.5f*beat;
            if(step%2==0)drum(mix,start,KICK,step%4==0?1.65f:1.15f,step);
            if(step%8==3 || step%8==6)drum(mix,start,SNAP,.48f,step);
        }
        float peak=0;
        for(float value:mix)peak=Math.max(peak,Math.abs(value));
        // Lower average level leaves room for the kick transients.
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

    private static void synth(float[] mix,float start,float duration,int midi,float gain,int endMidi) {
        int first=(int)(start*Sfx.RATE),count=Math.min((int)(duration*Sfx.RATE),mix.length-first);
        double phase=0;
        for(int i=0;i<count;i++) {
            float t=i/(float)Sfx.RATE;
            float pitch=midi==endMidi?midi:glideMidi(midi,endMidi,start+t);
            float hz=440f*(float)Math.pow(2,(pitch-69)/12f);
            phase+=Math.PI*2*hz/Sfx.RATE;
            // Rounded, lightly doubled sine voice: a soft octave and only a trace of upper partials.
            float tone=(float)(Math.sin(phase)+.12*Math.sin(phase*.997)
                    +.19*Math.sin(phase*2)+.065*Math.sin(phase*3)+.02*Math.sin(phase*4));
            float attack=Math.min(1,t/.018f),release=Math.min(1,(duration-t)/.040f);
            float swell=.78f+.22f*(float)Math.exp(-t*7);
            mix[first+i]+=tone*gain*attack*release*swell;
        }
    }

    /** Heavy rounded kick and short beatbox-like snare snaps. No cymbals or hi-hats. */
    private static void drum(float[] mix,float start,int kind,float gain,int seed) {
        int first=(int)(start*Sfx.RATE);
        float length=kind==KICK?.25f:.10f;
        int count=Math.min((int)(length*Sfx.RATE),mix.length-first),noise=137+seed*7919;
        float phase=0,sub=0,previous=0,filtered=0;
        for(int i=0;i<count;i++) {
            float t=i/(float)Sfx.RATE,u=t/length;
            noise=noise*1664525+1013904223;
            float white=(noise>>>8)/8388608f-1;
            float high=white-previous;previous=white;
            float hz=kind==KICK?58+130*(float)Math.exp(-t*42):185+100*(float)Math.exp(-t*60);
            phase+=Softbody.TAU*hz/Sfx.RATE;
            sub+=Softbody.TAU*58/Sfx.RATE;
            float voice;
            filtered+=(white-filtered)*.36f;
            if(kind==KICK) voice=(float)Math.sin(phase)*.90f+(float)Math.sin(sub)*.45f
                    +(float)Math.sin(phase*2)*.22f+high*.08f*(float)Math.exp(-t*95);
            else voice=(float)Math.sin(phase)*.48f+(float)Math.sin(phase*1.59f)*.18f
                    +filtered*.90f;
            mix[first+i]+=voice*gain*Math.min(1,t/.002f)*(float)Math.exp(-u*(kind==KICK?4.5f:6f));
        }
    }
}
