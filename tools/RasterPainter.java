package com.dddumpling.game;

/**
 * {@link Painter} backed by a plain int[] framebuffer, so frames can be rendered off-device
 * and written out as PNGs. Antialiasing comes from supersampling the whole buffer, which
 * keeps every primitive (including text) consistent without per-shape coverage maths.
 */
final class RasterPainter implements Painter {

    private final int ss;
    private final int w, h;      // logical size
    private final int bw, bh;    // buffer size
    private final int[] buf;

    private float tx, ty;
    /**
     * Cap height as a fraction of the type size — how far a line of text reaches above its
     * baseline. Named because it is the number any check on stacked text has to agree with: a gap
     * smaller than this times the lower line's size means the two collide.
     */
    static final float CAP = 0.72f;

    /** Clip in buffer pixels, inclusive. */
    private int clipL, clipT, clipR, clipB;
    /** Six slots per save(): tx, ty and the four clip edges. */
    private final float[] stack = new float[6 * 32];
    private int sp;

    RasterPainter(int w, int h, int ss) {
        this.w = w;
        this.h = h;
        this.ss = ss;
        this.bw = w * ss;
        this.bh = h * ss;
        this.buf = new int[bw * bh];
        clipL = 0;
        clipT = 0;
        clipR = bw - 1;
        clipB = bh - 1;
    }

    int logicalWidth() { return w; }

    int logicalHeight() { return h; }

    void clear(int color) {
        for (int i = 0; i < buf.length; i++) buf[i] = color;
    }

