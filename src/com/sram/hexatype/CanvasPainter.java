package com.sram.hexatype;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;

/** {@link Painter} backed by an Android {@link Canvas}. */
final class CanvasPainter implements Painter {

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint type = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Typeface regular = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final Typeface heavy = Typeface.create("sans-serif", Typeface.BOLD);

    private Canvas canvas;

    CanvasPainter() {
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        type.setLetterSpacing(0.09f);
    }

    void bind(Canvas c) {
        this.canvas = c;
    }

    private void build(float[] pts) {
        path.reset();
        path.moveTo(pts[0], pts[1]);
        for (int i = 2; i < pts.length; i += 2) path.lineTo(pts[i], pts[i + 1]);
        path.close();
    }

    @Override public void fillPoly(float[] pts, int color) {
        if (pts == null) return;
        build(pts);
        fill.setColor(color);
        canvas.drawPath(path, fill);
    }

    @Override public void strokePoly(float[] pts, int color, float width) {
        if (pts == null) return;
        build(pts);
        stroke.setColor(color);
        stroke.setStrokeWidth(width);
        canvas.drawPath(path, stroke);
    }

    @Override public void fillCircle(float cx, float cy, float r, int color) {
        fill.setColor(color);
        canvas.drawCircle(cx, cy, r, fill);
    }

    @Override public void strokeCircle(float cx, float cy, float r, int color, float width) {
        stroke.setColor(color);
        stroke.setStrokeWidth(width);
        canvas.drawCircle(cx, cy, r, stroke);
    }

    @Override public void fillEllipse(float cx, float cy, float rx, float ry, int color) {
        fill.setColor(color);
        canvas.drawOval(cx - rx, cy - ry, cx + rx, cy + ry, fill);
    }

    @Override public void polyline(float[] pts, int color, float width) {
        if (pts == null || pts.length < 4) return;
        path.reset();
        path.moveTo(pts[0], pts[1]);
        for (int i = 2; i < pts.length; i += 2) path.lineTo(pts[i], pts[i + 1]);
        stroke.setColor(color);
        stroke.setStrokeWidth(width);
        canvas.drawPath(path, stroke);
    }

    @Override public void fillRect(float l, float t, float r, float b, int color) {
        fill.setColor(color);
        canvas.drawRect(l, t, r, b, fill);
    }

    @Override public void line(float x1, float y1, float x2, float y2, int color, float width) {
        stroke.setColor(color);
        stroke.setStrokeWidth(width);
        canvas.drawLine(x1, y1, x2, y2, stroke);
    }

    @Override public void text(String s, float x, float y, float size, int color, int align,
            boolean bold) {
        type.setTypeface(bold ? heavy : regular);
        type.setTextSize(size);
        type.setColor(color);
        type.setTextAlign(align == LEFT ? Paint.Align.LEFT
                : align == RIGHT ? Paint.Align.RIGHT : Paint.Align.CENTER);
        canvas.drawText(s, x, y, type);
    }

    @Override public void save() {
        canvas.save();
    }

    @Override public void restore() {
        canvas.restore();
    }

    @Override public void translate(float dx, float dy) {
        canvas.translate(dx, dy);
    }
}
