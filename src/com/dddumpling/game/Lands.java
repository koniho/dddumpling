package com.dddumpling.game;

/** Five-stage scenery chapters. Cosmetic clocks never consume gameplay randomness. */
final class Lands extends Draw {
    static final int COUNT = 4;
    static final float FADE_TIME = 2.4f;
    static final int[] BG = {
        Glyph.mix(Draw.BG, 0xFF142A29, 0.5f), Glyph.mix(Draw.BG, 0xFF21182F, 0.5f),
        Glyph.mix(Draw.BG, 0xFF102735, 0.5f), Glyph.mix(Draw.BG, 0xFF241F2A, 0.5f)
    };
    static final int[] TINT = {0xFF79C99D, 0xFFB18CDB, 0xFF62BEC5, 0xFFE4A19C};
    private Lands() {}

    // Beyond the authored bosses, scenery repeats without adding encounters.
    static int forStage(int stage) { return (Math.max(1, stage) - 1) / Boss.EVERY % COUNT; }
    static int skitFor(int stage) { return (Math.max(1, stage) - 1) % Boss.EVERY % 3; }
    static float blend(GameCore c) {
        float t = c.landBlend;
        return t * t * (3f - 2f * t);
    }
    static int visualStage(GameCore c) { return c.state == GameCore.TITLE ? c.landChoice * Boss.EVERY + 1 : c.stage; }
    static int background(GameCore c) {
        return c.state == GameCore.TITLE ? Draw.BG
                : Glyph.mix(c.landFromBg, BG[forStage(visualStage(c))], blend(c));
    }
    static int tint(GameCore c) {
        return Glyph.mix(c.landFromTint, TINT[forStage(visualStage(c))], blend(c));
    }
    static int cloudTint(GameCore c, int layer) {
        if (c.state == GameCore.TITLE) return Sky.CLOUD_TINT[layer];
        int target = Glyph.mix(Sky.CLOUD_TINT[layer], TINT[forStage(visualStage(c))], 0.36f);
        return Glyph.mix(c.landCloudFrom[layer], target, blend(c));
    }
    static void fromIntro(GameCore c) {
        c.landFrom = -1; // The intro has clouds, but no land props to fade out.
        c.landFromBg = Draw.BG;
        c.landFromTint = TINT[0];
        System.arraycopy(Sky.CLOUD_TINT, 0, c.landCloudFrom, 0, Sky.CLOUD_TINT.length);
        c.landBlend = 0f;
    }
    static void transition(GameCore c, int stage) {
        if (forStage(visualStage(c)) == forStage(stage)) return;
        for (int layer = 0; layer < c.landCloudFrom.length; layer++)
            c.landCloudFrom[layer] = cloudTint(c, layer);
        c.landFromBg = background(c);
        c.landFromTint = tint(c);
        c.landFrom = forStage(visualStage(c));
        c.landBlend = 0f;
    }

    static void scenery(Painter p, GameCore c, Layout L) {
        if (c.state == GameCore.TITLE) return;
        p.save();
        p.clipRect(0, L.playTop, L.w, L.deckTop);
        float mix = blend(c);
        if (mix < 1f && c.landFrom >= 0) sceneryLayer(p, c, L, c.landFrom, 1f - mix);
        if (mix > 0f) sceneryLayer(p, c, L, forStage(visualStage(c)), mix);
        p.restore();
    }

    private static void sceneryLayer(Painter p, GameCore c, Layout L, int land, float fade) {
        for (int layer = 0; layer < 2; layer++) {
            for (int i = 0; i < 8; i++) {
                // Stable jitter breaks the rows without changing positions every frame or drawing RNG.
                int seed = 431 + land * 719 + layer * 263 + i * 47;
                float r = L.w * (0.065f + layer * 0.03f) * (0.65f + hash(seed) * 0.65f);
                float offset = (i + hash(seed + 13) * 0.88f) / 8f + layer * 0.037f;
                float phase = (offset + c.skyClock * (0.013f + layer * 0.009f)) % 1f;
                float y = -r * 3f + phase * (L.deckTop + r * 6f);
                float inset = 0.015f + hash(seed + 29) * 0.145f;
                float x = L.w * (i % 2 == 0 ? inset : 1f - inset);
                x += (float)Math.sin(i * 2.7f + c.skyClock * 0.22f) * r * 0.18f;
                softProp(p, land, x, y, r, (int)((layer == 0 ? 48 : 78) * fade), c.skyClock + i);
                // Small motes give the clear centre a little depth without resembling letters.
                float mx = L.w * (0.22f + hash(i * 37 + land * 19) * 0.56f);
                p.fillCircle(mx, y, L.unit * 0.04f,
                        Glyph.withAlpha(TINT[land], (int)(42 * fade)));
            }
        }
    }

