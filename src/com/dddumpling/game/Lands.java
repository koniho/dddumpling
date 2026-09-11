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
    static int background(GameCore c) {
        return c.state == GameCore.TITLE ? Draw.BG
                : Glyph.mix(c.landFromBg, BG[forStage(c.stage)], blend(c));
    }
    static int tint(GameCore c) {
        return Glyph.mix(c.landFromTint, TINT[forStage(c.stage)], blend(c));
    }
    static int cloudTint(GameCore c, int layer) {
        if (c.state == GameCore.TITLE) return Sky.CLOUD_TINT[layer];
        int target = Glyph.mix(Sky.CLOUD_TINT[layer], TINT[forStage(c.stage)], 0.36f);
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
        if (forStage(c.stage) == forStage(stage)) return;
        for (int layer = 0; layer < c.landCloudFrom.length; layer++)
            c.landCloudFrom[layer] = cloudTint(c, layer);
        c.landFromBg = background(c);
        c.landFromTint = tint(c);
        c.landFrom = forStage(c.stage);
        c.landBlend = 0f;
    }

    static void scenery(Painter p, GameCore c, Layout L) {
        if (c.state == GameCore.TITLE) return;
        p.save();
        p.clipRect(0, L.playTop, L.w, L.deckTop);
        float mix = blend(c);
        if (mix < 1f && c.landFrom >= 0) sceneryLayer(p, c, L, c.landFrom, 1f - mix);
        if (mix > 0f) sceneryLayer(p, c, L, forStage(c.stage), mix);
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
        int col = Glyph.withAlpha(Glyph.mix(Sky.CLOUD_TINT[1], TINT[land], 0.5f), a);
        int light = Glyph.withAlpha(0xFFE6FFE5, a * 2 / 3);
        if (land == 0) {
            // Glassy puddles and floating goo pearls.
            p.fillEllipse(x, y + r * 0.6f, r * 1.35f, r * 0.40f, col);
            p.fillEllipse(x - r * 0.2f, y + r * 0.51f, r * 0.8f, r * 0.1f, light);
            float bob = (float)Math.sin(t * 1.5f) * r * 0.16f;
            p.fillCircle(x, y - r * 0.15f + bob, r * 0.52f, col);
            p.fillCircle(x - r * 0.16f, y - r * 0.34f + bob, r * 0.10f, light);
        } else if (land == 1) {
            // Rounded jelly blocks with an offset dark face, like the dividing cube.
            p.fillPoly(pill(x + r * 0.13f, y + r * 0.18f, r, r * 0.9f, 6),
                    Glyph.withAlpha(0xFF624A91, a));
            p.fillPoly(pill(x, y, r, r * 0.9f, 6), col);
            p.line(x - r * 0.58f, y - r * 0.60f, x + r * 0.48f, y - r * 0.60f,
                    light, r * 0.08f);
            p.fillPoly(pill(x + r * 1.05f, y + r * 0.7f, r * 0.34f, r * 0.3f, 6), col);
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
            p.strokeCircle(x + r * 0.8f, y - r, r * 0.27f, light, r * 0.04f);
            p.strokeCircle(x + r * 0.4f, y - r * 1.6f, r * 0.13f, light, r * 0.035f);
        } else {
            p.fillPoly(pill(x, y + r * 0.48f, r * 0.24f, r * 0.8f, 8), light);
            p.fillEllipse(x, y, r * 1.12f, r * 0.65f, col);
            p.fillEllipse(x, y + r * 0.21f, r, r * 0.15f,
                    Glyph.withAlpha(0xFF775968, a));
            for (int k = 0; k < 3; k++)
                p.fillCircle(x + (k - 1) * r * 0.51f, y - r * (k == 1 ? 0.31f : 0.12f),
                        r * 0.12f, light);
        }
    }

    static void skit(Painter p, int stage, float x, float y, float r, float t, int a) {
        int land = forStage(stage), variant = skitFor(stage);
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
