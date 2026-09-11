package com.dddumpling.game;

/**
 * The six-character alphabet: a bright pastel colour per "letter", paired with the kawaii
 * character drawn by {@link Kawaii}. Colour and shape are both distinctive, so a word
 * stays readable at speed.
 *
 * Pure Java on purpose: the on-device renderer and the offline preview harness share it.
 */
final class Glyph {
    static final int COUNT = 6;

    static final int[] COLOR = {
        0xFFFFE1A0, // 0 dumpling   - warm cream
        0xFFFF8FA6, // 1 strawberry - berry pink
        0xFFFFBE85, // 2 cat        - apricot
        0xFFC3A8F5, // 3 grapes     - grape purple
        0xFF9AE9C0, // 4 squishy    - mint
        0xFF93D6F7, // 5 blob       - sky
    };

    static final String[] NAME = {"dumpling", "strawberry", "cat", "grapes", "squishy", "blob"};

    private Glyph() {}

    /** Flat-top hexagon: width 2r, height sqrt(3)*r. */
    static float[] hex(float cx, float cy, float r) {
        float[] pts = new float[12];
        for (int i = 0; i < 6; i++) {
            double a = Math.PI / 3.0 * i;
            pts[i * 2] = cx + r * (float) Math.cos(a);
            pts[i * 2 + 1] = cy + r * (float) Math.sin(a);
        }
        return pts;
    }

    /** Colour with its alpha channel replaced by {@code a} (0..255). */
    static int withAlpha(int color, int a) {
        if (a < 0) a = 0;
        if (a > 255) a = 255;
        return (color & 0x00FFFFFF) | (a << 24);
    }

    /** Linear blend from {@code a} to {@code b}; keeps a's alpha. */
    static int mix(int a, int b, float t) {
        if (t <= 0) return a;
        if (t >= 1) return (a & 0xFF000000) | (b & 0x00FFFFFF);
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        return (a & 0xFF000000) | (r << 16) | (g << 8) | bl;
    }

    /**
     * Bright pastel from a looping hue, for the fast colour cycling on a struck letter and
     * on an activated key. {@code phase} is in turns, not radians.
     */
    static int cycle(float phase) {
        float h = phase - (float) Math.floor(phase);
        return hsv(h * 6f, 0.40f, 1f);
    }

    /** @param h6 hue in 0..6 sextants */
    private static int hsv(float h6, float s, float v) {
        int sector = (int) Math.floor(h6) % 6;
        if (sector < 0) sector += 6;
        float f = h6 - (float) Math.floor(h6);
        int hi = (int) (v * 255);
        int lo = (int) (v * (1 - s) * 255);
        int up = (int) (v * (1 - s * (1 - f)) * 255);
        int dn = (int) (v * (1 - s * f) * 255);
        switch (sector) {
            case 0: return rgb(hi, up, lo);
            case 1: return rgb(dn, hi, lo);
            case 2: return rgb(lo, hi, up);
            case 3: return rgb(lo, dn, hi);
            case 4: return rgb(up, lo, hi);
            default: return rgb(hi, lo, dn);
        }
    }

    private static int rgb(int r, int g, int b) {
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
