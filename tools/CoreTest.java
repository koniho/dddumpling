package com.sram.hexatype;

/** Headless assertions over layout and rules. Run before every APK build. */
final class CoreTest {

    private static final float DT = 1f / 60f;
    private static int pass, fail;

    private static final class Mem implements GameCore.Store {
        int best;
        int saves;
        public int loadBest() { return best; }
        public void saveBest(int b) { best = b; saves++; }
    }

    public static void main(String[] args) {
        Layout L = new Layout();
        L.compute(1080, 2340, 0, 60, 0, 90);

        layout(L);
        targeting(L);
        scoringAndStages(L);
        breachAndGameOver(L);
        screens(L);
        entranceAndPersistence(L);
        warningsAndHarm(L);
        perfectPlaySurvives(L);
        fuzz(L);

        System.out.printf("%n%d passed, %d failed%n", pass, fail);
        if (fail > 0) System.exit(1);
    }

    // ---- cases --------------------------------------------------------------

    private static void layout(Layout L) {
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

    private static float dist(Layout L, int a, int b) {
        float dx = L.keyX[a] - L.keyX[b], dy = L.keyY[a] - L.keyY[b];
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static void targeting(Layout L) {
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
        c.tapKey(1, L);   // wrong for `low`, right for `high` — the lock must hold
        check("wrong key does not steal the lock", c.target == low);
        check("wrong key does not advance another word", high.pos == 0);
        check("wrong key breaks the combo", c.combo == 0 && comboBefore > 0);
        check("wrong key restarts the engaged word", low.pos == 0);
        check("restart flags the word for a flash", low.failPulse > 0f);
        check("restart keeps every letter", low.word.length == 2);

        // Retype it from the top.
        c.tapKey(0, L);
        check("retyping the restarted word advances again", low.pos == 1);
        c.tapKey(2, L);
        check("finishing a word releases the lock", c.target == null);
        check("finished word is marked dying", low.dying);

        c.tapKey(3, L);
        check("key matching nothing is a miss", c.target == null && c.combo == 0);
    }

    private static void scoringAndStages(Layout L) {
        group("scoring and stages");
        Mem store = new Mem();
        GameCore c = new GameCore(store, 2L);
        c.startGame();
        check("starts at stage 1 with full lives",
                c.stage == 1 && c.lives == GameCore.START_LIVES && c.score == 0);

        int scored = 0;
        for (int k = 1; k <= 8; k++) {
            c.enemies.clear();
            c.target = null;
            GameCore.Enemy e = add(c, L, new int[] {1, 1}, L.playTop + 50);
            c.tapKey(1, L);
            c.tapKey(1, L);
            for (int i = 0; i < 12; i++) c.update(DT, L);   // let the killing shot land
            check("kill " + k + " registered", c.kills == k);
            check("score increased on kill " + k, c.score > scored);
            scored = c.score;
        }
        check("stage advances after 8 kills", c.stage == 2);
        check("stage 2 is faster than stage 1", c.travelSeconds() < 15f);
        check("stage 2 spawns sooner", c.spawnInterval() < 2.5f);
        check("combo tracked", c.maxCombo >= 8);

        c.stage = 1;
        int w1 = c.maxWordLen();
        c.stage = 6;
        check("later stages use longer words", c.maxWordLen() > w1);
        check("word length is capped at 5", c.maxWordLen() <= 5);
        c.stage = 40;
        check("pacing floors out", c.travelSeconds() >= 4.2f && c.spawnInterval() >= 0.80f);
        check("enemy count is capped", c.maxEnemies() <= 7);
    }

    private static void breachAndGameOver(Layout L) {
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

    private static void screens(Layout L) {
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

    private static void entranceAndPersistence(Layout L) {
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

    private static void warningsAndHarm(Layout L) {
        group("warnings and harm");
        GameCore c = new GameCore(new Mem(), 21L);
        c.startGame();
        c.enemies.clear();

        GameCore.Enemy far = add(c, L, new int[] {0, 0}, L.playTop + 20);
        c.update(DT, L);
        check("a distant word raises no alarm", far.warn == 0f && c.warnLevel == 0f);

        c.enemies.clear();
        float band = (L.dangerY - L.playTop) * 0.20f;
        GameCore.Enemy near = add(c, L, new int[] {0, 0}, L.dangerY - band * 0.4f);
        c.update(DT, L);
        check("a closing word raises the alarm", near.warn > 0.5f && near.warn <= 1f);
        check("warnLevel follows the worst offender", c.warnLevel == near.warn);
        check("a closing word is still typeable", near.typeable());

        check("full health means no red tint", c.harm() == 0f);
        c.lives = 2;
        float h2 = c.harm();
        c.lives = 1;
        check("harm rises as lives fall", c.harm() > h2 && h2 > 0f);
        check("harm peaks below or at 1", c.harm() <= 1f);
        c.lives = 0;
        c.state = GameCore.OVER;
        check("no tint outside play", c.harm() == 0f);

        for (int i = 0; i < 6; i++) {
            check("cycle colour " + i + " is opaque", (Glyph.cycle(i / 6f) >>> 24) == 0xFF);
        }
        check("cycle wraps", Glyph.cycle(0f) == Glyph.cycle(1f));
    }

    private static void perfectPlaySurvives(Layout L) {
        group("perfect play");
        GameCore c = new GameCore(new Mem(), 8L);
        c.startGame();
        int frames = 0;
        while (frames < 60 * 120 && c.state == GameCore.PLAY) {
            c.update(DT, L);
            frames++;
            if (frames % 2 != 0) continue;
            GameCore.Enemy e = c.target != null && c.enemies.contains(c.target) && !c.target.dying
                    ? c.target : urgent(c);
            if (e != null && e.pos < e.word.length) c.tapKey(e.word[e.pos], L);
        }
        System.out.printf("    120s of perfect play: stage=%d score=%d kills=%d lives=%d%n",
                c.stage, c.score, c.kills, c.lives);
        check("perfect play keeps all lives", c.lives == GameCore.START_LIVES);
        check("perfect play survives 120s", c.state == GameCore.PLAY);
        check("perfect play reaches a late stage", c.stage >= 6);
        check("score accumulates", c.score > 1000);
    }

    private static void fuzz(Layout L) {
        group("fuzz");
        java.util.Random r = new java.util.Random(1234L);
        GameCore c = new GameCore(new Mem(), 12L);
        c.startGame();
        boolean sane = true;
        int overs = 0;
        for (int i = 0; i < 60 * 600; i++) {
            c.update(DT, L);
            if (r.nextInt(6) == 0) c.tapKey(r.nextInt(Glyph.COUNT), L);
            if (r.nextInt(4000) == 0) c.anyTap();
            if (c.state == GameCore.OVER) {
                overs++;
                for (int k = 0; k < 45; k++) c.update(DT, L);
                c.anyTap();
            }
            if (c.lives < 0 || c.score < 0 || c.enemies.size() > c.maxEnemies() + 1
                    || c.combo < 0) {
                sane = false;
                break;
            }
        }
        check("10 minutes of random mashing stays consistent", sane);
        check("random play does reach game over", overs > 0);
        System.out.printf("    fuzz: %d game-overs, %d particles live, %d shots live%n",
                overs, c.particles.size(), c.shots.size());
    }

    // ---- helpers ------------------------------------------------------------

    private static GameCore.Enemy urgent(GameCore c) {
        GameCore.Enemy best = null;
        for (int i = 0; i < c.enemies.size(); i++) {
            GameCore.Enemy e = c.enemies.get(i);
            if (e.dying) continue;
            if (best == null || e.y > best.y) best = e;
        }
        return best;
    }

    private static GameCore.Enemy add(GameCore c, Layout L, int[] word, float y) {
        GameCore.Enemy e = new GameCore.Enemy();
        e.word = word;
        e.baseX = (L.playLeft + L.playRight) / 2f;
        e.y = y;
        e.speed = 0f;
        e.sway = 0f;
        c.enemies.add(e);
        return e;
    }

    private static void advance(GameCore c, Layout L, float seconds) {
        for (float t = 0; t < seconds; t += DT) c.update(DT, L);
    }

    private static void group(String name) {
        System.out.println("\n[" + name + "]");
    }

    private static void check(String what, boolean ok) {
        if (ok) {
            pass++;
            System.out.println("  ok   " + what);
        } else {
            fail++;
            System.out.println("  FAIL " + what);
        }
    }
}
