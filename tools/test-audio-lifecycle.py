#!/usr/bin/env python3
"""Exercise the real Android adapter with bounded native tracks and injected failures."""
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SOURCES = {
'android/content/Context.java': 'package android.content; public class Context {}',
'android/os/Bundle.java': 'package android.os; public class Bundle {public float volume; public void putFloat(String k,float v){volume=v;}}',
'android/util/Log.java': 'package android.util; public class Log {public static int w(String t,String m,Throwable e){return 0;}}',
'android/media/AudioAttributes.java': '''package android.media; public class AudioAttributes {
 public static final int USAGE_GAME=1,CONTENT_TYPE_MUSIC=2,CONTENT_TYPE_SONIFICATION=4;
 public static class Builder {public Builder setUsage(int n){return this;} public Builder setContentType(int n){return this;} public AudioAttributes build(){return new AudioAttributes();}}
}''',
'android/media/AudioFormat.java': '''package android.media; public class AudioFormat {
 public static final int ENCODING_PCM_16BIT=2,CHANNEL_OUT_MONO=4;
 public static class Builder {public Builder setEncoding(int n){return this;} public Builder setSampleRate(int n){return this;} public Builder setChannelMask(int n){return this;} public AudioFormat build(){return new AudioFormat();}}
}''',
'android/media/AudioManager.java': 'package android.media; public class AudioManager {public static final int AUDIO_SESSION_ID_GENERATE=0;}',
'android/media/AudioTimestamp.java': 'package android.media; public class AudioTimestamp {public long framePosition,nanoTime;}',
'android/media/AudioTrack.java': '''package android.media;
import java.util.*;
public class AudioTrack {
 public static final int MODE_STATIC=0,STATE_UNINITIALIZED=0,STATE_INITIALIZED=1,STATE_NO_STATIC_DATA=2,SUCCESS=0,PLAYSTATE_PLAYING=3;
 public static final List<AudioTrack> all=new ArrayList<>();
 public static int live,peak,failInit,failWrite,shortWrite,failPlay,failReload,throwCreate;
 public int state=STATE_NO_STATIC_DATA,head,frames,plays; public float volume; public boolean released,playing,failStop,dead;
 public AudioTrack(AudioAttributes a,AudioFormat f,int bytes,int mode,int session){
  synchronized(AudioTrack.class){
   if(throwCreate>0){throwCreate--;throw new IllegalStateException("allocation");}
   all.add(this); if(failInit>0){failInit--;state=0;} else if(live>=40)state=0;
   else {live++;peak=Math.max(peak,live);} frames=bytes/2;
  }
 }
 public int getState(){return released?0:state;}
 public int write(short[] pcm,int off,int count){if(state==0)return -3;if(failWrite>0){failWrite--;return -6;}if(shortWrite>0){shortWrite--;state=1;return count-1;}state=1;return count;}
 public int setLoopPoints(int a,int b,int n){return state==1?0:-3;}
 public void play(){if(failPlay>0){failPlay--;throw new IllegalStateException("dead server");}if(getState()!=1||dead)throw new IllegalStateException("uninitialized");playing=true;plays++;}
 public void stop(){if(failStop||getState()!=1)throw new IllegalStateException("stop");playing=false;}
 public void pause(){if(dead)throw new IllegalStateException("pause");playing=false;}
 public void release(){synchronized(AudioTrack.class){if(!released && state!=0)live--;released=true;playing=false;}}
 public int reloadStaticData(){head=0;if(failReload>0){failReload--;return -6;}return dead?-6:0;}
 public int setPlaybackRate(int n){return dead?-6:0;}
 public int setVolume(float f){volume=f;return 0;}
 public int getPlayState(){return playing?3:1;}
 public int getPlaybackHeadPosition(){return head;}
 public int setPlaybackHeadPosition(int n){head=n;return 0;}
 public boolean getTimestamp(AudioTimestamp s){return false;}
 public static void reset(){if(live!=0)throw new AssertionError("leaked tracks: "+live);all.clear();peak=0;failInit=failWrite=shortWrite=failPlay=failReload=throwCreate=0;}
}''',
'android/speech/tts/Voice.java': '''package android.speech.tts; public class Voice {
 public java.util.Locale getLocale(){return java.util.Locale.US;} public boolean isNetworkConnectionRequired(){return false;} public int getQuality(){return 0;} public String getName(){return "test";}
}''',
'android/speech/tts/UtteranceProgressListener.java': '''package android.speech.tts; public abstract class UtteranceProgressListener {
 public abstract void onStart(String id); public abstract void onDone(String id); public abstract void onError(String id); public void onStop(String id,boolean b){} public void onRangeStart(String id,int start,int end,int frame){}
}''',
'android/speech/tts/TextToSpeech.java': '''package android.speech.tts; public class TextToSpeech {
 public static final int SUCCESS=0,LANG_MISSING_DATA=-1,LANG_NOT_SUPPORTED=-2,QUEUE_FLUSH=0,QUEUE_ADD=1;
 public interface OnInitListener {void onInit(int status);} public static class Engine {public static final String KEY_PARAM_VOLUME="volume";}
 public float volume; public int calls; public String text; public TextToSpeech(){}
 public TextToSpeech(android.content.Context c,OnInitListener l){throw new UnsupportedOperationException("no speech in harness");}
 public int setLanguage(java.util.Locale l){return 0;} public java.util.Set<Voice> getVoices(){return null;} public int setVoice(Voice v){return 0;}
 public int setOnUtteranceProgressListener(UtteranceProgressListener l){return 0;} public int stop(){return 0;} public void shutdown(){}
 public int setPitch(float f){return 0;} public int setSpeechRate(float f){return 0;}
 public int speak(String s,int q,android.os.Bundle b,String id){calls++;text=s;volume=b.volume;return 0;} public int playSilentUtterance(long n,int q,String id){return 0;}
}''',
'com/dddumpling/game/Music.java': '''package com.dddumpling.game;
import java.util.concurrent.CountDownLatch;
final class Music {
 static final int SWING_STYLE=0; static final float BOSS_GAIN=.7f;
 static volatile CountDownLatch entered,gate;
 static boolean isSynth(int s){return true;} static void preRender(int s){}
 static short[] loop(int s,boolean f){CountDownLatch wait=gate;if(wait!=null){gate=null;entered.countDown();try{wait.await();}catch(InterruptedException e){throw new RuntimeException(e);}}return new short[100];}
 static short[] bossLoop(int s){return loop(s,false);}
}''',
'com/dddumpling/game/VersusMusic.java': '''package com.dddumpling.game; final class VersusMusic {static final float ANNOUNCER_GAIN=.5f;static short[] clip(){return new short[100];}}''',
'com/dddumpling/game/TestAudioLifecycle.java': '''package com.dddumpling.game;
import android.media.AudioTrack;
import java.lang.reflect.*;
import java.util.concurrent.*;
public class TestAudioLifecycle {
 static int checks; static Method play; static Field tracks,music;
 static void check(String name,boolean ok){if(!ok)throw new AssertionError(name);checks++;}
 static Field field(String s)throws Exception{Field f=Audio.class.getDeclaredField(s);f.setAccessible(true);return f;}
 static void effect(Audio a,int id)throws Exception{play.invoke(a,id,1f);}
 static AudioTrack effectTrack(Audio a,int id)throws Exception{return ((AudioTrack[])tracks.get(a))[id];}
 static AudioTrack bgm(Audio a)throws Exception{return (AudioTrack)music.get(a);}
 static void ready(Audio a)throws Exception {
  long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
  while(System.nanoTime()<end){synchronized(a){if(!field("musicLoading").getBoolean(a))return;}Thread.sleep(2);}
  throw new AssertionError("music load timed out");
 }
 static void workersDone()throws Exception{for(Thread t:Thread.getAllStackTraces().keySet())if(t.getName().equals("hexatype-bgm")){t.join(5000);check("music worker finished",!t.isAlive());}}
 static Audio fresh(){AudioTrack.reset();return new Audio(null);}
 public static void main(String[] args)throws Exception {
  play=Audio.class.getDeclaredMethod("play",int.class,float.class);play.setAccessible(true);tracks=field("tracks");music=field("bgmTrack");
  Audio a=fresh();a.volumes(.4f,.3f);
  for(int pass=0;pass<3;pass++)for(int i=0;i<Sfx.COUNT;i++){
   effect(a,i);check("every effect plays across repeated full catalog",effectTrack(a,i)!=null && effectTrack(a,i).plays>0);
   check("native effect retention bounded",AudioTrack.live<=24);
   effectTrack(a,i).head=effectTrack(a,i).frames;
  }
  a.selectMusic(0);ready(a);a.rocket(1);a.bossCharge(1);
  check("music and loops retain headroom",AudioTrack.live<=27 && bgm(a).playing);
  check("music volume and rocket duck retained",Math.abs(bgm(a).volume-.4f*.68f)<.0001f);
  a.release();check("all players released",AudioTrack.live==0);
  effect(a,0);a.selectMusic(1);a.rocket(1);a.bossCharge(1);a.resumeMusic();
  check("disposed adapter cannot allocate",AudioTrack.live==0);

  for(String fault:new String[]{"failInit","failWrite","shortWrite","failPlay","failReload","throwCreate"}){
   a=fresh();AudioTrack.class.getField(fault).setInt(null,1);effect(a,0);
   check("transient effect failure recovers: "+fault,effectTrack(a,0)!=null && effectTrack(a,0).plays==1);
   check("failed effect player released: "+fault,AudioTrack.live==1);a.release();
  }
  a=fresh();AudioTrack.failInit=2;effect(a,0);check("persistent failure not cached",effectTrack(a,0)==null && AudioTrack.live==0);
  effect(a,0);check("later sound retries after persistent failure",effectTrack(a,0).plays==1);
  effectTrack(a,0).dead=true;effect(a,0);check("dead cached effect recreated",effectTrack(a,0).plays==1 && !effectTrack(a,0).dead);a.release();

  a=fresh();for(int i=0;i<24;i++)effect(a,i);
  AudioTrack active=effectTrack(a,0),idle=effectTrack(a,1);idle.head=idle.frames;
  effect(a,24);check("finished voice evicted before older active voice",idle.released && !active.released);a.release();

  for(int mode=0;mode<3;mode++){
   a=fresh();AudioTrack.failInit=2;
   if(mode==0)a.selectMusic(0);else if(mode==1)a.bossMusic(true);else a.versusMusic();ready(a);
   check("failed music not retained",bgm(a)==null && AudioTrack.live==0);
   if(mode==0)a.selectMusic(0);else if(mode==1)a.bossMusic(true);else a.versusMusic();ready(a);
   check("same music selection can retry",bgm(a)!=null && bgm(a).playing);
   a.release();
  }
  for(String fault:new String[]{"failWrite","shortWrite","throwCreate"}){
   a=fresh();AudioTrack.class.getField(fault).setInt(null,1);a.selectMusic(0);ready(a);
   check("music allocation/write recovers: "+fault,bgm(a)!=null && bgm(a).playing && AudioTrack.live==1);a.release();
  }
  a=fresh();AudioTrack.failInit=2;a.bossMusic(true);ready(a);a.startMusic();ready(a);
  check("startup retry preserves boss arrangement",field("musicMode").getInt(a)==1 && bgm(a).playing);a.release();
  a=fresh();AudioTrack.failInit=1;a.rocket(1);a.rocket(1);
  check("rocket retries failed initialization",field("rocketActive").getBoolean(a) && AudioTrack.live==1);
  AudioTrack.failInit=1;a.bossCharge(1);a.bossCharge(1);
  check("bubble retries failed initialization",field("bubbleActive").getBoolean(a) && AudioTrack.live==2);
  a.pauseMusic();for(AudioTrack t:AudioTrack.all)check("loops pause",!t.playing);
  a.resumeMusic();a.bossCharge(1);check("loops resume",field("bubbleActive").getBoolean(a));
  AudioTrack bubble=(AudioTrack)field("bubbleTrack").get(a);
  int chargePlays=bubble.plays;
  a.bossCharge(1);a.bossCharge(1);
  check("continued charge does not replay its finite sample",bubble.plays==chargePlays);
  a.bossCharge(.08f);
  check("new prompt rewinds one charge voice",bubble.plays==chargePlays+1 && bubble.head==0);
  a.bossCharge(0);
  check("charge cancellation releases the loop immediately",bubble.released && !bubble.playing
   && field("bubbleTrack").get(a)==null && !field("bubbleActive").getBoolean(a)
   && field("bubbleVolume").getFloat(a)==0);
  a.bossCharge(0);a.resumeMusic();
  check("cancelled charge cannot restart on resume",field("bubbleTrack").get(a)==null);
  a.rocket(1);a.bossCharge(.5f);a.pauseMusic();a.resumeMusic();
  check("background activation cannot revive combat sounds",field("bubbleTrack").get(a)==null
   && !field("rocketActive").getBoolean(a));
  a.release();
  a=fresh();a.pauseMusic();effect(a,0);check("UI effects still play while music is paused",effectTrack(a,0).playing);a.release();
  a=fresh();a.volumes(.4f,.3f);a.selectMusic(0);ready(a);
  android.speech.tts.TextToSpeech speech=new android.speech.tts.TextToSpeech();
  field("tts").set(a,speech);field("ttsBroken").setBoolean(a,false);field("ttsReady").setBoolean(a,true);
  a.explain("Ordinary tutorial");check("tutorial still ducks",field("ducked").getBoolean(a));
  a.companionExclaim(Narration.DOUBLE_RAINBOW);
  check("companion voice half volume",speech.calls==2 && Math.abs(speech.volume-.15f)<.0001f);
  check("companion voice leaves music unducked",!field("ducked").getBoolean(a) && Math.abs(bgm(a).volume-.4f)<.0001f);
  for(int note=0;note<12;note++) {
    a.catFlurry(note,12);AudioTrack meow=effectTrack(a,Sfx.catFlurryId(note,12));
    check("flurry meow plays each pitched sample "+note,meow.plays==1);
    check("flurry uses effects gain "+note,Math.abs(meow.volume-.3f*Sfx.CAT_FLURRY_GAIN)<.0001f);
  }
  a.catLaser();check("companion voice leaves effects unducked",Math.abs(effectTrack(a,Sfx.CAT_LASER).volume-.3f*Sfx.CAT_LASER_GAIN)<.0001f);
  a.volumes(.4f,0);a.companionExclaim(Narration.DOUBLE_RAINBOW);check("muted companion stays silent",speech.calls==2);
  a.volumes(.4f,.3f);a.explain("Ordinary tutorial");
  check("ordinary speech restores normal gain and ducking",Math.abs(speech.volume-.3f)<.0001f && field("ducked").getBoolean(a));a.release();
  a=fresh();a.volumes(.4f,.3f);for(int i=0;i<12;i++)a.catLaser();
  check("every prompt laser plays without meow debounce",effectTrack(a,Sfx.CAT_LASER).plays==12);
  check("soft laser uses effects volume",Math.abs(effectTrack(a,Sfx.CAT_LASER).volume-.3f*Sfx.CAT_LASER_GAIN)<.0001f);
  a.release();
  a=fresh();a.volumes(.4f,.3f);
  for(int slot=0;slot<Accessories.SLOTS;slot++)for(boolean off:new boolean[]{false,true}) {
    a.accessoryChange(slot,off);AudioTrack outfit=effectTrack(a,Sfx.accessoryId(slot,off));
    check("wardrobe cue routes at effects gain",outfit.plays==1 && Math.abs(outfit.volume-.3f*Sfx.ACCESSORY_GAIN)<.0001f);
    a.accessoryChange(slot,off);check("wardrobe cue has no cat debounce",outfit.plays==2);
  }
  a.volumes(.4f,0);a.accessoryChange(Accessories.FLAIR,false);
  check("muted flair and cheer stay silent",effectTrack(a,Sfx.accessoryId(Accessories.FLAIR,false)).volume==0);a.release();
  a=fresh();a.volumes(0,0);effect(a,0);a.selectMusic(0);ready(a);
  check("mute respected by recreated effects and music",effectTrack(a,0).volume==0 && bgm(a).volume==0);a.release();
  a=fresh();AudioTrack.failPlay=1;a.selectMusic(0);ready(a);check("music play failure retries",bgm(a).playing && AudioTrack.live==1);
  AudioTrack old=bgm(a);old.failStop=true;a.selectMusic(1);ready(a);
  check("stop failure cannot skip release",old.released && AudioTrack.live==1);
  bgm(a).dead=true;a.resumeMusic();ready(a);check("dead music rebuilt on resume",bgm(a).playing && !bgm(a).dead);a.release();

  a=fresh();Music.entered=new CountDownLatch(1);CountDownLatch gate=new CountDownLatch(1);Music.gate=gate;
  a.selectMusic(0);check("async preparation started",Music.entered.await(5,TimeUnit.SECONDS));a.pauseMusic();gate.countDown();ready(a);
  check("prepared music respects pause",bgm(a)!=null && !bgm(a).playing);a.resumeMusic();check("prepared music resumes",bgm(a).playing);a.release();

  a=fresh();Music.entered=new CountDownLatch(1);gate=new CountDownLatch(1);Music.gate=gate;
  a.selectMusic(0);check("old preparation started",Music.entered.await(5,TimeUnit.SECONDS));a.bossMusic(true);ready(a);AudioTrack current=bgm(a);gate.countDown();
  workersDone();check("stale preparation cannot replace music or allocate",bgm(a)==current && AudioTrack.live==1);a.release();

  a=fresh();Music.entered=new CountDownLatch(1);gate=new CountDownLatch(1);Music.gate=gate;
  a.selectMusic(0);check("pending disposal preparation started",Music.entered.await(5,TimeUnit.SECONDS));a.release();gate.countDown();workersDone();
  check("late preparation after release allocates nothing",AudioTrack.live==0);
  AudioTrack.reset();System.out.println(checks+" Android audio lifecycle checks passed");
 }
}''',
}

with tempfile.TemporaryDirectory(prefix='audio-lifecycle-') as tmp:
    folder = Path(tmp)
    for name, source in SOURCES.items():
        path = folder / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(source)
    classes = folder / 'classes'
    harness = ROOT / 'build/harness'
    subprocess.run(['javac', '-nowarn', '-cp', str(harness), '-d', str(classes),
                    *map(str, folder.rglob('*.java')),
                    str(ROOT / 'src/com/dddumpling/game/Audio.java'),
                    str(ROOT / 'src/com/dddumpling/game/CaveBandAudio.java')], check=True)
    subprocess.run(['java', '-cp', str(classes) + ':' + str(harness),
                    'com.dddumpling.game.TestAudioLifecycle'], check=True, timeout=90)
