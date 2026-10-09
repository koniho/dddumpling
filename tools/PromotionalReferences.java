package com.dddumpling.game;

import java.io.File;

/** Two portrait references for marketing illustrations; never substituted for store screenshots. */
final class PromotionalReferences {
    public static void main(String[] args) throws Exception {
        File dir=new File(args[0]);dir.mkdirs();
        int w=640,h=1400;Layout l=new Layout();l.compute(w,h,0,0,0,0);
        RasterPainter board=new RasterPainter(w,h,3);board.clear(0xFF1B172E);
        Kawaii.draw(board,Kawaii.DUMPLING,320,220,140,Glyph.COLOR[0],1,1);
        for(int land=0;land<4;land++)Lands.logo(board,land,320,490+land*250,85,255,0);
        Png.write(new File(dir,"game-art-and-lands.png"),board.resolve(),w,h);

        GameCore c=TestSurvival.start(TestSurvival.store(),173);
        c.survival.update(c,150);c.lives=1;c.takeHit(l.w*.5f,l);
        c.shake=c.flash=c.skyGlow=0;c.survival.ending=4;
        c.deathT=c.deathDuration()*.5f;c.time=c.deathDuration()*.5f;
        RasterPainter ending=new RasterPainter(w,h,3);ending.clear(0xFF000000);
        Renderer.draw(ending,c,l);
        Png.write(new File(dir,"survival-rainbow-curl.png"),ending.resolve(),w,h);
    }
}
