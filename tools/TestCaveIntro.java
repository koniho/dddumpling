package com.dddumpling.game;

final class TestCaveIntro extends Check {
    static void all(Layout L) {
        group("cave stage introduction");
        GameCore c=new GameCore(new Mem(),956L);c.caveChoice=0;c.startGame();
        for(int stage:new int[]{21,22,25}) {
            c.jumpToStage(stage,L);
            c.update(GameCore.BANNER_TIME-.1f,L);
            check("intro keeps expedition still "+stage,c.cave.z==0 && c.cave.cameraZ==0
                    && c.cave.timer==0 && c.stageBanner>0 && c.cave.phase==Cave.WALK);
            float aim=c.cave.aim;
            check("intro ignores cave steering "+stage,!c.cave.input.down(c,L,1,L.w*.2f,L.playTop+L.w*.1f)
                    && c.cave.aim==aim && c.cave.input.pointer<0);
            float banner=c.stageBanner;
            c.paused=true;c.update(.5f,L);c.paused=false;
            check("pause preserves cave introduction "+stage,c.stageBanner==banner && c.cave.z==0);
            c.settingsOpen=true;c.update(.5f,L);c.settingsOpen=false;
            check("settings preserve cave introduction "+stage,c.stageBanner==banner && c.cave.z==0);
            c.update(.15f,L);
            check("only time after intro advances cave "+stage,c.stageBanner==0
                    && Math.abs(c.cave.z-Cave.WALK_SPEED*.05f)<.0001f && c.stageGap==0);
        }
        c=new GameCore(new Mem(),959L);c.startGame();c.jumpToStage(21,L);
        c.update(CaveSelection.INTRO+.01f,L);c.cave.selection.pick(c,0);
        c.update(CaveSelection.DEPART+.01f,L);
        check("first explorer choice is followed by full introduction",c.cave.phase==Cave.WALK
                && c.cave.z==0 && c.stageBanner==GameCore.BANNER_TIME);
        c.update(GameCore.BANNER_TIME*.5f,L);
        check("first entrance waits through introduction",c.cave.z==0 && c.stageBanner>0);
        c.update(GameCore.BANNER_TIME*.5f+.1f,L);
        check("first entrance starts after introduction",c.cave.z>0 && c.stageBanner==0);
    }
}
