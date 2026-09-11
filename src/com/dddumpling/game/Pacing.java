package com.dddumpling.game;

/**
 * The stage difficulty dials. Pure functions of stage (and the player's speed setting), so this is
 * the one file to read when tuning the curve.
 *
 * Every dial reads its position off {@link #ramp} rather than off stage directly, so the whole curve
 * stretches from one constant. {@code GameCore} keeps one-line delegations to these.
 */
final class Pacing {

    private Pacing() {}

    /**
     * How far one stage advances the ramp. 5/9 because at a full step per stage every dial arrived at
     * once and the game hit a wall at stage 6; what landed at 6 now lands at 10. Stage 1 is
     * unaffected either way — the ramp starts at zero.
     */
    static final float RAMP = 5f / 9f;

    /** Hard ceiling on the presses any single word can demand. */
    static final int MAX_PRESSES = 8;

    /** 0 on the opening stage. */
    static float ramp(int stage) {
        return (stage - 1) * RAMP;
    }

    /** Reproduces the old integer {@code stage / 2} stepping off the ramp. */
    private static int step(int stage) {
        return (int) ((ramp(stage) + 1f) / 2f);
    }

    /** Seconds to fall from spawn to the danger line. Divided by speed, so 1.5x arrives faster. */
    static float travelSeconds(int stage, float speed) {
        return Math.max(4.2f, 15f - ramp(stage) * 1.05f) / speed;
    }

    static float spawnInterval(int stage, float speed) {
        float r = ramp(stage);
        // Past stage 10, faster falls and longer words supply the pressure. Give stage 11
        // more breathing room immediately, easing releases out to a 2.75-second cap.
        float seconds = r <= 5f ? 2.5f - r * 0.13f
                : 1.85f + Math.min(0.9f, (r - 5f) * 0.36f);
        return Math.max(1.35f, seconds) / speed;
    }

    static int maxEnemies(int stage) {
        // Five is still a full field on a phone. The old curve rose to six at stage 10 and seven
        // shortly afterward, making concurrencynot word speedthe late-game wall.
        return Math.min(5, 3 + step(stage));
    }

    static int maxWordLen(int stage) {
        return Math.min(5, 2 + step(stage));
    }

    static int minWordLen(int stage) {
        return Math.max(2, maxWordLen(stage) - 2);
    }

    /** Words this stage releases in total. */
    static int stageQuota(int stage) {
        return Math.min(10, 5 + step(stage));
    }

    /** Odds a tile becomes a stack. Stacks stay out of the opening stage. */
    static float stackChance(int stage) {
        return stage < 2 ? 0f : Math.min(0.55f, 0.13f * ramp(stage));
    }
}
