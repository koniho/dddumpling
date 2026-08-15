package com.sram.hexatype;

/**
 * The display case on the title screen: a shelf of collected squishies, browsed by touch.
 *
 * A filmstrip rather than a grid. Thirty tiles laid out at once would each be too small for
 * a face to read, and the point of the case is looking at the thing you won — so one entry
 * gets the middle of the shelf at full size and its neighbours sit half-size at the edges,
 * which also shows which way a swipe moves.
 *
 * It is closed by default: the title screen shows {@link #icon} in the middle instead, and a
 * tap on that fades the case in. The keys have nothing to do with any of it. They used to
 * drive the carousel, which meant the six letters did one thing on the title screen and a
 * completely different thing a frame later in play, and the case owned the middle of the
 * screen whether or not you had come to look at it.
 */
final class Showcase extends Draw {

    /** Neighbours shown either side of the focused entry. */
    static final int WINGS = 1;

    private Showcase() {}

    /** Vertical centre of the shelf, as a fraction of view height. */
    static final float SHELF_Y = 0.47f;

    /** How quickly a scroll's slide settles, in slides per second. */
    static final float SLIDE_RATE = 5.5f;

    // ---- geometry -----------------------------------------------------------
    // All of it derived, and all of it read by both the drawing and the hit tests, so the two
    // cannot disagree about where a target is.

    /**
     * Radius of the focused entry's plinth.
     *
     * Both caps matter: the width one keeps the plaque and its arrows on screen on a narrow
     * device, the height one stops the shelf swallowing a tall one.
     */
    static float focusR(Layout L) {
        return Math.min(L.w * 0.145f, L.h * 0.075f);
    }

    static float focusCy(Layout L) {
        return L.h * SHELF_Y;
    }

    /** Centre-to-centre spacing along the shelf. Under 2r, so the neighbours tuck in close. */
    static float step(Layout L) {
        return focusR(L) * 1.95f;
    }

    /** Half-width of the plaque the shelf stands on. */
    static float padX(Layout L) {
        return step(L) * (WINGS + 0.52f);
    }

    static float plaqueTop(Layout L) {
        return focusCy(L) - focusR(L) * 1.5f;
    }

    static float plaqueBot(Layout L) {
        return focusCy(L) + focusR(L) * 2.55f;
    }

    /** Centre line of the position bar. */
    static float barY(Layout L) {
        return plaqueBot(L) + L.unit * 1.85f;
    }

    /** Half-length of the position bar, caps included. */
    static float barHalf(Layout L) {
        return padX(L) * 0.94f;
    }

    private static float barCapR(Layout L) {
        return L.unit * 0.16f;
    }

    /**
     * Top of the whole panel, label included: above this a tap is outside the case.
     *
     * Well clear of the front face, because the box recedes upward as well as sideways and the
     * back edge lands above it. The labels are lifted for the same reason — at the old spacing
     * the top-back edge ran straight through the collected count.
     */
    static float panelTop(Layout L) {
        return plaqueTop(L) - L.unit * 2.85f;
    }

    /** Bottom of the whole panel, below the entry count. */
    static float panelBot(Layout L) {
        return plaqueBot(L) + L.unit * 3.35f;
    }

    static float closeR(Layout L) {
        return L.unit * 0.80f;
    }

    static float closeCx(Layout L) {
        return L.w / 2f + padX(L) - closeR(L) * 0.30f;
    }

    static float closeCy(Layout L) {
        return plaqueTop(L) - L.unit * 1.55f;
    }

    /** Scale of the closed case's badge: a small copy of the same box. */
    static float iconR(Layout L) {
        return focusR(L) * 0.62f;
    }

    static float iconHalfW(Layout L) {
        return iconR(L) * 1.50f;
    }

    static float iconHalfH(Layout L) {
        return iconR(L) * 1.25f;
    }

    /** How far the badge drifts, as a fraction of view width, end to end. */
    static final float ARC_SPAN = 0.30f;
    /** Seconds for one full sweep out and back. Slow: it is scenery, not a moving target. */
    static final float ARC_TIME = 8.5f;

    /**
     * The eye the badge is drawn from and swings around, at the lower middle of the screen —
     * roughly where a thumb is and where the player is looking from.
     */
    static float eyeY(Layout L) {
        return L.h * 0.95f;
    }

    static float iconCx(Layout L, float clock) {
        return L.w / 2f + L.w * ARC_SPAN / 2f
                * (float) Math.sin(clock * 6.2832f / ARC_TIME);
    }

