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
    static final int START = 10, STAGE_CLEAR = 11, POWER_CLEAR = 12, CHOP = 13, ZAP = 14;
    static final int COLLECT = 15, STAR = 16;
    static final int COUNT = 17;

    private Sfx() {}

    static short[] build(int id) {
        if (id >= SQUISH_0 && id < SQUISH_0 + Glyph.COUNT) return squish(id - SQUISH_0);
        switch (id) {
            case DRIP: return drip();
            case CLEAR: return clear();
            case WRONG: return wrong();
            case START: return start();
            case STAGE_CLEAR: return stageClear();
            case POWER_CLEAR: return powerClear();
            case CHOP: return chop();
            case ZAP: return zap();
            case COLLECT: return collect();
            case STAR: return star();
            default: return achievement();
        }
    }

    /** Welcoming triad on a new game: soft, unhurried, no shimmer. */
    static short[] start() {
        return arp(0.95f, new float[] {392f, 523f, 659f, 784f}, 0.13f, 2.4f, 0.10f, 0.26f);
    }

    /** Stage cleared: four quick steps up, brisk and matter-of-fact. */
    static short[] stageClear() {
        return arp(0.72f, new float[] {523f, 659f, 784f, 1047f}, 0.075f, 3.6f, 0.16f, 0.20f);
    }

    /**
     * A frenzy run to its end. The biggest of the three: a longer climb, heavier shimmer and
     * a sustained root under it, so it plainly outranks the ordinary stage-clear tone that it
     * replaces.
     */
    static short[] powerClear() {
        return arp(1.25f, new float[] {523f, 659f, 784f, 1047f, 1319f, 1568f, 2093f},
                0.085f, 2.4f, 0.30f, 0.38f);
    }

    /**
     * Rising arpeggio builder shared by the three announcement tones.
     *
     * @param step    seconds between note onsets
     * @param decay   per-note decay rate
     * @param shimmer level of the octave above each note
     * @param bass    level of a sustained root underneath
     */
    private static short[] arp(float len, float[] notes, float step, float decay, float shimmer,
            float bass) {
        int n = (int) (RATE * len);
        float[] v = new float[n];
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            float s = 0f;
            for (int k = 0; k < notes.length; k++) {
                float start = k * step;
                if (t * len < start) continue;
                float local = t * len - start;
                s += (float) Math.sin(2 * Math.PI * notes[k] * i / RATE)
                        * (float) Math.exp(-decay * local) * 0.40f;
                s += (float) Math.sin(4 * Math.PI * notes[k] * i / RATE)
                        * (float) Math.exp(-decay * 2f * local) * shimmer;
            }
            s += (float) Math.sin(2 * Math.PI * notes[0] * 0.5f * i / RATE) * bass
                    * (float) Math.exp(-1.4f * t * len);
            v[i] = s * envelope(t, 0.010f, 0.5f);
        }
        return render(v);
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

    /**
     * A light chop, for a letter cut by the FLING blade.
     *
     * Short and dry: this fires several times per swipe, so anything with a tail on it would
     * smear into a wash. Bandpassed noise with a fast downward sweep on the filter — the sweep
     * is what makes it read as a blade passing through rather than as a click.
     */
    static short[] chop() {
        int n = (int) (RATE * 0.072f);
        float[] v = new float[n];
        int seed = 987654321;
        float lo = 0f, hi = 0f, phase = 0f, deep = 0f;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            seed = seed * 1103515245 + 12345;
            float white = ((seed >> 16) & 0x7FFF) / 16383.5f - 1f;

            // Two one-pole filters in series make a cheap bandpass: lowpass the noise, then
            // subtract a slower lowpass to take the bottom out of it. The sweep on the first is
            // what makes it read as a blade passing through rather than as a click.
            float cut = 0.52f - 0.34f * t;
            lo += (white - lo) * cut;
            hi += (lo - hi) * 0.05f;
            float air = (lo - hi) * 0.34f;

            // The body: a mid tone dropping fast with a softer one an octave and a half under
            // it. Without this the chop was air and nothing else — audibly a hiss, not a cut.
            phase += 2f * (float) Math.PI * (620f - 420f * t) / RATE;
            deep += 2f * (float) Math.PI * (250f - 130f * t) / RATE;
            float body = (float) Math.sin(phase) * 0.78f * (float) Math.exp(-6f * t)
                    + (float) Math.sin(deep) * 0.64f * (float) Math.exp(-3.5f * t);

            v[i] = (air + body) * envelope(t, 0.002f, 8.5f);
        }
        return render(v);
    }

    /**
     * A lightning crack, for one hop of a MULTI chain.
     *
     * Three parts, and it needs all three. A bright noise crack with essentially no attack, so it
     * arrives rather than fades in. A hard-edged oscillator swept down two octaves in a few
     * milliseconds for the electric zap — a plain sine there reads as a musical note, and the odd
     * harmonics are what give it an edge. And a low thump underneath, which is the part that makes
     * a hop land instead of fizz.
     *
     * Longer than the chop it sits beside, because {@link Audio} gives each effect one track and
     * restarts it: the hops of a chain arrive close enough together that each truncates the last,
     * so only the final hop rings out in full. That is the intended shape — a rattle of cracks,
     * then a tail.
     */
    static short[] zap() {
        int n = (int) (RATE * 0.13f);
        float[] v = new float[n];
        int seed = 24681357;
        float lo = 0f, hi = 0f, phase = 0f, thump = 0f;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            seed = seed * 1103515245 + 12345;
            float white = ((seed >> 16) & 0x7FFF) / 16383.5f - 1f;

            lo += (white - lo) * (0.92f - 0.55f * t);
            hi += (lo - hi) * 0.10f;
            float crack = (lo - hi) * 1.05f * (float) Math.exp(-13f * t);

            phase += 2f * (float) Math.PI * (1750f * (float) Math.exp(-7f * t) + 120f) / RATE;
            float edge = (float) Math.sin(phase) + 0.45f * (float) Math.sin(phase * 3f)
                    + 0.22f * (float) Math.sin(phase * 5f);
            // Amplitude buzz at a rate that is no neat multiple of the sweep, so it stays
            // electrical instead of settling into a pitch.
            float buzz = 0.78f + 0.22f * (float) Math.sin(2f * (float) Math.PI * 63f * i / RATE);
            float zap = edge * 0.42f * buzz * (float) Math.exp(-9f * t);

            thump += 2f * (float) Math.PI * (95f - 35f * t) / RATE;
            float low = (float) Math.sin(thump) * 0.90f * (float) Math.exp(-11f * t);

            v[i] = (crack + zap + low) * envelope(t, 0.0008f, 5.5f);
        }
        return render(v);
    }

    /**
     * One of the run's dumplings being set down in the display case.
     *
     * A little glass chime with a knock under the front of it: the knock is the thing arriving on
     * a shelf, the chime is the glass it arrived in. Two partials a fifth apart rather than one,
     * because a single sine here is a beep and the interval is what reads as glass.
     *
     * Short, and it decays fast. A good run shelves several of these a tenth of a second apart,
     * and {@link Audio} gives each effect one track and restarts it — so a tail would be cut off
     * by the next landing anyway, and the part that got through would smear into the one after.
     * The same shape as the chain's crack, for the same reason.
     */
    static short[] collect() {
        int n = (int) (RATE * 0.15f);
        float[] v = new float[n];
        float phase = 0f, fifth = 0f, knock = 0f;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            // Both partials sag slightly as they ring, which is what a struck object does. Held
            // perfectly steady they sounded like a synth against the rest of the deck.
            phase += 2f * (float) Math.PI * (1180f - 90f * t) / RATE;
            fifth += 2f * (float) Math.PI * (1770f - 150f * t) / RATE;
            float ring = (float) Math.sin(phase) * (float) Math.exp(-7f * t)
                    + (float) Math.sin(fifth) * 0.45f * (float) Math.exp(-11f * t);

            // Gone almost before it is there: any longer and it is a drum, not a placement.
            knock += 2f * (float) Math.PI * (300f - 120f * t) / RATE;
            float tap = (float) Math.sin(knock) * 0.60f * (float) Math.exp(-38f * t);

            v[i] = (ring + tap) * envelope(t, 0.0015f, 4.5f);
        }
        return render(v);
    }

    /**
     * Taking a star: a bright two-note flick upward, over almost before it is there.
     *
     * The most repeated effect in the game — twenty of them inside five seconds, and the last few
     * less than a fifth of a second apart — so it is shorter than the shelving chime and decays
     * harder than anything except the chop. {@link Audio} pitches it up with the count, so what the
     * player hears over a course is one long ladder rather than the same note twenty times; the
     * buffer is therefore written at the bottom of that ladder.
     *
     * Two partials a fifth apart and a grace note a whole tone under the first, sounded for the
     * opening third only. That is what makes it read as a flick <em>up</em> to the note rather than
     * a plain ping, and it survives being resampled to a higher pitch, which a percussive tap does
     * not — it just turns into a tick.
     */
    static short[] star() {
        // Trimmed to what actually rings. At 115ms the last third was dead air, which costs nothing
        // to play but drags the buffer's zero-crossing rate down — and that rate is the harness's
        // stand-in for "is there a tone in here", so a sound padded with silence measures as a click.
        int n = (int) (RATE * 0.082f);
        float[] v = new float[n];
        float grace = 0f, note = 0f, fifth = 0f;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            grace += 2f * (float) Math.PI * 1245f / RATE;
            note += 2f * (float) Math.PI * 1480f / RATE;
            fifth += 2f * (float) Math.PI * 2217f / RATE;
            // The grace note is gone by a third of the way in; the note it flicks up to rings on.
            float lead = (float) Math.sin(grace) * 0.55f * (float) Math.exp(-18f * t);
            // Slow enough that the note rings through most of the buffer. The decays here are in
            // buffer-fractions, not seconds, so at -13 over 82ms the last third fell to nothing and
            // the whole thing measured as a click however short the buffer was made.
            float body = (float) Math.sin(note) * (float) Math.exp(-6.5f * t)
                    + (float) Math.sin(fifth) * 0.38f * (float) Math.exp(-10f * t);
            v[i] = (lead + body) * envelope(t, 0.0012f, 2.2f);
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
