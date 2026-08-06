package com.sram.hexatype;

/** Word content: stacked letters, destruction, entrance and letter persistence. */
final class TestWords extends Check {

    static void stackedLetters(Layout L) {
        group("stacked letters");
        GameCore c = new GameCore(new Mem(), 51L);
        c.startGame();

        // A 3-stack takes exactly three presses, and only the third clears the tile.
        c.enemies.clear();
        c.target = null;
        GameCore.Enemy e = add(c, L, new int[] {2, 4}, new int[] {3, 1}, L.playTop + 80);
        check("stack reports its depth", e.stacked(0) && !e.stacked(1));
        check("total presses counted", e.totalPresses() == 4);
        check("presses owed starts at the full depth", c.pressesLeft(e, 0) == 3);

        c.tapKey(2, L);
        check("first press does not clear the stack", e.pos == 0 && e.done == 1);
        check("presses owed drops", c.pressesLeft(e, 0) == 2);
        c.tapKey(2, L);
        check("second press does not clear it either", e.pos == 0 && e.done == 2);
        c.tapKey(2, L);
        check("third press clears the stack", e.pos == 1 && e.done == 0);
        check("cleared stack owes nothing", c.pressesLeft(e, 0) == 0);
        c.tapKey(4, L);
        check("plain letter after a stack still takes one press", e.pos == 2);

        // A wrong letter mid-stack resets the count along with the word.
        c.enemies.clear();
        c.target = null;
        GameCore.Enemy r = add(c, L, new int[] {0, 1}, new int[] {4, 2}, L.playTop + 80);
        c.tapKey(0, L);
        c.tapKey(0, L);
        check("partway into the stack", r.done == 2);
        c.tapKey(5, L);   // wrong
        check("wrong letter resets the stack count", r.done == 0);
        check("wrong letter also restarts the word", r.pos == 0);
        check("presses owed is back to full", c.pressesLeft(r, 0) == 4);
        check("the lock is released, not auto-reengaged", c.target == null);
        c.tapKey(0, L);
        check("re-engaging a stack starts from the first press",
                c.target == r && r.done == 1);

        // Generation invariants over many spawns at every stage.
        GameCore g = new GameCore(new Mem(), 52L);
        g.startGame();
        int worst = 0, maxNeed = 0, stacksSeen = 0, words = 0, plainTwins = 0;
        boolean overCap = false, badNeed = false, stackBesideTwin = false;
        for (int stage = 1; stage <= 14; stage++) {
            g.stage = stage;
            for (int n = 0; n < 400; n++) {
                g.enemies.clear();
                g.spawnedThisStage = 0;
                g.stageGap = 0;
                g.spawnTimer = 0;
                g.update(DT, L);
                if (g.enemies.isEmpty()) continue;
                GameCore.Enemy w = g.enemies.get(0);
                words++;
                int total = w.totalPresses();
                if (total > GameCore.MAX_PRESSES) overCap = true;
                if (total > worst) worst = total;
                for (int i = 0; i < w.need.length; i++) {
                    if (w.need[i] < 1 || w.need[i] > 4) badNeed = true;
                    if (w.need[i] > maxNeed) maxNeed = w.need[i];
                    if (w.need[i] > 1) stacksSeen++;

                    // A stack must differ from both its neighbours: it is the same key several
                    // times, so an identical letter beside it hides where one ends and the next
                    // begins. Plain letters may still pair up — that is readable, and forbidding
                    // it would thin the alphabet for no reason, so it is counted to prove the
                    // rule is narrow rather than a blanket no-repeats.
                    boolean stack = w.need[i] > 1;
                    if (i > 0 && w.word[i - 1] == w.word[i]) {
                        if (stack || w.need[i - 1] > 1) stackBesideTwin = true;
                        else plainTwins++;
                    }
                }
            }
        }
        System.out.printf("    %d words generated, worst total = %d presses, deepest stack = %d,"
                + " %d stacked tiles, %d plain twin pairs%n", words, worst, maxNeed, stacksSeen,
                plainTwins);
        check("no word ever exceeds " + GameCore.MAX_PRESSES + " presses", !overCap);
        check("every tile needs 1..4 presses", !badNeed);
        check("stacks do get generated", stacksSeen > 0);
        check("stacks reach depth 4 somewhere", maxNeed == 4);
        check("no stack ever sits beside its own letter", !stackBesideTwin);
        check("plain letters still pair up", plainTwins > 0);

        // Stage 1 must stay plain, so the mechanic is introduced rather than sprung.
        GameCore s1 = new GameCore(new Mem(), 53L);
        s1.startGame();
        boolean plainOpening = true;
        for (int n = 0; n < 300; n++) {
            s1.enemies.clear();
            s1.spawnedThisStage = 0;
            s1.stageGap = 0;
            s1.spawnTimer = 0;
            s1.stage = 1;
            s1.update(DT, L);
            if (s1.enemies.isEmpty()) continue;
            for (int need : s1.enemies.get(0).need) if (need != 1) plainOpening = false;
        }
        check("stage 1 has no stacks", plainOpening);
        check("later stages do stack", s1.stackChance() == 0f && chanceAt(s1, 5) > 0f);
    }

    static float chanceAt(GameCore c, int stage) {
        int was = c.stage;
        c.stage = stage;
        float v = c.stackChance();
        c.stage = was;
        return v;
    }

