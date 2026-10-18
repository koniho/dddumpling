package com.dddumpling.game;

import java.io.File;

/** Review-only artwork: deliberately absent from src/ and every game build source list.
 * Compile against build/harness into a separate output directory, then run this main.
 */
final class HalloweenGeometricPreview extends Draw {
    private static final int FACE=0xFF392C4F;
    private static final float[][] BODY={
        {-.76f,.73f,-.76f,-.32f,-.43f,-.80f,.43f,-.80f,.76f,-.32f,.76f,.73f,.38f,.48f,0,.78f,-.38f,.48f},
        {-.70f,-.70f,-.28f,-.47f,.28f,-.47f,.70f,-.70f,.70f,.37f,.40f,.68f,-.40f,.68f,-.70f,.37f},
        {-.48f,-.72f,.48f,-.72f,.84f,-.32f,.84f,.32f,.48f,.72f,-.48f,.72f,-.84f,.32f,-.84f,-.32f},
        {-.65f,-.45f,.65f,-.45f,.77f,.22f,.42f,.72f,-.42f,.72f,-.77f,.22f},
        {-.52f,-.73f,.52f,-.73f,.76f,-.48f,.76f,.48f,.52f,.71f,-.52f,.71f,-.76f,.48f,-.76f,-.48f},
        {-.74f,-.78f,-.26f,-.47f,.26f,-.47f,.74f,-.78f,.74f,.32f,.42f,.68f,-.42f,.68f,-.74f,.32f}
    };
    private static float[] points(float x,float y,float rx,float ry,float... coords) {
        float[] p=coords.clone();
        for(int i=0;i<p.length;i+=2){p[i]=x+p[i]*rx;p[i+1]=y+p[i+1]*ry;}
        return p;
    }
    private static void poly(Painter p,float x,float y,float r,int color,float... coords) {
        p.fillPoly(points(x,y,r,r,coords),color);
    }
    static void draw(Painter p,int g,float x,float y,float r) {
        int base=Skins.HALLOWEEN_COLORS[g];
        if(g==1)for(int side=-1;side<=1;side+=2)
            p.fillPoly(points(x,y,r*side,r,.55f,-.12f,1.29f,-.49f,1.17f,.20f,.92f,.02f,.73f,.40f,.53f,.15f),0xFFD64F69);
        if(g==4)for(int side=-1;side<=1;side+=2) {
            p.fillRect(x+r*(side<0?-.98f:.70f),y+r*.06f,x+r*(side<0?-.70f:.98f),y+r*.23f,0xFFC4B3D7);
            p.fillRect(x+r*(side<0?-1.02f:.91f),y-r*.01f,x+r*(side<0?-.91f:1.02f),y+r*.30f,0xFFE5DDF0);
        }
        p.fillPoly(points(x,y,r,r,BODY[g]),base);
        // A broad, offset face plane gives depth without thin edge strokes.
        p.fillPoly(points(x-r*.045f,y-r*.065f,r*.87f,r*.86f,BODY[g]),Glyph.mix(base,0xFFFFFFFF,g==5?.23f:.15f));
        if(g==2)poly(p,x,y,r,0xFF68945A,-.08f,-.68f,-.13f,-.98f,.09f,-1.03f,.05f,-.68f);
        if(g==3) {
            poly(p,x,y,r,0xFF5B3D7E,-.91f,-.42f,-.42f,-.61f,-.13f,-1.30f,.35f,-1.19f,.13f,-1.03f,.43f,-.61f,.91f,-.42f);
            poly(p,x,y,r,0xFFF2AE67,-.36f,-.70f,.35f,-.70f,.43f,-.57f,-.42f,-.57f);
        }
        if(g==4)poly(p,x,y,r,0xFF454554,-.76f,-.40f,-.52f,-.73f,.52f,-.73f,.76f,-.40f,.34f,-.40f,.16f,-.22f,-.02f,-.40f,-.28f,-.27f,-.43f,-.40f);
        if(g==5)for(int side=-1;side<=1;side+=2)
            p.fillPoly(points(x,y,r*side,r,.39f,-.43f,.62f,-.59f,.62f,-.31f),0xFFD79FBE);
        int ink=g==5?0xFFFFE9AF:FACE;
        for(int side=-1;side<=1;side+=2) {
            float ex=x+side*r*.28f,ey=y+r*.035f;
            if(g==2)poly(p,ex,ey,r,ink,-.17f,.13f,0,-.17f,.17f,.13f);
            else {
                p.fillEllipse(ex,ey,r*.15f,r*.19f,ink);
                p.fillCircle(ex-r*.035f,ey-r*.06f,r*.045f,g==5?FACE:0xFFFFFFFF);
            }
            p.fillEllipse(x+side*r*.46f,y+r*.26f,r*.12f,r*.075f,0xB8F495B6);
        }
        if(g==2) {
            poly(p,x,y,r,ink,-.27f,.31f,-.12f,.42f,.12f,.42f,.27f,.31f,.15f,.54f,-.15f,.54f);
        } else if(g==5) {
            poly(p,x,y,r,0xFFF4B1CB,-.085f,.25f,.085f,.25f,0,.34f);
            p.polyline(points(x,y,r,r,-.17f,.39f,-.09f,.45f,0,.35f,.09f,.45f,.17f,.39f),ink,r*.045f);
            for(int side=-1;side<=1;side+=2)p.line(x+side*r*.52f,y+r*.26f,x+side*r*.91f,y+r*.29f,0xFFE4D6EA,r*.05f);
        } else {
            p.polyline(points(x,y,r,r,-.15f,.31f,-.07f,.42f,.07f,.42f,.15f,.31f),ink,r*.05f);
            if(g==1)for(int side=-1;side<=1;side+=2)
                p.fillPoly(points(x,y,r*side,r,.055f,.35f,.17f,.35f,.11f,.49f),0xFFFFE9EA);
        }
    }
    public static void main(String[] args) throws Exception {
        int w=760,h=1600;
        RasterPainter p=new RasterPainter(w,h,2);p.clear(BG);
        p.text("HALLOWEEN / GEOMETRIC",w*.5f,64,32,INK,Painter.CENTER,true);
        p.text("ART PROTOTYPE - NOT IN GAME",w*.5f,101,17,INK_DIM,Painter.CENTER,false);
        p.text("CURRENT",145,163,20,INK_DIM,Painter.CENTER,true);
        p.text("GEOMETRIC",430,163,20,GOLD,Painter.CENTER,true);
        p.text("PROMPT",650,163,17,INK_DIM,Painter.CENTER,true);
        Painter current=Skins.painter(p,Skins.HALLOWEEN);
        for(int g=0;g<6;g++) {
            float y=255+g*220;
            Kawaii.draw(current,g,145,y,65,Skins.HALLOWEEN_COLORS[g],1,.4f);
            draw(p,g,430,y,80);
            int color=Skins.HALLOWEEN_COLORS[g];
            p.fillPoly(Glyph.hex(650,y,43),Glyph.withAlpha(color,40));
            p.strokePoly(Glyph.hex(650,y,43),Glyph.withAlpha(color,200),3);
            draw(p,g,650,y,28);
            p.text(Skins.HALLOWEEN_NAMES[g],430,y+104,19,INK,Painter.CENTER,true);
        }
        Png.write(new File(args[0]),p.resolve(),w,h);
    }
}
