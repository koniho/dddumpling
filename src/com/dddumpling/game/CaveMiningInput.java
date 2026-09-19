package com.dddumpling.game;

/** Hold or drag either side of the field to lean; deck presses remain immediate. */
final class CaveMiningInput {
    int pointer=-1;
    private final CaveMining owner;
    CaveMiningInput(CaveMining owner){this.owner=owner;}
    void release(){pointer=-1;owner.intent=owner.hold=0;}
    boolean down(GameCore c,Layout L,int id,float x,float y){
        if(!c.mining.accepts(c)||y<L.playTop||y>=L.deckTop||c.keyAt(x,y,L)>=0)return false;
        if(pointer<0){pointer=id;owner.steer((x/L.w-.5f)/.30f);}
        return true;
    }
    boolean move(GameCore c,Layout L,int id,float x,float y){
        if(pointer<0||id!=pointer)return false;
        if(!c.mining.accepts(c)){release();return true;}
        c.mining.steer((x/L.w-.5f)/.30f);return true;
    }
    void up(int id){if(id==pointer)release();}
}
