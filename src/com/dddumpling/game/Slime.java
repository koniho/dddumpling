package com.dddumpling.game;

/**
 * Draws a {@link Softbody} as slime: a translucent gooey mass with a soft inner core, a bright rim,
 * a wet specular that glistens on its own clock, a pooled underside, and a face.
 *
 * The whole read is in the layering, and one rule sits behind all of it — translucent primitives
 * that overlap double-blend (AGENTS.md), so every pass that carries the goo's own colour is
 * <em>one</em> polygon. The body is a single filled outline. The core is the character's own
 * silhouette rather than a second inset ring, because two rings read as concentric hard edges. And
 * the rim is opaque, which is worth reading the note at the stroke for.
 *
 * The only translucent things laid over the body are white: the specular and the underside sheen.
 * White over the goo is a highlight wherever it lands, so an overlap there is a brighter highlight
 * rather than a seam of the wrong colour — which is why the drips this had at first are gone. Three
 * goo-coloured circles hung off the bottom edge left a dark crescent where each one crossed the
 * silhouette, and with the seam or without it they read as feet.
 */
final class Slime extends Draw {

    /**
     * How much of the body's alpha the goo keeps. Not opaque, so the sky reads through it and it
     * looks wet rather than moulded; not much lower either, because the face inside it has to stay
     * legible against whatever the boss happens to be standing in front of.
     */
    private static final int BODY_A = 214;
    /**
     * The character's radius as a fraction of the body's.
     *
     * Held down by the two Kawaii characters that draw outside their own radius: the squishy's
     * squeeze marks reach 1.3 of it and the cat's whiskers nearly as far. At 0.78 those crossed the
     * rim on any ordinary frame; at this they only graze it on the far side of a deep dent, where
     * the outline has come in past 0.7 of the radius. That last case cannot be fixed by clipping —
     * {@link Painter} clips to a rectangle and never to a shape — so the choice is this number or
     * a smaller face, and this is as large as it goes.
     */
    private static final float FACE_R = 0.66f;

    private Slime() {}

    /**
     * @param b     the body, already stepped for this frame
     * @param clock a monotonic seconds clock, for the specular and the sheen. Passed in rather than
     *              read, for the same reason everything else here is: the preview harness
     *              hash-compares frames, so this has to be the game's own clock.
     * @param tint  the goo colour, ARGB. Its alpha is replaced.
     * @param face  a {@link Kawaii} character index, or -1 for a blank blob
     * @param happy 0..1, handed straight to {@link Kawaii#draw}
     * @param fade  0..1 overall alpha, for an arrival or an exit
     */
    static void draw(Painter p, Softbody b, float clock, int tint, int face, float happy,
            float fade) {
        if (fade <= 0f) return;
        float[] ring = b.outline();
        float cx = b.centreX(), cy = b.centreY(), r = b.radius();
        if (r <= 0f) return;
        // The half-extents, and the smaller of them. A body can be wider than it is tall, and the
        // mean radius of a wide one is bigger than either of the things that have to fit inside it
        // vertically — so a face or a sheen sized off `r` would hang out of the top and bottom of the
        // very body it is meant to be inside. Everything that has to stay in the silhouette is sized
        // off `core`; `r` is kept for the things that are proportions of the goo itself, like how
        // thick the rim is.
        float rx = Math.max(1e-3f, b.radiusX()), ry = Math.max(1e-3f, b.radiusY());
        float core = Math.min(rx, ry);
        // A struck body is briefly brighter and wetter. Deform is a look rather than a measurement,
        // so this is clamped hard: a solid hit would otherwise white the whole thing out.
        float hit = Math.min(1f, b.deform() * 4.5f);

        p.fillPoly(ring, Glyph.withAlpha(tint, (int) (BODY_A * fade)));

        // The core and the face in one pass. It was two — an inset copy of the outline for a deep
        // centre, then the character over it — and the two circles read as a mascot sealed in a bag.
        if (face >= 0) {
            // Squashes with the body, out of the real bounding box rather than out of a timer, so it
            // is flattened on exactly the frames the body is. Square-rooted because Kawaii
            // multiplies one axis by it and divides the other. The body's own resting width is
            // divided out first — see Softbody.squashAspect — or a boss that is wide by design would
            // wear a permanently squashed face.
            float sq = (float) Math.sqrt(Math.max(0.6f, Math.min(1.7f, b.squashAspect())));
            int inner = Glyph.withAlpha(Glyph.mix(tint, 0xFF241B50, 0.34f), (int) (104 * fade));
            Kawaii.draw(p, face, cx, cy + core * 0.05f, core * FACE_R, inner, sq, happy);
        }

        // The pooled underside: goo is thickest where it is sitting. Sunk well inside the outline so
        // it never touches the rim, and white rather than a darker tint so nothing about it can read
        // as a seam.
        float low = underside(ring, cx, core);
        p.fillEllipse(cx, low - core * 0.22f, rx * 0.50f, core * 0.20f,
                fadeBy(Glyph.withAlpha(0xFFFFFFFF, 40), fade));

        // Rim: the surface tension. It brightens where the body is deformed, which is what sells the
        // wobble — a rim of constant weight makes even a violent squish look like a still image.
        //
        // Opaque, and that is not a style choice. A stroked polygon is drawn as one quad per edge
        // with a round cap at each join, so at any alpha below full it double-blends at every single
        // vertex — and this ring has seventy-two of them, which came out as a dotted line all the
        // way round the boss. It is the translucent-overlap trap from AGENTS.md turning up somewhere
        // nothing else in the game meets it, since every other stroke here is a handful of points
        // long. The {@code fade} still reaches it, so an arrival is briefly stippled; a whole body
        // at half alpha is not a frame anybody reads the rim of.
        int rim = Glyph.mix(tint, 0xFFFFFFFF, 0.42f + 0.38f * hit);
        p.strokePoly(ring, Glyph.withAlpha(rim, (int) (255 * fade)), r * (0.055f + 0.025f * hit));

        // Specular, over the rim because it is on the wet surface rather than under it. Two ellipses
        // at hashed rates off the clock, the smaller riding inside the larger: one blob reads as a
        // painted spot, the pair reads as a curved wet surface. Its wobble is independent of the
        // body's, so a boss holding perfectly still still glistens.
        float sw = 1f + 0.12f * (float) Math.sin(clock * 1.9f + hash(3) * Softbody.TAU);
        float sh = 1f + 0.14f * (float) Math.sin(clock * 2.7f + hash(11) * Softbody.TAU);
        float gx = cx - b.spanX() * 0.21f, gy = cy - b.spanY() * 0.28f;
        p.fillEllipse(gx, gy, core * 0.26f * sw, core * 0.155f * sh,
                fadeBy(Glyph.withAlpha(0xFFFFFFFF, 74 + (int) (54 * hit)), fade));
        p.fillEllipse(gx - core * 0.06f, gy - core * 0.045f, core * 0.095f * sh, core * 0.065f * sw,
                fadeBy(Glyph.withAlpha(0xFFFFFFFF, 185), fade));
    }

