package com.dddumpling.game;

/**
 * The powerup: a single glowing letter that drifts horizontally across the sky, carrying one
 * of four modes. Destroying it costs one press of its letter.
 *
 * Only ever one at a time, and it is not a word — it never touches the stage quota, the squish
 * count or the accuracy tally.
 */
final class Power {

    /** All keys become wildcards: any press attacks the next letter. */
    static final int FLURRY = 0;
    /** Letters can be dragged bodily off the screen. */
    static final int FLING = 1;
    /** A press chains through every matching letter in every word, engaged or not. */
    static final int MULTI = 2;
    /**
     * One of your own collectibles bounces around the field squishing words.
     *
     * Last on purpose: it stars something out of the display case, so it can only turn up once
     * the case has something in it, and {@link GameCore} gates it by rolling over one fewer
     * mode. That trick only works while this is the highest index.
     */
    static final int TEAM = 3;
    static final int COUNT = 4;

    /** Player-facing pool. MULTI is retired but retains its id for compatibility. */
    static final int[] OFFERED = {FLURRY, FLING, TEAM};
    static int offeredCount(boolean teamAvailable) { return teamAvailable ? 3 : 2; }
    static int offeredAt(int chip) { return OFFERED[chip]; }

    static final String[] NAMES = {"FLURRY", "FLING", "MULTI", "TEAM SQUISH"};
    /**
     * The same four for the settings panel's playtest chips, where the space is a fifth of the
     * panel each and TEAM SQUISH ran straight out of its box the moment a fifth chip was added.
     * A separate table rather than a truncation, because which word to keep is a judgement.
     */
    static final String[] CHIP = {"FLURRY", "FLING", "MULTI", "TEAM"};
    static final String[] BLURB = {"ANY KEY HITS", "SWIPE TO SLICE", "CHAINS EVERY MATCH",
            "YOUR SQUISHY FIGHTS"};

    /**
     * How long the frenzy lasts. Ending it clears the stage outright, so this doubles as the
     * length of the last stretch of a stage once a powerup is caught.
     */
    static final float DURATION = 15f;
    /**
     * Base opening-stage rates before the enemy spawn boost: word arrivals, crowd size,
     * and fall speed.
     *
     * These are the *first-stage* figures. Every one of them is tapered by {@link #taper} as the
     * difficulty ramp climbs, because they used to be flat multipliers on top of a ramp that had
     * already halved the spawn interval — so they compounded with it. See below for what that cost.
     */
    static final float SPAWN_RATE = 6f;
    static final float CROWD_RATE = 4f;
    static final float FALL_RATE = 2f;

    /**
     * The base late-frenzy spawn multiple, before the additional enemy spawn boost.
     *
     * Note that a frenzy's press demand is *exactly* its spawn multiplier times the stage's own:
     * words cost the same to clear either way, they simply turn up N times as often. So this
     * number defines the base curve; spawnRate applies the additional enemy-only boost.
     *
     * It used to be a flat 6. That reads fine on the opening stages, where six times almost nothing
     * is still almost nothing — but by stage 10 a frenzy wanted 23 presses a second sustained, and
     * by stage 22 it wanted 43. Nobody has 23 presses a second. FLURRY is where this was felt worst,
     * because it is the one mode that buys accuracy rather than throughput: MULTI takes every
     * matching tile on the field with one press and so gets *better* the more crowded it is, TEAM
     * SQUISH fields a second killer, FLING cuts several tiles a stroke, and FLURRY does exactly one
     * press worth of work per press, same as ordinary play.
     */
    static final float LATE_RATIO = 2f;

    /**
     * Ramp position at which a frenzy would have no extra pace left at all, were it not floored —
     * so with the floor it is the distance over which the taper does its work. 7 puts the floor at
     * about stage 11, which is where the wall was being hit.
     */
    private static final float TAPER_SPAN = 7f;

    /**
     * How much of a frenzy's extra pace survives at ramp position {@code ramp}: 1 on the opening
     * stage, falling to the floor that {@link #LATE_RATIO} implies.
     *
     * Derived rather than written down, so the two numbers cannot drift apart: at the floor the
     * spawn multiplier has to come out at exactly {@code LATE_RATIO}, and every rate is
     * {@code 1 + (rate - 1) * taper}, so the floor is fixed by the spawn rate alone.
     */
    static float taper(float ramp) {
        float floor = (LATE_RATIO - 1f) / (SPAWN_RATE - 1f);
        float t = 1f - ramp / TAPER_SPAN;
        return t < floor ? floor : t;
    }

    /**
     * A first-stage rate tapered to where the ramp has got to. Never below 1: a frenzy may run out
     * of *extra* pace, but it can never run slower than the stage it interrupts.
     */
    private static float tapered(float rate, float ramp) {
        return 1f + (rate - 1f) * taper(ramp);
    }

    // Apply after tapering so every active powerup gets 30% more enemy spawn attempts.
    // Pickup timing, fall speed, and the crowd cap keep their original settings.
    static final float ENEMY_SPAWN_BOOST = 1.3f;
    static float spawnRate(float ramp) { return tapered(SPAWN_RATE, ramp) * ENEMY_SPAWN_BOOST; }

    static float crowdRate(float ramp) { return tapered(CROWD_RATE, ramp); }

    static float fallRate(float ramp) { return tapered(FALL_RATE, ramp); }

    /**
     * Clouds run this many times faster during the frenzy, at every stage.
     *
     * Deliberately not tapered. The rule is to taper what costs the player and leave what only
     * looks exciting: a late frenzy is a calmer thing to play than an early one now, and it must
     * not also *look* like a calmer thing, or the reward stops reading as a reward.
     */
    static final float SKY_RATE = 4f;
    // A frenzy used to buy 2.6s of extra interlude on top of whatever the round earned. Gone: it was
    // larger than the whole spread of GameCore's earned-mash ladder, so a hurt frenzy round out-paid
    // a perfect calm one and the ladder said nothing. The frenzy still announces itself with its own
    // tone and still clears the stage outright, which is payment enough.
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
