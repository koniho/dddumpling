package com.dddumpling.game;

/** A glass cabinet browsed in two axes: character shapes down, variants across. */
final class Showcase extends Draw {

    static final int FRUIT_ROW = 2, CANDY_ROW = 3;
    static final String[] ROW_NAME = {"BAO", "BUNS & FRIENDS", "FRUITS", "CANDIES", "STARLINGS", "GEL CUBES", "BOSSES"};

    static int row(int index) {
        index = wrap(index);
        switch (Collect.FAMILY[index]) {
            case Collect.FRUITS: return FRUIT_ROW;
            case Collect.GLOBS: return CANDY_ROW;
            case Collect.STARLINGS: return 4;
            case Collect.GEL_CUBES: return 5;
            case Collect.BOSSES: return 6;
            default: return Collect.SHAPE[index] == Collect.BAO ? 0 : 1;
        }
    }

    static int columns(int row) {
        int n = 0;
        for (int i = 0; i < Collect.COUNT; i++) if (row(i) == row) n++;
        return n;
    }

    static int column(int index) {
        int n = 0, shape = row(index);
        for (int i = 0; i < index; i++) if (row(i) == shape) n++;
        return n;
    }

    static int entry(int row, int column) {
        row = (row % ROW_NAME.length + ROW_NAME.length) % ROW_NAME.length;
        int col = Math.max(0, Math.min(columns(row) - 1, column));
        for (int i = 0; i < Collect.COUNT; i++) {
            if (row(i) == row && col-- == 0) return i;
        }
        return 0;
    }

    static int across(int index, int delta) {
        int row = row(index), n = columns(row);
        return entry(row, ((column(index) + delta) % n + n) % n);
    }

    static int down(int index, int delta) {
        return entry(row(index) + delta, column(index));
    }

    static float rowStep(Layout L) { return focusR(L) * 2.08f; }

    private Showcase() {}

