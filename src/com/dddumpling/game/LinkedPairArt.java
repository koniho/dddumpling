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
            float ax = c.enemyCentreX(a) + maskRadius(a, r) - r*0.14f, ay = a.y;
            float bx = c.enemyCentreX(b) - maskRadius(b, r) + r*0.14f, by = b.y;
            float mx = (ax + bx) * 0.5f, my = (ay + by) * 0.5f;
            if (my < L.playTop - r) continue;
            float wiggle = (float) Math.sin(c.clock * 1.4f) * r * 0.012f;
            int ac = Glyph.COLOR[a.word[0]], bc = Glyph.COLOR[b.word[0]];
            GameCore.Enemy waiting = a.linkWaiting ? a : b.linkWaiting ? b : null;
            float ahx = mx - r * 0.30f, bhx = mx + r * 0.30f;
            float ahy = my + wiggle, bhy = my - wiggle;
            float strain = Math.max(a.linkStrain, b.linkStrain);
            if (waiting != null) strain = Math.max(strain, 0.65f);
            float pose = Math.min(1f, strain/0.18f);
            // Hold a firm flex pose; only the short release tail blends back to rest.
            float limbR = r * (1f + pose*0.12f);
            float bend = 0.025f + pose*0.375f;
            Pulse wave = new Pulse(mx, Math.max(r*0.2f, (bx-ax)*0.5f), strain);
            // The entire limb layer is confined to the gap between the character silhouettes.
            // Its leaves, paws and flex overshoot cannot leak onto or behind either body.
            p.save();
            p.clipRect(L.playLeft, L.playTop, L.playRight, L.dangerY);
            p.clipOutCircle(c.enemyCentreX(a), a.y, maskRadius(a, r));
            p.clipOutCircle(c.enemyCentreX(b), b.y, maskRadius(b, r));
            limb(p, a.word[0], ax, ay, ahx, ahy, limbR, 1f, ac, false, bend, wave);
            limb(p, b.word[0], bx, by, bhx, bhy, limbR, -1f, bc, false, bend, wave);
            if (!fruit(a.word[0])) extremity(p, a.word[0], ahx, ahy, limbR, 1f, wave.tint(ac, ahx), false);
            if (!fruit(b.word[0])) extremity(p, b.word[0], bhx, bhy, limbR, -1f, wave.tint(bc, bhx), false);
            // Short foreground sections pass over the partner's limb; the rest stays behind it.
            if (fruit(a.word[0])) curl(p, ahx, ahy, limbR, 1f, ac, 0.35f, 0.65f, wave);
            if (fruit(b.word[0])) curl(p, bhx, bhy, limbR, -1f, bc, 0.65f, 0.90f, wave);
            p.restore();
            if (waiting != null) {
                GameCore.Enemy next = waiting == a ? b : a;
                float tx = c.tileX(next, Math.min(next.pos, next.word.length - 1), L);
                float ty = next.y;
                float pulse = 0.5f + 0.5f * (float) Math.sin(c.clock * 10f);
                p.strokePoly(Glyph.hex(tx, ty, r * (1.10f + pulse * 0.12f)), GOLD, r * 0.09f);

            }
        }
    }

    /** Include character extremities and late-stage threat jitter in the mask. */
    private static float maskRadius(GameCore.Enemy e, float r) {
        float attack = e.attacking ? Math.min(1f, e.attackT / GameCore.ATTACK_TIME) : 0f;
        // Follow the rendered tile's entrance and threat scale. A small inset puts the
        // shoulder beneath its border instead of leaving a visible gap around the key.
        float enter = 0.62f + 0.38f*e.enterT;
        float tile = Layout.HEAD_SCALE*enter*(1f+0.26f*attack+0.08f*e.warn);
        float jitter = Math.max(e.warn, attack)*(0.14f+0.30f*attack);
        return r*(tile*0.965f+jitter);
    }

    /** One highlight front travels from the clasp outward along both colored limbs. */
    private static final class Pulse {
        final float center, reach, phase, strength;
        Pulse(float center, float reach, float strain) {
            this.center = center;
            this.reach = reach;
            phase = (1f-strain)*1.6f;
            strength = strain > 0f ? 1f : 0f;
        }
        int tint(int color, float x) {
            float distance = Math.abs(x-center)/reach;
            float band = Math.max(0f, 1f-Math.abs(distance-phase)/0.30f);
            return Glyph.mix(color, GLOVE, band*strength*0.85f);
        }
    }

    private static boolean fruit(int glyph) { return glyph == 1 || glyph == 3; }

    private static void limb(Painter p, int glyph, float ax, float ay, float hx, float hy,
            float r, float dir, int color, boolean pointing, float bend, Pulse wave) {
        if (!fruit(glyph)) {
            arm(p, ax, ay, hx, hy, r, color, bend, wave);
            return;
        }
        float cx = hx + dir*r*0.24f;
        float endX = pointing ? hx + dir*r*0.85f : cx - dir*r*0.34f;
        float endY = hy - (pointing ? r*0.12f : 0f);
        float[] stem = new float[26];
        for (int i = 0; i <= 12; i++) {
            float t = i / 12f;
            stem[i*2] = ax + (endX-ax)*t;
            stem[i*2+1] = ay + (endY-ay)*t + (float)Math.sin(t*Math.PI)*r*bend;
        }
        float[] widths = new float[13];
        for (int i = 0; i <= 12; i++) {
            float t = i / 12f;
            widths[i] = r * (0.13f - 0.065f*t + 0.025f*(float)Math.sin(t*Math.PI));
        }
        tube(p, stem, widths, color, wave);
        // A leaf at the bend replaces the glove's mechanical elbow with a growing node.
        float lx = stem[10], ly = stem[11];
        leaf(p, lx, ly, lx-dir*r*0.32f, ly+r*0.38f, r*0.16f, wave.tint(color, lx));
        if (pointing) {
            // The unfurled tip and pointed leaf direct attention at the remaining key.
            leaf(p, endX-dir*r*0.28f, endY, endX+dir*r*0.12f, endY, r*0.13f, color);
        } else {
            curl(p, hx, hy, r, dir, color, 0f, 1f, wave);
        }
    }

    private static void leaf(Painter p, float x, float y, float tx, float ty, float width, int color) {
        float dx = tx-x, dy = ty-y;
        float length = Math.max(0.001f, (float)Math.sqrt(dx*dx+dy*dy));
        float nx = -dy/length*width, ny = dx/length*width;
        float mx = (x+tx)*0.5f, my = (y+ty)*0.5f;
        float[] shape = {x,y, mx+nx,my+ny, tx,ty, mx-nx,my-ny};
        p.fillPoly(shape, Glyph.mix(color, GLOVE, 0.12f));
        p.line(x,y,tx,ty,Glyph.mix(color,INK,0.30f),width*0.20f);
    }

    private static void curl(Painter p, float hx, float hy, float r, float dir, int color,
            float from, float to, Pulse wave) {
        float[] path = new float[34];
        for (int i = 0; i <= 16; i++) {
            float t = from + (to-from)*i/16f;
            float angle = (float)Math.PI + t*(float)Math.PI*1.8f;
            float radius = r*(0.34f-0.08f*t);
            path[i*2] = hx+dir*r*0.24f+dir*(float)Math.cos(angle)*radius;
            path[i*2+1] = hy+(float)Math.sin(angle)*radius;
        }
        float[] widths = new float[17];
        for (int i = 0; i <= 16; i++) {
            float t = from + (to-from)*i/16f;
            widths[i] = r*(0.075f-0.035f*t);
        }
        tube(p, path, widths, color, wave);
    }

    private static void extremity(Painter p, int glyph, float x, float y, float r,
            float dir, int color, boolean pointing) {
        if (glyph != 2 && glyph != 5) {
            hand(p, x, y, r, dir, color, pointing);
            return;
        }
        float cx = x+dir*r*0.14f;
        if (pointing) {
            capsule(p,cx,y-r*0.10f,x+dir*r*0.82f,y-r*0.10f,r*0.10f,color);
        }
        // A broad animal paw, with joined toes and pads inside the silhouette.
        p.fillEllipse(cx,y,r*0.36f,r*0.30f,color);
        for (int i = 0; i < 3; i++) {
            float toeX = cx + (i-1)*r*0.19f;
            float toeY = y-r*(i == 1 ? 0.24f : 0.18f);
            p.fillCircle(toeX,toeY,r*0.13f,color);
        }
        int pad = Glyph.mix(color,0xFFCF718C,0.55f);
        p.fillEllipse(cx,y+r*0.06f,r*0.14f,r*0.10f,pad);
        for (int i = 0; i < 3; i++) {
            p.fillEllipse(cx+(i-1)*r*0.16f,y-r*0.14f,r*0.045f,r*0.06f,pad);
        }
    }

    /** Nearly straight at rest; the elbow drops below the clasp into a held flex on rejection. */
    private static void arm(Painter p, float ax, float ay, float bx, float by, float r, int color, float bend, Pulse wave) {
        float ex = ax + (bx - ax) * 0.60f;
        float ey = Math.max(ay, by) + r * bend;
        // Fuller upper arm, pinched elbow, then a soft forearm taper into the wrist.
        taperedSegment(p, ax, ay, ex, ey, r*0.16f, r*0.115f, r*0.025f, color, wave);
        taperedSegment(p, ex, ey, bx, by, r*0.115f, r*0.095f, r*0.045f, color, wave);
        p.fillCircle(ex, ey, r * 0.155f, wave.tint(Glyph.mix(color, GLOVE, 0.14f), ex));
        p.fillCircle(ex - r * 0.035f, ey - r * 0.035f, r * 0.045f,
                Glyph.mix(color, GLOVE, 0.45f));
    }

    private static void taperedSegment(Painter p, float ax, float ay, float bx, float by,
            float start, float end, float fullness, int color, Pulse wave) {
        float[] points = new float[18], widths = new float[9];
        for (int i = 0; i <= 8; i++) {
            float t = i/8f;
            points[i*2] = ax+(bx-ax)*t;
            points[i*2+1] = ay+(by-ay)*t;
            widths[i] = start+(end-start)*t+fullness*(float)Math.sin(t*Math.PI);
        }
        tube(p, points, widths, color, wave);
    }

    /** Filled variable-width segments with rounded joins; no outline stroke. */
    private static void tube(Painter p, float[] points, float[] widths, int color, Pulse wave) {
        for (int i = 0; i < widths.length; i++) {
            float x = points[i*2], y = points[i*2+1];
            p.fillCircle(x, y, widths[i], wave.tint(color, x));
            if (i == 0) continue;
            float px = points[i*2-2], py = points[i*2-1];
            float dx = x-px, dy = y-py;
            float length = Math.max(0.001f, (float)Math.sqrt(dx*dx+dy*dy));
            float nx = -dy/length, ny = dx/length;
            float a = widths[i-1], b = widths[i];
            p.fillPoly(new float[] {px+nx*a,py+ny*a, x+nx*b,y+ny*b,
                    x-nx*b,y-ny*b, px-nx*a,py-ny*a}, wave.tint(color, (px+x)*0.5f));
        }
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
                    r * 0.10f, color);
        }
        p.fillEllipse(palmX, y, r * 0.34f, r * 0.27f, color);
        // Thumb curves across the lower palm, making a clasp rather than a paw print.
        capsule(p, x - dir*r*0.04f, y+r*0.16f, x+dir*r*0.24f, y+r*0.23f, r*0.10f, color);
        int seam = Glyph.mix(color, INK, 0.48f);
        for (int i = 0; i < 2; i++) {
            float fx = palmX + dir * r * (0.08f + i * 0.10f);
            p.line(fx, y-r*0.16f, fx, y-r*0.03f, seam, r*0.025f);
        }
        // Cuff bridges the forearm and palm; a soft highlight keeps the hand readable small.
        float cuffX = x - dir*r*0.19f;
        capsule(p, cuffX, y-r*0.10f, cuffX, y+r*0.10f, r*0.060f, Glyph.mix(color, GLOVE, 0.35f));
        p.arc(palmX, y, r*0.22f, r*0.15f, 210f, 65f,
                Glyph.mix(color, GLOVE, 0.42f), r*0.035f);
    }
}
