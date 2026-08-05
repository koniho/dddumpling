package com.sram.hexatype;

/**
 * The squishy that fights for you during TEAM SQUISH: one of your own collectibles, in a
 * glowing bubble, ricocheting around the play field. Anything it hits is squished whole, and
 * every word it takes leaves it a little bigger and a little brighter.
 *
 * Its own object for the same reason {@link Steamer} is: it is a self-contained ball of state
 * with a physics step, and putting it in {@link GameCore} would have been another eighty lines
 * in the file that is already the outlier.
 *
 * The step takes the core because a hit has to squish a whole word, which only the core can do
 * — the same arrangement {@link Fx} uses.
 */
final class Buddy {

    /** Which collectible turned up, or -1 when the mode is not running. */
    int who = -1;
    float x, y, vx, vy;
    /** Words squished this frenzy. Drives both the size and the glow. */
    int squishes;
    /** The word it is charging at, or null while it is just bouncing. */
    GameCore.Enemy chase;
    /** 1 right after a hit, decaying: the bubble flashes and swells. */
    float pulse;

    /** Radius at the start, in tile radii. */
    private static final float START = 1.05f;
    /** Added per word squished, and the cap on that — it must not fill the field. */
    private static final float GROW = 0.13f, GROW_MAX = 1.15f;
    /** Squishes for the glow to reach full. */
    private static final float GLOW_FULL = 6f;
    /** Drift speed, in view widths per second. */
    static final float SPEED = 0.42f;
    /** How much faster it travels while charging at something. */
    static final float CHARGE_RATE = 2.9f;

    /**
     * How long it takes to wind from drift speed up to charge speed, and to wind back down.
     * Speed used to change the instant a charge was called or dropped, which read as the bubble
     * being yanked rather than driven.
     */
    static final float SPIN_UP = 0.35f;
    /**
     * How much of the charge speed a hard turn gives up: it slows into the corner and winds back
     * up on the way out, which is what makes the turn look like weight rather than a rotation.
     *
     * Set so the worst case aims at about half the drift speed. The first value only came down to
     * just above drift, which meant a bubble already drifting gave up nothing at all to turn —
     * the slowdown was invisible in exactly the case it matters most. Turns past about 140
     * degrees now aim below drift speed. Not to zero, though: a bubble that stalls mid-corner
     * reads as a hitch rather than as weight.
     */
    static final float CORNER = 0.83f;

    /**
     * How long a full turn takes. A charge used to snap the heading straight at its target the
     * frame it was called, which read as a teleport of direction: the bubble was travelling one
     * way and then simply was not. Swinging it round at a fixed rate makes the charge a turn you
     * can watch, and gives a word an instant of grace to be typed out from under it.
     *
     * The largest turn it ever actually needs is half of this — a reversal, at 0.35s — since any
     * heading is at most 180 degrees from any other.
     */
    static final float TURN_TIME = 0.7f;
    /** Radians per second, from the above. */
    static final float TURN_RATE = 6.28319f / TURN_TIME;

    /** Sends a fresh squishy in, moving diagonally so it starts crossing the field at once. */
    void enter(int entry, Layout L, java.util.Random rnd) {
        who = entry;
        squishes = 0;
        pulse = 0f;
        chase = null;
        x = (L.playLeft + L.playRight) / 2f;
        y = L.playTop + (L.dangerY - L.playTop) * 0.35f;
        // A shallow angle would have it skimming one wall for seconds at a time.
        double a = (0.25 + rnd.nextFloat() * 0.5) * Math.PI * (rnd.nextBoolean() ? 1 : -1);
        float sp = SPEED * L.w;
        vx = sp * (float) Math.cos(a);
        vy = sp * (float) Math.sin(a);
    }

    void leave() {
        who = -1;
        chase = null;
    }

    boolean out() {
        return who < 0;
    }

    /** Current radius. */
    float radius(Layout L) {
        float grown = Math.min(GROW_MAX, squishes * GROW);
        return L.enemyR * (START + grown) * (1f + 0.18f * pulse);
    }

    /** 0..1 glow, climbing with every word taken. */
    float glow() {
        float g = squishes / GLOW_FULL;
        return (g > 1f ? 1f : g) * 0.7f + 0.3f * pulse;
    }

    /** Sends it at a word. Nothing else changes: the charge is a steer, not a teleport. */
    void charge(GameCore.Enemy e) {
        chase = e;
    }

