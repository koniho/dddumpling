package com.dddumpling.game;

/** The marked skin must reach the wall; a short finger wiggle did not teach the destination. */
final class SlimeGuide extends Draw {
    private SlimeGuide() {}

    static void draw(Painter p, GameCore c, Layout L, Boss b, int glob, boolean held, float fade) {
        float r = b.er[glob], x = b.ex[glob], y = b.ey[glob];
        float centre = (L.playLeft + L.playRight) * 0.5f;
        // Demonstrate a full pull; edge-born globs must cross the interior before they can hit.
        boolean right = held && b.globDragCanDamage ? x >= centre : x < centre;
        float side = right ? 1f : -1f;
        float edge = Boss.globSideEdge(L, r, right);
        float target = edge + side * r * 0.35f;
        float guideY = Math.max(L.playTop + r * 2f, Math.min(L.dangerY - r * 2f, y));
        float phase = ((Boss.GLOB_TIME - b.elife[glob]) / 1.8f) % 1f;
        float pull = Math.max(0f, Math.min(1f, (phase - 0.18f) / 0.57f));
        pull *= pull;
        float pulse = 0.65f + 0.35f * (float)Math.sin(c.clock * 7f);
        wall(p, L, r, guideY, right, fade * (held ? 1f : 0.72f) * pulse);
        if (held) wall(p, L, r, guideY, !right, fade * 0.35f);

        // Moving chevrons connect the patch to the actual damage boundary.
        for (int i = 1; i <= 3; i++) {
            float u = (i + (c.clock * 1.4f) % 1f) / 5f;
            float ax = x + (target - x) * u;
            float ay = y + (guideY - y) * u;
            float size = r * 0.24f;
            p.polyline(new float[]{ax-side*size, ay-size, ax, ay,
                    ax-side*size, ay+size}, Glyph.withAlpha(GOLD, (int)(fade * 145)), r * 0.09f);
        }
        if (held) return;
        float handX = x + (target - x) * pull;
        float handY = y + (guideY - y) * pull;
        float handFade = fade * Math.min(1f, (1f-phase) * 7f);
        Renderer.touchHint(p, handX, handY, r * 0.85f,
                right ? 2.45f : 0.69f, handFade, c.clock);
        if (phase > 0.75f) {
            float burst = (phase - 0.75f) / 0.25f;
            for (int ray = -2; ray <= 2; ray++) {
                float angle = ray * 0.45f;
                float dx = -side * (float)Math.cos(angle), dy = (float)Math.sin(angle);
                p.line(target + dx*r*.3f, guideY + dy*r*.3f,
                        target + dx*r*(.6f+burst*1.3f), guideY + dy*r*(.6f+burst*1.3f),
                        Glyph.withAlpha(GOLD, (int)(fade * (1f-burst) * 220)), r*.12f);
            }
        }
    }

    private static void wall(Painter p, Layout L, float r, float y, boolean right, float fade) {
        float x = Boss.globSideEdge(L, r, right);
        for (int layer = 3; layer >= 1; layer--)
            p.fillRect(x-r*.12f*layer, y-r*1.7f, x+r*.12f*layer, y+r*1.7f,
                    Glyph.withAlpha(GOLD, (int)(fade * 32 / layer)));
        p.line(x, y-r*1.5f, x, y+r*1.5f, Glyph.withAlpha(GOLD, (int)(fade*220)), r*.10f);
    }
}
