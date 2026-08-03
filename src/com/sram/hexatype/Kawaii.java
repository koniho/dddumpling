package com.sram.hexatype;

/**
 * The six "letters", drawn as original kawaii characters: dumpling, strawberry, cat,
 * grapes, squishy ball, squish blob. Each has its own face so they stay tellable apart
 * at a glance even before you learn the colours.
 *
 * Everything is vector geometry emitted through {@link Painter}, which keeps the APK
 * asset-free, crisp at any tile size, and renderable by the offline preview harness.
 */
final class Kawaii {

    static final int DUMPLING = 0, STRAWBERRY = 1, CAT = 2, GRAPES = 3, SQUISHY = 4, BLOB = 5;

    private static final int INK = 0xFF3A2E4F;      // face lines
    private static final int BLUSH = 0x66FF7C9E;
    private static final int SHINE = 0x8CFFFFFF;
    private static final int LEAF = 0xFF8FD9A0;
    private static final int SEED = 0xB3FFF3C4;

    private Kawaii() {}

    /**
     * Draws character {@code g} centred on cx,cy at radius r.
     *
     * @param body  fill colour (callers cycle this for hit flashes)
     * @param squash 1 = round; >1 wider and flatter, <1 taller. Drives the squish-and-stretch.
     * @param happy 0..1 blend from a neutral face to a delighted one
     */
    static void draw(Painter p, int g, float cx, float cy, float r, int body, float squash,
            float happy) {
        float rx = r * squash;
        float ry = r / squash;
        switch (g) {
            case DUMPLING: dumpling(p, cx, cy, rx, ry, body, happy); break;
            case STRAWBERRY: strawberry(p, cx, cy, rx, ry, body, happy); break;
            case CAT: cat(p, cx, cy, rx, ry, body, happy); break;
            case GRAPES: grapes(p, cx, cy, rx, ry, body, happy); break;
            case SQUISHY: squishy(p, cx, cy, rx, ry, body, happy); break;
            default: blob(p, cx, cy, rx, ry, body, happy); break;
        }
    }

    // ---- characters ---------------------------------------------------------

    /** Pleated half-moon with sleepy ^^ eyes and a tiny uwu mouth. */
    private static void dumpling(Painter p, float cx, float cy, float rx, float ry, int body,
            float happy) {
        // Body: a half-disc crown closed by a flat bottom, with a belly ellipse under it.
        // The array must hold exactly the arc points — a spare slot would leave a vertex
        // at the origin and spike off-screen.
        final int arc = 17;
        float[] dome = new float[arc * 2];
        for (int k = 0; k < arc; k++) {
            double a = Math.PI + Math.PI * k / (arc - 1.0);
            dome[k * 2] = cx + rx * 0.96f * (float) Math.cos(a);
            dome[k * 2 + 1] = cy + ry * 0.70f * (float) Math.sin(a);
        }
        p.fillPoly(dome, body);
        p.fillEllipse(cx, cy + ry * 0.28f, rx * 0.96f, ry * 0.44f, body);

        // Pleated crown: bumps sitting on the dome edge.
        float crown = cy - ry * 0.56f;
        for (int k = -2; k <= 2; k++) {
            float px = cx + k * rx * 0.38f;
            p.fillEllipse(px, crown, rx * 0.21f, ry * 0.19f, body);
            p.polyline(new float[] {px, crown - ry * 0.14f, px, crown + ry * 0.16f},
                    Glyph.withAlpha(INK, 55), r(rx) * 0.05f);
        }
        p.fillEllipse(cx - rx * 0.34f, cy + ry * 0.04f, rx * 0.22f, ry * 0.14f, SHINE);

        eyesArc(p, cx, cy, rx, ry, 0.34f, 0.06f, 0.20f);
        mouthW(p, cx, cy + ry * 0.34f, rx * 0.18f, happy);
        blush(p, cx, cy, rx, ry, 0.62f, 0.26f);
    }

