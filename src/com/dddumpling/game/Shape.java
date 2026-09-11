package com.dddumpling.game;

/**
 * The fifteen collectible bodies: bao, bun, shell, fin, crescent, wedge, cluster, pome,
 * citrus, glob, cube, gum, ring, cone, drop. Indexed by {@link Collect#SHAPE}.
 *
 * Each one is drawn to fit inside a nominal radius so the display case and the reveal can
 * size any entry the same way, and each takes its colours as arguments rather than reading
 * the catalogue — which is what lets {@link Trinket} draw the same body twice, once dark and
 * slightly larger, to outline a silhouette.
 */
final class Shape {

    private static final int INK = 0xFF3A2E4F;
    private static final int SHINE = 0x8CFFFFFF;
    /**
     * Leaves and stems are always these, never the entry's accent: the accent is spoken for
     * by the finish on several entries, and a grape's leaf is green whether or not the grape
     * is glittery.
     */
    private static final int LEAF = 0xFF8FD9A0, STEM = 0xFFB08968;

    private Shape() {}

    /**
     * Draws body {@code s}. {@code seed} varies the shapes that have a random-looking wobble;
     * {@code known} is false for a silhouette, which flattens the highlights and collapses
     * the leaves into the body colour.
     */
    static void draw(Painter p, int s, float cx, float cy, float r, int body,
            int accent, float fade, float seed, boolean known) {
        int fill = Draw.fadeBy(body, fade);
        int trim = Draw.fadeBy(accent, fade);
        // The shapes use their fade only for the specular highlight and the pleat lines, so
        // zeroing it here is what keeps a silhouette flat instead of glossy.
        float gloss = known ? fade : 0f;
        // Leaves and stems collapse into the body on a silhouette. Left as themselves they
        // were the one bit of real colour on an otherwise blacked-out shape, which gave the
        // fruit away and looked like a bug.
        int leaf = Draw.fadeBy(known ? LEAF : body, fade);
        int stem = Draw.fadeBy(known ? STEM : body, fade);
        switch (s) {
            case Collect.BAO: bao(p, cx, cy, r, fill, gloss); break;
            case Collect.BUN: bun(p, cx, cy, r, fill, trim, gloss); break;
            case Collect.SHELL: shell(p, cx, cy, r, fill, trim, gloss); break;
            case Collect.FIN: fin(p, cx, cy, r, fill, trim, gloss); break;
            case Collect.CRESCENT: crescent(p, cx, cy, r, fill, trim, gloss); break;
            case Collect.WEDGE: wedge(p, cx, cy, r, fill, leaf, gloss, known); break;
            case Collect.CLUSTER: cluster(p, cx, cy, r, fill, leaf, stem, gloss); break;
            case Collect.POME: pome(p, cx, cy, r, fill, leaf, stem, gloss); break;
            case Collect.CITRUS: citrus(p, cx, cy, r, fill, trim, leaf, gloss); break;
            case Collect.GLOB: glob(p, cx, cy, r, fill, gloss, seed); break;
            case Collect.CUBE: cube(p, cx, cy, r, fill, trim, gloss); break;
            case Collect.GEL_CUBE: gelCube(p, cx, cy, r, fill, trim, gloss, seed, known); break;
            case Collect.GUM: gum(p, cx, cy, r, fill, trim, gloss); break;
            case Collect.RING: ring(p, cx, cy, r, fill, trim, gloss); break;
            case Collect.CONE: cone(p, cx, cy, r, fill, trim, gloss); break;
            case Collect.STAR: starling(p, cx, cy, r, fill, trim, gloss); break;
            default: drop(p, cx, cy, r, fill, gloss); break;
        }
    }

