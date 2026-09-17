package com.dddumpling.game;

/** Short performances accumulate crystal charge within a run; misses never take lives. */
final class CaveBand {
    static final int GOAL=40;
    static final float WINDOW=.14f, REPORT=2.4f;
    final int[] glyph=new int[CaveSong.BEATS], result=new int[CaveSong.BEATS];
    int song,hits,misses,charge;
    float position,report,pulse,badPulse;
    boolean active,finished,won,paid;
    void begin(GameCore c) {
        song=Math.max(0,c.stage-21)%CaveSong.COUNT;
        hits=misses=0;position=report=pulse=badPulse=0;
        active=true;finished=won=paid=false;
        for(int i=0;i<glyph.length;i++) {
            glyph[i]=Roster.at(c.playRosterFull(),(CaveSong.RIFF[song][i%8]+i/4)%Roster.count(c.playRosterFull()));
            result[i]=CaveSong.cue(song,i)?0:2;
        }
        if(c.sound!=null)c.sound.bandStart(song,c.bgmChoice==Music.OFF);
    }
    void sample(GameCore c,float elapsed) {
        float audio=c.sound==null?-1:c.sound.bandTime();
        // NaN means preparing: no invisible count-in while PCM is being synthesized.
        if(Float.isNaN(audio))return;
        position=Math.max(position,audio<0?position+elapsed:audio);
    }
    void update(GameCore c,float elapsed) {
        pulse=Math.max(0,pulse-elapsed*4);badPulse=Math.max(0,badPulse-elapsed*4);
        if(finished) { report=Math.max(0,report-elapsed);return; }
        sample(c,elapsed);
        expire(c);
        if(position>=CaveSong.duration(song)) {
            finished=true;report=REPORT;won=charge>=GOAL;
            if(won)charge-=GOAL;
        }
    }
    private void expire(GameCore c) {
        float window=c.kidsRun?WINDOW*1.7f:WINDOW;
        for(int i=0;i<result.length;i++) if(result[i]==0 && position>CaveSong.at(song,i)+window) {
            result[i]=-1;misses++;
        }
    }
    int press(GameCore c,int g,float inputAge) {
        if(!active || finished || c.paused || c.settingsOpen)return -1;
        sample(c,0);expire(c);
        float window=c.kidsRun?WINDOW*1.7f:WINDOW;
        float pressedAt=position-Math.max(0,Math.min(1f,inputAge));
        int nearest=-1;float distance=window+.00001f;
        for(int i=0;i<result.length;i++)if(result[i]==0 || (result[i]==-1 && inputAge>0)) {
            float d=Math.abs(pressedAt-CaveSong.at(song,i));
            if(d<distance){distance=d;nearest=i;}
        }
        if(nearest<0 || glyph[nearest]!=g) {
            if(nearest>=0){if(result[nearest]==0)misses++;result[nearest]=-2;}
            badPulse=1;return -1;
        }
        if(result[nearest]==-1)misses--;
        result[nearest]=1;hits++;charge++;pulse=1;
        return nearest;
    }
    void stop(GameCore c) {
        if(active && c.sound!=null)c.sound.bandStop();
        active=false;
    }
    void reset(GameCore c) { stop(c);charge=0; }
}
