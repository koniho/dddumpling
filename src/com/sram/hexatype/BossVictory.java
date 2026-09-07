package com.sram.hexatype;

/**
 * Boss celebrations during the player's death transition. Gameplay receives a clean Boss when the
 * run ends, while this renderer draws the retained combat object as a visual-only snapshot.
 */
final class BossVictory extends Draw {
    private BossVictory() {}

    static void draw(Painter p, GameCore c, Layout L) {
        int kind = c.bossVictoryKind;
        if (kind < 0 || c.bossVictory == null || !c.dying()) return;
        float at = c.deathProgress();
        float fade = Math.min(1f, at * 7f) * Math.min(1f, (1f - at) * 6f);
        Boss b = c.bossVictory;
        float r = Boss.bodyR(L);
        float cx = b.body.centreX();
        float cy = b.body.centreY();
        float beat = c.clock * 7f;

        confetti(p, cx, cy, r, beat, fade, kind);
        p.save();
        p.translate(tauntX(kind, beat, r), tauntY(kind, beat, r));
        BossScreen.body(p, c, L, b, fade);
        p.restore();
    }

    /** Side-to-side swagger; each silhouette gets a slightly different taunting rhythm. */
    static float tauntX(int kind, float beat, float r) {
        float rate = kind == Boss.MAGPIE ? 1.30f : kind == Boss.OCTOPUS ? 0.82f
                : kind == Boss.SUMO ? 0.66f : 0.94f;
        float reach = kind == Boss.OCTOPUS ? 0.24f : kind == Boss.MAGPIE ? 0.20f
                : kind == Boss.SUMO ? 0.15f : 0.10f;
        return (float) Math.sin(beat * rate + kind * 0.73f) * r * reach;
    }

    /** A cocky hop/stomp layered with the swagger, without resizing the retained combat body. */
    static float tauntY(int kind, float beat, float r) {
        float rate = kind == Boss.DRUM ? 1.45f : kind == Boss.SLIME ? 0.72f : 1.05f;
        float lift = kind == Boss.SLIME ? 0.24f : kind == Boss.DRUM ? 0.16f : 0.11f;
        return -Math.abs((float) Math.sin(beat * rate + kind * 0.41f)) * r * lift;
    }

    private static void slime(Painter p, float x, float y, float r, float t, float fade) {
        float hop = Math.abs((float) Math.sin(t * 0.72f));
        y -= hop * r * 0.42f;
        float squash = 1f + 0.18f * (1f - hop);
        int col = fadeBy(Glyph.COLOR[Kawaii.SQUISHY], fade);
        Kawaii.draw(p, Kawaii.SQUISHY, x, y, r * 0.78f, col, squash, 1f);
        for (int i = 0; i < 7; i++) {
            float a = t * 0.22f + i * Softbody.TAU / 7f;
            float rise = (t * 0.10f + i * 0.17f) % 1f;
            p.fillCircle(x + (float) Math.cos(a) * r * (0.75f + rise * 0.35f),
                    y + r * 0.65f - rise * r * 1.8f, r * (0.08f + i % 3 * 0.018f),
                    Glyph.withAlpha(Glyph.COLOR[Kawaii.SQUISHY], (int) (150 * fade)));
        }
    }

    private static void triplets(Painter p, float x, float y, float r, float t, float fade) {
        for (int i = 0; i < 3; i++) {
            float phase = t * 0.32f + i * Softbody.TAU / 3f;
            float xx = x + (i - 1) * r * 0.72f + (float) Math.sin(phase) * r * 0.16f;
            float yy = y + (float) Math.cos(phase * 1.35f) * r * 0.28f;
            Kawaii.draw(p, Kawaii.GRAPES, xx, yy, r * 0.47f,
                    fadeBy(Glyph.COLOR[Kawaii.GRAPES], fade), 1f, 1f);
        }
        float clap = 0.5f + 0.5f * (float) Math.sin(t * 1.25f);
        p.strokeCircle(x, y - r * 0.15f, r * (0.18f + clap * 0.22f),
                Glyph.withAlpha(GOLD, (int) (210 * (1f - clap) * fade)), r * 0.07f);
    }