    /** Clear space between the open case panel and the bottom of the playable field. */
    static final float FIELD_MARGIN = 1.15f;

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
        return Math.min(L.w * 0.105f, L.h * 0.055f);
    }

    static float focusCy(Layout L) {
        return L.dangerY - focusR(L) * 4.35f - L.unit * (3.35f + FIELD_MARGIN);
    }

    /** Centre-to-centre spacing along the shelf. Under 2r, so the neighbours tuck in close. */
    static float step(Layout L) {
        return focusR(L) * 1.95f;
    }

    /** Half-width of the plaque the shelf stands on. */
    static float padX(Layout L) {
        return L.w * 0.43f;
    }

    static float plaqueTop(Layout L) {
        return focusCy(L) - focusR(L) * 3.2f;
    }

    static float plaqueBot(Layout L) {
        return focusCy(L) + focusR(L) * 4.35f;
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

    /** Top of the panel, including its title and collection count. */
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
    static final int HIT_UP = 7, HIT_DOWN = 8, HIT_ENTRY = 100;

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
        return HIT_NONE;
    }

    /** Direct tile selection uses the same moving grid coordinates as the drawing. */
    static int hit(GameCore c, Layout L, float x, float y) {
        int hit = hit(L, x, y);
        if (hit == HIT_CLOSE || hit == HIT_OUTSIDE || hit == HIT_BAR) return hit;
        if (x < L.w / 2f - padX(L) || x > L.w / 2f + padX(L)
                || y < plaqueTop(L) || y > focusCy(L) + focusR(L) * 3.12f) return hit;
        float gx = (x - L.w / 2f) / step(L) - c.caseSlide;
        float gy = (y - focusCy(L)) / rowStep(L) - c.caseSlideY;
        int dx = Math.round(gx), dy = Math.round(gy);
        int rr = row(c.caseIndex) + dy;
        if (rr < 0 || rr >= ROW_NAME.length) return HIT_NONE;
        int col = column(c.caseIndex) + dx;
        if (col < 0 || col >= columns(rr) || Math.abs(gx - dx) > 0.46f
                || Math.abs(gy - dy) > 0.44f) return hit;
        int index = entry(rr, col);
        return index == c.caseIndex && Math.abs(c.caseSlide) < 0.03f && Math.abs(c.caseSlideY) < 0.03f
                ? HIT_FOCUS : HIT_ENTRY + index;
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
        float turn = (cx - L.w / 2f) / (L.w * ARC_SPAN / 2f);
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

        float padX = padX(L), top = plaqueTop(L), bot = plaqueBot(L);
        float motionX = Math.max(-1f, Math.min(1f, c.casePanMotionX));
        float motionY = Math.max(-1f, Math.min(1f, c.casePanMotionY));
        float motion = Math.min(1f, (Math.abs(motionX) + Math.abs(motionY)) * 2f);
        float depthX = s * (0.84f - motionX * 0.50f);
        float depthY = s * (-0.57f - motionY * 0.32f);
        Cabinet.draw(p, c.clock, cx - padX, top, cx + padX, bot,
                depthX, depthY, 1f, s * (0.32f + motion * 0.12f), fade);
        p.text("DISPLAY CASE", cx, top - s * 2.05f, type(s * 0.62f), fadeBy(INK_DIM, fade),
                Painter.CENTER, true);
        p.text(Collect.owned(c.collected) + " OF " + Collect.COUNT + " COLLECTED", cx,
                top - s * 1.20f, type(s * 0.58f), fadeBy(INK, fade), Painter.CENTER, true);
        closeButton(p, L, fade);
        int selectedRow = row(i), selectedCol = column(i);
        p.save();
        p.clipRect(cx-padX, top, cx+padX, cy + r * 3.12f);
        for (int rr = 0; rr < ROW_NAME.length; rr++) {
            int dy = rr - selectedRow;
            float yy = cy + (dy + c.caseSlideY) * rowStep(L);
            if (yy + r * 1.1f < top || yy - r * 1.1f > cy + r * 3.12f) continue;
            float opacity = dy == 0 ? fade : fade * 0.76f;
            float shelfY = yy + r * 1.03f;
            p.fillPoly(new float[] {cx-padX,shelfY,cx+padX,shelfY,
                    cx+padX+depthX,shelfY+depthY,cx-padX+depthX,shelfY+depthY},
                    fadeBy(Glyph.withAlpha(0xFF8FE9FF, 16 + (int) (motion * 12)), fade));
            p.polyline(new float[] {cx-padX,shelfY,cx-padX+depthX,shelfY+depthY,
                    cx+padX+depthX,shelfY+depthY,cx+padX,shelfY,cx-padX,shelfY},
                    fadeBy(Glyph.withAlpha(INK, 65), fade), s * 0.055f);
            float labelY = yy - r * 0.88f;
            if (labelY - type(s * 0.40f) >= top && labelY < cy + r * 3.10f)
                p.text(ROW_NAME[rr], cx-padX+s*0.35f, labelY,
                        type(s*0.40f), fadeBy(INK_DIM, opacity), Painter.LEFT, true);
            for (int col = 0; col < columns(rr); col++) {
                float xx = cx + (col - selectedCol + c.caseSlide) * step;
                if (xx < cx-padX-r || xx > cx+padX+r) continue;
                int idx = entry(rr, col);
                boolean owned = Collect.has(c.collected, idx);
                boolean focus = dy == 0 && col == selectedCol;
                int tint = owned ? Collect.TIER_COLOR[Collect.TIER[idx]] : INK_DIM;
                float tileR = r * (focus ? 0.86f : 0.67f);
                p.fillPoly(Glyph.hex(xx, yy, tileR),
                        fadeBy(Glyph.withAlpha(tint, focus ? 42 : 15), opacity));
                p.strokePoly(Glyph.hex(xx, yy, tileR),
                        fadeBy(Glyph.withAlpha(tint, focus ? 210 : 65), opacity), s * (focus ? 0.09f : 0.04f));
                float phase = c.clock * (1.7f + hash(idx + 71) * 1.2f) + idx * 2.37f;
                float bob = (float) Math.sin(phase) * tileR * 0.065f;
                float sway = (float) Math.sin(phase * 0.73f) * tileR * 0.035f;
                float breathe = 1f + (float) Math.sin(phase * 1.13f) * 0.035f;
                Trinket.draw(p, idx, xx + sway, yy + bob, tileR * 0.82f * breathe,
                        c.clock, owned, opacity);
            }
        }
        p.restore();
        Cabinet.reflection(p, c.clock, cx - padX, top, cx + padX, bot,
                motionX, motionY, fade);
        int tier = Collect.TIER[i];
        int tint = known ? Collect.TIER_COLOR[tier] : INK_DIM;
        p.text(known ? Collect.NAME[i] : "??????", cx, cy + r * 3.63f, type(s * 0.78f),
                fadeBy(known ? INK : INK_DIM, fade), Painter.CENTER, true);
        p.text(known ? Collect.TIER_NAME[tier] : "NOT COLLECTED", cx, cy + r * 3.96f,
                type(s * 0.52f), fadeBy(tint, fade), Painter.CENTER, true);
        p.text(ROW_NAME[selectedRow] + "  " + (selectedCol+1) + " / " + columns(selectedRow),
                cx, bot + s * 0.95f, type(s * 0.54f), fadeBy(INK_DIM, fade), Painter.CENTER, true);
        p.text("COLLECTIONS: " + c.collectTotal,
                cx, bot + s * 2.80f, type(s * 0.48f), fadeBy(INK_DIM, fade), Painter.CENTER, true);

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
