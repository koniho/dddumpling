package com.sram.hexatype;

/**
 * The story popup: tap a collected entry in the display case and it tells you where it lives
 * and what its family gets up to, over a small looping animation.
 *
 * The animation is one of ten reusable beats from {@link Lore}, cast with the entry itself and
 * a family member. Every beat is a <em>loop</em> — the popup stays open until it is dismissed,
 * so a one-shot vignette would play once and then sit frozen, which reads as a hang.
 */
final class Storybook extends Draw {

    /** Seconds per loop of the animation. */
    static final float LOOP = 2.4f;

    private Storybook() {}

    /** How long the panel takes to spring open. */
    private static final float OPEN_TIME = 0.28f;

    static void draw(Painter p, GameCore c, Layout L) {
        if (!c.storyOpen()) return;
        int i = c.story;
        float s = L.unit;
        float cx = L.w / 2f;

        // Springs open: the panel scales up about its centre over the first fraction of a
        // second. Everything inside is laid out at full size and the whole thing is drawn
        // small, rather than each element easing on its own.
        float open = Math.min(1f, c.storyT / OPEN_TIME);
        float grow = ease(open);

        float pw = Math.min(L.w * 0.92f, s * 21f) / 2f;
        float ph = s * 6.2f;
        float cy = L.h * 0.47f;

        p.fillRect(0, 0, L.w, L.h, Glyph.withAlpha(0xFF0D0A18, (int) (200 * open)));

        float hw = pw * grow, hh = ph * grow;
        int tint = Collect.TIER_COLOR[Collect.TIER[i]];
        p.fillRect(cx - hw, cy - hh, cx + hw, cy + hh, 0xFF2A2348);
        p.strokePoly(new float[] {cx - hw, cy - hh, cx + hw, cy - hh, cx + hw, cy + hh,
                cx - hw, cy + hh}, Glyph.withAlpha(tint, 200), s * 0.06f);
        // Nothing legible until the panel is most of the way open; text scaled down with it
        // just looks like a rendering fault.
        if (open < 0.75f) return;

        // The stage sits in the top third, on its own slightly darker band so the scene reads
        // as a scene rather than as decoration behind the text.
        float stageH = s * 2.25f;
        float stageCy = cy - hh + stageH;
        p.fillRect(cx - hw + s * 0.22f, cy - hh + s * 0.22f, cx + hw - s * 0.22f,
                stageCy + stageH - s * 0.10f, Glyph.withAlpha(0xFF1B1730, 200));
        p.save();
        p.clipRect(cx - hw + s * 0.22f, cy - hh + s * 0.22f, cx + hw - s * 0.22f,
                stageCy + stageH - s * 0.10f);
        beat(p, i, cx, stageCy, s * 1.15f, (c.storyT / LOOP) % 1f, c.clock);
        p.restore();

        float y = stageCy + stageH + s * 1.05f;
        p.text(Collect.NAME[i], cx, y, s * 0.98f, INK, Painter.CENTER, true);
        p.text(Lore.WHERE[i], cx, y + s * 0.80f, s * 0.52f, fadeBy(tint, 0.85f),
                Painter.CENTER, false);

        float line = y + s * 1.95f;
        for (int k = 0; k < Lore.LINES; k++) {
            p.text(Lore.STORY[i][k], cx, line + k * s * 0.92f, s * 0.60f, INK,
                    Painter.CENTER, false);
        }

        float pulse = 0.55f + 0.45f * (float) Math.sin(c.clock * 3.2f);
        p.text("TAP TO CLOSE", cx, cy + hh - s * 0.55f, s * 0.54f,
                Glyph.withAlpha(INK_DIM, (int) (255 * pulse)), Painter.CENTER, true);
    }

    /** Ease-out-back, so the panel overshoots a little and settles. */
    private static float ease(float t) {
        if (t >= 1f) return 1f;
        float c1 = 1.70158f, c3 = c1 + 1f;
        float u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }

    // ---- the beats ----------------------------------------------------------

