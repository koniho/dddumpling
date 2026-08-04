package com.sram.hexatype;

/** Layout geometry, targeting, engagement, scoring, breaches and screen transitions. */
final class TestRules extends Check {

    static void layout(Layout L) {
        group("layout");
        for (int g = 0; g < Glyph.COUNT; g++) {
            check("key " + g + " inside width",
                    L.keyX[g] - L.keyR >= 0 && L.keyX[g] + L.keyR <= L.w);
            check("key " + g + " above bottom inset",
                    L.keyY[g] + Layout.SQ3_2 * L.keyR <= L.h - L.padB + 0.5f);
            check("key " + g + " hit-tests to itself", L.keyAt(L.keyX[g], L.keyY[g]) == g);
        }
        float clusterGap = (L.keyX[3] - L.keyR) - (L.keyX[2] + L.keyR);
        check("thumb clusters are separated", clusterGap > 0.6f * L.keyR);
        check("chevron tiles are hex-adjacent, not overlapping",
                dist(L, 0, 1) >= Layout.SQ3_2 * L.keyR * 1.98f - 0.5f);
        check("danger line above key deck", L.dangerY < L.keyTop);
        check("danger line below play top", L.dangerY > L.playTop);
        check("longest word fits the play area", L.wordWidth(5) < L.playRight - L.playLeft);
        // Regression: with immersive insets reporting 0, the centred HUD landed at y=24
        // on a 1080x2400 screen, i.e. underneath the punch-hole camera.
        Layout zero = new Layout();
        zero.compute(1080, 2400, 0, 0, 0, 0);
        float stageTop = zero.hudY - 0.95f * zero.unit - 0.58f * zero.unit;
        check("HUD clears the top edge even with zero insets", stageTop > 0.045f * 2400);
        check("HUD sits below the safe top", zero.hudY > zero.topSafe);
        check("play area starts below the HUD", zero.playTop > zero.hudY - zero.unit);
        Layout inset = new Layout();
        inset.compute(1080, 2400, 0, 140, 0, 60);
        check("a real top inset is still honoured", inset.topSafe >= 140f);

        check("tap between clusters hits nothing",
                L.keyAt((L.keyX[2] + L.keyX[3]) / 2f, L.keyY[0]) == -1);
        check("tap in the play field hits nothing", L.keyAt(L.w / 2f, L.h * 0.4f) == -1);
    }

