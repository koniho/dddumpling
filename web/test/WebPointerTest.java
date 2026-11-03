package com.dddumpling.game;

/** Browser pointer translation tested against the real gesture router and enemy rules. */
public final class WebPointerTest extends Check {
    public static void main(String[] args) {
        Ear ear=new Ear();IOSGame game=new IOSGame(new Mem(),ear,230);
        game.layout(390,844,0,0,0,0);game.debugScene("keyboard:6");
        GameCore c=game.core();Layout l=game.geometry();c.enemies.clear();
        GameCore.Enemy e=add(c,l,new int[]{0,1},new int[]{2,2},l.h*.3f);
        WebPointers p=new WebPointers(game);float x=c.keyX(l,0),y=c.keyY(l,0);
        p.event(0,7,x,y);check("first sparse pointer taps once",e.done==1 && ear.squishes==1);
        p.event(0,7,x,y);check("duplicate down is ignored",e.done==1 && ear.squishes==1);
        p.event(0,901,x,y);check("second pointer retains first and taps",e.pos==1 && ear.squishes==2);
        p.event(1,7,x,y);p.event(1,7,x,y);check("departing pointer is removed only once",ear.squishes==2);
        p.event(2,901,c.keyX(l,1),c.keyY(l,1));check("moving held finger does not press another key",e.done==0);
        p.cancel();p.event(1,901,x,y);p.event(0,901,c.keyX(l,1),c.keyY(l,1));
        check("cancel releases sparse IDs for reuse",e.done==1 && ear.squishes==3);
        p.cancel();game.keyDown('s',false);p.cancel();game.keyDown('s',false);
        check("pointer cancellation also releases keyboard latch",game.debugStatus().contains("keyboardPresses=2;"));
        System.out.println("Web pointers: "+pass+" passed, "+fail+" failed");if(fail>0)System.exit(1);
    }
}
