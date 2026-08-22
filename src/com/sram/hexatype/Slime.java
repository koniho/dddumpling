package com.sram.hexatype;

/**
 * Draws a {@link Softbody} as slime: a translucent gooey mass with a soft inner core, a bright rim,
 * a wet specular that glistens on its own clock, a pooled underside, and a face.
 *
 * The whole read is in the layering, and one rule sits behind all of it — translucent primitives
 * that overlap double-blend (CLAUDE.md), so every pass that carries the goo's own colour is
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
        // A struck body is briefly brighter and wetter. Deform is a look rather than a measurement,
        // so this is clamped hard: a solid hit would otherwise white the whole thing out.
        float hit = Math.min(1f, b.deform() * 4.5f);

        p.fillPoly(ring, Glyph.withAlpha(tint, (int) (BODY_A * fade)));

        // The core and the face in one pass. It was two — an inset copy of the outline for a deep
        // centre, then the character over it — and the two circles read as a mascot sealed in a bag.
        if (face >= 0) {
            // Squashes with the body, out of the real bounding box rather than out of a timer, so it
            // is flattened on exactly the frames the body is. Square-rooted because Kawaii
            // multiplies one axis by it and divides the other.
            float sq = (float) Math.sqrt(Math.max(0.6f, Math.min(1.7f, b.aspect())));
            int inner = Glyph.withAlpha(Glyph.mix(tint, 0xFF241B50, 0.34f), (int) (104 * fade));
            Kawaii.draw(p, face, cx, cy + r * 0.05f, r * FACE_R, inner, sq, happy);
        }

        // The pooled underside: goo is thickest where it is sitting. Sunk well inside the outline so
        // it never touches the rim, and white rather than a darker tint so nothing about it can read
        // as a seam.
        float low = underside(ring, cx, r);
        p.fillEllipse(cx, low - r * 0.22f, r * 0.50f, r * 0.20f,
                fadeBy(Glyph.withAlpha(0xFFFFFFFF, 40), fade));

        // Rim: the surface tension. It brightens where the body is deformed, which is what sells the
        // wobble — a rim of constant weight makes even a violent squish look like a still image.
        //
        // Opaque, and that is not a style choice. A stroked polygon is drawn as one quad per edge
        // with a round cap at each join, so at any alpha below full it double-blends at every single
        // vertex — and this ring has seventy-two of them, which came out as a dotted line all the
        // way round the boss. It is the translucent-overlap trap from CLAUDE.md turning up somewhere
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
        p.fillEllipse(gx, gy, r * 0.26f * sw, r * 0.155f * sh,
                fadeBy(Glyph.withAlpha(0xFFFFFFFF, 74 + (int) (54 * hit)), fade));
        p.fillEllipse(gx - r * 0.06f, gy - r * 0.045f, r * 0.095f * sh, r * 0.065f * sw,
                fadeBy(Glyph.withAlpha(0xFFFFFFFF, 185), fade));
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