    private static void drum(Painter p, float x, float y, float r, float t, float fade) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(t * 1.7f);
        Kawaii.draw(p, Kawaii.DUMPLING, x, y, r * (0.70f + pulse * 0.07f),
                fadeBy(Glyph.COLOR[Kawaii.DUMPLING], fade), 1.12f - pulse * 0.20f, 1f);
        for (int i = 0; i < 3; i++)
            p.strokeCircle(x, y, r * (0.78f + i * 0.20f + pulse * 0.10f),
                    Glyph.withAlpha(i == 1 ? ROSE : GOLD, (int) ((155 - i * 32) * fade)),
                    r * 0.045f);
        float swing = (float) Math.sin(t * 1.7f) * r * 0.28f;
        p.line(x - r * 0.82f, y - r * 0.95f, x - r * 0.24f, y - r * 0.12f + swing,
                Glyph.withAlpha(BAMBOO, (int) (255 * fade)), r * 0.09f);
        p.line(x + r * 0.82f, y - r * 0.95f, x + r * 0.24f, y - r * 0.12f - swing,
                Glyph.withAlpha(BAMBOO, (int) (255 * fade)), r * 0.09f);
    }

    private static void magpie(Painter p, float x, float y, float r, float t, float fade) {
        float flap = (float) Math.sin(t * 1.55f);
        int col = fadeBy(Glyph.COLOR[Kawaii.CAT], fade);
        Kawaii.draw(p, Kawaii.CAT, x, y, r * 0.68f, col, 1f, 1f);
        for (int side = -1; side <= 1; side += 2) {
            float wy = y - r * (0.12f + 0.24f * flap);
            p.fillPoly(new float[] {x + side * r * 0.36f, y,
                    x + side * r * 1.05f, wy - r * 0.30f,
                    x + side * r * 0.82f, wy + r * 0.38f},
                    Glyph.withAlpha(Glyph.mix(col, 0xFFFFFFFF, 0.22f), (int) (190 * fade)));
            float toss = (t * 0.12f + (side > 0 ? 0.5f : 0f)) % 1f;
            float kx = x + side * r * (0.45f + toss * 0.75f);
            float ky = y - r * (0.35f + (float) Math.sin(toss * Math.PI) * 1.05f);
            p.fillPoly(Glyph.hex(kx, ky, r * 0.15f), Glyph.withAlpha(GOLD, (int) (230 * fade)));
        }
    }

    private static void sumo(Painter p, float x, float y, float r, float t, float fade) {
        int step = (int) (t * 0.65f) & 1;
        float stomp = Math.abs((float) Math.sin(t * 1.02f));
        float lean = (step == 0 ? -1f : 1f) * r * 0.16f;
        Kawaii.draw(p, Kawaii.BLOB, x + lean, y - stomp * r * 0.18f, r * 0.82f,
                fadeBy(Glyph.COLOR[Kawaii.BLOB], fade), 1.24f - stomp * 0.17f, 1f);
        for (int side = -1; side <= 1; side += 2) {
            float dust = step == (side > 0 ? 1 : 0) ? 1f - stomp : 0.25f;
            p.fillCircle(x + side * r * 0.72f, y + r * 0.70f,
                    r * (0.10f + dust * 0.18f),
                    Glyph.withAlpha(BAMBOO, (int) (150 * dust * fade)));
        }
    }

    private static void divide(Painter p, float x, float y, float r, float t, float fade) {
        for (int i = 0; i < 8; i++) {
            float a = t * 0.30f + i * Softbody.TAU / 8f;
            float orbit = r * (0.62f + 0.10f * (float) Math.sin(t + i));
            float xx = x + (float) Math.cos(a) * orbit;
            float yy = y + (float) Math.sin(a) * orbit * 0.62f;
            float rr = r * (0.19f + 0.035f * (float) Math.sin(t * 1.4f + i));
            p.fillCircle(xx, yy, rr, Glyph.withAlpha(0xFF6D259D, (int) (220 * fade)));
            p.fillCircle(xx - rr * 0.25f, yy - rr * 0.25f, rr * 0.20f,
                    Glyph.withAlpha(0xFFFFFFFF, (int) (155 * fade)));
        }
        p.strokeCircle(x, y, r * (0.28f + 0.05f * (float) Math.sin(t)),
                Glyph.withAlpha(YELLOW, (int) (225 * fade)), r * 0.07f);
    }

    private static void octopus(Painter p, float x, float y, float r, float t, float fade) {
        int col = 0xFF861735;
        for (int arm = 0; arm < 8; arm++) {
            float side = arm < 4 ? -1f : 1f;
            float spread = (arm % 4 + 1) / 4f;
            float[] pts = new float[12];
            for (int n = 0; n < 6; n++) {
                float u = n / 5f;
                pts[n * 2] = x + side * r * (0.25f + spread * u)
                        + (float) Math.sin(t * 0.55f + arm + u * 3f) * r * 0.16f * u;
                pts[n * 2 + 1] = y + r * (0.20f + u * (0.45f + spread * 0.48f))
                        - (arm == 0 || arm == 7 ? (float) Math.sin(u * Math.PI) * r * 0.85f : 0f);
            }
            p.polyline(pts, Glyph.withAlpha(col, (int) (225 * fade)),
                    r * (0.28f - spread * 0.07f));
        }
        float bow = Math.abs((float) Math.sin(t * 0.62f));
        Kawaii.draw(p, Kawaii.BLOB, x, y - r * (0.12f + bow * 0.16f), r * 0.76f,
                Glyph.withAlpha(col, (int) (235 * fade)), 1.08f, 1f);
        // A tiny crooked victory crown.
        p.fillPoly(new float[] {x - r * 0.34f, y - r * 0.80f,
                x - r * 0.20f, y - r * 1.13f,
                x, y - r * 0.88f,
                x + r * 0.23f, y - r * 1.16f,
                x + r * 0.37f, y - r * 0.76f},
                Glyph.withAlpha(GOLD, (int) (235 * fade)));
    }

    private static void confetti(Painter p, float x, float y, float r, float t, float fade,
            int kind) {
        for (int i = 0; i < 12; i++) {
            float phase = t * (0.16f + (i % 3) * 0.025f) + i * 1.91f + kind;
            float xx = x + (float) Math.sin(phase) * r * (1.05f + (i % 4) * 0.13f);
            float fall = (t * 0.10f + i * 0.137f) % 1f;
            float yy = y - r * 1.15f + fall * r * 2.25f;
            int color = i % 3 == 0 ? GOLD : i % 3 == 1 ? ROSE : 0xFF8FD9A0;
            p.fillPoly(Glyph.hex(xx, yy, r * 0.09f),
                    Glyph.withAlpha(color, (int) (205 * fade)));
        }
    }
}
