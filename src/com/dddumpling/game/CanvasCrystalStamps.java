package com.dddumpling.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/** Three stage textures and three current arena textures; obsolete arena phases are released. */
final class CanvasCrystalStamps {
    private final Bitmap[] images=new Bitmap[CrystalField.LAYERS];
    private final Bitmap[] arena=new Bitmap[CrystalField.LAYERS];
    private final int[] arenaArt=new int[CrystalField.LAYERS];
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF bounds=new RectF();
    void warm(){for(int i=0;i<images.length;i++)image(i);}
    private Bitmap image(int layer) {
        int slot=layer%CrystalField.LAYERS;
        if(layer<CrystalField.LAYERS && images[slot]!=null)return images[slot];
        if(layer>=CrystalField.LAYERS && arena[slot]!=null && arenaArt[slot]==layer)return arena[slot];
        Bitmap b=Bitmap.createBitmap(CrystalField.STAMP_WIDTH,CrystalField.STAMP_HEIGHT,Bitmap.Config.ARGB_8888);
        CanvasPainter p=new CanvasPainter();p.bind(new Canvas(b));CrystalField.paintLayer(p,layer);
        b.prepareToDraw();
        if(layer<CrystalField.LAYERS)images[slot]=b;
        else {arena[slot]=b;arenaArt[slot]=layer;}
        return b;
    }
    void draw(Canvas canvas,int layer,float x,float y,float w,float h,int alpha) {
        paint.setAlpha(Math.max(0,Math.min(255,alpha)));bounds.set(x,y,x+w,y+h);
        canvas.drawBitmap(image(layer),null,bounds,paint);
    }
}
