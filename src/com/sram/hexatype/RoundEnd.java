package com.sram.hexatype;

/**
 * How a run ends, in three beats that hand off to each other:
 *
 * <ol>
 *   <li>{@link #swirl} — the words still on the field spiral away with trails while the world
 *       drains, over {@link GameCore#DEATH_TIME}.
 *   <li>{@link #dance} — the dumplings this run freed bounce in the middle of the summary, once
 *       the GAME OVER text has faded up.
 *   <li>{@link #homeward} — and then carry themselves to the display case, trailing stars, over
 *       the first moment of the title screen.
 * </ol>
 *
 * Its own file because it is a sequence rather than a screen: it draws over the field, over the
 * summary, and over the title, and none of those owns it. Nothing here holds state — every
 * position is a function of a countdown in {@link GameCore} and the entry's own index, so the
 * whole sequence is reproducible from a frozen frame.
 */
final class RoundEnd extends Draw {

    private RoundEnd() {}

    /** Turns a swirling tile makes before it is gone. */
    private static final float SWIRL_TURNS = 1.35f;
    /** Trail samples behind a swirling tile, and how far back in the animation each one sits. */
    private static final int SWIRL_TRAIL = 7;
    private static final float SWIRL_STEP = 0.028f;

    /**
     * Where the run's haul dances on the summary, as a fraction of view height. Below the accuracy
     * block rather than above it: at 0.405 the row landed on the score and crowded the dumpling,
     * and this band is empty on every summary — and nearer the middle of the screen besides.
     */
    private static final float ROW_Y = 0.565f;

    // ---- 1. the swirl -------------------------------------------------------

    /**
     * The words that were on screen when the last life went, spiralling away.
     *
     * Per tile rather than per word: a word coming apart into its letters is what the field
     * already does when a word is cleared, so death borrowing the same idea reads as of a piece
     * with it. The spiral is analytic — angle and radius from the tile's own start position and
     * the countdown — which is what lets the trail be drawn by evaluating the same path a few
     * frames into the past instead of recording where anything has been.
     */
    static void swirl(Painter p, GameCore c, Layout L) {
        if (!c.dying()) return;
        float u = c.deathProgress();
        float cx = L.w / 2f, cy = (L.playTop + L.dangerY) / 2f;

        for (int n = 0; n < c.enemies.size(); n++) {
            GameCore.Enemy e = c.enemies.get(n);
            for (int i = 0; i < e.word.length; i++) {
                if (e.resolved(i)) continue;
                float sx = c.tileX(e, i, L), sy = e.y;
                float a0 = (float) Math.atan2(sy - cy, sx - cx);
                float r0 = (float) Math.sqrt((sx - cx) * (sx - cx) + (sy - cy) * (sy - cy));
                // Alternating spin per tile, so a word unravels rather than sliding as a block.
                float dir = ((n + i) & 1) == 0 ? 1f : -1f;
                int col = Glyph.COLOR[e.word[i]];

                for (int k = SWIRL_TRAIL; k >= 0; k--) {
                    float t = u - k * SWIRL_STEP;
                    if (t <= 0f) continue;
                    // Eased so it hangs for an instant and then goes, and drawn back-to-front so
                    // the tile itself lands on top of its own trail.
                    float ease = t * t;
                    float ang = a0 + dir * SWIRL_TURNS * 6.28319f * ease;
                    // Inward only. An outward term looked better in isolation and threw tiles
                    // that started near the walls clean off the screen — a swirl has to stay
                    // somewhere it can be watched, so they wind down into the middle instead.
                    float rad = r0 * (1f - 0.72f * ease);
                    float px = cx + rad * (float) Math.cos(ang);
                    float py = cy + rad * (float) Math.sin(ang);
                    float rr = L.enemyR * (1f - 0.45f * ease) * (k == 0 ? 1f : 0.62f);
                    int a = (int) ((k == 0 ? 235 : 90) * (1f - ease) * (1f - (float) k / 9f));
                    if (a <= 2) continue;
                    if (k == 0) {
                        p.fillPoly(Glyph.hex(px, py, rr), Glyph.withAlpha(col, a / 3));
                        p.strokePoly(Glyph.hex(px, py, rr), Glyph.withAlpha(col, a),
                                rr * 0.14f);
                        Kawaii.draw(p, e.word[i], px, py, rr * 0.62f, Glyph.withAlpha(col, a),
                                1f, 0f);
                    } else {
                        p.fillPoly(Glyph.hex(px, py, rr), Glyph.withAlpha(col, a));
                    }
                }
            }
        }
    }

    // ---- 2. the dance -------------------------------------------------------

    /** How many dumplings this run freed. */
    static int hauled(GameCore c) {
        return Collect.owned(c.roundPrizes);
    }

    /** Radius one of them dances at, shrinking as the row fills so a big haul still fits. */
    static float rowR(Layout L, int count) {
        float room = (L.playRight - L.playLeft) / Math.max(1, count);
        return Math.min(L.h * 0.028f, room * 0.42f);
    }

    static float rowY(Layout L) {
        return L.h * ROW_Y;
    }

    /** Where the nth of {@code count} sits along the row. */
    static float rowX(Layout L, int n, int count) {
        float step = rowR(L, count) * 2.35f;
        return L.w / 2f + (n - (count - 1) / 2f) * step;
    }

