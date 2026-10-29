package com.dddumpling.game;

/** Bold silhouettes at gameplay size; shaped lenses and bevels in close-ups. */
final class AccessoryEyewear {
    private static final float[] HEART={0,-.48f,-.22f,-.82f,-.48f,-.99f,-.72f,-.95f,-.92f,-.72f,-1,-.39f,-.94f,-.08f,-.72f,.26f,0,1,.72f,.26f,.94f,-.08f,1,-.39f,.92f,-.72f,.72f,-.95f,.48f,-.99f,.22f,-.82f};
    private static final float[] HEX={-1,0,-.52f,-.88f,.52f,-.88f,1,0,.52f,.88f,-.52f,.88f};
    private static final float[] STAR=star();
    private static final float[] VISOR={-.96f,-.23f,-.82f,-.43f,-.42f,-.46f,0,-.35f,.42f,-.46f,.82f,-.43f,.96f,-.23f,.91f,.22f,.73f,.37f,.28f,.35f,.12f,.17f,-.12f,.17f,-.28f,.35f,-.73f,.37f,-.91f,.22f};
    private static float[] star(){float[] a=new float[20];for(int i=0;i<10;i++){float t=-1.5708f+i*.62832f,r=i%2==0?1:.53f;a[i*2]=(float)Math.cos(t)*r;a[i*2+1]=(float)Math.sin(t)*r;}return a;}
    private static float[] form(float[] a,float x,float y,float rx,float ry){float[] b=new float[a.length];for(int i=0;i<a.length;i+=2){b[i]=x+a[i]*rx;b[i+1]=y+a[i+1]*ry;}return b;}
    private static void rim(Painter p,float[] shape,float x,float y,float rx,float ry,float inside,int color){p.fillContours(new float[][]{form(shape,x,y,rx,ry),form(shape,x,y,rx*inside,ry*inside)},color);}
    static void draw(Painter p,int v,float x,float y,float r,float t,float detail,int color) {
        Painter trim=detail>0?new OpacityPainter(p,detail):p;
        if(v==4){snow(p,trim,x,y,r,detail);return;}
        int edge=v==0?0xFF9C4264:v==1?0xFF26757E:0xFF65509A;
        int bright=v==0?0xFFFFD3DF:v==1?0xFFC4FFFF:0xFFF3DCFF;
        p.line(x-r*.90f,y-r*.05f,x-r*.65f,y,edge,r*.10f);
        p.line(x+r*.65f,y,x+r*.90f,y-r*.05f,edge,r*.10f);
        p.polyline(new float[]{x-r*.13f,y,x-r*.07f,y-r*.075f,x+r*.07f,y-r*.075f,x+r*.13f,y},edge,r*.10f);
        float[] shape=v==0?HEART:v==1?HEX:STAR;
        float rx=r*(v==2?.43f:.36f),ry=r*(v==0?.36f:v==1?.34f:.43f);
        for(int side=-1;side<=1;side+=2) {
            float cx=x+side*r*.38f;
            rim(p,shape,cx,y+r*.035f,rx*1.06f,ry*1.06f,.69f,edge);
            rim(p,shape,cx,y,rx,ry,.72f,color);
            p.fillPoly(form(shape,cx,y,rx*.72f,ry*.72f),v==0?0x556F335C:v==1?0x554594AE:0x555F4DA3);
            if(detail>0) {
                rim(trim,shape,cx,y-r*.018f,rx*.94f,ry*.94f,.90f,bright);
                trim.line(cx-r*.15f,y-r*.13f,cx-r*.05f,y-r*.035f,0xA8FFFFFF,r*.04f);
                trim.line(cx-r*.10f,y-r*.18f,cx+r*.035f,y-r*.045f,0x55FFFFFF,r*.022f);
                trim.fillCircle(x+side*r*.83f,y-r*.035f,r*.034f,bright);
                if(v==2)AccessoryArt.star(trim,cx+side*r*.23f,y-r*.27f,r*(.045f+.025f*(.5f+.5f*(float)Math.sin(t*3+side))),0xFFFFE9A4);
            }
        }
    }
    private static void snow(Painter p,Painter trim,float x,float y,float r,float detail) {
        p.fillRect(x-r*1.02f,y-r*.12f,x+r*1.02f,y+r*.12f,0xFF7976B5);
        p.fillPoly(form(VISOR,x,y+r*.035f,r*1.04f,r*1.04f),0xFF393D68);
        p.fillPoly(form(VISOR,x,y,r,r),0xFFE3EFFF);
        p.fillPoly(form(VISOR,x,y-r*.015f,r*.87f,r*.77f),0xFF7DCDD9);
        p.fillPoly(form(new float[]{-.78f,-.19f,-.4f,-.32f,0,-.23f,.45f,-.32f,.77f,-.19f,.60f,.03f,-.65f,.08f},x,y,r,r),0xFFAAC7F1);
        p.fillPoly(form(new float[]{-.65f,.08f,.60f,.03f,.68f,.18f,.27f,.23f,.10f,.10f,-.12f,.10f,-.28f,.24f,-.64f,.22f},x,y,r,r),0xFF5B9EBD);
        if(detail>0) {
            trim.polyline(form(new float[]{-.76f,-.24f,-.4f,-.35f,0,-.25f,.4f,-.35f,.76f,-.24f},x,y,r,r),0xDFFFFFFF,r*.035f);
            for(int side=-1;side<=1;side+=2) {
                trim.line(x+side*r*.55f,y-r*.20f,x+side*r*.37f,y+r*.10f,0x96FFFFFF,r*.065f);
                trim.line(x+side*r*.43f,y-r*.21f,x+side*r*.27f,y+r*.04f,0x44FFFFFF,r*.025f);
                trim.fillRect(x+side*r*.96f-r*.026f,y-r*.085f,x+side*r*.96f+r*.026f,y+r*.085f,0xFFC7D0F4);
            }
        }
    }
}
