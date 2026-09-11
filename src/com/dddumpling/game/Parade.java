package com.dddumpling.game;

/**
 * The parade that closes an interlude something was won in: the collection marches in from the
 * left and forms a line, the new one drops into the end of it, and then they all march off to
 * the right. The next stage starts when they are gone.
 *
 * Three movements over one progress value, so the whole thing is a pure function of
 * {@link GameCore#paradeProgress()} and any frame of it can be rendered by the harness.
 */
final class Parade extends Draw {

    /** Figures in the line, the new one included. More than this will not fit legibly. */
    static final int LINE = 7;

    /**
     * Movement boundaries, as fractions of the parade. In, join, then off.
     *
     * The join gets a slightly larger share than an even split would give it, because it is the
     * beat the whole sequence exists for — the other two are just travel.
     */
    static final float IN_END = 0.34f, JOIN_END = 0.62f;

    private Parade() {}

    /** Vertical centre of the line, as a fraction of view height. */
    private static final float LINE_Y = 0.47f;

    static void draw(Painter p, GameCore c, Layout L, float fade) {
        float s = L.unit;
        float cx = L.w / 2f;
        float cy = L.h * LINE_Y;
        float t = c.paradeProgress();

        // Sized so a full line of LINE figures spans most of the width and still leaves a
        // margin — at any more than this the newcomer at the right end runs off the screen.
        // Capped on height too, so a short wide screen does not get a row of giants.
        float r = Math.min(L.w * 0.048f, L.h * 0.030f);
        float step = r * 2.35f;

        int[] mates = new int[LINE - 1];
        int n = companions(c, mates);
        // Slots are laid out for whoever actually turned up, so a line of three is centred
        // rather than sitting where a line of seven would have started.
        int slots = n + 1;
        float left = cx - step * (slots - 1) / 2f;

        // Two offsets applied to the whole line: one bringing the formation in from the left,
        // one taking it off to the right. A formation, not a race — giving each figure its own
        // start point at the left edge piled them all up on top of each other and read as a
        // heap sliding in rather than a procession walking in.
        float travel = L.w * 0.58f + step * slots;
        float entry = t >= IN_END ? 0f : (1f - easeOut(t / IN_END)) * travel;
        float exit = t <= JOIN_END ? 0f
                : easeIn((t - JOIN_END) / (1f - JOIN_END)) * travel;
        float shift = exit - entry;

        for (int k = 0; k < n; k++) {
            // Out of phase down the line, so they bob along independently instead of gliding.
            float bob = (float) Math.abs(Math.sin(t * 16f + k * 0.9f)) * r * 0.16f;
            one(p, mates[k], left + k * step + shift, cy - bob, r, c.clock, fade);
        }

        // The new one drops into the end of the line once the others are in place.
        float joinSlot = left + n * step + shift;
        if (t > IN_END) {
            float j = Math.min(1f, (t - IN_END) / (JOIN_END - IN_END));
            float drop = (1f - easeOut(j)) * L.h * 0.22f;
            float pop = 1f + 0.22f * (1f - j) * (1f - j);
            one(p, c.prize, joinSlot, cy - drop, r * pop, c.clock, fade);
            if (j > 0.7f) {
                // Burst on landing, fading as the line settles.
                float b = (j - 0.7f) / 0.3f;
                for (int k = 0; k < 6; k++) {
                    double a = 6.283 * k / 6 + 0.4;
                    spark(p, joinSlot + (float) Math.cos(a) * r * (1.1f + b * 1.5f),
                            cy + (float) Math.sin(a) * r * (0.9f + b * 1.2f),
                            r * 0.30f * (1f - b), fadeBy(GOLD, fade * (1f - b)));
                }
            }
        }

        // Ground line under them, so they read as walking rather than floating. It stays put
        // while they move over it, and fades up with their arrival so it is not lying there
        // before anybody is standing on it. Sized to the line that turned up, not a full one.
        float ground = Math.min(1f, t / IN_END);
        p.fillPoly(pill(cx, cy + r * 1.34f, step * (slots - 1) / 2f + r * 1.2f, r * 0.05f, 6),
                fadeBy(Glyph.withAlpha(INK, 45), fade * ground));

        captions(p, c, L, cx, cy, r, t, n, fade);
    }

