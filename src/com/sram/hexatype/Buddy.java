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
    private static final float SPEED = 0.42f;
    /** How much faster it travels while charging at something. */
    private static final float CHARGE_RATE = 2.9f;

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

        float sp = SPEED * L.w;
        if (chase != null) {
            float dx = c.enemyCentreX(chase) - x, dy = chase.y - y;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d > 1f) {
                vx = dx / d * sp * CHARGE_RATE;
                vy = dy / d * sp * CHARGE_RATE;
            }
        }

        x += vx * dt;
        y += vy * dt;

        float r = radius(L);
        // Walls. Reflected and pushed clear, so a fast charge into a corner cannot stick.
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
