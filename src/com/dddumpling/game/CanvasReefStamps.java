package com.dddumpling.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/** Twelve immutable, softly shaded rock sprites; under three MiB including cast shadows. */
final class CanvasReefStamps {
    private final Bitmap[] images=new Bitmap[Underwater.ROCK_COUNT];
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF bounds=new RectF();
    void draw(Canvas canvas,int art,float x,float y,float w,float h,int alpha) {
        if(images[art]==null) {
            Bitmap image=Bitmap.createBitmap(Underwater.ROCK_WIDTH,Underwater.ROCK_HEIGHT,Bitmap.Config.ARGB_8888);
            CanvasPainter p=new CanvasPainter();p.bind(new Canvas(image));Underwater.paintRock(p,art);
            image.prepareToDraw();images[art]=image;
        }
        paint.setAlpha(Math.max(0,Math.min(255,alpha)));
        bounds.set(x-w*1.3f,y-h*1.35f,x+w*1.8f,y+h*1.6f);
        canvas.drawBitmap(images[art],null,bounds,paint);
    }
}
