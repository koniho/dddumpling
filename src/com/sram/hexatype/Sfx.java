package com.sram.hexatype;

/**
 * Procedurally generated sound effects, as 16-bit mono PCM.
 *
 * Synthesised rather than sampled: no audio assets to license or ship, the APK stays tiny,
 * every sound can be pitched per character, and — like the rest of the game — it is pure
 * Java, so the harness can render the same buffers to WAV files and audition them without
 * building or installing anything.
 *
 * Every effect is peak-normalised to {@link #PEAK} by {@link #render}, so nothing is
 * louder than anything else by accident.
 */
final class Sfx {

    static final int RATE = 22050;

    /** Common peak level for every effect, as a fraction of full scale. */
    static final float PEAK = 0.85f;

    /** Sound ids, one preloaded buffer each. */
    static final int SQUISH_0 = 0, DRIP = 6, CLEAR = 7, WRONG = 8, ACHIEVEMENT = 9;
    static final int COUNT = 10;

    private Sfx() {}

    static short[] build(int id) {
        if (id >= SQUISH_0 && id < SQUISH_0 + Glyph.COUNT) return squish(id - SQUISH_0);
        switch (id) {
            case DRIP: return drip();
            case CLEAR: return clear();
            case WRONG: return wrong();
            default: return achievement();
        }
    }

    /**
     * Cute squishy blip for a correct press, pitched per character so the six letters are
     * audibly distinct and typing a word has a little melody to it.
     *
     * A squish is a fast downward pitch sweep with a wet, slightly noisy body: the sweep
     * reads as something soft deforming, the noise layer as the moisture. The small upward
     * flick at the tail is what stops it sounding like a plain synth "boop".
     */
    static short[] squish(int glyph) {
        int n = (int) (RATE * 0.115f);
        float[] v = new float[n];
        float base = 520f * (float) Math.pow(1.09f, glyph);
        float phase = 0f, wetState = 0f;
        int seed = 12345 + glyph * 7717;

        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            float f = base * (1f - 0.55f * t) * (1f + 0.30f * t * t);
            phase += 2f * (float) Math.PI * f / RATE;

            // Triangle-ish body: softer than a square, more present than a pure sine.
            float body = (float) Math.sin(phase) * 0.72f + (float) Math.sin(phase * 2f) * 0.18f;

            seed = seed * 1103515245 + 12345;
            float white = ((seed >> 16) & 0x7FFF) / 16383.5f - 1f;
            wetState += (white - wetState) * 0.35f;        // cheap one-pole lowpass
            float wet = wetState * 0.22f * (float) Math.exp(-9f * t);

            v[i] = (body + wet) * envelope(t, 0.012f, 3.4f);
        }
        return render(v);
    }

    /**
     * Water drip for taking damage: a wet upward-pitched plink over a low body, with a
     * splash transient at the front. The rising sweep is the trick — falling sweeps read as
     * bloops, rising ones read as a droplet hitting a surface.
     */
    static short[] drip() {
        int n = (int) (RATE * 0.42f);
        float[] v = new float[n];
        float phase = 0f, subPhase = 0f, splashState = 0f;
        int seed = 987654321;

        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            float rise = Math.min(1f, t / 0.18f);
            phase += 2f * (float) Math.PI * (380f + 900f * rise * rise) / RATE;
            subPhase += 2f * (float) Math.PI * (150f - 60f * t) / RATE;

            float plink = (float) Math.sin(phase) * (float) Math.exp(-7f * t);
            float sub = (float) Math.sin(subPhase) * 0.5f * (float) Math.exp(-4.5f * t);

            seed = seed * 1103515245 + 12345;
            float white = ((seed >> 16) & 0x7FFF) / 16383.5f - 1f;
            splashState += (white - splashState) * 0.5f;
            float splash = splashState * 0.5f * (float) Math.exp(-45f * t);

            v[i] = (plink + sub + splash) * envelope(t, 0.004f, 1.6f);
        }
        return render(v);
    }

    /** Bright rising arpeggio for clearing a word. */
    static short[] clear() {
        int n = (int) (RATE * 0.30f);
        float[] v = new float[n];
        float[] notes = {660f, 880f, 1174f};
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            int step = Math.min(notes.length - 1, (int) (t * notes.length));
            float local = t * notes.length - step;
            float s = (float) Math.sin(2f * Math.PI * notes[step] * i / RATE)
                    + 0.3f * (float) Math.sin(4f * Math.PI * notes[step] * i / RATE);
            v[i] = s * (float) Math.exp(-4f * local) * envelope(t, 0.006f, 0.6f);
        }
        return render(v);
    }

    /** Dull thunk for a wrong press: unpleasant, but not harsh. */
    static short[] wrong() {
        int n = (int) (RATE * 0.16f);
        float[] v = new float[n];
        float phase = 0f;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            phase += 2f * (float) Math.PI * (190f - 90f * t) / RATE;
            v[i] = ((float) Math.sin(phase) + 0.35f * (float) Math.sin(phase * 1.5f))
                    * envelope(t, 0.005f, 5.5f);
        }
        return render(v);
    }

    /** Achievement flourish for a flawless wave: a rising shimmer over a held fifth. */
    static short[] achievement() {
        int n = (int) (RATE * 0.85f);
        float[] v = new float[n];
        float[] notes = {523f, 659f, 784f, 1047f, 1319f, 1568f};
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            float s = 0f;
            for (int k = 0; k < notes.length; k++) {
                float start = k * 0.085f;
                if (t < start) continue;
                float local = t - start;
                s += (float) Math.sin(2f * Math.PI * notes[k] * i / RATE)
                        * (float) Math.exp(-2.9f * local) * 0.40f;
                // Octave shimmer on top of each note.
                s += (float) Math.sin(4f * Math.PI * notes[k] * i / RATE)
                        * (float) Math.exp(-5.5f * local) * 0.12f;
            }
            // Sustained low fifth underneath so it lands as a reward, not a beep.
            s += (float) Math.sin(2f * Math.PI * 261f * i / RATE) * 0.30f
                    * (float) Math.exp(-1.5f * t);
            v[i] = s * envelope(t, 0.010f, 0.8f);
        }
        return render(v);
    }

    /** Attack ramp, cosine-smoothed in, exponential out. */
    private static float envelope(float t, float attack, float decay) {
        float a = t < attack ? 0.5f - 0.5f * (float) Math.cos(Math.PI * t / attack) : 1f;
        return a * (float) Math.exp(-decay * t);
    }

    /** Peak-normalises to {@link #PEAK} and converts to 16-bit, so nothing can clip. */
    private static short[] render(float[] v) {
        float max = 0f;
        for (int i = 0; i < v.length; i++) {
            float a = Math.abs(v[i]);
            if (a > max) max = a;
        }
        float gain = max > 1e-6f ? PEAK / max : 0f;
        short[] out = new short[v.length];
        for (int i = 0; i < v.length; i++) out[i] = (short) (v[i] * gain * 32767f);
        return out;
    }
}