    /**
     * Plays vignette {@code Lore.BEAT[i]} with entry {@code i} and its family.
     *
     * @param t 0..1 through the loop; every beat must end where it started
     */
    static void beat(Painter p, int i, float cx, float cy, float r, float t, float clock) {
        int mate = Lore.PARTNER[i];
        int third = Lore.third(i);
        switch (Lore.BEAT[i]) {
            case Lore.HANDOFF: handoff(p, i, mate, third, cx, cy, r, t, clock); break;
            case Lore.STACK: stack(p, i, mate, third, cx, cy, r, t, clock); break;
            case Lore.PUSH: push(p, i, mate, third, cx, cy, r, t, clock); break;
            case Lore.PEEK: peek(p, i, mate, cx, cy, r, t, clock); break;
            case Lore.TUMBLE: tumble(p, i, mate, cx, cy, r, t, clock); break;
            case Lore.BOUNCE: bounce(p, i, mate, cx, cy, r, t, clock); break;
            case Lore.PICNIC: picnic(p, i, mate, third, cx, cy, r, t, clock); break;
            case Lore.CARRY: carry(p, i, mate, cx, cy, r, t, clock); break;
            case Lore.CHEER: cheer(p, i, mate, cx, cy, r, t, clock); break;
            default: seek(p, i, mate, cx, cy, r, t, clock); break;
        }
    }

    /** Two lob a little one back and forth, once each way per loop. */
    private static void handoff(Painter p, int a, int b, int c, float cx, float cy, float r,
            float t, float clock) {
        float d = r * 2.05f;
        one(p, a, cx - d, cy + r * 0.34f, r * 0.94f, clock);
        one(p, b, cx + d, cy + r * 0.34f, r * 0.94f, clock);
        // Two arcs per loop, alternating direction, so it returns to where it began.
        float lap = (t * 2f) % 1f;
        float dir = t < 0.5f ? 1f : -1f;
        float x = cx + dir * (-d + 2f * d * lap);
        float y = cy + r * 0.26f - (float) Math.sin(lap * Math.PI) * r * 1.35f;
        one(p, c, x, y, r * 0.50f, clock);
    }

    /** Three stacked up, wobbling, with the top one hopping off and back on. */
    private static void stack(Painter p, int a, int b, int c, float cx, float cy, float r,
            float t, float clock) {
        float wob = (float) Math.sin(t * 6.283f * 2f) * r * 0.13f;
        float hop = (float) Math.abs(Math.sin(t * 3.141f)) * r * 0.55f;
        one(p, a, cx, cy + r * 1.00f, r * 0.94f, clock);
        one(p, b, cx + wob * 0.6f, cy + r * 0.06f, r * 0.78f, clock);
        one(p, c, cx + wob, cy - r * 0.78f - hop, r * 0.62f, clock);
    }

    /** Two lean into something heavy; it slides out and settles back. */
    private static void push(Painter p, int a, int b, int c, float cx, float cy, float r,
            float t, float clock) {
        float shove = (float) Math.sin(t * 6.283f);
        float lean = 1f + 0.14f * Math.abs(shove);
        one(p, a, cx - r * 2.10f + shove * r * 0.30f, cy + r * 0.20f, r * 0.82f, clock, lean);
        one(p, b, cx - r * 0.95f + shove * r * 0.34f, cy + r * 0.24f, r * 0.76f, clock, lean);
        // The load: bigger than either of them, which is the joke, and set well clear of them
        // or the three read as a queue rather than as a shove.
        one(p, c, cx + r * 1.55f + shove * r * 0.42f, cy + r * 0.04f, r * 1.10f, clock);
        for (int k = 1; k <= 3; k++) {
            spark(p, cx + r * (2.7f + 0.28f * k) + shove * r * 0.4f, cy + r * 0.78f,
                    r * 0.11f, 0xFFA79DCC, 130 / k);
        }
    }

