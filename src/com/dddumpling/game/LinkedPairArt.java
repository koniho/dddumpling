package com.dddumpling.game;

/** Key-colored vines, paws and soft hands with a directional cue; shared by Android and iOS renderers. */
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
            limb(p, a.word[0], ax, ay, ahx, ahy, r, 1f, ac, waiting == a);
            limb(p, b.word[0], bx, by, bhx, bhy, r, -1f, bc, waiting == b);
            if (!fruit(a.word[0])) extremity(p, a.word[0], ahx, ahy, r, 1f, ac, waiting == a);
            if (!fruit(b.word[0])) extremity(p, b.word[0], bhx, bhy, r, -1f, bc, waiting == b);
            // Short foreground sections pass over the partner's limb; the rest stays behind it.
            if (fruit(a.word[0]) && waiting != a) curl(p, ahx, ahy, r, 1f, ac, 0.35f, 0.65f);
            if (fruit(b.word[0]) && waiting != b) curl(p, bhx, bhy, r, -1f, bc, 0.65f, 0.90f);
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

    private static boolean fruit(int glyph) { return glyph == 1 || glyph == 3; }

    private static void limb(Painter p, int glyph, float ax, float ay, float hx, float hy,
            float r, float dir, int color, boolean pointing) {
        if (!fruit(glyph)) {
            arm(p, ax, ay, hx, hy, r, color);
            return;
        }
        float cx = hx + dir*r*0.24f;
        float endX = pointing ? hx + dir*r*0.85f : cx - dir*r*0.34f;
        float endY = hy - (pointing ? r*0.12f : 0f);
        float[] stem = new float[26];
        for (int i = 0; i <= 12; i++) {
            float t = i / 12f;
            stem[i*2] = ax + (endX-ax)*t;
            stem[i*2+1] = ay + (endY-ay)*t + (float)Math.sin(t*Math.PI)*r*0.38f;
        }
        p.polyline(stem, INK, r*0.21f);
        p.polyline(stem, color, r*0.105f);
        // A leaf at the bend replaces the glove's mechanical elbow with a growing node.
        float lx = stem[10], ly = stem[11];
        leaf(p, lx, ly, lx-dir*r*0.32f, ly+r*0.38f, r*0.16f, color);
        if (pointing) {
            // The unfurled tip and pointed leaf direct attention at the remaining key.
            leaf(p, endX-dir*r*0.28f, endY, endX+dir*r*0.12f, endY, r*0.13f, color);
        } else {
            curl(p, hx, hy, r, dir, color, 0f, 1f);
        }
    }

    private static void leaf(Painter p, float x, float y, float tx, float ty, float width, int color) {
        float dx = tx-x, dy = ty-y;
        float length = Math.max(0.001f, (float)Math.sqrt(dx*dx+dy*dy));
        float nx = -dy/length*width, ny = dx/length*width;
        float mx = (x+tx)*0.5f, my = (y+ty)*0.5f;
        float[] shape = {x,y, mx+nx,my+ny, tx,ty, mx-nx,my-ny};
        p.fillPoly(shape, Glyph.mix(color, GLOVE, 0.12f));
        p.strokePoly(shape, INK, width*0.30f);
        p.line(x,y,tx,ty,Glyph.mix(color,INK,0.30f),width*0.20f);
    }

    private static void curl(Painter p, float hx, float hy, float r, float dir, int color,
            float from, float to) {
        float[] path = new float[34];
        for (int i = 0; i <= 16; i++) {
            float t = from + (to-from)*i/16f;
            float angle = (float)Math.PI + t*(float)Math.PI*1.8f;
            float radius = r*(0.34f-0.08f*t);
            path[i*2] = hx+dir*r*0.24f+dir*(float)Math.cos(angle)*radius;
            path[i*2+1] = hy+(float)Math.sin(angle)*radius;
        }
        p.polyline(path, INK, r*0.19f);
        p.polyline(path, color, r*0.10f);
    }

    private static void extremity(Painter p, int glyph, float x, float y, float r,
            float dir, int color, boolean pointing) {
        if (glyph != 2 && glyph != 5) {
            hand(p, x, y, r, dir, color, pointing);
            return;
        }
        float cx = x+dir*r*0.14f;
        if (pointing) {
            capsule(p,cx,y-r*0.10f,x+dir*r*0.82f,y-r*0.10f,r*0.13f,INK);
            capsule(p,cx,y-r*0.10f,x+dir*r*0.82f,y-r*0.10f,r*0.08f,color);
        }
        // A broad animal paw, with joined toes and pads inside the silhouette.
        p.fillEllipse(cx,y,r*0.38f,r*0.32f,INK);
        p.fillEllipse(cx,y,r*0.32f,r*0.26f,color);
        for (int i = 0; i < 3; i++) {
            float toeX = cx + (i-1)*r*0.19f;
            float toeY = y-r*(i == 1 ? 0.24f : 0.18f);
            p.fillCircle(toeX,toeY,r*0.14f,INK);
            p.fillCircle(toeX,toeY,r*0.10f,color);
        }
        int pad = Glyph.mix(color,0xFFCF718C,0.55f);
        p.fillEllipse(cx,y+r*0.06f,r*0.14f,r*0.10f,pad);
        for (int i = 0; i < 3; i++) {
            p.fillEllipse(cx+(i-1)*r*0.16f,y-r*0.14f,r*0.045f,r*0.06f,pad);
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
