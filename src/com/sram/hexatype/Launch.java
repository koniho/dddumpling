package com.sram.hexatype;

/**
 * The send-off when a run starts: the squishy the display case was showing swells out of its
 * badge in a pip of stars, bounces once where it stood, then leaps and bounces off the top of
 * the screen. Stage 1 begins as it drops away.
 *
 * Only when the case is parked on something collected — an uncollected entry is a silhouette
 * with a question mark on it, and sending that off would be sending off nothing. So the title
 * screen still starts instantly for a player who has not won anything yet.
 *
 * Position is a pure function of {@link GameCore#launchT}, and the badge's own drift is read off
 * the clock frozen at the press, so the squishy leaves from exactly where it was sitting.
 */
final class Launch extends Draw {

    private Launch() {}

    /** Total length of the send-off. The title screen dissolves under the first half of it. */
    static final float TIME = 1.05f;

    /** Grown and pipped by here. */
    static final float POP = 0.24f;
    /** First bounce back down. */
    static final float LAND = 0.50f;
    /** Contact with the top edge, and the second pip. */
    static final float TOP = 0.80f;

    /** How long a pip's stars last. */
    private static final float PIP = 0.20f;

    /** Size it swells to, as a multiple of the size it sat at in the badge. */
    private static final float GROWN = 2.1f;

    /** 0..1 through the send-off. */
    static float progress(GameCore c) {
        return 1f - c.launchT / TIME;
    }

    static void draw(Painter p, GameCore c, Layout L) {
        if (c.launchT <= 0f || c.launchWho < 0) return;
        float u = progress(c);
        float x0 = Showcase.iconCx(L, c.launchClock);
        float y0 = Showcase.iconCy(L, c.launchClock);
        float r0 = Showcase.iconR(L) * 0.62f;
        float roof = L.topSafe + r0 * GROWN;

        // Sideways: it walks to the middle of the screen on the way up, so the bounce off the
        // roof lands centre stage however far round the arc the badge had drifted.
        float across = ease(Math.min(1f, u / TOP));
        float x = x0 + (L.w / 2f - x0) * across;

        float y = y0;
        if (u >= POP && u < LAND) {
            // First bounce: a hop in place, up and back down.
            float v = (u - POP) / (LAND - POP);
            y = y0 - (y0 - roof) * 0.22f * (float) Math.sin(v * 3.1416f);
        } else if (u >= LAND && u < TOP) {
            // The leap: fast off the ground, decelerating into the roof.
            float v = (u - LAND) / (TOP - LAND);
            y = y0 + (roof - y0) * (1f - (1f - v) * (1f - v));
        } else if (u >= TOP) {
            // And off it again, gathering speed downward as the field takes over.
            float v = (u - TOP) / (1f - TOP);
            y = roof + (y0 - roof) * 0.85f * v * v;
        }

        // Swelling out of the badge with an overshoot, then squashed by each impact.
        float grow = u < POP ? GROWN * over(u / POP) : GROWN;
        float r = r0 * grow * (1f - 0.26f * hit(u, LAND) - 0.30f * hit(u, TOP));
        // Fading only right at the end, so it is gone by the first word of the wave.
        float fade = u < 0.9f ? 1f : Math.max(0f, (1f - u) / 0.1f);

        // Reaches chosen to clear the body: the stars go behind it, so a burst tucked inside the
        // silhouette is a burst nobody sees.
        stars(p, c, L, x0, y0, u - POP * 0.55f, r0 * 3.6f, fade);
        stars(p, c, L, x, y, u - LAND, r * 1.9f, fade);
        stars(p, c, L, x, y, u - TOP, r * 2.4f, fade);
        Trinket.draw(p, c.launchWho, x, y, r, c.clock, true, fade);
    }

    /** Ease out, for the walk to the middle of the screen. */
    private static float ease(float v) {
        return 1f - (1f - v) * (1f - v);
    }

    /** Overshoot: past 1 and back, so the swell has a snap in it. */
    private static float over(float v) {
        if (v >= 1f) return 1f;
        return 1f - (1f - v) * (1f - v) * (1f - 2.2f * v);
    }

    /** A brief 1..0 spike as {@code u} passes {@code at}, for the squash of an impact. */
    private static float hit(float u, float at) {
        float d = Math.abs(u - at);
        return d > 0.07f ? 0f : 1f - d / 0.07f;
    }

    /**
     * The pip: a ring of stars flung outward and fading.
     *
     * Deterministic, off the entry's own index rather than the RNG — the preview harness
     * hash-compares its frames between runs, so anything random here would make every check
     * differ. Eight arms at a fixed offset read as a sparkle anyway.
     */
    private static void stars(Painter p, GameCore c, Layout L, float x, float y, float age,
            float reach, float fade) {
        if (age < 0f || age >= PIP) return;
        float v = age / PIP;
        int a = (int) (235 * (1f - v) * fade);
        if (a <= 2) return;
        int tint = Collect.TIER_COLOR[Collect.TIER[c.launchWho]];
        float rot = c.launchWho * 0.7f;
        for (int k = 0; k < 8; k++) {
            double ang = rot + k * 0.7854f;
            float d = reach * (0.35f + 1.15f * v);
            float sx = x + d * (float) Math.cos(ang);
            float sy = y + d * (float) Math.sin(ang);
            float sr = reach * 0.30f * (1f - 0.65f * v);
            // Alternating so the burst has two colours in it rather than one flat spray.
            int col = Glyph.withAlpha(k % 2 == 0 ? GOLD : tint, a);
            p.fillPoly(star(sx, sy, sr, sr * 0.40f, 4, (float) ang), col);
        }
        // A shockwave ring under them, which is what makes the pip read as an impact.
        p.strokeCircle(x, y, reach * (0.3f + 1.1f * v), Glyph.withAlpha(INK, (int) (a * 0.5f)),
                reach * 0.09f * (1f - v));
    }
}
