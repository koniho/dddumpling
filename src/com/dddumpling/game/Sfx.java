package com.dddumpling.game;

/**
 * Sound effects as 16-bit mono PCM, shared by Android, iOS and the harness.
 * Most effects are synthesised; cart, rock and Octopulse cues embed recordings.
 * Recording provenance and regeneration: audio/recorded/README.md.
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
    static final int COURSE = 17, TALLY = 18, JOIN = 19, OVER = 20, BOSS_LAUGH = 21;
    static final int BOSS_DAMAGE = 22, BOSS_SPLIT = 23, BOLT_POP = 24;
    static final int DIVIDE_DAMAGE = 25, DIVIDE_SPLIT = 26;
    static final int DIVIDE_BOING_HEAVY = 27, DIVIDE_BOING_MEDIUM = 28,
            DIVIDE_BOING_LIGHT = 29, ROSTER_JOIN = 30, DIVIDE_DEACTIVATE = 31,
            SHIELD_BOUNCE = 32, SLIME_DAMAGE = 33, OCTO_CUE = 34, OCTO_LOCK = 35;
    static final int BOSS_TAUNT_0 = 36, BOLT_DEATH = BOSS_TAUNT_0 + Boss.COUNT,
            MUSHROOM_SHAKE = BOLT_DEATH + 1, MUSHROOM_SPORE = MUSHROOM_SHAKE + 1,
            LINKED_THUD = MUSHROOM_SPORE + 1, SHUFFLE_BLIP = LINKED_THUD + 1, DEBUFF_DOWN = SHUFFLE_BLIP + 1,
            SLIME_COVER = DEBUFF_DOWN + 1, SLIME_RELEASE = SLIME_COVER + 1,
            LAND_SHUFFLE = SLIME_RELEASE + 1, UI_BLOOP = LAND_SHUFFLE + 1, BLAST_OFF = UI_BLOOP + 1, DIVIDE_SUPERNOVA = BLAST_OFF + 1, OCTO_WAVE = DIVIDE_SUPERNOVA + 1, CAVE_RUMBLE = OCTO_WAVE + 1, CAVE_CRASH = CAVE_RUMBLE + 1,
            CAVE_AMBUSH = CAVE_CRASH + 1, CAVE_SINK = CAVE_AMBUSH + 1, MINING_CHEER = CAVE_SINK + 1,
            CART_ROLL = MINING_CHEER + 1, CART_SQUEAL = CART_ROLL + 1, CART_TUMBLE = CART_SQUEAL + 1, OCTO_DAMAGE = CART_TUMBLE + 1, COUNT = OCTO_DAMAGE + 1;
    static final float OCTO_WAVE_GAIN = .66f;

    private static final short[][] CACHE = new short[COUNT][];
    private static short[] rocketCache, bubbleCache;

    private Sfx() {}

    static synchronized short[] build(int id) {
        if (id < 0 || id >= COUNT) id = ACHIEVEMENT;
        if (CACHE[id] == null) CACHE[id] = renderEffect(id);
        return CACHE[id];
    }

    private static short[] renderEffect(int id) {
        if (id >= SQUISH_0 && id < SQUISH_0 + Glyph.COUNT) return squish(id - SQUISH_0);
        if (id >= BOSS_TAUNT_0 && id < BOSS_TAUNT_0 + Boss.COUNT)
            return bossTaunt(id - BOSS_TAUNT_0);
        if(id==CAVE_RUMBLE)return RockRecording.rumble();
        if(id==CAVE_CRASH)return RockRecording.crash();
        if(id>=CAVE_AMBUSH && id<=CAVE_SINK)return cave(id);
        if(id==MINING_CHEER)return miningCheer();
        if(id==CART_ROLL)return CartRecording.build();
        if(id>=CART_SQUEAL && id<=CART_TUMBLE)return cart(id);
        if (id == OCTO_WAVE) return OctoWaveRecording.build();
        switch (id) {
            case DRIP: return drip();
            case CLEAR: return clear();
            case WRONG: return wrong();
            case LINKED_THUD: return linkedThud();
            case SHUFFLE_BLIP: return shuffleBlip();
            case DEBUFF_DOWN: return debuffDown();
            case SLIME_COVER: return slimeCover(false);
            case SLIME_RELEASE: return slimeCover(true);
            case LAND_SHUFFLE: return landShuffle();
            case UI_BLOOP: return uiBloop();
            case START: return start();
            case STAGE_CLEAR: return stageClear();
            case POWER_CLEAR: return powerClear();
            case CHOP: return chop();
            case ZAP: return zap();
            case COLLECT: return collect();
            case STAR: return star();
            case COURSE: return course();
            case BLAST_OFF: return blastOff();
            case TALLY: return tally();
            case JOIN: return join();
            case OVER: return over();
            case BOSS_LAUGH: return bossLaugh();
            case BOSS_DAMAGE: return bossDamage();
            case BOSS_SPLIT: return bossSplit();
            case BOLT_POP: return boltPop();
            case BOLT_DEATH: return boltDeath();
            case MUSHROOM_SHAKE: return mushroomShake();
            case MUSHROOM_SPORE: return mushroomSpore();
            case DIVIDE_DAMAGE: return divideDamage();
            case DIVIDE_SPLIT: return divideSplit();
            case DIVIDE_BOING_HEAVY: return divideBoing(0);
            case DIVIDE_BOING_MEDIUM: return divideBoing(1);
            case DIVIDE_BOING_LIGHT: return divideBoing(2);
            case ROSTER_JOIN: return rosterJoin();
            case DIVIDE_DEACTIVATE: return divideDeactivate();
            case DIVIDE_SUPERNOVA: return divideSupernova();
            case SHIELD_BOUNCE: return shieldBounce();
            case SLIME_DAMAGE: return slimeDamage();
            case OCTO_DAMAGE: return octoDamage();
            case OCTO_CUE: return octoCue();
            case OCTO_LOCK: return octoLock();
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

    /**
     * The star course leaving: a rising whoosh under a swept tone, for the frame the ready lesson
     * ends and the checkpoints start coming.
     *
     * The one effect here that has to <em>grow</em> rather than decay, because it announces a
     * standstill turning into motion — see {@code StarPath.EASE_IN}, which is the second of
     * acceleration this is the sound of. So the envelope is deliberately back-heavy: the peak
     * lands about two thirds through, where the course is up to pace, and what precedes it is
     * mostly air. A decaying whoosh reads as something stopping.
     */
    static short[] course() {
        int n = (int) (RATE * 0.52f);
        float[] v = new float[n];
        int seed = 77345621;
        float lo = 0f, hi = 0f, phase = 0f, sub = 0f;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            seed = seed * 1103515245 + 12345;
            float white = ((seed >> 16) & 0x7FFF) / 16383.5f - 1f;

            // Bandpass opening upward: the air going by. Both poles climb, so the band travels
            // rather than merely brightening, which is what makes it a rush past rather than a hiss
            // getting louder.
            lo += (white - lo) * (0.06f + 0.40f * t * t);
            hi += (lo - hi) * (0.01f + 0.10f * t);
            float air = (lo - hi) * 1.5f;

            // A fifth swept up under it, so there is a pitch to follow and not only noise.
            phase += 2f * (float) Math.PI * (240f + 520f * t * t) / RATE;
            sub += 2f * (float) Math.PI * (160f + 300f * t * t) / RATE;
            float tone = ((float) Math.sin(phase) * 0.5f + (float) Math.sin(sub) * 0.34f)
                    * (0.25f + 0.75f * t);

            // Back-heavy envelope with the tail cut short: a long fade out would still be sounding
            // once the first checkpoints are arriving, and those have their own note.
            float swell = t < 0.72f ? t / 0.72f : Math.max(0f, (1f - t) / 0.28f);
            v[i] = (air + tone) * swell * swell;
        }
        return render(v);
    }

    /**
     * The tally at the end of an interlude nobody won: a soft two-note chime, the sound of a number
     * being read out rather than a prize.
     *
     * Deliberately unexciting and deliberately not the fanfare — both interludes end on a count, and
     * before this the count arrived in silence, which read as the game having lost interest. A fourth
     * apart and both partials sagging, so it is plainly related to the shelving chime and plainly
     * smaller. {@link Audio} pitches it with what was actually collected, so a near miss sounds
     * closer to a win than a poor attempt does.
     */
    static short[] tally() {
        int n = (int) (RATE * 0.30f);
        float[] v = new float[n];
        float phase = 0f, fourth = 0f;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            phase += 2f * (float) Math.PI * (620f - 40f * t) / RATE;
            fourth += 2f * (float) Math.PI * (830f - 60f * t) / RATE;
            // The upper partial comes in a third of the way through, which is what makes it read
            // as two notes said in order rather than one chord.
            float second = t < 0.30f ? 0f : (float) Math.exp(-5.5f * (t - 0.30f));
            float ring = (float) Math.sin(phase) * (float) Math.exp(-4.5f * t)
                    + (float) Math.sin(fourth) * 0.55f * second;
            v[i] = ring * envelope(t, 0.008f, 1.6f);
        }
        return render(v);
    }

    /**
     * The new dumpling taking its place in the parade line: a warm major chord, all three notes
     * together, with a soft attack.
     *
     * Struck together rather than rolled, unlike every other announcement here — the parade already
     * has an arpeggio's worth of movement in it, and one chord under the moment it lands is the
     * punctuation it was missing. The slow attack is what keeps it from sounding like the fanfare
     * a second time.
     */
    static short[] join() {
        int n = (int) (RATE * 0.62f);
        float[] v = new float[n];
        float[] notes = {392f, 494f, 587f, 784f};
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            float s = 0f;
            for (int k = 0; k < notes.length; k++) {
                s += (float) Math.sin(2f * Math.PI * notes[k] * i / RATE)
                        * (float) Math.exp(-2.2f * t) * (k == 3 ? 0.22f : 0.44f);
            }
            v[i] = s * envelope(t, 0.055f, 0.9f);
        }
        return render(v);
    }

    /** E5 down chromatically to B-flat, then a final defeated drop to G. */
    static final float[] OVER_NOTES = {659.25f, 622.25f, 587.33f, 554.37f,
            523.25f, 493.88f, 466.16f, 392.00f};

    /**
     * The end of a run: a chromatic-ish falling melody, and the only descending figure in the game.
     *
     * Every other announcement climbs, so the close semitone steps read immediately as the
     * opposite. The last minor-third drop gives the phrase a destination instead of sounding like a
     * scale exercise, while the quiet pedal underneath keeps it warm rather than buzzer-like.
     */
    static short[] over() {
        return arp(1.55f, OVER_NOTES, 0.16f, 2.4f, 0.07f, 0.14f);
    }

    /** Four short victory voices, one for each active boss. */
    static short[] bossTaunt(int kind) {
        float[] base = {330f, 275f, 150f, 205f};
        float[][] shape = {
                {1f, 1.26f, 0.92f, 1.38f},       // slime: bubbly cackle
                {1f, 0.71f, 1.41f, 0.59f},       // divide: split, opposed pitches
                {1f, 1.06f, 0.89f, 1.12f},       // octopus: close writhing warble
                {1f, 0.76f, 1.34f, 0.63f}        // mushroom: hollow spore cough
        };
        kind = Math.max(0, Math.min(Boss.COUNT - 1, kind));
        int n = (int) (RATE * 0.82f);
        float[] v = new float[n];
        int seed = 0x7a17 + kind * 7919;
        for (int k = 0; k < shape[kind].length; k++) {
            int at = (int) (RATE * (0.035f + k * (0.14f)));
            int len = (int) (RATE * (0.22f));
            float phase = 0f;
            for (int j = 0; j < len && at + j < n; j++) {
                float u = j / (float) len;
                float wobble = kind == Boss.OCTOPUS ? 1f + 0.08f * (float) Math.sin(u * 8f * Math.PI)
                        : kind == Boss.SLIME ? 1f + 0.12f * u : 1f - 0.04f * u;
                phase += TAU * base[kind] * shape[kind][k] * wobble / RATE;
                seed = seed * 1103515245 + 12345;
                float noise = ((seed >>> 16) & 0x7fff) / 16383.5f - 1f;
                float tone = (float) Math.sin(phase)
                        + (0.18f) * (float) Math.sin(phase * 2.01f);
                float grit = (kind == Boss.SPLITTER ? 0.24f : 0.04f)
                        * noise;
                v[at + j] += (tone + grit) * (float) Math.sin(Math.PI * u)
                        * (0.72f - k * 0.07f);
            }
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

    /** A short, low padded impact for a bond rejecting an incomplete chord. */
    static short[] linkedThud() {
        int n = (int)(RATE*0.14f);
        float[] v = new float[n];
        float phase = 0f;
        for (int i = 0; i < n; i++) {
            float t = i/(float)n;
            phase += 2f*(float)Math.PI*(65f+70f*(float)Math.exp(-t*10f))/RATE;
            float attack = Math.min(1f, t/0.025f);
            v[i] = attack*(float)Math.exp(-t*7f)
                    *((float)Math.sin(phase)+0.20f*(float)Math.sin(phase*2.13f));
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

    /**
     * Seamless, light rocket bed. Every component completes a whole number of cycles in this
     * one-second loop, so raising its playback rate during flight does not introduce a seam.
     */
    static synchronized short[] rocket() {
        if (rocketCache == null) rocketCache = renderRocket();
        return rocketCache;
    }

    /** Ignition punch and rising exhaust, fading before the climb-out ends. */
    private static short[] blastOff() {
        float[] v = new float[(int) (RATE * 0.65f)];
        java.util.Random rng = new java.util.Random(731L);
        float air = 0f;
        for (int i = 0; i < v.length; i++) {
            float t = (float) i / RATE, u = t / 0.65f;
            air += 0.24f * (rng.nextFloat() * 2f - 1f - air);
            float phase = TAU * (95f * t + 340f * t * t);
            float body = (float) Math.sin(phase) * 0.55f
                    + (float) Math.sin(phase * 1.51f) * 0.18f;
            float env = Math.min(1f, t / 0.012f) * (float) Math.pow(1f - u, 2.3f);
            v[i] = (body + air * 1.4f) * env;
        }
        return render(v);
    }

    private static short[] renderRocket() {
        int n = RATE;
        float[] v = new float[n];
        for (int i = 0; i < n; i++) {
            float t = (float) i / RATE;
            float rumble = (float) Math.sin(TAU * 86f * t) * 0.38f
                    + (float) Math.sin(TAU * 127f * t) * 0.28f;
            float air = (float) Math.sin(TAU * 191f * t + 0.7f) * 0.21f
                    + (float) Math.sin(TAU * 283f * t + 1.9f) * 0.15f
                    + (float) Math.sin(TAU * 421f * t + 0.2f) * 0.09f;
            float flutter = 0.78f + 0.22f * (float) Math.sin(TAU * 7f * t);
            v[i] = (rumble + air) * flutter;
        }
        return render(v);
    }

    private static final float TAU = 6.2831853f;

    /** Seamless wet bubble bed; playback rate and volume supply the charging build. */
    static synchronized short[] bubble() {
        if (bubbleCache == null) bubbleCache = renderBubble();
        return bubbleCache;
    }

    private static short[] renderBubble() {
        int n = (int) (RATE * 0.98f);
        float[] v = new float[n];
        float[] start = {0.03f, 0.28f, 0.54f, 0.76f};
        for (int i = 0; i < n; i++) {
            float t = (float) i / RATE;
            float s = 0f;
            for (int k = 0; k < start.length; k++) {
                float q = t - start[k];
                if (q < 0f || q > 0.18f) continue;
                float u = q / 0.18f;
                float phase = TAU * (92f * q + 48f * q * q);
                float wet = (float) Math.sin(phase) + 0.34f * (float) Math.sin(phase * 2.03f);
                s += wet * (float) Math.sin(Math.PI * u) * (1f - u * 0.35f);
            }
            v[i] = s * 0.62f;
        }
        return render(v);
    }

    /** A low, wobbling three-beat cackle for the slime launching a volley. */
    static short[] bossLaugh() {
        int n = (int) (RATE * 0.72f);
        float[] v = new float[n];
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            float beat = t * 3f - (int) (t * 3f);
            float gate = (float) Math.exp(-5f * beat);
            float f = 145f - 35f * t + 18f * (float) Math.sin(TAU * t * 3f);
            float ph = TAU * f * i / RATE;
            float voice = (float) Math.sin(ph) + 0.42f * (float) Math.sin(ph * 2.02f)
                    + 0.18f * (float) Math.sin(ph * 3.07f);
            float bloops = 0f;
            for (int k = 0; k < 3; k++) {
                float q = t * 0.72f - 0.10f - k * 0.17f;
                if (q < 0f || q > 0.16f) continue;
                float u = q / 0.16f;
                float bp = TAU * (105f * q - 42f * q * q);
                bloops += ((float) Math.sin(bp) + 0.5f * (float) Math.sin(bp * 2f))
                        * (float) Math.sin(Math.PI * u);
            }
            v[i] = (voice * gate * 0.58f + bloops * 0.82f)
                    * envelope(t, 0.008f, 1.4f);
        }
        return render(v);
    }

    /** Three round descending bloops for a damaging boss hit. */
    static short[] bossDamage() {
        int n = (int) (RATE * 0.52f);
        float[] v = new float[n];
        float[] start = {0f, 0.105f, 0.215f};
        float[] pitch = {255f, 195f, 142f};
        for (int k = 0; k < start.length; k++) {
            int at = (int) (start[k] * RATE);
            int len = (int) (RATE * 0.24f);
            float phase = 0f;
            for (int j = 0; j < len && at + j < n; j++) {
                float u = (float) j / len;
                float f = pitch[k] * (1f - 0.34f * u);
                phase += TAU * f / RATE;
                float round = (float) Math.sin(phase)
                        + 0.24f * (float) Math.sin(phase * 2.01f);
                float env = (float) Math.sin(Math.PI * u) * (1f - u * 0.28f);
                v[at + j] += round * env * (0.82f - k * 0.10f);
            }
        }
        return render(v);
    }

    /** A destroyed boss bolt: a compact space explosion, punch then sparkling debris. */
    static short[] boltPop() {
        int n = (int) (RATE * 0.22f);
        float[] v = new float[n];
        int seed = 0x51A7B00;
        float phase = 0f;
        for (int i = 0; i < n; i++) {
            float u = (float) i / n;
            seed = seed * 1664525 + 1013904223;
            float noise = ((seed >>> 9) & 0x7fffff) / 4194303.5f - 1f;
            float hz = 230f - 155f * u;
            phase += TAU * hz / RATE;
            float core = ((float) Math.sin(phase) + 0.32f * (float) Math.sin(phase * 1.97f))
                    * (float) Math.exp(-5.8f * u);
            float blast = noise * (float) Math.exp(-15f * u);
            float debris = noise * (0.18f + 0.16f * (float) Math.sin(TAU * 17f * u))
                    * (float) Math.exp(-4.5f * u);
            v[i] = (core * 0.78f + blast * 0.88f + debris) * envelope(u, 0.0015f, 1.2f);
        }
        return render(v);
    }

    /** A launched bolt's unmistakable final break: hard shell snap, falling core and debris. */
    static short[] boltDeath() {
        int n = (int) (RATE * 0.34f);
        float[] v = new float[n];
        int seed = 0xB017D1E;
        float phase = 0f;
        for (int i = 0; i < n; i++) {
            float u = i / (float) n;
            seed = seed * 1664525 + 1013904223;
            float noise = ((seed >>> 9) & 0x7fffff) / 4194303.5f - 1f;
            float hz = 410f * (1f - 0.72f * u);
            phase += TAU * hz / RATE;
            float snap = noise * (float) Math.exp(-42f * u);
            float core = ((float) Math.sin(phase) + 0.30f * (float) Math.sin(phase * 2.03f))
                    * (float) Math.exp(-5.2f * u);
            float shards = noise * (float) Math.exp(-8f * u)
                    * (0.18f + 0.12f * (float) Math.sin(TAU * 23f * u));
            v[i] = (snap * 1.05f + core * 0.80f + shards) * envelope(u, 0.0015f, 1.2f);
        }
        return render(v);
    }

    /** A handful of tiny rising bubbles for a glob successfully carried free. */
    static short[] mushroomShake() {
        int n = (int) (RATE * 0.13f);
        float[] v = new float[n];
        int seed = 0x5A4CE;
        float phase = 0f;
        for (int i = 0; i < n; i++) {
            float u = i / (float) n;
            seed = seed * 1664525 + 1013904223;
            float noise = ((seed >>> 9) & 0x7fffff) / 4194303.5f - 1f;
            phase += TAU * (760f - 230f * u) / RATE;
            float woody = (float) Math.sin(phase) * (float) Math.exp(-7f * u);
            float spores = noise * (float) Math.exp(-18f * u);
            v[i] = (woody * 0.76f + spores * 0.48f) * envelope(u, 0.003f, 1.4f);
        }
        return render(v);
    }

    /** A dry, airy seed-shaker sweep for Fly Agaric releasing a volley. */
    static short[] mushroomSpore() {
        int n = (int) (RATE * 0.46f);
        float[] v = new float[n];
        int seed = 0x5F0AE;
        for (int i = 0; i < n; i++) {
            float u = i / (float) n;
            seed = seed * 1664525 + 1013904223;
            float white = ((seed >>> 9) & 0x7fffff) / 4194303.5f - 1f;
            float grains = 0f;
            for (int k = 0; k < 7; k++) {
                float centre = 0.045f + k * 0.052f;
                float d = (u * 0.46f - centre) / 0.012f;
                grains += (float) Math.exp(-d * d) * (0.72f + 0.28f * (k % 2));
            }
            float airy = white * (0.18f + grains) * (1f - 0.55f * u);
            v[i] = airy * envelope(u, 0.006f, 1.7f);
        }
        return render(v);
    }

    /** Four soft scuffs under the Adventure Dumpling's short walk. */
    static short[] landShuffle() {
        float[] v=new float[(int)(RATE*0.70f)];
        int seed=0x1A4D;
        float soft=0f;
        for(int k=0;k<4;k++) {
            int start=(int)((0.12f+k*0.13f)*RATE),length=(int)(0.12f*RATE);
            for(int j=0;j<length && start+j<v.length;j++) {
                float u=j/(float)length;
                seed=seed*1664525+1013904223;
                float noise=((seed>>>9)&0x7FFFFF)/4194303.5f-1f;
                soft+=(noise-soft)*0.18f;
                float thump=(float)Math.sin(TAU*(145f+k%2*20f)*j/RATE);
                v[start+j]+=(soft*0.75f+thump*0.18f)*(float)Math.sin(Math.PI*u)*(float)Math.exp(-3f*u);
            }
        }
        return render(v);
    }

    /** Three rounded bubbles, rising into the fold and falling back out on release. */
    static short[] slimeCover(boolean release) {
        float[] v=new float[(int)(RATE*0.30f)];
        float[] notes={260f,360f,490f};
        for(int k=0;k<3;k++) {
            int start=(int)(k*0.085f*RATE), length=(int)(0.13f*RATE);
            float phase=0f,base=notes[release ? 2-k : k];
            for(int j=0;j<length && start+j<v.length;j++) {
                float u=j/(float)length;
                phase+=TAU*base*(1f+0.35f*(float)Math.sin(Math.PI*u))/RATE;
                float bubble=(float)Math.sin(phase)+0.18f*(float)Math.sin(phase*2f);
                v[start+j]+=bubble*(float)Math.sin(Math.PI*u)*(float)Math.exp(-2f*u);
            }
        }
        return render(v);
    }

    /** Tiny rounded tick, short enough to leave space between roulette changes. */
    static short[] uiBloop() {
        float[] v=new float[(int)(RATE*.11f)];float phase=0f;
        for(int i=0;i<v.length;i++) {
            float u=i/(float)v.length;
            phase+=TAU*(540f-350f*u)/RATE;
            v[i]=(float)Math.sin(phase)*envelope(u,.04f,4f)*(1f-u);
        }
        return render(v);
    }

    static short[] shuffleBlip() { return sweepTone(0.035f, 1050f, 850f, 1f); }

    /** Playful falling slide announcing a temporary debuff. */
    static short[] debuffDown() {
        int n = (int) (RATE * 0.48f);
        float[] v = new float[n];
        float phase = 0f;
        for (int i = 0; i < n; i++) {
            float u = i / (float) n;
            phase += TAU * (760f * (float) Math.pow(0.25f, u)) / RATE;
            float body = (float) Math.sin(phase) + 0.22f * (float) Math.sin(phase * 2f);
            v[i] = body * envelope(u, 0.025f, 1.5f) * Math.min(1f, (1f-u)*12f);
        }
        return render(v);
    }

    static short[] octoCue() { return sweepTone(0.16f, 720f, 980f, 0.72f); }
    static short[] octoLock() { return sweepTone(0.28f, 330f, 125f, 0.88f); }

    private static short[] sweepTone(float seconds, float from, float to, float gain) {
        int n = (int) (RATE * seconds); float[] v = new float[n]; float phase = 0f;
        for (int i = 0; i < n; i++) { float u = i / (float) n; phase += TAU * (from + (to - from) * u) / RATE; v[i] = (float) Math.sin(phase) * gain * envelope(u, 0.008f, 1.8f); }
        return render(v);
    }

    static short[] slimeDamage() {
        int n = (int) (RATE * 0.38f);
        float[] v = new float[n];
        float[] start = {0f, 0.070f, 0.145f, 0.225f};
        float[] pitch = {510f, 630f, 755f, 910f};
        for (int k = 0; k < start.length; k++) {
            int at = (int) (start[k] * RATE);
            int len = (int) (RATE * 0.115f);
            float phase = 0f;
            for (int j = 0; j < len && at + j < n; j++) {
                float u = (float) j / len;
                float f = pitch[k] * (1f + 0.30f * u);
                phase += TAU * f / RATE;
                float bubble = (float) Math.sin(phase)
                        + 0.16f * (float) Math.sin(phase * 2.01f);
                float env = (float) Math.sin(Math.PI * u) * (1f - 0.35f * u);
                v[at + j] += bubble * env * (0.72f - k * 0.055f);
            }
        }
        return render(v);
    }

    /** Bright elastic ricochet: a hard shield ping followed by a quick falling boing. */
    static short[] shieldBounce() {
        int n = (int) (RATE * 0.24f);
        float[] v = new float[n];
        for (int i = 0; i < n; i++) {
            float t = (float) i / RATE, u = (float) i / n;
            float ping = (float) Math.sin(TAU * 1180f * t) * (float) Math.exp(-18f * u);
            float boing = (float) Math.sin(TAU * (430f * t - 170f * t * t))
                    * (float) Math.exp(-6f * u);
            v[i] = (ping * 0.72f + boing * 0.66f) * envelope(u, 0.002f, 1.7f);
        }
        return render(v);
    }

    /** A taut skin tear followed by the wet, springy pop of a glob coming free. */
    static short[] bossSplit() {
        int n = (int) (RATE * 0.40f);
        float[] v = new float[n];
        for (int i = 0; i < n; i++) {
            float t = (float) i / RATE, u = (float) i / n;
            float snap = i < RATE / 32 ? (float) Math.sin(i * 2.71f)
                    * (1f - i * 32f / RATE) : 0f;
            float phase = TAU * (235f * t - 165f * t * t);
            float pop = (float) Math.sin(phase) + 0.36f * (float) Math.sin(phase * 2.03f);
            float bounce = (float) Math.sin(TAU * (390f * t + 85f * t * t))
                    * (float) Math.exp(-9f * u);
            v[i] = (snap * 0.50f + pop * 0.82f + bounce * 0.28f)
                    * envelope(u, 0.006f, 1.8f);
        }
        return render(v);
    }

    /** A tight snap followed by a springy falling pitch as the arm recoils. */
    static short[] octoDamage() {
        int n = (int)(RATE * .34f), seed = 0x0c70;
        float[] v = new float[n];
        float phase = 0f;
        for (int i = 0; i < n; i++) {
            float t = (float)i / RATE, u = (float)i / n;
            float hz = 170f + 560f * (float)Math.exp(-17f*t)
                    + 110f * (float)Math.sin(TAU*23f*t) * (float)Math.exp(-12f*t);
            phase += TAU * hz / RATE;
            seed = seed * 1664525 + 1013904223;
            float noise = ((seed >>> 8) / 8388608f) - 1f;
            float snap = noise * .48f * (float)Math.exp(-150f*t);
            float spring = (float)Math.sin(phase) + .22f*(float)Math.sin(phase*2f);
            float tail = Math.min(1f, (n - 1 - i) / (RATE * .025f));
            v[i] = (spring + snap) * envelope(u,.008f,3.8f) * tail;
        }
        return render(v);
    }

    /** A rounded water-drop pitch bend, short enough for repeated key hits. */
    static short[] divideDamage() {
        int n = (int) (RATE * 0.19f);
        float[] v = new float[n];
        float phase = 0f;
        for (int i = 0; i < n; i++) {
            float t = (float) i / RATE, u = (float) i / n;
            float hz = 210f + 430f * (float) Math.exp(-32f * t);
            phase += TAU * hz / RATE;
            v[i] = ((float) Math.sin(phase) + 0.12f * (float) Math.sin(phase * 2f))
                    * envelope(u, 0.035f, 3.6f);
        }
        return render(v);
    }

    /** Two opposed cracks and a falling sub tone for the permanent divide. */
    static short[] divideSplit() {
        int n = (int) (RATE * 0.62f);
        float[] v = new float[n];
        int seed = 0x2f17;
        for (int i = 0; i < n; i++) {
            float t = (float) i / RATE, u = (float) i / n;
            seed = seed * 1103515245 + 12345;
            float grit = ((seed >>> 16) & 0x7fff) / 16383.5f - 1f;
            float cracks = grit * ((float) Math.exp(-48f * u)
                    + (u > 0.16f ? 0.75f * (float) Math.exp(-55f * (u - 0.16f)) : 0f));
            float sub = (float) Math.sin(TAU * (145f * t - 62f * t * t));
            v[i] = (cracks * 0.48f + sub * 0.86f) * envelope(u, 0.003f, 1.7f);
        }
        return render(v);
    }

    /** Dense alternating arcade bleeps and elastic bubbles, launched with the cube burst. */
    static short[] divideSupernova() {
        int n = (int) (RATE * 1.4f);
        float[] v = new float[n];
        float[] notes = {1047f, 262f, 1568f, 392f, 1319f, 330f, 2093f, 523f};
        for (int k = 0; k < 16; k++) {
            int start = (int) (RATE * k * .068f);
            boolean bleep = k % 2 == 0;
            int length = (int) (RATE * (bleep ? .15f : .28f));
            float phase = 0f, hz = notes[k % notes.length];
            for (int j = 0; j < length && start + j < n; j++) {
                float u = j / (float) length;
                float pitch = bleep ? hz * (1f + .18f * u)
                        : hz * (.48f + 1.4f * (float) Math.exp(-6f * u));
                phase += TAU * pitch / RATE;
                float wave = (float) Math.sin(phase)
                        + (bleep ? .22f : .10f) * (float) Math.sin(phase * (bleep ? 3f : 2f));
                v[start + j] += wave * envelope(u, .04f, bleep ? 3f : 4f)
                        * (1f - u) * (.85f - k * .025f);
            }
        }
        float phase = 0f;
        for (int i = 0; i < n; i++) {
            float t = i / (float) RATE;
            phase += TAU * (85f + 190f * (float) Math.exp(-18f * t)) / RATE;
            v[i] += (float) Math.sin(phase) * (float) Math.exp(-9f * t)
                    * Math.min(1f, t / .006f) * .65f;
        }
        return render(v);
    }

    /** A soft bubble popping and sinking away as the cube goes dormant. */
    static short[] divideDeactivate() {
        int n = (int) (RATE * 0.36f);
        float[] v = new float[n];
        float phase = 0f;
        for (int i = 0; i < n; i++) {
            float t = (float) i / RATE, u = (float) i / n;
            float hz = 105f + 620f * (float) Math.exp(-16f * t);
            phase += TAU * hz / RATE;
            v[i] = ((float) Math.sin(phase) + 0.16f * (float) Math.sin(phase * 2f))
                    * envelope(u, 0.025f, 3.2f);
        }
        return render(v);
    }

    /**
     * A rubbery collision voice for Dark Divide pieces. Successive sizes lose body and gain
     * spring: the large glob lands with a slow 112 Hz belly wobble, while the smallest rings at
     * 260 Hz. A rapid downward pitch bend gives all three the unmistakable cartoon "boing".
     */
    static short[] divideBoing(int size) {
        int tier = Math.max(0, Math.min(2, size));
        float[] base = {112f, 174f, 260f};
        float[] length = {0.34f, 0.28f, 0.23f};
        float[] bend = {82f, 116f, 168f};
        int n = (int) (RATE * length[tier]);
        float[] v = new float[n];
        float phase = 0f;
        for (int i = 0; i < n; i++) {
            float u = (float) i / n;
            float hz = base[tier] + bend[tier] * (float) Math.exp(-12f * u);
            phase += TAU * hz / RATE;
            float rubber = (float) Math.sin(phase);
            float hollow = (float) Math.sin(phase * 0.503f) * (0.42f - 0.08f * tier);
            float twang = (float) Math.sin(phase * 2.01f) * 0.20f
                    * (float) Math.exp(-18f * u);
            v[i] = (rubber + hollow + twang) * envelope(u, 0.006f, 2.6f + tier * 0.6f);
        }
        return render(v);
    }

    /** Achievement flourish for a flawless wave: a rising shimmer over a held fifth. */
    static short[] rosterJoin() {
        int n = (int) (RATE * 1.05f);
        float[] v = new float[n];
        float[] notes = {392f, 523f, 659f, 784f, 1047f};
        float[] starts = {0f, 0f, 0f, 0.28f, 0.50f};
        for (int i = 0; i < n; i++) {
            float t = (float) i / RATE;
            float s = 0f;
            for (int k = 0; k < notes.length; k++) {
                float local = t - starts[k];
                if (local < 0f) continue;
                float brass = (float) Math.sin(TAU * notes[k] * t)
                        + 0.28f * (float) Math.sin(TAU * notes[k] * 2f * t);
                s += brass * (float) Math.exp(-3.1f * local) * (k < 3 ? 0.32f : 0.52f);
            }
            float drum = t < 0.075f ? (float) Math.sin(TAU * (105f - 540f * t) * t)
                    * (float) Math.exp(-42f * t) : 0f;
            v[i] = (s + drum * 0.75f) * envelope((float) i / n, 0.006f, 0.55f);
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
    private static short[] miningCheer() {
        float[] v=new float[(int)(RATE*.48f)];
        for(int i=0;i<v.length;i++){
            float t=i/(float)RATE,beat=t%.24f,u=beat/.24f;
            double phase=6.283185*(380*beat+170*beat*beat)+(t>.24?1.2:0);
            float voice=(float)(Math.sin(phase)+.45*Math.sin(phase*3)+.22*Math.sin(phase*5));
            v[i]=voice*(float)Math.sin(Math.PI*u)*(1-u)*.55f;
        }
        return render(v);
    }

    private static short[] cave(int id) {
        float duration=.32f;
        float[] v=new float[(int)(RATE*duration)];
        java.util.Random random=new java.util.Random(817+id);
        double bass=0,gravel=0,air=0;
        for(int i=0;i<v.length;i++){
            double t=i/(double)RATE,u=t/duration,noise=random.nextDouble()*2-1;
            bass+=.018*(noise-bass);gravel+=.085*(noise-gravel);air+=.32*(noise-air);
            // Inharmonic rock modes retain weight on speakers which cannot reproduce sub-bass.
            double body=.44*Math.sin(6.283185*51*t)+.32*Math.sin(6.283185*83*t+.7)
                    +.25*Math.sin(6.283185*149*t+1.4)+.20*Math.sin(6.283185*227*t+2.1)
                    +.12*Math.sin(6.283185*373*t);
            double texture=bass*4.8+gravel*1.9;
            double attack=Math.min(1,t/.009),decay=Math.pow(1-u,3.3),hit=0;
            if(id==CAVE_AMBUSH){
                double stomp=Math.exp(-t*24)+.6*Math.exp(-Math.max(0,t-.075)*35)*(t>.075?1:0);
                body*=stomp*1.4;texture*=.7;hit=air*.8*Math.exp(-t*55);
            }else if(id==CAVE_SINK){
                double gulp=.6+.4*Math.sin(6.283185*17*t+2*Math.sin(t*21));
                texture*=gulp*1.6;body*=.5;hit=gravel*Math.sin(6.283185*470*t)*.7;
            }
            v[i]=(float)((body+texture+hit)*attack*decay);
        }
        return render(v);
    }

    private static short[] cart(int id) {
        float duration=id==CART_SQUEAL?.28f:.34f;
        float[] v=new float[(int)(RATE*duration)];
        java.util.Random random=new java.util.Random(431+id);
        double low=0,grit=0;
        for(int i=0;i<v.length;i++){
            double t=i/(double)RATE,u=t/duration,n=random.nextDouble()*2-1;
            low+=.035*(n-low);grit+=.23*(n-grit);
            double body=.3*Math.sin(6.283185*67*t)+.24*Math.sin(6.283185*137*t+.8)
                    +.15*Math.sin(6.283185*243*t);
            double sample=(low*3+body)*.65;
            if(id==CART_SQUEAL){
                double phase=6.283185*(540*t+130*t*t)+.6*Math.sin(6.283185*37*t);
                sample+=.22*Math.sin(phase)+.08*Math.sin(phase*2.73)+grit*.5;
            }else{
                // A wobbly wooden chassis knock, kept separate from the happy crew voices.
                sample+=.38*Math.sin(6.283185*189*t+2*Math.sin(t*45))*Math.exp(-t*12)
                        +grit*.9*Math.exp(-t*26);
            }
            v[i]=(float)(sample*Math.min(1,t/.005)*Math.pow(1-u,3));
        }
        return render(v);
    }

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
