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
        freedT = 1.7f;
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
