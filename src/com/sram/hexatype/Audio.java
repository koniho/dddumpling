package com.sram.hexatype;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;

/**
 * Plays the {@link Sfx} buffers through {@link AudioTrack}.
 *
 * One static-mode track per sound, built lazily on first use and replayed with
 * stop/reload/play. Stack depth is handled by nudging the playback rate rather than
 * synthesising a variant per depth, which keeps the track count down.
 *
 * Every call is guarded: audio is a garnish, and must never be able to take the game down.
 */
final class Audio implements GameCore.Sound {

    private final AudioTrack[] tracks = new AudioTrack[Sfx.COUNT];
    private boolean broken;

    private final Context ctx;
    private AudioTrack bgmTrack;
    private AudioTrack rocketTrack;
    private AudioTrack bubbleTrack;
    private boolean rocketActive;
    private boolean bubbleActive;
    private float bubbleVolume;
    private MediaPlayer bgmPlayer;
    private boolean bgmStarted;
    private boolean frenzyPlaying;
    private boolean bossPlaying;
    private long boltPopUntil;
    /** -1 none, 0 selected track, 1 boss arrangement. Prevents same-track restarts. */
    private int musicMode = -1, playingStyle = -1;

    Audio(Context ctx) {
        this.ctx = ctx;
        new Thread(new Runnable() {
             public void run() {
                for (int id = 0; id < Sfx.COUNT; id++) Sfx.build(id);
                Sfx.rocket();
                Sfx.bubble();
                Music.preRender(Music.SWING_STYLE);
            }
        }, "hexatype-audio-prerender").start();
    }

    /**
     * Starts looping background music, off the calling thread because synthesising the loop
     * takes a moment. Prefers a user-supplied {@code res/raw/bgm} if one is present —
     * resolved by name so the build does not depend on the file existing.
     */
    private int choice = -1;

    /** Starts the loop for the current choice if it is not already playing, off the calling thread. */
    void startMusic() {
        if (bgmStarted || broken) return;
        bgmStarted = true;
        selectMusic(choice < 0 ? Music.SWING_STYLE : choice);
    }

    @Override public void selectMusic(final int style) {
        choice = style;
        bgmStarted = true;
        if (musicMode == 0 && playingStyle == style) return;
        musicMode = 0;
        playingStyle = style;
        bossPlaying = false;
        stopMusic();
        if (style == Music.OFF) return;
        new Thread(new Runnable() {
            @Override public void run() {
                // "MY TRACK" plays res/raw/bgm when present, and falls back to the synth
                // loop when it is not, so the option is never a dead end.
                if (style == Music.CUSTOM && playRawMusic()) return;
                playSynthMusic(Music.isSynth(style) ? style : Music.SWING_STYLE, frenzyPlaying);
            }
        }, "hexatype-bgm").start();
    }

    @Override public void bossMusic(final boolean active) {
        if (!active) {
            if (musicMode == 0 && playingStyle == choice) return;
            bossPlaying = false;
            musicMode = -1;
            selectMusic(choice);
            return;
        }
        if (musicMode == 1 && playingStyle == choice) return;
        bossPlaying = true;
        musicMode = 1;
        playingStyle = choice;
        stopMusic();
        if (choice == Music.OFF) return;
        bgmStarted = true;
        new Thread(new Runnable() {
             public void run() {
                int style = Music.isSynth(choice) ? choice : Music.SWING_STYLE;
                try {
                    short[] pcm = Music.bossLoop(style);
                    AudioTrack t = musicTrack(pcm);
                    t.setVolume(Music.BOSS_GAIN);
                    if (!bossPlaying) { t.release(); return; }
                    t.play();
                    bgmTrack = t;
                } catch (Throwable ignored) {}
            }
        }, "hexatype-boss-bgm").start();
    }