    /** Project a rounded cube from the live ring; dents and finger stretches stay in the skin. */
    static void cube(Painter p, Softbody b, float clock, int tint, boolean vulnerable, float fade) {
        if (fade <= 0f) return;
        float cx = b.centreX(), cy = b.centreY();
        float rx = b.radiusX(), ry = b.radiusY(), core = Math.min(rx, ry);
        float[] front = cubeOutline(b);
        int count = front.length / 2;
        float dx = core * 0.28f, dy = -core * 0.28f;
        cubeFacet(p, front, count * 5 / 8, count * 7 / 8, dx, dy,
                fadeBy(Glyph.mix(tint, 0xFFFFFFFF, 0.38f), fade));
        cubeFacet(p, front, count * 7 / 8, count + count / 8, dx, dy,
                fadeBy(Glyph.mix(tint, INK, 0.25f), fade));
        p.fillPoly(front, Glyph.withAlpha(tint, (int) (225 * fade)));
        int rim = fadeBy(Glyph.mix(tint, 0xFFFFFFFF, 0.52f), fade);
        p.strokePoly(front, rim, core * 0.035f);
        p.fillEllipse(cx - rx * 0.49f, cy - ry * 0.64f, core * 0.23f, core * 0.075f,
                Glyph.withAlpha(0xFFFFFFFF, (int) (150 * fade)));
        p.fillEllipse(cx - rx * 0.63f, cy - ry * 0.47f, core * 0.065f, core * 0.12f,
                Glyph.withAlpha(0xFFFFFFFF, (int) (195 * fade)));
        p.fillEllipse(cx + rx * 0.38f, cy + ry * 0.69f, core * 0.30f, core * 0.065f,
                Glyph.withAlpha(0xFFFFFFFF, (int) (48 * fade)));
        int face = fadeBy(0xFF251D3D, fade);
        float ex = core * 0.29f, ey = cy - core * 0.10f;
        for (int side = -1; side <= 1; side += 2) {
            float x = cx + side * ex;
            p.fillEllipse(x, ey, core * 0.085f, core * (vulnerable ? 0.145f : 0.10f), face);
            p.fillCircle(x - core * 0.018f, ey - core * 0.035f, core * 0.023f,
                    fadeBy(0xFFFFFFFF, fade));
            if (vulnerable)
                p.line(x - side * core * 0.09f, ey - core * 0.26f,
                        x + side * core * 0.09f, ey - core * 0.19f, face, core * 0.035f);
        }
        if (vulnerable) {
            p.fillEllipse(cx, cy + core * 0.27f, core * 0.105f, core * 0.135f, face);
            p.fillEllipse(cx + core * 0.53f, cy + core * 0.03f,
                    core * 0.05f, core * 0.10f, fadeBy(0xFFC4EEFF, fade));
        } else {
            p.polyline(new float[] {cx - core * 0.12f, cy + core * 0.22f,
                    cx, cy + core * 0.27f, cx + core * 0.12f, cy + core * 0.22f},
                    face, core * 0.035f);
        }
    }

