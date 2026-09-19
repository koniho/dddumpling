package com.dddumpling.game;

/** One pile finger; keys and other fingers cannot accidentally dispatch a full load. */
final class CaveMiningInput {
    int pointer=-1;
    float startX,startY,origin;
    void release(){pointer=-1;}
    boolean down(GameCore c,Layout L,int id,float x,float y) {
        CaveMining m=c.mining;
        if(!m.accepts(c) || !m.swipeReady() || !CaveMiningScreen.inCart(m,L,x,y))return false;
        if(pointer<0){pointer=id;startX=x;startY=y;origin=m.cartX;}
        return true;
    }
    boolean move(GameCore c,Layout L,int id,float x,float y) {
        if(pointer<0 || id!=pointer)return false;
        CaveMining m=c.mining;
        if(!m.accepts(c) || !m.swipeReady()){release();return true;}
        float dx=x-startX,dy=y-startY;
        m.cartX=origin+Math.max(-.20f,Math.min(.20f,dx/L.w));
        if(Math.abs(dx)>=L.w*.16f && Math.abs(dx)>Math.abs(dy)*1.3f)m.launch(c,dx<0?-1:1);
        return true;
    }
    void up(int id){if(id==pointer)release();}
}
