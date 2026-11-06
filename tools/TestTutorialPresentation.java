package com.dddumpling.game;

final class TestTutorialPresentation extends Check {
    static void all(Layout L) {
        group("Tutorial scenery continuity");
        for(int stage:new int[]{1,6,11,16,26}) {
            GameCore c=TestOnboarding.fresh(L,new Mem());
            c.stage=stage;c.landBlend=1;c.skyClock=12.5f;
            int background=Lands.background(c);
            c.onboarding.begin(c,Onboarding.STEAMER,L);
            GameCore q=c.onboarding.practice;
            check("Steamer begins in the waiting land "+stage,q.stage==stage && Lands.background(q)==background
                    && Lands.blend(q)==Lands.blend(c) && q.skyClock==c.skyClock);
            c.update(.4f,L);
            check("scene reveal cannot fade land back to title sky "+stage,Lands.background(q)==background && q.landBlend==1);
            c.onboarding.skip(c);
            check("skip restores the unchanged waiting land "+stage,c.stage==stage && Lands.background(c)==background);
        }
    }
}
