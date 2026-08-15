package com.sram.hexatype;

import java.util.Random;

/** Persistent course and flight state for the alternating star-path interlude. */
final class StarPath {
    static final int COUNT = 20;
    /**
     * The flight is down from five seconds to four: the same twenty checkpoints arrive in a fifth
     * less time, which together with the wider spacing below puts the scroll up by half.
     */
    static final float READY = 1.5f, FLY = 4f, EXIT = 0.75f, REPORT = 1.5f;
    /**
     * Course lengths visible at once; larger means wider gaps between its stars.
     *
     * This is also the spacing knob: the gap between two checkpoints is
     * {@code span * COURSE_SCREENS / COUNT}, and since the whole course still passes in {@link #FLY}
     * seconds, raising it spreads them out <em>and</em> speeds the scroll up in the same move.
     *
     * It went 3.2 down to 2.8 to get more of a sweep on screen — at six checkpoints the course read
     * as a straight diagonal however swoopy it was over its five seconds — and then back up to 3.4
     * because the stars wanted to be further apart, with the sweep period shortened to keep the
     * curve. Below about 2.4 the checkpoints touch each other at {@link #STAR_OUT} wide.
     */
    static final float COURSE_SCREENS = 3.4f;
    /** How sharply the scroll accelerates over the flight. 1 would be a constant crawl. */
    static final float RUSH = 1.45f;
    static final float TAU = 6.2831853f;

    /**
     * Top steering speed and the acceleration up to it, as fractions of the view width per second.
     * Named because the course is generated against them: what a sweep may ask for is exactly what
     * these can deliver, and the two used to be literals inside {@link #update} where nothing
     * generating a course could see them.
     *
     * Raised from 0.48 and 2.1 to buy the sweep its shorter period — the flyer has to be able to
     * keep up with a course that crosses the whole play area twice in five seconds, and this is the
     * side of that trade the player feels as the thing answering the key rather than lagging it.
     */
    static final float MAX_VX = 0.60f, ACCEL = 2.6f;

    /**
     * The sweep: how far either side of the middle a course reaches, and the seconds it takes to
     * come back. {@code 0.40} spans the play area — the flyer's own clamp keeps its centre a little
     * inside the edges, and {@link #EDGE} is set to exactly what it can reach.
     *
     * The period is deliberately shorter than the sweep can be <em>tracked</em> at: it asks about
     * 1.2 times {@link #MAX_VX}, so following the line is not possible and the course has to be
     * anticipated and cut across instead. That is the difficulty in this game — at a trackable
     * period every pilot the harness can write collects all twenty however slow its thumbs are.
     *
     * It is not a dial past that, though: it is a cliff. Measured against the harness pilot, 1.2
     * times still completes a course and 1.5 times collapses to a third of it, because a flyer that
     * can never catch up stops being late and starts being somewhere else entirely. Anything past
     * about 1.25 makes the game worse rather than harder; there is an assertion on it.
     */
    static final float SWEEP = 0.40f, SWEEP_TIME = 3.8f;
    /**
     * The ripple on top, so a course is not one bare sine to be read at a glance.
     *
     * Kept small because it spends the same budget as the sweep: the two demands add, and the sweep
     * is what the player asked to be able to see. A course is encountered over about four and a
     * quarter of the five seconds, so at this period it gets most of the way round its sweep: out to
     * one edge of the play area, back through the middle, and out to the other.
     */
    static final float RIPPLE = 0.02f, RIPPLE_TIME = 2.2f;
    /** Closest a checkpoint comes to the edge of the play area. */
    static final float EDGE = 0.5f - SWEEP;
    /**
     * How long the victory tableau holds once the last star is taken: the course stops dead, the
     * prize climbs out of that star, and then the parade takes the screen. Long enough to read the
     * name under it, short enough that it is not standing between a win and the parade.
     */
    static final float WIN_HOLD = 2.7f;
    /**
     * Flyer radius as a multiple of an enemy tile's, and star radii as multiples of the text
     * unit. One copy of each: the pickup test in here and the drawing in {@link StarScreen} have
     * to agree about how big these are, and they were two separate literals to begin with.
     *
     * All three are half again what they first were: at the original sizes the flyer and its
     * checkpoints were the smallest things on a screen that has nothing else on it. What did
     * <em>not</em> grow with them is the catch tolerance — see {@link #pickupR}.
     */
    static final float FLYER_R = 1.575f, STAR_OUT = 2.34f, STAR_IN = 1.035f;
    /**
     * The heart of a checkpoint, as a fraction of its outer radius: the pearl the drawing paints at
     * the centre of the petals, and what the pickup test below aims at. Was the glow disc around
     * that pearl, at 0.30 — the pearl is the tighter of the two things drawn there, and taking the
     * tighter one is a little less forgiving for the same story.
     */
    static final float HEART = 0.23f;

