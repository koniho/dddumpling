package com.dddumpling.game;

/**
 * The title's tiny display face. Shared by the title, town balloons and squishy names; each glyph is a
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

    // Extra letters for squishy names, using the same rounded, inflated silhouettes.
    private static final float[][] A = rounded(new float[][] {
        {-.49f,0f,-.20f,-.82f,.20f,-.82f,.49f,0f,.19f,0f,.13f,-.18f,-.13f,-.18f,-.19f,0f},
        {-.09f,-.37f,0f,-.64f,.09f,-.37f}
    });
    private static final float[][] B = rounded(new float[][] {
        {-.42f,0f,-.42f,-.82f,.10f,-.82f,.36f,-.73f,.42f,-.57f,.30f,-.42f,
         .45f,-.28f,.43f,-.13f,.29f,-.02f,.08f,0f},
        {-.14f,-.64f,.06f,-.64f,.15f,-.58f,.12f,-.51f,-.14f,-.49f},
        {-.14f,-.33f,.09f,-.33f,.17f,-.24f,.09f,-.17f,-.14f,-.17f}
    });
    private static final float[][] C = rounded(new float[][] {{
        .43f,-.70f,.25f,-.82f,-.08f,-.84f,-.36f,-.66f,-.49f,-.40f,-.38f,-.13f,
        -.12f,.03f,.20f,.03f,.43f,-.10f,.29f,-.31f,.12f,-.21f,-.04f,-.23f,
        -.19f,-.39f,-.08f,-.57f,.12f,-.59f,.28f,-.49f
    }});
    private static final float[][] E = rounded(new float[][] {{
        -.42f,0f,-.42f,-.82f,.40f,-.82f,.40f,-.60f,-.13f,-.60f,-.13f,-.51f,
        .28f,-.51f,.28f,-.31f,-.13f,-.31f,-.13f,-.22f,.42f,-.22f,.42f,0f
    }});
    private static final float[][] F = rounded(new float[][] {{
        -.42f,0f,-.42f,-.82f,.40f,-.82f,.40f,-.60f,-.12f,-.60f,-.12f,-.49f,
        .28f,-.49f,.28f,-.27f,-.12f,-.27f,-.12f,0f
    }});
    private static final float[][] H = rounded(new float[][] {{
        -.45f,0f,-.45f,-.82f,-.15f,-.82f,-.15f,-.53f,.15f,-.53f,.15f,-.82f,
        .45f,-.82f,.45f,0f,.15f,0f,.15f,-.29f,-.15f,-.29f,-.15f,0f
    }});
    private static final float[][] J = rounded(new float[][] {{
        -.10f,-.82f,.43f,-.82f,.43f,-.24f,.31f,-.04f,.09f,.04f,-.20f,0f,
        -.42f,-.17f,-.42f,-.36f,-.15f,-.36f,-.12f,-.23f,.04f,-.20f,.13f,-.30f,
        .13f,-.60f,-.10f,-.60f
    }});
    private static final float[][] K = rounded(new float[][] {{
        -.43f,0f,-.43f,-.82f,-.13f,-.82f,-.13f,-.50f,.14f,-.82f,.47f,-.82f,
        .12f,-.41f,.49f,0f,.13f,0f,-.13f,-.30f,-.13f,0f
    }});
    private static final float[][] Q = rounded(new float[][] {
        {0f,-.84f,.31f,-.77f,.47f,-.53f,.45f,-.22f,.29f,-.04f,.43f,.10f,
         .18f,.13f,.05f,.01f,-.23f,-.02f,-.44f,-.23f,-.47f,-.53f,-.29f,-.77f},
        {-.02f,-.59f,.16f,-.52f,.19f,-.33f,.03f,-.19f,-.15f,-.25f,-.21f,-.42f,-.15f,-.54f}
    });
    private static final float[][] R = rounded(new float[][] {
        {-.42f,0f,-.42f,-.82f,.12f,-.82f,.35f,-.72f,.43f,-.53f,.34f,-.33f,
         .19f,-.27f,.46f,0f,.09f,0f,-.13f,-.25f,-.13f,0f},
        {-.13f,-.61f,.05f,-.61f,.15f,-.53f,.10f,-.43f,-.13f,-.42f}
    });
    private static final float[][] S = rounded(new float[][] {{
        .40f,-.73f,.16f,-.84f,-.16f,-.82f,-.40f,-.66f,-.40f,-.46f,-.18f,-.33f,
        .15f,-.27f,.15f,-.18f,-.07f,-.17f,-.33f,-.28f,-.46f,-.09f,-.17f,.03f,
        .16f,.02f,.41f,-.13f,.43f,-.34f,.20f,-.48f,-.13f,-.55f,-.13f,-.63f,
        .06f,-.64f,.27f,-.54f
    }});
    private static final float[][] V = rounded(new float[][] {{
        -.49f,-.82f,-.17f,-.82f,0f,-.27f,.17f,-.82f,.49f,-.82f,.18f,0f,-.18f,0f
    }});
    private static final float[][] X = rounded(new float[][] {{
        -.47f,-.82f,-.13f,-.82f,0f,-.58f,.13f,-.82f,.47f,-.82f,.18f,-.41f,
        .48f,0f,.13f,0f,0f,-.25f,-.13f,0f,-.48f,0f,-.18f,-.41f
    }});
    private static final float[][] Y = rounded(new float[][] {{
        -.49f,-.82f,-.16f,-.82f,0f,-.50f,.16f,-.82f,.49f,-.82f,.15f,-.29f,
        .15f,0f,-.15f,0f,-.15f,-.29f
    }});

    // The authored points above establish each letter's proportions. Two closed Chaikin passes
    // turn those control cages into the soft continuous contours of an inflated display face.
    private static final float[][] RD = rounded(D), RU = rounded(U), RM = rounded(M),
            RP = rounded(P), RL = rounded(L), RI = rounded(I), RN = rounded(N), RG = rounded(G), RT = rounded(T), RO = rounded(O), RW = rounded(W);

    private static float[][] glyph(char ch) {
        switch (ch) {
            case 'T': return RT; case 'O': return RO; case 'W': return RW;
            case 'D': return RD; case 'U': return RU; case 'M': return RM; case 'P': return RP;
            case 'L': return RL; case 'I': return RI; case 'N': return RN; case 'G': return RG;
            case 'A': return A; case 'B': return B; case 'C': return C; case 'E': return E;
            case 'F': return F; case 'H': return H; case 'J': return J; case 'K': return K;
            case 'Q': return Q; case 'R': return R; case 'S': return S; case 'V': return V;
            case 'X': return X; case 'Y': return Y;
            default: return null;
        }
    }

    static boolean supports(char ch) { return ch==' ' || glyph(ch)!=null; }

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
        if(g==null) return;
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