    /** Box-filters the supersampled buffer down to logical resolution. */
    int[] resolve() {
        int[] out = new int[w * h];
        int n = ss * ss;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = 0, g = 0, b = 0;
                for (int dy = 0; dy < ss; dy++) {
                    int row = (y * ss + dy) * bw + x * ss;
                    for (int dx = 0; dx < ss; dx++) {
                        int c = buf[row + dx];
                        r += (c >> 16) & 0xFF;
                        g += (c >> 8) & 0xFF;
                        b += c & 0xFF;
                    }
                }
                out[y * w + x] = 0xFF000000 | ((r / n) << 16) | ((g / n) << 8) | (b / n);
            }
        }
        return out;
    }

    // ---- pixel plumbing -----------------------------------------------------

    private void blend(int x, int y, int color, float cov) {
        if (x < clipL || y < clipT || x > clipR || y > clipB || cov <= 0) return;
        float a = ((color >>> 24) / 255f) * (cov > 1 ? 1 : cov);
        if (a <= 0.0015f) return;
        int i = y * bw + x;
        int d = buf[i];
        int dr = (d >> 16) & 0xFF, dg = (d >> 8) & 0xFF, db = d & 0xFF;
        int sr = (color >> 16) & 0xFF, sg = (color >> 8) & 0xFF, sb = color & 0xFF;
        int r = (int) (sr * a + dr * (1 - a));
        int g = (int) (sg * a + dg * (1 - a));
        int b = (int) (sb * a + db * (1 - a));
        buf[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private void span(int y, int xa, int xb, int color) {
        if (y < 0 || y >= bh) return;
        if (xa < 0) xa = 0;
        if (xb > bw - 1) xb = bw - 1;
        for (int x = xa; x <= xb; x++) blend(x, y, color, 1f);
    }

    private float sx(float x) { return (x + tx) * ss; }

    private float sy(float y) { return (y + ty) * ss; }

    // ---- Painter ------------------------------------------------------------

    @Override public void fillPoly(float[] pts, int color) {
        if (pts == null || pts.length < 6 || (color >>> 24) == 0) return;
        int n = pts.length / 2;
        float[] px = new float[n], py = new float[n];
        float ymin = Float.MAX_VALUE, ymax = -Float.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            px[i] = sx(pts[i * 2]);
            py[i] = sy(pts[i * 2 + 1]);
            if (py[i] < ymin) ymin = py[i];
            if (py[i] > ymax) ymax = py[i];
        }
        int y0 = Math.max(0, (int) Math.floor(ymin));
        int y1 = Math.min(bh - 1, (int) Math.ceil(ymax));
        float[] xs = new float[n];
        for (int y = y0; y <= y1; y++) {
            float yc = y + 0.5f;
            int cnt = 0;
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                float ya = py[i], yb = py[j];
                if ((yc >= ya && yc < yb) || (yc >= yb && yc < ya)) {
                    float t = (yc - ya) / (yb - ya);
                    xs[cnt++] = px[i] + t * (px[j] - px[i]);
                }
            }
            if (cnt < 2) continue;
            for (int a = 1; a < cnt; a++) {           // insertion sort; cnt is tiny
                float v = xs[a];
                int b = a - 1;
                while (b >= 0 && xs[b] > v) {
                    xs[b + 1] = xs[b];
                    b--;
                }
                xs[b + 1] = v;
            }
            for (int k = 0; k + 1 < cnt; k += 2) {
                span(y, Math.round(xs[k]), Math.round(xs[k + 1]) - 1, color);
            }
        }
    }

    @Override public void strokePoly(float[] pts, int color, float width) {
        if (pts == null || pts.length < 6) return;
        int n = pts.length / 2;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            line(pts[i * 2], pts[i * 2 + 1], pts[j * 2], pts[j * 2 + 1], color, width);
        }
    }

    @Override public void fillContours(float[][] contours, int color) {
        if (contours == null || (color >>> 24) == 0) return;
        int edges = 0;
        float ymin = Float.MAX_VALUE, ymax = -Float.MAX_VALUE;
        for (float[] pts : contours) {
            if (pts == null) continue;
            edges += pts.length / 2;
            for (int i = 1; i < pts.length; i += 2) {
                float y = sy(pts[i]); ymin = Math.min(ymin, y); ymax = Math.max(ymax, y);
            }
        }
        if (edges < 3) return;
        float[] xs = new float[edges];
        int y0 = Math.max(0, (int) Math.floor(ymin));
        int y1 = Math.min(bh - 1, (int) Math.ceil(ymax));
        for (int y = y0; y <= y1; y++) {
            float yc = y + 0.5f; int cnt = 0;
            for (float[] pts : contours) {
                if (pts == null || pts.length < 6) continue;
                int n = pts.length / 2;
                for (int i = 0; i < n; i++) {
                    int j = (i + 1) % n;
                    float ya = sy(pts[i * 2 + 1]), yb = sy(pts[j * 2 + 1]);
                    if ((yc >= ya && yc < yb) || (yc >= yb && yc < ya)) {
                        float t = (yc - ya) / (yb - ya);
                        xs[cnt++] = sx(pts[i * 2]) + t * (sx(pts[j * 2]) - sx(pts[i * 2]));
                    }
                }
            }
            for (int a = 1; a < cnt; a++) {
                float v = xs[a]; int b = a - 1;
                while (b >= 0 && xs[b] > v) { xs[b + 1] = xs[b]; b--; }
                xs[b + 1] = v;
            }
            for (int k = 0; k + 1 < cnt; k += 2)
                span(y, Math.round(xs[k]), Math.round(xs[k + 1]) - 1, color);
        }
    }

    @Override public void arc(float cx, float cy, float rx, float ry, float start, float sweep,
            int color, float width) {
        int steps = Math.max(8, (int) (Math.abs(sweep) / 8f));
        float[] pts = new float[(steps + 1) * 2];
        for (int i = 0; i <= steps; i++) {
            double a = Math.toRadians(start + sweep * i / steps);
            pts[i * 2] = cx + rx * (float) Math.cos(a);
            pts[i * 2 + 1] = cy + ry * (float) Math.sin(a);
        }
        polyline(pts, color, width);
    }

    @Override public void fillCircle(float cx, float cy, float r, int color) {
        if ((color >>> 24) == 0 || r <= 0) return;
        float bx = sx(cx), by = sy(cy), br = r * ss;
        int x0 = Math.max(0, (int) Math.floor(bx - br - 1));
        int x1 = Math.min(bw - 1, (int) Math.ceil(bx + br + 1));
        int y0 = Math.max(0, (int) Math.floor(by - br - 1));
        int y1 = Math.min(bh - 1, (int) Math.ceil(by + br + 1));
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                float dx = x + 0.5f - bx, dy = y + 0.5f - by;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                blend(x, y, color, br + 0.5f - d);
            }
        }
    }

    @Override public void strokeCircle(float cx, float cy, float r, int color, float width) {
        float hw = width / 2f;
        int steps = Math.max(12, (int) (r * ss * 0.5f));
        float px = cx + r, py = cy;
        for (int i = 1; i <= steps; i++) {
            double a = 2 * Math.PI * i / steps;
            float qx = cx + r * (float) Math.cos(a), qy = cy + r * (float) Math.sin(a);
            line(px, py, qx, qy, color, hw * 2);
            px = qx;
            py = qy;
        }
    }

    @Override public void fillEllipse(float cx, float cy, float rx, float ry, int color) {
        if ((color >>> 24) == 0 || rx <= 0 || ry <= 0) return;
        float bx = sx(cx), by = sy(cy), brx = rx * ss, bry = ry * ss;
        int x0 = Math.max(0, (int) Math.floor(bx - brx - 1));
        int x1 = Math.min(bw - 1, (int) Math.ceil(bx + brx + 1));
        int y0 = Math.max(0, (int) Math.floor(by - bry - 1));
        int y1 = Math.min(bh - 1, (int) Math.ceil(by + bry + 1));
        float scale = Math.min(brx, bry);
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                float dx = (x + 0.5f - bx) / brx, dy = (y + 0.5f - by) / bry;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                // Convert normalised distance back to pixels for a 1px soft edge.
                blend(x, y, color, (1f - d) * scale + 0.5f);
            }
        }
    }

    @Override public void polyline(float[] pts, int color, float width) {
        if (pts == null || pts.length < 4) return;
        for (int i = 0; i + 3 < pts.length; i += 2) {
            line(pts[i], pts[i + 1], pts[i + 2], pts[i + 3], color, width);
        }
    }

    @Override public void fillRect(float l, float t, float r, float b, int color) {
        fillPoly(new float[] {l, t, r, t, r, b, l, b}, color);
    }

    @Override public void line(float x1, float y1, float x2, float y2, int color, float width) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float hw = Math.max(width, 0.4f) / 2f;
        if (len < 1e-4f) {
            fillCircle(x1, y1, hw, color);
            return;
        }
        float nx = -dy / len * hw, ny = dx / len * hw;
        fillPoly(new float[] {x1 + nx, y1 + ny, x2 + nx, y2 + ny, x2 - nx, y2 - ny, x1 - nx,
                y1 - ny}, color);
        fillCircle(x1, y1, hw, color);   // round joins/caps, matching the device paint
        fillCircle(x2, y2, hw, color);
    }

    /**
     * Every line drawn wider than the screen, or off either edge, since the last {@link #clearFit}.
     *
     * Text that does not fit is not a rendering bug you can see in a PNG — it is a clipped word, or
     * a line that reads fine here and wraps off a narrower device. Recorded rather than eyeballed
     * because there are dozens of lines across twenty screens. This bitmap approximation does
     * not measure Bungee; device text still needs a visual check.
     */
    static final java.util.List<String> unfit = new java.util.ArrayList<>();

    static void clearFit() {
        unfit.clear();
    }

    /**
     * How wide a line comes out in the harness font, for assertions about text fitting inside
     * something that is not the screen edge — {@code DOES NOT FIT} only reports the edge, and a
     * label leaving its box has now been drawn twice without anything catching it. These widths
     * describe the harness bitmap font, not the bundled Bungee face.
     */
    static float textWidth(String s, float size) {
        if (s == null || s.isEmpty()) return 0f;
        float advance = size * CAP / 7f * 6f + size * 0.09f;
        return s.length() * advance - size * 0.09f;
    }

    @Override public void text(String s, float x, float y, float size, int color, int align,
            boolean bold) {
        if (s == null || s.isEmpty() || (color >>> 24) == 0) return;
        float cell = size * CAP;
        float px = cell / 7f;
        float advance = px * 6f + size * 0.09f;
        float total = textWidth(s, size);
        float left = align == LEFT ? x : align == RIGHT ? x - total : x - total / 2f;
        float top = y - cell;
        // Logical coordinates: the supersampling happens further down, in fillRect. Text outside
        // the current clip is not reported — the display case deliberately draws a wing tile past
        // the plaque edge for the clip to cut off, and its question mark is off screen by design.
        float clipLo = clipL / (float) ss - tx;
        float clipHi = (clipR + 1) / (float) ss - tx;
        boolean visible = left < clipHi && left + total > clipLo;
        if (visible && (left < 0f || left + total > w)) {
            unfit.add(String.format("%-46s %5.0fpx wide, x %.0f..%.0f of %d",
                    '"' + s + '"', total, left, left + total, w));
        }
        for (int i = 0; i < s.length(); i++) {
            drawChar(s.charAt(i), left + i * advance, top, px, color, bold);
        }
    }

    private void drawChar(char ch, float x, float y, float px, int color, boolean bold) {
        String[] rows = Font.rows(ch);
        if (rows == null) return;
        float fat = bold ? px * 0.35f : 0f;
        for (int r = 0; r < 7; r++) {
            String row = rows[r];
            int c = 0;
            while (c < 5) {
                if (row.charAt(c) != '#') {
                    c++;
                    continue;
                }
                int start = c;
                while (c < 5 && row.charAt(c) == '#') c++;
                fillRect(x + start * px - fat, y + r * px - fat * 0.5f,
                        x + c * px + fat, y + (r + 1) * px + fat * 0.5f, color);
            }
        }
    }

    @Override public void clipRect(float l, float t, float r, float b) {
        // Intersects, like Canvas.clipRect, and respects the current translate.
        clipL = Math.max(clipL, (int) Math.floor(sx(l)));
        clipT = Math.max(clipT, (int) Math.floor(sy(t)));
        clipR = Math.min(clipR, (int) Math.ceil(sx(r)) - 1);
        clipB = Math.min(clipB, (int) Math.ceil(sy(b)) - 1);
    }

    @Override public void save() {
        stack[sp++] = tx;
        stack[sp++] = ty;
        stack[sp++] = clipL;
        stack[sp++] = clipT;
        stack[sp++] = clipR;
        stack[sp++] = clipB;
    }

    @Override public void restore() {
        clipB = (int) stack[--sp];
        clipR = (int) stack[--sp];
        clipT = (int) stack[--sp];
        clipL = (int) stack[--sp];
        ty = stack[--sp];
        tx = stack[--sp];
    }

    @Override public void translate(float dx, float dy) {
        tx += dx;
        ty += dy;
    }
}
