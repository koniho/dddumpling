package com.dddumpling.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.RectF;

/** Six bounded foliage images (1.5 MiB), generated once; positions and sway remain live. */
final class CanvasForestStamps {
    static final int COUNT=6;
    private static final int SIDE=256;
    private static final float UNIT=SIDE/5f;
    private final Bitmap[] images=new Bitmap[COUNT];
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF bounds=new RectF();
    private final float[] values=new float[20];
    private final ColorMatrix matrix=new ColorMatrix();
    // A small bounded cache covers the static palettes; event tints may replace old entries.
    private final int[] colors=new int[16];
    private final ColorMatrixColorFilter[] filters=new ColorMatrixColorFilter[16];
    private int next;

    void warm() {for(int art=0;art<COUNT;art++)image(art);}
    private Bitmap image(int art) {
        if(images[art]!=null)return images[art];
        Bitmap image=Bitmap.createBitmap(SIDE,SIDE,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(image);canvas.translate(SIDE*.5f,SIDE*.5f);
        CanvasPainter painter=new CanvasPainter();painter.bind(canvas);
        MushroomForest.stampArt(painter,art,UNIT);
        image.prepareToDraw();images[art]=image;return image;
    }
    private ColorMatrixColorFilter tint(int color) {
        if(color==0 || color==0xFF71917A)return null;
        for(int i=0;i<filters.length;i++)if(filters[i]!=null && colors[i]==color)return filters[i];
        java.util.Arrays.fill(values,0);
        values[0]=((color>>>16)&255)/113f;values[6]=((color>>>8)&255)/145f;
        values[12]=(color&255)/122f;values[18]=1;
        matrix.set(values);
        int slot=next++%filters.length;colors[slot]=color;
        return filters[slot]=new ColorMatrixColorFilter(matrix);
    }
    void draw(Canvas canvas,int art,float x,float y,float r,float lean,int color,int alpha) {
        paint.setAlpha(Math.max(0,Math.min(255,alpha)));paint.setColorFilter(tint(color));
        bounds.set(-r*2.5f,-r*2.5f,r*2.5f,r*2.5f);
        canvas.save();canvas.translate(x,y);
        if(art<3) {
            float side=lean<0?-1:1;
            canvas.scale(side,1);
            canvas.rotate((Math.abs(lean)-1)*29f);
        } else canvas.rotate((lean-MushroomForest.litterAngle(art))*180/(float)Math.PI);
        canvas.drawBitmap(image(art),null,bounds,paint);canvas.restore();
    }
}
