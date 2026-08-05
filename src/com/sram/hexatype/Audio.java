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
    private MediaPlayer bgmPlayer;
    private boolean bgmStarted;
    private boolean frenzyPlaying;

    Audio(Context ctx) {
        this.ctx = ctx;
    }

    /**
     * Starts looping background music, off the calling thread because synthesising the loop
     * takes a moment. Prefers a user-supplied {@code res/raw/bgm} if one is present —
     * resolved by name so the build does not depend on the file existing.
     */
    private int choice = -1;

    /** Starts (or restarts) the loop for the current choice, off the calling thread. */
    void startMusic() {
        if (bgmStarted || broken) return;
        bgmStarted = true;
        selectMusic(choice < 0 ? Music.SWING_STYLE : choice);
    }

    @Override public void selectMusic(final int style) {
        choice = style;
        bgmStarted = true;
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
        } catch (Throwable ignored) {
            // Nothing to pause.
        }
    }

    void resumeMusic() {
        try {
            if (bgmPlayer != null) bgmPlayer.start();
            if (bgmTrack != null) bgmTrack.play();
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

    private void play(int id, float rate) {
        AudioTrack t = track(id);
        if (t == null) return;
        try {
            t.stop();
            t.reloadStaticData();
            t.setPlaybackRate((int) (Sfx.RATE * rate));
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
    }

    /** English if the engine has it, its own default if not — never a refusal to speak. */
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
    }

    /**
     * Queues the whole reading at once, a sentence per utterance with a beat of silence between.
     *
     * The pitch and rate are set before each one because the engine captures them as an
     * utterance is queued — that is the only handle it gives on delivery, and setting them once
     * up front would read all thirty stories in the same flat voice.
     */
    private void read(int entry) {
        ttsPending = -1;
        try {
            String[] lines = Narration.lines(entry);
            for (int i = 0; i < lines.length; i++) {
                tts.setPitch(Narration.pitch(lines, i));
                tts.setSpeechRate(Narration.rate(lines, i));
                tts.speak(lines[i], i == 0
                        ? android.speech.tts.TextToSpeech.QUEUE_FLUSH
                        : android.speech.tts.TextToSpeech.QUEUE_ADD, null, "story-" + entry
                        + "-" + i);
                int gap = Narration.gapMs(lines, i);
                if (gap > 0) {
                    tts.playSilentUtterance(gap,
                            android.speech.tts.TextToSpeech.QUEUE_ADD, "gap-" + entry + "-" + i);
                }
            }
        } catch (Throwable t) {
            ttsBroken = true;
        }
    }

    void release() {
        stopMusic();
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
