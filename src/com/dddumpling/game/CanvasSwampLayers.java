package com.dddumpling.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.RectF;

/** Seventeen cropped tiles at at most 512 pixels per viewport width; replaces on resize. */
final class CanvasSwampLayers {
    private final Bitmap[] images=new Bitmap[SwampLayers.COUNT];
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF bounds=new RectF();
    private final CanvasPainter painter=new CanvasPainter();
    private float width,height,deckTop;
    void draw(Canvas canvas,int art,float w,float h,float deck,float shift,int alpha,ColorFilter filter) {
        if(width!=w || height!=h || deckTop!=deck) {
            // Let the renderer retire images referenced by an in-flight frame before collection.
            java.util.Arrays.fill(images,null);width=w;height=h;deckTop=deck;
        }
        float left=SwampLayers.left(art,w),top=SwampLayers.top(art,h,w);
        float right=left+SwampLayers.width(art,w),bottom=SwampLayers.bottom(art,h,w,deck);
        if(images[art]==null) {
            float scale=Math.min(1,512f/w);
            Bitmap image=Bitmap.createBitmap((int)Math.ceil((right-left)*scale),(int)Math.ceil((bottom-top)*scale),Bitmap.Config.ARGB_8888);
            Canvas target=new Canvas(image);
            target.scale(image.getWidth()/(right-left),image.getHeight()/(bottom-top));target.translate(-left,-top);
            painter.bind(target);painter.offscreenGrass=true;
            SwampLayers.paint(painter,art,w,h,deck);
            image.prepareToDraw();images[art]=image;
        }
        bounds.set(left+shift,top,right+shift,bottom);
        paint.setAlpha(alpha);paint.setColorFilter(filter);
        canvas.drawBitmap(images[art],null,bounds,paint);
    }
}