    static float flyerR(Layout L) { return L.enemyR * FLYER_R; }
    static float starOuter(Layout L) { return L.unit * STAR_OUT; }
    static float starInner(Layout L) { return L.unit * STAR_IN; }

    /**
     * How close a centre has to come to a checkpoint's centre to take it: the flyer has to cover
     * the star's heart, not merely brush a petal.
     *
     * The old rule was two flyer radii, which at the old sizes came to almost exactly this number —
     * and that is the point of writing it this way. Growing the flyer and the stars by half again is
     * a cosmetic change, and two flyer radii would have turned it into a difficulty change: the
     * catch band went from a ninth of the width to a sixth, which is wider than the course's own
     * wander, so a flyer sitting dead centre with nobody touching the keys completed courses on its
     * own. The soak bot found that, having no way to steer at all.
     */
    static float pickupR(Layout L) { return flyerR(L) + starOuter(L) * HEART; }

    final float[] sx = new float[COUNT];
    /** 1 on pickup, decaying to zero; drives the shine and burst without spawning objects. */
    final float[] burst = new float[COUNT];
    int collected;
    int who = -1;
    float timer, x, vx;
    boolean left, right, won;
    /**
     * Seconds of victory tableau left. While this is running every other kind of motion here is
     * frozen — the scroll, the steering and the pickup shines all hold where the win found them.
     */
    float winT;
    /** Which checkpoint completed the course, so the prize can climb out of that one. */
    int winStar = -1;
    /**
     * Set on any frame a checkpoint is taken, for the caller to turn into the slow-motion beat and
     * the climbing note.
     *
     * Not set by the one that completes the course: the victory tableau is a full stop already, so a
     * beat over the top of it only stretches its opening, and the achievement fanfare is what that
     * star sounds like. The note ladder therefore runs to nineteen and the fanfare takes the last.
     */
    boolean grabbed;
    /**
     * Set on the frame the course is completed and cleared by {@link GameCore} once it has paid
     * out. The tableau draws what was won, so the prize has to be picked before it is drawn —
     * which means the award happens here, not at the end of the interlude.
     */
    boolean awardPending;

    /**
     * The course: one long swoop out to one edge of the play area, back through the middle and away
     * to the other, with a small ripple riding on top of it.
     *
     * Shaped in <em>seconds</em> rather than in star index, which is the whole trick. The scroll
     * accelerates through the flight, so the last stars arrive about three times as fast as the
     * first; a random walk with a fixed step per star therefore demanded three times the sideways
     * speed at the end that it did at the start, and the steps had to be kept small enough for the
     * fast end — which is why the old course could only wiggle, over the middle two thirds of the
     * width. A sine in seconds asks the same speed of you everywhere, so the sweep can span the
     * whole play area and still sit inside what the steering can deliver.
     *
     * {@code TestStars} holds the arithmetic: peak demand is {@code SWEEP * 2pi / SWEEP_TIME} plus
     * the ripple's, and it has to stay under {@link #MAX_VX}. Widen the sweep and the period has to
     * grow with it, or the course stops being coverable at the fast end.
     */
    void make(Random rnd) {
        // How far round the sweep gets before the flight ends, and which way it goes first. Only
        // ever stretched, never squeezed: a shorter period is a faster demand, and the period here
        // is already sized to the steering.
        //
        // The side is taken from the second draw, not the first. Java's generator gives nearby
        // seeds the same leading bit, so a coin flipped first sent every course in a freshly
        // seeded harness the same way — the game shares one long-lived generator and would never
        // have shown it.
        float period = SWEEP_TIME * (1f + 0.20f * rnd.nextFloat());
        float dir = rnd.nextFloat() < 0.5f ? 1f : -1f;
        float ripplePhase = rnd.nextFloat() * TAU;
        for (int i = 0; i < COUNT; i++) {
            float t = encounterTime(i);
            // No sweep phase: every course leaves from the middle, which is where the flyer starts.
            // A random phase put the first star out at an edge and opened with a dash nobody could
            // have known was coming.
            float at = 0.5f + dir * SWEEP * (float) Math.sin(TAU * t / period)
                    + RIPPLE * (float) Math.sin(ripplePhase + TAU * t / RIPPLE_TIME);
            if (at < EDGE) at = EDGE;
            if (at > 1f - EDGE) at = 1f - EDGE;
            sx[i] = at;
        }
        collected = 0;
        for (int i = 0; i < COUNT; i++) burst[i] = 0f;
    }

