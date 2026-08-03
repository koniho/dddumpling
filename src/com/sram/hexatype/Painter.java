package com.sram.hexatype;

/**
 * The only drawing surface {@link Renderer} knows about. Implemented twice: by
 * CanvasPainter on the device, and by RasterPainter in the offline preview harness.
 * Keeping the scene description behind this seam means the PNGs I inspect while
 * developing come from the same code path the phone runs.
 *
 * Colours are ARGB ints. Alpha is honoured.
 */
interface Painter {
    int LEFT = -1, CENTER = 0, RIGHT = 1;

    void fillPoly(float[] pts, int color);

    void strokePoly(float[] pts, int color, float width);

    void fillCircle(float cx, float cy, float r, int color);

    void strokeCircle(float cx, float cy, float r, int color, float width);

    void fillEllipse(float cx, float cy, float rx, float ry, int color);

    /** Strokes an open path through the points (x0,y0,x1,y1,...). */
    void polyline(float[] pts, int color, float width);

    void fillRect(float l, float t, float r, float b, int color);

    void line(float x1, float y1, float x2, float y2, int color, float width);

    /** Draws {@code s} with its baseline at y, aligned about x. */
    void text(String s, float x, float y, float size, int color, int align, boolean bold);

    void save();

    void restore();

    void translate(float dx, float dy);
}