    /** Faint expanded silhouettes soften the edge while a low-opacity core preserves the shape. */
    private static void softProp(Painter p, int land, float x, float y, float r, int a, float t) {
        if (a < 3) return;
        prop(p, land, x, y, r * 1.16f, a / 10, t);
        prop(p, land, x, y, r * 1.09f, a / 6, t);
        prop(p, land, x, y, r * 1.04f, a / 4, t);
        prop(p, land, x, y, r, a / 2, t);
    }

    /** Shared scenery props also give each land's little stage skits a familiar toy. */
    static void prop(Painter p, int land, float x, float y, float r, int a, float t) {
        prop(p, land, x, y, r, a, t, false);
    }

    static void prop(Painter p, int land, float x, float y, float r, int a, float t, boolean silhouette) {
        prop(p, land, x, y, r, a, t, silhouette, false);
    }

    static void logo(Painter p, int land, float x, float y, float r, int a, float t, boolean silhouette) {
        prop(p, land, x, y, r, a, t, silhouette, true);
    }

    static void logo(Painter p, int land, float x, float y, float r, int a, float t) {
        logo(p, land, x, y, r, a, t, false);
    }

    private static void prop(Painter p, int land, float x, float y, float r, int a, float t,
            boolean silhouette, boolean animated) {
        int col = Glyph.withAlpha(silhouette ? 0xFF9A8FAF : Glyph.mix(Sky.CLOUD_TINT[1], TINT[land], 0.5f), a);
        int light = silhouette ? 0 : Glyph.withAlpha(0xFFE6FFE5, a * 2 / 3);
        if (land == 0) {
            // Planted, overlapping mounds read as a landscape even at picker size.
            slimeHill(p, x + r * 0.12f, y + r * 0.53f, r * 0.82f, r * 0.765f,
                    a, t, 0, silhouette, animated);
            slimeHill(p, x - r * 0.60f, y + r * 0.66f, r * 0.76f, r * 0.54f,
                    a, t, 1, silhouette, animated);
            slimeHill(p, x + r * 0.55f, y + r * 0.76f, r * 0.85f, r * 0.45f,
                    a, t, 2, silhouette, animated);
        } else if (land == 1) {
            // Uneven, softly faceted towers share the dividing cube's translucent purple.
            crystal(p, x - r * 0.62f, y + r * 0.78f, r * 0.42f, r * 1.46f, a, t + 1.7f, silhouette, animated);
            crystal(p, x + r * 0.60f, y + r * 0.85f, r * 0.43f, r * 1.78f, a, t + 3.2f, silhouette, animated);
            crystal(p, x - r * 0.05f, y + r * 0.94f, r * 0.48f, r * 2.32f, a, t, silhouette, animated);
        } else if (land == 2) {
            for (int stem = 0; stem < 3; stem++) {
                float[] kelp = new float[18];
                for (int n = 0; n < 9; n++) {
                    float u = n / 8f;
                    kelp[n*2] = x + (stem - 1) * r * 0.43f
                            + (float)Math.sin(t * 1.2f + u * 4f + stem) * r * 0.3f * u;
                    kelp[n*2+1] = y + r - u * r * (1.8f + stem * 0.3f);
                }
                // One filled ribbon avoids translucent stroke joints showing as bright dots.
                float[] ribbon = new float[36];
                for (int n = 0; n < 9; n++) {
                    float width = r * (0.08f - n * 0.006f);
                    ribbon[n*2] = kelp[n*2] - width;
                    ribbon[n*2+1] = kelp[n*2+1];
                    ribbon[(17-n)*2] = kelp[n*2] + width;
                    ribbon[(17-n)*2+1] = kelp[n*2+1];
                }
                p.fillPoly(ribbon, col);
            }
            if (animated && !silhouette) {
                for (int bubble = 0; bubble < 3; bubble++) {
                    float rise = (t * 0.30f + bubble * 0.34f) % 1f;
                    float bx = x + r * (0.55f + 0.18f * (float)Math.sin(t + bubble * 2f));
                    float by = y + r * (0.8f - rise * 2.5f);
                    int alpha = (int)(a * 0.65f * Math.sin(rise * Math.PI));
                    p.strokeCircle(bx, by, r * (0.10f + bubble * 0.055f),
                            Glyph.withAlpha(0xFFE6FFE5, alpha), r * 0.035f);
                }
            } else {
                p.strokeCircle(x + r * 0.8f, y - r, r * 0.27f, light, r * 0.04f);
                p.strokeCircle(x + r * 0.4f, y - r * 1.6f, r * 0.13f, light, r * 0.035f);
            }
        } else {
            if (animated) {
                float breath = (float)Math.sin(t * 2.4f);
                r *= 1f + breath * 0.055f;
                if (!silhouette) for (int puff = 0; puff < 5; puff++) {
                    float age = (t * 0.38f + puff * 0.055f) % 1f;
                    float px = x + (puff - 2) * r * (0.28f + age * 0.3f);
                    float py = y + r * (0.30f + age * 0.9f);
                    int alpha = (int)(a * 0.55f * Math.sin(age * Math.PI));
                    p.fillCircle(px, py, r * (0.035f + age * 0.05f),
                            Glyph.withAlpha(0xFFFFE3C6, alpha));
                }
            }
            p.fillPoly(pill(x, y + r * 0.48f, r * 0.24f, r * 0.8f, 8), silhouette ? col : light);
            p.fillEllipse(x, y, r * 1.12f, r * 0.65f, col);
            p.fillEllipse(x, y + r * 0.21f, r, r * 0.15f,
                    (silhouette ? col : Glyph.withAlpha(0xFF775968, a)));
            for (int k = 0; k < 3; k++)
                p.fillCircle(x + (k - 1) * r * 0.51f, y - r * (k == 1 ? 0.31f : 0.12f),
                        r * 0.12f, light);
        }
    }

