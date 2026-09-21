package com.dddumpling.game;

/**
 * The title's tiny display face. Shared by the title and town balloons; each glyph is a
 * hand-shaped compound vector silhouette, with an outer contour followed by any punched counters.
 */
final class TitleBubbleFont extends Draw {
    private TitleBubbleFont() {}

    private static final float[][] D = {
        {-0.40f,-0.82f, 0.05f,-0.82f, 0.30f,-0.75f, 0.45f,-0.58f, 0.49f,-0.40f,
          0.45f,-0.22f, 0.30f,-0.06f, 0.05f,0f, -0.40f,0f},
        {-0.14f,-0.59f, 0.02f,-0.59f, 0.15f,-0.54f, 0.21f,-0.43f, 0.21f,-0.36f,
          0.15f,-0.25f, 0.02f,-0.22f, -0.14f,-0.22f}
    };
    private static final float[][] U = {{
        -0.46f,-0.82f,-0.16f,-0.82f,-0.16f,-0.29f,-0.12f,-0.20f,-0.04f,-0.16f,
         0.05f,-0.16f,0.13f,-0.20f,0.17f,-0.29f,0.17f,-0.82f,0.47f,-0.82f,
         0.47f,-0.25f,0.41f,-0.10f,0.28f,-0.01f,0.05f,0.04f,-0.18f,-0.01f,
        -0.32f,-0.10f,-0.40f,-0.25f
    }};
    private static final float[][] M = {{
        -0.49f,0f,-0.49f,-0.82f,-0.21f,-0.82f,0f,-0.48f,0.21f,-0.82f,0.49f,-0.82f,
         0.49f,0f,0.20f,0f,0.20f,-0.40f,0.07f,-0.18f,-0.07f,-0.18f,-0.20f,-0.40f,
        -0.20f,0f
    }};
    private static final float[][] P = {
        {-0.40f,0f,-0.40f,-0.82f,0.10f,-0.82f,0.30f,-0.77f,0.43f,-0.63f,0.45f,-0.48f,
          0.40f,-0.33f,0.27f,-0.22f,0.08f,-0.18f,-0.12f,-0.18f,-0.12f,0f},
        {-0.12f,-0.62f,0.06f,-0.62f,0.13f,-0.59f,0.17f,-0.51f,0.14f,-0.43f,
          0.06f,-0.39f,-0.12f,-0.39f}
    };
    private static final float[][] L = {{
        -0.42f,-0.82f,-0.12f,-0.82f,-0.12f,-0.24f,-0.06f,-0.18f,0.42f,-0.18f,
         0.42f,0f,-0.42f,0f
    }};
    private static final float[][] I = {{
        -0.22f,-0.82f,0.22f,-0.82f,0.22f,-0.64f,0.14f,-0.59f,0.14f,-0.23f,
         0.22f,-0.18f,0.22f,0f,-0.22f,0f,-0.22f,-0.18f,-0.14f,-0.23f,
        -0.14f,-0.59f,-0.22f,-0.64f
    }};
    private static final float[][] N = {{
        -0.46f,0f,-0.46f,-0.82f,-0.18f,-0.82f,0.18f,-0.35f,0.18f,-0.82f,
         0.46f,-0.82f,0.46f,0f,0.19f,0f,-0.18f,-0.46f,-0.18f,0f
    }};
    private static final float[][] G = {
        {-0.01f,-0.84f,0.22f,-0.80f,0.40f,-0.68f,0.27f,-0.48f,0.15f,-0.56f,
         -0.01f,-0.59f,-0.15f,-0.54f,-0.23f,-0.42f,-0.21f,-0.27f,-0.11f,-0.17f,
          0.04f,-0.14f,0.19f,-0.18f,0.19f,-0.28f,0.02f,-0.28f,0.02f,-0.47f,
          0.47f,-0.47f,0.47f,-0.08f,0.27f,0.01f,0.03f,0.05f,-0.22f,0.01f,
         -0.40f,-0.14f,-0.49f,-0.38f,-0.44f,-0.61f,-0.27f,-0.78f},
        {-0.02f,-0.58f,-0.14f,-0.54f,-0.21f,-0.43f,-0.18f,-0.30f,-0.08f,-0.23f,
          0.02f,-0.22f,0.02f,-0.47f,0.18f,-0.47f,0.14f,-0.54f}
    };

