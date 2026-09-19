package com.dddumpling.game;

/** One beat map drives PCM placement, scrolling and judging. Four count-in beats, eight bars. */
final class CaveSong {
    static final int COUNT = 3, BEATS = 32;
    static final String[] NAMES = {"CRYSTAL CRUNCH", "TUNNEL TROUBLE", "BAT OUTTA BEDROCK"};
    static final int[][] TEMPO = {{90,110,130},{110,135,160},{125,150,175}};
    static final int[][] RIFF = {{0,0,7,10,0,3,7,3},{0,7,0,5,3,7,5,10},{0,3,7,12,10,7,5,3}};
    static final int[][] CHORD = {{0,0,5,5,3,3,7,7},{0,5,7,5,0,5,7,0},{0,3,5,7,0,5,3,7}};
    static final int[] ROOT = {40,45,42};
    private static final short[][] CACHE = new short[COUNT][];
    static int section(int beat) { return beat < 12 ? 0 : beat < 24 ? 1 : 2; }
    static int bpm(int song, int beat) { return TEMPO[song][section(beat)]; }
    static float at(int song, float beat) {
        double seconds = 4.0 * 60 / TEMPO[song][0];
        if (beat < 0) return (float)Math.max(0, seconds + beat * 60 / TEMPO[song][0]);
        for (int i = 0; i < (int)beat; i++) seconds += 60.0 / bpm(song,i);
        return (float)(seconds + (beat - (int)beat) * 60 / bpm(song,(int)beat));
    }
    static float duration(int song) { return at(song,BEATS) + .6f; }
    static float beat(int song, float seconds) {
        if (seconds < at(song,0)) return seconds * TEMPO[song][0] / 60f - 4f;
        for (int b=0;b<BEATS;b++) if(seconds<at(song,b+1))
            return b+(seconds-at(song,b))/(at(song,b+1)-at(song,b));
        return BEATS;
    }
    static int pitch(int song,int index) { return ROOT[song]+12+RIFF[song][index%8]+CHORD[song][index/4%8]; }
    static boolean cue(int song,int beat) { return song==0 || beat%8!=7; }
    static synchronized short[] backing(int song) {
        if(CACHE[song]!=null)return CACHE[song];
        float[] mix=new float[(int)(duration(song)*Sfx.RATE)+1];
        for(int b=-4;b<BEATS;b++) {
            float t=at(song,b),spb=60f/bpm(song,Math.max(0,b));
            if(b<0) { drum(mix,t,2,.32f,b+9); continue; }
            int root=ROOT[song]+CHORD[song][b/4];
            drum(mix,t,b%2==0?0:1,.55f,b+song*41);
            for(int h=0;h<2;h++)drum(mix,t+h*spb*.5f,2,.16f,b*2+h);
            // Fills announce both accelerations, then the final crash.
            if(b==11 || b==23 || b==31)
                for(int h=1;h<4;h++)drum(mix,t+h*spb*.25f,1,.27f,h+b);
            tone(mix,t,spb*.85f,root-12,.20f,0);
            int chugs=song==1?2:1;
            for(int k=0;k<chugs;k++) {
                float start=t+spb*k/chugs;
                tone(mix,start,spb*.7f/chugs,root,.11f,1);
                tone(mix,start,spb*.7f/chugs,root+7,.075f,1);
            }
            if(b%2==0 || song==2)tone(mix,t+spb*.5f,spb*.7f,root+24+(b%2)*7,.085f,2);
        }
        drum(mix,at(song,BEATS),1,.5f,99);
        CACHE[song]=pcm(mix);
        return CACHE[song];
    }
    /** Export a perfect performance for listening; gameplay adds this guitar live. */
    static short[] performance(int song) {
        short[] backing=backing(song);float[] mix=new float[backing.length];
        for(int i=0;i<mix.length;i++)mix[i]=backing[i]/32768f;
        for(int b=0;b<BEATS;b++)if(cue(song,b)) {
            short[] guitar=lead(song,b);int start=Math.round(at(song,b)*Sfx.RATE);
            for(int i=0;i<guitar.length && start+i<mix.length;i++)mix[start+i]+=guitar[i]/32768f;
        }
        short[] out=new short[mix.length];
        for(int i=0;i<out.length;i++)out[i]=(short)(Math.tanh(mix[i])*30000);
        return out;
    }

    static short[] lead(int song,int index) {
        float[] mix=new float[(int)(Sfx.RATE*.24f)];
        tone(mix,0,.23f,pitch(song,index),.46f,1);
        return pcm(mix);
    }
    private static void tone(float[] mix,float start,float length,int midi,float gain,int voice) {
        int from=Math.round(start*Sfx.RATE),n=Math.min((int)(length*Sfx.RATE),mix.length-from);
        double f=440*Math.pow(2,(midi-69)/12.0);
        for(int i=0;i<n;i++) {
            double t=i/(double)Sfx.RATE,phase=2*Math.PI*f*t;
            double wave=Math.sin(phase);
            if(voice==0) wave=.65*wave+.24*Math.sin(phase*2)+.11*Math.sin(phase*3);
            if(voice==1) wave=Math.tanh(2.4*(wave+.35*Math.sin(phase*2)+.18*Math.sin(phase*3)))*.7;
            if(voice==2) wave=.65*wave+.23*Math.sin(phase*2)+.12*Math.sin(phase*4);
            double envelope=Math.min(1,t/.004)*Math.exp(-t/(length*.48))*Math.min(1,(length-t)/.025);
            mix[from+i]+=(float)(wave*envelope*gain);
        }
    }
    private static void drum(float[] mix,float start,int kind,float gain,int seed) {
        int from=Math.round(start*Sfx.RATE),n=Math.min((int)(Sfx.RATE*(kind==2?.055f:.19f)),mix.length-from);
        int noise=seed*7237+11;
        for(int i=0;i<n;i++) {
            double t=i/(double)Sfx.RATE;
            noise=noise*1664525+1013904223;
            double hiss=((noise>>>8)/8388608.0-1),wave;
            if(kind==0)wave=Math.sin(2*Math.PI*(48*t+1.4*(1-Math.exp(-t*40))))*Math.exp(-t*25);
            else if(kind==1)wave=(.7*hiss+.3*Math.sin(2*Math.PI*185*t))*Math.exp(-t*24);
            else wave=hiss*Math.exp(-t*100);
            mix[from+i]+=(float)(gain*wave*Math.min(1,t/.0015));
        }
    }
    private static short[] pcm(float[] mix) {
        short[] pcm=new short[mix.length];
        for(int i=0;i<mix.length;i++)pcm[i]=(short)(Math.tanh(mix[i])*26000);
        return pcm;
    }
}
