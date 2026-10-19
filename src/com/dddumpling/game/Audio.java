package com.dddumpling.game;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * Plays the {@link Sfx} buffers through {@link AudioTrack}.
 *
 * A bounded cache of static-mode tracks, built lazily and replayed with
 * stop/reload/play. Stack depth is handled by nudging the playback rate rather than
 * synthesising a variant per depth, which keeps the track count down.
 *
 * Every call is guarded: audio is a garnish, and must never be able to take the game down.
 */
final class Audio implements GameCore.Sound {

    private final AudioTrack[] tracks = new AudioTrack[Sfx.COUNT];
    // Leave headroom below Android's per-UID track limit for music, loops and band voices.
    private static final int MAX_EFFECT_TRACKS = 24;
    private final long[] effectUse = new long[Sfx.COUNT];
    private long useSerial, lastFailureLog;
    private boolean released, musicLoading;

    private final CaveBandAudio band = new CaveBandAudio();
    private final Context ctx;
    private AudioTrack bgmTrack;
    private boolean musicPaused;
    private volatile float musicVolume=1f, effectsVolume=1f;
    private final float[] effectGains=new float[Sfx.COUNT];
    private float rocketGain;
    private boolean rocketDucked;
    private AudioTrack rocketTrack;
    private AudioTrack bubbleTrack;
    private boolean rocketActive;
    private boolean bubbleActive;
    private float bubbleVolume;
    private boolean bgmStarted;
    private boolean frenzyPlaying;
    private boolean bossPlaying;
    private long boltPopUntil;
    /** -1 none, 0 selected track, 1 boss arrangement, 2 versus fanfare. Prevents same-track restarts. */
    private int musicMode = -1, playingStyle = -1;
    private volatile int musicGeneration;

    Audio(Context ctx) {
        this.ctx = ctx;
        prepareSpeech();
        new Thread(new Runnable() {
             public void run() {
                for (int id = 0; id < Sfx.COUNT; id++) Sfx.build(id);
                Sfx.rocket();
                Sfx.bubble();
                VersusMusic.clip();
                Music.preRender(Music.SWING_STYLE);
            }
        }, "hexatype-audio-prerender").start();
    }

    private int choice = -1;

    /** Starts the loop for the current choice if it is not already playing, off the calling thread. */
    synchronized void startMusic() {
        if (released || bgmStarted) return;
        if (choice < 0) choice = Music.SWING_STYLE;
        requestMusic(musicMode < 0 ? 0 : musicMode, choice);
    }

    @Override public synchronized void bandStart(int song, boolean muted) {
        if (released) return;
        musicMode = -1; bossPlaying = frenzyPlaying = false;
        stopMusic(); band.start(song,muted); band.volume(musicVolume);
    }
    @Override public float bandTime() { return band.time(); }
    @Override public void bandNote(int song,int note) { band.note(note); }
    @Override public void bandPause(boolean paused) { band.pause(paused,false); }
    @Override public synchronized void bandStop() { band.stop(); musicMode = -1; }
    @Override public void bandMuted(boolean muted) { band.muted(muted); }

    @Override public synchronized void selectMusic(final int style) {
        choice = style;
        requestMusic(0, style);
    }

    @Override public synchronized void bossMusic(final boolean active) {
        requestMusic(active ? 1 : 0, choice);
    }

    @Override public synchronized void versusMusic() { requestMusic(2, choice); }

    private void requestMusic(final int mode, final int style) {
        if (released || band.active()) return;
        if (musicMode == mode && playingStyle == style && (musicLoading || usable(bgmTrack))) return;
        stopMusic();
        musicMode = mode; playingStyle = style;
        bossPlaying = mode == 1;
        if (mode == 2) frenzyPlaying = false;
        bgmStarted = musicLoading = true;
        final boolean fast = frenzyPlaying;
        final int generation = musicGeneration;
        new Thread(new Runnable() {
            public void run() {
                try {
                    int synth = Music.isSynth(style) ? style : Music.SWING_STYLE;
                    short[] pcm = mode == 2 ? VersusMusic.clip()
                            : mode == 1 ? Music.bossLoop(synth) : Music.loop(synth, fast);
                    startPreparedMusic(pcm, mode != 2, generation);
                } catch (Throwable failure) {
                    synchronized (Audio.this) {
                        if (generation == musicGeneration) {
                            musicLoading = bgmStarted = false;
                            audioFailure(failure);
                        }
                    }
                }
            }
        }, "hexatype-bgm").start();
    }