    private static final float[][] T = {{-.46f,-.82f,.46f,-.82f,.46f,-.58f,
        .15f,-.58f,.15f,0f,-.15f,0f,-.15f,-.58f,-.46f,-.58f}};
    private static final float[][] O = {
        {0f,-.84f,.30f,-.78f,.47f,-.57f,.49f,-.26f,.30f,-.03f,0f,.04f,
         -.30f,-.03f,-.49f,-.26f,-.47f,-.57f,-.30f,-.78f},
        {0f,-.59f,.16f,-.54f,.21f,-.40f,.16f,-.23f,0f,-.19f,
         -.16f,-.23f,-.21f,-.40f,-.16f,-.54f}
    };
    private static final float[][] W = {{-.50f,-.82f,-.23f,-.82f,-.17f,-.31f,
        -.08f,-.58f,.08f,-.58f,.17f,-.31f,.23f,-.82f,.50f,-.82f,
        .36f,0f,.11f,0f,0f,-.28f,-.11f,0f,-.36f,0f}};

    // The authored points above establish each letter's proportions. Two closed Chaikin passes
    // turn those control cages into the soft continuous contours of an inflated display face.
    private static final float[][] RD = rounded(D), RU = rounded(U), RM = rounded(M),
            RP = rounded(P), RL = rounded(L), RI = rounded(I), RN = rounded(N), RG = rounded(G), RT = rounded(T), RO = rounded(O), RW = rounded(W);

    private static float[][] glyph(char ch) {
        switch (ch) {
            case 'T': return RT; case 'O': return RO; case 'W': return RW;
            case 'D': return RD; case 'U': return RU; case 'M': return RM; case 'P': return RP;
            case 'L': return RL; case 'I': return RI; case 'N': return RN; case 'G': return RG;
            default: return RD;
        }
    }

    private static float[][] rounded(float[][] glyph) {
        float[][] out = new float[glyph.length][];
        for (int c = 0; c < glyph.length; c++) {
            float[] pts = glyph[c];
            for (int pass = 0; pass < 2; pass++) {
                int n = pts.length / 2;
                float[] soft = new float[pts.length * 2];
                for (int i = 0; i < n; i++) {
                    int j = (i + 1) % n;
                    float x = pts[i * 2], y = pts[i * 2 + 1];
                    float nx = pts[j * 2], ny = pts[j * 2 + 1];
                    soft[i * 4] = x * 0.75f + nx * 0.25f;
                    soft[i * 4 + 1] = y * 0.75f + ny * 0.25f;
                    soft[i * 4 + 2] = x * 0.25f + nx * 0.75f;
                    soft[i * 4 + 3] = y * 0.25f + ny * 0.75f;
                }
                pts = soft;
            }
            out[c] = pts;
        }
        return out;
    }

    private static float[][] place(float[][] source, float cx, float baseline, float h,
            float scaleX, float scaleY, float dx, float dy) {
        float[][] out = new float[source.length][];
        for (int c = 0; c < source.length; c++) {
            out[c] = new float[source[c].length];
            for (int i = 0; i < source[c].length; i += 2) {
                out[c][i] = cx + source[c][i] * h * scaleX + dx;
                out[c][i + 1] = baseline + source[c][i + 1] * h * scaleY + dy;
            }
        }
        return out;
    }

    static void draw(Painter p, char ch, float cx, float baseline, float h, int goo, float fade,
            float phase, float springShape) {
        float[][] g = glyph(ch);
        float breathe = 1f + 0.014f * (float) Math.sin(phase * 1.9f);
        // Preserve approximate volume: a body stretched vertically narrows, and a compressed one
        // bulges. This is the same squash/stretch illusion used by the soft slime bosses.
        float sy = breathe * springShape;
        float sx = breathe / (float) Math.sqrt(springShape);
        float[][] body = place(g, cx, baseline, h, sx, sy, 0f, 0f);

        // The slime recipe: one translucent goo silhouette, so its own layers never double-blend.
        p.fillContours(body, Glyph.withAlpha(goo, (int) (214 * fade)));

        // Surface-tension rim. Opaque at full fade for the same reason as Slime.draw(): translucent
        // polygon edges overlap at their joins and turn into a dotted seam.
        int rim = Glyph.mix(goo, 0xFFFFFFFF, 0.50f);
        for (float[] contour : body)
            p.strokePoly(contour, Glyph.withAlpha(rim, (int) (255 * fade)), h * 0.026f);

        // A pale pooled underside and a paired wet highlight, both white-only just like the bosses.
        p.fillEllipse(cx, baseline - h * 0.105f * sy, h * 0.19f * sx, h * 0.038f * sy,
                fadeBy(Glyph.withAlpha(0xFFFFFFFF, 34), fade));
        float glint = (float) Math.sin(phase * 1.7f) * h * 0.012f;
        p.fillEllipse(cx - h * 0.17f * sx + glint, baseline - h * 0.64f * sy,
                h * 0.105f * sx, h * 0.060f * sy, fadeBy(Glyph.withAlpha(0xFFFFFFFF, 76), fade));
        p.fillEllipse(cx - h * 0.205f * sx + glint, baseline - h * 0.67f * sy,
                h * 0.038f * sx, h * 0.024f * sy, fadeBy(Glyph.withAlpha(0xFFFFFFFF, 185), fade));
    }
}
