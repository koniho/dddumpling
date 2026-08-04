package com.sram.hexatype;

/**
 * Draws a collectible from {@link Collect}: a body from {@link Shape}, a surface from
 * {@link Finish}, and a face from here.
 *
 * Shape and finish are separate classes on purpose. Thirty bespoke drawings would be thirty
 * things to tune; this way a new entry in the catalogue is one row of data, and any fix to
 * the glitter or the holo sheen lands on every entry that wears it.
 *
 * Uncollected entries go through the same shape code with a flat dark fill and a question
 * mark over it, so the silhouette is a real outline of the thing you are missing rather
 * than a generic placeholder.
 */
final class Trinket {

    private static final int INK = 0xFF3A2E4F;        // face lines
    private static final int BLUSH = 0x66FF7C9E;
    /** Fill and outline for something not yet collected. */
    private static final int DARK = 0xFF2C2450, DARK_EDGE = 0xFF564A85;

    /**
     * Where the face sits and how big it is, per {@link Shape}, in units of the nominal
     * radius. A wedge wants its face low on the flesh; a cone wants it up on the scoop.
     */
    private static final float[] FACE_DY = {
        0.04f, 0.06f, 0.18f, 0.06f, 0.40f, 0.16f, 0.34f, 0.12f,
        0.02f, 0.02f, 0.06f, 0.14f, 0.62f, -0.34f, 0.22f,
    };
    private static final float[] FACE_R = {
        0.64f, 0.66f, 0.58f, 0.62f, 0.38f, 0.60f, 0.48f, 0.60f,
        0.64f, 0.70f, 0.64f, 0.60f, 0.38f, 0.52f, 0.62f,
    };

    private Trinket() {}

    /**
     * Draws entry {@code i} centred on cx,cy at nominal radius r.
     *
     * @param known false to draw the unknown silhouette instead
     * @param fade  0..1 master opacity, for dimming the neighbours in the display case
     */
    static void draw(Painter p, int i, float cx, float cy, float r, float clock, boolean known,
            float fade) {
        if (fade <= 0.01f || i < 0 || i >= Collect.COUNT) return;
        int shape = Collect.SHAPE[i];
        if (!known) {
            // Drawn twice, the outer copy a little larger: that gives any of the fifteen
            // shapes an outline without each having to know how to stroke itself, and
            // without an outline the silhouette is dark-on-dark and reads as nothing.
            Shape.draw(p, shape, cx, cy, r * 1.09f, DARK_EDGE, DARK_EDGE, fade, i * 1.7f, false);
            Shape.draw(p, shape, cx, cy, r, DARK, DARK, fade, i * 1.7f, false);
            // Kept small enough to leave the outline visible around it — the shape is the
            // clue, and a mark that fills the frame throws that away.
            float fr = r * FACE_R[shape];
            p.text("?", cx, cy + r * FACE_DY[shape] + fr * 0.52f, fr * 1.55f,
                    Draw.fadeBy(Glyph.withAlpha(Draw.INK, 210), fade), Painter.CENTER, true);
            return;
        }
        Shape.draw(p, shape, cx, cy, r, Collect.BODY[i], Collect.ACCENT[i], fade, i * 1.7f, true);
        Finish.draw(p, Collect.FINISH[i], cx, cy, r, Collect.ACCENT[i], clock, fade, i);
        face(p, i % 3, cx, cy + r * FACE_DY[shape], r * FACE_R[shape], fade);
    }

    // ---- faces --------------------------------------------------------------

    /**
     * One of three faces, chosen off the entry index. Three is enough that a shelf of
     * thirty does not look stamped from one mould, and few enough that they all stay
     * recognisably the same species.
     */
    private static void face(Painter p, int kind, float cx, float cy, float r, float fade) {
        int ink = Draw.fadeBy(INK, fade);
        float ew = r * 0.30f;
        if (kind == 0) {
            for (int s = -1; s <= 1; s += 2) {
                float ex = cx + s * ew;
                p.fillEllipse(ex, cy - r * 0.06f, r * 0.13f, r * 0.17f, ink);
                p.fillEllipse(ex - r * 0.04f, cy - r * 0.12f, r * 0.05f, r * 0.05f,
                        Draw.fadeBy(0xE6FFFFFF, fade));
            }
            p.polyline(new float[] {cx - r * 0.16f, cy + r * 0.24f, cx, cy + r * 0.34f,
                    cx + r * 0.16f, cy + r * 0.24f}, ink, r * 0.07f);
        } else if (kind == 1) {
            for (int s = -1; s <= 1; s += 2) {
                float ex = cx + s * ew;
                p.polyline(new float[] {ex - r * 0.15f, cy + r * 0.02f, ex, cy - r * 0.16f,
                        ex + r * 0.15f, cy + r * 0.02f}, ink, r * 0.08f);
            }
            float w = r * 0.15f;
            p.polyline(new float[] {cx - w * 2, cy + r * 0.24f, cx - w, cy + r * 0.38f,
                    cx, cy + r * 0.24f, cx + w, cy + r * 0.38f, cx + w * 2, cy + r * 0.24f},
                    ink, r * 0.07f);
        } else {
            for (int s = -1; s <= 1; s += 2) {
                float ex = cx + s * ew;
                p.polyline(new float[] {ex + s * r * 0.15f, cy - r * 0.18f,
                        ex - s * r * 0.13f, cy - r * 0.02f, ex + s * r * 0.15f,
                        cy + r * 0.14f}, ink, r * 0.08f);
            }
            p.fillEllipse(cx, cy + r * 0.30f, r * 0.19f, r * 0.13f, ink);
        }
        for (int s = -1; s <= 1; s += 2) {
            p.fillEllipse(cx + s * r * 0.56f, cy + r * 0.10f, r * 0.14f, r * 0.09f,
                    Draw.fadeBy(BLUSH, fade));
        }
    }
}