    /**
     * Roughly how many seconds into the flight star {@code i} comes level with the flyer.
     *
     * Deliberately approximate: it exists to shape the course against the steering, and a frame
     * either way changes nothing. It takes the flyer's mid-flight height, which is where it spends
     * all but the opening moment, so the first few stars come out at zero — they are already
     * level with it when the course starts, and that keeps the opening stars in the middle.
     */
    static float encounterTime(int i) {
        float ahead = (i + 0.5f) / COUNT - 0.5f / COURSE_SCREENS;
        if (ahead <= 0f) return 0f;
        return FLY * (float) Math.pow(ahead, 1f / RUSH);
    }

    void begin(int entry, Layout L) {
        who = entry;
        timer = READY + FLY + EXIT + REPORT;
        x = L.w * 0.5f;
        vx = 0f;
        left = right = won = false;
        winT = 0f;
        winStar = -1;
        awardPending = false;
        for (int i = 0; i < COUNT; i++) burst[i] = 0f;
    }

    boolean ready() { return timer > FLY + EXIT + REPORT; }
    boolean flying() { return timer <= FLY + EXIT + REPORT && timer > EXIT + REPORT; }
    boolean exiting() { return timer <= EXIT + REPORT && timer > REPORT; }
    boolean reporting() { return timer <= REPORT; }
    /** True while the victory tableau owns the screen, which is the end of a completed course. */
    boolean winning() { return winT > 0f; }
    /** 0..1 through the victory tableau. */
    float winProgress() {
        if (winT <= 0f) return 0f;
        float p = 1f - winT / WIN_HOLD;
        return p < 0f ? 0f : p > 1f ? 1f : p;
    }
    float flightProgress() {
        float p = (FLY + EXIT + REPORT - timer) / FLY;
        return p < 0 ? 0 : p > 1 ? 1 : p;
    }

    /** Scroll starts gently and accelerates continuously toward the end of the five seconds. */
    float traversalProgress() {
        return (float) Math.pow(flightProgress(), RUSH);
    }
    int count() { return Integer.bitCount(collected); }

    void hold(int key, boolean down) {
        if (key < 0 || key >= Glyph.COUNT || winning()) return;
        if (key < Glyph.COUNT / 2) left = down;
        else right = down;
    }

    void update(float dt, Layout L) {
        if (winning()) {
            // The course stops: no scroll, no steering, no further pickups. The tableau is drawn
            // over a still frame rather than over a course still sliding out from under it.
            //
            // The shines are the exception, and deliberately: a pickup burst frozen part-way
            // through leaves a white glint stuck beside the last star for the whole tableau, which
            // reads as a drawing fault. It is the pickup that just landed, so it finishes.
            for (int i = 0; i < COUNT; i++) burst[i] = Math.max(0f, burst[i] - dt * 1.8f);
            winT -= dt;
            if (winT <= 0f) {
                winT = 0f;
                // Spends whatever was left of the flight: the tableau replaces the climb-out and
                // the report, and this is the value GameCore reads to close the interlude.
                timer = 0f;
            }
            return;
        }
        timer -= dt;
        grabbed = false;
        for (int i = 0; i < COUNT; i++) burst[i] = Math.max(0f, burst[i] - dt * 1.8f);
        if (!flying()) return;
        float dir = left == right ? 0f : left ? -1f : 1f;
        float max = L.w * MAX_VX;
        float accel = L.w * ACCEL;
        float aim = dir * max;
        float step = accel * dt;
        if (vx < aim) vx = Math.min(aim, vx + step);
        else vx = Math.max(aim, vx - step);
        x += vx * dt;
        float r = flyerR(L);
        if (x < L.playLeft + r) { x = L.playLeft + r; vx = Math.max(0, vx); }
        if (x > L.playRight - r) { x = L.playRight - r; vx = Math.min(0, vx); }

        float cy = characterY(L);
        float pickup = pickupR(L);
        for (int i = 0; i < COUNT; i++) {
            if ((collected & (1 << i)) != 0) continue;
            float dx = x - starX(i, L), dy = cy - starY(i, L);
            if (dx * dx + dy * dy <= pickup * pickup) {
                collected |= 1 << i;
                burst[i] = 1f;
                if (count() == COUNT) {
                    // The last one. The course is over here rather than when the clock runs out,
                    // so the win is announced where it happened.
                    won = true;
                    winStar = i;
                    winT = WIN_HOLD;
                    awardPending = true;
                    // A thumb still down must not carry its lean into the tableau, or into the
                    // next course: the flight is finished either way.
                    left = right = false;
                    vx = 0f;
                    return;
                }
                grabbed = true;
            }
        }
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
