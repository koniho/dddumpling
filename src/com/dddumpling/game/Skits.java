package com.dddumpling.game;

/**
 * Ten short vignettes played under the stage title, each with two or three characters
 * interacting. They cycle by stage, so a run works through all of them in order.
 *
 * Every skit is a pure function of a 0..1 progress, which keeps them deterministic and lets
 * the harness render any frame of any one of them.
 */
final class Skits {

    static final int COUNT = 10;

    static final String[] NAMES = {
        "BONK", "SMITTEN", "TOSS", "KABOOM", "TOWER",
        "CHASE", "HIGH FIVE", "BOUNCE", "SHARE", "DANCE",
    };

    private Skits() {}

    /** Which skit a stage shows. Cycles, so every one gets seen. */
    static int forStage(int stage) {
        int i = (stage - 1) % COUNT;
        return i < 0 ? 0 : i;
    }

    /**
     * Draws skit {@code index} centred on cx,cy within a band of half-height {@code r}.
     *
     * @param t     0..1 progress through the vignette
     * @param alpha 0..255 master opacity
     */
    static void draw(Painter p, Layout L, int index, float cx, float cy, float r, float t,
            int alpha, float clock) {
        switch (index) {
            case 0: bonk(p, cx, cy, r, t, alpha); break;
            case 1: smitten(p, cx, cy, r, t, alpha); break;
            case 2: toss(p, cx, cy, r, t, alpha); break;
            case 3: kaboom(p, cx, cy, r, t, alpha); break;
            case 4: tower(p, cx, cy, r, t, alpha); break;
            case 5: chase(p, cx, cy, r, t, alpha); break;
            case 6: highFive(p, cx, cy, r, t, alpha); break;
            case 7: bounce(p, cx, cy, r, t, alpha); break;
            case 8: share(p, cx, cy, r, t, alpha); break;
            default: dance(p, cx, cy, r, t, alpha, clock); break;
        }
    }

    // ---- the skits ----------------------------------------------------------

    /** Two rush together, clonk heads, and reel apart seeing stars. */
    private static void bonk(Painter p, float cx, float cy, float r, float t, int a) {
        float in = Math.min(1f, t / 0.45f);
        float out = t > 0.45f ? (t - 0.45f) / 0.55f : 0f;
        float gap = r * (1.7f * (1f - in) + 0.75f + out * 1.1f);
        float tilt = out * 0.14f;

        face(p, Kawaii.CAT, cx - gap, cy + out * r * 0.2f, r * 0.62f, a, 1f - tilt, 0.1f);
        face(p, Kawaii.GRAPES, cx + gap, cy + out * r * 0.2f, r * 0.62f, a, 1f - tilt, 0.1f);
        if (out > 0f) {
            for (int k = 0; k < 5; k++) {
                double ang = -Math.PI / 2 + (k - 2) * 0.42;
                float d = r * (0.7f + out * 1.1f);
                spark(p, cx + (float) Math.cos(ang) * d, cy + (float) Math.sin(ang) * d,
                        r * 0.22f * (1f - out), 0xFFFFE07A, (int) (a * (1f - out)));
            }
        }
    }

    /** Two lean in and hearts float up between them. */
    private static void smitten(Painter p, float cx, float cy, float r, float t, int a) {
        float lean = (float) Math.sin(Math.min(1f, t / 0.5f) * Math.PI / 2) * r * 0.35f;
        face(p, Kawaii.DUMPLING, cx - r * 0.85f + lean, cy, r * 0.62f, a, 1f, 1f);
        face(p, Kawaii.STRAWBERRY, cx + r * 0.85f - lean, cy, r * 0.62f, a, 1f, 1f);
        for (int k = 0; k < 3; k++) {
            float ht = (t * 1.4f + k * 0.33f) % 1f;
            float hy = cy - r * 0.3f - ht * r * 1.5f;
            float hx = cx + (float) Math.sin(ht * 6f + k) * r * 0.25f;
            heart(p, hx, hy, r * 0.26f * (1f - ht * 0.4f),
                    Glyph.withAlpha(0xFFFF6B8E, (int) (a * (1f - ht))));
        }
    }

