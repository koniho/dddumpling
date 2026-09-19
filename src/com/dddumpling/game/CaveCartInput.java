package com.dddumpling.game;

/** Star Path's slider catchment and direct dragging, with one stable pointer owner. */
final class CaveCartInput {
    int pointer=-1;
    private float offset;
    private final CaveCart owner;
    CaveCartInput(CaveCart owner){this.owner=owner;}
    void release(){pointer=-1;offset=0;owner.intent=owner.hold=0;}
    static float knob(CaveCart m,Layout L){
        return StarScreen.sliderLeft(L)+(m.lean+1)*.5f*(StarScreen.sliderRight(L)-StarScreen.sliderLeft(L));
    }
    boolean down(GameCore c,Layout L,int id,float x,float y){
        if(!owner.accepts(c))return false;
        boolean slider=StarScreen.inSlider(L,x,y);
        boolean cart=Math.abs(x-L.w*.5f)<L.w*.31f
                && Math.abs(y-(L.playTop+CaveCartScreen.field(L)*.72f))<L.w*.22f;
        if(!slider && !cart)return y>=L.deckTop;
        if(pointer<0){pointer=id;offset=cart?knob(owner,L)-x:0;drag(L,x);}
        return true;
    }
    private void drag(Layout L,float x){
        owner.steer((x+offset-StarScreen.sliderLeft(L))/(StarScreen.sliderRight(L)-StarScreen.sliderLeft(L))*2-1);
        owner.lean=owner.intent;owner.hold=Float.POSITIVE_INFINITY;
    }
    boolean move(GameCore c,Layout L,int id,float x,float y){
        if(pointer<0||id!=pointer)return false;
        if(!owner.accepts(c)){release();return true;}
        drag(L,x);return true;
    }
    void up(int id){if(id==pointer){pointer=-1;offset=0;}}
}
