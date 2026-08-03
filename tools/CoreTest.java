package com.sram.hexatype;

/** Headless assertions over layout and rules. Run before every APK build. */
final class CoreTest {

    private static final float DT = 1f / 60f;
    private static int pass, fail;

    private static final class Mem implements GameCore.Store {
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

    public static void main(String[] args) {
        Layout L = new Layout();
        L.compute(1080, 2340, 0, 60, 0, 90);

        layout(L);
        targeting(L);
        engagement(L);
        scoringAndStages(L);
        breachAndGameOver(L);
        screens(L);
        stackedLetters(L);
        accuracyTracking(L);
        audio(L);
        settings(L);
        destruction(L);
        indicatorsAndGlow(L);
        sky(L);
        waves(L);
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

        c.tapKey(3, L);
        check("key matching nothing is a miss", c.target == null && c.combo == 0);
    }

    /** Engagement always takes the most urgent match, not the oldest or nearest word. */
    private static void engagement(Layout L) {
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

    private static void scoringAndStages(Layout L) {
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

    private static void stackedLetters(Layout L) {
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
        int worst = 0, maxNeed = 0, stacksSeen = 0, words = 0;
        boolean overCap = false, badNeed = false;
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
                }
            }
        }
        System.out.printf("    %d words generated, worst total = %d presses, deepest stack = %d,"
                + " %d stacked tiles%n", words, worst, maxNeed, stacksSeen);
        check("no word ever exceeds " + GameCore.MAX_PRESSES + " presses", !overCap);
        check("every tile needs 1..4 presses", !badNeed);
        check("stacks do get generated", stacksSeen > 0);
        check("stacks reach depth 4 somewhere", maxNeed == 4);

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

    private static float chanceAt(GameCore c, int stage) {
        int was = c.stage;
        c.stage = stage;
        float v = c.stackChance();
        c.stage = was;
        return v;
    }

    private static void accuracyTracking(Layout L) {
        group("accuracy");
        GameCore c = new GameCore(new Mem(), 61L);
        c.startGame();
        check("accuracy starts at 100%", c.accuracyPercent() == 100);
        check("no presses means the happiest face", c.accuracyMood() == 1f);

        c.enemies.clear();
        c.target = null;
        add(c, L, new int[] {0, 1, 2, 3}, L.playTop + 90);
        c.tapKey(0, L);
        check("a correct press counts as a hit", c.hits == 1 && c.misses == 0);
        c.tapKey(5, L);
        check("a wrong press counts as a miss", c.hits == 1 && c.misses == 1);
        check("accuracy halves at one for one", c.accuracyPercent() == 50);
        check("50% is the saddest face", c.accuracyMood() == 0f);

        // Push to 75%: three hits, one miss.
        c.tapKey(0, L);
        c.tapKey(1, L);
        check("accuracy climbs with hits", c.accuracyPercent() == 75);
        check("75% sits between the extremes",
                c.accuracyMood() > 0f && c.accuracyMood() < 1f);

        // Threshold checks, driven directly.
        c.hits = 60; c.misses = 40;
        check("60% is the saddest face", c.accuracyPercent() == 60 && c.accuracyMood() == 0f);
        c.hits = 59; c.misses = 41;
        check("below 60% stays saddest", c.accuracyMood() == 0f);
        c.hits = 90; c.misses = 10;
        check("90% is the happiest face", c.accuracyPercent() == 90 && c.accuracyMood() == 1f);
        c.hits = 97; c.misses = 3;
        check("above 90% stays happiest", c.accuracyMood() == 1f);
        c.hits = 75; c.misses = 25;
        check("75% maps to the midpoint", Math.abs(c.accuracyMood() - 0.5f) < 0.001f);

        // A flawless wave earns the gold dumpling; a single miss forfeits it.
        GameCore g = new GameCore(new Mem(), 62L);
        g.startGame();
        g.spawnedThisStage = g.stageQuota();
        g.enemies.clear();
        g.shots.clear();
        g.update(DT, L);
        check("a flawless wave triggers the gold dumpling", g.perfectBanner > 0f);
        check("the celebration expires", g.perfectBanner <= GameCore.PERFECT_TIME);
        advance(g, L, GameCore.PERFECT_TIME + 0.2f);
        check("the celebration ends", g.perfectBanner == 0f);

        GameCore m = new GameCore(new Mem(), 63L);
        m.startGame();
        m.enemies.clear();
        m.tapKey(0, L);   // nothing to hit: a miss
        check("the miss is recorded against the stage", m.missesThisStage == 1);
        m.spawnedThisStage = m.stageQuota();
        m.enemies.clear();
        m.shots.clear();
        m.update(DT, L);
        check("a wave with a miss earns no gold dumpling", m.perfectBanner == 0f);
        check("stage misses reset for the next wave", m.missesThisStage == 0);
        check("run totals are not reset by the stage", m.misses == 1);
    }

