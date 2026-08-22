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
                    0.18f * fade * Math.min(1f, c.slowdown / Blade.STAR_BEAT));
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
        // And it breathes while the flight is on: the same width and brightness held perfectly
        // still read as a painted line the course happened to be laid along, rather than as the
        // thing carrying the flyer. Only during the flight — the lesson and the tally are meant to
        // be moments where nothing moves, and a pulse under them undoes that.
        float beat = q.flying() ? 1f + 0.16f * (float) Math.sin(c.clock * 4.4f) : 1f;
        p.polyline(route, fadeBy(Glyph.withAlpha(0xFF241B50, 145), cf), pearl * 1.55f * beat);
        p.polyline(route, fadeBy(Glyph.withAlpha(0xFF6E72C8, 105), cf), pearl * 1.25f * beat);
        p.polyline(route, fadeBy(Glyph.withAlpha(0xFFBDEBFF,
                (int) (115 * (q.flying() ? beat : 1f))), cf), pearl * beat);
        if (q.flying()) pulse(p, q, route, L, c.clock, cf, pearl);
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

        float lean = q.lessonLean(c.clock);
        float x = q.flyerX(L) + lean * L.w, y = q.flyerY(L);
        if (q.ready()) {
            // Wordless lesson: the three left keys glow and the flyer leans left, then the
            // three right keys and a lean right. The real deck supplies the controls. Both the
            // lean and the glow fade out together over the last of the ready beat, so the flight
            // starts from a flyer standing still where it is about to fly from.
            float say = q.lessonFade();
            int side = lean < 0f ? 0 : 1;
            for (int g = side * 3; g < side * 3 + 3; g++) {
                float pulse = 1f + 0.08f * (float) Math.sin(c.clock * 8f + g);
                p.strokePoly(Glyph.hex(L.keyX[g], L.keyY[g], L.keyR * 1.18f * pulse),
                        fadeBy(Glyph.withAlpha(GOLD, 210), fade * say), L.keyR * 0.08f);
            }
        }

        float rr = StarPath.flyerR(L);
        // Keep the rocket exhaust through the climb-out. Its long exit plume leaves live stars
        // on screen after the flyer itself has cleared the top, making the departure a blast-off.
        if (q.ready() || q.flying() || q.exiting()) {
            // The lesson's lean is drawn onto the position rather than steered, so its sway has to
            // be asked for separately; in flight the steering itself is the answer.
            float sway = q.ready() ? q.lessonSway(c.clock) : q.vx / (L.w * StarPath.MAX_VX);
            wake(p, q, L, x, y, sway, c.clock, fade);
        }
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

    /**
     * Beads of light running up the course while it is being flown.
     *
     * Placed in screen space rather than along the route array, which matters more than it sounds:
     * the course is ten screens long and only about a fifth of one is in shot, so beads spread
     * evenly along the *route* would put nearly all of them off-screen and the one on it would
     * appear and vanish at odd intervals. Given a height, the route's own geometry supplies the x —
     * see {@link #routeX} — so a bead follows every bend of the visible stretch, and the period is
     * a screen-crossing in seconds, which stays put when the spacing knob moves.
     *
     * Upward, against the scroll, because that is the direction the flyer is travelling: the course
     * comes down past it, and a pulse running the other way reads as the path pulling the flyer on.
     */
    private static void pulse(Painter p, StarPath q, float[] route, Layout L, float clock,
            float cf, float pearl) {
        final int beads = 3;
        final float cross = 1.15f;
        for (int k = 0; k < beads; k++) {
            float u = frac(clock / cross + k / (float) beads);
            float y = L.dangerY + (L.playTop - L.dangerY) * u;
            float x = routeX(route, y);
            if (Float.isNaN(x)) continue;
            // Both ends of the run are fades: a bead switched on at the danger line and off at the
            // top of the field flickers exactly where the eye is following it.
            float lum = Math.min(1f, Math.min(u, 1f - u) / 0.16f);
            p.fillCircle(x, y, pearl * 1.45f,
                    fadeBy(Glyph.withAlpha(0xFFBDEBFF, (int) (70 * lum)), cf));
            p.fillCircle(x, y, pearl * 0.78f,
                    fadeBy(Glyph.withAlpha(0xFFFFFFFF, (int) (150 * lum)), cf));
        }
    }

    /**
     * Where the route is at height {@code y}, or NaN if it is not on the route at all.
     *
     * The samples run bottom to top and the course is a function of height, so this is one walk and
     * a lerp. Off either end returns NaN rather than clamping, so a bead is drawn nowhere instead
     * of being pinned to the last checkpoint.
     */
    private static float routeX(float[] route, float y) {
        for (int i = 0; i + 3 < route.length; i += 2) {
            float y0 = route[i + 1], y1 = route[i + 3];
            if ((y <= y0 && y >= y1) || (y >= y0 && y <= y1)) {
                float span = y1 - y0;
                float t = Math.abs(span) < 1e-4f ? 0f : (y - y0) / span;
                return route[i] + (route[i + 2] - route[i]) * t;
            }
        }
        return Float.NaN;
    }

    /**
     * The wake: mini stars in the six letter colours streaming off behind the flyer, thicker and
     * brighter the fuller the course is.
     *
     * The strength is {@code count()}, so it counts the stars carried over from a failed attempt as
     * well as the ones taken on this one — a course resumed at fifteen opens with the wake of a
     * flyer that has fifteen, which is the whole point of it. That also makes it the one thing on
     * screen that says how far along a playthrough is without a number.
     *
     * Stateless on purpose, and shaped like {@code Cabinet.shimmer}: preview output is a pure
     * function of state and hash-compared between runs, so there is no RNG and no stored particle
     * list here. Each spark's phase and rate come out of its index, and its position out of the
     * flyer's — which is also what keeps it working in a harness frame that jumped straight to the
     * middle of a flight instead of stepping there.
     */
    private static void wake(Painter p, StarPath q, Layout L, float x, float y, float sway,
            float clock, float fade) {
        float rr = StarPath.flyerR(L);
        float str = q.count() / (float) StarPath.COUNT;
        float exit = q.exitProgress();
        int n = 10 + q.count() + (int) (18f * exit);
        float len = rr * (1.35f + 1.85f * str + 1.4f * exit);
        // A widening rocket plume: every spark falls, while its hashed side and wobble spray the
        // exhaust around the downward axis. Steering only leans the whole cone a little.
        float drop = exit > 0f
                ? Math.max(len * 1.7f, (L.dangerY - L.playTop) * (0.35f + 0.45f * exit))
                : Math.min(len * 1.65f, Math.max(rr * 0.85f, L.deckTop - y - rr * 0.2f));
        for (int k = 0; k < n; k++) {
            float h1 = frac(k * 0.6180339f), h2 = frac(k * 0.7548777f), h3 = frac(k * 0.4501f);
            float rate = 2.4f + 1.8f * h2 + 1.4f * exit;
            float age = frac(clock * rate + h1);
            // Started clear of the flyer's own aura, or half the wake is behind the character it is
            // coming off and the count that drives it cannot be read.
            float out = rr * (0.44f - 0.20f * age)
                    * (0.72f + 0.45f * str + 0.22f * exit);
            float from = rr * 1.05f;
            float side = h2 * 2f - 1f;
            float spread = side * len * (0.16f + 0.58f * age) * age;
            float wobble = rr * (0.18f + 0.28f * h3) * age
                    * (float) Math.sin(StarPath.TAU * (h1 + age * (0.8f + h3)));
            float cx = x - sway * (from * 0.18f + len * 0.30f * age) + spread + wobble;
            float cy = y + from * 0.40f + drop * age * (0.82f + 0.30f * h3);
            float dim = 1f - age * age * 0.85f;
            int col = Glyph.withAlpha(Glyph.COLOR[k % Glyph.COUNT],
                    (int) ((125f + 130f * str) * dim));
            float streak = rr * (0.35f + 0.55f * age + 0.45f * exit);
            p.line(cx, cy - streak, cx, cy,
                    fadeBy(Glyph.withAlpha(Glyph.COLOR[k % Glyph.COUNT],
                            (int) (95f * dim)), fade), Math.max(1f, out * 0.24f));
            p.fillPoly(star(cx, cy, out, out * 0.42f, 5,
                    clock * (0.7f + h3) + h1 * StarPath.TAU), fadeBy(col, fade));
        }
    }

    /** Fractional part, for hashing an index into a phase. */
    private static float frac(float v) {
        return v - (float) Math.floor(v);
    }

    /** The climber: a soft aura, a rimmed hex, and whichever collectible is piloting it. */
    private static void flyer(Painter p, StarPath q, float rr, float x, float y, float clock,
            float fade) {
        if (q.dragging) {
            float pulse = 1f + 0.10f * (float) Math.sin(clock * 9f);
            p.fillCircle(x, y, rr * 1.62f * pulse,
                    fadeBy(Glyph.withAlpha(0xFF93D6F7, 48), fade));
            p.strokePoly(Glyph.hex(x, y, rr * 1.38f * pulse),
                    fadeBy(Glyph.withAlpha(0xFFFFFFFF, 190), fade), rr * 0.10f);
        }
        p.fillCircle(x, y, rr * 1.18f, fadeBy(Glyph.withAlpha(0xFF93D6F7,
                q.dragging ? 125 : 55), fade));
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

        // The flyer goes on first, so the prize it is looking up at is never behind it. No wake
        // here, though it is the one moment a full-strength one could be shown: the tableau is a
        // full stop, with the flyer standing still, and the prize's name sits directly under it —
        // twenty sparks at full brightness fell straight down through the label.
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

    /**
     * Smooth samples through the scrolling checkpoints, padded at both ends for tangents, with a
     * runway under the first one.
     *
     * The runway is a straight drop from the opening checkpoint to the danger line, and it is there
     * because of the spacing: at ten screens of course the first star waits a quarter of the field
     * above where the flyer sits for the ready lesson, so without it the flyer spends that whole
     * beat hanging under a line it is not on, with nothing between them. Dropped once the course has
     * scrolled far enough that the first checkpoint is past the line — after that there is no
     * "before the start" left to draw.
     */
    private static float[] spline(StarPath q, Layout L) {
        final int steps = 7;
        boolean runway = q.starY(0, L) < L.dangerY;
        float[] pts = new float[(StarPath.COUNT - 1) * steps * 2 + 2 + (runway ? 2 : 0)];
        int at = 0;
        if (runway) {
            pts[at++] = q.starX(0, L);
            pts[at++] = L.dangerY;
        }
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