    /**
     * The badge rides a circle centred on the eye, so it dips as it swings out rather than
     * sliding along a line — and it stays the same distance away, so it needs no scaling.
     */
    static float iconCy(Layout L, float clock) {
        float dx = iconCx(L, clock) - L.w / 2f;
        float radius = eyeY(L) - focusCy(L);
        return eyeY(L) - (float) Math.sqrt(Math.max(1f, radius * radius - dx * dx));
    }

    /**
     * How far round the arc the badge is, -1..1. The box's back face is swung by this, which is
     * what turns it to keep facing the eye.
     *
     * Exaggerated well past the true angle: at this distance honest perspective across an arc
     * 30% of the screen wide is two or three pixels of turn, which reads as none at all.
     */
    static float iconTurn(Layout L, float clock) {
        return (iconCx(L, clock) - L.w / 2f) / (L.w * ARC_SPAN / 2f);
    }

    // ---- hit tests ----------------------------------------------------------

    static final int HIT_NONE = 0;
    /** The focused entry, which opens its story. */
    static final int HIT_FOCUS = 1;
    static final int HIT_PREV = 2;
    static final int HIT_NEXT = 3;
    /** The position bar, which is dragged straight to an entry. */
    static final int HIT_BAR = 4;
    static final int HIT_CLOSE = 5;
    /** Anywhere off the panel, which puts the case away. */
    static final int HIT_OUTSIDE = 6;

    /**
     * True inside the focused entry.
     *
     * Tested against where the entry settles, not where it currently is: mid-scroll the shelf
     * is offset by up to half a step, and a target that slid out from under a thumb would feel
     * broken. The slide lasts a fifth of a second.
     */
    static boolean inFocus(Layout L, float x, float y) {
        float r = focusR(L) * 1.06f;
        float dx = x - L.w / 2f, dy = y - focusCy(L);
        // Circumscribed circle of the hex plinth, a touch generous: a mis-tap here costs
        // nothing but a panel you did not want.
        return dx * dx + dy * dy <= r * r * 1.20f;
    }

    /**
     * True inside the closed case's badge, which opens the case.
     *
     * Tracks the badge along its arc rather than covering the whole sweep: a target three times
     * the size of the thing you can see is worse than a moving one, and the drift is slow enough
     * that a thumb lands where the eye is already looking. Generous around it for the same
     * reason, and it has to stay clear of the keys wherever it is — there is an assertion on
     * both ends of the arc.
     */
    static boolean inIcon(Layout L, float clock, float x, float y) {
        return Math.abs(x - iconCx(L, clock)) <= iconHalfW(L) * 1.30f
                && Math.abs(y - iconCy(L, clock)) <= iconHalfH(L) * 1.45f;
    }

    /**
     * What a touch at x,y is aiming at while the case is open.
     *
     * The shelf's two halves are targets in their own right, and generously wide ones: the
     * neighbour tiles are drawn at half size, and a thumb that has to land on one of those to
     * turn the page would miss as often as not. Anything inside the panel that is not a
     * control is {@link #HIT_NONE} rather than outside, so a tap on the caption does not put
     * the case away underneath the finger.
     */
    static int hit(Layout L, float x, float y) {
        float cx = L.w / 2f;
        float s = L.unit;
        float dx = x - closeCx(L), dy = y - closeCy(L);
        float cr = closeR(L) * 1.30f;
        if (dx * dx + dy * dy <= cr * cr) return HIT_CLOSE;
        if (y < panelTop(L) || y > panelBot(L)) return HIT_OUTSIDE;
        if (Math.abs(x - cx) > padX(L) + s * 1.45f) return HIT_OUTSIDE;
        if (inFocus(L, x, y)) return HIT_FOCUS;
        if (Math.abs(y - barY(L)) <= s * 0.85f) return HIT_BAR;
        // The shelf band: everything at plinth height either side of the middle.
        if (y >= plaqueTop(L) - s * 0.55f && y <= plaqueBot(L)) {
            return x < cx ? HIT_PREV : HIT_NEXT;
        }
        return HIT_NONE;
    }

    /**
     * The entry under x on the position bar. The track is the whole catalogue laid end to end,
     * so this is the inverse of where {@link #scrollbar} puts the thumb.
     */
    static int barIndexAt(Layout L, float x) {
        float half = barHalf(L), cap = barCapR(L);
        float left = L.w / 2f - half + cap;
        float track = 2f * half - 2f * cap;
        int i = (int) Math.floor((x - left) / track * Collect.COUNT);
        return i < 0 ? 0 : i >= Collect.COUNT ? Collect.COUNT - 1 : i;
    }

