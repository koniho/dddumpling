package com.sram.hexatype;

/**
 * The star-path interlude on screen: the wordless ready lesson, the scrolling course, and the
 * victory tableau a completed course ends on.
 *
 * Its own file rather than more of {@link Screens}, which was already twice the size a file
 * here wants to be. Sizes come from {@link StarPath} so the drawing and the pickup test agree
 * about how big a star and a flyer are.
 */
final class StarScreen extends Draw {

    private StarScreen() {}

    /** Baseline of the star screen's checkpoint counter. */
    static float countY(Layout L) {
        return L.playTop + L.unit * 1.2f;
    }

    /** Type size of the READY prompt, whose caps have to clear {@link #countY}. */
    static float readySize(Layout L) {
        return type(L.unit * 1.1f);
    }

    /**
     * Baseline of the READY prompt. The gap below the counter goes through {@code type()} like
     * the sizes do: as a plain unit multiple it was 1.2 units, and at TEXT 1.34 READY's caps
     * came up through the counter's baseline. See the note on the global text scale in CLAUDE.md.
     */
    static float readyY(Layout L) {
        return countY(L) + type(L.unit * 1.35f);
    }

    static void draw(Painter p, GameCore c, Layout L) {
        StarPath q = c.stars;
        float fade = Math.min(1f, c.time / 0.35f)
                * (q.reporting() ? Math.min(1f, q.timer / 0.35f) : 1f);
        // The tableau fades itself out at the end instead, since its clock is not the interlude's.
        if (q.winning()) fade = Math.min(1f, q.winT / 0.35f);
        Screens.scrim(p, L, (int) (175 * fade));
        // The grab beat's gold rim, redrawn here because the shared one goes on under the scrim,
        // which is dark enough to swallow it whole. Clamped: a fling beat from the wave that opened
        // this interlude can still be running, and it is three times as long.
        if (c.slowdown > 0f) {
            Sky.vignette(p, L, GOLD,
                    0.18f * fade * Math.min(1f, c.slowdown / GameCore.STAR_BEAT));
        }
        float s = L.unit;

        // A completed course dims its own scenery: the trail and the spent checkpoints are still
        // there behind the tableau, but they stop competing with it for attention.
        float lit = q.winning() ? 1f - 0.85f * ease(q.winProgress() / 0.35f) : 1f;

        // Catmull-Rom samples turn the checkpoints into one soft flight trail. Three strokes
        // supply depth: a broad shadow, a coloured atmosphere, and a bright central filament.
        float[] route = spline(q, L);
        float cf = fade * lit;
        // The bright route is as wide as a star's centre pearl (2 * 0.23 of its outer radius).
        // The two broader passes sit behind it like a soft raised ribbon.
        float pearl = StarPath.starOuter(L) * 0.23f * 2f;
        p.polyline(route, fadeBy(Glyph.withAlpha(0xFF241B50, 145), cf), pearl * 1.55f);
        p.polyline(route, fadeBy(Glyph.withAlpha(0xFF6E72C8, 105), cf), pearl * 1.25f);
        p.polyline(route, fadeBy(Glyph.withAlpha(0xFFBDEBFF, 115), cf), pearl);
        for (int i = 0; i < StarPath.COUNT; i++) {
            float y = q.starY(i, L);
            if (y < L.playTop - s * 2f || y > L.dangerY + s * 2f) continue;
            boolean got = (q.collected & (1 << i)) != 0;
            float pulse = 1f + 0.11f * (float) Math.sin(c.clock * 5.5f + i * 1.31f);
            float turn = c.clock * (i % 2 == 0 ? 0.55f : -0.45f) + i * 0.37f;
            float outer = StarPath.starOuter(L) * pulse;
            float inner = StarPath.starInner(L) * pulse;
            int col = got ? Glyph.withAlpha(INK_DIM, 62)
                    : Glyph.withAlpha(GOLD, (int) (210 + 45 * Math.sin(c.clock * 6f + i)));
            float xStar = q.starX(i, L);
            p.fillPoly(cuteStar(xStar, y, outer * 1.16f, inner * 1.12f, turn),
                    fadeBy(Glyph.withAlpha(got ? INK_DIM : 0xFFFFA93A, got ? 22 : 45), cf));
            p.fillPoly(cuteStar(xStar, y, outer, inner, turn), fadeBy(col, cf));
            // A pearl-like heart makes each checkpoint feel like a little creature rather than
            // a navigation marker. Two translucent discs give it depth without a gradient API.
            int orb = got ? Glyph.withAlpha(INK_DIM, 70) : Glyph.withAlpha(0xFFFFF6C7, 245);
            p.fillCircle(xStar, y, outer * 0.30f, fadeBy(Glyph.withAlpha(0xFFFFA93A,
                    got ? 18 : 95), cf));
            // The pearl is what the pickup test aims at, so it is drawn at exactly the size that
            // test uses: this is the target, and the petals around it are decoration.
            p.fillCircle(xStar, y, outer * StarPath.HEART, fadeBy(orb, cf));
            p.fillCircle(xStar - outer * 0.065f, y - outer * 0.075f, outer * 0.065f,
                    fadeBy(Glyph.withAlpha(0xFFFFFFFF, got ? 45 : 210), cf));

            float b = q.burst[i];
            if (b > 0f) {
                float out = (1f - b) * s * 2.4f;
                int shine = fadeBy(Glyph.withAlpha(0xFFFFFFFF, (int) (255 * b)), cf);
                // A white glint crosses the star while eight sparks burst away from it.
                p.line(xStar - outer * 0.75f, y, xStar + outer * 0.75f, y, shine,
                        s * 0.12f * b);
                p.line(xStar, y - outer * 0.75f, xStar, y + outer * 0.75f, shine,
                        s * 0.12f * b);
                for (int k = 0; k < 8; k++) {
                    double a = k * Math.PI / 4 + i * 0.19f;
                    float ux = (float) Math.cos(a), uy = (float) Math.sin(a);
                    float x0 = xStar + ux * outer * 0.65f;
                    float y0 = y + uy * outer * 0.65f;
                    p.line(x0, y0, x0 + ux * out, y0 + uy * out, shine, s * 0.09f * b);
                }
            }
        }

        if (q.winning()) {
            victory(p, c, L, fade);
            // Gold, and it stays up: the number that was being counted is the thing just won.
            p.text(q.count() + " / " + StarPath.COUNT, L.w / 2f, countY(L), type(s * 0.78f),
                    fadeBy(GOLD, fade), Painter.CENTER, true);
            return;
        }

        float x = q.x, y = q.characterY(L);
        if (q.ready()) {
            // Wordless lesson: the three left keys glow and the flyer leans left, then the
            // three right keys and a lean right. The real deck supplies the controls.
            float wave = (float) Math.sin(c.clock * 3.4f);
            x += wave * L.w * 0.12f;
            int side = wave < 0 ? 0 : 1;
            for (int g = side * 3; g < side * 3 + 3; g++) {
                float pulse = 1f + 0.08f * (float) Math.sin(c.clock * 8f + g);
                p.strokePoly(Glyph.hex(L.keyX[g], L.keyY[g], L.keyR * 1.18f * pulse),
                        fadeBy(Glyph.withAlpha(GOLD, 210), fade), L.keyR * 0.08f);
            }
        }

        float rr = StarPath.flyerR(L);
        flyer(p, q, rr, x, y, c.clock, fade);

        String count = q.count() + " / " + StarPath.COUNT;
        p.text(count, L.w / 2f, countY(L), type(s * 0.78f), fadeBy(INK, fade),
                Painter.CENTER, true);
        if (q.ready()) {
            p.text("READY", L.w / 2f, readyY(L), readySize(L),
                    fadeBy(GOLD, fade), Painter.CENTER, true);
        } else if (q.reporting()) {
            p.text(q.won ? "ALL STARS!" : count + " STARS", L.w / 2f, L.h * 0.38f,
                    type(s * 1.15f), fadeBy(q.won ? GOLD : INK, fade), Painter.CENTER, true);
        }
    }

