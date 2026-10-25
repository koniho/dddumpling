package com.dddumpling.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/** Three immutable crystal edge layers, 5.3 MiB total; shimmer and impact light remain live. */
final class CanvasCrystalStamps {
    private final Bitmap[] images=new Bitmap[CrystalField.LAYERS];
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF bounds=new RectF();
    void warm(){for(int i=0;i<images.length;i++)image(i);}
    private Bitmap image(int layer) {
        if(images[layer]!=null)return images[layer];
        Bitmap b=Bitmap.createBitmap(CrystalField.STAMP_WIDTH,CrystalField.STAMP_HEIGHT,Bitmap.Config.ARGB_8888);
        CanvasPainter p=new CanvasPainter();p.bind(new Canvas(b));CrystalField.paintLayer(p,layer);
        b.prepareToDraw();images[layer]=b;return b;
    }
    void draw(Canvas canvas,int layer,float x,float y,float w,float h,int alpha) {
        paint.setAlpha(Math.max(0,Math.min(255,alpha)));bounds.set(x,y,x+w,y+h);
        canvas.drawBitmap(image(layer),null,bounds,paint);
    }
}