    private static boolean usable(AudioTrack track) {
        return track != null && track.getState() == AudioTrack.STATE_INITIALIZED;
    }

    private static void releaseTrack(AudioTrack track) {
        if (track == null) return;
        try { track.stop(); } catch (Throwable ignored) {}
        try { track.release(); } catch (Throwable ignored) {}
    }

    private AudioTrack createTrack(short[] pcm, boolean loop, boolean music) {
        AudioTrack track = new AudioTrack(
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(music ? AudioAttributes.CONTENT_TYPE_MUSIC
                                : AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
                new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(Sfx.RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
                pcm.length * 2, AudioTrack.MODE_STATIC, AudioManager.AUDIO_SESSION_ID_GENERATE);
        try {
            if (track.getState() == AudioTrack.STATE_UNINITIALIZED
                    || track.write(pcm, 0, pcm.length) != pcm.length || !usable(track))
                throw new IllegalStateException("AudioTrack initialization/write failed");
            if (loop && track.setLoopPoints(0, pcm.length, -1) != AudioTrack.SUCCESS)
                throw new IllegalStateException("AudioTrack loop setup failed");
            return track;
        } catch (Throwable failure) { releaseTrack(track); throw failure; }
    }

    private synchronized void stopMusic() {
        musicGeneration++;
        musicLoading = false;
        AudioTrack old = bgmTrack; bgmTrack = null;
        releaseTrack(old);
    }

    /** Allocate only the latest request, serialized with disposal and SFX allocations. */
    private synchronized void startPreparedMusic(short[] pcm, boolean loop, int generation) {
        if (released || generation != musicGeneration || band.active()) return;
        musicLoading = false;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                bgmTrack = createTrack(pcm, loop, true);
                applyMusicMix();
                if (!musicPaused) bgmTrack.play();
                return;
            } catch (Throwable failure) {
                releaseTrack(bgmTrack); bgmTrack = null;
                evictEffect(); audioFailure(failure);
            }
        }
        bgmStarted = false;
    }

    synchronized void pauseMusic() {
        musicPaused = true;
        band.pause(true,true);
        try { if (bgmTrack != null) bgmTrack.pause(); } catch (Throwable failure) {
            stopMusic(); audioFailure(failure);
        }
        try { if (rocketTrack != null) rocketTrack.pause(); } catch (Throwable failure) {
            releaseTrack(rocketTrack); rocketTrack = null; rocketActive = false;
            mixRocket(false); audioFailure(failure);
        }
        try { if (bubbleTrack != null) bubbleTrack.pause(); } catch (Throwable failure) {
            releaseTrack(bubbleTrack); bubbleTrack = null; audioFailure(failure);
        }
        bubbleActive = false;
    }

    synchronized void resumeMusic() {
        if (released) return;
        musicPaused = false;
        band.pause(false,true);
        try { if (bgmTrack != null) bgmTrack.play(); } catch (Throwable failure) {
            stopMusic(); audioFailure(failure);
        }
        if (!usable(bgmTrack) && !musicLoading && musicMode >= 0) requestMusic(musicMode, playingStyle);
        try { if (rocketTrack != null && rocketActive) rocketTrack.play(); } catch (Throwable failure) {
            releaseTrack(rocketTrack); rocketTrack = null; rocketActive = false;
            mixRocket(false); audioFailure(failure);
        }
    }

    private void discardEffect(int id) {
        AudioTrack old = tracks[id]; tracks[id] = null;
        releaseTrack(old);
    }

