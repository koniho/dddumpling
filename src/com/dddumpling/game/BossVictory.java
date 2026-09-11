package com.dddumpling.game;

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
        float rate = kind == Boss.OCTOPUS ? 0.82f
                : 0.94f;
        float reach = kind == Boss.OCTOPUS ? 0.24f : 0.10f;
        return (float) Math.sin(beat * rate + kind * 0.73f) * r * reach;
    }

    /** A cocky hop/stomp layered with the swagger, without resizing the retained combat body. */
    static float tauntY(int kind, float beat, float r) {
        float rate = kind == Boss.SLIME ? 0.72f : 1.05f;
        float lift = kind == Boss.SLIME ? 0.24f : 0.11f;
        return -Math.abs((float) Math.sin(beat * rate + kind * 0.41f)) * r * lift;
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
