package com.dddumpling.game;

/**
 * The FLING blade: a swipe that cuts every tile it sweeps past, plus its slow-motion beat and its
 * sparkle trail.
 *
 * Operates on {@code GameCore}'s fields rather than owning them, the same seam {@link Fx} uses — a
 * cut destroys words, which is the core's business. What lives here is the gesture: when a stroke
 * starts, when it ends by itself, and what one sweep cuts.
 */
final class Blade {

    private Blade() {}

    /** Cut width in tile radii. Generous: a swipe through moving targets, and the mode is the payoff. */
    static final float BLADE = 1.15f;
    /** Words in one stroke that earn the slow-motion beat. */
    static final int SLOW_KILLS = 2;
    /** Length of that beat, in real seconds. An impact, not an interlude. */
    static final float SLOW_TIME = 0.28f;
    /**
     * The same beat for taking a star, at a quarter of the length. A course hands out twenty of
     * these, the last few a fifth of a second apart, so at the fling's length a good course would be
     * continuously slow rather than punctuated. Every beat also stretches the flight in real time,
     * since the course clock is scaled by it. {@code TestStars} holds the total.
     */
    static final float STAR_BEAT = 0.075f;
    /** Fraction of normal speed during the beat. */
    static final float SLOW_RATE = 0.32f;
    /** How long the multi-word readout stays up. Longer than the beat: brief beat, readable number. */
    static final float SLICE_CALL_TIME = 1.1f;

    /**
     * What a definite move is, in tile radii, and how long without one ends the stroke.
     *
     * A stroke used to run from touch-down to touch-up, so a parked finger held one combo open for a
     * whole frenzy. A swipe is a motion, so stopping ends it.
     *
     * Measured as displacement from an anchor, not path length — a resting finger jitters a pixel or
     * two a frame and summing that keeps a combo alive by trembling. 0.35 radii is ~20px at 1080
     * wide: far more than jitter, far less than a real swipe covers in a frame. To stay awake a
     * finger need only average ~90px/s, and a slice worth calling travels ten times that, so nothing
     * fast is cut off mid-motion. The dwell is not shorter because a zigzag pauses a frame or two at
     * each corner, and one motion is one stroke.
     */
    static final float STROKE_MOVE = 0.35f;
    /** Seconds without a definite move that end the stroke. */
    static final float STROKE_DWELL = 0.22f;
    /**
     * Longest one stroke may run, in real seconds. Backstop for the dwell's one hole: a finger
     * creeping just fast enough to keep resetting the anchor is always moving. Set well past any
     * genuine slice — a full-width sweep is half a second, a long zigzag ~1.2s.
     */
    static final float STROKE_MAX = 1.6f;
    /** How long the edge lingers after a stroke ends, so its end is seen rather than inferred. */
    static final float STROKE_FADE = 0.18f;
    /** Sparkles a second along the blade. */
    static final float TRAIL_RATE = 100f;

    /** The finger has landed. Nothing is cut yet: a tap that does not travel cuts nothing. */
    static void begin(GameCore c, float x, float y) {
        c.touchDown = true;
        wake(c, x, y);
    }

    /**
     * Starts a fresh stroke with the combo back at nothing. Called both by the finger landing and by
     * it moving again after a rest — one touch can hold several swipes, each counting for itself.
     */
    private static void wake(GameCore c, float x, float y) {
        c.fingerDown = true;
        c.fingerX = x;
        c.fingerY = y;
        c.trailPrevX = x;
        c.trailPrevY = y;
        c.bladeFromX = x;
        c.bladeFromY = y;
        c.strokeAnchorX = x;
        c.strokeAnchorY = y;
        c.strokeIdle = 0f;
        c.strokeAge = 0f;
        c.strokeFade = 0f;
        c.flingUsed = true;
        c.strokeKills = 0;
        c.strokeCuts = 0;
    }

