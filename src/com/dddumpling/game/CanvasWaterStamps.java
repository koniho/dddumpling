package com.dddumpling.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/** A tiny Gaussian strip and glow replace repeated translucent full-area geometry. */
final class CanvasWaterStamps {
    private Bitmap band,glow;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF rect=new RectF();
    private float[] mesh=new float[36];
    private Bitmap image(boolean strip) {
        Bitmap cached=strip?band:glow;if(cached!=null)return cached;
        int width=strip?2:Underwater.WATER_TEXTURE,height=Underwater.WATER_TEXTURE;
        int[] pixels=new int[width*height];
        for(int y=0;y<height;y++)for(int x=0;x<width;x++)pixels[y*width+x]=Underwater.waterPixel(x,y,strip);
        cached=Bitmap.createBitmap(pixels,width,height,Bitmap.Config.ARGB_8888);cached.prepareToDraw();
        if(strip)band=cached;else glow=cached;return cached;
    }
    void band(Canvas canvas,float[] xy,float radius,int alpha) {
        if(mesh.length!=xy.length*2)mesh=new float[xy.length*2];
        for(int i=0;i<xy.length;i+=2) {
            mesh[i]=mesh[i+xy.length]=xy[i];
            mesh[i+1]=xy[i+1]-radius;mesh[i+xy.length+1]=xy[i+1]+radius;
        }
        paint.setAlpha(Math.max(0,Math.min(255,alpha)));
        canvas.drawBitmapMesh(image(true),xy.length/2-1,1,mesh,0,null,0,paint);
    }
    void glow(Canvas canvas,float x,float y,float rx,float ry,int alpha) {
        paint.setAlpha(Math.max(0,Math.min(255,alpha)));rect.set(x-rx,y-ry,x+rx,y+ry);
        canvas.drawBitmap(image(false),null,rect,paint);
    }
}