    /**
     * Moves it, bounces it off the field edges, and squishes whatever it runs into.
     *
     * @return words squished this step, so the caller can react to the moment
     */
    int update(GameCore c, float dt, Layout L) {
        if (out()) return 0;
        pulse = pulse - dt * 3.2f;
        if (pulse < 0f) pulse = 0f;

        // Forget a target that something else has already dealt with.
        if (chase != null && (!chase.typeable() || !c.enemies.contains(chase))) chase = null;

        // Heading and speed are both driven rather than assigned: the direction swings at
        // TURN_RATE, the speed accelerates toward whatever it is currently aiming for, and a turn
        // lowers that aim so it slows through the corner and winds back up coming out of it.
        //
        // Read back off the velocity each frame rather than kept as its own field: every bounce
        // reflects the velocity, and a stored copy of the speed would have to be talked out of
        // drifting away from it.
        float drift = SPEED * L.w;
        float head = (float) Math.atan2(vy, vx);
        float have = (float) Math.sqrt(vx * vx + vy * vy);
        float aim = drift;

        if (chase != null) {
            float dx = c.enemyCentreX(chase) - x, dy = chase.y - y;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d > 1f) {
                float turn = wrapPi((float) Math.atan2(dy, dx) - head);
                // How far off the target it is pointing, 0 dead on to 1 dead behind. The aim is
                // scaled by it before the turn is clamped, so the slowdown reflects the whole
                // turn still to come rather than this frame's slice of it.
                float off = Math.abs(turn) / 3.14159f;
                aim = drift * CHARGE_RATE * (1f - CORNER * off);
                float mostTurn = TURN_RATE * dt;
                if (turn > mostTurn) turn = mostTurn;
                else if (turn < -mostTurn) turn = -mostTurn;
                head += turn;
            }
        }

        // Same rate up and down, measured over the whole drift-to-charge range, so SPIN_UP means
        // the same thing whichever way the speed is going.
        float mostSpeed = drift * (CHARGE_RATE - 1f) / SPIN_UP * dt;
        if (have < aim) have = Math.min(aim, have + mostSpeed);
        else have = Math.max(aim, have - mostSpeed);
        vx = (float) Math.cos(head) * have;
        vy = (float) Math.sin(head) * have;

        x += vx * dt;
        y += vy * dt;

        float r = radius(L);
        // Walls. Reflected and pushed clear, so a fast charge into a corner cannot stick.
        // Bounces are deliberately exempt from the turn limit: a reflection eased over a third of
        // a second is a bubble travelling through the wall while it comes about. The limit is on
        // steering, which is a continuous input; a bounce is an impulse.
        if (x - r < L.playLeft) {
            x = L.playLeft + r;
            vx = Math.abs(vx);
        } else if (x + r > L.playRight) {
            x = L.playRight - r;
            vx = -Math.abs(vx);
        }
        if (y - r < L.playTop) {
            y = L.playTop + r;
            vy = Math.abs(vy);
        } else if (y + r > L.dangerY) {
            y = L.dangerY - r;
            vy = -Math.abs(vy);
        }

        return strike(c, L, r);
    }

    /**
     * An angle folded into -PI..PI, so a turn always takes the short way round. Without this a
     * steer across the -PI/PI seam goes the long way, which looks like a panic spin.
     *
     * A loop rather than a modulo: the input is the difference of two atan2 results, so it is
     * inside -2PI..2PI and one pass is always enough.
     */
    private static float wrapPi(float a) {
        while (a > 3.14159f) a -= 6.28319f;
        while (a < -3.14159f) a += 6.28319f;
        return a;
    }

    /**
     * Squishes any word the bubble is touching, and bounces off it.
     *
     * Downward through the list, because squishing a word can destroy it and mutate the list
     * under the loop — the trap this codebase has hit most.
     */
    private int strike(GameCore c, Layout L, float r) {
        int took = 0;
        for (int i = c.enemies.size() - 1; i >= 0; i--) {
            if (i >= c.enemies.size()) continue;
            GameCore.Enemy e = c.enemies.get(i);
            if (!e.typeable()) continue;

            // A word is a row, so the nearest point of its box is what the bubble meets.
            float half = L.wordWidth(e.word.length) / 2f;
            float ex = c.enemyCentreX(e);
            float nx = x < ex - half ? ex - half : x > ex + half ? ex + half : x;
            float ny = y < e.y - L.enemyR ? e.y - L.enemyR
                    : y > e.y + L.enemyR ? e.y + L.enemyR : y;
            float dx = x - nx, dy = y - ny;
            if (dx * dx + dy * dy > r * r) continue;

            // Bounce off the face it met. Dead centre gives no normal to work with, so fall
            // back to sending it back the way it came.
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 0.01f) {
                float ux = dx / len, uy = dy / len;
                float dot = vx * ux + vy * uy;
                if (dot < 0f) {
                    vx -= 2f * dot * ux;
                    vy -= 2f * dot * uy;
                }
            } else {
                vy = -vy;
            }

            if (chase == e) chase = null;
            c.buddySquish(e, L);
            squishes++;
            pulse = 1f;
            took++;
        }
        return took;
    }
}