    /**
     * Ends the stroke with the finger still down: the blade dies where it stopped and the counts
     * freeze for the readout. The anchor is left under the finger, not at the last move, so waking
     * again costs a whole {@link #STROKE_MOVE} from here — else a crept finger restarts on a twitch.
     */
    private static void rest(GameCore c) {
        if (!c.fingerDown) return;
        c.fingerDown = false;
        c.strokeFade = STROKE_FADE;
        c.strokeAnchorX = c.fingerX;
        c.strokeAnchorY = c.fingerY;
    }

    /**
     * Ticks what ends a stroke by itself: the dwell and the cap behind it.
     *
     * On real time and above every early return in {@code GameCore.update}, for two reasons. A
     * gesture is not part of the simulation, so a slow-motion beat must not hand it three times the
     * grace to stand still in. And a stroke has two exits — this and the player dying mid-swipe —
     * and a death never reaches the PLAY half, which would leave a blade lit over the summary.
     */
    static void updateStroke(GameCore c, float dt) {
        c.strokeFade = Math.max(0f, c.strokeFade - dt);
        if (!c.flinging() || c.state != GameCore.PLAY) {
            c.touchDown = false;
            c.fingerDown = false;
            return;
        }
        if (!c.fingerDown) return;
        c.strokeIdle += dt;
        c.strokeAge += dt;
        if (c.strokeIdle >= STROKE_DWELL || c.strokeAge >= STROKE_MAX) rest(c);
    }

    /**
     * Extends the stroke to x,y and cuts every tile the blade swept past on the way — not just those
     * under the end point, so a fast swipe cuts the whole line it crossed. Grabbing and dragging one
     * tile was the old model, and the reason the mode felt weak.
     *
     * @return tiles cut by this segment
     */
    static int sliceTo(GameCore c, float x, float y, Layout L) {
        float x0 = c.fingerX, y0 = c.fingerY;
        c.fingerX = x;
        c.fingerY = y;
        if (!c.flinging() || !c.touchDown) return 0;

        // From the anchor, not along the path — see STROKE_MOVE.
        float ax = x - c.strokeAnchorX, ay = y - c.strokeAnchorY;
        float move = L.enemyR * STROKE_MOVE;
        boolean moved = ax * ax + ay * ay >= move * move;
        if (!c.fingerDown) {
            // Rested out from under this finger. Moving again is a new swipe starting here, and the
            // stretch that woke it cuts nothing — as with the first touch of any stroke.
            if (moved) wake(c, x, y);
            return 0;
        }
        if (moved) {
            c.strokeAnchorX = x;
            c.strokeAnchorY = y;
            c.strokeIdle = 0f;
        }

        int cut = 0;
        float r = L.enemyR * BLADE;
        // Downward: destroying a word mutates the list from under the loop.
        for (int n = c.enemies.size() - 1; n >= 0; n--) {
            if (n >= c.enemies.size()) continue;
            GameCore.Enemy e = c.enemies.get(n);
            if (!e.typeable()) continue;
            for (int i = e.word.length - 1; i >= e.pos; i--) {
                if (e.gone[i]) continue;
                if (segDist2(c.tileX(e, i, L), e.y, x0, y0, x, y) > r * r) continue;
                boolean alive = !e.destroyed;
                // Sent along the stroke, so the piece flies the way the blade went. No clear tone:
                // the chop is this letter's sound, and a word finished here would land a chime on
                // top of its own last chop.
                c.removeTile(e, i, x - x0, y - y0, L, false);
                cut++;
                c.strokeCuts++;
                if (c.sound != null) c.sound.chop();
                if (alive && e.destroyed) {
                    c.strokeKills++;
                    // Every word after the first refreshes the beat, so a long sweep stays slow.
                    if (c.strokeKills >= SLOW_KILLS) slowdown(c);
                }
                // Trailing cuts of an unfinished word still belong on the readout, but only while
                // this stroke is the one being announced.
                if (c.strokeKills >= SLOW_KILLS) c.callCuts = c.strokeCuts;
                if (!e.typeable()) break;
            }
        }
        return cut;
    }