    // ---- drawing ------------------------------------------------------------

    /**
     * The closed case: a badge in the middle of the screen showing the entry the shelf is
     * parked on, with the count under it.
     *
     * Small on purpose, and holding the same spot the shelf appears in, so the case grows out
     * of the thing you tapped rather than arriving from nowhere.
     *
     * @param fade 0..1 opacity; it crosses over with the case itself
     */
    static void icon(Painter p, GameCore c, Layout L, float fade) {
        if (fade <= 0.004f) return;
        float s = L.unit;
        // Frozen on a start press, at the same instant the send-off reads: the squishy has to
        // leave from where the case is, not from where the case has drifted on to.
        float t = c.starting() ? c.launchClock : c.clock;
        float cx = iconCx(L, t), cy = iconCy(L, t);
        float r = iconR(L);
        int i = c.caseIndex;
        boolean known = Collect.has(c.collected, i);
        int tint = known ? Collect.TIER_COLOR[Collect.TIER[i]] : INK_DIM;
        float pulse = 0.5f + 0.5f * (float) Math.sin(c.clock * 2.2f);
        float hw = iconHalfW(L), hh = iconHalfH(L);

        // The same box as the open case, small, and turning as it drifts: the back face is swung
        // the opposite way to the badge's place on the arc, which is what keeps it facing the
        // eye down at the bottom of the screen. Downward, too, because that eye is below it —
        // this is the underside of a case being looked up at, where the open case is a cabinet
        // seen squarely from in front.
        // And breathing, which is what replaced TAP TO OPEN: a box that swells and settles reads
        // as a button, where a still one with a caption under it only read as a caption.
        float breathe = 1f + 0.045f * pulse;
        hw *= breathe;
        hh *= breathe;
        float turn = iconTurn(L, c.clock);
        Cabinet.draw(p, c.clock, cx - hw, cy - hh, cx + hw, cy + hh,
                -turn * s * 0.50f, s * 0.26f, 0.94f, s * 0.16f, fade);
        // The entry travels with the middle of the case, which is the point of the drift: the
        // badge is the squishy going past in a box, not a button with a picture on it.
        Trinket.draw(p, i, cx, cy + hh * 0.10f, r * 0.62f, c.clock, known, fade);
        // A tier-tinted glow behind the glass, so a chase entry still catches the eye closed.
        if (known) {
            p.strokePoly(new float[] {cx - hw, cy - hh, cx + hw, cy - hh, cx + hw, cy + hh,
                    cx - hw, cy + hh},
                    fadeBy(Glyph.withAlpha(tint, (int) (60 + 70 * pulse)), fade), s * 0.05f);
        }

        int have = Collect.owned(c.collected);
        p.text("DISPLAY CASE", cx, cy - hh - s * 1.00f, type(s * 0.62f), fadeBy(INK_DIM, fade),
                Painter.CENTER, true);
        p.text(have + " OF " + Collect.COUNT, cx, cy + hh + s * 1.15f, type(s * 0.70f),
                fadeBy(have >= Collect.COUNT ? GOLD : INK, fade), Painter.CENTER, true);
    }

    static void draw(Painter p, GameCore c, Layout L) {
        draw(p, c, L, 1f);
    }

