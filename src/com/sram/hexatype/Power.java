package com.sram.hexatype;

/**
 * The powerup: a single glowing letter that drifts horizontally across the sky, carrying one
 * of three modes. Destroying it costs one press of its letter.
 *
 * Only ever one at a time, and it is not a word — it never touches the stage quota, the kill
 * count or the accuracy tally.
 */
final class Power {

    /** All keys become wildcards: any press attacks the next letter. */
    static final int FLURRY = 0;
    /** Letters can be dragged bodily off the screen. */
    static final int FLING = 1;
    /** A press clears every matching letter in every word, engaged or not. */
    static final int MULTI = 2;
    static final int COUNT = 3;

    static final String[] NAMES = {"FLURRY", "FLING", "MULTI"};
    static final String[] BLURB = {"ANY KEY HITS", "SWIPE TO SLICE", "CLEARS EVERY MATCH"};

    /**
     * How long the frenzy lasts. Ending it clears the stage outright, so this doubles as the
     * length of the last stretch of a stage once a powerup is caught.
     */
    static final float DURATION = 15f;
    /** Words arrive this many times faster during the frenzy. */
    static final float SPAWN_RATE = 6f;
    /** And this many times more of them on screen at once. */
    static final float CROWD_RATE = 4f;
    /** And fall this many times faster. */
    static final float FALL_RATE = 2f;
    /** Clouds run this many times faster during the frenzy. */
    static final float SKY_RATE = 4f;
    /** Extra interlude time when the stage was ended by a frenzy. */
    static final float BONUS_EXTRA = 2.6f;
    /** Seconds to cross the screen. */
    static final float CROSS_TIME = 7.5f;
    /** Score for catching one. */
    static final int SCORE = 150;
    /** How long the burst plays after it is struck, before it stops existing. */
    static final float POP_TIME = 0.45f;
    /** Seconds between powerup appearances. */
    static final float SPAWN_MIN = 12f, SPAWN_MAX = 20f;

    int glyph;
    int effect;
    float x, y, vx;
    /** Age, for the glow and bob. */
    float t;
    /** Struck: no longer catchable, playing its burst. */
    boolean hit;
    float hitT;

    boolean catchable() {
        return !hit;
    }

    /** True once the burst has finished and it should be dropped. */
    boolean spent() {
        return hit && hitT >= POP_TIME;
    }

    void update(float dt) {
        t += dt;
        if (hit) {
            hitT += dt;
            return;
        }
        x += vx * dt;
    }

    /** True when it has drifted clear of the screen without being caught. */
    boolean offScreen(Layout L, float r) {
        return x < -r * 2.5f || x > L.w + r * 2.5f;
    }

    String name() {
        return NAMES[effect];
    }
}
