package com.dddumpling.game;

/**
 * A glass display cabinet: a box seen three-quarters on, drawn as translucent panes wrapped in
 * shimmering wireframes. {@link Showcase} stands the collection inside one, and its badge on the
 * title screen is the same box small.
 *
 * Its own file because it knows nothing about collectibles — give it a rectangle and it draws a
 * case around it.
 */
final class Cabinet extends Draw {

    private Cabinet() {}

    /**
     * Wireframes nested inside each other, outermost first: white, then two translucent tints.
     * Three copies of the same box a hair apart, each drifting on its own, is what gives the
     * glass its shimmer — one wireframe on its own reads as a diagram.
     */
    private static final int[] FRAME_TINT = {INK, 0xFF8FE9FF, ROSE};
    private static final int[] FRAME_ALPHA = {155, 120, 105};

    /** How far the box recedes, as a fraction of its depth: right and up, so we see two faces. */
    private static final float DEPTH_X = 0.62f, DEPTH_Y = -0.42f;

    /** Where the shelf plane sits, as a fraction of the box height up from the bottom. */
    private static final float SHELF = 0.24f;

    /**
     * A deterministic wobble in -1..1: two sines at hashed rates and phases.
     *
     * Deliberately not an RNG. The preview harness renders as a pure function of state, and
     * every frame it writes is compared by hash against the last run — a real random walk here
     * would make every check differ for no reason. Sines off the clock read as shimmer anyway,
     * because the eye is looking for glass, not for noise.
     */
    static float shimmer(float clock, int seed) {
        float a = hash(seed * 31 + 7), b = hash(seed * 17 + 3);
        return (float) (0.62 * Math.sin(clock * (2.1f + 3.2f * a) + a * 6.283f)
                + 0.38 * Math.sin(clock * (5.1f + 4.3f * b) + b * 6.283f));
    }

    /** Front glass catches more light while a row or column is being carried. */
    static void reflection(Painter p, float clock, float l, float t, float r, float b,
            float slideX, float slideY, float fade) {
        float w = r - l, h = b - t;
        float motion = Math.min(1f, (Math.abs(slideX) + Math.abs(slideY)) * 2f);
        float drift = (float) Math.sin(clock * 0.65f) * 0.20f;
        float x = l + w * (0.46f + drift + slideX * 0.28f + slideY * 0.22f);
        float lean = w * (0.27f + slideY * 0.08f), band = w * 0.075f;
        p.save();
        p.clipRect(l, t, r, b);
        p.fillPoly(new float[] {x,t,x+band,t,x-lean+band,b,x-lean,b},
                fadeBy(Glyph.withAlpha(0xFFB4F2FF, 8 + (int) (motion * 24)), fade));
        p.fillPoly(new float[] {x+band*1.5f,t,x+band*1.7f,t,
                x-lean+band*1.7f,b,x-lean+band*1.5f,b},
                fadeBy(Glyph.withAlpha(INK, 12 + (int) (motion * 30)), fade));
        float y = t + h * (0.5f + slideY * 0.28f) + shimmer(clock, 41) * h * 0.03f;
        p.line(l, y, l, Math.min(b, y + h * 0.18f),
                fadeBy(Glyph.withAlpha(0xFF8FE9FF, 80 + (int) (motion * 100)), fade), w * 0.004f);
        p.restore();
    }

    /**
     * The fixed three-quarter view, receding right and up: how the open case stands.
     *
     * @param depth how far the box recedes
     * @param inset spacing between the nested wireframes; also scales their drift and weight
     */
    static void draw(Painter p, float clock, float l, float t, float r, float b, float depth,
            float inset, float fade) {
        draw(p, clock, l, t, r, b, depth * DEPTH_X, depth * DEPTH_Y, 1f, inset, fade);
    }

