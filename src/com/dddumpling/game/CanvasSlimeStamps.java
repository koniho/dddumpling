package com.dddumpling.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/** Five immutable images, 4.7 MiB total; movement and lighting only change placement/alpha. */
final class CanvasSlimeStamps {
    private final Bitmap[] images=new Bitmap[SlimeBasinArt.COUNT];
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF bounds=new RectF();
    private final float[] mesh=new float[(SlimeBanks.ROWS+1)*4];
    void warm() {for(int art=0;art<images.length;art++)image(art);}
    private Bitmap image(int art) {
        if(images[art]!=null)return images[art];
        int w=SlimeBasinArt.width(art),h=SlimeBasinArt.height(art);
        Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        CanvasPainter p=new CanvasPainter();p.bind(new Canvas(bitmap));
        SlimeBasinArt.paint(p,art,w,h);bitmap.prepareToDraw();images[art]=bitmap;return bitmap;
    }
    void mesh(Canvas canvas,int art,float x,float y,float w,float h,float[] offsets,int alpha) {
        boolean left=art==SlimeBasinArt.LEFT_BANK;
        for(int row=0;row<=SlimeBanks.ROWS;row++) {
            float yy=y+h*row/SlimeBanks.ROWS;
            mesh[row*4]=x-(left?0:offsets[row]);mesh[row*4+1]=yy;
            mesh[row*4+2]=x+w+(left?offsets[row]:0);mesh[row*4+3]=yy;
        }
        paint.setAlpha(Math.max(0,Math.min(255,alpha)));
        canvas.drawBitmapMesh(image(art),1,SlimeBanks.ROWS,mesh,0,null,0,paint);
    }
    void draw(Canvas canvas,int art,float x,float y,float w,float h,int alpha) {
        paint.setAlpha(Math.max(0,Math.min(255,alpha)));bounds.set(x,y,x+w,y+h);
        canvas.drawBitmap(image(art),null,bounds,paint);
    }
}
