package com.dddumpling.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.RectF;

/** Two bounded textures, generated once; pose and attack tint never rebuild them. */
final class CanvasArtStamps {
    private static final int SIDE=384;
    private static final float EXTENT=1.25f, UNIT=SIDE/(EXTENT*2);
    private final Bitmap[] images=new Bitmap[ArtStamp.COUNT];
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF bounds=new RectF();
    private final float[] values=new float[20];
    private final ColorMatrix matrix=new ColorMatrix();
    private int priorTint;
    private float priorAmount=-1,priorPhase=-1;

    void warm() {for(int art=0;art<images.length;art++)image(art);}
    private Bitmap image(int art) {
        if(images[art]!=null)return images[art];
        Bitmap bitmap=Bitmap.createBitmap(SIDE,SIDE,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap);canvas.translate(SIDE*.5f,SIDE*.5f);
        CanvasPainter painter=new CanvasPainter();painter.bind(canvas);
        AlligatorArt.stampArt(painter,art,UNIT);
        bitmap.prepareToDraw();images[art]=bitmap;return bitmap;
    }
    void draw(Canvas canvas,int art,float x,float y,float w,float h,float angle,int tint,float amount,float phase) {
        if(amount!=priorAmount || tint!=priorTint || phase!=priorPhase) {
            if(amount==0 && phase==0)paint.setColorFilter(null);
            else {
                GatorPalette.matrix(values,tint,amount,phase);
                matrix.set(values);paint.setColorFilter(new ColorMatrixColorFilter(matrix));
            }
            priorAmount=amount;priorTint=tint;priorPhase=phase;
        }
        bounds.set(-w*EXTENT,-h*EXTENT,w*EXTENT,h*EXTENT);
        canvas.save();canvas.translate(x,y);canvas.rotate(angle*180/(float)Math.PI);
        canvas.drawBitmap(image(art),null,bounds,paint);canvas.restore();
    }
}