    /** One lobs a third overhead, back and forth. */
    private static void toss(Painter p, float cx, float cy, float r, float t, int a) {
        face(p, Kawaii.SQUISHY, cx - r * 1.25f, cy + r * 0.25f, r * 0.58f, a, 1f, 0.6f);
        face(p, Kawaii.BLOB, cx + r * 1.25f, cy + r * 0.25f, r * 0.58f, a, 1f, 0.6f);
        // Two arcs across the vignette, alternating direction.
        float lap = (t * 2f) % 1f;
        boolean rightward = t < 0.5f;
        float from = rightward ? -r * 1.25f : r * 1.25f;
        float to = rightward ? r * 1.25f : -r * 1.25f;
        float x = cx + from + (to - from) * lap;
        float y = cy + r * 0.15f - (float) Math.sin(lap * Math.PI) * r * 1.1f;
        face(p, Kawaii.STRAWBERRY, x, y, r * 0.34f, a, 1f, 1f);
        spark(p, x, y + r * 0.4f, r * 0.12f, 0xFFFFF3C4, (int) (a * 0.6f));
    }

    /** One creeps up on another and detonates. */
    private static void kaboom(Painter p, float cx, float cy, float r, float t, int a) {
        float creep = Math.min(1f, t / 0.4f);
        float boom = t > 0.45f ? Math.min(1f, (t - 0.45f) / 0.55f) : 0f;
        float sx = cx - r * 1.4f + creep * r * 0.85f;

        if (boom <= 0f) {
            face(p, Kawaii.GRAPES, sx, cy, r * 0.58f, a, 1f, 0.9f);
            face(p, Kawaii.CAT, cx + r * 0.65f, cy, r * 0.62f, a, 1f, 0.2f);
        } else {
            // Both blown apart, wide-eyed.
            float d = boom * r * 1.3f;
            face(p, Kawaii.GRAPES, sx - d, cy - boom * r * 0.4f, r * 0.58f,
                    (int) (a * (1f - boom * 0.4f)), 1f, 0f);
            face(p, Kawaii.CAT, cx + r * 0.65f + d, cy - boom * r * 0.4f, r * 0.62f,
                    (int) (a * (1f - boom * 0.4f)), 1f, 0f);
            for (int k = 0; k < 8; k++) {
                double ang = 2 * Math.PI * k / 8;
                float dd = r * (0.4f + boom * 1.6f);
                spark(p, cx + (float) Math.cos(ang) * dd, cy + (float) Math.sin(ang) * dd,
                        r * 0.26f * (1f - boom), 0xFFFFB68A, (int) (a * (1f - boom)));
            }
        }
    }

    /** Three stack up, wobble, and topple. */
    private static void tower(Painter p, float cx, float cy, float r, float t, int a) {
        float build = Math.min(1f, t / 0.55f);
        float fall = t > 0.7f ? (t - 0.7f) / 0.3f : 0f;
        float wob = (float) Math.sin(t * 16f) * r * 0.10f * build * (1f - fall);

        face(p, Kawaii.BLOB, cx, cy + r * 0.62f, r * 0.56f, a, 1f, 0.4f);
        if (build > 0.33f) {
            float k = Math.min(1f, (build - 0.33f) / 0.33f);
            face(p, Kawaii.SQUISHY, cx + wob + fall * r * 1.3f,
                    cy + r * 0.62f - k * r * 0.85f + fall * r * 0.5f, r * 0.50f, a, 1f, 0.5f);
        }
        if (build > 0.66f) {
            float k = Math.min(1f, (build - 0.66f) / 0.34f);
            face(p, Kawaii.STRAWBERRY, cx + wob * 1.8f - fall * r * 1.5f,
                    cy + r * 0.62f - k * r * 1.6f + fall * r * 0.9f, r * 0.44f, a, 1f,
                    fall > 0f ? 0f : 1f);
        }
        if (fall > 0f) {
            spark(p, cx + r * 1.2f, cy - r * 0.2f, r * 0.2f * (1f - fall), 0xFFFFE07A,
                    (int) (a * (1f - fall)));
        }
    }