    /** Seeded berry with a leafy calyx, wide sparkly eyes and an open grin. */
    private static void strawberry(Painter p, float cx, float cy, float rx, float ry, int body,
            float happy) {
        float[] berry = new float[2 * 22];
        for (int k = 0; k < 22; k++) {
            double a = -Math.PI / 2 + 2 * Math.PI * k / 22.0;
            // Wide at the shoulders, tapering to a point at the bottom.
            float t = (float) ((Math.sin(a) + 1) / 2);
            float w = 0.62f + 0.40f * (1f - t) * (1f - t * 0.2f);
            berry[k * 2] = cx + rx * w * (float) Math.cos(a);
            berry[k * 2 + 1] = cy + ry * (0.20f + 0.86f * (float) Math.sin(a));
        }
        p.fillPoly(berry, body);

        for (int k = 0; k < 5; k++) {
            double a = Math.PI + Math.PI * (k + 0.5) / 5.0;
            float lx = cx + rx * 0.62f * (float) Math.cos(a);
            float ly = cy - ry * 0.62f + ry * 0.16f * (float) Math.sin(a);
            p.fillPoly(new float[] {cx, cy - ry * 0.52f, lx, ly - ry * 0.30f,
                    lx + rx * 0.14f, ly + ry * 0.06f}, LEAF);
        }
        p.fillEllipse(cx - rx * 0.30f, cy - ry * 0.12f, rx * 0.16f, ry * 0.20f, SHINE);
        for (int k = 0; k < 6; k++) {
            float sx = cx + rx * (-0.52f + 0.42f * (k % 3));
            float sy = cy + ry * (0.30f + 0.30f * (k / 3));
            p.fillEllipse(sx, sy, rx * 0.05f, ry * 0.08f, SEED);
        }

        eyesRound(p, cx, cy, rx, ry, 0.32f, -0.06f, 0.15f, happy);
        mouthOpen(p, cx, cy + ry * 0.40f, rx * 0.22f, ry * (0.12f + 0.10f * happy));
        blush(p, cx, cy, rx, ry, 0.60f, 0.18f);
    }

    /** Round head, two ears, whiskers and a :3 mouth. */
    private static void cat(Painter p, float cx, float cy, float rx, float ry, int body,
            float happy) {
        for (int s = -1; s <= 1; s += 2) {
            float ex = cx + s * rx * 0.56f;
            p.fillPoly(new float[] {ex - rx * 0.26f, cy - ry * 0.52f, ex + rx * 0.22f,
                    cy - ry * 0.44f, ex + s * rx * 0.06f, cy - ry * 1.02f}, body);
            p.fillPoly(new float[] {ex - rx * 0.13f, cy - ry * 0.54f, ex + rx * 0.11f,
                    cy - ry * 0.50f, ex + s * rx * 0.03f, cy - ry * 0.82f},
                    Glyph.withAlpha(BLUSH, 190));
        }
        p.fillEllipse(cx, cy + ry * 0.06f, rx * 0.94f, ry * 0.84f, body);
        p.fillEllipse(cx - rx * 0.32f, cy - ry * 0.24f, rx * 0.20f, ry * 0.16f, SHINE);

        eyesRound(p, cx, cy, rx, ry, 0.34f, 0.00f, 0.155f, happy);
        // Nose plus the :3 curve.
        p.fillPoly(new float[] {cx, cy + ry * 0.34f, cx - rx * 0.09f, cy + ry * 0.24f,
                cx + rx * 0.09f, cy + ry * 0.24f}, INK);
        float w = rx * 0.20f, d = ry * (0.16f + 0.08f * happy);
        p.polyline(new float[] {cx - w * 2, cy + ry * 0.34f, cx - w, cy + ry * 0.34f + d,
                cx, cy + ry * 0.34f}, INK, r(rx) * 0.075f);
        p.polyline(new float[] {cx, cy + ry * 0.34f, cx + w, cy + ry * 0.34f + d,
                cx + w * 2, cy + ry * 0.34f}, INK, r(rx) * 0.075f);
        for (int s = -1; s <= 1; s += 2) {
            for (int k = -1; k <= 1; k++) {
                p.polyline(new float[] {cx + s * rx * 0.44f, cy + ry * (0.24f + k * 0.10f),
                        cx + s * rx * 0.92f, cy + ry * (0.18f + k * 0.17f)},
                        Glyph.withAlpha(INK, 130), r(rx) * 0.045f);
            }
        }
        blush(p, cx, cy, rx, ry, 0.58f, 0.26f);
    }

