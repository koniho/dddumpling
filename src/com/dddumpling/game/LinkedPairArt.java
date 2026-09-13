package com.dddumpling.game;

/** Rubber arms, white gloves and a directional cue; shared by Android and iOS renderers. */
final class LinkedPairArt {
    private static final int INK = 0xFF22253C, GLOVE = 0xFFFFF4DD, GOLD = 0xFFFFD56B;
    private LinkedPairArt() {}

    static void draw(Painter p, GameCore c, Layout L) {
        for (int i = 0; i < c.enemies.size(); i++) {
            GameCore.Enemy a = c.enemies.get(i), b = a.link;
            if (b == null || c.enemies.indexOf(b) <= i || a.destroyed || b.destroyed) continue;
            float r = L.enemyR;
            float ax = c.enemyCentreX(a) + r * 0.65f, ay = a.y;
            float bx = c.enemyCentreX(b) - r * 0.65f, by = b.y;
            float mx = (ax + bx) * 0.5f, my = (ay + by) * 0.5f;
            if (my < L.playTop - r) continue;
            float wiggle = (float) Math.sin(c.clock * 5f) * r * 0.13f;
            arm(p, ax, ay, mx - r * 0.20f, my + wiggle, r);
            arm(p, bx, by, mx + r * 0.20f, my - wiggle, r);
            GameCore.Enemy waiting = a.linkWaiting ? a : b.linkWaiting ? b : null;
            if (waiting == null) {
                // Interlocking mitten palms and three rounded knuckles each.
                glove(p, mx - r * 0.18f, my + wiggle, r * 0.40f, 1f);
                glove(p, mx + r * 0.18f, my - wiggle, r * 0.40f, -1f);
            } else {
                GameCore.Enemy next = waiting == a ? b : a;
                float tx = c.tileX(next, Math.min(next.pos, next.word.length - 1), L);
                float ty = next.y;
                float dx = tx - mx, dy = ty - my;
                float len = Math.max(1f, (float) Math.sqrt(dx * dx + dy * dy));
                dx /= len; dy /= len;
                float pulse = 0.5f + 0.5f * (float) Math.sin(c.clock * 10f);
                // A gloved index finger points at the exact next key, not merely the row.
                float hx = mx + dx * r * pulse * 0.18f;
                float hy = my + dy * r * pulse * 0.18f;
                finger(p, hx, hy, hx + dx * r * 0.85f, hy + dy * r * 0.85f, r * 0.18f);
                glove(p, hx, hy, r * 0.37f, -dx);
                p.strokePoly(Glyph.hex(tx, ty, r * (1.10f + pulse * 0.12f)), GOLD, r * 0.09f);
                float left = Math.max(0f, Math.min(1f, waiting.linkLeft / LinkedPairs.WINDOW));
                p.strokeCircle(hx, hy, r * 0.76f, Glyph.withAlpha(GLOVE, 55), r * 0.08f);
                p.arc(hx, hy, r * 0.76f, r * 0.76f, -90f, left * 360f, GOLD, r * 0.12f);
                float labelY = waiting.y + r * 1.45f;
                p.text("TOGETHER!", (c.enemyCentreX(a) + c.enemyCentreX(b)) * 0.5f, labelY,
                        r * 0.48f, GLOVE, Painter.CENTER, true);
                // A check is legible independently of color and marks the completed half.
                float cx = c.enemyCentreX(waiting), cy = waiting.y;
                p.polyline(new float[] {cx-r*0.25f,cy, cx-r*0.05f,cy+r*0.22f,
                        cx+r*0.35f,cy-r*0.28f}, GLOVE, r * 0.12f);
            }
        }
    }

    private static void arm(Painter p, float ax, float ay, float bx, float by, float r) {
        float[] path = new float[18];
        for (int i = 0; i < 9; i++) {
            float t = i / 8f;
            path[i*2] = ax + (bx-ax)*t + (float)Math.sin(t*Math.PI)*r*0.30f;
            path[i*2+1] = ay + (by-ay)*t;
        }
        p.polyline(path, INK, r * 0.25f);
        p.polyline(path, GOLD, r * 0.13f);
    }

    private static void finger(Painter p, float x, float y, float tx, float ty, float r) {
        p.line(x, y, tx, ty, INK, r * 2.5f);
        p.fillCircle(tx, ty, r * 1.25f, INK);
        p.line(x, y, tx, ty, GLOVE, r * 1.8f);
        p.fillCircle(tx, ty, r * 0.9f, GLOVE);
    }

    private static void glove(Painter p, float x, float y, float r, float direction) {
        p.fillEllipse(x, y, r * 1.1f, r * 0.9f, INK);
        p.fillEllipse(x, y, r * 0.92f, r * 0.72f, GLOVE);
        for (int i = 0; i < 3; i++) {
            float kx = x + (i - 1) * r * 0.5f;
            p.fillCircle(kx, y - r * 0.5f, r * 0.34f, INK);
            p.fillCircle(kx, y - r * 0.5f, r * 0.23f, GLOVE);
        }
        p.fillCircle(x + direction * r * 0.75f, y + r * 0.20f, r * 0.40f, INK);
        p.fillCircle(x + direction * r * 0.75f, y + r * 0.20f, r * 0.28f, GLOVE);
    }
}
