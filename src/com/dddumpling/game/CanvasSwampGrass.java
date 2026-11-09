package com.dddumpling.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.RectF;

/** Four 128x256 reed images: 512 KiB total, independent of frame or travel time. */
final class CanvasSwampGrass {
    private final Bitmap[] images=new Bitmap[SwampGrass.COUNT];
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF bounds=new RectF();
    void warm(){for(int art=0;art<images.length;art++)image(art);}
    private Bitmap image(int art) {
        if(images[art]!=null)return images[art];
        Bitmap bitmap=Bitmap.createBitmap(SwampGrass.WIDTH,SwampGrass.HEIGHT,Bitmap.Config.ARGB_8888);
        CanvasPainter p=new CanvasPainter();p.bind(new Canvas(bitmap));
        SwampGrass.paint(p,art,SwampGrass.ANCHOR_X,SwampGrass.ANCHOR_Y,SwampGrass.UNIT,0,1);
        bitmap.prepareToDraw();return images[art]=bitmap;
    }
    void draw(Canvas canvas,int art,float x,float y,float r,float sway,int alpha,ColorFilter filter) {
        float unit=r/SwampGrass.UNIT;
        bounds.set(-SwampGrass.ANCHOR_X*unit,-SwampGrass.ANCHOR_Y*unit,
                (SwampGrass.WIDTH-SwampGrass.ANCHOR_X)*unit,(SwampGrass.HEIGHT-SwampGrass.ANCHOR_Y)*unit);
        paint.setAlpha(Math.max(0,Math.min(255,alpha)));paint.setColorFilter(filter);
        canvas.save();canvas.translate(x,y);canvas.skew(-sway,0);
        canvas.drawBitmap(image(art),null,bounds,paint);canvas.restore();
    }
}