    /**
     * @param fade 0..1 master opacity: the case easing in on a tap, and the title screen
     *     dissolving on a start press, are the same knob
     */
    static void draw(Painter p, GameCore c, Layout L, float fade) {
        if (fade <= 0.004f) return;
        float s = L.unit;
        float cx = L.w / 2f;
        float cy = focusCy(L);
        float r = focusR(L);
        float step = step(L);

        int i = c.caseIndex;
        boolean known = Collect.has(c.collected, i);

        // The cabinet. Everything below is drawn on its front plane, inside the glass.
        float padX = padX(L);
        float top = plaqueTop(L), bot = plaqueBot(L);
        Cabinet.draw(p, c.clock, cx - padX, top, cx + padX, bot, s * 1.35f, s * 0.32f, fade);

        p.text("DISPLAY CASE", cx, top - s * 2.05f, type(s * 0.62f), fadeBy(INK_DIM, fade),
                Painter.CENTER, true);
        int have = Collect.owned(c.collected);
        p.text(have + " OF " + Collect.COUNT + " COLLECTED", cx, top - s * 1.20f, type(s * 0.58f),
                fadeBy(have >= Collect.COUNT ? GOLD : INK, fade), Painter.CENTER, true);
        closeButton(p, L, fade);

        // Everything on the shelf is clipped to the plaque. Mid-slide the whole row is offset,
        // which without this put a neighbour past the plaque edge and half off the screen.
        p.save();
        p.clipRect(cx - padX, top, cx + padX, bot);

        // Neighbours first, so the focused entry overlaps them rather than the other way
        // round. The slide offset is shared, which is what makes the row move as one shelf.
        // One tile further out than is ever fully visible: mid-slide the outermost gap would
        // otherwise show, and under a finger that gap is on screen for as long as the drag.
        float slide = c.caseSlide * step;
        for (int k = -WINGS - 1; k <= WINGS + 1; k++) {
            if (k == 0) continue;
            int idx = wrap(i + k);
            float x = cx + k * step + slide;
            Trinket.draw(p, idx, x, cy + r * 0.10f, r * 0.56f, c.clock,
                    Collect.has(c.collected, idx), 0.50f * fade);
        }

        // Focused entry, on a hex plinth tinted by its tier.
        int tier = Collect.TIER[i];
        int tint = known ? Collect.TIER_COLOR[tier] : INK_DIM;
        float bob = (float) Math.abs(Math.sin(c.clock * 1.9f)) * r * 0.06f;
        float fr = r * 1.06f;
        p.fillPoly(Glyph.hex(cx + slide, cy, fr),
                fadeBy(Glyph.withAlpha(tint, known ? 40 : 22), fade));
        // A collected plinth breathes, which is the only cue that it can be tapped for a
        // story. An uncollected one holds still, because it cannot.
        int edge = known ? (int) (185 + 70 * (0.5f + 0.5f * (float) Math.sin(c.clock * 2.6f)))
                : 90;
        // The focused entry throbs for the first moment the case is up, which is what replaced the
        // line telling you to tap it for a story. Only at the start: a permanent throb is
        // wallpaper, and the plinth's own slow breathe carries it from there.
        if (known) {
            float woo = Math.max(0f, 1f - c.caseT / 1.6f);
            if (woo > 0f) {
                float beat = 0.5f + 0.5f * (float) Math.sin(c.caseT * 12f);
                p.strokePoly(Glyph.hex(cx + slide, cy, fr * (1.12f + 0.10f * beat)),
                        fadeBy(Glyph.withAlpha(INK, (int) (200 * woo * beat)), fade), fr * 0.05f);
            }
        }
        p.strokePoly(Glyph.hex(cx + slide, cy, fr), fadeBy(Glyph.withAlpha(tint, edge), fade),
                fr * 0.06f);
        if (known && tier >= Collect.CHASE) {
            // Chase and grail entries get rays, so a full case still has standouts in it.
            for (int k = 3; k >= 1; k--) {
                p.fillPoly(star(cx + slide, cy, fr * (1.15f + 0.35f * k), fr * 0.42f, 8,
                        c.clock * 0.4f), fadeBy(Glyph.withAlpha(tint, 26 / k), fade));
            }
        }
        Trinket.draw(p, i, cx + slide, cy - bob, r * 0.86f, c.clock, known, fade);
        p.restore();

        // Caption. The name is withheld until the entry is collected; the family is not,
        // because knowing which shelf a gap belongs to is half of what makes it a gap.
        p.text(known ? Collect.NAME[i] : "??????", cx, cy + r * 1.90f, type(s * 0.86f),
                fadeBy(known ? INK : INK_DIM, fade), Painter.CENTER, true);
        p.text(known ? Collect.TIER_NAME[tier] : "NOT COLLECTED", cx, cy + r * 2.32f,
                type(s * 0.56f), fadeBy(known ? tint : INK_DIM, fade), Painter.CENTER, true);
        p.text(Collect.FAMILY_NAME[Collect.FAMILY[i]], cx, bot + s * 0.95f, type(s * 0.54f),
                fadeBy(INK_DIM, fade), Painter.CENTER, false);
        scrollbar(p, c, L, fade);
        p.text((i + 1) + " / " + Collect.COUNT, cx, bot + s * 2.80f, type(s * 0.54f),
                fadeBy(INK_DIM, fade), Painter.CENTER, true);
        // Baskets opened over every run, duplicates and all. Below the position bar rather than up
        // with the header: the count above is about the case in front of you and how much of it is
        // filled, this one is about the whole history behind it, and it is the only number here
        // that keeps climbing once the case is full. The gap goes through type() because the line
        // above it is type-scaled — a plain unit multiple here rides up onto it as TEXT grows.
        p.text("COLLECTIONS: " + c.collectTotal, cx, bot + s * 2.80f + type(s * 1.05f),
                type(s * 0.54f), fadeBy(INK_DIM, fade), Painter.CENTER, true);

        arrows(p, c, L, fade);
    }

