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

    void reset() {
        hits = 0;
        opens = 0;
        lidPulse = 0;
        flash = 0;
        freedT = 0;
    }

    /** How far the lid has lifted, 0..1. */
    float lidOpen() {
        float v = (float) hits / GameCore.STEAMER_HITS;
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }

    /** True once the lid is off; the caller awards the prize. */
    boolean strike() {
        lidPulse = 1f;
        flash = 1f;
        if (freedT > 0f) return false;      // already loose; let the celebration play
        hits++;
        if (hits < GameCore.STEAMER_HITS) return false;
        hits = 0;
        opens++;
        freedT = 1.7f;
        return true;
    }

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
