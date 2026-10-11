package com.dddumpling.game;

/** Lily pads, cattails and drifting fireflies; shared by the land, title and stage skits. */
final class Swamp extends Draw {
    private Swamp() {}
    static void prop(Painter painter,float x,float y,float r,int alpha,float t,boolean silhouette) {
        Painter p=new OpacityPainter(painter,alpha/255f);
        int green=silhouette?0xFF8C9A94:0xFF639F85;
        p.fillEllipse(x,y+r*.45f,r*1.15f,r*.33f,silhouette?green:0xAA357B75);
        p.arc(x,y+r*.45f,r*1.03f,r*.25f,5,175,0x886CCAB5,r*.03f);
        for(int i=0;i<3;i++) {
            float xx=x+(i-1)*r*.62f,yy=y+r*(.38f+i%2*.17f);
            float rr=r*(.38f+i*.04f);
            p.fillEllipse(xx,yy,rr,rr*.42f,green);
            p.line(xx,yy,xx+rr*.9f,yy-rr*.10f,silhouette?green:0xFFA7DDAC,r*.032f);
        }
        for(int i=0;i<3;i++) {
            float xx=x+r*(.42f+i*.24f),lean=(float)Math.sin(t*1.4f+i)*r*.11f;
            float top=y-r*(.95f+i%2*.25f);
            p.polyline(new float[]{xx,y+r*.45f,xx+lean*.4f,y-r*.2f,xx+lean,top},green,r*.045f);
            p.line(xx+lean,top+r*.20f,xx+lean,top-r*.18f,silhouette?green:0xFF715C62,r*.12f);
            p.line(xx,y+r*.28f,xx-r*.22f,y-r*.27f,green,r*.045f);
        }
        if(!silhouette)for(int i=0;i<4;i++) {
            float xx=x+r*(float)Math.sin(t*.45f+i*2.1f),yy=y-r*(.2f+.7f*(1+(float)Math.cos(t*.62f+i)));
            float glow=.5f+.5f*(float)Math.sin(t*2+i);
            p.fillCircle(xx,yy,r*.11f,fadeBy(0xFFF0F7AF,glow*.15f));
            p.fillCircle(xx,yy,r*.026f,fadeBy(0xFFFFFFC5,.35f+glow*.65f));
        }
    }
    static void skit(Painter p,int variant,float x,float y,float r,float t,int a) {
        prop(p,x,y+r*.4f,r,a,t*5,false);
        float hop=(float)Math.sin(t*Math.PI*4),xx=x+(float)Math.sin(t*Math.PI*2)*r*.65f;
        if(variant==1) {
            AlligatorArt.portrait(p,x+r*.3f,y+r*.3f,r*.48f,t*4,true,a/255f);
            xx=x-r*.48f;
        }
        Skits.face(p,Kawaii.DUMPLING,xx,y-r*(.05f+Math.abs(hop)*.65f),r*.37f,a,1+.12f*hop,.95f);
        if(hop<0)for(int i=0;i<5;i++)p.fillCircle(xx+(i-2)*r*.16f,y+r*.22f-Math.abs(hop)*r*(.2f+i%2*.2f),r*.035f,Glyph.withAlpha(AlligatorArt.LIGHT,a));
    }
}
