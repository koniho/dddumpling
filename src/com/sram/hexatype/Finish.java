package com.sram.hexatype;

/**
 * The nine collectible surfaces: matte, glitter, holo, galaxy, metallic, clear, glow,
 * tie-dye, confetti. Indexed by {@link Collect#FINISH}, drawn over a {@link Shape}.
 *
 * Everything here stays inside a conservative ellipse of the nominal radius, because
 * {@link Painter} can only clip to a rectangle and there is no way to clip a fill to an
 * arbitrary silhouette. The banded finishes are restricted further, to round shapes only,
 * by {@link Collect#banded} — with an assertion holding the catalogue to it.
 */
final class Finish {

    private Finish() {}

    /** Overlays finish {@code f}. Matte wears nothing, so this is a no-op for it. */
    static void draw(Painter p, int f, float cx, float cy, float r, int accent,
            float clock, float fade, int seed) {
        switch (f) {
            case Collect.GLITTER: specks(p, cx, cy, r, accent, clock, fade, seed, 18, false);
                break;
            case Collect.CONFETTI: specks(p, cx, cy, r, accent, clock, fade, seed, 20, true);
                break;
            case Collect.HOLO: holo(p, cx, cy, r, clock, fade); break;
            case Collect.GALAXY: galaxy(p, cx, cy, r, accent, clock, fade, seed); break;
            case Collect.METALLIC: metallic(p, cx, cy, r, accent, fade); break;
            case Collect.CLEAR: clear(p, cx, cy, r, fade); break;
            case Collect.GLOW: glow(p, cx, cy, r, accent, clock, fade); break;
            case Collect.TIEDYE: tiedye(p, cx, cy, r, accent, fade); break;
            default: break;                                    // matte wears nothing
        }
    }

    /**
     * Scattered flecks, twinkling. Glitter takes the accent colour; confetti takes a
     * different hue per fleck, which is the only thing separating the two.
     */
    private static void specks(Painter p, float cx, float cy, float r, int accent, float clock,
            float fade, int seed, int n, boolean rainbow) {
        for (int k = 0; k < n; k++) {
            float u = Draw.hash(seed * 977 + k * 31);
            float v = Draw.hash(seed * 613 + k * 57 + 9);
            // Polar placement with a sqrt radius, so the flecks spread evenly over the disc
            // instead of bunching in the middle.
            double a = u * 2 * Math.PI;
            float rad = r * 0.66f * (float) Math.sqrt(v);
            float x = cx + rad * (float) Math.cos(a);
            float y = cy + rad * 0.92f * (float) Math.sin(a);
            float twinkle = 0.45f + 0.55f * (float) Math.sin(clock * 3.4f + k * 1.7f);
            int col = rainbow ? Glyph.cycle(k / (float) n + seed * 0.07f) : accent;
            float sz = r * (rainbow ? 0.075f : 0.055f) * (0.7f + 0.5f * twinkle);
            if (rainbow) {
                p.fillPoly(new float[] {x - sz, y - sz * 0.5f, x + sz, y - sz * 0.9f,
                        x + sz * 0.8f, y + sz * 0.6f, x - sz * 0.9f, y + sz},
                        Draw.fadeBy(Glyph.withAlpha(col, 230), fade));
            } else {
                p.fillCircle(x, y, sz, Draw.fadeBy(Glyph.withAlpha(col,
                        (int) (150 + 100 * twinkle)), fade));
            }
        }
    }

    /** Iridescence: slanted strips cycling through hue, offset from each other. */
    private static void holo(Painter p, float cx, float cy, float r, float clock, float fade) {
        for (int k = 0; k < 4; k++) {
            // Corners are kept inside a 0.83r circle by construction, which is what lets
            // this sit on a round shape without poking out of it.
            float x0 = cx + r * (-0.46f + 0.31f * k);
            float lean = r * 0.20f, w = r * 0.065f;
            p.fillPoly(new float[] {x0 - w - lean, cy - r * 0.60f, x0 + w - lean,
                    cy - r * 0.60f, x0 + w + lean, cy + r * 0.60f, x0 - w + lean,
                    cy + r * 0.60f},
                    Draw.fadeBy(Glyph.withAlpha(Glyph.cycle(clock * 0.35f + k * 0.14f), 95),
                            fade));
        }
    }