    /** Prefer finished sounds; if every voice is busy, retire the oldest one. */
    private void evictEffect() {
        int oldest = -1, idle = -1;
        for (int i = 0; i < tracks.length; i++) {
            AudioTrack track = tracks[i];
            if (track == null) continue;
            if (oldest < 0 || effectUse[i] < effectUse[oldest]) oldest = i;
            boolean finished = !usable(track);
            try { finished |= track.getPlayState() != AudioTrack.PLAYSTATE_PLAYING
                    || track.getPlaybackHeadPosition() >= Sfx.build(i).length; }
            catch (Throwable ignored) { finished = true; }
            if (finished && (idle < 0 || effectUse[i] < effectUse[idle])) idle = i;
        }
        if (idle >= 0 || oldest >= 0) discardEffect(idle >= 0 ? idle : oldest);
    }

    private AudioTrack track(int id) {
        if (usable(tracks[id])) return tracks[id];
        discardEffect(id);
        int count = 0;
        for (AudioTrack track : tracks) if (track != null) count++;
        if (count >= MAX_EFFECT_TRACKS) evictEffect();
        tracks[id] = createTrack(Sfx.build(id), false, false);
        return tracks[id];
    }

    private void audioFailure(Throwable failure) {
        long now = System.nanoTime();
        if (lastFailureLog == 0 || now - lastFailureLog > 5000000000L) {
            lastFailureLog = now;
            android.util.Log.w("DDDumplingAudio", "Recoverable playback failure", failure);
        }
    }

    private void play(int id, float rate) { play(id, rate, 1f); }

    private synchronized void play(int id, float rate, float gain) {
        if (released || id < 0 || id >= Sfx.COUNT) return;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                AudioTrack track = track(id);
                track.stop();
                if (track.reloadStaticData() != AudioTrack.SUCCESS
                        || track.setPlaybackRate((int)(Sfx.RATE * rate)) != AudioTrack.SUCCESS)
                    throw new IllegalStateException("AudioTrack replay failed");
                effectGains[id] = gain;
                track.setVolume(gain * effectsVolume);
                track.play();
                effectUse[id] = ++useSerial;
                return;
            } catch (Throwable failure) {
                discardEffect(id); evictEffect(); audioFailure(failure);
            }
        }
    }

    @Override public void squish(int glyph, int depth) {
        // Deeper remaining stacks sound lower and rounder, so a stack audibly counts down.
        play(Sfx.squishId(glyph), (float) Math.pow(0.92, Math.max(0, depth - 1)));
    }

    @Override public void ninjaCombo() { play(Sfx.NINJA_COMBO,1f,Sfx.NINJA_COMBO_GAIN); }

    @Override public void ninjaSwish(int variant) {
        play(Sfx.ninjaSwishId(variant), 1f);
    }

    @Override public void zap(int hop) {
        // Climbs with the chain, capped so a long one stays a crack rather than turning into a
        // chirp — and well inside what setPlaybackRate will take.
        int step = hop < 1 ? 0 : hop > 9 ? 8 : hop - 1;
        play(Sfx.ZAP, 1f + 0.055f * step);
    }

    @Override public void collect(int nth) {
        // Climbs as the case fills, so a haul of four sounds like a run up the shelf rather than
        // the same chime four times. Capped well inside what setPlaybackRate will take.
        int step = nth < 0 ? 0 : nth > 7 ? 7 : nth;
        play(Sfx.COLLECT, 1f + 0.05f * step);
    }

    @Override public void star(int nth) {
        // Climbs the whole way up a course, so twenty stars are a ladder rather than one note
        // twenty times — that ladder is most of what tells you how the course is going without
        // looking at the counter. Smaller steps than the shelving chime because there are twenty of
        // them and not four: at the chime's 0.05 the last few came out as chirps.
        int step = nth < 1 ? 0 : nth > 16 ? 16 : nth - 1;
        play(Sfx.STAR, 1f + 0.032f * step);
    }

    @Override public void courseStart() {
        play(Sfx.COURSE, 1f);
    }

    @Override public void courseFinish() {
        play(Sfx.BLAST_OFF, 1f);
    }

    @Override public synchronized void rocket(float thrust) {
        if (released) return;
        try {
            if (thrust <= 0f) {
                if (rocketActive) mixRocket(false);
                rocketActive = false;
                if (rocketTrack != null) rocketTrack.pause();
                return;
            }
            if (rocketTrack == null) {
                short[] pcm = Sfx.rocket();
                rocketTrack = createTrack(pcm, true, false);
            }
            float p = Math.max(0f, Math.min(1f, thrust));
            rocketGain=0.16f + 0.28f * p;
            rocketTrack.setVolume(rocketGain * effectsVolume);
            rocketTrack.setPlaybackRate((int) (Sfx.RATE * (0.82f + 0.43f * p)));
            if (!rocketActive && !musicPaused) {
                mixRocket(true);
                rocketTrack.play();
                rocketActive = true;
            }
        } catch (Throwable failure) {
            releaseTrack(rocketTrack); rocketTrack = null; rocketActive = false;
            mixRocket(false); audioFailure(failure);
        }
    }

    /** Leaves a little headroom for the engine without muting the scene music. */
    private void mixRocket(boolean on) { rocketDucked=on; applyMusicMix(); }

    @Override public synchronized void volumes(float music,float effects) {
        musicVolume=music; effectsVolume=effects;
        applyMusicMix();
        try {
            for(int i=0;i<tracks.length;i++) if(tracks[i]!=null) tracks[i].setVolume(effectGains[i]*effects);
            if(rocketTrack!=null) rocketTrack.setVolume(rocketGain*effects);
            if(bubbleTrack!=null) bubbleTrack.setVolume(bubbleVolume*effects);
            if(effects==0f) hush();
        } catch(Throwable ignored) {}
    }
    private void applyMusicMix() {
        band.volume(musicVolume);
        float gain=musicVolume*(rocketDucked?.68f:1f)*(ducked?(musicMode==2?VersusMusic.ANNOUNCER_GAIN:.22f):1f);
        try {
            if(bgmTrack!=null) bgmTrack.setVolume((bossPlaying?Music.BOSS_GAIN:1f)*gain);
        } catch(Throwable ignored) {}
    }

    @Override public void tally(int nth) {
        // Rises with the count, so how the attempt went is audible before the number is read. A
        // wider step than the pickup ladder and over a longer count: this fires once, so there is
        // nothing for it to blend with, and the two ends want to be plainly different tones.
        int step = nth < 0 ? 0 : nth > 20 ? 20 : nth;
        play(Sfx.TALLY, 0.94f + 0.018f * step);
    }

    @Override public void paradeJoin() {
        play(Sfx.JOIN, 1f);
    }