    /**
     * The general case: the back face is the front one moved by (ox, oy) and shrunk by
     * {@code taper}, which is enough to point the box anywhere.
     *
     * A back face scaled toward a point is what perspective does, so a caller that derives
     * (ox, oy) from a fixed eye position gets a box that turns as it moves across the screen —
     * see {@link Showcase#icon}. It stays an axis-aligned rectangle either way, which is the
     * whole reason the four walls can be plain quads.
     *
     * The interior is filled dark under the glass before any of it. Tinted panes alone left the
     * squishies and their captions sitting on whatever sky happened to be behind them, and the
     * whole point of a case is looking at the thing in it.
     *
     * @param taper how much smaller the back face is; 1 for none
     */
    static void draw(Painter p, float clock, float l, float t, float r, float b, float ox,
            float oy, float taper, float inset, float fade) {
        if (fade <= 0.004f) return;
        float cx = (l + r) / 2f, cy = (t + b) / 2f;
        float bl = cx + ox + taper * (l - cx), br = cx + ox + taper * (r - cx);
        float bt = cy + oy + taper * (t - cy), bb = cy + oy + taper * (b - cy);

        // Back wall, then the four walls that reach it, then the front pane. Each is one polygon
        // per layer: translucent shapes that overlap double-blend, so a face built out of parts
        // comes out darker along its seams.
        p.fillPoly(new float[] {bl, bt, br, bt, br, bb, bl, bb},
                fadeBy(Glyph.withAlpha(0xFF241E3E, 168), fade));
        int wall = fadeBy(Glyph.withAlpha(0xFF2A2348, 150), fade);
        p.fillPoly(new float[] {l, t, r, t, br, bt, bl, bt}, wall);
        p.fillPoly(new float[] {l, b, r, b, br, bb, bl, bb}, wall);
        p.fillPoly(new float[] {l, t, bl, bt, bl, bb, l, b}, wall);
        p.fillPoly(new float[] {r, t, br, bt, br, bb, r, b}, wall);
        // Glass on the panes we are looking through, brightest along the top.
        p.fillPoly(new float[] {l, t, r, t, br, bt, bl, bt},
                fadeBy(Glyph.withAlpha(INK, 30), fade));
        p.fillPoly(new float[] {r, t, br, bt, br, bb, r, b},
                fadeBy(Glyph.withAlpha(FRAME_TINT[1], 22), fade));
        p.fillPoly(new float[] {l, t, bl, bt, bl, bb, l, b},
                fadeBy(Glyph.withAlpha(FRAME_TINT[1], 14), fade));
        p.fillPoly(new float[] {l, t, r, t, r, b, l, b},
                fadeBy(Glyph.withAlpha(INK, 12), fade));

        // The shelf, as its own receding plane, which is what gives the box an inside.
        float sy = b - (b - t) * SHELF;
        float bsy = cy + oy + taper * (sy - cy);
        p.fillPoly(new float[] {l, sy, r, sy, br, bsy, bl, bsy},
                fadeBy(Glyph.withAlpha(INK, 16), fade));
        p.polyline(new float[] {l, sy, bl, bsy, br, bsy, r, sy, l, sy},
                fadeBy(Glyph.withAlpha(INK, 60), fade), inset * 0.16f);

        for (int f = 0; f < FRAME_TINT.length; f++) {
            float in = f * inset;
            // Shallower and less tapered each frame in, so they nest in depth as well as across.
            float shrink = 1f - 0.16f * f;
            float jx = shimmer(clock, f * 5 + 1) * inset * 0.32f;
            float jy = shimmer(clock, f * 5 + 2) * inset * 0.26f;
            float fl = l + in + jx, ft = t + in + jy, fr = r - in + jx, fb = b - in + jy;
            float fcx = (fl + fr) / 2f, fcy = (ft + fb) / 2f;
            float ft2 = 1f - (1f - taper) * shrink;
            float kl = fcx + ox * shrink + ft2 * (fl - fcx);
            float kr = fcx + ox * shrink + ft2 * (fr - fcx);
            float kt = fcy + oy * shrink + ft2 * (ft - fcy);
            float kb = fcy + oy * shrink + ft2 * (fb - fcy);
            int col = fadeBy(Glyph.withAlpha(FRAME_TINT[f], FRAME_ALPHA[f]), fade);
            // Dimmer at the back, which is the only depth cue a wireframe has.
            int far = fadeBy(Glyph.withAlpha(FRAME_TINT[f], FRAME_ALPHA[f] * 55 / 100), fade);
            float w = inset * (0.20f - 0.03f * f);
            p.strokePoly(new float[] {fl, ft, fr, ft, fr, fb, fl, fb}, col, w);
            p.strokePoly(new float[] {kl, kt, kr, kt, kr, kb, kl, kb}, far, w * 0.8f);
            p.line(fl, ft, kl, kt, far, w * 0.8f);
            p.line(fr, ft, kr, kt, col, w * 0.8f);
            p.line(fr, fb, kr, kb, col, w * 0.8f);
            p.line(fl, fb, kl, kb, far, w * 0.8f);
        }
    }
}