    static void destruction(Layout L) {
        group("destruction");
        GameCore c = new GameCore(new Mem(), 91L);
        c.startGame();
        c.enemies.clear();
        c.target = null;

        GameCore.Enemy e = add(c, L, new int[] {0, 1, 2, 3}, L.playTop + 120);
        for (int i = 0; i < 4; i++) c.tapKey(e.word[i], L);
        check("word is fully typed", e.pos == 4 && e.dying);
        check("not yet destroyed while the shot flies", !e.destroyed);
        check("still listed", c.enemies.contains(e));

        float shakeBefore = c.shake;
        int squishesBefore = c.squishes;
        advance(c, L, 0.2f);                       // let the killing shot land
        check("impact starts the destroy animation", e.destroyed && e.destroyT >= 0f);
        check("the squish is credited at impact", c.squishes == squishesBefore + 1);
        check("destruction shakes the screen", c.shake > shakeBefore);
        check("a destroyed word is not typeable", !e.typeable());
        check("a destroyed word lingers on the field", c.enemies.contains(e));

        // A destroyed word must stop showing its warning state. The update loop skips
        // destroyed words, so anything left set here stays frozen on it for the whole
        // fly-apart — visible as jitter and rose telegraph rings on a word already dead.
        check("destroying clears the word's warning", e.warn == 0f);
        check("destroying clears any lunge", !e.attacking && e.attackT == 0f);
        check("destroying clears the fail flash", e.failPulse == 0f);

        // Fly directions: ends forced outward, middles to the nearer edge.
        check("fly directions assigned", e.flyDir != null && e.flyDir.length == 4);
        check("leftmost tile flies left", e.flyDir[0] == -1f);
        check("rightmost tile flies right", e.flyDir[3] == 1f);
        boolean middlesByProximity = true;
        for (int i = 1; i < 3; i++) {
            float want = c.tileX(e, i, L) < L.w / 2f ? -1f : 1f;
            if (e.flyDir[i] != want) middlesByProximity = false;
        }
        check("middle tiles fly to the nearer edge", middlesByProximity);

        advance(c, L, GameCore.DESTROY_TIME + 2 * DT);
        check("the destroyed word is gone once the animation ends", !c.enemies.contains(e));

        // A one-letter word has no interior, so it just takes the nearer edge.
        c.enemies.clear();
        c.target = null;
        GameCore.Enemy one = add(c, L, new int[] {4}, L.playTop + 120);
        one.baseX = L.w * 0.8f;
        c.tapKey(4, L);
        advance(c, L, 0.2f);
        check("a single-letter word flies to its nearer edge",
                one.flyDir.length == 1 && one.flyDir[0] == 1f);

        // The wave gate must wait for the animation, not just for the kill.
        GameCore w = new GameCore(new Mem(), 92L);
        w.startGame();
        w.enemies.clear();
        w.target = null;
        w.spawnedThisStage = w.stageQuota();
        GameCore.Enemy last = add(w, L, new int[] {5, 5}, L.playTop + 120);
        w.tapKey(5, L);
        w.tapKey(5, L);
        advance(w, L, 0.2f);
        check("last word of the wave is flying apart", last.destroyed);
        check("stage does not advance mid-animation", w.stage == 1 && !w.stageCleared());
        advance(w, L, GameCore.DESTROY_TIME);
        check("the interlude opens after the celebration",
                advanceToBonus(w, L) && w.stage == 1);
        advancePastBonus(w, L);
        check("stage advances on the way out of the interlude", w.stage == 2);
    }

    static void entranceAndPersistence(Layout L) {
        group("entrance and letter persistence");
        GameCore c = new GameCore(new Mem(), 31L);
        c.startGame();

        // Let the spawner produce one naturally so spawn geometry is what is under test.
        while (c.enemies.isEmpty()) c.update(DT, L);
        GameCore.Enemy e = c.enemies.get(0);
        check("words spawn fully above the top edge", e.y + L.enemyR < 0);
        check("entrance starts hidden", e.enterT == 0f);

        // Entrance is keyed to position, so it must still be mid-way at the top edge.
        while (c.enemies.contains(e) && e.y < 0) c.update(DT, L);
        check("entrance is partway as the word crosses the edge",
                e.enterT > 0f && e.enterT < 1f);
        while (c.enemies.contains(e) && e.y < L.enemyR * 2.5f) c.update(DT, L);
        check("entrance completes once fully on screen", e.enterT == 1f);
        check("word descends into the play area", e.y > 0);

        // A word's row must not reflow as it is typed.
        c.enemies.clear();
        c.target = null;
        GameCore.Enemy w = add(c, L, new int[] {0, 1, 2}, L.playTop + 100);
        float x0 = c.tileX(w, 0, L), x1 = c.tileX(w, 1, L), x2 = c.tileX(w, 2, L);
        c.tapKey(0, L);
        check("typing a letter leaves it in the word", w.word.length == 3 && w.pos == 1);
        check("cleared letter holds its position", c.tileX(w, 0, L) == x0);
        check("remaining letters do not shift", c.tileX(w, 1, L) == x1 && c.tileX(w, 2, L) == x2);
        c.tapKey(1, L);
        check("second letter also stays put", c.tileX(w, 1, L) == x1 && w.pos == 2);
        check("word is still three letters long", w.word.length == 3);

        // Tiles must not overlap at the widest word.
        check("tile spacing exceeds two head radii",
                L.enemyStep > 2f * Layout.HEAD_SCALE * L.enemyR * 0.99f);
        check("widest word still fits the play area",
                L.wordWidth(5) < L.playRight - L.playLeft);
    }

}