    private static void starling(Painter p, float cx, float cy, float r, int fill, int trim,
            float fade) {
        float[] pts = new float[20];
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5;
            float rr = r * (i % 2 == 0 ? 0.98f : 0.48f);
            pts[i * 2] = cx + rr * (float) Math.cos(a);
            pts[i * 2 + 1] = cy + rr * (float) Math.sin(a);
        }
        p.fillPoly(pts, fill);
        p.strokePoly(pts, Glyph.withAlpha(trim, (int) (150 * fade)), r * 0.055f);
        shine(p, cx - r * 0.25f, cy - r * 0.20f, r * 0.17f, r * 0.11f, fade);
    }

    /** Pleated bun: dome, belly, and a crown of five pinches. */
    private static void bao(Painter p, float cx, float cy, float r, int fill, float fade) {
        final int arc = 15;
        float[] dome = new float[arc * 2];
        for (int k = 0; k < arc; k++) {
            double a = Math.PI + Math.PI * k / (arc - 1.0);
            dome[k * 2] = cx + r * 0.92f * (float) Math.cos(a);
            dome[k * 2 + 1] = cy + r * 0.64f * (float) Math.sin(a);
        }
        p.fillPoly(dome, fill);
        p.fillEllipse(cx, cy + r * 0.24f, r * 0.92f, r * 0.42f, fill);
        for (int k = -2; k <= 2; k++) {
            float px = cx + k * r * 0.36f;
            p.fillEllipse(px, cy - r * 0.52f, r * 0.20f, r * 0.17f, fill);
            p.polyline(new float[] {px, cy - r * 0.64f, px, cy - r * 0.38f},
                    Draw.fadeBy(Glyph.withAlpha(INK, 55), fade), r * 0.05f);
        }
        shine(p, cx - r * 0.34f, cy + r * 0.02f, r * 0.22f, r * 0.13f, fade);
    }

    /** Smooth bun: no pleats, just a soft crease where the dough folded. */
    private static void bun(Painter p, float cx, float cy, float r, int fill, int trim,
            float fade) {
        p.fillEllipse(cx, cy + r * 0.04f, r * 0.90f, r * 0.80f, fill);
        p.polyline(new float[] {cx - r * 0.46f, cy - r * 0.40f, cx, cy - r * 0.52f,
                cx + r * 0.46f, cy - r * 0.40f}, Glyph.withAlpha(trim, 120), r * 0.06f);
        shine(p, cx - r * 0.32f, cy - r * 0.20f, r * 0.22f, r * 0.14f, fade);
    }

    /** Ribbed fan, apex at the bottom. */
    private static void shell(Painter p, float cx, float cy, float r, int fill, int trim,
            float fade) {
        final int n = 11;
        float ax = cx, ay = cy + r * 0.76f;
        float[] pts = new float[(n + 1) * 2];
        pts[0] = ax;
        pts[1] = ay;
        for (int k = 0; k < n; k++) {
            double a = Math.PI * (1.0 + k / (n - 1.0));
            pts[(k + 1) * 2] = cx + r * 0.92f * (float) Math.cos(a);
            pts[(k + 1) * 2 + 1] = ay + r * 1.38f * (float) Math.sin(a);
        }
        p.fillPoly(pts, fill);
        for (int k = 1; k < n - 1; k++) {
            p.line(ax, ay, pts[(k + 1) * 2], pts[(k + 1) * 2 + 1],
                    Glyph.withAlpha(trim, 110), r * 0.05f);
        }
        p.fillEllipse(ax, ay - r * 0.06f, r * 0.20f, r * 0.10f, Glyph.withAlpha(trim, 150));
    }

    /** A bun that grew a dorsal fin and a tail. */
    private static void fin(Painter p, float cx, float cy, float r, int fill, int trim,
            float fade) {
        p.fillPoly(new float[] {cx + r * 0.52f, cy + r * 0.02f, cx + r * 1.02f,
                cy - r * 0.44f, cx + r * 0.98f, cy + r * 0.42f}, trim);
        p.fillPoly(new float[] {cx - r * 0.20f, cy - r * 0.50f, cx + r * 0.28f,
                cy - r * 0.48f, cx + r * 0.02f, cy - r * 0.98f}, trim);
        p.fillEllipse(cx - r * 0.06f, cy + r * 0.04f, r * 0.86f, r * 0.70f, fill);
        shine(p, cx - r * 0.36f, cy - r * 0.18f, r * 0.20f, r * 0.12f, fade);
    }

    /** Banana: a band between two arcs, with a stub at each tip. */
    private static void crescent(Painter p, float cx, float cy, float r, int fill, int trim,
            float fade) {
        final int n = 12;
        float ccy = cy - r * 0.78f;
        float[] pts = new float[n * 4];
        for (int k = 0; k < n; k++) {
            double a = Math.PI * (0.13 + 0.74 * k / (n - 1.0));
            pts[k * 2] = cx + r * 1.00f * (float) Math.cos(a);
            pts[k * 2 + 1] = ccy + r * 1.42f * (float) Math.sin(a);
            // Inner arc walked back the other way, so the two join into one band. The gap
            // between the radii is the fruit's thickness: too narrow and it reads as a wire.
            double b = Math.PI * (0.87 - 0.74 * k / (n - 1.0));
            pts[(n + k) * 2] = cx + r * 0.62f * (float) Math.cos(b);
            pts[(n + k) * 2 + 1] = ccy + r * 0.80f * (float) Math.sin(b);
        }
        p.fillPoly(pts, fill);
        for (int s = -1; s <= 1; s += 2) {
            p.fillCircle(cx + s * r * 0.90f, cy - r * 0.28f, r * 0.13f,
                    Glyph.withAlpha(trim, 220));
        }
        shine(p, cx, cy + r * 0.44f, r * 0.30f, r * 0.09f, fade);
    }

    /** Melon wedge: flat cut on top, rind arcing below, seeds on the flesh. */
    private static void wedge(Painter p, float cx, float cy, float r, int fill, int rind,
            float fade, boolean known) {
        p.fillPoly(arcWedge(cx, cy, r, 0.94f, 1.16f), rind);
        p.fillPoly(arcWedge(cx, cy, r, 0.94f, 0.94f), fill);
        if (!known) return;      // seeds would read as noise on a silhouette
        for (int k = -1; k <= 1; k++) {
            p.fillEllipse(cx + k * r * 0.34f, cy + r * (k == 0 ? 0.24f : 0.10f), r * 0.06f,
                    r * 0.09f, Glyph.withAlpha(INK, 190));
        }
    }

    /** Half-disc hanging below a flat top edge, used for both the rind and the flesh. */
    private static float[] arcWedge(float cx, float cy, float r, float wf, float hf) {
        final int n = 13;
        float[] pts = new float[n * 2];
        for (int k = 0; k < n; k++) {
            double a = Math.PI * k / (n - 1.0);
            pts[k * 2] = cx + r * wf * (float) Math.cos(a);
            pts[k * 2 + 1] = cy - r * 0.44f + r * hf * (float) Math.sin(a);
        }
        return pts;
    }

    /** Three berries and a stem; the front one is the one with the face. */
    private static void cluster(Painter p, float cx, float cy, float r, int fill, int leaf,
            int stem, float fade) {
        for (int s = -1; s <= 1; s += 2) {
            p.polyline(new float[] {cx + s * r * 0.38f, cy - r * 0.30f, cx, cy - r * 0.88f},
                    Glyph.withAlpha(stem, 230), r * 0.06f);
        }
        p.fillPoly(new float[] {cx, cy - r * 0.84f, cx + r * 0.60f, cy - r * 0.96f,
                cx + r * 0.26f, cy - r * 0.58f}, leaf);
        p.fillCircle(cx - r * 0.42f, cy - r * 0.06f, r * 0.42f, fill);
        p.fillCircle(cx + r * 0.44f, cy + r * 0.02f, r * 0.38f, fill);
        p.fillCircle(cx + r * 0.02f, cy + r * 0.34f, r * 0.50f, fill);
        shine(p, cx - r * 0.18f, cy + r * 0.14f, r * 0.14f, r * 0.09f, fade);
    }

    /** Pear or peach: narrow shoulders over a wide base, with a stem and a leaf. */
    private static void pome(Painter p, float cx, float cy, float r, int fill, int leaf,
            int stem, float fade) {
        final int n = 22;
        float[] pts = new float[n * 2];
        for (int k = 0; k < n; k++) {
            double a = -Math.PI / 2 + 2 * Math.PI * k / n;
            float t = (float) ((Math.sin(a) + 1) / 2);       // 0 at the top, 1 at the base
            float wf = 0.54f + 0.40f * t;
            pts[k * 2] = cx + r * wf * (float) Math.cos(a);
            pts[k * 2 + 1] = cy + r * (0.10f + 0.82f * (float) Math.sin(a));
        }
        p.fillPoly(pts, fill);
        p.polyline(new float[] {cx, cy - r * 0.90f, cx + r * 0.04f, cy - r * 0.64f},
                Glyph.withAlpha(stem, 230), r * 0.07f);
        p.fillPoly(new float[] {cx + r * 0.02f, cy - r * 0.84f, cx + r * 0.58f,
                cy - r * 0.92f, cx + r * 0.26f, cy - r * 0.58f}, leaf);
        shine(p, cx - r * 0.28f, cy - r * 0.10f, r * 0.16f, r * 0.18f, fade);
    }

    /** Whole round fruit: dimple at the top, faint segment seams, a leaf. */
    private static void citrus(Painter p, float cx, float cy, float r, int fill, int trim,
            int leaf, float fade) {
        p.fillEllipse(cx, cy + r * 0.04f, r * 0.88f, r * 0.86f, fill);
        for (int s = -1; s <= 1; s += 2) {
            p.polyline(new float[] {cx + s * r * 0.10f, cy - r * 0.78f,
                    cx + s * r * 0.50f, cy + r * 0.04f, cx + s * r * 0.14f, cy + r * 0.84f},
                    Glyph.withAlpha(trim, 90), r * 0.05f);
        }
        p.fillEllipse(cx, cy - r * 0.78f, r * 0.14f, r * 0.08f, Glyph.withAlpha(INK, 90));
        p.fillPoly(new float[] {cx + r * 0.04f, cy - r * 0.80f, cx + r * 0.56f,
                cy - r * 0.94f, cx + r * 0.24f, cy - r * 0.56f}, leaf);
        shine(p, cx - r * 0.34f, cy - r * 0.28f, r * 0.20f, r * 0.13f, fade);
    }

    /** The amorphous stress ball: a circle with a slow three-lobe wobble baked in. */
    private static void glob(Painter p, float cx, float cy, float r, int fill, float fade,
            float seed) {
        final int n = 22;
        float[] pts = new float[n * 2];
        for (int k = 0; k < n; k++) {
            double a = 2 * Math.PI * k / n;
            float rr = r * (0.86f + 0.09f * (float) Math.sin(a * 3 + seed));
            pts[k * 2] = cx + rr * (float) Math.cos(a);
            pts[k * 2 + 1] = cy + rr * 0.94f * (float) Math.sin(a);
        }
        p.fillPoly(pts, fill);
        shine(p, cx - r * 0.32f, cy - r * 0.32f, r * 0.24f, r * 0.16f, fade);
    }

    /** Rounded cube, with a lit top face so it reads as a solid rather than a square. */
    private static void cube(Painter p, float cx, float cy, float r, int fill, int trim,
            float fade) {
        float hw = r * 0.78f;
        p.fillPoly(box(cx, cy + r * 0.12f, hw, r * 0.70f, r * 0.22f), fill);
        p.fillPoly(new float[] {cx - hw * 0.84f, cy - r * 0.58f, cx + hw * 0.52f,
                cy - r * 0.58f, cx + hw, cy - r * 0.30f, cx - hw * 0.36f, cy - r * 0.30f},
                Glyph.withAlpha(trim, 200));
        shine(p, cx - r * 0.30f, cy + r * 0.02f, r * 0.18f, r * 0.12f, fade);
    }

    /** A soft, translucent cube with bowed sides and bubbles suspended in its body. */
    private static void gelCube(Painter p, float cx, float cy, float r, int fill, int trim,
            float fade, float seed, boolean known) {
        float w = r * 0.82f, h = r * 0.76f;
        float wob = r * 0.045f * (float) Math.sin(seed);
        float[] skin = {
            cx - w * 0.72f, cy - h, cx + w * 0.58f, cy - h + wob,
            cx + w, cy - h * 0.55f, cx + w * 0.96f, cy + h * 0.62f,
            cx + w * 0.62f, cy + h, cx - w * 0.66f, cy + h - wob,
            cx - w, cy + h * 0.55f, cx - w * 0.96f, cy - h * 0.58f
        };
        p.fillPoly(skin, fill);
        p.strokePoly(skin, Glyph.withAlpha(trim, (int) (145 * fade)), r * 0.055f);
        if (known) {
            p.fillCircle(cx + r * 0.42f, cy - r * 0.32f, r * 0.10f,
                    Glyph.withAlpha(trim, (int) (105 * fade)));
            p.fillCircle(cx - r * 0.48f, cy + r * 0.38f, r * 0.07f,
                    Glyph.withAlpha(trim, (int) (90 * fade)));
        }
        shine(p, cx - r * 0.34f, cy - r * 0.28f, r * 0.20f, r * 0.13f, fade);
    }

    /** Rectangle with the corners cut off — a cube face, not a stadium. */
    private static float[] box(float cx, float cy, float hw, float hh, float c) {
        return new float[] {
            cx - hw + c, cy - hh, cx + hw - c, cy - hh, cx + hw, cy - hh + c,
            cx + hw, cy + hh - c, cx + hw - c, cy + hh, cx - hw + c, cy + hh,
            cx - hw, cy + hh - c, cx - hw, cy - hh + c,
        };
    }

    /** Tapered dome with a knobbly rim, sugar-coated. */
    private static void gum(Painter p, float cx, float cy, float r, int fill, int trim,
            float fade) {
        final int arc = 15;
        float[] pts = new float[(arc + 2) * 2];
        for (int k = 0; k < arc; k++) {
            double a = Math.PI + Math.PI * k / (arc - 1.0);
            float t = (float) k / (arc - 1);
            // Widens toward the base, so the dome tapers rather than sitting as a half-ball.
            float wf = 0.56f + 0.24f * (float) Math.sin(Math.PI * t);
            pts[k * 2] = cx + r * (wf + 0.18f) * (float) Math.cos(a);
            pts[k * 2 + 1] = cy - r * 0.10f + r * 0.86f * (float) Math.sin(a);
        }
        pts[arc * 2] = cx + r * 0.82f;
        pts[arc * 2 + 1] = cy + r * 0.74f;
        pts[arc * 2 + 2] = cx - r * 0.82f;
        pts[arc * 2 + 3] = cy + r * 0.74f;
        p.fillPoly(pts, fill);
        for (int k = 0; k < 7; k++) {
            double a = Math.PI + Math.PI * (k + 0.5) / 7.0;
            p.fillCircle(cx + r * 0.74f * (float) Math.cos(a),
                    cy - r * 0.10f + r * 0.80f * (float) Math.sin(a), r * 0.10f,
                    Glyph.withAlpha(trim, 170));
        }
        shine(p, cx - r * 0.24f, cy - r * 0.28f, r * 0.16f, r * 0.11f, fade);
    }

    /**
     * Donut. Built from overlapping beads around the circumference rather than a disc with a
     * hole punched in it, because {@link Painter} has no way to fill a polygon with a hole
     * and painting the hole in a background colour would only work over a known backdrop.
     */
    private static void ring(Painter p, float cx, float cy, float r, int fill, int trim,
            float fade) {
        final int n = 20;
        for (int k = 0; k < n; k++) {
            double a = 2 * Math.PI * k / n;
            p.fillCircle(cx + r * 0.58f * (float) Math.cos(a),
                    cy + r * 0.04f + r * 0.58f * (float) Math.sin(a), r * 0.30f, fill);
        }
        // Icing over the top half only, sitting a touch proud of the dough.
        for (int k = 0; k <= n / 2; k++) {
            double a = Math.PI + Math.PI * k / (n / 2.0);
            p.fillCircle(cx + r * 0.60f * (float) Math.cos(a),
                    cy - r * 0.02f + r * 0.60f * (float) Math.sin(a), r * 0.26f,
                    Glyph.withAlpha(trim, 230));
        }
        shine(p, cx - r * 0.52f, cy - r * 0.44f, r * 0.13f, r * 0.09f, fade);
    }

    /** Scoop on a waffle cone. */
    private static void cone(Painter p, float cx, float cy, float r, int fill, int trim,
            float fade) {
        p.fillPoly(new float[] {cx - r * 0.50f, cy + r * 0.02f, cx + r * 0.50f,
                cy + r * 0.02f, cx, cy + r * 0.94f}, trim);
        for (int k = -1; k <= 1; k++) {
            p.line(cx + k * r * 0.30f, cy + r * 0.04f, cx + k * r * 0.10f, cy + r * 0.62f,
                    Glyph.withAlpha(INK, 60), r * 0.04f);
        }
        p.fillCircle(cx - r * 0.28f, cy - r * 0.14f, r * 0.34f, fill);
        p.fillCircle(cx + r * 0.28f, cy - r * 0.14f, r * 0.34f, fill);
        p.fillCircle(cx, cy - r * 0.38f, r * 0.54f, fill);
        shine(p, cx - r * 0.22f, cy - r * 0.52f, r * 0.16f, r * 0.10f, fade);
    }

    /** Teardrop, point uppermost. */
    private static void drop(Painter p, float cx, float cy, float r, int fill, float fade) {
        final int n = 22;
        float[] pts = new float[n * 2];
        for (int k = 0; k < n; k++) {
            double a = -Math.PI / 2 + 2 * Math.PI * k / n;
            float t = (float) ((Math.sin(a) + 1) / 2);
            // Root, not linear: the taper has to be quick near the tip or the drop reads
            // as an egg.
            float wf = 0.88f * (float) Math.pow(t, 0.55);
            pts[k * 2] = cx + r * wf * (float) Math.cos(a);
            pts[k * 2 + 1] = cy + r * (0.10f + 0.84f * (float) Math.sin(a));
        }
        p.fillPoly(pts, fill);
        shine(p, cx - r * 0.26f, cy + r * 0.16f, r * 0.16f, r * 0.20f, fade);
    }

    private static void shine(Painter p, float cx, float cy, float rx, float ry, float fade) {
        p.fillEllipse(cx, cy, rx, ry, Draw.fadeBy(SHINE, fade));
    }
}