    /** The climber: a soft aura, a rimmed hex, and whichever collectible is piloting it. */
    private static void flyer(Painter p, StarPath q, float rr, float x, float y, float clock,
            float fade) {
        p.fillCircle(x, y, rr * 1.18f, fadeBy(Glyph.withAlpha(0xFF93D6F7, 55), fade));
        p.strokePoly(Glyph.hex(x, y, rr * 1.15f), fadeBy(Glyph.withAlpha(0xFF93D6F7, 150), fade),
                rr * 0.07f);
        if (q.who >= 0 && q.who < Collect.COUNT) {
            Trinket.draw(p, q.who, x, y, rr, clock, true, fade);
        } else {
            p.fillCircle(x, y, rr * 0.75f, fadeBy(Glyph.withAlpha(INK_DIM, 210), fade));
        }
    }

    /**
     * The victory tableau: every star taken, the course stopped, and the prize climbing out of
     * the checkpoint that finished it.
     *
     * Built as three beats over the one progress value so any frame of it renders in the harness:
     * a burst of rings off that star, the prize rising out of it and swelling into place, and the
     * flyer gliding in under it to stand and cheer. The parade follows, exactly as it follows a
     * won steamer — this is the moment the steamer's escape animation is for.
     */
    private static void victory(Painter p, GameCore c, Layout L, float fade) {
        StarPath q = c.stars;
        float s = L.unit;
        float t = q.winProgress();
        float rr = StarPath.flyerR(L);
        float span = L.dangerY - L.playTop;
        float big = Math.min(L.w * 0.17f, s * 4.0f);

        // Where the tableau composes itself, and stated rather than taken from the win: the last
        // star is wherever the course happened to bend, and a scene pinned to it would sit under the
        // heading on one course and down among the keys on the next. Both figures travel to here.
        float podX = L.w / 2f;
        float showY = L.playTop + span * 0.38f;
        float podY = showY + big + rr * 1.4f;

        float startY = q.characterY(L);
        float glide = ease(t / 0.30f);
        float fx = q.x + (podX - q.x) * glide;
        float fy = startY + (podY - startY) * glide;
        // Once it has arrived it bounces on the spot: the one thing still moving on a stopped
        // screen is the character being congratulated.
        float hop = glide >= 1f ? (float) Math.abs(Math.sin(c.clock * 5.4f)) * rr * 0.30f : 0f;

        // The star it came out of, frozen with the rest of the course.
        float wx = q.winStar >= 0 ? q.starX(q.winStar, L) : podX;
        float wy = q.winStar >= 0 ? q.starY(q.winStar, L) : showY;
        int tint = c.prize >= 0 ? Collect.TIER_COLOR[Collect.TIER[c.prize]] : GOLD;

        // Rings off that checkpoint, over the first quarter-second. The pickup shine cannot carry
        // this: every one of the twenty gets that, and only one of them ends the course.
        float ring = t / 0.22f;
        for (int k = 1; k <= 2 && ring < 1f; k++) {
            float rad = StarPath.starOuter(L) * (0.9f + ring * 1.1f * k);
            p.strokePoly(pill(wx, wy, rad, rad, 24),
                    fadeBy(Glyph.withAlpha(0xFFFFF6C7, (int) (220 * (1f - ring) / k)), fade),
                    s * 0.30f * (1f - ring));
        }

        // The prize: out of the star, up to its display height, growing the whole way.
        float e = ease(t / 0.45f);
        float px = wx + (podX - wx) * e;
        float py = wy + (showY - wy) * e;
        float pr = big * (0.12f + 0.88f * e);
        if (e >= 1f) pr = big * (1f + 0.045f * (float) Math.sin(c.clock * 3.2f));

        // Shining glow: three broad rays turning behind it, brightening as it rises. Tier-coloured,
        // so what came out is readable before the label under it is.
        for (int k = 3; k >= 1; k--) {
            p.fillPoly(star(px, py, pr * (1.25f + 0.5f * k), pr * 0.5f, 8, c.clock * 0.55f),
                    fadeBy(Glyph.withAlpha(tint, (int) (34 * e / k)), fade));
        }
        // Halo in five nested discs rather than one: a single translucent circle has a hard rim and
        // reads as a pale plate behind the prize instead of as light coming off it. Five is enough
        // steps that the outermost is too faint to show an edge.
        for (int k = 5; k >= 1; k--) {
            p.fillCircle(px, py, pr * (1f + 0.11f * k),
                    fadeBy(Glyph.withAlpha(0xFFFFF6C7, (int) (14 * e / k)), fade));
        }

        // The flyer goes on first, so the prize it is looking up at is never behind it.
        flyer(p, q, rr, fx, fy - hop, c.clock, fade);
        if (c.prize >= 0) Trinket.draw(p, c.prize, px, py, pr, c.clock, true, fade);

        // Sparks around the arrived prize, turning slowly the other way from the rays.
        if (e > 0.6f) {
            float sp = (e - 0.6f) / 0.4f;
            for (int k = 0; k < 8; k++) {
                double a = k * Math.PI / 4 - c.clock * 0.9f;
                float ux = (float) Math.cos(a), uy = (float) Math.sin(a);
                float beat = 0.75f + 0.25f * (float) Math.sin(c.clock * 4f + k);
                float x0 = px + ux * pr * 1.30f, y0 = py + uy * pr * 1.30f;
                p.line(x0, y0, x0 + ux * pr * 0.34f * beat, y0 + uy * pr * 0.34f * beat,
                        fadeBy(Glyph.withAlpha(0xFFFFFFFF, (int) (190 * sp)), fade), s * 0.10f);
            }
        }

        p.text("ALL STARS!", L.w / 2f, L.h * 0.235f,
                type(s * 1.5f * Screens.introScale(t * StarPath.WIN_HOLD)),
                fadeBy(GOLD, fade), Painter.CENTER, true);
        // The name arrives with the prize, not before it: until it is out there is nothing to name.
        if (c.prize >= 0 && e > 0.55f) {
            float said = Math.min(1f, (e - 0.55f) / 0.30f);
            Screens.prizeLabel(p, c, L, podX, podY + rr * 1.55f + type(s * 1.7f), fade * said);
        }
    }

