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
        check("touch hosts retain the two-finger lesson",!c.mouseStretch && TutorialSpeech.spoken(TutorialSpeech.PINCH).contains("two fingers"));
        c.mouseStretch=true;c.onboarding.speech=TutorialSpeech.PINCH;c.onboarding.speechPage=0;
        StringBuilder lesson=new StringBuilder();
        do {
            lesson.append(TutorialSpeech.spokenPage(TutorialSpeech.PINCH,c.onboarding.speechPage,c.preferences.skin,c.mouseStretch));
            if(!c.onboarding.moreSpeech(c))break;
            c.onboarding.speechPage++;
        } while(true);
        check("mouse lesson and pagination teach mirrored dragging",lesson.toString().contains("Drag the ready cube") && lesson.toString().contains("mirrored finger") && !lesson.toString().contains("two fingers"));
        p.cancel();game.debugScene("stage:10");c.onboarding.clear();c.onboarding.saved|=Onboarding.SKIPPED;
        c.stageGap=0;c.boss.intro=0;c.boss.update(1f/60,l,c.rnd);
        Boss b=c.boss;Softbody body=b.divideBody[0];float bx=body.centreX(),by=body.centreY(),r=body.radiusX();
        p.mouse(0,bx,by);p.mouse(2,bx+r*3,by);p.mouse(1,bx+r*3,by);
        check("mouse cannot split an uncharged glob",b.divideSplits==0);
        b.pieceHits[0]=Boss.DIVIDE_HITS;p.mouse(0,bx,by);
        check("mouse down creates a real two-finger pinch on the charged glob",b.pinchNode==0 && b.pinchStart>0);
        p.mouse(2,bx+r*.1f,by);check("short mouse drag does not split",b.divideSplits==0);
        p.cancel();p.mouse(0,bx,by);p.mouse(2,bx-r*1.3f,by);
        check("centre click also stretches naturally to the left",b.divideSplits==1);
        p.cancel();game.debugScene("stage:10");c.onboarding.clear();c.onboarding.saved|=Onboarding.SKIPPED;
        c.stageGap=0;c.boss.intro=0;b=c.boss;b.update(1f/60,l,c.rnd);b.pieceHits[0]=Boss.DIVIDE_HITS;
        body=b.divideBody[0];bx=body.centreX();by=body.centreY();r=body.radiusX();p.mouse(0,bx,by);
        p.mouse(2,bx+r*1.3f,by);
        check("mirrored mouse stretch uses the real split and effects",b.divideSplits==1 && c.shake>0);
        p.mouse(1,bx+r*1.3f,by);check("mouse release clears the synthetic pinch",b.pinchNode<0);
        b.update(1f/60,l,c.rnd);int child=b.pieceNodeIndex(0);body=b.divideBody[child];b.pieceHits[child]=Boss.DIVIDE_HITS;
        p.mouse(0,body.centreX(),body.centreY());p.cancel();
        check("cancel releases both synthetic fingers",b.pinchNode<0);
        int other=b.pieceNodeIndex(1);b.pieceHits[other]=Boss.DIVIDE_HITS;body=b.divideBody[other];
        p.mouse(0,body.centreX(),body.centreY());check("mouse targets the clicked vulnerable glob",b.pinchNode==other);p.cancel();
        System.out.println("Web pointers: "+pass+" passed, "+fail+" failed");if(fail>0)System.exit(1);
    }
}
