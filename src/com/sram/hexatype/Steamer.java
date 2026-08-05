package com.sram.hexatype;

/**
 * State of the between-stages minigame: the dim sum steamer being prised open.
 *
 * Its own object because the damage deliberately outlives any single interlude — it is the
 * one piece of run state that is neither per-stage nor per-word.
 */
final class Steamer {

    /** Presses landed, carried across interludes so the lid is chipped open over stages. */
    int hits;
    /** Times the dumpling has been freed this run. */
    int opens;
    /** 1 right after a press, decaying: pops the lid up. */
    float lidPulse;
    /** 1 right after a press, decaying: flashes and cycles the container colour. */
    float flash;
    /**
     * How long the freed prize takes to climb out. Named, because the renderer needs it to
     * drive the escape animation and had a second copy of the number inlined — changing one
     * without the other silently breaks the climb.
     */
    static final float FREE_TIME = 2.55f;
    /** Counts down while the freed dumpling flies away. */
    float freedT;

    /** Outcome of a press. */
    static final int WRONG = 0, OK = 1, SCORED = 2, FREED = 3;

    /** The two keys that have to be alternated, one per thumb. */
    int leftKey, rightKey;
    /** Which side the sequence wants next. */
    boolean expectLeft = true;

    void reset() {
        hits = 0;
        opens = 0;
        lidPulse = 0;
        flash = 0;
        freedT = 0;
        leftKey = 0;
        rightKey = Glyph.COUNT / 2;
        expectLeft = true;
    }

    /** Chooses a fresh pair — one key from each thumb's cluster — for this interlude. */
    void pick(java.util.Random rnd) {
        int half = Glyph.COUNT / 2;
        leftKey = rnd.nextInt(half);
        rightKey = half + rnd.nextInt(Glyph.COUNT - half);
        expectLeft = true;
    }

    /** The key the sequence is waiting for. */
    int wanted() {
        return expectLeft ? leftKey : rightKey;
    }

    // ---- the spinner --------------------------------------------------------

    /**
     * Characters the spinner steps through before it settles. Enough that it reads as a spin
     * rather than a shuffle, given a thumb only has three keys to offer.
     */
    static final int ROLL_STEPS = 17;

    /**
     * Which character a slot shows part-way through the spinner.
     *
     * Counted <em>backwards</em> from the answer rather than forwards from a start: that way
     * t == 1 lands on {@code finalKey} exactly, whatever the easing does in between. Getting
     * this the other way round leaves the spinner stopping one short of the pair the game then
     * asks for, which is unplayable and not obviously a bug.
     *
     * @param base  first key of this thumb's cluster
     * @param count keys in the cluster
     * @param t     0..1 progress through the spin
     */
    static int rolled(int finalKey, int base, int count, float t) {
        if (t >= 1f) return finalKey;
        if (t < 0f) t = 0f;
        // Ease-out cubic: steps come fast at first and crawl into place at the end, which is
        // what makes it read as slowing to a choice rather than simply stopping.
        float e = 1f - (1f - t) * (1f - t) * (1f - t);
        int togo = ROLL_STEPS - (int) (ROLL_STEPS * e);
        int rel = ((finalKey - base - togo) % count + count) % count;
        return base + rel;
    }

    /** Left slot of the spinner; the settled key once {@code t} reaches 1. */
    int shownLeft(float t) {
        return rolled(leftKey, 0, Glyph.COUNT / 2, t);
    }

    /** Right slot of the spinner; the settled key once {@code t} reaches 1. */
    int shownRight(float t) {
        int half = Glyph.COUNT / 2;
        return rolled(rightKey, half, Glyph.COUNT - half, t);
    }

    /**
     * A press during the interlude. Only the two chosen keys count, and only in alternation:
     * a completed left-then-right pair is worth one hit. Anything else restarts the pair.
     */
    int press(int g) {
        lidPulse = 1f;
        flash = 1f;
        if (freedT > 0f) return OK;          // already loose; let the celebration play
        if (g != wanted()) {
            expectLeft = true;
            return WRONG;
        }
        if (expectLeft) {
            expectLeft = false;
            return OK;                       // half of a pair
        }
        expectLeft = true;
        hits++;
        if (hits < GameCore.STEAMER_HITS) return SCORED;
        hits = 0;
        opens++;
        freedT = FREE_TIME;
        return FREED;
    }

    /** How far the lid has lifted, 0..1. */
    float lidOpen() {
        float v = (float) hits / GameCore.STEAMER_HITS;
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }

    /** True once the lid is off; the caller awards the prize. */
    void update(float dt) {
        lidPulse = decay(lidPulse, dt * 4.5f);
        flash = decay(flash, dt * 3.0f);
        freedT = decay(freedT, dt);
    }

    private static float decay(float v, float amount) {
        v -= amount;
        return v < 0 ? 0 : v;
    }
}