    /** Records which effects fired, so the rules can be checked against the audio events. */
    private static final class Ear implements GameCore.Sound {
        int squishes, clears, wrongs, damages, achievements;
        int lastGlyph = -1, lastDepth = -1;
        int music = -1, musicCalls;
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
    }

    private static void audio(Layout L) {
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

    private static boolean silentRunSurvives(Layout L) {
        GameCore c = new GameCore(new Mem(), 74L);
        c.sound = null;
        c.startGame();
        for (int i = 0; i < 600; i++) {
            c.update(DT, L);
            c.tapKey(i % Glyph.COUNT, L);
        }
        return true;
    }

    private static void settings(Layout L) {
        group("settings");
        Mem store = new Mem();
        GameCore c = new GameCore(store, 81L);
        Ear ear = new Ear();
        c.sound = ear;
        c.startGame();
        check("defaults to normal speed", c.speed == 1f);
        check("settings start closed", !c.settingsOpen);

        // Opening freezes the simulation.
        c.enemies.clear();
        GameCore.Enemy e = add(c, L, new int[] {0, 1}, L.playTop + 100);
        e.speed = 200f;
        c.update(DT, L);
        float movedY = e.y;
        c.openSettings();
        check("opening settings pauses", c.settingsOpen);
        advance(c, L, 1.0f);
        check("nothing moves while paused", e.y == movedY);
        check("the clock still runs so the panel animates", c.clock > 0f);
        c.closeSettings();
        c.update(DT, L);
        check("closing resumes the simulation", e.y > movedY);

        // Speed clamps and persists.
        c.setSpeed(1.3f);
        check("speed applies", Math.abs(c.speed - 1.3f) < 1e-6f);
        check("speed persists", Math.abs(store.speed - 1.3f) < 1e-6f && store.speedSaves == 1);
        c.setSpeed(9f);
        check("speed clamps at the top", c.speed == GameCore.SPEED_MAX);
        c.setSpeed(-4f);
        check("speed clamps at the bottom", c.speed == GameCore.SPEED_MIN);
        check("a corrupt stored speed falls back", GameCore.clampSpeed(Float.NaN) == 1f);

        GameCore reloaded = new GameCore(store, 82L);
        check("stored speed is reloaded",
                Math.abs(reloaded.speed - GameCore.SPEED_MIN) < 1e-6f);

        // Faster speed means less time to react and tighter spawns.
        c.stage = 3;
        c.setSpeed(0.5f);
        float slowTravel = c.travelSeconds(), slowSpawn = c.spawnInterval();
        c.setSpeed(1.5f);
        check("higher speed shortens the fall", c.travelSeconds() < slowTravel);
        check("higher speed tightens spawns", c.spawnInterval() < slowSpawn);
        check("the fall floor still applies at max speed", c.travelSeconds() > 0f);

        // Music selection persists and notifies the audio layer.
        c.setBgm(Music.MARCH);
        check("music choice applies", c.bgmChoice == Music.MARCH);
        check("music choice persists", store.bgm == Music.MARCH && store.bgmSaves == 1);
        check("the audio layer is told", ear.music == Music.MARCH && ear.musicCalls == 1);
        c.setBgm(-1);
        check("a bogus low choice is ignored", c.bgmChoice == Music.MARCH);
        c.setBgm(Music.NAMES.length);
        check("a bogus high choice is ignored", c.bgmChoice == Music.MARCH);
        c.setBgm(Music.OFF);
        check("music can be turned off", c.bgmChoice == Music.OFF);

        // Every synth style must produce a clean, correctly sized loop.
        boolean stylesOk = true;
        for (int style = 0; style < Music.NAMES.length; style++) {
            if (!Music.isSynth(style)) continue;
            short[] loop = Music.loop(style);
            if (loop.length != Music.loopFrames(style)) stylesOk = false;
            int max = 0;
            for (int i = 0; i < loop.length; i++) max = Math.max(max, Math.abs(loop[i]));
            if (max >= 32767 || max < 2000) stylesOk = false;
        }
        check("every music style renders cleanly", stylesOk);
        check("OFF and MY TRACK are not synth styles",
                !Music.isSynth(Music.OFF) && !Music.isSynth(Music.CUSTOM));
        check("an unknown style still returns audio", Music.loop(99).length > 0);

        // Panel hit-testing.
        SettingsUi ui = new SettingsUi();
        ui.compute(L, Music.NAMES.length);
        check("panel fits on screen",
                ui.panelT >= L.topSafe && ui.panelB <= L.h && ui.panelL > 0);
        check("a tap outside closes",
                ui.hit(L.w / 2f, ui.panelB + 20f) == SettingsUi.HIT_OUTSIDE);
        check("the close button is hit", ui.hit(ui.closeCx, ui.closeCy) == SettingsUi.HIT_CLOSE);
        check("the slider is hit",
                ui.hit((ui.sliderL + ui.sliderR) / 2f, ui.sliderY) == SettingsUi.HIT_SLIDER);
        boolean rowsOk = true;
        for (int i = 0; i < Music.NAMES.length; i++) {
            if (ui.hit(ui.optionL() + 5f, ui.optionCy(i)) != SettingsUi.HIT_OPTION + i) {
                rowsOk = false;
            }
        }
        check("every music row is hittable", rowsOk);
        check("slider left end reads minimum", ui.speedAt(ui.sliderL) == GameCore.SPEED_MIN);
        check("slider right end reads maximum", ui.speedAt(ui.sliderR) == GameCore.SPEED_MAX);
        check("slider clamps past its ends",
                ui.speedAt(ui.sliderL - 500f) == GameCore.SPEED_MIN
                        && ui.speedAt(ui.sliderR + 500f) == GameCore.SPEED_MAX);
        check("slider midpoint is centre speed",
                Math.abs(ui.speedAt((ui.sliderL + ui.sliderR) / 2f) - 1f) < 0.03f);
        check("knob tracks the value",
                Math.abs(ui.knobX(GameCore.SPEED_MIN) - ui.sliderL) < 0.5f
                        && Math.abs(ui.knobX(GameCore.SPEED_MAX) - ui.sliderR) < 0.5f);

        // The stage readout is the settings button, and must not swallow key taps.
        check("the stage readout opens settings", L.inStageTap(L.w / 2f, L.hudY));
        boolean keysClear = true;
        for (int g = 0; g < Glyph.COUNT; g++) {
            if (L.inStageTap(L.keyX[g], L.keyY[g])) keysClear = false;
        }
        check("the settings region does not cover any key", keysClear);
        check("mid-field taps do not open settings", !L.inStageTap(L.w / 2f, L.h * 0.5f));
    }

    private static void destruction(Layout L) {
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
        int killsBefore = c.kills;
        advance(c, L, 0.2f);                       // let the killing shot land
        check("impact starts the destroy animation", e.destroyed && e.destroyT >= 0f);
        check("the kill is credited at impact", c.kills == killsBefore + 1);
        check("destruction shakes the screen", c.shake > shakeBefore);
        check("a destroyed word is not typeable", !e.typeable());
        check("a destroyed word lingers on the field", c.enemies.contains(e));

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
        check("stage advances once the animation completes", w.stage == 2);
    }

    private static void indicatorsAndGlow(Layout L) {
        group("indicator and glow");

        // The red edge glow must not survive the run that caused it.
        GameCore c = new GameCore(new Mem(), 93L);
        c.startGame();
        c.lives = 1;
        c.enemies.clear();
        add(c, L, new int[] {0}, L.dangerY - L.enemyR + 1);
        advance(c, L, GameCore.ATTACK_TIME * 0.5f);
        check("a lunging word lights the edge glow", c.warnLevel > 0f);
        advance(c, L, GameCore.ATTACK_TIME + 4 * DT);
        check("reached game over", c.state == GameCore.OVER);
        check("the edge glow clears on game over", c.warnLevel == 0f);
        advance(c, L, 1.0f);
        check("and stays clear", c.warnLevel == 0f);

        // Non-fatal damage must also clear it.
        GameCore d = new GameCore(new Mem(), 94L);
        d.startGame();
        d.enemies.clear();
        add(d, L, new int[] {0}, L.dangerY - L.enemyR + 1);
        advance(d, L, GameCore.ATTACK_TIME + 4 * DT);
        d.enemies.clear();
        d.update(DT, L);
        check("the glow clears after surviving a hit", d.warnLevel == 0f);

        // The lock indicator eases between letters rather than jumping.
        GameCore k = new GameCore(new Mem(), 95L);
        k.startGame();
        k.enemies.clear();
        k.target = null;
        GameCore.Enemy e = add(k, L, new int[] {0, 1, 2}, L.playTop + 150);
        k.tapKey(0, L);
        k.update(DT, L);
        float atFirst = k.caretXFor(e, L);
        check("indicator starts on the locked word", k.caretOwner == e);
        k.tapKey(1, L);
        k.update(DT, L);
        float justAfter = k.caretXFor(e, L);
        float destination = k.tileX(e, e.pos, L);
        check("indicator has begun moving", justAfter != atFirst);
        check("indicator has not jumped straight there",
                Math.abs(justAfter - destination) > 1f);
        check("indicator is heading the right way",
                Math.abs(justAfter - destination) < Math.abs(atFirst - destination));
        advance(k, L, 0.5f);
        check("indicator arrives", Math.abs(k.caretXFor(e, L) - k.tileX(e, e.pos, L)) < 1f);

        // Switching words snaps instead of gliding across the screen.
        k.enemies.clear();
        k.target = null;
        GameCore.Enemy other = add(k, L, new int[] {3, 4}, L.playTop + 400);
        other.baseX = L.playLeft + L.enemyR * 3f;
        k.tapKey(3, L);
        k.update(DT, L);
        check("a fresh lock snaps into place",
                Math.abs(k.caretXFor(other, L) - k.tileX(other, other.pos, L)) < 1f);
    }

    private static void sky(Layout L) {
        group("cloud sky");
        GameCore c = new GameCore(new Mem(), 111L);
        c.startGame();

        check("three cloud layers", GameCore.CLOUD_LAYERS == 3);
        check("one layer in front, two behind", Renderer.CLOUD_FRONT_LAYER == 2);

        boolean speedsRise = true;
        for (int l = 1; l < GameCore.CLOUD_LAYERS; l++) {
            if (GameCore.CLOUD_SPEED[l] <= GameCore.CLOUD_SPEED[l - 1]) speedsRise = false;
        }
        check("each layer drifts faster than the one behind it", speedsRise);
        check("all layers actually move", GameCore.CLOUD_SPEED[0] > 0f);

        // Phases must stay in 0..1 and wrap, never run away.
        boolean inRange = true, moved = true, wrapped = false;
        float[][] before = new float[GameCore.CLOUD_LAYERS][GameCore.CLOUDS_PER_LAYER];
        for (int l = 0; l < GameCore.CLOUD_LAYERS; l++) {
            for (int i = 0; i < GameCore.CLOUDS_PER_LAYER; i++) {
                before[l][i] = c.cloudPhase(l, i);
                if (before[l][i] < 0f || before[l][i] >= 1f) inRange = false;
            }
        }
        advance(c, L, 3f);
        for (int l = 0; l < GameCore.CLOUD_LAYERS; l++) {
            for (int i = 0; i < GameCore.CLOUDS_PER_LAYER; i++) {
                float now = c.cloudPhase(l, i);
                if (now < 0f || now >= 1f) inRange = false;
                if (now == before[l][i]) moved = false;
            }
        }
        check("cloud phases stay inside 0..1", inRange);
        check("clouds drift with the clock", moved);

        // Long run: the front layer must wrap many times and stay bounded.
        advance(c, L, 400f);
        for (int l = 0; l < GameCore.CLOUD_LAYERS; l++) {
            for (int i = 0; i < GameCore.CLOUDS_PER_LAYER; i++) {
                float v = c.cloudPhase(l, i);
                if (v < 0f || v >= 1f) inRange = false;
            }
        }
        // 400s at the front-layer speed is well over one full traversal.
        wrapped = 400f * GameCore.CLOUD_SPEED[Renderer.CLOUD_FRONT_LAYER] > 1f;
        check("phases stay bounded over a long run", inRange);
        check("the front layer wraps repeatedly", wrapped);

        // Drift is a pure function of the clock, so two cores at the same time agree.
        GameCore d = new GameCore(new Mem(), 222L);
        d.clock = c.clock;
        boolean deterministic = true;
        for (int l = 0; l < GameCore.CLOUD_LAYERS; l++) {
            for (int i = 0; i < GameCore.CLOUDS_PER_LAYER; i++) {
                if (Math.abs(d.cloudPhase(l, i) - c.cloudPhase(l, i)) > 1e-5f) {
                    deterministic = false;
                }
            }
        }
        check("cloud drift is identical for any core at the same clock", deterministic);

        // A cloud must be entirely out of the sky before its phase wraps, or it pops out
        // of existence mid-screen.
        boolean exitsCleanly = true, entersCleanly = true;
        for (int l = 0; l < GameCore.CLOUD_LAYERS; l++) {
            float margin = Renderer.cloudMargin(L, l);
            GameCore z = new GameCore(new Mem(), 333L);
            z.clock = 0f;
            for (int i = 0; i < GameCore.CLOUDS_PER_LAYER; i++) {
                z.cloudY[l][i] = 0f;                 // phase 0: just entering
                if (Renderer.cloudY(z, L, l, i) + margin > 0.5f) entersCleanly = false;
                z.cloudY[l][i] = 0.99999f;           // phase ~1: just leaving
                if (Renderer.cloudY(z, L, l, i) - margin < L.deckTop - 0.5f) {
                    exitsCleanly = false;
                }
            }
        }
        check("clouds start fully above the sky", entersCleanly);
        check("clouds leave fully below the sky before wrapping", exitsCleanly);
        boolean marginsCover = true;
        for (int l = 0; l < GameCore.CLOUD_LAYERS; l++) {
            if (Renderer.cloudMargin(L, l) <= Renderer.cloudHeight(L, l)) marginsCover = false;
        }
        check("the exit margin exceeds the cloud height", marginsCover);

        boolean spread = true;
        for (int l = 0; l < GameCore.CLOUD_LAYERS; l++) {
            for (int i = 0; i < GameCore.CLOUDS_PER_LAYER; i++) {
                if (c.cloudX[l][i] < 0f || c.cloudX[l][i] > 1f) spread = false;
                if (c.cloudW[l][i] <= 0f) spread = false;
            }
        }
        check("cloud placement is on screen and sized", spread);

        // The clip is what lets clouds run past the sky's edge without touching the key
        // deck, so it is worth checking directly rather than trusting it.
        RasterPainter rp = new RasterPainter(40, 40, 1);
        rp.clear(0xFF000000);
        rp.save();
        rp.clipRect(0, 0, 40, 20);
        rp.fillRect(0, 0, 40, 40, 0xFFFFFFFF);
        rp.restore();
        int[] px = rp.resolve();
        check("clip keeps painting inside the region", (px[5 * 40 + 5] & 0xFF) > 200);
        check("clip blocks painting outside the region", (px[30 * 40 + 5] & 0xFF) < 40);

        rp.fillRect(0, 0, 40, 40, 0xFFFFFFFF);
        px = rp.resolve();
        check("restore lifts the clip again", (px[30 * 40 + 5] & 0xFF) > 200);

        // Clips must intersect, never widen.
        RasterPainter rq = new RasterPainter(40, 40, 1);
        rq.clear(0xFF000000);
        rq.save();
        rq.clipRect(0, 0, 20, 20);
        rq.clipRect(0, 0, 40, 40);
        rq.fillRect(0, 0, 40, 40, 0xFFFFFFFF);
        rq.restore();
        px = rq.resolve();
        check("a second clip cannot widen the first", (px[30 * 40 + 30] & 0xFF) < 40);
    }

    private static void waves(Layout L) {
        group("stage waves");
        GameCore c = new GameCore(new Mem(), 41L);
        c.startGame();
        check("stage starts with nothing released", c.spawnedThisStage == 0);
        check("quota grows with stage", quotaAt(c, 8) > quotaAt(c, 1));
        check("quota is capped", quotaAt(c, 99) <= 10);
        c.stage = 1;

        int frames = 0, stageChanges = 0;
        int prevStage = c.stage;
        boolean overQuota = false, spawnedEarly = false, emptyOnAdvance = true;

        // Play perfectly through several waves, watching the wave invariants every frame.
        while (frames < 60 * 400 && stageChanges < 4 && c.state == GameCore.PLAY) {
            c.update(DT, L);
            frames++;

            if (c.spawnedThisStage > c.stageQuota()) overQuota = true;
            if (c.stage != prevStage) {
                stageChanges++;
                // Sampled on the advancing frame itself: the last word of a wave leaves the
                // field during that same frame, so the previous frame can still hold it.
                if (!c.enemies.isEmpty()) emptyOnAdvance = false;
                if (c.spawnedThisStage != 0) spawnedEarly = true;
                prevStage = c.stage;
            }

            if (frames % 2 != 0) continue;
            GameCore.Enemy e =
                    c.target != null && c.enemies.contains(c.target) && c.target.typeable()
                            ? c.target : urgent(c);
            if (e != null && e.pos < e.word.length) c.tapKey(e.word[e.pos], L);
        }

        System.out.printf("    %d stage transitions in %.0fs, reached stage %d%n",
                stageChanges, frames * DT, c.stage);
        check("stages do advance", stageChanges >= 3);
        check("a stage never releases more than its quota", !overQuota);
        check("field is empty when a stage advances", emptyOnAdvance);
        check("next wave starts from zero released", !spawnedEarly);

        // Once the quota is out and the field is clear, the gap must hold off the next wave.
        GameCore w = new GameCore(new Mem(), 42L);
        w.startGame();
        w.spawnedThisStage = w.stageQuota();
        w.enemies.clear();
        w.shots.clear();
        check("quota exhausted means the stage is cleared", w.stageCleared());
        int before = w.stage;
        w.update(DT, L);
        check("clearing the wave advances the stage", w.stage == before + 1);
        check("a breather follows the wave", w.stageGap > 0f);
        int released = w.spawnedThisStage;
        advance(w, L, GameCore.STAGE_GAP * 0.6f);
        check("nothing spawns during the breather",
                w.spawnedThisStage == released && w.enemies.isEmpty());
        advance(w, L, GameCore.STAGE_GAP);
        check("the next wave starts after the breather", w.spawnedThisStage > 0);

        // A breached word still counts as resolved, so a stage cannot stall on a miss.
        GameCore b = new GameCore(new Mem(), 43L);
        b.startGame();
        b.spawnedThisStage = b.stageQuota();
        b.enemies.clear();
        add(b, L, new int[] {0}, L.dangerY - L.enemyR + 1);
        // A second word keeps the field occupied, so the stage cannot advance and reset the
        // counter before it can be observed.
        add(b, L, new int[] {3, 3}, L.playTop + 40);
        int resolved = b.resolvedThisStage;
        int stageWas = b.stage;
        advance(b, L, GameCore.ATTACK_TIME + 2 * DT);
        check("a breached word counts as resolved", b.resolvedThisStage == resolved + 1);
        check("a breach alone does not advance the stage", b.stage == stageWas);

        // Clear the survivor: the stage must then advance despite one word being lost.
        b.enemies.clear();
        b.shots.clear();
        b.update(DT, L);
        check("stage still advances after a breach", b.stage == stageWas + 1);
    }

    private static int quotaAt(GameCore c, int stage) {
        int was = c.stage;
        c.stage = stage;
        int q = c.stageQuota();
        c.stage = was;
        return q;
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
        int[] need = new int[word.length];
        for (int i = 0; i < need.length; i++) need[i] = 1;
        return add(c, L, word, need, y);
    }

    private static GameCore.Enemy add(GameCore c, Layout L, int[] word, int[] need, float y) {
        GameCore.Enemy e = new GameCore.Enemy();
        e.word = word;
        e.need = need;
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