    private static void captions(Painter p, GameCore c, Layout L, float cx, float cy, float r,
            float t, int n, float fade) {
        float s = L.unit;
        int tint = Collect.TIER_COLOR[Collect.TIER[c.prize]];

        // The parade closes a winning round, so it carries the stage announcement the status
        // report would otherwise have made.
        p.text("STAGE " + c.stage + " CLEAR", cx, L.h * 0.23f, type(s * 0.86f), fadeBy(GOLD, fade),
                Painter.CENTER, true);

        // The name arrives with the figure, not before: until it has landed there is nothing
        // to announce.
        if (t > IN_END) {
            float j = Math.min(1f, (t - IN_END) / (JOIN_END - IN_END));
            float pop = 1f + 0.30f * (1f - j) * (1f - j);
            p.text(Collect.NAME[c.prize], cx, L.h * 0.325f, type(s * 1.15f * pop),
                    fadeBy(INK, fade * j), Painter.CENTER, true);
            p.text(c.prizeNew ? "JOINS THE COLLECTION" : "BACK IN THE LINE", cx,
                    L.h * 0.325f + s * 1.05f, type(s * 0.62f), fadeBy(tint, fade * j),
                    Painter.CENTER, true);
        } else {
            p.text("THE COLLECTION", cx, L.h * 0.325f, type(s * 0.92f), fadeBy(INK_DIM, fade),
                    Painter.CENTER, true);
        }

        int have = Collect.owned(c.collected);
        p.text(have + " OF " + Collect.COUNT + " COLLECTED", cx, cy + r * 2.45f, type(s * 0.60f),
                fadeBy(have >= Collect.COUNT ? GOLD : INK, fade), Painter.CENTER, true);
        // Only worth saying when the line is a sample rather than the whole thing.
        if (have > LINE) {
            p.text("AND " + (have - (n + 1)) + " MORE", cx, cy + r * 2.45f + s * 0.85f,
                    type(s * 0.52f), fadeBy(INK_DIM, fade), Painter.CENTER, false);
        }
    }

    /**
     * Fills {@code out} with collected entries other than the prize, walking forward from the
     * prize through the catalogue so the line starts with its own family and only then spreads.
     *
     * @return how many turned up, which can be zero on the very first win
     */
    static int companions(GameCore c, int[] out) {
        int n = 0;
        for (int k = 1; k < Collect.COUNT && n < out.length; k++) {
            int i = Showcase.wrap(c.prize + k);
            if (i != c.prize && Collect.has(c.collected, i)) out[n++] = i;
        }
        return n;
    }

    private static float easeOut(float t) {
        float u = 1f - t;
        return 1f - u * u * u;
    }

    private static float easeIn(float t) {
        return t * t * t;
    }

    private static void one(Painter p, int i, float cx, float cy, float r, float clock,
            float fade) {
        p.fillCircle(cx, cy, r * 1.16f, fadeBy(Glyph.withAlpha(Collect.BODY[i], 15), fade));
        Trinket.draw(p, i, cx, cy, r, clock, true, fade);
    }

    /** Four-point twinkle, as used by the stage and story vignettes. */
    private static void spark(Painter p, float cx, float cy, float r, int col) {
        if ((col >>> 24) <= 2 || r <= 0.2f) return;
        p.fillPoly(new float[] {cx, cy - r, cx + r * 0.28f, cy, cx, cy + r, cx - r * 0.28f, cy},
                col);
        p.fillPoly(new float[] {cx - r, cy, cx, cy - r * 0.28f, cx + r, cy, cx, cy + r * 0.28f},
                col);
    }
}