    private AudioTrack musicTrack(short[] pcm) {
        AudioTrack t = new AudioTrack(
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(),
                new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(Sfx.RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
                pcm.length * 2, AudioTrack.MODE_STATIC, AudioManager.AUDIO_SESSION_ID_GENERATE);
        t.write(pcm, 0, pcm.length);
        t.setLoopPoints(0, pcm.length, -1);
        return t;
    }

    private void stopMusic() {
        try {
            if (bgmPlayer != null) {
                bgmPlayer.release();
                bgmPlayer = null;
            }
            if (bgmTrack != null) {
                bgmTrack.stop();
                bgmTrack.release();
                bgmTrack = null;
            }
        } catch (Throwable ignored) {
            // Already gone.
        }
    }

    private boolean playRawMusic() {
        try {
            int id = ctx.getResources().getIdentifier("bgm", "raw", ctx.getPackageName());
            if (id == 0) return false;
            MediaPlayer mp = MediaPlayer.create(ctx, id);
            if (mp == null) return false;
            mp.setLooping(true);
            mp.setVolume(0.55f, 0.55f);
            mp.start();
            bgmPlayer = mp;
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private void playSynthMusic(int style, boolean frenzy) {
        try {
            short[] pcm = Music.loop(style, frenzy);
            AudioTrack t = new AudioTrack(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                    new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(Sfx.RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    pcm.length * 2, AudioTrack.MODE_STATIC,
                    AudioManager.AUDIO_SESSION_ID_GENERATE);
            t.write(pcm, 0, pcm.length);
            // -1 repeats forever; the loop is written to wrap cleanly.
            t.setLoopPoints(0, pcm.length, -1);
            t.play();
            bgmTrack = t;
        } catch (Throwable ignored) {
            // No music on this device; the game is perfectly playable silent.
        }
    }

    void pauseMusic() {
        try {
            if (bgmPlayer != null) bgmPlayer.pause();
            if (bgmTrack != null) bgmTrack.pause();
            if (rocketTrack != null) rocketTrack.pause();
        } catch (Throwable ignored) {
            // Nothing to pause.
        }
    }

    void resumeMusic() {
        try {
            if (bgmPlayer != null) bgmPlayer.start();
            if (bgmTrack != null) bgmTrack.play();
            if (rocketTrack != null && rocketActive) rocketTrack.play();
        } catch (Throwable ignored) {
            // Nothing to resume.
        }
    }

    private AudioTrack track(int id) {
        if (broken || id < 0 || id >= Sfx.COUNT) return null;
        if (tracks[id] != null) return tracks[id];
        try {
            short[] pcm = Sfx.build(id);
            AudioTrack t = new AudioTrack(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(Sfx.RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    pcm.length * 2, AudioTrack.MODE_STATIC,
                    AudioManager.AUDIO_SESSION_ID_GENERATE);
            t.write(pcm, 0, pcm.length);
            tracks[id] = t;
            return t;
        } catch (Throwable e) {
            // A device that will not give us a track just plays silently.
            broken = true;
            return null;
        }
    }

    private void play(int id, float rate) { play(id, rate, 1f); }

    private void play(int id, float rate, float gain) {
        AudioTrack t = track(id);
        if (t == null) return;
        try {
            t.stop();
            t.reloadStaticData();
            t.setPlaybackRate((int) (Sfx.RATE * rate));
            t.setVolume(gain);
            t.play();
        } catch (Throwable ignored) {
            // Mid-playback state races are not worth crashing over.
        }
    }

    @Override public void squish(int glyph, int depth) {
        // Deeper remaining stacks sound lower and rounder, so a stack audibly counts down.
        play(Sfx.SQUISH_0 + glyph, (float) Math.pow(0.92, Math.max(0, depth - 1)));
    }

    @Override public void chop() {
        play(Sfx.CHOP, 1f);
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

    @Override public void rocket(float thrust) {
        try {
            if (thrust <= 0f) {
                if (rocketActive) mixRocket(false);
                rocketActive = false;
                if (rocketTrack != null) rocketTrack.pause();
                return;
            }
            if (rocketTrack == null) {
                short[] pcm = Sfx.rocket();
                rocketTrack = new AudioTrack(
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build(),
                        new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(Sfx.RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build(),
                        pcm.length * 2, AudioTrack.MODE_STATIC,
                        AudioManager.AUDIO_SESSION_ID_GENERATE);
                rocketTrack.write(pcm, 0, pcm.length);
                rocketTrack.setLoopPoints(0, pcm.length, -1);
            }
            float p = Math.max(0f, Math.min(1f, thrust));
            rocketTrack.setVolume(0.16f + 0.28f * p);
            rocketTrack.setPlaybackRate((int) (Sfx.RATE * (0.82f + 0.43f * p)));
            if (!rocketActive) {
                mixRocket(true);
                rocketTrack.play();
                rocketActive = true;
            }
        } catch (Throwable ignored) {
            rocketActive = false;
        }
    }

    /** Leaves a little headroom for the engine without muting the selected music. */
    private void mixRocket(boolean on) {
        try {
            if (bgmPlayer != null) {
                float v = on ? 0.38f : 0.55f;
                bgmPlayer.setVolume(v, v);
            }
            if (bgmTrack != null) bgmTrack.setVolume(on ? 0.68f : 1f);
        } catch (Throwable ignored) {
            // The engine still plays if music volume cannot be changed.
        }
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

    @Override public void clearWord() {
        play(Sfx.CLEAR, 1f);
    }

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
        if (kind >= 0 && kind < Boss.COUNT) play(Sfx.BOSS_TAUNT_0 + kind, 1f, 0.78f);
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
    @Override public void divideBoing(float weight) {
        float w = Math.max(0f, Math.min(1f, weight));
        int id = w >= 0.67f ? Sfx.DIVIDE_BOING_HEAVY
                : w >= 0.34f ? Sfx.DIVIDE_BOING_MEDIUM : Sfx.DIVIDE_BOING_LIGHT;
        play(id, 1f, 0.58f + 0.16f * w);
    }

    @Override public void shieldBounce() { play(Sfx.SHIELD_BOUNCE, 1f, 0.72f); }
    @Override public void slimeDamage() { play(Sfx.SLIME_DAMAGE, 1f, 0.76f); }
    @Override public void octoCue() { play(Sfx.OCTO_CUE, 1f, 0.74f); }
    @Override public void octoLock() { play(Sfx.OCTO_LOCK, 1f, 0.70f); }

    @Override public void boltPop() {
        // Let the 170ms envelope reach zero; stopping it mid-wave is an audible click.
        long now = System.nanoTime();
        if (now < boltPopUntil) return;
        boltPopUntil = now + 165000000L;
        play(Sfx.BOLT_POP, 1f, 0.68f);
    }

    @Override public void bossCharge(float charge) {
        try {
            if (bubbleTrack == null && charge <= 0f) return;
            if (bubbleTrack == null) {
                short[] pcm = Sfx.bubble();
                bubbleTrack = new AudioTrack(
                        new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
                        new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(Sfx.RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
                        pcm.length * 2, AudioTrack.MODE_STATIC, AudioManager.AUDIO_SESSION_ID_GENERATE);
                bubbleTrack.write(pcm, 0, pcm.length);
                bubbleTrack.setLoopPoints(0, pcm.length, -1);
            }
            float p = Math.max(0f, Math.min(1f, charge));
            float target = p * 0.10f;
            bubbleVolume += (target - bubbleVolume) * 0.18f;
            bubbleTrack.setVolume(bubbleVolume);
            // Fixed-rate playback avoids resampler zipper noise. Once created, the zero-ended loop
            // keeps running silently between charges so no waveform is ever paused mid-cycle.
            if (!bubbleActive) {
                bubbleTrack.setPlaybackRate(Sfx.RATE);
                bubbleTrack.play();
                bubbleActive = true;
            }
        } catch (Throwable ignored) {
            bubbleActive = false;
        }
    }

    @Override public void gameStart() {
        play(Sfx.START, 1f);
    }

    @Override public void stageClear() {
        play(Sfx.STAGE_CLEAR, 1f);
    }

    @Override public void powerClear() {
        play(Sfx.POWER_CLEAR, 1f);
    }

    /** Swaps the looping track for the driven variant of whatever the player selected. */
    @Override public void frenzy(boolean on) {
        if (frenzyPlaying == on) return;
        frenzyPlaying = on;
        if (!bgmStarted || choice == Music.OFF) return;
        stopMusic();
        final int style = Music.isSynth(choice) ? choice : Music.SWING_STYLE;
        final boolean fast = on;
        new Thread(new Runnable() {
            @Override public void run() {
                // A custom track cannot be sped up on API 21, so the frenzy always uses the
                // synth variant; the player's own track resumes when it ends.
                if (!fast && choice == Music.CUSTOM && playRawMusic()) return;
                playSynthMusic(style, fast);
            }
        }, "hexatype-bgm").start();
    }

    // ---- narration ----------------------------------------------------------

    private android.speech.tts.TextToSpeech tts;
    private boolean ttsReady, ttsBroken;
    /** Queued while the engine is still waking up, or -1 for nothing waiting. */
    private int ttsPending = -1;

    /**
     * The story popup, read aloud. The engine is built on first use and takes a moment to come
     * up, so the entry is parked until it does.
     *
     * Guarded throughout and switched off for good on any failure: plenty of devices have no
     * speech engine at all, and a missing voice must never be more than a missing voice.
     */
    @Override public void narrate(int entry) {
        if (ttsBroken) return;
        ttsPending = entry;
        if (tts != null) {
            if (ttsReady) read(entry);
            return;
        }
        try {
            tts = new android.speech.tts.TextToSpeech(ctx,
                    new android.speech.tts.TextToSpeech.OnInitListener() {
                        @Override public void onInit(int status) {
                            if (status != android.speech.tts.TextToSpeech.SUCCESS) {
                                ttsBroken = true;
                                return;
                            }
                            setVoice();
                            listen();
                            ttsReady = true;
                            // The panel may well have been dismissed while it started up.
                            if (ttsPending >= 0) read(ttsPending);
                        }
                    });
        } catch (Throwable t) {
            ttsBroken = true;
        }
    }

    @Override public void hush() {
        ttsPending = -1;
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

                        @Override public void onDone(String id) {
                            if (id != null && id.equals(lastUtterance)) duck(false);
                        }

                        @Override public void onStop(String id, boolean interrupted) {
                            duck(false);
                        }

                        @SuppressWarnings("deprecation")
                        @Override public void onError(String id) {
                            duck(false);
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

    /**
     * Queues the whole reading at once: the name, the place, then the story in one piece, with a
     * beat of silence between them.
     *
     * Volume is set explicitly to full. It is a scale of the music stream rather than a gain, so
     * full is as loud as this can be made from here — the rest of "louder" is the music getting out
     * of the way, which is what {@link #duck} does.
     */
    private void read(int entry) {
        ttsPending = -1;
        try {
            String[] lines = Narration.lines(entry);
            android.os.Bundle params = new android.os.Bundle();
            params.putFloat(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f);
            // One pitch for the whole reading, set once. See Narration for why it stopped moving.
            tts.setPitch(Narration.PITCH);
            duck(true);
            for (int i = 0; i < lines.length; i++) {
                tts.setSpeechRate(Narration.rate(i));
                lastUtterance = "story-" + entry + "-" + i;
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
    private String lastUtterance;
    /** True while the music is held down for the voice. */
    private boolean ducked;

    /**
     * Holds the music down while the voice is speaking, and lets it back up after.
     *
     * This is the real volume control. A speech engine's own volume parameter is a fraction of the
     * stream it plays on, so it cannot be pushed past what the music is already using — the way to
     * make a voice louder is to make everything else quieter.
     */
    private void duck(boolean on) {
        if (ducked == on) return;
        ducked = on;
        try {
            // The custom track is mixed at 0.55 to begin with; the synth loop at unity.
            if (bgmPlayer != null) {
                float v = on ? 0.55f * 0.25f : 0.55f;
                bgmPlayer.setVolume(v, v);
            }
            if (bgmTrack != null) bgmTrack.setVolume(on ? 0.22f : 1f);
        } catch (Throwable ignored) {
            // Ducking is a courtesy; failing at it must not stop the voice.
        }
    }

    void release() {
        stopMusic();
        try {
            if (rocketTrack != null) rocketTrack.release();
            rocketTrack = null;
            rocketActive = false;
            if (bubbleTrack != null) bubbleTrack.release();
            bubbleTrack = null;
            bubbleActive = false;
        } catch (Throwable ignored) {
            // Already gone.
        }
        try {
            if (tts != null) {
                tts.stop();
                tts.shutdown();
                tts = null;
            }
        } catch (Throwable ignored) {
            // Going away regardless.
        }
        for (int i = 0; i < tracks.length; i++) {
            if (tracks[i] == null) continue;
            try {
                tracks[i].release();
            } catch (Throwable ignored) {
                // Already gone.
            }
            tracks[i] = null;
        }
    }
}