@Override public void rosterJoin() {
        play(Sfx.ROSTER_JOIN, 1f);
    }

    @Override public void gameOver() {
        play(Sfx.OVER, 1f);
    }
    @Override public void survivalGameOver() { play(Sfx.SURVIVAL_OVER, 1f); }

    @Override public void clearWord() {
        play(Sfx.CLEAR, 1f);
    }

    @Override public void shuffleBlip() { play(Sfx.SHUFFLE_BLIP, 1f, 0.3f); }
    @Override public void uiBloop() { play(Sfx.UI_BLOOP, 1f, 0.22f); }
    @Override public void scoreResetConfirm() { play(Sfx.SCORE_RESET_CONFIRM,1f,.7f); }
    @Override public void scoreResetBrush(int stroke) {
        play(Sfx.SCORE_RESET_BRUSH,1f+.035f*Math.max(0,Math.min(2,stroke)),.62f);
    }
    @Override public void landShuffle() { play(Sfx.LAND_SHUFFLE, 1f, 0.16f); }
    @Override public void slimeCover(boolean release) { play(release ? Sfx.SLIME_RELEASE : Sfx.SLIME_COVER, 1f, 0.65f); }
    @Override public void debuffDown() { play(Sfx.DEBUFF_DOWN, 1f, 0.8f); }
    @Override public void caveEvent(int sound) { play(sound, 1f, .85f); }
    @Override public void linkedThud() { play(Sfx.LINKED_THUD, 1f, 0.85f); }

    @Override public void wrong() {
        play(Sfx.WRONG, 1f);
    }

    @Override public void damage() {
        play(Sfx.DRIP, 1f);
    }

    @Override public void achievement() {
        play(Sfx.ACHIEVEMENT, 1f);
    }

    @Override public void bossLaugh() {
        play(Sfx.BOSS_LAUGH, 1f);
    }

    @Override public void bossTaunt(int kind) {
        if (kind >= 0 && kind < Boss.COUNT) play(Sfx.bossTauntId(kind), 1f, 0.78f);
    }

    @Override public void bossDamage() {
        play(Sfx.BOSS_DAMAGE, 0.92f, 0.72f);
    }

    @Override public void bossSplit() {
        play(Sfx.BOSS_SPLIT, 1.08f, 0.68f);
    }

    @Override public void divideDamage() { play(Sfx.DIVIDE_DAMAGE, 1f, 0.76f); }
    @Override public void divideSplit() { play(Sfx.DIVIDE_SPLIT, 1f, 0.78f); }
