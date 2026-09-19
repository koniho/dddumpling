package com.dddumpling.game;

/** Warps the existing mushroom art toward its planted roots, keeping every detail attached. */
final class MushroomDeath implements Painter {
    static final float WITHER_END = .85f, FLATTEN_END = 2.2f, SPREAD_END = 2.9f, FADE_START = 2.95f;
    final Painter p;
    final float cx, ground, radius, shrink, flat, spread, melt, brown, opacity;
    final Layout layout;

    MushroomDeath(Painter p, Boss b, Layout L, float alpha) {
        this.p = p; layout = L;
        radius = Boss.bodyR(L); cx = b.body.homeX;
        ground = b.body.homeY + radius * 3.55f;
        float time = b.leaveProgress() * Boss.LEAVE;
        shrink = ease(time, 0f, WITHER_END);
        brown = ease(time, .12f, WITHER_END);
        flat = ease(time, WITHER_END, FLATTEN_END);
        spread = ease(time, FLATTEN_END, SPREAD_END);
        melt = ease(time, SPREAD_END, Boss.LEAVE);
        opacity = alpha * (1f - ease(time, FADE_START, Boss.LEAVE));
    }

    static float ease(float time, float start, float end) {
        float u = Math.max(0f, Math.min(1f, (time - start) / (end - start)));
        return u * u * (3f - 2f * u);
    }

    float x(float x, float y) {
        float dx = x - cx;
        float scale = y > ground ? 1f - shrink * .76f : 1f - shrink * .32f + flat * .08f;
        float wrinkle = (float) Math.sin((y - ground) / radius * 9f + dx / radius * 4f);
        return centreX() + dx * scale * (1f + spread * .8f)
                * (1f + .055f * shrink * (1f - flat) * wrinkle);
    }

    float centreX() { return cx + (layout.w * .5f - cx) * spread; }

    float y(float y) {
        float dy = y - ground;
        if (dy > 0f) return ground + dy * (1f - shrink * .78f) * (1f - flat * .85f) * (1f - spread * .65f);
        return ground + dy * (1f - shrink * .18f) * (1f - flat * .96f) * (1f - spread * .65f) + radius * .28f * melt;
    }

    int color(int color) {
        int light = (((color >>> 16) & 255) * 54 + ((color >>> 8) & 255) * 183 + (color & 255) * 19) / 256;
        int dry = Glyph.mix(0xFF3E2518, 0xFFAD8855, light / 255f);
        return Glyph.withAlpha(Glyph.mix(color, dry, brown), Math.round((color >>> 24) * opacity));
    }

    float width(float width) { return width * (1f - shrink * .25f) * (1f - flat * .8f); }
    float[] points(float[] raw) {
        float[] result = new float[raw.length];
        for (int i = 0; i < raw.length; i += 2) {
            result[i] = x(raw[i], raw[i + 1]); result[i + 1] = y(raw[i + 1]);
        }
        return result;
    }
    float[] ellipse(float x, float y, float rx, float ry) {
        float[] xy = new float[64];
        for (int i = 0; i < 32; i++) {
            float angle = Softbody.TAU * i / 32;
            xy[i * 2] = x + (float) Math.cos(angle) * rx;
            xy[i * 2 + 1] = y + (float) Math.sin(angle) * ry;
        }
        return xy;
    }

    void groundCover() {
        float amount = ease(flat, .35f, 1f);
        p.fillEllipse(centreX(), ground, radius * (1.2f + amount * .4f) * (1f + spread * 1.8f),
                radius * .075f * (1f - spread * .65f),
                Glyph.withAlpha(0xFF654125, (int) (115 * amount * opacity)));
        // The roots remain visible below ground; the collapsing cap and stalk sink behind it.
        p.save();
        float margin = layout.w * .05f;
        p.clipRect(-margin, -margin, layout.w + margin, ground + radius * .035f);
    }
    void endGroundCover() { p.restore(); }

    public void fillPoly(float[] xy, int c) { p.fillPoly(points(xy), color(c)); }
    public void fillContours(float[][] xy, int c) {
        float[][] mapped = new float[xy.length][];
        for (int i = 0; i < xy.length; i++) mapped[i] = points(xy[i]);
        p.fillContours(mapped, color(c));
    }
    public void strokePoly(float[] xy, int c, float w) { p.strokePoly(points(xy), color(c), width(w)); }
    public void polyline(float[] xy, int c, float w) { p.polyline(points(xy), color(c), width(w)); }
    public void fillCircle(float x, float y, float r, int c) { fillEllipse(x, y, r, r, c); }
    public void strokeCircle(float x, float y, float r, int c, float w) { strokePoly(ellipse(x,y,r,r),c,w); }
    public void fillEllipse(float x, float y, float rx, float ry, int c) { fillPoly(ellipse(x,y,rx,ry),c); }
    public void fillRect(float l, float t, float r, float b, int c) { fillPoly(new float[] {l,t,r,t,r,b,l,b},c); }
    public void line(float x1,float y1,float x2,float y2,int c,float w) {
        p.line(x(x1,y1),y(y1),x(x2,y2),y(y2),color(c),width(w));
    }
    public void arc(float x,float y,float rx,float ry,float start,float sweep,int c,float w) {
        float[] xy = new float[66];
        for (int i = 0; i <= 32; i++) {
            float a = (start + sweep * i / 32f) * Softbody.TAU / 360f;
            xy[i*2] = x + (float)Math.cos(a)*rx; xy[i*2+1] = y + (float)Math.sin(a)*ry;
        }
        polyline(xy,c,w);
    }
    public void text(String s,float x,float y,float size,int c,int align,boolean bold) {
        p.text(s,x(x,y),y(y),width(size),color(c),align,bold);
    }
    public void clipRect(float l,float t,float r,float b) { p.clipRect(x(l,t),y(t),x(r,b),y(b)); }
    public void clipOutCircle(float x,float y,float r) { p.clipOutCircle(x(x,y),y(y),width(r)); }
    public void save() { p.save(); }
    public void restore() { p.restore(); }
    public void translate(float dx,float dy) { p.translate(dx,dy); }
}