    /** A merry chase, left to right, with a sparkle wake. */
    private static void chase(Painter p, float cx, float cy, float r, float t, int a) {
        float x = cx - r * 1.6f + t * r * 3.2f;
        float hop = (float) Math.abs(Math.sin(t * 12f)) * r * 0.18f;
        face(p, Kawaii.CAT, x, cy - hop, r * 0.58f, a, 1f, 1f);
        face(p, Kawaii.GRAPES, x - r * 1.0f, cy - hop * 0.6f, r * 0.54f, a, 1f, 0f);
        for (int k = 1; k <= 3; k++) {
            spark(p, x - r * (1.5f + 0.35f * k), cy + r * 0.3f, r * 0.11f,
                    0xFFC3A8F5, (int) (a * 0.5f / k));
        }
    }

    /** Two swing in and clap, with a starburst on contact. */
    private static void highFive(Painter p, float cx, float cy, float r, float t, int a) {
        float swing = Math.min(1f, t / 0.5f);
        float hit = t > 0.5f ? Math.min(1f, (t - 0.5f) / 0.5f) : 0f;
        float gap = r * (1.5f - swing * 0.75f + hit * 0.3f);
        face(p, Kawaii.DUMPLING, cx - gap, cy, r * 0.60f, a, 1f, 0.5f + hit * 0.5f);
        face(p, Kawaii.BLOB, cx + gap, cy, r * 0.60f, a, 1f, 0.5f + hit * 0.5f);
        if (hit > 0f) {
            burst(p, cx, cy - r * 0.1f, r * (0.4f + hit * 1.0f), 0xFFFFF3C4,
                    (int) (a * (1f - hit)));
        }
    }

    /** One uses the other as a trampoline. */
    private static void bounce(Painter p, float cx, float cy, float r, float t, int a) {
        face(p, Kawaii.SQUISHY, cx, cy + r * 0.6f, r * 0.62f, a,
                1f + 0.18f * (float) Math.abs(Math.sin(t * 6.3f)), 0.7f);
        float lap = (t * 2f) % 1f;
        float y = cy + r * 0.1f - (float) Math.sin(lap * Math.PI) * r * 1.25f;
        face(p, Kawaii.STRAWBERRY, cx + (float) Math.sin(t * 6.3f) * r * 0.2f, y, r * 0.46f,
                a, 1f, 1f);
        if (lap < 0.16f) {
            burst(p, cx, cy + r * 0.15f, r * (0.3f + lap * 3f), 0xFF9AE9C0,
                    (int) (a * (1f - lap * 6f)));
        }
    }

    /** One hands a heart to a sad one, who brightens. */
    private static void share(Painter p, float cx, float cy, float r, float t, int a) {
        float give = Math.min(1f, t / 0.6f);
        float cheer = t > 0.6f ? Math.min(1f, (t - 0.6f) / 0.4f) : 0f;
        face(p, Kawaii.DUMPLING, cx - r * 0.9f, cy, r * 0.60f, a, 1f, 1f);
        face(p, Kawaii.GRAPES, cx + r * 0.9f, cy - cheer * r * 0.14f, r * 0.60f, a, 1f, cheer);
        float hx = cx - r * 0.55f + give * r * 1.1f;
        heart(p, hx, cy - r * 0.15f - (float) Math.sin(give * Math.PI) * r * 0.3f,
                r * 0.24f, Glyph.withAlpha(0xFFFF6B8E, a));
        if (cheer > 0f) {
            for (int k = 0; k < 3; k++) {
                spark(p, cx + r * (0.6f + 0.3f * k), cy - r * (0.7f + 0.2f * k),
                        r * 0.14f * cheer, 0xFFFFE07A, (int) (a * cheer));
            }
        }
    }

