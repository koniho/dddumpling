package com.dddumpling.game;

/** Little sea-grass performances, with foliage in front of the cast for actual occlusion. */
final class SeaSkits extends Draw {
    private SeaSkits() {}

    static void hide(Painter p, float x, float y, float r, float t, int a) {
        float arrive = ease(Math.min(1f, t / 0.32f));
        float peek = t < 0.60f ? 0f : (float)Math.sin(Math.min(1f, (t - 0.60f) / 0.40f) * Math.PI);
        float cx = x - r * 1.8f * (1f - arrive) + peek * r * 0.95f;
        float cy = y + r * 0.22f - Math.abs((float)Math.sin(t * Math.PI * 5f)) * r * 0.06f;
        grass(p, x, y + r * 0.85f, r * 0.78f, a / 3, t * 4f, 0f);
        Skits.face(p, Kawaii.CAT, cx, cy, r * 0.43f, a, 1.04f - peek * 0.08f, peek);
        grass(p, x, y + r * 0.85f, r * 0.78f, a, t * 4f, peek * 0.2f);
    }

    static void trudge(Painter p, float x, float y, float r, float t, int a) {
        float walk = t - (float)Math.sin(t * Math.PI * 6f) * 0.65f / (float)(Math.PI * 6f);
        float leadX = x + (walk * 3.4f - 1.25f) * r;
        for (int patch = 0; patch < 3; patch++)
            grass(p, x + (patch - 1) * r * 1.15f, y + r * 0.6f,
                    r * 0.58f, a / 3, t * 5f + patch, 0f);
        for (int who = 1; who >= 0; who--) {
            float stride = t * (float)Math.PI * 6f - who * 1.1f;
            float lift = Math.abs((float)Math.sin(stride));
            float cx = leadX - who * r * 0.9f;
            float cy = y + r * 0.12f - lift * r * 0.12f;
            int ink = Glyph.withAlpha(0xFF534357, a);
            for (int foot = -1; foot <= 1; foot += 2)
                p.fillEllipse(cx + foot * r * 0.18f + (float)Math.cos(stride) * foot * r * 0.08f,
                        y + r * 0.48f, r * 0.12f, r * 0.06f, ink);
            Skits.face(p, who == 0 ? Kawaii.CAT : Kawaii.DUMPLING, cx, cy,
                    r * 0.40f, a, 1.08f - lift * 0.14f, t > 0.85f ? 1f : 0.15f);
        }
        for (int patch = 0; patch < 3; patch++) {
            float px = x + (patch - 1) * r * 1.15f;
            float bend = Math.max(-0.35f, Math.min(0.35f, (leadX - px) / r * 0.3f));
            grass(p, px, y + r * 0.75f, r * 0.47f, a * 4 / 5, t * 5f + patch, bend);
        }
    }

    private static float ease(float t) { return t * t * (3f - 2f * t); }

    private static void grass(Painter p, float x, float ground, float r, int a, float t, float bend) {
        for (int blade = 0; blade < 7; blade++) {
            float[] shape = new float[36];
            float height = r * (1.5f + 0.45f * hash(31 + blade * 73));
            for (int n = 0; n < 9; n++) {
                float u = n / 8f;
                float cx = x + (blade - 3) * r * 0.20f
                        + ((float)Math.sin(t + blade * 0.72f + u * 2f) * 0.15f + bend) * r * u * u;
                float width = r * (0.17f * (1f - u) + 0.015f);
                shape[n * 2] = cx - width;
                shape[n * 2 + 1] = ground - height * u;
                shape[(17 - n) * 2] = cx + width;
                shape[(17 - n) * 2 + 1] = ground - height * u;
            }
            int tint = Glyph.mix(0xFF3E8C89, 0xFF8AD4B8, blade / 9f);
            p.fillPoly(shape, Glyph.withAlpha(tint, a));
        }
    }
}