    /** Deep space: darkened body, a nebula smear, and a scatter of stars. */
    private static void galaxy(Painter p, float cx, float cy, float r, int accent, float clock,
            float fade, int seed) {
        p.fillEllipse(cx, cy + r * 0.02f, r * 0.74f, r * 0.70f,
                Draw.fadeBy(Glyph.withAlpha(accent, 185), fade));
        p.fillEllipse(cx - r * 0.16f, cy - r * 0.12f, r * 0.42f, r * 0.24f,
                Draw.fadeBy(Glyph.withAlpha(0xFFB79BF0, 90), fade));
        for (int k = 0; k < 16; k++) {
            float u = Draw.hash(seed * 331 + k * 71), v = Draw.hash(seed * 197 + k * 13);
            float x = cx + r * 0.62f * (u * 2f - 1f);
            float y = cy + r * 0.58f * (v * 2f - 1f);
            float tw = 0.4f + 0.6f * (float) Math.sin(clock * 4.1f + k * 2.3f);
            p.fillCircle(x, y, r * 0.035f * (0.6f + tw),
                    Draw.fadeBy(Glyph.withAlpha(0xFFFFFFFF, (int) (120 + 135 * tw)), fade));
        }
    }

    /** Chrome: one hard specular streak plus a small hot spot, no soft gradient. */
    private static void metallic(Painter p, float cx, float cy, float r, int accent,
            float fade) {
        p.fillPoly(new float[] {cx - r * 0.52f, cy - r * 0.10f, cx - r * 0.16f,
                cy - r * 0.52f, cx + r * 0.02f, cy - r * 0.44f, cx - r * 0.34f,
                cy - r * 0.02f}, Draw.fadeBy(Glyph.withAlpha(accent, 225), fade));
        p.fillEllipse(cx + r * 0.30f, cy + r * 0.26f, r * 0.14f, r * 0.09f,
                Draw.fadeBy(Glyph.withAlpha(0xFFFFFFFF, 190), fade));
    }

    /** See-through: a milky wash and the far wall showing through as an inner outline. */
    private static void clear(Painter p, float cx, float cy, float r, float fade) {
        p.fillEllipse(cx, cy + r * 0.02f, r * 0.70f, r * 0.64f,
                Draw.fadeBy(Glyph.withAlpha(0xFFFFFFFF, 70), fade));
        p.strokeCircle(cx, cy + r * 0.10f, r * 0.44f,
                Draw.fadeBy(Glyph.withAlpha(0xFFFFFFFF, 130), fade), r * 0.05f);
    }

    /** Glow: halos outside the body, which is the one finish that may exceed the radius. */
    private static void glow(Painter p, float cx, float cy, float r, int accent, float clock,
            float fade) {
        float breathe = 0.85f + 0.15f * (float) Math.sin(clock * 2.2f);
        for (int k = 3; k >= 1; k--) {
            p.fillCircle(cx, cy, r * (1.0f + 0.22f * k) * breathe,
                    Draw.fadeBy(Glyph.withAlpha(accent, 34 / k), fade));
        }
    }

    /**
     * Two-tone swirl: horizontal bands of the accent, each no wider than the body is at its
     * narrower edge, so the stack stays inside the silhouette.
     */
    private static void tiedye(Painter p, float cx, float cy, float r, int accent,
            float fade) {
        final int bands = 4;
        float top = -r * 0.28f, step = (r * 1.00f) / bands;
        for (int k = 0; k < bands; k += 2) {
            float y0 = top + k * step, y1 = y0 + step;
            float w = Math.min(span(r, y0), span(r, y1));
            // Ends offset in opposite directions, which is what makes it read as swirled
            // rather than as stripes.
            float lean = r * 0.10f * (k == 0 ? 1f : -1f);
            p.fillPoly(new float[] {cx - w + lean, cy + y0, cx + w + lean, cy + y0,
                    cx + w - lean, cy + y1, cx - w - lean, cy + y1},
                    Draw.fadeBy(Glyph.withAlpha(accent, 150), fade));
        }
    }

    /** Half-width of the reference body ellipse at vertical offset {@code dy}. */
    private static float span(float r, float dy) {
        float ry = r * 0.86f;
        float t = 1f - (dy * dy) / (ry * ry);
        return t <= 0f ? 0f : r * 0.80f * (float) Math.sqrt(t);
    }
}