    /** Two hop in time with a note or two. */
    private static void dance(Painter p, float cx, float cy, float r, float t, int a,
            float clock) {
        float beat = t * 8f;
        float l = (float) Math.abs(Math.sin(beat)) * r * 0.30f;
        float rr = (float) Math.abs(Math.cos(beat)) * r * 0.30f;
        face(p, Kawaii.CAT, cx - r * 0.85f, cy - l, r * 0.60f, a, 1f + 0.1f * l / r, 1f);
        face(p, Kawaii.BLOB, cx + r * 0.85f, cy - rr, r * 0.60f, a, 1f + 0.1f * rr / r, 1f);
        for (int k = 0; k < 2; k++) {
            float nt = (t * 1.6f + k * 0.5f) % 1f;
            note(p, cx + (k == 0 ? -1 : 1) * r * 1.5f, cy - r * 0.2f - nt * r * 1.2f,
                    r * 0.22f, Glyph.withAlpha(0xFF93D6F7, (int) (a * (1f - nt))));
        }
    }

    // ---- parts --------------------------------------------------------------

    /** A character with a soft halo, so it reads against the clouds behind it. */
    private static void face(Painter p, int glyph, float cx, float cy, float r, int a,
            float squash, float happy) {
        if (a <= 2) return;
        int col = Glyph.withAlpha(Glyph.COLOR[glyph], a);
        p.fillCircle(cx, cy, r * 1.35f, Glyph.withAlpha(Glyph.COLOR[glyph], a / 7));
        Kawaii.draw(p, glyph, cx, cy, r, col, squash, happy);
    }

    private static void heart(Painter p, float cx, float cy, float r, int col) {
        p.fillCircle(cx - r * 0.45f, cy - r * 0.28f, r * 0.55f, col);
        p.fillCircle(cx + r * 0.45f, cy - r * 0.28f, r * 0.55f, col);
        p.fillPoly(new float[] {cx - r * 0.95f, cy - r * 0.10f, cx + r * 0.95f, cy - r * 0.10f,
                cx, cy + r * 1.05f}, col);
    }

    /** Four-point twinkle. */
    private static void spark(Painter p, float cx, float cy, float r, int tint, int a) {
        if (a <= 2 || r <= 0.2f) return;
        int col = Glyph.withAlpha(tint, a);
        p.fillPoly(new float[] {cx, cy - r, cx + r * 0.28f, cy, cx, cy + r, cx - r * 0.28f, cy},
                col);
        p.fillPoly(new float[] {cx - r, cy, cx, cy - r * 0.28f, cx + r, cy, cx, cy + r * 0.28f},
                col);
    }

    /** Radiating rays, for impacts. */
    private static void burst(Painter p, float cx, float cy, float r, int tint, int a) {
        if (a <= 2) return;
        int col = Glyph.withAlpha(tint, a);
        for (int k = 0; k < 8; k++) {
            double ang = 2 * Math.PI * k / 8 + 0.2;
            float x0 = cx + (float) Math.cos(ang) * r * 0.45f;
            float y0 = cy + (float) Math.sin(ang) * r * 0.35f;
            p.line(x0, y0, cx + (float) Math.cos(ang) * r * 0.9f,
                    cy + (float) Math.sin(ang) * r * 0.9f, col, r * 0.20f);
        }
    }

    private static void note(Painter p, float cx, float cy, float r, int col) {
        p.fillEllipse(cx, cy + r * 0.55f, r * 0.42f, r * 0.32f, col);
        p.fillRect(cx + r * 0.28f, cy - r * 0.7f, cx + r * 0.44f, cy + r * 0.6f, col);
        p.fillPoly(new float[] {cx + r * 0.44f, cy - r * 0.7f, cx + r * 0.95f, cy - r * 0.45f,
                cx + r * 0.44f, cy - r * 0.3f}, col);
    }
}