    /** The finger has lifted. The kill and cut counts survive it, for the readout. */
    static void end(GameCore c) {
        rest(c);
        c.touchDown = false;
    }

    /** The slow-motion beat when one stroke takes several words. */
    private static void slowdown(GameCore c) {
        c.slowdown = SLOW_TIME;
        c.sliceCall = SLICE_CALL_TIME;
        // Taken here rather than read live: this fires again on every further word, so a sweep
        // counts up, and it stops when the stroke does instead of being reset by the next one.
        c.callKills = c.strokeKills;
        c.callCuts = c.strokeCuts;
        // Restrained: each destroyed word has already flashed and flooded the sky, and a third wash
        // whited out the field at the moment there was something worth looking at.
        c.shake = Math.max(c.shake, 0.35f);
        c.flash = Math.max(c.flash, 0.45f);
        c.flashColor = GameCore.FLASH_CLEAR;
        if (c.sound != null) c.sound.achievement();
    }

    /** Squared distance from a point to segment a-b. */
    static float segDist2(float px, float py, float ax, float ay, float bx, float by) {
        float dx = bx - ax, dy = by - ay;
        float len2 = dx * dx + dy * dy;
        // A stationary finger degenerates to a point, which is still a legitimate test.
        float t = len2 <= 1e-6f ? 0f : ((px - ax) * dx + (py - ay) * dy) / len2;
        if (t < 0f) t = 0f;
        else if (t > 1f) t = 1f;
        float qx = ax + dx * t - px, qy = ay + dy * t - py;
        return qx * qx + qy * qy;
    }

    /**
     * The sparkle trail: under the finger once the player is dragging, along the demonstration path
     * until they have. One rate for both, so the hint looks like the thing it teaches.
     */
    static void updateTrail(GameCore c, float dt, Layout L) {
        // The finger is released by updateStroke, not here: this runs below the PLAY return and a
        // frenzy can end on a frame that never reaches it.
        if (!c.flinging()) return;
        c.demoX = L.w * 0.5f + (float) Math.sin(c.clock * 2.2f) * L.w * 0.26f;
        c.demoY = L.h * 0.45f + (float) Math.cos(c.clock * 1.5f) * L.h * 0.04f;

        float sx;
        float sy;
        if (c.fingerDown) {
            sx = c.fingerX;
            sy = c.fingerY;
        } else if (!c.flingUsed) {
            sx = c.demoX;
            sy = c.demoY;
        } else {
            c.trailAcc = 0f;
            return;
        }

        // A parked finger lays nothing, and it stops laying at exactly the speed that stops keeping
        // a stroke awake, so one number governs both. 100 sparkles a second on one spot is a glowing
        // blob, and leaving the edge alone freezes it along the last stretch actually swept.
        if (c.fingerDown) {
            float mx = sx - c.trailPrevX, my = sy - c.trailPrevY;
            float still = L.enemyR * (STROKE_MOVE / STROKE_DWELL) * dt;
            if (mx * mx + my * my < still * still) {
                c.trailAcc = 0f;
                return;
            }
        }

        // Captured before the emission below advances trailPrev to the finger.
        c.bladeFromX = c.trailPrevX;
        c.bladeFromY = c.trailPrevY;
        c.trailAcc += dt;
        float per = 1f / TRAIL_RATE;
        int emit = 0;
        while (c.trailAcc >= per) {
            c.trailAcc -= per;
            emit++;
        }
        // Spread along the path swept since last frame, not all at the current point: a quick swipe
        // otherwise leaves a dotted line.
        for (int k = 0; k < emit; k++) {
            float f = emit == 1 ? 1f : (float) k / (emit - 1);
            Fx.sparkle(c, c.rnd, c.trailPrevX + (sx - c.trailPrevX) * f,
                    c.trailPrevY + (sy - c.trailPrevY) * f, L.enemyR * 0.85f,
                    Glyph.cycle(c.clock * 1.6f));
        }
        c.trailPrevX = sx;
        c.trailPrevY = sy;
    }
}
