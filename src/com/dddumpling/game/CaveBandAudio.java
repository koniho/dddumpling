package com.dddumpling.game;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTimestamp;
import android.media.AudioTrack;

/** Finite backing track is the rhythm clock, including when its volume is zero. */
final class CaveBandAudio {
    private AudioTrack backing;
    private final AudioTrack[] leads=new AudioTrack[4];
    private short[][] notes;
    private final AudioTimestamp stamp=new AudioTimestamp();
    private int generation,voice;
    private long resumedAt;
    private boolean active,loading,failed,localPause,appPause,muted;
    private float volume=1f,position,duration;
    synchronized void start(final int song,boolean off) {
        stop();active=loading=true;failed=false;muted=off;position=0;localPause=false;
        duration=CaveSong.duration(song);final int token=++generation;
        new Thread(new Runnable(){public void run(){
            AudioTrack track=null;AudioTrack[] prepared=new AudioTrack[4];
            try {
                short[] pcm=CaveSong.backing(song);
                short[][] rendered=new short[CaveSong.BEATS][];
                for(int i=0;i<rendered.length;i++)rendered[i]=CaveSong.lead(song,i);
                track=track(pcm);
                for(int i=0;i<prepared.length;i++)prepared[i]=track(rendered[0]);
                synchronized(CaveBandAudio.this) {
                    if(token!=generation){release(track);for(AudioTrack t:prepared)release(t);return;}
                    backing=track;notes=rendered;
                    for(int i=0;i<leads.length;i++)leads[i]=prepared[i];
                    loading=false;mix();
                    if(!localPause && !appPause)backing.play();
                }
            } catch(Throwable ignored) {
                release(track);for(AudioTrack t:prepared)release(t);
                synchronized(CaveBandAudio.this){if(token==generation){loading=false;failed=true;backing=null;}}
            }
        }},"cave-band-audio").start();
    }
    private static AudioTrack track(short[] pcm) {
        AudioTrack t=new AudioTrack(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(),
                new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(Sfx.RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
                pcm.length*2,AudioTrack.MODE_STATIC,AudioManager.AUDIO_SESSION_ID_GENERATE);
        if(t.getState()!=AudioTrack.STATE_INITIALIZED){t.release();throw new IllegalStateException("audio unavailable");}
        t.write(pcm,0,pcm.length);return t;
    }
    synchronized float time() {
        if(!active || failed)return -1;
        if(loading || backing==null)return Float.NaN;
        if(localPause || appPause)return position;
        try {
            float now;
            if(backing.getTimestamp(stamp) && stamp.nanoTime>=resumedAt)
                now=(float)(stamp.framePosition/(double)Sfx.RATE+(System.nanoTime()-stamp.nanoTime)*1e-9);
            else now=(backing.getPlaybackHeadPosition()&0xffffffffL)/(float)Sfx.RATE;
            position=Math.max(position,Math.min(duration,now));
        } catch(Throwable ignored){failed=true;return -1;}
        return position;
    }
    synchronized void note(int index) {
        if(loading || failed || !active || localPause || appPause || muted || notes==null)return;
        try {
            AudioTrack t=leads[voice++%leads.length];short[] pcm=notes[index];
            t.stop();t.reloadStaticData();t.write(pcm,0,pcm.length);t.setPlaybackHeadPosition(0);
            t.setVolume(volume);t.play();
        }catch(Throwable ignored){}
    }
    synchronized void pause(boolean value,boolean app) {
        time();if(app)appPause=value;else localPause=value;
        if(backing==null)return;
        try {
            if(appPause || localPause) {backing.pause();for(AudioTrack t:leads)if(t!=null)t.stop();}
            else if(position<duration){resumedAt=System.nanoTime();backing.play();}
        }catch(Throwable ignored){}
    }
    synchronized void muted(boolean value) {muted=value;mix();}
    synchronized void volume(float gain) {volume=gain;mix();}
    private void mix() {
        try {
            if(backing!=null)backing.setVolume(muted?0:volume);
            for(AudioTrack t:leads)if(t!=null)t.setVolume(muted?0:volume);
        }catch(Throwable ignored){}
    }
    synchronized boolean active(){return active;}
    synchronized void stop() {
        generation++;active=loading=false;release(backing);backing=null;notes=null;
        for(int i=0;i<leads.length;i++){release(leads[i]);leads[i]=null;}
    }
    private static void release(AudioTrack t){if(t!=null)try{t.release();}catch(Throwable ignored){}}
}
