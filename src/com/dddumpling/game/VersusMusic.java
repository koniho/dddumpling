package com.dddumpling.game;

/** Soft synth fanfare with melody and beatbox rhythm from the user's Oct 16 voice memos. */
final class VersusMusic {
    static final float SOURCE_BPM=206f, BPM=SOURCE_BPM+20f;
    static final float TEMPO_SCALE=SOURCE_BPM/BPM, DURATION=4.3f*TEMPO_SCALE;
    static final float ANNOUNCER_GAIN=.65f;
    private static final float MASTER_PEAK=19000f,MASTER_RMS=4600f;
    // Measured beatbox attacks relative to its first thump; scale every gap equally to retain the performed groove.
    static final float[] KICKS=atTempo(0,.605f,1.200f,1.765f,2.335f,2.940f,3.495f,3.770f,4.080f);
    static final float[] SNAPS=atTempo(.435f,.875f,1.600f,2.035f,2.775f,3.215f);
    static final float GLIDE_START=KICKS[6];
    private static final int[][] MELODY={{71,71,69,71},{72,72,69,71},{71,71,69,71}};
    private static final int KICK=0,SNAP=1;
    private static short[] cached;
    private VersusMusic() {}

    private static float[] atTempo(float... times) {
        for(int i=0;i<times.length;i++)times[i]*=TEMPO_SCALE;
        return times;
    }

    static float noteDuration(float gap) {return gap*.58f;}

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
        // Each synth phrase follows the four actual attacks: thump, snap, thump, snap.
        for(int phrase=0;phrase<3;phrase++) {
            float[] starts={KICKS[phrase*2],SNAPS[phrase*2],KICKS[phrase*2+1],SNAPS[phrase*2+1]};
            for(int n=0;n<4;n++) {
                float next=n<3?starts[n+1]:KICKS[phrase*2+2];
                chord(mix,starts[n],noteDuration(next-starts[n]),MELODY[phrase][n],n%2==0?1.08f:1,false);
            }
            // Low chords stay within their phrase, leaving the final kicks to the gliss.
            for(int midi:phrase<2?new int[]{45,52,57,60}:new int[]{52,59,64,68})
                synth(mix,starts[0],noteDuration(starts[1]-starts[0]),midi,.05f,midi);
        }
        chord(mix,GLIDE_START,DURATION-GLIDE_START,71,1,true);
        for(int i=0;i<KICKS.length;i++)drum(mix,KICKS[i],KICK,1.65f,i);
        for(int i=0;i<SNAPS.length;i++)drum(mix,SNAPS[i],SNAP,.48f,32+i);
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

    private static void chord(float[] mix,float start,float duration,int midi,float accent,boolean glide) {
        airyVoice(mix,start,duration,midi,.25f*accent,glide?52:midi);
        synth(mix,start,duration,midi-12,.11f*accent,glide?40:midi-12);
        // A minor, C major and E major; the final E chord resolves into boss A minor.
        int third=midi==69?60:64,fifth=midi==69?64:midi==72?67:68;
        synth(mix,start,duration,third,.10f*accent,glide?44:third);
        synth(mix,start,duration,fifth,.09f*accent,glide?47:fifth);
        // Airy chord tones above the lead, following the same attacks and final descent.
        synth(mix,start,duration,third+12,.075f*accent,glide?56:third+12);
        synth(mix,start,duration,fifth+12,.055f*accent,glide?59:fifth+12);
    }

    /** Original ARR1-style breathy ah voice; no sampled recording. */
    private static void airyVoice(float[] mix,float start,float duration,int midi,float gain,int endMidi) {
        int first=(int)(start*Sfx.RATE),count=Math.min((int)(duration*Sfx.RATE),mix.length-first);
        float[] weights=new float[12];float energy=0;
        // Fixed spectral shape follows pitch like a transposed vocal sample.
        for(int h=1;h<=weights.length;h++) {
            float hz=h*493.88f;
            float a=(hz-850)/300,b=(hz-1350)/380,c=(hz-2700)/600;
            float weight=.12f/h+.85f*(float)Math.exp(-a*a)
                    +.65f*(float)Math.exp(-b*b)+.22f*(float)Math.exp(-c*c);
            weights[h-1]=weight;energy+=weight*weight;
        }
        float normal=(float)Math.sqrt(energy),airLow=0,airHigh=0;
        int noise=137+first*31+midi*7919;double phase=0;
        for(int i=0;i<count;i++) {
            float t=i/(float)Sfx.RATE;
            float pitch=midi==endMidi?midi:glideMidi(midi,endMidi,start+t);
            pitch+=.06f*(float)Math.sin(t*Softbody.TAU*4.8f);
            phase+=Softbody.TAU*440*Math.pow(2,(pitch-69)/12f)/Sfx.RATE;
            float voice=0;
            for(int h=1;h<=weights.length;h++)voice+=(float)Math.sin(phase*h)*weights[h-1];
            voice/=normal;
            // A little sampler grain and filtered breath keep the vowel airy, not buzzy.
            voice=voice*.6f+Math.round(voice*128)/128f*.4f;
            noise=noise*1664525+1013904223;
            float white=(noise>>>8)/8388608f-1;
            airLow+=(white-airLow)*.42f;airHigh+=(white-airHigh)*.10f;
            float breath=(airLow-airHigh)*(.36f+.28f*(float)Math.exp(-t*35));
            float attack=Math.min(1,t/.020f),release=Math.min(1,(duration-t)/.025f);
            float swell=.82f+.18f*(float)Math.exp(-t*8);
            mix[first+i]+=(voice+breath)*gain*attack*release*swell;
        }
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
        float length=(kind==KICK?.25f:.10f)*TEMPO_SCALE;
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
