package com.sram.hexatype;

/** Effect normalisation and which sound fires on which event. */
final class TestAudio extends Check {

    static void audio(Layout L) {
        group("audio");
        int peak = (int) (Sfx.PEAK * 32767f);
        boolean allNormalised = true, allClean = true, allSane = true;
        for (int id = 0; id < Sfx.COUNT; id++) {
            short[] pcm = Sfx.build(id);
            int max = 0;
            for (int i = 0; i < pcm.length; i++) max = Math.max(max, Math.abs(pcm[i]));
            // Peak-normalised: every effect tops out at the same level.
            if (Math.abs(max - peak) > 2) allNormalised = false;
            if (max >= 32767) allClean = false;
            if (pcm.length < Sfx.RATE / 20 || pcm.length > Sfx.RATE * 2) allSane = false;
        }
        check("every effect is normalised to the same peak", allNormalised);
        check("no effect clips", allClean);
        check("effect lengths are sane", allSane);

        short[] loop = Music.loop(Music.SWING_STYLE);
        check("music loop is the expected length", loop.length == Music.loopFrames(Music.SWING_STYLE));
        check("music loop is several seconds", loop.length > Sfx.RATE * 5);
        int lmax = 0;
        for (int i = 0; i < loop.length; i++) lmax = Math.max(lmax, Math.abs(loop[i]));
        check("music sits below the effects", lmax < peak);
        check("music is audible", lmax > peak / 4);

        // Effects must fire on the right events.
        Ear ear = new Ear();
        GameCore c = new GameCore(new Mem(), 71L);
        c.sound = ear;
        c.startGame();
        c.enemies.clear();
        c.target = null;
        GameCore.Enemy e = add(c, L, new int[] {3, 1}, new int[] {2, 1}, L.playTop + 80);

        c.tapKey(3, L);
        check("a correct press squishes", ear.squishes == 1);
        check("the squish knows the letter", ear.lastGlyph == 3);
        check("the squish knows the stack depth", ear.lastDepth == 2);
        c.tapKey(0, L);
        check("a wrong press thunks", ear.wrongs == 1);
        check("a wrong press does not squish", ear.squishes == 1);

        c.tapKey(3, L);
        c.tapKey(3, L);
        c.tapKey(1, L);
        check("no clear sound before the shot lands", ear.clears == 0);
        advance(c, L, 0.3f);
        check("clearing a word plays the clear sound", ear.clears == 1);

        c.enemies.clear();
        add(c, L, new int[] {0}, L.dangerY - L.enemyR + 1);
        advance(c, L, GameCore.ATTACK_TIME + 2 * DT);
        check("taking damage plays the drip", ear.damages == 1);

        Ear ear2 = new Ear();
        GameCore g = new GameCore(new Mem(), 72L);
        g.sound = ear2;
        g.startGame();
        g.spawnedThisStage = g.stageQuota();
        g.enemies.clear();
        g.shots.clear();
        g.update(DT, L);
        advancePastBonus(g, L);
        check("a flawless wave plays the achievement", ear2.achievements == 1);

        Ear ear3 = new Ear();
        GameCore m = new GameCore(new Mem(), 73L);
        m.sound = ear3;
        m.startGame();
        m.enemies.clear();
        m.tapKey(0, L);                      // a miss forfeits the reward
        m.spawnedThisStage = m.stageQuota();
        m.enemies.clear();
        m.shots.clear();
        m.update(DT, L);
        check("a flawed wave plays no achievement", ear3.achievements == 0);

        check("a null sound seam is safe", silentRunSurvives(L));
    }

    static boolean silentRunSurvives(Layout L) {
        GameCore c = new GameCore(new Mem(), 74L);
        c.sound = null;
        c.startGame();
        for (int i = 0; i < 600; i++) {
            c.update(DT, L);
            c.tapKey(i % Glyph.COUNT, L);
        }
        return true;
    }

}
