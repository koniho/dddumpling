package com.sram.hexatype;

/**
 * Shared test harness: the pass/fail tally, the assertion and grouping helpers, the
 * simulation drivers, and the Store/Sound stubs.
 *
 * The Test* classes extend this purely so that check(), advance(), add(), Mem and Ear
 * resolve unqualified — it keeps the assertions terse, which is the whole point of them.
 */
abstract class Check {

    static final float DT = 1f / 60f;
    static int pass, fail;

    static final class Mem implements GameCore.Store {
        int best;
        int saves;
        float speed = 1f;
        int bgm;
        int speedSaves, bgmSaves;
        public int loadBest() { return best; }
        public void saveBest(int b) { best = b; saves++; }
        public float loadSpeed() { return speed; }
        public void saveSpeed(float v) { speed = v; speedSaves++; }
        public int loadBgm() { return bgm; }
        public void saveBgm(int v) { bgm = v; bgmSaves++; }
    }

    static final class Ear implements GameCore.Sound {
        int squishes, clears, wrongs, damages, achievements;
        int lastGlyph = -1, lastDepth = -1;
        int music = -1, musicCalls;
        int starts, stageClears, powerClears, frenzyCalls;
        boolean frenzyOn;
        public void squish(int glyph, int depth) {
            squishes++;
            lastGlyph = glyph;
            lastDepth = depth;
        }
        public void clearWord() { clears++; }
        public void wrong() { wrongs++; }
        public void damage() { damages++; }
        public void achievement() { achievements++; }
        public void selectMusic(int choice) { music = choice; musicCalls++; }
        public void gameStart() { starts++; }
        public void stageClear() { stageClears++; }
        public void powerClear() { powerClears++; }
        public void frenzy(boolean on) { frenzyCalls++; frenzyOn = on; }
    }

    static GameCore.Enemy urgent(GameCore c) {
        GameCore.Enemy best = null;
        for (int i = 0; i < c.enemies.size(); i++) {
            GameCore.Enemy e = c.enemies.get(i);
            if (e.dying) continue;
            if (best == null || e.y > best.y) best = e;
        }
        return best;
    }

    static GameCore.Enemy add(GameCore c, Layout L, int[] word, float y) {
        int[] need = new int[word.length];
        for (int i = 0; i < need.length; i++) need[i] = 1;
        return add(c, L, word, need, y);
    }

    static GameCore.Enemy add(GameCore c, Layout L, int[] word, int[] need, float y) {
        GameCore.Enemy e = new GameCore.Enemy();
        e.word = word;
        e.need = need;
        e.gone = new boolean[word.length];
        e.goneT = new float[word.length];
        e.goneDx = new float[word.length];
        e.goneDy = new float[word.length];
        e.baseX = (L.playLeft + L.playRight) / 2f;
        e.y = y;
        e.speed = 0f;
        e.sway = 0f;
        c.enemies.add(e);
        return e;
    }

    /**
     * Steps until the interlude opens, sitting through the flawless-wave celebration that
     * now precedes it. Bounded, so a core that never gets there fails rather than hangs.
     */
    static boolean advanceToBonus(GameCore c, Layout L) {
        for (int i = 0; i < 60 * 30 && c.state != GameCore.BONUS; i++) c.update(DT, L);
        return c.state == GameCore.BONUS;
    }

    /** Steps out the far side of the interlude, whatever its current length. */
    static boolean advancePastBonus(GameCore c, Layout L) {
        advanceToBonus(c, L);
        for (int i = 0; i < 60 * 30 && c.state == GameCore.BONUS; i++) c.update(DT, L);
        return c.state != GameCore.BONUS;
    }

    static void advance(GameCore c, Layout L, float seconds) {
        for (float t = 0; t < seconds; t += DT) c.update(DT, L);
    }

    static void group(String name) {
        System.out.println("\n[" + name + "]");
    }

    static void check(String what, boolean ok) {
        if (ok) {
            pass++;
            System.out.println("  ok   " + what);
        } else {
            fail++;
            System.out.println("  FAIL " + what);
        }
    }
}
