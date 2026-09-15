package com.dddumpling.game;

/** Authored fork contents and geometry, independent of encounters and their clocks. */
final class CaveRoute {
    static final float[] FORKS = {2f, 5f, 8f};
    static final float HEART_OFFSET = 1.35f;
    private static final int[][] EVENTS = {
        {Cave.ROCKS, Cave.SHADOW}, {Cave.SAND, Cave.ROCKS}, {Cave.SHADOW, Cave.SAND}
    };
    private static final boolean[][] HEARTS = {{true,false},{true,false},{true,true}};
    private CaveRoute() {}
    static int event(int fork, int side) { return EVENTS[fork][side < 0 ? 0 : 1]; }
    static boolean heart(int fork, int side) { return HEARTS[fork][side < 0 ? 0 : 1]; }
    static float centre(float at) { return .5f + .035f * (float)Math.sin(at * 1.8f); }
    static float x(int fork, int side, float at) {
        float t = Math.max(0f, Math.min(1f, (at - FORKS[fork]) / 2f));
        return centre(at) + side * .26f * (float)Math.sin(t * Math.PI);
    }
}