    private static void slimeHill(Painter p, float x, float base, float w, float h,
            int a, float t, int layer, boolean silhouette, boolean animated) {
        float wobble = (float)Math.sin(t * 1.6f + layer * 1.8f) * (animated ? 0.045f : 0.018f);
        w *= 1f + wobble;
        h *= 1f - wobble;
        int green = Glyph.mix(Sky.CLOUD_TINT[1], 0xFF83C779, animated ? 0.80f : 0.50f);
        green = Glyph.mix(green, 0xFFB1E49B, layer * 0.10f);
        int edge = Glyph.mix(green, 0xFF345B48, 0.40f);
        p.fillPoly(hillShape(x, base, w, h),
                Glyph.withAlpha(silhouette ? 0xFF9A8FAF : edge, a));
        if (silhouette) return;
        p.fillPoly(hillShape(x, base - h * 0.10f, w * 0.97f, h * 0.89f),
                Glyph.withAlpha(green, a));
        p.fillPoly(hillShape(x - w * 0.10f, base - h * 0.23f, w * 0.65f, h * 0.70f),
                Glyph.withAlpha(0xFFD3EFBD, a / 5));
    }

    /** A broad dome with a shallow curved foot, rather than a floating oval. */
    private static float[] hillShape(float x, float base, float w, float h) {
        float[] shape = new float[50];
        for (int i = 0; i <= 20; i++) {
            float angle = (float)Math.PI * i / 20f;
            shape[i * 2] = x - (float)Math.cos(angle) * w;
            shape[i * 2 + 1] = base - (float)Math.sin(angle) * h;
        }
        shape[42] = x + w * 0.8f; shape[43] = base + h * 0.08f;
        shape[44] = x + w * 0.3f; shape[45] = base + h * 0.11f;
        shape[46] = x - w * 0.3f; shape[47] = base + h * 0.11f;
        shape[48] = x - w * 0.8f; shape[49] = base + h * 0.08f;
        return shape;
    }