    /** Cluster of berries with a stem; the face sits on the middle one. */
    private static void grapes(Painter p, float cx, float cy, float rx, float ry, int body,
            float happy) {
        p.polyline(new float[] {cx, cy - ry * 0.92f, cx + rx * 0.06f, cy - ry * 0.60f},
                0xFFB08968, r(rx) * 0.09f);
        p.fillPoly(new float[] {cx + rx * 0.04f, cy - ry * 0.86f, cx + rx * 0.62f,
                cy - ry * 0.96f, cx + rx * 0.30f, cy - ry * 0.58f}, LEAF);

        // Rows of 3/2/1 berries, back to front so the front one reads cleanly.
        float br = rx * 0.30f;
        float[][] rows = {{-2, 0, 2}, {-1, 1}, {0}};
        for (int rowIdx = 0; rowIdx < rows.length; rowIdx++) {
            for (int k = 0; k < rows[rowIdx].length; k++) {
                float gx = cx + rows[rowIdx][k] * br * 0.92f;
                float gy = cy - ry * 0.34f + rowIdx * ry * 0.46f;
                p.fillEllipse(gx, gy, br, br * (ry / rx), body);
                p.fillEllipse(gx - br * 0.30f, gy - br * 0.32f, br * 0.20f, br * 0.16f, SHINE);
            }
        }

        float fy = cy + ry * 0.24f;
        eyesRound(p, cx, fy, rx, ry, 0.16f, 0.00f, 0.085f, happy);
        p.fillEllipse(cx, fy + ry * 0.17f, rx * (0.06f + 0.04f * happy),
                ry * (0.07f + 0.05f * happy), INK);
    }

    /** A squeezed stress ball: squinting >< eyes, big grin, squish marks. */
    private static void squishy(Painter p, float cx, float cy, float rx, float ry, int body,
            float happy) {
        p.fillEllipse(cx, cy, rx * 0.98f, ry * 0.92f, body);
        p.fillEllipse(cx - rx * 0.34f, cy - ry * 0.34f, rx * 0.24f, ry * 0.18f, SHINE);
        // Squeeze dents on both flanks.
        for (int s = -1; s <= 1; s += 2) {
            p.fillEllipse(cx + s * rx * 0.90f, cy, rx * 0.16f, ry * 0.30f,
                    Glyph.withAlpha(INK, 34));
            for (int k = -1; k <= 1; k++) {
                p.polyline(new float[] {cx + s * rx * 1.06f, cy + ry * k * 0.28f,
                        cx + s * rx * 1.30f, cy + ry * k * 0.34f},
                        Glyph.withAlpha(0xFFFFFFFF, 90), r(rx) * 0.05f);
            }
        }
        // Squinting >< — one arrow per eye, pointing inward. Two overlaid arrows would
        // read as X_X (dead) rather than squeezed.
        float ey = cy - ry * 0.12f, ew = rx * 0.19f, eh = ry * 0.19f;
        for (int s = -1; s <= 1; s += 2) {
            float ex = cx + s * rx * 0.36f;
            p.polyline(new float[] {ex + s * ew, ey - eh, ex - s * ew * 0.9f, ey,
                    ex + s * ew, ey + eh}, INK, r(rx) * 0.09f);
        }
        mouthOpen(p, cx, cy + ry * 0.36f, rx * (0.26f + 0.06f * happy),
                ry * (0.16f + 0.10f * happy));
        blush(p, cx, cy, rx, ry, 0.66f, 0.16f);
    }