    /** One hides behind the other and pops out at the side. */
    private static void peek(Painter p, int a, int b, float cx, float cy, float r, float t,
            float clock) {
        // Only the first half of the loop shows it; the rest it stays hidden.
        float out = Math.max(0f, (float) Math.sin(t * 6.283f));
        one(p, b, cx + r * 1.30f * out + r * 0.55f, cy + r * 0.20f - out * r * 0.18f,
                r * 0.72f, clock);
        one(p, a, cx - r * 0.30f, cy + r * 0.12f, r * 1.05f, clock);
        if (out > 0.55f) {
            spark(p, cx + r * 2.05f, cy - r * 0.70f, r * 0.20f * out, 0xFFFFE07A,
                    (int) (220 * out));
        }
    }

    /** One rolls across and back while the other falls about laughing. */
    private static void tumble(Painter p, int a, int b, float cx, float cy, float r, float t,
            float clock) {
        float roll = (float) Math.sin(t * 6.283f);
        // Squash oscillates faster than the travel, which is as close to rolling as a shape
        // with no rotation can get.
        float squash = 1f + 0.22f * (float) Math.sin(t * 6.283f * 4f);
        one(p, a, cx - r * 0.40f + roll * r * 1.45f, cy + r * 0.34f, r * 0.82f, clock, squash);
        float laugh = 1f + 0.16f * (float) Math.abs(Math.sin(t * 6.283f * 3f));
        one(p, b, cx + r * 1.85f, cy + r * 0.14f, r * 0.90f, clock, laugh);
        for (int k = 0; k < 2; k++) {
            float ht = (t * 2f + k * 0.5f) % 1f;
            spark(p, cx + r * (2.5f + 0.3f * k), cy - r * 0.5f - ht * r * 0.8f,
                    r * 0.15f * (1f - ht), 0xFFFFE07A, (int) (200 * (1f - ht)));
        }
    }

    /** Bouncing on rising puffs of steam. */
    private static void bounce(Painter p, int a, int b, float cx, float cy, float r, float t,
            float clock) {
        for (int k = 0; k < 4; k++) {
            float ht = (t + k * 0.25f) % 1f;
            p.fillEllipse(cx + (k - 1.5f) * r * 0.95f, cy + r * 1.05f - ht * r * 1.9f,
                    r * 0.30f * (1f - ht * 0.5f), r * 0.13f,
                    Glyph.withAlpha(INK, (int) (70 * (1f - ht))));
        }
        for (int s = -1; s <= 1; s += 2) {
            float lift = (float) Math.abs(Math.sin(t * 6.283f + (s > 0 ? 1.57f : 0f)));
            one(p, s < 0 ? a : b, cx + s * r * 1.25f, cy + r * 0.40f - lift * r * 0.95f,
                    r * 0.86f, clock, 1f + 0.12f * (1f - lift));
        }
    }

    /** A row of three sharing something, each taking a turn to dip in. */
    private static void picnic(Painter p, int a, int b, int c, float cx, float cy, float r,
            float t, float clock) {
        int[] cast = {a, b, c};
        for (int k = 0; k < 3; k++) {
            // Each dips in its own third of the loop, so they take turns rather than nodding
            // in unison.
            float own = (t - k / 3f) % 1f;
            if (own < 0f) own += 1f;
            float dip = own < 0.25f ? (float) Math.sin(own / 0.25f * 3.141f) : 0f;
            one(p, cast[k], cx + (k - 1) * r * 1.55f, cy + r * 0.10f + dip * r * 0.34f,
                    r * 0.84f, clock, 1f + 0.10f * dip);
        }
        // The spread they are all leaning over.
        p.fillPoly(pill(cx, cy + r * 1.12f, r * 2.0f, r * 0.16f, 8),
                Glyph.withAlpha(BAMBOO, 200));
    }

