package com.sram.hexatype;

/**
 * Original looping background music, synthesised at runtime.
 *
 * A jaunty four-bar swing vamp — walking bass, off-beat chord comping, a plucky melody and
 * a shaker — over a ii-V-I turnaround, so it loops without an obvious seam. Written here
 * rather than shipped as an audio file: no licensing question, nothing to bundle, and the
 * harness can render it to WAV and audition it like any other sound.
 *
 * If you have a track you hold the rights to, drop it in as {@code res/raw/bgm.<ext>} and
 * {@link Audio} plays that instead; see README.md.
 */
final class Music {

    /** Selectable tracks. The last entry plays {@code res/raw/bgm} if one is present. */
    static final String[] NAMES = {"MOOG SWING", "LOFI DRIFT", "CHIP MARCH", "OFF", "MY TRACK"};
    static final int SWING_STYLE = 0, DRIFT = 1, MARCH = 2, OFF = 3, CUSTOM = 4;

    private static final int BARS = 4;
    private static final int BEATS = BARS * 4;

    /** Per-style tempo, swing, brightness, bass level and melody gain. */
    private static final float[][] STYLE = {
        //  bpm    swing  bright  bass   lead   decay
        {  132f,   0.64f,  0.30f,  0.52f, 0.34f, 3.1f},   // MOOG SWING
        {   76f,   0.52f,  0.16f,  0.46f, 0.26f, 1.7f},   // LOFI DRIFT
        {  156f,   0.50f,  0.62f,  0.44f, 0.32f, 4.4f},   // CHIP MARCH
    };

    /** Semitone offsets from A2 for each chord's root, one per bar: Dm7 G7 Cmaj7 A7. */
    private static final int[] ROOT = {5, 10, 3, 0};
    /** Chord tones (semitones above the root) for the comp voicing. */
    private static final int[][] VOICING = {
        {0, 3, 7, 10},   // m7
        {0, 4, 7, 10},   // 7
        {0, 4, 7, 11},   // maj7
        {0, 4, 7, 10},   // 7
    };
    /** Melody as (beat, semitone-above-root, beats-long) triples over the whole loop. */
    private static final float[][] MELODY = {
        {0.0f, 12, 0.75f}, {1.0f, 10, 0.5f}, {1.66f, 7, 0.75f}, {3.0f, 10, 1.0f},
        {4.0f, 11, 0.75f}, {5.0f, 9, 0.5f}, {5.66f, 7, 0.75f}, {7.0f, 4, 1.0f},
        {8.0f, 7, 0.75f}, {9.0f, 11, 0.5f}, {9.66f, 12, 0.75f}, {11.0f, 11, 1.0f},
        {12.0f, 9, 0.5f}, {12.66f, 7, 0.5f}, {13.66f, 4, 0.75f}, {15.0f, 0, 1.0f},
    };

    private Music() {}

    /** True when the style index names a synthesised track rather than OFF or a file. */
    static boolean isSynth(int style) {
        return style >= 0 && style < STYLE.length;
    }

    /** Seconds per beat. */
    private static float beat(int style) {
        return 60f / STYLE[style][0];
    }

    /** Tempo multiplier for the powerup variant. */
    private static final float FRENZY_TEMPO = 1.35f;

    /** Total loop length in samples for the given style. */
    static int loopFrames(int style) {
        return loopFrames(style, false);
    }

    static int loopFrames(int style, boolean frenzy) {
        if (!isSynth(style)) style = SWING_STYLE;
        float spb = beat(style) / (frenzy ? FRENZY_TEMPO : 1f);
        return (int) (Sfx.RATE * spb * BEATS);
    }

    static short[] loop(int style) {
        return loop(style, false);
    }

    /**
     * The whole loop as 16-bit mono PCM, peak-normalised then trimmed to sit under the SFX.
     *
     * The frenzy variant is the same synth voice and the same progression, but faster,
     * straightened out of its swing, and driven by a four-on-the-floor kick with offbeat
     * hats — so it reads as the same music under pressure rather than as a different track.
     */
    static short[] loop(int style, boolean frenzy) {
        if (!isSynth(style)) style = SWING_STYLE;
        float swing = frenzy ? 0.5f : STYLE[style][1];
        float bright = STYLE[style][2];
        float bassAmp = STYLE[style][3];
        float leadAmp = STYLE[style][4];
        float leadDecay = STYLE[style][5];

        int n = loopFrames(style, frenzy);
        float[] v = new float[n];
        float spb = beat(style) / (frenzy ? FRENZY_TEMPO : 1f);

        for (int bar = 0; bar < BARS; bar++) {
            int root = ROOT[bar];
            int[] voice = VOICING[bar];

            for (int b = 0; b < 4; b++) {
                float t0 = (bar * 4 + b) * spb;

                // Walking bass: every beat when swinging, on 1 and 3 for the slow drift.
                int[] walk = {0, 7, 0, 4};
                boolean playBass = style != DRIFT || b % 2 == 0;
                if (playBass) {
                    addVoice(v, t0, spb * (style == DRIFT ? 1.7f : 0.92f),
                            note(root + walk[b] - 12), bassAmp, 2.6f, bright * 0.9f);
                }

                // Comping stabs on the (optionally swung) off-beat.
                float tOff = t0 + spb * swing;
                for (int k = 0; k < voice.length; k++) {
                    addVoice(v, tOff, spb * (style == DRIFT ? 1.1f : 0.34f),
                            note(root + voice[k] + 12), style == DRIFT ? 0.07f : 0.11f,
                            style == DRIFT ? 2.2f : 7.5f, bright * 1.6f);
                }

                if (frenzy) {
                    // Four on the floor: a kick on every beat, hats on every eighth.
                    addKick(v, t0, spb * 0.55f);
                    addNoise(v, t0, 0.030f, 0.06f, bar * 4 + b);
                    addNoise(v, t0 + spb * 0.5f, 0.026f, 0.10f, 128 + bar * 4 + b);
                } else {
                    // Percussion: shaker for swing/drift, a crisper tick for the march.
                    addNoise(v, t0, 0.045f, style == MARCH ? 0.09f : 0.05f, bar * 4 + b);
                    if (style != DRIFT) {
                        addNoise(v, tOff, 0.035f, 0.07f, 64 + bar * 4 + b);
                    }
                }
            }
        }

        for (int m = 0; m < MELODY.length; m++) {
            float startBeat = MELODY[m][0];
            // The slow drift plays every other melody note, so it breathes.
            if (style == DRIFT && m % 2 == 1) continue;
            int bar = Math.min(BARS - 1, (int) (startBeat / 4));
            float freq = note(ROOT[bar] + (int) MELODY[m][1] + 12);
            addVoice(v, startBeat * spb, MELODY[m][2] * spb * (style == DRIFT ? 1.8f : 0.9f),
                    freq, leadAmp, leadDecay, bright);
        }

        return render(v);
    }