    /**
     * The run's haul, bouncing. Starts once the summary has finished fading up, so it reads as
     * the dumplings coming out to be counted rather than as part of the screen arriving.
     *
     * A duplicate is in here alongside a first win: you opened the basket either way.
     */
    static void dance(Painter p, GameCore c, Layout L) {
        int count = hauled(c);
        if (count == 0 || c.state != GameCore.OVER || c.overFade() < 1f) return;
        float since = c.time - (GameCore.DEATH_TIME + GameCore.OVER_FADE);
        float in = Math.min(1f, since / 0.3f);
        if (in <= 0f) return;

        float r = rowR(L, count);
        float y = rowY(L);
        int n = 0;
        for (int i = 0; i < Collect.COUNT; i++) {
            if (!Collect.has(c.roundPrizes, i)) continue;
            // Offset phases so the row bounces as a line rather than in lockstep.
            float phase = c.clock * 6.5f - n * 0.7f;
            float hop = Math.abs((float) Math.sin(phase));
            // Kept low: at 0.85r the top of a bounce reached into the accuracy readout above.
            float bounce = r * 0.60f * hop * in;
            // Widest at the bottom of the bounce, tallest at the top: the squash sells the weight.
            float squash = 1f + 0.16f * (float) Math.cos(phase * 2f);
            float px = rowX(L, n, count);
            p.fillEllipse(px, y + r * 0.95f, r * 0.62f * squash, r * 0.16f,
                    Glyph.withAlpha(0xFF000000, (int) (70 * in)));
            Trinket.draw(p, i, px, y - bounce, r / squash, c.clock, true, in);
            n++;
        }
    }

    // ---- 3. the flight home -------------------------------------------------

    /** Stars strung out behind a flyer. */
    private static final int TRAIL = 7;
    private static final float TRAIL_STEP = 0.045f;

    /**
     * How much of the flight separates one departure from the next, and the shortest a single
     * trip may be squeezed to.
     *
     * They leave in a line and land in a line: each one arriving in its own moment is what lets a
     * chime be played per landing instead of one chord for the lot. The whole haul used to leave
     * staggered and then converge on the same instant, which looked fine and sounded like a single
     * event.
     */
    private static final float LEAD = 0.09f, MIN_SPAN = 0.30f;

    /** Flight progress at which flyer {@code n} of {@code count} sets off. */
    static float lead(int n, int count) {
        // A big haul packs its departures tighter rather than leaving the last one no flight at
        // all — the alternative is a dumpling that starts and lands on the same frame.
        float gap = Math.min(LEAD, (1f - MIN_SPAN) / Math.max(1, count - 1));
        return n * gap;
    }

    /** How much of the flight one trip lasts. The same for all of them, so they fly in step. */
    static float span(int count) {
        return 1f - lead(count - 1, count);
    }

    /** Flight progress at which flyer {@code n} is taken into the case. The last lands at 1. */
    static float arrival(int n, int count) {
        return lead(n, count) + span(count);
    }

    /** 0..1 along flyer {@code n}'s own trip, at flight progress {@code u}. */
    static float trip(float u, int n, int count) {
        return clamp01((u - lead(n, count)) / span(count));
    }

    /**
     * The haul carrying itself from where it was dancing to the display case, over the first
     * moment of the title screen.
     *
     * The case is a moving target — the badge drifts along its arc — so the destination is read
     * live rather than captured, and each flyer aims at wherever the badge is this frame.
     */
    static void homeward(Painter p, GameCore c, Layout L) {
        if (!c.homing()) return;
        int count = hauled(c);
        if (count == 0) return;
        float u = c.homeProgress();
        float tx = Showcase.iconCx(L, c.clock), ty = Showcase.iconCy(L, c.clock);
        float r0 = rowR(L, count), y0 = rowY(L);

        int n = 0;
        for (int i = 0; i < Collect.COUNT; i++) {
            if (!Collect.has(c.roundPrizes, i)) continue;
            float x0 = rowX(L, n, count);
            float t = trip(u, n, count);

            for (int k = TRAIL; k >= 0; k--) {
                float ts = t - k * TRAIL_STEP;
                if (ts <= 0f) continue;
                // Smoothstepped rather than eased purely out: a pure ease-out covered three
                // quarters of the distance in the first half of the flight, so it was over before
                // the trail had strung out behind it. This leaves and lands, with travel between.
                float ease = ts * ts * (3f - 2f * ts);
                float px = x0 + (tx - x0) * ease;
                float py = y0 + (ty - y0) * ease;
                // Bowed off the straight line, most at the middle of the trip.
                py -= L.h * 0.08f * (float) Math.sin(ease * 3.14159f);
                if (k == 0) {
                    // Shrinking away almost to nothing as it lands, so it reads as being taken
                    // into the case rather than as three dumplings stacking up on the badge.
                    float rr = r0 * (1f - 0.85f * ease);
                    Trinket.draw(p, i, px, py, rr, c.clock, true, 1f - ease * ease);
                } else {
                    float back = 1f - (float) k / (TRAIL + 1f);
                    float sr = r0 * 0.5f * (1f - 0.6f * ease) * back;
                    int a = (int) (235 * (1f - ease * ease) * back);
                    if (a <= 2 || sr <= 0.2f) continue;
                    int tint = k % 2 == 0 ? YELLOW : Collect.TIER_COLOR[Collect.TIER[i]];
                    p.fillPoly(star(px, py, sr, sr * 0.40f, 4, ts * 5f + k),
                            Glyph.withAlpha(tint, a));
                }
            }
            n++;
        }
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : v > 1f ? 1f : v;
    }
}