    /**
     * Position bar under the shelf: the track is all thirty entries, the thumb is the slice
     * currently on it. It carries what the "12 / 30" cannot — that the strip wraps, and how
     * far round it you are — and it is a handle as well as a readout: dragging it goes
     * straight to an entry, which is the only way to cross the catalogue in one gesture.
     */
    private static void scrollbar(Painter p, GameCore c, Layout L, float fade) {
        float cx = L.w / 2f;
        float y = barY(L);
        float half = barHalf(L);
        float h = barCapR(L);
        int span = 2 * WINGS + 1;
        p.fillPoly(pill(cx, y, half, h, 6), fadeBy(Glyph.withAlpha(INK, 34), fade));

        // Thumb width is the visible slice of the strip; its centre tracks the focused entry,
        // slid by the same fraction the shelf is sliding so bar and shelf move as one.
        float trackW = 2f * half - 2f * h;
        float thumbHalf = Math.max(h, trackW * span / (2f * Collect.COUNT));
        float pos = (c.caseIndex - c.caseSlide + 0.5f) / Collect.COUNT;
        float tx = cx - half + h + trackW * pos;
        // Brighter under the finger, so a drag that has left the bar is visibly still driving
        // it rather than seeming to have come loose.
        int thumb = c.caseDragging ? INK : INK_DIM;
        p.fillPoly(pill(tx, y, thumbHalf + h, h, 6), fadeBy(Glyph.withAlpha(thumb, 210), fade));

        // Ticks for what is already collected, so the bar doubles as a map of the gaps.
        for (int k = 0; k < Collect.COUNT; k++) {
            if (!Collect.has(c.collected, k)) continue;
            float kx = cx - half + h + trackW * (k + 0.5f) / Collect.COUNT;
            // Nearly the full height of the track: at half this they were dots you had to look
            // for, and the point of them is reading the gaps at a glance.
            p.fillCircle(kx, y, h * 0.84f,
                    fadeBy(Glyph.withAlpha(Collect.TIER_COLOR[Collect.TIER[k]], 235), fade));
        }
    }

    /**
     * Triangles just outside the plaque, one either side. They are the tap targets for a step
     * as well as the hint that the shelf moves — the whole half of the shelf behind each one
     * steps the same way, but a thumb needs somewhere to aim.
     *
     * Drawn as polygons rather than typed as characters because the harness font is ASCII-only,
     * and an arrow that only appears on the device is an arrow that never gets checked.
     */
    private static void arrows(Painter p, GameCore c, Layout L, float fade) {
        float s = L.unit;
        float d = s * 0.42f;
        float cy = focusCy(L);
        float padX = padX(L);
        // Nudged outward and back on a slow cycle, in the direction each one moves the shelf.
        float pulse = (float) Math.sin(c.clock * 2.4f) * s * 0.16f;
        for (int side = -1; side <= 1; side += 2) {
            float x = L.w / 2f + side * (padX + s * 0.62f + pulse);
            float tip = x + side * d;
            p.fillPoly(new float[] {tip, cy, x - side * d, cy - d, x - side * d, cy + d},
                    fadeBy(Glyph.withAlpha(INK, 150), fade));
        }
    }

    /** The X that puts the case away, on the plaque's top corner. */
    private static void closeButton(Painter p, Layout L, float fade) {
        float cx = closeCx(L), cy = closeCy(L), r = closeR(L);
        p.fillCircle(cx, cy, r, fadeBy(Glyph.withAlpha(0xFF2A2348, 215), fade));
        p.strokeCircle(cx, cy, r, fadeBy(Glyph.withAlpha(INK, 70), fade), r * 0.10f);
        float d = r * 0.42f;
        int ink = fadeBy(Glyph.withAlpha(INK, 210), fade);
        p.line(cx - d, cy - d, cx + d, cy + d, ink, r * 0.16f);
        p.line(cx - d, cy + d, cx + d, cy - d, ink, r * 0.16f);
    }

    static int wrap(int i) {
        int n = Collect.COUNT;
        return ((i % n) + n) % n;
    }
}