    /** Ease-out cubic, clamped at both ends — every beat of the tableau is timed with it. */
    private static float ease(float t) {
        if (t <= 0f) return 0f;
        if (t >= 1f) return 1f;
        float u = 1f - t;
        return 1f - u * u * u;
    }

    /** Smooth samples through the scrolling checkpoints, padded at both ends for tangents. */
    private static float[] spline(StarPath q, Layout L) {
        final int steps = 7;
        float[] pts = new float[(StarPath.COUNT - 1) * steps * 2 + 2];
        int at = 0;
        for (int i = 0; i < StarPath.COUNT - 1; i++) {
            int a = Math.max(0, i - 1), b = i, cc = i + 1,
                    d = Math.min(StarPath.COUNT - 1, i + 2);
            for (int k = 0; k < steps; k++) {
                float t = k / (float) steps;
                pts[at++] = catmull(q.starX(a, L), q.starX(b, L), q.starX(cc, L),
                        q.starX(d, L), t);
                pts[at++] = catmull(q.starY(a, L), q.starY(b, L), q.starY(cc, L),
                        q.starY(d, L), t);
            }
        }
        pts[at++] = q.starX(StarPath.COUNT - 1, L);
        pts[at] = q.starY(StarPath.COUNT - 1, L);
        return pts;
    }

