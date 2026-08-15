package com.sram.hexatype;

import java.util.Random;

/** Persistent course and flight state for the alternating star-path interlude. */
final class StarPath {
    static final int COUNT = 20;
    static final float READY = 1.5f, FLY = 5f, EXIT = 0.75f, REPORT = 1.5f;
    /** Course lengths visible at once; larger means wider gaps between its stars. */
    static final float COURSE_SCREENS = 3.2f;
    final float[] sx = new float[COUNT];
    /** 1 on pickup, decaying to zero; drives the shine and burst without spawning objects. */
    final float[] burst = new float[COUNT];
    int collected;
    int who = -1;
    float timer, x, vx;
    boolean left, right, won;

    void make(Random rnd) {
        float at = 0.5f;
        for (int i = 0; i < COUNT; i++) {
            // Bends are deliberately bounded by what eased steering can cover between late,
            // faster encounters. The old 0.34 jump could demand a third of the screen in 0.2s.
            at += (rnd.nextFloat() - 0.5f) * 0.18f;
            if (at < 0.16f) at = 0.16f;
            if (at > 0.84f) at = 0.84f;
            sx[i] = at;
        }
        collected = 0;
        for (int i = 0; i < COUNT; i++) burst[i] = 0f;
    }

    void begin(int entry, Layout L) {
        who = entry;
        timer = READY + FLY + EXIT + REPORT;
        x = L.w * 0.5f;
        vx = 0f;
        left = right = won = false;
        for (int i = 0; i < COUNT; i++) burst[i] = 0f;
    }

    boolean ready() { return timer > FLY + EXIT + REPORT; }
    boolean flying() { return timer <= FLY + EXIT + REPORT && timer > EXIT + REPORT; }
    boolean exiting() { return timer <= EXIT + REPORT && timer > REPORT; }
    boolean reporting() { return timer <= REPORT; }
    float flightProgress() {
        float p = (FLY + EXIT + REPORT - timer) / FLY;
        return p < 0 ? 0 : p > 1 ? 1 : p;
    }

    /** Scroll starts gently and accelerates continuously toward the end of the five seconds. */
    float traversalProgress() {
        return (float) Math.pow(flightProgress(), 1.45f);
    }
    int count() { return Integer.bitCount(collected); }

    void hold(int key, boolean down) {
        if (key < 0 || key >= Glyph.COUNT) return;
        if (key < Glyph.COUNT / 2) left = down;
        else right = down;
    }

    void update(float dt, Layout L) {
        timer -= dt;
        for (int i = 0; i < COUNT; i++) burst[i] = Math.max(0f, burst[i] - dt * 1.8f);
        if (!flying()) return;
        float dir = left == right ? 0f : left ? -1f : 1f;
        float max = L.w * 0.48f;
        float accel = L.w * 2.1f;
        float aim = dir * max;
        float step = accel * dt;
        if (vx < aim) vx = Math.min(aim, vx + step);
        else vx = Math.max(aim, vx - step);
        x += vx * dt;
        float r = L.enemyR * 1.05f;
        if (x < L.playLeft + r) { x = L.playLeft + r; vx = Math.max(0, vx); }
        if (x > L.playRight - r) { x = L.playRight - r; vx = Math.min(0, vx); }

        float cy = characterY(L);
        float pickup = r * 2.0f;
        for (int i = 0; i < COUNT; i++) {
            if ((collected & (1 << i)) != 0) continue;
            float dx = x - starX(i, L), dy = cy - starY(i, L);
            if (dx * dx + dy * dy <= pickup * pickup) {
                collected |= 1 << i;
                burst[i] = 1f;
            }
        }
        won = count() == COUNT;
    }

    float characterY(Layout L) {
        float p = flightProgress();
        float bottom = L.dangerY - L.enemyR * 1.3f;
        float middle = L.playTop + (L.dangerY - L.playTop) * 0.50f;
        if (exiting()) {
            float q = (EXIT + REPORT - timer) / EXIT;
            return middle + (L.playTop - L.enemyR * 3f - middle) * q * q;
        }
        return bottom + (middle - bottom) * p;
    }

    float starX(int i, Layout L) { return L.playLeft + sx[i] * (L.playRight - L.playLeft); }

    /** The course scrolls down past the climber; only a segment is visible at once. */
    float starY(int i, Layout L) {
        float span = L.dangerY - L.playTop;
        float course = (i + 0.5f) / COUNT;
        return L.dangerY - (course - traversalProgress()) * span * COURSE_SCREENS;
    }

    void finishAttempt() { left = right = false; }
}
