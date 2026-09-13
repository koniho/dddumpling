package com.dddumpling.game;

/** Jointed, key-colored mitten arms and a directional cue; shared by Android and iOS renderers. */
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
            int ac = Glyph.COLOR[a.word[0]], bc = Glyph.COLOR[b.word[0]];
            GameCore.Enemy waiting = a.linkWaiting ? a : b.linkWaiting ? b : null;
            float ahx = mx - r * 0.30f, bhx = mx + r * 0.30f;
            float ahy = my + wiggle, bhy = my - wiggle;
            if (waiting != null) {
                ahy += r * (waiting == a ? -0.16f : 0.32f);
                bhy += r * (waiting == b ? -0.16f : 0.32f);
            }
            arm(p, ax, ay, ahx, ahy, r, ac);
            arm(p, bx, by, bhx, bhy, r, bc);
            hand(p, ahx, ahy, r, 1f, ac, waiting == a);
            hand(p, bhx, bhy, r, -1f, bc, waiting == b);
            if (waiting != null) {
                GameCore.Enemy next = waiting == a ? b : a;
                float tx = c.tileX(next, Math.min(next.pos, next.word.length - 1), L);
                float ty = next.y;
                float pulse = 0.5f + 0.5f * (float) Math.sin(c.clock * 10f);
                float hx = mx, hy = my;
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

    /** Upper arm and forearm meet at a visible round elbow, below the clasp. */
    private static void arm(Painter p, float ax, float ay, float bx, float by, float r, int color) {
        float ex = ax + (bx - ax) * 0.38f;
        float ey = Math.max(ay, by) + r * 0.48f;
        float[] path = {ax, ay, ex, ey, bx, by};
        p.polyline(path, INK, r * 0.26f);
        p.polyline(path, color, r * 0.15f);
        p.fillCircle(ex, ey, r * 0.19f, INK);
        p.fillCircle(ex, ey, r * 0.13f, color);
        p.fillCircle(ex - r * 0.035f, ey - r * 0.035f, r * 0.045f,
                Glyph.mix(color, GLOVE, 0.55f));
    }

    private static void capsule(Painter p, float x, float y, float tx, float ty, float radius, int color) {
        p.line(x, y, tx, ty, color, radius * 2f);
        p.fillCircle(x, y, radius, color);
        p.fillCircle(tx, ty, radius, color);
    }

    /** A smooth mitten silhouette with a cuff, folded fingers and one distinct thumb. */
    private static void hand(Painter p, float x, float y, float r, float dir, int color, boolean pointing) {
        float palmX = x + dir * r * 0.13f;
        // The extended index is part of its owner's silhouette and keeps that key's color.
        if (pointing) {
            capsule(p, palmX, y - r * 0.12f, x + dir * r * 0.85f, y - r * 0.12f,
                    r * 0.115f, INK);
            capsule(p, palmX, y - r * 0.12f, x + dir * r * 0.85f, y - r * 0.12f,
                    r * 0.070f, color);
        }
        p.fillEllipse(palmX, y, r * 0.36f, r * 0.29f, INK);
        p.fillEllipse(palmX, y, r * 0.30f, r * 0.23f, color);
        // Thumb curves across the lower palm, making a clasp rather than a paw print.
        capsule(p, x - dir*r*0.04f, y+r*0.16f, x+dir*r*0.24f, y+r*0.23f, r*0.12f, INK);
        capsule(p, x - dir*r*0.04f, y+r*0.16f, x+dir*r*0.24f, y+r*0.23f, r*0.075f, color);
        int seam = Glyph.mix(color, INK, 0.48f);
        for (int i = 0; i < 2; i++) {
            float fx = palmX + dir * r * (0.08f + i * 0.10f);
            p.line(fx, y-r*0.16f, fx, y-r*0.03f, seam, r*0.025f);
        }
        // Cuff bridges the forearm and palm; a soft highlight keeps the hand readable small.
        float cuffX = x - dir*r*0.19f;
        capsule(p, cuffX, y-r*0.12f, cuffX, y+r*0.12f, r*0.075f, INK);
        capsule(p, cuffX, y-r*0.10f, cuffX, y+r*0.10f, r*0.040f, Glyph.mix(color, GLOVE, 0.35f));
        p.arc(palmX, y, r*0.22f, r*0.15f, 210f, 65f,
                Glyph.mix(color, GLOVE, 0.42f), r*0.035f);
    }
}