    static float[] cubeOutline(Softbody b) {
        float rx = b.radiusX(), ry = b.radiusY();
        float[] source = b.outline(), front = new float[source.length];
        int count = front.length / 2;
        for (int i = 0; i < count; i++) {
            float angle = Softbody.TAU * i / count;
            float co = (float) Math.cos(angle), si = (float) Math.sin(angle);
            float c4 = co * co * co * co, s4 = si * si * si * si;
            float round = (float) Math.pow(c4 * c4 + s4 * s4, -0.125);
            // Add the rest-shape projection rather than magnifying a pulled node's displacement.
            front[i * 2] = source[i * 2] + rx * co * (round - 1f);
            front[i * 2 + 1] = source[i * 2 + 1] + ry * si * (round - 1f);
        }
        return front;
    }

    private static void cubeFacet(Painter p, float[] ring, int start, int end,
            float dx, float dy, int color) {
        int count = ring.length / 2, length = end - start + 1;
        float[] facet = new float[length * 4];
        for (int i = 0; i < length; i++) {
            int at = ((start + i) % count) * 2;
            facet[i * 2] = ring[at] + dx;
            facet[i * 2 + 1] = ring[at + 1] + dy;
            int back = (length * 2 - 1 - i) * 2;
            facet[back] = ring[at];
            facet[back + 1] = ring[at + 1];
        }
        p.fillPoly(facet, color);
    }

    /**
     * The soft body's own structure, drawn faintly inside it: the node ring and the spokes out to it.
     *
     * This is what makes the jiggle legible. A smooth silhouette wobbling is surprisingly hard to
     * read as physics — the eye needs something interior to compare the edge against, and the mesh is
     * already there in the sim, so drawing it costs nothing but the lines. It deforms with the body
     * because it *is* the body: every point here is a node, not a scaled copy of the outline.
     *
     * Faint, and inside the rim rather than on it. At full strength it reads as a wireframe and the
     * creature stops being a creature.
     *
     * @param heat 0..1 extra brightness, for a body that has just been hit or is being stretched
     */
    static void mesh(Painter p, Softbody b, int tint, float heat, float fade) {
        int n = b.nodes();
        if (n < 3) return;
        float cx = b.centreX(), cy = b.centreY();
        int col = Glyph.mix(tint, 0xFFFFFFFF, 0.55f);
        int a = (int) ((26 + 44 * heat) * fade);
        if (a <= 1) return;
        float w = b.radius() * 0.018f;

        // Spokes, every other node so the middle does not fill in with lines.
        for (int i = 0; i < n; i += 2) {
            float nx = b.nodeX(i), ny = b.nodeY(i);
            p.line(cx + (nx - cx) * 0.18f, cy + (ny - cy) * 0.18f,
                    cx + (nx - cx) * 0.86f, cy + (ny - cy) * 0.86f,
                    Glyph.withAlpha(col, a), w);
        }
        // And one ring inside the skin, on the same nodes, so the spokes have something to meet.
        float[] ring = new float[n * 2];
        for (int i = 0; i < n; i++) {
            ring[i * 2] = cx + (b.nodeX(i) - cx) * 0.62f;
            ring[i * 2 + 1] = cy + (b.nodeY(i) - cy) * 0.62f;
        }
        // Closed by hand: polyline leaves an open path, and a missing segment on a ring is the one
        // thing the eye does notice.
        float[] closed = new float[ring.length + 2];
        System.arraycopy(ring, 0, closed, 0, ring.length);
        closed[ring.length] = ring[0];
        closed[ring.length + 1] = ring[1];
        p.polyline(closed, Glyph.withAlpha(col, a), w);
    }

    /**
     * Lowest point of the outline near {@code atX}, or the bottom of it if nothing is near.
     *
     * Read off the outline rather than derived from the centre, so the sheen rides with the body:
     * when a squash flattens it, the pooled bottom comes up with the edge instead of staying where
     * a round blob would have put it.
     */
    private static float underside(float[] ring, float atX, float r) {
        float best = -Float.MAX_VALUE, any = -Float.MAX_VALUE;
        float band = r * 0.30f;
        for (int i = 0; i < ring.length; i += 2) {
            if (ring[i + 1] > any) any = ring[i + 1];
            if (Math.abs(ring[i] - atX) > band) continue;
            if (ring[i + 1] > best) best = ring[i + 1];
        }
        return best > -Float.MAX_VALUE ? best : any;
    }
}