    static float dist(Layout L, int a, int b) {
        float dx = L.keyX[a] - L.keyX[b], dy = L.keyY[a] - L.keyY[b];
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    static void targeting(Layout L) {
        group("targeting");
        GameCore c = new GameCore(new Mem(), 1L);
        c.startGame();
        c.enemies.clear();
        GameCore.Enemy high = add(c, L, new int[] {0, 1}, L.playTop + 100);
        GameCore.Enemy low = add(c, L, new int[] {0, 2}, L.playTop + 600);

        c.tapKey(0, L);
        check("locks the most urgent matching word", c.target == low);
        check("consumed one glyph", low.pos == 1 && high.pos == 0);

        int comboBefore = c.combo;
        c.tapKey(1, L);   // wrong for `low`, and must not jump across to `high` either
        check("wrong key releases the lock", c.target == null);
        check("wrong key does not advance another word", high.pos == 0);
        check("wrong key breaks the combo", c.combo == 0 && comboBefore > 0);
        check("wrong key restarts the engaged word", low.pos == 0);
        check("restart flags the word for a flash", low.failPulse > 0f);
        check("restart keeps every letter", low.word.length == 2);
        check("the indicator lets go too", c.caretOwner == null);

        // Re-engaging is a deliberate press, and picks the most urgent match again.
        c.tapKey(0, L);
        check("re-engaging locks the lowest matching word", c.target == low);
        check("re-engaging advances that word", low.pos == 1);
        check("re-engaging leaves the higher word alone", high.pos == 0);
        c.tapKey(2, L);
        check("finishing a word releases the lock", c.target == null);
        check("finished word is marked dying", low.dying);
        check("a doomed word stops warning at once", low.warn == 0f);

        c.tapKey(3, L);
        check("key matching nothing is a miss", c.target == null && c.combo == 0);
    }

    static void engagement(Layout L) {
        group("engagement");
        GameCore c = new GameCore(new Mem(), 101L);
        c.startGame();
        c.enemies.clear();
        c.target = null;

        // Three words all starting with the same letter, at different heights.
        GameCore.Enemy top = add(c, L, new int[] {2, 0}, L.playTop + 80);
        GameCore.Enemy mid = add(c, L, new int[] {2, 1}, L.playTop + 500);
        GameCore.Enemy low = add(c, L, new int[] {2, 3}, L.playTop + 900);

        c.tapKey(2, L);
        check("engages the word closest to the player", c.target == low);
        check("higher words are untouched", top.pos == 0 && mid.pos == 0);

        // Clear the lowest, then the next press must take the next-lowest.
        c.tapKey(3, L);
        advance(c, L, GameCore.DESTROY_TIME + 0.3f);
        c.tapKey(2, L);
        check("next engagement takes the new lowest", c.target == mid);
        check("the topmost word is still untouched", top.pos == 0);

        // A partially typed word is matched on its next letter, not its first.
        c.enemies.clear();
        c.target = null;
        GameCore.Enemy partial = add(c, L, new int[] {0, 4}, L.playTop + 700);
        GameCore.Enemy fresh = add(c, L, new int[] {4, 0}, L.playTop + 300);
        c.tapKey(0, L);
        check("engaged the partial word", c.target == partial && partial.pos == 1);
        c.tapKey(1, L);   // wrong: drops the lock and resets `partial`
        check("lock released", c.target == null && partial.pos == 0);

        // Now 4 matches `fresh` at its first letter and `partial` at its second, but
        // `partial` was reset, so only `fresh` matches — and it is higher up.
        c.tapKey(4, L);
        check("matching is on the next needed letter", c.target == fresh);

        // A word already flying apart must never be engaged.
        c.enemies.clear();
        c.target = null;
        GameCore.Enemy dead = add(c, L, new int[] {5}, L.playTop + 900);
        GameCore.Enemy alive = add(c, L, new int[] {5, 5}, L.playTop + 200);
        c.tapKey(5, L);
        advance(c, L, 0.2f);
        check("the cleared word is flying apart", dead.destroyed);
        c.tapKey(5, L);
        check("engagement skips a word being destroyed", c.target == alive);
    }

    static void scoringAndStages(Layout L) {
        group("scoring and stages");
        Mem store = new Mem();
        GameCore c = new GameCore(store, 2L);
        c.startGame();
        check("starts at stage 1 with full lives",
                c.stage == 1 && c.lives == GameCore.START_LIVES && c.score == 0);

        int scored = 0;
        for (int k = 1; k <= 4; k++) {
            c.enemies.clear();
            c.target = null;
            add(c, L, new int[] {1, 1}, L.playTop + 50);
            c.tapKey(1, L);
            c.tapKey(1, L);
            for (int i = 0; i < 12; i++) c.update(DT, L);   // let the killing shot land
            check("kill " + k + " registered", c.kills == k);
            check("score increased on kill " + k, c.score > scored);
            scored = c.score;
        }
        check("combo tracked", c.maxCombo >= 4);

        c.stage = 2;
        check("stage 2 is faster than stage 1", c.travelSeconds() < 15f);
        check("stage 2 spawns sooner", c.spawnInterval() < 2.5f);

        c.stage = 1;
        int w1 = c.maxWordLen();
        c.stage = 6;
        check("later stages use longer words", c.maxWordLen() > w1);
        check("word length is capped at 5", c.maxWordLen() <= 5);
        c.stage = 40;
        check("pacing floors out", c.travelSeconds() >= 4.2f && c.spawnInterval() >= 0.80f);
        check("enemy count is capped", c.maxEnemies() <= 7);
    }

    static void breachAndGameOver(Layout L) {
        group("breach and game over");
        Mem store = new Mem();
        GameCore c = new GameCore(store, 3L);
        c.startGame();
        c.score = 500;

        for (int i = 1; i <= GameCore.START_LIVES; i++) {
            c.enemies.clear();
            c.target = null;
            GameCore.Enemy e = add(c, L, new int[] {0, 0}, L.dangerY - L.enemyR + 1);
            c.update(DT, L);
            check("crossing the line starts the attack lunge", e.attacking);
            check("a lunging word cannot be typed", !e.typeable());
            check("lunge holds the life for now", c.lives == GameCore.START_LIVES - i + 1);
            advance(c, L, GameCore.ATTACK_TIME + DT);
            check("breach " + i + " costs a life", c.lives == GameCore.START_LIVES - i);
            // Not isEmpty(): the spawner keeps running while the lunge plays out.
            check("breach " + i + " removes the enemy", !c.enemies.contains(e));
        }
        check("no lives left means game over", c.state == GameCore.OVER);
        check("best score persisted", store.best == 500 && store.saves == 1);

        GameCore c2 = new GameCore(store, 4L);
        check("best is reloaded on a fresh game", c2.best == 500);
        c2.startGame();
        check("restart resets score and lives",
                c2.score == 0 && c2.lives == GameCore.START_LIVES && c2.stage == 1);
        check("restart keeps best", c2.best == 500);
    }

    static void screens(Layout L) {
        group("screens");
        GameCore c = new GameCore(new Mem(), 6L);
        check("boots to the title screen", c.state == GameCore.TITLE);
        c.anyTap();
        check("tap starts the game", c.state == GameCore.PLAY);

        c.lives = 1;
        c.enemies.clear();
        add(c, L, new int[] {0}, L.dangerY - L.enemyR + 1);
        advance(c, L, GameCore.ATTACK_TIME + 2 * DT);
        check("reached game over", c.state == GameCore.OVER);
        check("game over clears the field", c.enemies.isEmpty() && c.shots.isEmpty());

        c.anyTap();
        check("game over ignores taps for the first 0.6s", c.state == GameCore.OVER);
        for (int i = 0; i < 45; i++) c.update(DT, L);
        c.anyTap();
        check("game over restarts after the grace period", c.state == GameCore.PLAY);

        check("keys are inert on non-play screens", !new GameCore(new Mem(), 9L).tapKey(0, L));
    }

}