    private static void crystal(Painter p, float x, float base, float w, float h,
            int a, float t, boolean silhouette, boolean animated) {
        float top = base - h, shoulder = top + h * 0.22f;
        float[] outline = {x - w, base - h * 0.07f, x - w, shoulder,
                x - w * 0.18f, top, x + w * 0.80f, shoulder - h * 0.025f,
                x + w, base - h * 0.10f, x + w * 0.30f, base, x - w * 0.65f, base};
        int tint = Glyph.mix(Sky.CLOUD_TINT[1], TINT[1], 0.5f);
        p.fillPoly(roundedCrystal(outline), Glyph.withAlpha(silhouette ? 0xFF9A8FAF : tint, a));
        if (silhouette) return;
        p.fillPoly(roundedCrystal(new float[]{x + w * 0.12f, shoulder + h * 0.08f,
                x + w * 0.80f, shoulder - h * 0.025f, x + w, base - h * 0.10f,
                x + w * 0.30f, base, x + w * 0.12f, base - h * 0.04f}),
                Glyph.withAlpha(Glyph.mix(tint, 0xFF463257, 0.48f), a * 3 / 4));
        p.fillPoly(roundedCrystal(new float[]{x - w, shoulder, x - w * 0.18f, top,
                x + w * 0.80f, shoulder - h * 0.025f, x + w * 0.12f, shoulder + h * 0.08f}),
                Glyph.withAlpha(0xFFD6C3EB, a / 2));
        if (animated) {
            float sweep = (t * 0.35f) % 1f;
            float sy = base - h * (0.18f + sweep * 0.53f);
            int alpha = (int)(a * 0.7f * Math.sin(sweep * Math.PI));
            p.fillPoly(new float[]{x-w*.76f, sy, x+w*.05f, sy-h*.10f,
                    x+w*.05f, sy-h*.16f, x-w*.76f, sy-h*.06f},
                    Glyph.withAlpha(0xFFF3E7FF, alpha));
        }
        float shimmer = 0.5f + 0.5f * (float)Math.sin(t * 1.25f);
        p.fillPoly(pill(x - w * 0.29f, base - h * 0.47f, w * 0.22f, h * 0.24f, 6),
                Glyph.withAlpha(0xFFE9DDF6, (int)(a * (0.15f + shimmer * 0.16f))));
        p.fillPoly(roundedCrystal(new float[]{x - w * 0.92f, base - h * 0.24f,
                x + w * 0.86f, base - h * 0.19f, x + w, base - h * 0.10f,
                x + w * 0.30f, base, x - w * 0.65f, base}),
                Glyph.withAlpha(0xFF5B456F, a / 3));
    }

    /** Small rounded corners retain the crystal facets without sharp needle tips. */
    private static float[] roundedCrystal(float[] points) {
        int n = points.length / 2;
        float[] round = new float[n * 10];
        for (int i = 0; i < n; i++) {
            int prev = (i + n - 1) % n, next = (i + 1) % n;
            float x = points[i * 2], y = points[i * 2 + 1];
            float ax = x + (points[prev * 2] - x) * 0.12f;
            float ay = y + (points[prev * 2 + 1] - y) * 0.12f;
            float bx = x + (points[next * 2] - x) * 0.12f;
            float by = y + (points[next * 2 + 1] - y) * 0.12f;
            for (int step = 0; step < 5; step++) {
                float u = step / 4f, v = 1f - u;
                round[i * 10 + step * 2] = v * v * ax + 2f * v * u * x + u * u * bx;
                round[i * 10 + step * 2 + 1] = v * v * ay + 2f * v * u * y + u * u * by;
            }
        }
        return round;
    }

    static void skit(Painter p, int stage, float x, float y, float r, float t, int a) {
        int land = forStage(stage), variant = skitFor(stage);
        if (land == 2 && variant == 1) { SeaSkits.hide(p, x, y, r, t, a); return; }
        if (land == 2 && variant == 2) { SeaSkits.trudge(p, x, y, r, t, a); return; }
        float arc = (float)Math.sin(t * Math.PI);
        float hop = Math.abs((float)Math.sin(t * Math.PI * 2f));
        int actor = land == 0 ? Kawaii.BLOB : land == 1 ? Kawaii.SQUISHY
                : land == 2 ? Kawaii.CAT : Kawaii.DUMPLING;
        if (variant == 0) {
            // Puddle bounce, block leap, bubble ride, or mushroom trampoline.
            prop(p, land, x, y + r * 0.55f, r * 0.68f, a * 3 / 4, t * 5f);
            Skits.face(p, actor, x, y - r * (0.3f + hop * 1.15f), r * 0.53f, a,
                    1.15f - hop * 0.3f, 1f);
        } else if (variant == 1) {
            // A pair play catch with a goo pearl, jelly block, bubble garden, or tiny cap.
            Skits.face(p, actor, x - r * 1.25f, y + r * 0.3f, r * 0.5f, a, 1f, 1f);
            Skits.face(p, Kawaii.STRAWBERRY, x + r * 1.25f, y + r * 0.3f, r * 0.5f, a, 1f, 1f);
            prop(p, land, x + (t * 2f - 1f) * r * 1.2f, y - arc * r * 1.1f,
                    r * 0.32f, a, t * 5f);
        } else {
            // Peek out from a giant prop, then pop up together.
            Skits.face(p, actor, x - r * (0.3f + arc), y - arc * r * 0.65f,
                    r * 0.5f, a, 1f, arc);
            Skits.face(p, Kawaii.DUMPLING, x + r * (0.3f + arc), y - arc * r * 0.65f,
                    r * 0.5f, a, 1f, arc);
            prop(p, land, x, y + r * 0.25f, r * 0.85f, a, t * 5f);
        }
    }
}