    private static float catmull(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t, t3 = t2 * t;
        return 0.5f * ((2f * p1) + (-p0 + p2) * t
                + (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2
                + (-p0 + 3f * p1 - 3f * p2 + p3) * t3);
    }

    /** Five broad padded points, with paired shoulder vertices instead of needle-like tips. */
    private static float[] cuteStar(float cx, float cy, float outer, float inner, float turn) {
        float[] pts = new float[40];
        int at = 0;
        for (int i = 0; i < 5; i++) {
            float tip = turn + i * 6.283185f / 5f;
            float before = tip - 3.141593f / 5f;
            // Wide valley, two shoulders across a blunt tip, then the next wide valley.
            pts[at++] = cx + inner * (float) Math.cos(before + 0.20f);
            pts[at++] = cy + inner * (float) Math.sin(before + 0.20f);
            pts[at++] = cx + outer * 0.91f * (float) Math.cos(tip - 0.105f);
            pts[at++] = cy + outer * 0.91f * (float) Math.sin(tip - 0.105f);
            pts[at++] = cx + outer * 0.91f * (float) Math.cos(tip + 0.105f);
            pts[at++] = cy + outer * 0.91f * (float) Math.sin(tip + 0.105f);
            pts[at++] = cx + inner * (float) Math.cos(tip + 3.141593f / 5f - 0.20f);
            pts[at++] = cy + inner * (float) Math.sin(tip + 3.141593f / 5f - 0.20f);
        }
        return pts;
    }
}