    /** Equal-tempered frequency, {@code semis} semitones above A2 (110 Hz). */
    private static float note(int semis) {
        return 110f * (float) Math.pow(2.0, semis / 12.0);
    }

    /**
     * Adds one voice with a Moog-ish character: a soft sawtooth built from a few harmonics,
     * rolled off by a decaying lowpass so each note opens bright and closes warm, with a
     * touch of vibrato. Wraps past the end of the buffer back to the start, so a note whose
     * tail crosses the loop point lands where the loop resumes instead of clicking.
     */
    private static void addVoice(float[] v, float start, float dur, float freq, float amp,
            float decay, float bright) {
        int i0 = (int) (start * Sfx.RATE);
        int len = (int) (dur * Sfx.RATE);
        float lp = 0f;
        for (int i = 0; i < len; i++) {
            float t = (float) i / len;
            // Gentle vibrato, as an analogue lead would have.
            float vib = 1f + 0.006f * (float) Math.sin(2 * Math.PI * 5.5f * i / Sfx.RATE);
            double ph = 2 * Math.PI * freq * vib * i / Sfx.RATE;

            // Band-limited saw: harmonics at 1/n, which is warmer than a raw ramp.
            float s = 0f;
            for (int k = 1; k <= 6; k++) {
                s += (float) Math.sin(ph * k) / k * (k == 1 ? 1f : bright);
            }

            // Filter envelope: opens on the attack, closes as the note decays.
            float cut = 0.16f + 0.55f * bright * (float) Math.exp(-3.5f * t);
            lp += (s - lp) * Math.min(1f, cut);

            float env = (t < 0.012f ? t / 0.012f : 1f) * (float) Math.exp(-decay * t);
            v[(i0 + i) % v.length] += lp * env * amp;
        }
    }

    /**
     * Four-on-the-floor kick: a short sine whose pitch drops steeply, which is what gives a
     * kick its thump rather than a tone.
     */
    private static void addKick(float[] v, float start, float dur) {
        int i0 = (int) (start * Sfx.RATE);
        int len = (int) (dur * Sfx.RATE);
        float phase = 0f;
        for (int i = 0; i < len; i++) {
            float t = (float) i / len;
            float f = 120f * (float) Math.exp(-7f * t) + 42f;
            phase += 2f * (float) Math.PI * f / Sfx.RATE;
            float env = (t < 0.006f ? t / 0.006f : 1f) * (float) Math.exp(-7.5f * t);
            v[(i0 + i) % v.length] += (float) Math.sin(phase) * env * 0.85f;
        }
    }

    private static void addNoise(float[] v, float start, float dur, float amp, int seed) {
        int i0 = (int) (start * Sfx.RATE);
        int len = (int) (dur * Sfx.RATE);
        int s = 22222 + seed * 7919;
        float hp = 0f;
        for (int i = 0; i < len; i++) {
            float t = (float) i / len;
            s = s * 1103515245 + 12345;
            float white = ((s >> 16) & 0x7FFF) / 16383.5f - 1f;
            hp = white - hp * 0.25f;                       // crude highpass: a shaker hiss
            v[(i0 + i) % v.length] += hp * amp * (float) Math.exp(-14f * t);
        }
    }

    /** Peak-normalised like the effects, then trimmed so it sits under them in the mix. */
    private static short[] render(float[] v) {
        float max = 0f;
        for (int i = 0; i < v.length; i++) {
            float a = Math.abs(v[i]);
            if (a > max) max = a;
        }
        float gain = max > 1e-6f ? (Sfx.PEAK * 0.42f) / max : 0f;
        short[] out = new short[v.length];
        for (int i = 0; i < v.length; i++) out[i] = (short) (v[i] * gain * 32767f);
        return out;
    }
}
