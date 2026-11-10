package com.dddumpling.game;

/**
 * The only drawing surface {@link Renderer} knows about. Implemented twice: by
 * CanvasPainter on the device, and by RasterPainter in the offline preview harness.
 * Keeping the scene description behind this seam means the PNGs I inspect while
 * developing come from the same code path the phone runs.
 *
 * Colours are ARGB ints. Alpha is honoured.
 */
interface Painter {
    default int accessory(int who,int slot) {return -1;}
    /** Projected accessory radius as a fraction of viewport width (640-unit standalone previews). */
    default float accessorySize(float radius) {return radius/640f;}
    /** Optional attachment geometry for display-case picking; ordinary drawing skips it. */
    default boolean accessoryTargets() {return false;}
    default void accessoryTarget(int slot,float[] corners) {}
    default int skin() { return Skins.DEFAULT; }
    default DuckBodies ducks() { return null; }
    default GatorCompanion gator() { return null; }
    default ArtStamp stamps() { return null; }

    int LEFT = -1, CENTER = 0, RIGHT = 1;

    void fillPoly(float[] pts, int color);

    /** Fills a compound even-odd vector shape; inner contours cut transparent counters. */
    void fillContours(float[][] contours, int color);

    void strokePoly(float[] pts, int color, float width);

    void fillCircle(float cx, float cy, float r, int color);

    void strokeCircle(float cx, float cy, float r, int color, float width);

    /** Strokes an elliptical arc in screen-space degrees, clockwise from the right edge. */
    void arc(float cx, float cy, float rx, float ry, float start, float sweep, int color, float width);

    void fillEllipse(float cx, float cy, float rx, float ry, int color);

    /** Strokes an open path through the points (x0,y0,x1,y1,...). */
    void polyline(float[] pts, int color, float width);

    /** Connected round segments; hosts may batch these without rebuilding a stroked path. */
    default void fastPolyline(float[] pts,int color,float width) {polyline(pts,color,width);}

    void fillRect(float l, float t, float r, float b, int color);

    /** Continuous vertical color ramp. Native hosts draw one gradient; previews sample rows. */
    default void verticalGradient(float l,float t,float r,float b,int[] colors,float[] stops) {
        if(b<=t || colors.length<2 || colors.length!=stops.length)return;
        int stop=0;
        for(float y=t;y<b;) {
            float next=Math.min(b,(float)Math.floor(y)+1),u=((y+next)*.5f-t)/(b-t);
            while(stop<stops.length-2 && u>stops[stop+1])stop++;
            float mix=Math.max(0,Math.min(1,(u-stops[stop])/(stops[stop+1]-stops[stop])));
            int a=colors[stop],z=colors[stop+1];
            int color=Glyph.withAlpha(Glyph.mix(a,z,mix),Math.round((a>>>24)+((z>>>24)-(a>>>24))*mix));
            fillRect(l,y,r,next,color);y=next;
        }
    }

    void line(float x1, float y1, float x2, float y2, int color, float width);

    /** Draws {@code s} with its baseline at y, aligned about x. */
    void text(String s, float x, float y, float size, int color, int align, boolean bold);

    /**
     * Intersects the current clip with this rectangle. Undone by {@link #restore()}, so it
     * must be paired with a {@link #save()}.
     */
    void clipRect(float l, float t, float r, float b);

    /** Excludes a circular foreground silhouette until restore(). */
    void clipOutCircle(float cx, float cy, float radius);

    void save();

    void restore();

    void translate(float dx, float dy);
}
