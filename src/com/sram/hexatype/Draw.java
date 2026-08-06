package com.sram.hexatype;

/**
 * Palette and reusable geometry, shared by every drawing class.
 *
 * The renderers extend this so colours and shape helpers resolve unqualified — the
 * alternative was prefixing several hundred references, which buys nothing.
 */
abstract class Draw {

    static final int BG = 0xFF1B1730;
    static final int BG_HI = 0xFF251F42;
    static final int BG_HURT = 0xFF4A0F22;
    static final int INK = 0xFFF6F1FF;
    static final int INK_DIM = 0xFFA79DCC;
    static final int ROSE = 0xFFFF7C9E;
    static final int GOLD = 0xFFFFCE4A;
    /** Brighter and purer than GOLD, which shares the game-over screen with it. */
    static final int YELLOW = 0xFFFFE536;
    /** The deck goes this colour as a run ends. */
    static final int RED = 0xFFE33B4B;
    /** What the sky drains to as a run ends. */
    static final int BG_DEATH = 0xFF0B2A1C;
    static final int BAMBOO = 0xFFD9AE6E;
    static final int BAMBOO_DARK = 0xFF8E6B3A;

    /**
     * Every text size on screen goes through {@link #type}, which multiplies by this. One knob for
     * legibility on a phone held at arm's length, rather than a hundred hand-tuned multiples.
     *
     * Not 2, which was the first ask: at 2 the long lines — the title screen's two-line
     * explanation, ANY KEY FOR THE TITLE SCREEN, the story panel's 36-character lines — run off
     * their panels or off the screen, and several panels are sized from the text they hold. This
     * is as far as it goes with everything still inside its box.
     */
    static final float TEXT = 1.34f;

    /**
     * A text size, scaled. Wrapped round the size argument of every text call so the scale is
     * visible at the point of use — a multiplier hidden inside the two Painter backends would
     * make {@code size} stop meaning pixels, which is the sort of thing that costs an afternoon
     * later.
     */
    static float type(float size) {
        return size * TEXT;
    }

    /** Scales a colour's existing alpha by {@code f}, for fading a whole scene at once. */
    static int fadeBy(int color, float f) {
        if (f >= 1f) return color;
        int a = (color >>> 24) & 0xFF;
        return Glyph.withAlpha(color, (int) (a * (f < 0f ? 0f : f)));
    }

    /**
     * Rainbow for FLURRY, phased by height so the colour change starts at the bottom of the
     * screen and travels upward. Everything on one wave, so the keys and the letters above
     * them read as a single sweep rather than as independent flickering.
     */
    static int rainbowAt(float y, Layout L, float clock) {
        float up = 1f - y / L.h;                 // 0 at the bottom, 1 at the top
        return Glyph.cycle(clock * 0.85f - up * 0.8f);
    }

    /**
     * Edge glow from overlapping strips rather than discrete rings: each layer reaches
     * from an edge inward by a shrinking amount, all at the same low alpha, so the
     * build-up is a smooth ramp instead of visible bands. Corners get both a horizontal
     * and a vertical layer, which is what a vignette wants anyway.
     */
    /**
     * Fully rounded rectangle — a stadium: straight top and bottom edges with semicircular
     * caps, cap radius equal to the half-height. Emitted as one polygon rather than a rect
     * plus two circles, because translucent fills would double-blend where those overlap.
     *
     * @param rx half the total length, caps included
     * @param ry half the height, which is also the cap radius
     */
    static float[] pill(float cx, float cy, float rx, float ry, int segs) {
        float straight = Math.max(0f, rx - ry);
        float[] pts = new float[(segs + 1) * 4];
        int i = 0;
        // Right cap: top, round the outside, to bottom.
        for (int k = 0; k <= segs; k++) {
            double a = -Math.PI / 2 + Math.PI * k / segs;
            pts[i++] = cx + straight + ry * (float) Math.cos(a);
            pts[i++] = cy + ry * (float) Math.sin(a);
        }
        // Left cap: bottom, round the outside, back to top.
        for (int k = 0; k <= segs; k++) {
            double a = Math.PI / 2 + Math.PI * k / segs;
            pts[i++] = cx - straight + ry * (float) Math.cos(a);
            pts[i++] = cy + ry * (float) Math.sin(a);
        }
        return pts;
    }

    /**
     * The caret over the thing you have to press next: a small triangle pointing down at it.
     *
     * Shared so the interlude marks its wanted letter exactly the way the field marks a head tile.
     * Two hand-rolled triangles would drift apart the first time either was tuned, and the whole
     * point of it is that the player recognises the same mark in both places.
     *
     * @param r radius of the thing it points at
     */
    static void caret(Painter p, float cx, float cy, float r, int color) {
        float y = cy - r * 1.55f, w = r * 0.40f;
        p.fillPoly(new float[] {cx - w, y - w, cx + w, y - w, cx, y + w * 0.75f}, color);
    }

    /** Star polygon with {@code points} spikes, rotated by {@code rot} radians. */
    static float[] star(float cx, float cy, float outer, float inner, int points,
            float rot) {
        float[] pts = new float[points * 4];
        for (int i = 0; i < points * 2; i++) {
            double a = rot + Math.PI * i / points;
            float rr = (i % 2 == 0) ? outer : inner;
            pts[i * 2] = cx + rr * (float) Math.cos(a);
            pts[i * 2 + 1] = cy + rr * (float) Math.sin(a);
        }
        return pts;
    }

    /** Deterministic 0..1 from an int, so cloud shapes are stable across frames. */
    static float hash(int seed) {
        int h = seed * 374761393 + 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (float) 0xFFFFFF;
    }
}
