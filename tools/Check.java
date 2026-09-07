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
        long collected;
        int collectedSaves;
        int collectTotal;
        int collectTotalSaves;
        int rosterState = 1, rosterSaves;
        public int loadBest() { return best; }
        public void saveBest(int b) { best = b; saves++; }
        public float loadSpeed() { return speed; }
        public void saveSpeed(float v) { speed = v; speedSaves++; }
        public int loadBgm() { return bgm; }
        public void saveBgm(int v) { bgm = v; bgmSaves++; }
        public long loadCollected() { return collected; }
        public void saveCollected(long v) { collected = v; collectedSaves++; }
        public int loadCollectTotal() { return collectTotal; }
        public void saveCollectTotal(int v) { collectTotal = v; collectTotalSaves++; }
        public int loadRosterState() { return rosterState; }
        public void saveRosterState(int v) { rosterState = v; rosterSaves++; }
    }

    static final class Ear implements GameCore.Sound {
        int squishes, clears, wrongs, damages, achievements, bossLaughs, bossDamages, slimeDamages, bossSplits,
                bossChargeCalls, boltPops, boltDeaths, shieldBounces, octoCues, octoLocks, divideDamages, divideSplits, divideDeactivates, divideBoings, chops, zaps;
        float bossCharge, maxBossCharge;
        float lastDivideBoingWeight = -1f;
        int collects;
        /** Stars taken, and the count the last one announced. */
        int stars;
        int lastStar = -1;
        /** Every haul position announced, in the order it was announced. */
        final java.util.List<Integer> shelved = new java.util.ArrayList<Integer>();
        int lastZapHop = -1;
        int lastGlyph = -1, lastDepth = -1;
        int music = -1, musicCalls;
        int starts, stageClears, powerClears, frenzyCalls;
        /** Interlude punctuation: course launches, tallies read out, parade joins, runs ended. */
        int courseStarts, rocketCalls, tallies, joins, gameOvers, bossTaunts;
        int lastBossTaunt = -1;
        float rocketThrust, firstRocket = -1f, maxRocket;
        int rocketStops;
        int lastTally = -1;
        boolean frenzyOn;
        boolean bossMusic;
        int bossMusicCalls;
        int narrations, hushes;
        int narrated = -1;
        public void squish(int glyph, int depth) {
            squishes++;
            lastGlyph = glyph;
            lastDepth = depth;
        }
        public void clearWord() { clears++; }
        public void wrong() { wrongs++; }
        public void damage() { damages++; }
        public void achievement() { achievements++; }
        public void bossLaugh() { bossLaughs++; }
        public void bossDamage() { bossDamages++; }
        public void slimeDamage() { slimeDamages++; }
        public void bossSplit() { bossSplits++; }
        public void divideDamage() { divideDamages++; }
        public void divideSplit() { divideSplits++; }
        public void divideDeactivate() { divideDeactivates++; }
        public void divideBoing(float weight) {
            divideBoings++;
            lastDivideBoingWeight = weight;
        }
        public void boltPop() { boltPops++; }
        public void boltDeath() { boltDeaths++; }
        public void shieldBounce() { shieldBounces++; }
        public void octoCue() { octoCues++; }
        public void octoLock() { octoLocks++; }
        public void bossCharge(float charge) {
            bossChargeCalls++;
            bossCharge = charge;
            maxBossCharge = Math.max(maxBossCharge, charge);
        }
        public void chop() { chops++; }
        public void zap(int hop) { zaps++; lastZapHop = hop; }
        public void collect(int nth) { collects++; shelved.add(nth); }
        public void star(int nth) { stars++; lastStar = nth; }
        public void courseStart() { courseStarts++; }
        public void rocket(float thrust) {
            rocketCalls++;
            rocketThrust = thrust;
            if (thrust > 0f && firstRocket < 0f) firstRocket = thrust;
            if (thrust > maxRocket) maxRocket = thrust;
            if (thrust == 0f) rocketStops++;
        }
        public void tally(int nth) { tallies++; lastTally = nth; }
        public void paradeJoin() { joins++; }
        public void rosterJoin() { achievements++; }
        public void gameOver() { gameOvers++; }
        public void bossTaunt(int kind) { bossTaunts++; lastBossTaunt = kind; }
        public void selectMusic(int choice) { music = choice; musicCalls++; }
        public void bossMusic(boolean active) { bossMusic = active; bossMusicCalls++; }
        public void gameStart() { starts++; }
        public void stageClear() { stageClears++; }
        public void powerClear() { powerClears++; }
        public void frenzy(boolean on) { frenzyCalls++; frenzyOn = on; }
        public void narrate(int entry) { narrations++; narrated = entry; }
        public void hush() { hushes++; }
    }

    /**
     * One frame of competent boss play, for the drivers that are not measuring bosses but now have
     * to get past them.
     *
     * A boss has to be beaten for its stage to end, so any long-running driver that only types will
     * sit on stage 5 until its clock runs out — which silently turns "frenzies happen over a long
     * run" and "every mode turns up" into tests of the first four stages. This is the smallest thing
     * that keeps them measuring what they were written to measure: no reaction limit and no miss
     * rate, because it is not pretending to be a player. {@link Bot} is the one with hands.
     *
     * @return true when it did something with the boss this frame
     */
    static boolean bossPlay(GameCore c, Layout L) {
        if (!c.bossFighting()) return false;
        Boss b = c.boss;
        if (b.kind == Boss.SPLITTER && b.vulnerablePiece() >= 0) {
            b.beginPinch(100f);
            return b.pinch(100f * (Boss.DIVIDE_SCALE + 0.01f));
        }
        // Bolts first: the only thing on a boss stage that costs a life.
        for (int g = 0; g < Glyph.COUNT; g++) {
            if (b.boltWants(g)) return c.tapKey(g, L);
        }
        // Carry anything held straight to where it goes. One frame, since this is not a hand.
        if (b.held >= 0) {
            int i = b.held;
            boolean key = b.etype[i] == Boss.E_KEY && b.keyOf[i] >= 0;
            if (key) c.dragBoss(L.keyX[b.keyOf[i]], L.deckTop + 1f, L);
            else c.dragBoss(L.playRight + 1f, b.ey[i], L);
            if (b.held >= 0) c.releaseBoss();
            return true;
        }
        if (b.shovable()) return c.swipeUp(L);
        for (int i = 0; i < Boss.ELEMS; i++) {
            if (b.draggable(i)) return c.grabBoss(b.ex[i], b.ey[i]);
        }
        for (int i = 0; i < Boss.ELEMS; i++) {
            boolean worth = (b.etype[i] == Boss.E_HEAD && !b.headAwake(i))
                    || (b.etype[i] == Boss.E_SKIN && b.tapBeat && b.open());
            if (worth) return c.tapBoss(b.ex[i], b.ey[i], L);
        }
        // Nothing engaged is the boss's precedence condition, so a press only reaches it then.
        boolean engaged = c.target != null && c.enemies.contains(c.target) && c.target.typeable();
        if (engaged) return false;
        for (int g = 0; g < Glyph.COUNT; g++) {
            if (b.wants(g)) return c.tapKey(g, L);
        }
        return false;
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

    /**
     * Steps through the whole death sequence: the hold, the summary fading up, and the grace
     * after it. A press before this lands is ignored by design, so every test that means to
     * dismiss the game-over screen has to get past it.
     */
    static void advancePastDeath(GameCore c, Layout L) {
        advance(c, L, GameCore.DEATH_TIME + GameCore.OVER_FADE + GameCore.OVER_GRACE + 0.2f);
    }

    /**
     * Presses one word of the current stage costs, averaged over the real generator rather than
     * estimated: word length and stack depth both move with the ramp, and the press budget caps the
     * total, so this is not a formula anybody should be writing out by hand.
     */
    static float pressesPerWord(GameCore c, long seed) {
        java.util.Random rnd = new java.util.Random(seed);
        int lo = c.minWordLen(), hi = c.maxWordLen();
        int n = 20000;
        float total = 0f;
        for (int i = 0; i < n; i++) {
            GameCore.Enemy e = new GameCore.Enemy();
            Words.fill(e, lo + rnd.nextInt(hi - lo + 1), c.stackChance(), rnd);
            total += e.totalPresses();
        }
        return total / n;
    }

    /** How fast the TEAM SQUISH buddy is going, in px/s. */
    static float speedOf(Buddy b) {
        return (float) Math.sqrt(b.vx * b.vx + b.vy * b.vy);
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

    /**
     * Steps to the interlude and then through the spinner, so the caller can press. Presses
     * are refused for the whole of the spin, which silently zeroes any test that taps the
     * moment BONUS begins.
     */
    static boolean advanceToMash(GameCore c, Layout L) {
        if (!advanceToBonus(c, L)) return false;
        for (int i = 0; i < 60 * 10 && c.bonusRolling(); i++) c.update(DT, L);
        return c.bonusMashing();
    }

    /**
     * Presses a start key on the title screen and steps through the fade-out, so the caller
     * lands in play. A bare press only begins the dissolve — play starts when it finishes.
     */
    static boolean startFromTitle(GameCore c, Layout L, int key) {
        c.tapKey(key, L);
        for (int i = 0; i < 60 * 5 && c.state == GameCore.TITLE; i++) c.update(DT, L);
        return c.state == GameCore.PLAY;
    }

    static void advance(GameCore c, Layout L, float seconds) {
        for (float t = 0; t < seconds; t += DT) c.update(DT, L);
    }

    /**
     * True when every character has a bitmap in the harness font. A character it does not know
     * silently vanishes from the PNGs, so text that only renders on the device is text nobody
     * ever checks.
     */
    static boolean printable(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Font.rows(s.charAt(i)) == null) return false;
        }
        return true;
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
