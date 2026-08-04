package com.sram.hexatype;

/**
 * The display case on the title screen: a shelf of collected squishies, scrolled one at a
 * time with the outer two keys.
 *
 * A filmstrip rather than a grid. Thirty tiles laid out at once would each be too small for
 * a face to read, and the point of the case is looking at the thing you won — so one entry
 * gets the middle of the shelf at full size and its neighbours sit half-size at the edges,
 * which also shows which way the outer keys move.
 */
final class Showcase extends Draw {

    /** Neighbours shown either side of the focused entry. */
    static final int WINGS = 1;

    private Showcase() {}

    /** Vertical centre of the shelf, as a fraction of view height. */
    static final float SHELF_Y = 0.47f;

    /** How quickly a scroll's slide settles, in slides per second. */
    static final float SLIDE_RATE = 5.5f;

    static void draw(Painter p, GameCore c, Layout L) {
        float s = L.unit;
        float cx = L.w / 2f;
        float cy = L.h * SHELF_Y;
        // Both caps matter: the width one keeps the plaque and its arrows on screen on a
        // narrow device, the height one stops the shelf swallowing a tall one.
        float r = Math.min(L.w * 0.145f, L.h * 0.075f);
        // Under 2r, so the neighbours tuck in close enough to read as the same shelf.
        float step = r * 1.95f;

        int i = c.caseIndex;
        boolean known = Collect.has(c.collected, i);

        // Plaque. Deliberately a plain panel: everything interesting on this screen is the
        // thing standing on it.
        float padX = step * (WINGS + 0.52f);
        float top = cy - r * 1.5f, bot = cy + r * 2.55f;
        p.fillRect(cx - padX, top, cx + padX, bot, Glyph.withAlpha(0xFF2A2348, 190));
        p.strokePoly(new float[] {cx - padX, top, cx + padX, top, cx + padX, bot,
                cx - padX, bot}, Glyph.withAlpha(INK, 40), s * 0.05f);

        p.text("DISPLAY CASE", cx, top - s * 1.30f, s * 0.62f, INK_DIM, Painter.CENTER, true);
        int have = Collect.owned(c.collected);
        p.text(have + " OF " + Collect.COUNT + " COLLECTED", cx, top - s * 0.45f, s * 0.58f,
                have >= Collect.COUNT ? GOLD : INK, Painter.CENTER, true);

        // Everything on the shelf is clipped to the plaque. Mid-slide the whole row is offset
        // by up to a full step, which without this put a neighbour past the plaque edge and
        // half off the screen.
        p.save();
        p.clipRect(cx - padX, top, cx + padX, bot);

        // Neighbours first, so the focused entry overlaps them rather than the other way
        // round. The slide offset is shared, which is what makes the row move as one shelf.
        float slide = c.caseSlide * step;
        for (int k = -WINGS; k <= WINGS; k++) {
            if (k == 0) continue;
            int idx = wrap(i + k);
            float x = cx + k * step + slide;
            Trinket.draw(p, idx, x, cy + r * 0.10f, r * 0.56f, c.clock,
                    Collect.has(c.collected, idx), 0.50f);
        }

        // Focused entry, on a hex plinth tinted by its tier.
        int tier = Collect.TIER[i];
        int tint = known ? Collect.TIER_COLOR[tier] : INK_DIM;
        float bob = (float) Math.abs(Math.sin(c.clock * 1.9f)) * r * 0.06f;
        float fr = r * 1.06f;
        p.fillPoly(Glyph.hex(cx + slide, cy, fr), Glyph.withAlpha(tint, known ? 40 : 22));
        p.strokePoly(Glyph.hex(cx + slide, cy, fr), Glyph.withAlpha(tint, known ? 215 : 90),
                fr * 0.06f);
        if (known && tier >= Collect.CHASE) {
            // Chase and grail entries get rays, so a full case still has standouts in it.
            for (int k = 3; k >= 1; k--) {
                p.fillPoly(star(cx + slide, cy, fr * (1.15f + 0.35f * k), fr * 0.42f, 8,
                        c.clock * 0.4f), Glyph.withAlpha(tint, 26 / k));
            }
        }
        Trinket.draw(p, i, cx + slide, cy - bob, r * 0.86f, c.clock, known, 1f);
        p.restore();

        // Caption. The name is withheld until the entry is collected; the family is not,
        // because knowing which shelf a gap belongs to is half of what makes it a gap.
        p.text(known ? Collect.NAME[i] : "??????", cx, cy + r * 1.90f, s * 0.86f,
                known ? INK : INK_DIM, Painter.CENTER, true);
        p.text(known ? Collect.TIER_NAME[tier] : "NOT COLLECTED", cx, cy + r * 2.32f,
                s * 0.56f, known ? tint : INK_DIM, Painter.CENTER, true);
        p.text(Collect.FAMILY_NAME[Collect.FAMILY[i]], cx, bot + s * 0.95f, s * 0.54f,
                INK_DIM, Painter.CENTER, false);
        scrollbar(p, c, L, cx, bot + s * 1.85f, padX * 0.94f);
        p.text((i + 1) + " / " + Collect.COUNT, cx, bot + s * 2.80f, s * 0.54f, INK_DIM,
                Painter.CENTER, true);

        arrows(p, L, cy, padX);
    }