    /** A big one walking with a small one riding on top. */
    private static void carry(Painter p, int a, int b, float cx, float cy, float r, float t,
            float clock) {
        float step = (float) Math.sin(t * 6.283f * 2f);
        float sway = (float) Math.sin(t * 6.283f) * r * 0.45f;
        one(p, a, cx + sway, cy + r * 0.52f + Math.abs(step) * r * 0.06f, r * 1.02f, clock,
                1f + 0.07f * Math.abs(step));
        // The rider lags the sway a little, which is what makes it look carried.
        one(p, b, cx + sway * 0.82f, cy - r * 0.72f - Math.abs(step) * r * 0.09f, r * 0.58f,
                clock);
        for (int k = 0; k < 2; k++) {
            spark(p, cx + sway - r * (1.3f + 0.4f * k), cy + r * 1.25f, r * 0.10f,
                    0xFFA79DCC, 110 / (k + 1));
        }
    }

    /** Both leap, and sparkles burst at the top of the jump. */
    private static void cheer(Painter p, int a, int b, float cx, float cy, float r, float t,
            float clock) {
        float jump = (float) Math.abs(Math.sin(t * 6.283f));
        for (int s = -1; s <= 1; s += 2) {
            one(p, s < 0 ? a : b, cx + s * r * 1.25f, cy + r * 0.42f - jump * r * 0.80f,
                    r * 0.92f, clock, 1f - 0.10f * jump);
        }
        if (jump > 0.6f) {
            float k0 = (jump - 0.6f) / 0.4f;
            for (int k = 0; k < 6; k++) {
                double ang = 6.283 * k / 6 + 0.3;
                spark(p, cx + (float) Math.cos(ang) * r * (1.0f + k0 * 1.5f),
                        cy - r * 0.35f + (float) Math.sin(ang) * r * (0.7f + k0 * 1.0f),
                        r * 0.19f * (1f - k0), 0xFFFFF3C4, (int) (235 * (1f - k0)));
            }
        }
    }

    /** Both scan left, then right, and spot something. */
    private static void seek(Painter p, int a, int b, float cx, float cy, float r, float t,
            float clock) {
        float scan = (float) Math.sin(t * 6.283f);
        one(p, a, cx - r * 1.20f + scan * r * 0.34f, cy + r * 0.20f, r * 0.92f, clock,
                1f + 0.06f * scan);
        one(p, b, cx + r * 1.20f + scan * r * 0.34f, cy + r * 0.24f, r * 0.86f, clock,
                1f - 0.06f * scan);
        // The thing they are looking for, showing itself at the far edge of each sweep.
        float found = Math.max(0f, Math.abs(scan) - 0.82f) / 0.18f;
        if (found > 0f) {
            float x = cx + (scan > 0 ? 1f : -1f) * r * 2.45f;
            spark(p, x, cy - r * 0.55f, r * 0.26f * found, 0xFFFFE07A, (int) (240 * found));
        }
    }

    // ---- parts --------------------------------------------------------------

    private static void one(Painter p, int i, float cx, float cy, float r, float clock) {
        one(p, i, cx, cy, r, clock, 1f);
    }

    /**
     * One collectible in a scene, with a soft halo so it reads against the panel.
     *
     * @param squash >1 wider and flatter, which is how the beats stretch and squeeze
     */
    private static void one(Painter p, int i, float cx, float cy, float r, float clock,
            float squash) {
        p.fillCircle(cx, cy, r * 1.22f, Glyph.withAlpha(Collect.BODY[i], 20));
        // Squash is applied to the radius pair rather than to a transform, since Painter has
        // no scale: a wider radius and a shorter one is the same thing at this size.
        Trinket.draw(p, i, cx, cy, r * squash, clock, true, 1f);
    }

    /** Four-point twinkle, the same shape the stage vignettes use. */
    private static void spark(Painter p, float cx, float cy, float r, int tint, int a) {
        if (a <= 2 || r <= 0.2f) return;
        int col = Glyph.withAlpha(tint, a);
        p.fillPoly(new float[] {cx, cy - r, cx + r * 0.28f, cy, cx, cy + r, cx - r * 0.28f, cy},
                col);
        p.fillPoly(new float[] {cx - r, cy, cx, cy - r * 0.28f, cx + r, cy, cx, cy + r * 0.28f},
                col);
    }
}