@Override public void divideDeactivate() { play(Sfx.DIVIDE_DEACTIVATE, 1f, 0.82f); }
    @Override public void divideSupernova() { play(Sfx.DIVIDE_SUPERNOVA, 1f, 0.95f); }
    @Override public void divideBoing(float weight) {
        float w = Math.max(0f, Math.min(1f, weight));
        int id = w >= 0.67f ? Sfx.DIVIDE_BOING_HEAVY
                : w >= 0.34f ? Sfx.DIVIDE_BOING_MEDIUM : Sfx.DIVIDE_BOING_LIGHT;
        play(id, 1f, 0.58f + 0.16f * w);
    }

    @Override public void shieldBounce() { play(Sfx.SHIELD_BOUNCE, 1f, 0.72f); }
    @Override public void gatorParry() { play(Sfx.GATOR_PARRY, 1f); }
    @Override public void catLaser() { play(Sfx.CAT_LASER,1f,Sfx.CAT_LASER_GAIN); }
    @Override public void catFlurry(int note,int count) { play(Sfx.catFlurryId(note,count),1f,Sfx.CAT_FLURRY_GAIN); }
    @Override public void catMeow(int variant) { play(Sfx.catMeowId(variant),1f,Sfx.CAT_MEOW_GAIN); }
    @Override public void gatorTaunt(int variant) { play(Sfx.gatorTauntId(variant), 1f, Sfx.GATOR_TAUNT_GAIN); }
    @Override public void gatorPrompt(int variant) { play(Sfx.gatorPromptId(variant), 1f, Sfx.GATOR_PROMPT_GAIN); }
    @Override public void gatorSplash(boolean entry) { play(Sfx.GATOR_SPLASH, entry?Sfx.GATOR_ENTRY_RATE:1f, Sfx.GATOR_SPLASH_GAIN); }
    @Override public void slimeDragBoip() { play(Sfx.SLIME_DRAG_BOIP,1f,Sfx.SLIME_DRAG_BOIP_GAIN); }
    @Override public void slimeVulnerable() { play(Sfx.SLIME_VULNERABLE,1f,Sfx.SLIME_VULNERABLE_GAIN); }
    @Override public void slimeSquish(int variant) { play(Sfx.slimeSquishId(variant), 1f, Sfx.SLIME_SQUISH_GAIN); }
    @Override public void octoWave() { play(Sfx.OCTO_WAVE, 1f, Sfx.OCTO_WAVE_GAIN); }
    @Override public void octoDamage() { play(Sfx.OCTO_DAMAGE, 1f, .82f); }
    @Override public void octoCue() { play(Sfx.OCTO_CUE, 1f, 0.74f); }
    @Override public void octoLock() { play(Sfx.OCTO_LOCK, 1f, 0.70f); }
    @Override public void mushroomShake() { play(Sfx.MUSHROOM_SHAKE, 1f, 0.78f); }
    @Override public void mushroomSpore() { play(Sfx.MUSHROOM_SPORE, 1f, 0.72f); }

    @Override public void boltPop() {
        // Let the 170ms envelope reach zero; stopping it mid-wave is an audible click.
        long now = System.nanoTime();
        if (now < boltPopUntil) return;
        boltPopUntil = now + 165000000L;
        play(Sfx.BOLT_POP, 1f, 0.68f);
    }

    @Override public void boltDeath() { play(Sfx.BOLT_DEATH, 1f, 0.82f); }

    @Override public synchronized void bossCharge(float charge) {
        if (released) return;
        try {
            if (bubbleTrack == null && charge <= 0f) return;
            if (bubbleTrack == null) {
                short[] pcm = Sfx.bubble();
                bubbleTrack = createTrack(pcm, true, false);
            }
            float p = Math.max(0f, Math.min(1f, charge));
            float target = p * 0.10f;
            bubbleVolume += (target - bubbleVolume) * 0.18f;
            bubbleTrack.setVolume(bubbleVolume * effectsVolume);
            // Fixed-rate playback avoids resampler zipper noise. Once created, the zero-ended loop
            // keeps running silently between charges so no waveform is ever paused mid-cycle.
            if (!bubbleActive && !musicPaused) {
                bubbleTrack.setPlaybackRate(Sfx.RATE);
                bubbleTrack.play();
                bubbleActive = true;
            }
        } catch (Throwable failure) {
            releaseTrack(bubbleTrack); bubbleTrack = null; bubbleActive = false;
            audioFailure(failure);
        }
    }

    @Override public void gameStart() {
        play(Sfx.START, 1f, 0.72f);
    }

    @Override public void stageClear() {
        play(Sfx.STAGE_CLEAR, 1f);
    }

    @Override public void powerClear() {
        play(Sfx.POWER_CLEAR, 1f);
    }

    /** Swaps the looping track for the driven variant of the current scene. */
    @Override public synchronized void frenzy(boolean on) {
        if (released || frenzyPlaying == on) return;
        frenzyPlaying = on;
        if (!bgmStarted) return;
        stopMusic();
        requestMusic(musicMode < 0 ? 0 : musicMode, choice);
    }

    // ---- narration ----------------------------------------------------------

    private android.speech.tts.TextToSpeech tts;
    private boolean ttsReady, ttsBroken;
    /** Queued while the engine is still waking up, or -1 for nothing waiting. */
    private int ttsPending = -1;
    private boolean ttsPendingName,ttsPendingVersus,ttsPendingCompanion;
    private String ttsPendingExplanation;
    private long speechSerial;
    private volatile String versusUtterance;
    private volatile int versusOffset=-1;
    @Override public int versusSpeechOffset() {return versusOffset;}

    /**
     * The story popup, read aloud. Requests wait if the title-screen speech warmup has not
     * finished yet.
     *
     * Guarded throughout and switched off for good on any failure: plenty of devices have no
     * speech engine at all, and a missing voice must never be more than a missing voice.
     */
    @Override public synchronized void narrate(int entry) {
        requestSpeech(entry, false);
    }

    @Override public synchronized void announceSquishy(int entry) {
        requestSpeech(entry, true);
    }

    @Override public synchronized void announceVersus(int entry,int boss) {
        announceVersus(entry,boss,Skins.DEFAULT);
    }
    @Override public synchronized void announceVersus(int entry,int boss,int skin) {
        requestExplanation(Narration.versus(entry,boss,skin),true);
    }

    @Override public synchronized void explain(String text) {
        requestExplanation(text,false);
    }

    @Override public synchronized void companionExclaim(String text) {
        requestExplanation(text,false,true);
    }

    private void requestExplanation(String text,boolean versus) {
        requestExplanation(text,versus,false);
    }

    private void requestExplanation(String text,boolean versus,boolean companion) {
        if(effectsVolume<=0f || ttsBroken || text==null || text.isEmpty())return;
        versusUtterance=null;versusOffset=-1;
        ttsPending=-1;ttsPendingExplanation=text;ttsPendingVersus=versus;ttsPendingCompanion=companion;
        prepareSpeech();
        if(ttsReady)readPending();
    }

    private void requestSpeech(int entry, boolean nameOnly) {
        versusUtterance=null;versusOffset=-1;
        if(effectsVolume<=0f || ttsBroken || entry<0 || entry>=Collect.COUNT) return;
        ttsPendingExplanation=null;
        ttsPending = entry;
        ttsPendingName = nameOnly;
        prepareSpeech();
        if (ttsReady) readPending();
    }

    /** Warm up on the title screen so the short name call is ready for the first run. */
    private synchronized void prepareSpeech() {
        if(tts!=null || ttsBroken) return;
        try {
            tts = new android.speech.tts.TextToSpeech(ctx,
                    new android.speech.tts.TextToSpeech.OnInitListener() {
                        @Override public void onInit(int status) {
                            synchronized (Audio.this) {
                                if (ttsBroken || status != android.speech.tts.TextToSpeech.SUCCESS) {
                                    ttsBroken = true;
                                    return;
                                }
                                setVoice();
                                listen();
                                ttsReady = true;
                                // The panel may well have been dismissed while it started up.
                                if (ttsPending >= 0 || ttsPendingExplanation!=null) readPending();
                            }
                        }
                    });
        } catch (Throwable t) {
            ttsBroken = true;
        }
    }

    @Override public synchronized void hush() {
        versusUtterance=null;versusOffset=-1;
        ttsPending = -1;
        ttsPendingExplanation=null;
        lastUtterance = null;
        try {
            if (tts != null && ttsReady) tts.stop();
        } catch (Throwable ignored) {
            // Nothing to salvage; the panel is gone either way.
        }
        duck(false);
    }

    /**
     * Watches for the end of the reading so the music can come back up.
     *
     * On the last utterance only, and on any error or stop: there is no queue-drained callback, so
     * the id of the last thing queued is what stands in for one. Without this the music would stay
     * ducked for the rest of the session the first time a story was opened.
     */
    private void listen() {
        try {
            tts.setOnUtteranceProgressListener(
                    new android.speech.tts.UtteranceProgressListener() {
                        @Override public void onStart(String id) { }

                        @Override public void onRangeStart(String id,int start,int end,int frame) {
                            synchronized(Audio.this) {
                                if(id!=null && id.equals(versusUtterance))versusOffset=start;
                            }
                        }

                        @Override public void onDone(String id) {
                            if (id != null && id.equals(lastUtterance)) duck(false);
                        }

                        @Override public void onStop(String id, boolean interrupted) {
                            if (id != null && id.equals(lastUtterance)) duck(false);
                        }

                        @SuppressWarnings("deprecation")
                        @Override public void onError(String id) {
                            if (id != null && id.equals(lastUtterance)) duck(false);
                        }
                    });
        } catch (Throwable ignored) {
            // No listener means the music simply comes back up on the next hush().
        }
    }

    /**
     * English if the engine has it, its own default if not — never a refusal to speak. Then the
     * best voice the engine will admit to having.
     *
     * Voice choice matters more to how this sounds than pitch and rate together: prosody is built
     * into a voice, and a higher-quality one has more of it. Network voices are skipped — a story
     * popup must read the same on a train as at home.
     */
    private void setVoice() {
        try {
            int got = tts.setLanguage(java.util.Locale.US);
            if (got == android.speech.tts.TextToSpeech.LANG_MISSING_DATA
                    || got == android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(java.util.Locale.getDefault());
            }
        } catch (Throwable ignored) {
            // Leave it on whatever it starts with.
        }
        try {
            android.speech.tts.Voice best = null;
            java.util.Set<android.speech.tts.Voice> all = tts.getVoices();
            if (all != null) {
                for (android.speech.tts.Voice v : all) {
                    if (v == null || v.isNetworkConnectionRequired()) continue;
                    if (!"en".equals(v.getLocale().getLanguage())) continue;
                    if (best == null || v.getQuality() > best.getQuality()) best = v;
                }
            }
            if (best != null) tts.setVoice(best);
        } catch (Throwable ignored) {
            // Any engine that will not enumerate its voices keeps the one it chose.
        }
    }

    /** Speech follows effects volume; ducking keeps the scene music below it. */
    private void readPending() {
        if(ttsPendingExplanation!=null) {
            String text=ttsPendingExplanation;ttsPendingExplanation=null;
            if(effectsVolume<=0f)return;
            try {
                android.os.Bundle params=new android.os.Bundle();
                params.putFloat(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME,effectsVolume*(ttsPendingCompanion?Narration.COMPANION_GAIN:1));
                tts.setPitch(ttsPendingVersus?Narration.VERSUS_PITCH:Narration.PITCH);
                tts.setSpeechRate(ttsPendingVersus?Narration.VERSUS_RATE:.9f);
                lastUtterance="tutorial-"+(++speechSerial);
                versusUtterance=ttsPendingVersus?lastUtterance:null;duck(!ttsPendingCompanion);
                if(tts.speak(text,android.speech.tts.TextToSpeech.QUEUE_FLUSH,params,lastUtterance)
                        !=android.speech.tts.TextToSpeech.SUCCESS)duck(false);
            } catch(Throwable t) { ttsBroken=true;duck(false); }
            return;
        }
        int entry=ttsPending;
        if(entry<0) return;
        if(!ttsPendingName) { read(entry); return; }
        ttsPending=-1;
        if(effectsVolume<=0f) return;
        try {
            android.os.Bundle params=new android.os.Bundle();
            params.putFloat(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME,effectsVolume);
            tts.setPitch(Narration.NAME_PITCH);
            tts.setSpeechRate(Narration.NAME_RATE);
            lastUtterance="name-"+(++speechSerial);
            duck(true);
            if(tts.speak(Narration.name(entry),android.speech.tts.TextToSpeech.QUEUE_FLUSH,
                    params,lastUtterance)!=android.speech.tts.TextToSpeech.SUCCESS) duck(false);
        } catch(Throwable t) { ttsBroken=true; duck(false); }
    }

    /** Queue the name, place and whole story, with a beat between them. */
    private void read(int entry) {
        if(effectsVolume<=0f) { ttsPending=-1; return; }
        ttsPending = -1;
        try {
            String[] lines = Narration.lines(entry);
            long serial=++speechSerial;
            android.os.Bundle params = new android.os.Bundle();
            params.putFloat(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME, effectsVolume);
            // One pitch for the whole reading, set once. See Narration for why it stopped moving.
            tts.setPitch(Narration.PITCH);
            duck(true);
            for (int i = 0; i < lines.length; i++) {
                tts.setSpeechRate(Narration.rate(i));
                lastUtterance = "story-" + serial + "-" + i;
                tts.speak(lines[i], i == 0
                        ? android.speech.tts.TextToSpeech.QUEUE_FLUSH
                        : android.speech.tts.TextToSpeech.QUEUE_ADD, params, lastUtterance);
                int gap = Narration.gapMs(lines, i);
                if (gap > 0) {
                    tts.playSilentUtterance(gap,
                            android.speech.tts.TextToSpeech.QUEUE_ADD, "gap-" + entry + "-" + i);
                }
            }
        } catch (Throwable t) {
            ttsBroken = true;
            duck(false);
        }
    }

    /** The last utterance queued, so the listener knows when the reading is over. */
    private volatile String lastUtterance;
    /** True while the music is held down for the voice. */
    private boolean ducked;

    /**
     * Holds the music down while the voice is speaking, and lets it back up after.
     *
     * This is the real volume control. A speech engine's own volume parameter is a fraction of the
     * stream it plays on, so it cannot be pushed past what the music is already using — the way to
     * make a voice louder is to make everything else quieter.
     */
    private synchronized void duck(boolean on) { ducked=on; applyMusicMix(); }

    synchronized void release() {
        released = true;
        ttsBroken = true;
        hush(); band.stop(); stopMusic();
        releaseTrack(rocketTrack); rocketTrack = null; rocketActive = false;
        releaseTrack(bubbleTrack); bubbleTrack = null; bubbleActive = false;
        try { if (tts != null) tts.stop(); } catch (Throwable ignored) {}
        try { if (tts != null) tts.shutdown(); } catch (Throwable ignored) {}
        tts = null;
        for (int i = 0; i < tracks.length; i++) discardEffect(i);
    }
}