    /**
     * Position bar under the shelf: the track is all thirty entries, the thumb is the slice
     * currently on it. It carries what the "12 / 30" cannot — that the strip wraps, and how
     * far round it you are — and the thumb is wide enough to be a real handle rather than a
     * tick, because {@link #WINGS} neighbours really are on screen either side.
     */
    private static void scrollbar(Painter p, GameCore c, Layout L, float cx, float y,
            float half) {
        float s = L.unit;
        float h = s * 0.16f;
        int span = 2 * WINGS + 1;
        p.fillPoly(pill(cx, y, half, h, 6), Glyph.withAlpha(INK, 34));

        // Thumb width is the visible slice of the strip; its centre tracks the focused entry,
        // slid by the same fraction the shelf is sliding so bar and shelf move as one.
        float trackW = 2f * half - 2f * h;
        float thumbHalf = Math.max(h, trackW * span / (2f * Collect.COUNT));
        float pos = (c.caseIndex - c.caseSlide + 0.5f) / Collect.COUNT;
        float tx = cx - half + h + trackW * pos;
        p.fillPoly(pill(tx, y, thumbHalf + h, h, 6), Glyph.withAlpha(INK_DIM, 210));

        // Ticks for what is already collected, so the bar doubles as a map of the gaps.
        for (int k = 0; k < Collect.COUNT; k++) {
            if (!Collect.has(c.collected, k)) continue;
            float kx = cx - half + h + trackW * (k + 0.5f) / Collect.COUNT;
            p.fillCircle(kx, y, h * 0.42f,
                    Glyph.withAlpha(Collect.TIER_COLOR[Collect.TIER[k]], 235));
        }
    }

    /**
     * Triangles pointing out of the plaque toward the two keys that scroll it. Drawn as
     * polygons rather than typed as characters because the harness font is ASCII-only, and
     * an arrow that only appears on the device is an arrow that never gets checked.
     */
    private static void arrows(Painter p, Layout L, float cy, float padX) {
        float s = L.unit;
        float d = s * 0.42f;
        for (int side = -1; side <= 1; side += 2) {
            float x = L.w / 2f + side * (padX + s * 0.62f);
            float tip = x + side * d;
            p.fillPoly(new float[] {tip, cy, x - side * d, cy - d, x - side * d, cy + d},
                    Glyph.withAlpha(INK, 150));
        }
    }

    static int wrap(int i) {
        int n = Collect.COUNT;
        return ((i % n) + n) % n;
    }
}
