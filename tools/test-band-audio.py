#!/usr/bin/env python3
"""Run the real Android band adapter against the static AudioTrack lifecycle contract."""
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SOURCES = {
    'android/media/AudioAttributes.java': '''package android.media;
public class AudioAttributes {
    public static final int USAGE_GAME=1, CONTENT_TYPE_MUSIC=2;
    public static class Builder {
        public Builder setUsage(int n){return this;}
        public Builder setContentType(int n){return this;}
        public AudioAttributes build(){return new AudioAttributes();}
    }
}''',
    'android/media/AudioFormat.java': '''package android.media;
public class AudioFormat {
    public static final int ENCODING_PCM_16BIT=2, CHANNEL_OUT_MONO=4;
    public static class Builder {
        public Builder setEncoding(int n){return this;}
        public Builder setSampleRate(int n){return this;}
        public Builder setChannelMask(int n){return this;}
        public AudioFormat build(){return new AudioFormat();}
    }
}''',
    'android/media/AudioManager.java': '''package android.media;
public class AudioManager { public static final int AUDIO_SESSION_ID_GENERATE=0; }''',
    'android/media/AudioTimestamp.java': '''package android.media;
public class AudioTimestamp { public long framePosition,nanoTime; }''',
    'android/media/AudioTrack.java': '''package android.media;
import java.util.ArrayList;
public class AudioTrack {
    public static final int MODE_STATIC=0, STATE_UNINITIALIZED=0, STATE_INITIALIZED=1, STATE_NO_STATIC_DATA=2;
    public static final ArrayList<AudioTrack> tracks=new ArrayList<AudioTrack>();
    public static boolean failWrite;
    public int state=STATE_NO_STATIC_DATA, head, plays;
    public float volume;
    public boolean released,playing;
    public AudioTrack(AudioAttributes a,AudioFormat f,int size,int mode,int session){tracks.add(this);}
    public int getState(){return state;}
    public int write(short[] data,int offset,int count){
        if(failWrite)return -1;
        state=STATE_INITIALIZED;return count;
    }
    public void play(){
        if(state!=STATE_INITIALIZED || released)throw new IllegalStateException();
        playing=true;plays++;
    }
    public void stop(){playing=false;}
    public void pause(){playing=false;}
    public void release(){released=true;playing=false;}
    public int reloadStaticData(){return 0;}
    public int setPlaybackHeadPosition(int n){head=n;return 0;}
    public int setVolume(float gain){volume=gain;return 0;}
    public boolean getTimestamp(AudioTimestamp s){return false;}
    public int getPlaybackHeadPosition(){return head;}
}''',
    'com/dddumpling/game/TestBandAudio.java': '''package com.dddumpling.game;
import android.media.AudioTrack;
public class TestBandAudio {
    private static int checks;
    static void check(String name,boolean ok){if(!ok)throw new AssertionError(name);checks++;}
    static float ready(CaveBandAudio band)throws Exception {
        long end=System.nanoTime()+10000000000L;
        float time;
        while(Float.isNaN(time=band.time()) && System.nanoTime()<end)Thread.sleep(5);
        return time;
    }
    public static void main(String[] args)throws Exception {
        CaveBandAudio band=new CaveBandAudio();
        for(int song=0;song<CaveSong.COUNT;song++) {
            AudioTrack.tracks.clear();band.volume(.6f);band.start(song,false);
            check("static audio loads before readiness check",ready(band)==0);
            check("backing and four guitar voices prepared",AudioTrack.tracks.size()==5);
            AudioTrack backing=AudioTrack.tracks.get(0);
            check("backing starts audibly without key presses",backing.playing && backing.volume==.6f);
            for(int beat:new int[]{0,11,12,23,24,31}) {
                backing.head=Math.round(CaveSong.at(song,beat)*Sfx.RATE);
                check("playback clock follows chart through tempo changes",
                      Math.abs(band.time()-CaveSong.at(song,beat))<1f/Sfx.RATE);
            }
            band.note(12);
            int leadPlays=0;for(int i=1;i<5;i++)leadPlays+=AudioTrack.tracks.get(i).plays;
            check("landed key plays guitar",leadPlays==1);
            band.volume(0);check("mute preserves transport",backing.playing && backing.volume==0);
            band.volume(.6f);float time=band.time();band.pause(true,false);
            backing.head+=100;
            check("pause freezes music and clock",!backing.playing && band.time()==time);
            band.pause(false,false);check("resume restarts backing",backing.playing);
            band.stop();
            boolean released=true;for(AudioTrack t:AudioTrack.tracks)released&=t.released;
            check("exit releases every voice",released && !band.active());
        }
        AudioTrack.tracks.clear();AudioTrack.failWrite=true;band.start(0,false);
        check("failed PCM write uses fallback clock",ready(band)<0);
        check("failed track is released",AudioTrack.tracks.get(0).released);
        band.stop();AudioTrack.failWrite=false;
        band.pause(true,true);band.start(0,false);
        check("loading while app paused succeeds",ready(band)==0);
        AudioTrack backing=AudioTrack.tracks.get(AudioTrack.tracks.size()-5);
        check("app pause prevents playback",!backing.playing);
        band.pause(false,true);check("app resume starts playback",backing.playing);
        band.stop();
        System.out.println(checks+" Android band audio checks passed");
    }
}''',
}

with tempfile.TemporaryDirectory(prefix='band-audio-') as tmp:
    folder = Path(tmp)
    for name, source in SOURCES.items():
        path = folder / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(source)
    classes = folder / 'classes'
    # The pure harness provides the real score/synth and its dependencies.
    harness = ROOT / 'build/harness'
    subprocess.run(['javac', '-nowarn', '-cp', str(harness), '-d', str(classes),
                    *map(str, folder.rglob('*.java')),
                    str(ROOT / 'src/com/dddumpling/game/CaveBandAudio.java')], check=True)
    subprocess.run(['java', '-cp', str(classes) + ':' + str(harness),
                    'com.dddumpling.game.TestBandAudio'], check=True)