    /** Rounded plush blob with nubby ears and content ‿‿ eyes. */
    private static void blob(Painter p, float cx, float cy, float rx, float ry, int body,
            float happy) {
        for (int s = -1; s <= 1; s += 2) {
            p.fillEllipse(cx + s * rx * 0.56f, cy - ry * 0.62f, rx * 0.26f, ry * 0.26f, body);
        }
        float[] shape = new float[2 * 24];
        for (int k = 0; k < 24; k++) {
            double a = -Math.PI / 2 + 2 * Math.PI * k / 24.0;
            float t = (float) ((Math.sin(a) + 1) / 2);   // 0 top, 1 bottom
            float w = 0.72f + 0.28f * t;                 // pear-ish: wider at the base
            shape[k * 2] = cx + rx * w * (float) Math.cos(a);
            shape[k * 2 + 1] = cy + ry * (0.06f + 0.90f * (float) Math.sin(a));
        }
        p.fillPoly(shape, body);
        p.fillEllipse(cx - rx * 0.36f, cy - ry * 0.26f, rx * 0.22f, ry * 0.18f, SHINE);
        p.fillEllipse(cx, cy + ry * 0.46f, rx * 0.46f, ry * 0.26f, Glyph.withAlpha(0xFFFFFFFF, 46));

        eyesArc(p, cx, cy, rx, ry, 0.36f, -0.04f, -0.18f);
        p.polyline(new float[] {cx - rx * 0.11f, cy + ry * 0.26f, cx, cy + ry * (0.34f + 0.06f * happy),
                cx + rx * 0.11f, cy + ry * 0.26f}, INK, r(rx) * 0.07f);
        blush(p, cx, cy, rx, ry, 0.62f, 0.14f);
    }

    // ---- shared face parts --------------------------------------------------

    /** Solid oval eyes with a highlight; opens wider as {@code happy} rises. */
    private static void eyesRound(Painter p, float cx, float cy, float rx, float ry, float dx,
            float dy, float size, float happy) {
        float w = rx * size, h = ry * size * (1.18f + 0.25f * happy);
        for (int s = -1; s <= 1; s += 2) {
            float ex = cx + s * rx * dx, ey = cy + ry * dy;
            p.fillEllipse(ex, ey, w, h, INK);
            p.fillEllipse(ex - w * 0.30f, ey - h * 0.34f, w * 0.34f, h * 0.30f, 0xE6FFFFFF);
        }
    }

    /** Arc eyes: positive {@code bow} curves down (^^), negative curves up (‿‿). */
    private static void eyesArc(Painter p, float cx, float cy, float rx, float ry, float dx,
            float dy, float bow) {
        float w = rx * 0.19f, d = ry * bow;
        for (int s = -1; s <= 1; s += 2) {
            float ex = cx + s * rx * dx, ey = cy + ry * dy;
            p.polyline(new float[] {ex - w, ey, ex - w * 0.5f, ey - d, ex, ey - d * 1.15f,
                    ex + w * 0.5f, ey - d, ex + w, ey}, INK, r(rx) * 0.085f);
        }
    }

    private static void mouthW(Painter p, float cx, float cy, float w, float happy) {
        float d = w * (0.55f + 0.35f * happy);
        p.polyline(new float[] {cx - w, cy, cx - w * 0.5f, cy + d, cx, cy,
                cx + w * 0.5f, cy + d, cx + w, cy}, INK, w * 0.30f);
    }

    private static void mouthOpen(Painter p, float cx, float cy, float w, float h) {
        float[] m = new float[2 * 13];
        for (int k = 0; k < 13; k++) {
            double a = Math.PI * k / 12.0;
            m[k * 2] = cx + w * (float) Math.cos(a);
            m[k * 2 + 1] = cy + h * (float) Math.sin(a) * 1.6f;
        }
        p.fillPoly(m, INK);
        p.fillEllipse(cx, cy + h * 1.05f, w * 0.42f, h * 0.52f, 0xCCFF9EB5);
    }

    private static void blush(Painter p, float cx, float cy, float rx, float ry, float dx,
            float dy) {
        for (int s = -1; s <= 1; s += 2) {
            p.fillEllipse(cx + s * rx * dx, cy + ry * dy, rx * 0.19f, ry * 0.12f, BLUSH);
        }
    }

    /** Stroke widths scale off the unsquashed radius so squashing never thins the lines. */
    private static float r(float rx) {
        return rx;
    }
}
